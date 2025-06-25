package com.simulator.controller;

import com.simulator.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @GetMapping("/browser")
    public String schemaBrowser(Model model) {
        try {
            List<String> schemas = schemaService.getAllSchemas();
            model.addAttribute("schemas", schemas);
            
            return "schema-browser";
        } catch (Exception e) {
            model.addAttribute("error", "스키마 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("schemas", List.of()); // 빈 리스트 제공
            return "schema-browser";
        }
    }

    @GetMapping("/{schemaName}/tables")
    @ResponseBody
    public Map<String, Map<String, Object>> getTablesInSchema(@PathVariable String schemaName) {
        try {
            List<Map<String, Object>> tablesList = schemaService.getTablesInSchema(schemaName);
            Map<String, Map<String, Object>> tablesMap = new java.util.HashMap<>();
            
            for (Map<String, Object> table : tablesList) {
                String tableName = (String) table.get("table_name");
                tablesMap.put(tableName, table);
            }
            
            return tablesMap;
        } catch (Exception e) {
            throw new RuntimeException("테이블 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/table/{schemaName}/{tableName}")
    public String tableData(@PathVariable String schemaName, 
                           @PathVariable String tableName,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "50") int size,
                           Model model) {
        try {
            Map<String, Object> tableInfo = schemaService.getTableData(schemaName, tableName, page, size);
            List<Map<String, Object>> columns = schemaService.getTableColumns(schemaName, tableName);
            
            model.addAttribute("schemaName", schemaName);
            model.addAttribute("tableName", tableName);
            model.addAttribute("columns", columns);
            model.addAttribute("tableData", tableInfo);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            return "table-data";
        } catch (Exception e) {
            model.addAttribute("error", "테이블 데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            return "table-data";
        }
    }

    @PostMapping("/execute-sql")
    @ResponseBody
    public Map<String, Object> executeSql(@RequestParam String sql) {
        try {
            return schemaService.executeSql(sql);
        } catch (Exception e) {
            return Map.of("error", "SQL 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 