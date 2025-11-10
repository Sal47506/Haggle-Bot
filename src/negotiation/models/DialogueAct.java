package negotiation.models;

import negotiation.strategy.MoveType;

/**
 * Represents a dialogue act in the negotiation.
 * Combines the strategic intent with concrete parameters (price, etc.)
 */
public class DialogueAct {
    private MoveType intent;
    private double price;
    private String text;
    
    public DialogueAct(MoveType intent, double price) {
        this.intent = intent;
        this.price = price;
        this.text = "";
    }
    
    public DialogueAct(MoveType intent, double price, String text) {
        this.intent = intent;
        this.price = price;
        this.text = text;
    }
    
    public MoveType getIntent() {
        return intent;
    }
    
    public void setIntent(MoveType intent) {
        this.intent = intent;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return String.format("DialogueAct{intent=%s, price=%.2f, text='%s'}", 
                           intent, price, text);
    }
}

