package server;

import models.Customer;
import models.GameState;
import models.Message;
import models.SpawnedItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced GameRoom with server-authoritative game loop
 * Implements drag-and-drop gameplay with real-time state synchronization
 */
public class EnhancedGameRoom {
    private final String roomId;
    private final String creator;
    private final List<String> players;
    private final Map<String, Integer> scores;
    private final Map<String, Integer> combos;
    private final int maxPlayers = 4;
    private final long createdTime;

    // Game state
    private List<Customer> customers;
    private List<SpawnedItem> items;
    private int gameTimeRemaining; // in seconds
    private double customerSpawnTimer;
    private double itemSpawnTimer;
    private boolean gameRunning = false;

    // Game loop
    private ScheduledExecutorService gameLoopScheduler;
    private static final double TICK_RATE = 0.1; // 10 ticks per second (100ms)
    private static final int GAME_DURATION = 180; // 180 seconds
    private static final int MAX_CUSTOMERS = 3;
    private static final int MAX_ITEMS = 10;

    // Timers
    private static final double CUSTOMER_SPAWN_MIN = 2.0;
    private static final double CUSTOMER_SPAWN_MAX = 5.0;
    private static final double ITEM_SPAWN_MIN = 0.5;
    private static final double ITEM_SPAWN_MAX = 1.5;
    private static final double CUSTOMER_TIMEOUT = 15.0; // 15 seconds to serve
    private static final double ITEM_LIFETIME = 8.0; // 8 seconds before despawn

    private Random random = new Random();

    public EnhancedGameRoom(String roomId, String creator) {
        this.roomId = roomId;
        this.creator = creator;
        this.players = Collections.synchronizedList(new ArrayList<>());
        this.scores = new ConcurrentHashMap<>();
        this.combos = new ConcurrentHashMap<>();
        this.createdTime = System.currentTimeMillis();
        this.customers = Collections.synchronizedList(new ArrayList<>());
        this.items = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Add player to room
     */
    public synchronized boolean addPlayer(String username) {
        if (players.size() >= maxPlayers || players.contains(username)) {
            return false;
        }

        players.add(username);
        scores.put(username, 0);
        combos.put(username, 0);
        return true;
    }

    /**
     * Remove player from room
     */
    public synchronized boolean removePlayer(String username) {
        boolean removed = players.remove(username);
        if (removed) {
            scores.remove(username);
            combos.remove(username);
        }
        return removed;
    }

    /**
     * Start the game loop
     */
    public synchronized void startGame() {
        if (gameRunning || players.size() < 2) {
            return;
        }

        System.out.println("🎮 Starting game in room: " + roomId);

        // Initialize game state
        gameRunning = true;
        gameTimeRemaining = GAME_DURATION;
        customerSpawnTimer = 1.0; // Spawn first customer soon
        itemSpawnTimer = 0.5; // Spawn first items immediately
        customers.clear();
        items.clear();

        // Reset scores and combos
        for (String player : players) {
            scores.put(player, 0);
            combos.put(player, 0);
        }

        // Start game loop
        gameLoopScheduler = Executors.newSingleThreadScheduledExecutor();
        gameLoopScheduler.scheduleAtFixedRate(
                this::gameTick,
                0,
                (long)(TICK_RATE * 1000),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Main game tick (runs every 100ms)
     */
    private void gameTick() {
        try {
            // Update game time
            gameTimeRemaining -= (int)(TICK_RATE * 10);
            if (gameTimeRemaining <= 0) {
                endGame();
                return;
            }

            // Update customers
            synchronized (customers) {
                Iterator<Customer> custIter = customers.iterator();
                while (custIter.hasNext()) {
                    Customer customer = custIter.next();
                    boolean timedOut = customer.updateTime(TICK_RATE);

                    if (timedOut) {
                        handleCustomerTimeout(customer);
                        custIter.remove();
                    }
                }
            }

            // Update items
            synchronized (items) {
                items.removeIf(item -> item.updateTime(TICK_RATE));
            }

            // Spawn new customers
            customerSpawnTimer -= TICK_RATE;
            if (customerSpawnTimer <= 0 && customers.size() < MAX_CUSTOMERS) {
                spawnCustomer();
                customerSpawnTimer = CUSTOMER_SPAWN_MIN +
                        (random.nextDouble() * (CUSTOMER_SPAWN_MAX - CUSTOMER_SPAWN_MIN));
            }

            // Spawn new items
            itemSpawnTimer -= TICK_RATE;
            if (itemSpawnTimer <= 0 && items.size() < MAX_ITEMS) {
                spawnItem();
                itemSpawnTimer = ITEM_SPAWN_MIN +
                        (random.nextDouble() * (ITEM_SPAWN_MAX - ITEM_SPAWN_MIN));
            }

            // Broadcast game state to all players
            broadcastGameState();

        } catch (Exception e) {
            System.err.println("❌ Game tick error in room " + roomId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Spawn a new customer
     */
    private void spawnCustomer() {
        // Find available slot (0, 1, or 2)
        Set<Integer> usedSlots = new HashSet<>();
        for (Customer c : customers) {
            usedSlots.add(c.getSlotIndex());
        }

        int slot = -1;
        for (int i = 0; i < MAX_CUSTOMERS; i++) {
            if (!usedSlots.contains(i)) {
                slot = i;
                break;
            }
        }

        if (slot != -1) {
            String requestItem = SpawnedItem.getRandomItemName();
            Customer customer = new Customer(requestItem, CUSTOMER_TIMEOUT, slot);
            customers.add(customer);

            System.out.println("👤 Spawned customer at slot " + slot + " wants " + requestItem);
        }
    }

    /**
     * Spawn a new item
     */
    private void spawnItem() {
        String itemName = SpawnedItem.getRandomItemName();
        SpawnedItem item = new SpawnedItem(itemName, ITEM_LIFETIME);
        items.add(item);

        System.out.println("🎁 Spawned item: " + itemName + " at (" +
                String.format("%.2f", item.getX()) + ", " +
                String.format("%.2f", item.getY()) + ")");
    }

    /**
     * Handle customer timeout (penalty for all players)
     */
    private void handleCustomerTimeout(Customer customer) {
        System.out.println("⏰ Customer timeout: " + customer.getCustomerID());

        customer.setMood("angry");

        // Penalty: All players lose points and combos reset
        synchronized (scores) {
            for (String player : players) {
                int currentScore = scores.getOrDefault(player, 0);
                scores.put(player, Math.max(0, currentScore - 10));
                combos.put(player, 0);
            }
        }

        // Schedule removal after showing angry face
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            // Customer will be removed in next tick
        }, 1500, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }

    /**
     * Handle player action (drag-drop item to customer)
     */
    public synchronized void handlePlayerAction(String username, String itemID, String customerID) {
        if (!gameRunning) return;

        // Find item and customer
        SpawnedItem droppedItem = null;
        Customer targetCustomer = null;

        synchronized (items) {
            for (SpawnedItem item : items) {
                if (item.getItemID().equals(itemID)) {
                    droppedItem = item;
                    break;
                }
            }
        }

        synchronized (customers) {
            for (Customer customer : customers) {
                if (customer.getCustomerID().equals(customerID)) {
                    targetCustomer = customer;
                    break;
                }
            }
        }

        // Validate action
        if (droppedItem == null || targetCustomer == null) {
            System.out.println("⚠️  Invalid action: item or customer not found");
            return;
        }

        // Remove item immediately (consumed)
        items.remove(droppedItem);

        // Check if correct item
        if (targetCustomer.wantsItem(droppedItem.getItemName())) {
            // SUCCESS!
            handleCorrectItem(username, targetCustomer);
        } else {
            // FAIL!
            handleWrongItem(username, targetCustomer);
        }
    }

    /**
     * Handle correct item delivery
     */
    private void handleCorrectItem(String username, Customer customer) {
        int currentCombo = combos.getOrDefault(username, 0);
        int newCombo = currentCombo + 1;
        combos.put(username, newCombo);

        int points = 10 * newCombo; // Combo multiplier
        int currentScore = scores.getOrDefault(username, 0);
        scores.put(username, currentScore + points);

        customer.setMood("happy");

        System.out.println("✅ " + username + " served correctly! +" + points +
                " points (combo x" + newCombo + ")");

        // Schedule customer removal
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            customers.remove(customer);
        }, 1000, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }

    /**
     * Handle wrong item delivery
     */
    private void handleWrongItem(String username, Customer customer) {
        // Reset combo
        combos.put(username, 0);

        // Penalty
        int currentScore = scores.getOrDefault(username, 0);
        scores.put(username, Math.max(0, currentScore - 5));

        customer.setMood("angry");

        System.out.println("❌ " + username + " gave wrong item! -5 points");

        // Customer stays (mood will reset in 1.5 seconds)
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            if (customer.getMood().equals("angry")) {
                customer.setMood("neutral");
            }
        }, 1500, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }

    /**
     * Broadcast current game state to all players
     */
    private void broadcastGameState() {
        GameState state = new GameState(
                gameTimeRemaining,
                new ArrayList<>(customers),
                new ArrayList<>(items),
                new HashMap<>(scores),
                new HashMap<>(combos)
        );

        String json = state.toJson();
        Message stateMessage = new Message("S2C_GAME_STATE_UPDATE", json);

        broadcast(stateMessage);
    }

    /**
     * End the game
     */
    private synchronized void endGame() {
        if (!gameRunning) return;

        gameRunning = false;

        if (gameLoopScheduler != null) {
            gameLoopScheduler.shutdown();
        }

        System.out.println("🏁 Game ended in room: " + roomId);

        // Calculate rankings
        String rankings = getFinalRankings();

        // Broadcast game end
        Message endMessage = new Message("S2C_GAME_END", rankings);
        broadcast(endMessage);

        // Save to database (done in GameServer)
    }

    /**
     * Get final rankings
     */
    public String getFinalRankings() {
        List<Map.Entry<String, Integer>> rankings = new ArrayList<>(scores.entrySet());
        rankings.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : rankings) {
            sb.append(rank).append(".")
                    .append(entry.getKey()).append(":")
                    .append(entry.getValue()).append("\n");
            rank++;
        }

        return sb.toString();
    }

    /**
     * Broadcast message to all players in room
     */
    public void broadcast(Message message) {
        synchronized (players) {
            for (String player : players) {
                ClientHandler handler = GameServer.getClient(player);
                if (handler != null) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    // Getters
    public String getRoomId() { return roomId; }
    public String getCreator() { return creator; }
    public int getPlayerCount() { return players.size(); }
    public List<String> getPlayers() { return new ArrayList<>(players); }
    public boolean canStart() { return players.size() >= 2; }
    public boolean isEmpty() { return players.isEmpty(); }
    public boolean isGameRunning() { return gameRunning; }

    @Override
    public String toString() {
        return String.format("GameRoom[%s: %d/%d players, %s]",
                roomId, players.size(), maxPlayers, gameRunning ? "PLAYING" : "WAITING");
    }
}