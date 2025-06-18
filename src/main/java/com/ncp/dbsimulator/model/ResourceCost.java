package com.ncp.dbsimulator.model;

public class ResourceCost {
    private double cpuCost;
    private double ioCost;
    private double memoryCost;
    private double networkCost;
    private double totalCost;
    private String analysisDetails;

    // Constructors
    public ResourceCost() {}

    // Getters and Setters
    public double getCpuCost() {
        return cpuCost;
    }

    public void setCpuCost(double cpuCost) {
        this.cpuCost = cpuCost;
    }

    public double getIoCost() {
        return ioCost;
    }

    public void setIoCost(double ioCost) {
        this.ioCost = ioCost;
    }

    public double getMemoryCost() {
        return memoryCost;
    }

    public void setMemoryCost(double memoryCost) {
        this.memoryCost = memoryCost;
    }

    public double getNetworkCost() {
        return networkCost;
    }

    public void setNetworkCost(double networkCost) {
        this.networkCost = networkCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getAnalysisDetails() {
        return analysisDetails;
    }

    public void setAnalysisDetails(String analysisDetails) {
        this.analysisDetails = analysisDetails;
    }
} 