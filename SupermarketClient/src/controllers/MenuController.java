
// ===== MenuController.java =====
package controllers;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import models.Message;
import network.NetworkManager;
import utils.UIHelper;

import java.util.function.Consumer;

/**
 * Main menu controller
 */
public class MenuController {
    private Stage stage;
    private Consumer<Boolean> onStartGame;
    private Runnable onShowLeaderboard;
    private NetworkManager network;
    
    private String currentRoomId;
    private VBox waitingRoomLayout;
    private Label roomPlayerCount;
    
    public MenuController(Stage stage, Consumer<Boolean> onStartGame, Runnable onShowLeaderboard) {
        this.stage = stage;
        this.onStartGame = onStartGame;
        this.onShowLeaderboard = onShowLeaderboard;
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
        
        Text welcome = new Text("Welcome! 👋");
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        welcome.setFill(javafx.scene.paint.Color.WHITE);
        
        Button singleBtn = UIHelper.createButton("🎮 SINGLE PLAYER", UIHelper.DANGER_COLOR);
        singleBtn.setOnAction(e -> onStartGame.accept(true));
        
        Button multiBtn = UIHelper.createButton("👥 MULTIPLAYER", UIHelper.INFO_COLOR);
        multiBtn.setOnAction(e -> showMultiplayerOptions());
        
        Button leaderboardBtn = UIHelper.createButton("🏆 LEADERBOARD", UIHelper.WARNING_COLOR);
        leaderboardBtn.setOnAction(e -> {
            network.getLeaderboard();
            onShowLeaderboard.run();
        });
        
        Button exitBtn = UIHelper.createButton("🚪 EXIT", "#95a5a6");
        exitBtn.setOnAction(e -> javafx.application.Platform.exit());
        
        root.getChildren().addAll(welcome, singleBtn, multiBtn, leaderboardBtn, exitBtn);
        
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
    
    private void showMultiplayerOptions() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createSolidBackground("#ecf0f1"));
        
        Text title = UIHelper.createTitle("👥 Multiplayer Mode");
        
        Button createBtn = UIHelper.createButton("CREATE ROOM", UIHelper.PRIMARY_COLOR);
        createBtn.setOnAction(e -> network.createRoom());
        
        HBox joinBox = new HBox(10);
        joinBox.setAlignment(Pos.CENTER);
        
        TextField roomField = UIHelper.createTextField("Room ID", 200);
        Button joinBtn = UIHelper.createSmallButton("JOIN", UIHelper.SECONDARY_COLOR);
        joinBtn.setOnAction(e -> {
            if (!roomField.getText().isEmpty()) {
                network.joinRoom(roomField.getText().trim());
            }
        });
        
        joinBox.getChildren().addAll(roomField, joinBtn);
        
        Button backBtn = UIHelper.createButton("← BACK", "#95a5a6");
        backBtn.setOnAction(e -> showMainMenu());
        
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
        
        Button startBtn = UIHelper.createButton("START GAME", UIHelper.PRIMARY_COLOR);
        startBtn.setDisable(playerCount < 2);
        startBtn.setOnAction(e -> network.startGame(currentRoomId));
        
        Button leaveBtn = UIHelper.createButton("LEAVE ROOM", UIHelper.DANGER_COLOR);
        leaveBtn.setOnAction(e -> {
            network.leaveRoom(currentRoomId);
            showMainMenu();
        });
        
        root.getChildren().addAll(title, roomPlayerCount, waitText, startBtn, leaveBtn);
        
        waitingRoomLayout = root;
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
    
    // Message handlers
    
    public void handleRoomUpdate(Message message) {
        String[] data = message.getData().split(":");
        
        switch (message.getType()) {
            case "ROOM_CREATED":
            case "ROOM_JOINED":
                if (data.length >= 2) {
                    showWaitingRoom(data[0], Integer.parseInt(data[1]));
                }
                break;
                
            case "PLAYER_JOINED":
            case "PLAYER_LEFT":
                if (data.length >= 2 && roomPlayerCount != null) {
                    int count = Integer.parseInt(data[1]);
                    roomPlayerCount.setText("Players: " + count + "/4");
                    
                    // Enable/disable start button
                    if (waitingRoomLayout != null) {
                        for (javafx.scene.Node node : waitingRoomLayout.getChildren()) {
                            if (node instanceof Button) {
                                Button btn = (Button) node;
                                if (btn.getText().contains("START")) {
                                    btn.setDisable(count < 2);
                                }
                            }
                        }
                    }
                }
                break;
        }
    }
    
    public void handleJoinFail(Message message) {
        UIHelper.showError("Error", message.getData());
    }
}