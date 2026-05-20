CREATE TABLE IF NOT EXISTS registration_verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_registration_verification_email
    ON registration_verification_codes(email);

CREATE INDEX idx_registration_verification_created_at
    ON registration_verification_codes(created_at);
