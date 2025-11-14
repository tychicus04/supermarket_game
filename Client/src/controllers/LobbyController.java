package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.effect.*;
import javafx.application.Platform;
import models.Message;
import network.NetworkManager;
import utils.UIHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static constants.GameConstants.*;

/**
 * Lobby Controller - Waiting room with friends list and invite system
 * Displays players, room info, and allows inviting friends
 */
public class LobbyController {
    private Stage stage;
    private NetworkManager network;

    private String currentUsername;
    private String currentRoomId;
    private List<String> playersInRoom;
    private List<Map<String, String>> friendsList;
    private List<Map<String, String>> availableRooms;

    // UI Components
    private Label roomIdLabel;
    private Text playersCountText;
    private Text waitingMessageText;
    private VBox playerSlotsBox;
    private VBox friendsListBox;
    private VBox roomsListBox;
    private VBox searchResultsBox;
    private VBox friendRequestsBox;
    private Button startGameButton;
    private Label[] playerLabels; // P1, P2, P3, P4

    private BorderPane mainRoot;
    private boolean inRoom = false;

    private Runnable onGameStart;
    private Runnable onBackToMenu;

    public LobbyController(Stage stage, Runnable onGameStart, Runnable onBackToMenu) {
        this.stage = stage;
        this.network = NetworkManager.getInstance();
        this.onGameStart = onGameStart;
        this.onBackToMenu = onBackToMenu;
        this.playersInRoom = new ArrayList<>();
        this.friendsList = new ArrayList<>();
        this.availableRooms = new ArrayList<>();
        this.playerLabels = new Label[4];
    }

    public void show(String username, String roomId, List<String> initialPlayers) {
        System.out.println("📋 LobbyController.show() called");
        System.out.println("   username: " + username);
        System.out.println("   roomId: " + roomId);
        System.out.println("   initialPlayers: " + initialPlayers);

        this.currentUsername = username;
        this.currentRoomId = roomId;
        this.playersInRoom = new ArrayList<>(initialPlayers);
        this.inRoom = (roomId != null && !roomId.isEmpty());

        System.out.println("   inRoom: " + inRoom);
        System.out.println("   Calling createLobbyUI()...");

        createLobbyUI();

        // Request friend list and room list from server
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIENDS, ""));
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));
        if (!inRoom) {
            network.sendMessage(new Message(MESSAGE_TYPE_GET_ROOM_LIST, ""));
        }
    }

    /**
     * Create lobby UI
     */
    private void createLobbyUI() {
        // ALWAYS create a new BorderPane to avoid "already set as root" error
        mainRoot = new BorderPane();

        // Set background image based on room status
        String backgroundPath = inRoom ?
            "/resources/assets/images/backgrounds/backgroundManHinhCho.png" :
            "/resources/assets/images/backgrounds/backgroundLobby.png";

        System.out.println("Loading background: " + backgroundPath + " (inRoom=" + inRoom + ")");

        try {
            Image bgImage = new Image(getClass().getResourceAsStream(backgroundPath));
            BackgroundImage backgroundImage = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
            );
            mainRoot.setBackground(new Background(backgroundImage));
        } catch (Exception e) {
            System.err.println("Failed to load background image: " + e.getMessage());
            // Fallback to gradient if image not found
            mainRoot.setStyle(UIHelper.createGradientBackground("#2c3e50", "#34495e"));
        }

        if (inRoom) {
            // Show room view (current implementation)
            System.out.println("Creating ROOM VIEW for room: " + currentRoomId);
            showRoomView();
        } else {
            // Show room browser
            System.out.println("Creating ROOM BROWSER");
            showRoomBrowser();
        }

        // Lấy kích thước hiện tại của stage để giữ nguyên kích thước/fullscreen
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;

        // Create NEW scene each time to avoid "already set as root" error
        Scene scene = new Scene(mainRoot, width, height);
        stage.setScene(scene);

        // Ensure stage is visible and on top
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();

        System.out.println("   ✅ Scene set successfully, stage showing: " + stage.isShowing());
    }

    /**
     * Show the room browser UI
     */
    private void showRoomBrowser() {
        // Top: Title
        VBox topSection = createBrowserTitle();

        // Center: Room list
        VBox centerSection = createRoomList();

        // Right: Friends section
        VBox rightSection = createFriendsSection();

        // Bottom: Actions
        HBox bottomSection = createBrowserBottomActions();

        mainRoot.setTop(topSection);
        mainRoot.setCenter(centerSection);
        mainRoot.setRight(rightSection);
        mainRoot.setBottom(bottomSection);
    }

    /**
     * Show the room view UI (when in a room)
     */
    private void showRoomView() {
        // Create centered waiting room UI (simple and focused)
        VBox centerContent = new VBox(40);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(80));

        // Room ID display
        Text roomTitle = new Text("Room: " + currentRoomId);
        roomTitle.setFont(Font.font("System", FontWeight.BOLD, 42));
        roomTitle.setFill(Color.WHITE);
        roomTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 12, 0.8, 0, 5);");

        // Players count
        playersCountText = new Text("Players: " + playersInRoom.size() + "/4");
        playersCountText.setFont(Font.font("System", FontWeight.BOLD, 36));
        playersCountText.setFill(Color.WHITE);
        playersCountText.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 10, 0.7, 0, 4);");

        // Waiting message
        waitingMessageText = new Text("Waiting for more players...");
        waitingMessageText.setFont(Font.font("System", FontWeight.NORMAL, 28));
        waitingMessageText.setFill(Color.web("#95a5a6"));
        waitingMessageText.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 8, 0.6, 0, 3);");

        // Buttons container
        VBox buttonsBox = new VBox(25);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(40, 0, 0, 0));

        // Start game button
        startGameButton = createStyledButton("START GAME", "#27ae60", "#229954");
        startGameButton.setPrefWidth(280);
        startGameButton.setPrefHeight(60);
        startGameButton.setStyle(startGameButton.getStyle() + "-fx-font-size: 20px;");
        startGameButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_START_GAME, currentRoomId));
        });
        startGameButton.setDisable(playersInRoom.size() < 2);

        // Only show start button for room creator
        if (!playersInRoom.isEmpty() && !currentUsername.equals(playersInRoom.get(0))) {
            startGameButton.setVisible(false);
            startGameButton.setManaged(false);
        }

        // Leave room button
        Button leaveButton = createStyledButton("LEAVE ROOM", "#e74c3c", "#c0392b");
        leaveButton.setPrefWidth(280);
        leaveButton.setPrefHeight(60);
        leaveButton.setStyle(leaveButton.getStyle() + "-fx-font-size: 20px;");
        leaveButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_LEAVE_ROOM, currentRoomId));
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        buttonsBox.getChildren().addAll(startGameButton, leaveButton);

        // Add all to center content
        centerContent.getChildren().addAll(roomTitle, playersCountText, waitingMessageText, buttonsBox);

        mainRoot.setCenter(centerContent);
    }

    /**
     * Create browser title section
     */
    private VBox createBrowserTitle() {
        VBox titleBox = new VBox(20);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(40, 20, 40, 20));

        // Glass-morphism effect container - more vibrant
        titleBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(41, 128, 185, 0.4), rgba(52, 152, 219, 0.3));" +
            "-fx-background-radius: 25px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.5);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 25px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 25, 0.4, 0, 8);"
        );

        Text title = new Text("🏠 ROOM BROWSER");
        title.setFont(Font.font("System", FontWeight.BOLD, 52));
        title.setFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 10, 0.7, 0, 4);" +
            "-fx-stroke: rgba(0, 0, 0, 0.5);" +
            "-fx-stroke-width: 1px;"
        );

        Label subtitle = new Label("Choose a room to join or create your own");
        subtitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        subtitle.setTextFill(Color.web("#FFEB3B"));
        subtitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 5, 0.5, 0, 2);");

        titleBox.getChildren().addAll(title, subtitle);
        return titleBox;
    }

    /**
     * Create room list display
     */
    private VBox createRoomList() {
        VBox listContainer = new VBox(20);
        listContainer.setAlignment(Pos.TOP_CENTER);
        listContainer.setPadding(new Insets(20));

        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(20, 30, 20, 30));
        headerBox.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(52, 152, 219, 0.5), rgba(41, 128, 185, 0.4));" +
            "-fx-background-radius: 18px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.5);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 18px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 15, 0.3, 0, 5);"
        );

        Text roomsTitle = new Text("📋 AVAILABLE ROOMS");
        roomsTitle.setFont(Font.font("System", FontWeight.BOLD, 30));
        roomsTitle.setFill(Color.WHITE);
        roomsTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 6, 0.5, 0, 3);");

        Button refreshButton = createStyledButton("🔄 Refresh", "#3498db", "#2980b9");
        refreshButton.setPrefWidth(140);
        refreshButton.setPrefHeight(45);
        refreshButton.setStyle(refreshButton.getStyle() + "-fx-font-size: 16px;");
        refreshButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_GET_ROOM_LIST, ""));
        });

        headerBox.getChildren().addAll(roomsTitle, refreshButton);

        ScrollPane roomsScroll = new ScrollPane();
        roomsScroll.setPrefHeight(450);
        roomsScroll.setFitToWidth(true);
        roomsScroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;"
        );

        roomsListBox = new VBox(15);
        roomsListBox.setPadding(new Insets(15));
        roomsListBox.setAlignment(Pos.TOP_CENTER);
        roomsListBox.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.3);" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.2);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 15px;"
        );

        Label noRoomsLabel = new Label("⚠ No rooms available. Create one!");
        noRoomsLabel.setTextFill(Color.web("#FFF176"));
        noRoomsLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        noRoomsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 5, 0.4, 0, 2);");
        roomsListBox.getChildren().add(noRoomsLabel);

        roomsScroll.setContent(roomsListBox);

        listContainer.getChildren().addAll(headerBox, roomsScroll);
        return listContainer;
    }

    /**
     * Create bottom actions for browser
     */
    private HBox createBrowserBottomActions() {
        HBox actions = new HBox(30);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(30));

        Button backButton = createStyledButton("⬅ BACK TO MENU", "#e74c3c", "#c0392b");
        backButton.setPrefWidth(220);
        backButton.setPrefHeight(55);
        backButton.setStyle(backButton.getStyle() + "-fx-font-size: 18px;");
        backButton.setOnAction(e -> {
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        Button createRoomButton = createStyledButton("➕ CREATE ROOM", "#27ae60", "#229954");
        createRoomButton.setPrefWidth(220);
        createRoomButton.setPrefHeight(55);
        createRoomButton.setStyle(createRoomButton.getStyle() + "-fx-font-size: 18px;");
        createRoomButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_CREATE_ROOM, ""));
        });

        actions.getChildren().addAll(backButton, createRoomButton);
        return actions;
    }

    /**
     * Create room info section
     */
    private VBox createRoomInfo() {
        VBox infoBox = new VBox(20);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(40, 50, 40, 50));

        // Glass-morphism container - more vibrant
        infoBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(46, 204, 113, 0.35), rgba(39, 174, 96, 0.25));" +
            "-fx-background-radius: 30px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.6);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 30px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 30, 0.4, 0, 10);"
        );

        Text title = new Text("⏳ WAITING ROOM");
        title.setFont(Font.font("System", FontWeight.BOLD, 56));
        title.setFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 1.0), 12, 0.8, 0, 5);" +
            "-fx-stroke: rgba(0, 0, 0, 0.7);" +
            "-fx-stroke-width: 2px;"
        );

        HBox roomInfo = new HBox(20);
        roomInfo.setAlignment(Pos.CENTER);
        roomInfo.setPadding(new Insets(20, 35, 20, 35));
        roomInfo.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(52, 152, 219, 0.6), rgba(41, 128, 185, 0.5));" +
            "-fx-background-radius: 18px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.8);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 18px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 10, 0.3, 0, 4);"
        );

        Label roomLabel = new Label("🎮 Room ID:");
        roomLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        roomLabel.setTextFill(Color.WHITE);
        roomLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 4, 0.5, 0, 2);");

        roomIdLabel = new Label(currentRoomId);
        roomIdLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        roomIdLabel.setTextFill(Color.web("#FFEB3B"));
        roomIdLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 1.0), 5, 0.6, 0, 3);" +
            "-fx-padding: 8px 20px;" +
            "-fx-background-color: rgba(0, 0, 0, 0.5);" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: rgba(255, 235, 59, 0.5);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 10px;"
        );

        Button copyButton = createStyledButton("📋 Copy", "#2ecc71", "#27ae60");
        copyButton.setPrefWidth(120);
        copyButton.setPrefHeight(45);
        copyButton.setStyle(copyButton.getStyle() + "-fx-font-size: 16px;");
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard =
                    javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content =
                    new javafx.scene.input.ClipboardContent();
            content.putString(currentRoomId);
            clipboard.setContent(content);

            UIHelper.showInfo("Copied", "Room ID copied to clipboard!");
        });

        roomInfo.getChildren().addAll(roomLabel, roomIdLabel, copyButton);

        infoBox.getChildren().addAll(title, roomInfo);
        return infoBox;
    }

    /**
     * Create player slots display (P1-P4)
     */
    private VBox createPlayerSlots() {
        VBox slotsContainer = new VBox(25);
        slotsContainer.setAlignment(Pos.CENTER);
        slotsContainer.setPadding(new Insets(30));

        // Title with background
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(15, 40, 15, 40));
        titleBox.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(155, 89, 182, 0.5), rgba(142, 68, 173, 0.4));" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.5);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 15px;"
        );

        Text playersTitle = new Text("👥 PLAYERS");
        playersTitle.setFont(Font.font("System", FontWeight.BOLD, 36));
        playersTitle.setFill(Color.WHITE);
        playersTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 8, 0.6, 0, 4);");
        titleBox.getChildren().add(playersTitle);

        playerSlotsBox = new VBox(18);
        playerSlotsBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 4; i++) {
            HBox playerSlot = createPlayerSlot(i);
            playerSlotsBox.getChildren().add(playerSlot);
        }

        slotsContainer.getChildren().addAll(titleBox, playerSlotsBox);
        return slotsContainer;
    }

    /**
     * Create a single player slot
     */
    private HBox createPlayerSlot(int slotIndex) {
        HBox slot = new HBox(25);
        slot.setAlignment(Pos.CENTER_LEFT);
        slot.setPadding(new Insets(25, 40, 25, 40));
        slot.setPrefWidth(500);
        slot.setMinHeight(90);

        // Beautiful gradient card with glass effect - more vibrant colors
        String[] gradientColors = {
            "linear-gradient(to right, rgba(52, 152, 219, 0.6), rgba(41, 128, 185, 0.5))",   // P1 - Blue
            "linear-gradient(to right, rgba(46, 204, 113, 0.6), rgba(39, 174, 96, 0.5))",   // P2 - Green
            "linear-gradient(to right, rgba(155, 89, 182, 0.6), rgba(142, 68, 173, 0.5))",  // P3 - Purple
            "linear-gradient(to right, rgba(230, 126, 34, 0.6), rgba(211, 84, 0, 0.5))"     // P4 - Orange
        };

        slot.setStyle(
            "-fx-background-color: " + gradientColors[slotIndex] + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.6);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 18, 0.35, 0, 6);"
        );

        Label positionLabel = new Label("P" + (slotIndex + 1));
        positionLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        positionLabel.setTextFill(Color.WHITE);
        positionLabel.setPrefWidth(70);
        positionLabel.setAlignment(Pos.CENTER);
        positionLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 6, 0.6, 0, 3);" +
            "-fx-background-color: rgba(0, 0, 0, 0.5);" +
            "-fx-background-radius: 12px;" +
            "-fx-padding: 8px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.4);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 12px;"
        );

        Label playerLabel = new Label("⚠ Empty Slot");
        playerLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        playerLabel.setTextFill(Color.web("#FFD54F"));  // Yellow for better contrast
        playerLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 5, 0.5, 0, 2);" +
            "-fx-background-color: rgba(0, 0, 0, 0.4);" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 5px 15px;"
        );
        playerLabels[slotIndex] = playerLabel;

        // Ready indicator (for future use)
        Label readyLabel = new Label("");
        readyLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        readyLabel.setTextFill(Color.web("#4CAF50"));
        readyLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 4, 0.4, 0, 2);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        slot.getChildren().addAll(positionLabel, playerLabel, spacer, readyLabel);
        return slot;
    }

    /**
     * Create friends section (right sidebar) with search and friend requests
     */
    private VBox createFriendsSection() {
        VBox friendsSection = new VBox(20);
        friendsSection.setPadding(new Insets(25, 20, 25, 20));
        friendsSection.setPrefWidth(340);
        friendsSection.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.4));" +
            "-fx-background-radius: 25px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.4);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 25px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 25, 0.4, 0, 8);"
        );

        // Header box
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(12, 20, 12, 20));
        headerBox.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(155, 89, 182, 0.5), rgba(142, 68, 173, 0.4));" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.4);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 15px;"
        );

        Text friendsTitle = new Text("👥 FRIENDS");
        friendsTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
        friendsTitle.setFill(Color.WHITE);
        friendsTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 6, 0.5, 0, 3);");
        headerBox.getChildren().add(friendsTitle);

        // Search users section
        VBox searchSection = new VBox(12);
        Label searchLabel = new Label("🔍 Find Friends");
        searchLabel.setTextFill(Color.web("#FFD54F"));
        searchLabel.setFont(Font.font("System", FontWeight.BOLD, 17));
        searchLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 4, 0.4, 0, 2);");

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);

        TextField searchField = new TextField();
        searchField.setPromptText("Search username...");
        searchField.setPrefWidth(200);
        searchField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95);" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.6);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;"
        );

        Button searchButton = createStyledButton("🔍", "#3498db", "#2980b9");
        searchButton.setPrefWidth(50);
        searchButton.setPrefHeight(38);
        searchButton.setStyle(searchButton.getStyle() + "-fx-font-size: 16px;");
        searchButton.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (searchTerm.length() >= 2) {
                network.sendMessage(new Message(MESSAGE_TYPE_SEARCH_USERS, searchTerm));
            }
        });

        searchBox.getChildren().addAll(searchField, searchButton);

        // Search results box (hidden by default)
        VBox searchResultsBox = new VBox(6);
        searchResultsBox.setVisible(false);
        searchResultsBox.setManaged(false);
        searchResultsBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.15);" +
            "-fx-padding: 12px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.2);" +
            "-fx-border-width: 1.5px;" +
            "-fx-border-radius: 10px;"
        );

        searchSection.getChildren().addAll(searchLabel, searchBox, searchResultsBox);

        // Friend requests section
        VBox requestsSection = new VBox(12);
        Label requestsLabel = new Label("📬 Friend Requests");
        requestsLabel.setTextFill(Color.web("#FFD54F"));
        requestsLabel.setFont(Font.font("System", FontWeight.BOLD, 17));
        requestsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 4, 0.4, 0, 2);");

        VBox requestsListBox = new VBox(8);
        requestsListBox.setStyle(
            "-fx-background-color: rgba(155, 89, 182, 0.25);" +
            "-fx-padding: 12px;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(155, 89, 182, 0.5);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 12px;"
        );

        Button refreshRequestsButton = createStyledButton("📬 Check", "#9b59b6", "#8e44ad");
        refreshRequestsButton.setPrefWidth(120);
        refreshRequestsButton.setPrefHeight(38);
        refreshRequestsButton.setStyle(refreshRequestsButton.getStyle() + "-fx-font-size: 14px;");
        refreshRequestsButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));
        });

        requestsSection.getChildren().addAll(requestsLabel, refreshRequestsButton, requestsListBox);

        // Friends list
        Label myFriendsLabel = new Label("💚 My Friends");
        myFriendsLabel.setTextFill(Color.web("#4CAF50"));
        myFriendsLabel.setFont(Font.font("System", FontWeight.BOLD, 17));
        myFriendsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 4, 0.4, 0, 2);");

        ScrollPane friendsScroll = new ScrollPane();
        friendsScroll.setPrefHeight(220);
        friendsScroll.setFitToWidth(true);
        friendsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        friendsListBox = new VBox(10);
        friendsListBox.setPadding(new Insets(12));
        friendsListBox.setStyle(
            "-fx-background-color: rgba(46, 204, 113, 0.2);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(46, 204, 113, 0.4);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 12px;"
        );

        Label noFriendsLabel = new Label("⚠ No friends yet");
        noFriendsLabel.setTextFill(Color.web("#FFD54F"));
        noFriendsLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        noFriendsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 4, 0.4, 0, 2);");
        friendsListBox.getChildren().add(noFriendsLabel);

        friendsScroll.setContent(friendsListBox);

        Button refreshFriendsButton = createStyledButton("🔄 Refresh", "#27ae60", "#229954");
        refreshFriendsButton.setPrefWidth(120);
        refreshFriendsButton.setPrefHeight(38);
        refreshFriendsButton.setStyle(refreshFriendsButton.getStyle() + "-fx-font-size: 14px;");
        refreshFriendsButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIENDS, ""));
        });

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3);");
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3);");

        friendsSection.getChildren().addAll(headerBox, searchSection,
                sep1, requestsSection, sep2, myFriendsLabel,
                refreshFriendsButton, friendsScroll);

        // Store references for later updates
        this.searchResultsBox = searchResultsBox;
        this.friendRequestsBox = requestsListBox;

        return friendsSection;
    }

    /**
     * Create bottom action buttons
     */
    private HBox createBottomActions() {
        HBox actions = new HBox(30);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(30));

        Button leaveButton = createStyledButton("🚪 LEAVE ROOM", "#e74c3c", "#c0392b");
        leaveButton.setPrefWidth(200);
        leaveButton.setPrefHeight(55);
        leaveButton.setStyle(leaveButton.getStyle() + "-fx-font-size: 18px;");
        leaveButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_LEAVE_ROOM, currentRoomId));
            // Navigate back to menu
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        startGameButton = createStyledButton("🎮 START GAME", "#27ae60", "#229954");
        startGameButton.setPrefWidth(200);
        startGameButton.setPrefHeight(55);
        startGameButton.setStyle(startGameButton.getStyle() + "-fx-font-size: 18px;");
        startGameButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_START_GAME, currentRoomId));
        });
        startGameButton.setDisable(playersInRoom.size() < 2);

        // Only show start button for room creator
        if (!currentUsername.equals(playersInRoom.get(0))) {
            startGameButton.setVisible(false);
        }

        actions.getChildren().addAll(leaveButton, startGameButton);
        return actions;
    }

    /**
     * Update player slots display
     */
    private void updatePlayerSlots() {
        // Update players count text if in room view
        if (!inRoom) {
            return;
        }

        // Update players count text
        if (playersCountText != null) {
            Platform.runLater(() -> {
                playersCountText.setText("Players: " + playersInRoom.size() + "/4");

                // Update waiting message based on player count
                if (waitingMessageText != null) {
                    if (playersInRoom.size() >= 2) {
                        waitingMessageText.setText("Ready to start!");
                        waitingMessageText.setFill(Color.web("#2ecc71"));
                    } else {
                        waitingMessageText.setText("Waiting for more players...");
                        waitingMessageText.setFill(Color.web("#95a5a6"));
                    }
                }
            });
        }

        // Update start button state
        if (startGameButton != null) {
            Platform.runLater(() -> {
                startGameButton.setDisable(playersInRoom.size() < 2);
            });
        }
    }

    /**
     * Update friends list display
     */
    private void updateFriendsList() {
        friendsListBox.getChildren().clear();

        if (friendsList.isEmpty()) {
            Label noFriends = new Label("No friends yet");
            noFriends.setTextFill(Color.web("#95a5a6"));
            friendsListBox.getChildren().add(noFriends);
            return;
        }

        for (Map<String, String> friend : friendsList) {
            HBox friendItem = createFriendItem(friend);
            friendsListBox.getChildren().add(friendItem);
        }
    }

    /**
     * Update room list display
     */
    private void updateRoomsList() {
        roomsListBox.getChildren().clear();

        if (availableRooms.isEmpty()) {
            Label noRooms = new Label("No rooms available. Create one!");
            noRooms.setTextFill(Color.web("#95a5a6"));
            noRooms.setFont(Font.font("Arial", 14));
            roomsListBox.getChildren().add(noRooms);
            return;
        }

        for (Map<String, String> room : availableRooms) {
            HBox roomItem = createRoomItem(room);
            roomsListBox.getChildren().add(roomItem);
        }
    }

    /**
     * Create a room list item
     */
    private HBox createRoomItem(Map<String, String> room) {
        HBox item = new HBox(20);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(20, 25, 20, 25));
        item.setPrefWidth(650);
        item.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(52, 152, 219, 0.3), rgba(41, 128, 185, 0.25));" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.6);" +
            "-fx-border-radius: 15px;" +
            "-fx-border-width: 2.5px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 12, 0.3, 0, 4);"
        );

        VBox infoBox = new VBox(8);

        Label roomIdLabel = new Label("Room: " + room.get("roomId"));
        roomIdLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        roomIdLabel.setTextFill(Color.WHITE);
        roomIdLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 4, 0.4, 0, 2);");

        Label detailsLabel = new Label("Host: " + room.get("creator") +
                " • Players: " + room.get("playerCount") + "/" + room.get("maxPlayers"));
        detailsLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        detailsLabel.setTextFill(Color.web("#ecf0f1"));
        detailsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 3, 0.3, 0, 1);");

        infoBox.getChildren().addAll(roomIdLabel, detailsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button joinButton = createStyledButton("Request Join", "#27ae60", "#229954");
        joinButton.setOnAction(e -> {
            String roomId = room.get("roomId");
            network.sendMessage(new Message(MESSAGE_TYPE_REQUEST_JOIN, roomId));
            joinButton.setDisable(true);
            joinButton.setText("Requested...");
        });

        item.getChildren().addAll(infoBox, spacer, joinButton);

        // Hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(52, 152, 219, 0.45), rgba(41, 128, 185, 0.35));" +
                "-fx-background-radius: 15px;" +
                "-fx-border-color: rgba(52, 152, 219, 0.8);" +
                "-fx-border-radius: 15px;" +
                "-fx-border-width: 2.5px;" +
                "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.5), 15, 0.4, 0, 5);" +
                "-fx-cursor: hand;"
            );
        });

        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(52, 152, 219, 0.3), rgba(41, 128, 185, 0.25));" +
                "-fx-background-radius: 15px;" +
                "-fx-border-color: rgba(52, 152, 219, 0.6);" +
                "-fx-border-radius: 15px;" +
                "-fx-border-width: 2.5px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 12, 0.3, 0, 4);"
            );
        });

        return item;
    }

    // ==================== MESSAGE HANDLERS ====================

    /**
     * Handle room update (player joined/left)
     */
    public void handleRoomUpdate(Message message) {
        String data = message.getData();

        switch (message.getType()) {
            case "S2C_ROOM_UPDATE":
                updateRoomFromJson(data);
                break;

            case "PLAYER_JOINED":
                    // Only update if we're in room view (playerLabels is initialized)
                    if (inRoom && playerLabels != null && playerLabels[0] != null) {
                        updatePlayerSlots();
                    }
                String[] parts = data.split(":");
                if (parts.length >= 2) {
                    // Update player list (simplified - server should send full list)
                    // For now, just update the count
                    updatePlayerSlots();
                }
                break;
        }
    }

    /**
     * Handle friend list update from server
     */
    public void handleFriendListUpdate(Message message) {
        String jsonData = message.getData();
        parseFriendsList(jsonData);
        updateFriendsList();
    }

    /**
     * Handle invite received
     */
    public void handleInviteReceived(Message message) {
        String[] parts = message.getData().split(";");
        if (parts.length >= 2) {
            String inviterUsername = parts[0];
            String roomId = parts[1];

            boolean accept = UIHelper.showConfirm(
                    "Room Invitation",
                    inviterUsername + " invited you to join room: " + roomId + "\n\nAccept?"
            );

            if (accept) {
                network.sendMessage(new Message(MESSAGE_TYPE_JOIN_ROOM, roomId));
            }
        }
    }

    /**
     * Handle room list update from server
     */
    public void handleRoomListUpdate(Message message) {
        String jsonData = message.getData();
        parseRoomsList(jsonData);
        updateRoomsList();
    }

    /**
     * Handle join request from another player (for room creator)
     */
    public void handleJoinRequest(Message message) {
        String[] parts = message.getData().split(";");
        if (parts.length >= 2) {
            String requesterUsername = parts[0];
            String roomId = parts[1];

            boolean accept = UIHelper.showConfirm(
                    "Join Request",
                    requesterUsername + " wants to join your room.\n\nAccept?"
            );

            if (accept) {
                network.sendMessage(new Message(MESSAGE_TYPE_ACCEPT_JOIN, requesterUsername + ";" + roomId));
            } else {
                network.sendMessage(new Message(MESSAGE_TYPE_REJECT_JOIN, requesterUsername + ";" + roomId));
            }
        }
    }

    /**
     * Handle join request approved
     */
    public void handleJoinApproved(Message message) {
        String roomId = message.getData();
        UIHelper.showInfo("Join Approved", "You can now join the room!");
        // Automatically join the room
        network.sendMessage(new Message(MESSAGE_TYPE_JOIN_ROOM, roomId));
    }

    /**
     * Handle join request rejected
     */
    public void handleJoinRejected(Message message) {
        String reason = message.getData();
        UIHelper.showError("Join Rejected", reason);
        // Refresh room list
        network.sendMessage(new Message(MESSAGE_TYPE_GET_ROOM_LIST, ""));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Update room info from JSON
     */
    private void updateRoomFromJson(String json) {
        // Parse player list from JSON
        // Simplified parsing (in real app, use proper JSON library)
        playersInRoom.clear();

        // Extract player names from JSON array
        int start = json.indexOf("[");
        int end = json.indexOf("]");
        if (start != -1 && end != -1) {
            String playersStr = json.substring(start + 1, end);
            String[] players = playersStr.replace("\"", "").split(",");
            for (String player : players) {
                if (!player.trim().isEmpty()) {
                    playersInRoom.add(player.trim());
                }
            }
        }

        updatePlayerSlots();
    }

    /**
     * Parse friends list from JSON
     */
    private void parseFriendsList(String json) {
        friendsList.clear();

        // Simple JSON array parsing
        if (json.equals("[]")) return;

        json = json.substring(1, json.length() - 1); // Remove [ ]

        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    String friendJson = json.substring(start, i + 1);
                    Map<String, String> friend = parseFriendObject(friendJson);
                    if (friend != null) {
                        friendsList.add(friend);
                    }
                    start = i + 2; // Skip }, and space
                }
            }
        }
    }

    /**
     * Parse rooms list from JSON
     */
    private void parseRoomsList(String json) {
        availableRooms.clear();

        // Simple JSON array parsing
        if (json.equals("[]")) return;

        json = json.substring(1, json.length() - 1); // Remove [ ]

        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    String roomJson = json.substring(start, i + 1);
                    Map<String, String> room = parseRoomObject(roomJson);
                    if (room != null) {
                        availableRooms.add(room);
                    }
                    start = i + 2; // Skip }, and space
                }
            }
        }
    }

    /**
     * Parse single friend object from JSON
     */
    private Map<String, String> parseFriendObject(String json) {
        Map<String, String> friend = new HashMap<>();

        // Extract username
        int usernameStart = json.indexOf("\"username\":\"") + 12;
        int usernameEnd = json.indexOf("\"", usernameStart);
        if (usernameStart > 11 && usernameEnd > usernameStart) {
            friend.put("username", json.substring(usernameStart, usernameEnd));
        }

        // Extract user_id
        int idStart = json.indexOf("\"user_id\":") + 10;
        int idEnd = json.indexOf("}", idStart);
        if (idStart > 9) {
            String idStr = json.substring(idStart, idEnd).trim();
            friend.put("user_id", idStr);
        }

        return friend.isEmpty() ? null : friend;
    }

    /**
     * Parse single room object from JSON
     */
    private Map<String, String> parseRoomObject(String json) {
        Map<String, String> room = new HashMap<>();

        // Extract roomId
        int roomIdStart = json.indexOf("\"roomId\":\"") + 10;
        int roomIdEnd = json.indexOf("\"", roomIdStart);
        if (roomIdStart > 9 && roomIdEnd > roomIdStart) {
            room.put("roomId", json.substring(roomIdStart, roomIdEnd));
        }

        // Extract creator
        int creatorStart = json.indexOf("\"creator\":\"") + 11;
        int creatorEnd = json.indexOf("\"", creatorStart);
        if (creatorStart > 10 && creatorEnd > creatorStart) {
            room.put("creator", json.substring(creatorStart, creatorEnd));
        }

        // Extract playerCount
        int countStart = json.indexOf("\"playerCount\":") + 14;
        int countEnd = json.indexOf(",", countStart);
        if (countEnd == -1) countEnd = json.indexOf("}", countStart);
        if (countStart > 13) {
            String countStr = json.substring(countStart, countEnd).trim();
            room.put("playerCount", countStr);
        }

        // Extract maxPlayers
        int maxStart = json.indexOf("\"maxPlayers\":") + 13;
        int maxEnd = json.indexOf("}", maxStart);
        if (maxStart > 12) {
            String maxStr = json.substring(maxStart, maxEnd).trim();
            room.put("maxPlayers", maxStr);
        }

        return room.isEmpty() ? null : room;
    }

    // ==================== FRIEND MANAGEMENT HANDLERS ====================

    /**
     * Handle search results
     */
    public void handleSearchResults(Message message) {
        String json = message.getData();
        List<String> results = parseSimpleStringArray(json);

        searchResultsBox.getChildren().clear();

        if (results.isEmpty()) {
            Label noResults = new Label("No users found");
            noResults.setTextFill(Color.web("#95a5a6"));
            searchResultsBox.getChildren().add(noResults);
        } else {
            for (String username : results) {
                HBox userItem = createSearchResultItem(username);
                searchResultsBox.getChildren().add(userItem);
            }
        }

        searchResultsBox.setVisible(true);
        searchResultsBox.setManaged(true);
    }

    /**
     * Create search result item
     */
    private HBox createSearchResultItem(String username) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;"
        );

        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 3, 0.3, 0, 1);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = createStyledButton("+", "#27ae60", "#229954");
        addButton.setPrefWidth(45);
        addButton.setPrefHeight(35);
        addButton.setPadding(new Insets(5, 10, 5, 10));
        addButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_SEND_FRIEND_REQUEST, username));
            addButton.setDisable(true);
            addButton.setText("OK");
        });

        item.getChildren().addAll(nameLabel, spacer, addButton);
        return item;
    }

    /**
     * Handle friend requests list
     */
    public void handleFriendRequests(Message message) {
        String json = message.getData();
        List<String> requests = parseSimpleStringArray(json);

        friendRequestsBox.getChildren().clear();

        if (requests.isEmpty()) {
            Label noRequests = new Label("No pending requests");
            noRequests.setTextFill(Color.web("#95a5a6"));
            noRequests.setFont(Font.font("Arial", 11));
            friendRequestsBox.getChildren().add(noRequests);
        } else {
            for (String fromUser : requests) {
                HBox requestItem = createFriendRequestItem(fromUser);
                friendRequestsBox.getChildren().add(requestItem);
            }
        }
    }

    /**
     * Create friend request item with improved styling
     */
    private HBox createFriendRequestItem(String fromUser) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(155, 89, 182, 0.4), rgba(142, 68, 173, 0.3));" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: rgba(155, 89, 182, 0.7);" +
            "-fx-border-radius: 10px;" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(155, 89, 182, 0.4), 10, 0.3, 0, 3);"
        );

        Label nameLabel = new Label(fromUser);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 3, 0.5, 0, 2);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button acceptButton = createStyledButton("Accept", "#27ae60", "#229954");
        acceptButton.setPrefWidth(80);
        acceptButton.setPrefHeight(35);
        acceptButton.setTooltip(new Tooltip("Accept"));
        acceptButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_ACCEPT_FRIEND, fromUser));
            friendRequestsBox.getChildren().remove(item);
        });

        Button rejectButton = createStyledButton("Reject", "#e74c3c", "#c0392b");
        rejectButton.setPrefWidth(80);
        rejectButton.setPrefHeight(35);
        rejectButton.setTooltip(new Tooltip("Reject"));
        rejectButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_REJECT_FRIEND, fromUser));
            friendRequestsBox.getChildren().remove(item);
        });

        item.getChildren().addAll(nameLabel, spacer, acceptButton, rejectButton);

        // Hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(155, 89, 182, 0.55), rgba(142, 68, 173, 0.45));" +
                "-fx-background-radius: 10px;" +
                "-fx-border-color: rgba(155, 89, 182, 0.9);" +
                "-fx-border-radius: 10px;" +
                "-fx-border-width: 2px;" +
                "-fx-effect: dropshadow(gaussian, rgba(155, 89, 182, 0.6), 15, 0.4, 0, 5);"
            );
        });

        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(155, 89, 182, 0.4), rgba(142, 68, 173, 0.3));" +
                "-fx-background-radius: 10px;" +
                "-fx-border-color: rgba(155, 89, 182, 0.7);" +
                "-fx-border-radius: 10px;" +
                "-fx-border-width: 2px;" +
                "-fx-effect: dropshadow(gaussian, rgba(155, 89, 182, 0.4), 10, 0.3, 0, 3);"
            );
        });

        return item;
    }

    /**
     * Handle friend list update with online status
     */
    public void handleFriendList(Message message) {
        String json = message.getData();
        parseFriendListWithOnline(json);
        updateFriendsList();
    }

    /**
     * Parse friend list with online status
     */
    private void parseFriendListWithOnline(String json) {
        friendsList.clear();

        if (json.equals("[]")) return;

        json = json.substring(1, json.length() - 1); // Remove [ ]

        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    String friendJson = json.substring(start, i + 1);
                    Map<String, String> friend = parseFriendWithOnline(friendJson);
                    if (friend != null) {
                        friendsList.add(friend);
                    }
                    start = i + 2;
                }
            }
        }
    }

    /**
     * Parse friend object with online status
     */
    private Map<String, String> parseFriendWithOnline(String json) {
        Map<String, String> friend = new HashMap<>();

        // Extract username
        int usernameStart = json.indexOf("\"username\":\"") + 12;
        int usernameEnd = json.indexOf("\"", usernameStart);
        if (usernameStart > 11 && usernameEnd > usernameStart) {
            friend.put("username", json.substring(usernameStart, usernameEnd));
        }

        // Extract online status
        int onlineStart = json.indexOf("\"online\":") + 9;
        int onlineEnd = json.indexOf("}", onlineStart);
        if (onlineStart > 8) {
            String onlineStr = json.substring(onlineStart, onlineEnd).trim();
            friend.put("online", onlineStr);
        }

        return friend.isEmpty() ? null : friend;
    }

    /**
     * Update friend list item to include invite button with improved styling
     */
    private HBox createFriendItem(Map<String, String> friend) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15, 18, 15, 18));
        item.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.2);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-radius: 12px;" +
            "-fx-border-width: 1.5px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.2, 0, 3);"
        );

        String username = friend.get("username");
        boolean isOnline = "true".equals(friend.get("online"));

        // Status indicator - larger and more visible
        Label statusLabel = new Label("●");
        statusLabel.setTextFill(isOnline ? Color.web("#2ecc71") : Color.web("#95a5a6"));
        statusLabel.setFont(Font.font(18));
        statusLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, " +
            (isOnline ? "rgba(46, 204, 113, 0.9)" : "rgba(149, 165, 166, 0.5)") +
            ", 6, 0.7, 0, 0);"
        );

        // Username - larger, bold, better contrast
        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 3, 0.4, 0, 2);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Invite button (only show if in room)
        if (inRoom && currentRoomId != null) {
            Button inviteButton = createStyledButton("📧 Invite", "#3498db", "#2980b9");
            inviteButton.setPrefWidth(90);
            inviteButton.setTooltip(new javafx.scene.control.Tooltip("Invite to your room"));
            inviteButton.setOnAction(e -> {
                network.sendMessage(new Message(MESSAGE_TYPE_INVITE_TO_ROOM,
                        username + ";" + currentRoomId));
                UIHelper.showInfo("Invited", "Invitation sent to " + username);
            });

            item.getChildren().addAll(statusLabel, nameLabel, spacer, inviteButton);
        } else {
            item.getChildren().addAll(statusLabel, nameLabel, spacer);
        }

        // Hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.3);" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: rgba(255, 255, 255, 0.5);" +
                "-fx-border-radius: 12px;" +
                "-fx-border-width: 1.5px;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.2), 10, 0.3, 0, 3);" +
                "-fx-cursor: hand;"
            );
        });

        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.2);" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                "-fx-border-radius: 12px;" +
                "-fx-border-width: 1.5px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.2, 0, 3);"
            );
        });

        return item;
    }

    /**
     * Handle room invite received
     */
    public void handleRoomInvite(Message message) {
        String[] parts = message.getData().split(";");
        if (parts.length >= 2) {
            String inviterUsername = parts[0];
            String roomId = parts[1];

            Platform.runLater(() -> {
                boolean accept = UIHelper.showConfirm("Room Invite",
                    inviterUsername + " invited you to join room " + roomId + ".\nDo you want to join?");

                if (accept) {
                    network.sendMessage(new Message(MESSAGE_TYPE_REQUEST_JOIN, roomId));
                }
            });
        }
    }

    /**
     * Create styled button with custom colors
     */
    private Button createStyledButton(String text, String normalColor, String hoverColor) {
        Button button = new Button(text);
        button.setStyle(
            "-fx-background-color: " + normalColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0.3, 0, 2);"
        );

        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: " + hoverColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 8, 0.4, 0, 3);"
        ));

        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: " + normalColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0.3, 0, 2);"
        ));

        return button;
    }

    /**
     * Parse simple string array from JSON
     */
    private List<String> parseSimpleStringArray(String json) {
        List<String> result = new ArrayList<>();

        if (json == null || json.equals("[]")) {
            return result;
        }

        // Remove brackets
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        // Split by comma and clean quotes
        String[] parts = json.split(",");
        for (String part : parts) {
            String cleaned = part.trim().replace("\"", "");
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }

        return result;
    }
}
