package negotiation.strategy;

import negotiation.models.Offer;
import java.util.List;
import java.util.Random;

/**
 * Concrete implementation of negotiation strategy.
 * Uses concession curves, bluffing, and adaptive behavior.
 */
public class StrategyProfile implements INegotiationStrategy {
    private String strategyName;
    private double concessionRate; // How fast to make concessions
    private double bluffProbability; // Probability of bluffing
    private double aggressionLevel; // How aggressive (0.0 = passive, 1.0 = aggressive)
    private double timeDecayFactor; // How quickly concessions increase over time
    private Random random;
    
    public StrategyProfile(String name, double concessionRate, double bluffProbability, 
                          double aggressionLevel, double timeDecayFactor) {
        this.strategyName = name;
        this.concessionRate = concessionRate;
        this.bluffProbability = bluffProbability;
        this.aggressionLevel = aggressionLevel;
        this.timeDecayFactor = timeDecayFactor;
        this.random = new Random();
    }
    
    /**
     * Predefined strategy: Aggressive
     */
    public static StrategyProfile aggressive() {
        return new StrategyProfile("Aggressive", 0.02, 0.3, 0.9, 0.05);
    }
    
    /**
     * Predefined strategy: Cooperative
     */
    public static StrategyProfile cooperative() {
        return new StrategyProfile("Cooperative", 0.05, 0.1, 0.3, 0.08);
    }
    
    /**
     * Predefined strategy: Balanced
     */
    public static StrategyProfile balanced() {
        return new StrategyProfile("Balanced", 0.03, 0.2, 0.5, 0.06);
    }
    
    @Override
    public double calculateConcession(int round, double currentOffer, 
                                     double targetPrice, double reservationPrice) {
        // Time-dependent concession: increases over rounds
        double timeFactor = 1.0 + (timeDecayFactor * round);
        
        // Calculate gap to reservation price
        double gap = Math.abs(reservationPrice - currentOffer);
        
        // Base concession
        double concession = gap * concessionRate * timeFactor;
        
        // Adjust by aggression (less aggressive = larger concessions)
        concession *= (1.0 - (aggressionLevel * 0.5));
        
        return concession;
    }
    
    @Override
    public boolean shouldBluff(int round) {
        // Bluffing decreases as rounds progress
        double adjustedProbability = bluffProbability * (1.0 - (round * 0.1));
        return random.nextDouble() < Math.max(0, adjustedProbability);
    }
    
    @Override
    public MoveType chooseMoveType(int round, Offer incomingOffer, List<Offer> history) {
        // Early rounds: more likely to counter
        if (round < 3) {
            return random.nextDouble() < 0.8 ? MoveType.COUNTER_OFFER : MoveType.INQUIRE;
        }
        
        // Middle rounds: mix of strategies
        if (round < 7) {
            double r = random.nextDouble();
            if (r < 0.4) return MoveType.COUNTER_OFFER;
            if (r < 0.6) return MoveType.INSIST;
            if (r < 0.8) return MoveType.INQUIRE;
            return MoveType.AGREE;
        }
        
        // Late rounds: more willing to accept
        if (round >= 10) {
            return random.nextDouble() < 0.6 ? MoveType.ACCEPT : MoveType.COUNTER_OFFER;
        }
        
        return MoveType.COUNTER_OFFER;
    }
    
    @Override
    public String getStrategyName() {
        return strategyName;
    }
    
    @Override
    public void updateParameters(boolean success, double utility) {
        // Adaptive learning: adjust parameters based on outcome
        if (success) {
            // Successful negotiation - reinforce current strategy slightly
            if (utility > 0.7) {
                // Very good outcome - no change needed
                return;
            } else {
                // Moderate outcome - slightly increase concession rate
                concessionRate *= 1.05;
            }
        } else {
            // Failed negotiation - adjust strategy
            concessionRate *= 1.1; // More willing to concede
            aggressionLevel *= 0.9; // Less aggressive
        }
        
        // Keep parameters in reasonable bounds
        concessionRate = Math.min(0.1, concessionRate);
        aggressionLevel = Math.max(0.1, Math.min(1.0, aggressionLevel));
    }
    
    // Getters
    public double getConcessionRate() { return concessionRate; }
    public double getBluffProbability() { return bluffProbability; }
    public double getAggressionLevel() { return aggressionLevel; }
    public double getTimeDecayFactor() { return timeDecayFactor; }
}
