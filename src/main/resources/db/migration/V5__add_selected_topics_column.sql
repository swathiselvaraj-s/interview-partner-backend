-- Add selected_topics column to interview_sessions table
ALTER TABLE interview_sessions
    ADD COLUMN IF NOT EXISTS selected_topics JSON;
