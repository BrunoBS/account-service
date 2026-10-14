-- Environment names are limited to 50 characters by the published request contract.
-- Settings are JSON documents and are now persisted as native JSON values.
ALTER TABLE environments
    MODIFY name VARCHAR(50) NOT NULL,
    MODIFY settings JSON NOT NULL;
