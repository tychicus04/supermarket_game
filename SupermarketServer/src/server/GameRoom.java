package server;

import models.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a game room for multiplayer
 */
public class GameRoom {
    private final String roomId;
    private final String creator;
    private final List<String> players;
    private final Map<String, Integer> scores;
    private final int maxPlayers = 2;
    private final long createdTime;
    
    public GameRoom(String roomId, String creator) {
        this.roomId = roomId;
        this.creator = creator;
        this.players = Collections.synchronizedList(new ArrayList<>());
        this.scores = new ConcurrentHashMap<>();
        this.createdTime = System.currentTimeMillis();
    }
    
    /**
     * Add player to room
     * @return true if added successfully
     */
    public synchronized boolean addPlayer(String username) {
        if (players.size() >= maxPlayers) {
            return false;
        }
        
        if (players.contains(username)) {
            return false;
        }
        
        players.add(username);
        scores.put(username, 0);
        return true;
    }
    
    /**
     * Remove player from room
     */
    public synchronized boolean removePlayer(String username) {
        boolean removed = players.remove(username);
        if (removed) {
            scores.remove(username);
        }
        return removed;
    }
    
    /**
     * Check if room can start game
     */
    public boolean canStart() {
        return players.size() >= 2;
    }
    
    /**
     * Check if room is empty
     */
    public boolean isEmpty() {
        return players.isEmpty();
    }
    
    /**
     * Update player score
     */
    public void updateScore(String username, int score) {
        scores.put(username, score);
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
    public String getRoomId() {
        return roomId;
    }
    
    public String getCreator() {
        return creator;
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    public List<String> getPlayers() {
        return new ArrayList<>(players);
    }
    
    public long getCreatedTime() {
        return createdTime;
    }

    public void resetScores() {
        synchronized (scores) {
            for (String player : players) {
                scores.put(player, 0);
            }
        }
    }

    public Map<String, Integer> getScoresMap() {
        return this.scores;
    }

    public void broadcastToOthers(Message message, String playerToExclude) {
        synchronized (players) {
            for (String player : players) {
                // Chỉ gửi nếu không phải là người chơi bị loại trừ
                if (!player.equals(playerToExclude)) {
                    ClientHandler handler = GameServer.getClient(player);
                    if (handler != null) {
                        handler.sendMessage(message);
                    }
                }
            }
        }
    }
    
    @Override
    public String toString() {
        return "GameRoom{" +
                "roomId='" + roomId + '\'' +
                ", players=" + players.size() + "/" + maxPlayers +
                ", creator='" + creator + '\'' +
                '}';
    }
}