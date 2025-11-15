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
import models.Message;
import utils.SoundManager;

public class MatchHistoryController {
    private Stage stage;
    private Runnable onBack;
    private VBox historyBox;
    private VBox statsBox;
    private SoundManager soundManager;

    public MatchHistoryController(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
        this.soundManager = SoundManager.getInstance();
    }

    public void show() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-image: url('/resources/assets/images/backgrounds/backgroundLeaderBoard.png'); " +
                "-fx-background-size: cover; -fx-background-position: center center; " +
                "-fx-background-repeat: no-repeat;");

        VBox container = new VBox(15);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(30));
        container.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); " +
                "-fx-background-radius: 15px; " +
                "-fx-border-color: #3E2A1C; " +
                "-fx-border-width: 4px; " +
                "-fx-border-radius: 15px; " +
                "-fx-max-width: 900px;");

        Text title = new Text("MATCH HISTORY");
        title.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 28px; " +
                "-fx-fill: #FFD700; -fx-stroke: black; -fx-stroke-width: 3px;");

        statsBox = new VBox(5);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-background-color: rgba(240, 240, 240, 0.9); " +
                "-fx-padding: 15px; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: #3E2A1C; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 10px;");

        Label statsLoading = new Label("Loading stats...");
        statsLoading.setStyle("-fx-font-family: 'Courier New', monospace; " +
                "-fx-font-size: 14px; -fx-text-fill: #333;");
        statsBox.getChildren().add(statsLoading);

        historyBox = new VBox(10);
        historyBox.setStyle("-fx-padding: 10px;");

        Label loading = new Label("Loading match history...");
        loading.setStyle("-fx-font-family: 'Courier New', monospace; " +
                "-fx-font-size: 14px; -fx-text-fill: #333;");
        historyBox.getChildren().add(loading);

        ScrollPane scrollPane = new ScrollPane(historyBox);
        scrollPane.setStyle("-fx-background: transparent; " +
                "-fx-background-color: rgba(255, 255, 255, 0.8); " +
                "-fx-border-color: #3E2A1C; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 10px;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setPrefWidth(800);

        Button backBtn = new Button("BACK");
        backBtn.setStyle("-fx-min-width: 280px; -fx-pref-width: 300px; -fx-pref-height: 55px; " +
                "-fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-background-color: #E94F37; -fx-border-color: #3E2A1C; " +
                "-fx-border-width: 3px; -fx-background-radius: 8px; " +
                "-fx-border-radius: 8px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> {
            soundManager.play("menu_button");
            onBack.run();
        });

        backBtn.setOnMouseEntered(e ->
                backBtn.setStyle(backBtn.getStyle() + "-fx-background-color: #D43F2F;"));
        backBtn.setOnMouseExited(e ->
                backBtn.setStyle(backBtn.getStyle().replace("-fx-background-color: #D43F2F;",
                        "-fx-background-color: #E94F37;")));

        container.getChildren().addAll(title, statsBox, scrollPane, backBtn);
        root.getChildren().add(container);

        double width = stage.getWidth() > 0 ? stage.getWidth() : 1024;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 768;

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
    }

    /**
     * Handle match history data from server
     * Format: result|opponent|myScore|opponentScore|playedAt
     */
    public void handleMatchHistory(Message message) {
        historyBox.getChildren().clear();

        String data = message.getData();

        if (data.isEmpty()) {
            Label noData = new Label("No matches played yet!");
            noData.setStyle("-fx-font-family: 'Courier New', monospace; " +
                    "-fx-font-size: 16px; -fx-text-fill: #666; " +
                    "-fx-padding: 20px;");
            historyBox.getChildren().add(noData);
            return;
        }

        String[] lines = data.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length >= 5) {
                String result = parts[0];
                String opponent = parts[1];
                String myScore = parts[2];
                String opponentScore = parts[3];
                String playedAt = parts[4];

                HBox entry = createMatchEntry(result, opponent, myScore, opponentScore, playedAt);
                historyBox.getChildren().add(entry);
            }
        }
    }

    /**
     * Handle match stats data from server
     * Format: wins|losses|draws|total_matches
     */
    public void handleMatchStats(Message message) {
        statsBox.getChildren().clear();

        String data = message.getData();
        String[] parts = data.split("\\|");

        if (parts.length >= 4) {
            int wins = Integer.parseInt(parts[0]);
            int losses = Integer.parseInt(parts[1]);
            int draws = Integer.parseInt(parts[2]);
            int totalMatches = Integer.parseInt(parts[3]);

            double winRate = totalMatches > 0 ? (wins * 100.0 / totalMatches) : 0;

            Label statsTitle = new Label("YOUR STATS");
            statsTitle.setStyle("-fx-font-family: 'Courier New', monospace; " +
                    "-fx-font-size: 18px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #333;");

            HBox statsRow = new HBox(30);
            statsRow.setAlignment(Pos.CENTER);

            Label winsLabel = new Label("Wins: " + wins);
            winsLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                    "-fx-font-size: 14px; -fx-text-fill: #2ECC71; " +
                    "-fx-font-weight: bold;");

            Label lossesLabel = new Label("Losses: " + losses);
            lossesLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                    "-fx-font-size: 14px; -fx-text-fill: #E74C3C; " +
                    "-fx-font-weight: bold;");

            Label drawsLabel = new Label("Draws: " + draws);
            drawsLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                    "-fx-font-size: 14px; -fx-text-fill: #95A5A6; " +
                    "-fx-font-weight: bold;");

            Label winRateLabel = new Label(String.format("Win Rate: %.1f%%", winRate));
            winRateLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                    "-fx-font-size: 14px; -fx-text-fill: #3498DB; " +
                    "-fx-font-weight: bold;");

            statsRow.getChildren().addAll(winsLabel, lossesLabel, drawsLabel, winRateLabel);
            statsBox.getChildren().addAll(statsTitle, statsRow);
        }
    }

    private HBox createMatchEntry(String result, String opponent, String myScore,
                                  String opponentScore, String playedAt) {
        HBox entry = new HBox(20);
        entry.setAlignment(Pos.CENTER_LEFT);
        entry.setPadding(new Insets(12));

        String baseStyle = "-fx-background-radius: 8px; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 8px;";

        if (result.equals("WIN")) {
            entry.setStyle(baseStyle +
                    "-fx-background-color: rgba(46, 204, 113, 0.2); " +
                    "-fx-border-color: #2ECC71;");
        } else if (result.equals("LOSE")) {
            entry.setStyle(baseStyle +
                    "-fx-background-color: rgba(231, 76, 60, 0.2); " +
                    "-fx-border-color: #E74C3C;");
        } else {
            entry.setStyle(baseStyle +
                    "-fx-background-color: rgba(149, 165, 166, 0.2); " +
                    "-fx-border-color: #95A5A6;");
        }

        String resultIcon = result.equals("WIN") ? "🏆" : (result.equals("LOSE") ? "💔" : "🤝");
        Label resultLabel = new Label(resultIcon + " " + result);
        resultLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                "-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-text-fill: #333;");
        resultLabel.setPrefWidth(120);
        resultLabel.setAlignment(Pos.CENTER);

        Label opponentLabel = new Label("vs " + opponent);
        opponentLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                "-fx-font-size: 14px; -fx-text-fill: #333;");
        opponentLabel.setPrefWidth(200);
        opponentLabel.setAlignment(Pos.CENTER_LEFT);

        Label scoreLabel = new Label(myScore + " - " + opponentScore);
        scoreLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                "-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-text-fill: #333;");
        scoreLabel.setPrefWidth(100);
        scoreLabel.setAlignment(Pos.CENTER);

        String dateStr = playedAt.split(" ")[0];
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-family: 'Courier New', monospace; " +
                "-fx-font-size: 12px; -fx-text-fill: #666;");
        dateLabel.setPrefWidth(150);
        dateLabel.setAlignment(Pos.CENTER_RIGHT);

        entry.getChildren().addAll(resultLabel, opponentLabel, scoreLabel, dateLabel);

        return entry;
    }
}