package com.simulator.model;

import jakarta.validation.constraints.NotBlank;

public class QueryAnalysisRequest {
    
    @NotBlank(message = "SQL 쿼리는 필수입니다")
    private String sqlQuery;
    
    private String schemaName;
    
    public QueryAnalysisRequest() {}
    
    public QueryAnalysisRequest(String sqlQuery, String schemaName) {
        this.sqlQuery = sqlQuery;
        this.schemaName = schemaName;
    }
    
    public String getSqlQuery() {
        return sqlQuery;
    }
    
    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
} 