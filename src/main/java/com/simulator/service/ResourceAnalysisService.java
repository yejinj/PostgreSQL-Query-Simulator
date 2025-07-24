package com.simulator.service;

import com.simulator.model.QueryAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceAnalysisService {
    
    // 성능 점수 상수
    private static final double EXCELLENT_SCORE = 95.0;
    private static final double GOOD_SCORE = 85.0;
    private static final double AVERAGE_SCORE = 70.0;
    private static final double POOR_SCORE = 50.0;
    private static final double BAD_SCORE = 30.0;
    
    // 실행 시간 임계값 (밀리초)
    private static final double FAST_TIME_MS = 10.0;
    private static final double MEDIUM_TIME_MS = 100.0;
    private static final double SLOW_TIME_MS = 1000.0;
    private static final double VERY_SLOW_TIME_MS = 5000.0;
    
    // 메모리 효율성 임계값
    private static final double MEMORY_EFFICIENT_HITS_PER_ROW = 1.0;
    private static final double MEMORY_AVERAGE_HITS_PER_ROW = 5.0;
    private static final double MEMORY_POOR_HITS_PER_ROW = 10.0;
    private static final double MEMORY_BAD_HITS_PER_ROW = 20.0;
    
    // 네트워크 효율성 임계값 (바이트)
    private static final long NETWORK_EFFICIENT_ROW_SIZE = 100L;
    private static final long NETWORK_GOOD_ROW_SIZE = 500L;
    private static final long NETWORK_AVERAGE_ROW_SIZE = 1000L;
    private static final long NETWORK_POOR_ROW_SIZE = 2000L;
    
    // 가중치 상수
    private static final double CPU_WEIGHT = 0.3;
    private static final double IO_WEIGHT = 0.4;
    private static final double MEMORY_WEIGHT = 0.2;
    private static final double NETWORK_WEIGHT = 0.1;
    
    // 성능 등급 기준점
    private static final double GRADE_A_PLUS = 90.0;
    private static final double GRADE_A = 80.0;
    private static final double GRADE_B_PLUS = 70.0;
    private static final double GRADE_B = 60.0;
    private static final double GRADE_C_PLUS = 50.0;
    private static final double GRADE_C = 40.0;
    private static final double GRADE_D = 30.0;
    
    // 병목 감지 임계값
    private static final double CPU_BOTTLENECK_THRESHOLD = 80.0;    // CPU 사용률 80% 이상
    private static final double IO_BOTTLENECK_THRESHOLD = 50.0;     // I/O 대기 50ms 이상
    private static final long MEMORY_BOTTLENECK_THRESHOLD = 1048576L; // 1MB 이상 메모리 사용
    private static final long DISK_IO_BOTTLENECK_THRESHOLD = 1000L;    // 1000 블록 이상 디스크 I/O
    
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
    
    // 시간별 메트릭을 분석하여 병목 지점 감지 (고급 분석)
    public List<QueryAnalysisResult.BottleneckPoint> detectBottlenecks(
            QueryAnalysisResult.TimeSeriesMetrics timeSeriesMetrics,
            QueryAnalysisResult.ResourceUsage resourceUsage) {
        
        List<QueryAnalysisResult.BottleneckPoint> bottlenecks = new ArrayList<>();
        
        if (timeSeriesMetrics == null || timeSeriesMetrics.getTimePoints() == null) {
            return bottlenecks;
        }
        
        List<QueryAnalysisResult.TimePoint> timePoints = timeSeriesMetrics.getTimePoints();
        
        // 1. 급격한 변화 지점 감지 (이전 값과 비교)
        for (int i = 1; i < timePoints.size(); i++) {
            QueryAnalysisResult.TimePoint current = timePoints.get(i);
            QueryAnalysisResult.TimePoint previous = timePoints.get(i-1);
            
            // CPU 급증 감지 (50% 이상 증가)
            double cpuDelta = current.getCpuUsage() - previous.getCpuUsage();
            if (cpuDelta > 50.0) {
                String detailedDesc = String.format("%s → %s 작업 전환 시점에서 CPU가 %.1f%%에서 %.1f%%로 급증 (%.1f%% 증가)", 
                    previous.getOperationType(), current.getOperationType(), 
                    previous.getCpuUsage(), current.getCpuUsage(), cpuDelta);
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    current.getTimestamp(),
                    "CPU_SPIKE",
                    Math.min(100.0, cpuDelta * 2), // 급증 정도에 따른 심각도
                    detailedDesc
                );
                bottleneck.setAffectedOperation(current.getOperationType());
                bottleneck.setRecommendation(generateCpuSpikeRecommendation(previous.getOperationType(), current.getOperationType(), cpuDelta));
                bottleneck.setDurationMs(current.getTimestamp() - previous.getTimestamp());
                bottlenecks.add(bottleneck);
            }
            
            // I/O 급증 감지 (30ms 이상 증가)
            double ioDelta = current.getIoWaitTime() - previous.getIoWaitTime();
            if (ioDelta > 30.0) {
                String detailedDesc = String.format("%s → %s 작업에서 I/O 대기가 %.1fms에서 %.1fms로 급증 (+%.1fms)", 
                    previous.getOperationType(), current.getOperationType(),
                    previous.getIoWaitTime(), current.getIoWaitTime(), ioDelta);
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    current.getTimestamp(),
                    "IO_SPIKE",
                    Math.min(100.0, ioDelta * 3), // I/O 급증 정도에 따른 심각도
                    detailedDesc
                );
                bottleneck.setAffectedOperation(current.getOperationType());
                bottleneck.setRecommendation(generateIoSpikeRecommendation(previous.getOperationType(), current.getOperationType(), ioDelta));
                bottleneck.setDurationMs(current.getTimestamp() - previous.getTimestamp());
                bottlenecks.add(bottleneck);
            }
            
            // 메모리 급증 감지 (500MB 이상 증가)
            long memoryDelta = current.getMemoryUsage() - previous.getMemoryUsage();
            if (memoryDelta > 500 * 1024 * 1024) { // 500MB
                String detailedDesc = String.format("%s → %s 작업에서 메모리가 %.1fMB에서 %.1fMB로 급증 (+%.1fMB)", 
                    previous.getOperationType(), current.getOperationType(),
                    previous.getMemoryUsage() / 1024.0 / 1024.0, 
                    current.getMemoryUsage() / 1024.0 / 1024.0, 
                    memoryDelta / 1024.0 / 1024.0);
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    current.getTimestamp(),
                    "MEMORY_SPIKE",
                    Math.min(100.0, (memoryDelta / 1024.0 / 1024.0) / 10.0), // 메모리 증가량에 따른 심각도
                    detailedDesc
                );
                bottleneck.setAffectedOperation(current.getOperationType());
                bottleneck.setRecommendation(generateMemorySpikeRecommendation(current.getOperationType(), memoryDelta));
                bottleneck.setDurationMs(current.getTimestamp() - previous.getTimestamp());
                bottlenecks.add(bottleneck);
            }
        }
        
        // 2. 절대값 기준 병목 감지
        for (int i = 0; i < timePoints.size(); i++) {
            QueryAnalysisResult.TimePoint point = timePoints.get(i);
            
            // 높은 CPU 사용률 
            if (point.getCpuUsage() > CPU_BOTTLENECK_THRESHOLD) {
                String recommendation = generateCpuRecommendation(point.getOperationType(), point.getCpuUsage());
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    point.getTimestamp(),
                    "HIGH_CPU",
                    calculateSeverity(point.getCpuUsage(), CPU_BOTTLENECK_THRESHOLD, 100.0),
                    String.format("%s 작업에서 CPU 사용률이 %.1f%%로 높습니다", 
                                  point.getOperationType(), point.getCpuUsage())
                );
                bottleneck.setAffectedOperation(point.getOperationType());
                bottleneck.setRecommendation(recommendation);
                bottleneck.setDurationMs(calculateDuration(timePoints, i));
                bottlenecks.add(bottleneck);
            }
            
            // 높은 I/O 대기 시간
            if (point.getIoWaitTime() > IO_BOTTLENECK_THRESHOLD) {
                String recommendation = generateIoRecommendation(point.getOperationType(), point.getIoWaitTime());
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    point.getTimestamp(),
                    "HIGH_IO",
                    calculateSeverity(point.getIoWaitTime(), IO_BOTTLENECK_THRESHOLD, 200.0),
                    String.format("%s 작업에서 I/O 대기 시간이 %.1fms로 높습니다", 
                                  point.getOperationType(), point.getIoWaitTime())
                );
                bottleneck.setAffectedOperation(point.getOperationType());
                bottleneck.setRecommendation(recommendation);
                bottleneck.setDurationMs(calculateDuration(timePoints, i));
                bottlenecks.add(bottleneck);
            }
            
            // 3. 메모리 병목 감지 (대용량 데이터 처리)
            if (point.getMemoryUsage() > MEMORY_BOTTLENECK_THRESHOLD) {
                String recommendation = generateMemoryRecommendation(point.getOperationType(), point.getMemoryUsage());
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    point.getTimestamp(),
                    "MEMORY",
                    calculateSeverity(point.getMemoryUsage(), MEMORY_BOTTLENECK_THRESHOLD, MEMORY_BOTTLENECK_THRESHOLD * 10),
                    String.format("%s 작업에서 메모리 사용량이 %.1fMB로 높습니다", 
                                  point.getOperationType(), point.getMemoryUsage() / 1024.0 / 1024.0)
                );
                bottleneck.setAffectedOperation(point.getOperationType());
                bottleneck.setRecommendation(recommendation);
                bottleneck.setDurationMs(calculateDuration(timePoints, i));
                bottlenecks.add(bottleneck);
            }
            
            // 4. 디스크 I/O 병목 감지 (읽기/쓰기 패턴 분석)
            long totalDiskIO = point.getDiskReads() + point.getDiskWrites();
            if (totalDiskIO > DISK_IO_BOTTLENECK_THRESHOLD) {
                String recommendation = generateDiskIoRecommendation(point.getOperationType(), 
                                                                     point.getDiskReads(), 
                                                                     point.getDiskWrites());
                
                QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                    point.getTimestamp(),
                    "DISK_IO",
                    calculateSeverity(totalDiskIO, DISK_IO_BOTTLENECK_THRESHOLD, DISK_IO_BOTTLENECK_THRESHOLD * 5),
                    String.format("%s 작업에서 디스크 I/O가 %d 블록 (읽기: %d, 쓰기: %d)으로 높습니다", 
                                  point.getOperationType(), totalDiskIO, point.getDiskReads(), point.getDiskWrites())
                );
                bottleneck.setAffectedOperation(point.getOperationType());
                bottleneck.setRecommendation(recommendation);
                bottleneck.setDurationMs(calculateDuration(timePoints, i));
                bottlenecks.add(bottleneck);
            }
            
            // 5. 특정 노드 타입별 특수 병목 감지
            detectSpecialBottlenecks(point, bottlenecks, i, timePoints);
        }
        
        // 심각도 기준으로 정렬 (높은 순)
        bottlenecks.sort((a, b) -> Double.compare(b.getSeverity(), a.getSeverity()));
        
        // 중복 병목 제거 (같은 시간대, 같은 타입)
        return removeDuplicateBottlenecks(bottlenecks);
    }
    
    // 심각도 계산 (0-100)
    private double calculateSeverity(double actualValue, double threshold, double maxValue) {
        if (actualValue <= threshold) return 0.0;
        double normalizedValue = Math.min(actualValue, maxValue);
        return ((normalizedValue - threshold) / (maxValue - threshold)) * 100.0;
    }
    
    // 병목 지속 시간 계산
    private double calculateDuration(List<QueryAnalysisResult.TimePoint> timePoints, int currentIndex) {
        if (currentIndex >= timePoints.size() - 1) {
            return 1.0; // 마지막 포인트면 1ms로 가정
        }
        
        double currentTime = timePoints.get(currentIndex).getTimestamp();
        double nextTime = timePoints.get(currentIndex + 1).getTimestamp();
        return Math.max(1.0, nextTime - currentTime);
    }
    
    // CPU 급증에 대한 구체적인 권장사항 생성
    private String generateCpuSpikeRecommendation(String fromOperation, String toOperation, double cpuDelta) {
        return String.format("%s에서 %s로 전환 시 CPU가 %.1f%% 급증했습니다. " +
            "조인 순서를 변경하거나 중간 결과를 줄이는 WHERE 조건을 추가하세요.", 
            fromOperation, toOperation, cpuDelta);
    }
    
    // I/O 급증에 대한 구체적인 권장사항 생성
    private String generateIoSpikeRecommendation(String fromOperation, String toOperation, double ioDelta) {
        return String.format("%s에서 %s로 전환 시 I/O가 %.1fms 급증했습니다. " +
            "인덱스를 추가하거나 메모리 설정(work_mem, shared_buffers)을 늘려 디스크 액세스를 줄이세요.", 
            fromOperation, toOperation, ioDelta);
    }
    
    // 메모리 급증에 대한 구체적인 권장사항 생성
    private String generateMemorySpikeRecommendation(String operationType, long memoryDelta) {
        double deltaMB = memoryDelta / 1024.0 / 1024.0;
        return String.format("%s 작업에서 메모리가 %.1fMB 급증했습니다. " +
            "LIMIT 절 추가, 부분 집계 사용, 또는 work_mem 설정을 조정하여 메모리 사용을 최적화하세요.", 
            operationType, deltaMB);
    }
    
    private double analyzeCpuIntensity(double actualTimeMs) {
        // CPU 집약도 분석 (낮을수록 좋음)
        if (actualTimeMs < FAST_TIME_MS) return EXCELLENT_SCORE;
        if (actualTimeMs < MEDIUM_TIME_MS) return GOOD_SCORE;
        if (actualTimeMs < SLOW_TIME_MS) return AVERAGE_SCORE;
        if (actualTimeMs < VERY_SLOW_TIME_MS) return POOR_SCORE;
        return BAD_SCORE;
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
        if (memoryHitsPerRow < MEMORY_EFFICIENT_HITS_PER_ROW) return EXCELLENT_SCORE;
        if (memoryHitsPerRow < MEMORY_AVERAGE_HITS_PER_ROW) return 80.0;
        if (memoryHitsPerRow < MEMORY_POOR_HITS_PER_ROW) return 65.0;
        if (memoryHitsPerRow < MEMORY_BAD_HITS_PER_ROW) return POOR_SCORE;
        return BAD_SCORE;
    }
    
    private double analyzeNetworkEfficiency(QueryAnalysisResult.ResourceUsage resourceUsage) {
        long planRows = resourceUsage.getPlanRows();
        long planWidth = resourceUsage.getPlanWidth();
        
        if (planRows == 0) return 100.0;
        
        // 행당 데이터 크기 분석
        long avgRowSize = planWidth;
        
        if (avgRowSize < NETWORK_EFFICIENT_ROW_SIZE) return EXCELLENT_SCORE;
        if (avgRowSize < NETWORK_GOOD_ROW_SIZE) return 80.0;
        if (avgRowSize < NETWORK_AVERAGE_ROW_SIZE) return 65.0;
        if (avgRowSize < NETWORK_POOR_ROW_SIZE) return POOR_SCORE;
        return BAD_SCORE;
    }
    
    private double calculateOverallPerformance(double cpu, double io, double memory, double network) {
        // 가중 평균 계산 (CPU와 I/O에 더 높은 가중치)
        return (cpu * CPU_WEIGHT + io * IO_WEIGHT + memory * MEMORY_WEIGHT + network * NETWORK_WEIGHT);
    }
    
    private String getPerformanceGrade(double score) {
        if (score >= GRADE_A_PLUS) return "A+";
        if (score >= GRADE_A) return "A";
        if (score >= GRADE_B_PLUS) return "B+";
        if (score >= GRADE_B) return "B";
        if (score >= GRADE_C_PLUS) return "C+";
        if (score >= GRADE_C) return "C";
        if (score >= GRADE_D) return "D";
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

    // CPU 병목에 대한 구체적인 권장사항 생성
    private String generateCpuRecommendation(String operationType, double cpuUsage) {
        switch (operationType) {
            case "Seq Scan":
                return "인덱스 추가를 통해 Full Table Scan을 피하세요. WHERE 절의 조건 컬럼에 인덱스를 생성하세요.";
            case "Nested Loop":
                return "조인 키에 인덱스를 추가하거나 Hash Join으로 변경하세요. 작은 테이블을 먼저 조인하세요.";
            case "Sort":
                return "ORDER BY 절을 제거하거나 인덱스를 사용한 정렬을 고려하세요. work_mem 설정을 확인하세요.";
            case "Hash":
                return "Hash 테이블 크기를 줄이기 위해 WHERE 절로 데이터를 필터링하세요.";
            case "Aggregate":
                return "GROUP BY 절의 컬럼에 인덱스를 추가하거나 HAVING 절을 WHERE 절로 변경하세요.";
            default:
                return "쿼리 조건을 최적화하거나 인덱스 사용을 고려하세요.";
        }
    }
    
    // I/O 병목에 대한 구체적인 권장사항 생성
    private String generateIoRecommendation(String operationType, double ioWaitTime) {
        switch (operationType) {
            case "Seq Scan":
                return "인덱스를 추가하여 디스크 읽기를 줄이세요. 필요한 컬럼만 SELECT하세요.";
            case "Index Scan":
                return "커버링 인덱스를 사용하여 테이블 액세스를 줄이세요. 복합 인덱스를 고려하세요.";
            case "Bitmap Heap Scan":
                return "인덱스 선택도를 높이기 위해 조건을 추가하거나 인덱스를 조정하세요.";
            case "Hash Join":
                return "조인 조건을 최적화하거나 작은 테이블을 해시 테이블로 사용하세요.";
            default:
                return "SSD 사용을 고려하거나 shared_buffers 설정을 증가시키세요.";
        }
    }
    
    // 메모리 병목에 대한 구체적인 권장사항 생성
    private String generateMemoryRecommendation(String operationType, long memoryUsage) {
        switch (operationType) {
            case "Sort":
                return "work_mem 설정을 증가시키거나 ORDER BY 절을 제거하세요. 인덱스 기반 정렬을 고려하세요.";
            case "Hash":
                return "Hash 조인 대신 Nested Loop 조인을 고려하거나 조건을 추가하여 데이터를 줄이세요.";
            case "Aggregate":
                return "GROUP BY 절의 데이터 양을 줄이거나 파티션 기반 집계를 고려하세요.";
            default:
                return "LIMIT 절을 추가하거나 결과셋 크기를 줄이세요. 메모리 설정을 확인하세요.";
        }
    }
    
    // 디스크 I/O 병목에 대한 구체적인 권장사항 생성
    private String generateDiskIoRecommendation(String operationType, long diskReads, long diskWrites) {
        if (diskReads > diskWrites * 10) {
            // 읽기 집약적인 경우
            switch (operationType) {
                case "Seq Scan":
                    return "인덱스를 추가하여 읽기 블록 수를 줄이세요. 필요한 컬럼만 SELECT하세요.";
                case "Index Scan":
                    return "커버링 인덱스를 사용하여 테이블 액세스를 제거하세요.";
                default:
                    return "shared_buffers를 증가시키거나 인덱스를 최적화하세요.";
            }
        } else if (diskWrites > diskReads * 5) {
            // 쓰기 집약적인 경우
            return "배치 처리를 사용하거나 트랜잭션 크기를 조정하세요. checkpoint 설정을 확인하세요.";
        } else {
            // 읽기/쓰기 혼합
            return "인덱스 최적화와 함께 버퍼 풀 크기를 증가시키세요.";
        }
    }
    
    // 특정 노드 타입별 특수 병목 감지
    private void detectSpecialBottlenecks(QueryAnalysisResult.TimePoint point, 
                                         List<QueryAnalysisResult.BottleneckPoint> bottlenecks,
                                         int index, List<QueryAnalysisResult.TimePoint> timePoints) {
        String operationType = point.getOperationType();
        
        // 1. Nested Loop 조인에서 내부 테이블 스캔이 반복되는 경우
        if (operationType.equals("Nested Loop") && point.getCpuUsage() > 60.0) {
            QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                point.getTimestamp(),
                "JOIN_INEFFICIENCY",
                85.0,
                "Nested Loop 조인에서 비효율적인 반복 스캔이 발생하고 있습니다"
            );
            bottleneck.setAffectedOperation(operationType);
            bottleneck.setRecommendation("내부 테이블의 조인 키에 인덱스를 추가하거나 Hash Join으로 변경하세요");
            bottleneck.setDurationMs(calculateDuration(timePoints, index));
            bottlenecks.add(bottleneck);
        }
        
        // 2. Sort 노드에서 디스크 정렬이 발생하는 경우 
        if (operationType.equals("Sort") && point.getIoWaitTime() > 20.0) {
            QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                point.getTimestamp(),
                "DISK_SORT",
                80.0,
                "정렬 작업이 디스크에서 수행되어 성능이 저하됩니다"
            );
            bottleneck.setAffectedOperation(operationType);
            bottleneck.setRecommendation("work_mem 설정을 증가시키거나 인덱스를 사용한 정렬을 고려하세요");
            bottleneck.setDurationMs(calculateDuration(timePoints, index));
            bottlenecks.add(bottleneck);
        }
        
        // 3. Hash 조인에서 메모리 부족으로 인한 성능 저하
        if (operationType.equals("Hash") && point.getMemoryUsage() > MEMORY_BOTTLENECK_THRESHOLD * 2) {
            QueryAnalysisResult.BottleneckPoint bottleneck = new QueryAnalysisResult.BottleneckPoint(
                point.getTimestamp(),
                "HASH_MEMORY",
                75.0,
                "Hash 테이블 크기가 너무 커서 메모리 부족이 발생합니다"
            );
            bottleneck.setAffectedOperation(operationType);
            bottleneck.setRecommendation("work_mem을 증가시키거나 작은 테이블을 해시 테이블로 사용하세요");
            bottleneck.setDurationMs(calculateDuration(timePoints, index));
            bottlenecks.add(bottleneck);
        }
    }
    
    // 중복 병목 제거
    private List<QueryAnalysisResult.BottleneckPoint> removeDuplicateBottlenecks(
            List<QueryAnalysisResult.BottleneckPoint> bottlenecks) {
        List<QueryAnalysisResult.BottleneckPoint> uniqueBottlenecks = new ArrayList<>();
        
        for (QueryAnalysisResult.BottleneckPoint bottleneck : bottlenecks) {
            boolean isDuplicate = false;
            
            for (QueryAnalysisResult.BottleneckPoint existing : uniqueBottlenecks) {
                // 같은 시간대(±5ms), 같은 타입의 병목은 중복으로 간주
                if (Math.abs(bottleneck.getTimestamp() - existing.getTimestamp()) < 5.0 &&
                    bottleneck.getBottleneckType().equals(existing.getBottleneckType())) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                uniqueBottlenecks.add(bottleneck);
            }
        }
        
        return uniqueBottlenecks;
    }
} 