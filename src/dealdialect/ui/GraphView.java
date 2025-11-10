package dealdialect.ui;

import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import dealdialect.engine.*;
import java.util.List;

/**
 * Graph panel showing live negotiation visualization.
 * Displays offer curves, trust, concessions, and bluff markers.
 */
public class GraphView {
    
    private VBox view;
    private LineChart<Number, Number> offerChart;
    private LineChart<Number, Number> trustChart;
    private ScatterChart<Number, Number> bluffChart;
    
    private NegotiationEngine engine;
    
    public GraphView(NegotiationEngine engine) {
        this.engine = engine;
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(10));
        view.setPrefWidth(600);
        
        // Title
        Label title = new Label("Negotiation Dynamics");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Offer chart
        NumberAxis offerX = new NumberAxis();
        offerX.setLabel("Round");
        NumberAxis offerY = new NumberAxis();
        offerY.setLabel("Price ($)");
        
        offerChart = new LineChart<>(offerX, offerY);
        offerChart.setTitle("Offer Progression");
        offerChart.setPrefHeight(250);
        offerChart.setCreateSymbols(true);
        
        XYChart.Series<Number, Number> buyerSeries = new XYChart.Series<>();
        buyerSeries.setName("Buyer Offers");
        XYChart.Series<Number, Number> sellerSeries = new XYChart.Series<>();
        sellerSeries.setName("Seller Offers");
        
        offerChart.getData().addAll(buyerSeries, sellerSeries);
        
        // Trust chart
        NumberAxis trustX = new NumberAxis();
        trustX.setLabel("Round");
        NumberAxis trustY = new NumberAxis(0, 1, 0.1);
        trustY.setLabel("Trust Level");
        
        trustChart = new LineChart<>(trustX, trustY);
        trustChart.setTitle("Trust Trajectory");
        trustChart.setPrefHeight(200);
        
        XYChart.Series<Number, Number> buyerTrust = new XYChart.Series<>();
        buyerTrust.setName("Buyer Trust");
        XYChart.Series<Number, Number> sellerTrust = new XYChart.Series<>();
        sellerTrust.setName("Seller Trust");
        
        trustChart.getData().addAll(buyerTrust, sellerTrust);
        
        // Bluff markers
        NumberAxis bluffX = new NumberAxis();
        bluffX.setLabel("Round");
        NumberAxis bluffY = new NumberAxis();
        bluffY.setLabel("Price ($)");
        
        bluffChart = new ScatterChart<>(bluffX, bluffY);
        bluffChart.setTitle("Bluff Events");
        bluffChart.setPrefHeight(150);
        
        XYChart.Series<Number, Number> bluffMarkers = new XYChart.Series<>();
        bluffMarkers.setName("Bluffs");
        bluffChart.getData().add(bluffMarkers);
        
        view.getChildren().addAll(title, offerChart, trustChart, bluffChart);
    }
    
    public void update() {
        updateOfferChart();
        updateTrustChart();
        updateBluffChart();
    }
    
    private void updateOfferChart() {
        DialogueState state = engine.getState();
        
        XYChart.Series<Number, Number> buyerSeries = offerChart.getData().get(0);
        XYChart.Series<Number, Number> sellerSeries = offerChart.getData().get(1);
        
        buyerSeries.getData().clear();
        sellerSeries.getData().clear();
        
        List<Offer> buyerOffers = state.getOffersFrom(Role.BUYER);
        List<Offer> sellerOffers = state.getOffersFrom(Role.SELLER);
        
        for (Offer offer : buyerOffers) {
            buyerSeries.getData().add(new XYChart.Data<>(offer.getRoundNumber(), offer.getPrice()));
        }
        
        for (Offer offer : sellerOffers) {
            sellerSeries.getData().add(new XYChart.Data<>(offer.getRoundNumber(), offer.getPrice()));
        }
        
        // Add ZOPA shading (TODO: Custom rendering)
    }
    
    private void updateTrustChart() {
        // TODO: Track trust over time and plot
    }
    
    private void updateBluffChart() {
        DialogueState state = engine.getState();
        XYChart.Series<Number, Number> bluffMarkers = bluffChart.getData().get(0);
        bluffMarkers.getData().clear();
        
        for (Offer offer : state.getHistory()) {
            if (offer.isBluff()) {
                bluffMarkers.getData().add(new XYChart.Data<>(
                    offer.getRoundNumber(), offer.getPrice()
                ));
            }
        }
    }
    
    public VBox getView() {
        return view;
    }
}

