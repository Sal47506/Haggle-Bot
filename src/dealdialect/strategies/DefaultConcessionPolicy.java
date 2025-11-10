package dealdialect.strategies;

import dealdialect.engine.*;

/**
 * Default implementation of ConcessionPolicy.
 * Supports multiple concession curves.
 */
public class DefaultConcessionPolicy implements ConcessionPolicy {
    private ConcessionCurve curve;
    private double concessionRate;  // Base concession rate
    
    public DefaultConcessionPolicy(ConcessionCurve curve, double concessionRate) {
        this.curve = curve;
        this.concessionRate = Math.max(0.01, Math.min(0.5, concessionRate));
    }
    
    @Override
    public double calculateConcession(DealContext context, DialogueState state, Role role,
                                     double currentPrice, double targetPrice, double reservationPrice) {
        double gap = Math.abs(reservationPrice - currentPrice);
        double totalGap = Math.abs(reservationPrice - targetPrice);
        
        if (gap < 0.01) return 0.0;  // Already at reservation
        
        int round = state.getCurrentRound();
        int maxRounds = context.getTimeLimit();
        double progress = (double) round / maxRounds;
        
        double baseConcession = gap * concessionRate;
        
        switch (curve) {
            case LINEAR:
                return baseConcession;
                
            case BOULWARE:
                // Concede slowly early, quickly late
                return baseConcession * Math.pow(progress, 2);
                
            case CONCEDER:
                // Concede quickly early, slowly late
                return baseConcession * Math.pow(1 - progress, 2);
                
            case TIT_FOR_TAT:
                // Match opponent's last concession
                Offer oppLast = state.getLastOfferFrom(role.opposite());
                if (oppLast != null) {
                    java.util.List<Offer> oppOffers = state.getOffersFrom(role.opposite());
                    if (oppOffers.size() >= 2) {
                        Offer oppPrevious = oppOffers.get(oppOffers.size() - 2);
                        double oppConcession = oppLast.getConcessionAmount(oppPrevious);
                        return Math.min(baseConcession, oppConcession);
                    }
                }
                return baseConcession * 0.5;  // Default to half
                
            case TIME_DEPENDENT:
                // Increase concessions as time runs out
                double timePressure = Math.pow(progress, 1.5);
                return baseConcession * (1 + timePressure);
                
            default:
                return baseConcession;
        }
    }
    
    @Override
    public ConcessionCurve getConcessionCurve() {
        return curve;
    }
    
    @Override
    public void setConcessionCurve(ConcessionCurve curve) {
        this.curve = curve;
    }
    
    @Override
    public double calculateReactiveConcession(double opponentConcession, double timePressure) {
        // Match 50-100% of opponent's concession based on time pressure
        double matchRate = 0.5 + (0.5 * timePressure);
        return opponentConcession * matchRate;
    }
    
    @Override
    public String getPolicyName() {
        return "DefaultConcessionPolicy(" + curve + ")";
    }
    
    // Getters and setters
    public double getConcessionRate() { return concessionRate; }
    public void setConcessionRate(double concessionRate) { 
        this.concessionRate = Math.max(0.01, Math.min(0.5, concessionRate)); 
    }
}

