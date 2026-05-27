CREATE TABLE IF NOT EXISTS contract_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    landlord_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    default_monthly_rent DECIMAL(12,2) NULL,
    default_deposit_amount DECIMAL(12,2) NULL,
    default_electric_price DECIMAL(10,2) NULL,
    default_water_price DECIMAL(10,2) NULL,
    default_other_fees DECIMAL(10,2) NULL,
    move_in_rules TEXT NULL,
    service_notes TEXT NULL,
    additional_terms TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_templates_landlord
        FOREIGN KEY (landlord_id) REFERENCES users(id)
);

CREATE INDEX idx_contract_templates_landlord_updated
    ON contract_templates (landlord_id, updated_at DESC);
