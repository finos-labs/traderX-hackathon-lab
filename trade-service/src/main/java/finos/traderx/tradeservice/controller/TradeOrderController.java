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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.CdmTrade;
import finos.traderx.tradeservice.model.TradeSide;
import finos.traderx.tradeservice.adapter.TradeOrderToCDMAdapter;
import finos.traderx.tradeservice.repository.CdmTradeRepository;
// CDM imports removed - using JSON-based approach
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/trade", produces = "application/json")
public class TradeOrderController {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderController.class);

	@Autowired
	private Publisher<TradeOrder> tradePublisher;
	
	@Autowired
	private Publisher<String> cdmTradePublisher;
	
	@Autowired
	private TradeOrderToCDMAdapter cdmAdapter;
	
	@Autowired
	private CdmTradeRepository cdmTradeRepository;
	
	private RestTemplate restTemplate = new RestTemplate();

	@Value("${reference.data.service.url}")
	private String referenceDataServiceAddress;

	@Value("${account.service.url}")
	private String accountServiceAddress;
	
	@Value("${traderx.cdm.enabled:false}")
	private boolean cdmEnabled;

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	public ResponseEntity<TradeOrder> createTradeOrder(@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder) {
		log.info("Called createTradeOrder with CDM enabled: {}", cdmEnabled);
		
		if (!validateTicker(tradeOrder.getSecurity())) 
		{
			throw new ResourceNotFoundException(tradeOrder.getSecurity() + " not found in Reference data service.");
		}
		else if(!validateAccount(tradeOrder.getAccountId()))
		{
			throw new ResourceNotFoundException(tradeOrder.getAccountId() + " not found in Account service.");
		}
		else
		{
			try{
				log.info("Trade is valid. Submitting {}", tradeOrder);
				
				if (cdmEnabled) {
					// CDM-enhanced processing - DIRECT DATABASE APPROACH
					log.info("🚀 Processing trade using FINOS CDM 6.0.0 model - Direct Storage");
					
					try {
						// Set trade order context for CDM processing
						cdmAdapter.setCurrentTradeOrder(tradeOrder);
						
						// Create CDM-compliant JSON following FINOS CDM Event Model
						String cdmJson = cdmAdapter.createCDMTradeJSON(tradeOrder);
						
						// Clear trade order context
						cdmAdapter.clearCurrentTradeOrder();
						
						// Create and save CDM trade directly to database
						CdmTrade cdmTrade = new CdmTrade();
						cdmTrade.setId("CDM-" + tradeOrder.getId());
						cdmTrade.setAccountId(tradeOrder.getAccountId());
						cdmTrade.setCreated(new java.util.Date());
						cdmTrade.setUpdated(new java.util.Date());
						cdmTrade.setSecurity(tradeOrder.getSecurity());
						cdmTrade.setSide(tradeOrder.getSide());
						cdmTrade.setQuantity(tradeOrder.getQuantity());
						cdmTrade.setState("CDM_PROCESSED");
						cdmTrade.setCdmTrade(cdmJson);
						
						// Save directly to CDMTRADES table
						log.info("💾 Attempting to save CDM trade to database: {}", cdmTrade.getId());
						CdmTrade savedTrade = cdmTradeRepository.save(cdmTrade);
						log.info("✅ Successfully saved CDM trade to CDMTRADES table: {} (DB ID: {})", cdmTrade.getId(), savedTrade.getId());
						
						// Also publish to message bus for compatibility
						cdmTradePublisher.publish("/trades/cdm", cdmJson);
						log.info("📤 Published CDM JSON for trade: {}", tradeOrder.getId());
						
					} catch (Exception e) {
						log.error("❌ CDM processing failed, continuing with legacy processing", e);
					}
				} else {
					// Legacy processing
					log.info("Processing trade using legacy model");
				}
				
				// Always publish to legacy channel for compatibility
				log.info("📤 Publishing trade to legacy /trades channel: {}", tradeOrder.getId());
				tradePublisher.publish("/trades", tradeOrder);
				log.info("✅ Successfully published trade to legacy channel: {}", tradeOrder.getId());
				
				return ResponseEntity.ok(tradeOrder);
			} catch (PubSubException e){
				throw new RuntimeException("Failed to publish trade order", e);
			}
		}
	}

	private boolean validateTicker(String ticker)
	{
		// Move whole method to a sperate class that handles all reference data 
		// so we can mock it and run without this service up.
		String url = this.referenceDataServiceAddress + "//stocks/" + ticker;
		ResponseEntity<Security> response = null;

		try {
			response = this.restTemplate.getForEntity(url, Security.class);
			log.info("Validate ticker " + response.getBody().toString());
			return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getRawStatusCode() == 404) {
				log.info(ticker + " not found in reference data service.");
			}
			else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}		
	
	private boolean validateAccount(Integer id)
	{
		// Move whole method to a sperate class that handles all accounts 
		// so we can mock it and run without this service up.

		String url = this.accountServiceAddress + "//account/" + id;
		ResponseEntity<Account> response = null;

		try 
		{
				response = this.restTemplate.getForEntity(url, Account.class);
				log.info("Validate account " + response.getBody().toString());
				return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getRawStatusCode() == 404) {
				log.info("Account" + id + " not found in account service.");				
			}
			else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}
	
	@Operation(description = "Get CDM trades from database")
	@GetMapping("/cdm-trades-simple")
	public ResponseEntity<String> getCDMTradesSimple() {
		try {
			long count = cdmTradeRepository.count();
			java.util.List<CdmTrade> trades = cdmTradeRepository.findAll();
			return ResponseEntity.ok("CDM Trades Count: " + count + ", Trades: " + trades.toString());
		} catch (Exception e) {
			return ResponseEntity.ok("Error: " + e.getMessage());
		}
	}
	
	@Operation(description = "Test CDM database connection and save")
	@PostMapping("/test-cdm-save")
	public ResponseEntity<Map<String, Object>> testCDMSave(@RequestBody TradeOrder tradeOrder) {
		log.info("🧪 Testing CDM database save for trade: {}", tradeOrder.getId());
		
		Map<String, Object> result = new HashMap<>();
		
		try {
			// Step 1: Check database connection
			long initialCount = cdmTradeRepository.count();
			log.info("📊 Initial CDM trades count: {}", initialCount);
			
			// Step 2: Create CDM-compliant JSON
			String cdmJson = cdmAdapter.createCDMTradeJSON(tradeOrder);
			
			// Step 3: Create CDM trade with unique ID
			CdmTrade cdmTrade = new CdmTrade();
			String uniqueId = "TEST-CDM-" + tradeOrder.getId() + "-" + System.currentTimeMillis();
			cdmTrade.setId(uniqueId);
			cdmTrade.setAccountId(tradeOrder.getAccountId());
			cdmTrade.setCreated(new java.util.Date());
			cdmTrade.setUpdated(new java.util.Date());
			cdmTrade.setSecurity(tradeOrder.getSecurity());
			cdmTrade.setSide(tradeOrder.getSide());
			cdmTrade.setQuantity(tradeOrder.getQuantity());
			cdmTrade.setState("TEST_CDM_PROCESSED");
			cdmTrade.setCdmTrade(cdmJson);
			
			log.info("💾 Attempting to save CDM trade with ID: {}", uniqueId);
			
			// Step 4: Save with flush to force immediate persistence
			CdmTrade savedTrade = cdmTradeRepository.saveAndFlush(cdmTrade);
			log.info("✅ Saved CDM trade: {}", savedTrade.getId());
			
			// Step 5: Verify by counting again
			long finalCount = cdmTradeRepository.count();
			log.info("📊 Final CDM trades count: {}", finalCount);
			
			// Step 6: Try to find the saved trade
			java.util.Optional<CdmTrade> foundTrade = cdmTradeRepository.findById(uniqueId);
			
			result.put("success", true);
			result.put("initialCount", initialCount);
			result.put("finalCount", finalCount);
			result.put("savedTradeId", savedTrade.getId());
			result.put("tradeFound", foundTrade.isPresent());
			result.put("cdmJson", cdmJson);
			result.put("message", "✅ CDM database test completed");
			
			return ResponseEntity.ok(result);
			
		} catch (Exception e) {
			log.error("❌ CDM database test failed", e);
			result.put("success", false);
			result.put("error", e.getMessage());
			result.put("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
			result.put("message", "❌ CDM database test failed: " + e.getMessage());
			return ResponseEntity.status(500).body(result);
		}
	}
	

	
	@Operation(description = "Get CDM database contents")
	@GetMapping("/cdm-database")
	public ResponseEntity<Map<String, Object>> getCDMDatabase() {
		Map<String, Object> result = new HashMap<>();
		
		try {
			long count = cdmTradeRepository.count();
			java.util.List<CdmTrade> allTrades = cdmTradeRepository.findAll();
			
			result.put("success", true);
			result.put("totalCdmTrades", count);
			result.put("trades", allTrades.stream()
				.map(trade -> java.util.Map.of(
					"id", trade.getId(),
					"security", trade.getSecurity(),
					"quantity", trade.getQuantity(),
					"side", trade.getSide().toString(),
					"state", trade.getState(),
					"created", trade.getCreated().toString(),
					"cdmJson", trade.getCdmTrade().substring(0, Math.min(200, trade.getCdmTrade().length())) + "..."
				))
				.collect(java.util.stream.Collectors.toList()));
			
			result.put("message", "✅ Retrieved " + count + " CDM trades from in-memory database");
			
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", e.getMessage());
			result.put("message", "❌ Failed to retrieve CDM trades: " + e.getMessage());
		}
		
		return ResponseEntity.ok(result);
	}
	
	@Operation(description = "Get CDM integration status with database info")
	@GetMapping("/cdm-status")
	public ResponseEntity<Map<String, Object>> getCDMStatus() {
		Map<String, Object> status = new HashMap<>();
		status.put("cdmEnabled", cdmEnabled);
		status.put("cdmVersion", "6.0.0");
		status.put("integration", "FINOS Common Domain Model");
		status.put("status", cdmEnabled ? "ACTIVE" : "DISABLED");
		status.put("timestamp", java.time.LocalDateTime.now().toString());
		status.put("features", java.util.Arrays.asList(
			"ExecutionInstruction Creation",
			"BusinessEvent Processing", 
			"Industry Standard Compliance",
			"Dual-Mode Processing"
		));
		
		// Add database information
		try {
			long cdmTradeCount = cdmTradeRepository.count();
			status.put("cdmTradesInDatabase", cdmTradeCount);
			
			if (cdmTradeCount > 0) {
				java.util.List<CdmTrade> recentTrades = cdmTradeRepository.findAll();
				status.put("recentCdmTrades", recentTrades.stream()
					.limit(5)
					.map(trade -> trade.getId() + " (" + trade.getSecurity() + ", " + trade.getQuantity() + " shares)")
					.collect(java.util.stream.Collectors.toList()));
			}
		} catch (Exception e) {
			status.put("cdmTradesInDatabase", "Error: " + e.getMessage());
		}
		
		// Add CDM database data for demo
		try {
			long cdmCount = cdmTradeRepository.count();
			status.put("cdmTradesInMemoryDB", cdmCount);
			
			if (cdmCount > 0) {
				java.util.List<CdmTrade> trades = cdmTradeRepository.findAll();
				status.put("cdmTradesData", trades.stream()
					.map(trade -> java.util.Map.of(
						"id", trade.getId(),
						"security", trade.getSecurity(),
						"quantity", trade.getQuantity(),
						"side", trade.getSide().toString(),
						"accountId", trade.getAccountId(),
						"state", trade.getState(),
						"created", trade.getCreated().toString(),
						"cdmJsonPreview", trade.getCdmTrade().length() > 100 ? 
							trade.getCdmTrade().substring(0, 100) + "..." : trade.getCdmTrade()
					))
					.collect(java.util.stream.Collectors.toList()));
			}
		} catch (Exception e) {
			status.put("cdmTradesInMemoryDB", "Error: " + e.getMessage());
		}
		
		return ResponseEntity.ok(status);
	}
	
	@Operation(description = "Get CDM trades count")
	@GetMapping("/cdm-count")
	public ResponseEntity<String> getCDMCount() {
		try {
			long count = cdmTradeRepository.count();
			return ResponseEntity.ok("CDM Trades: " + count);
		} catch (Exception e) {
			return ResponseEntity.ok("Error: " + e.getMessage());
		}
	}
	
	@Operation(description = "Demo: Show CDM trades for presentation")
	@GetMapping("/demo")
	public ResponseEntity<Map<String, Object>> getDemoData() {
		Map<String, Object> demo = new HashMap<>();
		demo.put("title", "FINOS CDM Integration Demo");
		demo.put("cdmVersion", "6.0.0");
		demo.put("status", "ACTIVE");
		
		try {
			long count = cdmTradeRepository.count();
			demo.put("totalCdmTrades", count);
			
			if (count > 0) {
				java.util.List<CdmTrade> trades = cdmTradeRepository.findAll();
				demo.put("cdmTrades", trades.stream().limit(10).map(trade -> 
					"ID: " + trade.getId() + " | " + trade.getSecurity() + " | " + trade.getQuantity() + " shares | " + trade.getSide()
				).collect(java.util.stream.Collectors.toList()));
			} else {
				demo.put("message", "No CDM trades found - submit a trade to see CDM processing");
			}
		} catch (Exception e) {
			demo.put("error", e.getMessage());
		}
		
		return ResponseEntity.ok(demo);
	}
	
	@Operation(description = "Test CDM processing directly")
	@PostMapping("/test-cdm")
	public ResponseEntity<Map<String, Object>> testCDMProcessing(@RequestBody TradeOrder tradeOrder) {
		log.info("🧪 Testing CDM processing for trade: {}", tradeOrder.getId());
		
		Map<String, Object> result = new HashMap<>();
		
		try {
			if (cdmEnabled) {
				// Process with CDM-compliant JSON
				String cdmJson = cdmAdapter.createCDMTradeJSON(tradeOrder);
				
				result.put("success", true);
				result.put("cdmProcessed", true);
				result.put("tradeId", tradeOrder.getId());
				result.put("cdmBusinessEvent", cdmJson);
				result.put("message", "✅ CDM BusinessEvent created successfully");
				
				log.info("✅ CDM test successful for trade: {}", tradeOrder.getId());
			} else {
				result.put("success", false);
				result.put("cdmProcessed", false);
				result.put("message", "❌ CDM processing is disabled");
			}
		} catch (Exception e) {
			log.error("❌ CDM test failed", e);
			result.put("success", false);
			result.put("error", e.getMessage());
			result.put("message", "❌ CDM processing failed: " + e.getMessage());
		}
		
		return ResponseEntity.ok(result);
	}
	
	@Operation(description = "Get CDM trades from database")
	@GetMapping("/cdm-trades")
	public ResponseEntity<java.util.List<CdmTrade>> getCDMTrades() {
		log.info("📊 Retrieving CDM trades from database");
		
		try {
			java.util.List<CdmTrade> cdmTrades = cdmTradeRepository.findAll();
			log.info("✅ Found {} CDM trades in database", cdmTrades.size());
			return ResponseEntity.ok(cdmTrades);
		} catch (Exception e) {
			log.error("❌ Failed to retrieve CDM trades", e);
			return ResponseEntity.status(500).body(java.util.Collections.emptyList());
		}
	}
	
	@Operation(description = "Process trade using dedicated CDM pipeline")
	@PostMapping("/cdm-pipeline")
	public ResponseEntity<Map<String, Object>> processCDMTrade(@RequestBody TradeOrder tradeOrder) {
		log.info("🏛️ Processing trade using pure FINOS CDM workflow: {}", tradeOrder.getId());
		
		Map<String, Object> result = new HashMap<>();
		
		try {
			// Step 1: Create CDM-compliant JSON following FINOS CDM Event Model
			String cdmJson = cdmAdapter.createCDMTradeJSON(tradeOrder);
			
			// Step 4: Create and save CDM trade with detailed logging
			CdmTrade cdmTrade = new CdmTrade();
			cdmTrade.setId("CDM-PURE-" + tradeOrder.getId());
			cdmTrade.setAccountId(tradeOrder.getAccountId());
			cdmTrade.setCreated(new java.util.Date());
			cdmTrade.setUpdated(new java.util.Date());
			cdmTrade.setSecurity(tradeOrder.getSecurity());
			cdmTrade.setSide(tradeOrder.getSide());
			cdmTrade.setQuantity(tradeOrder.getQuantity());
			cdmTrade.setState("CDM_PURE_PROCESSED");
			cdmTrade.setCdmTrade(cdmJson);
			
			log.info("💾 Attempting to save pure CDM trade: {}", cdmTrade.getId());
			
			// Force transaction and check for errors
			CdmTrade savedTrade = cdmTradeRepository.saveAndFlush(cdmTrade);
			
			log.info("✅ Successfully saved pure CDM trade: {} (Saved ID: {})", 
				cdmTrade.getId(), savedTrade.getId());
			
			// Verify it was saved by counting records
			long count = cdmTradeRepository.count();
			log.info("📊 Total CDM trades in database: {}", count);
			
			result.put("success", true);
			result.put("cdmTradeId", savedTrade.getId());
			result.put("cdmBusinessEvent", cdmJson);
			result.put("totalCdmTrades", count);
			result.put("message", "✅ Pure CDM trade processed and saved successfully");
			
			return ResponseEntity.ok(result);
			
		} catch (Exception e) {
			log.error("❌ Pure CDM processing failed", e);
			result.put("success", false);
			result.put("error", e.getMessage());
			result.put("message", "❌ Pure CDM processing failed: " + e.getMessage());
			return ResponseEntity.status(500).body(result);
		}
	}

    // ========== MISSING BASIC TRADE ENDPOINTS ==========
    
    @Operation(description = "Get all trades")
    @GetMapping("/trades")
    public ResponseEntity<Map<String, Object>> getAllTrades() {
        log.info("📊 Retrieving all trades");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // For now, return CDM trades as the main trade data
            // In a full implementation, you'd have a separate TradeRepository
            java.util.List<CdmTrade> cdmTrades = cdmTradeRepository.findAll();
            
            // Convert CDM trades to simple trade format for UI compatibility
            java.util.List<Map<String, Object>> trades = cdmTrades.stream()
                .map(cdmTrade -> {
                    Map<String, Object> trade = new HashMap<>();
                    trade.put("id", cdmTrade.getId());
                    trade.put("security", cdmTrade.getSecurity());
                    trade.put("quantity", cdmTrade.getQuantity());
                    trade.put("side", cdmTrade.getSide().toString());
                    trade.put("state", cdmTrade.getState());
                    trade.put("updated", cdmTrade.getUpdated());
                    trade.put("accountId", cdmTrade.getAccountId());
                    return trade;
                })
                .collect(java.util.stream.Collectors.toList());
            
            response.put("success", true);
            response.put("totalTrades", trades.size());
            response.put("trades", trades);
            response.put("message", String.format("✅ Retrieved %d trades", trades.size()));
            
            log.info("✅ Successfully retrieved {} trades", trades.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to retrieve trades", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Failed to retrieve trades: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(description = "Create a new trade")
    @PostMapping("/trades")
    public ResponseEntity<Map<String, Object>> createTrade(@RequestBody TradeOrder tradeOrder) {
        log.info("📝 Creating new trade: {}", tradeOrder.getId());
        
        // Reuse the existing createTradeOrder logic
        try {
            ResponseEntity<TradeOrder> result = createTradeOrder(tradeOrder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("trade", result.getBody());
            response.put("message", "✅ Trade created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to create trade: {}", tradeOrder.getId(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "❌ Failed to create trade: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== POSITIONS ENDPOINT (FALLBACK) ==========
    
    @Operation(description = "Get positions for account (calculated from trades)")
    @GetMapping("/positions/{accountId}")
    public ResponseEntity<java.util.List<Map<String, Object>>> getPositions(
            @org.springframework.web.bind.annotation.PathVariable Integer accountId) {
        log.info("📊 Calculating positions for account: {}", accountId);
        
        try {
            // Get all CDM trades for the account
            java.util.List<CdmTrade> cdmTrades = cdmTradeRepository.findByAccountId(accountId);
            
            // Calculate positions by security
            Map<String, Map<String, Object>> positionsMap = new HashMap<>();
            
            for (CdmTrade trade : cdmTrades) {
                String security = trade.getSecurity();
                int quantity = trade.getQuantity();
                
                // Adjust quantity based on side (Buy = +, Sell = -)
                if (trade.getSide() == TradeSide.Sell) {
                    quantity = -quantity;
                }
                
                positionsMap.computeIfAbsent(security, k -> {
                    Map<String, Object> position = new HashMap<>();
                    position.put("security", security);
                    position.put("quantity", 0);
                    position.put("updated", trade.getUpdated());
                    return position;
                });
                
                Map<String, Object> position = positionsMap.get(security);
                int currentQuantity = (Integer) position.get("quantity");
                position.put("quantity", currentQuantity + quantity);
                
                // Update timestamp to latest trade
                if (trade.getUpdated().after((java.util.Date) position.get("updated"))) {
                    position.put("updated", trade.getUpdated());
                }
            }
            
            // Convert to list and filter out zero positions
            java.util.List<Map<String, Object>> positions = positionsMap.values().stream()
                .filter(pos -> (Integer) pos.get("quantity") != 0)
                .collect(java.util.stream.Collectors.toList());
            
            log.info("✅ Calculated {} positions for account: {}", positions.size(), accountId);
            return ResponseEntity.ok(positions);
            
        } catch (Exception e) {
            log.error("❌ Failed to calculate positions for account: {}", accountId, e);
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }
}