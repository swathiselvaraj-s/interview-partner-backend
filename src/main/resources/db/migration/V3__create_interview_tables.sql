-- Interview Sessions Table
CREATE TABLE IF NOT EXISTS interview_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    mode VARCHAR(20) NOT NULL,
    role VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    time_limit INT,
    total_questions INT DEFAULT 0,
    answered_questions INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    score FLOAT(53) DEFAULT 0.0,
    communication_score FLOAT(53) DEFAULT 0.0,
    clarity_score FLOAT(53) DEFAULT 0.0,
    correctness_score FLOAT(53) DEFAULT 0.0,
    status VARCHAR(20) NOT NULL,
    feedback_summary TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_session_id ON interview_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_user_status ON interview_sessions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_status_started ON interview_sessions(status, started_at);

-- Questions Table
CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    difficulty INT,
    sequence_number INT NOT NULL,
    expected_answer TEXT,
    options JSON,
    correct_option_index INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_session_sequence UNIQUE (session_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_session_questions ON questions(session_id);

-- Answers Table
CREATE TABLE IF NOT EXISTS answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    answer_text TEXT NOT NULL,
    selected_option_index INT,
    is_correct BOOLEAN,
    score FLOAT(53),
    ai_feedback TEXT,
    time_spent_seconds INT,
    code_language VARCHAR(50),
    code_output TEXT,
    execution_log TEXT,
    communication_score FLOAT(53),
    clarity_score FLOAT(53),
    correctness_score FLOAT(53),
    answered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_question_answer UNIQUE (question_id)
);

CREATE INDEX IF NOT EXISTS idx_session_answers ON answers(session_id);
CREATE INDEX IF NOT EXISTS idx_question_answers ON answers(question_id);
