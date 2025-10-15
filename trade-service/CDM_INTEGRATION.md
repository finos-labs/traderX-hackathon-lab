# FINOS CDM Integration for TraderX

This document describes the integration of FINOS Common Domain Model (CDM) compliant structures into TraderX.

## Overview

TraderX now creates CDM-compliant JSON structures following the official FINOS CDM Event Model specification. This provides:

- **CDM Event Model Compliance**: Following FINOS CDM Event Model specification
- **Proper CDM Structure**: BusinessEvent, Trade, Party, Product structures
- **Validation**: CDM structure validation and compliance checking
- **Industry Standard**: Alignment with FINOS CDM specification

## Important Note

This implementation demonstrates CDM compliance using JSON structures that follow the FINOS CDM Event Model. For full production use, you would integrate the complete CDM Java library with Rosetta DSL runtime.

## Key Components

### 1. TradeOrderToCDMAdapter
- Converts TraderX `TradeOrder` to CDM-compliant JSON structures
- Creates CDM `BusinessEvent` JSON following FINOS CDM Event Model
- Validates CDM structure compliance

### 2. CDMController
- `/cdm/process-trade` - Process trades through real CDM pipeline
- `/cdm/compare` - Compare mock vs real CDM formats
- `/cdm/validate` - Validate CDM JSON against FINOS specification
- `/cdm/compliance` - Get CDM compliance report

### 3. CDMValidationService
- Validates CDM JSON structures against FINOS specification
- Provides compliance checking and error reporting
- Supports both `Trade` and `BusinessEvent` JSON validation

## CDM Dependencies

```gradle
// FINOS CDM Integration - Demonstration with JSON structures
implementation 'org.finos.cdm:cdm-java:6.0.0'
implementation 'com.regnosys.rosetta:rosetta-runtime:11.25.1'
implementation 'com.regnosys.rosetta:rosetta-common:11.25.1'
implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.2'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2'
```

## API Examples

### Process Trade Through CDM
```bash
POST /cdm/process-trade
{
  "id": "TRADE-123",
  "accountId": 12345,
  "security": "AAPL",
  "side": "Buy",
  "quantity": 100
}
```

### Compare Mock vs Real CDM
```bash
POST /cdm/compare
{
  "id": "TRADE-123",
  "accountId": 12345,
  "security": "AAPL", 
  "side": "Buy",
  "quantity": 100
}
```

### Validate CDM JSON
```bash
POST /cdm/validate
{
  "cdmJson": "{\"eventDate\":\"2025-10-15\",\"intent\":{\"intent\":\"EXECUTION\"},...}"
}
```

## Mock vs Real CDM

### Mock CDM (Old)
```json
{
  "cdmVersion": "6.0.0",
  "businessEventType": "EXECUTION",
  "tradeId": "TRADE-123",
  "security": "AAPL",
  "quantity": 100,
  "side": "Buy",
  "accountId": 12345,
  "timestamp": "2025-10-15T04:39:36Z"
}
```

### Real FINOS CDM (New)
```json
{
  "eventIdentifier": [{"assignedIdentifier": {"identifier": {"value": "EVENT-123"}}}],
  "eventDate": "2025-10-15",
  "eventTime": "2025-10-15T04:39:36Z",
  "intent": {"intent": "EXECUTION"},
  "primitives": [{
    "execution": {
      "after": {
        "tradeIdentifier": [{"assignedIdentifier": {"identifier": {"value": "TRADE-123"}}}],
        "tradableProduct": {
          "product": {
            "security": {
              "securityType": "EQUITY",
              "identifier": [{"identifier": {"value": "AAPL"}, "source": "TICKER"}]
            }
          },
          "tradeLot": [{
            "priceQuantity": [{
              "quantity": [{"amount": 100, "unit": {"currency": {"value": "USD"}}}]
            }]
          }],
          "counterparty": [{
            "role": "PARTY_1",
            "partyReference": {
              "partyId": [{"identifier": {"value": "ACCOUNT-12345"}}]
            }
          }]
        },
        "tradeDate": {"value": "2025-10-15"},
        "party": [{"partyId": [{"identifier": {"value": "ACCOUNT-12345"}}]}]
      }
    }
  }],
  "after": {
    "trade": {...},
    "state": {"positionState": "EXECUTED"}
  }
}
```

## Benefits of Real CDM Integration

1. **Industry Standard**: Uses official FINOS CDM specification
2. **Interoperability**: Compatible with other CDM-compliant systems
3. **Validation**: Built-in validation and type safety
4. **Documentation**: Follows documented CDM Event Model
5. **Future-Proof**: Aligned with industry standards and evolution

## References

- [FINOS CDM Documentation](https://cdm.finos.org/docs/event-model/)
- [FINOS CDM GitHub](https://github.com/finos/common-domain-model)
- [CDM Event Model](https://cdm.finos.org/docs/event-model/)
- [Rosetta DSL](https://docs.rosetta-technology.io/)

## Testing

Start the application and test the CDM endpoints:

```bash
# Start TraderX
docker-compose up

# Test CDM processing
curl -X POST http://localhost:18092/cdm/process-trade \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST-123","accountId":12345,"security":"AAPL","side":"Buy","quantity":100}'

# Compare implementations  
curl -X POST http://localhost:18092/cdm/compare \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST-123","accountId":12345,"security":"AAPL","side":"Buy","quantity":100}'

# Get compliance report
curl http://localhost:18092/cdm/compliance
```