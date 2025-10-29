package utils;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

/**
 * UI Helper utilities for consistent styling
 */
public class UIHelper {
    
    // Color scheme
    public static final String PRIMARY_COLOR = "#27ae60";
    public static final String SECONDARY_COLOR = "#3498db";
    public static final String DANGER_COLOR = "#e74c3c";
    public static final String WARNING_COLOR = "#f39c12";
    public static final String INFO_COLOR = "#9b59b6";
    public static final String DARK_COLOR = "#2c3e50";
    public static final String LIGHT_COLOR = "#ecf0f1";
    
    /**
     * Create styled button
     */
    public static Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        btn.setPrefHeight(50);
        styleButton(btn, color);
        return btn;
    }
    
    /**
     * Create small button
     */
    public static Button createSmallButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(150);
        btn.setPrefHeight(40);
        styleButton(btn, color);
        btn.setStyle(btn.getStyle() + " -fx-font-size: 14px;");
        return btn;
    }
    
    /**
     * Apply button styling
     */
    private static void styleButton(Button btn, String color) {
        String style = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand;",
            color
        );
        btn.setStyle(style);
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(style.replace(color, "derive(" + color + ", -20%)")));
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(style));
    }
    
    /**
     * Create styled text field
     */
    public static TextField createTextField(String prompt, int width) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(width);
        field.setStyle("-fx-font-size: 16px; -fx-padding: 10px;");
        return field;
    }
    
    /**
     * Create styled password field
     */
    public static PasswordField createPasswordField(String prompt, int width) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setMaxWidth(width);
        field.setStyle("-fx-font-size: 16px; -fx-padding: 10px;");
        return field;
    }
    
    /**
     * Create title text
     */
    public static Text createTitle(String text) {
        Text title = new Text(text);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.web(DARK_COLOR));
        return title;
    }
    
    /**
     * Create heading text
     */
    public static Text createHeading(String text) {
        Text heading = new Text(text);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        heading.setFill(Color.WHITE);
        return heading;
    }
    
    /**
     * Create label with style
     */
    public static Label createLabel(String text, int fontSize, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        label.setTextFill(color);
        return label;
    }
    
    /**
     * Show info alert
     */
    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }
    
    /**
     * Show error alert
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }
    
    /**
     * Show warning alert
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }
    
    /**
     * Show confirmation dialog
     */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        return result == ButtonType.OK;
    }
    
    /**
     * Generic alert
     */
    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Create gradient background
     */
    public static String createGradientBackground(String color1, String color2) {
        return String.format(
            "-fx-background-color: linear-gradient(to bottom, %s, %s);",
            color1, color2
        );
    }
    
    /**
     * Create solid background
     */
    public static String createSolidBackground(String color) {
        return String.format("-fx-background-color: %s;", color);
    }
    
    /**
     * Create game item button
     */
    public static Button createItemButton(String text, int size) {
        Button btn = new Button(text);
        btn.setPrefSize(size, size);
        btn.setStyle(
            "-fx-font-size: 24px; " +
            "-fx-background-color: #4ecdc4; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand;"
        );
        return btn;
    }
    
    /**
     * Flash effect for node
     */
    public static void flashColor(Button button, String color, int durationMs) {
        String originalStyle = button.getStyle();
        button.setStyle(originalStyle.replaceFirst(
            "-fx-background-color: [^;]+", 
            "-fx-background-color: " + color));
        
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(durationMs),
                e -> button.setStyle(originalStyle)
            )
        );
        timeline.play();
    }
    
    /**
     * Create info panel
     */
    public static VBox createInfoPanel(String title, String value) {
        VBox panel = new VBox(5);
        panel.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        panel.getChildren().addAll(titleLabel, valueLabel);
        return panel;
    }
}