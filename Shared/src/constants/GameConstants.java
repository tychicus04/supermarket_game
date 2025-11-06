package constants;

public class GameConstants {
    public static final int SERVER_PORT = 8888;
    public static final String SERVER_HOST = "localhost";

    // Client to Server (C2S) message types
    public static final String MESSAGE_TYPE_LOGIN = "LOGIN";
    public static final String MESSAGE_TYPE_REGISTER = "REGISTER";
    public static final String MESSAGE_TYPE_CREATE_ROOM = "CREATE_ROOM";
    public static final String MESSAGE_TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final String MESSAGE_TYPE_LEAVE_ROOM = "LEAVE_ROOM";
    public static final String MESSAGE_TYPE_START_GAME = "START_GAME";
    public static final String MESSAGE_TYPE_GAME_SCORE = "GAME_SCORE";
    public static final String MESSAGE_TYPE_LEADERBOARD = "GET_LEADERBOARD";
    public static final String MESSAGE_TYPE_LOGOUT = "LOGOUT";
    public static final String MESSAGE_TYPE_GET_ROOM_LIST = "C2S_GET_ROOM_LIST";
    public static final String MESSAGE_TYPE_REQUEST_JOIN = "C2S_REQUEST_JOIN";
    public static final String MESSAGE_TYPE_ACCEPT_JOIN = "C2S_ACCEPT_JOIN";
    public static final String MESSAGE_TYPE_REJECT_JOIN = "C2S_REJECT_JOIN";
    public static final String MESSAGE_TYPE_SEARCH_USERS = "C2S_SEARCH_USERS";
    public static final String MESSAGE_TYPE_SEND_FRIEND_REQUEST = "C2S_SEND_FRIEND_REQUEST";
    public static final String MESSAGE_TYPE_ACCEPT_FRIEND = "C2S_ACCEPT_FRIEND";
    public static final String MESSAGE_TYPE_REJECT_FRIEND = "C2S_REJECT_FRIEND";
    public static final String MESSAGE_TYPE_GET_FRIENDS = "C2S_GET_FRIENDS";
    public static final String MESSAGE_TYPE_GET_FRIEND_REQUESTS = "C2S_GET_FRIEND_REQUESTS";
    public static final String MESSAGE_TYPE_REMOVE_FRIEND = "C2S_REMOVE_FRIEND";
    public static final String MESSAGE_TYPE_INVITE_TO_ROOM = "C2S_INVITE_TO_ROOM";
    public static final String MESSAGE_TYPE_PING = "PING";
    public static final String MESSAGE_TYPE_PONG = "PONG";

    // Server to Client (S2C) message types
    public static final String MESSAGE_TYPE_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String MESSAGE_TYPE_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String MESSAGE_TYPE_REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String MESSAGE_TYPE_REGISTER_FAIL = "REGISTER_FAIL";
    public static final String MESSAGE_TYPE_ROOM_CREATED = "ROOM_CREATED";
    public static final String MESSAGE_TYPE_ROOM_JOINED = "ROOM_JOINED";
    public static final String MESSAGE_TYPE_PLAYER_JOINED = "PLAYER_JOINED";
    public static final String MESSAGE_TYPE_PLAYER_LEFT = "PLAYER_LEFT";
    public static final String MESSAGE_TYPE_JOIN_FAIL = "JOIN_FAIL";
    public static final String MESSAGE_TYPE_S2C_ROOM_LIST = "S2C_ROOM_LIST";
    public static final String MESSAGE_TYPE_S2C_JOIN_REQUEST = "S2C_JOIN_REQUEST";
    public static final String MESSAGE_TYPE_S2C_JOIN_APPROVED = "S2C_JOIN_APPROVED";
    public static final String MESSAGE_TYPE_S2C_JOIN_REJECTED = "S2C_JOIN_REJECTED";
    public static final String MESSAGE_TYPE_S2C_INVITE_TO_ROOM = "S2C_INVITE_TO_ROOM";
    public static final String MESSAGE_TYPE_S2C_SEARCH_RESULTS = "S2C_SEARCH_RESULTS";
    public static final String MESSAGE_TYPE_S2C_FRIEND_REQUESTS = "S2C_FRIEND_REQUESTS";
    public static final String MESSAGE_TYPE_S2C_FRIEND_LIST = "S2C_FRIEND_LIST";
    public static final String MESSAGE_TYPE_S2C_FRIEND_REQUEST_SENT = "S2C_FRIEND_REQUEST_SENT";
    public static final String MESSAGE_TYPE_S2C_FRIEND_REQUEST_FAIL = "S2C_FRIEND_REQUEST_FAIL";
    public static final String MESSAGE_TYPE_S2C_FRIEND_ACCEPTED = "S2C_FRIEND_ACCEPTED";
    public static final String MESSAGE_TYPE_S2C_FRIEND_REJECTED = "S2C_FRIEND_REJECTED";
    public static final String MESSAGE_TYPE_S2C_FRIEND_REMOVED = "S2C_FRIEND_REMOVED";
    public static final String MESSAGE_TYPE_S2C_INVITE_SENT = "S2C_INVITE_SENT";
    public static final String MESSAGE_TYPE_S2C_FRIEND_REQUEST_RECEIVED = "S2C_FRIEND_REQUEST_RECEIVED";
    public static final String MESSAGE_TYPE_S2C_ROOM_INVITE = "S2C_ROOM_INVITE";
    public static final String MESSAGE_TYPE_LOGOUT_SUCCESS = "LOGOUT_SUCCESS";
    public static final String MESSAGE_TYPE_S2C_FRIEND_STATUS_CHANGED = "S2C_FRIEND_STATUS_CHANGED";
    public static final String MESSAGE_TYPE_GAME_START = "GAME_START";
    public static final String MESSAGE_TYPE_SCORE_UPDATE = "SCORE_UPDATE";
    public static final String MESSAGE_TYPE_ERROR = "ERROR";
    public static final String MESSAGE_TYPE_ROOM_DELETED = "ROOM_DELETED";
    public static final String MESSAGE_TYPE_S2C_JOIN_REQUEST_SENT = "S2C_JOIN_REQUEST_SENT";
    public static final String MESSAGE_TYPE_S2C_JOIN_REQUEST_FAIL = "S2C_JOIN_REQUEST_FAIL";

}
