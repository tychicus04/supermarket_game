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

import java.util.ArrayList;
import java.util.List;

import static constants.GameConstants.*;

/**
 * Main client application
 */
public class Main extends Application {
    private NetworkManager networkManager;
    private Stage primaryStage;
    private String currentUsername;

    private LoginController loginController;
    private MenuController menuController;
    private LobbyController lobbyController;
    private ImprovedGameController gameController;
    private LeaderboardController leaderboardController;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("🏪 Supermarket Game");
        
        // Initialize assets
        utils.AssetManager.getInstance();
        
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
                case MESSAGE_TYPE_LOGIN_SUCCESS:
                    // Extract username from message data (format: "Welcome username!")
                    String data = message.getData().toString();
                    if (data.startsWith("Welcome ")) {
                        currentUsername = data.substring(8, data.length() - 1); // Remove "Welcome " and "!"
                    }
                    loginController.handleLoginSuccess();
                    break;
                case MESSAGE_TYPE_LOGIN_FAIL:
                    loginController.handleLoginFail(message);
                    break;
                case MESSAGE_TYPE_REGISTER_SUCCESS:
                    loginController.handleRegisterSuccess(message);
                    break;
                case MESSAGE_TYPE_REGISTER_FAIL:
                    loginController.handleRegisterFail(message);
                    break;
                case MESSAGE_TYPE_ROOM_CREATED:
                case MESSAGE_TYPE_ROOM_JOINED:
                    // Parse room data: "roomId:playerCount"
                    String roomData = message.getData().toString();
                    String[] parts = roomData.split(":");
                    if (parts.length >= 2) {
                        String roomId = parts[0];
                        // Get list of players (for now just current user)
                        List<String> players = new ArrayList<>();
                        players.add(currentUsername);

                        // Show lobby controller in room mode
                        if (lobbyController != null) {
                            System.out.println("🎮 Showing lobby for room: " + roomId);
                            lobbyController.show(currentUsername, roomId, players);
                        }
                    }
                    break;
                case MESSAGE_TYPE_PLAYER_JOINED:
                case MESSAGE_TYPE_PLAYER_LEFT:
                    // Update lobby controller if active
                    if (lobbyController != null) {
                        lobbyController.handleRoomUpdate(message);
                    }
                    break;
                case MESSAGE_TYPE_JOIN_FAIL:
                    menuController.handleJoinFail(message);
                    break;
                case MESSAGE_TYPE_S2C_ROOM_LIST:
                    if (lobbyController != null) {
                        lobbyController.handleRoomListUpdate(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_JOIN_REQUEST:
                    if (lobbyController != null) {
                        lobbyController.handleJoinRequest(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_JOIN_APPROVED:
                    if (lobbyController != null) {
                        lobbyController.handleJoinApproved(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_JOIN_REJECTED:
                    if (lobbyController != null) {
                        lobbyController.handleJoinRejected(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_INVITE_TO_ROOM:
                    if (lobbyController != null) {
                        lobbyController.handleInviteReceived(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_SEARCH_RESULTS:
                    if (lobbyController != null) {
                        lobbyController.handleSearchResults(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_FRIEND_REQUESTS:
                    if (lobbyController != null) {
                        lobbyController.handleFriendRequests(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_FRIEND_LIST:
                    if (lobbyController != null) {
                        lobbyController.handleFriendList(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_FRIEND_REQUEST_SENT:
                case MESSAGE_TYPE_S2C_FRIEND_REQUEST_FAIL:
                case MESSAGE_TYPE_S2C_FRIEND_ACCEPTED:
                case MESSAGE_TYPE_S2C_FRIEND_REJECTED:
                case MESSAGE_TYPE_S2C_FRIEND_REMOVED:
                case MESSAGE_TYPE_S2C_INVITE_SENT:
                case MESSAGE_TYPE_S2C_FRIEND_REQUEST_RECEIVED:
                    // Show notification
                    utils.UIHelper.showInfo("Friend", message.getData().toString());
                    // Refresh friend list if in lobby
                    if (lobbyController != null) {
                        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_FRIENDS, ""));
                    }
                    break;
                case MESSAGE_TYPE_S2C_ROOM_INVITE:
                    if (lobbyController != null) {
                        lobbyController.handleRoomInvite(message);
                    }
                    break;
                case MESSAGE_TYPE_LOGOUT_SUCCESS:
                    handleLogoutSuccess(message);
                    break;
                case MESSAGE_TYPE_S2C_FRIEND_STATUS_CHANGED:
                    if (lobbyController != null) {
                        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_FRIENDS, ""));
                    }
                    break;
                case MESSAGE_TYPE_GAME_START:
                    // Server has started the game, switch to game screen
                    Platform.runLater(() -> showGameScreen(false));
                    break;
                case MESSAGE_TYPE_SCORE_UPDATE:
                    gameController.handleScoreUpdate(message);
                    break;
                case MESSAGE_TYPE_LEADERBOARD:
                    leaderboardController.handleLeaderboard(message);
                    break;
                case MESSAGE_TYPE_ERROR:
                    handleError(message);
                    break;
            }
        });
    }
    
    private void handleError(Message message) {
        utils.UIHelper.showError("Error", message.getData());
    }
    
    private void handleLogout() {
        // Stop any playing music
        utils.SoundManager.getInstance().stopMusic();

        // Send logout message to server
        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_LOGOUT, ""));

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
        System.out.println("🏠 showLobby() called");
        System.out.println("   currentUsername: " + currentUsername);
        System.out.println("   lobbyController is null: " + (lobbyController == null));

        // Show lobby in browse mode (not in a room)
        String username = currentUsername != null ? currentUsername : "Player";
        lobbyController.show(username, null, new java.util.ArrayList<>());
    }

    public static void main(String[] args) {
        launch(args);
    }
}