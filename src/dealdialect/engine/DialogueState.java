package dealdialect.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete state of the negotiation dialogue.
 * Tracks history, trust levels, and outcome.
 */
public class DialogueState {
    private List<Offer> history;
    private double buyerTrust;    // Buyer's trust in seller (0.0 to 1.0)
    private double sellerTrust;   // Seller's trust in buyer (0.0 to 1.0)
    private boolean terminal;     // Is negotiation finished?
    private Double dealPrice;     // Final agreed price (null if no deal)
    private int currentRound;
    
    // Additional state tracking
    private int buyerBluffCount;
    private int sellerBluffCount;
    private int buyerBluffDetected;
    private int sellerBluffDetected;
    
    public DialogueState() {
        this.history = new ArrayList<>();
        this.buyerTrust = 1.0;  // Start with full trust
        this.sellerTrust = 1.0;
        this.terminal = false;
        this.dealPrice = null;
        this.currentRound = 0;
        this.buyerBluffCount = 0;
        this.sellerBluffCount = 0;
        this.buyerBluffDetected = 0;
        this.sellerBluffDetected = 0;
    }
    
    /**
     * Add an offer to history
     */
    public void addOffer(Offer offer) {
        offer.setRoundNumber(currentRound);
        history.add(offer);
        
        // Track bluff counts
        if (offer.isBluff()) {
            if (offer.getRole() == Role.BUYER) {
                buyerBluffCount++;
            } else {
                sellerBluffCount++;
            }
        }
    }
    
    /**
     * Get last offer from a specific role
     */
    public Offer getLastOfferFrom(Role role) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Offer offer = history.get(i);
            if (offer.getRole() == role) {
                return offer;
            }
        }
        return null;
    }
    
    /**
     * Get all offers from a specific role
     */
    public List<Offer> getOffersFrom(Role role) {
        List<Offer> offers = new ArrayList<>();
        for (Offer offer : history) {
            if (offer.getRole() == role) {
                offers.add(offer);
            }
        }
        return offers;
    }
    
    /**
     * Check if agreement has been reached
     */
    public boolean hasAgreement() {
        return terminal && dealPrice != null;
    }
    
    /**
     * Get trust level for a specific role
     */
    public double getTrust(Role role) {
        return role == Role.BUYER ? buyerTrust : sellerTrust;
    }
    
    /**
     * Set trust level for a specific role
     */
    public void setTrust(Role role, double trust) {
        trust = Math.max(0.0, Math.min(1.0, trust));  // Clamp to [0,1]
        if (role == Role.BUYER) {
            buyerTrust = trust;
        } else {
            sellerTrust = trust;
        }
    }
    
    /**
     * Update trust based on bluff detection
     */
    public void updateTrustOnBluffDetection(Role bluffer, double penalty) {
        Role victim = bluffer.opposite();
        double currentTrust = getTrust(victim);
        setTrust(victim, currentTrust - penalty);
        
        if (bluffer == Role.BUYER) {
            buyerBluffDetected++;
        } else {
            sellerBluffDetected++;
        }
    }
    
    /**
     * Advance to next round
     */
    public void nextRound() {
        currentRound++;
    }
    
    /**
     * Get bluff success rate for a role
     */
    public double getBluffSuccessRate(Role role) {
        int totalBluffs = role == Role.BUYER ? buyerBluffCount : sellerBluffCount;
        int detected = role == Role.BUYER ? buyerBluffDetected : sellerBluffDetected;
        
        if (totalBluffs == 0) return 1.0;
        return 1.0 - ((double) detected / totalBluffs);
    }
    
    // Getters and setters
    public List<Offer> getHistory() { return new ArrayList<>(history); }
    
    public double getBuyerTrust() { return buyerTrust; }
    public void setBuyerTrust(double buyerTrust) { 
        this.buyerTrust = Math.max(0.0, Math.min(1.0, buyerTrust)); 
    }
    
    public double getSellerTrust() { return sellerTrust; }
    public void setSellerTrust(double sellerTrust) { 
        this.sellerTrust = Math.max(0.0, Math.min(1.0, sellerTrust)); 
    }
    
    public boolean isTerminal() { return terminal; }
    public void setTerminal(boolean terminal) { this.terminal = terminal; }
    
    public Double getDealPrice() { return dealPrice; }
    public void setDealPrice(Double dealPrice) { 
        this.dealPrice = dealPrice;
        this.terminal = true;
    }
    
    public int getCurrentRound() { return currentRound; }
    
    public int getBuyerBluffCount() { return buyerBluffCount; }
    public int getSellerBluffCount() { return sellerBluffCount; }
    public int getBuyerBluffDetected() { return buyerBluffDetected; }
    public int getSellerBluffDetected() { return sellerBluffDetected; }
    
    @Override
    public String toString() {
        return String.format("DialogueState{round=%d, history=%d offers, terminal=%s, deal=$%.2f, trust=B:%.2f/S:%.2f}",
            currentRound, history.size(), terminal, dealPrice != null ? dealPrice : 0.0, buyerTrust, sellerTrust);
    }
}

