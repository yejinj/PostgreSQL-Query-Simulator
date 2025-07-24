-- TimescaleDB 확장 활성화 (필요시)
-- CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 1. 시스템 메트릭 테이블
CREATE TABLE system_metrics (
    time TIMESTAMPTZ NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    host_name VARCHAR(100) NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    tags JSONB,
    PRIMARY KEY (time, metric_name, host_name, database_name)
);

-- 2. 쿼리 성능 메트릭 테이블
CREATE TABLE query_performance_metrics (
    time TIMESTAMPTZ NOT NULL,
    query_id VARCHAR(100),
    query_hash VARCHAR(64),
    execution_time_ms DOUBLE PRECISION,
    cpu_usage_percent DOUBLE PRECISION,
    io_wait_time_ms DOUBLE PRECISION,
    memory_usage_mb DOUBLE PRECISION,
    disk_reads INTEGER,
    disk_writes INTEGER,
    buffer_hits INTEGER,
    buffer_misses INTEGER,
    node_type VARCHAR(50),
    operation_type VARCHAR(50),
    table_name VARCHAR(100),
    index_name VARCHAR(100),
    database_name VARCHAR(100) NOT NULL,
    host_name VARCHAR(100) NOT NULL,
    user_name VARCHAR(100),
    application_name VARCHAR(100),
    PRIMARY KEY (time, query_hash, host_name, database_name)
);

-- 3. 연결 및 세션 메트릭 테이블
CREATE TABLE connection_metrics (
    time TIMESTAMPTZ NOT NULL,
    active_connections INTEGER,
    idle_connections INTEGER,
    waiting_connections INTEGER,
    max_connections INTEGER,
    connection_utilization_percent DOUBLE PRECISION,
    longest_query_duration_ms DOUBLE PRECISION,
    longest_transaction_duration_ms DOUBLE PRECISION,
    database_name VARCHAR(100) NOT NULL,
    host_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (time, host_name, database_name)
);

-- 4. 락 및 대기 메트릭 테이블
CREATE TABLE lock_metrics (
    time TIMESTAMPTZ NOT NULL,
    lock_type VARCHAR(50),
    lock_mode VARCHAR(50),
    granted BOOLEAN,
    wait_duration_ms DOUBLE PRECISION,
    blocking_query_id VARCHAR(100),
    blocked_query_id VARCHAR(100),
    table_name VARCHAR(100),
    database_name VARCHAR(100) NOT NULL,
    host_name VARCHAR(100) NOT NULL
);

-- 5. 테이블 및 인덱스 통계 테이블
CREATE TABLE table_index_metrics (
    time TIMESTAMPTZ NOT NULL,
    schema_name VARCHAR(100),
    table_name VARCHAR(100),
    index_name VARCHAR(100),
    table_size_mb DOUBLE PRECISION,
    index_size_mb DOUBLE PRECISION,
    seq_scan_count BIGINT,
    seq_tup_read BIGINT,
    idx_scan_count BIGINT,
    idx_tup_fetch BIGINT,
    n_tup_ins BIGINT,
    n_tup_upd BIGINT,
    n_tup_del BIGINT,
    vacuum_count BIGINT,
    autovacuum_count BIGINT,
    analyze_count BIGINT,
    autoanalyze_count BIGINT,
    database_name VARCHAR(100) NOT NULL,
    host_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (time, schema_name, table_name, host_name, database_name)
);

-- TimescaleDB 하이퍼테이블 생성 (TimescaleDB 사용시)
-- SELECT create_hypertable('system_metrics', 'time');
-- SELECT create_hypertable('query_performance_metrics', 'time');
-- SELECT create_hypertable('connection_metrics', 'time');
-- SELECT create_hypertable('lock_metrics', 'time');
-- SELECT create_hypertable('table_index_metrics', 'time');

-- 인덱스 생성
CREATE INDEX idx_system_metrics_time_name ON system_metrics (time DESC, metric_name);
CREATE INDEX idx_query_metrics_time_hash ON query_performance_metrics (time DESC, query_hash);
CREATE INDEX idx_query_metrics_execution_time ON query_performance_metrics (execution_time_ms DESC);
CREATE INDEX idx_connection_metrics_time ON connection_metrics (time DESC);
CREATE INDEX idx_lock_metrics_time_type ON lock_metrics (time DESC, lock_type);
CREATE INDEX idx_table_metrics_time_table ON table_index_metrics (time DESC, table_name);

-- 데이터 보존 정책 (선택사항)
-- SELECT add_retention_policy('system_metrics', INTERVAL '30 days');
-- SELECT add_retention_policy('query_performance_metrics', INTERVAL '7 days');
-- SELECT add_retention_policy('connection_metrics', INTERVAL '30 days');
-- SELECT add_retention_policy('lock_metrics', INTERVAL '7 days');
-- SELECT add_retention_policy('table_index_metrics', INTERVAL '30 days'); 