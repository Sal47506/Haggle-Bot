package negotiation.models;

/**
 * Implementation of utility function for negotiation agents.
 * Evaluates offers based on price, time pressure, and risk tolerance.
 */
public class UtilityModel implements IUtilityFunction {
    private double reservationPrice;
    private double targetPrice;
    private double riskTolerance;
    private double timePressure;
    private String role; // "buyer" or "seller"
    
    /**
     * Constructor for buyer/seller utility model
     * @param reservationPrice minimum acceptable (for seller) or maximum acceptable (for buyer)
     * @param targetPrice ideal price
     * @param riskTolerance willingness to take risks (0.0 to 1.0)
     * @param timePressure urgency to close deal (0.0 to 1.0)
     * @param role "buyer" or "seller"
     */
    public UtilityModel(double reservationPrice, double targetPrice, 
                       double riskTolerance, double timePressure, String role) {
        this.reservationPrice = reservationPrice;
        this.targetPrice = targetPrice;
        this.riskTolerance = riskTolerance;
        this.timePressure = timePressure;
        this.role = role.toLowerCase();
    }
    
    @Override
    public double evaluate(Offer offer, int round) {
        double baseUtility = calculateBaseUtility(offer.getPrice());
        double timeDiscount = calculateTimeDiscount(round);
        double riskAdjustment = calculateRiskAdjustment(offer.getPrice());
        
        // Add utility for extras
        double extrasUtility = 0.0;
        if (offer.hasWarranty()) extrasUtility += 0.1;
        if (offer.hasFreeDelivery()) extrasUtility += 0.05;
        
        return (baseUtility + extrasUtility) * timeDiscount * riskAdjustment;
    }
    
    private double calculateBaseUtility(double price) {
        if (role.equals("buyer")) {
            // Buyer: lower price is better
            if (price > reservationPrice) return 0.0; // Unacceptable
            return 1.0 - ((price - targetPrice) / (reservationPrice - targetPrice));
        } else {
            // Seller: higher price is better
            if (price < reservationPrice) return 0.0; // Unacceptable
            return 1.0 - ((targetPrice - price) / (targetPrice - reservationPrice));
        }
    }
    
    private double calculateTimeDiscount(int round) {
        // Utility decreases with time based on time pressure
        return 1.0 - (timePressure * round * 0.05);
    }
    
    private double calculateRiskAdjustment(double price) {
        // Risk-averse agents prefer prices closer to target
        double deviation = Math.abs(price - targetPrice) / Math.abs(targetPrice);
        return 1.0 - (deviation * (1.0 - riskTolerance));
    }
    
    @Override
    public boolean isAcceptable(Offer offer, int round) {
        return evaluate(offer, round) > 0.5; // Accept if utility > 50%
    }
    
    @Override
    public double getReservationPrice() {
        return reservationPrice;
    }
    
    @Override
    public double getTargetPrice() {
        return targetPrice;
    }
    
    @Override
    public double getTimePressure() {
        return timePressure;
    }
    
    public double getRiskTolerance() {
        return riskTolerance;
    }
    
    public String getRole() {
        return role;
    }
}
