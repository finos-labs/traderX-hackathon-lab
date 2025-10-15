package finos.traderx.tradeprocessor.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.CdmTrade;
import finos.traderx.tradeprocessor.service.TradeService;
import finos.traderx.tradeprocessor.repository.CdmTradeRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/tradeservice")
public class TradeServiceController {

	private static final Logger log = LoggerFactory.getLogger(TradeServiceController.class);

	@Autowired
	TradeService tradeService;
	
	@Autowired
	CdmTradeRepository cdmTradeRepository;

	@PostMapping("/order")
	public ResponseEntity<TradeBookingResult> processOrder(@RequestBody TradeOrder order) {
		TradeBookingResult result= tradeService.processTrade(order);
		return ResponseEntity.ok(result);
	}
	
	@GetMapping("/cdm-trades")
	public ResponseEntity<Map<String, Object>> getCDMTrades() {
		log.info("📊 Retrieving CDM trades from shared database");
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			List<CdmTrade> cdmTrades = cdmTradeRepository.findAll();
			
			response.put("success", true);
			response.put("totalTrades", cdmTrades.size());
			response.put("trades", cdmTrades);
			response.put("message", String.format("✅ Retrieved %d CDM trades from shared database", cdmTrades.size()));
			response.put("databaseType", "H2 TCP (Shared)");
			
			log.info("✅ Successfully retrieved {} CDM trades from shared database", cdmTrades.size());
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			log.error("❌ Failed to retrieve CDM trades from shared database", e);
			response.put("success", false);
			response.put("error", e.getMessage());
			response.put("message", "❌ Failed to retrieve CDM trades: " + e.getMessage());
			return ResponseEntity.status(500).body(response);
		}
	}

}
