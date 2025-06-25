package com.simulator.service;

import com.simulator.model.QueryAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ResourceAnalysisService {
    
    public QueryAnalysisResult.ResourceMetrics analyzeResourceUsage(QueryAnalysisResult.ResourceUsage resourceUsage) {
        QueryAnalysisResult.ResourceMetrics metrics = new QueryAnalysisResult.ResourceMetrics();
        
        // 1. CPU 사용률 분석 (실행 시간 기반)
        double cpuIntensity = analyzeCpuIntensity(resourceUsage.getActualTime());
        metrics.setCpuIntensity(cpuIntensity);
        
        // 2. I/O 효율성 분석 (디스크 읽기/쓰기 기반)
        double ioEfficiency = analyzeIoEfficiency(resourceUsage);
        metrics.setIoEfficiency(ioEfficiency);
        
        // 3. 메모리 사용 효율성 분석 (버퍼 히트율 기반)
        double memoryEfficiency = analyzeMemoryEfficiency(resourceUsage);
        metrics.setMemoryEfficiency(memoryEfficiency);
        
        // 4. 네트워크 효율성 분석 (데이터 전송량 기반)
        double networkEfficiency = analyzeNetworkEfficiency(resourceUsage);
        metrics.setNetworkEfficiency(networkEfficiency);
        
        // 5. 전체 성능 점수 계산
        double overallPerformance = calculateOverallPerformance(cpuIntensity, ioEfficiency, memoryEfficiency, networkEfficiency);
        metrics.setOverallPerformance(overallPerformance);
        
        // 6. 성능 등급 설정
        metrics.setPerformanceGrade(getPerformanceGrade(overallPerformance));
        
        return metrics;
    }
    
    private double analyzeCpuIntensity(double actualTimeMs) {
        // CPU 집약도 분석 (낮을수록 좋음)
        if (actualTimeMs < 10) return 95.0; // 매우 빠름
        if (actualTimeMs < 100) return 85.0; // 빠름
        if (actualTimeMs < 1000) return 70.0; // 보통
        if (actualTimeMs < 5000) return 50.0; // 느림
        return 30.0; // 매우 느림
    }
    
    private double analyzeIoEfficiency(QueryAnalysisResult.ResourceUsage resourceUsage) {
        long totalReads = resourceUsage.getSharedBlksRead();
        long totalHits = resourceUsage.getSharedBlksHit();
        
        if (totalReads + totalHits == 0) return 100.0;
        
        // 버퍼 히트율 계산
        double hitRatio = (double) totalHits / (totalReads + totalHits);
        return hitRatio * 100;
    }
    
    private double analyzeMemoryEfficiency(QueryAnalysisResult.ResourceUsage resourceUsage) {
        long sharedBlksHit = resourceUsage.getSharedBlksHit();
        long planRows = resourceUsage.getPlanRows();
        
        if (planRows == 0) return 100.0;
        
        // 행당 메모리 히트 비율
        double memoryHitsPerRow = (double) sharedBlksHit / planRows;
        
        // 효율성 점수 계산 (낮을수록 좋음)
        if (memoryHitsPerRow < 1) return 95.0;
        if (memoryHitsPerRow < 5) return 80.0;
        if (memoryHitsPerRow < 10) return 65.0;
        if (memoryHitsPerRow < 20) return 50.0;
        return 30.0;
    }
    
    private double analyzeNetworkEfficiency(QueryAnalysisResult.ResourceUsage resourceUsage) {
        long planRows = resourceUsage.getPlanRows();
        long planWidth = resourceUsage.getPlanWidth();
        
        if (planRows == 0) return 100.0;
        
        // 행당 데이터 크기 분석
        long avgRowSize = planWidth;
        
        if (avgRowSize < 100) return 95.0; // 매우 효율적
        if (avgRowSize < 500) return 80.0; // 효율적
        if (avgRowSize < 1000) return 65.0; // 보통
        if (avgRowSize < 2000) return 50.0; // 비효율적
        return 30.0; // 매우 비효율적
    }
    
    private double calculateOverallPerformance(double cpu, double io, double memory, double network) {
        // 가중 평균 계산 (CPU와 I/O에 더 높은 가중치)
        return (cpu * 0.3 + io * 0.4 + memory * 0.2 + network * 0.1);
    }
    
    private String getPerformanceGrade(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C+";
        if (score >= 40) return "C";
        if (score >= 30) return "D";
        return "F";
    }
    
    // 리소스 사용량 분석 리포트 생성
    public String generateResourceReport(QueryAnalysisResult.ResourceUsage resourceUsage, QueryAnalysisResult.ResourceMetrics metrics) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 리소스 사용량 상세 분석 ===\n\n");
        
        report.append("📊 실행 메트릭:\n");
        report.append(String.format("• 실행 시간: %.2f ms\n", resourceUsage.getActualTime()));
        report.append(String.format("• 처리된 행 수: %d\n", resourceUsage.getPlanRows()));
        report.append(String.format("• 평균 행 크기: %d bytes\n", resourceUsage.getPlanWidth()));
        
        report.append("\n💾 메모리 사용량:\n");
        report.append(String.format("• 버퍼 히트: %d blocks\n", resourceUsage.getSharedBlksHit()));
        report.append(String.format("• 디스크 읽기: %d blocks\n", resourceUsage.getSharedBlksRead()));
        report.append(String.format("• 디스크 쓰기: %d blocks\n", resourceUsage.getSharedBlksWritten()));
        
        report.append("\n📈 성능 분석:\n");
        report.append(String.format("• CPU 효율성: %.1f%%\n", metrics.getCpuIntensity()));
        report.append(String.format("• I/O 효율성: %.1f%%\n", metrics.getIoEfficiency()));
        report.append(String.format("• 메모리 효율성: %.1f%%\n", metrics.getMemoryEfficiency()));
        report.append(String.format("• 네트워크 효율성: %.1f%%\n", metrics.getNetworkEfficiency()));
        
        report.append(String.format("\n🏆 종합 성능 점수: %.1f%% (%s)\n", 
            metrics.getOverallPerformance(), metrics.getPerformanceGrade()));
        
        return report.toString();
    }
} 