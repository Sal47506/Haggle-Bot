package negotiation.strategy;

import negotiation.models.Offer;
import java.util.List;

/**
 * Interface for negotiation strategies.
 * Different strategies implement different approaches to making concessions,
 * bluffing, and selecting negotiation moves.
 */
public interface INegotiationStrategy {
    
    /**
     * Calculate the concession amount for the current round
     * @param round the current negotiation round
     * @param currentOffer the current offer on the table
     * @param targetPrice the agent's target price
     * @param reservationPrice the agent's reservation (walk-away) price
     * @return the concession amount
     */
    double calculateConcession(int round, double currentOffer, double targetPrice, double reservationPrice);
    
    /**
     * Determine if the agent should bluff in this round
     * @param round the current negotiation round
     * @return true if should bluff, false otherwise
     */
    boolean shouldBluff(int round);
    
    /**
     * Choose the next move type based on strategy and current state
     * @param round the current negotiation round
     * @param incomingOffer the offer received (may be null)
     * @param history the history of previous offers
     * @return the move type to make
     */
    MoveType chooseMoveType(int round, Offer incomingOffer, List<Offer> history);
    
    /**
     * Get the strategy's name
     * @return strategy name
     */
    String getStrategyName();
    
    /**
     * Update strategy parameters based on feedback
     * @param success whether the previous negotiation was successful
     * @param utility the utility gained from the negotiation
     */
    void updateParameters(boolean success, double utility);
}

