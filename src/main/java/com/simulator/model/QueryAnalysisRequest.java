package com.simulator.model;

import jakarta.validation.constraints.NotBlank;

public class QueryAnalysisRequest {
    // 사용자가 입력한 SQL 쿼리. 빈 값이면 안 됨
    @NotBlank(message = "SQL 쿼리는 필수입니다")
    private String sqlQuery;

    // 실행할 대상 스키마 (선택 사항)
    private String schemaName;

    // 기본 생성자 (폼 바인딩 등에서 필요)
    public QueryAnalysisRequest() {}

    // 생성자 - 필드 두 개 초기화
    public QueryAnalysisRequest(String sqlQuery, String schemaName) {
        this.sqlQuery = sqlQuery;
        this.schemaName = schemaName;
    }

    // getter / setter : 값을 읽거나 저장하는 데 필요함 (폼, JSON 바인딩용)
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
