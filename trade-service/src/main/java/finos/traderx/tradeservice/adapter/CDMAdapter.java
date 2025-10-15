package finos.traderx.tradeservice.adapter;

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
public class PostAdapter {
  public static void main(String[] args) {
    TradeState tradeState= createTradeState();
    System.out.println( rosettaObjectToJSON(tradeState) );
  }
  public static TradeState createTradeState(){
    // Create a CDM trade object That represents a simple Equity transaction
    // TODO: Map from the TradeXTrade Object
    // String id, int accountId, String security, TradeSide side, int quantity
    // TODO: Get from <accountId>
    String partyId = "Bank-A";
    String partyName = "Bank of Alpha";
    // TODO: Get from <security>
    String securityId = "IBM";
    String securityName = "International Business Machines Corp";
    // TODO: Get from <side>
    String side = "Buy";
    // Assume Buy for now
    boolean isBuy = true;
    // TODO: Get from <currency>
    String currency = "USD";
    // TODO: Get from <quantity>
    Double quantity = 10D;
    // TODO: Get from <id>
    String tradeId = "123";
    // Assumptions for other trade details
    Double price = 100D;          // lets assume socks are prices at $100 for simplicity
    LocalDate tradeDate = LocalDate.now();
    LocalDate settleDate = LocalDate.now();
    // Step: Create Parties involved in the trade
    // party from <accountId>
    Party partyA = Party.builder()
        .addPartyId(PartyIdentifier.builder().setIdentifierValue(partyId))
        .setNameValue(partyName)
        .build();
    // TraderX represents an electronic trading platform
    Party partyB = Party.builder()
        .addPartyId(PartyIdentifier.builder().setIdentifierValue("TraderX"))
        .setNameValue("Trader X Platform")
        .build();
    List<Party> parties = List.of(partyA, partyB);
    PartyRole partyARole = PartyRole.builder().setRole( isBuy ? PartyRoleEnum.BUYER : PartyRoleEnum.SELLER);
    PartyRole partyBRole = PartyRole.builder().setRole(isBuy ? PartyRoleEnum.SELLER : PartyRoleEnum.BUYER);
    List<PartyRole> partyRoles = List.of(partyARole, partyBRole);
    // Set the Role of the parties (during Execution)
    Counterparty counterpartyA = Counterparty.builder()
            .setPartyReferenceValue(partyA)
            .setRole(CounterpartyRoleEnum.PARTY_1)
            .build();
    Counterparty counterpartyB = Counterparty.builder()
            .setPartyReferenceValue(partyB)
            .setRole(CounterpartyRoleEnum.PARTY_1)
            .build();
    // Step: Create a Product (define the type of Financial Instrument traded)
    // Define the Asset/Instrument being Traded (the product)
    Instrument instrument = Instrument.builder()
        .setSecurity(Security.builder()
            .setInstrumentType(InstrumentTypeEnum.EQUITY)
            .setFundType(FundProductTypeEnum.EXCHANGE_TRADED_FUND)
            .setEquityType(EquityTypeEnum.ORDINARY)
            .addIdentifier(AssetIdentifier.builder()
                .setIdentifierType(AssetIdTypeEnum.BBGTICKER)
                .setIdentifierValue(securityId))                  // Stock Ticker
            .addIdentifier(AssetIdentifier.builder()
                .setIdentifierType(AssetIdTypeEnum.NAME)
                .setIdentifierValue(securityName)))                // Stock Name
        .build();
    // Define the Quantity - Equity Quantity
    NonNegativeQuantitySchedule quantitySchedule1 = NonNegativeQuantitySchedule.builder()
        .setValue(BigDecimal.valueOf(quantity))                      // Quantity
        .setUnit(UnitType.builder().setFinancialUnit(FinancialUnitEnum.SHARE))     // Units
        .build();
    // Define the Quantity - Currency Quantity
    NonNegativeQuantitySchedule quantitySchedule2 = NonNegativeQuantitySchedule.builder()
        .setValue(BigDecimal.valueOf(price * quantity))                     // Quantity
        .setUnit(UnitType.builder().setCurrencyValue(currency))              // Units
        .build();
    // Define the Price of the product being traded
    PriceSchedule priceSchedule = PriceSchedule.builder()
        .setValue(BigDecimal.valueOf(price))                     // Price (Open 15th Oct 2025)
        .setUnit(UnitType.builder().setCurrencyValue(currency))              // Price Currency
        .setPerUnitOf(UnitType.builder().setFinancialUnit(FinancialUnitEnum.SHARE))  // Per Unit
        .setPriceType(PriceTypeEnum.ASSET_PRICE)                    // Price Type
        .build();
    // Define the Settlement details of the product being traded
    SettlementDate settlementDate = SettlementDate.builder()
        .setAdjustableDates(AdjustableDates.builder()
            .addAdjustedDateValue(Date.of(settleDate)))  // Settlement Date
        .build();
    SettlementTerms settlementTerms = SettlementTerms.builder()
        .setSettlementType(SettlementTypeEnum.CASH)
        .setTransferSettlementType(TransferSettlementEnum.DELIVERY_VERSUS_PAYMENT)
        .setSettlementDate(settlementDate)
        .build();
    // Quantify the transaction in terms of Price and Quantity
    ResolvablePriceQuantity priceQuantity = ResolvablePriceQuantity.builder()
        .addPriceSchedule(ReferenceWithMetaPriceSchedule.builder()
            .setReference(Reference.builder().setReference("PriceSchedule"))
            .setValue(priceSchedule))
        .setQuantitySchedule(ReferenceWithMetaNonNegativeQuantitySchedule.builder()
            .setReference(Reference.builder().setReference("QuantitySchedule"))
            .setValue(quantitySchedule1))
        .build();
    // Create the Economic Terms with an Asset Payout (how this product is transacted)
    EconomicTerms economicTerms = EconomicTerms.builder()
        .addPayout(Payout.builder().setSettlementPayout(SettlementPayout.builder()
            .setPayerReceiver(PayerReceiver.builder()
                .setPayer(CounterpartyRoleEnum.PARTY_1)
                .setReceiver(CounterpartyRoleEnum.PARTY_2))
                .setUnderlier(Underlier.builder()
                    .setProduct(Product.builder()
                        .setTransferableProduct(TransferableProduct.builder()
                            .setInstrument(instrument))))
            .setSettlementTerms(settlementTerms)
            .setPriceQuantity(priceQuantity)
        )).build();
    // Create the Economic Terms (the economics of the trade)
    Product product = Product.builder()
        .setTransferableProduct(TransferableProduct.builder()
            .setInstrument(instrument)
            .setEconomicTerms(economicTerms))
        .build();
    TradeIdentifier tradeIdentifier = TradeIdentifier.builder()
        .addAssignedIdentifier(AssignedIdentifier.builder()
            .setIdentifierValue(tradeId));
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
    TradeState tradeState = TradeState.builder()
        .setState(State.builder()
            .setPositionState(PositionStatusEnum.SETTLED))
        .setTrade(trade)
        .build();
    return tradeState;
  }
  public static String rosettaObjectToJSON(Object object){
    try {
      ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
