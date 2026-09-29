-- Standardize every current `settings` column on the native MySQL JSON type.
-- This keeps the database contract aligned with the API/schema contract and
-- also covers catalog tables created by earlier migrations.
-- Existing invalid JSON intentionally makes this migration fail instead of
-- silently preserving data that violates the contract.

DELIMITER $$

CREATE PROCEDURE migrate_settings_columns_to_json()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE table_name_value VARCHAR(64);
    DECLARE nullable_value VARCHAR(3);
    DECLARE settings_columns CURSOR FOR
        SELECT TABLE_NAME, IS_NULLABLE
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND COLUMN_NAME = 'settings'
           AND DATA_TYPE <> 'json';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN settings_columns;

    settings_loop: LOOP
        FETCH settings_columns INTO table_name_value, nullable_value;
        IF done THEN
            LEAVE settings_loop;
        END IF;

        SET @alter_settings_sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` MODIFY COLUMN `settings` JSON ',
            IF(nullable_value = 'YES', 'NULL', 'NOT NULL')
        );

        PREPARE alter_settings_statement FROM @alter_settings_sql;
        EXECUTE alter_settings_statement;
        DEALLOCATE PREPARE alter_settings_statement;
    END LOOP;

    CLOSE settings_columns;
END$$

DELIMITER ;

CALL migrate_settings_columns_to_json();
DROP PROCEDURE migrate_settings_columns_to_json;
