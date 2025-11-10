package dealdialect.engine;

/**
 * Represents a single offer in the negotiation.
 * Includes price, text, bluff metadata, and trust impact.
 */
public class Offer {
    private Role role;
    private double price;
    private String utterance;
    private boolean isBluff;
    private double bluffStrength;  // 0.0 = truthful, 1.0 = complete lie
    private double trustAfter;  // Trust level after this offer
    private long timestamp;
    private int roundNumber;
    
    // Intent classification
    private Intent intent;
    
    public enum Intent {
        OFFER,           // Making an offer
        COUNTER,         // Counter-offering
        JUSTIFY,         // Justifying price
        THREATEN,        // Threatening to walk away
        BLUFF_PUFF,      // Puffing/exaggerating
        WALK_AWAY,       // Actually walking away
        ACCEPT,          // Accepting offer
        REJECT,          // Rejecting offer
        INQUIRE          // Asking questions
    }
    
    public Offer(Role role, double price, String utterance) {
        this.role = role;
        this.price = price;
        this.utterance = utterance;
        this.isBluff = false;
        this.bluffStrength = 0.0;
        this.trustAfter = 1.0;
        this.timestamp = System.currentTimeMillis();
        this.intent = Intent.OFFER;
    }
    
    public Offer(Role role, double price, String utterance, Intent intent) {
        this(role, price, utterance);
        this.intent = intent;
    }
    
    /**
     * Create a bluffing offer
     */
    public static Offer createBluff(Role role, double price, String utterance, double bluffStrength) {
        Offer offer = new Offer(role, price, utterance);
        offer.isBluff = true;
        offer.bluffStrength = bluffStrength;
        offer.intent = Intent.BLUFF_PUFF;
        return offer;
    }
    
    /**
     * Check if this is a concession from previous offer
     */
    public boolean isConcessionFrom(Offer previous) {
        if (previous == null || previous.role != this.role) return false;
        
        if (role == Role.BUYER) {
            return this.price > previous.price;  // Buyer increases offer
        } else {
            return this.price < previous.price;  // Seller decreases ask
        }
    }
    
    /**
     * Calculate concession amount from previous offer
     */
    public double getConcessionAmount(Offer previous) {
        if (!isConcessionFrom(previous)) return 0.0;
        return Math.abs(this.price - previous.price);
    }
    
    // Getters and setters
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getUtterance() { return utterance; }
    public void setUtterance(String utterance) { this.utterance = utterance; }
    
    public boolean isBluff() { return isBluff; }
    public void setBluff(boolean bluff) { isBluff = bluff; }
    
    public double getBluffStrength() { return bluffStrength; }
    public void setBluffStrength(double bluffStrength) { 
        this.bluffStrength = Math.max(0.0, Math.min(1.0, bluffStrength)); 
    }
    
    public double getTrustAfter() { return trustAfter; }
    public void setTrustAfter(double trustAfter) { this.trustAfter = trustAfter; }
    
    public long getTimestamp() { return timestamp; }
    
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    
    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }
    
    @Override
    public String toString() {
        return String.format("Offer{%s, $%.2f, intent=%s, bluff=%s}", 
            role, price, intent, isBluff);
    }
}

