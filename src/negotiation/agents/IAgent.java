package negotiation.agents;

import negotiation.models.Offer;
import negotiation.models.Message;

/**
 * Core interface for negotiation agents.
 * Agents can be buyers or sellers who make offers and respond to counteroffers.
 */
public interface IAgent {
    
    /**
     * Propose an initial or counter offer based on current negotiation state
     * @return the offer proposed by this agent
     */
    Offer proposeOffer();
    
    /**
     * Respond to an incoming offer from the other agent
     * @param incoming the offer received from the other party
     * @return this agent's response offer (or null if rejecting)
     */
    Offer respondToOffer(Offer incoming);
    
    /**
     * Generate natural language text for the current negotiation move
     * @param category the type of utterance to generate (e.g., "opening", "counter", "accept")
     * @return the generated sentence
     */
    String speak(String category);
    
    /**
     * Update the agent's internal state after a negotiation round
     * @param message the message from the previous round
     */
    void updateState(Message message);
    
    /**
     * Check if the agent wants to walk away from the negotiation
     * @param currentRound the current round number
     * @return true if agent walks away, false otherwise
     */
    boolean shouldWalkAway(int currentRound);
    
    /**
     * Get the agent's name or identifier
     * @return agent name
     */
    String getName();
    
    /**
     * Get the agent's role (buyer or seller)
     * @return role as string
     */
    String getRole();
    
    /**
     * Reset the agent's state for a new negotiation
     */
    void reset();
}

