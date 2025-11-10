package dealdialect.strategies;

import dealdialect.engine.*;

/**
 * Interface for modeling truthfulness, trust decay, and bluff detection.
 */
public interface TruthfulnessPolicy {
    
    /**
     * Detect if an offer is a bluff
     * @param offer The offer to analyze
     * @param context The deal context
     * @param state Current dialogue state
     * @param detector The role trying to detect
     * @return Probability that offer is a bluff [0.0, 1.0]
     */
    double detectBluffProbability(Offer offer, DealContext context, DialogueState state, Role detector);
    
    /**
     * Update trust after detecting a bluff
     * @param state Current dialogue state
     * @param bluffer The role that bluffed
     * @param detectionConfidence Confidence in detection [0.0, 1.0]
     */
    void updateTrustOnBluff(DialogueState state, Role bluffer, double detectionConfidence);
    
    /**
     * Calculate trust decay over time
     * @param currentTrust Current trust level
     * @param roundNumber Current round
     * @return Decayed trust level
     */
    double calculateTrustDecay(double currentTrust, int roundNumber);
    
    /**
     * Determine if a lie/puff is allowed given context
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role considering lying
     * @param lieStrength How strong the lie would be
     * @return true if allowed, false otherwise
     */
    boolean isLieAllowed(DealContext context, DialogueState state, Role role, double lieStrength);
    
    /**
     * Get policy name
     */
    String getPolicyName();
}

