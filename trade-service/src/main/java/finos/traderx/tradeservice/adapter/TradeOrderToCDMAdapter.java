package finos.traderx.tradeservice.adapter;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

// FINOS CDM-compliant JSON structures
// Reference: https://github.com/finos/common-domain-model
// Reference: https://cdm.finos.org/docs/event-model/
// 
// Note: This implementation creates CDM-compliant JSON structures following
// the official FINOS CDM Event Model specification without requiring
// the full CDM Java library dependencies

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FINOS CDM adapter based on actual CDM 6.0.0 specification
 * Reference: https://cdm.finos.org/docs/event-model/
 * GitHub: https://github.com/finos/common-domain-model
 * 
 * Note: This is a simplified implementation for demonstration.
 * Real CDM integration requires proper understanding of the full CDM model.
 */
@Component
public class TradeOrderToCDMAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(TradeOrderToCDMAdapter.class);
    
    /**
     * Create a CDM-compliant JSON representation of a trade
     * This follows the CDM Event Model structure but uses JSON for simplicity
     * In a full implementation, you would use the actual CDM Java objects
     */
    public String createCDMCompliantTradeJSON(TradeOrder tradeOrder) {
        log.info("🔄 Creating CDM-compliant JSON for TradeOrder: {}", tradeOrder.getId());
        
        try {
            // Create CDM Event Model compliant structure
            // Based on actual CDM BusinessEvent structure from FINOS CDM docs
            String cdmJson = String.format("""
                {
                  "eventIdentifier": [
                    {
                      "assignedIdentifier": [
                        {
                          "identifier": {
                            "value": "EVENT-%s-%d"
                          },
                          "version": 1
                        }
                      ]
                    }
                  ],
                  "eventDate": "%s",
                  "eventTime": "%s",
                  "intent": {
                    "intent": "Execution"
                  },
                  "primitives": [
                    {
                      "execution": {
                        "after": {
                          "tradeIdentifier": [
                            {
                              "assignedIdentifier": [
                                {
                                  "identifier": {
                                    "value": "%s"
                                  },
                                  "version": 1
                                }
                              ]
                            }
                          ],
                          "tradableProduct": {
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
                            "tradeLot": [
                              {
                                "priceQuantity": [
                                  {
                                    "quantity": [
                                      {
                                        "amount": %d,
                                        "unit": {
                                          "currency": {
                                            "value": "USD"
                                          }
                                        }
                                      }
                                    ]
                                  }
                                ]
                              }
                            ],
                            "counterparty": [
                              {
                                "role": "Party1",
                                "partyReference": {
                                  "partyId": [
                                    {
                                      "identifier": {
                                        "value": "ACCOUNT-%d"
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          },
                          "tradeDate": {
                            "value": "%s"
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
                          ]
                        }
                      }
                    }
                  ],
                  "after": {
                    "trade": {
                      "tradeIdentifier": [
                        {
                          "assignedIdentifier": [
                            {
                              "identifier": {
                                "value": "%s"
                              }
                            }
                          ]
                        }
                      ]
                    },
                    "state": {
                      "positionState": "Executed"
                    }
                  },
                  "cdmVersion": "6.0.0",
                  "eventModel": "FINOS CDM Event Model"
                }
                """,
                tradeOrder.getId(), System.currentTimeMillis(),
                LocalDate.now().toString(),
                ZonedDateTime.now().toString(),
                tradeOrder.getId(),
                tradeOrder.getSecurity(),
                tradeOrder.getQuantity(),
                tradeOrder.getAccountId(),
                LocalDate.now().toString(),
                tradeOrder.getAccountId(),
                tradeOrder.getId()
            );
            
            log.info("✅ Created CDM-compliant JSON for {} shares of {}", 
                tradeOrder.getQuantity(), tradeOrder.getSecurity());
            return cdmJson;
            
        } catch (Exception e) {
            log.error("❌ Error creating CDM-compliant JSON", e);
            throw new RuntimeException("Failed to create CDM-compliant JSON", e);
        }
    }
    

    
    /**
     * Create a simplified CDM Trade representation
     * This demonstrates the CDM structure without full complexity
     */
    public String createSimplifiedCDMTrade(TradeOrder tradeOrder) {
        log.info("🏗️ Creating simplified CDM Trade representation: {}", tradeOrder.getId());
        
        try {
            String cdmTrade = String.format("""
                {
                  "tradeIdentifier": [
                    {
                      "assignedIdentifier": [
                        {
                          "identifier": {
                            "value": "%s"
                          },
                          "version": 1
                        }
                      ]
                    }
                  ],
                  "tradableProduct": {
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
                    "tradeLot": [
                      {
                        "priceQuantity": [
                          {
                            "quantity": [
                              {
                                "amount": %d,
                                "unit": {
                                  "currency": {
                                    "value": "USD"
                                  }
                                }
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  },
                  "tradeDate": {
                    "value": "%s"
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
                  "side": "%s"
                }
                """,
                tradeOrder.getId(),
                tradeOrder.getSecurity(),
                tradeOrder.getQuantity(),
                LocalDate.now().toString(),
                tradeOrder.getAccountId(),
                tradeOrder.getSide().toString()
            );
            
            log.info("✅ Created simplified CDM Trade for {} shares of {}", 
                tradeOrder.getQuantity(), tradeOrder.getSecurity());
            return cdmTrade;
            
        } catch (Exception e) {
            log.error("❌ Error creating simplified CDM Trade", e);
            throw new RuntimeException("Failed to create CDM Trade", e);
        }
    }

    
    /**
     * Get current trade order from context (thread-local or passed parameter)
     * This is a helper method to access the current trade being processed
     */
    private TradeOrder getCurrentTradeOrder() {
        // In a real implementation, this would get the trade from thread-local context
        // or be passed as a parameter. For now, return null and handle gracefully.
        return currentTradeOrder.get();
    }
    
    // Thread-local to store current trade order being processed
    private static final ThreadLocal<TradeOrder> currentTradeOrder = new ThreadLocal<>();
    
    /**
     * Set the current trade order for CDM processing
     */
    public void setCurrentTradeOrder(TradeOrder tradeOrder) {
        currentTradeOrder.set(tradeOrder);
    }
    
    /**
     * Clear the current trade order context
     */
    public void clearCurrentTradeOrder() {
        currentTradeOrder.remove();
    }
    
    /**
     * Validate CDM JSON structure against expected CDM format
     */
    public boolean validateCDMStructure(String cdmJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(cdmJson);
            
            // Check for key CDM Event Model fields
            boolean hasEventIdentifier = jsonNode.has("eventIdentifier");
            boolean hasEventDate = jsonNode.has("eventDate");
            boolean hasPrimitives = jsonNode.has("primitives");
            boolean hasCdmVersion = jsonNode.has("cdmVersion");
            
            log.info("CDM validation - eventIdentifier: {}, eventDate: {}, primitives: {}, cdmVersion: {}", 
                hasEventIdentifier, hasEventDate, hasPrimitives, hasCdmVersion);
            
            return hasCdmVersion && (hasEventIdentifier || hasEventDate);
            
        } catch (Exception e) {
            log.error("❌ Error validating CDM JSON structure", e);
            return false;
        }
    }

    /**
     * Get CDM compliance information
     */
    public String getCDMComplianceInfo() {
        return String.format("""
            {
              "cdmVersion": "6.0.0",
              "framework": "FINOS Common Domain Model",
              "documentation": "https://cdm.finos.org/docs/event-model/",
              "github": "https://github.com/finos/common-domain-model",
              "implementation": "Simplified CDM-compliant JSON structure",
              "compliance": "Follows CDM Event Model specification",
              "note": "This is a demonstration implementation. Full CDM integration requires the complete CDM Java library and proper Rosetta DSL setup."
            }
            """);
    }
    
    /**
     * Create enhanced CDM JSON representation following FINOS CDM Event Model
     * Reference: https://cdm.finos.org/docs/event-model
     */
    private String createFallbackCDMJson(String eventInfo) {
        try {
            TradeOrder tradeOrder = getCurrentTradeOrder();
            
            if (tradeOrder != null) {
                // Create CDM Event Model compliant JSON structure
                return String.format(
                    "{\n" +
                    "  \"cdmVersion\": \"6.0.0\",\n" +
                    "  \"eventModel\": \"FINOS CDM Event Model\",\n" +
                    "  \"eventDate\": \"%s\",\n" +
                    "  \"eventQualifier\": \"%s\",\n" +
                    "  \"businessEvent\": {\n" +
                    "    \"eventIdentifier\": \"EVENT-%s\",\n" +
                    "    \"tradeExecution\": {\n" +
                    "      \"tradeId\": \"%s\",\n" +
                    "      \"security\": \"%s\",\n" +
                    "      \"quantity\": %d,\n" +
                    "      \"side\": \"%s\",\n" +
                    "      \"accountId\": %d,\n" +
                    "      \"executionType\": \"MARKET_ORDER\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"primitiveEvent\": {\n" +
                    "    \"execution\": {\n" +
                    "      \"executionType\": \"NEW_TRADE\",\n" +
                    "      \"after\": {\n" +
                    "        \"tradeIdentifier\": \"%s\",\n" +
                    "        \"tradableProduct\": \"%s\",\n" +
                    "        \"quantity\": %d\n" +
                    "      }\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"timestamp\": \"%s\"\n" +
                    "}",
                    LocalDate.now().toString(),
                    "NewTrade",
                    tradeOrder.getId(),
                    tradeOrder.getId(),
                    tradeOrder.getSecurity(),
                    tradeOrder.getQuantity(),
                    tradeOrder.getSide(),
                    tradeOrder.getAccountId(),
                    tradeOrder.getId(),
                    tradeOrder.getSecurity(),
                    tradeOrder.getQuantity(),
                    java.time.Instant.now().toString()
                );
            } else {
                // Minimal CDM structure when no trade context
                return String.format(
                    "{\n" +
                    "  \"cdmVersion\": \"6.0.0\",\n" +
                    "  \"eventModel\": \"FINOS CDM Event Model\",\n" +
                    "  \"eventDate\": \"%s\",\n" +
                    "  \"eventQualifier\": \"%s\",\n" +
                    "  \"timestamp\": \"%s\"\n" +
                    "}",
                    LocalDate.now().toString(),
                    "NewTrade",
                    java.time.Instant.now().toString()
                );
            }
        } catch (Exception e) {
            log.error("❌ Error creating enhanced CDM JSON", e);
            return "{\"error\":\"Failed to serialize CDM event\",\"cdmVersion\":\"6.0.0\",\"eventModel\":\"FINOS CDM Event Model\"}";
        }
    }
    
    /**
     * Create complete CDM representation following FINOS CDM Event Model
     */
    public String createCDMTradeJSON(TradeOrder tradeOrder) {
        try {
            setCurrentTradeOrder(tradeOrder);
            
            // Create full CDM BusinessEvent structure
            return createCDMCompliantTradeJSON(tradeOrder);
            
        } catch (Exception e) {
            log.error("❌ Error creating CDM trade JSON", e);
            return createFallbackCDMJson(null);
        } finally {
            clearCurrentTradeOrder();
        }
    }

    /**
     * Create CDM Trade JSON (just the trade, not the business event)
     */
    public String createCDMTradeOnlyJSON(TradeOrder tradeOrder) {
        try {
            return createSimplifiedCDMTrade(tradeOrder);
        } catch (Exception e) {
            log.error("❌ Error creating CDM trade-only JSON", e);
            throw new RuntimeException("Failed to create CDM Trade JSON", e);
        }
    }
}