package controllers;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Message;
import network.NetworkManager;
import utils.AssetManager;
import utils.SoundManager;
import utils.UIHelper;

import java.util.*;

/**
 * Drag-and-Drop Game Controller
 * Client-side rendering and input handling only
 * All game logic runs on server
 */
public class DragDropGameController {
    private Stage stage;
    private Runnable onGameEnd;
    private NetworkManager network;
    private AssetManager assets;
    private SoundManager sound;

    // UI Components
    private Label gameTimeLabel;
    private Map<String, Label> playerScoreLabels;
    private Map<String, Label> playerComboLabels;
    private VBox[] customerSlots; // 3 slots for customers
    private Pane itemSpawnArea;
    private StackPane gameRoot;

    // Client-side cache
    private Map<String, ImageView> itemViews; // itemID -> ImageView
    private Map<String, String> slotCustomerIds; // slotIndex -> customerID

    private String currentUsername;
    private String currentRoomId;
    private boolean isGameActive = false;

    public DragDropGameController(Stage stage, Runnable onGameEnd) {
        this.stage = stage;
        this.onGameEnd = onGameEnd;
        this.network = NetworkManager.getInstance();
        this.assets = AssetManager.getInstance();
        this.sound = SoundManager.getInstance();
        this.itemViews = new HashMap<>();
        this.slotCustomerIds = new HashMap<>();
        this.playerScoreLabels = new HashMap<>();
        this.playerComboLabels = new HashMap<>();
    }

    public void show(String username, String roomId, List<String> allPlayers) {
        this.currentUsername = username;
        this.currentRoomId = roomId;
        this.isGameActive = true;

        createGameUI(allPlayers);

        sound.playGameStart();
        sound.playMusic("gameplay_music");
    }

    /**
     * Create drag-and-drop game UI
     */
    private void createGameUI(List<String> allPlayers) {
        gameRoot = new StackPane();

        // Background
        ImageView background = new ImageView();
        Image bgImage = assets.getImage("bg_game");
        if (bgImage != null) {
            background.setImage(bgImage);
            background.setFitWidth(900);
            background.setFitHeight(700);
        }

        // Main layout
        BorderPane mainLayout = new BorderPane();

        // Top bar: Game time + Player stats
        VBox topSection = createTopSection(allPlayers);

        // Center: Customers + Item spawn area
        VBox centerSection = createCenterSection();

        mainLayout.setTop(topSection);
        mainLayout.setCenter(centerSection);

        gameRoot.getChildren().addAll(background, mainLayout);

        Scene scene = new Scene(gameRoot, 900, 700);
        stage.setScene(scene);
    }

    /**
     * Create top section with time and player stats
     */
    private VBox createTopSection(List<String> allPlayers) {
        VBox topSection = new VBox(10);
        topSection.setPadding(new Insets(15));
        topSection.setStyle("-fx-background-color: rgba(50, 50, 50, 0.9);");

        // Game time
        HBox timeBox = new HBox(10);
        timeBox.setAlignment(Pos.CENTER);

        Label timeIcon = new Label("⏰");
        timeIcon.setFont(Font.font(24));
        timeIcon.setTextFill(Color.WHITE);

        gameTimeLabel = new Label("180s");
        gameTimeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        gameTimeLabel.setTextFill(Color.web("#ffe66d"));

        timeBox.getChildren().addAll(timeIcon, gameTimeLabel);

        // Players (4 slots: P1, P2, P3, P4)
        HBox playersBox = new HBox(20);
        playersBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 4; i++) {
            String playerName = i < allPlayers.size() ? allPlayers.get(i) : "---";
            VBox playerBox = createPlayerBox(playerName, i + 1);
            playersBox.getChildren().add(playerBox);
        }

        topSection.getChildren().addAll(timeBox, playersBox);
        return topSection;
    }

    /**
     * Create player info box
     */
    private VBox createPlayerBox(String playerName, int playerNum) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                "-fx-background-radius: 10px;");

        Label pLabel = new Label("P" + playerNum);
        pLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        pLabel.setTextFill(Color.web("#95a5a6"));

        Label nameLabel = new Label(playerName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.WHITE);

        Label scoreLabel = new Label("0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        scoreLabel.setTextFill(Color.web("#27ae60"));
        playerScoreLabels.put(playerName, scoreLabel);

        Label comboLabel = new Label("x1");
        comboLabel.setFont(Font.font("Arial", 12));
        comboLabel.setTextFill(Color.web("#e74c3c"));
        playerComboLabels.put(playerName, comboLabel);

        box.getChildren().addAll(pLabel, nameLabel, scoreLabel, comboLabel);
        return box;
    }

    /**
     * Create center section with customers and items
     */
    private VBox createCenterSection() {
        VBox centerSection = new VBox(20);
        centerSection.setAlignment(Pos.CENTER);
        centerSection.setPadding(new Insets(20));

        // Customer area (3 slots)
        HBox customerArea = createCustomerArea();

        // Item spawn area
        itemSpawnArea = new Pane();
        itemSpawnArea.setPrefSize(800, 400);
        itemSpawnArea.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                "-fx-background-radius: 15px;");

        centerSection.getChildren().addAll(customerArea, itemSpawnArea);
        return centerSection;
    }

    /**
     * Create customer slots (drop targets)
     */
    private HBox createCustomerArea() {
        HBox customerArea = new HBox(20);
        customerArea.setAlignment(Pos.CENTER);
        customerArea.setPadding(new Insets(10));

        customerSlots = new VBox[3];

        for (int i = 0; i < 3; i++) {
            customerSlots[i] = createCustomerSlot(i);
            customerArea.getChildren().add(customerSlots[i]);
        }

        return customerArea;
    }

    /**
     * Create a single customer slot (drop target)
     */
    private VBox createCustomerSlot(int slotIndex) {
        VBox slot = new VBox(10);
        slot.setAlignment(Pos.CENTER);
        slot.setPadding(new Insets(15));
        slot.setPrefWidth(200);
        slot.setPrefHeight(180);
        slot.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); " +
                "-fx-background-radius: 15px; " +
                "-fx-border-color: #3498db; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 15px;");
        slot.setVisible(false); // Hidden by default

        // Customer image
        ImageView customerImg = new ImageView();
        customerImg.setFitWidth(60);
        customerImg.setFitHeight(60);
        customerImg.setPreserveRatio(true);

        // Request label
        Label requestLabel = new Label("");
        requestLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // Progress bar
        ProgressBar progressBar = new ProgressBar(1.0);
        progressBar.setPrefWidth(180);
        progressBar.setStyle("-fx-accent: #4CAF50;");

        // Timer label
        Label timerLabel = new Label("15s");
        timerLabel.setFont(Font.font("Arial", 12));

        slot.getChildren().addAll(customerImg, requestLabel, progressBar, timerLabel);

        // Set up drop target
        setupDropTarget(slot, slotIndex);

        return slot;
    }

    /**
     * Set up slot as drop target
     */
    private void setupDropTarget(VBox slot, int slotIndex) {
        slot.setOnDragOver(event -> {
            if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        slot.setOnDragEntered(event -> {
            if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
                slot.setStyle(slot.getStyle().replace("rgba(255, 255, 255, 0.8)",
                        "rgba(100, 200, 255, 0.9)"));
            }
            event.consume();
        });

        slot.setOnDragExited(event -> {
            slot.setStyle(slot.getStyle().replace("rgba(100, 200, 255, 0.9)",
                    "rgba(255, 255, 255, 0.8)"));
            event.consume();
        });

        slot.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                String itemID = db.getString();
                String customerID = slotCustomerIds.get(String.valueOf(slotIndex));

                if (customerID != null) {
                    // Send action to server
                    network.sendMessage(new Message("C2S_GAME_ACTION_DROP",
                            itemID + ";" + customerID));

                    sound.playPickup();
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Handle game state update from server
     */
    public void handleGameStateUpdate(Message message) {
        if (!isGameActive) return;

        String jsonData = message.getData();

        Platform.runLater(() -> {
            try {
                updateGameState(jsonData);
            } catch (Exception e) {
                System.err.println("❌ Error updating game state: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Parse and update game state (simplified JSON parsing)
     */
    private void updateGameState(String json) {
        // Update game time
        int timeRemaining = extractIntValue(json, "timeRemaining");
        gameTimeLabel.setText(timeRemaining + "s");

        if (timeRemaining <= 10) {
            gameTimeLabel.setTextFill(Color.web("#e74c3c"));
        }

        // Update players' scores and combos
        updatePlayerStats(json);

        // Update customers
        updateCustomers(json);

        // Update items
        updateItems(json);
    }

    /**
     * Update player statistics
     */
    private void updatePlayerStats(String json) {
        // Extract scores object
        String scoresSection = extractSection(json, "scores");
        String combosSection = extractSection(json, "combos");

        for (String playerName : playerScoreLabels.keySet()) {
            // Update score
            String scorePattern = "\"" + playerName + "\":";
            int score = extractValueAfterPattern(scoresSection, scorePattern);
            Label scoreLabel = playerScoreLabels.get(playerName);
            if (scoreLabel != null) {
                scoreLabel.setText(String.valueOf(score));
            }

            // Update combo
            int combo = extractValueAfterPattern(combosSection, scorePattern);
            Label comboLabel = playerComboLabels.get(playerName);
            if (comboLabel != null) {
                comboLabel.setText("x" + (combo + 1));
            }
        }
    }

    /**
     * Update customer slots
     */
    private void updateCustomers(String json) {
        String customersArray = extractArray(json, "customers");
        List<String> customerJsons = splitArray(customersArray);

        // Clear slot mapping
        slotCustomerIds.clear();

        // Hide all slots first
        for (VBox slot : customerSlots) {
            if (customerJsons.isEmpty()) {
                slot.setVisible(false);
            }
        }

        // Update each customer
        for (String customerJson : customerJsons) {
            int slot = extractIntValue(customerJson, "slot");
            String id = extractStringValue(customerJson, "id");
            String item = extractStringValue(customerJson, "item");
            double timeRemaining = extractDoubleValue(customerJson, "timeRemaining");
            double timeMax = extractDoubleValue(customerJson, "timeMax");
            String mood = extractStringValue(customerJson, "mood");

            if (slot >= 0 && slot < 3) {
                updateCustomerSlot(slot, id, item, timeRemaining, timeMax, mood);
            }
        }
    }

    /**
     * Update a specific customer slot
     */
    private void updateCustomerSlot(int slotIndex, String customerID, String requestItem,
                                    double timeRemaining, double timeMax, String mood) {
        VBox slot = customerSlots[slotIndex];
        slot.setVisible(true);

        // Store customer ID for this slot
        slotCustomerIds.put(String.valueOf(slotIndex), customerID);

        // Update customer image
        ImageView customerImg = (ImageView) slot.getChildren().get(0);
        Image moodImage = assets.getImage("customer_" + mood);
        if (moodImage != null) {
            customerImg.setImage(moodImage);
        }

        // Update request label
        Label requestLabel = (Label) slot.getChildren().get(1);
        requestLabel.setText(getEmojiForItem(requestItem) + " " + requestItem);

        // Update progress bar
        ProgressBar progressBar = (ProgressBar) slot.getChildren().get(2);
        double progress = timeRemaining / timeMax;
        progressBar.setProgress(progress);

        // Color based on time
        if (progress < 0.3) {
            progressBar.setStyle("-fx-accent: #e74c3c;");
        } else if (progress < 0.6) {
            progressBar.setStyle("-fx-accent: #f39c12;");
        } else {
            progressBar.setStyle("-fx-accent: #4CAF50;");
        }

        // Update timer
        Label timerLabel = (Label) slot.getChildren().get(3);
        timerLabel.setText((int)Math.ceil(timeRemaining) + "s");
    }

    /**
     * Update items in spawn area
     */
    private void updateItems(String json) {
        String itemsArray = extractArray(json, "items");
        List<String> itemJsons = splitArray(itemsArray);

        Set<String> activeItemIds = new HashSet<>();

        // Update/create items
        for (String itemJson : itemJsons) {
            String id = extractStringValue(itemJson, "id");
            String name = extractStringValue(itemJson, "name");
            double x = extractDoubleValue(itemJson, "x");
            double y = extractDoubleValue(itemJson, "y");

            activeItemIds.add(id);

            ImageView itemView = itemViews.get(id);
            if (itemView == null) {
                // Create new item
                itemView = createItemView(id, name, x, y);
                itemViews.put(id, itemView);
                itemSpawnArea.getChildren().add(itemView);

                // Spawn animation
                animateItemSpawn(itemView);
            } else {
                // Update position if needed
                updateItemPosition(itemView, x, y);
            }
        }

        // Remove items not in server state
        Set<String> toRemove = new HashSet<>();
        for (String id : itemViews.keySet()) {
            if (!activeItemIds.contains(id)) {
                toRemove.add(id);
            }
        }

        for (String id : toRemove) {
            ImageView view = itemViews.remove(id);
            if (view != null) {
                itemSpawnArea.getChildren().remove(view);
            }
        }
    }

    /**
     * Create draggable item view
     */
    private ImageView createItemView(String itemID, String itemName, double x, double y) {
        ImageView itemView = new ImageView();
        itemView.setFitWidth(50);
        itemView.setFitHeight(50);
        itemView.setPreserveRatio(true);

        // Try to load image
        Image itemImage = assets.getItemImage(itemName.toLowerCase());
        if (itemImage != null) {
            itemView.setImage(itemImage);
        } else {
            // Fallback: create label with emoji
            // (For simplicity, we'll skip this in ImageView)
        }

        // Position (convert normalized 0-1 to pixel coordinates)
        double pixelX = x * itemSpawnArea.getPrefWidth();
        double pixelY = y * itemSpawnArea.getPrefHeight();
        itemView.setLayoutX(pixelX - 25); // Center
        itemView.setLayoutY(pixelY - 25);

        // Tooltip
        Tooltip.install(itemView, new Tooltip(itemName));

        // Set up dragging
        setupDraggable(itemView, itemID);

        return itemView;
    }

    /**
     * Set up item as draggable
     */
    private void setupDraggable(ImageView itemView, String itemID) {
        itemView.setOnDragDetected(event -> {
            Dragboard db = itemView.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            content.putString(itemID);
            db.setContent(content);

            // Drag image
            db.setDragView(itemView.getImage(), 25, 25);

            event.consume();
        });

        itemView.setOnDragDone(event -> {
            event.consume();
        });
    }

    /**
     * Update item position (smooth transition)
     */
    private void updateItemPosition(ImageView itemView, double x, double y) {
        double pixelX = x * itemSpawnArea.getPrefWidth() - 25;
        double pixelY = y * itemSpawnArea.getPrefHeight() - 25;

        // Only animate if position changed significantly
        double dx = Math.abs(itemView.getLayoutX() - pixelX);
        double dy = Math.abs(itemView.getLayoutY() - pixelY);

        if (dx > 5 || dy > 5) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), itemView);
            tt.setToX(pixelX - itemView.getLayoutX());
            tt.setToY(pixelY - itemView.getLayoutY());
            tt.play();
        }
    }

    /**
     * Animate item spawn
     */
    private void animateItemSpawn(ImageView itemView) {
        itemView.setScaleX(0.1);
        itemView.setScaleY(0.1);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), itemView);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    /**
     * Handle game end
     */
    public void handleGameEnd(Message message) {
        isGameActive = false;

        Platform.runLater(() -> {
            sound.fadeOutMusic(1.0);
            sound.playGameOver();

            showGameOverScreen(message.getData());
        });
    }

    /**
     * Show game over screen with rankings
     */
    private void showGameOverScreen(String rankings) {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createGradientBackground("#667eea", "#764ba2"));

        Text title = new Text("🎮 GAME OVER!");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setFill(Color.WHITE);

        // Rankings
        VBox rankingsBox = new VBox(10);
        rankingsBox.setAlignment(Pos.CENTER);

        String[] lines = rankings.split("\n");
        for (String line : lines) {
            Label rankLabel = new Label(line);
            rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            rankLabel.setTextFill(Color.WHITE);
            rankingsBox.getChildren().add(rankLabel);
        }

        Button menuBtn = UIHelper.createButton("← BACK TO LOBBY", UIHelper.PRIMARY_COLOR);
        menuBtn.setOnAction(e -> {
            sound.stopMusic();
            onGameEnd.run();
        });

        root.getChildren().addAll(title, rankingsBox, menuBtn);

        Scene scene = new Scene(root, 700, 600);
        stage.setScene(scene);
    }

    // ==================== UTILITY METHODS ====================

    private String getEmojiForItem(String itemName) {
        return AssetManager.getEmojiForItem(itemName);
    }

    // Simplified JSON parsing methods
    private int extractIntValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private double extractDoubleValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0.0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) ||
                json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private String extractSection(String json, String key) {
        String pattern = "\"" + key + "\":{";
        int start = json.indexOf(pattern);
        if (start == -1) return "{}";
        start += pattern.length() - 1;
        int braceCount = 1;
        int end = start + 1;
        while (end < json.length() && braceCount > 0) {
            if (json.charAt(end) == '{') braceCount++;
            if (json.charAt(end) == '}') braceCount--;
            end++;
        }
        return json.substring(start, end);
    }

    private String extractArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int start = json.indexOf(pattern);
        if (start == -1) return "[]";
        start += pattern.length() - 1;
        int bracketCount = 1;
        int end = start + 1;
        while (end < json.length() && bracketCount > 0) {
            if (json.charAt(end) == '[') bracketCount++;
            if (json.charAt(end) == ']') bracketCount--;
            end++;
        }
        return json.substring(start, end);
    }

    private List<String> splitArray(String arrayJson) {
        List<String> result = new ArrayList<>();
        if (arrayJson.equals("[]")) return result;

        arrayJson = arrayJson.substring(1, arrayJson.length() - 1); // Remove [ ]
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            if (c == ',' && braceCount == 0) {
                result.add(arrayJson.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (start < arrayJson.length()) {
            result.add(arrayJson.substring(start).trim());
        }

        return result;
    }

    private int extractValueAfterPattern(String text, String pattern) {
        int start = text.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(text.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }
}