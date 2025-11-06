
// ===== MenuController.java =====
package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import models.Message;
import network.NetworkManager;
import utils.UIHelper;

import java.util.function.Consumer;

import static constants.GameConstants.*;

/**
 * Main menu controller
 */
public class MenuController {
    private Stage stage;
    private Consumer<Boolean> onStartGame;
    private Runnable onShowLeaderboard;
    private Runnable onShowLobby;
    private Runnable onLogout;
    private NetworkManager network;
    
    private String currentRoomId;
    private Label roomPlayerCount;
    private Button startGameButton;

    public MenuController(Stage stage, Consumer<Boolean> onStartGame,
                         Runnable onShowLeaderboard, Runnable onShowLobby, Runnable onLogout) {
        this.stage = stage;
        this.onStartGame = onStartGame;
        this.onShowLeaderboard = onShowLeaderboard;
        this.onShowLobby = onShowLobby;
        this.onLogout = onLogout;
        this.network = NetworkManager.getInstance();
    }
    
    public void show() {
        showMainMenu();
    }
    
    private void showMainMenu() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createGradientBackground("#74ebd5", "#ACB6E5"));
        
        Text welcome = new Text("Welcome!");
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        welcome.setFill(javafx.scene.paint.Color.WHITE);
        
        Button singleBtn = UIHelper.createButton("SINGLE PLAYER", UIHelper.DANGER_COLOR);
        singleBtn.setOnAction(e -> handleSinglePlayerClick());

        Button multiBtn = UIHelper.createButton("MULTIPLAYER", UIHelper.INFO_COLOR);
        multiBtn.setOnAction(e -> handleMultiplayerClick());

        Button leaderboardBtn = UIHelper.createButton("LEADERBOARD", UIHelper.WARNING_COLOR);
        leaderboardBtn.setOnAction(e -> handleLeaderboardClick());

        Button logoutBtn = UIHelper.createButton("LOGOUT", "#e67e22");
        logoutBtn.setOnAction(e -> handleLogoutClick());

        root.getChildren().addAll(welcome, singleBtn, multiBtn, leaderboardBtn, logoutBtn);

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
    
    private void showMultiplayerOptions() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createSolidBackground("#ecf0f1"));
        
        Text title = UIHelper.createTitle("Multiplayer Mode");
        
        Button createBtn = UIHelper.createButton("CREATE ROOM", UIHelper.PRIMARY_COLOR);
        createBtn.setOnAction(e -> handleCreateRoomClick());

        HBox joinBox = new HBox(10);
        joinBox.setAlignment(Pos.CENTER);
        
        TextField roomField = UIHelper.createTextField("Room ID", 200);
        Button joinBtn = UIHelper.createSmallButton("JOIN", UIHelper.SECONDARY_COLOR);
        joinBtn.setOnAction(e -> handleJoinRoomClick(roomField.getText()));

        joinBox.getChildren().addAll(roomField, joinBtn);
        
        Button backBtn = UIHelper.createButton("BACK", "#95a5a6");
        backBtn.setOnAction(e -> handleBackToMainMenuClick());

        root.getChildren().addAll(title, createBtn, joinBox, backBtn);
        
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
    
    private void showWaitingRoom(String roomId, int playerCount) {
        this.currentRoomId = roomId;
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createSolidBackground("#2c3e50"));
        
        Text title = new Text("Room: " + roomId);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(javafx.scene.paint.Color.WHITE);
        
        roomPlayerCount = UIHelper.createLabel("Players: " + playerCount + "/4", 20, 
            javafx.scene.paint.Color.web("#ecf0f1"));
        
        Text waitText = new Text("Waiting for more players...");
        waitText.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        waitText.setFill(javafx.scene.paint.Color.web("#95a5a6"));
        
        startGameButton = UIHelper.createButton("START GAME", UIHelper.PRIMARY_COLOR);
        startGameButton.setDisable(playerCount < 2);
        startGameButton.setOnAction(e -> handleStartGameClick());

        Button leaveBtn = UIHelper.createButton("LEAVE ROOM", UIHelper.DANGER_COLOR);
        leaveBtn.setOnAction(e -> handleLeaveRoomClick());

        root.getChildren().addAll(title, roomPlayerCount, waitText, startGameButton, leaveBtn);

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }

    public void handleRoomUpdate(Message message) {
        String[] data = message.getData().toString().split(":");
        
        switch (message.getType()) {
            case MESSAGE_TYPE_ROOM_CREATED:
            case MESSAGE_TYPE_ROOM_JOINED:
                if (data.length >= 2) {
                    showWaitingRoom(data[0], Integer.parseInt(data[1]));
                }
                break;
                
            case MESSAGE_TYPE_PLAYER_JOINED:
            case MESSAGE_TYPE_PLAYER_LEFT:
                if (data.length >= 2 && roomPlayerCount != null) {
                    int count = Integer.parseInt(data[1]);
                    roomPlayerCount.setText("Players: " + count + "/4");
                    
                    // Enable/disable start button
                    if (startGameButton != null) {
                        startGameButton.setDisable(count < 2);
                    }
                }
                break;
        }
    }
    
    public void handleJoinFail(Message message) {
        UIHelper.showError("Error", message.getData().toString());
    }

    /**
     * Handle single player button click
     */
    private void handleSinglePlayerClick() {
        onStartGame.accept(true);
    }

    /**
     * Handle multiplayer button click
     */
    private void handleMultiplayerClick() {
        if (onShowLobby != null) {
            onShowLobby.run();
        } else {
            showMultiplayerOptions();
        }
    }

    /**
     * Handle leaderboard button click
     */
    private void handleLeaderboardClick() {
        network.getLeaderboard();
        onShowLeaderboard.run();
    }

    /**
     * Handle logout button click
     */
    private void handleLogoutClick() {
        if (onLogout != null) {
            onLogout.run();
        }
    }

    /**
     * Handle back button click from multiplayer options
     */
    private void handleBackToMainMenuClick() {
        showMainMenu();
    }

    /**
     * Handle create room button click
     */
    private void handleCreateRoomClick() {
        network.createRoom();
    }

    /**
     * Handle join room button click
     */
    private void handleJoinRoomClick(String roomId) {
        if (roomId != null && !roomId.trim().isEmpty()) {
            network.joinRoom(roomId.trim());
        } else {
            UIHelper.showError("Error", "Please enter a valid Room ID");
        }
    }

    /**
     * Handle start game button click
     */
    private void handleStartGameClick() {
        if (currentRoomId != null) {
            network.startGame(currentRoomId);
        }
    }

    /**
     * Handle leave room button click
     */
    private void handleLeaveRoomClick() {
        if (currentRoomId != null) {
            network.leaveRoom(currentRoomId);
        }
        showMainMenu();
    }
}