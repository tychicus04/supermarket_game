package client;

import controllers.ImprovedGameController;
import controllers.LeaderboardController;
import controllers.LobbyController;
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
    private String currentUsername; // Track logged in username

    // Controllers
    private LoginController loginController;
    private MenuController menuController;
    private LobbyController lobbyController;
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
            this::showLeaderboard,
            this::showLobby,
            this::handleLogout);
        lobbyController = new LobbyController(primaryStage,
            () -> showGameScreen(false),
            this::showMenuScreen);
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
                    // Extract username from message data (format: "Welcome username!")
                    String data = message.getData();
                    if (data.startsWith("Welcome ")) {
                        currentUsername = data.substring(8, data.length() - 1); // Remove "Welcome " and "!"
                    }
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
                    if (lobbyController != null) {
                        lobbyController.handleRoomUpdate(message);
                    }
                    break;
                case "JOIN_FAIL":
                    menuController.handleJoinFail(message);
                    break;
                case "S2C_ROOM_LIST":
                    if (lobbyController != null) {
                        lobbyController.handleRoomListUpdate(message);
                    }
                    break;
                case "S2C_JOIN_REQUEST":
                    if (lobbyController != null) {
                        lobbyController.handleJoinRequest(message);
                    }
                    break;
                case "S2C_JOIN_APPROVED":
                    if (lobbyController != null) {
                        lobbyController.handleJoinApproved(message);
                    }
                    break;
                case "S2C_JOIN_REJECTED":
                    if (lobbyController != null) {
                        lobbyController.handleJoinRejected(message);
                    }
                    break;
                case "S2C_INVITE_TO_ROOM":
                    if (lobbyController != null) {
                        lobbyController.handleInviteReceived(message);
                    }
                    break;
                case "S2C_SEARCH_RESULTS":
                    if (lobbyController != null) {
                        lobbyController.handleSearchResults(message);
                    }
                    break;
                case "S2C_FRIEND_REQUESTS":
                    if (lobbyController != null) {
                        lobbyController.handleFriendRequests(message);
                    }
                    break;
                case "S2C_FRIEND_LIST":
                    if (lobbyController != null) {
                        lobbyController.handleFriendList(message);
                    }
                    break;
                case "S2C_FRIEND_REQUEST_SENT":
                case "S2C_FRIEND_REQUEST_FAIL":
                case "S2C_FRIEND_ACCEPTED":
                case "S2C_FRIEND_REJECTED":
                case "S2C_FRIEND_REMOVED":
                case "S2C_INVITE_SENT":
                case "S2C_FRIEND_REQUEST_RECEIVED":
                    // Show notification
                    utils.UIHelper.showInfo("Friend", message.getData());
                    // Refresh friend list if in lobby
                    if (lobbyController != null) {
                        networkManager.sendMessage(new models.Message("C2S_GET_FRIENDS", ""));
                    }
                    break;
                case "S2C_ROOM_INVITE":
                    if (lobbyController != null) {
                        lobbyController.handleRoomInvite(message);
                    }
                    break;
                case "LOGOUT_SUCCESS":
                    handleLogoutSuccess(message);
                    break;
                case "S2C_FRIEND_STATUS_CHANGED":
                    // Friend went online/offline - refresh friend list if in lobby
                    if (lobbyController != null) {
                        networkManager.sendMessage(new models.Message("C2S_GET_FRIENDS", ""));
                    }
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
    
    private void handleLogout() {
        // Send logout message to server
        networkManager.sendMessage(new models.Message("LOGOUT", ""));

        // Clear current username
        currentUsername = null;

        // Show confirmation
        utils.UIHelper.showInfo("Logged Out", "You have been logged out successfully.");

        // Go back to login screen
        showLoginScreen();
    }

    private void handleLogoutSuccess(Message message) {
        // Logout confirmed by server
        System.out.println("📡 " + message.getData());
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
    
    private void showLobby() {
        // Show lobby in browse mode (not in a room)
        String username = currentUsername != null ? currentUsername : "Player";
        lobbyController.show(username, null, new java.util.ArrayList<>());
    }

    public static void main(String[] args) {
        launch(args);
    }
}