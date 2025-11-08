package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Message;
import network.NetworkManager;
import utils.UIHelper;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

/**
 * Login/Register screen controller
 */
public class LoginController {
    private Stage stage;
    private Runnable onLoginSuccess;
    private NetworkManager network;
    
    private TextField usernameField;
    private PasswordField passwordField;
    private Label statusLabel;
    
    public LoginController(Stage stage, Runnable onLoginSuccess) {
        this.stage = stage;
        this.onLoginSuccess = onLoginSuccess;
        this.network = NetworkManager.getInstance();
    }
    
    public void show() {
        // Root container with background image
        StackPane root = new StackPane();
        root.getStyleClass().add("login-root");

        // Login box container - positioned at center-bottom
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPrefSize(480, 280);
        loginBox.setMaxSize(480, 280);
        loginBox.getStyleClass().add("login-box");

        // Position the box: center horizontally, 20-25% from bottom
        StackPane.setAlignment(loginBox, Pos.CENTER);
        StackPane.setMargin(loginBox, new Insets(0, 0, 150, 0)); // Bottom margin to position

        // Title - pixel style
        Label title = new Label("LOGIN TO PLAY");
        title.getStyleClass().add("login-title");

        // Input fields with pixel styling
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(380);
        usernameField.setPrefHeight(40);
        usernameField.getStyleClass().add("login-input");
        usernameField.setStyle(usernameField.getStyle() + "-fx-prompt-text-fill: #000000;");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(380);
        passwordField.setPrefHeight(40);
        passwordField.getStyleClass().add("login-input");
        passwordField.setStyle(passwordField.getStyle() + "-fx-prompt-text-fill: #000000;");

        // Status label
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(380);
        statusLabel.setAlignment(Pos.CENTER);

        // Buttons with pixel style
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button loginBtn = new Button("LOGIN");
        loginBtn.setPrefWidth(180);
        loginBtn.setPrefHeight(45);
        loginBtn.getStyleClass().add("pixel-btn-login");

        Button registerBtn = new Button("REGISTER");
        registerBtn.setPrefWidth(180);
        registerBtn.setPrefHeight(45);
        registerBtn.getStyleClass().add("pixel-btn-register");

        buttonBox.getChildren().addAll(loginBtn, registerBtn);
        
        // Slogan text at bottom
        Label sloganLabel = new Label("Quản lý siêu thị mơ ước của bạn!");
        sloganLabel.getStyleClass().add("slogan-text");

        // Add fade-in animation for slogan
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), sloganLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(0.85);
        fadeIn.setCycleCount(1);
        fadeIn.setAutoReverse(false);
        fadeIn.play();

        // Events
        loginBtn.setOnAction(e -> handleLogin());
        registerBtn.setOnAction(e -> handleRegister());
        
        // Enter key to login
        passwordField.setOnAction(e -> handleLogin());
        
        // Add all components to login box
        loginBox.getChildren().addAll(
            title,
            usernameField,
            passwordField,
            buttonBox,
            statusLabel,
            sloganLabel
        );

        root.getChildren().add(loginBox);

        // Create scene with CSS
        Scene scene = new Scene(root, 1024, 768);

        // Load CSS file
        try {
            java.net.URL cssResource = getClass().getResource("/resources/assets/css/login-pixel-style.css");
            if (cssResource != null) {
                String css = cssResource.toExternalForm();
                scene.getStylesheets().add(css);
            } else {
                System.err.println("CSS file not found, using fallback styles");
                applyFallbackStyles(root, loginBox, title, usernameField, passwordField,
                                   loginBtn, registerBtn, sloganLabel);
            }
        } catch (Exception e) {
            System.err.println("Failed to load CSS: " + e.getMessage());
            // Fallback to inline styles if CSS fails
            applyFallbackStyles(root, loginBox, title, usernameField, passwordField,
                               loginBtn, registerBtn, sloganLabel);
        }

        stage.setScene(scene);
    }
    
    /**
     * Fallback inline styles if CSS file cannot be loaded
     */
    private void applyFallbackStyles(StackPane root, VBox loginBox, Label title,
                                     TextField usernameField, PasswordField passwordField,
                                     Button loginBtn, Button registerBtn, Label sloganLabel) {
        // Root background
        root.setStyle("-fx-background-image: url('/resources/assets/images/backgrounds/backgroundLogin.png'); " +
                     "-fx-background-size: cover; -fx-background-position: center center; " +
                     "-fx-background-repeat: no-repeat;");

        // Login box
        loginBox.setStyle("-fx-background-color: #F4E1B2; " +
                         "-fx-border-color: #623B1C; -fx-border-width: 4px; " +
                         "-fx-background-radius: 15px; -fx-border-radius: 15px; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 2, 2);");

        // Title
        title.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 18px; " +
                      "-fx-text-fill: #4B2C09; -fx-font-weight: bold;");

        // Input fields
        String inputStyle = "-fx-background-color: #FFF4DC; -fx-border-color: #A87B4D; " +
                           "-fx-border-width: 2px; -fx-text-fill: #3E2A1C; " +
                           "-fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; " +
                           "-fx-background-radius: 3px; -fx-border-radius: 3px; " +
                           "-fx-prompt-text-fill: #000000;";
        usernameField.setStyle(inputStyle);
        passwordField.setStyle(inputStyle);

        // Login button
        loginBtn.setStyle("-fx-background-color: #FFD45E; -fx-border-color: #6B3F14; " +
                         "-fx-border-width: 3px; -fx-text-fill: #4B2C09; " +
                         "-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; " +
                         "-fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");

        // Register button
        registerBtn.setStyle("-fx-background-color: #FF8A4C; -fx-border-color: #6B3F14; " +
                           "-fx-border-width: 3px; -fx-text-fill: #4B2C09; " +
                           "-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; " +
                           "-fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");

        // Slogan
        sloganLabel.setStyle("-fx-font-family: Georgia, serif; -fx-font-size: 13px; " +
                           "-fx-font-style: italic; -fx-text-fill: #5B3D1C; -fx-opacity: 0.85;");
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("❌ Please fill all fields");
            return;
        }
        
        // Connect if not connected
        if (!network.isConnected()) {
            if (!network.connect()) {
                statusLabel.setText("❌ Cannot connect to server!");
                return;
            }
        }
        
        statusLabel.setText("⏳ Logging in...");
        network.login(username, password);
    }
    
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("❌ Please fill all fields");
            return;
        }
        
        if (username.length() < 3) {
            statusLabel.setText("❌ Username must be at least 3 characters");
            return;
        }
        
        if (password.length() < 4) {
            statusLabel.setText("❌ Password must be at least 4 characters");
            return;
        }
        
        // Connect if not connected
        if (!network.isConnected()) {
            if (!network.connect()) {
                statusLabel.setText("❌ Cannot connect to server!");
                return;
            }
        }
        
        statusLabel.setText("⏳ Registering...");
        network.register(username, password);
    }
    
    // Message handlers
    
    public void handleLoginSuccess(Message message) {
        statusLabel.setText("✅ Login successful!");
        onLoginSuccess.run();
    }
    
    public void handleLoginFail(Message message) {
        statusLabel.setText("❌ " + message.getData());
    }
    
    public void handleRegisterSuccess(Message message) {
        statusLabel.setText("✅ " + message.getData());
        UIHelper.showInfo("Success", message.getData());
    }
    
    public void handleRegisterFail(Message message) {
        statusLabel.setText("❌ " + message.getData());
    }
}
