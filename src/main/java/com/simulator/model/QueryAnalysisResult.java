package com.simulator.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// 전체 쿼리 분석 결과를 담는 객체
public class QueryAnalysisResult {

    // 사용자가 입력한 원본 SQL 쿼리
    private String originalQuery;

    // PostgreSQL의 실행계획 (FORMAT JSON 형태 문자열)
    private String executionPlan;

    // 실제 실행에서 사용된 리소스 정보
    private ResourceUsage resourceUsage;

    // 리소스를 기반으로 계산된 성능 메트릭
    private ResourceMetrics resourceMetrics;

    // 성능 개선을 위한 추천 리스트
    private List<OptimizationSuggestion> optimizationSuggestions;

    // 시간별 메트릭 데이터 (차트 시각화용)
    private TimeSeriesMetrics timeSeriesMetrics;

    // 감지된 병목 지점들
    private List<BottleneckPoint> bottleneckPoints;

    // AI 기반 병목 분석 결과
    private List<AIBottleneckAnalysis> aiBottleneckAnalyses;

    // AI가 생성한 최적화된 SQL 쿼리
    private String optimizedQuery;

    // 쿼리 실행 결과 (SELECT 문일 경우)
    private List<java.util.Map<String, Object>> queryResult;

    // 기본 생성자
    public QueryAnalysisResult() {}

    // --- Getter / Setter ---
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public String getExecutionPlan() { return executionPlan; }
    public void setExecutionPlan(String executionPlan) { this.executionPlan = executionPlan; }

    public ResourceUsage getResourceUsage() { return resourceUsage; }
    public void setResourceUsage(ResourceUsage resourceUsage) { this.resourceUsage = resourceUsage; }

    public ResourceMetrics getResourceMetrics() { return resourceMetrics; }
    public void setResourceMetrics(ResourceMetrics resourceMetrics) { this.resourceMetrics = resourceMetrics; }

    public List<OptimizationSuggestion> getOptimizationSuggestions() { return optimizationSuggestions; }
    public void setOptimizationSuggestions(List<OptimizationSuggestion> optimizationSuggestions) {
        this.optimizationSuggestions = optimizationSuggestions;
    }

    public TimeSeriesMetrics getTimeSeriesMetrics() { return timeSeriesMetrics; }
    public void setTimeSeriesMetrics(TimeSeriesMetrics timeSeriesMetrics) { this.timeSeriesMetrics = timeSeriesMetrics; }

    public List<BottleneckPoint> getBottleneckPoints() { return bottleneckPoints; }
    public void setBottleneckPoints(List<BottleneckPoint> bottleneckPoints) { this.bottleneckPoints = bottleneckPoints; }

    public List<AIBottleneckAnalysis> getAiBottleneckAnalyses() { return aiBottleneckAnalyses; }
    public void setAiBottleneckAnalyses(List<AIBottleneckAnalysis> aiBottleneckAnalyses) { this.aiBottleneckAnalyses = aiBottleneckAnalyses; }

    public String getOptimizedQuery() { return optimizedQuery; }
    public void setOptimizedQuery(String optimizedQuery) { this.optimizedQuery = optimizedQuery; }

    // 편의 메서드들 (템플릿에서 사용)
    public double getActualTime() {
        return resourceUsage != null ? resourceUsage.getActualTime() : 0.0;
    }

    public long getTotalRows() {
        return resourceUsage != null ? resourceUsage.getPlanRows() : 0L;
    }

    public long getTotalCost() {
        return resourceUsage != null ? resourceUsage.getTotalCost() : 0L;
    }

    public List<java.util.Map<String, Object>> getQueryResult() { return queryResult; }
    public void setQueryResult(List<java.util.Map<String, Object>> queryResult) { this.queryResult = queryResult; }

    // ---------------------------------------------------------
    // 내부 클래스 1: 리소스 사용량 통계
    public static class ResourceUsage {
        private long totalCost;             // 실행 계획 상 비용
        private long planRows;              // 예상 반환 행 수
        private long planWidth;             // 한 행의 평균 크기
        private double actualTime;          // 실제 실행 시간
        private long sharedBlksHit;         // 캐시에서 읽은 블록 수
        private long sharedBlksRead;        // 디스크에서 읽은 블록 수
        private long sharedBlksDirtied;     // 수정된 블록 수
        private long sharedBlksWritten;     // 디스크에 기록한 블록 수

        // --- Getter / Setter ---
        public long getTotalCost() { return totalCost; }
        public void setTotalCost(long totalCost) { this.totalCost = totalCost; }

        public long getPlanRows() { return planRows; }
        public void setPlanRows(long planRows) { this.planRows = planRows; }

        public long getPlanWidth() { return planWidth; }
        public void setPlanWidth(long planWidth) { this.planWidth = planWidth; }

        public double getActualTime() { return actualTime; }
        public void setActualTime(double actualTime) { this.actualTime = actualTime; }

        public long getSharedBlksHit() { return sharedBlksHit; }
        public void setSharedBlksHit(long sharedBlksHit) { this.sharedBlksHit = sharedBlksHit; }

        public long getSharedBlksRead() { return sharedBlksRead; }
        public void setSharedBlksRead(long sharedBlksRead) { this.sharedBlksRead = sharedBlksRead; }

        public long getSharedBlksDirtied() { return sharedBlksDirtied; }
        public void setSharedBlksDirtied(long sharedBlksDirtied) { this.sharedBlksDirtied = sharedBlksDirtied; }

        public long getSharedBlksWritten() { return sharedBlksWritten; }
        public void setSharedBlksWritten(long sharedBlksWritten) { this.sharedBlksWritten = sharedBlksWritten; }
    }

    // ---------------------------------------------------------
    // 내부 클래스 2: 계산된 성능 지표
    public static class ResourceMetrics {
        private double cpuIntensity;          // CPU 사용 비중
        private double ioEfficiency;          // I/O 효율성
        private double memoryEfficiency;      // 메모리 효율성
        private double networkEfficiency;     // 네트워크 사용 효율
        private double overallPerformance;    // 전체 점수 (0~100)
        private String performanceGrade;      // 등급 (A/B/C...)

        // --- Getter / Setter ---
        public double getCpuIntensity() { return cpuIntensity; }
        public void setCpuIntensity(double cpuIntensity) { this.cpuIntensity = cpuIntensity; }

        public double getIoEfficiency() { return ioEfficiency; }
        public void setIoEfficiency(double ioEfficiency) { this.ioEfficiency = ioEfficiency; }

        public double getMemoryEfficiency() { return memoryEfficiency; }
        public void setMemoryEfficiency(double memoryEfficiency) { this.memoryEfficiency = memoryEfficiency; }

        public double getNetworkEfficiency() { return networkEfficiency; }
        public void setNetworkEfficiency(double networkEfficiency) { this.networkEfficiency = networkEfficiency; }

        public double getOverallPerformance() { return overallPerformance; }
        public void setOverallPerformance(double overallPerformance) { this.overallPerformance = overallPerformance; }

        public String getPerformanceGrade() { return performanceGrade; }
        public void setPerformanceGrade(String performanceGrade) { this.performanceGrade = performanceGrade; }
    }

    // ---------------------------------------------------------
    // 내부 클래스 3: 최적화 제안
    public static class OptimizationSuggestion {
        private String type;                         // 예: "인덱스 추천"
        private String description;                  // 어떤 부분이 문제인지 설명
        private String suggestedQuery;               // 수정된 쿼리 제안
        private double expectedPerformanceImprovement; // 개선 예상 점수 (예: +20%)

        // 생성자 (제안 종류, 설명)
        public OptimizationSuggestion(String type, String description) {
            this.type = type;
            this.description = description;
        }

        // --- Getter / Setter ---
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSuggestedQuery() { return suggestedQuery; }
        public void setSuggestedQuery(String suggestedQuery) { this.suggestedQuery = suggestedQuery; }

        public double getExpectedPerformanceImprovement() { return expectedPerformanceImprovement; }
        public void setExpectedPerformanceImprovement(double expectedPerformanceImprovement) {
            this.expectedPerformanceImprovement = expectedPerformanceImprovement;
        }
    }

    // ---------------------------------------------------------
    // 내부 클래스 4: 시간별 메트릭 데이터 (차트 시각화용)
    public static class TimeSeriesMetrics {
        private List<TimePoint> timePoints;
        private double totalExecutionTime;
        private String timeUnit; // "ms" 또는 "seconds"
        
        public TimeSeriesMetrics() {}
        
        public List<TimePoint> getTimePoints() { return timePoints; }
        public void setTimePoints(List<TimePoint> timePoints) { this.timePoints = timePoints; }
        
        public double getTotalExecutionTime() { return totalExecutionTime; }
        public void setTotalExecutionTime(double totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }
        
        public String getTimeUnit() { return timeUnit; }
        public void setTimeUnit(String timeUnit) { this.timeUnit = timeUnit; }
    }

    // ---------------------------------------------------------
    // 내부 클래스 5: 시간별 데이터 포인트
    public static class TimePoint {
        private double timestamp;           // 시간 (시작부터의 경과 시간)
        private double cpuUsage;           // CPU 사용률 (0-100)
        private double ioWaitTime;         // I/O 대기 시간
        private long memoryUsage;          // 메모리 사용량 (bytes)
        private long diskReads;            // 디스크 읽기 수
        private long diskWrites;           // 디스크 쓰기 수
        private String operationType;      // 실행 중인 작업 타입 (Seq Scan, Index Scan 등)
        private String nodeName;           // 실행계획 노드명
        
        public TimePoint() {}
        
        public TimePoint(double timestamp, double cpuUsage, double ioWaitTime) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.ioWaitTime = ioWaitTime;
        }
        
        // --- Getter / Setter ---
        public double getTimestamp() { return timestamp; }
        public void setTimestamp(double timestamp) { this.timestamp = timestamp; }
        
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public double getIoWaitTime() { return ioWaitTime; }
        public void setIoWaitTime(double ioWaitTime) { this.ioWaitTime = ioWaitTime; }
        
        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public long getDiskReads() { return diskReads; }
        public void setDiskReads(long diskReads) { this.diskReads = diskReads; }
        
        public long getDiskWrites() { return diskWrites; }
        public void setDiskWrites(long diskWrites) { this.diskWrites = diskWrites; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    }

    // ---------------------------------------------------------
    // 내부 클래스 6: 병목 지점 정보
    public static class BottleneckPoint {
        private double timestamp;           // 병목이 발생한 시점
        private String bottleneckType;      // 병목 타입 (CPU, IO, MEMORY, NETWORK)
        private double severity;            // 심각도 (0-100)
        private String description;         // 병목 설명
        private String affectedOperation;   // 영향받은 작업
        private String recommendation;      // 개선 권장사항
        private double durationMs;          // 병목 지속 시간
        
        public BottleneckPoint() {}
        
        public BottleneckPoint(double timestamp, String bottleneckType, double severity, String description) {
            this.timestamp = timestamp;
            this.bottleneckType = bottleneckType;
            this.severity = severity;
            this.description = description;
        }
        
        // --- Getter / Setter ---
        public double getTimestamp() { return timestamp; }
        public void setTimestamp(double timestamp) { this.timestamp = timestamp; }
        
        public String getBottleneckType() { return bottleneckType; }
        public void setBottleneckType(String bottleneckType) { this.bottleneckType = bottleneckType; }
        
        public double getSeverity() { return severity; }
        public void setSeverity(double severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getAffectedOperation() { return affectedOperation; }
        public void setAffectedOperation(String affectedOperation) { this.affectedOperation = affectedOperation; }
        
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        
        public double getDurationMs() { return durationMs; }
        public void setDurationMs(double durationMs) { this.durationMs = durationMs; }
    }

    // ---------------------------------------------------------
    // 내부 클래스 7: AI 기반 병목 분석 결과
    public static class AIBottleneckAnalysis {
        private String bottleneckType;           // 병목 유형 (예: "전체 테이블 스캔", "비효율적인 조인")
        private String severityLevel;            // 심각도 (심각, 높음, 보통, 낮음)
        private String detailedDescription;      // 상세 설명
        private String recommendation;           // 권장사항
        private List<String> sqlSuggestions;     // 구체적인 SQL 개선 제안
        private double expectedImprovement;      // 예상 성능 개선율 (%)
        private double impactScore;              // 영향도 점수 (0-100)
        private double confidenceLevel;          // AI 분석 신뢰도 (0-100)
        private String affectedTables;           // 영향받는 테이블들
        private String affectedColumns;          // 영향받는 컬럼들
        
        public AIBottleneckAnalysis() {}
        
        // --- Getter / Setter ---
        public String getBottleneckType() { return bottleneckType; }
        public void setBottleneckType(String bottleneckType) { this.bottleneckType = bottleneckType; }
        
        public String getSeverityLevel() { return severityLevel; }
        public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }
        
        public String getDetailedDescription() { return detailedDescription; }
        public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
        
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        
        public List<String> getSqlSuggestions() { return sqlSuggestions; }
        public void setSqlSuggestions(List<String> sqlSuggestions) { this.sqlSuggestions = sqlSuggestions; }
        
        public double getExpectedImprovement() { return expectedImprovement; }
        public void setExpectedImprovement(double expectedImprovement) { this.expectedImprovement = expectedImprovement; }
        
        public double getImpactScore() { return impactScore; }
        public void setImpactScore(double impactScore) { this.impactScore = impactScore; }
        
        public double getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
        
        public String getAffectedTables() { return affectedTables; }
        public void setAffectedTables(String affectedTables) { this.affectedTables = affectedTables; }
        
        public String getAffectedColumns() { return affectedColumns; }
        public void setAffectedColumns(String affectedColumns) { this.affectedColumns = affectedColumns; }
    }
}
