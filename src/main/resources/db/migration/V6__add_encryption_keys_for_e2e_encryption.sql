ALTER TABLE users
    ADD encrypted_private_key VARCHAR(4096);

ALTER TABLE users
    ADD public_key VARCHAR(2048);