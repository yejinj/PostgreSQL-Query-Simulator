package com.ncp.dbsimulator.dto;

import com.ncp.dbsimulator.model.ExecutionPlan;
import com.ncp.dbsimulator.model.ResourceCost;

import java.util.List;

public class QueryAnalysisResponse {
    private ExecutionPlan executionPlan;
    private ResourceCost resourceCost;
    private List<String> suggestions;
    private boolean success;
    private String errorMessage;

    // Constructors
    public QueryAnalysisResponse() {}

    // Getters and Setters
    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }

    public ResourceCost getResourceCost() {
        return resourceCost;
    }

    public void setResourceCost(ResourceCost resourceCost) {
        this.resourceCost = resourceCost;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
} 