# Hướng Dẫn Thêm Dữ Liệu Vào Database

## 📊 Tổng Quan

Database `supermarket_game.db` đang chạy trong Docker container và được mount từ folder `./data`. Có nhiều cách để thêm dữ liệu.

## 🚀 Cách 1: Dùng Script Python (Đơn Giản Nhất)

### Chạy Script
```bash
python scripts/add_sample_data.py
```

### Dữ Liệu Sẽ Thêm
- ✅ 10 users mẫu (alice, bob, charlie, diana, eve, frank, grace, henry, ivy, jack)
- ✅ Password cho tất cả: `password123`
- ✅ 2-5 điểm số ngẫu nhiên cho mỗi user (500-2500 points)
- ✅ Các mối quan hệ bạn bè ngẫu nhiên

### Xem Kết Quả
```bash
python scripts/query_db.py
```

## 🐳 Cách 2: Dùng Script SQL Qua Docker

### Chạy SQL Script
```bash
docker exec -i supermarket-game-server sqlite3 /app/data/supermarket_game.db < scripts/sample_data.sql
```

### Hoặc Dùng File Batch (Windows)
```bash
scripts\add_data.bat
```

## 💻 Cách 3: Truy Cập Trực Tiếp SQLite Trong Container

### Mở SQLite Shell
```bash
docker exec -it supermarket-game-server sqlite3 /app/data/supermarket_game.db
```

### Thêm User Mới
```sql
-- Password hash for "password123" is '482c811da5d5b4bc6d497ffa98491e38'
INSERT INTO users (username, password_hash) 
VALUES ('newuser', '482c811da5d5b4bc6d497ffa98491e38');
```

### Thêm Điểm Số
```sql
INSERT INTO scores (username, score, played_at) 
VALUES ('newuser', 1500, datetime('now'));
```

### Xem Dữ Liệu
```sql
-- Xem tất cả users
SELECT * FROM users;

-- Xem bảng xếp hạng
SELECT username, MAX(score) as high_score 
FROM scores 
GROUP BY username 
ORDER BY high_score DESC;

-- Thoát
.quit
```

## 📋 Cách 4: Dùng Python Script Trực Tiếp

```python
import sqlite3
import hashlib

# Kết nối database
conn = sqlite3.connect('data/supermarket_game.db')
cur = conn.cursor()

# Hash password
password_hash = hashlib.md5('mypassword'.encode()).hexdigest()

# Thêm user
cur.execute("INSERT INTO users (username, password_hash) VALUES (?, ?)", 
            ('myuser', password_hash))

# Thêm điểm
cur.execute("INSERT INTO scores (username, score) VALUES (?, ?)", 
            ('myuser', 1500))

conn.commit()
conn.close()
```

## 🔍 Kiểm Tra Database

### Xem Thống Kê
```bash
python scripts/query_db.py
```

### Xem Trong Container
```bash
docker exec supermarket-game-server sqlite3 /app/data/supermarket_game.db "SELECT COUNT(*) as total_users FROM users;"
```

## 📊 Cấu Trúc Database

### Bảng: users
- `id` (INTEGER PRIMARY KEY)
- `username` (TEXT UNIQUE)
- `password_hash` (TEXT) - MD5 hash
- `created_at` (TEXT)

### Bảng: scores
- `id` (INTEGER PRIMARY KEY)
- `username` (TEXT)
- `score` (INTEGER)
- `played_at` (TEXT)

### Bảng: friends
- `user_id` (INTEGER)
- `friend_id` (INTEGER)

### Bảng: friend_requests
- `id` (INTEGER PRIMARY KEY)
- `from_user` (TEXT)
- `to_user` (TEXT)
- `status` (TEXT) - 'pending', 'accepted', 'rejected'
- `created_at` (TEXT)

## ⚠️ Lưu Ý

1. **Password Hash**: Tất cả password được hash bằng MD5
   - Password `password123` = hash `482c811da5d5b4bc6d497ffa98491e38`
   - Dùng Python: `hashlib.md5('password'.encode()).hexdigest()`

2. **Database Path**: 
   - Trong container: `/app/data/supermarket_game.db`
   - Trên host: `./data/supermarket_game.db`

3. **Container Name**: `supermarket-game-server`

4. **Backup Trước Khi Thêm**:
   ```bash
   copy data\supermarket_game.db data\supermarket_game.db.backup
   ```

## 🎮 Test Login

Sau khi thêm dữ liệu, test login với:
- **Username**: alice, bob, charlie, v.v.
- **Password**: password123

## 🆘 Troubleshooting

### Lỗi: Database is locked
```bash
# Restart container
docker restart supermarket-game-server
```

### Lỗi: Container not found
```bash
# Check containers
docker ps -a

# Start container
docker start supermarket-game-server
```

### Lỗi: Permission denied
```bash
# Check file permissions
ls -la data/

# Fix permissions (Linux/Mac)
chmod 666 data/supermarket_game.db
```

## 📚 Tài Liệu Thêm

- SQLite Documentation: https://www.sqlite.org/docs.html
- Docker Documentation: https://docs.docker.com/

