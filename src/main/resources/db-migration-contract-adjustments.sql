CREATE TABLE IF NOT EXISTS contract_adjustments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    seeker_id BIGINT NOT NULL,
    landlord_id BIGINT NOT NULL,
    proposer_role ENUM('SEEKER','LANDLORD') NOT NULL,
    extension_months INT NULL,
    proposed_monthly_rent DECIMAL(12,2) NULL,
    proposed_deposit_amount DECIMAL(12,2) NULL,
    proposed_electric_price DECIMAL(10,2) NULL,
    proposed_water_price DECIMAL(10,2) NULL,
    proposed_other_fees DECIMAL(10,2) NULL,
    proposed_contract_terms TEXT NULL,
    proposed_move_in_rules TEXT NULL,
    proposed_service_notes TEXT NULL,
    proposed_additional_terms TEXT NULL,
    proposal_note TEXT NULL,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    responder_role ENUM('SEEKER','LANDLORD') NULL,
    response_note TEXT NULL,
    responded_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_adjustment_booking FOREIGN KEY (booking_id) REFERENCES rental_bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_adjustment_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_adjustment_seeker FOREIGN KEY (seeker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_adjustment_landlord FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_contract_adjustment_booking_created ON contract_adjustments(booking_id, created_at);
CREATE INDEX idx_contract_adjustment_booking_status ON contract_adjustments(booking_id, status);
