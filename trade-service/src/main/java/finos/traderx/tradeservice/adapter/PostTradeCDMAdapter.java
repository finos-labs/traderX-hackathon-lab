package finos.traderx.tradeservice.adapter;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

// FINOS CDM Real Implementation Imports
// Based on the skeleton code provided for real CDM integration
import cdm.base.datetime.AdjustableDates;
import cdm.base.math.FinancialUnitEnum;
import cdm.base.math.NonNegativeQuantitySchedule;
import cdm.base.math.UnitType;
import cdm.base.math.metafields.ReferenceWithMetaNonNegativeQuantitySchedule;
import cdm.base.staticdata.asset.common.*;
import cdm.base.staticdata.identifier.AssignedIdentifier;
import cdm.base.staticdata.party.*;
import cdm.event.common.*;
import cdm.event.position.PositionStatusEnum;
import cdm.observable.asset.PriceSchedule;
import cdm.observable.asset.PriceTypeEnum;
import cdm.observable.asset.metafields.ReferenceWithMetaPriceSchedule;
import cdm.product.common.settlement.*;
import cdm.product.template.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.records.Date;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Post-Trade CDM Adapter for real FINOS CDM integration
 * 
 * This adapter creates actual CDM TradeState objects using the real CDM Java library
 * for post-trade processing workflows. Based on the skeleton code provided by Alston.
 * 
 * Reference: CDM Java 6.0.0
 * Purpose: POC for real CDM implementation in TraderX
 */
@Component
public class PostTradeCDMAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(PostTradeCDMAdapter.class);
    
    // Default values for CDM trade creation
    private static final String DEFAULT_CURRENCY = "USD";
    private static final Double DEFAULT_PRICE = 100.0; // Assume $100 for simplicity
    private static final String TRADERX_PARTY_ID = "TraderX";
    private static final String TRADERX_PARTY_NAME = "Trader X Platform";
    
    /**
     * Create a CDM TradeState from a TradeX TradeOrder
     * This is the main method for post-trade processing
     */
    public TradeState createCDMTradeState(TradeOrder tradeOrder) {
        log.info("🔄 Creating CDM TradeState for TradeOrder: {}", tradeOrder.getId());
        
        try {
            // Map TradeX fields to CDM parameters
            String tradeId = tradeOrder.getId();
            int accountId = tradeOrder.getAccountId();
            String security = tradeOrder.getSecurity();
            TradeSide side = tradeOrder.getSide();
            int quantity = tradeOrder.getQuantity();
            
            // Create CDM TradeState with mapped values
            TradeState tradeState = createTradeState(
                tradeId, 
                accountId, 
                security, 
                side, 
                quantity
            );
            
            log.info("✅ Created CDM TradeState for {} shares of {} ({})", 
                quantity, security, side);
            
            return tradeState;
            
        } catch (Exception e) {
            log.error("❌ Error creating CDM TradeState for TradeOrder: {}", tradeOrder.getId(), e);
            throw new RuntimeException("Failed to create CDM TradeState", e);
        }
    }
    
    /**
     * Create CDM TradeState with specific parameters
     * Based on the skeleton code provided
     */
    public TradeState createTradeState(String tradeId, int accountId, String security, TradeSide side, int quantity) {
        log.debug("Creating CDM TradeState - ID: {}, Account: {}, Security: {}, Side: {}, Quantity: {}", 
            tradeId, accountId, security, side, quantity);
        
        // Map TradeX data to CDM parameters
        String partyId = "ACCOUNT-" + accountId;
        String partyName = "Account " + accountId;
        String securityId = security;
        String securityName = getSecurityName(security);
        boolean isBuy = (side == TradeSide.Buy);
        String currency = DEFAULT_CURRENCY;
        Double quantityDouble = (double) quantity;
        Double price = DEFAULT_PRICE;
        LocalDate tradeDate = LocalDate.now();
        LocalDate settleDate = LocalDate.now().plusDays(2); // T+2 settlement
        
        // Step 1: Create Parties involved in the trade
        Party partyA = Party.builder()
            .addPartyId(PartyIdentifier.builder().setIdentifierValue(partyId))
            .setNameValue(partyName)
            .build();
        
        Party partyB = Party.builder()
            .addPartyId(PartyIdentifier.builder().setIdentifierValue(TRADERX_PARTY_ID))
            .setNameValue(TRADERX_PARTY_NAME)
            .build();
        
        List<Party> parties = List.of(partyA, partyB);
        
        // Step 2: Define Party Roles
        PartyRole partyARole = PartyRole.builder()
            .setRole(isBuy ? PartyRoleEnum.BUYER : PartyRoleEnum.SELLER)
            .build();
        
        PartyRole partyBRole = PartyRole.builder()
            .setRole(isBuy ? PartyRoleEnum.SELLER : PartyRoleEnum.BUYER)
            .build();
        
        List<PartyRole> partyRoles = List.of(partyARole, partyBRole);
        
        // Step 3: Set Counterparty roles
        Counterparty counterpartyA = Counterparty.builder()
            .setPartyReferenceValue(partyA)
            .setRole(CounterpartyRoleEnum.PARTY_1)
            .build();
        
        Counterparty counterpartyB = Counterparty.builder()
            .setPartyReferenceValue(partyB)
            .setRole(CounterpartyRoleEnum.PARTY_2)
            .build();
        
        // Step 4: Create Financial Instrument
        Instrument instrument = Instrument.builder()
            .setSecurity(Security.builder()
                .setInstrumentType(InstrumentTypeEnum.EQUITY)
                .setFundType(FundProductTypeEnum.EXCHANGE_TRADED_FUND)
                .setEquityType(EquityTypeEnum.ORDINARY)
                .addIdentifier(AssetIdentifier.builder()
                    .setIdentifierType(AssetIdTypeEnum.BBGTICKER)
                    .setIdentifierValue(securityId))
                .addIdentifier(AssetIdentifier.builder()
                    .setIdentifierType(AssetIdTypeEnum.NAME)
                    .setIdentifierValue(securityName)))
            .build();
        
        // Step 5: Define Quantities
        NonNegativeQuantitySchedule quantitySchedule1 = NonNegativeQuantitySchedule.builder()
            .setValue(BigDecimal.valueOf(quantityDouble))
            .setUnit(UnitType.builder().setFinancialUnit(FinancialUnitEnum.SHARE))
            .build();
        
        NonNegativeQuantitySchedule quantitySchedule2 = NonNegativeQuantitySchedule.builder()
            .setValue(BigDecimal.valueOf(price * quantityDouble))
            .setUnit(UnitType.builder().setCurrencyValue(currency))
            .build();
        
        // Step 6: Define Price
        PriceSchedule priceSchedule = PriceSchedule.builder()
            .setValue(BigDecimal.valueOf(price))
            .setUnit(UnitType.builder().setCurrencyValue(currency))
            .setPerUnitOf(UnitType.builder().setFinancialUnit(FinancialUnitEnum.SHARE))
            .setPriceType(PriceTypeEnum.ASSET_PRICE)
            .build();
        
        // Step 7: Define Settlement
        SettlementDate settlementDate = SettlementDate.builder()
            .setAdjustableDates(AdjustableDates.builder()
                .addAdjustedDateValue(Date.of(settleDate)))
            .build();
        
        SettlementTerms settlementTerms = SettlementTerms.builder()
            .setSettlementType(SettlementTypeEnum.CASH)
            .setTransferSettlementType(TransferSettlementEnum.DELIVERY_VERSUS_PAYMENT)
            .setSettlementDate(settlementDate)
            .build();
        
        // Step 8: Create Price Quantity relationship
        ResolvablePriceQuantity priceQuantity = ResolvablePriceQuantity.builder()
            .addPriceSchedule(ReferenceWithMetaPriceSchedule.builder()
                .setReference(Reference.builder().setReference("PriceSchedule"))
                .setValue(priceSchedule))
            .setQuantitySchedule(ReferenceWithMetaNonNegativeQuantitySchedule.builder()
                .setReference(Reference.builder().setReference("QuantitySchedule"))
                .setValue(quantitySchedule1))
            .build();
        
        // Step 9: Create Economic Terms
        EconomicTerms economicTerms = EconomicTerms.builder()
            .addPayout(Payout.builder()
                .setSettlementPayout(SettlementPayout.builder()
                    .setPayerReceiver(PayerReceiver.builder()
                        .setPayer(CounterpartyRoleEnum.PARTY_1)
                        .setReceiver(CounterpartyRoleEnum.PARTY_2))
                    .setUnderlier(Underlier.builder()
                        .setProduct(Product.builder()
                            .setTransferableProduct(TransferableProduct.builder()
                                .setInstrument(instrument))))
                    .setSettlementTerms(settlementTerms)
                    .setPriceQuantity(priceQuantity)))
            .build();
        
        // Step 10: Create Product
        Product product = Product.builder()
            .setTransferableProduct(TransferableProduct.builder()
                .setInstrument(instrument)
                .setEconomicTerms(economicTerms))
            .build();
        
        // Step 11: Create Trade Identifier
        TradeIdentifier tradeIdentifier = TradeIdentifier.builder()
            .addAssignedIdentifier(AssignedIdentifier.builder()
                .setIdentifierValue(tradeId))
            .build();
        
        // Step 12: Create Trade
        Trade trade = Trade.builder()
            .addParty(parties)
            .addPartyRole(partyRoles)
            .addCounterparty(counterpartyA)
            .addCounterparty(counterpartyB)
            .setExecutionDetails(ExecutionDetails.builder()
                .setExecutionType(ExecutionTypeEnum.ELECTRONIC))
            .setTradeDateValue(Date.of(tradeDate))
            .addTradeIdentifier(tradeIdentifier)
            .setProduct(NonTransferableProduct.builder()
                .setEconomicTerms(economicTerms))
            .build();
        
        // Step 13: Create TradeState
        TradeState tradeState = TradeState.builder()
            .setState(State.builder()
                .setPositionState(PositionStatusEnum.SETTLED))
            .setTrade(trade)
            .build();
        
        log.debug("✅ CDM TradeState created successfully");
        return tradeState;
    }
    
    /**
     * Convert CDM TradeState to JSON using Rosetta Object Mapper
     */
    public String convertTradeStateToJSON(TradeState tradeState) {
        log.debug("Converting CDM TradeState to JSON");
        
        try {
            ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tradeState);
            
            log.debug("✅ CDM TradeState converted to JSON successfully");
            return json;
            
        } catch (Exception e) {
            log.error("❌ Error converting CDM TradeState to JSON", e);
            throw new RuntimeException("Failed to convert CDM TradeState to JSON", e);
        }
    }
    
    /**
     * Create CDM JSON representation from TradeOrder
     * This combines TradeState creation and JSON conversion
     */
    public String createCDMTradeJSON(TradeOrder tradeOrder) {
        log.info("🔄 Creating CDM JSON for TradeOrder: {}", tradeOrder.getId());
        
        try {
            TradeState tradeState = createCDMTradeState(tradeOrder);
            String json = convertTradeStateToJSON(tradeState);
            
            log.info("✅ Created CDM JSON for {} shares of {} ({})", 
                tradeOrder.getQuantity(), tradeOrder.getSecurity(), tradeOrder.getSide());
            
            return json;
            
        } catch (Exception e) {
            log.error("❌ Error creating CDM JSON for TradeOrder: {}", tradeOrder.getId(), e);
            throw new RuntimeException("Failed to create CDM JSON", e);
        }
    }
    
    /**
     * Validate CDM TradeState structure
     */
    public boolean validateCDMTradeState(TradeState tradeState) {
        try {
            boolean hasState = tradeState.getState() != null;
            boolean hasTrade = tradeState.getTrade() != null;
            boolean hasTradeId = hasTrade && 
                tradeState.getTrade().getTradeIdentifier() != null && 
                !tradeState.getTrade().getTradeIdentifier().isEmpty();
            
            log.debug("CDM TradeState validation - hasState: {}, hasTrade: {}, hasTradeId: {}", 
                hasState, hasTrade, hasTradeId);
            
            return hasState && hasTrade && hasTradeId;
            
        } catch (Exception e) {
            log.error("❌ Error validating CDM TradeState", e);
            return false;
        }
    }
    
    /**
     * Get security name from ticker (simplified mapping)
     */
    private String getSecurityName(String ticker) {
        // In a real implementation, this would lookup from a reference data service
        return switch (ticker.toUpperCase()) {
            case "IBM" -> "International Business Machines Corp";
            case "AAPL" -> "Apple Inc";
            case "MSFT" -> "Microsoft Corporation";
            case "GOOGL" -> "Alphabet Inc";
            case "TSLA" -> "Tesla Inc";
            default -> ticker + " Corporation";
        };
    }
    
    /**
     * Get CDM adapter information
     */
    public String getCDMAdapterInfo() {
        return String.format("""
            {
              "adapter": "PostTradeCDMAdapter",
              "cdmVersion": "6.0.0",
              "framework": "FINOS Common Domain Model",
              "implementation": "Real CDM Java Library",
              "purpose": "Post-trade processing with CDM compliance",
              "features": [
                "Real CDM TradeState creation",
                "Rosetta Object Mapper integration",
                "TradeX to CDM mapping",
                "Post-trade workflow support"
              ],
              "documentation": "https://cdm.finos.org/docs/",
              "github": "https://github.com/finos/common-domain-model"
            }
            """);
    }
    
    /**
     * Create CDM BusinessEvent for post-trade processing
     * This wraps the TradeState in a proper CDM BusinessEvent
     */
    public BusinessEvent createCDMBusinessEvent(TradeOrder tradeOrder) {
        log.info("🔄 Creating CDM BusinessEvent for TradeOrder: {}", tradeOrder.getId());
        
        try {
            TradeState tradeState = createCDMTradeState(tradeOrder);
            
            // Create BusinessEvent wrapper
            BusinessEvent businessEvent = BusinessEvent.builder()
                .addEventIdentifier(Identifier.builder()
                    .addAssignedIdentifier(AssignedIdentifier.builder()
                        .setIdentifierValue("EVENT-" + tradeOrder.getId() + "-" + System.currentTimeMillis())))
                .setEventDate(Date.of(LocalDate.now()))
                .setIntent(EventIntentEnum.EXECUTION)
                .addAfter(tradeState)
                .build();
            
            log.info("✅ Created CDM BusinessEvent for TradeOrder: {}", tradeOrder.getId());
            return businessEvent;
            
        } catch (Exception e) {
            log.error("❌ Error creating CDM BusinessEvent for TradeOrder: {}", tradeOrder.getId(), e);
            throw new RuntimeException("Failed to create CDM BusinessEvent", e);
        }
    }
    
    /**
     * Convert CDM BusinessEvent to JSON
     */
    public String convertBusinessEventToJSON(BusinessEvent businessEvent) {
        try {
            ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(businessEvent);
        } catch (Exception e) {
            log.error("❌ Error converting CDM BusinessEvent to JSON", e);
            throw new RuntimeException("Failed to convert CDM BusinessEvent to JSON", e);
        }
    }
}