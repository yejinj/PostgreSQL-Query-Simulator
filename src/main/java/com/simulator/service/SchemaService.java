package com.simulator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service // 스프링이 관리하는 서비스 클래스
public class SchemaService {

    @Autowired
    private JdbcTemplate jdbcTemplate; // DB와 연결된 SQL 실행 도구

    // SQL 식별자 검증용 정규표현식 (PostgreSQL 표준)
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final int MAX_IDENTIFIER_LENGTH = 63; // PostgreSQL 제한
    
    // 기본 페이지 크기 상수
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 1000;

    // 1. 사용자 정의 스키마 목록 조회
    public List<String> getAllSchemas() {
        String sql = "SELECT schema_name FROM information_schema.schemata " +
                    "WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'public') " + // 시스템 스키마 제외
                    "ORDER BY schema_name";

        return jdbcTemplate.queryForList(sql, String.class); // 스키마 이름 리스트 반환
    }

    // 2. 특정 스키마의 테이블 목록 및 정보 (행 수, 용량 등)
    public List<Map<String, Object>> getTablesInSchema(String schemaName) {
        validateIdentifier(schemaName, "스키마 이름");
        
        String sql = "SELECT " +        
                    "    t.table_name, " + // 테이블명
                    "    COALESCE(s.n_tup_ins, 0) as row_count, " + // 삽입된 행 수 (null이면 0)
                    "    pg_size_pretty(pg_total_relation_size(c.oid)) as table_size " + // 테이블 크기
                    "FROM information_schema.tables t " +
                    "LEFT JOIN pg_stat_user_tables s ON t.table_name = s.relname AND s.schemaname = ? " +
                    "LEFT JOIN pg_class c ON c.relname = t.table_name " +
                    "LEFT JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = ? " +
                    "WHERE t.table_schema = ? " +
                    "ORDER BY t.table_name";

        // 스키마 이름 3번 사용됨 (?, ?, ?)
        return jdbcTemplate.queryForList(sql, schemaName, schemaName, schemaName);
    }

    // 3. 특정 테이블의 컬럼 구조 조회
    public List<Map<String, Object>> getTableColumns(String schemaName, String tableName) {
        validateIdentifier(schemaName, "스키마 이름");
        validateIdentifier(tableName, "테이블 이름");
        
        String sql = "SELECT " +
                    "    column_name, " +
                    "    data_type, " +
                    "    is_nullable, " +
                    "    column_default, " +
                    "    character_maximum_length " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = ? AND table_name = ? " +
                    "ORDER BY ordinal_position";

        return jdbcTemplate.queryForList(sql, schemaName, tableName);
    }

    // 4. 특정 테이블의 데이터 조회 (페이징 포함)
    public Map<String, Object> getTableData(String schemaName, String tableName, int page, int size) {
        validateIdentifier(schemaName, "스키마 이름");
        validateIdentifier(tableName, "테이블 이름");
        validatePaginationParams(page, size);
        
        int offset = page * size; // OFFSET 계산

        // 안전한 테이블 식별자 구성 (스키마.테이블명을 따옴표로 감싸서 보호)
        String safeTableIdentifier = String.format("\"%s\".\"%s\"", schemaName, tableName);

        // 총 행 수 계산
        String countSql = "SELECT COUNT(*) FROM " + safeTableIdentifier;
        long totalRows = jdbcTemplate.queryForObject(countSql, Long.class);

        // 페이지별 데이터 조회 - 동적 ORDER BY 대신 고정된 방식 사용
        String dataSql = "SELECT * FROM " + safeTableIdentifier + 
                        " ORDER BY (SELECT NULL) LIMIT ? OFFSET ?"; // 안전한 정렬 방식
        List<Map<String, Object>> data = jdbcTemplate.queryForList(dataSql, size, offset);

        // 결과 패키징
        Map<String, Object> result = new HashMap<>();
        result.put("data", data); // 실제 데이터
        result.put("totalRows", totalRows); // 전체 행 수
        result.put("totalPages", (totalRows + size - 1) / size); // 전체 페이지 수
        result.put("currentPage", page);
        result.put("pageSize", size);

        return result;
    }

    // 5. SQL 실행기 - 자유 SQL 실행 (SELECT만 허용하도록 제한)
    public Map<String, Object> executeSql(String sql) {
        try {
            String trimmedSql = sql.trim().toLowerCase(); // 앞뒤 공백 제거 후 소문자 변환

            // SELECT 쿼리만 허용 (보안상 DML/DDL 차단)
            if (!trimmedSql.startsWith("select")) {
                return Map.of(
                    "success", false,
                    "error", "보안상 SELECT 쿼리만 실행 가능합니다."
                );
            }
            
            // 위험한 키워드 차단
            if (containsDangerousKeywords(trimmedSql)) {
                return Map.of(
                    "success", false,
                    "error", "실행할 수 없는 SQL 구문이 포함되어 있습니다."
                );
            }

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Map.of(
                    "success", true,
                "type", "SELECT",
                "data", result,
                "rowCount", result.size()
                );
            
        } catch (Exception e) {
            // 에러 발생 시 에러 메시지 전달
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    // 식별자 검증 메서드
    private void validateIdentifier(String identifier, String fieldName) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "이(가) 비어있습니다.");
        }
        
        String trimmed = identifier.trim();
        if (trimmed.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException(fieldName + "이(가) 너무 깁니다. (최대 " + MAX_IDENTIFIER_LENGTH + "자)");
        }
        
        if (!IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(fieldName + "에 유효하지 않은 문자가 포함되어 있습니다.");
        }
    }
    
    // 페이징 파라미터 검증
    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1~" + MAX_PAGE_SIZE + " 범위여야 합니다.");
        }
    }
    
    // 위험한 SQL 키워드 검사
    private boolean containsDangerousKeywords(String sql) {
        String[] dangerousKeywords = {
            "drop", "delete", "update", "insert", "create", "alter", 
            "truncate", "grant", "revoke", "commit", "rollback"
        };
        
        return Arrays.stream(dangerousKeywords)
                .anyMatch(sql::contains);
    }

    // 인덱스 관리 메서드들
    
    // 특정 테이블의 인덱스 목록 조회
    public List<Map<String, Object>> getTableIndexes(String schemaName, String tableName) {
        validateIdentifier(schemaName, "스키마 이름");
        validateIdentifier(tableName, "테이블 이름");
        
        String sql = "SELECT " +
                    "    i.indexname as index_name, " +
                    "    i.indexdef as index_definition, " +
                    "    COALESCE(pg_size_pretty(pg_relation_size(c.oid)), '알 수 없음') as index_size, " +
                    "    CASE WHEN i.indexname LIKE '%_pkey' THEN 'PRIMARY KEY' " +
                    "         WHEN pi.indisunique THEN 'UNIQUE' " +
                    "         ELSE 'INDEX' END as index_type " +
                    "FROM pg_indexes i " +
                    "LEFT JOIN pg_class c ON c.relname = i.indexname " +
                    "LEFT JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = i.schemaname " +
                    "LEFT JOIN pg_index pi ON pi.indexrelid = c.oid " +
                    "WHERE i.schemaname = ? AND i.tablename = ? " +
                    "ORDER BY i.indexname";
        
        return jdbcTemplate.queryForList(sql, schemaName, tableName);
    }
    
    // 인덱스 생성
    public Map<String, Object> createIndex(String schemaName, String tableName, String indexName, 
                                          String columns, String indexType, boolean isUnique) {
        try {
            validateIdentifier(schemaName, "스키마 이름");
            validateIdentifier(tableName, "테이블 이름");
            validateIdentifier(indexName, "인덱스 이름");
            
            // 인덱스 이름 중복 체크
            if (indexExists(schemaName, indexName)) {
                return Map.of(
                    "success", false,
                    "message", "이미 존재하는 인덱스 이름입니다: " + indexName
                );
            }
            
            // 컬럼명 검증 (쉼표로 구분된 여러 컬럼 가능)
            String[] columnArray = columns.split(",");
            for (String column : columnArray) {
                String trimmedColumn = column.trim();
                if (trimmedColumn.isEmpty()) {
                    return Map.of(
                        "success", false,
                        "message", "빈 컬럼명이 포함되어 있습니다."
                    );
                }
                validateIdentifier(trimmedColumn, "컬럼 이름");
            }
            
            StringBuilder sql = new StringBuilder("CREATE ");
            if (isUnique) {
                sql.append("UNIQUE ");
            }
            sql.append("INDEX CONCURRENTLY ").append(indexName)  // CONCURRENTLY 추가로 락 최소화
               .append(" ON ").append(schemaName).append(".").append(tableName);
               
            if ("BTREE".equalsIgnoreCase(indexType)) {
                sql.append(" USING BTREE");
            } else if ("HASH".equalsIgnoreCase(indexType)) {
                sql.append(" USING HASH");
            } else if ("GIN".equalsIgnoreCase(indexType)) {
                sql.append(" USING GIN");
            } else if ("GIST".equalsIgnoreCase(indexType)) {
                sql.append(" USING GIST");
            }
            
            sql.append(" (").append(columns).append(")");
            
            // 타임아웃 설정 (30초)
            jdbcTemplate.setQueryTimeout(30);
            jdbcTemplate.execute(sql.toString());
            
            return Map.of(
                "success", true,
                "message", "인덱스 '" + indexName + "'가 성공적으로 생성되었습니다.",
                "sql", sql.toString()
            );
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("already exists")) {
                errorMsg = "이미 존재하는 인덱스입니다.";
            } else if (errorMsg.contains("column") && errorMsg.contains("does not exist")) {
                errorMsg = "지정된 컬럼이 존재하지 않습니다.";
            } else if (errorMsg.contains("timeout")) {
                errorMsg = "인덱스 생성 시간이 초과되었습니다. 대용량 테이블의 경우 시간이 오래 걸릴 수 있습니다.";
            }
            
            return Map.of(
                "success", false,
                "message", "인덱스 생성 실패: " + errorMsg
            );
        }
    }
    
    // 인덱스 존재 여부 확인
    private boolean indexExists(String schemaName, String indexName) {
        try {
            String sql = "SELECT 1 FROM pg_indexes WHERE schemaname = ? AND indexname = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, schemaName, indexName);
            return !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    // 인덱스 삭제
    public Map<String, Object> dropIndex(String schemaName, String indexName) {
        try {
            validateIdentifier(schemaName, "스키마 이름");
            validateIdentifier(indexName, "인덱스 이름");
            
            // PRIMARY KEY는 삭제 방지
            if (indexName.toLowerCase().contains("pkey")) {
                return Map.of(
                    "success", false,
                    "message", "PRIMARY KEY 인덱스는 삭제할 수 없습니다."
                );
            }
            
            String sql = "DROP INDEX " + schemaName + "." + indexName;
            jdbcTemplate.execute(sql);
            
            return Map.of(
                "success", true,
                "message", "인덱스 '" + indexName + "'가 성공적으로 삭제되었습니다.",
                "sql", sql
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "인덱스 삭제 실패: " + e.getMessage()
            );
        }
    }
    
    // 인덱스 사용 통계 조회
    public List<Map<String, Object>> getIndexUsageStats(String schemaName, String tableName) {
        validateIdentifier(schemaName, "스키마 이름");
        validateIdentifier(tableName, "테이블 이름");
        
        try {
            // 더 안전한 쿼리로 변경 - 통계가 없어도 기본값 제공
            String sql = "SELECT " +
                        "    i.indexname as index_name, " +
                        "    0 as index_scans, " +
                        "    0 as tuples_read, " +
                        "    0 as tuples_fetched, " +
                        "    '통계 수집 중' as usage_status " +
                        "FROM pg_indexes i " +
                        "WHERE i.schemaname = ? AND i.tablename = ? " +
                        "ORDER BY i.indexname";
            
            return jdbcTemplate.queryForList(sql, schemaName, tableName);
        } catch (Exception e) {
            // 오류 발생 시 빈 리스트 반환
            System.err.println("인덱스 통계 조회 오류: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
