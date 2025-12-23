-- Enable TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Metrics table (denormalized)
CREATE TABLE IF NOT EXISTS metric_samples (
    ts            TIMESTAMPTZ       NOT NULL,
    metric_name   TEXT              NOT NULL,
    metric_type   TEXT              NOT NULL,
    value         DOUBLE PRECISION,
    sketch        BYTEA,
    labels        JSONB             NOT NULL
);

-- Convert to hypertable
SELECT create_hypertable(
    'metric_samples',
    'ts',
    if_not_exists => TRUE
);

-- Core index: metric + time
CREATE INDEX IF NOT EXISTS idx_metric_name_ts
ON metric_samples (metric_name, ts DESC);

-- Labels index (high selectivity)
CREATE INDEX IF NOT EXISTS idx_metric_labels
ON metric_samples
USING GIN (labels jsonb_path_ops);

-- Optional metric type index
CREATE INDEX IF NOT EXISTS idx_metric_type
ON metric_samples (metric_type);

-- Enable compression
ALTER TABLE metric_samples
SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'metric_name'
);

-- Compress chunks older than 7 days
SELECT add_compression_policy(
    'metric_samples',
    INTERVAL '7 days'
);

-- Retain data for 90 days
SELECT add_retention_policy(
    'metric_samples',
    INTERVAL '90 days'
);

-- Daily chunks
SELECT set_chunk_time_interval(
    'metric_samples',
    INTERVAL '1 day'
);
