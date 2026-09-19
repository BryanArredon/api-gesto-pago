CREATE TABLE rate_limit_hits (
    subject      VARCHAR(150) NOT NULL,
    endpoint     VARCHAR(150) NOT NULL,
    window_start TIMESTAMPTZ  NOT NULL,
    count        INTEGER      NOT NULL DEFAULT 0,
    PRIMARY KEY (subject, endpoint, window_start)
);

CREATE INDEX idx_rate_limit_window ON rate_limit_hits (window_start);
