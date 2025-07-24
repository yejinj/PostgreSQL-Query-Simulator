package com.simulator.controller;

import com.simulator.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller // 스프링 컨트롤러 지정
@RequestMapping("/schema") // "/schema"로 시작하는 모든 URL 처리
public class SchemaController {

    @Autowired
    private SchemaService schemaService; // 실제 DB에서 데이터 가져오는 서비스

    // 페이징 관련 상수
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 1000;

    // 0. 인덱스 생성 테스트 페이지
    @GetMapping("/test-index")
    public String testIndexPage() {
        return "test-index";
    }
    
    // 0-1. 간단한 테스트 페이지
    @GetMapping("/simple-test")
    public String simpleTest() {
        return "simple-test";
    }
    
    // 1. 스키마 브라우저 페이지
    @GetMapping("/browser")
    public String schemaBrowser(Model model) {
        try {
            List<String> schemas = schemaService.getAllSchemas(); // 스키마 이름들 조회
            model.addAttribute("schemas", schemas); // HTML 렌더링을 위한 모델 추가
            return "schema-browser"; // → templates/schema-browser.html
        } catch (Exception e) {
            model.addAttribute("error", "스키마 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("schemas", List.of()); // 빈 리스트라도 넘겨줘야 렌더링 오류 없음
            return "schema-browser";
        }
    }

    // 2. 특정 스키마의 테이블 목록 조회
    @GetMapping("/{schemaName}/tables")
    @ResponseBody
    public Map<String, Map<String, Object>> getTablesInSchema(@PathVariable String schemaName) {
        try {
            List<Map<String, Object>> tablesList = schemaService.getTablesInSchema(schemaName);
            
            // 테이블 이름 → 테이블 정보 매핑
            Map<String, Map<String, Object>> tablesMap = new java.util.HashMap<>();
            for (Map<String, Object> table : tablesList) {
                String tableName = (String) table.get("table_name");
                tablesMap.put(tableName, table);
            }
            return tablesMap; // JSON 형태로 반환
        } catch (Exception e) {
            throw new RuntimeException("테이블 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 3. 특정 테이블의 데이터 조회 페이지
    @GetMapping("/table/{schemaName}/{tableName}")
    public String tableData(@PathVariable String schemaName, 
                            @PathVariable String tableName,
                            @RequestParam(value = "page", defaultValue = "0") int page, // 페이징 파라미터
                            @RequestParam(value = "size", defaultValue = "50") int size,
                            Model model) {
        try {
            // 입력값 검증
            validatePageParams(page, size);
            
            // 해당 테이블의 일부 데이터 가져오기 (LIMIT / OFFSET 사용)
            Map<String, Object> tableInfo = schemaService.getTableData(schemaName, tableName, page, size);
            
            // 테이블 컬럼 목록 가져오기 (컬럼명, 타입 등)
            List<Map<String, Object>> columns = schemaService.getTableColumns(schemaName, tableName);

            // HTML 렌더링용 정보들 전달
            model.addAttribute("schemaName", schemaName);
            model.addAttribute("tableName", tableName);
            model.addAttribute("columns", columns);
            model.addAttribute("tableData", tableInfo);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            return "table-data"; // → templates/table-data.html
        } catch (Exception e) {
            model.addAttribute("error", "테이블 데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            return "table-data";
        }
    }

    // 4. SQL 직접 실행 API (SELECT만 허용)
    @PostMapping("/execute-sql")
    @ResponseBody
    public Map<String, Object> executeSql(@RequestParam String sql) {
        try {
            // SQL 입력값 기본 검증
            if (sql == null || sql.trim().isEmpty()) {
                return Map.of("error", "SQL 쿼리가 비어있습니다.");
            }
            
            if (sql.length() > 10000) { // SQL 길이 제한
                return Map.of("error", "SQL 쿼리가 너무 깁니다. (최대 10,000자)");
            }
            
            return schemaService.executeSql(sql); // 자유 쿼리 실행 결과 반환
        } catch (Exception e) {
            // 실패 시 에러 메시지 포함한 JSON 반환
            return Map.of("error", "SQL 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 페이지 파라미터 검증 메서드
    private void validatePageParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1~" + MAX_PAGE_SIZE + " 범위여야 합니다.");
        }
    }

    // 인덱스 관리 엔드포인트들
    
    // 5. 특정 테이블의 인덱스 목록 조회
    @GetMapping("/{schemaName}/{tableName}/indexes")
    @ResponseBody
    public Map<String, Object> getTableIndexes(@PathVariable String schemaName, 
                                               @PathVariable String tableName) {
        try {
            List<Map<String, Object>> indexes = schemaService.getTableIndexes(schemaName, tableName);
            List<Map<String, Object>> usageStats = schemaService.getIndexUsageStats(schemaName, tableName);
            
            return Map.of(
                "success", true,
                "indexes", indexes,
                "usageStats", usageStats
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "인덱스 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
    
    // 6. 인덱스 생성
    @PostMapping("/{schemaName}/{tableName}/indexes")
    @ResponseBody
    public Map<String, Object> createIndex(@PathVariable String schemaName,
                                          @PathVariable String tableName,
                                          @RequestParam String indexName,
                                          @RequestParam String columns,
                                          @RequestParam(defaultValue = "BTREE") String indexType,
                                          @RequestParam(defaultValue = "false") boolean isUnique) {
        try {
            System.out.println("=== 인덱스 생성 요청 받음 ===");
            System.out.println("Schema: " + schemaName);
            System.out.println("Table: " + tableName);
            System.out.println("Index Name: " + indexName);
            System.out.println("Columns: " + columns);
            System.out.println("Type: " + indexType);
            System.out.println("Unique: " + isUnique);
            
            Map<String, Object> result = schemaService.createIndex(schemaName, tableName, indexName, columns, indexType, isUnique);
            System.out.println("인덱스 생성 결과: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("인덱스 생성 오류: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                "success", false,
                "message", "인덱스 생성 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
    
    // 7. 인덱스 삭제
    @DeleteMapping("/{schemaName}/indexes/{indexName}")
    @ResponseBody
    public Map<String, Object> dropIndex(@PathVariable String schemaName,
                                        @PathVariable String indexName) {
        try {
            return schemaService.dropIndex(schemaName, indexName);
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "인덱스 삭제 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
}
