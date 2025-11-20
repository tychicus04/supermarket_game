# Use Case Diagrams - Supermarket Game

## Tổng quan / Overview

Thư mục này chứa các sơ đồ use case chi tiết cho các tính năng chính của game Supermarket:
- **Sảnh chờ online** (Online Lobby)
- **Bảng xếp hạng** (Leaderboard)
- **Lịch sử đấu** (Match History)

This directory contains detailed use case diagrams for the main features of Supermarket Game:
- **Online Lobby**
- **Leaderboard**
- **Match History**

---

## Danh sách sơ đồ / Diagram List

### 1. Online Lobby Use Case (`online-lobby-use-case.puml`)

**Mô tả / Description:**
Sơ đồ use case cho hệ thống sảnh chờ online, bao gồm quản lý phòng và quản lý bạn bè.

**Các chức năng chính / Main Features:**
- **Quản lý phòng / Room Management:**
  - Tạo phòng / Create Room
  - Xem danh sách phòng / Browse Rooms
  - Vào phòng / Join Room
  - Yêu cầu vào phòng / Request Join
  - Rời phòng / Leave Room
  - Bắt đầu trò chơi / Start Game
  - Chấp nhận/Từ chối yêu cầu / Accept/Reject Join Request
  - Đuổi người chơi / Kick Player

- **Quản lý bạn bè / Friend Management:**
  - Tìm người dùng / Search Users
  - Gửi lời mời kết bạn / Send Friend Request
  - Xem lời mời kết bạn / View Friend Requests
  - Chấp nhận/Từ chối kết bạn / Accept/Reject Friend Request
  - Xem danh sách bạn bè / View Friends List
  - Mời bạn vào phòng / Invite Friend to Room

**Actors:**
- Player (Người chơi)
- Room Creator (Người tạo phòng) - extends Player
- System (Hệ thống)

---

### 2. Leaderboard Use Case (`leaderboard-use-case.puml`)

**Mô tả / Description:**
Sơ đồ use case cho hệ thống bảng xếp hạng, hiển thị top 10 người chơi có điểm cao nhất.

**Các chức năng chính / Main Features:**
- Xem bảng xếp hạng / View Leaderboard
- Xem top 10 người chơi / View Top 10 Players
- Xem hạng của mình / See Own Ranking
- So sánh điểm / Compare Scores
- Hiển thị huy chương cho top 3 / Show Medals for Top 3
- Lấy điểm cao nhất của mỗi người chơi / Get Max Score per Player

**Actors:**
- Player (Người chơi)
- System (Hệ thống)
- Database (Cơ sở dữ liệu)

**Logic:**
- Chỉ hiển thị top 10 người chơi / Only shows top 10 players
- Lấy điểm cao nhất của mỗi người / Shows each player's highest score
- Huy chương: 🥇 (1st), 🥈 (2nd), 🥉 (3rd)

---

### 3. Match History Use Case (`match-history-use-case.puml`)

**Mô tả / Description:**
Sơ đồ use case cho hệ thống lịch sử đấu, theo dõi các trận đấu và thống kê.

**Các chức năng chính / Main Features:**
- **Xem thông tin / View Information:**
  - Xem lịch sử đấu (20 trận gần nhất) / View Match History (20 recent matches)
  - Xem thống kê / View Statistics
  - Xem tỷ lệ thắng / View Win Rate
  - Xem chi tiết trận đấu / View Match Details

- **Thống kê / Statistics:**
  - Số trận thắng / Wins
  - Số trận thua / Losses
  - Số trận hòa / Draws
  - Tỷ lệ thắng / Win Rate (%)

- **Ghi nhận trận đấu / Match Recording:**
  - Tự động lưu khi kết thúc game / Auto-save on game end
  - Lưu điểm số / Store scores
  - Lưu đối thủ / Store opponent
  - Lưu thời gian / Store timestamp

**Actors:**
- Player (Người chơi)
- System (Hệ thống)
- Database (Cơ sở dữ liệu)

**Logic kết quả / Result Logic:**
- WIN (🏆): winner = player
- DRAW (🤝): winner = null
- LOSE (💔): winner = opponent

---

## Cách xem sơ đồ / How to View Diagrams

### Phương pháp 1: PlantUML Online
1. Truy cập [PlantUML Web Server](http://www.plantuml.com/plantuml/uml/)
2. Copy nội dung file `.puml` vào editor
3. Click "Submit" để xem sơ đồ

### Phương pháp 2: VS Code với PlantUML Extension
1. Cài đặt extension "PlantUML" trong VS Code
2. Mở file `.puml`
3. Nhấn `Alt+D` để xem preview

### Phương pháp 3: Export to Image
```bash
# Cài đặt PlantUML (yêu cầu Java)
brew install plantuml  # macOS
apt-get install plantuml  # Ubuntu/Debian

# Export to PNG
plantuml online-lobby-use-case.puml
plantuml leaderboard-use-case.puml
plantuml match-history-use-case.puml

# Export to SVG (vector graphics)
plantuml -tsvg online-lobby-use-case.puml
plantuml -tsvg leaderboard-use-case.puml
plantuml -tsvg match-history-use-case.puml
```

---

## Cấu trúc file / File Structure

```
docs/use-case-diagrams/
├── README.md                      # File này / This file
├── online-lobby-use-case.puml     # Sơ đồ sảnh chờ online
├── leaderboard-use-case.puml      # Sơ đồ bảng xếp hạng
└── match-history-use-case.puml    # Sơ đồ lịch sử đấu
```

---

## Chi tiết kỹ thuật / Technical Details

### Message Protocol
Tất cả các tính năng sử dụng message-based protocol qua socket:
- Client gửi request với `MESSAGE_TYPE_*`
- Server xử lý và trả response với `MESSAGE_TYPE_S2C_*`

### Database Tables
- **friends** - Quan hệ bạn bè / Friend relationships
- **friend_requests** - Lời mời kết bạn / Friend requests
- **scores** - Điểm số game / Game scores
- **match_history** - Lịch sử đấu / Match history

### File Implementation
- **Client Controllers:**
  - `Client/src/controllers/LobbyController.java` (1,596 dòng)
  - `Client/src/controllers/LeaderboardController.java` (247 dòng)
  - `Client/src/controllers/MatchHistoryController.java` (320 dòng)

- **Server:**
  - `SupermarketServer/src/server/ClientHandler.java` - Xử lý tất cả message types
  - `SupermarketServer/src/server/GameRoom.java` - Quản lý phòng
  - `SupermarketServer/src/database/DatabaseManager.java` - Truy vấn database

---

## Ghi chú / Notes

### Relationships trong sơ đồ / Diagram Relationships
- **<<include>>**: Chức năng bắt buộc phải có / Required functionality
- **<<extend>>**: Chức năng mở rộng tùy chọn / Optional extension
- **Inheritance (--|>)**: Quan hệ kế thừa / Inheritance relationship

### Màu sắc / Color Coding
- **Green (Xanh lá)**: Win / Thắng
- **Red (Đỏ)**: Lose / Thua
- **Gray (Xám)**: Draw / Hòa
- **Blue (Xanh dương)**: Win Rate / Tỷ lệ thắng
- **Gold (Vàng)**: Leaderboard Title / Tiêu đề bảng xếp hạng

---

## Tác giả / Author
Generated for Supermarket Game Project
Date: 2025-11-20
