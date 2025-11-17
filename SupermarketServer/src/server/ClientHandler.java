package server;

import database.DatabaseManager;
import models.Message;
import utils.JsonBuilder;
import utils.ServerErrorHandler;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import static constants.GameConstants.*;

/**
 * Handles individual client connections
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DatabaseManager database;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean running = true;
    private String currentRoomId = null;

    public ClientHandler(Socket socket, DatabaseManager database) {
        this.socket = socket;
        this.database = database;
    }
    
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            while (running) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (EOFException e) {
            if (username != null) {
                System.out.println("Client disconnected: " + username);
            }
        } catch (IOException e) {
            System.err.println("Network error for client " +
                (username != null ? username : "unknown") + ": " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Message deserialization error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error handling client " +
                (username != null ? username : "unknown") + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    /**
     * Route messages to appropriate handlers
     */
    private void handleMessage(Message msg) {
        try {
            switch (msg.getType()) {
                case MESSAGE_TYPE_LOGIN:
                    handleLogin(msg);
                    break;
                case MESSAGE_TYPE_REGISTER:
                    handleRegister(msg);
                    break;
                case MESSAGE_TYPE_CREATE_ROOM:
                    handleCreateRoom(msg);
                    break;
                case MESSAGE_TYPE_JOIN_ROOM:
                    handleJoinRoom(msg);
                    break;
                case MESSAGE_TYPE_LEAVE_ROOM:
                    handleLeaveRoom(msg);
                    break;
                case MESSAGE_TYPE_START_GAME:
                    handleStartGame(msg);
                    break;
                case MESSAGE_TYPE_GAME_SCORE:
                    handleGameScore(msg);
                    break;
                case MESSAGE_TYPE_LEADERBOARD:
                    handleGetLeaderboard();
                    break;
                case MESSAGE_TYPE_GET_ROOM_LIST:
                    handleGetRoomList();
                    break;
                case MESSAGE_TYPE_REQUEST_JOIN:
                    handleRequestJoin(msg);
                    break;
                case MESSAGE_TYPE_ACCEPT_JOIN:
                    handleAcceptJoin(msg);
                    break;
                case MESSAGE_TYPE_REJECT_JOIN:
                    handleRejectJoin(msg);
                    break;
                case MESSAGE_TYPE_SEARCH_USERS:
                    handleSearchUsers(msg);
                    break;
                case MESSAGE_TYPE_SEND_FRIEND_REQUEST:
                    handleSendFriendRequest(msg);
                    break;
                case MESSAGE_TYPE_ACCEPT_FRIEND:
                    handleAcceptFriendRequest(msg);
                    break;
                case MESSAGE_TYPE_REJECT_FRIEND:
                    handleRejectFriendRequest(msg);
                    break;
                case MESSAGE_TYPE_GET_FRIENDS:
                    handleGetFriends();
                    break;
                case MESSAGE_TYPE_GET_FRIEND_REQUESTS:
                    handleGetFriendRequests();
                    break;
                case MESSAGE_TYPE_REMOVE_FRIEND:
                    handleRemoveFriend(msg);
                    break;
                case MESSAGE_TYPE_INVITE_TO_ROOM:
                    handleInviteToRoom(msg);
                    break;
                case MESSAGE_TYPE_KICK_PLAYER:
                    handleKickPlayer(msg);
                    break;
                case MESSAGE_TYPE_GET_MATCH_HISTORY:
                    handleGetMatchHistory();
                    break;
                case MESSAGE_TYPE_GET_MATCH_STATS:
                    handleGetMatchStats();
                    break;
                case MESSAGE_TYPE_LOGOUT:
                    handleLogout();
                    break;
                case MESSAGE_TYPE_PING:
                    sendMessage(new Message(MESSAGE_TYPE_PONG, ""));
                    break;
                default:
                    System.out.println("Unknown message type: " + msg.getType());
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleLogin(Message msg) {
        String[] data = msg.getData().split(":", 2);
        if (data.length != 2) {
            sendMessage(new Message("LOGIN_FAIL", "Invalid login format"));
            return;
        }
        
        String user = data[0].trim();
        String pass = data[1];
        
        if (database.validateUser(user, pass)) {
            username = user;
            GameServer.registerClient(username, this);
            sendMessage(new Message(MESSAGE_TYPE_LOGIN_SUCCESS, "Welcome " + username + "!"));
            System.out.println("Login successful: " + username);
            // Notify all friends that this user is now online
            List<String> friends = database.getFriends(username);
            for (String friend : friends) {
                ClientHandler friendHandler = GameServer.getClient(friend);
                if (friendHandler != null) {
                    friendHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_STATUS_CHANGED,
                            username + ";online"));
                    System.out.println("🟢 Notified " + friend + " that " + username + " is online");
                }
            }
        } else {
            sendMessage(new Message(MESSAGE_TYPE_LOGIN_FAIL, "Invalid username or password"));
            System.out.println("Login failed for: " + user);
        }
    }
    
    private void handleRegister(Message msg) {
        String[] data = msg.getData().split(":", 2);
        if (data.length != 2) {
            sendMessage(new Message("REGISTER_FAIL", "Invalid registration format"));
            return;
        }
        
        String user = data[0].trim();
        String pass = data[1];
        
        // Validate username
        String usernameError = ServerErrorHandler.validateUsername(user);
        if (usernameError != null) {
            sendMessage(new Message(MESSAGE_TYPE_REGISTER_FAIL, usernameError));
            return;
        }
        
        // Validate password
        String passwordError = ServerErrorHandler.validatePassword(pass);
        if (passwordError != null) {
            sendMessage(new Message(MESSAGE_TYPE_REGISTER_FAIL, passwordError));
            return;
        }
        
        if (database.registerUser(user, pass)) {
            sendMessage(new Message(MESSAGE_TYPE_REGISTER_SUCCESS, "Account created! Please login."));
            ServerErrorHandler.logInfo("REGISTER", "New user registered: " + user);
        } else {
            sendMessage(new Message(MESSAGE_TYPE_REGISTER_FAIL, "Username already exists"));
            ServerErrorHandler.logWarning("REGISTER", "Registration failed (duplicate): " + user);
        }
    }
    
    private void handleCreateRoom(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }
        
        String roomId = "ROOM" + System.currentTimeMillis();
        GameRoom room = GameServer.createRoom(roomId, username);
        room.addPlayer(username);
        this.currentRoomId = roomId;
        sendMessage(new Message(MESSAGE_TYPE_ROOM_CREATED, roomId + ":" + room.getPlayerCount()));
        room.broadcastRoomUpdate();
        GameServer.broadcastRoomListUpdate();
        System.out.println("Broadcasted room list update (room created)");
    }
    
    private void handleJoinRoom(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }
        
        String roomId = msg.getData().trim();
        GameRoom room = GameServer.getRoom(roomId);
        
        if (room == null) {
            sendMessage(new Message(MESSAGE_TYPE_JOIN_FAIL, "Room not found"));
            return;
        }

        // Atomically get existing players and add new player to prevent race condition
        List<String> playersAlreadyInRoom = room.getPlayersAndTryAddPlayer(username);

        if (playersAlreadyInRoom != null) {
            this.currentRoomId = roomId;

            sendMessage(new Message(MESSAGE_TYPE_ROOM_JOINED, roomId + ":" + room.getPlayerCount()));

            // Broadcast room update to all players in room (including new player)
            room.broadcastRoomUpdate();

            System.out.println(username + " joined room " + roomId +
                             " (" + room.getPlayerCount() + "/4)");
        } else {
            sendMessage(new Message(MESSAGE_TYPE_JOIN_FAIL, "Room is full or you're already in it"));
        }
    }
    
    private void handleLeaveRoom(Message msg) {
        String roomId = msg.getData();
        GameRoom room = GameServer.getRoom(roomId);
        if (room == null) return; // Phòng không tồn tại

        MultiplayerGameSession session = GameServer.getGameSession(roomId);
        // Kiểm tra xem game có đang chạy không
        boolean wasGameActive = (session != null && session.isActive());

        if (wasGameActive) {
            // 1. Game đang chạy: Dừng game, báo P2 thắng
            System.out.println("Player " + username + " left MID-GAME, stopping game in room " + roomId);
            session.stopGame("OPPONENT_LEFT", this.username);
        }

        String creator = room.getCreator();
        room.removePlayer(username); // Xóa host khỏi phòng
        this.currentRoomId = null;

        boolean shouldDelete = false;

        if (room.isEmpty()) {
            shouldDelete = true; // Luôn xóa nếu phòng trống
        }
        // 2. Chỉ xóa phòng nếu Host thoát KHI ĐANG Ở LOBBY (game không chạy)
        else if (username.equals(creator)) {
            shouldDelete = true;
            System.out.println("🗑️ Creator " + username + " left room " + roomId + ", deleting room...");
            GameServer.broadcastToRoom(roomId,
                    new Message(MESSAGE_TYPE_ROOM_DELETED, "Room creator left. Room has been closed."));
        }

        if (shouldDelete) {
            GameServer.deleteRoom(roomId);
        } else {
            GameServer.broadcastToRoom(roomId,
                    new Message(MESSAGE_TYPE_PLAYER_LEFT, username + ":" + room.getPlayerCount()));
        }
    }

    private void handleStartGame(Message msg) {
        String roomId = msg.getData();
        GameRoom room = GameServer.getRoom(roomId);

        if (room == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Room not found"));
            return;
        }

        // Kiểm tra xem người gửi có phải chủ phòng không
        if (!username.equals(room.getCreator())) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Only the room creator can start the game"));
            return;
        }

        // Logic của client (ImprovedGameController) được thiết kế cho 2 người
        if (room.getPlayerCount() == 2) {
            MultiplayerGameSession session = GameServer.startGameSession(roomId);
            if (session != null) {
                System.out.println("🎮 Multiplayer game started in room: " + roomId);
            } else {
                sendMessage(new Message(MESSAGE_TYPE_ERROR, "Failed to start game (session active?)"));
            }
        } else {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Game requires exactly 2 players to start"));
        }
    }

    private void handleGameScore(Message msg) {
        if (username == null || currentRoomId == null) return;

        MultiplayerGameSession session = GameServer.getGameSession(currentRoomId);

        // Nếu có session và game đang chạy
        if (session != null && session.isActive()) {
            // Gọi hàm mới của session
            session.handlePlayerScoreUpdate(username, msg.getData().toString());
        }
    }
    
    private void handleGetLeaderboard() {
        String leaderboard = database.getLeaderboard(10);
        sendMessage(new Message(MESSAGE_TYPE_LEADERBOARD, leaderboard));
    }

    /**
     * Handle get match history request
     */
    private void handleGetMatchHistory() {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String matchHistory = database.getMatchHistory(username, 20);
        sendMessage(new Message(MESSAGE_TYPE_S2C_MATCH_HISTORY, matchHistory));
    }

    /**
     * Handle get match stats request
     */
    private void handleGetMatchStats() {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        java.util.Map<String, Integer> stats = database.getMatchStats(username);
        String statsData = stats.get("wins") + "|" +
                stats.get("losses") + "|" +
                stats.get("draws") + "|" +
                stats.get("total_matches");
        sendMessage(new Message(MESSAGE_TYPE_S2C_MATCH_STATS, statsData));
    }

    /**
     * Handle get room list request
     */
    private void handleGetRoomList() {
        String roomListJson = GameServer.getRoomListJson();
        sendMessage(new Message(MESSAGE_TYPE_S2C_ROOM_LIST, roomListJson));
    }

    /**
     * Handle join request to a room
     */
    private void handleRequestJoin(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String roomId = msg.getData().trim();
        GameRoom room = GameServer.getRoom(roomId);

        if (room == null) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_REQUEST_FAIL, "Room not found"));
            return;
        }

        if (room.getPlayerCount() >= 2) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_REQUEST_FAIL, "Room is full"));
            return;
        }

        // Send request to room creator
        String creator = room.getCreator();
        ClientHandler creatorHandler = GameServer.getClient(creator);

        if (creatorHandler != null) {
            creatorHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_REQUEST,
                username + ";" + roomId));
            sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_REQUEST_SENT,
                "Join request sent to " + creator));
            System.out.println(username + " requested to join room " + roomId);
        } else {
            sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_REQUEST_FAIL, "Room creator offline"));
        }
    }

    /**
     * Handle accept join request
     */
    private void handleAcceptJoin(Message msg) {
        String[] parts = msg.getData().split(";");
        if (parts.length != 2) return;

        String requestingPlayer = parts[0];
        String roomId = parts[1];

        GameRoom room = GameServer.getRoom(roomId);
        if (room == null) return;

        // Check if requester is the room creator
        if (!username.equals(room.getCreator())) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Only room creator can accept requests"));
            return;
        }

        ClientHandler requesterHandler = GameServer.getClient(requestingPlayer);
        if (requesterHandler != null) {
            requesterHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_APPROVED, roomId));
            System.out.println(username + " accepted " + requestingPlayer + "'s join request");
        }
    }

    /**
     * Handle reject join request
     */
    private void handleRejectJoin(Message msg) {
        String[] parts = msg.getData().split(";");
        if (parts.length != 2) return;

        String requestingPlayer = parts[0];
        String roomId = parts[1];

        GameRoom room = GameServer.getRoom(roomId);
        if (room == null) return;

        // Check if requester is the room creator
        if (!username.equals(room.getCreator())) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Only room creator can reject requests"));
            return;
        }

        ClientHandler requesterHandler = GameServer.getClient(requestingPlayer);
        if (requesterHandler != null) {
            requesterHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_JOIN_REJECTED,
                "Join request rejected by " + username));
            System.out.println(username + " rejected " + requestingPlayer + "'s join request");
        }
    }

    /**
     * Handle search users
     */
    private void handleSearchUsers(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String searchTerm = msg.getData().trim();
        if (searchTerm.length() < 2) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_SEARCH_RESULTS, "[]"));
            return;
        }

        List<String> results = database.searchUsers(searchTerm, username);
        String json = JsonBuilder.buildStringArray(results);
        sendMessage(new Message(MESSAGE_TYPE_S2C_SEARCH_RESULTS, json));
    }

    /**
     * Handle send friend request
     */
    private void handleSendFriendRequest(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String toUser = ((String) msg.getData()).trim();

        if (toUser.equals(username)) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REQUEST_FAIL, "Cannot add yourself"));
            return;
        }

        if (database.sendFriendRequest(username, toUser)) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REQUEST_SENT,
                "Friend request sent to " + toUser));

            // Notify the target user if online
            ClientHandler targetHandler = GameServer.getClient(toUser);
            if (targetHandler != null) {
                targetHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REQUEST_RECEIVED, username));
            }

            System.out.println(username + " sent friend request to " + toUser);
        } else {
            sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REQUEST_FAIL,
                "Request already exists or you are already friends"));
        }
    }

    /**
     * Handle accept friend request
     */
    private void handleAcceptFriendRequest(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }
        String fromUser = ((String) msg.getData()).trim();

        if (database.acceptFriendRequest(fromUser, username)) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_ACCEPTED, fromUser));

            // Notify the requester
            ClientHandler requesterHandler = GameServer.getClient(fromUser);
            if (requesterHandler != null) {
                requesterHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_ACCEPTED,
                    username + " accepted your friend request"));
            }

            // Send updated friend lists to both
            sendFriendList();
            if (requesterHandler != null) {
                requesterHandler.sendFriendList();
            }

            System.out.println(username + " accepted friend request from " + fromUser);
        } else {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Failed to accept friend request"));
        }
    }

    /**
     * Handle reject friend request
     */
    private void handleRejectFriendRequest(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }
        String fromUser = (msg.getData()).trim();

        if (database.rejectFriendRequest(fromUser, username)) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REJECTED, fromUser));

            // Notify the requester that their request was rejected
            ClientHandler requesterHandler = GameServer.getClient(fromUser);
            if (requesterHandler != null) {
                requesterHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REJECTED,
                    username + " rejected your friend request"));
            }
        } else {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Failed to reject friend request"));
        }
    }

    /**
     * Handle get friends list
     */
    private void handleGetFriends() {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        sendFriendList();
    }

    /**
     * Send friend list to client
     */
    private void sendFriendList() {
        List<String> friends = database.getFriends(username);

        JsonBuilder.ArrayBuilder builder = new JsonBuilder.ArrayBuilder();
        for (String friend : friends) {
            boolean isOnline = GameServer.getClient(friend) != null;
            String friendObj = JsonBuilder.buildFriendObject(friend, isOnline);
            builder.addObject(friendObj);
        }

        sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_LIST, builder.build()));
    }

    /**
     * Handle get friend requests
     */
    private void handleGetFriendRequests() {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        List<String> requests = database.getPendingFriendRequests(username);
        String json = JsonBuilder.buildStringArray(requests);
        sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REQUESTS, json));
    }

    /**
     * Handle remove friend
     */
    private void handleRemoveFriend(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String friendUsername = msg.getData().trim();

        if (database.removeFriend(username, friendUsername)) {
            sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REMOVED, friendUsername));

            // Notify the other user if online
            ClientHandler friendHandler = GameServer.getClient(friendUsername);
            if (friendHandler != null) {
                friendHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_REMOVED, username));
                friendHandler.sendFriendList();
            }

            // Send updated friend list
            sendFriendList();

            System.out.println("👥 " + username + " removed friend " + friendUsername);
        } else {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Failed to remove friend"));
        }
    }

    /**
     * Handle invite to room
     */
    private void handleInviteToRoom(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String[] parts = (msg.getData()).split(";");
        if (parts.length != 2) return;

        String friendUsername = parts[0];
        String roomId = parts[1];

        if (!database.areFriends(username, friendUsername)) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Can only invite friends"));
            return;
        }

        ClientHandler friendHandler = GameServer.getClient(friendUsername);
        if (friendHandler != null) {
            friendHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_ROOM_INVITE, username + ";" + roomId));
            sendMessage(new Message(MESSAGE_TYPE_S2C_INVITE_SENT, "Invite sent to " + friendUsername));
            System.out.println("📧 " + username + " invited " + friendUsername + " to room " + roomId);
        } else {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Friend is offline"));
        }
    }

    /**
     * Handle kick player from room
     */
    private void handleKickPlayer(Message msg) {
        if (username == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Not logged in"));
            return;
        }

        String[] parts = (msg.getData()).split(";");
        if (parts.length != 2) return;

        String playerToKick = parts[0];
        String roomId = parts[1];

        GameRoom room = GameServer.getRoom(roomId);
        if (room == null) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Room not found"));
            return;
        }

        if (!room.getCreator().equals(username)) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Only room creator can remove players"));
            return;
        }

        if (playerToKick.equals(username)) {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Cannot remove yourself"));
            return;
        }

        if (room.removePlayer(playerToKick)) {
            ClientHandler kickedHandler = GameServer.getClient(playerToKick);
            if (kickedHandler != null) {
                kickedHandler.setCurrentRoomId(null);
                kickedHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_KICKED_FROM_ROOM, "You have been removed from the room"));
            }
            room.broadcastRoomUpdate();
        } else {
            sendMessage(new Message(MESSAGE_TYPE_ERROR, "Player not in room"));
        }
    }

    /**
     * Handle logout request
     */
    private void handleLogout() {
        if (username != null) {
            System.out.println("User logged out: " + username);

            // Notify friends that user is now offline
            List<String> friends = database.getFriends(username);
            for (String friend : friends) {
                ClientHandler friendHandler = GameServer.getClient(friend);
                if (friendHandler != null) {
                    friendHandler.sendMessage(new Message(MESSAGE_TYPE_S2C_FRIEND_STATUS_CHANGED,
                        username + ";offline"));
                }
            }

            sendMessage(new Message(MESSAGE_TYPE_LOGOUT_SUCCESS, "Goodbye!"));
        }

        running = false;
    }

    /**
     * Send message to this client
     */
    public void sendMessage(Message msg) {
        try {
            synchronized (out) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to send message to " + username);
        }
    }

    /**
     * Get username of this client
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set current room ID
     */
    public void setCurrentRoomId(String roomId) {
        this.currentRoomId = roomId;
    }

    /**
     * Cleanup on disconnect
     */
    private void cleanup() {
        running = false;

        if (username != null) {
            this.currentRoomId = null;
            for (String roomId : GameServer.getAllRoomIds()) {
                GameRoom room = GameServer.getRoom(roomId);
                if (room != null && room.getPlayers().contains(username)) {

                    MultiplayerGameSession session = GameServer.getGameSession(roomId);
                    // Kiểm tra xem game có đang chạy không
                    boolean wasGameActive = (session != null && session.isActive());

                    if (wasGameActive) {
                        // 1. Game đang chạy: Dừng game, báo P2 thắng
                        System.out.println("Player " + username + " disconnected MID-GAME, stopping game in room " + roomId);
                        session.stopGame("OPPONENT_LEFT", this.username);
                    }

                    String creator = room.getCreator();
                    room.removePlayer(username); // Xóa host khỏi phòng

                    boolean shouldDelete = false;

                    if (room.isEmpty()) {
                        shouldDelete = true; // Luôn xóa nếu phòng trống
                    }
                    // 2. Chỉ xóa phòng nếu Host disconnect KHI ĐANG Ở LOBBY
                    else if (username.equals(creator)) {
                        shouldDelete = true;
                        System.out.println("🗑️ Creator " + username + " disconnected from room " + roomId + ", deleting room...");
                        GameServer.broadcastToRoom(roomId,
                                new Message(MESSAGE_TYPE_ROOM_DELETED, "Room creator disconnected. Room closed."));
                    }

                    if (shouldDelete) {
                        GameServer.deleteRoom(roomId);
                    } else {
                        GameServer.broadcastToRoom(roomId,
                                new Message(MESSAGE_TYPE_PLAYER_LEFT, username + ":" + room.getPlayerCount()));
                    }
                }
            }
            GameServer.unregisterClient(username);
        }

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            socket.close();
        } catch (IOException e) {
            // Ignored
        }
    }
}
