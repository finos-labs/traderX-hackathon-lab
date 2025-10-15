# Post-Trade CDM Integration

## Overview

The `PostTradeCDMAdapter` provides real FINOS CDM integration for post-trade processing workflows in TraderX. This adapter creates actual CDM `TradeState` and `BusinessEvent` objects using the real CDM Java library (cdm-java 6.0.0).

## Key Features

- **Real CDM Library Integration**: Uses actual FINOS CDM Java objects, not JSON mockups
- **Post-Trade Processing**: Designed for post-trade workflows and downstream system integration
- **Type-Safe CDM Construction**: Uses CDM builders for type-safe object creation
- **Rosetta Serialization**: Proper CDM JSON serialization using RosettaObjectMapper
- **TradeX Integration**: Seamless mapping from TradeX `TradeOrder` to CDM `TradeState`

## Architecture

```
TradeOrder (TraderX) → PostTradeCDMAdapter → CDM TradeState → CDM BusinessEvent → JSON
```

## API Endpoints

### Process Trade Through Real CDM Library
```http
POST /cdm/post-trade/process
Content-Type: application/json

{
  "id": "TRADE-123",
  "accountId": 12345,
  "security": "AAPL",
  "side": "Buy",
  "quantity": 100
}
```

**Response:**
```json
{
  "success": true,
  "tradeId": "TRADE-123",
  "cdmImplementation": "Real FINOS CDM Java Library",
  "cdmTradeState": "{ ... full CDM TradeState JSON ... }",
  "cdmBusinessEvent": "{ ... full CDM BusinessEvent JSON ... }",
  "postTradeWorkflow": true,
  "message": "✅ Post-trade CDM processing completed using real FINOS CDM library"
}
```

### Get All CDM Trades in Real CDM Format
```http
GET /cdm/post-trade/trades
```

**Response:**
```json
{
  "success": true,
  "totalTrades": 5,
  "cdmTrades": [
    {
      "tradeId": "TRADE-123",
      "cdmTradeState": "{ ... real CDM TradeState ... }",
      "cdmImplementation": "Real FINOS CDM Java Library",
      "postTradeReady": true
    }
  ]
}
```

### Compare JSON-based vs Real CDM Implementation
```http
POST /cdm/post-trade/compare
Content-Type: application/json

{
  "id": "TRADE-123",
  "accountId": 12345,
  "security": "AAPL",
  "side": "Buy",
  "quantity": 100
}
```

### Get Post-Trade CDM Adapter Information
```http
GET /cdm/post-trade/info
```

## CDM Object Structure

The adapter creates the following CDM objects:

### 1. CDM TradeState
- **State**: Position status (SETTLED)
- **Trade**: Complete trade information with parties, products, execution details

### 2. CDM BusinessEvent
- **Event Identifier**: Unique event ID
- **Event Date**: Trade date
- **Intent**: EXECUTION
- **After State**: TradeState after execution

### 3. CDM Trade Components
- **Parties**: Account party and TraderX platform party
- **Party Roles**: BUYER/SELLER based on trade side
- **Counterparties**: PARTY_1 and PARTY_2 roles
- **Instrument**: Equity security with identifiers
- **Economic Terms**: Settlement, pricing, and payout terms
- **Execution Details**: Electronic execution type

## Mapping from TradeX to CDM

| TradeX Field | CDM Mapping | Notes |
|--------------|-------------|-------|
| `id` | `TradeIdentifier.assignedIdentifier.identifierValue` | Unique trade ID |
| `accountId` | `Party.partyId.identifierValue` | Account as party |
| `security` | `Instrument.security.identifier.identifierValue` | Security ticker |
| `side` | `PartyRole.role` (BUYER/SELLER) | Trade direction |
| `quantity` | `NonNegativeQuantitySchedule.value` | Share quantity |
| Current date | `Trade.tradeDate` | Execution date |
| Default $100 | `PriceSchedule.value` | Assumed price |

## Usage Examples

### Basic Post-Trade Processing
```java
@Autowired
private PostTradeCDMAdapter postTradeCdmAdapter;

// Create CDM TradeState from TradeOrder
TradeState cdmTradeState = postTradeCdmAdapter.createCDMTradeState(tradeOrder);

// Convert to JSON for downstream systems
String cdmJson = postTradeCdmAdapter.convertTradeStateToJSON(cdmTradeState);

// Create BusinessEvent for event processing
BusinessEvent businessEvent = postTradeCdmAdapter.createCDMBusinessEvent(tradeOrder);
```

### CDM Validation
```java
// Validate CDM TradeState structure
boolean isValid = postTradeCdmAdapter.validateCDMTradeState(tradeState);

// Get adapter information
String adapterInfo = postTradeCdmAdapter.getCDMAdapterInfo();
```

## Dependencies

The adapter requires the following CDM dependencies in `build.gradle`:

```gradle
dependencies {
    implementation 'cdm:cdm-java:6.0.0'
    implementation 'com.regnosys.rosetta:rosetta-common:+'
}
```

## Differences from JSON-based CDM

| Aspect | JSON-based CDM | Real CDM Library |
|--------|----------------|------------------|
| **Implementation** | Manual JSON construction | Type-safe CDM builders |
| **Validation** | Basic JSON structure checks | Full CDM specification compliance |
| **Serialization** | Custom JSON formatting | Rosetta Object Mapper |
| **Extensibility** | Limited to predefined structure | Full CDM model extensibility |
| **Type Safety** | Runtime JSON validation | Compile-time type checking |
| **CDM Compliance** | Mimics CDM structure | True CDM specification adherence |

## Benefits for Post-Trade Processing

1. **Industry Standard Compliance**: Uses actual FINOS CDM specification
2. **Downstream Integration**: CDM-compliant output for regulatory reporting
3. **Event Processing**: Proper CDM BusinessEvent structure for event-driven workflows
4. **Regulatory Reporting**: CDM format accepted by regulatory systems
5. **Interoperability**: Standard format for cross-institution communication
6. **Future-Proof**: Follows evolving CDM specification

## Testing

Test the post-trade CDM processing:

```bash
# Test basic CDM processing
curl -X POST http://localhost:18080/cdm/post-trade/process \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST-123","accountId":12345,"security":"AAPL","side":"Buy","quantity":100}'

# Compare implementations
curl -X POST http://localhost:18080/cdm/post-trade/compare \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST-123","accountId":12345,"security":"AAPL","side":"Buy","quantity":100}'

# Get all CDM trades
curl http://localhost:18080/cdm/post-trade/trades

# Get adapter info
curl http://localhost:18080/cdm/post-trade/info
```

## Next Steps

1. **Integration Testing**: Test with real post-trade systems
2. **Performance Optimization**: Optimize CDM object creation for high throughput
3. **Event Processing**: Integrate with event-driven architecture
4. **Regulatory Reporting**: Connect to regulatory reporting systems
5. **Cross-Institution**: Enable CDM-based trade communication

## References

- [FINOS CDM Documentation](https://cdm.finos.org/docs/)
- [CDM GitHub Repository](https://github.com/finos/common-domain-model)
- [CDM Event Model](https://cdm.finos.org/docs/event-model/)
- [TraderXcdm Reference](https://github.com/finos/traderXcdm)