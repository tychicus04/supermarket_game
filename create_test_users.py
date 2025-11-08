import sqlite3

# Connect to database
conn = sqlite3.connect('data/supermarket_game.db')
cursor = conn.cursor()

# Create test accounts
test_accounts = [
    ('admin', 'admin123'),
    ('player1', 'pass123'),
    ('test', 'test'),
]

print("=" * 60)
print("🔧 CREATING TEST ACCOUNTS")
print("=" * 60)

for username, password in test_accounts:
    try:
        cursor.execute("INSERT INTO users (username, password) VALUES (?, ?)", (username, password))
        print(f"✅ Created: {username} / {password}")
    except sqlite3.IntegrityError:
        print(f"⚠️  Already exists: {username}")

conn.commit()

# Show all users
print("\n" + "=" * 60)
print("👥 ALL USERS IN DATABASE")
print("=" * 60)
cursor.execute("SELECT username, password, created_at FROM users")
users = cursor.fetchall()

for user in users:
    print(f"\nUsername: {user[0]}")
    print(f"Password: {user[1]}")
    print(f"Created:  {user[2]}")
    print("-" * 60)

conn.close()

print("\n✅ Done! You can now login with these credentials")
