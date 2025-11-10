package dealdialect.strategies;

import dealdialect.engine.*;

/**
 * Interface for bluffing strategy.
 * Decides when and how strongly to bluff.
 */
public interface BluffPolicy {
    
    /**
     * Decide whether to bluff on this move
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role considering bluffing
     * @return true to bluff, false to be truthful
     */
    boolean shouldBluff(DealContext context, DialogueState state, Role role);
    
    /**
     * Determine bluff strength (0.0 = minor puff, 1.0 = outright lie)
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role bluffing
     * @return Bluff strength in [0.0, 1.0]
     */
    double getBluffStrength(DealContext context, DialogueState state, Role role);
    
    /**
     * Generate a bluffed price based on true reservation price
     * @param truePrice The actual reservation price
     * @param bluffStrength How strongly to bluff
     * @param role The role bluffing
     * @return The bluffed price
     */
    double generateBluffedPrice(double truePrice, double bluffStrength, Role role);
    
    /**
     * Get policy name
     */
    String getPolicyName();
}

