package negotiation.models;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents a complete negotiation transcript from a dataset.
 * Stores dialogue, offers, agents, and outcome information.
 */
public class NegotiationTranscript {
    private String transcriptId;
    private Item item;
    private AgentInfo buyerInfo;
    private AgentInfo sellerInfo;
    private List<String> utterances;
    private List<Integer> agentTurns;
    private List<DialogueAct> dialogueActs;
    private boolean agreed;
    private Offer finalOffer;
    
    public NegotiationTranscript(String transcriptId) {
        this.transcriptId = transcriptId;
        this.utterances = new ArrayList<>();
        this.agentTurns = new ArrayList<>();
        this.dialogueActs = new ArrayList<>();
        this.agreed = false;
    }
    
    /**
     * Inner class to store agent information from datasets
     */
    public static class AgentInfo {
        private String role;
        private double target;
        private double bottomLine;
        
        public AgentInfo(String role, double target, double bottomLine) {
            this.role = role;
            this.target = target;
            this.bottomLine = bottomLine;
        }
        
        public String getRole() { return role; }
        public double getTarget() { return target; }
        public double getBottomLine() { return bottomLine; }
    }
    
    // Getters and setters
    public String getTranscriptId() { return transcriptId; }
    
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    
    public AgentInfo getBuyerInfo() { return buyerInfo; }
    public void setBuyerInfo(AgentInfo buyerInfo) { this.buyerInfo = buyerInfo; }
    
    public AgentInfo getSellerInfo() { return sellerInfo; }
    public void setSellerInfo(AgentInfo sellerInfo) { this.sellerInfo = sellerInfo; }
    
    public List<String> getUtterances() { return utterances; }
    public void addUtterance(String utterance) { this.utterances.add(utterance); }
    
    public List<Integer> getAgentTurns() { return agentTurns; }
    public void addAgentTurn(int turn) { this.agentTurns.add(turn); }
    
    public List<DialogueAct> getDialogueActs() { return dialogueActs; }
    public void addDialogueAct(DialogueAct act) { this.dialogueActs.add(act); }
    
    public boolean isAgreed() { return agreed; }
    public void setAgreed(boolean agreed) { this.agreed = agreed; }
    
    public Offer getFinalOffer() { return finalOffer; }
    public void setFinalOffer(Offer finalOffer) { this.finalOffer = finalOffer; }
    
    public int getLength() { return utterances.size(); }
    
    @Override
    public String toString() {
        return String.format("Transcript{id=%s, length=%d, agreed=%b}", 
                           transcriptId, getLength(), agreed);
    }
}

