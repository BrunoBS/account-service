ALTER TABLE platform_features
ADD COLUMN shareable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shared_contracts
ADD COLUMN feature_id BIGINT NOT NULL,
ADD INDEX idx_shared_contracts_feature (feature_id),
ADD CONSTRAINT fk_shared_contracts_feature FOREIGN KEY (feature_id) REFERENCES platform_features (id);
