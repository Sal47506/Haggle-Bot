package dealdialect.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import dealdialect.engine.*;
import dealdialect.metrics.Metrics;
import java.util.Map;

/**
 * Metrics panel showing live negotiation statistics.
 */
public class MetricsView {
    
    private VBox view;
    private NegotiationEngine engine;
    
    // Metric labels
    private Label roundLabel;
    private Label buyerTrustLabel;
    private Label sellerTrustLabel;
    private Label buyerBluffLabel;
    private Label sellerBluffLabel;
    private Label sscLabel;
    
    public MetricsView(NegotiationEngine engine) {
        this.engine = engine;
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(10));
        view.setPrefWidth(300);
        view.setStyle("-fx-background-color: #FFFEF7; -fx-border-color: #CCCCCC;");
        
        Label title = new Label("Live Metrics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        roundLabel = new Label("Round: 0 / 20");
        buyerTrustLabel = new Label("Buyer Trust: 1.00");
        sellerTrustLabel = new Label("Seller Trust: 1.00");
        buyerBluffLabel = new Label("Buyer Bluffs: 0 (100% success)");
        sellerBluffLabel = new Label("Seller Bluffs: 0 (100% success)");
        sscLabel = new Label("SSC: N/A");
        
        view.getChildren().addAll(
            title,
            new Separator(),
            roundLabel,
            new Separator(),
            buyerTrustLabel,
            sellerTrustLabel,
            new Separator(),
            buyerBluffLabel,
            sellerBluffLabel,
            new Separator(),
            sscLabel
        );
    }
    
    public void update() {
        DialogueState state = engine.getState();
        DealContext context = engine.getContext();
        
        roundLabel.setText(String.format("Round: %d / %d", 
            state.getCurrentRound(), context.getTimeLimit()));
        
        buyerTrustLabel.setText(String.format("Buyer Trust: %.2f", state.getBuyerTrust()));
        sellerTrustLabel.setText(String.format("Seller Trust: %.2f", state.getSellerTrust()));
        
        buyerBluffLabel.setText(String.format("Buyer Bluffs: %d (%.0f%% success)",
            state.getBuyerBluffCount(),
            state.getBluffSuccessRate(Role.BUYER) * 100));
        
        sellerBluffLabel.setText(String.format("Seller Bluffs: %d (%.0f%% success)",
            state.getSellerBluffCount(),
            state.getBluffSuccessRate(Role.SELLER) * 100));
        
        // Calculate SSC if deal is made
        if (state.hasAgreement()) {
            Metrics metrics = new Metrics(context, state);
            Map<String, Object> allMetrics = metrics.calculateAll();
            
            Object ssc = allMetrics.get("buyer_surplus_share");
            if (ssc != null) {
                sscLabel.setText(String.format("Buyer SSC: %.2f%%", (Double)ssc * 100));
            }
        }
    }
    
    public VBox getView() {
        return view;
    }
}

