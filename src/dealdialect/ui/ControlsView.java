package dealdialect.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import dealdialect.engine.*;
import dealdialect.strategies.*;

/**
 * Control panel with strategy sliders.
 * Allows real-time adjustment of AI strategy parameters.
 */
public class ControlsView {
    
    private VBox view;
    private NegotiationEngine engine;
    private Role aiRole;
    
    // Sliders
    private Slider aggressionSlider;
    private Slider riskAversionSlider;
    private Slider bluffProbSlider;
    private Slider bluffStrengthSlider;
    private Slider timePressureSlider;
    
    public ControlsView(NegotiationEngine engine, Role aiRole) {
        this.engine = engine;
        this.aiRole = aiRole;
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(15);
        view.setPadding(new Insets(10));
        view.setPrefWidth(300);
        view.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #CCCCCC;");
        
        Label title = new Label("AI Strategy Controls");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Aggression slider
        VBox aggressionBox = createSliderBox("Aggression", 0.6);
        aggressionSlider = (Slider) ((HBox)aggressionBox.getChildren().get(1)).getChildren().get(0);
        aggressionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateAggression(newVal.doubleValue());
        });
        
        // Risk aversion slider
        VBox riskBox = createSliderBox("Risk Aversion", 0.4);
        riskAversionSlider = (Slider) ((HBox)riskBox.getChildren().get(1)).getChildren().get(0);
        riskAversionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRiskAversion(newVal.doubleValue());
        });
        
        // Bluff probability slider
        VBox bluffProbBox = createSliderBox("Bluff Probability", 0.3);
        bluffProbSlider = (Slider) ((HBox)bluffProbBox.getChildren().get(1)).getChildren().get(0);
        bluffProbSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateBluffProbability(newVal.doubleValue());
        });
        
        // Bluff strength slider
        VBox bluffStrengthBox = createSliderBox("Max Bluff Strength", 0.6);
        bluffStrengthSlider = (Slider) ((HBox)bluffStrengthBox.getChildren().get(1)).getChildren().get(0);
        bluffStrengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateBluffStrength(newVal.doubleValue());
        });
        
        // Time pressure slider
        VBox timePressureBox = createSliderBox("Time Pressure", 0.5);
        timePressureSlider = (Slider) ((HBox)timePressureBox.getChildren().get(1)).getChildren().get(0);
        
        view.getChildren().addAll(
            title,
            new Separator(),
            aggressionBox,
            riskBox,
            bluffProbBox,
            bluffStrengthBox,
            timePressureBox
        );
    }
    
    private VBox createSliderBox(String label, double initialValue) {
        VBox box = new VBox(5);
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        HBox sliderBox = new HBox(10);
        Slider slider = new Slider(0.0, 1.0, initialValue);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(0.5);
        slider.setPrefWidth(200);
        
        Label valueLabel = new Label(String.format("%.2f", initialValue));
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });
        
        sliderBox.getChildren().addAll(slider, valueLabel);
        box.getChildren().addAll(nameLabel, sliderBox);
        
        return box;
    }
    
    private void updateAggression(double value) {
        // TODO: Update AI's move policy aggression
    }
    
    private void updateRiskAversion(double value) {
        // TODO: Update AI's move policy risk aversion
    }
    
    private void updateBluffProbability(double value) {
        // TODO: Update AI's bluff policy probability
    }
    
    private void updateBluffStrength(double value) {
        // TODO: Update AI's bluff policy max strength
    }
    
    public VBox getView() {
        return view;
    }
}

