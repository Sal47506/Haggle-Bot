package dealdialect.strategies;

import dealdialect.engine.*;
import java.util.List;

/**
 * Models and tracks estimates about the opponent.
 * Updated as negotiation progresses.
 */
public class OpponentModel {
    private Role opponentRole;
    private double estimatedReservationPrice;
    private double estimatedTargetPrice;
    private double estimatedAggression;   // 0.0 = passive, 1.0 = aggressive
    private double estimatedRiskTolerance; // 0.0 = risk-averse, 1.0 = risk-seeking
    private double estimatedTruthfulness;  // 0.0 = frequent liar, 1.0 = always truthful
    private double observedTrust;
    
    // Tracking metrics
    private int totalMoves;
    private double totalConcessionAmount;
    private int bluffAttempts;
    private int detectedBluffs;
    
    public OpponentModel(Role opponentRole) {
        this.opponentRole = opponentRole;
        // Initialize with neutral estimates
        this.estimatedReservationPrice = 0.0;
        this.estimatedTargetPrice = 0.0;
        this.estimatedAggression = 0.5;
        this.estimatedRiskTolerance = 0.5;
        this.estimatedTruthfulness = 0.8;  // Assume mostly truthful initially
        this.observedTrust = 1.0;
        this.totalMoves = 0;
        this.totalConcessionAmount = 0.0;
        this.bluffAttempts = 0;
        this.detectedBluffs = 0;
    }
    
    /**
     * Update model based on observed offer
     */
    public void updateFromOffer(Offer offer, DealContext context, DialogueState state) {
        if (offer.getRole() != opponentRole) return;
        
        totalMoves++;
        
        // Update reservation price estimate
        updateReservationEstimate(offer, context);
        
        // Update aggression estimate
        updateAggressionEstimate(offer, state);
        
        // Track bluffs
        if (offer.isBluff()) {
            bluffAttempts++;
        }
        
        // Update trust observation
        observedTrust = state.getTrust(opponentRole.opposite());
    }
    
    /**
     * Update reservation price estimate based on offers
     */
    private void updateReservationEstimate(Offer offer, DealContext context) {
        double offeredPrice = offer.getPrice();
        
        // Use Bayesian updating
        if (totalMoves == 1) {
            // First offer gives initial estimate
            if (opponentRole == Role.BUYER) {
                estimatedReservationPrice = offeredPrice * 1.2;  // Assume 20% above first offer
            } else {
                estimatedReservationPrice = offeredPrice * 0.8;  // Assume 20% below first ask
            }
        } else {
            // Refine estimate with each offer
            double alpha = 0.3;  // Learning rate
            if (opponentRole == Role.BUYER) {
                estimatedReservationPrice = alpha * (offeredPrice * 1.1) + 
                                           (1 - alpha) * estimatedReservationPrice;
            } else {
                estimatedReservationPrice = alpha * (offeredPrice * 0.9) + 
                                           (1 - alpha) * estimatedReservationPrice;
            }
        }
    }
    
    /**
     * Update aggression estimate based on concession behavior
     */
    private void updateAggressionEstimate(Offer offer, DialogueState state) {
        List<Offer> opponentOffers = state.getOffersFrom(opponentRole);
        
        if (opponentOffers.size() < 2) return;
        
        Offer previous = opponentOffers.get(opponentOffers.size() - 2);
        double concession = offer.getConcessionAmount(previous);
        totalConcessionAmount += concession;
        
        double avgConcession = totalConcessionAmount / (totalMoves - 1);
        
        // Low concessions = high aggression
        if (avgConcession < 5.0) {
            estimatedAggression = 0.8;
        } else if (avgConcession > 20.0) {
            estimatedAggression = 0.3;
        } else {
            estimatedAggression = 0.5;
        }
    }
    
    /**
     * Update bluff detection
     */
    public void recordBluffDetection() {
        detectedBluffs++;
        // Lower truthfulness estimate
        estimatedTruthfulness = Math.max(0.1, estimatedTruthfulness - 0.1);
    }
    
    /**
     * Get estimated ZOPA overlap
     */
    public double getEstimatedZOPA(double myReservation) {
        if (opponentRole == Role.BUYER) {
            return Math.max(0, estimatedReservationPrice - myReservation);
        } else {
            return Math.max(0, myReservation - estimatedReservationPrice);
        }
    }
    
    /**
     * Predict opponent's next offer
     */
    public double predictNextOffer(DialogueState state) {
        List<Offer> offers = state.getOffersFrom(opponentRole);
        if (offers.isEmpty()) return estimatedTargetPrice;
        
        Offer last = offers.get(offers.size() - 1);
        double avgConcession = totalMoves > 1 ? totalConcessionAmount / (totalMoves - 1) : 0;
        
        if (opponentRole == Role.BUYER) {
            return last.getPrice() + avgConcession;
        } else {
            return last.getPrice() - avgConcession;
        }
    }
    
    // Getters
    public Role getOpponentRole() { return opponentRole; }
    public double getEstimatedReservationPrice() { return estimatedReservationPrice; }
    public double getEstimatedTargetPrice() { return estimatedTargetPrice; }
    public double getEstimatedAggression() { return estimatedAggression; }
    public double getEstimatedRiskTolerance() { return estimatedRiskTolerance; }
    public double getEstimatedTruthfulness() { return estimatedTruthfulness; }
    public double getObservedTrust() { return observedTrust; }
    public int getTotalMoves() { return totalMoves; }
    public double getAverageConcession() { 
        return totalMoves > 1 ? totalConcessionAmount / (totalMoves - 1) : 0; 
    }
    public double getBluffRate() {
        return bluffAttempts > 0 ? (double) detectedBluffs / bluffAttempts : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("OpponentModel{%s: reserv=$%.2f, aggr=%.2f, truth=%.2f}",
            opponentRole, estimatedReservationPrice, estimatedAggression, estimatedTruthfulness);
    }
}

