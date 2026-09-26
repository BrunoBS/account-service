ALTER TABLE messages
DROP
FOREIGN KEY fk_messages_platform_service;

ALTER TABLE platform_services
DROP
CHECK ck_platform_services_code;

ALTER TABLE platform_features
DROP
CHECK ck_platform_features_code;

ALTER TABLE platform_feature_contexts
DROP
CHECK ck_platform_feature_contexts_code;

UPDATE platform_services
SET code = LOWER(REPLACE(code, '_', '-'));

UPDATE platform_features
SET code = LOWER(REPLACE(code, '_', '-'));

UPDATE platform_feature_contexts
SET code = LOWER(REPLACE(code, '_', '-'));

UPDATE messages
SET service_code = LOWER(REPLACE(service_code, '_', '-'));

ALTER TABLE platform_services
    ADD CONSTRAINT ck_platform_services_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE platform_features
    ADD CONSTRAINT ck_platform_features_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE platform_feature_contexts
    ADD CONSTRAINT ck_platform_feature_contexts_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_platform_service
        FOREIGN KEY (service_code) REFERENCES platform_services (code);
