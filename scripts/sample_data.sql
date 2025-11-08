-- Sample data for Supermarket Game Database
-- Run this inside Docker container or with sqlite3 command

-- Add sample users (password hash for "password123")
INSERT OR IGNORE INTO users (username, password_hash) VALUES
    ('alice', '482c811da5d5b4bc6d497ffa98491e38'),
    ('bob', '482c811da5d5b4bc6d497ffa98491e38'),
    ('charlie', '482c811da5d5b4bc6d497ffa98491e38'),
    ('diana', '482c811da5d5b4bc6d497ffa98491e38'),
    ('eve', '482c811da5d5b4bc6d497ffa98491e38'),
    ('frank', '482c811da5d5b4bc6d497ffa98491e38'),
    ('grace', '482c811da5d5b4bc6d497ffa98491e38'),
    ('henry', '482c811da5d5b4bc6d497ffa98491e38'),
    ('ivy', '482c811da5d5b4bc6d497ffa98491e38'),
    ('jack', '482c811da5d5b4bc6d497ffa98491e38');

-- Add sample scores
INSERT INTO scores (username, score, played_at) VALUES
    ('alice', 1500, datetime('now', '-5 days')),
    ('alice', 1800, datetime('now', '-3 days')),
    ('alice', 2100, datetime('now', '-1 day')),
    ('bob', 1200, datetime('now', '-4 days')),
    ('bob', 1600, datetime('now', '-2 days')),
    ('charlie', 1900, datetime('now', '-6 days')),
    ('charlie', 2000, datetime('now', '-1 day')),
    ('diana', 1700, datetime('now', '-3 days')),
    ('eve', 1400, datetime('now', '-2 days')),
    ('frank', 1550, datetime('now', '-5 days'));

-- Show results
SELECT 'Users added:' as message, COUNT(*) as count FROM users;
SELECT 'Scores added:' as message, COUNT(*) as count FROM scores;
SELECT 'Top players:' as message;
SELECT username, MAX(score) as high_score
FROM scores
GROUP BY username
ORDER BY high_score DESC
LIMIT 5;

