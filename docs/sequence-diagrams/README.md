# Sequence Diagrams - Supermarket Game

## Tổng quan / Overview

Thư mục này chứa biểu đồ tuần tự (Sequence Diagrams) cho 3 chức năng chính:
1. **Xem bảng xếp hạng** - Leaderboard
2. **Hiển thị danh sách phòng trong lobby** - Lobby Room List
3. **Xem lịch sử đấu** - Match History

Biểu đồ tuần tự mô tả luồng tương tác giữa các components theo thời gian.

---

## 📋 Danh sách biểu đồ / Diagram List

### 1. Leaderboard Sequence Diagram (`leaderboard-sequence.puml`)

**Mô tả / Description:**
Biểu đồ tuần tự cho luồng xem bảng xếp hạng từ khi người chơi click button đến khi hiển thị top 10.

**Luồng / Flow:**
```
Player → LeaderboardController → NetworkManager → ClientHandler → DatabaseManager → Database
                                                                                      ↓
Player ← LeaderboardController ← NetworkManager ← ClientHandler ← DatabaseManager ← Query Result
```

**Các bước / Steps:**
1. Player click "LEADERBOARD" button
2. Controller hiển thị "Loading..."
3. Gửi request `MESSAGE_TYPE_LEADERBOARD` tới server
4. Server query database: `SELECT username, MAX(score) ... LIMIT 10`
5. Server format data và gửi về client
6. Controller parse data và tạo UI entries với medals
7. Hiển thị top 10 players cho người chơi

**Message Types:**
- Request: `MESSAGE_TYPE_LEADERBOARD`
- Response: `MESSAGE_TYPE_LEADERBOARD|data`

---

### 2. Lobby Room List Sequence Diagram (`lobby-room-list-sequence.puml`)

**Mô tả / Description:**
Biểu đồ tuần tự cho luồng hiển thị danh sách phòng với auto-refresh mỗi 3 giây.

**Luồng / Flow:**
```
Player → LobbyController → NetworkManager → ClientHandler → RoomBroadcaster → GameRoom
                                                                               ↓
Player ← LobbyController ← NetworkManager ← ClientHandler ← RoomBroadcaster ← Room List
```

**Các bước / Steps:**
1. Player click "ONLINE LOBBY"
2. Controller start auto-refresh timer (3 seconds)
3. Gửi request `MESSAGE_TYPE_GET_ROOM_LIST`
4. Server collect tất cả active rooms từ RoomBroadcaster
5. Loop qua từng room để lấy JSON data
6. Server gửi JSON array về client
7. Controller parse và tạo UI entry cho mỗi phòng
8. Auto-refresh: Repeat step 3-7 mỗi 3 giây

**Message Types:**
- Request: `MESSAGE_TYPE_GET_ROOM_LIST`
- Response: `MESSAGE_TYPE_S2C_ROOM_LIST|jsonData`

**Đặc điểm / Features:**
- Auto-refresh every 3 seconds
- Hiển thị: Room ID, Creator, Player Count (X/2)
- Real-time updates khi có phòng mới/xóa

---

### 3. Match History Sequence Diagram (`match-history-sequence.puml`)

**Mô tả / Description:**
Biểu đồ tuần tự cho luồng xem lịch sử đấu, bao gồm parallel requests cho history và statistics.

**Luồng / Flow:**
```
                      ┌─→ Request Match History → Database (20 records)
Player → Controller ──┤
                      └─→ Request Match Stats → Database (aggregation)
                                    ↓
Player ← Controller ←─ Both responses received ← Server
```

**Các bước / Steps:**
1. Player click "MATCH HISTORY"
2. Controller hiển thị "Loading..."
3. **Parallel requests:**
   - Request A: `MESSAGE_TYPE_GET_MATCH_HISTORY` (20 trận gần nhất)
   - Request B: `MESSAGE_TYPE_GET_MATCH_STATS` (wins/losses/draws)
4. Server query database cho cả hai requests
5. Server format và gửi responses về
6. Controller hiển thị:
   - Statistics panel ở top (W/L/D, Win Rate)
   - Match list với icons và details
7. Loop qua 20 matches để tạo UI entries

**Message Types:**
- Request 1: `MESSAGE_TYPE_GET_MATCH_HISTORY`
- Response 1: `MESSAGE_TYPE_S2C_MATCH_HISTORY|data`
- Request 2: `MESSAGE_TYPE_GET_MATCH_STATS`
- Response 2: `MESSAGE_TYPE_S2C_MATCH_STATS|stats`

**Đặc điểm / Features:**
- Parallel requests để tăng performance
- Icons: 🏆 WIN (green), 💔 LOSE (red), 🤝 DRAW (gray)
- Win Rate calculation: (Wins / Total) × 100%

---

## 🎨 Cách xem biểu đồ / How to View Diagrams

### Phương pháp 1: PlantUML Online (Nhanh nhất)
1. Truy cập: http://www.plantuml.com/plantuml/uml/
2. Copy nội dung file `.puml`
3. Click "Submit"

### Phương pháp 2: VS Code
1. Cài extension "PlantUML" trong VS Code
2. Mở file `.puml`
3. Nhấn `Alt+D` để preview

### Phương pháp 3: Command Line
```bash
# Export to PNG
plantuml leaderboard-sequence.puml
plantuml lobby-room-list-sequence.puml
plantuml match-history-sequence.puml

# Export to SVG
plantuml -tsvg *.puml
```

---

## 📁 Cấu trúc / Structure

```
docs/sequence-diagrams/
├── README.md                         # File này
├── leaderboard-sequence.puml         # Biểu đồ tuần tự Bảng xếp hạng
├── lobby-room-list-sequence.puml     # Biểu đồ tuần tự Danh sách phòng
└── match-history-sequence.puml       # Biểu đồ tuần tự Lịch sử đấu
```

---

## 🔄 Message Protocol Summary

### Request-Response Pattern
Tất cả chức năng sử dụng pattern:
```
Client: MESSAGE_TYPE_[ACTION]
Server: MESSAGE_TYPE_S2C_[ACTION]|data
```

### Data Format
- **Pipe-delimited (|)**: Separate message type và data
- **Newline-separated (\n)**: Multiple entries
- **Colon (:)**: Key-value pairs
- **JSON**: Complex objects (Room list)

### Example Messages
```
// Leaderboard
Client: "MESSAGE_TYPE_LEADERBOARD"
Server: "MESSAGE_TYPE_LEADERBOARD|1.PlayerA:5000\n2.PlayerB:4500\n..."

// Room List
Client: "MESSAGE_TYPE_GET_ROOM_LIST"
Server: "MESSAGE_TYPE_S2C_ROOM_LIST|[{\"roomId\":\"R1\",\"creator\":\"User1\",\"playerCount\":1}]"

// Match History
Client: "MESSAGE_TYPE_GET_MATCH_HISTORY"
Server: "MESSAGE_TYPE_S2C_MATCH_HISTORY|WIN|OpponentA|3500|2800|2024-11-20 15:30\n..."

// Match Stats
Client: "MESSAGE_TYPE_GET_MATCH_STATS"
Server: "MESSAGE_TYPE_S2C_MATCH_STATS|15|8|2|25"
```

---

## ⏱️ Timing & Performance

### Leaderboard
- **Trigger**: User click
- **Refresh**: On-demand only
- **Query time**: ~100ms
- **Data size**: Top 10 entries

### Lobby Room List
- **Trigger**: User enter lobby
- **Refresh**: Auto every 3 seconds
- **Query time**: ~50ms
- **Data size**: All active rooms (usually < 20)

### Match History
- **Trigger**: User click
- **Refresh**: On-demand only
- **Query time**: ~150ms (2 queries in parallel)
- **Data size**: 20 recent matches + statistics

---

## 📚 Tài liệu liên quan / Related Documentation

- Use Case Diagrams: `../use-case-diagrams/`
- Class Diagrams: `../class-diagrams/`
- Source Code:
  - Client Controllers: `/Client/src/controllers/`
  - Server Handlers: `/SupermarketServer/src/server/`
  - Network Protocol: `/Shared/src/constants/GameConstants.java`

---

Tác giả: Generated for Supermarket Game Project
Ngày: 2025-11-20
