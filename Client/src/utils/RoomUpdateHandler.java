package utils;

import models.Message;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to handle room update operations and reduce code duplication
 * in LobbyController and other controllers.
 */
public class RoomUpdateHandler {

    /**
     * Parse room update from JSON and extract player list
     */
    public static RoomUpdate parseRoomUpdate(String json) {
        RoomUpdate update = new RoomUpdate();

        // Extract creator
        String creator = JsonParser.extractString(json, "creator");
        update.setCreator(creator);

        // Extract players array
        String playersArray = JsonParser.extractArray(json, "players");
        if (playersArray != null) {
            String[] players = JsonParser.splitArrayItems(playersArray);
            for (String player : players) {
                String trimmed = player.trim();
                if (!trimmed.isEmpty()) {
                    update.addPlayer(trimmed);
                }
            }
        }

        return update;
    }

    /**
     * Parse player joined/left message
     * Format: "username:playerCount"
     */
    public static PlayerChangeEvent parsePlayerChange(String data, boolean isJoin) {
        String[] parts = data.split(":", 2);
        if (parts.length >= 1) {
            PlayerChangeEvent event = new PlayerChangeEvent();
            event.setUsername(parts[0]);
            event.setJoin(isJoin);
            if (parts.length >= 2) {
                try {
                    event.setPlayerCount(Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    event.setPlayerCount(-1);
                }
            }
            return event;
        }
        return null;
    }

    /**
     * Parse friend list from JSON
     */
    public static List<FriendInfo> parseFriendsList(String json) {
        List<FriendInfo> friends = new ArrayList<>();

        if (JsonParser.isEmptyArray(json)) {
            return friends;
        }

        String[] friendObjects = JsonParser.splitJsonArray(json);
        for (String friendJson : friendObjects) {
            String username = JsonParser.extractString(friendJson, "username");
            boolean isOnline = JsonParser.extractBoolean(friendJson, "online", false);

            if (username != null) {
                friends.add(new FriendInfo(username, isOnline));
            }
        }

        return friends;
    }

    /**
     * Parse room list from JSON
     */
    public static List<RoomInfo> parseRoomsList(String json) {
        List<RoomInfo> rooms = new ArrayList<>();

        if (JsonParser.isEmptyArray(json)) {
            return rooms;
        }

        String[] roomObjects = JsonParser.splitJsonArray(json);
        for (String roomJson : roomObjects) {
            String roomId = JsonParser.extractString(roomJson, "roomId");
            String creator = JsonParser.extractString(roomJson, "creator");
            int playerCount = JsonParser.extractInt(roomJson, "playerCount", 0);
            int maxPlayers = JsonParser.extractInt(roomJson, "maxPlayers", 4);

            if (roomId != null) {
                rooms.add(new RoomInfo(roomId, creator, playerCount, maxPlayers));
            }
        }

        return rooms;
    }

    /**
     * Parse invite message
     * Format: "inviterUsername;roomId"
     */
    public static InviteInfo parseInvite(String data) {
        String[] parts = data.split(";", 2);
        if (parts.length >= 2) {
            return new InviteInfo(parts[0], parts[1]);
        }
        return null;
    }

    /**
     * Parse join request message
     * Format: "requesterUsername;roomId"
     */
    public static JoinRequestInfo parseJoinRequest(String data) {
        String[] parts = data.split(";", 2);
        if (parts.length >= 2) {
            return new JoinRequestInfo(parts[0], parts[1]);
        }
        return null;
    }

    // ==================== DATA CLASSES ====================

    public static class RoomUpdate {
        private String creator;
        private final List<String> players = new ArrayList<>();

        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        public List<String> getPlayers() { return players; }
        public void addPlayer(String player) { players.add(player); }
    }

    public static class PlayerChangeEvent {
        private String username;
        private boolean isJoin;
        private int playerCount = -1;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public boolean isJoin() { return isJoin; }
        public void setJoin(boolean join) { isJoin = join; }
        public int getPlayerCount() { return playerCount; }
        public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    }

    public static class FriendInfo {
        private final String username;
        private final boolean online;

        public FriendInfo(String username, boolean online) {
            this.username = username;
            this.online = online;
        }

        public String getUsername() { return username; }
        public boolean isOnline() { return online; }

        public Map<String, String> toMap() {
            Map<String, String> map = new HashMap<>();
            map.put("username", username);
            map.put("online", String.valueOf(online));
            return map;
        }
    }

    public static class RoomInfo {
        private final String roomId;
        private final String creator;
        private final int playerCount;
        private final int maxPlayers;

        public RoomInfo(String roomId, String creator, int playerCount, int maxPlayers) {
            this.roomId = roomId;
            this.creator = creator;
            this.playerCount = playerCount;
            this.maxPlayers = maxPlayers;
        }

        public String getRoomId() { return roomId; }
        public String getCreator() { return creator; }
        public int getPlayerCount() { return playerCount; }
        public int getMaxPlayers() { return maxPlayers; }

        public Map<String, String> toMap() {
            Map<String, String> map = new HashMap<>();
            map.put("roomId", roomId);
            map.put("creator", creator);
            map.put("playerCount", String.valueOf(playerCount));
            map.put("maxPlayers", String.valueOf(maxPlayers));
            return map;
        }
    }

    public static class InviteInfo {
        private final String inviterUsername;
        private final String roomId;

        public InviteInfo(String inviterUsername, String roomId) {
            this.inviterUsername = inviterUsername;
            this.roomId = roomId;
        }

        public String getInviterUsername() { return inviterUsername; }
        public String getRoomId() { return roomId; }
    }

    public static class JoinRequestInfo {
        private final String requesterUsername;
        private final String roomId;

        public JoinRequestInfo(String requesterUsername, String roomId) {
            this.requesterUsername = requesterUsername;
            this.roomId = roomId;
        }

        public String getRequesterUsername() { return requesterUsername; }
        public String getRoomId() { return roomId; }
    }
}

