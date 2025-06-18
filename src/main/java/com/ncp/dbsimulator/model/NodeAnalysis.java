package com.ncp.dbsimulator.model;

import java.util.List;

public class NodeAnalysis {
    private String nodeType;
    private int depth;
    private double totalCost;
    private double startupCost;
    private long planRows;
    private int planWidth;
    private double actualTotalTime;
    private long actualRows;
    private int actualLoops;
    private long sharedHitBlocks;
    private long sharedReadBlocks;
    private long sharedDirtiedBlocks;
    private long sharedWrittenBlocks;
    private String relationName;
    private String alias;
    private String joinType;
    private List<String> sortKeys;
    private String sortMethod;
    private long sortSpaceUsed;
    private String sortSpaceType;

    // Constructors
    public NodeAnalysis() {}

    // Getters and Setters
    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getStartupCost() {
        return startupCost;
    }

    public void setStartupCost(double startupCost) {
        this.startupCost = startupCost;
    }

    public long getPlanRows() {
        return planRows;
    }

    public void setPlanRows(long planRows) {
        this.planRows = planRows;
    }

    public int getPlanWidth() {
        return planWidth;
    }

    public void setPlanWidth(int planWidth) {
        this.planWidth = planWidth;
    }

    public double getActualTotalTime() {
        return actualTotalTime;
    }

    public void setActualTotalTime(double actualTotalTime) {
        this.actualTotalTime = actualTotalTime;
    }

    public long getActualRows() {
        return actualRows;
    }

    public void setActualRows(long actualRows) {
        this.actualRows = actualRows;
    }

    public int getActualLoops() {
        return actualLoops;
    }

    public void setActualLoops(int actualLoops) {
        this.actualLoops = actualLoops;
    }

    public long getSharedHitBlocks() {
        return sharedHitBlocks;
    }

    public void setSharedHitBlocks(long sharedHitBlocks) {
        this.sharedHitBlocks = sharedHitBlocks;
    }

    public long getSharedReadBlocks() {
        return sharedReadBlocks;
    }

    public void setSharedReadBlocks(long sharedReadBlocks) {
        this.sharedReadBlocks = sharedReadBlocks;
    }

    public long getSharedDirtiedBlocks() {
        return sharedDirtiedBlocks;
    }

    public void setSharedDirtiedBlocks(long sharedDirtiedBlocks) {
        this.sharedDirtiedBlocks = sharedDirtiedBlocks;
    }

    public long getSharedWrittenBlocks() {
        return sharedWrittenBlocks;
    }

    public void setSharedWrittenBlocks(long sharedWrittenBlocks) {
        this.sharedWrittenBlocks = sharedWrittenBlocks;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getJoinType() {
        return joinType;
    }

    public void setJoinType(String joinType) {
        this.joinType = joinType;
    }

    public List<String> getSortKeys() {
        return sortKeys;
    }

    public void setSortKeys(List<String> sortKeys) {
        this.sortKeys = sortKeys;
    }

    public String getSortMethod() {
        return sortMethod;
    }

    public void setSortMethod(String sortMethod) {
        this.sortMethod = sortMethod;
    }

    public long getSortSpaceUsed() {
        return sortSpaceUsed;
    }

    public void setSortSpaceUsed(long sortSpaceUsed) {
        this.sortSpaceUsed = sortSpaceUsed;
    }

    public String getSortSpaceType() {
        return sortSpaceType;
    }

    public void setSortSpaceType(String sortSpaceType) {
        this.sortSpaceType = sortSpaceType;
    }
} 