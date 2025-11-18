package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import models.Message;
import network.NetworkManager;
import utils.JsonParser;
import utils.RoomUpdateHandler;
import utils.RoomUpdateHandler.*;
import utils.SoundManager;
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
    private Runnable onShowMatchHistory;
    private Runnable onShowLobby;
    private Runnable onLogout;
    private SoundManager soundManager;
    private NetworkManager network;

    private String currentRoomId;
    private Label roomPlayerCount;

    private Button startGameButton;

    public MenuController(Stage stage, Consumer<Boolean> onStartGame,
                          Runnable onShowLeaderboard, Runnable onShowMatchHistory,
                          Runnable onShowLobby, Runnable onLogout) {
        this.stage = stage;
        this.onStartGame = onStartGame;
        this.onShowLeaderboard = onShowLeaderboard;
        this.onShowMatchHistory = onShowMatchHistory;
        this.onShowLobby = onShowLobby;
        this.soundManager = SoundManager.getInstance();
        this.onLogout = onLogout;
        this.network = NetworkManager.getInstance();
    }

    public void show() {
        soundManager.playMenuMusic();
        showMainMenu();
    }

    private void showMainMenu() {
        // Root với background image
        StackPane root = new StackPane();
        root.getStyleClass().add("menu-root");

        // Menu container - positioned center-right
        VBox menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.getStyleClass().add("menu-container");

        // Position slightly to the right
        StackPane.setAlignment(menuContainer, Pos.CENTER_RIGHT);
        StackPane.setMargin(menuContainer, new Insets(0, 80, 0, 0));

        // Welcome text - pixel style với màu vàng và viền đỏ giống ảnh
        Text welcome = new Text("WELCOME");
        welcome.setStyle("-fx-font-family: 'Arial Black', 'Impact', sans-serif; " +
                        "-fx-font-size: 64px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-fill: linear-gradient(to bottom, #FFE066 0%, #FFCC00 100%); " +
                        "-fx-stroke: #8B4513; " +
                        "-fx-stroke-width: 4px;");

        // Tạo BUTTON Single Player theo cùng class với các nút khác để đồng nhất style
        Button singleBtn = new Button("PRACTICE");
        singleBtn.getStyleClass().addAll("pixel-menu-btn", "btn-single");
        singleBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            handleSinglePlayerClick();
        });

        // Multiplayer button - purple
        Button multiBtn = new Button("PLAY ONLINE");
        multiBtn.getStyleClass().addAll("pixel-menu-btn", "btn-multi");
        multiBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            if (onShowLobby != null) {
                onShowLobby.run();
            } else {
                showMultiplayerOptions();
            }
        });

        // Leaderboard button - golden yellow
        Button leaderboardBtn = new Button("LEADERBOARD");
        leaderboardBtn.getStyleClass().addAll("pixel-menu-btn", "btn-leaderboard");
        leaderboardBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            network.getLeaderboard();
            onShowLeaderboard.run();
        });

        // Match History button - cyan
        Button matchHistoryBtn = new Button("MATCH HISTORY");
        matchHistoryBtn.getStyleClass().addAll("pixel-menu-btn", "btn-history");
        matchHistoryBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            if (onShowMatchHistory != null) {
                onShowMatchHistory.run();
            }
        });

        // Settings button - blue
        Button settingsBtn = new Button("SETTINGS");
        settingsBtn.getStyleClass().addAll("pixel-menu-btn", "btn-settings");
        settingsBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            showSettings();
        });

        // Logout button - orange
        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.getStyleClass().addAll("pixel-menu-btn", "btn-logout");
        logoutBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            if (onLogout != null) {
                onLogout.run();
            }
        });

        menuContainer.getChildren().addAll(welcome, singleBtn, multiBtn, leaderboardBtn, matchHistoryBtn, settingsBtn, logoutBtn);
        root.getChildren().add(menuContainer);

        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;

        Scene scene = new Scene(root, width, height);

        try {
            java.net.URL cssResource = getClass().getResource("/resources/assets/css/menu-pixel-style.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Menu CSS not found, using fallback");
                applyFallbackMenuStyles(root, singleBtn, multiBtn, leaderboardBtn, matchHistoryBtn, settingsBtn, logoutBtn);
            }
        } catch (Exception e) {
            System.err.println("Failed to load menu CSS: " + e.getMessage());
            applyFallbackMenuStyles(root, singleBtn, multiBtn, leaderboardBtn, matchHistoryBtn, settingsBtn, logoutBtn);
        }

        stage.setScene(scene);

        animateButtonFadeIn(singleBtn, 0.0);
        animateButtonFadeIn(multiBtn, 0.1);
        animateButtonFadeIn(leaderboardBtn, 0.2);
        animateButtonFadeIn(matchHistoryBtn, 0.3);
        animateButtonFadeIn(settingsBtn, 0.4);
        animateButtonFadeIn(logoutBtn, 0.5);
    }

    /**
     * Animate button fade-in from bottom
     */
    private void animateButtonFadeIn(Button button, double delaySeconds) {
        button.setOpacity(0);
        button.setTranslateY(30);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), button);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.seconds(delaySeconds));

        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.5), button);
        translate.setFromY(30);
        translate.setToY(0);
        translate.setDelay(Duration.seconds(delaySeconds));

        ParallelTransition parallel = new ParallelTransition(fade, translate);

        // Reset transform after animation to prevent hover conflicts
        parallel.setOnFinished(e -> {
            button.setOpacity(1.0);
            button.setTranslateY(0);
        });

        parallel.play();
    }

    /**
     * Fallback styles if CSS cannot be loaded
     */
    private void applyFallbackMenuStyles(StackPane root,
                                         Button singleBtn,
                                         Button multiBtn,
                                         Button leaderboardBtn,
                                         Button matchHistoryBtn,
                                         Button settingsBtn, Button logoutBtn) {
        // Background
        root.setStyle("-fx-background-image: url('/resources/assets/images/backgrounds/backgroundMenu.png'); " +
                     "-fx-background-size: cover; -fx-background-position: center center; " +
                     "-fx-background-repeat: no-repeat;");

        // Button base style - lớn hơn, chữ màu đen
        String baseStyle = "-fx-min-width: 380px; -fx-pref-width: 400px; -fx-pref-height: 75px; " +
                          "-fx-font-family: 'Arial Black', 'Impact', sans-serif; -fx-font-size: 22px; " +
                          "-fx-font-weight: bold; -fx-text-fill: black; " +
                          "-fx-border-color: #3E2A1C; -fx-border-width: 3px; " +
                          "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
                          "-fx-cursor: hand;";

        // Single player - greenish (matches other style)
        if (singleBtn != null) {
            singleBtn.setStyle(baseStyle + "-fx-background-color: #6CCF6C;");
        }

        // Multiplayer - purple
        multiBtn.setStyle(baseStyle + "-fx-background-color: #A36FD1;");

        // Leaderboard - golden
        leaderboardBtn.setStyle(baseStyle + "-fx-background-color: #FFB347;");

        // Match History - cyan
        matchHistoryBtn.setStyle(baseStyle + "-fx-background-color: #5DADE2;");

        // Settings - blue
        settingsBtn.setStyle(baseStyle + "-fx-background-color: #4DA6FF;");

        // Logout - orange
        logoutBtn.setStyle(baseStyle + "-fx-background-color: #E78640;");
    }

    private void showMultiplayerOptions() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createSolidBackground("#ecf0f1"));


        Text title = UIHelper.createTitle("Multiplayer Mode");

        Button createBtn = UIHelper.createButton("CREATE ROOM", UIHelper.PRIMARY_COLOR);
        createBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            handleCreateRoomClick();
        });

        HBox joinBox = new HBox(10);
        joinBox.setAlignment(Pos.CENTER);

        TextField roomField = UIHelper.createTextField("Room ID", 200);
        Button joinBtn = UIHelper.createSmallButton("JOIN", UIHelper.SECONDARY_COLOR);

        joinBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            handleJoinRoomClick(roomField.getText());
        });

        joinBox.getChildren().addAll(roomField, joinBtn);

        Button backBtn = UIHelper.createButton("BACK", "#95a5a6");
        backBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            handleBackToMainMenuClick();
        });

        root.getChildren().addAll(title, createBtn, joinBox, backBtn);

        // Lấy kích thước hiện tại của stage để giữ nguyên kích thước/fullscreen
        double width = stage.getWidth() > 0 ? stage.getWidth() : 600;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 500;

        Scene scene = new Scene(root, width, height);
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
        startGameButton.setOnAction(e -> {
            soundManager.play("menu_button");
            handleStartGameClick();
        });

        Button leaveBtn = UIHelper.createButton("LEAVE ROOM", UIHelper.DANGER_COLOR);
        leaveBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            handleLeaveRoomClick();
        });

        root.getChildren().addAll(title, roomPlayerCount, waitText, startGameButton, leaveBtn);

        double width = stage.getWidth() > 0 ? stage.getWidth() : 600;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 500;
        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
    }

    public void handleRoomUpdate(Message message) {
        String data = message.getData();

        switch (message.getType()) {
            case MESSAGE_TYPE_ROOM_CREATED:
            case MESSAGE_TYPE_ROOM_JOINED:
                // Format: "roomId:playerCount"
                String[] parts = data.split(":", 2);
                if (parts.length >= 2) {
                    showWaitingRoom(parts[0], Integer.parseInt(parts[1]));
                }
                break;
                
            case MESSAGE_TYPE_PLAYER_JOINED:
            case MESSAGE_TYPE_PLAYER_LEFT:
                // Use RoomUpdateHandler for consistent parsing
                PlayerChangeEvent event = RoomUpdateHandler.parsePlayerChange(data,
                    message.getType().equals(MESSAGE_TYPE_PLAYER_JOINED));

                if (event != null && roomPlayerCount != null) {
                    int count = event.getPlayerCount();
                    if (count >= 0) {
                        roomPlayerCount.setText("Players: " + count + "/4");

                        // Enable/disable start button
                        if (startGameButton != null) {
                            startGameButton.setDisable(count < 2);
                        }
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
        stage.setFullScreen(false);
        stage.setWidth(1024);
        stage.setHeight(1024);
        stage.centerOnScreen();
        onStartGame.accept(true);
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

    /**
     * Show settings screen
     */
    private void showSettings() {
        SettingsController settingsController = new SettingsController(stage, this::showMainMenu);
        settingsController.show();
    }
}

