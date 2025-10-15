package finos.traderx.tradeprocessor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.repository.TradeRepository;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.messaging.Publisher;

import cdm.event.common.BusinessEvent;

/**
 * Service to process CDM BusinessEvents and convert them to TradeX models
 */
@Service
public class CDMTradeService {
    
    private static final Logger log = LoggerFactory.getLogger(CDMTradeService.class);
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private PositionRepository positionRepository;
    
    @Autowired
    private Publisher<Trade> tradePublisher;
    
    @Autowired
    private Publisher<Position> positionPublisher;
    
    @Value("${traderx.cdm.enabled:false}")
    private boolean cdmEnabled;
    
    @Value("${traderx.cdm.audit-trail:true}")
    private boolean auditTrail;
    
    /**
     * Process CDM BusinessEvent and convert to TradeX models
     */
    public void processCDMBusinessEvent(BusinessEvent businessEvent) {
        if (!cdmEnabled) {
            log.debug("CDM processing disabled, skipping BusinessEvent");
            return;
        }
        
        log.info("Processing CDM BusinessEvent with intent: {}", businessEvent.getIntent());
        
        try {
            // Simple CDM processing - just log for now
            log.info("📊 CDM BusinessEvent received: {}", businessEvent.getIntent());
            
            // For demo: create a simple trade record
            Trade trade = new Trade();
            trade.setId("CDM-" + System.currentTimeMillis());
            trade.setAccountId(22214); // Default account for demo
            trade.setSecurity("CDM-DEMO");
            trade.setQuantity(100);
            trade.setState(finos.traderx.tradeprocessor.model.TradeState.Settled);
            trade.setCreated(new java.util.Date());
            trade.setUpdated(new java.util.Date());
            
            // Store CDM JSON
            trade.setCdmJson(businessEvent.toString());
            
            tradeRepository.save(trade);
            log.info("Saved CDM-processed trade: {}", trade.getId());
            
            // Skip position update for demo
            Position position = null;
            
            // Publish events for frontend compatibility
            publishTradeEvents(trade, position);
            
            if (auditTrail) {
                log.info("CDM BusinessEvent processed successfully - Trade: {}, Position: {}", 
                    trade.getId(), position.getSecurity());
            }
            
        } catch (Exception e) {
            log.error("Error processing CDM BusinessEvent", e);
            throw new RuntimeException("Failed to process CDM BusinessEvent", e);
        }
    }
    
    /**
     * Update position based on trade
     */
    private Position updatePosition(Trade trade) {
        Position position = positionRepository.findByAccountIdAndSecurity(
            trade.getAccountId(), trade.getSecurity());
        
        if (position == null) {
            // Skip position creation for demo
            return null;
        } else {
            // Update existing position quantity
            int newQuantity = position.getQuantity() + trade.getQuantity();
            position.setQuantity(newQuantity);
        }
        
        positionRepository.save(position);
        return position;
    }
    

    
    /**
     * Publish events for frontend compatibility
     */
    private void publishTradeEvents(Trade trade, Position position) {
        try {
            // Publish trade event
            tradePublisher.publish("/accounts/" + trade.getAccountId() + "/trades", trade);
            
            // Publish position update
            positionPublisher.publish("/accounts/" + trade.getAccountId() + "/positions", position);
            
            log.debug("Published CDM-processed events for account: {}", trade.getAccountId());
            
        } catch (Exception e) {
            log.error("Error publishing CDM-processed events", e);
        }
    }
}