# BMN Tender System: Server-Side Database Migration Proposal (Supabase/PostgreSQL)

This document outlines a strategy to migrate the in-memory Java application logic (like in `TenderController.java`) directly into a powerful backend database like **PostgreSQL (via Supabase)**.

By leveraging PostgreSQL's advanced features such as **Stored Procedures, Functions, and Triggers**, we can handle the heavy lifting and business rules directly in the database. This allows your backend server (or serverless Edge Functions in Supabase) to remain thin, fast, and scalable.

---

## 1. Entity-Relationship Schema

First, we design the database tables to mirror your existing domain models (`Buyer`, `Seller`, `TenderRequest`, `Offer`, `Product`, `Payment`).

```sql
-- ENUM for User Roles
CREATE TYPE user_role AS ENUM ('BUYER', 'SELLER');

-- 1. Users Table (Handles both Buyers and Sellers)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role user_role NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 2. Products Table (Belongs to Sellers)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 3. Tender Requests (Posted by Buyers)
CREATE TABLE tender_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    query TEXT NOT NULL,
    preferences TEXT,
    buyer_address TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- 'ACTIVE', 'CLOSED'
    created_at TIMESTAMP DEFAULT NOW()
);

-- 4. Offers (Submitted by Sellers for a Tender Request)
CREATE TABLE offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID REFERENCES tender_requests(id) ON DELETE CASCADE,
    seller_id UUID REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    price NUMERIC(15, 2) NOT NULL,
    quantity INT DEFAULT 1,
    rating NUMERIC(3, 2) DEFAULT 0.0,
    comment TEXT,
    ai_score NUMERIC(15, 2) DEFAULT 0.0, -- Will be auto-calculated by DB
    is_selected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 5. Payments (Completed Checkouts)
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    total_amount NUMERIC(15, 2) NOT NULL,
    buyer_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 6. Payment Items (Links Offers to Payments)
CREATE TABLE payment_items (
    payment_id UUID REFERENCES payments(id) ON DELETE CASCADE,
    offer_id UUID REFERENCES offers(id) ON DELETE CASCADE,
    price NUMERIC(15, 2) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY(payment_id, offer_id)
);
```

---

## 2. In-Database Processing Logic (PL/pgSQL)

We will recreate the logic found in `TenderController.java` inside PostgreSQL.

### A. Auto-Calculating AI Score using Triggers

In the Java code, `aiScore` is calculated as `(rating / price) * 10000`. We can automate this in Postgres using a `BEFORE INSERT OR UPDATE` trigger.

```sql
-- Function to calculate the AI Score
CREATE OR REPLACE FUNCTION calculate_offer_ai_score()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.price > 0 THEN
        NEW.ai_score := (NEW.rating / NEW.price) * 10000;
    ELSE
        NEW.ai_score := 0;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger attached to the offers table
CREATE TRIGGER trg_calculate_offer_score
BEFORE INSERT OR UPDATE ON offers
FOR EACH ROW
EXECUTE FUNCTION calculate_offer_ai_score();
```
*Benefit:* The backend server never has to calculate scores. It just inserts the offer, and the database computes the score instantly.

---

### B. Fetching the "Best Offers" (Sorting via Database)

Instead of pulling all offers into Java and sorting them, the database does it naturally through indexing and SQL functions.

```sql
-- Function to retrieve sorted offers by request_id
CREATE OR REPLACE FUNCTION get_best_offers(p_request_id UUID)
RETURNS TABLE (
    offer_id UUID,
    seller_name VARCHAR,
    product_name VARCHAR,
    price NUMERIC,
    quantity INT,
    rating NUMERIC,
    ai_score NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        o.id, u.name, p.name, o.price, o.quantity, o.rating, o.ai_score
    FROM offers o
    JOIN users u ON o.seller_id = u.id
    JOIN products p ON o.product_id = p.id
    WHERE o.request_id = p_request_id
    ORDER BY o.ai_score DESC;
END;
$$ LANGUAGE plpgsql;
```
*Benefit:* Calling `SELECT * FROM get_best_offers('request-uuid');` via Supabase API instantly returns the ranked list, minimizing network payload.

---

### C. Checkout Transaction (Stored Procedure)

Checkout requires atomicity (all or nothing). We need to create a payment record, link the selected offers to the payment, and mark the tender request as closed. This is perfect for a database transaction.

```sql
CREATE OR REPLACE PROCEDURE process_checkout(
    p_buyer_id UUID,
    p_request_id UUID,
    p_offer_ids UUID[],
    p_buyer_address TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_payment_id UUID;
    v_total_amount NUMERIC := 0;
    v_offer_record RECORD;
BEGIN
    -- 1. Create the Payment Record (temporarily with 0 total)
    INSERT INTO payments (buyer_id, total_amount, buyer_address)
    VALUES (p_buyer_id, 0, p_buyer_address)
    RETURNING id INTO v_payment_id;

    -- 2. Process each selected offer
    FOR v_offer_record IN 
        SELECT id, price, quantity FROM offers WHERE id = ANY(p_offer_ids)
    LOOP
        -- Insert into payment_items
        INSERT INTO payment_items (payment_id, offer_id, price, quantity)
        VALUES (v_payment_id, v_offer_record.id, v_offer_record.price, v_offer_record.quantity);
        
        -- Accumulate total amount
        v_total_amount := v_total_amount + (v_offer_record.price * v_offer_record.quantity);

        -- Mark offer as selected
        UPDATE offers SET is_selected = TRUE WHERE id = v_offer_record.id;
    END LOOP;

    -- 3. Update the final total amount on the payment
    UPDATE payments SET total_amount = v_total_amount WHERE id = v_payment_id;

    -- 4. Close the tender request
    UPDATE tender_requests SET status = 'CLOSED' WHERE id = p_request_id;
    
    -- Transaction automatically commits if no exceptions occur
END;
$$;
```
*Benefit:* Using a stored procedure guarantees data consistency. If anything fails (e.g., invalid offer ID), the entire checkout rolls back automatically.

---

### D. Advanced Text Processing in DB (Optional)

The Java method `parseOrderText("fruit tea 2 hot tea 3")` can actually be moved to Postgres using advanced Regular Expressions, though this is often easier in application code. Here is how a database function could parse it:

```sql
CREATE OR REPLACE FUNCTION parse_order_text(p_text TEXT)
RETURNS TABLE(product_name TEXT, quantity INT) AS $$
BEGIN
    RETURN QUERY
    WITH matches AS (
        -- Extracts text followed by an optional number
        SELECT regexp_matches(p_text, '([a-zA-Z\s]+?)\s*(\d+)?(?=\s+[a-zA-Z]|$)', 'g') AS match
    )
    SELECT 
        TRIM(match[1]) AS product_name, 
        COALESCE(NULLIF(match[2], '')::INT, 1) AS quantity
    FROM matches;
END;
$$ LANGUAGE plpgsql;

-- Usage: SELECT * FROM parse_order_text('fruit tea 2 hot tea 3');
-- Returns:
-- product_name | quantity
-- -------------+---------
-- fruit tea    | 2
-- hot tea      | 3
```

---

## 3. Real-time Subscriptions (Supabase specific feature)

In your Java code, you used `TenderListener` to notify the UI when new requests or offers are submitted.

Supabase provides **Realtime WebSockets** natively. Instead of custom listeners, the frontend simply subscribes to database changes:

```javascript
// Supabase JS Example for listening to new offers instantly
const subscription = supabase
  .channel('public:offers')
  .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'offers' }, payload => {
    console.log('New offer received!', payload.new);
  })
  .subscribe();
```

## Conclusion

By moving the processing into PostgreSQL:
1. **Performance:** Sorting `AI Scores` and managing data constraints happen at the C-level speed of the database engine.
2. **Data Integrity:** Checkout procedures ensure you never get half-complete orders.
3. **Reduced Code:** You eliminate most of the `TenderController.java` file, replacing it with a simple Supabase API client that interacts with these secure Database Functions and Procedures.
