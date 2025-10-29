package controllers;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import models.Message;
import utils.UIHelper;

public class LeaderboardController {
    private Stage stage;
    private Runnable onBack;
    private VBox leaderboardBox;
    
    public LeaderboardController(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }
    
    public void show() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));
        root.setStyle(UIHelper.createGradientBackground("#1e3c72", "#2a5298"));
        
        Text title = new Text("🏆 LEADERBOARD");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(javafx.scene.paint.Color.web("#ffd700"));
        
        leaderboardBox = new VBox(10);
        leaderboardBox.setAlignment(Pos.CENTER);
        
        Label loading = new Label("Loading...");
        loading.setTextFill(javafx.scene.paint.Color.WHITE);
        loading.setFont(Font.font("Arial", 18));
        leaderboardBox.getChildren().add(loading);
        
        Button backBtn = UIHelper.createButton("← BACK", UIHelper.DANGER_COLOR);
        backBtn.setOnAction(e -> onBack.run());
        
        ScrollPane scrollPane = new ScrollPane(leaderboardBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(350);
        
        root.getChildren().addAll(title, scrollPane, backBtn);
        
        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
    
    public void handleLeaderboard(Message message) {
        leaderboardBox.getChildren().clear();
        
        String data = message.getData();
        
        if (data.isEmpty()) {
            Label noData = new Label("No scores yet!");
            noData.setTextFill(javafx.scene.paint.Color.WHITE);
            noData.setFont(Font.font("Arial", 18));
            leaderboardBox.getChildren().add(noData);
            return;
        }
        
        String[] lines = data.split("\n");
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                HBox entry = createLeaderboardEntry(parts[0], parts[1]);
                leaderboardBox.getChildren().add(entry);
            }
        }
    }
    
    private HBox createLeaderboardEntry(String rank, String name) {
        HBox entry = new HBox(20);
        entry.setAlignment(Pos.CENTER);
        entry.setPadding(new Insets(10));
        entry.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                     "-fx-background-radius: 10px;");
        
        Label rankLabel = new Label(rank);
        rankLabel.setTextFill(javafx.scene.paint.Color.web("#ffd700"));
        rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        rankLabel.setPrefWidth(50);
        
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        nameLabel.setFont(Font.font("Arial", 18));
        nameLabel.setPrefWidth(200);
        
        entry.getChildren().addAll(rankLabel, nameLabel);
        return entry;
    }
}