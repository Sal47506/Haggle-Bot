package dealdialect.engine;

import negotiation.models.Item;

/**
 * Complete context for a negotiation deal.
 * Contains item info, valuation bounds, and time constraints.
 */
public class DealContext {
    private Item item;
    private double msrp;  // Manufacturer's suggested retail price
    private double buyerValue;  // Buyer's maximum willingness to pay
    private double sellerCost;  // Seller's minimum acceptable price
    private int timeLimit;  // Maximum rounds
    private long startTime;
    
    // Optional metadata
    private String category;
    private boolean allowBluffing;
    private boolean allowPuffing;
    
    public DealContext(Item item, double msrp, double buyerValue, double sellerCost, int timeLimit) {
        this.item = item;
        this.msrp = msrp;
        this.buyerValue = buyerValue;
        this.sellerCost = sellerCost;
        this.timeLimit = timeLimit;
        this.startTime = System.currentTimeMillis();
        this.allowBluffing = true;
        this.allowPuffing = true;
    }
    
    /**
     * Check if deal is possible (ZOPA exists)
     */
    public boolean hasBATNA() {
        return buyerValue >= sellerCost;
    }
    
    /**
     * Get Zone of Possible Agreement size
     */
    public double getZOPASize() {
        return Math.max(0, buyerValue - sellerCost);
    }
    
    /**
     * Get elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    // Getters and setters
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    
    public double getMsrp() { return msrp; }
    public void setMsrp(double msrp) { this.msrp = msrp; }
    
    public double getBuyerValue() { return buyerValue; }
    public void setBuyerValue(double buyerValue) { this.buyerValue = buyerValue; }
    
    public double getSellerCost() { return sellerCost; }
    public void setSellerCost(double sellerCost) { this.sellerCost = sellerCost; }
    
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isAllowBluffing() { return allowBluffing; }
    public void setAllowBluffing(boolean allowBluffing) { this.allowBluffing = allowBluffing; }
    
    public boolean isAllowPuffing() { return allowPuffing; }
    public void setAllowPuffing(boolean allowPuffing) { this.allowPuffing = allowPuffing; }
    
    @Override
    public String toString() {
        return String.format("DealContext{item=%s, MSRP=%.2f, buyerValue=%.2f, sellerCost=%.2f, ZOPA=%.2f}",
            item != null ? item.getTitle() : "null", msrp, buyerValue, sellerCost, getZOPASize());
    }
}

