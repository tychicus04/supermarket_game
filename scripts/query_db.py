#!/usr/bin/env python3
"""
Simple database query tool for Supermarket Game
Usage: python scripts/query_db.py
"""
import sqlite3
import sys
from pathlib import Path

DB_PATH = "data/supermarket_game.db"

def connect_db():
    """Connect to database"""
    if not Path(DB_PATH).exists():
        print(f"ERROR: Database not found at {DB_PATH}")
        return None
    return sqlite3.connect(DB_PATH)

def show_stats(conn):
    """Show database statistics"""
    cur = conn.cursor()
    
    print("\n" + "="*60)
    print("DATABASE STATISTICS")
    print("="*60)
    
    # Total counts
    cur.execute("SELECT COUNT(*) FROM users")
    total_users = cur.fetchone()[0]
    
    cur.execute("SELECT COUNT(*) FROM scores")
    total_games = cur.fetchone()[0]
    
    cur.execute("SELECT COUNT(*) FROM friends")
    total_friends = cur.fetchone()[0]
    
    cur.execute("SELECT COUNT(*) FROM friend_requests WHERE status='pending'")
    pending_requests = cur.fetchone()[0]
    
    print(f"Total Users: {total_users}")
    print(f"Total Games Played: {total_games}")
    print(f"Total Friendships: {total_friends}")
    print(f"Pending Friend Requests: {pending_requests}")
    print("="*60)

def list_users(conn):
    """List all users"""
    cur = conn.cursor()
    cur.execute("SELECT id, username, created_at FROM users ORDER BY created_at DESC")
    users = cur.fetchall()
    
    print("\n" + "="*60)
    print("ALL USERS")
    print("="*60)
    
    if not users:
        print("(No users found)")
    else:
        print(f"{'ID':<5} {'Username':<20} {'Created At':<25}")
        print("-"*60)
        for user in users:
            print(f"{user[0]:<5} {user[1]:<20} {user[2]:<25}")
    print("="*60)

def show_leaderboard(conn, limit=10):
    """Show top players"""
    cur = conn.cursor()
    cur.execute("""
        SELECT username, MAX(score) as high_score, COUNT(*) as games_played
        FROM scores
        GROUP BY username
        ORDER BY high_score DESC
        LIMIT ?
    """, (limit,))
    
    leaders = cur.fetchall()
    
    print("\n" + "="*60)
    print(f"TOP {limit} PLAYERS")
    print("="*60)
    
    if not leaders:
        print("(No scores found)")
    else:
        print(f"{'Rank':<6} {'Username':<20} {'High Score':<12} {'Games':<8}")
        print("-"*60)
        for idx, leader in enumerate(leaders, 1):
            print(f"{idx:<6} {leader[0]:<20} {leader[1]:<12} {leader[2]:<8}")
    print("="*60)

def user_details(conn, username):
    """Show details for a specific user"""
    cur = conn.cursor()
    
    # Check if user exists
    cur.execute("SELECT id, username, created_at FROM users WHERE username = ?", (username,))
    user = cur.fetchone()
    
    if not user:
        print(f"\nUser '{username}' not found!")
        return
    
    print("\n" + "="*60)
    print(f"USER DETAILS: {username}")
    print("="*60)
    print(f"ID: {user[0]}")
    print(f"Username: {user[1]}")
    print(f"Created: {user[2]}")
    
    # Stats
    cur.execute("""
        SELECT COUNT(*) as games, 
               MAX(score) as high_score, 
               AVG(score) as avg_score,
               SUM(score) as total_score
        FROM scores WHERE username = ?
    """, (username,))
    stats = cur.fetchone()
    
    print(f"\nGame Statistics:")
    print(f"  Games Played: {stats[0]}")
    print(f"  High Score: {stats[1] or 0}")
    print(f"  Average Score: {stats[2]:.1f if stats[2] else 0}")
    print(f"  Total Score: {stats[3] or 0}")
    
    # Recent games
    cur.execute("""
        SELECT score, played_at 
        FROM scores 
        WHERE username = ? 
        ORDER BY played_at DESC 
        LIMIT 5
    """, (username,))
    recent = cur.fetchall()
    
    if recent:
        print(f"\nRecent Games:")
        for game in recent:
            print(f"  Score: {game[0]} - Played: {game[1]}")
    
    # Friends
    cur.execute("""
        SELECT CASE 
            WHEN user1 = ? THEN user2 
            WHEN user2 = ? THEN user1 
        END as friend
        FROM friends 
        WHERE user1 = ? OR user2 = ?
    """, (username, username, username, username))
    friends = cur.fetchall()
    
    print(f"\nFriends ({len(friends)}):")
    if friends:
        for friend in friends:
            print(f"  - {friend[0]}")
    else:
        print("  (No friends)")
    
    print("="*60)

def custom_query(conn):
    """Execute custom SQL query"""
    print("\nEnter SQL query (or 'back' to return):")
    query = input("> ").strip()
    
    if query.lower() == 'back':
        return
    
    try:
        cur = conn.cursor()
        cur.execute(query)
        
        if query.lower().startswith('select'):
            results = cur.fetchall()
            if results:
                # Print column names
                print("\n" + "-"*60)
                col_names = [desc[0] for desc in cur.description]
                print(" | ".join(col_names))
                print("-"*60)
                
                # Print rows
                for row in results:
                    print(" | ".join(str(val) for val in row))
                print("-"*60)
                print(f"({len(results)} rows)")
            else:
                print("(No results)")
        else:
            conn.commit()
            print(f"Query executed successfully. Rows affected: {cur.rowcount}")
            
    except Exception as e:
        print(f"ERROR: {e}")

def interactive_menu():
    """Interactive menu"""
    conn = connect_db()
    if not conn:
        return
    
    while True:
        print("\n" + "="*60)
        print("SUPERMARKET GAME - DATABASE QUERY TOOL")
        print("="*60)
        print("1. Show Statistics")
        print("2. List All Users")
        print("3. Show Leaderboard")
        print("4. User Details")
        print("5. Custom SQL Query")
        print("0. Exit")
        print("="*60)
        
        choice = input("Select option: ").strip()
        
        if choice == '1':
            show_stats(conn)
        elif choice == '2':
            list_users(conn)
        elif choice == '3':
            limit = input("Top N players (default 10): ").strip()
            limit = int(limit) if limit else 10
            show_leaderboard(conn, limit)
        elif choice == '4':
            username = input("Enter username: ").strip()
            if username:
                user_details(conn, username)
        elif choice == '5':
            custom_query(conn)
        elif choice == '0':
            print("Goodbye!")
            break
        else:
            print("Invalid option!")
    
    conn.close()

if __name__ == '__main__':
    try:
        interactive_menu()
    except KeyboardInterrupt:
        print("\n\nInterrupted by user.")
        sys.exit(0)
