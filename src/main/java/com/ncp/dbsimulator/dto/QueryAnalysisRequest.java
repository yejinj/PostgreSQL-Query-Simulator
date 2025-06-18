package com.ncp.dbsimulator.dto;

public class QueryAnalysisRequest {
    private String query;
    private boolean includeBuffers = true;
    private boolean includeAnalyze = true;

    // Constructors
    public QueryAnalysisRequest() {}

    public QueryAnalysisRequest(String query) {
        this.query = query;
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isIncludeBuffers() {
        return includeBuffers;
    }

    public void setIncludeBuffers(boolean includeBuffers) {
        this.includeBuffers = includeBuffers;
    }

    public boolean isIncludeAnalyze() {
        return includeAnalyze;
    }

    public void setIncludeAnalyze(boolean includeAnalyze) {
        this.includeAnalyze = includeAnalyze;
    }
} 