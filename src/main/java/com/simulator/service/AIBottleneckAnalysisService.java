package com.simulator.service;

import com.simulator.model.QueryAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIBottleneckAnalysisService {

    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;

    // 기본 분석 메서드 (기존 호환성 유지)
    public QueryAnalysisResult.AIBottleneckAnalysis analyzeBottlenecks(
            String query, 
            String executionPlan, 
            long executionTime,
            String resourceUsage) {
        
        return openAIAnalysisService.analyzeBottlenecks(query, executionPlan, executionTime, resourceUsage);
    }

    // 고급 분석 메서드 (병목 지점과 시간별 메트릭 포함)
    public QueryAnalysisResult.AIBottleneckAnalysis analyzeBottlenecksWithMetrics(
            String query, 
            String executionPlan, 
            long executionTime,
            String resourceUsage,
            List<QueryAnalysisResult.BottleneckPoint> bottleneckPoints,
            QueryAnalysisResult.TimeSeriesMetrics timeSeriesMetrics) {
        
        return openAIAnalysisService.analyzeBottlenecksWithMetrics(
            query, executionPlan, executionTime, resourceUsage, bottleneckPoints, timeSeriesMetrics);
    }
} 