package server;

import models.Message;

import java.util.*;
import java.util.concurrent.*;

import static constants.GameConstants.*;

/**
 * Manages a multiplayer game session between 2 players
 * - Same item request for both players
 * - First correct answer wins
 * - Timeout decreases over time to increase difficulty
 */
public class MultiplayerGameSession {
    private final String roomId;
    private final GameRoom room;
    private final List<String> players;
    private final Map<String, Integer> scores;
    private final Map<String, Integer> combos;

    // Game state
    private String currentRequest;
    private int customerTimeout;
    private int maxCustomerTimeout = 10;
    private static final int MIN_CUSTOMER_TIMEOUT = 3;
    private int requestsServed = 0;
    private int timeLeft = 120;
    private boolean gameActive = false;

    // Items pool
    private static final String[] ALL_ITEMS = {
        "MILK", "BREAD", "APPLE", "CARROT",
        "ORANGE", "EGGS", "CHEESE", "MEAT", "SODA"
    };

    private final Random random = new Random();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> gameTimerTask;
    private ScheduledFuture<?> customerTimerTask;

    public MultiplayerGameSession(String roomId, GameRoom room) {
        this.roomId = roomId;
        this.room = room;
        this.players = new ArrayList<>(room.getPlayers());
        this.scores = new ConcurrentHashMap<>();
        this.combos = new ConcurrentHashMap<>();

        // Initialize scores and combos
        for (String player : players) {
            scores.put(player, 0);
            combos.put(player, 0);
        }
    }

    /**
     * Start the game session
     */
    public void startGame() {
        gameActive = true;
        timeLeft = 120;
        customerTimeout = maxCustomerTimeout;
        requestsServed = 0;

        // Generate first request
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];

        // Send game start message
        room.broadcast(new Message(MESSAGE_TYPE_GAME_START, roomId));

        // Send initial request to all players
        broadcastNewRequest();

        // Start timers
        scheduler = Executors.newScheduledThreadPool(2);
        startGameTimer();
        startCustomerTimer();

        System.out.println("🎮 Multiplayer game started in room " + roomId);
    }

    /**
     * Start main game timer (120 seconds)
     */
    private void startGameTimer() {
        gameTimerTask = scheduler.scheduleAtFixedRate(() -> {
            timeLeft--;

            // Broadcast game state every 5 seconds
            if (timeLeft % 5 == 0) {
                broadcastGameState();
            }

            // Game over
            if (timeLeft <= 0) {
                endGame();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Start customer timeout timer
     */
    private void startCustomerTimer() {
        customerTimerTask = scheduler.scheduleAtFixedRate(() -> {
            customerTimeout--;

            // Customer timeout - generate new request
            if (customerTimeout <= 0) {
                handleCustomerTimeout();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Handle when customer times out (no one answered)
     */
    private void handleCustomerTimeout() {
        // Reset all players' combo
        for (String player : players) {
            combos.put(player, 0);
        }

        // Generate new request with decreased timeout
        generateNewRequest();
    }

    /**
     * Handle player selecting an item
     */
    public synchronized void handleItemSelected(String username, String itemName) {
        if (!gameActive) return;
        if (!players.contains(username)) return;

        // Check if correct
        if (itemName.equals(currentRequest)) {
            handleCorrectAnswer(username);
        } else {
            handleWrongAnswer(username);
        }
    }

    /**
     * Handle correct answer from a player
     */
    private void handleCorrectAnswer(String username) {
        // Increase combo
        int combo = combos.getOrDefault(username, 0) + 1;
        combos.put(username, combo);

        // Calculate points
        int points = 10 * combo;
        int newScore = scores.getOrDefault(username, 0) + points;
        scores.put(username, newScore);

        // Increase difficulty
        requestsServed++;
        if (requestsServed % 2 == 0 && maxCustomerTimeout > MIN_CUSTOMER_TIMEOUT) {
            maxCustomerTimeout--;
            System.out.println("⬆️ Difficulty increased! Timeout: " + maxCustomerTimeout + "s");
        }

        // Send correct response to the player who answered
        ClientHandler handler = GameServer.getClient(username);
        if (handler != null) {
            String data = currentRequest + "|" + maxCustomerTimeout + "|" +
                         generateNextRequest() + "|" + newScore + "|" +
                         getOpponentScore(username);
            handler.sendMessage(new Message(MESSAGE_TYPE_S2C_ITEM_CORRECT, data));
        }

        // Broadcast game state
        broadcastGameState();
    }

    /**
     * Generate next request and return it
     */
    private String generateNextRequest() {
        String newRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        currentRequest = newRequest;
        customerTimeout = maxCustomerTimeout;
        return newRequest;
    }

    /**
     * Handle wrong answer from a player
     */
    private void handleWrongAnswer(String username) {
        // Reset combo
        combos.put(username, 0);

        // Send wrong response
        ClientHandler handler = GameServer.getClient(username);
        if (handler != null) {
            handler.sendMessage(new Message(MESSAGE_TYPE_S2C_ITEM_WRONG, ""));
        }
    }

    /**
     * Generate new request and broadcast to all players
     */
    private void generateNewRequest() {
        currentRequest = ALL_ITEMS[random.nextInt(ALL_ITEMS.length)];
        customerTimeout = maxCustomerTimeout;

        broadcastNewRequest();
    }

    /**
     * Broadcast new request to all players
     */
    private void broadcastNewRequest() {
        String data = currentRequest + "|" + maxCustomerTimeout;
        room.broadcast(new Message(MESSAGE_TYPE_S2C_NEW_REQUEST, data));
    }

    /**
     * Broadcast current game state to all players
     */
    private void broadcastGameState() {
        if (players.size() >= 2) {
            String player1 = players.get(0);
            String player2 = players.get(1);

            // Send to player 1
            String data1 = scores.getOrDefault(player1, 0) + "|" +
                          scores.getOrDefault(player2, 0) + "|" + timeLeft;
            ClientHandler handler1 = GameServer.getClient(player1);
            if (handler1 != null) {
                handler1.sendMessage(new Message(MESSAGE_TYPE_S2C_GAME_STATE, data1));
            }

            // Send to player 2 (swap scores)
            String data2 = scores.getOrDefault(player2, 0) + "|" +
                          scores.getOrDefault(player1, 0) + "|" + timeLeft;
            ClientHandler handler2 = GameServer.getClient(player2);
            if (handler2 != null) {
                handler2.sendMessage(new Message(MESSAGE_TYPE_S2C_GAME_STATE, data2));
            }
        }
    }

    /**
     * Get opponent score
     */
    private int getOpponentScore(String username) {
        for (String player : players) {
            if (!player.equals(username)) {
                return scores.getOrDefault(player, 0);
            }
        }
        return 0;
    }

    /**
     * End the game session
     */
    private void endGame() {
        gameActive = false;

        // Stop timers
        if (gameTimerTask != null) gameTimerTask.cancel(false);
        if (customerTimerTask != null) customerTimerTask.cancel(false);
        if (scheduler != null) scheduler.shutdown();

        // Update final scores in room
        for (String player : players) {
            room.updateScore(player, scores.getOrDefault(player, 0));
        }

        // Broadcast game over
        room.broadcast(new Message(MESSAGE_TYPE_S2C_GAME_OVER, room.getFinalRankings()));

        System.out.println("🏁 Game ended in room " + roomId);
        System.out.println("Final scores: " + scores);

        // Remove session from GameServer
        GameServer.removeGameSession(roomId);
    }

    /**
     * Stop the game (e.g., player left)
     */
    public void stopGame() {
        if (gameActive) {
            endGame();
        }
    }

    public boolean isActive() {
        return gameActive;
    }
}

