package com.simulator.model;

import java.math.BigDecimal;
import java.util.List;

public class QueryAnalysisResult {
    
    private String originalQuery;
    private String executionPlan;
    private ResourceUsage resourceUsage;
    private ResourceMetrics resourceMetrics;
    private List<OptimizationSuggestion> optimizationSuggestions;
    
    public QueryAnalysisResult() {}
    
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }
    
    public String getExecutionPlan() {
        return executionPlan;
    }
    
    public void setExecutionPlan(String executionPlan) {
        this.executionPlan = executionPlan;
    }
    
    public ResourceUsage getResourceUsage() {
        return resourceUsage;
    }
    
    public void setResourceUsage(ResourceUsage resourceUsage) {
        this.resourceUsage = resourceUsage;
    }
    
    public ResourceMetrics getResourceMetrics() {
        return resourceMetrics;
    }
    
    public void setResourceMetrics(ResourceMetrics resourceMetrics) {
        this.resourceMetrics = resourceMetrics;
    }
    
    public List<OptimizationSuggestion> getOptimizationSuggestions() {
        return optimizationSuggestions;
    }
    
    public void setOptimizationSuggestions(List<OptimizationSuggestion> optimizationSuggestions) {
        this.optimizationSuggestions = optimizationSuggestions;
    }
    
    // 내부 클래스들
    public static class ResourceUsage {
        private long totalCost;
        private long planRows;
        private long planWidth;
        private double actualTime;
        private long sharedBlksHit;
        private long sharedBlksRead;
        private long sharedBlksDirtied;
        private long sharedBlksWritten;
        
        // getters and setters
        public long getTotalCost() { return totalCost; }
        public void setTotalCost(long totalCost) { this.totalCost = totalCost; }
        
        public long getPlanRows() { return planRows; }
        public void setPlanRows(long planRows) { this.planRows = planRows; }
        
        public long getPlanWidth() { return planWidth; }
        public void setPlanWidth(long planWidth) { this.planWidth = planWidth; }
        
        public double getActualTime() { return actualTime; }
        public void setActualTime(double actualTime) { this.actualTime = actualTime; }
        
        public long getSharedBlksHit() { return sharedBlksHit; }
        public void setSharedBlksHit(long sharedBlksHit) { this.sharedBlksHit = sharedBlksHit; }
        
        public long getSharedBlksRead() { return sharedBlksRead; }
        public void setSharedBlksRead(long sharedBlksRead) { this.sharedBlksRead = sharedBlksRead; }
        
        public long getSharedBlksDirtied() { return sharedBlksDirtied; }
        public void setSharedBlksDirtied(long sharedBlksDirtied) { this.sharedBlksDirtied = sharedBlksDirtied; }
        
        public long getSharedBlksWritten() { return sharedBlksWritten; }
        public void setSharedBlksWritten(long sharedBlksWritten) { this.sharedBlksWritten = sharedBlksWritten; }
    }
    
    public static class ResourceMetrics {
        private double cpuIntensity;
        private double ioEfficiency;
        private double memoryEfficiency;
        private double networkEfficiency;
        private double overallPerformance;
        private String performanceGrade;
        
        // getters and setters
        public double getCpuIntensity() { return cpuIntensity; }
        public void setCpuIntensity(double cpuIntensity) { this.cpuIntensity = cpuIntensity; }
        
        public double getIoEfficiency() { return ioEfficiency; }
        public void setIoEfficiency(double ioEfficiency) { this.ioEfficiency = ioEfficiency; }
        
        public double getMemoryEfficiency() { return memoryEfficiency; }
        public void setMemoryEfficiency(double memoryEfficiency) { this.memoryEfficiency = memoryEfficiency; }
        
        public double getNetworkEfficiency() { return networkEfficiency; }
        public void setNetworkEfficiency(double networkEfficiency) { this.networkEfficiency = networkEfficiency; }
        
        public double getOverallPerformance() { return overallPerformance; }
        public void setOverallPerformance(double overallPerformance) { this.overallPerformance = overallPerformance; }
        
        public String getPerformanceGrade() { return performanceGrade; }
        public void setPerformanceGrade(String performanceGrade) { this.performanceGrade = performanceGrade; }
    }
    
    public static class OptimizationSuggestion {
        private String type;
        private String description;
        private String suggestedQuery;
        private double expectedPerformanceImprovement;
        
        public OptimizationSuggestion(String type, String description) {
            this.type = type;
            this.description = description;
        }
        
        // getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSuggestedQuery() { return suggestedQuery; }
        public void setSuggestedQuery(String suggestedQuery) { this.suggestedQuery = suggestedQuery; }
        
        public double getExpectedPerformanceImprovement() { return expectedPerformanceImprovement; }
        public void setExpectedPerformanceImprovement(double expectedPerformanceImprovement) { 
            this.expectedPerformanceImprovement = expectedPerformanceImprovement; 
        }
    }
} 