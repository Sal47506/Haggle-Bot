package negotiation.models;

import negotiation.agents.IAgent;

/**
 * Represents a message in the negotiation dialogue.
 * Contains both the linguistic content and the associated offer/dialogue act.
 */
public class Message {
    private IAgent sender;
    private String text;
    private Offer offer;
    private DialogueAct dialogueAct;
    private long timestamp;
    private int roundNumber;
    
    public Message(IAgent sender, String text, DialogueAct dialogueAct) {
        this.sender = sender;
        this.text = text;
        this.dialogueAct = dialogueAct;
        this.offer = null;
        this.timestamp = System.currentTimeMillis();
        this.roundNumber = 0;
    }
    
    public Message(IAgent sender, String text, Offer offer, DialogueAct dialogueAct) {
        this.sender = sender;
        this.text = text;
        this.offer = offer;
        this.dialogueAct = dialogueAct;
        this.timestamp = System.currentTimeMillis();
        this.roundNumber = 0;
    }
    
    // Getters and setters
    public IAgent getSender() { return sender; }
    public void setSender(IAgent sender) { this.sender = sender; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public Offer getOffer() { return offer; }
    public void setOffer(Offer offer) { this.offer = offer; }
    
    public DialogueAct getDialogueAct() { return dialogueAct; }
    public void setDialogueAct(DialogueAct dialogueAct) { this.dialogueAct = dialogueAct; }
    
    public long getTimestamp() { return timestamp; }
    
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    
    @Override
    public String toString() {
        return String.format("[Round %d] %s (%s): %s [%s]", 
                           roundNumber,
                           sender.getName(), 
                           sender.getRole(),
                           text, 
                           dialogueAct != null ? dialogueAct.getIntent() : "NONE");
    }
}
