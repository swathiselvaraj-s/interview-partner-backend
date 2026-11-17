-- Analytics Table
CREATE TABLE IF NOT EXISTS analytics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    category VARCHAR(100) NOT NULL,
    score FLOAT(53),
    communication_score FLOAT(53),
    clarity_score FLOAT(53),
    correctness_score FLOAT(53),
    questions_attempted INT DEFAULT 0,
    questions_correct INT DEFAULT 0,
    average_time_per_question FLOAT(53),
    strengths JSON,
    weaknesses JSON,
    recommendations JSON,
    difficulty_level VARCHAR(50),
    interview_mode VARCHAR(50),
    recorded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analytics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_analytics_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_user_category ON analytics(user_id, category);
CREATE INDEX IF NOT EXISTS idx_recorded_at ON analytics(recorded_at);

-- Performance Trends Table
CREATE TABLE IF NOT EXISTS performance_trends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trend_date DATE NOT NULL,
    overall_score FLOAT(53),
    communication_score FLOAT(53),
    clarity_score FLOAT(53),
    correctness_score FLOAT(53),
    total_interviews INT DEFAULT 0,
    improvement_rate FLOAT(53),
    category_scores JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trend_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_trend_date UNIQUE (user_id, trend_date)
);

CREATE INDEX IF NOT EXISTS idx_trend_date ON performance_trends(trend_date);
