package com.ncp.dbsimulator.model;

import java.util.List;

public class ExecutionPlan {
    private String originalQuery;
    private String jsonPlan;
    private double totalCost;
    private double actualTotalTime;
    private List<NodeAnalysis> nodeAnalyses;
    private long totalBufferHits;
    private long totalBufferReads;
    private double estimatedResourceCost;

    // Constructors
    public ExecutionPlan() {}

    // Getters and Setters
    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public String getJsonPlan() {
        return jsonPlan;
    }

    public void setJsonPlan(String jsonPlan) {
        this.jsonPlan = jsonPlan;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getActualTotalTime() {
        return actualTotalTime;
    }

    public void setActualTotalTime(double actualTotalTime) {
        this.actualTotalTime = actualTotalTime;
    }

    public List<NodeAnalysis> getNodeAnalyses() {
        return nodeAnalyses;
    }

    public void setNodeAnalyses(List<NodeAnalysis> nodeAnalyses) {
        this.nodeAnalyses = nodeAnalyses;
    }

    public long getTotalBufferHits() {
        return totalBufferHits;
    }

    public void setTotalBufferHits(long totalBufferHits) {
        this.totalBufferHits = totalBufferHits;
    }

    public long getTotalBufferReads() {
        return totalBufferReads;
    }

    public void setTotalBufferReads(long totalBufferReads) {
        this.totalBufferReads = totalBufferReads;
    }

    public double getEstimatedResourceCost() {
        return estimatedResourceCost;
    }

    public void setEstimatedResourceCost(double estimatedResourceCost) {
        this.estimatedResourceCost = estimatedResourceCost;
    }
} 