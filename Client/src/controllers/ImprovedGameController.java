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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Message;
import utils.AssetManager;
import utils.SoundManager;


import java.util.*;

/**
 * Chỉ chỉnh sửa logic gameplay:
 * - Cố định ma trận vật phẩm 3x3, map phím 1..9 -> item
 * - Server/Client: mỗi yêu cầu là 1 list các vật phẩm (string); người chơi phải bấm đúng theo thứ tự
 * - Độ khó: 5s/yêu cầu, mỗi 15s giảm 1s (tối thiểu 1s)
 * - Bỏ combo; sai trừ điểm, đúng theo thứ tự; hoàn tất list thì +1 điểm và chuyển yêu cầu mới
 */
public class ImprovedGameController {

    private Stage primaryStage;
    private Runnable onBackToMenu;

    // UI Components
    private Label scoreLabel;
    private Label opponentScoreLabel;
    private Label timeLabel;
    private Label requestLabel;
    private Label customerTimerLabel;
    private ProgressBar customerBar;
    private ImageView customerImage;
    private SoundManager soundManager;

    private VBox[][] gridCells = new VBox[3][3];
    private Map<KeyCode, VBox> keyToCellMap = new HashMap<>();

    // Gameplay state
    private static final String[][] ITEM_MATRIX = {
            {"MILK", "BREAD", "APPLE"},
            {"CARROT", "ORANGE", "EGGS"},
            {"CHEESE", "MEAT", "SODA"}
    };

    private final Map<KeyCode, String> keyToItem = new HashMap<>();

    private List<String> currentSequence = new ArrayList<>();
    private int currentIndex = 0;

    private int myScore = 0;
    private int opponentScore = 0;
    private long gameStartMillis = 0L;
    private long roundStartMillis = 0L;

    private double allowedTimeSeconds = 15.0;
    private Timeline roundTimer;
    private Timeline hudTicker;
    private Timeline gameTimer;

    private static final int SEQUENCE_LEN = 4;
    private static final double MIN_ALLOWED = 2.0;
    private static final int GAME_DURATION_SECONDS = 60;

    private boolean isSinglePlayer = true;
    private Label gameTimeLabel;
    private boolean gameEnded = false;

    // Visual effect colors
    private static final Color DEFAULT_BG_COLOR = Color.web("#ecf0f1");
    private static final Color CORRECT_BG_COLOR = Color.web("#d5f4e6");
    private static final Color CORRECT_BORDER_COLOR = Color.web("#27ae60");
    private static final Color WRONG_BG_COLOR = Color.web("#fadbd8");
    private static final Color WRONG_BORDER_COLOR = Color.web("#e74c3c");

    // Constructor
    public ImprovedGameController(Stage stage, Runnable onBackToMenu) {
        this.primaryStage = stage;
        this.onBackToMenu = onBackToMenu;
        this.soundManager = SoundManager.getInstance();
    }

    // Public API

    /**
     * Reset all game state to initial values
     * Called when starting a new game or restarting
     */
    private void resetGameState() {
        // Stop any running timers first
        stopAllTimers();

        // Stop music
        if (soundManager != null) {
            soundManager.stopMusic();
        }

        // Reset scores
        myScore = 0;
        opponentScore = 0;

        // Reset timing variables
        allowedTimeSeconds = 15.0;
        gameStartMillis = 0L;
        roundStartMillis = 0L;

        // Reset game state
        gameEnded = false;
        currentSequence = new ArrayList<>();
        currentIndex = 0;

        // Clear mappings (will be recreated)
        keyToItem.clear();
        keyToCellMap.clear();

        // Reset grid cells array
        gridCells = new VBox[3][3];

        System.out.println("Game state reset complete");
    }

    /**
     * Main game screen
     */
    public void show(boolean isSinglePlayer) {
        this.isSinglePlayer = isSinglePlayer;

        resetGameState();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Background image
        Image bgImage = AssetManager.getImage("bg_game");
        if (bgImage != null) {
            BackgroundImage background = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, false, true)
            );
            root.setBackground(new Background(background));
        }

        Label title = new Label("Supermarket Game");
        title.setFont(Font.font(28));
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 2);");

        HBox scoreBox = new HBox(24);
        scoreBox.setAlignment(Pos.CENTER);
        scoreLabel = mkTag("Your Score: 0");
        opponentScoreLabel = mkTag("Opponent: 0");
        timeLabel = mkTag("Time/Req: 5.0s");

        gameTimeLabel = new Label("⏱️ Time: 1:00");
        gameTimeLabel.setFont(Font.font(20));
        gameTimeLabel.setTextFill(Color.WHITE);
        gameTimeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: rgba(231, 76, 60, 0.8); -fx-padding: 5 15; -fx-background-radius: 10;");

        scoreBox.getChildren().addAll(scoreLabel, opponentScoreLabel, timeLabel, gameTimeLabel);

        // Customer image
        customerImage = new ImageView();
        customerImage.setFitWidth(120);
        customerImage.setFitHeight(120);
        setCustomerEmotion("neutral");

        VBox customerBox = new VBox(8);
        customerBox.setAlignment(Pos.CENTER);
        customerBox.setPadding(new Insets(10));
        customerBox.setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-border-color: #e74c3c; " +
                        "-fx-border-width: 4px; " +
                        "-fx-border-style: solid; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 3, 3);"
        );

        Label customerTitle = new Label("🎯 CUSTOMER");
        customerTitle.setFont(Font.font("Courier New", 14));
        customerTitle.setTextFill(Color.web("#e74c3c"));
        customerTitle.setStyle("-fx-font-weight: bold;");

        customerBox.getChildren().addAll(customerTitle, customerImage);

        // Order list
        requestLabel = new Label("Waiting for game to start...");
        requestLabel.setFont(Font.font("Courier New", 22));
        requestLabel.setTextFill(Color.web("#2c3e50"));
        requestLabel.setWrapText(true);
        requestLabel.setMaxWidth(500);
        requestLabel.setPadding(new Insets(15, 20, 15, 20));
        requestLabel.setAlignment(Pos.CENTER);
        requestLabel.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-background-color: #fef9e7; " +
                        "-fx-border-color: #34495e; " +
                        "-fx-border-width: 4px; " +
                        "-fx-border-style: solid; " +
                        "-fx-border-insets: 0; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 3, 3);"
        );

        HBox topGameArea = new HBox(20, customerBox, requestLabel);
        topGameArea.setAlignment(Pos.CENTER);
        topGameArea.setPadding(new Insets(10, 0, 10, 0));

        // Create 3x3 grid with stored references
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        // Define key arrays
        KeyCode[] digitKeys = {
                KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6,
                KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9
        };

        KeyCode[] numpadKeys = {
                KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3,
                KeyCode.NUMPAD4, KeyCode.NUMPAD5, KeyCode.NUMPAD6,
                KeyCode.NUMPAD7, KeyCode.NUMPAD8, KeyCode.NUMPAD9
        };

        int id = 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                String name = ITEM_MATRIX[r][c];
                VBox cell = mkItemCell(id, name);
                gridCells[r][c] = cell;
                grid.add(cell, c, r);

                // Map both digit and numpad keys to the same cell
                int idx = id - 1;
                keyToCellMap.put(digitKeys[idx], cell);
                keyToCellMap.put(numpadKeys[idx], cell);

                id++;
            }
        }

        // Progress bar
        customerBar = new ProgressBar(1);
        customerBar.setPrefWidth(420);
        customerTimerLabel = new Label("");
        customerTimerLabel.setFont(Font.font(14));
        customerTimerLabel.setTextFill(Color.web("#e74c3c"));
        VBox progressBox = new VBox(4, customerBar, customerTimerLabel);
        progressBox.setAlignment(Pos.CENTER);

        // Instructions
        Label instructions = new Label("Press 1-9 to select items in order!");
        instructions.setFont(Font.font(16));
        instructions.setTextFill(Color.WHITE);
        instructions.setStyle("-fx-background-color: rgba(52, 73, 94, 0.9); -fx-padding: 8 15; -fx-background-radius: 8;");

        Button backBtn = new Button("Back to Menu");
        backBtn.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );
        backBtn.setOnAction(e -> {
            stopAllTimers();
            soundManager.stopMusic();
            resetGameState();
            Platform.runLater(() -> {
                if (onBackToMenu != null) {
                    onBackToMenu.run();
                }
            });
        });

        root.getChildren().addAll(title, scoreBox, topGameArea, grid, progressBox, instructions, backBtn);

        Scene scene = new Scene(root, 1000, 900);
        scene.setOnKeyPressed(ev -> {
            if (gameEnded) return;

            KeyCode kc = ev.getCode();
            // Check if it's a digit key from 1-9 (not 0)
            if (kc == KeyCode.DIGIT1 || kc == KeyCode.DIGIT2 || kc == KeyCode.DIGIT3 ||
                    kc == KeyCode.DIGIT4 || kc == KeyCode.DIGIT5 || kc == KeyCode.DIGIT6 ||
                    kc == KeyCode.DIGIT7 || kc == KeyCode.DIGIT8 || kc == KeyCode.DIGIT9 ||
                    kc == KeyCode.NUMPAD1 || kc == KeyCode.NUMPAD2 || kc == KeyCode.NUMPAD3 ||
                    kc == KeyCode.NUMPAD4 || kc == KeyCode.NUMPAD5 || kc == KeyCode.NUMPAD6 ||
                    kc == KeyCode.NUMPAD7 || kc == KeyCode.NUMPAD8 || kc == KeyCode.NUMPAD9) {
                handleKeyPress(kc);
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();

        initKeyMap();
        startNewRound();
        startGameTimer();

        // Play game start and theme music
        soundManager.playGameStart();
        soundManager.playGameTheme();
    }

    private void initKeyMap() {
        KeyCode[] digitKeys = {
                KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6,
                KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9
        };

        KeyCode[] numpadKeys = {
                KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3,
                KeyCode.NUMPAD4, KeyCode.NUMPAD5, KeyCode.NUMPAD6,
                KeyCode.NUMPAD7, KeyCode.NUMPAD8, KeyCode.NUMPAD9
        };

        int idx = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                String itemName = ITEM_MATRIX[r][c];
                // Map both digit and numpad keys to the same item
                keyToItem.put(digitKeys[idx], itemName);
                keyToItem.put(numpadKeys[idx], itemName);
                idx++;
            }
        }
    }

    /**
     * Handle keyboard input with enhanced visual feedback
     */
    private void handleKeyPress(KeyCode kc) {
        if (currentSequence.isEmpty()) return;

        String expected = currentSequence.get(currentIndex);
        String pressed = keyToItem.get(kc);

        // Get the cell that was pressed
        VBox pressedCell = keyToCellMap.get(kc);

        if (pressed != null && pressed.equals(expected)) {
            highlightCorrectCell(pressedCell);

            currentIndex++;
            requestLabel.setText(renderSequence(currentSequence, currentIndex));
            flashRequestProgress();
            setCustomerEmotion("happy");

            // Play pickup sound for each correct item
            soundManager.playPickup();

            myScore++;
            updateScoreLabels();

            if (currentIndex >= currentSequence.size()) {
                soundManager.playCorrect();

                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    resetAllCellColors();
                    startNewRound();
                });
            }
        } else {
            flashWrongCell(pressedCell);
            myScore = Math.max(0, myScore - 1);
            updateScoreLabels();
            shakeRequest();
            setCustomerEmotion("angry");
            soundManager.playWrong();
        }
    }

    /**
     * Highlight a cell as correct (green glow effect)
     */
    private void highlightCorrectCell(VBox cell) {
        if (cell == null) return;

        // Change background to green with glowing effect
        cell.setBackground(new Background(new BackgroundFill(
                CORRECT_BG_COLOR,
                new CornerRadii(12),
                Insets.EMPTY
        )));

        // Add glowing border effect
        DropShadow glow = new DropShadow();
        glow.setColor(CORRECT_BORDER_COLOR);
        glow.setRadius(15);
        glow.setSpread(0.6);
        cell.setEffect(glow);

        // Add scale animation
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), cell);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), cell);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        SequentialTransition seq = new SequentialTransition(scaleUp, scaleDown);
        seq.play();
    }

    /**
     * Flash a cell as wrong (red blink effect) then reset to normal
     */
    private void flashWrongCell(VBox cell) {
        if (cell == null) return;

        // Create blinking animation
        Timeline blink = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> {
                            cell.setBackground(new Background(new BackgroundFill(
                                    WRONG_BG_COLOR,
                                    new CornerRadii(12),
                                    Insets.EMPTY
                            )));
                            DropShadow errorGlow = new DropShadow();
                            errorGlow.setColor(WRONG_BORDER_COLOR);
                            errorGlow.setRadius(15);
                            errorGlow.setSpread(0.6);
                            cell.setEffect(errorGlow);
                        }
                ),
                new KeyFrame(Duration.millis(200),
                        e -> resetCellColor(cell)
                ),
                new KeyFrame(Duration.millis(400),
                        e -> {
                            cell.setBackground(new Background(new BackgroundFill(
                                    WRONG_BG_COLOR,
                                    new CornerRadii(12),
                                    Insets.EMPTY
                            )));
                            DropShadow errorGlow = new DropShadow();
                            errorGlow.setColor(WRONG_BORDER_COLOR);
                            errorGlow.setRadius(15);
                            errorGlow.setSpread(0.6);
                            cell.setEffect(errorGlow);
                        }
                ),
                new KeyFrame(Duration.millis(600),
                        e -> resetCellColor(cell)
                )
        );
        blink.setCycleCount(1);
        blink.play();

        // Shake animation
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), cell);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }

    /**
     * Reset a single cell to default appearance
     */
    private void resetCellColor(VBox cell) {
        if (cell == null) return;

        cell.setBackground(new Background(new BackgroundFill(
                DEFAULT_BG_COLOR,
                new CornerRadii(12),
                Insets.EMPTY
        )));
        cell.setEffect(new DropShadow(6, Color.gray(0, 0.15)));
    }

    /**
     * Reset all cells to default appearance
     */
    private void resetAllCellColors() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                resetCellColor(gridCells[r][c]);
            }
        }

        // Also reset request label to default
        requestLabel.setTextFill(Color.web("#2c3e50"));
        requestLabel.setStyle(
                "-fx-font-weight: bold; " +
                        "-fx-background-color: #fef9e7; " +
                        "-fx-border-color: #34495e; " +
                        "-fx-border-width: 4px; " +
                        "-fx-border-style: solid; " +
                        "-fx-border-insets: 0; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 3, 3);"
        );
    }

    private void startNewRound() {
        if (gameEnded) {
            System.out.println("startNewRound called but game has ended - skipping");
            return;
        }

        currentSequence = genSeq();
        currentIndex = 0;
        roundStartMillis = System.currentTimeMillis();
        requestLabel.setText(renderSequence(currentSequence, currentIndex));
        resetAllCellColors();
        setCustomerEmotion("neutral");

        if (roundTimer != null) roundTimer.stop();
        roundTimer = new Timeline(new KeyFrame(Duration.millis(50), ev -> {
            double el = (System.currentTimeMillis() - roundStartMillis) / 1000.0;
            double rem = allowedTimeSeconds - el;
            if (rem <= 0) {
                roundTimer.stop();
                onRoundTimeout();
            } else {
                customerBar.setProgress(rem / allowedTimeSeconds);
                customerTimerLabel.setText(String.format("%.1fs", rem));
            }
        }));
        roundTimer.setCycleCount(Timeline.INDEFINITE);
        roundTimer.play();

        if (hudTicker == null || hudTicker.getStatus() != Timeline.Status.RUNNING) {
            hudTicker = new Timeline(new KeyFrame(Duration.millis(100), ev -> {
                long played = System.currentTimeMillis() - gameStartMillis;
                double sec = played / 1000.0;
                if (sec >= 15 && allowedTimeSeconds > MIN_ALLOWED) {
                    allowedTimeSeconds = Math.max(MIN_ALLOWED, allowedTimeSeconds - 0.1);
                    timeLabel.setText(String.format("Time/Req: %.1fs", allowedTimeSeconds));
                }
            }));
            hudTicker.setCycleCount(Timeline.INDEFINITE);
            hudTicker.play();
        }
    }

    /**
     * Called when time runs out for current round
     * Reset all cells even if incomplete
     */
    private void onRoundTimeout() {
        if (gameEnded) return;

        setCustomerEmotion("angry");
        shakeRequest();

        // Reset all cell colors when timeout
        resetAllCellColors();

        // Start new round after brief delay
        PauseTransition pause = new PauseTransition(Duration.millis(800));
        pause.setOnFinished(e -> {
            if (!gameEnded) {
                startNewRound();
            }
        });
        pause.play();
    }

    private void startGameTimer() {
        gameStartMillis = System.currentTimeMillis();
        gameEnded = false;
        gameTimer = new Timeline(new KeyFrame(Duration.millis(100), ev -> {
            long elapsed = System.currentTimeMillis() - gameStartMillis;
            long remaining = (GAME_DURATION_SECONDS * 1000L) - elapsed;

            if (remaining <= 0) {
                gameTimer.stop();
                endGame();
            } else {
                int seconds = (int) (remaining / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                gameTimeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));

                if (remaining < 10000) {
                    gameTimeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: rgba(231, 76, 60, 0.95); -fx-padding: 5 15; -fx-background-radius: 10;");
                }
            }
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();
    }

    private List<String> genSeq() {
        List<String> all = new ArrayList<>();
        for (String[] row : ITEM_MATRIX) {
            all.addAll(Arrays.asList(row));
        }
        Collections.shuffle(all);
        return all.subList(0, Math.min(SEQUENCE_LEN, all.size()));
    }

    private String renderSequence(List<String> seq, int idx) {
        if (seq.isEmpty()) return "Waiting...";
        StringBuilder sb = new StringBuilder("Order: ");
        for (int i = 0; i < seq.size(); i++) {
            if (i == idx) sb.append("➤ ");
            sb.append(seq.get(i));
            if (i < seq.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public void handleGameStart() {
        System.out.println("Game started!");
    }

    public void handleScoreUpdate(Message message) {
        System.out.println("Score update: " + message.getData());
    }

    private void endGame() {
        gameEnded = true;
        stopAllTimers();
        soundManager.stopMusic();

        // Reset all cells when game ends
        resetAllCellColors();

        VBox endScreen = new VBox(20);
        endScreen.setAlignment(Pos.CENTER);
        endScreen.setPadding(new Insets(40));
        endScreen.setStyle("-fx-background-color: rgba(44, 62, 80, 0.95);");

        Label gameOverLabel = new Label("TIME'S UP!");
        gameOverLabel.setFont(Font.font("Courier New", 42));
        gameOverLabel.setTextFill(Color.web("#e74c3c"));
        gameOverLabel.setStyle("-fx-font-weight: bold;");

        Label finalScoreLabel = new Label("Final Score: " + myScore);
        finalScoreLabel.setFont(Font.font(32));
        finalScoreLabel.setTextFill(Color.WHITE);
        finalScoreLabel.setStyle("-fx-font-weight: bold;");

        Label performanceLabel = new Label(getPerformanceMessage(myScore));
        performanceLabel.setFont(Font.font(20));
        performanceLabel.setTextFill(Color.web("#f39c12"));
        performanceLabel.setWrapText(true);
        performanceLabel.setMaxWidth(500);
        performanceLabel.setAlignment(Pos.CENTER);

        Button playAgainBtn = new Button("Play Again");
        playAgainBtn.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-background-color: #27ae60; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15 30; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand;"
        );
        playAgainBtn.setOnAction(e -> {
            show(isSinglePlayer);
        });

        Button menuBtn = new Button("Back to Menu");
        menuBtn.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15 30; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand;"
        );
        menuBtn.setOnAction(e -> {
            soundManager.stopMusic();
            Platform.runLater(() -> {
                if (onBackToMenu != null) {
                    onBackToMenu.run();
                }
            });
        });

        HBox buttonBox = new HBox(20, playAgainBtn, menuBtn);
        buttonBox.setAlignment(Pos.CENTER);

        endScreen.getChildren().addAll(
                gameOverLabel,
                finalScoreLabel,
                performanceLabel,
                buttonBox
        );
        Scene endScene = new Scene(endScreen, 900, 800);
        primaryStage.setScene(endScene);
        primaryStage.show();
        try {
            soundManager.playGameOver();
        } catch (Exception e) {
            // Sound not available, ignore
        }
    }

    private String getPerformanceMessage(int score) {
        if (score >= 20) {
            return "EXCELLENT! You're a supermarket master!";
        } else if (score >= 15) {
            return "GREAT JOB! Keep it up!";
        } else if (score >= 10) {
            return "GOOD! You're getting better!";
        } else if (score >= 5) {
            return "NOT BAD! Practice makes perfect!";
        } else {
            return "KEEP TRYING! You can do better!";
        }
    }

    private void stopAllTimers() {
        if (roundTimer != null) roundTimer.stop();
        if (hudTicker != null) hudTicker.stop();
        if (gameTimer != null) gameTimer.stop();
    }

    private void setCustomerEmotion(String emotion) {
        Image img = AssetManager.getImage("customer_" + emotion);
        if (img != null) {
            customerImage.setImage(img);
        }
    }

    // ====== UI helpers ======
    private Label mkTag(String text) {
        Label l = new Label(text);
        l.setFont(Font.font(16));
        l.setTextFill(Color.web("#2c3e50"));
        return l;
    }

    private VBox mkItemCell(int num, String name) {
        Label k = new Label("#" + num);
        k.setFont(Font.font(14));
        k.setTextFill(Color.web("#95a5a6"));

        Label n = new Label(name);
        n.setFont(Font.font(18));
        n.setTextFill(Color.web("#34495e"));

        Image img = AssetManager.getItemImage(name.toLowerCase());
        ImageView iv;
        if (img != null) {
            iv = new ImageView(img);
        } else {
            Label placeholder = new Label("📦");
            placeholder.setFont(Font.font(48));
            placeholder.setTextFill(Color.web("#3498db"));
            VBox box = new VBox(6, k, placeholder, n);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(10));
            box.setPrefSize(120, 120);
            box.setBackground(new Background(new BackgroundFill(DEFAULT_BG_COLOR, new CornerRadii(12), Insets.EMPTY)));
            box.setEffect(new DropShadow(6, Color.gray(0, 0.15)));
            return box;
        }

        iv.setFitWidth(64);
        iv.setFitHeight(64);

        VBox box = new VBox(6, k, iv, n);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setPrefSize(120, 120);
        box.setBackground(new Background(new BackgroundFill(DEFAULT_BG_COLOR, new CornerRadii(12), Insets.EMPTY)));
        box.setEffect(new DropShadow(6, Color.gray(0, 0.15)));
        return box;
    }

    private void updateScoreLabels() {
        scoreLabel.setText("Your Score: " + myScore);
        opponentScoreLabel.setText("Opponent: " + opponentScore);
    }

    private void flashRequestProgress() {
        requestLabel.setTextFill(Color.web("#27ae60"));
        requestLabel.setStyle(
            "-fx-font-weight: bold; " +
            "-fx-background-color: #d5f4e6; " +
            "-fx-border-color: #27ae60; " +
            "-fx-border-width: 4px; " +
            "-fx-border-style: solid; " +
            "-fx-border-insets: 0; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(39,174,96,0.6), 8, 0, 3, 3);"
        );
    }

    private void shakeRequest() {
        requestLabel.setTextFill(Color.web("#e74c3c"));
        requestLabel.setStyle(
            "-fx-font-weight: bold; " +
            "-fx-background-color: #fadbd8; " +
            "-fx-border-color: #e74c3c; " +
            "-fx-border-width: 4px; " +
            "-fx-border-style: solid; " +
            "-fx-border-insets: 0; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.6), 8, 0, 3, 3);"
        );
    }
}