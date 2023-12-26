package coding.capstone.product_recommender;

public class Product {
    private final long id;
    private final String brand;
    private final String category;
    private final double price;

    public Product(long id, String brand, String category, double price) {
        this.id = id;
        this.brand = brand;
        this.category = category;
        this.price = price;
    }
    @Override
    public String toString() {
        /** Defines how Product information is displayed as a string */
       return "Product ID: "  + this.id + ",\nCategory: " + this.category + ",\nBrand: " + this.brand + ",\nPrice: $" + this.price;
    }
    /** @return the product's ID **/
    public long getId() {
        return id;
    }
    /** @return the product's brand **/
    public String getBrand() {
        return brand;
    }
    /** @return the product's category **/
    public String getCategory() {
        return category;
    }
    /** @return the product's price **/
    public double getPrice() {
        return price;
    }
}
