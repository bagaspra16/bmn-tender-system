<style>
    body, p, h1, h2, h3, h4, h5, h6, li, td, th, div{
        color: black !important;
    }
</style>

# Polymorphism in the Codebase: Overloading & Overriding

Based on your codebase, there are excellent examples of both **Overloading** (Compile-time Polymorphism) and **Overriding** (Runtime Polymorphism). 

Here is a detailed breakdown using the `Seller.java` and `TenderController.java` classes to explain how both concepts work and the core differences between compile-time and runtime execution.

## 1. Compile-Time Polymorphism (Overloading)

**Compile-time polymorphism** (also known as static binding or early binding) occurs when multiple methods or constructors have the same name but differ in their parameter lists (different number of parameters, different types, or both). The Java compiler determines which method to execute at compile time based on the method signature.

### Example A: Method Overloading (`TenderController.java`)
The `TenderController` class has two `postRequest` methods with different parameters:

```java
// Method 1: Takes query, preferences, and buyerAddress
public void postRequest(String query, String preferences, String buyerAddress) {
    currentUser.setAddress(buyerAddress);
    TenderRequest request = new TenderRequest(currentUser, query, preferences, buyerAddress);
    // ...
}

// Method 2: Takes only query and preferences (backward-compatible overload)
public void postRequest(String query, String preferences) {
    postRequest(query, preferences, currentUser.getAddress());
}
```

### Example B: Constructor Overloading (`Seller.java`)
The `Seller` class also demonstrates this through its constructors:

```java
public Seller(String name) { /* ... */ }
public Seller(String name, String contactId) { /* ... */ }
public Seller(String name, String contactId, String address) { /* ... */ }
```

**Why it is Compile-Time Polymorphism:**
When you write a line of code calling `postRequest("Nasi Padang", "Pedas")`, the Java compiler immediately checks the arguments (two strings) and permanently links that line of code to the second `postRequest` method. This decision is finalized *before* the program ever runs. The execution path is statically bound during compilation.

## 2. Runtime Polymorphism (Overriding)

**Runtime polymorphism** (also known as dynamic binding or late binding) occurs when a subclass provides a specific implementation for a method that is already defined in its parent class. The JVM determines which method to call at runtime based on the actual object type, not the reference variable type.

### Example: Method Overriding (`Seller.java` and `Payment.java`)

In Java, every class implicitly inherits from the `Object` class, which provides a default `toString()` method. The `Seller` and `Payment` classes **override** this method to provide their own specific behavior.

From `Seller.java`:
```java
@Override
public String toString() {
    return name;
}
```

From `Payment.java`:
```java
@Override
public String toString() {
    return String.format("Order #%s — Rp %,.0f (%s)", id, totalAmount, getFormattedTimestamp());
}
```

**Why it is Runtime Polymorphism:**
Imagine a scenario where the UI components (like a `JList` or `JComboBox` in `SellerInputCard`) hold a list of references of type `Object`, but those objects are actually `Seller` instances:

```java
Object myItem = new Seller("Toko C", "0812345");
System.out.println(myItem.toString());
```

1. **At Compile-Time**: The compiler sees `myItem` as an `Object` reference. It checks if the `Object` class has a `toString()` method (it does). The code compiles successfully. However, the compiler *does not* know exactly which `toString()` will be executed.
2. **At Runtime**: The Java Virtual Machine (JVM) looks at the actual object created in memory (which is a `Seller` object). It dynamically decides to call the overridden `toString()` method inside the `Seller` class, outputting `"Toko C"` instead of the default memory address string provided by the parent `Object` class.

## Summary: Compile-Time vs. Runtime

| Feature | Compile-Time Polymorphism (Overloading) | Runtime Polymorphism (Overriding) |
| :--- | :--- | :--- |
| **Binding Time** | Resolved by the compiler **before** the program runs (Early Binding). | Resolved by the JVM **while** the program is running (Late Binding). |
| **Mechanism** | Achieved by having multiple methods/constructors with the same name but different parameters within the same class. | Achieved by redefining a parent class's method in a child class with the exact same signature. |
| **Examples in Code** | `TenderController.postRequest(...)` methods.<br>`Seller(...)` constructors. | `@Override public String toString()` in `Seller.java` and `Payment.java`. |
