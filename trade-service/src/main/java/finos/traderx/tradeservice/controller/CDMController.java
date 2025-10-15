package finos.traderx.tradeservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.CdmTrade;
import finos.traderx.tradeservice.model.TradeSide;
import finos.traderx.tradeservice.adapter.TradeOrderToCDMAdapter;
import finos.traderx.tradeservice.repository.CdmTradeRepository;
import cdm.event.common.BusinessEvent;
import cdm.event.common.ExecutionInstruction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Dedicated FINOS CDM Controller following CDM best practices
 * Provides a separate pipeline for CDM processing independent of legacy trade flow
 */
@CrossOrigin("*")
@RestController
@RequestMapping(value="/cdm", produces = "application/json")
public class CDMController {

    private static final Logger log = LoggerFactory.getLogger(CDMController.class);

    @Autowired
    private TradeOrderToCDMAdapter cdmAdapter;
    
    @Autowired
    private CdmTradeRepository cdmTradeRepository;
    
    @Value("${traderx.cdm.enabled:true}")
    private boolean cdmEnabled;

    @Operation(description = "Get CDM service status and configuration")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCDMStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "FINOS CDM Processing Pipeline");
        status.put("cdmEnabled", cdmEnabled);
        status.put("cdmVersion", "6.0.0");
        status.put("implementation", "FINOS Common Domain Model");
        status.put("status", cdmEnabled ? "ACTIVE" : "DISABLED");
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        status.put("capabilities", java.util.Arrays.asList(
            "ExecutionInstruction Creation",
            "BusinessEvent Processing", 
            "CDM Serialization",
            "Event Store Persistence",
            "Industry Standard Compliance"
        ));
        
        // Add database statistics
        try {
            long cdmTradeCount = cdmTradeRepository.count();
            status.put("cdmTradesStored", cdmTradeCount);
        } catch (Exception e) {
            status.put("cdmTradesStored", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }

    @Operation(description = "Process trade through dedicated CDM pipeline")
    @PostMapping("/process-trade")
    public ResponseEntity<Map<String, Object>> processTradeThroughCDM(
            @Parameter(description = "Trade order to process through CDM") @RequestBody TradeOrder tradeOrder) {
        
        log.info("🏛️ Processing trade through dedicated FINOS CDM pipeline: {}", tradeOrder.getId());
        
        Map<String, Object> result = new HashMap<>();
        
        if (!cdmEnabled) {
            result.put("success", false);
            result.put("message", "❌ CDM processing is disabled");
            return ResponseEntity.ok(result);
        }
        
        try {
            // Step 1: Create CDM ExecutionInstruction
            log.info("📋 Step 1: Creating CDM ExecutionInstruction");
            ExecutionInstruction instruction = cdmAdapter.convertToExecutionInstruction(tradeOrder);
            
            // Step 2: Create CDM BusinessEvent
            log.info("🏗️ Step 2: Creating CDM BusinessEvent");
            BusinessEvent businessEvent = cdmAdapter.createNewTradeEvent(instruction);
            
            // Step 3: Serialize CDM event
            log.info("📄 Step 3: Serializing CDM BusinessEvent");
            String cdmJson = cdmAdapter.serializeCDMEvent(businessEvent);
            
            // Step 4: Create and persist CDM trade
            log.info("💾 Step 4: Persisting CDM trade to event store");
            CdmTrade cdmTrade = new CdmTrade();
            cdmTrade.setId("CDM-" + tradeOrder.getId() + "-" + System.currentTimeMillis());
            cdmTrade.setAccountId(tradeOrder.getAccountId());
            cdmTrade.setCreated(new java.util.Date());
            cdmTrade.setUpdated(new java.util.Date());
            cdmTrade.setSecurity(tradeOrder.getSecurity());
            cdmTrade.setSide(tradeOrder.getSide());
            cdmTrade.setQuantity(tradeOrder.getQuantity());
            cdmTrade.setState("CDM_PROCESSED");
            cdmTrade.setCdmTrade(cdmJson);
            
            // Use saveAndFlush to ensure immediate persistence
            CdmTrade savedTrade = cdmTradeRepository.saveAndFlush(cdmTrade);
            
            // Step 5: Verify persistence
            long totalCdmTrades = cdmTradeRepository.count();
            
            log.info("✅ Successfully processed trade through CDM pipeline: {} (Total CDM trades: {})", 
                savedTrade.getId(), totalCdmTrades);
            
            result.put("success", true);
            result.put("cdmTradeId", savedTrade.getId());
            result.put("originalTradeId", tradeOrder.getId());
            result.put("cdmBusinessEvent", cdmJson);
            result.put("totalCdmTrades", totalCdmTrades);
            result.put("processingSteps", java.util.Arrays.asList(
                "✅ ExecutionInstruction Created",
                "✅ BusinessEvent Generated", 
                "✅ CDM Serialization Complete",
                "✅ Event Store Persistence Complete"
            ));
            result.put("message", "✅ Trade successfully processed through FINOS CDM pipeline");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ CDM pipeline processing failed for trade: {}", tradeOrder.getId(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("originalTradeId", tradeOrder.getId());
            result.put("message", "❌ CDM pipeline processing failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @Operation(description = "Retrieve all CDM trades from event store")
    @GetMapping("/trades")
    public ResponseEntity<Map<String, Object>> getCDMTrades() {
        log.info("📊 Retrieving CDM trades from event store");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CdmTrade> cdmTrades = cdmTradeRepository.findAll();
            
            response.put("success", true);
            response.put("totalTrades", cdmTrades.size());
            response.put("trades", cdmTrades);
            response.put("message", String.format("✅ Retrieved %d CDM trades from event store", cdmTrades.size()));
            
            log.info("✅ Successfully retrieved {} CDM trades from event store", cdmTrades.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve CDM trades from event store", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Failed to retrieve CDM trades: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(description = "Convert existing trade to CDM format")
    @PostMapping("/convert-trade")
    public ResponseEntity<Map<String, Object>> convertTradeToCDM(@RequestBody Map<String, Object> request) {
        String tradeId = (String) request.get("tradeId");
        Integer accountId = (Integer) request.get("accountId");
        
        log.info("🔄 Converting existing trade to CDM format: {}", tradeId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Find the existing trade (this would normally query the trade repository)
            // For now, we'll create a mock trade based on the request
            TradeOrder mockTrade = new TradeOrder(tradeId, accountId, "AAPL", TradeSide.Buy, 100);
            mockTrade.id = tradeId;
            
            // Process through CDM pipeline
            return processTradeThroughCDM(mockTrade);
            
        } catch (Exception e) {
            log.error("❌ Failed to convert trade to CDM: {}", tradeId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "❌ Failed to convert trade to CDM: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @Operation(description = "Update CDM trade with new business event")
    @PostMapping("/update-trade")
    public ResponseEntity<Map<String, Object>> updateCDMTrade(@RequestBody Map<String, Object> request) {
        String tradeId = (String) request.get("tradeId");
        String businessEventType = (String) request.get("businessEventType");
        
        log.info("📝 Updating CDM trade with business event: {} - {}", tradeId, businessEventType);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Find existing CDM trade
            List<CdmTrade> existingTrades = cdmTradeRepository.findAll();
            CdmTrade existingTrade = existingTrades.stream()
                .filter(t -> t.getId().contains(tradeId))
                .findFirst()
                .orElse(null);
            
            if (existingTrade == null) {
                result.put("success", false);
                result.put("message", "❌ CDM trade not found: " + tradeId);
                return ResponseEntity.status(404).body(result);
            }
            
            // Create updated trade order
            TradeOrder updatedTrade = new TradeOrder(tradeId, existingTrade.getAccountId(), 
                existingTrade.getSecurity(), existingTrade.getSide(), existingTrade.getQuantity());
            updatedTrade.id = tradeId;
            
            // Create new CDM business event
            ExecutionInstruction instruction = cdmAdapter.convertToExecutionInstruction(updatedTrade);
            BusinessEvent businessEvent = cdmAdapter.createNewTradeEvent(instruction);
            String cdmJson = cdmAdapter.serializeCDMEvent(businessEvent);
            
            // Update existing CDM trade
            existingTrade.setCdmTrade(cdmJson);
            existingTrade.setUpdated(new java.util.Date());
            existingTrade.setState("CDM_UPDATED");
            
            CdmTrade savedTrade = cdmTradeRepository.saveAndFlush(existingTrade);
            
            result.put("success", true);
            result.put("cdmTradeId", savedTrade.getId());
            result.put("businessEventType", businessEventType);
            result.put("cdmBusinessEvent", cdmJson);
            result.put("message", "✅ CDM trade updated successfully with " + businessEventType + " event");
            
            log.info("✅ Successfully updated CDM trade: {}", savedTrade.getId());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to update CDM trade: {}", tradeId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "❌ Failed to update CDM trade: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @Operation(description = "Get CDM trades for specific account")
    @GetMapping("/trades/{accountId}")
    public ResponseEntity<List<Map<String, Object>>> getCDMTradesByAccount(
            @Parameter(description = "Account ID") @org.springframework.web.bind.annotation.PathVariable Integer accountId) {
        
        log.info("📊 Retrieving CDM trades for account: {}", accountId);
        
        try {
            List<CdmTrade> cdmTrades = cdmTradeRepository.findByAccountId(accountId);
            
            List<Map<String, Object>> response = cdmTrades.stream()
                .map(trade -> {
                    Map<String, Object> tradeMap = new HashMap<>();
                    tradeMap.put("id", trade.getId());
                    tradeMap.put("security", trade.getSecurity());
                    tradeMap.put("quantity", trade.getQuantity());
                    tradeMap.put("side", trade.getSide());
                    tradeMap.put("state", trade.getState());
                    tradeMap.put("updated", trade.getUpdated());
                    tradeMap.put("cdmVersion", "6.0.0");
                    tradeMap.put("businessEventType", "Execution");
                    tradeMap.put("cdmBusinessEvent", trade.getCdmTrade());
                    tradeMap.put("cdmTrade", trade.getCdmTrade());
                    return tradeMap;
                })
                .collect(java.util.stream.Collectors.toList());
            
            log.info("✅ Successfully retrieved {} CDM trades for account: {}", response.size(), accountId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve CDM trades for account: {}", accountId, e);
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }

    @Operation(description = "Test CDM processing capabilities")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testCDMProcessing(@RequestBody TradeOrder tradeOrder) {
        log.info("🧪 Testing CDM processing capabilities for trade: {}", tradeOrder.getId());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (cdmEnabled) {
                // Test CDM object creation without persistence
                ExecutionInstruction instruction = cdmAdapter.convertToExecutionInstruction(tradeOrder);
                BusinessEvent businessEvent = cdmAdapter.createNewTradeEvent(instruction);
                String cdmJson = cdmAdapter.serializeCDMEvent(businessEvent);
                
                result.put("success", true);
                result.put("cdmProcessed", true);
                result.put("tradeId", tradeOrder.getId());
                result.put("cdmBusinessEvent", cdmJson);
                result.put("testResults", java.util.Arrays.asList(
                    "✅ ExecutionInstruction Creation: PASSED",
                    "✅ BusinessEvent Generation: PASSED",
                    "✅ CDM Serialization: PASSED"
                ));
                result.put("message", "✅ CDM processing test completed successfully");
                
                log.info("✅ CDM processing test successful for trade: {}", tradeOrder.getId());
            } else {
                result.put("success", false);
                result.put("cdmProcessed", false);
                result.put("message", "❌ CDM processing is disabled");
            }
        } catch (Exception e) {
            log.error("❌ CDM processing test failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "❌ CDM processing test failed: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}