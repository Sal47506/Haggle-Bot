package negotiation.agents;

import negotiation.models.*;
import negotiation.strategy.INegotiationStrategy;
import negotiation.strategy.MoveType;
import negotiation.grammar.GrammarEngine;

/**
 * Seller agent implementation.
 * Tries to maximize price while ensuring sale above reservation price.
 */
public class SellerAgent extends Agent {
    private double minAcceptable;
    private double targetPrice;
    
    public SellerAgent(String name, IUtilityFunction utilityModel, 
                      INegotiationStrategy strategy, GrammarEngine grammar) {
        super(name, "seller", utilityModel, strategy, grammar);
        this.minAcceptable = utilityModel.getReservationPrice();
        this.targetPrice = utilityModel.getTargetPrice();
    }
    
    @Override
    public Offer proposeOffer() {
        int round = getCurrentRound();
        Offer lastOffer = getLastOffer();
        
        // First offer - start above target
        if (lastOffer == null) {
            double openingPrice = targetPrice * 1.2; // Start at 120% of target
            if (negotiatingItem != null) {
                openingPrice = Math.max(openingPrice, negotiatingItem.getListPrice());
            }
            return new Offer(openingPrice);
        }
        
        // Calculate concession based on strategy (negative for seller - price goes down)
        double currentPrice = lastOffer.getPrice();
        double concession = strategy.calculateConcession(
            round, currentPrice, targetPrice, minAcceptable
        );
        
        double newPrice = Math.max(currentPrice - Math.abs(concession), minAcceptable);
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
        if (incoming.getPrice() < minAcceptable) {
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
        
        // Walk away if price below minimum and we've tried enough rounds
        return lastOffer.getPrice() < minAcceptable && currentRound > 5;
    }
    
    public double getMinAcceptable() {
        return minAcceptable;
    }
    
    public double getTargetPrice() {
        return targetPrice;
    }
}
