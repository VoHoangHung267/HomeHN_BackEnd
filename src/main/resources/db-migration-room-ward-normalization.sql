SET SQL_SAFE_UPDATES = 0;

UPDATE rooms
SET ward = TRIM(COALESCE(NULLIF(ward, ''), district))
WHERE ward IS NULL OR TRIM(ward) = '';

UPDATE rooms SET ward = 'Cửa Nam' WHERE ward = 'Cua Nam';
UPDATE rooms SET ward = 'Ngọc Hà' WHERE ward = 'Ngoc Ha';
UPDATE rooms SET ward = 'Văn Miếu - Quốc Tử Giám' WHERE ward IN ('Văn Miếu', 'Van Mieu', 'Văn Miếu - Quốc Tử Giám');
UPDATE rooms SET ward = 'Ô Chợ Dừa' WHERE ward IN ('Ô Chợ Dừa', 'O Cho Dua');
UPDATE rooms SET ward = 'Nghĩa Đô' WHERE ward IN ('Nghia Do', 'Nghĩa Đô');
UPDATE rooms SET ward = 'Yên Hòa' WHERE ward IN ('Yen Hoa', 'Yên Hòa');
UPDATE rooms SET ward = 'Phú Thượng' WHERE ward IN ('Phu Thuong', 'Phú Thượng');
UPDATE rooms SET ward = 'Phú Diễn' WHERE ward IN ('Phu Dien', 'Phú Diễn');
UPDATE rooms SET ward = 'Xuân Đỉnh' WHERE ward IN ('Xuan Dinh', 'Xuân Đỉnh');
UPDATE rooms SET ward = 'Thượng Cát' WHERE ward IN ('Thuong Cat', 'Thượng Cát');
UPDATE rooms SET ward = 'Tây Mỗ' WHERE ward IN ('Tay Mo', 'Tây Mỗ');
UPDATE rooms SET ward = 'Bồ Đề' WHERE ward IN ('Bo De', 'Bồ Đề');
UPDATE rooms SET ward = 'Việt Hưng' WHERE ward IN ('Viet Hung', 'Việt Hưng');
UPDATE rooms SET ward = 'Phúc Lợi' WHERE ward IN ('Phuc Loi', 'Phúc Lợi');
UPDATE rooms SET ward = 'Dương Nội' WHERE ward IN ('Duong Noi', 'Dương Nội');
UPDATE rooms SET ward = 'Yên Nghĩa' WHERE ward IN ('Yen Nghia', 'Yên Nghĩa');
UPDATE rooms SET ward = 'Thanh Liệt' WHERE ward IN ('Thanh Liet', 'Thanh Liệt');
UPDATE rooms SET ward = 'Thường Tín' WHERE ward IN ('Thuong Tin', 'Thường Tín');
UPDATE rooms SET ward = 'Sơn Tây' WHERE ward IN ('Son Tay', 'Sơn Tây');
UPDATE rooms SET ward = 'Quốc Oai' WHERE ward IN ('Quoc Oai', 'Quốc Oai');
UPDATE rooms SET ward = 'Thuận An' WHERE ward IN ('Thuan An', 'Thuận An');
UPDATE rooms SET ward = 'Đông Anh' WHERE ward IN ('Dong Anh', 'Đông Anh');
UPDATE rooms SET ward = 'Sóc Sơn' WHERE ward IN ('Soc Son', 'Sóc Sơn');
UPDATE rooms SET ward = 'Nội Bài' WHERE ward IN ('Noi Bai', 'Nội Bài');

ALTER TABLE rooms
MODIFY COLUMN ward VARCHAR(255) NOT NULL;

SET @drop_district_index := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'rooms'
              AND index_name = 'idx_rooms_district'
        ),
        'DROP INDEX idx_rooms_district ON rooms',
        'SELECT 1'
    )
);
PREPARE stmt_drop_district_index FROM @drop_district_index;
EXECUTE stmt_drop_district_index;
DEALLOCATE PREPARE stmt_drop_district_index;

SET @drop_district_column := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'rooms'
              AND column_name = 'district'
        ),
        'ALTER TABLE rooms DROP COLUMN district',
        'SELECT 1'
    )
);
PREPARE stmt_drop_district_column FROM @drop_district_column;
EXECUTE stmt_drop_district_column;
DEALLOCATE PREPARE stmt_drop_district_column;

SET @create_ward_index := (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'rooms'
              AND index_name = 'idx_rooms_ward'
        ),
        'SELECT 1',
        'CREATE INDEX idx_rooms_ward ON rooms(ward)'
    )
);
PREPARE stmt_create_ward_index FROM @create_ward_index;
EXECUTE stmt_create_ward_index;
DEALLOCATE PREPARE stmt_create_ward_index;

SET SQL_SAFE_UPDATES = 1;
