package negotiation.agents;

import negotiation.models.*;
import negotiation.strategy.INegotiationStrategy;
import negotiation.strategy.MoveType;
import negotiation.grammar.GrammarEngine;

/**
 * Buyer agent implementation.
 * Tries to minimize price while maximizing utility.
 */
public class BuyerAgent extends Agent {
    private double maxBudget;
    private double targetPrice;
    
    public BuyerAgent(String name, IUtilityFunction utilityModel, 
                     INegotiationStrategy strategy, GrammarEngine grammar) {
        super(name, "buyer", utilityModel, strategy, grammar);
        this.targetPrice = utilityModel.getTargetPrice();
        this.maxBudget = utilityModel.getReservationPrice();
    }
    
    @Override
    public Offer proposeOffer() {
        int round = getCurrentRound();
        Offer lastOffer = getLastOffer();
        
        // First offer - start below target
        if (lastOffer == null) {
            double openingPrice = targetPrice * 0.8; // Start at 80% of target
            return new Offer(openingPrice);
        }
        
        // Calculate concession based on strategy
        double currentPrice = lastOffer.getPrice();
        double concession = strategy.calculateConcession(
            round, currentPrice, targetPrice, maxBudget
        );
        
        double newPrice = Math.min(currentPrice + concession, maxBudget);
        return new Offer(newPrice);
    }
    
    @Override
    public Offer respondToOffer(Offer incoming) {
        int round = getCurrentRound();
        
        // Check if offer is acceptable
        if (utilityModel.isAcceptable(incoming, round)) {
            MoveType move = strategy.chooseMoveType(round, incoming, offerHistory);
            if (move == MoveType.ACCEPT) {
                return incoming; // Accept the offer
            }
        }
        
        // Check if we should walk away
        if (incoming.getPrice() > maxBudget) {
            if (round > 5 || !strategy.shouldBluff(round)) {
                return null; // Walk away
            }
        }
        
        // Make counter offer
        return proposeOffer();
    }
    
    @Override
    public boolean shouldWalkAway(int currentRound) {
        Offer lastOffer = getLastOffer();
        if (lastOffer == null) return false;
        
        // Walk away if price exceeds budget and we've tried enough rounds
        return lastOffer.getPrice() > maxBudget && currentRound > 5;
    }
    
    public double getMaxBudget() {
        return maxBudget;
    }
    
    public double getTargetPrice() {
        return targetPrice;
    }
}
