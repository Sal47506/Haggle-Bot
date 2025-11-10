package negotiation.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the outcome of a negotiation.
 * Contains final agreement details and utility scores for both parties.
 */
public class NegotiationResult {
    private boolean agreed;
    private Offer finalOffer;
    private double buyerUtility;
    private double sellerUtility;
    private int totalRounds;
    private List<Message> transcript;
    private String failureReason;
    
    public NegotiationResult() {
        this.agreed = false;
        this.finalOffer = null;
        this.buyerUtility = 0.0;
        this.sellerUtility = 0.0;
        this.totalRounds = 0;
        this.transcript = new ArrayList<>();
        this.failureReason = "";
    }
    
    // Getters and setters
    public boolean isAgreed() { return agreed; }
    public void setAgreed(boolean agreed) { this.agreed = agreed; }
    
    public Offer getFinalOffer() { return finalOffer; }
    public void setFinalOffer(Offer finalOffer) { this.finalOffer = finalOffer; }
    
    public double getBuyerUtility() { return buyerUtility; }
    public void setBuyerUtility(double buyerUtility) { this.buyerUtility = buyerUtility; }
    
    public double getSellerUtility() { return sellerUtility; }
    public void setSellerUtility(double sellerUtility) { this.sellerUtility = sellerUtility; }
    
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    
    public List<Message> getTranscript() { return transcript; }
    public void setTranscript(List<Message> transcript) { this.transcript = transcript; }
    public void addMessage(Message message) { this.transcript.add(message); }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    /**
     * Calculate the total social welfare (sum of utilities)
     */
    public double getSocialWelfare() {
        return buyerUtility + sellerUtility;
    }
    
    /**
     * Calculate the Nash product (product of utilities above disagreement point)
     */
    public double getNashProduct() {
        return Math.max(0, buyerUtility) * Math.max(0, sellerUtility);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== NEGOTIATION RESULT ===\n");
        sb.append(String.format("Status: %s\n", agreed ? "AGREEMENT REACHED" : "NO DEAL"));
        if (agreed) {
            sb.append(String.format("Final Offer: %s\n", finalOffer));
            sb.append(String.format("Buyer Utility: %.2f\n", buyerUtility));
            sb.append(String.format("Seller Utility: %.2f\n", sellerUtility));
            sb.append(String.format("Social Welfare: %.2f\n", getSocialWelfare()));
        } else {
            sb.append(String.format("Reason: %s\n", failureReason));
        }
        sb.append(String.format("Total Rounds: %d\n", totalRounds));
        return sb.toString();
    }
}
