package com.simulator.controller;

import com.simulator.model.QueryAnalysisRequest;
import com.simulator.model.QueryAnalysisResult;
import com.simulator.service.QueryAnalysisService;
import com.simulator.service.DatabaseMetricsService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller // 웹 요청을 받는 컨트롤러임을 나타냄
@RequestMapping("/") // 루트 경로로 들어오는 요청을 처리함
public class QueryAnalysisController {

    @Autowired
    private QueryAnalysisService queryAnalysisService; // 쿼리 분석을 실제로 수행하는 서비스
    
    @Autowired
    private DatabaseMetricsService databaseMetricsService; // 실제 데이터베이스 메트릭 수집 서비스

    // 1. 메인 페이지 접속 (GET /)
    @GetMapping("/")
    public String index(Model model) {
        // 빈 폼 객체를 생성해서 index.html로 전달함
        model.addAttribute("queryRequest", new QueryAnalysisRequest());
        return "index"; // → templates/index.html 렌더링
    }

    // 2. 폼에서 쿼리 분석 요청 (POST /analyze)
    @PostMapping("/analyze")
    public String analyzeQuery(
            @Valid @ModelAttribute("queryRequest") QueryAnalysisRequest request, // 사용자가 입력한 쿼리
            BindingResult bindingResult, // 유효성 검증 결과
            Model model) { // HTML에 전달할 데이터들

        if (bindingResult.hasErrors()) {
            return "index"; // 입력값에 문제가 있으면 다시 폼 페이지로
        }

        try {
            // 쿼리를 분석하고 결과를 받음
            QueryAnalysisResult result = queryAnalysisService.analyzeQuery(request);

            // 분석 결과와 요청 쿼리를 모델에 담아 result.html에 넘김
            model.addAttribute("result", result);
            model.addAttribute("queryRequest", request);
            return "result"; // → templates/result.html 렌더링

        } catch (Exception e) {
            // 분석 도중 예외가 발생하면 에러 메시지 출력
            model.addAttribute("error", "쿼리 분석 중 오류가 발생했습니다: " + e.getMessage());
            return "index"; // 다시 index로 보냄
        }
    }

    // 3. API 방식 - GET 요청 (예: /api/analyze?sqlQuery=...)
    @GetMapping("/api/analyze")
    @ResponseBody
    public QueryAnalysisResult analyzeQueryApi(
            @RequestParam String sqlQuery, // 쿼리 문자열
            @RequestParam(required = false) String schemaName) { // 스키마는 선택 사항

        // DTO 객체로 감싸서 서비스로 전달
        QueryAnalysisRequest request = new QueryAnalysisRequest(sqlQuery, schemaName);
        return queryAnalysisService.analyzeQuery(request); // 분석 결과 JSON으로 리턴
    }

    // 4. API 방식 - POST 요청 (JSON body로 전송)
    @PostMapping("/api/analyze")
    @ResponseBody
    public QueryAnalysisResult analyzeQueryApiPost(
            @Valid @RequestBody QueryAnalysisRequest request) {
        return queryAnalysisService.analyzeQuery(request); // JSON으로 분석 결과 반환
    }

    // 5. 공통 예외 처리 (모든 예외 잡아서 텍스트로 응답)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public String handleIllegalArgumentException(IllegalArgumentException e) {
        return "입력값 오류: " + e.getMessage();
    }
    
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public String handleRuntimeException(RuntimeException e) {
        return "실행 오류: " + e.getMessage();
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleException(Exception e) {
        return "시스템 오류: " + e.getMessage(); // 예외 메시지를 텍스트로 응답
    }
    
    // 실제 데이터베이스 메트릭 API
    @GetMapping("/api/database-metrics")
    @ResponseBody
    public Map<String, Object> getDatabaseMetrics() {
        return databaseMetricsService.getDatabaseMetrics();
    }
    
    // 실시간 쿼리 성능 메트릭 API
    @GetMapping("/api/query-performance-metrics")
    @ResponseBody
    public Map<String, Object> getQueryPerformanceMetrics() {
        return databaseMetricsService.getQueryPerformanceMetrics();
    }
}
