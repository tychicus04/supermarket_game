package server;

import models.Message;
import java.util.Map;
import java.util.concurrent.*;
import static constants.GameConstants.*;

/**
 * Phiên bản "tối giản" của GameSession.
 * Chỉ hoạt động như một bộ đếm 60 giây và một trạm trung chuyển điểm.
 * Toàn bộ logic game (tạo order, bấm phím) đều nằm ở Client.
 */
public class MultiplayerGameSession {
    private final String roomId;
    private final GameRoom room;
    private final Map<String, Integer> scores; // Lấy từ GameRoom
    private boolean gameActive = false;
    private int timeLeft = 60; // Chỉ đếm 60 giây
    private static final int GAME_DURATION_SECONDS = 60;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> gameTimerTask;

    public MultiplayerGameSession(String roomId, GameRoom room) {
        this.roomId = roomId;
        this.room = room;
        // Sử dụng trực tiếp map 'scores' của GameRoom
        this.scores = room.getScoresMap();
    }

    /**
     * Bắt đầu game
     */
    public void startGame() {
        gameActive = true;
        timeLeft = GAME_DURATION_SECONDS;

        // Reset điểm trong GameRoom về 0
        room.resetScores();

        // Gửi tin nhắn GAME_START cho client
        room.broadcast(new Message(MESSAGE_TYPE_GAME_START, roomId));

        // Bắt đầu timer 60 giây
        scheduler = Executors.newScheduledThreadPool(1);
        startGameTimer();

        System.out.println("🎮 Game (Minimal Logic) started in room " + roomId);
    }

    /**
     * Bắt đầu game timer (60 giây)
     */
    private void startGameTimer() {
        gameTimerTask = scheduler.scheduleAtFixedRate(() -> {
            timeLeft--;

            // Cứ 5 giây lại broadcast 1 lần để đồng bộ
            if (timeLeft % 5 == 0) {
                broadcastGameState();
            }

            // Hết giờ
            if (timeLeft <= 0) {
                endGame(null ,null);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * (MỚI) Xử lý khi nhận được điểm từ Client (Client gửi GAME_SCORE)
     */
    public synchronized void handlePlayerScoreUpdate(String username, String scoreData) {
        if (!gameActive) return;

        try {
            int newScore = Integer.parseInt(scoreData);
            // Cập nhật điểm trực tiếp vào map của GameRoom
            scores.put(username, newScore);

            // Gửi ngay lập tức trạng thái mới cho mọi người
            broadcastGameState();
        } catch (NumberFormatException e) {
            System.err.println("Invalid score data from " + username + ": " + scoreData);
        }
    }

    /**
     * (SỬA LẠI) Phát sóng trạng thái game
     * Định dạng phải khớp với Client `handleGameState`:
     * "[items]|[timeout]|[username1]:[score1]|[username2]:[score2]"
     */
    private void broadcastGameState() {
        StringBuilder data = new StringBuilder();

        // Client `handleGameState` của bạn cần 2 phần tử đầu
        data.append("0|"); // Placeholder cho "remainingItems"
        data.append(timeLeft + "|"); // Thời gian còn lại của game (từ server)

        // Nối điểm của từng người chơi
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            // Đảm bảo chỉ gửi điểm của những người còn trong phòng
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
        if (!gameActive) return; // Đảm bảo chỉ chạy 1 lần
        gameActive = false;

        // Dừng timer
        if (gameTimerTask != null) gameTimerTask.cancel(false);
        if (scheduler != null) scheduler.shutdown();

        String payload;
        if (reason != null) {
            // Nếu có lý do (VD: "OPPONENT_LEFT"), gửi lý do đó
            payload = reason;
        } else {
            // Nếu không (hết giờ bình thường), gửi bảng xếp hạng
            payload = room.getFinalRankings();
        }

        room.broadcast(new Message(MESSAGE_TYPE_S2C_GAME_OVER, payload));
        Message gameOverMsg = new Message(MESSAGE_TYPE_S2C_GAME_OVER, payload);

        if (leavingPlayer != null) {
            // Chỉ gửi cho người còn lại, KHÔNG gửi cho người vừa thoát
            room.broadcastToOthers(gameOverMsg, leavingPlayer);
        } else {
            // Hết giờ bình thường, gửi cho tất cả mọi người
            room.broadcast(gameOverMsg);
        }
        // Broadcast game over
        // Client sẽ nhận S2C_GAME_OVER, gọi handleGameOver(),
        // sau đó gọi showGameOverScreen() (tự so sánh điểm và hiển thị Thắng/Thua)
//        room.broadcast(new Message(MESSAGE_TYPE_S2C_GAME_OVER, room.getFinalRankings()));

        System.out.println("🏁 Game ended in room " + roomId);
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
}