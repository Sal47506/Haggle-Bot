package dealdialect.strategies;

import dealdialect.engine.*;

/**
 * Interface for deciding the next negotiation move.
 * Strategies implement how to select offers based on state.
 */
public interface MovePolicy {
    
    /**
     * Decide the next offer based on current state
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role making the move
     * @return The next offer to make, or null to walk away
     */
    Offer decideNextOffer(DealContext context, DialogueState state, Role role);
    
    /**
     * Decide whether to accept the opponent's last offer
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role deciding
     * @return true to accept, false to continue negotiating
     */
    boolean shouldAccept(DealContext context, DialogueState state, Role role);
    
    /**
     * Decide whether to walk away from negotiation
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role deciding
     * @return true to walk away, false to continue
     */
    boolean shouldWalkAway(DealContext context, DialogueState state, Role role);
    
    /**
     * Get policy name
     */
    String getPolicyName();
}

