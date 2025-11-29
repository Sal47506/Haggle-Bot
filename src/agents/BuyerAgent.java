package agents;

import dialogue.MarkovDialogueGenerator;
import models.NegotiationState;
import java.util.*;

public class BuyerAgent {
    
    private MarkovDialogueGenerator dialogueGen;
    private double reservationPrice;
    private double targetPrice;
    private double currentOffer;
    private NegotiationState state;
    private Random random;
    private List<Double> offerHistory;
    
    public BuyerAgent(String datasetPath, double reservationPrice, double targetPrice) throws Exception {
        this.dialogueGen = new MarkovDialogueGenerator(datasetPath, 3);
        this.reservationPrice = reservationPrice;
        this.targetPrice = targetPrice;
        this.currentOffer = targetPrice;
        this.random = new Random();
        this.offerHistory = new ArrayList<>();
        this.state = new NegotiationState(0, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
    }
    
    public String respondToSeller(String sellerMessage, double sellerPrice) {
        String intent = decideIntent(sellerPrice);
        double offerPrice = decidePrice(intent, sellerPrice);
        
        currentOffer = offerPrice;
        offerHistory.add(offerPrice);
        
        state = new NegotiationState(
            state.getRound() + 1,
            currentOffer,
            reservationPrice,
            targetPrice,
            sellerPrice,
            offerHistory
        );
        
        dialogueGen.updateContext(state, sellerMessage);
        
        String response = dialogueGen.generate(intent, offerPrice, sellerMessage);
        
        return response;
    }
    
    public String makeInitialOffer() {
        currentOffer = targetPrice;
        offerHistory.clear();
        offerHistory.add(currentOffer);
        
        state = new NegotiationState(1, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
        dialogueGen.updateContext(state, null);
        
        return dialogueGen.generate("OFFER", currentOffer);
    }
    
    private String decideIntent(double sellerPrice) {
        if (sellerPrice <= reservationPrice) {
            state.setDealReached(true);
            return "ACCEPT";
        }
        
        double priceGap = Math.abs(sellerPrice - currentOffer);
        double relativegGap = priceGap / sellerPrice;
        
        if (state.getRound() > 7 && sellerPrice <= reservationPrice * 1.2) {
            state.setDealReached(true);
            return "ACCEPT";
        }
        
        if (relativegGap < 0.15 && state.getRound() > 4) {
            state.setDealReached(true);
            return "ACCEPT";
        }
        
        if (state.getRound() > 10) {
            if (sellerPrice <= reservationPrice * 1.3) {
                state.setDealReached(true);
                return "ACCEPT";
            }
            return "REJECT";
        }
        
        if (sellerPrice > reservationPrice * 2.0 && state.getRound() < 3) {
            return "REJECT";
        }
        
        return "COUNTER";
    }
    
    private double decidePrice(String intent, double sellerPrice) {
        switch (intent) {
            case "ACCEPT":
                return sellerPrice;
                
            case "COUNTER":
                double progressFactor = Math.min(1.0, state.getRound() / 8.0);
                double minOffer = targetPrice;
                double maxOffer = reservationPrice;
                
                double baseCounter = minOffer + (maxOffer - minOffer) * progressFactor;
                
                double gapToBridge = (sellerPrice - currentOffer) * 0.3;
                double newOffer = currentOffer + gapToBridge;
                
                newOffer = Math.max(newOffer, baseCounter);
                newOffer = Math.min(newOffer, reservationPrice);
                
                newOffer = Math.max(newOffer, currentOffer * 1.05);
                
                return Math.round(newOffer * 100.0) / 100.0;
                
            case "REJECT":
                double rejectOffer = currentOffer * 1.02;
                return Math.round(rejectOffer * 100.0) / 100.0;
                
            default:
                return currentOffer;
        }
    }
    
    public boolean isDealReached() {
        return state != null && state.isDealReached();
    }
    
    public double getCurrentOffer() {
        return currentOffer;
    }
    
    public double getReservationPrice() {
        return reservationPrice;
    }
    
    public NegotiationState getState() {
        return state;
    }
    
    public void reset() {
        this.currentOffer = targetPrice;
        this.offerHistory.clear();
        this.state = new NegotiationState(0, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
        dialogueGen.resetConversation();
    }
    
    public List<Double> getOfferHistory() {
        return new ArrayList<>(offerHistory);
    }
}

