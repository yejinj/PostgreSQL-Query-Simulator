package com.simulator.controller;

import com.simulator.service.MetricQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricController {
    
    @Autowired
    private MetricQueryService metricQueryService;
    
    /**
     * 시스템 메트릭 조회
     * GET /api/metrics/system?metric=jvm_cpu_usage_percent&minutes=60
     */
    @GetMapping("/system")
    public List<Map<String, Object>> getSystemMetrics(
            @RequestParam String metric,
            @RequestParam(defaultValue = "60") int minutes) {
        return metricQueryService.getSystemMetrics(metric, minutes);
    }
    
    /**
     * 연결 메트릭 트렌드 조회
     * GET /api/metrics/connections?hours=24
     */
    @GetMapping("/connections")
    public List<Map<String, Object>> getConnectionTrends(
            @RequestParam(defaultValue = "24") int hours) {
        return metricQueryService.getConnectionTrends(hours);
    }
    
    /**
     * 상위 느린 쿼리 조회
     * GET /api/metrics/slow-queries?limit=10&hours=24
     */
    @GetMapping("/slow-queries")
    public List<Map<String, Object>> getSlowQueries(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours) {
        return metricQueryService.getTopSlowQueries(limit, hours);
    }
    
    /**
     * 테이블 사용 통계 조회
     * GET /api/metrics/table-stats?days=7
     */
    @GetMapping("/table-stats")
    public List<Map<String, Object>> getTableStats(
            @RequestParam(defaultValue = "7") int days) {
        return metricQueryService.getTableUsageStats(days);
    }
    
    /**
     * 실시간 대시보드 데이터
     * GET /api/metrics/dashboard
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return metricQueryService.getRealtimeDashboard();
    }
    
    /**
     * 알람 체크
     * GET /api/metrics/alerts
     */
    @GetMapping("/alerts")
    public List<Map<String, Object>> getAlerts() {
        return metricQueryService.checkAlerts();
    }
    
    /**
     * 집계 메트릭 조회
     * GET /api/metrics/aggregated?metric=jvm_cpu_usage_percent&interval=hour&periods=24
     */
    @GetMapping("/aggregated")
    public List<Map<String, Object>> getAggregatedMetrics(
            @RequestParam String metric,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "24") int periods) {
        return metricQueryService.getAggregatedMetrics(metric, interval, periods);
    }
    
    /**
     * 데이터베이스 헬스 점수
     * GET /api/metrics/health
     */
    @GetMapping("/health")
    public Map<String, Object> getHealthScore() {
        return metricQueryService.getDatabaseHealthScore();
    }
    
    /**
     * 사용 가능한 메트릭 목록
     * GET /api/metrics/available
     */
    @GetMapping("/available")
    public Map<String, Object> getAvailableMetrics() {
        Map<String, Object> metrics = Map.of(
            "system_metrics", List.of(
                "jvm_cpu_usage_percent",
                "jvm_memory_used_mb", 
                "jvm_memory_usage_percent",
                "database_size_mb",
                "cache_hit_ratio_percent",
                "total_transactions"
            ),
            "connection_metrics", List.of(
                "active_connections",
                "idle_connections", 
                "connection_utilization_percent",
                "longest_query_duration_ms"
            ),
            "query_metrics", List.of(
                "execution_time_ms",
                "buffer_hits",
                "buffer_misses",
                "cache_hit_ratio"
            ),
            "table_metrics", List.of(
                "table_size_mb",
                "seq_scan_count",
                "idx_scan_count",
                "index_usage_ratio"
            )
        );
        
        return metrics;
    }
} 