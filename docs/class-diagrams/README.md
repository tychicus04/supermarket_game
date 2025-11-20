# Class Diagrams - Supermarket Game

## Tổng quan / Overview

Thư mục này chứa biểu đồ lớp (Class Diagrams) cho 3 chức năng chính:
1. **Xem bảng xếp hạng** - Leaderboard
2. **Hiển thị danh sách phòng trong lobby** - Lobby Room List
3. **Xem lịch sử đấu** - Match History

---

## 📋 Danh sách biểu đồ / Diagram List

### 1. Leaderboard Class Diagram (`leaderboard-class.puml`)

**Mô tả / Description:**
Biểu đồ lớp cho chức năng xem bảng xếp hạng top 10 người chơi.

**Các lớp chính / Main Classes:**

**Client Side:**
- `LeaderboardController` - Controller quản lý UI và logic hiển thị bảng xếp hạng
- `NetworkManager` - Quản lý kết nối socket và gửi/nhận messages

**Server Side:**
- `ClientHandler` - Xử lý request từ client
- `DatabaseManager` - Quản lý kết nối database và queries
- `LeaderboardEntry` - Data model cho một entry trong bảng xếp hạng

---

### 2. Lobby Room List Class Diagram (`lobby-room-list-class.puml`)

**Mô tả / Description:**
Biểu đồ lớp cho chức năng hiển thị và quản lý danh sách phòng trong lobby.

**Các lớp chính / Main Classes:**

**Client Side:**
- `LobbyController` - Controller quản lý lobby UI và auto-refresh
- `RoomInfo` - Data model chứa thông tin một phòng
- `NetworkManager` - Quản lý communication với server

**Server Side:**
- `ClientHandler` - Xử lý requests liên quan đến rooms
- `GameRoom` - Model đại diện cho một phòng game
- `RoomBroadcaster` - Quản lý và broadcast danh sách phòng global

**Đặc điểm / Features:**
- Auto-refresh mỗi 3 giây
- Hiển thị thông tin phòng: ID, creator, player count
- Maximum 2 players per room

---

### 3. Match History Class Diagram (`match-history-class.puml`)

**Mô tả / Description:**
Biểu đồ lớp cho chức năng xem lịch sử đấu và thống kê.

**Các lớp chính / Main Classes:**

**Client Side:**
- `MatchHistoryController` - Controller quản lý hiển thị lịch sử và thống kê
- `MatchInfo` - Data model cho một trận đấu
- `MatchStats` - Data model cho thống kê tổng quan
- `NetworkManager` - Quản lý network communication

**Server Side:**
- `ClientHandler` - Xử lý requests lịch sử đấu
- `DatabaseManager` - Query database cho match history
- `MatchRecord` - Model lưu trữ thông tin một trận đấu

**Đặc điểm / Features:**
- Hiển thị 20 trận gần nhất
- Statistics: Wins, Losses, Draws, Win Rate
- Chi tiết: Result, Opponent, Scores, Date

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
# Cài PlantUML
brew install plantuml  # macOS
apt-get install plantuml  # Ubuntu

# Export to PNG
plantuml leaderboard-class.puml
plantuml lobby-room-list-class.puml
plantuml match-history-class.puml

# Export to SVG
plantuml -tsvg *.puml
```

---

## 📁 Cấu trúc / Structure

```
docs/class-diagrams/
├── README.md                      # File này
├── leaderboard-class.puml         # Biểu đồ lớp Bảng xếp hạng
├── lobby-room-list-class.puml     # Biểu đồ lớp Danh sách phòng
└── match-history-class.puml       # Biểu đồ lớp Lịch sử đấu
```

---

## 🔗 Mối quan hệ giữa các lớp / Class Relationships

### Dependencies (uses)
- Controllers sử dụng NetworkManager để communicate
- NetworkManager connect tới ClientHandler
- ClientHandler sử dụng DatabaseManager hoặc RoomBroadcaster

### Composition
- Controller chứa các UI components (VBox, HBox, Labels)
- RoomBroadcaster chứa Map of GameRooms

### Data Flow
```
Client Controller → NetworkManager → ClientHandler → Database/Storage
                                                    ↓
Client Controller ← NetworkManager ← ClientHandler ← Query Results
```

---

## 📚 Tài liệu liên quan / Related Documentation

- Use Case Diagrams: `../use-case-diagrams/`
- Sequence Diagrams: `../sequence-diagrams/`
- Source Code:
  - Client Controllers: `/Client/src/controllers/`
  - Server Handlers: `/SupermarketServer/src/server/`
  - Database: `/SupermarketServer/src/database/`

---

Tác giả: Generated for Supermarket Game Project
Ngày: 2025-11-20
