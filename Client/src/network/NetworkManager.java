package network;

import models.Message;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.function.Consumer;

public class NetworkManager {
    private static NetworkManager instance;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> messageHandler;
    private Thread listenerThread;
    private boolean connected = false;
    
    private String serverHost = "localhost";
//    private String serverHost = "172.11.162.226";
    private int serverPort = 8888;
    
    private NetworkManager() {
        loadConfig();
    }
    
    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    /**
     * Load configuration from properties file
     */
    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                
                serverHost = prop.getProperty("server.host", "localhost");
                serverPort = Integer.parseInt(prop.getProperty("server.port", "8888"));
                
                System.out.println("📡 Config loaded: " + serverHost + ":" + serverPort);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Using default config: localhost:8888");
        }
    }
    
    /**
     * Connect to server
     */
    public boolean connect() {
        try {
            System.out.println("🔌 Connecting to " + serverHost + ":" + serverPort + "...");
            
            socket = new Socket(serverHost, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            connected = true;
            startListening();
            
            System.out.println("✅ Connected to server");
            return true;
            
        } catch (IOException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            connected = false;
            return false;
        }
    }
    
    /**
     * Start listening for server messages
     */
    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (connected && !Thread.currentThread().isInterrupted()) {
                    Message message = (Message) in.readObject();
                    
                    if (messageHandler != null) {
                        messageHandler.accept(message);
                    }
                }
            } catch (EOFException e) {
                System.out.println("📡 Server disconnected");
                connected = false;
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("❌ Error receiving message: " + e.getMessage());
                }
                connected = false;
            }
        });
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Send message to server
     */
    public boolean sendMessage(Message message) {
        if (!connected) {
            System.err.println("❌ Not connected to server");
            return false;
        }
        
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
            return true;
            
        } catch (IOException e) {
            System.err.println("❌ Failed to send message: " + e.getMessage());
            connected = false;
            return false;
        }
    }
    
    /**
     * Set message handler callback
     */
    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() {
        connected = false;
        
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            
            System.out.println("📡 Disconnected from server");
            
        } catch (IOException e) {
            System.err.println("⚠️ Error during disconnect: " + e.getMessage());
        }
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    /**
     * Send ping to server
     */
    public void sendPing() {
        sendMessage(new Message("PING", ""));
    }
    
    // Convenience methods for common messages
    
    public void login(String username, String password) {
        sendMessage(new Message("LOGIN", username + ":" + password));
    }
    
    public void register(String username, String password) {
        sendMessage(new Message("REGISTER", username + ":" + password));
    }
    
    public void createRoom() {
        sendMessage(new Message("CREATE_ROOM", ""));
    }
    
    public void joinRoom(String roomId) {
        sendMessage(new Message("JOIN_ROOM", roomId));
    }
    
    public void leaveRoom(String roomId) {
        sendMessage(new Message("LEAVE_ROOM", roomId));
    }
    
    public void startGame(String roomId) {
        sendMessage(new Message("START_GAME", roomId));
    }
    
    public void sendScore(String roomId, int score) {
        sendMessage(new Message("GAME_SCORE", roomId + ":" + score));
    }
    
    public void getLeaderboard() {
        sendMessage(new Message("GET_LEADERBOARD", ""));
    }
}