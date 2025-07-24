package com.simulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.model.QueryAnalysisRequest;
import com.simulator.model.QueryAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class QueryAnalysisService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceAnalysisService resourceAnalysisService;

    @Autowired
    private OptimizationService optimizationService;

    @Autowired
    private AIBottleneckAnalysisService aiBottleneckAnalysisService;

    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 쿼리 분석 메인 로직
    public QueryAnalysisResult analyzeQuery(QueryAnalysisRequest request) {
        try {
            // 1. 실행계획 생성: ANALYZE + BUFFERS + FORMAT JSON
            String explainQuery = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + request.getSqlQuery();

            // 2. 실행계획 실행 → 결과를 Map 리스트로 받음
            List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(explainQuery);

            // 3. "QUERY PLAN" 필드 추출 → PGobject 처리 포함
            Object queryPlanObj = explainResult.get(0).get("QUERY PLAN");

            String jsonPlan;
            if (queryPlanObj != null && queryPlanObj.getClass().getName().equals("org.postgresql.util.PGobject")) {
                try {
                    jsonPlan = (String) queryPlanObj.getClass().getMethod("getValue").invoke(queryPlanObj);
                } catch (Exception e) {
                    jsonPlan = queryPlanObj.toString();
                }
            } else {
                jsonPlan = queryPlanObj.toString();
            }

            // 4. JSON 문자열을 Jackson으로 파싱
            JsonNode planNode = objectMapper.readTree(jsonPlan);
            JsonNode plan = planNode.get(0).get("Plan");  // 핵심 Plan 노드만 추출
            
            // 디버그: 실행 계획 JSON 출력
            System.out.println("=== 실행 계획 JSON ===");
            System.out.println(jsonPlan);

            // 5. 리소스 사용량 추출 (cost, blocks, time 등) → 재귀 방식
            QueryAnalysisResult.ResourceUsage resourceUsage = extractResourceUsage(plan);
            
            // 디버그: 리소스 사용량 로그 출력
            System.out.println("=== 리소스 사용량 디버그 ===");
            System.out.println("버퍼 히트: " + resourceUsage.getSharedBlksHit());
            System.out.println("디스크 읽기: " + resourceUsage.getSharedBlksRead());
            System.out.println("디스크 더티: " + resourceUsage.getSharedBlksDirtied());
            System.out.println("디스크 쓰기: " + resourceUsage.getSharedBlksWritten());
            System.out.println("실행 시간: " + resourceUsage.getActualTime());

            // 6. 시간별 메트릭 데이터 추출 (새로운 기능)
            QueryAnalysisResult.TimeSeriesMetrics timeSeriesMetrics = extractTimeSeriesMetrics(plan);

            // 7. 사용량 기반 성능 점수 계산
            QueryAnalysisResult.ResourceMetrics resourceMetrics =
                resourceAnalysisService.analyzeResourceUsage(resourceUsage);

            // 8. 병목 지점 감지 (새로운 기능)
            List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints =
                resourceAnalysisService.detectBottlenecks(timeSeriesMetrics, resourceUsage);

            // 9. 실행계획 기반 최적화 제안 생성 (인덱스, JOIN 등)
            List<QueryAnalysisResult.OptimizationSuggestion> suggestions =
                optimizationService.generateSuggestions(request.getSqlQuery(), plan);

            // 10. AI 기반 병목 분석 (OpenAI API 사용 - 그래프 메트릭 포함)
            QueryAnalysisResult.AIBottleneckAnalysis aiAnalysis =
                aiBottleneckAnalysisService.analyzeBottlenecksWithMetrics(
                    request.getSqlQuery(),
                    jsonPlan,
                    (long) resourceUsage.getActualTime(),
                    String.format("실행시간: %.2fms, 버퍼히트: %d, 디스크읽기: %d, 계획비용: %d", 
                        resourceUsage.getActualTime(),
                        resourceUsage.getSharedBlksHit(),
                        resourceUsage.getSharedBlksRead(),
                        resourceUsage.getTotalCost()),
                    bottleneckPoints,
                    timeSeriesMetrics
                );
            
            List<QueryAnalysisResult.AIBottleneckAnalysis> aiAnalyses = List.of(aiAnalysis);

            // 11. AI 최적화 SQL 생성
            String optimizedSQL = null;
            try {
                System.out.println("=== AI 최적화 SQL 생성 시도 ===");
                optimizedSQL = openAIAnalysisService.generateOptimizedSQL(
                    request.getSqlQuery(), 
                    jsonPlan, 
                    bottleneckPoints
                );
                System.out.println("AI 최적화 SQL 생성 성공, 길이: " + (optimizedSQL != null ? optimizedSQL.length() : "null"));
                if (optimizedSQL != null && !optimizedSQL.trim().isEmpty()) {
                    System.out.println("생성된 최적화 SQL 미리보기: " + optimizedSQL.substring(0, Math.min(100, optimizedSQL.length())) + "...");
                }
            } catch (Exception e) {
                System.err.println("AI 최적화 SQL 생성 실패: " + e.getMessage());
                e.printStackTrace();
                
                // 폴백: 기본 최적화 제안
                optimizedSQL = "-- AI 최적화 SQL 생성에 실패했습니다.\n-- 다음은 기본 최적화 제안입니다:\n\n" + 
                               request.getSqlQuery() + "\n\n-- 권장사항:\n-- 1. 적절한 인덱스 생성\n-- 2. WHERE 조건 최적화\n-- 3. JOIN 순서 조정";
            }

            // 12. 최종 분석 결과 객체 구성
            QueryAnalysisResult result = new QueryAnalysisResult();
            result.setOriginalQuery(request.getSqlQuery());
            result.setExecutionPlan(jsonPlan);         // 원본 JSON 문자열도 포함
            result.setResourceUsage(resourceUsage);     // 리소스 요약
            result.setResourceMetrics(resourceMetrics); // 점수화된 메트릭
            result.setTimeSeriesMetrics(timeSeriesMetrics); // 시간별 데이터
            result.setBottleneckPoints(bottleneckPoints);   // 병목 지점들
            result.setOptimizationSuggestions(suggestions); // 최적화 제안 목록
            result.setAiBottleneckAnalyses(aiAnalyses);     // AI 병목 분석 결과
            result.setOptimizedQuery(optimizedSQL);         // AI 최적화 SQL

            return result;

        } catch (Exception e) {
            throw new RuntimeException("쿼리 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 실행계획 JSON 내 리소스 사용량 정보 추출 (재귀적으로 합산)
    private QueryAnalysisResult.ResourceUsage extractResourceUsage(JsonNode plan) {
        QueryAnalysisResult.ResourceUsage usage = new QueryAnalysisResult.ResourceUsage();

        // 기본 메트릭: 비용, 예상 row 수, row 크기
        usage.setTotalCost(plan.get("Total Cost").asLong());
        usage.setPlanRows(plan.get("Plan Rows").asLong());
        usage.setPlanWidth(plan.get("Plan Width").asLong());

        // 실제 실행 시간
        if (plan.has("Actual Total Time")) {
            usage.setActualTime(plan.get("Actual Total Time").asDouble());
        }

        // 버퍼 읽기 관련 메트릭 - Buffers 객체 내부에 있는 경우
        if (plan.has("Buffers")) {
            JsonNode buffers = plan.get("Buffers");
            if (buffers.has("Shared Hit")) {
                usage.setSharedBlksHit(buffers.get("Shared Hit").asLong());
            }
            if (buffers.has("Shared Read")) {
                usage.setSharedBlksRead(buffers.get("Shared Read").asLong());
            }
            if (buffers.has("Shared Dirtied")) {
                usage.setSharedBlksDirtied(buffers.get("Shared Dirtied").asLong());
            }
            if (buffers.has("Shared Written")) {
                usage.setSharedBlksWritten(buffers.get("Shared Written").asLong());
            }
        }
        // 버퍼 읽기 관련 메트릭 - 직접 필드에 있는 경우 (fallback)
        if (plan.has("Shared Hit Blocks")) {
            usage.setSharedBlksHit(plan.get("Shared Hit Blocks").asLong());
        }
        if (plan.has("Shared Read Blocks")) {
            usage.setSharedBlksRead(plan.get("Shared Read Blocks").asLong());
        }
        if (plan.has("Shared Dirtied Blocks")) {
            usage.setSharedBlksDirtied(plan.get("Shared Dirtied Blocks").asLong());
        }
        if (plan.has("Shared Written Blocks")) {
            usage.setSharedBlksWritten(plan.get("Shared Written Blocks").asLong());
        }

        // 하위 Plan이 있을 경우 재귀 호출하여 사용량 누적
        if (plan.has("Plans")) {
            JsonNode plans = plan.get("Plans");
            for (JsonNode subPlan : plans) {
                QueryAnalysisResult.ResourceUsage subUsage = extractResourceUsage(subPlan);
                usage.setSharedBlksHit(usage.getSharedBlksHit() + subUsage.getSharedBlksHit());
                usage.setSharedBlksRead(usage.getSharedBlksRead() + subUsage.getSharedBlksRead());
                usage.setSharedBlksDirtied(usage.getSharedBlksDirtied() + subUsage.getSharedBlksDirtied());
                usage.setSharedBlksWritten(usage.getSharedBlksWritten() + subUsage.getSharedBlksWritten());
                // 실행 시간은 최대값 사용 (누적하지 않음)
                if (subUsage.getActualTime() > usage.getActualTime()) {
                    usage.setActualTime(subUsage.getActualTime());
                }
            }
        }

        return usage;
    }

    // 실행계획에서 시간별 메트릭 데이터 추출
    private QueryAnalysisResult.TimeSeriesMetrics extractTimeSeriesMetrics(JsonNode plan) {
        QueryAnalysisResult.TimeSeriesMetrics metrics = new QueryAnalysisResult.TimeSeriesMetrics();
        List<QueryAnalysisResult.TimePoint> timePoints = new ArrayList<>();
        
        // 전체 실행 시간 추출
        double totalTime = plan.has("Actual Total Time") ? plan.get("Actual Total Time").asDouble() : 0.0;
        metrics.setTotalExecutionTime(Math.max(totalTime, 0.1)); // 최소값 보장
        metrics.setTimeUnit("ms");
        
        // 실행계획 노드들을 시간순으로 분석하여 TimePoint 생성
        extractTimePointsFromPlan(plan, timePoints, 0.0, totalTime);
        
        // 시간 포인트들을 더 잘 분산시키기
        redistributeTimePoints(timePoints, totalTime);
        
        // 타임스탬프 기준으로 정렬
        timePoints.sort((a, b) -> Double.compare(a.getTimestamp(), b.getTimestamp()));
        
        metrics.setTimePoints(timePoints);
        return metrics;
    }
    
    // 시간 포인트들을 더 균등하게 분산
    private void redistributeTimePoints(List<QueryAnalysisResult.TimePoint> timePoints, double totalTime) {
        if (timePoints.isEmpty()) return;
        
        // 최소 시간 간격 설정 (총 시간의 5% 또는 최소 0.01ms)
        double minInterval = Math.max(totalTime * 0.05, 0.01);
        
        for (int i = 0; i < timePoints.size(); i++) {
            QueryAnalysisResult.TimePoint point = timePoints.get(i);
            
            // 기본 시간 분산: 각 노드를 전체 시간에 걸쳐 균등 분배
            double baseTime = (totalTime / Math.max(timePoints.size(), 1)) * i;
            
            // 원래 타임스탬프와 기본 시간의 가중평균 (70% 기본시간, 30% 원래시간)
            double redistributedTime = baseTime * 0.7 + point.getTimestamp() * 0.3;
            
            // 최소 간격 보장
            if (i > 0) {
                double prevTime = timePoints.get(i - 1).getTimestamp();
                redistributedTime = Math.max(redistributedTime, prevTime + minInterval);
            }
            
            // 최대 시간 제한
            redistributedTime = Math.min(redistributedTime, totalTime);
            
            point.setTimestamp(redistributedTime);
        }
    }
    
    // 실행계획 노드별로 TimePoint 생성 (재귀적 분석)
    private void extractTimePointsFromPlan(JsonNode plan, List<QueryAnalysisResult.TimePoint> timePoints, 
                                           double startTime, double totalTime) {
        if (plan == null) return;
        
        String nodeType = plan.has("Node Type") ? plan.get("Node Type").asText() : "Unknown";
        double actualTotalTime = plan.has("Actual Total Time") ? plan.get("Actual Total Time").asDouble() : 0.0;
        double actualStartupTime = plan.has("Actual Startup Time") ? plan.get("Actual Startup Time").asDouble() : 0.0;
        
        // 시간 값 검증 및 보정
        actualTotalTime = Math.max(0.0, actualTotalTime);
        actualStartupTime = Math.max(0.0, actualStartupTime);
        startTime = Math.max(0.0, startTime);
        
        // 실제 실행 시간 = 총 시간 - 시작 시간 (음수 방지)
        double actualExecutionTime = Math.max(0.0, actualTotalTime - actualStartupTime);
        
        // 현재 노드의 TimePoint 생성
        QueryAnalysisResult.TimePoint timePoint = new QueryAnalysisResult.TimePoint();
        timePoint.setTimestamp(startTime);
        timePoint.setOperationType(nodeType);
        timePoint.setNodeName(generateNodeName(plan));
        
        // CPU 사용률 계산 (전체 실행 시간 대비 현재 노드의 실행 시간 비율)
        double cpuUsage = totalTime > 0 ? Math.min(100.0, (actualExecutionTime / totalTime) * 100.0) : 0.0;
        
        // 노드 타입별 CPU 사용률 가중치 적용
        cpuUsage = applyCpuWeightByNodeType(cpuUsage, nodeType);
        
        timePoint.setCpuUsage(cpuUsage);
        
        // I/O 대기 시간 계산 (더 정확한 계산)
        double ioWaitTime = calculateAdvancedIoWaitTime(plan, actualExecutionTime);
        timePoint.setIoWaitTime(ioWaitTime);
        
        // 메모리 및 디스크 사용량 추출
        extractResourceMetrics(plan, timePoint);
        
        timePoints.add(timePoint);
        
        // 하위 노드들 처리 (병렬 실행 고려)
        if (plan.has("Plans")) {
            JsonNode subPlans = plan.get("Plans");
            
            // 조인 타입에 따른 시간 분배
            if (isParallelExecution(nodeType)) {
                // 병렬 실행의 경우 동시 시작
                for (JsonNode subPlan : subPlans) {
                    extractTimePointsFromPlan(subPlan, timePoints, startTime, totalTime);
                }
            } else {
                // 순차 실행의 경우 시간 누적
            double currentOffset = startTime;
            for (JsonNode subPlan : subPlans) {
                    double subPlanTotalTime = subPlan.has("Actual Total Time") ? 
                        subPlan.get("Actual Total Time").asDouble() : 0.0;
                    double subPlanStartupTime = subPlan.has("Actual Startup Time") ? 
                        subPlan.get("Actual Startup Time").asDouble() : 0.0;
                    
                    // 하위 노드 시간 값 검증 및 보정
                    subPlanTotalTime = Math.max(0.0, subPlanTotalTime);
                    subPlanStartupTime = Math.max(0.0, subPlanStartupTime);
                    
                extractTimePointsFromPlan(subPlan, timePoints, currentOffset, totalTime);
                    
                    // 실행 시간 누적 (음수 방지)
                    double subPlanExecutionTime = Math.max(0.0, subPlanTotalTime - subPlanStartupTime);
                    currentOffset = Math.max(0.0, currentOffset + subPlanExecutionTime);
                }
            }
        }
    }
    
    // 노드명 생성 (테이블명 포함)
    private String generateNodeName(JsonNode plan) {
        String nodeType = plan.has("Node Type") ? plan.get("Node Type").asText() : "Unknown";
        String relationName = plan.has("Relation Name") ? plan.get("Relation Name").asText() : "";
        String indexName = plan.has("Index Name") ? plan.get("Index Name").asText() : "";
        
        StringBuilder name = new StringBuilder(nodeType);
        if (!relationName.isEmpty()) {
            name.append(" on ").append(relationName);
        }
        if (!indexName.isEmpty()) {
            name.append(" using ").append(indexName);
        }
        
        return name.toString();
    }
    
    // 노드 타입별 CPU 사용률 가중치 적용
    private double applyCpuWeightByNodeType(double baseCpuUsage, String nodeType) {
        double weight = 1.0;
        
        switch (nodeType) {
            case "Seq Scan":
                weight = 1.5; // Sequential Scan은 CPU 집약적
                break;
            case "Index Scan":
            case "Index Only Scan":
                weight = 0.8; // Index 사용시 CPU 효율적
                break;
            case "Nested Loop":
                weight = 2.0; // Nested Loop는 매우 CPU 집약적
                break;
            case "Hash Join":
                weight = 1.3; // Hash Join은 CPU 사용량 중간
                break;
            case "Merge Join":
                weight = 1.1; // Merge Join은 상대적으로 효율적
                break;
            case "Sort":
                weight = 1.8; // Sort는 CPU 집약적
                break;
            case "Hash":
                weight = 1.4; // Hash 테이블 생성은 CPU 사용
                break;
            case "Aggregate":
                weight = 1.2; // 집계 함수는 CPU 사용
                break;
            default:
                weight = 1.0;
        }
        
        return Math.min(100.0, baseCpuUsage * weight);
    }
    
    // 고급 I/O 대기 시간 계산
    private double calculateAdvancedIoWaitTime(JsonNode plan, double executionTime) {
        long sharedReads = 0;
        long sharedWrites = 0;
        long sharedHits = 0;
        
        // Buffers 정보에서 디스크 I/O 추출
        if (plan.has("Buffers")) {
            JsonNode buffers = plan.get("Buffers");
            sharedReads = buffers.has("Shared Read") ? buffers.get("Shared Read").asLong() : 0;
            sharedWrites = buffers.has("Shared Written") ? buffers.get("Shared Written").asLong() : 0;
            sharedHits = buffers.has("Shared Hit") ? buffers.get("Shared Hit").asLong() : 0;
        }
        
        // I/O 대기 시간 계산
        double ioWaitTime = 0.0;
        
        // 실제 디스크 I/O가 있는 경우
        if (sharedReads > 0 || sharedWrites > 0) {
            // 디스크 I/O 시간 = (읽기 * 0.1ms) + (쓰기 * 0.2ms)
            ioWaitTime = (sharedReads * 0.1) + (sharedWrites * 0.2);
        }
        
        // 캐시 히트가 많은 경우 I/O 대기 시간 감소
        if (sharedHits > 0) {
            double hitRatio = (double) sharedHits / (sharedHits + sharedReads);
            ioWaitTime = ioWaitTime * (1.0 - hitRatio);
        }
        
        // 실행 시간 대비 I/O 대기 시간 비율 제한
        return Math.min(ioWaitTime, executionTime * 0.8);
    }
    
    // 병렬 실행 여부 판단
    private boolean isParallelExecution(String nodeType) {
        return nodeType.equals("Hash Join") || 
               nodeType.equals("Merge Join") || 
               nodeType.equals("Bitmap Heap Scan") ||
               nodeType.startsWith("Parallel");
    }
    
    // 리소스 메트릭 추출
    private void extractResourceMetrics(JsonNode plan, QueryAnalysisResult.TimePoint timePoint) {
        // 메모리 사용량 추정 (Plan Rows * Plan Width)
        long planRows = plan.has("Plan Rows") ? plan.get("Plan Rows").asLong() : 0;
        long planWidth = plan.has("Plan Width") ? plan.get("Plan Width").asLong() : 0;
        timePoint.setMemoryUsage(planRows * planWidth);
        
        // 디스크 읽기/쓰기 수 추출
        if (plan.has("Buffers")) {
            JsonNode buffers = plan.get("Buffers");
            timePoint.setDiskReads(buffers.has("Shared Read") ? buffers.get("Shared Read").asLong() : 0);
            timePoint.setDiskWrites(buffers.has("Shared Written") ? buffers.get("Shared Written").asLong() : 0);
        }
    }
}
