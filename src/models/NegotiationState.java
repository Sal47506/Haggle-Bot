package models;

import java.util.List;
import java.util.ArrayList;

public class NegotiationState {
    private int round;
    private double lastOfferPrice;
    private double myReservationPrice;
    private double myTargetPrice;
    private double opponentLastOffer;
    private double priceGap;
    private double priceGapPercentage;
    private int consecutiveRejects;
    private boolean dealReached;
    private List<Double> offerHistory;
    
    public NegotiationState(int round, double lastOfferPrice, 
                          double myReservationPrice, double myTargetPrice,
                          double opponentLastOffer, List<Double> offerHistory) {
        this.round = round;
        this.lastOfferPrice = lastOfferPrice;
        this.myReservationPrice = myReservationPrice;
        this.myTargetPrice = myTargetPrice;
        this.opponentLastOffer = opponentLastOffer;
        this.offerHistory = offerHistory != null ? new ArrayList<>(offerHistory) : new ArrayList<>();
        
        // Calculate derived features
        this.priceGap = Math.abs(lastOfferPrice - opponentLastOffer);
        double avgPrice = (lastOfferPrice + opponentLastOffer) / 2.0;
        this.priceGapPercentage = avgPrice > 0 ? priceGap / avgPrice : 0.0;
        this.consecutiveRejects = 0;
        this.dealReached = false;
    }
    
    /**
     * Convert state to feature vector for neural network
     */
    public double[] toFeatureVector() {
        return new double[] {
            round / 20.0,  // Normalized round
            lastOfferPrice / 1000.0,  // Normalized price
            myReservationPrice / 1000.0,
            myTargetPrice / 1000.0,
            opponentLastOffer / 1000.0,
            priceGapPercentage,
            consecutiveRejects / 5.0,
            dealReached ? 1.0 : 0.0
        };
    }
    
    // Getters
    public int getRound() { return round; }
    public double getLastOfferPrice() { return lastOfferPrice; }
    public double getMyReservationPrice() { return myReservationPrice; }
    public double getMyTargetPrice() { return myTargetPrice; }
    public double getOpponentLastOffer() { return opponentLastOffer; }
    public double getPriceGap() { return priceGap; }
    public double getPriceGapPercentage() { return priceGapPercentage; }
    public int getConsecutiveRejects() { return consecutiveRejects; }
    public boolean isDealReached() { return dealReached; }
    public List<Double> getOfferHistory() { return new ArrayList<>(offerHistory); }
    
    // Setters
    public void setConsecutiveRejects(int consecutiveRejects) { 
        this.consecutiveRejects = consecutiveRejects; 
    }
    public void setDealReached(boolean dealReached) { 
        this.dealReached = dealReached; 
    }
    public void addOffer(double price) {
        offerHistory.add(price);
    }
}

