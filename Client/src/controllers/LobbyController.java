package controllers;

import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
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
import javafx.util.Duration;
import models.Message;
import network.NetworkManager;
import utils.JsonParser;
import utils.RoomUpdateHandler;
import utils.RoomUpdateHandler.*;
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
    private String currentRoomCreator;
    private List<String> playersInRoom;
    private List<Map<String, String>> friendsList;
    private List<Map<String, String>> availableRooms;

    // Track sent requests and received requests
    private List<String> sentFriendRequests;
    private List<String> receivedFriendRequests;

    private Timeline friendsRefreshTimer;
    private Timeline roomsRefreshTimer;

    // UI Components
    private VBox playerSlotsBox;
    private VBox friendsListBox;
    private VBox roomsListBox;
    private VBox searchResultsBox;
    private VBox friendRequestsBox;
    private Button startGameButton;
    private Label[] playerLabels;
    private HBox[] playerSlots;

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
        this.sentFriendRequests = new ArrayList<>();
        this.receivedFriendRequests = new ArrayList<>();
        this.playerLabels = new Label[2];
        this.playerSlots = new HBox[2];
    }

    public void show(String username, String roomId, List<String> initialPlayers) {
        this.currentUsername = username;
        this.currentRoomId = roomId;
        this.playersInRoom = new ArrayList<>(initialPlayers);
        this.inRoom = (roomId != null && !roomId.isEmpty());

        if (inRoom && !initialPlayers.isEmpty()) {
            this.currentRoomCreator = initialPlayers.get(0);
            System.out.println("LobbyController.show() - Room creator set to: " + currentRoomCreator);
        }

        createLobbyUI();

        // Request friend list and room list from server
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIENDS, ""));
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));
        if (!inRoom) {
            network.sendMessage(new Message(MESSAGE_TYPE_GET_ROOM_LIST, ""));
        }

        startAutoRefreshFriendsAndRooms();
    }

    /**
     * Start auto-refresh timers for friends and rooms
     */
    private void startAutoRefreshFriendsAndRooms() {
        // Stop existing timers if any
        stopAutoRefresh();

        // Refresh friends list every 5 seconds
        friendsRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIENDS, ""));
        }));
        friendsRefreshTimer.setCycleCount(Animation.INDEFINITE);
        friendsRefreshTimer.play();

        if (!inRoom) {
            roomsRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                network.sendMessage(new Message(MESSAGE_TYPE_GET_ROOM_LIST, ""));
            }));
            roomsRefreshTimer.setCycleCount(Animation.INDEFINITE);
            roomsRefreshTimer.play();
        }
    }

    /**
     * Stop auto-refresh timers
     */
    private void stopAutoRefresh() {
        if (friendsRefreshTimer != null) {
            friendsRefreshTimer.stop();
            friendsRefreshTimer = null;
        }
        if (roomsRefreshTimer != null) {
            roomsRefreshTimer.stop();
            roomsRefreshTimer = null;
        }
    }

    /**
     * Create lobby UI
     */
    private void createLobbyUI() {
        mainRoot = new BorderPane();
        mainRoot.setPadding(new Insets(20));

        // Set background image - with null check
        try {
            java.net.URL backgroundUrl = getClass().getResource("/assets/images/backgrounds/backgroundLeaderBoard.png");
            if (backgroundUrl != null) {
                String backgroundPath = backgroundUrl.toExternalForm();
                mainRoot.setStyle(
                    "-fx-background-image: url('" + backgroundPath + "');" +
                    "-fx-background-size: cover;" +
                    "-fx-background-position: center;" +
                    "-fx-background-repeat: no-repeat;"
                );
            } else {
                // Fallback to gradient background if image not found
                System.err.println("Warning: Background image not found, using gradient fallback");
                mainRoot.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e, #0f3460);"
                );
            }
        } catch (Exception e) {
            System.err.println("Error loading background image: " + e.getMessage());
            mainRoot.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e, #0f3460);"
            );
        }

        if (inRoom) {
            showRoomView();
        } else {
            showRoomBrowser();
        }
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;
        Scene scene = new Scene(mainRoot, width, height);
        stage.setScene(scene);
    }

    /**
     * Show the room browser UI
     */
    private void showRoomBrowser() {
        VBox topSection = createBrowserTitle();
        VBox centerSection = createRoomList();
        VBox rightSection = createFriendsSection();
        HBox bottomSection = createBrowserBottomActions();

        mainRoot.setTop(topSection);
        mainRoot.setCenter(centerSection);
        mainRoot.setRight(rightSection);
        mainRoot.setBottom(bottomSection);
    }

    /**
     * Show the room view UI
     */
    private void showRoomView() {
        VBox topSection = createRoomInfo();
        VBox centerSection = createPlayerSlots();
        VBox rightSection = createFriendsSection();
        HBox bottomSection = createBottomActions();

        mainRoot.setTop(topSection);
        mainRoot.setCenter(centerSection);
        mainRoot.setRight(rightSection);
        mainRoot.setBottom(bottomSection);

        updatePlayerSlots();
    }

    /**
     * Create browser title section
     */
    private VBox createBrowserTitle() {
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(20));
        titleBox.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.6);" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(255, 215, 0, 0.5);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 15px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 15, 0, 0, 5);"
        );

        Text title = new Text("ROOM BROWSER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.web("#FFD700")); // Gold color
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 5, 0, 0, 3);");

        Label subtitle = new Label("Choose a room to join or create your own");
        subtitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        subtitle.setTextFill(Color.web("#FFFFFF"));
        subtitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);");

        titleBox.getChildren().addAll(title, subtitle);
        return titleBox;
    }

    /**
     * Create room list display
     */
    private VBox createRoomList() {
        VBox listContainer = new VBox(15);
        listContainer.setAlignment(Pos.TOP_CENTER);
        listContainer.setPadding(new Insets(20));
        listContainer.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.5);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.6);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 12px;"
        );

        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER);

        Text roomsTitle = new Text("AVAILABLE ROOMS");
        roomsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        roomsTitle.setFill(Color.web("#3498db"));
        roomsTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0, 0, 2);");

        headerBox.getChildren().addAll(roomsTitle);

        ScrollPane roomsScroll = new ScrollPane();
        roomsScroll.setPrefHeight(400);
        roomsScroll.setFitToWidth(true);
        roomsScroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;"
        );

        roomsListBox = new VBox(10);
        roomsListBox.setPadding(new Insets(10));
        roomsListBox.setAlignment(Pos.TOP_CENTER);

        Label noRoomsLabel = new Label("No rooms available. Create one!");
        noRoomsLabel.setTextFill(Color.web("#FFFFFF"));
        noRoomsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        noRoomsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);");
        roomsListBox.getChildren().add(noRoomsLabel);

        roomsScroll.setContent(roomsListBox);

        listContainer.getChildren().addAll(headerBox, roomsScroll);
        return listContainer;
    }

    /**
     * Create bottom actions for browser
     */
    private HBox createBrowserBottomActions() {
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(20));

        Button backButton = new Button("BACK TO MENU");
        backButton.setPrefWidth(180);
        backButton.setPrefHeight(50);
        backButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        backButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #c0392b, #a93226);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        );
        backButton.setOnMouseEntered(e -> backButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 15, 0, 0, 5);"
        ));
        backButton.setOnMouseExited(e -> backButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #c0392b, #a93226);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        ));
        backButton.setOnAction(e -> {
            stopAutoRefresh();
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        Button createRoomButton = new Button("CREATE ROOM");
        createRoomButton.setPrefWidth(180);
        createRoomButton.setPrefHeight(50);
        createRoomButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        createRoomButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        );
        createRoomButton.setOnMouseEntered(e -> createRoomButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 15, 0, 0, 5);"
        ));
        createRoomButton.setOnMouseExited(e -> createRoomButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        ));
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
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(20));
        infoBox.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.6);" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(255, 215, 0, 0.5);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 15px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 15, 0, 0, 5);"
        );

        Text title = new Text("GAME LOBBY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.web("#FFD700"));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 5, 0, 0, 3);");

        infoBox.getChildren().add(title);
        return infoBox;
    }

    /**
     * Create player slots display
     */
    private VBox createPlayerSlots() {
        VBox slotsContainer = new VBox(15);
        slotsContainer.setAlignment(Pos.CENTER);
        slotsContainer.setPadding(new Insets(20));
        slotsContainer.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.5);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.6);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 12px;"
        );

        Text playersTitle = new Text("PLAYERS");
        playersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        playersTitle.setFill(Color.web("#3498db"));
        playersTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0, 0, 2);");

        playerSlotsBox = new VBox(10);
        playerSlotsBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 2; i++) {
            HBox playerSlot = createPlayerSlot(i);
            playerSlots[i] = playerSlot;
            playerSlotsBox.getChildren().add(playerSlot);
        }

        slotsContainer.getChildren().addAll(playersTitle, playerSlotsBox);
        return slotsContainer;
    }

    /**
     * Create a single player slot
     */
    private HBox createPlayerSlot(int slotIndex) {
        HBox slot = new HBox(15);
        slot.setAlignment(Pos.CENTER_LEFT);
        slot.setPadding(new Insets(15, 20, 15, 20));
        slot.setPrefWidth(350);
        slot.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.8);" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.6);" +
            "-fx-border-radius: 10px;" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 3);"
        );

        Label positionLabel = new Label("P" + (slotIndex + 1));
        positionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        positionLabel.setTextFill(Color.web("#FFD700"));
        positionLabel.setPrefWidth(50);
        positionLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(255, 215, 0, 0.7), 4, 0, 0, 2);");

        Label playerLabel = new Label("--- Waiting ---");
        playerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        playerLabel.setTextFill(Color.web("#FFFFFF"));
        playerLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");
        playerLabels[slotIndex] = playerLabel;

        // Ready indicator (for future use)
        Label readyLabel = new Label("");
        readyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        readyLabel.setTextFill(Color.web("#27ae60"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        slot.getChildren().addAll(positionLabel, playerLabel, spacer, readyLabel);
        return slot;
    }

    /**
     * Create friends section (right sidebar) with search and friend requests
     */
    private VBox createFriendsSection() {
        VBox friendsSection = new VBox(15);
        friendsSection.setPadding(new Insets(20));
        friendsSection.setPrefWidth(300);
        friendsSection.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.5);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(155, 89, 182, 0.6);" +
            "-fx-border-radius: 12px;" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        );

        Text friendsTitle = new Text("FRIENDS");
        friendsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        friendsTitle.setFill(Color.web("#9b59b6"));
        friendsTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0, 0, 2);");

        // Search users section
        VBox searchSection = new VBox(8);
        Label searchLabel = new Label("Find Friends");
        searchLabel.setTextFill(Color.web("#FFFFFF"));
        searchLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        searchLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

        HBox searchBox = new HBox(5);
        searchBox.setAlignment(Pos.CENTER);

        TextField searchField = new TextField();
        searchField.setPromptText("Search username...");
        searchField.setPrefWidth(180);
        searchField.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5);" +
            "-fx-background-radius: 5px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-radius: 5px;" +
            "-fx-padding: 8px;" +
            "-fx-font-size: 14px;"
        );

        Button searchButton = new Button("🔍");
        searchButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 5px;" +
            "-fx-padding: 8px 15px;"
        );
        searchButton.setOnMouseEntered(e -> searchButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #5dade2, #3498db);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 5px;" +
            "-fx-padding: 8px 15px;"
        ));
        searchButton.setOnMouseExited(e -> searchButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 5px;" +
            "-fx-padding: 8px 15px;"
        ));
        searchButton.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (searchTerm.length() >= 2) {
                network.sendMessage(new Message(MESSAGE_TYPE_SEARCH_USERS, searchTerm));
            }
        });

        searchBox.getChildren().addAll(searchField, searchButton);

        // Search results box
        VBox searchResultsBox = new VBox(5);
        searchResultsBox.setVisible(false);
        searchResultsBox.setManaged(false);
        searchResultsBox.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.8);" +
            "-fx-padding: 10px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.4);" +
            "-fx-border-radius: 8px;" +
            "-fx-border-width: 1px;"
        );

        searchSection.getChildren().addAll(searchLabel, searchBox, searchResultsBox);

        // Friend requests section
        VBox requestsSection = new VBox(8);
        Label requestsLabel = new Label("Friend Requests");
        requestsLabel.setTextFill(Color.web("#FFFFFF"));
        requestsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        requestsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

        VBox requestsListBox = new VBox(5);
        requestsListBox.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.6);" +
            "-fx-padding: 10px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(155, 89, 182, 0.4);" +
            "-fx-border-radius: 8px;" +
            "-fx-border-width: 1px;"
        );

        requestsSection.getChildren().addAll(requestsLabel, requestsListBox);

        // Friends list
        Label myFriendsLabel = new Label("My Friends");
        myFriendsLabel.setTextFill(Color.web("#FFFFFF"));
        myFriendsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        myFriendsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

        ScrollPane friendsScroll = new ScrollPane();
        friendsScroll.setPrefHeight(200);
        friendsScroll.setFitToWidth(true);
        friendsScroll.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;"
        );

        friendsListBox = new VBox(5);
        friendsListBox.setPadding(new Insets(10));
        friendsListBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.1);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(255, 255, 255, 0.2);" +
            "-fx-border-radius: 8px;" +
            "-fx-border-width: 1px;"
        );

        friendsScroll.setContent(friendsListBox);

        Label noFriendsLabel = new Label("No friends yet");
        noFriendsLabel.setTextFill(Color.web("#FFFFFF"));
        noFriendsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        noFriendsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");
        friendsListBox.getChildren().add(noFriendsLabel);

        friendsSection.getChildren().addAll(friendsTitle, searchSection,
                new javafx.scene.control.Separator(), requestsSection,
                new javafx.scene.control.Separator(), myFriendsLabel,
                friendsScroll);

        // Store references for later updates
        this.searchResultsBox = searchResultsBox;
        this.friendRequestsBox = requestsListBox;

        return friendsSection;
    }

    /**
     * Create bottom action buttons
     */
    private HBox createBottomActions() {
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(20));

        Button leaveButton = new Button("LEAVE ROOM");
        leaveButton.setPrefWidth(180);
        leaveButton.setPrefHeight(50);
        leaveButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        leaveButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #c0392b, #a93226);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        );
        leaveButton.setOnMouseEntered(e -> leaveButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 15, 0, 0, 5);"
        ));
        leaveButton.setOnMouseExited(e -> leaveButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #c0392b, #a93226);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        ));
        leaveButton.setOnAction(e -> {
            stopAutoRefresh();
            network.sendMessage(new Message(MESSAGE_TYPE_LEAVE_ROOM, currentRoomId));
            show(currentUsername, null, new ArrayList<>());
        });

        startGameButton = new Button("START GAME");
        startGameButton.setPrefWidth(180);
        startGameButton.setPrefHeight(50);
        startGameButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        startGameButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        );
        startGameButton.setOnMouseEntered(e -> startGameButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 15, 0, 0, 5);"
        ));
        startGameButton.setOnMouseExited(e -> startGameButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        ));
        startGameButton.setOnAction(e -> {
            stopAutoRefresh();
            network.sendMessage(new Message(MESSAGE_TYPE_START_GAME, currentRoomId));
        });
        startGameButton.setDisable(playersInRoom.size() < 2);

        // Only show start button for room creator
        if (playersInRoom.isEmpty() || !currentUsername.equals(playersInRoom.get(0))) {
            startGameButton.setVisible(false);
        }

        actions.getChildren().addAll(leaveButton, startGameButton);
        return actions;
    }

    /**
     * Update player slots display
     */
    private void updatePlayerSlots() {
        if (!inRoom || playerLabels == null || playerLabels[0] == null) {
            return;
        }

        boolean isCurrentUserHost = currentUsername.equals(currentRoomCreator);
        for (int i = 0; i < 2; i++) {
            Label label = playerLabels[i];
            HBox slot = playerSlots[i];

            // Remove all existing kick buttons
            slot.getChildren().removeIf(node -> node instanceof Button);

            if (i < playersInRoom.size()) {
                String playerName = playersInRoom.get(i);
                label.setText(playerName);
                if (playerName.equals(currentUsername)) {
                    label.setTextFill(Color.web("#f39c12"));
                    label.setStyle(
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, rgba(243, 156, 18, 0.6), 4, 0, 0, 2);"
                    );
                } else {
                    label.setTextFill(Color.WHITE);
                    label.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");
                }

                // Add kick button if current user is host and this player is not the host
                boolean isThisPlayerHost = playerName.equals(currentRoomCreator);
                System.out.println("Slot " + i + ": " + playerName + ", isHost=" + isThisPlayerHost);

                if (isCurrentUserHost && !isThisPlayerHost) {
                    System.out.println("  -> Adding KICK button for " + playerName);

                    Button kickButton = new Button("KICK");
                    kickButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                            "-fx-padding: 8px 15px; -fx-background-radius: 5px; " +
                            "-fx-border-color: #c0392b; -fx-border-width: 2px; -fx-border-radius: 5px;");
                    kickButton.setTooltip(new javafx.scene.control.Tooltip("Kick " + playerName + " from room"));

                    kickButton.setOnMouseEntered(e ->
                        kickButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                            "-fx-padding: 8px 15px; -fx-background-radius: 5px; " +
                            "-fx-border-color: #a93226; -fx-border-width: 2px; -fx-border-radius: 5px;"));

                    kickButton.setOnMouseExited(e ->
                        kickButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                            "-fx-padding: 8px 15px; -fx-background-radius: 5px; " +
                            "-fx-border-color: #c0392b; -fx-border-width: 2px; -fx-border-radius: 5px;"));

                    kickButton.setOnAction(e -> {
                        System.out.println("KICK button clicked for: " + playerName);
                        network.sendMessage(new Message(MESSAGE_TYPE_KICK_PLAYER, playerName + ";" + currentRoomId));
                    });

                    // Add button before the last element (readyLabel)
                    int insertIndex = slot.getChildren().size() - 1;
                    if (insertIndex < 0) insertIndex = 0;
                    slot.getChildren().add(insertIndex, kickButton);
                    System.out.println("  -> Button added at index " + insertIndex + ", total children: " + slot.getChildren().size());
                } else {
                    System.out.println("  -> No kick button (isHost=" + isCurrentUserHost + ", isThisPlayerHost=" + isThisPlayerHost + ")");
                }
            } else {
                label.setText("--- Waiting ---");
                label.setTextFill(Color.web("#FFFFFF"));
                label.setStyle(
                    "-fx-font-style: italic;" +
                    "-fx-opacity: 0.6;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);"
                );
            }
        }

        // Update start button
        if (startGameButton != null) {
            startGameButton.setDisable(playersInRoom.size() < 2);
        }
    }

    /**
     * Update friends list display
     */
    private void updateFriendsList() {
        if (friendsListBox == null) {
            return;
        }

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
        if (roomsListBox == null) {
            return;
        }

        roomsListBox.getChildren().clear();

        if (availableRooms.isEmpty()) {
            Label noRooms = new Label("No rooms available. Create one!");
            noRooms.setTextFill(Color.web("#FFFFFF"));
            noRooms.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            noRooms.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);");
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
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15, 20, 15, 20));
        item.setPrefWidth(600);
        item.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.8);" +
            "-fx-background-radius: 10px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.7);" +
            "-fx-border-radius: 10px;" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 3);"
        );

        VBox infoBox = new VBox(5);

        Label hostLabel = new Label("Host: " + room.get("creator"));
        hostLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        hostLabel.setTextFill(Color.web("#FFD700"));
        hostLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

        Label detailsLabel = new Label("Players: " + room.get("playerCount") + "/" + room.get("maxPlayers"));
        detailsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        detailsLabel.setTextFill(Color.web("#FFFFFF"));
        detailsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

        infoBox.getChildren().addAll(hostLabel, detailsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button joinButton = new Button("Request Join");
        joinButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        joinButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 10px 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);"
        );
        joinButton.setOnMouseEntered(e -> {
            if (!joinButton.isDisabled()) {
                joinButton.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60);" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-padding: 10px 20px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 8, 0, 0, 3);"
                );
            }
        });
        joinButton.setOnMouseExited(e -> {
            if (!joinButton.isDisabled()) {
                joinButton.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-padding: 10px 20px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);"
                );
            }
        });
        joinButton.setOnAction(e -> {
            String roomId = room.get("roomId");
            network.sendMessage(new Message(MESSAGE_TYPE_REQUEST_JOIN, roomId));
            joinButton.setDisable(true);
            joinButton.setText("Requested...");
            joinButton.setStyle(
                "-fx-background-color: #7f8c8d;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 10px 20px;"
            );
        });

        item.getChildren().addAll(infoBox, spacer, joinButton);
        return item;
    }

    // ==================== MESSAGE HANDLERS ====================

    /**
     * Handle room update (player joined/left)
     */
    public void handleRoomUpdate(Message message) {
        String data = message.getData();

        switch (message.getType()) {
            case MESSAGE_TYPE_S2C_ROOM_UPDATE:
                updateRoomFromJson(data);
                Platform.runLater(this::updateFriendsList);
                break;

            case MESSAGE_TYPE_PLAYER_JOINED:
                String[] joinParts = data.split(":");
                if (joinParts.length >= 1) {
                    String playerWhoJoined = joinParts[0];
                    if (!playersInRoom.contains(playerWhoJoined)) {
                        playersInRoom.add(playerWhoJoined);
                        Platform.runLater(() -> {
                            updatePlayerSlots();
                            updateFriendsList();
                        });
                    }
                }
                break;

            case MESSAGE_TYPE_PLAYER_LEFT:
                String[] leftParts = data.split(":");
                if (leftParts.length >= 1) {
                    String playerWhoLeft = leftParts[0];
                    playersInRoom.remove(playerWhoLeft);
                    Platform.runLater(() -> {
                        updatePlayerSlots();
                        updateFriendsList();
                    });
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
        network.sendMessage(new Message(MESSAGE_TYPE_JOIN_ROOM, roomId));
    }

    /**
     * Handle join request rejected
     */
    public void handleJoinRejected(Message message) {
        String reason = message.getData();
        UIHelper.showError("Join Rejected", reason);
        network.sendMessage(new Message(MESSAGE_TYPE_GET_ROOM_LIST, ""));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Update room info from JSON
     */
    private void updateRoomFromJson(String json) {
        System.out.println("=== UPDATE ROOM FROM JSON ===");
        System.out.println("JSON: " + json);

        RoomUpdate update = RoomUpdateHandler.parseRoomUpdate(json);
        currentRoomCreator = update.getCreator();

        playersInRoom.clear();
        playersInRoom.addAll(update.getPlayers());

        System.out.println("Room creator: " + currentRoomCreator);
        System.out.println("Players in room: " + playersInRoom);

        updatePlayerSlots();
    }

    /**
     * Parse friends list from JSON
     */
    private void parseFriendsList(String json) {
        friendsList.clear();

        // Note: This parses the old format without online status
        // For backward compatibility, we'll manually parse or use basic parsing
        if (JsonParser.isEmptyArray(json)) return;

        String[] friendObjects = JsonParser.splitJsonArray(json);
        for (String friendJson : friendObjects) {
            Map<String, String> friend = new HashMap<>();
            String username = JsonParser.extractString(friendJson, "username");
            String userId = String.valueOf(JsonParser.extractInt(friendJson, "user_id", 0));

            if (username != null) {
                friend.put("username", username);
                friend.put("user_id", userId);
                friendsList.add(friend);
            }
        }
    }

    /**
     * Parse rooms list from JSON
     */
    private void parseRoomsList(String json) {
        List<RoomInfo> rooms = RoomUpdateHandler.parseRoomsList(json);

        availableRooms.clear();
        for (RoomInfo room : rooms) {
            availableRooms.add(room.toMap());
        }
    }

    // ==================== FRIEND MANAGEMENT HANDLERS ====================

    /**
     * Handle search results
     */
    public void handleSearchResults(Message message) {
        if (searchResultsBox == null) {
            return;
        }

        String json = message.getData().toString();
        List<String> results = parseSimpleStringArray(json);

        searchResultsBox.getChildren().clear();

        if (results.isEmpty()) {
            // Create a more visible "no results" message
            VBox noResultsBox = new VBox(10);
            noResultsBox.setAlignment(Pos.CENTER);
            noResultsBox.setPadding(new Insets(20));
            noResultsBox.setStyle(
                "-fx-background-color: rgba(231, 76, 60, 0.2);" +
                "-fx-border-color: rgba(231, 76, 60, 0.5);" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
            );

            Label noResultsIcon = new Label("🔍");
            noResultsIcon.setFont(Font.font(32));

            Label noResults = new Label("No users found");
            noResults.setTextFill(Color.web("#FFFFFF"));
            noResults.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            noResults.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

            Label hint = new Label("Try searching with a different username");
            hint.setTextFill(Color.web("#CCCCCC"));
            hint.setFont(Font.font("Arial", 12));
            hint.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 1, 0, 0, 1);");

            noResultsBox.getChildren().addAll(noResultsIcon, noResults, hint);
            searchResultsBox.getChildren().add(noResultsBox);
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
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 10, 8, 10));
        item.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.7);" +
            "-fx-background-radius: 5px;" +
            "-fx-border-color: rgba(52, 152, 219, 0.4);" +
            "-fx-border-radius: 5px;" +
            "-fx-border-width: 1px;"
        );

        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0, 0, 1);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Check if already friend
        boolean isAlreadyFriend = friendsList.stream()
            .anyMatch(f -> username.equals(f.get("username")));

        // Check if already sent request
        boolean hasSentRequest = sentFriendRequests.contains(username);

        // Check if received request from this user
        boolean hasReceivedRequest = receivedFriendRequests.contains(username);

        Button addButton = new Button("➕");
        addButton.setFont(Font.font(14));

        // Disable and change appearance if already friend or has pending request
        if (isAlreadyFriend) {
            addButton.setText("✓ Friend");
            addButton.setDisable(true);
            addButton.setStyle(
                "-fx-background-color: #7f8c8d;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 5px 10px;"
            );
        } else if (hasSentRequest) {
            addButton.setText("Pending");
            addButton.setDisable(true);
            addButton.setStyle(
                "-fx-background-color: #95a5a6;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 5px 10px;"
            );
        } else if (hasReceivedRequest) {
            addButton.setText("Accept");
            addButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
                "-fx-text-fill: white;" +
                "-fx-cursor: hand;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 5px 10px;"
            );
            addButton.setOnAction(e -> {
                network.sendMessage(new Message(MESSAGE_TYPE_ACCEPT_FRIEND, username));
                addButton.setDisable(true);
                addButton.setText("✓");
            });
        } else {
            // Normal add friend button
            addButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
                "-fx-text-fill: white;" +
                "-fx-cursor: hand;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 5px 10px;"
            );
            addButton.setOnMouseEntered(e -> {
                if (!addButton.isDisabled()) {
                    addButton.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60);" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-padding: 5px 10px;"
                    );
                }
            });
            addButton.setOnMouseExited(e -> {
                if (!addButton.isDisabled()) {
                    addButton.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #27ae60, #229954);" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-padding: 5px 10px;"
                    );
                }
            });
            addButton.setOnAction(e -> {
                network.sendMessage(new Message(MESSAGE_TYPE_SEND_FRIEND_REQUEST, username));
                sentFriendRequests.add(username); // Track sent request
                addButton.setDisable(true);
                addButton.setText("Pending");
                addButton.setStyle(
                    "-fx-background-color: #95a5a6;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 5px;" +
                    "-fx-padding: 5px 10px;"
                );
            });
        }

        item.getChildren().addAll(nameLabel, spacer, addButton);
        return item;
    }

    /**
     * Handle friend requests list
     */
    public void handleFriendRequests(Message message) {
        if (friendRequestsBox == null) {
            return;
        }

        String json = message.getData();
        List<String> requests = parseSimpleStringArray(json);

        // Update received requests list
        receivedFriendRequests.clear();
        receivedFriendRequests.addAll(requests);

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
     * Handle friend request sent successfully
     */
    public void handleFriendRequestSent(Message message) {
        // Already tracked in sentFriendRequests list when button clicked
        System.out.println("Friend request sent: " + message.getData());
    }

    /**
     * Handle friend request failed
     */
    public void handleFriendRequestFailed(Message message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Friend Request Failed");
            alert.setHeaderText(null);
            alert.setContentText(message.getData());
            alert.showAndWait();
        });
    }

    /**
     * Handle received friend request from another user
     */
    public void handleFriendRequestReceived(Message message) {
        String fromUser = message.getData();
        if (!receivedFriendRequests.contains(fromUser)) {
            receivedFriendRequests.add(fromUser);
        }

        // Refresh friend requests list
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Friend Request");
            alert.setHeaderText(null);
            alert.setContentText(fromUser + " sent you a friend request!");
            alert.showAndWait();
        });
    }

    /**
     * Handle friend request accepted
     */
    public void handleFriendAccepted(Message message) {
        String data = message.getData();

        // Remove from sent requests if we sent it
        sentFriendRequests.removeIf(username -> data.contains(username));

        // Refresh friends list
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIENDS, ""));
        network.sendMessage(new Message(MESSAGE_TYPE_GET_FRIEND_REQUESTS, ""));

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Friend Added");
            alert.setHeaderText(null);
            alert.setContentText(data);
            alert.showAndWait();
        });
    }

    /**
     * Create friend request item with improved styling
     */
    private HBox createFriendRequestItem(String fromUser) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-color: rgba(155,89,182,0.3); " +
                "-fx-background-radius: 6px; " +
                "-fx-border-color: rgba(155,89,182,0.6); " +
                "-fx-border-radius: 6px; " +
                "-fx-border-width: 1.5px;");

        Label nameLabel = new Label(fromUser);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button acceptButton = new Button("Accept");
        acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-padding: 6px 12px; -fx-background-radius: 5px;");
        acceptButton.setOnMouseEntered(e ->
            acceptButton.setStyle("-fx-background-color: #229954; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-padding: 6px 12px; -fx-background-radius: 5px;"));
        acceptButton.setOnMouseExited(e ->
            acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-padding: 6px 12px; -fx-background-radius: 5px;"));
        acceptButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_ACCEPT_FRIEND, fromUser));
            friendRequestsBox.getChildren().remove(item);
        });

        Button rejectButton = new Button("Reject");
        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-padding: 6px 12px; -fx-background-radius: 5px;");
        rejectButton.setOnMouseEntered(e ->
            rejectButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-padding: 6px 12px; -fx-background-radius: 5px;"));
        rejectButton.setOnMouseExited(e ->
            rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-padding: 6px 12px; -fx-background-radius: 5px;"));
        rejectButton.setOnAction(e -> {
            network.sendMessage(new Message(MESSAGE_TYPE_REJECT_FRIEND, fromUser));
            friendRequestsBox.getChildren().remove(item);
        });

        item.getChildren().addAll(nameLabel, spacer, acceptButton, rejectButton);
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
        List<FriendInfo> friends = RoomUpdateHandler.parseFriendsList(json);

        friendsList.clear();
        for (FriendInfo friend : friends) {
            friendsList.add(friend.toMap());
        }
    }

    /**
     * Update friend list item to include invite button with improved styling
     */
    private HBox createFriendItem(Map<String, String> friend) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setStyle(
            "-fx-background-color: rgba(30, 30, 30, 0.7);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(155, 89, 182, 0.5);" +
            "-fx-border-radius: 8px;" +
            "-fx-border-width: 1.5px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 2);"
        );

        String username = friend.get("username");
        boolean isOnline = "true".equals(friend.get("online"));

        // Status indicator - larger and more visible
        Label statusLabel = new Label("●");
        statusLabel.setTextFill(isOnline ? Color.web("#27ae60") : Color.web("#95a5a6"));
        statusLabel.setFont(Font.font(18));
        statusLabel.setStyle("-fx-effect: dropshadow(gaussian, " +
            (isOnline ? "rgba(39, 174, 96, 0.9)" : "rgba(149, 165, 166, 0.6)") +
            ", 6, 0.7, 0, 0);");

        // Username - larger, bold, better contrast
        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 2);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Check if friend is already in the room
        boolean friendInRoom = playersInRoom.contains(username);

        // Show button only if in room
        if (inRoom && currentRoomId != null) {
            if (friendInRoom) {
                boolean isCurrentUserHost = currentUsername.equals(currentRoomCreator);
                boolean isFriendTheHost = username.equals(currentRoomCreator);

                if (isCurrentUserHost && !isFriendTheHost) {
                    Label inRoomLabel = new Label("In Room");
                    inRoomLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 11px; " +
                            "-fx-font-weight: bold; -fx-padding: 4px 8px; " +
                            "-fx-background-color: rgba(243, 156, 18, 0.2); " +
                            "-fx-background-radius: 4px; -fx-border-color: #f39c12; " +
                            "-fx-border-radius: 4px; -fx-border-width: 1px;");

                    item.getChildren().addAll(statusLabel, nameLabel, spacer, inRoomLabel);
                } else {
                    Label inRoomLabel = new Label(isFriendTheHost ? "Host" : "In Room");
                    inRoomLabel.setStyle("-fx-text-fill: " + (isFriendTheHost ? "#27ae60" : "#f39c12") + "; -fx-font-size: 11px; " +
                            "-fx-font-weight: bold; -fx-padding: 4px 8px; " +
                            "-fx-background-color: rgba(" + (isFriendTheHost ? "39, 174, 96" : "243, 156, 18") + ", 0.2); " +
                            "-fx-background-radius: 4px; -fx-border-color: " + (isFriendTheHost ? "#27ae60" : "#f39c12") + "; " +
                            "-fx-border-radius: 4px; -fx-border-width: 1px;");

                    item.getChildren().addAll(statusLabel, nameLabel, spacer, inRoomLabel);
                }
            } else {
                Button inviteButton = new Button("Invite");
                inviteButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-padding: 8px 15px; -fx-background-radius: 6px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.5), 4, 0, 0, 2);");
                inviteButton.setTooltip(new javafx.scene.control.Tooltip("Invite to your room"));
                inviteButton.setOnMouseEntered(e ->
                    inviteButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-padding: 8px 15px; -fx-background-radius: 6px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(41, 128, 185, 0.7), 6, 0, 0, 2);"));
                inviteButton.setOnMouseExited(e ->
                    inviteButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-padding: 8px 15px; -fx-background-radius: 6px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.5), 4, 0, 0, 2);"));
                inviteButton.setOnAction(e -> {
                    network.sendMessage(new Message(MESSAGE_TYPE_INVITE_TO_ROOM,
                            username + ";" + currentRoomId));
                });

                item.getChildren().addAll(statusLabel, nameLabel, spacer, inviteButton);
            }
        } else {
            item.getChildren().addAll(statusLabel, nameLabel, spacer);
        }

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

            boolean accept = UIHelper.showConfirm(
                    "Room Invitation",
                    inviterUsername + " invited you to join room: " + roomId + "\n\nAccept?"
            );

            if (accept) {
                network.sendMessage(new Message("JOIN_ROOM", roomId));
            }
        }
    }

    /**
     * Parse simple string array from JSON
     */
    private List<String> parseSimpleStringArray(String json) {
        List<String> result = new ArrayList<>();

        if (JsonParser.isEmptyArray(json)) return result;

        String arrayContent = json.substring(1, json.length() - 1);
        String[] items = JsonParser.splitArrayItems(arrayContent);

        for (String item : items) {
            if (!item.trim().isEmpty()) {
                result.add(item.trim());
            }
        }

        return result;
    }

    public void showCurrentRoom() {
        if (mainRoot != null && mainRoot.getScene() != null) {
            stage.setScene(mainRoot.getScene());
            stage.show();

            updatePlayerSlots();
        } else {
            if (currentUsername != null && currentRoomId != null) {
                show(currentUsername, currentRoomId, playersInRoom);
            } else {
                if (onBackToMenu != null) onBackToMenu.run();
            }
        }
    }
}

