# BMN Tender System

**"Buyer is King" - A Digital Marketplace Simulation**

The **BMN Tender System** is a desktop application where you (the Buyer) are in control. Instead of browsing a store, you tell the market what you want, and Sellers compete to offer you the best deal.

## How It Works
1.  **Ask**: Type a request (e.g., "I want Nasi Goreng") in the chat.
2.  **Receive**: Sellers instantly reply with offers (Product, Price, Rating).
3.  **Choose**: The system highlights the best deal ("King's Choice"), but **YOU** decide what to buy. Select one, many, or none!
4.  **Buy**: Add your choices to the cart and see the total.

## How to Run

### Requirements
- **Java JDK 17** or higher.
- **Maven** installed and added to PATH.

### Linux / macOS
1.  Open your terminal in the project folder.
2.  Run the helper script:
    ```bash
    chmod +x run.sh  # (Only needed once)
    ./run.sh
    ```

### Windows
1.  Open Command Prompt (cmd) or PowerShell in the project folder.
2.  Run the batch script:
    ```cmd
    run.bat
    ```

---

## Technical Highlights
- **"Fantastic" Design**: Vibrant, randomized Seller colors and bold typography.
- **Recommendations**: An AI engine calculates the best value (`Rating / Price`) and highlights it for you.
- **Smart Formatting**: Prices automatically format to Rupiah (e.g., `100.000`).

## Latest Features (2026 Update)

- **Quantity-aware Offers & Packages**
  - Sellers can set **quantity** for each product; the system calculates **unit price × qty** and total correctly.
  - Buyer requests like `fruit tea 2 hot tea 3` are parsed and matched to offers so totals like `2×9k + 3×6k = 36k` appear correctly in cart and payment history.

- **Three-Window Marketplace Layout**
  - **Buyer** (left): chat-style request input, parsed-order hints, live cart with total and delivery address, no horizontal scrolling.
  - **Recommended for Buyer** (middle): a dedicated column showing **grouped offers by seller** in a clean 2-column grid of cards:
    - Each seller shows a **package total** and list of items.
    - Buyer can **add entire package** or **only selected items** directly into the cart.
  - **Seller Display** (right): consistent-height seller cards for all merchants, with vertical scrolling so merchants stay tidy and readable.
  - Layout uses resizable split panes so you can drag dividers to decide which window is biggest.

- **Seller Contact Popup with Real Map Preview**
  - Each merchant has a **contact & info popup**:
    - Seller name, short ID, phone/WhatsApp, and address.
    - **Static map preview** (OpenStreetMap-based) rendered directly inside the dialog.
    - **Open full map** button opens the location in your default browser.
    - **WhatsApp chat** button opens a WA conversation using normalized Indonesian phone numbers.

- **Clean Text Layout (No Sideways Scrolling)**
  - Chat bubbles and system messages use fixed content widths and HTML wrapping, so messages stay **vertical** and easy to read.
  - The “Recommended for Buyer” grid uses fixed-width card content and disables horizontal scroll for a **stable, dashboard-like feel**.

- **🤖 Agentic Processing (Groq-powered AI Auto-Win)**
  - Sellers now have access to a **"🤖 Agentic AI"** button.
  - When clicked, a self-contained Groq agent (using the powerful `llama-3.3-70b-versatile` model) analyzes the buyer's request and automatically constructs a competitive package.
  - If competing offers exist, the AI will actively try to undercut the best score by ≥10% to ensure the seller "wins" the tender.
  - Securely configured to load the Groq API Key from a local `.env` file to prevent leaks.

- **🗄️ Database Processing Architecture (Supabase / PostgreSQL)**
  - A comprehensive blueprint has been designed to migrate the in-memory Java application logic directly into **Supabase PostgreSQL**.
  - **Triggers**: Auto-calculate the AI Score for each offer the moment it hits the database.
  - **Stored Procedures**: Manage transactional atomic checkouts, linking offers to payments and marking requests as closed securely.
  - **Functions**: Fetch best-ranked offers directly from the database, reducing server-side processing overhead.
