package com.simulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.model.QueryAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIAnalysisService {

    @Autowired
    private WebClient openAIWebClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryAnalysisResult.AIBottleneckAnalysis analyzeBottlenecks(
            String query, 
            String executionPlan, 
            long executionTime,
            String resourceUsage) {
        
        try {
            String prompt = buildAnalysisPrompt(query, executionPlan, executionTime, resourceUsage, null, null);
            String response = callOpenAI(prompt);
            return parseOpenAIResponse(response);
        } catch (Exception e) {
            // OpenAI 호출 실패 시 기본 분석 제공
            return createFallbackAnalysis(query, executionTime);
        }
    }

    // 최적화 SQL 생성
    public String generateOptimizedSQL(String originalQuery, String executionPlan, 
                                     List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints) {
        try {
            String prompt = buildOptimizationPrompt(originalQuery, executionPlan, bottleneckPoints);
            String response = callOpenAI(prompt);
            return extractOptimizedSQL(response);
        } catch (Exception e) {
            // OpenAI 호출 실패 시 기본 최적화 제안
            return generateFallbackOptimizedSQL(originalQuery);
        }
    }

    // 병목 지점과 시간별 메트릭을 포함한 고급 분석
    public QueryAnalysisResult.AIBottleneckAnalysis analyzeBottlenecksWithMetrics(
            String query, 
            String executionPlan, 
            long executionTime,
            String resourceUsage,
            List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints,
            QueryAnalysisResult.TimeSeriesMetrics timeSeriesMetrics) {
        
        try {
            String prompt = buildAnalysisPrompt(query, executionPlan, executionTime, resourceUsage, bottleneckPoints, timeSeriesMetrics);
            String response = callOpenAI(prompt);
            return parseOpenAIResponse(response);
        } catch (Exception e) {
            // OpenAI 호출 실패 시 기본 분석 제공
            return createFallbackAnalysisWithMetrics(query, executionTime, bottleneckPoints, timeSeriesMetrics);
        }
    }

    private String buildAnalysisPrompt(String query, String executionPlan, long executionTime, String resourceUsage, 
                                     List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints,
                                     QueryAnalysisResult.TimeSeriesMetrics timeSeriesMetrics) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append(String.format("""
            다음 PostgreSQL 쿼리의 성능 병목 현상을 분석해주세요. 응답은 반드시 JSON 형식으로 제공하고, 한국어로 작성해주세요.
            
            쿼리:
            %s
            
            실행 계획:
            %s
            
            실행 시간: %d ms
            리소스 사용량: %s
            """, query, executionPlan, executionTime, resourceUsage));

        // 병목 지점 정보가 있으면 추가
        if (bottleneckPoints != null && !bottleneckPoints.isEmpty()) {
            prompt.append("\n=== 감지된 병목 지점들 ===\n");
            for (QueryAnalysisResult.BottleneckPoint bp : bottleneckPoints) {
                prompt.append(String.format("시간: %.2fms, 유형: %s, 심각도: %.1f, 설명: %s\n",
                    bp.getTimestamp(), bp.getBottleneckType(), bp.getSeverity(), bp.getDescription()));
            }
        }

        // 시간별 메트릭 정보가 있으면 추가
        if (timeSeriesMetrics != null && timeSeriesMetrics.getTimePoints() != null) {
            prompt.append("\n=== 시간별 성능 메트릭 ===\n");
            List<QueryAnalysisResult.TimePoint> timePoints = timeSeriesMetrics.getTimePoints();
            
            // 주요 성능 급변 지점 찾기
            for (int i = 1; i < Math.min(timePoints.size(), 5); i++) {
                QueryAnalysisResult.TimePoint prev = timePoints.get(i-1);
                QueryAnalysisResult.TimePoint curr = timePoints.get(i);
                
                double cpuChange = curr.getCpuUsage() - prev.getCpuUsage();
                double ioChange = curr.getIoWaitTime() - prev.getIoWaitTime();
                long memoryChange = curr.getMemoryUsage() - prev.getMemoryUsage();
                
                if (Math.abs(cpuChange) > 20 || Math.abs(ioChange) > 10 || Math.abs(memoryChange) > 100) {
                    prompt.append(String.format("시간 %.2f~%.2fms: CPU %+.1f%%, I/O %+.1fms, 메모리 %+.1fMB (%s 작업)\n",
                        prev.getTimestamp(), curr.getTimestamp(), cpuChange, ioChange, 
                        memoryChange / 1024.0 / 1024.0, curr.getOperationType()));
                }
            }
            
            // 최대 리소스 사용 지점
            QueryAnalysisResult.TimePoint maxCpuPoint = timePoints.stream()
                .max((a, b) -> Double.compare(a.getCpuUsage(), b.getCpuUsage()))
                .orElse(null);
            if (maxCpuPoint != null && maxCpuPoint.getCpuUsage() > 70) {
                prompt.append(String.format("최대 CPU 사용: %.1f%% (%.2fms, %s 작업)\n",
                    maxCpuPoint.getCpuUsage(), maxCpuPoint.getTimestamp(), maxCpuPoint.getOperationType()));
            }
        }

        prompt.append("""
            
            위의 병목 지점 분석과 시간별 메트릭을 바탕으로 다음 JSON 형식으로 응답해주세요:
            {
              "bottleneckType": "INDEX_MISSING|INEFFICIENT_JOIN|FULL_TABLE_SCAN|RESOURCE_CONTENTION|QUERY_COMPLEXITY|CARTESIAN_PRODUCT|MEMORY_OVERFLOW",
              "severityLevel": "LOW|MEDIUM|HIGH|CRITICAL",
              "description": "병목 현상에 대한 상세한 한국어 설명 (그래프의 급변 지점과 연관지어 설명)",
              "recommendations": [
                "구체적인 개선 방안 1 (병목 지점 기반)",
                "구체적인 개선 방안 2 (시간별 패턴 기반)"
              ],
              "optimizedQuery": "최적화된 쿼리 (있는 경우)",
              "estimatedImprovement": "예상 성능 향상률 (예: 30-50%)",
              "impactScore": 85,
              "confidenceLevel": 92,
              "bottleneckTimePoints": "그래프에서 문제가 발생한 시점들 (예: 0.2ms, 0.5ms)"
            }
            
            특히 다음 사항들을 중점적으로 분석해주세요:
            1. 그래프에서 CPU/메모리/I/O가 급증하는 시점과 그 원인
            2. 특정 작업(Seq Scan, Hash Join 등)에서 발생하는 리소스 집중 현상
            3. 시간에 따른 성능 패턴과 병목 지점의 상관관계
            4. CROSS JOIN이나 카티시안 곱으로 인한 지수적 성능 저하 패턴
            """);

        return prompt.toString();
    }

    private String callOpenAI(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", new Object[]{
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                },
                "max_tokens", 1500,  // 더 상세한 분석을 위해 토큰 수 증가
                "temperature", 0.3
            );

            String response = openAIWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }

    private QueryAnalysisResult.AIBottleneckAnalysis parseOpenAIResponse(String response) {
        try {
            // JSON 응답에서 실제 JSON 부분만 추출
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}") + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonContent = response.substring(jsonStart, jsonEnd);
                JsonNode jsonNode = objectMapper.readTree(jsonContent);

                QueryAnalysisResult.AIBottleneckAnalysis analysis = new QueryAnalysisResult.AIBottleneckAnalysis();
                analysis.setBottleneckType(jsonNode.path("bottleneckType").asText("QUERY_COMPLEXITY"));
                analysis.setSeverityLevel(jsonNode.path("severityLevel").asText("MEDIUM"));
                analysis.setDetailedDescription(jsonNode.path("description").asText("AI 분석 결과를 파싱할 수 없습니다."));
                analysis.setRecommendation(String.join(", ", parseRecommendations(jsonNode.path("recommendations"))));
                analysis.setSqlSuggestions(List.of(jsonNode.path("optimizedQuery").asText("")));
                analysis.setExpectedImprovement(parseImprovementPercentage(jsonNode.path("estimatedImprovement").asText("10-20%")));
                analysis.setImpactScore(jsonNode.path("impactScore").asDouble(50.0));
                analysis.setConfidenceLevel(jsonNode.path("confidenceLevel").asDouble(70.0));
                
                // 병목 시점 정보 파싱
                String bottleneckTimePoints = jsonNode.path("bottleneckTimePoints").asText("");
                if (!bottleneckTimePoints.isEmpty()) {
                    analysis.setAffectedTables("병목 시점: " + bottleneckTimePoints);
                }
                
                return analysis;
            }
        } catch (Exception e) {
            // JSON 파싱 실패 시 폴백
        }
        
        return createFallbackAnalysis("", 0);
    }

    private String[] parseRecommendations(JsonNode recommendationsNode) {
        if (recommendationsNode.isArray()) {
            return objectMapper.convertValue(recommendationsNode, String[].class);
        }
        return new String[]{"추천사항을 파싱할 수 없습니다."};
    }

    private double parseImprovementPercentage(String improvementText) {
        try {
            // "30-50%" 형태에서 평균값 추출
            if (improvementText.contains("-")) {
                String[] parts = improvementText.replace("%", "").split("-");
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0].trim());
                    double max = Double.parseDouble(parts[1].trim());
                    return (min + max) / 2.0;
                }
            }
            // "30%" 형태에서 숫자만 추출
            return Double.parseDouble(improvementText.replace("%", "").trim());
        } catch (Exception e) {
            return 15.0; // 기본값
        }
    }

    private QueryAnalysisResult.AIBottleneckAnalysis createFallbackAnalysis(String query, long executionTime) {
        String severityLevel = executionTime > 5000 ? "HIGH" : executionTime > 1000 ? "MEDIUM" : "LOW";
        
        QueryAnalysisResult.AIBottleneckAnalysis analysis = new QueryAnalysisResult.AIBottleneckAnalysis();
        analysis.setBottleneckType("QUERY_COMPLEXITY");
        analysis.setSeverityLevel(severityLevel);
        analysis.setDetailedDescription("OpenAI API를 사용할 수 없어 기본 분석을 제공합니다. 쿼리 실행 시간이 " + executionTime + "ms로 측정되었습니다.");
        analysis.setRecommendation("인덱스 사용을 검토해보세요, WHERE 절 조건을 최적화해보세요, 불필요한 조인을 제거해보세요");
        analysis.setSqlSuggestions(Arrays.asList("-- 기본 최적화 제안을 위해 쿼리를 검토해보세요"));
        analysis.setExpectedImprovement(20.0);
        analysis.setImpactScore(60.0);
        analysis.setConfidenceLevel(50.0);
        
        return analysis;
    }

    private QueryAnalysisResult.AIBottleneckAnalysis createFallbackAnalysisWithMetrics(String query, long executionTime,
            List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints,
            QueryAnalysisResult.TimeSeriesMetrics timeSeriesMetrics) {
        
        QueryAnalysisResult.AIBottleneckAnalysis analysis = createFallbackAnalysis(query, executionTime);
        
        // 병목 지점 기반 추가 분석
        if (bottleneckPoints != null && !bottleneckPoints.isEmpty()) {
            StringBuilder description = new StringBuilder(analysis.getDetailedDescription());
            description.append("\n\n감지된 병목 지점들:\n");
            
            for (QueryAnalysisResult.BottleneckPoint bp : bottleneckPoints) {
                description.append(String.format("- %.2fms: %s (심각도: %.1f)\n", 
                    bp.getTimestamp(), bp.getDescription(), bp.getSeverity()));
            }
            
            analysis.setDetailedDescription(description.toString());
            
            // 가장 심각한 병목 지점 기반으로 타입 결정
            QueryAnalysisResult.BottleneckPoint mostSevere = bottleneckPoints.stream()
                .max((a, b) -> Double.compare(a.getSeverity(), b.getSeverity()))
                .orElse(null);
                
            if (mostSevere != null) {
                switch (mostSevere.getBottleneckType()) {
                    case "CPU":
                        analysis.setBottleneckType("RESOURCE_CONTENTION");
                        break;
                    case "IO":
                        analysis.setBottleneckType("FULL_TABLE_SCAN");
                        break;
                    case "JOIN_INEFFICIENCY":
                        analysis.setBottleneckType("INEFFICIENT_JOIN");
                        break;
                    default:
                        analysis.setBottleneckType("QUERY_COMPLEXITY");
                }
                
                if (mostSevere.getSeverity() > 80) {
                    analysis.setSeverityLevel("HIGH");
                } else if (mostSevere.getSeverity() > 60) {
                    analysis.setSeverityLevel("MEDIUM");
                }
            }
        }
        
        return analysis;
    }

    // 최적화 프롬프트 생성
    private String buildOptimizationPrompt(String originalQuery, String executionPlan, 
                                         List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다음 PostgreSQL 쿼리를 최적화해주세요:\n\n");
        prompt.append("원본 쿼리:\n```sql\n").append(originalQuery).append("\n```\n\n");
        
        if (executionPlan != null && !executionPlan.isEmpty()) {
            prompt.append("실행 계획:\n").append(executionPlan).append("\n\n");
        }
        
        if (bottleneckPoints != null && !bottleneckPoints.isEmpty()) {
            prompt.append("감지된 병목 지점들:\n");
            for (QueryAnalysisResult.BottleneckPoint point : bottleneckPoints) {
                prompt.append("- ").append(point.getDescription())
                      .append(" (심각도: ").append(point.getSeverity()).append(")\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("최적화 요구사항:\n");
        prompt.append("1. 동일한 결과를 반환하는 최적화된 SQL을 제공해주세요\n");
        prompt.append("2. 인덱스 활용, JOIN 순서 최적화, 서브쿼리 개선 등을 고려해주세요\n");
        prompt.append("3. PostgreSQL 특화 기능(CTE, 윈도우 함수 등)을 활용해주세요\n");
        prompt.append("4. 응답은 다음 형식으로 해주세요:\n\n");
        prompt.append("OPTIMIZED_SQL:\n```sql\n[최적화된 SQL]\n```\n\n");
        prompt.append("EXPLANATION:\n[최적화 설명]\n");
        
        return prompt.toString();
    }

    // OpenAI 응답에서 최적화된 SQL 추출
    private String extractOptimizedSQL(String response) {
        try {
            // OPTIMIZED_SQL 섹션 찾기
            String sqlMarker = "OPTIMIZED_SQL:";
            int sqlStart = response.indexOf(sqlMarker);
            if (sqlStart == -1) {
                // 대안 마커들 시도
                String[] markers = {"```sql", "SELECT", "WITH"};
                for (String marker : markers) {
                    sqlStart = response.indexOf(marker);
                    if (sqlStart != -1) break;
                }
            } else {
                sqlStart += sqlMarker.length();
            }
            
            if (sqlStart == -1) {
                return "-- AI가 최적화된 SQL을 생성하지 못했습니다\n" + response;
            }
            
            // SQL 코드 블록 추출
            int codeStart = response.indexOf("```sql", sqlStart);
            if (codeStart != -1) {
                codeStart += 6; // "```sql".length()
                int codeEnd = response.indexOf("```", codeStart);
                if (codeEnd != -1) {
                    return response.substring(codeStart, codeEnd).trim();
                }
            }
            
            // 코드 블록이 없으면 첫 번째 SELECT부터 추출
            int selectStart = response.indexOf("SELECT", sqlStart);
            if (selectStart != -1) {
                // 다음 섹션 또는 끝까지
                int nextSection = response.indexOf("EXPLANATION:", selectStart);
                if (nextSection == -1) nextSection = response.length();
                return response.substring(selectStart, nextSection).trim();
            }
            
            return "-- 최적화된 SQL 추출 실패\n" + response;
            
        } catch (Exception e) {
            return "-- SQL 추출 중 오류 발생: " + e.getMessage() + "\n" + response;
        }
    }

    // 기본 최적화 SQL 생성 (OpenAI 실패 시)
    private String generateFallbackOptimizedSQL(String originalQuery) {
        String lower = originalQuery.toLowerCase().trim();
        
        StringBuilder optimized = new StringBuilder();
        optimized.append("-- AI 최적화 실패 시 기본 제안\n");
        optimized.append("-- 원본 쿼리를 기반으로 한 일반적인 최적화 제안:\n\n");
        
        if (lower.contains("cross join")) {
            optimized.append("-- CROSS JOIN을 INNER JOIN으로 최적화 고려\n");
            optimized.append("-- 조건절을 WHERE에서 ON으로 이동 권장\n\n");
        }
        
        if (lower.contains("select *")) {
            optimized.append("-- SELECT *를 필요한 컬럼만 명시하도록 최적화\n\n");
        }
        
        if (lower.contains("order by") && !lower.contains("limit")) {
            optimized.append("-- ORDER BY 사용 시 LIMIT 추가 고려\n\n");
        }
        
        optimized.append(originalQuery);
        optimized.append("\n\n-- 추가 최적화 권장사항:\n");
        optimized.append("-- 1. 적절한 인덱스 생성\n");
        optimized.append("-- 2. 통계 정보 업데이트 (ANALYZE)\n");
        optimized.append("-- 3. 조건절 선택도 개선\n");
        
        return optimized.toString();
    }
} 