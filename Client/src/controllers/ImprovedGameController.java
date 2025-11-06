package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import constants.GameConstants;

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
    private int opponentScore = 0; // Điểm của đối thủ
    private int timeLeft = 120;
    private int customerTimeout = 10;
    private int maxCustomerTimeout = 10; // Thời gian chờ tối đa, sẽ giảm dần
    private static final int MIN_CUSTOMER_TIMEOUT = 3; // Tối thiểu 3 giây
    private int requestsServed = 0; // Đếm số lần phục vụ để giảm thời gian
    private int combo = 0;
    private int highestCombo = 0;
    private boolean gameActive = false; // Track game state

    // Timelines
    private Timeline gameTimeline;
    private Timeline customerTimeline;

    // UI Components
    private Label scoreLabel;
    private Label opponentScoreLabel; // Label điểm đối thủ
    private Label comboLabel;
    private Label timeLabel;
    private Label customerTimerLabel;
    private Label requestLabel;
    private ProgressBar customerBar;
    private ImageView customerImage;
    private StackPane gameRoot;

    // Items
    private List<ItemButton> itemButtons;
    private String currentRequest;
    private List<String> availableItems; // Pool items còn lại để spawn
    private Map<ItemButton, Integer> itemDisplayTime; // Thời gian hiển thị còn lại của mỗi item
    private Map<ItemButton, Timeline> itemTimers; // Timer cho mỗi item
    private int baseItemDisplayTime = 8; // Thời gian hiển thị ban đầu (giây)
    private static final int MIN_ITEM_DISPLAY_TIME = 3; // Tối thiểu 3 giây

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
        this.availableItems = new ArrayList<>();
        this.itemDisplayTime = new HashMap<>();
        this.itemTimers = new HashMap<>();
    }

    public void show(boolean isSinglePlayer) {
        this.isSinglePlayer = isSinglePlayer;
        if (isSinglePlayer) {
            startGame();
        }
    }

    public void handleGameStart(Message message) {
        // Multiplayer game start
        if (message.getData() != null) {
            this.currentRoomId = message.getData().toString();
        }
        startGame();
    }

    public void handleScoreUpdate(Message message) {
        System.out.println("Score update: " + message.getData());
    }

    /**
     * Handle server response when item is selected correctly
     */
    public void handleItemCorrect(Message message) {
        Platform.runLater(() -> {
            String data = message.getData().toString();
            String[] parts = data.split("\\|");
            String itemName = parts[0];
            int newTimeout = Integer.parseInt(parts[1]);
            String newRequest = parts[2];
            int yourScore = Integer.parseInt(parts[3]);
            int opponentScore = Integer.parseInt(parts[4]);

            // Update scores
            score = yourScore;
            opponentScore = this.opponentScore;
            scoreLabel.setText(String.valueOf(score));
            if (opponentScoreLabel != null) {
                opponentScoreLabel.setText(String.valueOf(opponentScore));
            }

            // Update combo
            combo++;
            if (combo > highestCombo) highestCombo = combo;
            comboLabel.setText("x" + combo);

            // Sound and visual feedback
            sound.playCorrect();
            sound.playComboIncrease(combo);
            sound.playCustomerHappy();
            updateCustomerMood("happy");

            // Find button and change item
            Random random = new Random();
            for (ItemButton btn : itemButtons) {
                if (btn.getItemName().equals(itemName)) {
                    flashButton(btn, "#27ae60");
                    showScorePopup(10 * combo, btn);
                    String newItem = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
                    btn.changeItem(newItem);
                    break;
                }
            }

            // Update request and timeout
            maxCustomerTimeout = newTimeout;
            customerTimeout = newTimeout;
            currentRequest = newRequest;
            updateRequestDisplay();
        });
    }

    /**
     * Handle server response when item is selected incorrectly
     */
    public void handleItemWrong(Message message) {
        Platform.runLater(() -> {
            String itemName = message.getData().toString();

            // Reset combo
            combo = 0;
            comboLabel.setText("x1");

            // Sound and visual feedback
            sound.playWrong();
            sound.playComboBreak();
            updateCustomerMood("angry");

            // Find button and flash red
            for (ItemButton btn : itemButtons) {
                if (btn.getItemName().equals(itemName)) {
                    flashButton(btn, "#e74c3c");
                    shakeNode(btn);
                    break;
                }
            }
        });
    }

    /**
     * Handle new request from server (multiplayer)
     */
    public void handleNewRequest(Message message) {
        Platform.runLater(() -> {
            String data = message.getData().toString();
            String[] parts = data.split("\\|");
            String newRequest = parts[0];
            int newTimeout = Integer.parseInt(parts[1]);

            currentRequest = newRequest;
            maxCustomerTimeout = newTimeout;
            customerTimeout = newTimeout;
            updateRequestDisplay();
        });
    }

    /**
     * Handle game state update from server
     */
    public void handleGameState(Message message) {
        Platform.runLater(() -> {
            String data = message.getData().toString();
            String[] parts = data.split("\\|");
            int p1Score = Integer.parseInt(parts[0]);
            int p2Score = Integer.parseInt(parts[1]);
            int timeLeft = Integer.parseInt(parts[2]);

            // Update UI
            this.timeLeft = timeLeft;
            timeLabel.setText(timeLeft + "s");

            // Update scores (determine which is yours)
            if (opponentScoreLabel != null) {
                opponentScoreLabel.setText(String.valueOf(p2Score));
            }
        });
    }

    /**
     * Handle game over from server
     */
    public void handleGameOver(Message message) {
        Platform.runLater(() -> {
            gameActive = false;
            endGame();
        });
    }

    private void startGame() {
        // Reset state
        score = 0;
        opponentScore = 0;
        timeLeft = 120;
        customerTimeout = 10;
        maxCustomerTimeout = 10;
        requestsServed = 0;
        combo = 0;
        highestCombo = 0;
        gameActive = true;
        baseItemDisplayTime = 8; // Reset thời gian hiển thị về 8 giây

        // Reset available items pool
        availableItems.clear();
        availableItems.addAll(Arrays.asList(ALL_ITEMS));
        Collections.shuffle(availableItems);

        // Play game start sound
        sound.playGameStart();
        sound.playMusic("gameplay_music");

        // Create UI
        createGameUI();

        // Start timers (only for single player)
        if (isSinglePlayer) {
            startGameTimers();
        } else {
            // In multiplayer, only start display timer, game logic handled by server
            startMultiplayerTimers();
        }
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

        // Score (Your score)
        VBox scoreBox = createStatBox("💰", isSinglePlayer ? "Score" : "You", "0");
        scoreLabel = (Label) ((VBox) scoreBox.getChildren().get(1)).getChildren().get(0);

        // Opponent score (chỉ hiện trong multiplayer)
        if (!isSinglePlayer) {
            VBox opponentScoreBox = createStatBox("🎯", "Opponent", "0");
            opponentScoreLabel = (Label) ((VBox) opponentScoreBox.getChildren().get(1)).getChildren().get(0);
            topBar.getChildren().add(opponentScoreBox);
        }

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

        // Tạo 9 item buttons, ban đầu tất cả đều ẩn
        for (int i = 0; i < 9; i++) {
            ItemButton itemBtn = new ItemButton(null); // null = ẩn
            itemBtn.setOnAction(e -> handleItemClick(itemBtn, random));

            grid.add(itemBtn, i % 3, i / 3);
            itemButtons.add(itemBtn);
        }

        // Spawn 3 items đầu tiên
        if (isSinglePlayer) {
            for (int i = 0; i < 3; i++) {
                spawnNewItem();
            }
        }

        return grid;
    }

    /**
     * Custom ItemButton with image support
     */
    private class ItemButton extends Button {
        private String itemName;
        private ImageView imageView;
        private boolean isHidden;

        public ItemButton(String itemName) {
            this.itemName = itemName;
            this.isHidden = (itemName == null);

            setPrefSize(140, 140);

            if (isHidden) {
                // Ẩn - hiển thị dấu ?
                setStyle("-fx-background-color: #bdc3c7; " +
                        "-fx-background-radius: 15px; " +
                        "-fx-cursor: default;");
                setText("?");
                setFont(Font.font(50));
                setTextFill(Color.web("#7f8c8d"));
                setDisable(true);
            } else {
                // Hiển thị item
                setStyle("-fx-background-color: #4ecdc4; " +
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
            }

            // Hover effect
            setOnMouseEntered(e -> {
                if (!isHidden && !isDisabled()) {
                    setStyle("-fx-background-color: #45b7af; " +
                            "-fx-background-radius: 15px; " +
                            "-fx-cursor: hand; " +
                            "-fx-scale-x: 1.05; " +
                            "-fx-scale-y: 1.05;");
                    sound.playButtonHover();
                }
            });

            setOnMouseExited(e -> {
                if (!isHidden && !isDisabled()) {
                    setStyle("-fx-background-color: #4ecdc4; " +
                            "-fx-background-radius: 15px; " +
                            "-fx-cursor: hand;");
                }
            });
        }

        public String getItemName() {
            return itemName;
        }

        public boolean isHidden() {
            return isHidden;
        }

        public void showItem(String newItemName) {
            this.itemName = newItemName;
            this.isHidden = false;
            setDisable(false);

            setStyle("-fx-background-color: #4ecdc4; " +
                    "-fx-background-radius: 15px; " +
                    "-fx-cursor: hand;");

            // Update image or emoji
            Image itemImage = assets.getItemImage(newItemName.toLowerCase());

            if (itemImage != null) {
                if (imageView == null) {
                    imageView = new ImageView();
                    imageView.setFitWidth(80);
                    imageView.setFitHeight(80);
                    imageView.setPreserveRatio(true);
                }
                imageView.setImage(itemImage);
                setGraphic(imageView);
                setText("");
            } else {
                setGraphic(null);
                setText(getEmojiForItem(newItemName));
                setFont(Font.font(50));
            }

            // Spawn animation
            setScaleX(0.1);
            setScaleY(0.1);
            ScaleTransition st = new ScaleTransition(Duration.millis(300), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        }

        public void hideItem() {
            this.itemName = null;
            this.isHidden = true;
            setDisable(true);

            setGraphic(null);
            setText("?");
            setFont(Font.font(40));
            setStyle("-fx-background-color: #bdc3c7; " +
                    "-fx-background-radius: 15px; " +
                    "-fx-text-fill: #7f8c8d;");

            // Shrink animation
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setToX(0.1);
            st.setToY(0.1);
            st.setOnFinished(e -> {
                setScaleX(1.0);
                setScaleY(1.0);
            });
            st.play();
        }

        public void changeItem(String newItemName) {
            this.itemName = newItemName;

            // Update image or emoji
            Image itemImage = assets.getItemImage(newItemName.toLowerCase());

            if (itemImage != null) {
                if (imageView == null) {
                    imageView = new ImageView();
                    imageView.setFitWidth(80);
                    imageView.setFitHeight(80);
                    imageView.setPreserveRatio(true);
                }
                imageView.setImage(itemImage);
                setGraphic(imageView);
                setText("");
            } else {
                setGraphic(null);
                setText(getEmojiForItem(newItemName));
                setFont(Font.font(50));
            }

            // Animation khi thay đổi
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setFromX(0.8);
            st.setFromY(0.8);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        }
    }

    /**
     * Handle item click
     */
    private void handleItemClick(ItemButton btn, Random random) {
        if (!gameActive || btn.isHidden()) return;

        sound.playPickup();

        String itemName = btn.getItemName();

        if (isSinglePlayer) {
            // Single player: xử lý local
            if (itemName.equals(currentRequest)) {
                handleCorrectItem(btn, random);
            } else {
                handleWrongItem(btn);
            }
        } else {
            // Multiplayer: gửi tới server
            network.sendMessage(new Message(
                GameConstants.MESSAGE_TYPE_ITEM_SELECTED,
                currentRoomId + "|" + itemName
            ));
        }
    }

    /**
     * Handle correct item selection (for single player)
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

        // Tăng số lần phục vụ và giảm dần thời gian chờ
        requestsServed++;

        // Giảm thời gian chờ tối đa sau mỗi 2 lần phục vụ thành công
        if (requestsServed % 2 == 0 && maxCustomerTimeout > MIN_CUSTOMER_TIMEOUT) {
            maxCustomerTimeout--;
            System.out.println("Độ khó tăng! Thời gian chờ giảm xuống: " + maxCustomerTimeout + "s");
        }

        // Giảm thời gian hiển thị item sau mỗi 3 lần phục vụ
        if (requestsServed % 3 == 0 && baseItemDisplayTime > MIN_ITEM_DISPLAY_TIME) {
            baseItemDisplayTime--;
            System.out.println("Độ khó tăng! Thời gian hiển thị item giảm xuống: " + baseItemDisplayTime + "s");
        }

        // Reset customer timer với thời gian mới (đã giảm)
        customerTimeout = maxCustomerTimeout;

        // New request
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        updateRequestDisplay();

        // Stop timer của item này
        stopItemTimer(btn);

        // Ẩn item đã chọn
        btn.hideItem();

        // Spawn item mới sau 0.5s
        Timeline spawnDelay = new Timeline(new KeyFrame(Duration.millis(500), e -> spawnNewItem()));
        spawnDelay.play();
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
     * Spawn item mới từ pool luân phiên
     */
    private void spawnNewItem() {
        if (!gameActive) return;

        // Tìm button đang ẩn
        ItemButton hiddenBtn = null;
        for (ItemButton btn : itemButtons) {
            if (btn.isHidden()) {
                hiddenBtn = btn;
                break;
            }
        }

        if (hiddenBtn == null) {
            // Không còn chỗ trống
            return;
        }

        // Refill pool nếu hết
        if (availableItems.isEmpty()) {
            availableItems.addAll(Arrays.asList(ALL_ITEMS));
            Collections.shuffle(availableItems);
        }

        // Lấy item từ pool
        String itemToSpawn = availableItems.remove(0);

        // Hiển thị item
        hiddenBtn.showItem(itemToSpawn);

        // Start timer cho item này
        startItemTimer(hiddenBtn);
    }

    /**
     * Start timer cho một item - tự động ẩn sau thời gian
     */
    private void startItemTimer(ItemButton btn) {
        // Set thời gian hiển thị
        itemDisplayTime.put(btn, baseItemDisplayTime);

        // Tạo timeline countdown
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int timeLeft = itemDisplayTime.get(btn) - 1;
            itemDisplayTime.put(btn, timeLeft);

            // Thay đổi màu button khi sắp hết thời gian
            if (timeLeft <= 2) {
                btn.setStyle("-fx-background-color: #e74c3c; " +
                           "-fx-background-radius: 15px; " +
                           "-fx-cursor: hand;");
            } else if (timeLeft <= 4) {
                btn.setStyle("-fx-background-color: #f39c12; " +
                           "-fx-background-radius: 15px; " +
                           "-fx-cursor: hand;");
            }

            // Hết thời gian - ẩn item và spawn mới
            if (timeLeft <= 0) {
                stopItemTimer(btn);
                btn.hideItem();

                // Spawn item mới sau 0.3s
                Timeline spawnDelay = new Timeline(new KeyFrame(Duration.millis(300), ev -> spawnNewItem()));
                spawnDelay.play();
            }
        }));
        timer.setCycleCount(baseItemDisplayTime);
        timer.play();

        // Lưu timer
        itemTimers.put(btn, timer);
    }

    /**
     * Stop timer của một item
     */
    private void stopItemTimer(ItemButton btn) {
        Timeline timer = itemTimers.get(btn);
        if (timer != null) {
            timer.stop();
            itemTimers.remove(btn);
        }
        itemDisplayTime.remove(btn);
    }

    /**
     * Stop tất cả item timers
     */
    private void stopAllItemTimers() {
        for (Timeline timer : itemTimers.values()) {
            if (timer != null) {
                timer.stop();
            }
        }
        itemTimers.clear();
        itemDisplayTime.clear();
    }

    /**
     * Start game timers
     */
    private void startGameTimers() {
        // Customer timeout timer
        customerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            customerTimeout--;
            customerTimerLabel.setText("⏱ " + customerTimeout + "s");

            // Cập nhật progress bar dựa trên maxCustomerTimeout hiện tại
            customerBar.setProgress((double) customerTimeout / maxCustomerTimeout);

            // Color based on time (dựa trên phần trăm thời gian còn lại)
            double timePercent = (double) customerTimeout / maxCustomerTimeout;
            if (timePercent <= 0.3) {
                customerBar.setStyle("-fx-accent: #e74c3c;");
            } else if (timePercent <= 0.6) {
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
     * Start multiplayer timers (only for display, logic handled by server)
     */
    private void startMultiplayerTimers() {
        // Customer timeout timer (just for display)
        customerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            customerTimeout--;
            customerTimerLabel.setText("⏱ " + customerTimeout + "s");

            // Cập nhật progress bar dựa trên maxCustomerTimeout hiện tại
            customerBar.setProgress((double) customerTimeout / maxCustomerTimeout);

            // Color based on time (dựa trên phần trăm thời gian còn lại)
            double timePercent = (double) customerTimeout / maxCustomerTimeout;
            if (timePercent <= 0.3) {
                customerBar.setStyle("-fx-accent: #e74c3c;");
            } else if (timePercent <= 0.6) {
                customerBar.setStyle("-fx-accent: #f39c12;");
            } else {
                customerBar.setStyle("-fx-accent: #4CAF50;");
            }
        }));
        customerTimeline.setCycleCount(Timeline.INDEFINITE);
        customerTimeline.play();
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

        // Reset với thời gian chờ hiện tại (đã giảm dần)
        customerTimeout = maxCustomerTimeout;
        Random random = new Random();
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        updateRequestDisplay();
    }

    /**
     * End game
     */
    private void endGame() {
        gameActive = false;

        // Stop timers
        if (gameTimeline != null) gameTimeline.stop();
        if (customerTimeline != null) customerTimeline.stop();
        stopAllItemTimers();

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