package com.simulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.simulator.model.QueryAnalysisResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service // Spring이 관리하는 서비스 클래스
public class OptimizationService {

    // 성능 개선 예상치 상수
    private static final double INDEX_IMPROVEMENT = 30.0;
    private static final double INDEX_TUNING_IMPROVEMENT = 20.0;
    private static final double JOIN_IMPROVEMENT = 40.0;
    private static final double CARTESIAN_PRODUCT_IMPROVEMENT = 80.0;
    private static final double LIKE_IMPROVEMENT = 25.0;
    private static final double FUNCTION_IN_WHERE_IMPROVEMENT = 35.0;
    private static final double SELECT_STAR_IMPROVEMENT = 15.0;
    private static final double PAGINATION_IMPROVEMENT = 50.0;
    private static final double SUBQUERY_IMPROVEMENT = 30.0;
    
    // 임계값 상수
    private static final long HIGH_COST_THRESHOLD = 1000L;
    private static final long LARGE_RESULT_SET_THRESHOLD = 1000L;
    
    // 정규표현식 패턴 (성능상 미리 컴파일)
    private static final Pattern LIKE_WILDCARD_PATTERN = Pattern.compile("LIKE\\s+['\"]%.*%['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUNCTION_IN_WHERE_PATTERN = Pattern.compile("WHERE\\s+\\w+\\s*\\(.*\\)\\s*[=<>]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT_STAR_PATTERN = Pattern.compile("SELECT\\s+\\*", Pattern.CASE_INSENSITIVE);

    // 🔍 쿼리와 실행계획을 기반으로 모든 최적화 제안 생성
    public List<QueryAnalysisResult.OptimizationSuggestion> generateSuggestions(
            String originalQuery, JsonNode executionPlan) {

        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();

        suggestions.addAll(analyzeIndexUsage(originalQuery, executionPlan));        // 인덱스 분석
        suggestions.addAll(analyzeJoinPerformance(originalQuery, executionPlan));   // 조인 분석
        suggestions.addAll(analyzeWhereClause(originalQuery, executionPlan));       // WHERE 절 분석
        suggestions.addAll(analyzeSelectClause(originalQuery, executionPlan));      // SELECT 절 분석
        suggestions.addAll(analyzeGeneralOptimizations(originalQuery, executionPlan)); // 기타 분석

        return suggestions;
    }

    // 📌 1. 인덱스 관련 분석
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeIndexUsage(String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();

        // Seq Scan 감지 → 인덱스 추천
        if (containsSeqScan(plan)) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "INDEX",
                "Sequential Scan이 발견되었습니다. WHERE 절의 컬럼에 인덱스를 추가하면 성능이 향상될 수 있습니다."
            );
            suggestion.setExpectedPerformanceImprovement(INDEX_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        // Index Scan인데 비용이 비정상적으로 높은 경우
        if (containsIneffectiveIndexScan(plan)) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "INDEX",
                "인덱스가 비효율적으로 사용되고 있습니다. 복합 인덱스나 커버링 인덱스를 고려해보세요."
            );
            suggestion.setExpectedPerformanceImprovement(INDEX_TUNING_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    // 📌 2. 조인 관련 분석
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeJoinPerformance(String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();

        // Nested Loop Join → 인덱스 추가나 다른 조인 방식 권장
        if (containsNestedLoopJoin(plan)) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "JOIN",
                "Nested Loop Join이 사용되고 있습니다. 조인 키에 인덱스를 추가하거나 Hash Join을 유도해보세요."
            );
            suggestion.setExpectedPerformanceImprovement(JOIN_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        // JOIN인데 ON 절 누락 → 카티시안 곱 가능성
        if (query.toUpperCase().contains("JOIN") && !query.toUpperCase().contains("ON")) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "JOIN",
                "명시적인 JOIN 조건이 없어 카티시안 곱이 발생할 수 있습니다. ON 절을 추가하세요."
            );
            suggestion.setExpectedPerformanceImprovement(CARTESIAN_PRODUCT_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    // 📌 3. WHERE 절 분석
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeWhereClause(String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();

        // LIKE '%abc%' → 인덱스 못 씀
        if (LIKE_WILDCARD_PATTERN.matcher(query).find()) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "WHERE",
                "양쪽 와일드카드를 사용한 LIKE 패턴이 발견되었습니다. 풀텍스트 검색이나 다른 검색 방식을 고려해보세요."
            );
            suggestion.setExpectedPerformanceImprovement(LIKE_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        // WHERE 절에 함수가 적용된 조건 → 인덱스 무력화
        if (FUNCTION_IN_WHERE_PATTERN.matcher(query).find()) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "WHERE",
                "WHERE 절에서 컬럼에 함수가 적용되어 있습니다. 인덱스 사용이 불가능할 수 있으니 조건을 다시 작성해보세요."
            );
            suggestion.setExpectedPerformanceImprovement(FUNCTION_IN_WHERE_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    // 📌 4. SELECT 절 분석
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeSelectClause(String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();

        // SELECT * → 불필요한 데이터 과다 조회
        if (SELECT_STAR_PATTERN.matcher(query).find()) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "SELECT",
                "SELECT * 사용이 감지되었습니다. 필요한 컬럼만 선택하여 네트워크 트래픽과 메모리 사용량을 줄이세요."
            );
            suggestion.setExpectedPerformanceImprovement(SELECT_STAR_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    // 📌 5. 일반적인 최적화 (LIMIT 누락, IN 서브쿼리)
    private List<QueryAnalysisResult.OptimizationSuggestion> analyzeGeneralOptimizations(String query, JsonNode plan) {
        List<QueryAnalysisResult.OptimizationSuggestion> suggestions = new ArrayList<>();

        // LIMIT 없는 대용량 조회 → 페이징 권장
        if (!query.toUpperCase().contains("LIMIT") && plan.get("Plan Rows").asLong() > LARGE_RESULT_SET_THRESHOLD) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "GENERAL",
                "대용량 결과셋이 예상됩니다. LIMIT 절을 추가하여 페이징을 구현하는 것을 고려해보세요."
            );
            suggestion.setExpectedPerformanceImprovement(PAGINATION_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        // IN (SELECT ...) → EXISTS나 JOIN으로 변경 권장
        if (query.toUpperCase().contains("IN (SELECT")) {
            var suggestion = new QueryAnalysisResult.OptimizationSuggestion(
                "GENERAL",
                "IN 서브쿼리가 발견되었습니다. EXISTS나 JOIN으로 변경하면 성능이 향상될 수 있습니다."
            );
            suggestion.setExpectedPerformanceImprovement(SUBQUERY_IMPROVEMENT);
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    // -------------------- 🔍 헬퍼 함수 ---------------------

    // 재귀적으로 특정 Node Type이 있는지 검색
    private boolean searchNodeType(JsonNode plan, String nodeType) {
        if (plan.has("Node Type") && plan.get("Node Type").asText().equals(nodeType)) {
            return true;
        }
        if (plan.has("Plans")) {
            for (JsonNode subPlan : plan.get("Plans")) {
                if (searchNodeType(subPlan, nodeType)) return true;
            }
        }
        return false;
    }

    private boolean containsSeqScan(JsonNode plan) {
        return searchNodeType(plan, "Seq Scan");
    }

    private boolean containsIneffectiveIndexScan(JsonNode plan) {
        return searchNodeType(plan, "Index Scan") && plan.get("Total Cost").asLong() > HIGH_COST_THRESHOLD;
    }

    private boolean containsNestedLoopJoin(JsonNode plan) {
        return searchNodeType(plan, "Nested Loop");
    }
}
