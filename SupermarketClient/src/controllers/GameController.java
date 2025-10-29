package controllers;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Message;
import network.NetworkManager;
import utils.UIHelper;

import java.util.*;

/**
 * Game controller - handles game play
 */
public class GameController {
    private Stage stage;
    private Runnable onGameEnd;
    private NetworkManager network;
    
    private int score = 0;
    private int timeLeft = 120;
    private int customerTimeout = 10;
    private int combo = 0;
    
    private Timeline gameTimeline;
    private Timeline customerTimeline;
    private Timeline spawnTimeline;
    
    private Label scoreLabel;
    private Label comboLabel;
    private Label timeLabel;
    private Label customerTimerLabel;
    private Label requestLabel;
    private ProgressBar customerBar;
    
    private boolean isSinglePlayer;
    private String currentRoomId;
    
    public GameController(Stage stage, Runnable onGameEnd) {
        this.stage = stage;
        this.onGameEnd = onGameEnd;
        this.network = NetworkManager.getInstance();
    }
    
    public void show(boolean isSinglePlayer) {
        this.isSinglePlayer = isSinglePlayer;
        // Will start when GAME_START message received if multiplayer
        if (isSinglePlayer) {
            startGame();
        }
    }
    
    public void handleGameStart(Message message) {
        startGame();
    }
    
    public void handleScoreUpdate(Message message) {
        // Can display other players' scores if needed
        System.out.println("Score update: " + message.getData());
    }
    
    private void startGame() {
        score = 0;
        timeLeft = 120;
        customerTimeout = 10;
        combo = 0;
        
        BorderPane root = new BorderPane();
        root.setStyle(UIHelper.createSolidBackground("#fff5e6"));
        
        // Top bar
        HBox topBar = createTopBar();
        
        // Customer section
        VBox customerBox = createCustomerSection();
        
        // Items grid
        GridPane itemsGrid = createItemsGrid();
        
        root.setTop(topBar);
        root.setCenter(new VBox(20, customerBox, itemsGrid));
        
        Scene scene = new Scene(root, 700, 650);
        stage.setScene(scene);
        
        // Start timers
        startGameTimers();
    }
    
    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-background-color: #ff6b6b;");
        
        scoreLabel = UIHelper.createLabel("💰 Score: 0", 20, Color.WHITE);
        comboLabel = UIHelper.createLabel("🔥 Combo: x1", 20, Color.web("#ffd700"));
        timeLabel = UIHelper.createLabel("⏰ Time: 120s", 20, Color.WHITE);
        
        topBar.getChildren().addAll(scoreLabel, comboLabel, new Label("  "), timeLabel);
        return topBar;
    }
    
    private VBox createCustomerSection() {
        VBox customerBox = new VBox(10);
        customerBox.setAlignment(Pos.CENTER);
        customerBox.setPadding(new Insets(20));
        
        Text customerText = new Text("👤 Customer wants:");
        customerText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        requestLabel = new Label("🥛 MILK");
        requestLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        requestLabel.setStyle("-fx-background-color: #ffe66d; -fx-padding: 15px; -fx-background-radius: 10px;");
        
        customerBar = new ProgressBar(1.0);
        customerBar.setPrefWidth(300);
        customerBar.setStyle("-fx-accent: #4CAF50;");
        
        customerTimerLabel = UIHelper.createLabel("⏱ 10s", 16, Color.web("#e74c3c"));
        
        customerBox.getChildren().addAll(customerText, requestLabel, customerBar, customerTimerLabel);
        return customerBox;
    }
    
    private GridPane createItemsGrid() {
        GridPane itemsGrid = new GridPane();
        itemsGrid.setAlignment(Pos.CENTER);
        itemsGrid.setHgap(15);
        itemsGrid.setVgap(15);
        itemsGrid.setPadding(new Insets(30));
        
        String[] allItems = {"🥛 MILK", "🍞 BREAD", "🍎 APPLE", "🥕 CARROT", 
                            "🍊 ORANGE", "🥚 EGGS", "🧀 CHEESE", "🥩 MEAT", "🥤 SODA"};
        
        List<Button> itemButtons = new ArrayList<>();
        Map<Button, Boolean> itemAvailable = new HashMap<>();
        Random random = new Random();
        
        String[] currentRequest = {allItems[random.nextInt(allItems.length)]};
        requestLabel.setText(currentRequest[0]);
        
        for (int i = 0; i < 9; i++) {
            String item = allItems[i];
            Button itemBtn = UIHelper.createItemButton("?", 120);
            itemBtn.setDisable(true);
            itemAvailable.put(itemBtn, false);
            
            itemBtn.setOnAction(e -> handleItemClick(itemBtn, itemAvailable, currentRequest, 
                                                     allItems, random));
            
            itemsGrid.add(itemBtn, i % 3, i / 3);
            itemButtons.add(itemBtn);
            itemBtn.setUserData(item);
        }
        
        // Spawn timeline
        spawnTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> 
            spawnItems(itemButtons, itemAvailable, random)));
        spawnTimeline.setCycleCount(Timeline.INDEFINITE);
        spawnTimeline.play();
        
        return itemsGrid;
    }
    
    private void handleItemClick(Button btn, Map<Button, Boolean> available, 
                                 String[] currentRequest, String[] allItems, Random random) {
        if (!available.get(btn)) return;
        
        String itemText = btn.getText();
        
        if (itemText.equals(currentRequest[0])) {
            // Correct
            combo++;
            int points = 10 * combo;
            score += points;
            scoreLabel.setText("💰 Score: " + score);
            comboLabel.setText("🔥 Combo: x" + combo);
            
            customerTimeout = 10;
            currentRequest[0] = allItems[random.nextInt(allItems.length)];
            requestLabel.setText(currentRequest[0]);
            
            UIHelper.flashColor(btn, "#27ae60", 200);
            hideItem(btn, available);
            
        } else {
            // Wrong
            combo = 0;
            score = Math.max(0, score - 5);
            scoreLabel.setText("💰 Score: " + score);
            comboLabel.setText("🔥 Combo: x1");
            
            UIHelper.flashColor(btn, "#e74c3c", 300);
        }
    }
    
    private void spawnItems(List<Button> buttons, Map<Button, Boolean> available, Random random) {
        List<Button> hiddenButtons = new ArrayList<>();
        for (Button btn : buttons) {
            if (!available.get(btn)) {
                hiddenButtons.add(btn);
            }
        }
        
        if (!hiddenButtons.isEmpty()) {
            int spawnCount = Math.min(random.nextInt(2) + 1, hiddenButtons.size());
            Collections.shuffle(hiddenButtons);
            
            for (int i = 0; i < spawnCount; i++) {
                Button btn = hiddenButtons.get(i);
                String itemName = (String) btn.getUserData();
                
                btn.setText(itemName);
                btn.setDisable(false);
                available.put(btn, true);
                
                ScaleTransition st = new ScaleTransition(Duration.millis(300), btn);
                st.setFromX(0.1);
                st.setFromY(0.1);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            }
        }
    }
    
    private void hideItem(Button btn, Map<Button, Boolean> available) {
        btn.setDisable(true);
        available.put(btn, false);
        btn.setText("?");
        btn.setStyle("-fx-font-size: 24px; -fx-background-color: #bdc3c7; " +
                   "-fx-text-fill: #7f8c8d; -fx-background-radius: 10px;");
    }
    
    private void startGameTimers() {
        // Customer timeout
        customerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            customerTimeout--;
            customerTimerLabel.setText("⏱ " + customerTimeout + "s");
            customerBar.setProgress(customerTimeout / 10.0);
            
            if (customerTimeout <= 3) {
                customerBar.setStyle("-fx-accent: #e74c3c;");
            } else if (customerTimeout <= 6) {
                customerBar.setStyle("-fx-accent: #f39c12;");
            } else {
                customerBar.setStyle("-fx-accent: #4CAF50;");
            }
            
            if (customerTimeout <= 0) {
                combo = 0;
                score = Math.max(0, score - 15);
                scoreLabel.setText("💰 Score: " + score);
                comboLabel.setText("🔥 Combo: x1");
                
                customerTimeout = 10;
                requestLabel.setStyle("-fx-background-color: #e74c3c; -fx-padding: 15px; -fx-background-radius: 10px;");
                
                Timeline flash = new Timeline(new KeyFrame(Duration.millis(500), 
                    evt -> requestLabel.setStyle("-fx-background-color: #ffe66d; -fx-padding: 15px; -fx-background-radius: 10px;")));
                flash.play();
            }
        }));
        customerTimeline.setCycleCount(Timeline.INDEFINITE);
        customerTimeline.play();
        
        // Main timer
        gameTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timeLabel.setText("⏰ Time: " + timeLeft + "s");
            
            if (timeLeft <= 10) {
                timeLabel.setTextFill(Color.web("#e74c3c"));
            }
            
            if (timeLeft <= 0) {
                endGame();
            }
        }));
        gameTimeline.setCycleCount(120);
        gameTimeline.play();
    }
    
    private void endGame() {
        if (gameTimeline != null) gameTimeline.stop();
        if (customerTimeline != null) customerTimeline.stop();
        if (spawnTimeline != null) spawnTimeline.stop();
        
        // Send score to server
        String roomId = isSinglePlayer ? "SINGLE" : currentRoomId;
        network.sendScore(roomId, score);
        
        showGameOverScreen();
    }
    
    private void showGameOverScreen() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle(UIHelper.createGradientBackground("#667eea", "#764ba2"));
        
        Text title = new Text("🎮 GAME OVER!");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.WHITE);
        
        Text scoreText = new Text("Your Score: " + score);
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        scoreText.setFill(Color.web("#ffe66d"));
        
        String rating = score >= 500 ? "🏆 LEGENDARY!" :
                       score >= 300 ? "⭐ EXCELLENT!" :
                       score >= 150 ? "👍 GOOD JOB!" : "💪 KEEP TRYING!";
        
        Text ratingText = new Text(rating);
        ratingText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        ratingText.setFill(Color.web("#ffd700"));
        
        Button menuBtn = UIHelper.createButton("← BACK TO MENU", UIHelper.PRIMARY_COLOR);
        menuBtn.setOnAction(e -> onGameEnd.run());
        
        root.getChildren().addAll(title, scoreText, ratingText, menuBtn);
        
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
}
