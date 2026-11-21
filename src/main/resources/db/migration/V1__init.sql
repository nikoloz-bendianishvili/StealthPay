CREATE TABLE stealth_payment (
    id                   UUID          PRIMARY KEY,
    stealth_address      VARCHAR(42)   NOT NULL,
    ephemeral_public_key VARCHAR(68)   NOT NULL,
    view_tag             INTEGER       NOT NULL CHECK (view_tag BETWEEN 0 AND 255),
    amount_wei           NUMERIC(78,0) NOT NULL CHECK (amount_wei >= 0),
    tx_hash              VARCHAR(66)   NOT NULL,
    block_number         BIGINT        NOT NULL,
    log_index            INTEGER       NOT NULL,
    status               VARCHAR(10)   NOT NULL CHECK (status IN ('UNSPENT', 'SWEPT')),
    CONSTRAINT uq_stealth_payment_tx_log UNIQUE (tx_hash, log_index)
);

CREATE INDEX idx_stealth_payment_status ON stealth_payment (status);

CREATE TABLE scanner_state (
    id         BIGINT PRIMARY KEY CHECK (id = 1),
    last_block BIGINT NOT NULL
);
