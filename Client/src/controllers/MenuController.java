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
import utils.UIHelper;

import java.util.function.Consumer;

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
    private VBox waitingRoomLayout;
    private Label roomPlayerCount;

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

        // Welcome text - pixel style
//        Text welcome = new Text("Welcome 👋");
//        welcome.getStyleClass().add("menu-welcome");

        // Multiplayer button - purple
        Button multiBtn = new Button("👥 MULTIPLAYER");
        multiBtn.getStyleClass().addAll("pixel-menu-btn", "btn-multi");
        multiBtn.setOnAction(e -> {
            if (onShowLobby != null) {
                onShowLobby.run();
            } else {
                showMultiplayerOptions();
            }
        });

        // Leaderboard button - golden yellow
        Button leaderboardBtn = new Button("🏆 LEADERBOARD");
        leaderboardBtn.getStyleClass().addAll("pixel-menu-btn", "btn-leaderboard");
        leaderboardBtn.setOnAction(e -> {
            network.getLeaderboard();
            onShowLeaderboard.run();
        });

        // Logout button - orange
        Button logoutBtn = new Button("🚪 LOGOUT");
        logoutBtn.getStyleClass().addAll("pixel-menu-btn", "btn-logout");
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        menuContainer.getChildren().addAll(multiBtn, leaderboardBtn, logoutBtn);
        root.getChildren().add(menuContainer);

        // Lấy kích thước hiện tại của stage để giữ nguyên kích thước/fullscreen
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;

        Scene scene = new Scene(root, width, height);

        // Load CSS
        try {
            java.net.URL cssResource = getClass().getResource("/resources/assets/css/menu-pixel-style.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Menu CSS not found, using fallback");
                applyFallbackMenuStyles(root,multiBtn, leaderboardBtn, logoutBtn);
            }
        } catch (Exception e) {
            System.err.println("Failed to load menu CSS: " + e.getMessage());
            applyFallbackMenuStyles(root, multiBtn, leaderboardBtn, logoutBtn);
        }

        stage.setScene(scene);

        // Apply fade-in animations
        animateButtonFadeIn(multiBtn, 0.1);
        animateButtonFadeIn(leaderboardBtn, 0.2);
        animateButtonFadeIn(logoutBtn, 0.3);
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
                                         Button multiBtn,
                                         Button leaderboardBtn, Button logoutBtn) {
        // Background
        root.setStyle("-fx-background-image: url('/resources/assets/images/backgrounds/backgroundMenu.png'); " +
                     "-fx-background-size: cover; -fx-background-position: center center; " +
                     "-fx-background-repeat: no-repeat;");

        // Welcome text - lớn, dễ đọc và nổi bật
        // Sử dụng font hệ thống đậm để đảm bảo hiển thị nếu font pixel không có sẵn
//        welcome.setStyle("-fx-font-family: 'Arial Black', 'Impact', sans-serif; -fx-font-size: 44px; " +
//                        "-fx-font-weight: bold; -fx-fill: #FFD700; " +
//                        "-fx-stroke: #D32F2F; -fx-stroke-width: 3px;");

        // Button base style - lớn hơn, chữ màu đen
        String baseStyle = "-fx-min-width: 380px; -fx-pref-width: 400px; -fx-pref-height: 75px; " +
                          "-fx-font-family: 'Arial Black', 'Impact', sans-serif; -fx-font-size: 22px; " +
                          "-fx-font-weight: bold; -fx-text-fill: black; " +
                          "-fx-border-color: #3E2A1C; -fx-border-width: 3px; " +
                          "-fx-background-radius: 8px; -fx-border-radius: 8px; " +
                          "-fx-cursor: hand;";

        // Multiplayer - purple
        multiBtn.setStyle(baseStyle + "-fx-background-color: #A36FD1;");

        // Leaderboard - golden
        leaderboardBtn.setStyle(baseStyle + "-fx-background-color: #FFB347;");

        // Logout - orange
        logoutBtn.setStyle(baseStyle + "-fx-background-color: #E78640;");
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

        // Lấy kích thước hiện tại của stage để giữ nguyên kích thước/fullscreen
        double width = stage.getWidth() > 0 ? stage.getWidth() : 600;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 500;

        Scene scene = new Scene(root, width, height);
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