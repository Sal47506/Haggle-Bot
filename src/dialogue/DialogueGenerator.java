package dialogue;

import models.NegotiationState;

public interface DialogueGenerator {
    String generate(String intent, double price);
    String generate(String intent, double price, String opponentMessage);
    void updateContext(NegotiationState state, String lastMessage);
    void resetConversation();
    void setItemContext(String item);
}
