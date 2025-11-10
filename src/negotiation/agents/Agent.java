package negotiation.agents;

import negotiation.models.*;
import negotiation.strategy.INegotiationStrategy;
import negotiation.grammar.GrammarEngine;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for negotiation agents.
 * Implements common functionality and defines abstract methods for subclasses.
 */
public abstract class Agent implements IAgent {
    protected String name;
    protected String role;
    protected IUtilityFunction utilityModel;
    protected INegotiationStrategy strategy;
    protected GrammarEngine grammar;
    protected List<Message> conversationHistory;
    protected List<Offer> offerHistory;
    protected Item negotiatingItem;
    
    public Agent(String name, String role, IUtilityFunction utilityModel, 
                 INegotiationStrategy strategy, GrammarEngine grammar) {
        this.name = name;
        this.role = role;
        this.utilityModel = utilityModel;
        this.strategy = strategy;
        this.grammar = grammar;
        this.conversationHistory = new ArrayList<>();
        this.offerHistory = new ArrayList<>();
    }
    
    @Override
    public abstract Offer proposeOffer();
    
    @Override
    public abstract Offer respondToOffer(Offer incoming);
    
    @Override
    public String speak(String category) {
        if (grammar != null) {
            return grammar.generateSentence(category);
        }
        return "";
    }
    
    @Override
    public void updateState(Message message) {
        conversationHistory.add(message);
        if (message.getOffer() != null) {
            offerHistory.add(message.getOffer());
        }
    }
    
    @Override
    public boolean shouldWalkAway(int currentRound) {
        // Default implementation - can be overridden
        if (!offerHistory.isEmpty()) {
            Offer lastOffer = offerHistory.get(offerHistory.size() - 1);
            return !utilityModel.isAcceptable(lastOffer, currentRound);
        }
        return false;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getRole() {
        return role;
    }
    
    @Override
    public void reset() {
        conversationHistory.clear();
        offerHistory.clear();
    }
    
    // Protected helper methods for subclasses
    protected Offer getLastOffer() {
        return offerHistory.isEmpty() ? null : offerHistory.get(offerHistory.size() - 1);
    }
    
    protected int getCurrentRound() {
        return conversationHistory.size();
    }
    
    public void setNegotiatingItem(Item item) {
        this.negotiatingItem = item;
    }
    
    public Item getNegotiatingItem() {
        return negotiatingItem;
    }
    
    public IUtilityFunction getUtilityModel() {
        return utilityModel;
    }
    
    public INegotiationStrategy getStrategy() {
        return strategy;
    }
    
    public List<Offer> getOfferHistory() {
        return new ArrayList<>(offerHistory);
    }
}
