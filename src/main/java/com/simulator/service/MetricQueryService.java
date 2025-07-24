package com.simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class MetricQueryService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 시스템 메트릭 조회 (최근 N분간)
     */
    public List<Map<String, Object>> getSystemMetrics(String metricName, int minutesBack) {
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(minutesBack);
        
        String sql = """
            SELECT 
                time,
                metric_name,
                metric_value,
                host_name,
                database_name,
                tags
            FROM system_metrics 
            WHERE time >= ? 
            AND metric_name = ?
            ORDER BY time DESC
            """;
        
        return jdbcTemplate.queryForList(sql, since, metricName);
    }
    
    /**
     * 연결 메트릭 트렌드 조회
     */
    public List<Map<String, Object>> getConnectionTrends(int hoursBack) {
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusHours(hoursBack);
        
        String sql = """
            SELECT 
                DATE_TRUNC('minute', time) as time_bucket,
                AVG(active_connections) as avg_active_connections,
                AVG(idle_connections) as avg_idle_connections,
                AVG(connection_utilization_percent) as avg_utilization,
                MAX(longest_query_duration_ms) as max_query_duration,
                MAX(longest_transaction_duration_ms) as max_transaction_duration
            FROM connection_metrics 
            WHERE time >= ?
            GROUP BY time_bucket
            ORDER BY time_bucket DESC
            """;
        
        return jdbcTemplate.queryForList(sql, since);
    }
    
    /**
     * 쿼리 성능 상위 N개 조회
     */
    public List<Map<String, Object>> getTopSlowQueries(int limit, int hoursBack) {
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusHours(hoursBack);
        
        String sql = """
            SELECT 
                query_hash,
                AVG(execution_time_ms) as avg_execution_time,
                MAX(execution_time_ms) as max_execution_time,
                COUNT(*) as execution_count,
                SUM(buffer_hits) as total_buffer_hits,
                SUM(buffer_misses) as total_buffer_misses,
                ROUND(SUM(buffer_hits)::numeric / NULLIF(SUM(buffer_hits) + SUM(buffer_misses), 0) * 100, 2) as cache_hit_ratio
            FROM query_performance_metrics 
            WHERE time >= ?
            AND execution_time_ms IS NOT NULL
            GROUP BY query_hash
            ORDER BY avg_execution_time DESC
            LIMIT ?
            """;
        
        return jdbcTemplate.queryForList(sql, since, limit);
    }
    
    /**
     * 테이블 사용 통계 조회
     */
    public List<Map<String, Object>> getTableUsageStats(int daysBack) {
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusDays(daysBack);
        
        String sql = """
            WITH latest_stats AS (
                SELECT DISTINCT ON (schema_name, table_name)
                    schema_name,
                    table_name,
                    table_size_mb,
                    seq_scan_count,
                    idx_scan_count,
                    n_tup_ins,
                    n_tup_upd,
                    n_tup_del,
                    time
                FROM table_index_metrics
                WHERE time >= ?
                ORDER BY schema_name, table_name, time DESC
            )
            SELECT 
                schema_name,
                table_name,
                table_size_mb,
                seq_scan_count,
                idx_scan_count,
                CASE 
                    WHEN seq_scan_count + idx_scan_count = 0 THEN 0
                    ELSE ROUND(idx_scan_count::numeric / (seq_scan_count + idx_scan_count) * 100, 2)
                END as index_usage_ratio,
                n_tup_ins + n_tup_upd + n_tup_del as total_modifications
            FROM latest_stats
            ORDER BY table_size_mb DESC
            """;
        
        return jdbcTemplate.queryForList(sql, since);
    }
    
    /**
     * 실시간 성능 대시보드 데이터
     */
    public Map<String, Object> getRealtimeDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 현재 연결 상태
        String connectionSql = """
            SELECT 
                active_connections,
                idle_connections,
                connection_utilization_percent,
                longest_query_duration_ms
            FROM connection_metrics 
            ORDER BY time DESC 
            LIMIT 1
            """;
        
        Map<String, Object> connectionStatus = jdbcTemplate.queryForMap(connectionSql);
        dashboard.put("connections", connectionStatus);
        
        // 최근 시스템 메트릭
        String systemSql = """
            SELECT 
                metric_name,
                metric_value,
                time
            FROM system_metrics 
            WHERE time >= NOW() - INTERVAL '5 minutes'
            AND metric_name IN ('jvm_cpu_usage_percent', 'jvm_memory_usage_percent', 'cache_hit_ratio_percent')
            ORDER BY time DESC
            """;
        
        List<Map<String, Object>> systemMetrics = jdbcTemplate.queryForList(systemSql);
        dashboard.put("system_metrics", systemMetrics);
        
        // 현재 실행 중인 쿼리
        String activeSql = """
            SELECT 
                COUNT(*) FILTER (WHERE state = 'active') as active_queries,
                COUNT(*) FILTER (WHERE state = 'idle in transaction') as idle_in_transaction,
                MAX(EXTRACT(EPOCH FROM (NOW() - query_start)) * 1000) as longest_running_ms
            FROM pg_stat_activity 
            WHERE datname = current_database()
            """;
        
        Map<String, Object> activeQueries = jdbcTemplate.queryForMap(activeSql);
        dashboard.put("active_queries", activeQueries);
        
        return dashboard;
    }
    
    /**
     * 메트릭 알람 조건 체크
     */
    public List<Map<String, Object>> checkAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // CPU 사용률 알람 (80% 이상)
        String cpuAlertSql = """
            SELECT 
                'CPU_HIGH' as alert_type,
                metric_value as current_value,
                '80' as threshold,
                time
            FROM system_metrics 
            WHERE metric_name = 'jvm_cpu_usage_percent'
            AND metric_value > 80
            AND time >= NOW() - INTERVAL '5 minutes'
            ORDER BY time DESC 
            LIMIT 1
            """;
        
        List<Map<String, Object>> cpuAlerts = jdbcTemplate.queryForList(cpuAlertSql);
        alerts.addAll(cpuAlerts);
        
        // 연결 사용률 알람 (90% 이상)
        String connectionAlertSql = """
            SELECT 
                'CONNECTION_HIGH' as alert_type,
                connection_utilization_percent as current_value,
                '90' as threshold,
                time
            FROM connection_metrics 
            WHERE connection_utilization_percent > 90
            AND time >= NOW() - INTERVAL '5 minutes'
            ORDER BY time DESC 
            LIMIT 1
            """;
        
        List<Map<String, Object>> connAlerts = jdbcTemplate.queryForList(connectionAlertSql);
        alerts.addAll(connAlerts);
        
        // 느린 쿼리 알람 (5초 이상)
        String slowQueryAlertSql = """
            SELECT 
                'SLOW_QUERY' as alert_type,
                execution_time_ms as current_value,
                '5000' as threshold,
                time
            FROM query_performance_metrics 
            WHERE execution_time_ms > 5000
            AND time >= NOW() - INTERVAL '5 minutes'
            ORDER BY time DESC 
            LIMIT 5
            """;
        
        List<Map<String, Object>> slowAlerts = jdbcTemplate.queryForList(slowQueryAlertSql);
        alerts.addAll(slowAlerts);
        
        return alerts;
    }
    
    /**
     * 시계열 데이터 집계 (시간대별, 일별 등)
     */
    public List<Map<String, Object>> getAggregatedMetrics(String metricName, String interval, int periodsBack) {
        String truncFunction;
        String intervalString;
        
        switch (interval.toLowerCase()) {
            case "hour":
                truncFunction = "hour";
                intervalString = periodsBack + " hours";
                break;
            case "day":
                truncFunction = "day";
                intervalString = periodsBack + " days";
                break;
            case "minute":
                truncFunction = "minute";
                intervalString = periodsBack + " minutes";
                break;
            default:
                truncFunction = "hour";
                intervalString = periodsBack + " hours";
        }
        
        String sql = String.format("""
            SELECT 
                DATE_TRUNC('%s', time) as time_bucket,
                AVG(metric_value) as avg_value,
                MIN(metric_value) as min_value,
                MAX(metric_value) as max_value,
                COUNT(*) as data_points
            FROM system_metrics 
            WHERE metric_name = ?
            AND time >= NOW() - INTERVAL '%s'
            GROUP BY time_bucket
            ORDER BY time_bucket DESC
            """, truncFunction, intervalString);
        
        return jdbcTemplate.queryForList(sql, metricName);
    }
    
    /**
     * 데이터베이스 헬스 체크 종합 점수
     */
    public Map<String, Object> getDatabaseHealthScore() {
        Map<String, Object> healthScore = new HashMap<>();
        
        try {
            // CPU 점수 (80% 이하면 만점)
            String cpuSql = """
                SELECT AVG(metric_value) as avg_cpu 
                FROM system_metrics 
                WHERE metric_name = 'jvm_cpu_usage_percent' 
                AND time >= NOW() - INTERVAL '30 minutes'
                """;
            Double avgCpu = jdbcTemplate.queryForObject(cpuSql, Double.class);
            int cpuScore = Math.max(0, 100 - (int)((avgCpu != null ? avgCpu : 0) - 80) * 5); // 80% 이상에서 점수 감점
            
            // 연결 점수
            String connSql = """
                SELECT AVG(connection_utilization_percent) as avg_conn 
                FROM connection_metrics 
                WHERE time >= NOW() - INTERVAL '30 minutes'
                """;
            Double avgConn = jdbcTemplate.queryForObject(connSql, Double.class);
            int connScore = Math.max(0, 100 - (int)((avgConn != null ? avgConn : 0) - 70) * 3); // 70% 이상에서 점수 감점
            
            // 캐시 히트 점수 (95% 이상이면 만점)
            String cacheSql = """
                SELECT AVG(metric_value) as avg_cache 
                FROM system_metrics 
                WHERE metric_name = 'cache_hit_ratio_percent' 
                AND time >= NOW() - INTERVAL '30 minutes'
                """;
            Double avgCache = jdbcTemplate.queryForObject(cacheSql, Double.class);
            int cacheScore = Math.min(100, (int)(avgCache != null ? avgCache : 0));
            
            // 종합 점수
            int totalScore = (cpuScore + connScore + cacheScore) / 3;
            
            healthScore.put("total_score", totalScore);
            healthScore.put("cpu_score", cpuScore);
            healthScore.put("connection_score", connScore);
            healthScore.put("cache_score", cacheScore);
            healthScore.put("status", totalScore >= 80 ? "HEALTHY" : totalScore >= 60 ? "WARNING" : "CRITICAL");
            
        } catch (Exception e) {
            healthScore.put("total_score", 0);
            healthScore.put("status", "ERROR");
            healthScore.put("error", e.getMessage());
        }
        
        return healthScore;
    }
} 