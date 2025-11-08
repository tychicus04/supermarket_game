#!/usr/bin/env python3
"""
Add sample data to Supermarket Game database
Usage: python scripts/add_sample_data.py
"""
import sqlite3
import sys
import hashlib
from pathlib import Path
from datetime import datetime

DB_PATH = "data/supermarket_game.db"

def hash_password(password):
    """Hash password using MD5 (same as server)"""
    return hashlib.md5(password.encode()).hexdigest()

def connect_db():
    """Connect to database"""
    if not Path(DB_PATH).exists():
        print(f"ERROR: Database not found at {DB_PATH}")
        print("Make sure Docker container is running and database is mounted")
        return None
    return sqlite3.connect(DB_PATH)

def add_sample_users(conn):
    """Add sample users to database"""
    cur = conn.cursor()

    sample_users = [
        ("alice", "password123"),
        ("bob", "password123"),
        ("charlie", "password123"),
        ("diana", "password123"),
        ("eve", "password123"),
        ("frank", "password123"),
        ("grace", "password123"),
        ("henry", "password123"),
        ("ivy", "password123"),
        ("jack", "password123"),
    ]

    added = 0
    skipped = 0

    print("\n" + "="*60)
    print("ADDING SAMPLE USERS")
    print("="*60)

    for username, password in sample_users:
        try:
            # Check if user exists
            cur.execute("SELECT id FROM users WHERE username = ?", (username,))
            if cur.fetchone():
                print(f"⏭️  User '{username}' already exists - skipped")
                skipped += 1
                continue

            # Add user
            password_hash = hash_password(password)
            cur.execute(
                "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                (username, password_hash)
            )
            conn.commit()
            print(f"✅ Added user: {username} (password: {password})")
            added += 1

        except sqlite3.Error as e:
            print(f"❌ Error adding user '{username}': {e}")

    print("-"*60)
    print(f"Summary: {added} added, {skipped} skipped")
    print("="*60)

    return added

def add_sample_scores(conn):
    """Add sample scores for existing users"""
    import random

    cur = conn.cursor()

    # Get all users
    cur.execute("SELECT username FROM users")
    users = [row[0] for row in cur.fetchall()]

    if not users:
        print("\n⚠️  No users found. Please add users first.")
        return 0

    print("\n" + "="*60)
    print("ADDING SAMPLE SCORES")
    print("="*60)

    added = 0

    # Add 2-5 scores for each user
    for username in users:
        num_games = random.randint(2, 5)

        for i in range(num_games):
            score = random.randint(500, 2500)

            try:
                cur.execute(
                    "INSERT INTO scores (username, score, played_at) VALUES (?, ?, datetime('now'))",
                    (username, score)
                )
                added += 1
            except sqlite3.Error as e:
                print(f"❌ Error adding score for '{username}': {e}")

        conn.commit()
        print(f"✅ Added {num_games} scores for: {username}")

    print("-"*60)
    print(f"Summary: {added} scores added")
    print("="*60)

    return added

def add_friendships(conn):
    """Add sample friendships"""
    import random

    cur = conn.cursor()

    # Get all users
    cur.execute("SELECT id, username FROM users")
    users = cur.fetchall()

    if len(users) < 2:
        print("\n⚠️  Need at least 2 users to create friendships.")
        return 0

    print("\n" + "="*60)
    print("ADDING SAMPLE FRIENDSHIPS")
    print("="*60)

    added = 0

    # Create some random friendships
    for i in range(min(10, len(users) * 2)):
        user1, user2 = random.sample(users, 2)
        user1_id, user1_name = user1
        user2_id, user2_name = user2

        try:
            # Check if friendship already exists
            cur.execute("""
                SELECT 1 FROM friends
                WHERE (user_id = ? AND friend_id = ?)
                   OR (user_id = ? AND friend_id = ?)
            """, (user1_id, user2_id, user2_id, user1_id))

            if cur.fetchone():
                continue

            # Add friendship (bidirectional)
            cur.execute(
                "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)",
                (user1_id, user2_id)
            )
            cur.execute(
                "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)",
                (user2_id, user1_id)
            )
            conn.commit()

            print(f"✅ Created friendship: {user1_name} ↔ {user2_name}")
            added += 1

        except sqlite3.Error as e:
            print(f"❌ Error creating friendship: {e}")

    print("-"*60)
    print(f"Summary: {added} friendships created")
    print("="*60)

    return added

def show_summary(conn):
    """Show database summary after adding data"""
    cur = conn.cursor()

    print("\n" + "="*60)
    print("DATABASE SUMMARY")
    print("="*60)

    cur.execute("SELECT COUNT(*) FROM users")
    total_users = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM scores")
    total_scores = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM friends")
    total_friends = cur.fetchone()[0]

    print(f"📊 Total Users: {total_users}")
    print(f"🎮 Total Scores: {total_scores}")
    print(f"👥 Total Friendships: {total_friends}")

    # Show top 5 players
    cur.execute("""
        SELECT username, MAX(score) as high_score
        FROM scores
        GROUP BY username
        ORDER BY high_score DESC
        LIMIT 5
    """)

    top_players = cur.fetchall()

    if top_players:
        print("\n🏆 Top 5 Players:")
        for idx, (username, score) in enumerate(top_players, 1):
            print(f"   {idx}. {username}: {score} points")

    print("="*60)

def main():
    """Main function"""
    print("\n" + "="*60)
    print("SUPERMARKET GAME - ADD SAMPLE DATA")
    print("="*60)

    # Connect to database
    conn = connect_db()
    if not conn:
        sys.exit(1)

    try:
        # Add sample data
        users_added = add_sample_users(conn)

        if users_added > 0:
            scores_added = add_sample_scores(conn)
            friends_added = add_friendships(conn)
        else:
            print("\nℹ️  No new users added. You can still add scores and friendships.")
            response = input("Add scores for existing users? (y/n): ")
            if response.lower() == 'y':
                scores_added = add_sample_scores(conn)
                friends_added = add_friendships(conn)

        # Show summary
        show_summary(conn)

        print("\n✅ Done! Sample data has been added to the database.")
        print("You can now login with any of these accounts:")
        print("   Username: alice, bob, charlie, etc.")
        print("   Password: password123")

    except Exception as e:
        print(f"\n❌ Error: {e}")
        sys.exit(1)

    finally:
        conn.close()

if __name__ == "__main__":
    main()

