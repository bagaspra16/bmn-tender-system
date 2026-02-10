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
