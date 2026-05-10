ALTER TABLE rooms
    MODIFY status ENUM('ACTIVE','PENDING','REJECTED','HIDDEN','EXPIRED','RENTED') NOT NULL DEFAULT 'PENDING';

CREATE TABLE IF NOT EXISTS viewing_appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    seeker_id BIGINT NOT NULL,
    landlord_id BIGINT NOT NULL,
    requested_at DATETIME NOT NULL,
    message TEXT,
    landlord_note TEXT,
    status ENUM('PENDING','ACCEPTED','RESCHEDULED','REJECTED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_viewing_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_viewing_seeker FOREIGN KEY (seeker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_viewing_landlord FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_viewing_seeker ON viewing_appointments(seeker_id, requested_at);
CREATE INDEX idx_viewing_landlord ON viewing_appointments(landlord_id, requested_at);
CREATE INDEX idx_viewing_status ON viewing_appointments(status);
