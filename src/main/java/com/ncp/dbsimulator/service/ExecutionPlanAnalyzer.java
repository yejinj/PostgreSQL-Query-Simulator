// ExecutionPlanAnalyzer Service Implementation

package com.ncp.dbsimulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncp.dbsimulator.model.ExecutionPlan;
import com.ncp.dbsimulator.model.NodeAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExecutionPlanAnalyzer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecutionPlan analyzeQuery(String query) {
        try {
            // EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) 실행
            String explainQuery = String.format("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) %s", query);
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(explainQuery);
            String jsonPlan = (String) result.get(0).get("QUERY PLAN");
            
            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(jsonPlan);
            JsonNode planNode = rootNode.get(0).get("Plan");
            
            ExecutionPlan executionPlan = new ExecutionPlan();
            executionPlan.setOriginalQuery(query);
            executionPlan.setJsonPlan(jsonPlan);
            executionPlan.setTotalCost(planNode.get("Total Cost").asDouble());
            executionPlan.setActualTotalTime(planNode.get("Actual Total Time").asDouble());
            
            // 노드별 분석
            List<NodeAnalysis> nodeAnalyses = new ArrayList<>();
            analyzeNode(planNode, nodeAnalyses, 0);
            executionPlan.setNodeAnalyses(nodeAnalyses);
            
            return executionPlan;
            
        } catch (Exception e) {
            throw new RuntimeException("쿼리 분석 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    private void analyzeNode(JsonNode node, List<NodeAnalysis> analyses, int depth) {
        NodeAnalysis analysis = new NodeAnalysis();
        analysis.setNodeType(node.get("Node Type").asText());
        analysis.setDepth(depth);
        
        // 기본 비용 정보
        if (node.has("Total Cost")) {
            analysis.setTotalCost(node.get("Total Cost").asDouble());
        }
        if (node.has("Startup Cost")) {
            analysis.setStartupCost(node.get("Startup Cost").asDouble());
        }
        if (node.has("Plan Rows")) {
            analysis.setPlanRows(node.get("Plan Rows").asLong());
        }
        if (node.has("Plan Width")) {
            analysis.setPlanWidth(node.get("Plan Width").asInt());
        }
        
        // 실제 실행 정보
        if (node.has("Actual Total Time")) {
            analysis.setActualTotalTime(node.get("Actual Total Time").asDouble());
        }
        if (node.has("Actual Rows")) {
            analysis.setActualRows(node.get("Actual Rows").asLong());
        }
        if (node.has("Actual Loops")) {
            analysis.setActualLoops(node.get("Actual Loops").asInt());
        }
        
        // 버퍼 정보
        if (node.has("Shared Hit Blocks")) {
            analysis.setSharedHitBlocks(node.get("Shared Hit Blocks").asLong());
        }
        if (node.has("Shared Read Blocks")) {
            analysis.setSharedReadBlocks(node.get("Shared Read Blocks").asLong());
        }
        if (node.has("Shared Dirtied Blocks")) {
            analysis.setSharedDirtiedBlocks(node.get("Shared Dirtied Blocks").asLong());
        }
        if (node.has("Shared Written Blocks")) {
            analysis.setSharedWrittenBlocks(node.get("Shared Written Blocks").asLong());
        }
        
        // 테이블 정보
        if (node.has("Relation Name")) {
            analysis.setRelationName(node.get("Relation Name").asText());
        }
        if (node.has("Alias")) {
            analysis.setAlias(node.get("Alias").asText());
        }
        
        // 조인 정보
        if (node.has("Join Type")) {
            analysis.setJoinType(node.get("Join Type").asText());
        }
        
        // 정렬 정보
        if (node.has("Sort Key")) {
            JsonNode sortKeys = node.get("Sort Key");
            if (sortKeys.isArray()) {
                List<String> keys = new ArrayList<>();
                for (JsonNode key : sortKeys) {
                    keys.add(key.asText());
                }
                analysis.setSortKeys(keys);
            }
        }
        
        if (node.has("Sort Method")) {
            analysis.setSortMethod(node.get("Sort Method").asText());
        }
        if (node.has("Sort Space Used")) {
            analysis.setSortSpaceUsed(node.get("Sort Space Used").asLong());
        }
        if (node.has("Sort Space Type")) {
            analysis.setSortSpaceType(node.get("Sort Space Type").asText());
        }
        
        analyses.add(analysis);
        
        // 하위 노드들 재귀적으로 분석
        if (node.has("Plans")) {
            JsonNode plans = node.get("Plans");
            for (JsonNode childNode : plans) {
                analyzeNode(childNode, analyses, depth + 1);
            }
        }
    }
    
    public List<String> getSuggestedIndexes(ExecutionPlan plan) {
        List<String> suggestions = new ArrayList<>();
        
        for (NodeAnalysis node : plan.getNodeAnalyses()) {
            // Sequential Scan이 많은 비용을 차지하는 경우
            if ("Seq Scan".equals(node.getNodeType()) && node.getTotalCost() > 1000) {
                if (node.getRelationName() != null) {
                    suggestions.add(String.format("%s 테이블에 인덱스 생성을 고려해보세요", node.getRelationName()));
                }
            }
            
            // 정렬 작업이 디스크를 사용하는 경우
            if ("Sort".equals(node.getNodeType()) && "disk".equals(node.getSortSpaceType())) {
                suggestions.add("정렬 작업이 디스크를 사용하고 있습니다. work_mem 증가나 인덱스 활용을 고려해보세요");
            }
            
            // 해시 조인에서 배치가 발생하는 경우
            if ("Hash Join".equals(node.getNodeType()) && node.getActualRows() > node.getPlanRows() * 2) {
                suggestions.add("해시 조인에서 예상보다 많은 행이 처리되었습니다. 통계 업데이트를 고려해보세요");
            }
        }
        
        return suggestions;
    }
}
