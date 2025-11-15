package server;

import database.DatabaseManager;
import models.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import static constants.GameConstants.*;

/**
 * Phiên bản "tối giản" của GameSession.
 * Chỉ hoạt động như một bộ đếm 60 giây và một trạm trung chuyển điểm.
 * Toàn bộ logic game đều nằm ở Client.
 */
public class MultiplayerGameSession {
    private final String roomId;
    private final GameRoom room;
    private final Map<String, Integer> scores;
    private final DatabaseManager database;
    private boolean gameActive = false;
    private int timeLeft = 60;
    private static final int GAME_DURATION_SECONDS = 60;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> gameTimerTask;

    public MultiplayerGameSession(String roomId, GameRoom room, DatabaseManager database) {
        this.roomId = roomId;
        this.room = room;
        this.database = database;
        this.scores = room.getScoresMap();
    }

    /**
     * Bắt đầu game
     */
    public void startGame() {
        gameActive = true;
        timeLeft = GAME_DURATION_SECONDS;
        room.resetScores();
        room.broadcast(new Message(MESSAGE_TYPE_GAME_START, roomId));
        scheduler = Executors.newScheduledThreadPool(1);
        startGameTimer();
        System.out.println("Game (Minimal Logic) started in room " + roomId);
    }

    /**
     * Bắt đầu game timer (60 giây)
     */
    private void startGameTimer() {
        gameTimerTask = scheduler.scheduleAtFixedRate(() -> {
            timeLeft--;
            if (timeLeft % 5 == 0) {
                broadcastGameState();
            }
            if (timeLeft <= 0) {
                endGame(null ,null);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Xử lý khi nhận được điểm từ Client
     */
    public synchronized void handlePlayerScoreUpdate(String username, String scoreData) {
        if (!gameActive) return;

        try {
            int newScore = Integer.parseInt(scoreData);
            scores.put(username, newScore);
            broadcastGameState();
        } catch (NumberFormatException e) {
            System.err.println("Invalid score data from " + username + ": " + scoreData);
        }
    }

    /**
     * Phát sóng trạng thái game
     * Định dạng phải khớp với Client `handleGameState`:
     * "[items]|[timeout]|[username1]:[score1]|[username2]:[score2]"
     */
    private void broadcastGameState() {
        StringBuilder data = new StringBuilder();
        data.append("0|");
        data.append(timeLeft).append("|");
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (room.getPlayers().contains(entry.getKey())) {
                data.append(entry.getKey()).append(":").append(entry.getValue()).append("|");
            }
        }
        room.broadcast(new Message(MESSAGE_TYPE_S2C_GAME_STATE, data.toString()));
    }

    /**
     * Kết thúc game
     */
    private void endGame(String reason, String leavingPlayer) {
        if (!gameActive) return;
        gameActive = false;
        if (gameTimerTask != null) gameTimerTask.cancel(false);
        if (scheduler != null) scheduler.shutdown();
        saveScoresToDatabase();
        String payload;
        if (reason != null) {
            payload = reason;
        } else {
            payload = room.getFinalRankings();
        }

        Message gameOverMsg = new Message(MESSAGE_TYPE_S2C_GAME_OVER, payload);

        if (leavingPlayer != null) {
            room.broadcastToOthers(gameOverMsg, leavingPlayer);
        } else {
            room.broadcast(gameOverMsg);
        }
        System.out.println("Game ended in room " + roomId);
        GameServer.removeGameSession(roomId);
    }

    public void stopGame() {
        endGame(null, null);
    }

    public void stopGame(String reason, String leavingPlayer) {
        endGame(reason, leavingPlayer); // Dừng game với lý do
    }

    public boolean isActive() {
        return gameActive;
    }

    /**
     * Lưu điểm của tất cả người chơi vào database
     */
    private void saveScoresToDatabase() {
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String username = entry.getKey();
            int score = entry.getValue();

            if (database.saveScore(username, score)) {
                System.out.println("Saved score for " + username + ": " + score);
            } else {
                System.err.println("Failed to save score for " + username);
            }
        }

        List<String> players = room.getPlayers();
        if (players.size() == 2) {
            String player1 = players.get(0);
            String player2 = players.get(1);
            int player1Score = scores.getOrDefault(player1, 0);
            int player2Score = scores.getOrDefault(player2, 0);

            if (database.saveMatchHistory(roomId, player1, player2, player1Score, player2Score)) {
                System.out.println("Saved match history: " + player1 + " vs " + player2);
            } else {
                System.err.println("Failed to save match history");
            }
        }
    }
}