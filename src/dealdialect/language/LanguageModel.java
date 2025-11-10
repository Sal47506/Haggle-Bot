package dealdialect.language;

import dealdialect.engine.*;

/**
 * Interface for natural language generation in negotiations.
 * Can be implemented with LLMs or template-based systems.
 */
public interface LanguageModel {
    
    /**
     * Generate an utterance for an offer
     * @param intent The negotiation intent
     * @param price The price being offered
     * @param context The deal context
     * @param state Current dialogue state
     * @param role The role speaking
     * @return Generated utterance text
     */
    String generateUtterance(Offer.Intent intent, double price, DealContext context, 
                            DialogueState state, Role role);
    
    /**
     * Generate bluff/puff text
     * @param intent The bluff intent
     * @param bluffStrength How strong the bluff is
     * @param context The deal context
     * @param role The role bluffing
     * @return Generated bluff text
     */
    String generateBluffText(Offer.Intent intent, double bluffStrength, 
                            DealContext context, Role role);
    
    /**
     * Parse human input to extract price and intent
     * @param humanInput The text from human
     * @return Parsed offer, or null if can't parse
     */
    Offer parseHumanInput(String humanInput, Role humanRole);
    
    /**
     * Get model name/type
     */
    String getModelName();
}

