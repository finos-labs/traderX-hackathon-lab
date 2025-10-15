package finos.traderx.tradeprocessor.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.*;
import finos.traderx.tradeprocessor.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.annotation.PostConstruct;

@Service
public class TradeService {
	private static final Logger log = LoggerFactory.getLogger(TradeService.class);
	
	public TradeService() {
		log.info("🚀 TradeService constructor called - CDM support enabled");
	}

	@Autowired
	TradeRepository tradeRepository;
	
	@Autowired
	CdmTradeRepository cdmTradeRepository;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	private boolean cdmTableInitialized = false;

	@Autowired
	PositionRepository positionRepository;

	
    @Autowired 
    private Publisher<Trade> tradePublisher;
    
    @Autowired
    private Publisher<Position> positionPublisher;
    
	@PostConstruct
	public void initializeService() {
		log.info("🚀 Initializing TradeService with CDM support...");
		log.info("📋 CDM Repository: {}", cdmTradeRepository != null ? "AVAILABLE" : "NULL");
		initializeCdmTable();
		log.info("✅ TradeService initialization complete");
	}
	
	public TradeBookingResult processTrade(TradeOrder order) {
		log.info("Trade order received: "+order);
		
		// CDM Processing - Create CDM trade entry
		log.info("🚀 Starting CDM processing for trade: {}", order.getId());
		try {
			CdmTrade cdmTrade = new CdmTrade();
			cdmTrade.setId("CDM-" + order.getId());
			cdmTrade.setAccountId(order.getAccountId());
			cdmTrade.setCreated(new Date());
			cdmTrade.setUpdated(new Date());
			cdmTrade.setSecurity(order.getSecurity());
			cdmTrade.setSide(order.getSide());
			cdmTrade.setQuantity(order.getQuantity());
			cdmTrade.setState("CDM_PROCESSED");
			
			// Create CDM BusinessEvent JSON
			String cdmJson = String.format(
				"{\"cdmVersion\":\"6.0.0\",\"businessEventType\":\"EXECUTION\",\"tradeId\":\"%s\",\"security\":\"%s\",\"quantity\":%d,\"side\":\"%s\",\"accountId\":%d,\"timestamp\":\"%s\"}",
				order.getId(), order.getSecurity(), order.getQuantity(), order.getSide(), order.getAccountId(), new Date().toString()
			);
			cdmTrade.setCdmTrade(cdmJson);
			
			log.info("💾 Saving CDM trade to database: {}", cdmTrade.getId());
			cdmTradeRepository.save(cdmTrade);
			log.info("✅ Successfully saved CDM trade: {}", cdmTrade.getId());
		} catch (Exception e) {
			log.error("❌ Failed to save CDM trade: {}", e.getMessage(), e);
		}
		
		// Ensure CDM table exists
		if (!cdmTableInitialized) {
			initializeCdmTable();
		}
		
		// Create CDM Trade
		CdmTrade cdmTrade = new CdmTrade();
		cdmTrade.setAccountId(order.getAccountId());
		cdmTrade.setId(UUID.randomUUID().toString());
		cdmTrade.setCreated(new Date());
		cdmTrade.setUpdated(new Date());
		cdmTrade.setSecurity(order.getSecurity());
		cdmTrade.setSide(order.getSide());
		cdmTrade.setQuantity(order.getQuantity());
		cdmTrade.setState("CDM_PROCESSED");
		
		// Create CDM JSON (simplified for demo)
		String cdmJson = "{\"tradeId\":\"" + cdmTrade.getId() + "\",\"cdmVersion\":\"6.0.0\",\"businessEventType\":\"EXECUTION\"}";
		cdmTrade.setCdmTrade(cdmJson);
		
		try {
			cdmTradeRepository.save(cdmTrade);
			log.info("✅ Saved CDM trade: {}", cdmTrade.getId());
		} catch (Exception e) {
			log.error("❌ Failed to save CDM trade: {}", e.getMessage());
			// Try to initialize table and retry
			initializeCdmTable();
			try {
				cdmTradeRepository.save(cdmTrade);
				log.info("✅ Saved CDM trade after table creation: {}", cdmTrade.getId());
			} catch (Exception e2) {
				log.error("❌ CDM save failed even after table creation: {}", e2.getMessage());
			}
		}
		
		// Also create legacy trade for compatibility
        Trade t=new Trade();
        t.setAccountId(order.getAccountId());

		log.info("Setting a random TradeID");
		t.setId(UUID.randomUUID().toString());


        t.setCreated(new Date());
        t.setUpdated(new Date());
        t.setSecurity(order.getSecurity());
        t.setSide(order.getSide());
        t.setQuantity(order.getQuantity());
		t.setState(TradeState.New);
		Position position=positionRepository.findByAccountIdAndSecurity(order.getAccountId(), order.getSecurity());
		log.info("Position for "+order.getAccountId()+" "+order.getSecurity()+" is "+position);
		if(position==null) {
			log.info("Creating new position for "+order.getAccountId()+" "+order.getSecurity());
			position=new Position();
			position.setAccountId(order.getAccountId());
			position.setSecurity(order.getSecurity());
			position.setQuantity(0);
		}
		int newQuantity=((order.getSide()==TradeSide.Buy)?1:-1)*t.getQuantity();
		position.setQuantity(position.getQuantity()+newQuantity);
		log.info("Trade {}",t);
		tradeRepository.save(t);
		positionRepository.save(position);
		// Simulate the handling of this trade...
		// Now mark as processing
		t.setUpdated(new Date());
		t.setState(TradeState.Processing);
		// Now mark as settled
		t.setUpdated(new Date());
		t.setState(TradeState.Settled);
		tradeRepository.save(t);
		

		TradeBookingResult result=new TradeBookingResult(t, position);
		log.info("Trade Processing complete : "+result);
		try{
			log.info("Publishing : "+result);
			tradePublisher.publish("/accounts/"+order.getAccountId()+"/trades", result.getTrade());
			positionPublisher.publish("/accounts/"+order.getAccountId()+"/positions", result.getPosition());
		} catch (PubSubException exc){
			log.error("Error publishing trade "+order,exc);
		}
		
		return result;	
	}
	
	@Transactional
	private void initializeCdmTable() {
		if (cdmTableInitialized) return;
		
		try {
			log.info("🚀 Creating CDM table...");
			
			// First check if table exists
			try {
				entityManager.createNativeQuery("SELECT COUNT(*) FROM CDMTRADES").getSingleResult();
				log.info("✅ CDMTRADES table already exists");
				cdmTableInitialized = true;
				return;
			} catch (Exception e) {
				log.info("📋 CDMTRADES table does not exist, creating...");
			}
			
			// Create the table
			entityManager.createNativeQuery(
				"CREATE TABLE CDMTRADES ( " +
				"ID VARCHAR(50) PRIMARY KEY, " +
				"ACCOUNTID INTEGER, " +
				"CREATED TIMESTAMP, " +
				"UPDATED TIMESTAMP, " +
				"SECURITY VARCHAR(15), " +
				"SIDE VARCHAR(10), " +
				"QUANTITY INTEGER, " +
				"STATE VARCHAR(20), " +
				"CDMTRADEOBJ VARCHAR(2000) " +
				")"
			).executeUpdate();
			
			cdmTableInitialized = true;
			log.info("✅ CDM table created successfully");
			
		} catch (Exception e) {
			log.error("❌ Failed to create CDM table: {}", e.getMessage(), e);
		}
	}

}
