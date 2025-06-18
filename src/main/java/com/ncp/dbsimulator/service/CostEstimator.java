// CostEstimator Service Implementation

package com.ncp.dbsimulator.service;

import com.ncp.dbsimulator.model.ExecutionPlan;
import com.ncp.dbsimulator.model.NodeAnalysis;
import com.ncp.dbsimulator.model.ResourceCost;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class CostEstimator {

    // 사용자 요구사항에 맞는 정확한 비용 모델 (원화 기준)
    private static final double CPU_SECOND_COST = 0.005;      // 1 CPU 초당 ₩0.005
    private static final double DISK_READ_MB_COST = 0.0002;   // 1MB 디스크 읽기당 ₩0.0002
    private static final double DISK_WRITE_MB_COST = 0.0003;  // 1MB 디스크 쓰기당 ₩0.0003
    private static final double SORT_HASH_OPERATION_COST = 0.002; // 1회 정렬/해시 연산당 ₩0.002
    private static final double ROW_PROCESSING_COST = 0.00001;    // 1개 행 처리당 ₩0.00001
    
    // PostgreSQL 블록 크기 (8KB)
    private static final double BLOCK_SIZE_KB = 8.0;
    private static final double KB_TO_MB = 1024.0;

    public ResourceCost calculateResourceCost(ExecutionPlan plan) {
        ResourceCost cost = new ResourceCost();
        
        // 각 노드별 상세 비용 분석
        Map<String, Double> costBreakdown = new HashMap<>();
        
        // CPU 비용 계산
        double cpuCost = calculateCpuCost(plan);
        costBreakdown.put("CPU 비용", cpuCost);
        
        // 디스크 I/O 비용 계산
        Map<String, Double> diskCosts = calculateDiskCosts(plan);
        costBreakdown.put("디스크 읽기 비용", diskCosts.get("read"));
        costBreakdown.put("디스크 쓰기 비용", diskCosts.get("write"));
        
        // 정렬/해시 연산 비용 계산
        double sortHashCost = calculateSortHashCost(plan);
        costBreakdown.put("정렬/해시 비용", sortHashCost);
        
        // 행 처리 비용 계산
        double rowProcessingCost = calculateRowProcessingCost(plan);
        costBreakdown.put("행 처리 비용", rowProcessingCost);
        
        // 총 비용
        double totalCost = cpuCost + diskCosts.get("read") + diskCosts.get("write") + 
                          sortHashCost + rowProcessingCost;
        
        cost.setTotalCost(round(totalCost));
        cost.setCpuCost(round(cpuCost));
        cost.setIoCost(round(diskCosts.get("read") + diskCosts.get("write")));
        cost.setMemoryCost(round(sortHashCost));  // 정렬/해시를 메모리 비용으로 분류
        cost.setNetworkCost(round(rowProcessingCost));  // 행 처리를 네트워크 비용으로 분류
        
        // 상세 분석 정보 (한국어)
        cost.setAnalysisDetails(generateKoreanAnalysisDetails(plan, costBreakdown));
        
        return cost;
    }

    private double calculateCpuCost(ExecutionPlan plan) {
        // 실제 실행 시간(ms)을 초로 변환하여 CPU 비용 계산
        double totalTimeSeconds = plan.getActualTotalTime() / 1000.0;
        
        // 복잡도에 따른 CPU 사용량 추정
        double cpuIntensity = estimateCpuIntensity(plan);
        
        return totalTimeSeconds * cpuIntensity * CPU_SECOND_COST;
    }

    private Map<String, Double> calculateDiskCosts(ExecutionPlan plan) {
        Map<String, Double> costs = new HashMap<>();
        long totalReadBlocks = 0;
        long totalWrittenBlocks = 0;
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            totalReadBlocks += node.getSharedReadBlocks();
            totalWrittenBlocks += node.getSharedWrittenBlocks() + node.getSharedDirtiedBlocks();
        }
        
        // 블록을 MB로 변환
        double readMB = (totalReadBlocks * BLOCK_SIZE_KB) / KB_TO_MB;
        double writeMB = (totalWrittenBlocks * BLOCK_SIZE_KB) / KB_TO_MB;
        
        costs.put("read", readMB * DISK_READ_MB_COST);
        costs.put("write", writeMB * DISK_WRITE_MB_COST);
        
        return costs;
    }

    private double calculateSortHashCost(ExecutionPlan plan) {
        double totalCost = 0.0;
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            // 정렬 연산 비용
            if ("Sort".equals(node.getNodeType())) {
                int loops = Math.max(1, node.getActualLoops());
                totalCost += SORT_HASH_OPERATION_COST * loops;
            }
            
            // 해시 연산 비용
            if ("Hash".equals(node.getNodeType()) || 
                "Hash Join".equals(node.getNodeType()) ||
                "HashAggregate".equals(node.getNodeType())) {
                int loops = Math.max(1, node.getActualLoops());
                totalCost += SORT_HASH_OPERATION_COST * loops;
            }
        }
        
        return totalCost;
    }

    private double calculateRowProcessingCost(ExecutionPlan plan) {
        long totalRowsProcessed = 0;
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            long actualRows = node.getActualRows();
            int loops = Math.max(1, node.getActualLoops());
            totalRowsProcessed += actualRows * loops;
        }
        
        return totalRowsProcessed * ROW_PROCESSING_COST;
    }

    private double estimateCpuIntensity(ExecutionPlan plan) {
        double intensity = 1.0; // 기본 강도
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            // 복잡한 조인 작업
            if (node.getJoinType() != null) {
                if ("Merge Join".equals(node.getNodeType())) {
                    intensity += 0.3;
                } else if ("Hash Join".equals(node.getNodeType())) {
                    intensity += 0.4;
                } else if ("Nested Loop".equals(node.getNodeType())) {
                    intensity += 0.5;
                }
            }
            
            // 집계 작업
            if ("Aggregate".equals(node.getNodeType()) || 
                "HashAggregate".equals(node.getNodeType()) ||
                "GroupAggregate".equals(node.getNodeType())) {
                intensity += 0.3;
            }
            
            // 서브쿼리
            if ("SubPlan".equals(node.getNodeType()) || 
                "InitPlan".equals(node.getNodeType())) {
                intensity += 0.2;
            }
        }
        
        return Math.min(intensity, 3.0); // 최대 3배까지 제한
    }

    private String generateKoreanAnalysisDetails(ExecutionPlan plan, Map<String, Double> costBreakdown) {
        StringBuilder details = new StringBuilder();
        
        details.append("=== 쿼리 비용 분석 결과 ===\n\n");
        
        // 총 비용 요약
        details.append(String.format("총 추정 비용: ₩%.3f원\n\n", costBreakdown.values().stream().mapToDouble(Double::doubleValue).sum()));
        
        // 비용 항목별 세부사항
        details.append("비용 세부 내역:\n");
        costBreakdown.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // 비용 내림차순 정렬
            .forEach(entry -> {
                double percentage = (entry.getValue() / costBreakdown.values().stream().mapToDouble(Double::doubleValue).sum()) * 100;
                details.append(String.format("• %s: ₩%.3f원 (%.1f%%)\n", 
                    entry.getKey(), entry.getValue(), percentage));
            });
        
        details.append("\n");
        
        // 실행 계획 분석
        details.append("실행 계획 분석:\n");
        details.append(String.format("• 총 실행 시간: %.2f ms\n", plan.getActualTotalTime()));
        details.append(String.format("• PostgreSQL 예상 비용: %.2f\n", plan.getTotalCost()));
        
        // 주요 비용 발생 노드
        details.append("\n주요 비용 발생 구간:\n");
        plan.getNodeAnalyses().stream()
            .filter(node -> node.getTotalCost() > plan.getTotalCost() * 0.1) // 전체 비용의 10% 이상
            .sorted((a, b) -> Double.compare(b.getTotalCost(), a.getTotalCost()))
            .forEach(node -> {
                String description = getNodeDescription(node);
                details.append(String.format("• %s - %s (PostgreSQL 비용: %.2f)\n", 
                    node.getNodeType(), description, node.getTotalCost()));
            });
        
        // I/O 성능 분석
        addIoPerformanceAnalysis(details, plan);
        
        // 최적화 제안
        addOptimizationSuggestions(details, plan);
        
        return details.toString();
    }

    private String getNodeDescription(NodeAnalysis node) {
        StringBuilder desc = new StringBuilder();
        
        if (node.getRelationName() != null) {
            desc.append(node.getRelationName()).append(" 테이블");
        }
        
        if (node.getJoinType() != null) {
            desc.append(" (").append(node.getJoinType()).append(" 조인)");
        }
        
        if (node.getActualRows() > 0) {
            desc.append(String.format(" %,d행 처리", node.getActualRows()));
        }
        
        return desc.length() > 0 ? desc.toString() : "데이터 처리";
    }

    private void addIoPerformanceAnalysis(StringBuilder details, ExecutionPlan plan) {
        long totalReads = plan.getNodeAnalyses().stream()
            .mapToLong(NodeAnalysis::getSharedReadBlocks)
            .sum();
        long totalHits = plan.getNodeAnalyses().stream()
            .mapToLong(NodeAnalysis::getSharedHitBlocks)
            .sum();
        
        if (totalReads + totalHits > 0) {
            double hitRatio = (double) totalHits / (totalReads + totalHits) * 100;
            details.append("\nI/O 성능 분석:\n");
            details.append(String.format("• 버퍼 캐시 히트율: %.1f%%\n", hitRatio));
            details.append(String.format("• 디스크 읽기: %,d 블록 (%.2f MB)\n", 
                totalReads, (totalReads * BLOCK_SIZE_KB) / KB_TO_MB));
            details.append(String.format("• 캐시 히트: %,d 블록\n", totalHits));
            
            if (hitRatio < 90) {
                details.append("경고: 캐시 히트율이 낮습니다. 인덱스 최적화나 메모리 증설을 고려해보세요.\n");
            }
        }
    }

    private void addOptimizationSuggestions(StringBuilder details, ExecutionPlan plan) {
        details.append("\n최적화 제안:\n");
        
        boolean hasSeqScan = false;
        boolean hasSortSpill = false;
        boolean hasNestedLoop = false;
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            // Sequential Scan 감지
            if ("Seq Scan".equals(node.getNodeType()) && node.getActualRows() > 1000) {
                hasSeqScan = true;
            }
            
            // 정렬 작업이 디스크 사용
            if ("Sort".equals(node.getNodeType()) && "disk".equals(node.getSortSpaceType())) {
                hasSortSpill = true;
            }
            
            // Nested Loop with high cost
            if ("Nested Loop".equals(node.getNodeType()) && node.getTotalCost() > plan.getTotalCost() * 0.3) {
                hasNestedLoop = true;
            }
        }
        
        if (hasSeqScan) {
            details.append("• 전체 테이블 스캔이 감지되었습니다. 적절한 인덱스 생성을 고려해보세요.\n");
        }
        
        if (hasSortSpill) {
            details.append("• 정렬 작업이 디스크를 사용하고 있습니다. work_mem 설정을 증가시켜보세요.\n");
        }
        
        if (hasNestedLoop) {
            details.append("• 비용이 높은 Nested Loop 조인이 감지되었습니다. 조인 순서나 인덱스를 최적화해보세요.\n");
        }
        
        if (!hasSeqScan && !hasSortSpill && !hasNestedLoop) {
            details.append("• 현재 쿼리는 잘 최적화되어 있습니다.\n");
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
