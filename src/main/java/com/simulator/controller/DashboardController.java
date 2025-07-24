package com.simulator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "metrics-dashboard";
    }
    
    @GetMapping("/metrics")
    public String metricsRedirect() {
        return "redirect:/dashboard";
    }
} 