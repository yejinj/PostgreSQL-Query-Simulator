package com.simulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.simulator.model.QueryAnalysisResult;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OptimizationService {
    
    public List<QueryAnalysisResult.OptimizationSuggestion> generateSuggestions(
            String originalQuery, JsonNode executionPlan) {
        
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // 1. 인덱스 관련 제안
        suggestions.addAll(analyzeIndexUsage(originalQuery, executionPlan));
        
        // 2. 조인 관련 제안
        suggestions.addAll(analyzeJoinPerformance(originalQuery, executionPlan));
        
        // 3. WHERE 절 최적화 제안
        suggestions.addAll(analyzeWhereClause(originalQuery, executionPlan));
        
        // 4. SELECT 절 최적화 제안
        suggestions.addAll(analyzeSelectClause(originalQuery, executionPlan));
        
        // 5. 기타 일반적인 최적화 제안
        suggestions.addAll(analyzeGeneralOptimizations(originalQuery, executionPlan));
        
        return suggestions;
    }
    
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeIndexUsage(
            String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // Seq Scan 감지
        if (containsSeqScan(plan)) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "INDEX", 
                    "Sequential Scan이 발견되었습니다. WHERE 절의 컬럼에 인덱스를 추가하면 성능이 향상될 수 있습니다."
                );
            suggestion.setExpectedPerformanceImprovement(30.00);
            suggestions.add(suggestion);
        }
        
        // 비효율적인 인덱스 스캔 감지
        if (containsIneffectiveIndexScan(plan)) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "INDEX", 
                    "인덱스가 비효율적으로 사용되고 있습니다. 복합 인덱스나 커버링 인덱스를 고려해보세요."
                );
            suggestion.setExpectedPerformanceImprovement(20.00);
            suggestions.add(suggestion);
        }
        
        return suggestions;
    }
    
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeJoinPerformance(
            String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // Nested Loop Join 감지
        if (containsNestedLoopJoin(plan)) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "JOIN", 
                    "Nested Loop Join이 사용되고 있습니다. 조인 키에 인덱스를 추가하거나 Hash Join을 유도해보세요."
                );
            suggestion.setExpectedPerformanceImprovement(40.00);
            suggestions.add(suggestion);
        }
        
        // 카티시안 곱 감지
        if (query.toUpperCase().contains("JOIN") && !query.toUpperCase().contains("ON")) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "JOIN", 
                    "명시적인 JOIN 조건이 없어 카티시안 곱이 발생할 수 있습니다. ON 절을 추가하세요."
                );
            suggestion.setExpectedPerformanceImprovement(80.00);
            suggestions.add(suggestion);
        }
        
        return suggestions;
    }
    
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeWhereClause(
            String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // LIKE '%...%' 패턴 감지
        if (Pattern.compile("LIKE\\s+['\"]%.*%['\"]", Pattern.CASE_INSENSITIVE).matcher(query).find()) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "WHERE", 
                    "양쪽 와일드카드를 사용한 LIKE 패턴이 발견되었습니다. 풀텍스트 검색이나 다른 검색 방식을 고려해보세요."
                );
            suggestion.setExpectedPerformanceImprovement(25.00);
            suggestions.add(suggestion);
        }
        
        // 함수 적용된 컬럼 조건 감지
        if (Pattern.compile("WHERE\\s+\\w+\\s*\\(.*\\)\\s*[=<>]", Pattern.CASE_INSENSITIVE).matcher(query).find()) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "WHERE", 
                    "WHERE 절에서 컬럼에 함수가 적용되어 있습니다. 인덱스 사용이 불가능할 수 있으니 조건을 다시 작성해보세요."
                );
            suggestion.setExpectedPerformanceImprovement(35.00);
            suggestions.add(suggestion);
        }
        
        return suggestions;
    }
    
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeSelectClause(
            String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // SELECT * 사용 감지
        if (Pattern.compile("SELECT\\s+\\*", Pattern.CASE_INSENSITIVE).matcher(query).find()) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "SELECT", 
                    "SELECT * 사용이 감지되었습니다. 필요한 컬럼만 선택하여 네트워크 트래픽과 메모리 사용량을 줄이세요."
                );
            suggestion.setExpectedPerformanceImprovement(15.00);
            suggestions.add(suggestion);
        }
        
        return suggestions;
    }
    
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeGeneralOptimizations(
            String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // LIMIT 절 없는 대용량 결과 감지
        if (!query.toUpperCase().contains("LIMIT") && plan.get("Plan Rows").asLong() > 1000) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "GENERAL", 
                    "대용량 결과셋이 예상됩니다. LIMIT 절을 추가하여 페이징을 구현하는 것을 고려해보세요."
                );
            suggestion.setExpectedPerformanceImprovement(50.00);
            suggestions.add(suggestion);
        }
        
        // 서브쿼리 최적화 제안
        if (query.toUpperCase().contains("IN (SELECT")) {
            QueryAnalysisResult.OptimizationSuggestion suggestion = 
                new QueryAnalysisResult.OptimizationSuggestion(
                    "GENERAL", 
                    "IN 서브쿼리가 발견되었습니다. EXISTS나 JOIN으로 변경하면 성능이 향상될 수 있습니다."
                );
            suggestion.setExpectedPerformanceImprovement(30.00);
            suggestions.add(suggestion);
        }
        
        return suggestions;
    }
    
    // 헬퍼 메서드들
    private boolean containsSeqScan(JsonNode plan) {
        return searchNodeType(plan, "Seq Scan");
    }
    
    private boolean containsIneffectiveIndexScan(JsonNode plan) {
        // 인덱스 스캔이지만 비용이 높은 경우 감지 로직
        return searchNodeType(plan, "Index Scan") && plan.get("Total Cost").asLong() > 1000;
    }
    
    private boolean containsNestedLoopJoin(JsonNode plan) {
        return searchNodeType(plan, "Nested Loop");
    }
    
    private boolean searchNodeType(JsonNode plan, String nodeType) {
        if (plan.has("Node Type") && plan.get("Node Type").asText().equals(nodeType)) {
            return true;
        }
        
        if (plan.has("Plans")) {
            for (JsonNode subPlan : plan.get("Plans")) {
                if (searchNodeType(subPlan, nodeType)) {
                    return true;
                }
            }
        }
        
        return false;
    }
} 