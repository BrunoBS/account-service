-- Validate existing history before reducing the column size; do not truncate identities.
ALTER TABLE workspace_onboarding_completions
    ADD CONSTRAINT chk_onboarding_requester
        CHECK (CHAR_LENGTH(TRIM(user_identifier)) > 0 AND CHAR_LENGTH(user_identifier) <= 10);

ALTER TABLE workspace_onboarding_completions
    DROP CHECK chk_onboarding_requester,
    CHANGE COLUMN user_identifier requester VARCHAR(10) NOT NULL,
    ADD CONSTRAINT chk_onboarding_requester
        CHECK (CHAR_LENGTH(TRIM(requester)) > 0 AND CHAR_LENGTH(requester) <= 10);
