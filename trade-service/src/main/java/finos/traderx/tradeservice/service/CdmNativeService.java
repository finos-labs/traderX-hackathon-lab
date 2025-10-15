package finos.traderx.tradeservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import finos.traderx.tradeservice.model.*;
import finos.traderx.tradeservice.repository.*;
import finos.traderx.tradeservice.adapter.TradeOrderToCDMAdapter;

import java.util.Date;
import java.util.UUID;

/**
 * CDM Native Service for processing trades through proper CDM workflow
 * This service uses CDM-native tables and follows FINOS CDM patterns
 */
@Service
public class CdmNativeService {
    
    private static final Logger log = LoggerFactory.getLogger(CdmNativeService.class);
    
    @Autowired
    private CdmTradeRepository cdmTradeRepository;
    
    @Autowired
    private CdmAccountRepository cdmAccountRepository;
    
    @Autowired
    private CdmPositionRepository cdmPositionRepository;
    
    @Autowired
    private TradeOrderToCDMAdapter cdmAdapter;
    
    /**
     * Process a trade through the CDM native workflow
     * This creates proper CDM entities and updates positions
     */
    public CdmTrade processCdmTrade(TradeOrder tradeOrder) {
        log.info("🏛️ Processing trade through CDM native workflow: {}", tradeOrder.getId());
        
        try {
            // 1. Ensure CDM Account exists
            CdmAccount cdmAccount = ensureCdmAccount(tradeOrder.getAccountId());
            
            // 2. Create CDM Trade with proper CDM BusinessEvent
            String cdmTradeJson = cdmAdapter.createCDMTradeJSON(tradeOrder);
            
            CdmTrade cdmTrade = new CdmTrade(
                "CDM-" + tradeOrder.getId(),
                tradeOrder.getAccountId(),
                tradeOrder.getSecurity(),
                tradeOrder.getSide(),
                tradeOrder.getQuantity(),
                "CDM_PROCESSED",
                cdmTradeJson
            );
            
            // 3. Save CDM Trade
            CdmTrade savedTrade = cdmTradeRepository.save(cdmTrade);
            log.info("✅ Saved CDM native trade: {}", savedTrade.getId());
            
            // 4. Update CDM Position
            updateCdmPosition(tradeOrder);
            
            return savedTrade;
            
        } catch (Exception e) {
            log.error("❌ Failed to process CDM native trade: {}", tradeOrder.getId(), e);
            throw new RuntimeException("CDM native processing failed", e);
        }
    }
    
    /**
     * Ensure CDM Account exists, create if not found
     */
    private CdmAccount ensureCdmAccount(Integer accountId) {
        CdmAccount cdmAccount = cdmAccountRepository.findById(accountId).orElse(null);
        
        if (cdmAccount == null) {
            log.info("🏗️ Creating new CDM Account: {}", accountId);
            
            // Create CDM Party JSON for the account
            String cdmPartyJson = String.format(
                "{\"name\":{\"value\":\"Account %d\"},\"partyId\":[{\"identifier\":{\"value\":\"%d\"}}]}",
                accountId, accountId
            );
            
            cdmAccount = new CdmAccount(accountId, "Account " + accountId, cdmPartyJson);
            cdmAccount = cdmAccountRepository.save(cdmAccount);
            
            log.info("✅ Created CDM Account: {}", cdmAccount.getId());
        }
        
        return cdmAccount;
    }
    
    /**
     * Update CDM Position based on trade
     */
    private void updateCdmPosition(TradeOrder tradeOrder) {
        log.info("📊 Updating CDM position for {}/{}", tradeOrder.getAccountId(), tradeOrder.getSecurity());
        
        try {
            CdmPosition position = cdmPositionRepository.findByAccountIdAndSecurity(
                tradeOrder.getAccountId(), 
                tradeOrder.getSecurity()
            );
            
            int quantityChange = tradeOrder.getQuantity();
            if (tradeOrder.getSide() == TradeSide.Sell) {
                quantityChange = -quantityChange;
            }
            
            if (position == null) {
                // Create new position
                String cdmPositionJson = createCdmPositionJson(tradeOrder, quantityChange);
                position = new CdmPosition(
                    tradeOrder.getAccountId(),
                    tradeOrder.getSecurity(),
                    new Date(),
                    quantityChange,
                    cdmPositionJson
                );
            } else {
                // Update existing position
                int newQuantity = position.getQuantity() + quantityChange;
                position.setQuantity(newQuantity);
                position.setUpdated(new Date());
                
                String cdmPositionJson = createCdmPositionJson(tradeOrder, newQuantity);
                position.setCdmPositionObj(cdmPositionJson);
            }
            
            cdmPositionRepository.save(position);
            log.info("✅ Updated CDM position: {} {} shares", position.getSecurity(), position.getQuantity());
            
        } catch (Exception e) {
            log.error("❌ Failed to update CDM position", e);
        }
    }
    
    /**
     * Create CDM Position JSON following FINOS CDM Position model
     */
    private String createCdmPositionJson(TradeOrder tradeOrder, int quantity) {
        return String.format("""
            {
              "positionIdentifier": [
                {
                  "identifier": {
                    "value": "POS-%d-%s"
                  }
                }
              ],
              "product": {
                "security": {
                  "securityType": "Equity",
                  "identifier": [
                    {
                      "identifier": {
                        "value": "%s"
                      },
                      "source": "TICKER"
                    }
                  ]
                }
              },
              "quantity": {
                "amount": %d,
                "unit": {
                  "currency": {
                    "value": "USD"
                  }
                }
              },
              "party": [
                {
                  "partyId": [
                    {
                      "identifier": {
                        "value": "ACCOUNT-%d"
                      }
                    }
                  ]
                }
              ],
              "cdmVersion": "6.0.0",
              "positionType": "LONG"
            }
            """,
            tradeOrder.getAccountId(),
            tradeOrder.getSecurity(),
            tradeOrder.getSecurity(),
            quantity,
            tradeOrder.getAccountId()
        );
    }
}