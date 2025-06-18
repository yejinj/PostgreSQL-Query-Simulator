package com.ncp.dbsimulator.controller;

import com.ncp.dbsimulator.dto.QueryAnalysisRequest;
import com.ncp.dbsimulator.dto.QueryAnalysisResponse;
import com.ncp.dbsimulator.model.ExecutionPlan;
import com.ncp.dbsimulator.model.ResourceCost;
import com.ncp.dbsimulator.service.ExecutionPlanAnalyzer;
import com.ncp.dbsimulator.service.CostEstimator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api")
public class QueryAnalysisController {

    @Autowired
    private ExecutionPlanAnalyzer executionPlanAnalyzer;

    @Autowired
    private CostEstimator costEstimator;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/analyze")
    @ResponseBody
    public ResponseEntity<QueryAnalysisResponse> analyzeQuery(@RequestBody QueryAnalysisRequest request) {
        try {
            // 쿼리 실행 계획 분석
            ExecutionPlan executionPlan = executionPlanAnalyzer.analyzeQuery(request.getQuery());
            
            // 리소스 비용 계산
            ResourceCost resourceCost = costEstimator.calculateResourceCost(executionPlan);
            
            // 인덱스 제안
            List<String> suggestions = executionPlanAnalyzer.getSuggestedIndexes(executionPlan);
            
            // 응답 생성
            QueryAnalysisResponse response = new QueryAnalysisResponse();
            response.setExecutionPlan(executionPlan);
            response.setResourceCost(resourceCost);
            response.setSuggestions(suggestions);
            response.setSuccess(true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            QueryAnalysisResponse errorResponse = new QueryAnalysisResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("쿼리 분석 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("DB Resource Simulator is running");
    }

    @GetMapping("/analysis")
    public String analysisPage(Model model) {
        return "analysis";
    }
} 