package negotiation.models;

/**
 * Interface for utility functions that evaluate offers.
 * Different agents may have different preferences and risk tolerances.
 */
public interface IUtilityFunction {
    
    /**
     * Evaluate the utility of an offer for this agent
     * @param offer the offer to evaluate
     * @param round the current negotiation round
     * @return utility value (higher is better)
     */
    double evaluate(Offer offer, int round);
    
    /**
     * Determine if an offer is acceptable (above reservation utility)
     * @param offer the offer to check
     * @param round the current round
     * @return true if acceptable, false otherwise
     */
    boolean isAcceptable(Offer offer, int round);
    
    /**
     * Get the reservation (minimum acceptable) price
     * @return reservation price
     */
    double getReservationPrice();
    
    /**
     * Get the target (ideal) price
     * @return target price
     */
    double getTargetPrice();
    
    /**
     * Get the time pressure factor (how urgency affects utility)
     * @return time pressure factor
     */
    double getTimePressure();
}

