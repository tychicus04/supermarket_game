package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utils.SoundManager;

public class SettingsController {
    private Stage stage;
    private Runnable onBack;
    private SoundManager soundManager;

    public SettingsController(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
        this.soundManager = SoundManager.getInstance();
    }

    public void show() {
        // Root với background
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e 0%, #16213e 100%);");

        // Main container
        VBox container = new VBox(30);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(50));
        container.setMaxWidth(600);

        // Title
        Text title = new Text("SETTINGS");
        title.setStyle("-fx-font-family: 'Courier New', monospace; " +
                      "-fx-font-size: 48px; " +
                      "-fx-font-weight: bold; " +
                      "-fx-fill: linear-gradient(to bottom, #FFE066 0%, #FFCC00 100%); " +
                      "-fx-stroke: #8B4513; " +
                      "-fx-stroke-width: 3px;");

        // Settings box
        VBox settingsBox = new VBox(25);
        settingsBox.setAlignment(Pos.CENTER);
        settingsBox.setPadding(new Insets(30));
        settingsBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                            "-fx-background-radius: 15px; " +
                            "-fx-border-color: #FFD700; " +
                            "-fx-border-width: 3px; " +
                            "-fx-border-radius: 15px;");

        // Sound Effects Section
        VBox soundSection = createSoundSection();

        // Music Section
        VBox musicSection = createMusicSection();

        // Sound Enable/Disable
        HBox soundToggleBox = createToggleBox("Enable Sound Effects", soundManager.isSoundEnabled(), (enabled) -> {
            soundManager.setSoundEnabled(enabled);
            if (enabled) {
                soundManager.play("menu_button");
            }
        });

        // Music Enable/Disable
        HBox musicToggleBox = createToggleBox("Enable Music", soundManager.isMusicEnabled(), (enabled) -> {
            soundManager.setMusicEnabled(enabled);
            if (enabled) {
                soundManager.playMusic("menu_music");
            } else {
                soundManager.stopMusic();
            }
        });

        settingsBox.getChildren().addAll(soundSection, musicSection, soundToggleBox, musicToggleBox);

        // Back button
        Button backBtn = getButton();

        container.getChildren().addAll(title, settingsBox, backBtn);
        root.getChildren().add(container);

        // Get current stage size
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
    }

    private Button getButton() {
        Button backBtn = new Button("BACK TO MENU");
        backBtn.setStyle("-fx-min-width: 280px; " +
                        "-fx-pref-height: 55px; " +
                        "-fx-font-family: 'Courier New', monospace; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: #E94F37; " +
                        "-fx-border-color: #3E2A1C; " +
                        "-fx-border-width: 3px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backBtn.getStyle() + "-fx-background-color: #D63F2D;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(backBtn.getStyle() + "-fx-background-color: #E94F37;"));
        backBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            // Save settings before going back
            soundManager.saveSettings();
            onBack.run();
        });
        return backBtn;
    }

    private VBox createSoundSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.CENTER_LEFT);

        // Label
        Label label = new Label("Sound Effects Volume");
        label.setStyle("-fx-font-family: 'Courier New', monospace; " +
                      "-fx-font-size: 18px; " +
                      "-fx-font-weight: bold; " +
                      "-fx-text-fill: white;");

        // Volume value display
        Label valueLabel = new Label(String.format("%.0f%%", soundManager.getSoundVolume() * 100));
        valueLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                           "-fx-font-size: 16px; " +
                           "-fx-text-fill: #FFD700;");

        // Slider
        Slider slider = new Slider(0, 100, soundManager.getSoundVolume() * 100);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(5);
        slider.setPrefWidth(400);
        slider.setStyle("-fx-control-inner-background: #2C3E50;");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double volume = newVal.doubleValue() / 100.0;
            soundManager.setSoundVolume(volume);
            valueLabel.setText(String.format("%.0f%%", newVal.doubleValue()));

            // Play test sound every 10%
            if (Math.abs(newVal.doubleValue() - oldVal.doubleValue()) >= 10) {
                soundManager.play("menu_button");
            }
        });

        HBox sliderBox = new HBox(15, slider, valueLabel);
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(label, sliderBox);
        return section;
    }

    private VBox createMusicSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.CENTER_LEFT);

        // Label
        Label label = new Label("Music Volume");
        label.setStyle("-fx-font-family: 'Courier New', monospace; " +
                      "-fx-font-size: 18px; " +
                      "-fx-font-weight: bold; " +
                      "-fx-text-fill: white;");

        // Volume value display
        Label valueLabel = new Label(String.format("%.0f%%", soundManager.getMusicVolume() * 100));
        valueLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                           "-fx-font-size: 16px; " +
                           "-fx-text-fill: #FFD700;");

        // Slider
        Slider slider = new Slider(0, 100, soundManager.getMusicVolume() * 100);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(5);
        slider.setPrefWidth(400);
        slider.setStyle("-fx-control-inner-background: #2C3E50;");

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double volume = newVal.doubleValue() / 100.0;
            soundManager.setMusicVolume(volume);
            valueLabel.setText(String.format("%.0f%%", newVal.doubleValue()));
        });

        HBox sliderBox = new HBox(15, slider, valueLabel);
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        section.getChildren().addAll(label, sliderBox);
        return section;
    }

    private HBox createToggleBox(String text, boolean initialValue, ToggleCallback callback) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Courier New', monospace; " +
                      "-fx-font-size: 16px; " +
                      "-fx-text-fill: white;");

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(initialValue);
        checkBox.setStyle("-fx-text-fill: white;");
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            callback.onToggle(newVal);
        });

        box.getChildren().addAll(checkBox, label);
        return box;
    }

    @FunctionalInterface
    private interface ToggleCallback {
        void onToggle(boolean enabled);
    }
}

