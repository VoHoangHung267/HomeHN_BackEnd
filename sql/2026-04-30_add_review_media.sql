USE homehn_db;

CREATE TABLE IF NOT EXISTS review_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    media_type ENUM('IMAGE','VIDEO') NOT NULL,
    media_url TEXT NOT NULL,
    public_id VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_media_review
        FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);

CREATE INDEX idx_review_media_review_sort
    ON review_media(review_id, sort_order);
