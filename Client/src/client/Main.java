package client;

import controllers.ImprovedGameController;
import controllers.LeaderboardController;
import controllers.LobbyController;
import controllers.LoginController;
import controllers.MatchHistoryController;
import controllers.MenuController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import models.Message;
import network.NetworkManager;

import java.util.List;

import static constants.GameConstants.*;

/**
 * Main client application
 */
public class Main extends Application {
    private NetworkManager networkManager;
    private Stage primaryStage;
    private String currentUsername;
    private String currentRoomId;
    private boolean gameIsOver = false;

    private LoginController loginController;
    private MenuController menuController;
    private LobbyController lobbyController;
    private ImprovedGameController gameController;
    private LeaderboardController leaderboardController;
    private MatchHistoryController matchHistoryController;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Supermarket Game");
        utils.AssetManager.getInstance();
        networkManager = NetworkManager.getInstance();
        networkManager.setMessageHandler(this::handleServerMessage);
        initializeControllers();
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
            this::showMatchHistory,
            this::showLobby,
            this::handleLogout);
        lobbyController = new LobbyController(primaryStage,
            () -> showGameScreen(false),
            this::showMenuScreen);
        gameController = new ImprovedGameController(
                primaryStage,
                this::showMenuScreen,
                () -> {
                    gameIsOver = false;

                    if (this.currentRoomId != null) {
                        lobbyController.showCurrentRoom();
                    } else {
                        utils.UIHelper.showError("Room Not Found", "The room was closed by the host.");
                        showLobby();
                    }
                },
                networkManager::sendMessage);
        leaderboardController = new LeaderboardController(primaryStage, this::showMenuScreen);
        matchHistoryController = new MatchHistoryController(primaryStage, this::showMenuScreen);
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
                    String createdRoomData = message.getData();
                    String[] createdParts = createdRoomData.split(":");
                    this.currentRoomId = createdParts[0];
                    List<String> creatorList = new java.util.ArrayList<>();
                    creatorList.add(currentUsername);
                    lobbyController.show(currentUsername, this.currentRoomId, creatorList);
                    break;

                case MESSAGE_TYPE_ROOM_JOINED:
                    String joinedRoomData = message.getData().toString();
                    String[] joinedParts = joinedRoomData.split(":");
                    this.currentRoomId = joinedParts[0];
                    lobbyController.show(currentUsername, this.currentRoomId, new java.util.ArrayList<>());
                    break;

                case MESSAGE_TYPE_PLAYER_JOINED:
                case MESSAGE_TYPE_PLAYER_LEFT:
                case MESSAGE_TYPE_S2C_ROOM_UPDATE:
                    menuController.handleRoomUpdate(message);
                    if (lobbyController != null) {
                        lobbyController.handleRoomUpdate(message);
                    }
                    break;
                case MESSAGE_TYPE_ROOM_DELETED:
                    if (gameIsOver) {
                        this.currentRoomId = null;
                        System.out.println("Room deleted, but game over screen is active.");
                    } else {
                        this.currentRoomId = null;
                        utils.UIHelper.showError("Room Closed", message.getData().toString());
                        showLobby();
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
                case MESSAGE_TYPE_S2C_INVITE_SENT:
                    break;
                case MESSAGE_TYPE_S2C_FRIEND_REQUEST_RECEIVED:
                    if (lobbyController != null) {
                        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));
                    }
                    break;
                case MESSAGE_TYPE_S2C_FRIEND_ACCEPTED:
                case MESSAGE_TYPE_S2C_FRIEND_REJECTED:
                case MESSAGE_TYPE_S2C_FRIEND_REMOVED:
                    if (lobbyController != null) {
                        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_FRIENDS, ""));
                        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));
                    }
                    break;
                case MESSAGE_TYPE_S2C_ROOM_INVITE:
                    if (lobbyController != null) {
                        lobbyController.handleRoomInvite(message);
                    }
                    break;
                case MESSAGE_TYPE_S2C_KICKED_FROM_ROOM:
                    this.currentRoomId = null;
                    utils.UIHelper.showError("Removed from Room", message.getData());
                    showLobby();
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
                    gameIsOver = false;
                    Platform.runLater(() -> showGameScreen(false));
                    break;
                case MESSAGE_TYPE_S2C_GAME_STATE:
                    gameController.handleGameState(message);
                    break;
                case MESSAGE_TYPE_S2C_GAME_OVER:
                    gameIsOver = true;
                    gameController.handleGameOver(message);
                    break;
                case MESSAGE_TYPE_LEADERBOARD:
                    leaderboardController.handleLeaderboard(message);
                    break;
                case MESSAGE_TYPE_S2C_MATCH_HISTORY:
                    matchHistoryController.handleMatchHistory(message);
                    break;
                case MESSAGE_TYPE_S2C_MATCH_STATS:
                    matchHistoryController.handleMatchStats(message);
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
        utils.SoundManager.getInstance().stopMusic();
        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_LOGOUT, ""));
        currentUsername = null;
        utils.UIHelper.showInfo("Logged Out", "You have been logged out successfully.");
        showLoginScreen();
    }

    private void handleLogoutSuccess(Message message) {
        System.out.println(message.getData());
    }

    private void showLoginScreen() {
        loginController.show();
    }
    
    private void showMenuScreen() {
        menuController.show();
    }
    
    private void showGameScreen(boolean isSinglePlayer) {
        gameController.show(isSinglePlayer, currentUsername, currentRoomId);
    }
    
    private void showLeaderboard() {
        leaderboardController.show();
    }

    private void showMatchHistory() {
        matchHistoryController.show();
        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_MATCH_HISTORY, ""));
        networkManager.sendMessage(new models.Message(MESSAGE_TYPE_GET_MATCH_STATS, ""));
    }
    
    private void showLobby() {
        String username = currentUsername != null ? currentUsername : "Player";
        lobbyController.show(username, null, new java.util.ArrayList<>());
    }

    public static void main(String[] args) {
        launch(args);
    }
}