package dealdialect.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import dealdialect.engine.*;
import dealdialect.language.TemplateLanguageModel;
import dealdialect.strategies.*;
import negotiation.models.Item;

/**
 * Main JavaFX application for DealDialect.
 * Interactive negotiation UI with human vs AI.
 */
public class MainApp extends Application {
    
    private NegotiationEngine engine;
    private ChatView chatView;
    private GraphView graphView;
    private ControlsView controlsView;
    private MetricsView metricsView;
    
    private Role humanRole = Role.BUYER;
    private Role aiRole = Role.SELLER;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DealDialect - AI Negotiation System");
        
        // Initialize negotiation
        initializeNegotiation();
        
        // Create UI components
        chatView = new ChatView(engine, humanRole);
        graphView = new GraphView(engine);
        controlsView = new ControlsView(engine, aiRole);
        metricsView = new MetricsView(engine);
        
        // Layout
        BorderPane root = new BorderPane();
        
        // Left: Chat
        root.setLeft(chatView.getView());
        
        // Center: Graph
        root.setCenter(graphView.getView());
        
        // Right: Controls + Metrics
        VBox rightPanel = new VBox(10);
        rightPanel.getChildren().addAll(
            controlsView.getView(),
            metricsView.getView()
        );
        root.setRight(rightPanel);
        
        // Create scene
        Scene scene = new Scene(root, 1400, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start with AI's first offer if seller
        if (aiRole == Role.SELLER) {
            makeAIMove();
        }
    }
    
    private void initializeNegotiation() {
        // Create sample deal
        Item laptop = new Item("electronics", "MacBook Pro 2023", 
                             "Like new condition, 16GB RAM", 1500.0);
        DealContext context = new DealContext(laptop, 1500.0, 1400.0, 1200.0, 20);
        
        // Create language model
        TemplateLanguageModel langModel = new TemplateLanguageModel();
        
        // Create engine
        engine = new NegotiationEngine(context, langModel);
        
        // Set up AI policies
        ConcessionPolicy concession = new DefaultConcessionPolicy(
            ConcessionPolicy.ConcessionCurve.TIME_DEPENDENT, 0.05
        );
        MovePolicy aiMove = new DefaultMovePolicy(0.6, 0.4, concession);
        BluffPolicy aiBluff = new DefaultBluffPolicy(0.3, 0.6);
        
        engine.setMovePolicy(aiRole, aiMove);
        engine.setBluffPolicy(aiRole, aiBluff);
        engine.setConcessionPolicy(aiRole, concession);
        engine.setTruthfulnessPolicy(new DefaultTruthfulnessPolicy(0.7, 0.02));
        
        // Add listener for UI updates
        engine.addListener(new NegotiationEngine.NegotiationListener() {
            @Override
            public void onOfferMade(Offer offer, DialogueState state) {
                javafx.application.Platform.runLater(() -> {
                    chatView.addOffer(offer);
                    graphView.update();
                    metricsView.update();
                });
            }
            
            @Override
            public void onBluffDetected(Offer offer, double confidence, DialogueState state) {
                javafx.application.Platform.runLater(() -> {
                    chatView.highlightBluff(offer, confidence);
                });
            }
        });
    }
    
    private void makeAIMove() {
        if (!engine.getState().isTerminal()) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);  // Simulate thinking
                    Offer aiOffer = engine.step(aiRole);
                    
                    if (aiOffer != null && aiOffer.getIntent() == Offer.Intent.ACCEPT) {
                        showAgreementDialog();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void showAgreementDialog() {
        // TODO: Show agreement dialog with final metrics
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

