package dealdialect.strategies;

import dealdialect.engine.*;

/**
 * Interface for concession strategy.
 * Handles how agents make concessions over time.
 */
public interface ConcessionPolicy {
    
    /**
     * Calculate the next concession amount
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role making concession
     * @param currentPrice Current offered price
     * @param targetPrice Ideal price for this role
     * @param reservationPrice Walk-away price for this role
     * @return Concession amount
     */
    double calculateConcession(DealContext context, DialogueState state, Role role,
                              double currentPrice, double targetPrice, double reservationPrice);
    
    /**
     * Determine concession curve type
     */
    enum ConcessionCurve {
        LINEAR,          // Constant concession per round
        BOULWARE,        // Concede slowly early, quickly late
        CONCEDER,        // Concede quickly early, slowly late
        TIT_FOR_TAT,     // Match opponent's concessions
        TIME_DEPENDENT   // Based on time pressure
    }
    
    /**
     * Get the concession curve being used
     */
    ConcessionCurve getConcessionCurve();
    
    /**
     * Set the concession curve
     */
    void setConcessionCurve(ConcessionCurve curve);
    
    /**
     * Calculate reactive concession based on opponent's last move
     * @param opponentConcession Amount opponent conceded
     * @param timePressure Time pressure factor [0.0, 1.0]
     * @return Reactive concession amount
     */
    double calculateReactiveConcession(double opponentConcession, double timePressure);
    
    /**
     * Get policy name
     */
    String getPolicyName();
}

