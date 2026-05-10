USE homehn_db;

ALTER TABLE reports
    ADD COLUMN landlord_response_type ENUM('WILL_FIX','CONTEST') NULL AFTER admin_note,
    ADD COLUMN landlord_response_note TEXT NULL AFTER landlord_response_type,
    ADD COLUMN landlord_responded_at DATETIME NULL AFTER landlord_response_note;
