package dealdialect.strategies;

import dealdialect.engine.*;
import java.util.Random;

/**
 * Default implementation of BluffPolicy.
 * Bluffs probabilistically based on trust and time pressure.
 */
public class DefaultBluffPolicy implements BluffPolicy {
    private double bluffProbability;  // Base probability of bluffing
    private double maxBluffStrength;  // Maximum bluff strength allowed
    private Random random;
    
    public DefaultBluffPolicy(double bluffProbability, double maxBluffStrength) {
        this.bluffProbability = Math.max(0.0, Math.min(1.0, bluffProbability));
        this.maxBluffStrength = Math.max(0.0, Math.min(1.0, maxBluffStrength));
        this.random = new Random();
    }
    
    @Override
    public boolean shouldBluff(DealContext context, DialogueState state, Role role) {
        // Don't bluff if not allowed
        if (!context.isAllowBluffing()) return false;
        
        // Don't bluff too early
        if (state.getCurrentRound() < 2) return false;
        
        // Adjust bluff probability based on trust
        double trust = state.getTrust(role.opposite());
        double adjustedProbability = bluffProbability * trust;  // Less likely to bluff if opponent doesn't trust us
        
        // Adjust based on previous bluff success
        double bluffSuccessRate = state.getBluffSuccessRate(role);
        if (bluffSuccessRate < 0.5) {
            adjustedProbability *= 0.5;  // Reduce if we've been caught
        }
        
        // Increase probability with time pressure
        double timePressure = (double) state.getCurrentRound() / context.getTimeLimit();
        if (timePressure > 0.6) {
            adjustedProbability *= 1.5;
        }
        
        return random.nextDouble() < adjustedProbability;
    }
    
    @Override
    public double getBluffStrength(DealContext context, DialogueState state, Role role) {
        // Start with max allowed strength
        double strength = maxBluffStrength;
        
        // Reduce based on trust - higher trust allows stronger bluffs
        double trust = state.getTrust(role.opposite());
        strength *= trust;
        
        // Reduce if we've been caught bluffing before
        double bluffSuccessRate = state.getBluffSuccessRate(role);
        strength *= bluffSuccessRate;
        
        // Random variation
        strength *= (0.7 + random.nextDouble() * 0.3);  // 70-100% of calculated strength
        
        return Math.max(0.1, Math.min(maxBluffStrength, strength));
    }
    
    @Override
    public double generateBluffedPrice(double truePrice, double bluffStrength, Role role) {
        // Calculate price range for bluffing
        double bluffAmount = truePrice * bluffStrength * 0.3;  // Up to 30% of true price
        
        if (role == Role.BUYER) {
            // Buyer bluffs lower (pretends can't afford more)
            return truePrice - bluffAmount;
        } else {
            // Seller bluffs higher (pretends won't go lower)
            return truePrice + bluffAmount;
        }
    }
    
    @Override
    public String getPolicyName() {
        return "DefaultBluffPolicy";
    }
    
    // Getters and setters
    public double getBluffProbability() { return bluffProbability; }
    public void setBluffProbability(double bluffProbability) { 
        this.bluffProbability = Math.max(0.0, Math.min(1.0, bluffProbability)); 
    }
    
    public double getMaxBluffStrength() { return maxBluffStrength; }
    public void setMaxBluffStrength(double maxBluffStrength) { 
        this.maxBluffStrength = Math.max(0.0, Math.min(1.0, maxBluffStrength)); 
    }
}

