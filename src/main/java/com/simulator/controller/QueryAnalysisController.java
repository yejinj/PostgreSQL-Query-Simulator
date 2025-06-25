package com.simulator.controller;

import com.simulator.model.QueryAnalysisRequest;
import com.simulator.model.QueryAnalysisResult;
import com.simulator.service.QueryAnalysisService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class QueryAnalysisController {
    
    @Autowired
    private QueryAnalysisService queryAnalysisService;
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("queryRequest", new QueryAnalysisRequest());
        return "index";
    }
    
    @PostMapping("/analyze")
    public String analyzeQuery(@Valid @ModelAttribute("queryRequest") QueryAnalysisRequest request,
                              BindingResult bindingResult,
                              Model model) {
        
        if (bindingResult.hasErrors()) {
            return "index";
        }
        
        try {
            QueryAnalysisResult result = queryAnalysisService.analyzeQuery(request);
            model.addAttribute("result", result);
            model.addAttribute("queryRequest", request);
            return "result";
            
        } catch (Exception e) {
            model.addAttribute("error", "쿼리 분석 중 오류가 발생했습니다: " + e.getMessage());
            return "index";
        }
    }
    
    @GetMapping("/api/analyze")
    @ResponseBody
    public QueryAnalysisResult analyzeQueryApi(@RequestParam String sqlQuery,
                                               @RequestParam(required = false) String schemaName) {
        QueryAnalysisRequest request = new QueryAnalysisRequest(sqlQuery, schemaName);
        return queryAnalysisService.analyzeQuery(request);
    }
    
    @PostMapping("/api/analyze")
    @ResponseBody
    public QueryAnalysisResult analyzeQueryApiPost(@Valid @RequestBody QueryAnalysisRequest request) {
        return queryAnalysisService.analyzeQuery(request);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleException(Exception e) {
        return "오류: " + e.getMessage();
    }
} 