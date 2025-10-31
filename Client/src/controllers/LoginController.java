package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Message;
import network.NetworkManager;
import utils.UIHelper;

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
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createSolidBackground("#f0f0f0"));
        
        // Title
        Text title = UIHelper.createTitle("🏪 SUPERMARKET GAME");
        
        // Input fields
        usernameField = UIHelper.createTextField("Username", 300);
        passwordField = UIHelper.createPasswordField("Password", 300);
        
        // Status label
        statusLabel = new Label();
        statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button loginBtn = UIHelper.createSmallButton("LOGIN", UIHelper.PRIMARY_COLOR);
        Button registerBtn = UIHelper.createSmallButton("REGISTER", UIHelper.SECONDARY_COLOR);
        
        buttonBox.getChildren().addAll(loginBtn, registerBtn);
        
        // Events
        loginBtn.setOnAction(e -> handleLogin());
        registerBtn.setOnAction(e -> handleRegister());
        
        // Enter key to login
        passwordField.setOnAction(e -> handleLogin());
        
        root.getChildren().addAll(title, usernameField, passwordField, buttonBox, statusLabel);
        
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
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
