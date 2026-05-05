-- =====================================================================
-- BMN Tender System — Supabase / PostgreSQL schema
-- Project: TenderSystem  (gjrfchvevxevgmihowsf.supabase.co)
-- Run this in the Supabase SQL Editor once.
-- =====================================================================

-- ----- Types -----
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('BUYER', 'SELLER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ----- Tables -----
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role        user_role NOT NULL,
    name        VARCHAR(255) NOT NULL,
    phone       VARCHAR(50),
    address     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id   UUID REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tender_requests (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id      UUID REFERENCES users(id) ON DELETE CASCADE,
    query         TEXT NOT NULL,
    preferences   TEXT,
    buyer_address TEXT NOT NULL,
    status        VARCHAR(50) DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS offers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id   UUID REFERENCES tender_requests(id) ON DELETE CASCADE,
    seller_id    UUID REFERENCES users(id) ON DELETE CASCADE,
    product_id   UUID REFERENCES products(id) ON DELETE SET NULL,
    price        NUMERIC(15, 2) NOT NULL,
    quantity     INT DEFAULT 1,
    rating       NUMERIC(3, 2) DEFAULT 0.0,
    comment      TEXT,
    ai_score     NUMERIC(15, 2) DEFAULT 0.0,
    is_selected  BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_offers_request ON offers(request_id);
CREATE INDEX IF NOT EXISTS idx_offers_score   ON offers(request_id, ai_score DESC);

-- Payments use a short 8-char human-friendly code as primary key
-- (matches the existing Java Payment.id format).
CREATE TABLE IF NOT EXISTS payments (
    id            VARCHAR(8) PRIMARY KEY,
    buyer_id      UUID REFERENCES users(id) ON DELETE CASCADE,
    total_amount  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    buyer_address TEXT NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payment_items (
    payment_id  VARCHAR(8) REFERENCES payments(id) ON DELETE CASCADE,
    offer_id    UUID       REFERENCES offers(id)   ON DELETE CASCADE,
    price       NUMERIC(15, 2) NOT NULL,
    quantity    INT NOT NULL,
    PRIMARY KEY (payment_id, offer_id)
);

-- =====================================================================
-- Trigger: auto-calc ai_score on offers ((rating / price) * 10000)
-- =====================================================================
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

DROP TRIGGER IF EXISTS trg_calculate_offer_score ON offers;
CREATE TRIGGER trg_calculate_offer_score
BEFORE INSERT OR UPDATE ON offers
FOR EACH ROW EXECUTE FUNCTION calculate_offer_ai_score();

-- =====================================================================
-- Stored procedure: atomic checkout
-- =====================================================================
CREATE OR REPLACE PROCEDURE process_checkout(
    p_payment_id    VARCHAR,
    p_buyer_id      UUID,
    p_request_id    UUID,
    p_offer_ids     UUID[],
    p_buyer_address TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_total NUMERIC := 0;
    r RECORD;
BEGIN
    INSERT INTO payments (id, buyer_id, total_amount, buyer_address)
    VALUES (p_payment_id, p_buyer_id, 0, p_buyer_address);

    FOR r IN SELECT id, price, quantity FROM offers WHERE id = ANY(p_offer_ids)
    LOOP
        INSERT INTO payment_items (payment_id, offer_id, price, quantity)
        VALUES (p_payment_id, r.id, r.price, r.quantity);

        v_total := v_total + (r.price * r.quantity);

        UPDATE offers SET is_selected = TRUE WHERE id = r.id;
    END LOOP;

    UPDATE payments SET total_amount = v_total WHERE id = p_payment_id;

    IF p_request_id IS NOT NULL THEN
        UPDATE tender_requests SET status = 'CLOSED' WHERE id = p_request_id;
    END IF;
END;
$$;

-- =====================================================================
-- Seed data: 10 dummy sellers + 1 default buyer
-- (Idempotent: only inserts when the table is empty.)
-- =====================================================================
INSERT INTO users (role, name, phone, address)
SELECT 'BUYER', 'King Buyer 👑', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'BUYER' AND name = 'King Buyer 👑');

INSERT INTO users (role, name, phone, address)
SELECT * FROM (VALUES
    ('SELLER'::user_role, 'Merchant #1',  '081234567890', 'Jl. Sudirman No. 12, Jakarta Pusat'),
    ('SELLER'::user_role, 'Merchant #2',  '082198765432', 'Jl. Thamrin 45, Jakarta Selatan'),
    ('SELLER'::user_role, 'Merchant #3',  '085711112222', 'Jl. Gatot Subroto 88, Jakarta'),
    ('SELLER'::user_role, 'Merchant #4',  '087833334444', 'Jl. Rasuna Said Kav 1, Kuningan'),
    ('SELLER'::user_role, 'Merchant #5',  '081355556666', 'Jl. MH Thamrin 28, Jakarta'),
    ('SELLER'::user_role, 'Merchant #6',  '081977778888', 'Jl. HR Rasuna Said, Setiabudi'),
    ('SELLER'::user_role, 'Merchant #7',  '085299990000', 'Jl. Jenderal Sudirman Kav 52, Jakarta'),
    ('SELLER'::user_role, 'Merchant #8',  '083122223333', 'Jl. Prof. Dr. Satrio Kav 18, Jakarta'),
    ('SELLER'::user_role, 'Merchant #9',  '084544445555', 'Jl. Kuningan Barat 1, Jakarta'),
    ('SELLER'::user_role, 'Merchant #10', '086766667777', 'Jl. Senopati 23, Kebayoran Baru')
) AS s(role, name, phone, address)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'SELLER');
