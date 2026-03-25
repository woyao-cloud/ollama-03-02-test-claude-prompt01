-- OAuth2 Connection Table
-- Stores OAuth2 provider connections for users

CREATE TABLE oauth2_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    provider_username VARCHAR(100),
    display_name VARCHAR(100),
    email VARCHAR(255),
    avatar_url VARCHAR(500),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_oauth2_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_oauth2_provider_user UNIQUE (provider, provider_user_id)
);

-- Indexes
CREATE INDEX idx_oauth2_user ON oauth2_connections(user_id);
CREATE INDEX idx_oauth2_provider ON oauth2_connections(provider);
CREATE INDEX idx_oauth2_provider_user ON oauth2_connections(provider, provider_user_id);

-- Comments
COMMENT ON TABLE oauth2_connections IS 'OAuth2 provider connections for users';
COMMENT ON COLUMN oauth2_connections.provider IS 'OAuth2 provider type (GOOGLE, GITHUB, etc.)';
COMMENT ON COLUMN oauth2_connections.provider_user_id IS 'User ID from the OAuth2 provider';
COMMENT ON COLUMN oauth2_connections.is_primary IS 'Whether this is the primary OAuth2 connection for the user';
