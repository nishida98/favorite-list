CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE UNIQUE INDEX uq_users_email ON users (email);
CREATE UNIQUE INDEX uq_users_nickname_lower ON users ((LOWER(nickname)));
CREATE INDEX idx_users_deleted_at ON users (deleted_at);

CREATE TABLE user_login_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NULL,
    attempted_email VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(80) NULL,
    token_jti VARCHAR(255) NULL,
    token_hash VARCHAR(255) NULL,
    issued_at TIMESTAMP WITH TIME ZONE NULL,
    expires_at TIMESTAMP WITH TIME ZONE NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NULL,
    last_used_at TIMESTAMP WITH TIME ZONE NULL,
    ip_address VARCHAR(80) NULL,
    user_agent VARCHAR(512) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_login_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_user_login_sessions_status
        CHECK (status IN ('SUCCESS', 'FAILED', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_user_login_sessions_token_jti ON user_login_sessions (token_jti);
CREATE INDEX idx_user_login_sessions_user_id ON user_login_sessions (user_id);
CREATE INDEX idx_user_login_sessions_status ON user_login_sessions (status);
CREATE INDEX idx_user_login_sessions_expires_at ON user_login_sessions (expires_at);
