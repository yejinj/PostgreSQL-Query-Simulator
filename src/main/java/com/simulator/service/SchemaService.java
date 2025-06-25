package com.simulator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getAllSchemas() {
        String sql = "SELECT schema_name FROM information_schema.schemata " +
                    "WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'public') " +
                    "ORDER BY schema_name";
        
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<Map<String, Object>> getTablesInSchema(String schemaName) {
        String sql = "SELECT " +
                    "    t.table_name, " +
                    "    COALESCE(s.n_tup_ins, 0) as row_count, " +
                    "    pg_size_pretty(pg_total_relation_size(c.oid)) as table_size " +
                    "FROM information_schema.tables t " +
                    "LEFT JOIN pg_stat_user_tables s ON t.table_name = s.relname AND s.schemaname = ? " +
                    "LEFT JOIN pg_class c ON c.relname = t.table_name " +
                    "LEFT JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = ? " +
                    "WHERE t.table_schema = ? " +
                    "ORDER BY t.table_name";
        
        return jdbcTemplate.queryForList(sql, schemaName, schemaName, schemaName);
    }

    public List<Map<String, Object>> getTableColumns(String schemaName, String tableName) {
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

    public Map<String, Object> getTableData(String schemaName, String tableName, int page, int size) {
        int offset = page * size;
        
        // 전체 행 수 조회
        String countSql = "SELECT COUNT(*) FROM " + schemaName + "." + tableName;
        long totalRows = jdbcTemplate.queryForObject(countSql, Long.class);
        
        // 페이지네이션된 데이터 조회
        String dataSql = "SELECT * FROM " + schemaName + "." + tableName + 
                        " ORDER BY 1 LIMIT ? OFFSET ?";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(dataSql, size, offset);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("totalRows", totalRows);
        result.put("totalPages", (totalRows + size - 1) / size);
        result.put("currentPage", page);
        result.put("pageSize", size);
        
        return result;
    }

    public Map<String, Object> executeSql(String sql) {
        try {
            // SQL이 SELECT 쿼리인지 확인
            String trimmedSql = sql.trim().toLowerCase();
            
            if (trimmedSql.startsWith("select")) {
                List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                return Map.of(
                    "success", true,
                    "type", "SELECT",
                    "data", result,
                    "rowCount", result.size()
                );
            } else {
                int affectedRows = jdbcTemplate.update(sql);
                return Map.of(
                    "success", true,
                    "type", "UPDATE",
                    "affectedRows", affectedRows
                );
            }
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
} 