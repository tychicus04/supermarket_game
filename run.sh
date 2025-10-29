#!/bin/bash

# Supermarket Game Server Control Script

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_banner() {
    echo -e "${BLUE}"
    echo "═══════════════════════════════════════"
    echo "   🏪 SUPERMARKET GAME SERVER"
    echo "═══════════════════════════════════════"
    echo -e "${NC}"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker is not installed${NC}"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}❌ Docker Compose is not installed${NC}"
        exit 1
    fi
}

start_server() {
    echo -e "${GREEN}🚀 Starting server...${NC}"
    docker-compose up -d

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Server started successfully!${NC}"
        echo ""
        echo "Server is running on port 8888"
        echo ""
        echo "View logs: ./run.sh logs"
        echo "Stop server: ./run.sh stop"
    else
        echo -e "${RED}❌ Failed to start server${NC}"
    fi
}

stop_server() {
    echo -e "${YELLOW}🛑 Stopping server...${NC}"
    docker-compose down
    echo -e "${GREEN}✅ Server stopped${NC}"
}

restart_server() {
    echo -e "${YELLOW}🔄 Restarting server...${NC}"
    docker-compose restart
    echo -e "${GREEN}✅ Server restarted${NC}"
}

show_logs() {
    echo -e "${BLUE}📋 Showing logs (Ctrl+C to exit)...${NC}"
    docker-compose logs -f
}

show_status() {
    echo -e "${BLUE}📊 Server Status:${NC}"
    docker-compose ps
}

build_image() {
    echo -e "${BLUE}🔨 Building Docker image...${NC}"
    docker-compose build --no-cache

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Build successful!${NC}"
    else
        echo -e "${RED}❌ Build failed${NC}"
    fi
}

show_help() {
    echo "Usage: ./run.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start    - Start the server"
    echo "  stop     - Stop the server"
    echo "  restart  - Restart the server"
    echo "  logs     - Show server logs"
    echo "  status   - Show server status"
    echo "  build    - Build Docker image"
    echo "  help     - Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./run.sh start"
    echo "  ./run.sh logs"
    echo "  ./run.sh stop"
}

# Main script
print_banner
check_docker

case "$1" in
    start)
        start_server
        ;;
    stop)
        stop_server
        ;;
    restart)
        restart_server
        ;;
    logs)
        show_logs
        ;;
    status)
        show_status
        ;;
    build)
        build_image
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${YELLOW}No command specified${NC}"
        echo ""
        show_help
        ;;
esac