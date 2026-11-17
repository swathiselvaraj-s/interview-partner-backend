-- Add new columns to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS github_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS portfolio_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS current_company VARCHAR(255),
    ADD COLUMN IF NOT EXISTS current_position VARCHAR(255),
    ADD COLUMN IF NOT EXISTS education VARCHAR(255),
    ADD COLUMN IF NOT EXISTS skills TEXT,
    ADD COLUMN IF NOT EXISTS availability VARCHAR(50),
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500),
    ADD COLUMN IF NOT EXISTS notifications_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_updates_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_preferred_role ON users(preferred_role);
CREATE INDEX IF NOT EXISTS idx_user_last_login ON users(last_login_at);
CREATE INDEX IF NOT EXISTS idx_user_active ON users(active);
