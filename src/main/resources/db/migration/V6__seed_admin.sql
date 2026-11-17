-- Flyway Migration V6: Seed default admin user (or promote if email exists)
INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    role,
    preferred_role,
    preferred_difficulty,
    preferred_mode,
    active
) VALUES (
    'admin@interviewpartner.com',
    '$2a$10$zRkBHmmI9L9NaAip.S6XGOGKqpJTlSBgzaTBO.r6YUOKPy879XVRS',
    'System',
    'Admin',
    'ROLE_ADMIN',
    'JAVA',
    'ADVANCED',
    'CHAT',
    TRUE
)
ON CONFLICT (email) DO UPDATE SET role = 'ROLE_ADMIN';
