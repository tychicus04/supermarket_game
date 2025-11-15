package server;

import database.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main Game Server
 * Handles client connections and manages game rooms
 */
public class GameServer {
    private static final int PORT = 8888;
    private static final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<String, ClientHandler>();
    private static final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<String, GameRoom>();
    private static final Map<String, MultiplayerGameSession> gameSessions = new ConcurrentHashMap<>();
    private static DatabaseManager database;

    public static void main(String[] args) {
        database = new DatabaseManager();
        if (!database.initialize()) {
            System.err.println("Database initialization failed!");
            return;
        }
        System.out.println("Database initialized successfully");

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for connections...\n");

            startRoomCleanupThread();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, database);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Thread to cleanup empty rooms
     */
    private static void startRoomCleanupThread() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            Iterator<Map.Entry<String, GameRoom>> iterator = activeRooms.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, GameRoom> entry = iterator.next();
                if (entry.getValue().isEmpty()) {
                    System.out.println("Cleaning up empty room: " + entry.getKey());
                    iterator.remove();
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Register a client connection
     */
    public static void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        System.out.println("Player registered: " + username + " (Total: " + connectedClients.size() + ")");
    }

    /**
     * Unregister a client connection
     */
    public static void unregisterClient(String username) {
        connectedClients.remove(username);
        System.out.println("Player disconnected: " + username + " (Total: " + connectedClients.size() + ")");
    }

    /**
     * Create a new game room
     */
    public static GameRoom createRoom(String roomId, String creator) {
        GameRoom room = new GameRoom(roomId, creator);
        activeRooms.put(roomId, room);
        System.out.println("Room created: " + roomId + " by " + creator);
        return room;
    }

    /**
     * Get a game room by ID
     */
    public static GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    /**
     * Delete a room
     */
    public static void deleteRoom(String roomId) {
        activeRooms.remove(roomId);
        System.out.println("🗑️ Room deleted: " + roomId);
    }

    /**
     * Get all room IDs
     */
    public static java.util.Set<String> getAllRoomIds() {
        return new java.util.HashSet<>(activeRooms.keySet());
    }

    /**
     * Get a client handler by username
     */
    public static ClientHandler getClient(String username) {
        return connectedClients.get(username);
    }

    /**
     * Broadcast message to all players in a room
     */
    public static void broadcastToRoom(String roomId, models.Message message) {
        GameRoom room = activeRooms.get(roomId);
        if (room != null) {
            room.broadcast(message);
        }
    }

    /**
     * Get list of all active rooms as JSON
     */
    public static String getRoomListJson() {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Map.Entry<String, GameRoom> entry : activeRooms.entrySet()) {
            if (!first) json.append(",");
            first = false;

            GameRoom room = entry.getValue();
            json.append("{");
            json.append("\"roomId\":\"").append(room.getRoomId()).append("\",");
            json.append("\"creator\":\"").append(room.getCreator()).append("\",");
            json.append("\"playerCount\":").append(room.getPlayerCount()).append(",");
            json.append("\"maxPlayers\":4");
            json.append("}");
        }

        json.append("]");
        return json.toString();
    }

    /**
     * Broadcast updated room list to all connected clients
     */
    public static void broadcastRoomListUpdate() {
        String roomListJson = getRoomListJson();
        models.Message updateMessage = new models.Message("S2C_ROOM_LIST", roomListJson);

        for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
            entry.getValue().sendMessage(updateMessage);
        }
    }

    /**
     * Start a multiplayer game session
     */
    public static MultiplayerGameSession startGameSession(String roomId) {
        GameRoom room = activeRooms.get(roomId);
        if (room == null || gameSessions.containsKey(roomId)) {
            return null;
        }

        MultiplayerGameSession session = new MultiplayerGameSession(roomId, room, database);
        gameSessions.put(roomId, session);
        session.startGame();
        return session;
    }

    /**
     * (MỚI) Lấy một game session đang hoạt động
     */
    public static MultiplayerGameSession getGameSession(String roomId) {
        return gameSessions.get(roomId);
    }

    /**
     * (MỚI) Xóa một game session (thường được gọi bởi chính session khi kết thúc)
     */
    public static void removeGameSession(String roomId) {
        gameSessions.remove(roomId);
        System.out.println("Session removed for room: " + roomId);
    }
}
