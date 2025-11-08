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
import java.util.concurrent.ThreadLocalRandom;

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

    // ====== Giữ nguyên các field UI đã có trong project ======
    private Label scoreLabel;
    private Label opponentScoreLabel;
    private Label timeLabel;
    private Label requestLabel;
    private Label customerTimerLabel;
    private ProgressBar customerBar;
    private ImageView customerImage;
    private VBox root; // giả định layout hiện có
    private HBox itemsRow; // thanh/khung hiện vật phẩm, vẫn hiển thị nhưng bỏ click
    private SoundManager soundManager;


    // ====== Gameplay state (MỚI) ======
    // Ma trận cố định 3x3 tất cả vật phẩm: ánh xạ phím 1..9 (hàng-trước-cột)
    // Ví dụ: chị có thể thay thế tên cho khớp asset thực tế trong AssetManager
    private static final String[][] ITEM_MATRIX = {
            {"MILK",    "BREAD",   "APPLE"},
            {"CARROT",  "ORANGE",  "EGGS"},
            {"CHEESE",  "MEAT",    "SODA"}
    };

    // Map phím số -> item (1..9 theo thứ tự: trên xuống, trái sang phải)
    private final Map<KeyCode, String> keyToItem = new HashMap<>();

    // Yêu cầu hiện tại (list string) và chỉ số đang cần nhập
    private List<String> currentSequence = new ArrayList<>();
    private int currentIndex = 0;

    // Điểm & thời gian
    private int myScore = 0;
    private int opponentScore = 0; // vẫn giữ để hiển thị
    private long gameStartMillis = 0L;
    private long roundStartMillis = 0L;

    // Thời gian cho mỗi yêu cầu (theo độ khó, tự giảm)
    private double allowedTimeSeconds = 5.0; // mặc định
    private Timeline roundTimer;              // đếm ngược từng yêu cầu
    private Timeline hudTicker;               // cập nhật HUD mỗi 100ms
    private Timeline gameTimer;               // đếm ngược thời gian chơi tổng

    // Các cấu hình nhỏ
    private static final int SEQUENCE_LEN = 4; // độ dài list yêu cầu (có thể chỉnh)
    private static final double MIN_ALLOWED = 2.0;
    private static final int GAME_DURATION_SECONDS = 60; // Thời gian chơi: 1 phút

    private boolean isSinglePlayer = true;
    private Label gameTimeLabel; // Hiển thị thời gian còn lại của màn chơi
    private boolean gameEnded = false;

    // Constructor
    public ImprovedGameController(Stage stage, Runnable onBackToMenu) {
        this.primaryStage = stage;
        this.onBackToMenu = onBackToMenu;
        this.soundManager = SoundManager.getInstance();
    }

    // ====== Public API (GIỮ NGUYÊN TÊN) ======

    /** Màn chơi chính */
    public void show(boolean isSinglePlayer) {
        this.isSinglePlayer = isSinglePlayer;
        // -- xây UI (giữ cấu trúc cũ, chỉ tóm tắt phần không ảnh hưởng logic) --
        root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Thêm ảnh nền
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

        Label title = new Label("🏪 Supermarket Game");
        title.setFont(Font.font(28));
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 2);");

        HBox scoreBox = new HBox(24);
        scoreBox.setAlignment(Pos.CENTER);
        scoreLabel = mkTag("Your Score: 0");
        opponentScoreLabel = mkTag("Opponent: 0");
        timeLabel = mkTag("Time/Req: 5.0s");

        // Thêm game timer (thời gian còn lại của màn chơi)
        gameTimeLabel = new Label("⏱️ Time: 1:00");
        gameTimeLabel.setFont(Font.font(20));
        gameTimeLabel.setTextFill(Color.WHITE);
        gameTimeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: rgba(231, 76, 60, 0.8); -fx-padding: 5 15; -fx-background-radius: 10;");

        scoreBox.getChildren().addAll(scoreLabel, opponentScoreLabel, timeLabel, gameTimeLabel);

        // Load customer image - bắt đầu với neutral (chuyển lên trên)
        customerImage = new ImageView();
        customerImage.setFitWidth(120);
        customerImage.setFitHeight(120);
        setCustomerEmotion("neutral");

        // Thêm viền pixel cho customer image
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

        // Pixel-style order list with decorative border
        requestLabel = new Label("Waiting for game to start...");
        requestLabel.setFont(Font.font("Courier New", 22)); // Pixel-style monospace font
        requestLabel.setTextFill(Color.web("#2c3e50"));
        requestLabel.setWrapText(true);
        requestLabel.setMaxWidth(500);
        requestLabel.setPadding(new Insets(15, 20, 15, 20));
        requestLabel.setAlignment(Pos.CENTER);
        // Pixel-style border with retro gaming colors
        requestLabel.setStyle(
            "-fx-font-weight: bold; " +
            "-fx-background-color: #fef9e7; " +
            "-fx-border-color: #34495e; " +
            "-fx-border-width: 4px; " +
            "-fx-border-style: solid; " +
            "-fx-border-insets: 0; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 3, 3);"
        );

        // HBox để đặt customer và order list cạnh nhau
        HBox topGameArea = new HBox(20, customerBox, requestLabel);
        topGameArea.setAlignment(Pos.CENTER);
        topGameArea.setPadding(new Insets(10, 0, 10, 0));

        // Hiển thị bảng 3x3 cố định – mỗi ô gán #n và tên item; bỏ click chuột
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        int id = 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                String name = ITEM_MATRIX[r][c];
                VBox cell = mkItemCell(id, name);
                grid.add(cell, c, r);
                id++;
            }
        }

        // Thanh tiến độ (đặt bên dưới grid)
        customerBar = new ProgressBar(1);
        customerBar.setPrefWidth(420);
        customerTimerLabel = new Label("");
        customerTimerLabel.setFont(Font.font(14));
        customerTimerLabel.setTextFill(Color.WHITE);

        HBox progressBox = new HBox(12, customerBar, customerTimerLabel);
        progressBox.setAlignment(Pos.CENTER);

        // Add back button
        Button backButton = new Button("🔙 Back to Menu");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 8 15;");
        backButton.setOnAction(e -> {
            stopAllTimers();
            onBackToMenu.run();
            soundManager.stopMusic();
        });

        root.getChildren().addAll(title, scoreBox, topGameArea, grid, progressBox, backButton);

        Scene scene = new Scene(root, 820, 640);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Map phím 1..9 vào item
        initKeyMap();

        // Đăng ký handler phím – bỏ hoàn toàn click chuột
        scene.setOnKeyPressed(evt -> handleKey(evt.getCode()));

        // Bắt đầu game
        handleGameStart();
        soundManager.playGameStart();
    }

    /** Bắt đầu game – GIỮ TÊN */
    public void handleGameStart() {
        myScore = 0;
        opponentScore = 0;
        gameEnded = false;
        updateScoreLabels();

        gameStartMillis = System.currentTimeMillis();
        allowedTimeSeconds = 5.0;

        soundManager.playGameTheme();
        // Stop existing timers
        stopAllTimers();

        // HUD ticker - cập nhật mỗi 100ms
        hudTicker = new Timeline(
                new KeyFrame(Duration.millis(100), e -> tickHud()));
        hudTicker.setCycleCount(Animation.INDEFINITE);
        hudTicker.play();

        // Game timer - đếm ngược thời gian chơi (60 giây)
        gameTimer = new Timeline(
                new KeyFrame(Duration.millis(100), e -> updateGameTimer()));
        gameTimer.setCycleCount(Animation.INDEFINITE);
        gameTimer.play();

        nextRequest();
        setCustomerEmotion("neutral");
    }

    /** Cập nhật điểm từ server – GIỮ TÊN */
    public void handleScoreUpdate(Message message) {
        // Có thể parse message để cập nhật opponentScore nếu server gửi
        // Ở client demo: chỉ in log để giữ API
        System.out.println("Score update: " + message.getData());
    }

    /** Server báo đúng item – GIỮ TÊN */
    public void handleItemCorrect(Message message) {
        // Trong luật mới, điểm chỉ + khi hoàn tất cả chuỗi
        // Giữ nguyên để không phá API; không cộng lẻ theo item nữa
        System.out.println("Correct (per-item) ignored – using per-sequence scoring.");
    }

    /** Server báo sai item – GIỮ TÊN */
    public void handleItemWrong(Message message) {
        // Giữ API, nhưng logic trừ điểm đã chuyển sang handleKey()
        System.out.println("Wrong (per-item) handled locally.");
    }

    // ====== Logic gameplay MỚI ======

    /** Tạo map phím 1..9 vào item theo ma trận cố định */
    private void initKeyMap() {
        KeyCode[] keys = {
                KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6,
                KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9
        };
        int idx = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                keyToItem.put(keys[idx++], ITEM_MATRIX[r][c]);
            }
        }
        // Trên keypad số (nếu máy có)
        KeyCode[] numpad = {
                KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3,
                KeyCode.NUMPAD4, KeyCode.NUMPAD5, KeyCode.NUMPAD6,
                KeyCode.NUMPAD7, KeyCode.NUMPAD8, KeyCode.NUMPAD9
        };
        idx = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                keyToItem.put(numpad[idx++], ITEM_MATRIX[r][c]);
            }
        }
    }

    /** Xử lý khi người chơi bấm phím */
    private void handleKey(KeyCode code) {
        if (gameEnded || !keyToItem.containsKey(code)) return;

        String expect = currentSequence.get(currentIndex);
        String got = keyToItem.get(code);
        if (got.equals(expect)) {
            // đúng vị trí
            currentIndex++;
            flashRequestProgress();
            setCustomerEmotion("happy"); // Customer vui
            soundManager.playPickup();

            if (currentIndex >= currentSequence.size()) {
                // hoàn tất chuỗi -> +1 điểm, chuyển yêu cầu mới
                myScore += 1;
                updateScoreLabels();
                soundManager.playCorrect();
                nextRequest();
            }
        } else {
            // sai -> trừ 1 điểm, không chuyển yêu cầu
            myScore = Math.max(0, myScore - 1);
            updateScoreLabels();
            shakeRequest();
            setCustomerEmotion("angry"); // Customer tức giận
            soundManager.playWrong();
        }
    }

    /** Sinh chuỗi yêu cầu ngẫu nhiên (string) theo ma trận cố định */
    private List<String> generateSequence(int len) {
        List<String> flat = new ArrayList<>(9);
        for (String[] row : ITEM_MATRIX) flat.addAll(Arrays.asList(row));

        List<String> seq = new ArrayList<>(len);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            seq.add(flat.get(rnd.nextInt(flat.size())));
        }
        return seq;
    }

    /** Tính allowedTimeSeconds theo độ khó: 5s – mỗi 15s giảm 1s, tối thiểu 1s */
    private void recomputeAllowedTime() {
        long elapsed = (System.currentTimeMillis() - gameStartMillis) / 1000; // s
        long steps = elapsed / 15; // mỗi 15s giảm 1
        double t = 15.0 - steps;
        allowedTimeSeconds = Math.max(MIN_ALLOWED, t);
        timeLabel.setText(String.format("Time/Req: %.1fs", allowedTimeSeconds));
    }

    /** Bắt đầu một yêu cầu mới */
    private void nextRequest() {
        // Chốt độ khó tại thời điểm ra đề
        recomputeAllowedTime();

        currentSequence = generateSequence(SEQUENCE_LEN);
        currentIndex = 0;
        requestLabel.setText(renderSequence(currentSequence, currentIndex));
        requestLabel.setTextFill(Color.web("#2c3e50"));
        // Reset về style mặc định
        requestLabel.setStyle(
            "-fx-font-weight: bold; " +
            "-fx-background-color: #fef9e7; " +
            "-fx-border-color: #34495e; " +
            "-fx-border-width: 4px; " +
            "-fx-border-style: solid; " +
            "-fx-border-insets: 0; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 3, 3);"
        );

        // Reset/bắt timer cho yêu cầu này
        if (roundTimer != null) roundTimer.stop();
        roundStartMillis = System.currentTimeMillis();

        roundTimer = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateRoundCountdown()),
                new KeyFrame(Duration.millis(100))
        );
        roundTimer.setCycleCount(Animation.INDEFINITE);
        roundTimer.play();
    }

    /** Hiển thị chuỗi yêu cầu, đánh dấu tiến độ (đã nhập/đang chờ) */
    private String renderSequence(List<String> seq, int index) {
        StringBuilder sb = new StringBuilder("Order: ");
        for (int i = 0; i < seq.size(); i++) {
            if (i == index) {
                sb.append("[").append(seq.get(i)).append("]");
            } else {
                sb.append(seq.get(i));
            }
            if (i < seq.size() - 1) sb.append("  →  ");
        }
        return sb.toString();
    }

    /** Mỗi 100ms cập nhật HUD, giảm allowedTime theo mốc 15s */
    private void tickHud() {
        recomputeAllowedTime(); // để label luôn phản ánh độ khó hiện tại
    }

    /** Cập nhật đồng hồ cho yêu cầu hiện tại; hết giờ -> chuyển đề KHÔNG trừ điểm */
    private void updateRoundCountdown() {
        if (gameEnded) return;
        
        long elapsedMs = System.currentTimeMillis() - roundStartMillis;
        double remain = allowedTimeSeconds - (elapsedMs / 1000.0);
        if (remain <= 0) {
            // Hết thời gian của yêu cầu này: KHÔNG trừ điểm, chỉ chuyển yêu cầu mới
            nextRequest();
            setCustomerEmotion("neutral");
            return;
        }
        customerTimerLabel.setText(String.format("Remain: %.1fs", Math.max(0, remain)));
        customerBar.setProgress(Math.max(0, remain / Math.max(1.0, allowedTimeSeconds)));
        // cập nhật tiến độ trong label
        requestLabel.setText(renderSequence(currentSequence, currentIndex));
    }
    
    /** Cập nhật thời gian còn lại của màn chơi (60 giây) */
    private void updateGameTimer() {
        if (gameEnded) return;
        
        long elapsedMs = System.currentTimeMillis() - gameStartMillis;
        double elapsedSeconds = elapsedMs / 1000.0;
        double remainSeconds = GAME_DURATION_SECONDS - elapsedSeconds;
        
        if (remainSeconds <= 0) {
            // Hết thời gian chơi -> kết thúc game
            endGame();
            return;
        }
        
        // Hiển thị dạng MM:SS
        int minutes = (int) remainSeconds / 60;
        int seconds = (int) remainSeconds % 60;
        gameTimeLabel.setText(String.format("⏱️ Time: %d:%02d", minutes, seconds));
        
        // Đổi màu khi còn ít thời gian
        if (remainSeconds < 10) {
            gameTimeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: rgba(192, 57, 43, 0.9); -fx-padding: 5 15; -fx-background-radius: 10; -fx-text-fill: white;");
        } else if (remainSeconds < 30) {
            gameTimeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: rgba(230, 126, 34, 0.8); -fx-padding: 5 15; -fx-background-radius: 10; -fx-text-fill: white;");
        }
    }
    
    /** Kết thúc game */
    private void endGame() {
        gameEnded = true;
        stopAllTimers();
        soundManager.stopMusic();
        
        // Hiển thị màn hình game over
        Platform.runLater(() -> {
            showGameOverScreen();
        });
    }
    
    /** Hiển thị màn hình game over */
    private void showGameOverScreen() {
        VBox gameOverRoot = new VBox(30);
        gameOverRoot.setAlignment(Pos.CENTER);
        gameOverRoot.setPadding(new Insets(50));
        
        // Thêm ảnh nền
        Image bgImage = AssetManager.getImage("bg_game");
        if (bgImage != null) {
            BackgroundImage background = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, true)
            );
            gameOverRoot.setBackground(new Background(background));
        } else {
            gameOverRoot.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");
        }
        
        // Game Over Title
        Label gameOverTitle = new Label("⏱️ TIME'S UP!");
        gameOverTitle.setFont(Font.font("Arial", 60));
        gameOverTitle.setTextFill(Color.web("#e74c3c"));
        gameOverTitle.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 15, 0, 0, 3);");
        
        // Score Panel
        VBox scorePanel = new VBox(15);
        scorePanel.setAlignment(Pos.CENTER);
        scorePanel.setPadding(new Insets(30));
        scorePanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);");
        
        Label finalScoreLabel = new Label("FINAL SCORE");
        finalScoreLabel.setFont(Font.font("Arial", 24));
        finalScoreLabel.setTextFill(Color.web("#7f8c8d"));
        
        Label scoreValue = new Label(String.valueOf(myScore));
        scoreValue.setFont(Font.font("Arial", 72));
        scoreValue.setTextFill(Color.web("#2c3e50"));
        scoreValue.setStyle("-fx-font-weight: bold;");
        
        Label pointsLabel = new Label("points");
        pointsLabel.setFont(Font.font("Arial", 20));
        pointsLabel.setTextFill(Color.web("#95a5a6"));
        
        // Hiển thị đánh giá
        Label performanceLabel = new Label(getPerformanceMessage(myScore));
        performanceLabel.setFont(Font.font("Arial", 18));
        performanceLabel.setTextFill(Color.web("#3498db"));
        performanceLabel.setStyle("-fx-font-style: italic;");
        
        scorePanel.getChildren().addAll(finalScoreLabel, scoreValue, pointsLabel, performanceLabel);
        
        // Buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button playAgainBtn = new Button("🔄 Play Again");
        playAgainBtn.setFont(Font.font("Arial", 18));
        playAgainBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        playAgainBtn.setOnMouseEntered(e -> playAgainBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;"));
        playAgainBtn.setOnMouseExited(e -> playAgainBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;"));
        playAgainBtn.setOnAction(e -> {
            show(isSinglePlayer); // Restart game
        });
        
        Button mainMenuBtn = new Button("🏠 Main Menu");
        mainMenuBtn.setFont(Font.font("Arial", 18));
        mainMenuBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        mainMenuBtn.setOnMouseEntered(e -> mainMenuBtn.setStyle("-fx-background-color: #5dade2; -fx-text-fill: white; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;"));
        mainMenuBtn.setOnMouseExited(e -> mainMenuBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;"));
        mainMenuBtn.setOnAction(e -> {
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });
        
        buttonBox.getChildren().addAll(playAgainBtn, mainMenuBtn);
        
        gameOverRoot.getChildren().addAll(gameOverTitle, scorePanel, buttonBox);
        
        Scene gameOverScene = new Scene(gameOverRoot, 820, 640);
        primaryStage.setScene(gameOverScene);
        primaryStage.show();
        
        // Play game over sound if available
        try {
            soundManager.playGameOver();
        } catch (Exception e) {
            // Sound not available, ignore
        }
    }
    
    /** Get performance message based on score */
    private String getPerformanceMessage(int score) {
        if (score >= 20) {
            return "🌟 EXCELLENT! You're a supermarket master!";
        } else if (score >= 15) {
            return "🎉 GREAT JOB! Keep it up!";
        } else if (score >= 10) {
            return "👍 GOOD! You're getting better!";
        } else if (score >= 5) {
            return "💪 NOT BAD! Practice makes perfect!";
        } else {
            return "🎯 KEEP TRYING! You can do better!";
        }
    }
    
    /** Dừng tất cả timers */
    private void stopAllTimers() {
        if (roundTimer != null) roundTimer.stop();
        if (hudTicker != null) hudTicker.stop();
        if (gameTimer != null) gameTimer.stop();
    }
    
    /** Set customer emotion (happy/neutral/angry) */
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

        // Try to load image, use placeholder if not found
        Image img = AssetManager.getItemImage(name.toLowerCase());
        ImageView iv;
        
        if (img != null) {
            iv = new ImageView(img);
        } else {
            // Create a colored rectangle as placeholder
            Label placeholder = new Label("📦");
            placeholder.setFont(Font.font(48));
            placeholder.setTextFill(Color.web("#3498db"));
            VBox box = new VBox(6, k, placeholder, n);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(10));
            box.setPrefSize(120, 120);
            box.setBackground(new Background(new BackgroundFill(Color.web("#ecf0f1"), new CornerRadii(12), Insets.EMPTY)));
            box.setEffect(new DropShadow(6, Color.gray(0, 0.15)));
            return box;
        }
        
        iv.setFitWidth(64);
        iv.setFitHeight(64);

        VBox box = new VBox(6, k, iv, n);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setPrefSize(120, 120);
        box.setBackground(new Background(new BackgroundFill(Color.web("#ecf0f1"), new CornerRadii(12), Insets.EMPTY)));
        box.setEffect(new DropShadow(6, Color.gray(0, 0.15)));
        // KHÔNG đăng ký onMouseClicked -> bỏ click chuột
        return box;
    }

    private void updateScoreLabels() {
        scoreLabel.setText("Your Score: " + myScore);
        opponentScoreLabel.setText("Opponent: " + opponentScore);
    }

    private void flashRequestProgress() {
        requestLabel.setTextFill(Color.web("#27ae60")); // Green for correct
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
        requestLabel.setTextFill(Color.web("#e74c3c")); // Red for wrong
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

    // ====== Giữ nguyên chữ ký phương thức cũ (nếu có) ======

    /** Ví dụ: vẫn trả emoji nếu project cũ gọi tới (không ảnh hưởng gameplay) */
    private String getEmojiForItem(String itemName) {
        // Fallback if AssetManager doesn't have emoji method
        return "📦";
    }
    
    // ====== Methods called from Main.java ======
    
    /** Called when receiving NEW_REQUEST from server (multiplayer) */
    public void handleNewRequest(Message message) {
        // In multiplayer mode, server sends the new request
        if (!isSinglePlayer) {
            String data = message.getData().toString();
            String[] items = data.split(",");
            currentSequence = new ArrayList<>(Arrays.asList(items));
            currentIndex = 0;
            requestLabel.setText(renderSequence(currentSequence, currentIndex));
        }
    }
//
//    /** Called when receiving ITEM_RESULT from server */
//    public void handleItemCorrect(Message message) {
//        // Server confirms item was correct
//        System.out.println("✓ Server confirmed correct item");
//    }
//
//    /** Called when receiving ITEM_WRONG from server */
//    public void handleItemWrong(Message message) {
//        // Server says wrong item
//        System.out.println("✗ Server says wrong item");
//        shakeRequest();
//    }
    
    /** Called when receiving GAME_STATE from server */
    public void handleGameState(Message message) {
        // Parse game state: remainingItems|timeout|player1:score1|player2:score2
        String data = message.getData().toString();
        String[] parts = data.split("\\|");
        
        if (parts.length >= 3) {
            // Update timeout
            try {
                allowedTimeSeconds = Double.parseDouble(parts[1]);
                timeLabel.setText(String.format("Time/Req: %.1fs", allowedTimeSeconds));
            } catch (NumberFormatException e) {
                // Ignore
            }
            
            // Update scores
            for (int i = 2; i < parts.length; i++) {
                String[] playerScore = parts[i].split(":");
                if (playerScore.length == 2) {
                    String playerName = playerScore[0];
                    int score = Integer.parseInt(playerScore[1]);
                    
                    // Update opponent score (assuming first player is opponent)
                    if (i == 2) {
                        opponentScore = score;
                    }
                }
            }
            updateScoreLabels();
        }
    }
    
    /** Called when game is over */
    public void handleGameOver(Message message) {
        if (roundTimer != null) roundTimer.stop();
        if (hudTicker != null) hudTicker.stop();
        
        String result = message.getData().toString();
        utils.UIHelper.showInfo("Game Over", result);
        
        // Show option to go back to menu
        Platform.runLater(() -> {
            if (onBackToMenu != null) {
                onBackToMenu.run();
            }
        });
    }
}
