-- Existing V28 installations already have the seven original resource categories.
-- Future domains can now register their own resource_type and DEFAULT binding.
ALTER TABLE schema_configuration
DROP CHECK ck_schema_configuration_resource_type;

ALTER TABLE schema_configuration
MODIFY COLUMN resource_type VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
MODIFY COLUMN resource_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
