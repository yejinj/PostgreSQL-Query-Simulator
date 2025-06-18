// ResultRenderer Service Implementation

package com.ncp.dbsimulator.service;

import com.ncp.dbsimulator.model.ExecutionPlan;
import com.ncp.dbsimulator.model.NodeAnalysis;
import com.ncp.dbsimulator.model.ResourceCost;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResultRenderer {

    private final NumberFormat currencyFormat;
    private final NumberFormat numberFormat;

    public ResultRenderer() {
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.KOREA);
        this.numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
    }

    /**
     * 쿼리 분석 결과를 사용자 친화적인 형태로 렌더링
     */
    public String renderAnalysisResult(String query, ExecutionPlan plan, ResourceCost cost, int executionsPerMonth) {
        StringBuilder result = new StringBuilder();
        
        // 헤더
        result.append("=".repeat(80)).append("\n");
        result.append("          PostgreSQL 쿼리 비용 분석 결과\n");
        result.append("=".repeat(80)).append("\n\n");
        
        // 쿼리 정보
        result.append("분석 대상 쿼리:\n");
        result.append("─".repeat(40)).append("\n");
        result.append(formatQuery(query)).append("\n\n");
        
        // 비용 요약
        result.append(renderCostSummary(cost, executionsPerMonth));
        
        // 상세 분석
        result.append(renderDetailedAnalysis(plan, cost));
        
        // 노드별 기여도 분석
        result.append(renderNodeContributionAnalysis(plan));
        
        // 월간 예상 비용
        if (executionsPerMonth > 0) {
            result.append(renderMonthlyProjection(cost, executionsPerMonth));
        }
        
        // 최적화 제안
        result.append(renderOptimizationSuggestions(plan));
        
        result.append("=".repeat(80)).append("\n");
        
        return result.toString();
    }

    /**
     * 쿼리를 보기 좋게 포맷팅
     */
    private String formatQuery(String query) {
        return query.trim()
            .replaceAll("(?i)\\bSELECT\\b", "\nSELECT")
            .replaceAll("(?i)\\bFROM\\b", "\nFROM")
            .replaceAll("(?i)\\bWHERE\\b", "\nWHERE")
            .replaceAll("(?i)\\bJOIN\\b", "\nJOIN")
            .replaceAll("(?i)\\bORDER BY\\b", "\nORDER BY")
            .replaceAll("(?i)\\bGROUP BY\\b", "\nGROUP BY")
            .replaceAll("(?i)\\bHAVING\\b", "\nHAVING")
            .trim();
    }

    /**
     * 비용 요약 렌더링
     */
    private String renderCostSummary(ResourceCost cost, int executionsPerMonth) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("비용 분석 결과\n");
        summary.append("─".repeat(40)).append("\n");
        
        // 총 비용 (강조)
        summary.append(String.format("총 추정 비용: %s원\n\n", 
            formatCurrency(cost.getTotalCost())));
        
        // 비용 세부 내역
        summary.append("비용 세부 내역:\n");
        summary.append(String.format("  • CPU 비용:        %s원\n", formatCurrency(cost.getCpuCost())));
        summary.append(String.format("  • I/O 비용:        %s원\n", formatCurrency(cost.getIoCost())));
        summary.append(String.format("  • 메모리 비용:      %s원\n", formatCurrency(cost.getMemoryCost())));
        summary.append(String.format("  • 네트워크 비용:    %s원\n", formatCurrency(cost.getNetworkCost())));
        summary.append("\n");
        
        // 비용 구성 비율
        summary.append(renderCostBreakdownChart(cost));
        
        return summary.toString();
    }

    /**
     * 비용 구성 비율을 간단한 차트로 표시
     */
    private String renderCostBreakdownChart(ResourceCost cost) {
        StringBuilder chart = new StringBuilder();
        
        double total = cost.getTotalCost();
        if (total <= 0) return "";
        
        Map<String, Double> costMap = new LinkedHashMap<>();
        costMap.put("CPU", cost.getCpuCost());
        costMap.put("I/O", cost.getIoCost());
        costMap.put("메모리", cost.getMemoryCost());
        costMap.put("네트워크", cost.getNetworkCost());
        
        chart.append("비용 구성 비율:\n");
        
        costMap.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(entry -> {
                double percentage = (entry.getValue() / total) * 100;
                int barLength = (int) (percentage / 5); // 5%당 1개 블록
                String bar = "█".repeat(Math.max(1, barLength));
                
                chart.append(String.format("  %s %s %.1f%%\n", 
                    entry.getKey().length() < 4 ? entry.getKey() + " ".repeat(4 - entry.getKey().length()) : entry.getKey(),
                    bar, percentage));
            });
        
        chart.append("\n");
        return chart.toString();
    }

    /**
     * 상세 분석 렌더링
     */
    private String renderDetailedAnalysis(ExecutionPlan plan, ResourceCost cost) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("실행 계획 상세 분석\n");
        analysis.append("─".repeat(40)).append("\n");
        
        analysis.append(String.format("실제 실행 시간: %s ms\n", 
            numberFormat.format(plan.getActualTotalTime())));
        analysis.append(String.format("PostgreSQL 예상 비용: %s\n", 
            numberFormat.format(plan.getTotalCost())));
        
        // I/O 성능 분석
        long totalReads = plan.getNodeAnalyses().stream()
            .mapToLong(NodeAnalysis::getSharedReadBlocks)
            .sum();
        long totalHits = plan.getNodeAnalyses().stream()
            .mapToLong(NodeAnalysis::getSharedHitBlocks)
            .sum();
        
        if (totalReads + totalHits > 0) {
            double hitRatio = (double) totalHits / (totalReads + totalHits) * 100;
            analysis.append(String.format("버퍼 캐시 히트율: %.1f%%\n", hitRatio));
            analysis.append(String.format("디스크 읽기: %s 블록\n", numberFormat.format(totalReads)));
            analysis.append(String.format("캐시 히트: %s 블록\n", numberFormat.format(totalHits)));
        }
        
        analysis.append("\n");
        return analysis.toString();
    }

    /**
     * 노드별 기여도 분석
     */
    private String renderNodeContributionAnalysis(ExecutionPlan plan) {
        StringBuilder contribution = new StringBuilder();
        
        contribution.append("주요 비용 발생 구간\n");
        contribution.append("─".repeat(40)).append("\n");
        
        List<NodeAnalysis> significantNodes = plan.getNodeAnalyses().stream()
            .filter(node -> node.getTotalCost() > plan.getTotalCost() * 0.05) // 5% 이상
            .sorted((a, b) -> Double.compare(b.getTotalCost(), a.getTotalCost()))
            .limit(5)
            .collect(Collectors.toList());
        
        if (significantNodes.isEmpty()) {
            contribution.append("• 특별히 비용이 높은 구간이 없습니다.\n\n");
        } else {
            for (int i = 0; i < significantNodes.size(); i++) {
                NodeAnalysis node = significantNodes.get(i);
                double percentage = (node.getTotalCost() / plan.getTotalCost()) * 100;
                
                contribution.append(String.format("%d. %s", i + 1, getNodeTypeKorean(node.getNodeType())));
                
                if (node.getRelationName() != null) {
                    contribution.append(String.format(" (%s 테이블)", node.getRelationName()));
                }
                
                contribution.append(String.format("\n  비용: %s (%.1f%%)", 
                    numberFormat.format(node.getTotalCost()), percentage));
                
                if (node.getActualRows() > 0) {
                    contribution.append(String.format(" | 처리행수: %s", 
                        numberFormat.format(node.getActualRows())));
                }
                
                contribution.append("\n\n");
            }
        }
        
        return contribution.toString();
    }

    /**
     * 월간 예상 비용 계산 및 렌더링
     */
    private String renderMonthlyProjection(ResourceCost cost, int executionsPerMonth) {
        StringBuilder projection = new StringBuilder();
        
        double monthlyTotal = cost.getTotalCost() * executionsPerMonth;
        
        projection.append("월간 비용 예측\n");
        projection.append("─".repeat(40)).append("\n");
        
        projection.append(String.format("월간 실행 횟수: %s회\n", 
            numberFormat.format(executionsPerMonth)));
        projection.append(String.format("쿼리당 비용: %s원\n", 
            formatCurrency(cost.getTotalCost())));
        projection.append(String.format("월간 총 비용: %s원\n\n", 
            formatCurrency(monthlyTotal)));
        
        // 연간 비용도 표시
        double yearlyTotal = monthlyTotal * 12;
        projection.append(String.format("연간 예상 비용: %s원\n\n", 
            formatCurrency(yearlyTotal)));
        
        // 비용 등급 표시
        projection.append(renderCostGrade(monthlyTotal));
        
        return projection.toString();
    }

    /**
     * 비용 등급 표시
     */
    private String renderCostGrade(double monthlyCost) {
        StringBuilder grade = new StringBuilder();
        
        grade.append("비용 등급: ");
        
        if (monthlyCost < 1000) {
            grade.append("매우 낮음 (1천원 미만)");
        } else if (monthlyCost < 10000) {
            grade.append("낮음 (1만원 미만)");
        } else if (monthlyCost < 100000) {
            grade.append("보통 (10만원 미만)");
        } else if (monthlyCost < 1000000) {
            grade.append("높음 (100만원 미만)");
        } else {
            grade.append("매우 높음 (100만원 이상)");
        }
        
        grade.append("\n\n");
        return grade.toString();
    }

    /**
     * 최적화 제안 렌더링
     */
    private String renderOptimizationSuggestions(ExecutionPlan plan) {
        StringBuilder suggestions = new StringBuilder();
        
        suggestions.append("최적화 제안\n");
        suggestions.append("─".repeat(40)).append("\n");
        
        List<String> advices = generateOptimizationAdvices(plan);
        
        if (advices.isEmpty()) {
            suggestions.append("현재 쿼리는 잘 최적화되어 있습니다!\n\n");
        } else {
            for (String advice : advices) {
                suggestions.append("• ").append(advice).append("\n");
            }
            suggestions.append("\n");
        }
        
        return suggestions.toString();
    }

    /**
     * 최적화 조언 생성
     */
    private List<String> generateOptimizationAdvices(ExecutionPlan plan) {
        List<String> advices = new ArrayList<>();
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            // Sequential Scan 감지
            if ("Seq Scan".equals(node.getNodeType()) && node.getActualRows() > 1000) {
                advices.add(String.format("%s 테이블에서 전체 스캔 발생 → 인덱스 생성 검토 필요", 
                    node.getRelationName() != null ? node.getRelationName() : "테이블"));
            }
            
            // 정렬 작업이 디스크 사용
            if ("Sort".equals(node.getNodeType()) && "disk".equals(node.getSortSpaceType())) {
                advices.add("정렬 작업이 디스크 사용 중 → work_mem 설정 증가 검토");
            }
            
            // 비용이 높은 Nested Loop
            if ("Nested Loop".equals(node.getNodeType()) && 
                node.getTotalCost() > plan.getTotalCost() * 0.3) {
                advices.add("비용이 높은 Nested Loop 감지 → 조인 방식 또는 인덱스 최적화 필요");
            }
            
            // 해시 조인에서 예상과 다른 행 수
            if ("Hash Join".equals(node.getNodeType()) && 
                node.getActualRows() > node.getPlanRows() * 2) {
                advices.add("해시 조인에서 예상보다 많은 행 처리 → 통계 정보 업데이트 필요");
            }
        }
        
        // 캐시 히트율 확인
        long totalReads = plan.getNodeAnalyses().stream()
            .mapToLong(NodeAnalysis::getSharedReadBlocks).sum();
        long totalHits = plan.getNodeAnalyses().stream()
            .mapToLong(NodeAnalysis::getSharedHitBlocks).sum();
        
        if (totalReads + totalHits > 0) {
            double hitRatio = (double) totalHits / (totalReads + totalHits) * 100;
            if (hitRatio < 90) {
                advices.add("버퍼 캐시 히트율 낮음 → 메모리 증설 또는 인덱스 최적화 검토");
            }
        }
        
        return advices.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 노드 타입을 한국어로 변환
     */
    private String getNodeTypeKorean(String nodeType) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("Seq Scan", "순차 스캔");
        typeMap.put("Index Scan", "인덱스 스캔");
        typeMap.put("Index Only Scan", "인덱스 전용 스캔");
        typeMap.put("Bitmap Heap Scan", "비트맵 힙 스캔");
        typeMap.put("Bitmap Index Scan", "비트맵 인덱스 스캔");
        typeMap.put("Hash Join", "해시 조인");
        typeMap.put("Merge Join", "병합 조인");
        typeMap.put("Nested Loop", "중첩 루프");
        typeMap.put("Sort", "정렬");
        typeMap.put("Hash", "해시");
        typeMap.put("Aggregate", "집계");
        typeMap.put("HashAggregate", "해시 집계");
        typeMap.put("GroupAggregate", "그룹 집계");
        typeMap.put("Limit", "제한");
        
        return typeMap.getOrDefault(nodeType, nodeType);
    }

    /**
     * 통화 포맷팅
     */
    private String formatCurrency(double amount) {
        if (amount < 0.001) {
            return String.format("%.6f", amount);
        } else if (amount < 1) {
            return String.format("%.3f", amount);
        } else {
            return numberFormat.format(amount);
        }
    }

    /**
     * JSON 형태의 API 응답용 결과 생성
     */
    public Map<String, Object> renderApiResponse(String query, ExecutionPlan plan, ResourceCost cost, int executionsPerMonth) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("query", query);
        
        // 비용 정보
        Map<String, Object> costResult = new HashMap<>();
        costResult.put("totalCost", cost.getTotalCost());
        costResult.put("cpuCost", cost.getCpuCost());
        costResult.put("ioCost", cost.getIoCost());
        costResult.put("memoryCost", cost.getMemoryCost());
        costResult.put("networkCost", cost.getNetworkCost());
        
        if (executionsPerMonth > 0) {
            costResult.put("monthlyCost", cost.getTotalCost() * executionsPerMonth);
            costResult.put("executionsPerMonth", executionsPerMonth);
        }
        
        response.put("costResult", costResult);
        
        // 실행 계획 정보
        Map<String, Object> planInfo = new HashMap<>();
        planInfo.put("executionTime", plan.getActualTotalTime());
        planInfo.put("postgresqlCost", plan.getTotalCost());
        planInfo.put("nodeCount", plan.getNodeAnalyses().size());
        
        response.put("planInfo", planInfo);
        
        // 최적화 제안
        response.put("optimizationSuggestions", generateOptimizationAdvices(plan));
        
        return response;
    }
} 