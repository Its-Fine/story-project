ALTER TABLE users
    ADD COLUMN oidc_issuer VARCHAR(255),
    ADD COLUMN oidc_subject VARCHAR(255);

CREATE UNIQUE INDEX ux_users_oidc_identity
    ON users (oidc_issuer, oidc_subject)
    WHERE oidc_issuer IS NOT NULL AND oidc_subject IS NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT ck_users_oidc_identity_pair
    CHECK (
        (oidc_issuer IS NULL AND oidc_subject IS NULL)
        OR (oidc_issuer IS NOT NULL AND oidc_subject IS NOT NULL)
    );
