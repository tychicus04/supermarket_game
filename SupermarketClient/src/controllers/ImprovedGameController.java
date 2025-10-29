package controllers;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Message;
import network.NetworkManager;
import utils.AssetManager;
import utils.SoundManager;
import utils.UIHelper;

import java.util.*;

/**
 * Improved GameController với assets, animations và better UI
 */
public class ImprovedGameController {
    private Stage stage;
    private Runnable onGameEnd;
    private NetworkManager network;
    private AssetManager assets;
    private SoundManager sound;

    // Game state
    private int score = 0;
    private int timeLeft = 120;
    private int customerTimeout = 10;
    private int combo = 0;
    private int highestCombo = 0;

    // Timelines
    private Timeline gameTimeline;
    private Timeline customerTimeline;
    private Timeline spawnTimeline;

    // UI Components
    private Label scoreLabel;
    private Label comboLabel;
    private Label timeLabel;
    private Label customerTimerLabel;
    private Label requestLabel;
    private ProgressBar customerBar;
    private ImageView customerImage;
    private StackPane gameRoot;

    // Items
    private List<ItemButton> itemButtons;
    private Map<ItemButton, Boolean> itemAvailable;
    private String currentRequest;

    private boolean isSinglePlayer;
    private String currentRoomId;

    // Item data
    private static final String[] ALL_ITEMS = {
            "MILK", "BREAD", "APPLE", "CARROT",
            "ORANGE", "EGGS", "CHEESE", "MEAT", "SODA"
    };

    public ImprovedGameController(Stage stage, Runnable onGameEnd) {
        this.stage = stage;
        this.onGameEnd = onGameEnd;
        this.network = NetworkManager.getInstance();
        this.assets = AssetManager.getInstance();
        this.sound = SoundManager.getInstance();
        this.itemButtons = new ArrayList<>();
        this.itemAvailable = new HashMap<>();
    }

    public void show(boolean isSinglePlayer) {
        this.isSinglePlayer = isSinglePlayer;
        if (isSinglePlayer) {
            startGame();
        }
    }

    public void handleGameStart(Message message) {
        startGame();
    }

    public void handleScoreUpdate(Message message) {
        System.out.println("Score update: " + message.getData());
    }

    private void startGame() {
        // Reset state
        score = 0;
        timeLeft = 120;
        customerTimeout = 10;
        combo = 0;
        highestCombo = 0;

        // Play game start sound
        sound.playGameStart();
        sound.playMusic("gameplay_music");

        // Create UI
        createGameUI();

        // Start timers
        startGameTimers();
    }

    /**
     * Create improved game UI with assets
     */
    private void createGameUI() {
        gameRoot = new StackPane();

        // Background
        ImageView background = createBackground();

        // Main content
        BorderPane mainContent = new BorderPane();

        // Top bar (stats)
        HBox topBar = createTopBar();

        // Center: Customer + Items
        VBox centerContent = new VBox(30);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20));

        VBox customerBox = createCustomerSection();
        GridPane itemsGrid = createItemsGrid();

        centerContent.getChildren().addAll(customerBox, itemsGrid);

        mainContent.setTop(topBar);
        mainContent.setCenter(centerContent);

        gameRoot.getChildren().addAll(background, mainContent);

        Scene scene = new Scene(gameRoot, 800, 700);
        stage.setScene(scene);
    }

    /**
     * Create background
     */
    private ImageView createBackground() {
        ImageView bgView = new ImageView();

        Image bgImage = assets.getImage("bg_game");
        if (bgImage != null) {
            bgView.setImage(bgImage);
            bgView.setFitWidth(800);
            bgView.setFitHeight(700);
            bgView.setPreserveRatio(false);
        } else {
            // Fallback gradient
            bgView.setStyle(UIHelper.createGradientBackground("#fff5e6", "#ffe6cc"));
        }

        return bgView;
    }

    /**
     * Create top stats bar
     */
    private HBox createTopBar() {
        HBox topBar = new HBox(30);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-background-color: rgba(255, 107, 107, 0.95); " +
                "-fx-background-radius: 0 0 15 15;");

        // Add drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        topBar.setEffect(shadow);

        // Score
        VBox scoreBox = createStatBox("💰", "Score", "0");
        scoreLabel = (Label) ((VBox) scoreBox.getChildren().get(1)).getChildren().get(0);

        // Combo
        VBox comboBox = createStatBox("🔥", "Combo", "x1");
        comboLabel = (Label) ((VBox) comboBox.getChildren().get(1)).getChildren().get(0);

        // Time
        VBox timeBox = createStatBox("⏰", "Time", "120s");
        timeLabel = (Label) ((VBox) timeBox.getChildren().get(1)).getChildren().get(0);

        topBar.getChildren().addAll(scoreBox, comboBox, timeBox);

        return topBar;
    }

    /**
     * Create stat display box
     */
    private VBox createStatBox(String icon, String label, String value) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Arial", 24));

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.rgb(255, 255, 255, 0.8));

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        valueLabel.setTextFill(Color.WHITE);

        textBox.getChildren().addAll(valueLabel, titleLabel);
        box.getChildren().addAll(iconLabel, textBox);

        return box;
    }

    /**
     * Create customer section with image
     */
    private VBox createCustomerSection() {
        VBox customerBox = new VBox(15);
        customerBox.setAlignment(Pos.CENTER);
        customerBox.setPadding(new Insets(20));
        customerBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); " +
                "-fx-background-radius: 15px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        customerBox.setMaxWidth(500);

        // Customer image
        customerImage = new ImageView();
        customerImage.setFitWidth(80);
        customerImage.setFitHeight(80);
        customerImage.setPreserveRatio(true);

        Image neutralCustomer = assets.getImage("customer_neutral");
        if (neutralCustomer != null) {
            customerImage.setImage(neutralCustomer);
        } else {
            // Fallback to emoji
            Label customerEmoji = new Label("👤");
            customerEmoji.setFont(Font.font(60));
        }

        // Speech bubble
        VBox speechBubble = new VBox(5);
        speechBubble.setAlignment(Pos.CENTER);
        speechBubble.setStyle("-fx-background-color: #ffe66d; " +
                "-fx-padding: 15px 25px; " +
                "-fx-background-radius: 15px;");

        Text wantsText = new Text("I want:");
        wantsText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        requestLabel = new Label("🥛 MILK");
        requestLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        requestLabel.setTextFill(Color.web("#2c3e50"));

        speechBubble.getChildren().addAll(wantsText, requestLabel);

        // Timer bar
        customerBar = new ProgressBar(1.0);
        customerBar.setPrefWidth(300);
        customerBar.setPrefHeight(20);
        customerBar.setStyle("-fx-accent: #4CAF50;");

        customerTimerLabel = new Label("⏱ 10s");
        customerTimerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        customerTimerLabel.setTextFill(Color.web("#e74c3c"));

        customerBox.getChildren().addAll(
                customerImage,
                speechBubble,
                customerBar,
                customerTimerLabel
        );

        return customerBox;
    }

    /**
     * Create items grid with images
     */
    private GridPane createItemsGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));

        Random random = new Random();
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        updateRequestDisplay();

        for (int i = 0; i < 9; i++) {
            String itemName = ALL_ITEMS[i];
            ItemButton itemBtn = new ItemButton(itemName);
            itemBtn.setDisable(true);
            itemAvailable.put(itemBtn, false);

            itemBtn.setOnAction(e -> handleItemClick(itemBtn, random));

            grid.add(itemBtn, i % 3, i / 3);
            itemButtons.add(itemBtn);
        }

        // Start spawn timeline
        spawnTimeline = new Timeline(new KeyFrame(Duration.seconds(2),
                e -> spawnItems(random)));
        spawnTimeline.setCycleCount(Timeline.INDEFINITE);
        spawnTimeline.play();

        return grid;
    }

    /**
     * Custom ItemButton with image support
     */
    private class ItemButton extends Button {
        private String itemName;
        private ImageView imageView;

        public ItemButton(String itemName) {
            this.itemName = itemName;

            setPrefSize(140, 140);
            setStyle("-fx-background-color: #bdc3c7; " +
                    "-fx-background-radius: 15px; " +
                    "-fx-cursor: hand;");

            // Try to load image
            Image itemImage = assets.getItemImage(itemName.toLowerCase());

            if (itemImage != null) {
                imageView = new ImageView(itemImage);
                imageView.setFitWidth(80);
                imageView.setFitHeight(80);
                imageView.setPreserveRatio(true);
                setGraphic(imageView);
                setText("");
            } else {
                // Fallback to emoji
                setText(getEmojiForItem(itemName));
                setFont(Font.font(50));
            }

            // Hover effect
            setOnMouseEntered(e -> {
                if (!isDisabled()) {
                    setStyle("-fx-background-color: #95a5a6; " +
                            "-fx-background-radius: 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-scale-x: 1.05; " +
                            "-fx-scale-y: 1.05;");
                    sound.playButtonHover();
                }
            });

            setOnMouseExited(e -> {
                if (!isDisabled()) {
                    setStyle("-fx-background-color: #ecf0f1; " +
                            "-fx-background-radius: 15px; " +
                            "-fx-cursor: hand;");
                }
            });
        }

        public String getItemName() {
            return itemName;
        }

        public void show() {
            String emoji = getEmojiForItem(itemName);

            if (imageView != null) {
                // Show image
                setGraphic(imageView);
            } else {
                // Show emoji
                setText(emoji);
            }

            setDisable(false);
            setStyle("-fx-background-color: #4ecdc4; " +
                    "-fx-background-radius: 15px; " +
                    "-fx-cursor: hand;");

            // Spawn animation
            setScaleX(0.1);
            setScaleY(0.1);
            ScaleTransition st = new ScaleTransition(Duration.millis(300), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        }

        public void hide() {
            setDisable(true);
            setGraphic(null);
            setText("?");
            setFont(Font.font(40));
            setStyle("-fx-background-color: #bdc3c7; " +
                    "-fx-background-radius: 15px; " +
                    "-fx-text-fill: #7f8c8d;");
        }
    }

    /**
     * Handle item click
     */
    private void handleItemClick(ItemButton btn, Random random) {
        if (!itemAvailable.get(btn)) return;

        sound.playPickup();

        String itemName = btn.getItemName();

        if (itemName.equals(currentRequest)) {
            // Correct!
            handleCorrectItem(btn, random);
        } else {
            // Wrong!
            handleWrongItem(btn);
        }
    }

    /**
     * Handle correct item selection
     */
    private void handleCorrectItem(ItemButton btn, Random random) {
        combo++;
        if (combo > highestCombo) highestCombo = combo;

        int points = 10 * combo;
        score += points;

        // Update UI
        scoreLabel.setText(String.valueOf(score));
        comboLabel.setText("x" + combo);

        // Sound
        sound.playCorrect();
        sound.playComboIncrease(combo);
        sound.playCustomerHappy();

        // Visual feedback
        flashButton(btn, "#27ae60");
        showScorePopup(points, btn);
        updateCustomerMood("happy");

        // Reset customer timer
        customerTimeout = 10;

        // New request
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        updateRequestDisplay();

        // Hide item
        hideItem(btn);
    }

    /**
     * Handle wrong item selection
     */
    private void handleWrongItem(ItemButton btn) {
        combo = 0;
        score = Math.max(0, score - 5);

        // Update UI
        scoreLabel.setText(String.valueOf(score));
        comboLabel.setText("x1");

        // Sound
        sound.playWrong();
        sound.playComboBreak();

        // Visual feedback
        flashButton(btn, "#e74c3c");
        shakeNode(btn);
        updateCustomerMood("angry");
    }

    /**
     * Update request display
     */
    private void updateRequestDisplay() {
        String emoji = getEmojiForItem(currentRequest);
        requestLabel.setText(emoji + " " + currentRequest);
    }

    /**
     * Update customer mood
     */
    private void updateCustomerMood(String mood) {
        Image moodImage = assets.getImage("customer_" + mood);
        if (moodImage != null && customerImage != null) {
            customerImage.setImage(moodImage);

            // Bounce animation
            ScaleTransition st = new ScaleTransition(Duration.millis(200), customerImage);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.2);
            st.setToY(1.2);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }
    }

    /**
     * Flash button color
     */
    private void flashButton(Button btn, String color) {
        String originalStyle = btn.getStyle();
        btn.setStyle(originalStyle.replaceFirst(
                "-fx-background-color: [^;]+",
                "-fx-background-color: " + color
        ));

        Timeline flash = new Timeline(new KeyFrame(Duration.millis(300),
                e -> btn.setStyle(originalStyle)));
        flash.play();
    }

    /**
     * Shake animation
     */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    /**
     * Show score popup
     */
    private void showScorePopup(int points, javafx.scene.Node anchor) {
        Label popup = new Label("+" + points);
        popup.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        popup.setTextFill(Color.web("#27ae60"));

        // Position near button
        double x = anchor.getLayoutX() + anchor.getTranslateX();
        double y = anchor.getLayoutY() + anchor.getTranslateY();

        popup.setLayoutX(x);
        popup.setLayoutY(y - 50);

        gameRoot.getChildren().add(popup);

        // Animate up and fade
        TranslateTransition tt = new TranslateTransition(Duration.seconds(1), popup);
        tt.setByY(-50);

        FadeTransition ft = new FadeTransition(Duration.seconds(1), popup);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> gameRoot.getChildren().remove(popup));
        pt.play();
    }

    /**
     * Spawn items randomly
     */
    private void spawnItems(Random random) {
        List<ItemButton> hiddenButtons = new ArrayList<>();
        for (ItemButton btn : itemButtons) {
            if (!itemAvailable.get(btn)) {
                hiddenButtons.add(btn);
            }
        }

        if (!hiddenButtons.isEmpty()) {
            int spawnCount = Math.min(random.nextInt(2) + 1, hiddenButtons.size());
            Collections.shuffle(hiddenButtons);

            for (int i = 0; i < spawnCount; i++) {
                ItemButton btn = hiddenButtons.get(i);
                btn.show();
                itemAvailable.put(btn, true);
            }
        }
    }

    /**
     * Hide item
     */
    private void hideItem(ItemButton btn) {
        itemAvailable.put(btn, false);

        // Shrink animation
        ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
        st.setToX(0.1);
        st.setToY(0.1);
        st.setOnFinished(e -> btn.hide());
        st.play();
    }

    /**
     * Start game timers
     */
    private void startGameTimers() {
        // Customer timeout timer
        customerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            customerTimeout--;
            customerTimerLabel.setText("⏱ " + customerTimeout + "s");
            customerBar.setProgress(customerTimeout / 10.0);

            // Color based on time
            if (customerTimeout <= 3) {
                customerBar.setStyle("-fx-accent: #e74c3c;");
            } else if (customerTimeout <= 6) {
                customerBar.setStyle("-fx-accent: #f39c12;");
            } else {
                customerBar.setStyle("-fx-accent: #4CAF50;");
            }

            // Timeout!
            if (customerTimeout <= 0) {
                handleCustomerTimeout();
            }
        }));
        customerTimeline.setCycleCount(Timeline.INDEFINITE);
        customerTimeline.play();

        // Main game timer
        gameTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timeLabel.setText(timeLeft + "s");

            // Warning at 10s
            if (timeLeft == 10) {
                timeLabel.setTextFill(Color.web("#e74c3c"));
                sound.playTimerWarning();
            }

            // Game over
            if (timeLeft <= 0) {
                endGame();
            }
        }));
        gameTimeline.setCycleCount(120);
        gameTimeline.play();
    }

    /**
     * Handle customer timeout
     */
    private void handleCustomerTimeout() {
        combo = 0;
        score = Math.max(0, score - 15);

        scoreLabel.setText(String.valueOf(score));
        comboLabel.setText("x1");

        sound.playCustomerAngry();
        updateCustomerMood("angry");

        // Flash request label
        requestLabel.setStyle("-fx-background-color: #e74c3c; " +
                "-fx-padding: 15px; " +
                "-fx-background-radius: 10px;");

        Timeline flash = new Timeline(new KeyFrame(Duration.millis(500),
                evt -> requestLabel.setStyle("")));
        flash.play();

        // Reset
        customerTimeout = 10;
        Random random = new Random();
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        updateRequestDisplay();
    }

    /**
     * End game
     */
    private void endGame() {
        // Stop timers
        if (gameTimeline != null) gameTimeline.stop();
        if (customerTimeline != null) customerTimeline.stop();
        if (spawnTimeline != null) spawnTimeline.stop();

        // Stop music
        sound.fadeOutMusic(1.0);
        sound.playGameOver();

        // Send score
        String roomId = isSinglePlayer ? "SINGLE" : currentRoomId;
        network.sendScore(roomId, score);

        showGameOverScreen();
    }

    /**
     * Show game over screen
     */
    private void showGameOverScreen() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createGradientBackground("#667eea", "#764ba2"));

        Text title = new Text("🎮 GAME OVER!");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setFill(Color.WHITE);

        VBox scoreBox = new VBox(10);
        scoreBox.setAlignment(Pos.CENTER);

        Text scoreText = new Text("Final Score");
        scoreText.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        scoreText.setFill(Color.rgb(255, 255, 255, 0.8));

        Text scoreValue = new Text(String.valueOf(score));
        scoreValue.setFont(Font.font("Arial", FontWeight.BOLD, 72));
        scoreValue.setFill(Color.web("#ffe66d"));

        scoreBox.getChildren().addAll(scoreText, scoreValue);

        // Rating
        String rating = score >= 500 ? "🏆 LEGENDARY!" :
                score >= 300 ? "⭐ EXCELLENT!" :
                        score >= 150 ? "👍 GOOD JOB!" : "💪 KEEP TRYING!";

        Text ratingText = new Text(rating);
        ratingText.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        ratingText.setFill(Color.web("#ffd700"));

        // Stats
        VBox statsBox = new VBox(5);
        statsBox.setAlignment(Pos.CENTER);

        Text comboStat = new Text("Highest Combo: x" + highestCombo);
        comboStat.setFont(Font.font("Arial", 18));
        comboStat.setFill(Color.WHITE);

        statsBox.getChildren().add(comboStat);

        // Button
        Button menuBtn = UIHelper.createButton("← BACK TO MENU", UIHelper.PRIMARY_COLOR);
        menuBtn.setOnAction(e -> {
            sound.stopMusic();
            onGameEnd.run();
        });

        root.getChildren().addAll(title, scoreBox, ratingText, statsBox, menuBtn);

        Scene scene = new Scene(root, 700, 600);
        stage.setScene(scene);
    }

    /**
     * Get emoji for item name
     */
    private String getEmojiForItem(String itemName) {
        return AssetManager.getEmojiForItem(itemName);
    }
}