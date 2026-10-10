-- Complete the settings contract for resources that did not yet persist settings.
-- Existing rows receive an empty JSON object so the new contract remains NOT NULL.
ALTER TABLE platform_microservices
ADD COLUMN settings JSON NULL AFTER description;

UPDATE platform_microservices
SET
    settings = JSON_OBJECT()
WHERE
    settings IS NULL;

ALTER TABLE platform_microservices
MODIFY COLUMN settings JSON NOT NULL;

ALTER TABLE publishers
ADD COLUMN settings JSON NULL AFTER deprecated;

UPDATE publishers
SET
    settings = JSON_OBJECT()
WHERE
    settings IS NULL;

ALTER TABLE publishers
MODIFY COLUMN settings JSON NOT NULL;
