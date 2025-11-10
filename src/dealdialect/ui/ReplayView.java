package dealdialect.ui;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;

/**
 * Replay view for step-by-step negotiation playback.
 */
public class ReplayView {
    private VBox view;
    
    public ReplayView() {
        initializeView();
    }
    
    private void initializeView() {
        view = new VBox(10);
        view.setPadding(new Insets(10));
        
        Label title = new Label("Replay Mode");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Playback controls
        HBox controls = new HBox(10);
        Button prevButton = new Button("◀ Previous");
        Button playButton = new Button("▶ Play");
        Button nextButton = new Button("Next ▶");
        Slider speedSlider = new Slider(0.5, 3.0, 1.0);
        
        controls.getChildren().addAll(prevButton, playButton, nextButton, 
                                      new Label("Speed:"), speedSlider);
        
        view.getChildren().addAll(title, controls);
    }
    
    public VBox getView() {
        return view;
    }
}

