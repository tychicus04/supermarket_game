package client;

import controllers.ImprovedGameController;
import controllers.LeaderboardController;
import controllers.LoginController;
import controllers.MenuController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import models.Message;
import network.NetworkManager;

/**
 * Main client application
 */
public class Main extends Application {
    private NetworkManager networkManager;
    private Stage primaryStage;
    
    // Controllers
    private LoginController loginController;
    private MenuController menuController;
    private ImprovedGameController gameController;
    private LeaderboardController leaderboardController;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("🏪 Supermarket Game");
        
        // Initialize network
        networkManager = NetworkManager.getInstance();
        networkManager.setMessageHandler(this::handleServerMessage);
        
        // Initialize controllers
        initializeControllers();
        
        // Show login screen
        showLoginScreen();
        
        primaryStage.setOnCloseRequest(e -> {
            networkManager.disconnect();
            Platform.exit();
        });
        
        primaryStage.show();
    }
    
    /**
     * Initialize all controllers
     */
    private void initializeControllers() {
        loginController = new LoginController(primaryStage, this::showMenuScreen);
        menuController = new MenuController(primaryStage, 
            this::showGameScreen, 
            this::showLeaderboard);
        gameController = new ImprovedGameController(primaryStage, this::showMenuScreen);
        leaderboardController = new LeaderboardController(primaryStage, this::showMenuScreen);
    }
    
    /**
     * Handle messages from server
     */
    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            System.out.println("📩 Received: " + message.getType());
            
            switch (message.getType()) {
                case "LOGIN_SUCCESS":
                    loginController.handleLoginSuccess(message);
                    break;
                case "LOGIN_FAIL":
                    loginController.handleLoginFail(message);
                    break;
                case "REGISTER_SUCCESS":
                    loginController.handleRegisterSuccess(message);
                    break;
                case "REGISTER_FAIL":
                    loginController.handleRegisterFail(message);
                    break;
                case "ROOM_CREATED":
                case "ROOM_JOINED":
                case "PLAYER_JOINED":
                case "PLAYER_LEFT":
                    menuController.handleRoomUpdate(message);
                    break;
                case "JOIN_FAIL":
                    menuController.handleJoinFail(message);
                    break;
                case "GAME_START":
                    gameController.handleGameStart(message);
                    break;
                case "SCORE_UPDATE":
                    gameController.handleScoreUpdate(message);
                    break;
                case "LEADERBOARD":
                    leaderboardController.handleLeaderboard(message);
                    break;
                case "ERROR":
                    handleError(message);
                    break;
            }
        });
    }
    
    private void handleError(Message message) {
        utils.UIHelper.showError("Error", message.getData());
    }
    
    // Screen navigation
    
    private void showLoginScreen() {
        loginController.show();
    }
    
    private void showMenuScreen() {
        menuController.show();
    }
    
    private void showGameScreen(boolean isSinglePlayer) {
        gameController.show(isSinglePlayer);
    }
    
    private void showLeaderboard() {
        leaderboardController.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}