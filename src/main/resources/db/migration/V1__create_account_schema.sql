CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    requester VARCHAR(255) NOT NULL,
    acronym VARCHAR(5) NOT NULL,
    settings TEXT NULL,
    authorizer_group VARCHAR(255) NULL,
    email_group VARCHAR(320) NOT NULL,
    onboarding BOOLEAN NOT NULL DEFAULT FALSE,
    lifecycle VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_identifier UNIQUE (identifier),
    CONSTRAINT uk_accounts_name UNIQUE (name),
    CONSTRAINT ck_accounts_account_type CHECK (account_type IN ('ADMIN', 'MANAGER')),
    CONSTRAINT ck_accounts_lifecycle CHECK (lifecycle IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE account_approvers (
    id VARCHAR(36) NOT NULL,
    account_id BIGINT NOT NULL,
    functional VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    CONSTRAINT pk_account_approvers PRIMARY KEY (id),
    CONSTRAINT fk_account_approvers_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_account_approvers_account_id ON account_approvers (account_id);

CREATE INDEX idx_accounts_lifecycle ON accounts (lifecycle);

CREATE INDEX idx_accounts_account_type ON accounts (account_type);
