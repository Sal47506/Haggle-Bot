package negotiation.strategy;

/**
 * Enumeration of possible negotiation moves.
 * Based on dialogue act taxonomy from negotiation literature.
 */
public enum MoveType {
    /**
     * Make an initial price offer
     */
    INIT_PRICE,
    
    /**
     * Make a counter-offer with a different price
     */
    COUNTER_OFFER,
    
    /**
     * Accept the current offer
     */
    ACCEPT,
    
    /**
     * Reject the current offer without counter
     */
    REJECT,
    
    /**
     * Provide information or ask questions
     */
    INFORM,
    
    /**
     * Request information from the other party
     */
    INQUIRE,
    
    /**
     * Insist on current position
     */
    INSIST,
    
    /**
     * Express agreement or positive sentiment
     */
    AGREE,
    
    /**
     * Express disagreement or negative sentiment
     */
    DISAGREE,
    
    /**
     * Offer additional items or services (e.g., delivery, warranty)
     */
    OFFER_SIDE_DEAL,
    
    /**
     * Walk away from negotiation
     */
    QUIT,
    
    /**
     * Unknown or unclear intent
     */
    UNKNOWN;
    
    /**
     * Convert string intent from dataset to MoveType
     * @param intent intent string from dataset
     * @return corresponding MoveType
     */
    public static MoveType fromIntent(String intent) {
        if (intent == null || intent.isEmpty()) {
            return UNKNOWN;
        }
        
        switch (intent.toLowerCase().replace("-", "_")) {
            case "init_price":
            case "initprice":
                return INIT_PRICE;
            case "accept":
                return ACCEPT;
            case "reject":
                return REJECT;
            case "counter":
            case "counter_offer":
                return COUNTER_OFFER;
            case "inform":
                return INFORM;
            case "inquire":
            case "ask":
                return INQUIRE;
            case "insist":
                return INSIST;
            case "agree":
                return AGREE;
            case "disagree":
                return DISAGREE;
            case "offer":
                return OFFER_SIDE_DEAL;
            case "quit":
            case "walk_away":
                return QUIT;
            default:
                return UNKNOWN;
        }
    }
}
