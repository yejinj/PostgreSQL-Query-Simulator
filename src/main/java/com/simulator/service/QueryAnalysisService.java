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

@Service
public class QueryAnalysisService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ResourceAnalysisService resourceAnalysisService;
    
    @Autowired
    private OptimizationService optimizationService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public QueryAnalysisResult analyzeQuery(QueryAnalysisRequest request) {
        try {
            // 1. EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) 실행
            String explainQuery = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + request.getSqlQuery();
            
            List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(explainQuery);
            Object queryPlanObj = explainResult.get(0).get("QUERY PLAN");
            
            // PostgreSQL JSON 결과를 올바르게 처리
            String jsonPlan;
            if (queryPlanObj != null && queryPlanObj.getClass().getName().equals("org.postgresql.util.PGobject")) {
                try {
                    // 리플렉션을 사용하여 getValue() 메소드 호출
                    jsonPlan = (String) queryPlanObj.getClass().getMethod("getValue").invoke(queryPlanObj);
                } catch (Exception e) {
                    jsonPlan = queryPlanObj.toString();
                }
            } else {
                jsonPlan = queryPlanObj.toString();
            }
            
            // 2. JSON 파싱
            JsonNode planNode = objectMapper.readTree(jsonPlan);
            JsonNode plan = planNode.get(0).get("Plan");
            
            // 3. 실행 계획에서 리소스 사용량 추출
            QueryAnalysisResult.ResourceUsage resourceUsage = extractResourceUsage(plan);
            
            // 4. 리소스 분석
            QueryAnalysisResult.ResourceMetrics resourceMetrics = 
                resourceAnalysisService.analyzeResourceUsage(resourceUsage);
            
            // 5. 최적화 제안 생성
            List<QueryAnalysisResult.OptimizationSuggestion> suggestions = 
                optimizationService.generateSuggestions(request.getSqlQuery(), plan);
            
            // 6. 결과 생성
            QueryAnalysisResult result = new QueryAnalysisResult();
            result.setOriginalQuery(request.getSqlQuery());
            result.setExecutionPlan(jsonPlan);
            result.setResourceUsage(resourceUsage);
            result.setResourceMetrics(resourceMetrics);
            result.setOptimizationSuggestions(suggestions);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("쿼리 분석 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    private QueryAnalysisResult.ResourceUsage extractResourceUsage(JsonNode plan) {
        QueryAnalysisResult.ResourceUsage usage = new QueryAnalysisResult.ResourceUsage();
        
        // 기본 계획 정보
        usage.setTotalCost(plan.get("Total Cost").asLong());
        usage.setPlanRows(plan.get("Plan Rows").asLong());
        usage.setPlanWidth(plan.get("Plan Width").asLong());
        
        // 실제 실행 시간
        if (plan.has("Actual Total Time")) {
            usage.setActualTime(plan.get("Actual Total Time").asDouble());
        }
        
        // 버퍼 사용량
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
        
        // 하위 계획들도 재귀적으로 처리
        if (plan.has("Plans")) {
            JsonNode plans = plan.get("Plans");
            for (JsonNode subPlan : plans) {
                QueryAnalysisResult.ResourceUsage subUsage = extractResourceUsage(subPlan);
                // 하위 계획의 리소스 사용량을 누적
                usage.setSharedBlksHit(usage.getSharedBlksHit() + subUsage.getSharedBlksHit());
                usage.setSharedBlksRead(usage.getSharedBlksRead() + subUsage.getSharedBlksRead());
                usage.setSharedBlksDirtied(usage.getSharedBlksDirtied() + subUsage.getSharedBlksDirtied());
                usage.setSharedBlksWritten(usage.getSharedBlksWritten() + subUsage.getSharedBlksWritten());
            }
        }
        
        return usage;
    }
} 