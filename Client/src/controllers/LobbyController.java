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
import models.Message;
import network.NetworkManager;
import utils.UIHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this.currentUsername = username;
        this.currentRoomId = roomId;
        this.playersInRoom = new ArrayList<>(initialPlayers);
        this.inRoom = (roomId != null && !roomId.isEmpty());

        createLobbyUI();

        // Request friend list and room list from server
        network.sendMessage(new Message("C2S_GET_FRIENDS", ""));
        network.sendMessage(new Message("C2S_GET_FRIEND_REQUESTS", ""));
        if (!inRoom) {
            network.sendMessage(new Message("C2S_GET_ROOM_LIST", ""));
        }
    }

    /**
     * Create lobby UI
     */
    private void createLobbyUI() {
        mainRoot = new BorderPane();
        mainRoot.setPadding(new Insets(20));
        mainRoot.setStyle(UIHelper.createGradientBackground("#2c3e50", "#34495e"));

        if (inRoom) {
            // Show room view (current implementation)
            showRoomView();
        } else {
            // Show room browser
            showRoomBrowser();
        }

        Scene scene = new Scene(mainRoot, 1000, 700);
        stage.setScene(scene);
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
        // Top: Room info
        VBox topSection = createRoomInfo();

        // Center: Players display
        VBox centerSection = createPlayerSlots();

        // Right: Friends list and actions
        VBox rightSection = createFriendsSection();

        // Bottom: Actions
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

        Text title = new Text("🏠 ROOM BROWSER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.WHITE);

        Label subtitle = new Label("Choose a room to join or create your own");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#95a5a6"));

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

        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER);

        Text roomsTitle = new Text("📋 AVAILABLE ROOMS");
        roomsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        roomsTitle.setFill(Color.WHITE);

        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> {
            network.sendMessage(new Message("C2S_GET_ROOM_LIST", ""));
        });

        headerBox.getChildren().addAll(roomsTitle, refreshButton);

        ScrollPane roomsScroll = new ScrollPane();
        roomsScroll.setPrefHeight(400);
        roomsScroll.setFitToWidth(true);
        roomsScroll.setStyle("-fx-background-color: transparent;");

        roomsListBox = new VBox(10);
        roomsListBox.setPadding(new Insets(10));
        roomsListBox.setAlignment(Pos.TOP_CENTER);

        Label noRoomsLabel = new Label("No rooms available. Create one!");
        noRoomsLabel.setTextFill(Color.web("#95a5a6"));
        noRoomsLabel.setFont(Font.font("Arial", 14));
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

        Button backButton = UIHelper.createButton("🔙 BACK TO MENU", UIHelper.DANGER_COLOR);
        backButton.setOnAction(e -> {
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        Button createRoomButton = UIHelper.createButton("➕ CREATE ROOM", UIHelper.PRIMARY_COLOR);
        createRoomButton.setOnAction(e -> {
            network.sendMessage(new Message("CREATE_ROOM", ""));
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

        Text title = new Text("🏠 GAME LOBBY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.WHITE);

        HBox roomInfo = new HBox(10);
        roomInfo.setAlignment(Pos.CENTER);

        Label roomLabel = new Label("Room ID:");
        roomLabel.setFont(Font.font("Arial", 14));
        roomLabel.setTextFill(Color.web("#95a5a6"));

        roomIdLabel = new Label(currentRoomId);
        roomIdLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        roomIdLabel.setTextFill(Color.web("#3498db"));

        Button copyButton = new Button("📋 Copy");
        copyButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-cursor: hand;");
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
        VBox slotsContainer = new VBox(15);
        slotsContainer.setAlignment(Pos.CENTER);
        slotsContainer.setPadding(new Insets(20));

        Text playersTitle = new Text("👥 PLAYERS");
        playersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        playersTitle.setFill(Color.WHITE);

        playerSlotsBox = new VBox(10);
        playerSlotsBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 4; i++) {
            HBox playerSlot = createPlayerSlot(i);
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
        slot.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                "-fx-background-radius: 10px;");

        Label positionLabel = new Label("P" + (slotIndex + 1));
        positionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        positionLabel.setTextFill(Color.web("#3498db"));
        positionLabel.setPrefWidth(40);

        Label playerLabel = new Label("--- Empty ---");
        playerLabel.setFont(Font.font("Arial", 16));
        playerLabel.setTextFill(Color.web("#95a5a6"));
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
        friendsSection.setPrefWidth(280);
        friendsSection.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); " +
                "-fx-background-radius: 10px;");

        Text friendsTitle = new Text("👥 FRIENDS");
        friendsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        friendsTitle.setFill(Color.WHITE);

        // Search users section
        VBox searchSection = new VBox(8);
        Label searchLabel = new Label("🔍 Find Friends");
        searchLabel.setTextFill(Color.WHITE);
        searchLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        HBox searchBox = new HBox(5);
        searchBox.setAlignment(Pos.CENTER);

        TextField searchField = new TextField();
        searchField.setPromptText("Search username...");
        searchField.setPrefWidth(180);

        Button searchButton = new Button("🔍");
        searchButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-cursor: hand;");
        searchButton.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (searchTerm.length() >= 2) {
                network.sendMessage(new Message("C2S_SEARCH_USERS", searchTerm));
            }
        });

        searchBox.getChildren().addAll(searchField, searchButton);

        // Search results box (hidden by default)
        VBox searchResultsBox = new VBox(5);
        searchResultsBox.setVisible(false);
        searchResultsBox.setManaged(false);
        searchResultsBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-padding: 10px; -fx-background-radius: 5px;");

        searchSection.getChildren().addAll(searchLabel, searchBox, searchResultsBox);

        // Friend requests section
        VBox requestsSection = new VBox(8);
        Label requestsLabel = new Label("📩 Friend Requests");
        requestsLabel.setTextFill(Color.WHITE);
        requestsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        VBox requestsListBox = new VBox(5);
        requestsListBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); " +
                "-fx-padding: 8px; -fx-background-radius: 5px;");

        Button refreshRequestsButton = new Button("🔄 Check Requests");
        refreshRequestsButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5px 10px;");
        refreshRequestsButton.setOnAction(e -> {
            network.sendMessage(new Message("C2S_GET_FRIEND_REQUESTS", ""));
        });

        requestsSection.getChildren().addAll(requestsLabel, refreshRequestsButton, requestsListBox);

        // Friends list
        Label myFriendsLabel = new Label("💚 My Friends");
        myFriendsLabel.setTextFill(Color.WHITE);
        myFriendsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        ScrollPane friendsScroll = new ScrollPane();
        friendsScroll.setPrefHeight(200);
        friendsScroll.setFitToWidth(true);
        friendsScroll.setStyle("-fx-background-color: transparent;");

        friendsListBox = new VBox(5);
        friendsListBox.setPadding(new Insets(10));
        friendsListBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); " +
                "-fx-background-radius: 5px;");

        friendsScroll.setContent(friendsListBox);

        Label noFriendsLabel = new Label("No friends yet");
        noFriendsLabel.setTextFill(Color.web("#95a5a6"));
        noFriendsLabel.setFont(Font.font("Arial", 12));
        friendsListBox.getChildren().add(noFriendsLabel);

        Button refreshFriendsButton = new Button("🔄 Refresh");
        refreshFriendsButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5px 10px;");
        refreshFriendsButton.setOnAction(e -> {
            network.sendMessage(new Message("C2S_GET_FRIENDS", ""));
        });

        friendsSection.getChildren().addAll(friendsTitle, searchSection,
                new javafx.scene.control.Separator(), requestsSection,
                new javafx.scene.control.Separator(), myFriendsLabel,
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
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(20));

        Button leaveButton = UIHelper.createButton("🚪 LEAVE ROOM", UIHelper.DANGER_COLOR);
        leaveButton.setOnAction(e -> {
            network.sendMessage(new Message("LEAVE_ROOM", currentRoomId));
            // Navigate back to menu
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });

        startGameButton = UIHelper.createButton("🎮 START GAME", UIHelper.PRIMARY_COLOR);
        startGameButton.setOnAction(e -> {
            network.sendMessage(new Message("C2S_START_GAME", currentRoomId));
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
        for (int i = 0; i < 4; i++) {
            Label label = playerLabels[i];
            if (i < playersInRoom.size()) {
                String playerName = playersInRoom.get(i);
                label.setText(playerName);
                label.setTextFill(Color.WHITE);

                if (playerName.equals(currentUsername)) {
                    label.setStyle("-fx-font-weight: bold; -fx-text-fill: #f39c12;");
                }
            } else {
                label.setText("--- Empty ---");
                label.setTextFill(Color.web("#95a5a6"));
                label.setStyle("");
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
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15, 20, 15, 20));
        item.setPrefWidth(600);
        item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: #3498db; " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 2px;");

        VBox infoBox = new VBox(5);

        Label roomIdLabel = new Label("🏠 " + room.get("roomId"));
        roomIdLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        roomIdLabel.setTextFill(Color.WHITE);

        Label detailsLabel = new Label("Host: " + room.get("creator") +
                " • Players: " + room.get("playerCount") + "/" + room.get("maxPlayers"));
        detailsLabel.setFont(Font.font("Arial", 12));
        detailsLabel.setTextFill(Color.web("#95a5a6"));

        infoBox.getChildren().addAll(roomIdLabel, detailsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button joinButton = new Button("📩 Request Join");
        joinButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-cursor: hand; " +
                "-fx-padding: 10px 20px;");
        joinButton.setOnAction(e -> {
            String roomId = room.get("roomId");
            network.sendMessage(new Message("C2S_REQUEST_JOIN", roomId));
            joinButton.setDisable(true);
            joinButton.setText("⏳ Requested...");
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
            case "S2C_ROOM_UPDATE":
                updateRoomFromJson(data);
                break;

            case "PLAYER_JOINED":
            case "PLAYER_LEFT":
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
                network.sendMessage(new Message("JOIN_ROOM", roomId));
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
                network.sendMessage(new Message("C2S_ACCEPT_JOIN", requesterUsername + ";" + roomId));
            } else {
                network.sendMessage(new Message("C2S_REJECT_JOIN", requesterUsername + ";" + roomId));
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
        network.sendMessage(new Message("JOIN_ROOM", roomId));
    }

    /**
     * Handle join request rejected
     */
    public void handleJoinRejected(Message message) {
        String reason = message.getData();
        UIHelper.showError("Join Rejected", reason);
        // Refresh room list
        network.sendMessage(new Message("C2S_GET_ROOM_LIST", ""));
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
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(5));
        item.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-background-radius: 3px;");

        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", 13));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("➕");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 3px 8px;");
        addButton.setOnAction(e -> {
            network.sendMessage(new Message("C2S_SEND_FRIEND_REQUEST", username));
            addButton.setDisable(true);
            addButton.setText("✓");
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

        Button acceptButton = new Button("✓ Accept");
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
            network.sendMessage(new Message("C2S_ACCEPT_FRIEND", fromUser));
            friendRequestsBox.getChildren().remove(item);
        });

        Button rejectButton = new Button("✗ Reject");
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
            network.sendMessage(new Message("C2S_REJECT_FRIEND", fromUser));
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
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.15); " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");

        String username = friend.get("username");
        boolean isOnline = "true".equals(friend.get("online"));

        // Status indicator - larger and more visible
        Label statusLabel = new Label("●");
        statusLabel.setTextFill(isOnline ? Color.web("#27ae60") : Color.web("#95a5a6"));
        statusLabel.setFont(Font.font(16));
        statusLabel.setStyle("-fx-effect: dropshadow(gaussian, " +
            (isOnline ? "rgba(39, 174, 96, 0.8)" : "rgba(149, 165, 166, 0.5)") +
            ", 4, 0.5, 0, 0);");

        // Username - larger, bold, better contrast
        Label nameLabel = new Label(username);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Invite button (only show if in room)
        if (inRoom && currentRoomId != null) {
            Button inviteButton = new Button("📧 Invite");
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
                network.sendMessage(new Message("C2S_INVITE_TO_ROOM",
                        username + ";" + currentRoomId));
                UIHelper.showInfo("Invited", "Invitation sent to " + username);
            });

            item.getChildren().addAll(statusLabel, nameLabel, spacer, inviteButton);
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

        if (json.equals("[]")) return result;

        json = json.substring(1, json.length() - 1); // Remove [ ]

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

