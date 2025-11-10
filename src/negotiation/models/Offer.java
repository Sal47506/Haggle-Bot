package negotiation.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an offer in the negotiation.
 * Can handle both single-issue (price only) and multi-issue bargaining.
 */
public class Offer {
    private double price;
    private int quantity;
    private boolean warranty;
    private boolean freeDelivery;
    private Map<String, Object> extras;
    
    // For multi-issue negotiation (Deal or No Deal style)
    private Map<String, Integer> itemAllocation; // item category -> count allocated to proposer
    
    private long timestamp;
    
    public Offer(double price) {
        this.price = price;
        this.quantity = 1;
        this.warranty = false;
        this.freeDelivery = false;
        this.extras = new HashMap<>();
        this.itemAllocation = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public Offer(double price, int quantity) {
        this(price);
        this.quantity = quantity;
    }
    
    /**
     * Constructor for multi-issue negotiation
     */
    public Offer(Map<String, Integer> itemAllocation) {
        this.itemAllocation = new HashMap<>(itemAllocation);
        this.price = 0.0;
        this.quantity = 1;
        this.warranty = false;
        this.freeDelivery = false;
        this.extras = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and setters
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public boolean hasWarranty() { return warranty; }
    public void setWarranty(boolean warranty) { this.warranty = warranty; }
    
    public boolean hasFreeDelivery() { return freeDelivery; }
    public void setFreeDelivery(boolean freeDelivery) { this.freeDelivery = freeDelivery; }
    
    public Map<String, Object> getExtras() { return extras; }
    public void addExtra(String key, Object value) { this.extras.put(key, value); }
    
    public Map<String, Integer> getItemAllocation() { return itemAllocation; }
    public void setItemAllocation(Map<String, Integer> allocation) { 
        this.itemAllocation = new HashMap<>(allocation); 
    }
    
    public long getTimestamp() { return timestamp; }
    
    /**
     * Create a copy of this offer
     */
    public Offer copy() {
        Offer copy = new Offer(this.price, this.quantity);
        copy.warranty = this.warranty;
        copy.freeDelivery = this.freeDelivery;
        copy.extras = new HashMap<>(this.extras);
        copy.itemAllocation = new HashMap<>(this.itemAllocation);
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Offer{price=%.2f, qty=%d", price, quantity));
        if (warranty) sb.append(", warranty");
        if (freeDelivery) sb.append(", free delivery");
        if (!itemAllocation.isEmpty()) {
            sb.append(", items=").append(itemAllocation);
        }
        sb.append("}");
        return sb.toString();
    }
}
