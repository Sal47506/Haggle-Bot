package dealdialect.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import dealdialect.engine.*;

/**
 * Chat panel showing negotiation dialogue.
 * Message bubbles for human and AI with quick action buttons.
 */
public class ChatView {
    
    private VBox view;
    private ScrollPane scrollPane;
    private VBox messagesContainer;
    private TextField inputField;
    private NegotiationEngine engine;
    private Role humanRole;
    
    public ChatView(NegotiationEngine engine, Role humanRole) {
        this.engine = engine;
        this.humanRole = humanRole;
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(10));
        view.setPrefWidth(400);
        
        // Title
        Label title = new Label("Negotiation Chat");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Messages area
        messagesContainer = new VBox(10);
        messagesContainer.setPadding(new Insets(10));
        
        scrollPane = new ScrollPane(messagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Input area
        HBox inputArea = new HBox(10);
        inputField = new TextField();
        inputField.setPromptText("Enter your offer...");
        inputField.setPrefWidth(250);
        
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        
        inputField.setOnAction(e -> sendMessage());
        
        inputArea.getChildren().addAll(inputField, sendButton);
        
        // Quick action buttons
        HBox quickButtons = createQuickButtons();
        
        view.getChildren().addAll(title, scrollPane, quickButtons, inputArea);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }
    
    private HBox createQuickButtons() {
        HBox buttons = new HBox(5);
        buttons.setPadding(new Insets(5));
        
        Button counterBtn = new Button("Counter");
        counterBtn.setOnAction(e -> inputField.setText("How about $"));
        
        Button acceptBtn = new Button("Accept");
        acceptBtn.setOnAction(e -> sendQuickMessage("I accept your offer!"));
        
        Button rejectBtn = new Button("Reject");
        rejectBtn.setOnAction(e -> sendQuickMessage("That's too high/low for me."));
        
        Button bluffBtn = new Button("Bluff");
        bluffBtn.setOnAction(e -> inputField.setText("This is my final offer: $"));
        
        Button justifyBtn = new Button("Ask Why");
        justifyBtn.setOnAction(e -> sendQuickMessage("Can you justify that price?"));
        
        Button threatBtn = new Button("Threaten");
        threatBtn.setOnAction(e -> sendQuickMessage("I might have to walk away..."));
        
        buttons.getChildren().addAll(counterBtn, acceptBtn, rejectBtn, 
                                     bluffBtn, justifyBtn, threatBtn);
        return buttons;
    }
    
    public void addOffer(Offer offer) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        
        // Create message bubble
        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(10));
        bubble.setMaxWidth(300);
        
        Label roleLabel = new Label(offer.getRole().toString());
        roleLabel.setStyle("-fx-font-weight: bold;");
        
        Label messageLabel = new Label(offer.getUtterance());
        messageLabel.setWrapText(true);
        
        Label priceLabel = new Label(String.format("$%.2f", offer.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        bubble.getChildren().addAll(roleLabel, messageLabel, priceLabel);
        
        // Style based on role
        if (offer.getRole() == humanRole) {
            bubble.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10;");
            messageBox.setAlignment(Pos.CENTER_LEFT);
        } else {
            bubble.setStyle("-fx-background-color: #C8E6C9; -fx-background-radius: 10;");
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        }
        
        // Highlight if bluff
        if (offer.isBluff()) {
            bubble.setStyle(bubble.getStyle() + "-fx-border-color: red; -fx-border-width: 2;");
        }
        
        messageBox.getChildren().add(bubble);
        messagesContainer.getChildren().add(messageBox);
        
        // Auto-scroll
        scrollPane.setVvalue(1.0);
    }
    
    public void highlightBluff(Offer offer, double confidence) {
        // TODO: Add visual indication of detected bluff
    }
    
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            sendQuickMessage(text);
            inputField.clear();
        }
    }
    
    private void sendQuickMessage(String text) {
        // Process human input
        engine.processHumanInput(text, humanRole);
        
        // Trigger AI response
        // TODO: Call parent to make AI move
    }
    
    public VBox getView() {
        return view;
    }
}

