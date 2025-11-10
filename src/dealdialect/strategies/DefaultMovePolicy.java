package dealdialect.strategies;

import dealdialect.engine.*;
import java.util.Random;

/**
 * Default implementation of MovePolicy.
 * Uses time-dependent concessions with anchoring.
 */
public class DefaultMovePolicy implements MovePolicy {
    private double aggression;  // 0.0 = passive, 1.0 = aggressive
    private double riskAversion; // 0.0 = risk-seeking, 1.0 = risk-averse
    private ConcessionPolicy concessionPolicy;
    private Random random;
    
    public DefaultMovePolicy(double aggression, double riskAversion, ConcessionPolicy concessionPolicy) {
        this.aggression = Math.max(0.0, Math.min(1.0, aggression));
        this.riskAversion = Math.max(0.0, Math.min(1.0, riskAversion));
        this.concessionPolicy = concessionPolicy;
        this.random = new Random();
    }
    
    @Override
    public Offer decideNextOffer(DealContext context, DialogueState state, Role role) {
        // Get reservation and target prices
        double reservation = role == Role.BUYER ? context.getBuyerValue() : context.getSeller

Cost();
        double target = role == Role.BUYER ? context.getSellerCost() : context.getBuyerValue();
        
        // Get last offer from this role
        Offer lastOwn = state.getLastOfferFrom(role);
        Offer lastOpponent = state.getLastOfferFrom(role.opposite());
        
        double nextPrice;
        
        if (lastOwn == null) {
            // First offer - anchor based on aggression
            nextPrice = calculateAnchor(target, reservation, aggression, role);
        } else {
            // Calculate concession
            double currentPrice = lastOwn.getPrice();
            double concession = concessionPolicy.calculateConcession(
                context, state, role, currentPrice, target, reservation
            );
            
            // Apply reactive concession if opponent made a move
            if (lastOpponent != null && lastOpponent.getRoundNumber() > lastOwn.getRoundNumber()) {
                Offer oppPrevious = getPreviousOffer(state, role.opposite(), lastOpponent);
                if (oppPrevious != null) {
                    double oppConcession = lastOpponent.getConcessionAmount(oppPrevious);
                    double timePressure = (double) state.getCurrentRound() / context.getTimeLimit();
                    double reactiveConcession = concessionPolicy.calculateReactiveConcession(
                        oppConcession, timePressure
                    );
                    concession = Math.max(concession, reactiveConcession);
                }
            }
            
            // Apply concession
            if (role == Role.BUYER) {
                nextPrice = Math.min(currentPrice + concession, reservation);
            } else {
                nextPrice = Math.max(currentPrice - concession, reservation);
            }
        }
        
        // Generate utterance (placeholder)
        String utterance = generateOfferText(nextPrice, role, state);
        
        return new Offer(role, nextPrice, utterance, Offer.Intent.COUNTER);
    }
    
    @Override
    public boolean shouldAccept(DealContext context, DialogueState state, Role role) {
        Offer opponentOffer = state.getLastOfferFrom(role.opposite());
        if (opponentOffer == null) return false;
        
        double reservation = role == Role.BUYER ? context.getBuyerValue() : context.getSellerCost();
        double offeredPrice = opponentOffer.getPrice();
        
        // Accept if within reservation price
        if (role == Role.BUYER && offeredPrice <= reservation) {
            return true;
        } else if (role == Role.SELLER && offeredPrice >= reservation) {
            return true;
        }
        
        // Accept if close to reservation and time is running out
        double timePressure = (double) state.getCurrentRound() / context.getTimeLimit();
        if (timePressure > 0.8) {
            double gap = Math.abs(offeredPrice - reservation);
            double threshold = Math.abs(context.getMsrp() * 0.05);  // 5% of MSRP
            return gap < threshold;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldWalkAway(DealContext context, DialogueState state, Role role) {
        // Check time limit
        if (state.getCurrentRound() >= context.getTimeLimit()) {
            return true;
        }
        
        // Check if opponent's offers are moving away
        Offer last = state.getLastOfferFrom(role.opposite());
        if (last != null) {
            double reservation = role == Role.BUYER ? context.getBuyerValue() : context.getSellerCost();
            double gap = Math.abs(last.getPrice() - reservation);
            
            // If gap is large and aggression is high, walk away
            if (aggression > 0.7 && gap > context.getMsrp() * 0.3) {
                return random.nextDouble() < 0.2;  // 20% chance
            }
        }
        
        // Check trust levels
        double trust = state.getTrust(role);
        if (trust < 0.3 && state.getCurrentRound() > 5) {
            return random.nextDouble() < 0.15;  // 15% chance with low trust
        }
        
        return false;
    }
    
    @Override
    public String getPolicyName() {
        return "DefaultMovePolicy";
    }
    
    /**
     * Calculate anchoring price for first offer
     */
    private double calculateAnchor(double target, double reservation, double aggression, Role role) {
        double gap = Math.abs(reservation - target);
        double anchorShift = gap * aggression;
        
        if (role == Role.BUYER) {
            return target + anchorShift;  // Start higher than target
        } else {
            return target - anchorShift;  // Start lower than target
        }
    }
    
    /**
     * Get previous offer from a role before the given offer
     */
    private Offer getPreviousOffer(DialogueState state, Role role, Offer current) {
        java.util.List<Offer> offers = state.getOffersFrom(role);
        for (int i = offers.size() - 1; i >= 0; i--) {
            if (offers.get(i) != current && offers.get(i).getRoundNumber() < current.getRoundNumber()) {
                return offers.get(i);
            }
        }
        return null;
    }
    
    /**
     * Generate offer text (placeholder - will be replaced by LanguageModel)
     */
    private String generateOfferText(double price, Role role, DialogueState state) {
        if (state.getHistory().isEmpty()) {
            return String.format("I can %s for $%.2f", 
                role == Role.BUYER ? "pay" : "sell", price);
        } else {
            return String.format("How about $%.2f?", price);
        }
    }
    
    // Getters and setters
    public double getAggression() { return aggression; }
    public void setAggression(double aggression) { 
        this.aggression = Math.max(0.0, Math.min(1.0, aggression)); 
    }
    
    public double getRiskAversion() { return riskAversion; }
    public void setRiskAversion(double riskAversion) { 
        this.riskAversion = Math.max(0.0, Math.min(1.0, riskAversion)); 
    }
}

