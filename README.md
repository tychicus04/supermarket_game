# 🏪 Supermarket Game - Docker Setup

Docker setup cho Supermarket Game Server với **Liberica JDK 8 Full**

## ✨ Latest Features (Nov 2025)

### 🆕 Room Browser & Request Join System
- **Room List Display**: Xem tất cả các phòng đang hoạt động với thông tin chi tiết
- **Request to Join**: Gửi yêu cầu tham gia đến chủ phòng
- **Host Approval System**: Chủ phòng chấp nhận/từ chối requests
- **Shared Module Architecture**: Code models dùng chung giữa Server và Client

📖 **Chi tiết**: [ROOM_BROWSER_GUIDE.md](ROOM_BROWSER_GUIDE.md)

## 🏗️ Project Structure

```
SupermarketGame/
├── Shared/                    # 📦 Shared module (models, constants)
├── SupermarketServer/         # 🖥️  Server module
├── Client/                    # 💻 Client module
├── compile.sh                 # 🔨 Compile all modules
├── run-server.sh             # ▶️  Run server
└── run-client.sh             # ▶️  Run client
```

## 📦 Files Included

```
Dockerfile.server          - Docker build file (JRE runtime)
Dockerfile.server-full     - Docker build file (Full JDK runtime)
docker-compose.yml         - Docker Compose configuration
.dockerignore             - Docker ignore rules
Makefile                  - Make commands (recommended)
build.sh                  - Build script
run.sh                    - Run script
test-server.sh            - Health check script
config-docker.properties  - Client config để connect với Docker server
DOCKER_GUIDE.md          - Hướng dẫn chi tiết
LIBERICA_JDK.md          - Liberica JDK technical guide
```

## ☕ Java Runtime

Dự án sử dụng **Liberica JDK 8 Full** từ BellSoft:
- ✅ TCK Certified OpenJDK
- ✅ Full featured (includes JavaFX)
- ✅ Long-term support
- ✅ Optimized performance
- ✅ Completely free

Xem [LIBERICA_JDK.md](LIBERICA_JDK.md) để biết chi tiết.

## 🚀 Quick Start

### Cách 1: Sử dụng Make (Dễ nhất)

```bash
# Setup lần đầu
make install

# Build image
make build

# Start server
make start

# View logs
make logs

# Stop server
make stop

# Health check
make status
```

### Cách 2: Sử dụng Docker Compose

```bash
# Build và start
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### Cách 3: Sử dụng scripts

```bash
# Make scripts executable
chmod +x *.sh

# Build
./build.sh

# Start server
./run.sh start

# View logs
./run.sh logs

# Test server
./test-server.sh

# Stop server
./run.sh stop
```

## 📋 Requirements

- Docker 20.10+
- Docker Compose 1.29+
- Port 8888 available

## 🔧 Cấu Hình Client

Copy `config-docker.properties` vào `SupermarketClient/src/resources/config.properties`

Hoặc update trực tiếp:

```properties
# Nếu server chạy trên máy local
server.host=localhost
server.port=8888

# Nếu server chạy trên máy khác
server.host=<IP-của-server>
server.port=8888
```

## 📊 Commands Overview

### Make commands
- `make help` - Show all commands
- `make build` - Build Docker image
- `make start` - Start server
- `make stop` - Stop server
- `make logs` - View logs
- `make status` - Check status
- `make restart` - Restart server
- `make shell` - Access container shell
- `make backup` - Backup database
- `make clean` - Clean up
- `make dev` - Development mode (với logs)

### Docker Compose commands
- `docker-compose up -d` - Start
- `docker-compose down` - Stop
- `docker-compose logs -f` - Logs
- `docker-compose ps` - Status
- `docker-compose restart` - Restart

### Script commands
- `./run.sh start` - Start
- `./run.sh stop` - Stop
- `./run.sh logs` - Logs
- `./run.sh status` - Status
- `./test-server.sh` - Health check

## 📍 Ports

- **8888** - Game Server Port

## 💾 Data Persistence

Database được lưu trong thư mục `./data/`:
- `supermarket_game.db` - Main database file

## 🔍 Troubleshooting

### Server không start
```bash
# Check logs
docker-compose logs

# Rebuild
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Port đã được sử dụng
```bash
# Check what's using port 8888
netstat -an | grep 8888
lsof -i :8888

# Change port in docker-compose.yml
ports:
  - "9999:8888"  # Use port 9999 instead
```

### Client không connect được
1. Check server is running: `docker-compose ps`
2. Check logs: `docker-compose logs`
3. Test connection: `./test-server.sh`
4. Update client config: `config-docker.properties`

## 📖 Documentation

Xem `DOCKER_GUIDE.md` để có hướng dẫn chi tiết về:
- Advanced configuration
- Production deployment
- Performance tuning
- Monitoring
- Backup & restore

## 🎮 Test Server

```bash
# Health check
./test-server.sh

# Manual test
nc -zv localhost 8888
```

## ✅ Verification

Sau khi start server, bạn sẽ thấy:

```
✅ Database initialized successfully
✅ Server started on port 8888
📡 Waiting for connections...
```

## 📞 Support

- Issues: Check `docker-compose logs`
- Database: `./data/supermarket_game.db`
- Full guide: `DOCKER_GUIDE.md`

---

**Happy Gaming! 🎮**