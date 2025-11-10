package dealdialect.strategies;

import dealdialect.engine.*;
import java.util.List;

/**
 * Default implementation of TruthfulnessPolicy.
 * Uses heuristics for bluff detection and trust updates.
 */
public class DefaultTruthfulnessPolicy implements TruthfulnessPolicy {
    private double detectionSensitivity;  // 0.0 = oblivious, 1.0 = very perceptive
    private double trustDecayRate;  // How fast trust decays naturally
    
    public DefaultTruthfulnessPolicy(double detectionSensitivity, double trustDecayRate) {
        this.detectionSensitivity = Math.max(0.0, Math.min(1.0, detectionSensitivity));
        this.trustDecayRate = Math.max(0.0, Math.min(0.1, trustDecayRate));
    }
    
    @Override
    public double detectBluffProbability(Offer offer, DealContext context, DialogueState state, Role detector) {
        if (offer.getRole() == detector) return 0.0;  // Can't detect own bluff
        
        double detectionProb = 0.0;
        
        // Check for inconsistencies in offers
        List<Offer> opponentOffers = state.getOffersFrom(offer.getRole());
        if (opponentOffers.size() > 2) {
            // Check if concession pattern is suspicious
            double avgConcession = calculateAverageConcession(opponentOffers);
            Offer previous = opponentOffers.get(opponentOffers.size() - 2);
            double currentConcession = offer.getConcessionAmount(previous);
            
            // Sudden large concessions might indicate previous bluff
            if (currentConcession > avgConcession * 2.0) {
                detectionProb += 0.3;
            }
            
            // Very small concessions might indicate bluffing about reservation
            if (currentConcession < avgConcession * 0.3 && state.getCurrentRound() > 5) {
                detectionProb += 0.2;
            }
        }
        
        // Check if price is far from expected range
        double expectedReservation = estimateReservation(offer.getRole(), context);
        double priceDiff = Math.abs(offer.getPrice() - expectedReservation);
        double maxDiff = context.getZOPASize();
        
        if (maxDiff > 0 && priceDiff > maxDiff * 0.5) {
            detectionProb += 0.25;
        }
        
        // Trust level affects detection
        double trust = state.getTrust(detector);
        if (trust < 0.5) {
            detectionProb += 0.2;  // Low trust increases suspicion
        }
        
        // Apply detection sensitivity
        detectionProb *= detectionSensitivity;
        
        return Math.min(1.0, detectionProb);
    }
    
    @Override
    public void updateTrustOnBluff(DialogueState state, Role bluffer, double detectionConfidence) {
        Role victim = bluffer.opposite();
        double currentTrust = state.getTrust(victim);
        
        // Trust penalty based on detection confidence
        double penalty = 0.15 * detectionConfidence;  // Up to 15% trust loss
        
        state.setTrust(victim, currentTrust - penalty);
        state.updateTrustOnBluffDetection(bluffer, penalty);
    }
    
    @Override
    public double calculateTrustDecay(double currentTrust, int roundNumber) {
        // Natural trust decay over time (negotiation fatigue)
        double decay = trustDecayRate * roundNumber;
        return Math.max(0.5, currentTrust - decay);  // Don't go below 0.5
    }
    
    @Override
    public boolean isLieAllowed(DealContext context, DialogueState state, Role role, double lieStrength) {
        // Check context permissions
        if (!context.isAllowBluffing()) return false;
        if (!context.isAllowPuffing() && lieStrength > 0.3) return false;
        
        // Don't allow lies that would be obviously detected
        double detectionProb = lieStrength * 0.8;  // Stronger lies more detectable
        if (detectionProb > 0.7) return false;
        
        // Don't allow if trust is already very low
        double trust = state.getTrust(role.opposite());
        if (trust < 0.3 && lieStrength > 0.5) return false;
        
        return true;
    }
    
    @Override
    public String getPolicyName() {
        return "DefaultTruthfulnessPolicy";
    }
    
    /**
     * Helper: Calculate average concession from offer history
     */
    private double calculateAverageConcession(List<Offer> offers) {
        if (offers.size() < 2) return 0.0;
        
        double totalConcession = 0.0;
        int count = 0;
        
        for (int i = 1; i < offers.size(); i++) {
            double concession = offers.get(i).getConcessionAmount(offers.get(i-1));
            if (concession > 0) {
                totalConcession += concession;
                count++;
            }
        }
        
        return count > 0 ? totalConcession / count : 0.0;
    }
    
    /**
     * Helper: Estimate opponent's reservation price
     */
    private double estimateReservation(Role role, DealContext context) {
        if (role == Role.BUYER) {
            return context.getBuyerValue();
        } else {
            return context.getSellerCost();
        }
    }
    
    // Getters and setters
    public double getDetectionSensitivity() { return detectionSensitivity; }
    public void setDetectionSensitivity(double detectionSensitivity) { 
        this.detectionSensitivity = Math.max(0.0, Math.min(1.0, detectionSensitivity)); 
    }
    
    public double getTrustDecayRate() { return trustDecayRate; }
    public void setTrustDecayRate(double trustDecayRate) { 
        this.trustDecayRate = Math.max(0.0, Math.min(0.1, trustDecayRate)); 
    }
}

