package negotiation.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item being negotiated.
 * Based on the Craigslist Bargains dataset structure.
 */
public class Item {
    private String category;
    private String title;
    private String description;
    private double listPrice;
    private List<String> images;
    
    // For multi-issue negotiation (Deal or No Deal style)
    private int count;
    private double[] values; // values for each agent
    
    public Item(String category, String title, String description, double listPrice) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.listPrice = listPrice;
        this.images = new ArrayList<>();
        this.count = 1;
        this.values = new double[2];
    }
    
    /**
     * Constructor for multi-issue negotiation items
     */
    public Item(String category, int count, double buyerValue, double sellerValue) {
        this.category = category;
        this.title = category;
        this.description = "";
        this.count = count;
        this.values = new double[]{buyerValue, sellerValue};
        this.images = new ArrayList<>();
        this.listPrice = sellerValue;
    }
    
    // Getters and setters
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getListPrice() { return listPrice; }
    public void setListPrice(double listPrice) { this.listPrice = listPrice; }
    
    public List<String> getImages() { return images; }
    public void addImage(String imagePath) { this.images.add(imagePath); }
    
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    
    public double getBuyerValue() { return values[0]; }
    public double getSellerValue() { return values[1]; }
    public void setValues(double buyerValue, double sellerValue) {
        this.values[0] = buyerValue;
        this.values[1] = sellerValue;
    }
    
    @Override
    public String toString() {
        return String.format("Item{category='%s', title='%s', price=%.2f}", 
                           category, title, listPrice);
    }
}

