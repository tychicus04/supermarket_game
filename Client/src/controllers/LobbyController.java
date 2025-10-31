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

    // UI Components
    private Label roomIdLabel;
    private VBox playerSlotsBox;
    private VBox friendsListBox;
    private Button startGameButton;
    private Label[] playerLabels; // P1, P2, P3, P4

    private Runnable onGameStart;

    public LobbyController(Stage stage, Runnable onGameStart) {
        this.stage = stage;
        this.network = NetworkManager.getInstance();
        this.onGameStart = onGameStart;
        this.playersInRoom = new ArrayList<>();
        this.friendsList = new ArrayList<>();
        this.playerLabels = new Label[4];
    }

    public void show(String username, String roomId, List<String> initialPlayers) {
        this.currentUsername = username;
        this.currentRoomId = roomId;
        this.playersInRoom = new ArrayList<>(initialPlayers);

        createLobbyUI();

        // Request friend list from server
        network.sendMessage(new Message("C2S_GET_FRIEND_LIST", ""));
    }

    /**
     * Create lobby UI
     */
    private void createLobbyUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle(UIHelper.createGradientBackground("#2c3e50", "#34495e"));

        // Top: Room info
        VBox topSection = createRoomInfo();

        // Center: Players display
        VBox centerSection = createPlayerSlots();

        // Right: Friends list and actions
        VBox rightSection = createFriendsSection();

        // Bottom: Actions
        HBox bottomSection = createBottomActions();

        root.setTop(topSection);
        root.setCenter(centerSection);
        root.setRight(rightSection);
        root.setBottom(bottomSection);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);

        updatePlayerSlots();
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
     * Create friends section (right sidebar)
     */
    private VBox createFriendsSection() {
        VBox friendsSection = new VBox(15);
        friendsSection.setPadding(new Insets(20));
        friendsSection.setPrefWidth(250);
        friendsSection.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); " +
                "-fx-background-radius: 10px;");

        Text friendsTitle = new Text("👥 FRIENDS");
        friendsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        friendsTitle.setFill(Color.WHITE);

        // Add friend section
        HBox addFriendBox = new HBox(5);
        addFriendBox.setAlignment(Pos.CENTER);

        TextField friendUsernameField = new TextField();
        friendUsernameField.setPromptText("Username");
        friendUsernameField.setPrefWidth(150);

        Button addButton = new Button("➕");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-cursor: hand;");
        addButton.setOnAction(e -> {
            String targetUsername = friendUsernameField.getText().trim();
            if (!targetUsername.isEmpty()) {
                network.sendMessage(new Message("C2S_FRIEND_REQUEST", targetUsername));
                friendUsernameField.clear();
                UIHelper.showInfo("Sent", "Friend request sent to " + targetUsername);
            }
        });

        addFriendBox.getChildren().addAll(friendUsernameField, addButton);

        // Friends list
        ScrollPane friendsScroll = new ScrollPane();
        friendsScroll.setPrefHeight(300);
        friendsScroll.setFitToWidth(true);
        friendsScroll.setStyle("-fx-background-color: transparent;");

        friendsListBox = new VBox(5);
        friendsListBox.setPadding(new Insets(10));

        friendsScroll.setContent(friendsListBox);

        Label noFriendsLabel = new Label("No friends yet");
        noFriendsLabel.setTextFill(Color.web("#95a5a6"));
        noFriendsLabel.setFont(Font.font("Arial", 12));
        friendsListBox.getChildren().add(noFriendsLabel);

        friendsSection.getChildren().addAll(friendsTitle, addFriendBox, friendsScroll);
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
            // Go back to menu (handled by Main.java)
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
     * Create friend list item with invite button
     */
    private HBox createFriendItem(Map<String, String> friend) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8));
        item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); " +
                "-fx-background-radius: 5px;");

        Label nameLabel = new Label(friend.get("username"));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status indicator (online/offline - for future implementation)
        Label statusLabel = new Label("●");
        statusLabel.setTextFill(Color.web("#95a5a6")); // Gray = unknown status
        statusLabel.setFont(Font.font(10));

        Button inviteButton = new Button("📧");
        inviteButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-cursor: hand; " +
                "-fx-padding: 5px 10px;");
        inviteButton.setTooltip(new Tooltip("Invite to room"));
        inviteButton.setOnAction(e -> {
            String friendUserId = friend.get("user_id");
            network.sendMessage(new Message("C2S_INVITE_TO_ROOM",
                    friendUserId + ";" + currentRoomId));
            UIHelper.showInfo("Invited", "Invitation sent to " + friend.get("username"));
        });

        item.getChildren().addAll(nameLabel, spacer, statusLabel, inviteButton);
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
}