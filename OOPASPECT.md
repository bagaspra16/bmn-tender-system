## BMN Tender System – OOP Aspects

This document explains how Object-Oriented Programming (OOP) concepts are applied in the BMN Tender System, and which exact classes / structures use each aspect.

---

## 1. Encapsulation

**Idea**: Hide internal data and expose only what is needed through methods (getters/setters or behaviors).

- **Model classes (data + logic)**
  - `Buyer` (`src/main/java/com/bnm/tender/model/Buyer.java`)
    - Fields:
      - `private String id;`
      - `private String name;`
      - `private String address;`
    - Access via:
      - `public String getId()`, `getName()`, `getAddress()`
      - `public void setAddress(String address)`
  - `Seller` (`src/main/java/com/bnm/tender/model/Seller.java`)
    - Fields:
      - `private String id;`
      - `private String name;`
      - `private String contactId;`
      - `private String address;`
    - Access via:
      - `getId()`, `getName()`, `getContactId()`, `getAddress()`
      - `setContactId(...)`, `setAddress(...)`
  - `Product`, `TenderRequest`, `Payment`, `Offer`
    - All follow the same pattern: `private` fields with `public` getters (and setters where needed).
    - Example: `Offer` exposes `getTotalPrice()` instead of exposing `price` and `quantity` publicly.

- **View classes (UI panels)**
  - `BuyerPanel`, `SellerPanel`, `RecommendationPanel`, `SellerInputCard`, `OrderHistoryPanel`, `SellerContactDialog`
    - All keep Swing components `private` (e.g., `private JTextField requestInput;`, `private List<Offer> selectedOffers;`).
    - External code cannot directly manipulate internal components; it uses **public methods** such as `addOfferToCart(Offer offer)` in `BuyerPanel`.

**Why this is encapsulation**: Internal state (fields and Swing components) is hidden. Only specific methods are exposed, keeping each class responsible for its own correctness and UI behavior.

---

## 2. Inheritance

**Idea**: Create specialized classes based on a base class, inheriting its behavior.

- **Swing UI inheritance**
  - `MainFrame extends JFrame`
    - Gains window behavior (title bar, close/minimize/maximize) from `JFrame`.
  - `BuyerPanel extends JPanel`
  - `SellerPanel extends JPanel`
  - `SellerInputCard extends JPanel`
  - `RecommendationPanel extends JPanel`
  - `OrderHistoryPanel extends JPanel`
    - All inherit drawing, layout, and event-handling support from `JPanel`.
  - `SellerContactDialog extends JDialog`
    - Inherits dialog-window behavior (modal popup, close button, etc.).

**Where to see it**:
- In each of these classes, the first line is `public class X extends JPanel` or `extends JFrame` / `extends JDialog`.

**Why this matters**: We reuse the Swing framework’s behavior and only implement our **custom parts** (layout, buttons, labels) instead of reinventing basic window/panel logic.

---

## 3. Abstraction (Controller as API)

**Idea**: Define clear methods (an API) that hide implementation details from callers.

- **`TenderController` as application API**
  - File: `src/main/java/com/bnm/tender/controller/TenderController.java`
  - Exposes high-level operations:
    - `public void postRequest(String query, String preferences, String buyerAddress)`
    - `public void submitOffer(String requestId, Offer offer)`
    - `public List<Offer> getBestOffers(String requestId)`
    - `public Payment checkout(List<Offer> selectedOffers, String buyerAddress)`
    - `public Map<String, Integer> parseOrderText(String text)`
    - `public List<Seller> getSellers()`
    - `public List<Payment> getPaymentHistory()`
    - `public Buyer getCurrentUser()`
  - Views do **not** need to know how offers are stored or how scores are computed; they simply call these methods.

- **`TenderListener` interface**
  - Declares:
    - `void onNewRequest(TenderRequest request);`
    - `void onNewOffer(String requestId, Offer offer);`
    - `default void onPaymentCompleted(Payment payment) {}`
  - Abstracts the idea of “someone interested in updates” without knowing if it’s the Buyer panel, Seller panel, Recommendations, or Order History.

**Why this is abstraction**: UI classes interact with the controller through **well-defined methods**, not by directly manipulating model lists or internal maps. The controller “API” abstracts the marketplace logic.

---

## 4. Polymorphism

**Idea**: Same interface / method call, but different behavior depending on the actual class at runtime.

- **`TenderListener` polymorphism**
  - Implemented by:
    - `BuyerPanel implements TenderController.TenderListener`
    - `SellerPanel implements TenderController.TenderListener`
    - `RecommendationPanel implements TenderController.TenderListener`
    - `OrderHistoryPanel implements TenderController.TenderListener`
  - `TenderController` holds a `List<TenderListener> listeners` and calls:
    - `listener.onNewRequest(request);`
    - `listener.onNewOffer(requestId, offer);`
    - `listener.onPaymentCompleted(payment);`
  - At runtime:
    - `BuyerPanel` adds chat bubbles and info messages.
    - `SellerPanel` updates the list of incoming requests on the left.
    - `RecommendationPanel` redraws recommendation cards in the middle window.
    - `OrderHistoryPanel` refreshes the order history list.

- **Swing renderer polymorphism**
  - In `SellerPanel`, an anonymous subclass of `DefaultListCellRenderer` overrides `getListCellRendererComponent(...)` to render each `TenderRequest` with:
    - Bold query text and smaller address text.
    - Different background when selected.

**Why this is polymorphism**: The controller calls the **same methods** on all listeners, but each concrete class responds with its own behavior.

---

## 5. Overloading

**Idea**: Same method name, different parameter lists (compile-time polymorphism).

- **`Offer` constructors**
  - In `Offer` (`src/main/java/com/bnm/tender/model/Offer.java`):
    - Full constructor:
      - `Offer(Seller seller, Product product, double price, int quantity, double rating, String comment, TenderRequest request)`
    - Convenience constructor:
      - `Offer(Seller seller, Product product, double price, double rating, String comment, TenderRequest request)`
        - Delegates to the full one with `quantity` defaulted to 1.

- **`TenderController.postRequest`**
  - `public void postRequest(String query, String preferences, String buyerAddress)`
  - `public void postRequest(String query, String preferences)`
    - Overload that uses current buyer’s address (`currentUser.getAddress()`).

**Why this is overloading**: It provides multiple ways to call the same logical operation, depending on how much information the caller has.

---

## 6. Overriding

**Idea**: Subclass or implementing class provides its own implementation of a method defined in a parent class / interface.

- **`TenderListener` implementations**
  - `BuyerPanel`, `SellerPanel`, `RecommendationPanel`, `OrderHistoryPanel`
    - Override:
      - `onNewRequest(TenderRequest request)`
      - `onNewOffer(String requestId, Offer offer)`
      - `onPaymentCompleted(Payment payment)` (where needed)
    - `OrderHistoryPanel` overrides `onPaymentCompleted` to call `refreshList()` and redraw cards.

- **Custom list cell renderer**
  - In `SellerPanel`, anonymous `DefaultListCellRenderer` overrides:
    - `getListCellRendererComponent(...)`
    - Adds HTML styling, custom background, and separators between requests.

- **`toString()` in model classes**
  - `Offer.toString()` returns a human-readable summary: `sellerName - productName xqty @ price (rating*)`.
  - `Payment.toString()` summarizes order ID, amount, and formatted timestamp.

**Why this is overriding**: We take a method signature defined in an interface/base class and provide a more specific implementation for our needs.

---

## 7. Reusability

**Idea**: Write code once and reuse it in many places instead of duplicating logic.

- **`StyleUtil` (`src/main/java/com/bnm/tender/util/StyleUtil.java`)**
  - Reused styling across all views:
    - Colors (`BG_CREAM`, `VIBRANT_MINT`, etc.).
    - Fonts (`FONT_HEADER`, `FONT_BODY`, `FONT_SMALL`).
    - Icons (emoji for user, seller, product, price, rating, cart, send).
    - Methods:
      - `styleHeader(JLabel label)`
      - `styleActionButton(JButton button, Color bgColor)`
      - `getSellerColor(String sellerName)` – sends each seller card a stable pastel color.
      - `formatRupiah(double amount)` – used everywhere for consistent price formatting.

- **`SellerInputCard` reused for all sellers**
  - In `SellerPanel`, for each `Seller` from `TenderController.getSellers()`:
    - Creates one `SellerInputCard(seller)`.
  - One implementation of:
    - Input validation (`name` and `price` non-empty).
    - Price formatting (`100000` → `100.000`).
    - Quantity handling via `quantitySpinner`.
    - Offer creation (`new Offer(...)`) and submission.

- **`RecommendationPanel`**
  - Single implementation handles **any number of requests and sellers**:
    - Groups `Offer` objects by `Seller`.
    - Displays them in 2-column grid cards.
    - Provides “Add whole package” and “Add selected items” buttons for each seller.
  - Works only by reading `TenderController.getBestOffers(requestId)`; no per-request custom code needed.

**Why this is reusability**: Shared logic is centralized and applied everywhere, reducing bugs and making style / behavior changes much easier.

---

## 8. Where each OOP concept appears (quick reference table)

| Concept         | Main Classes / Files                                                                                         | Example usage                                                                                      |
|----------------|---------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Encapsulation  | `Buyer`, `Seller`, `Offer`, `Product`, `TenderRequest`, `Payment`, all `*Panel` classes                      | Private fields + public getters/setters; UI components hidden behind behaviors.                   |
| Inheritance    | `MainFrame`, `BuyerPanel`, `SellerPanel`, `SellerInputCard`, `RecommendationPanel`, `OrderHistoryPanel`, `SellerContactDialog` | Extend Swing base classes (`JFrame`, `JPanel`, `JDialog`) to get window/panel behavior.           |
| Abstraction    | `TenderController`, `TenderListener`                                                                          | Controller methods as API (`postRequest`, `submitOffer`, `checkout`); listeners abstract UI updates. |
| Polymorphism   | All `TenderListener` implementations, custom `DefaultListCellRenderer`                                        | Same method calls (`onNewRequest`, `onNewOffer`) produce different behavior in different panels.  |
| Overloading    | `Offer` constructors, `TenderController.postRequest`                                                          | Same method name with different parameters for convenience and defaults.                          |
| Overriding     | `onNewRequest`, `onNewOffer`, `onPaymentCompleted`, `getListCellRendererComponent`, `toString()` in models   | Custom implementations replacing or enhancing base/interface behavior.                            |
| Reusability    | `StyleUtil`, `SellerInputCard`, `RecommendationPanel`, `TenderController`                                    | Shared utility methods, reusable card component for every seller, central business logic.         |