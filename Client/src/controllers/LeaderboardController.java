package controllers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import models.Message;

public class LeaderboardController {
    private Stage stage;
    private Runnable onBack;
    private VBox leaderboardBox;
    
    public LeaderboardController(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }
    
    public void show() {
        // Root với background image
        StackPane root = new StackPane();
        root.getStyleClass().add("leaderboard-root");

        // Main container
        VBox container = new VBox(15);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(30));
        container.getStyleClass().add("leaderboard-container");

        // Title - pixel style
        Text title = new Text("🏆 LEADERBOARD");
        title.getStyleClass().add("leaderboard-title");

        // Leaderboard box
        leaderboardBox = new VBox(10);
        leaderboardBox.getStyleClass().add("leaderboard-box");

        Label loading = new Label("Loading...");
        loading.getStyleClass().add("loading-label");
        leaderboardBox.getChildren().add(loading);
        
        // Scroll pane
        ScrollPane scrollPane = new ScrollPane(leaderboardBox);
        scrollPane.getStyleClass().add("leaderboard-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setPrefWidth(700);

        // Back button - pixel style
        Button backBtn = new Button("← BACK");
        backBtn.getStyleClass().add("leaderboard-back-btn");
        backBtn.setOnAction(e -> onBack.run());

        container.getChildren().addAll(title, scrollPane, backBtn);
        root.getChildren().add(container);

        // Lấy kích thước hiện tại của stage để giữ nguyên kích thước/fullscreen
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;

        Scene scene = new Scene(root, width, height);

        // Load CSS
        try {
            java.net.URL cssResource = getClass().getResource("/resources/assets/css/leaderboard-pixel-style.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Leaderboard CSS not found, using fallback");
                applyFallbackStyles(root, title, backBtn);
            }
        } catch (Exception e) {
            System.err.println("Failed to load leaderboard CSS: " + e.getMessage());
            applyFallbackStyles(root, title, backBtn);
        }

        stage.setScene(scene);
    }
    
    /**
     * Fallback styles if CSS cannot be loaded
     */
    private void applyFallbackStyles(StackPane root, Text title, Button backBtn) {
        root.setStyle("-fx-background-image: url('/resources/assets/images/backgrounds/backgroundLeaderBoard.png'); " +
                     "-fx-background-size: cover; -fx-background-position: center center; " +
                     "-fx-background-repeat: no-repeat;");

        title.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 28px; " +
                      "-fx-fill: #FFD700; -fx-stroke: black; -fx-stroke-width: 3px;");

        backBtn.setStyle("-fx-min-width: 280px; -fx-pref-width: 300px; -fx-pref-height: 55px; " +
                        "-fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-text-fill: white; " +
                        "-fx-background-color: #E94F37; -fx-border-color: #3E2A1C; " +
                        "-fx-border-width: 3px; -fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; -fx-cursor: hand;");
    }

    public void handleLeaderboard(Message message) {
        leaderboardBox.getChildren().clear();
        
        String data = message.getData();
        
        if (data.isEmpty()) {
            Label noData = new Label("No scores yet!");
            noData.getStyleClass().add("no-scores-label");
            leaderboardBox.getChildren().add(noData);
            return;
        }
        
        String[] lines = data.split("\n");
        int rank = 1;
        for (String line : lines) {
            // Format: "username:score" hoặc "rank:username:score"
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                String username = parts[0];
                String score = parts[1];

                // Nếu có 3 phần, phần đầu là rank
                if (parts.length >= 3) {
                    username = parts[1];
                    score = parts[2];
                }

                HBox entry = createLeaderboardEntry(rank, username, score);
                leaderboardBox.getChildren().add(entry);
                rank++;
            }
        }
    }
    
    private HBox createLeaderboardEntry(int rank, String username, String score) {
        HBox entry = new HBox(20);
        entry.getStyleClass().add("leaderboard-entry");

        // Add special styling for top 3
        if (rank == 1) {
            entry.getStyleClass().add("rank-1");
        } else if (rank == 2) {
            entry.getStyleClass().add("rank-2");
        } else if (rank == 3) {
            entry.getStyleClass().add("rank-3");
        }

        // Rank with medal emoji
        String rankText = "";
        if (rank == 1) rankText = "🥇 " + rank;
        else if (rank == 2) rankText = "🥈 " + rank;
        else if (rank == 3) rankText = "🥉 " + rank;
        else rankText = String.valueOf(rank);

        Label rankLabel = new Label(rankText);
        rankLabel.getStyleClass().add("rank-label");
        rankLabel.setPrefWidth(80);
        rankLabel.setAlignment(Pos.CENTER);

        // Player name
        Label nameLabel = new Label("👤 " + username);
        nameLabel.getStyleClass().add("player-name-label");
        nameLabel.setPrefWidth(300);
        nameLabel.setAlignment(Pos.CENTER_LEFT);

        // Score with coin emoji
        Label scoreLabel = new Label("💰 " + score);
        scoreLabel.getStyleClass().add("score-label");
        scoreLabel.setPrefWidth(150);
        scoreLabel.setAlignment(Pos.CENTER_RIGHT);

        entry.getChildren().addAll(rankLabel, nameLabel, scoreLabel);

        return entry;
    }

    /**
     * Animate new score highlight (call this when a new score is added)
     */
    private void animateNewScore(HBox entry) {
        entry.getStyleClass().add("new-score-highlight");

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(2), e -> {
                entry.getStyleClass().remove("new-score-highlight");
            })
        );
        timeline.play();
    }
}