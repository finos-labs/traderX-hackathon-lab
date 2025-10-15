-- FINOS CDM Native Schema for TraderX
-- Based on: https://github.com/tomhealey-icma/traderXcdm/blob/main/database/initialSchema.sql

-- Drop existing tables if they exist
DROP TABLE IF EXISTS CdmTrades;
DROP TABLE IF EXISTS CdmAccountUsers; 
DROP TABLE IF EXISTS CdmPositions;
DROP TABLE IF EXISTS CdmAccounts;
DROP SEQUENCE IF EXISTS CdmACCOUNTS_SEQ;

-- CDM Native Tables following traderXcdm reference
CREATE TABLE CdmAccounts ( 
    ID INTEGER PRIMARY KEY, 
    DisplayName VARCHAR(50), 
    CdmAccountObj JSON(2000) 
);

CREATE TABLE CdmAccountUsers ( 
    AccountID INTEGER NOT NULL, 
    Username VARCHAR(15) NOT NULL, 
    CdmAccountUserObj JSON(2000), 
    PRIMARY KEY (AccountID, Username)
);

ALTER TABLE CdmAccountUsers ADD FOREIGN KEY (AccountID) REFERENCES CdmAccounts(ID);

CREATE TABLE CdmPositions ( 
    AccountID INTEGER, 
    Security VARCHAR(15), 
    Updated TIMESTAMP, 
    Quantity INTEGER, 
    CdmPositionObj JSON(2000), 
    PRIMARY KEY (AccountID, Security) 
);

ALTER TABLE CdmPositions ADD FOREIGN KEY (AccountID) REFERENCES CdmAccounts(ID);

CREATE TABLE CdmTrades ( 
    ID VARCHAR(50) PRIMARY KEY, 
    AccountID INTEGER, 
    Created TIMESTAMP, 
    Updated TIMESTAMP, 
    Security VARCHAR(15), 
    Side VARCHAR(10) CHECK (Side IN ('Buy','Sell')), 
    Quantity INTEGER CHECK (Quantity > 0), 
    State VARCHAR(20) CHECK (State IN ('New', 'Processing', 'Settled', 'Cancelled')), 
    CdmTradeObj JSON(10000)
);

ALTER TABLE CdmTrades ADD FOREIGN KEY (AccountID) REFERENCES CdmAccounts(ID);

CREATE SEQUENCE CdmACCOUNTS_SEQ START WITH 65000 INCREMENT BY 1;

-- Insert CDM Accounts with proper FINOS CDM Party structures
INSERT INTO CdmAccounts (ID, DisplayName, CdmAccountObj) VALUES 
(22214, 'Test Account 20', '{"name":{"value":"Test Account 20"},"partyId":[{"identifier":{"value":"22214"}}]}'),
(11413, 'Private Clients Fund TTXX', '{"name":{"value":"Private Clients Fund TTXX"},"partyId":[{"identifier":{"value":"11413"}}]}'),
(42422, 'Algo Execution Partners', '{"name":{"value":"Algo Execution Partners"},"partyId":[{"identifier":{"value":"42422"}}]}'),
(52355, 'Big Corporate Fund', '{"name":{"value":"Big Corporate Fund"},"partyId":[{"identifier":{"value":"52355"}}]}'),
(62654, 'Hedge Fund TXY1', '{"name":{"value":"Hedge Fund TXY1"},"partyId":[{"identifier":{"value":"62654"}}]}'),
(10031, 'Internal Trading Book', '{"name":{"value":"Internal Trading Book"},"partyId":[{"identifier":{"value":"10031"}}]}'),
(44044, 'Trading Account 1', '{"name":{"value":"Trading Account 1"},"partyId":[{"identifier":{"value":"44044"}}]}');

-- Insert CDM Account Users
INSERT INTO CdmAccountUsers (AccountID, Username, CdmAccountUserObj) VALUES 
(22214, 'user01', '{"name":{"value":"user01"},"partyId":[{"identifier":{"value":"22214"}}]}'),
(22214, 'user03', '{"name":{"value":"user03"},"partyId":[{"identifier":{"value":"22214"}}]}'),
(22214, 'user09', '{"name":{"value":"user09"},"partyId":[{"identifier":{"value":"22214"}}]}'),
(22214, 'user05', '{"name":{"value":"user05"},"partyId":[{"identifier":{"value":"22214"}}]}'),
(22214, 'user07', '{"name":{"value":"user07"},"partyId":[{"identifier":{"value":"22214"}}]}');

-- Sample CDM Trades with proper FINOS CDM Trade structures
INSERT INTO CdmTrades(ID, Created, Updated, Security, Side, Quantity, State, AccountID, CdmTradeObj) VALUES
('CDM-SAMPLE-001', NOW(), NOW(), 'IBM', 'Sell', 100, 'Settled', 22214, 
'{"tradeIdentifier":[{"assignedIdentifier":[{"identifier":{"value":"CDM-SAMPLE-001","meta":{"scheme":"UTI"}}}],"identifierType":"UNIQUE_TRANSACTION_IDENTIFIER"}],"tradeDate":{"value":"2025-10-15"},"tradableProduct":{"product":{"security":{"identifier":[{"identifier":{"value":"IBM"}}]}},"tradeLot":[{"priceQuantity":[{"price":[{"value":{"value":100.0,"priceType":"ASSET_PRICE","priceExpression":"PERCENTAGE_OF_NOTIONAL"}}],"quantity":[{"value":{"value":100}}]}]}],"counterparty":[{"role":"PARTY_2","partyReference":{"value":{"name":{"value":"22214"}}}}]},"party":[{"name":{"value":"22214"}},{"name":{"value":"22214"}}],"partyRole":[{"role":"SELLER"},{"role":"BUYER"}],"executionDetails":{"executionType":"ON_VENUE"}}');

-- Sample CDM Positions
INSERT INTO CdmPositions (AccountID, Security, Updated, Quantity, CdmPositionObj) VALUES
(22214, 'IBM', NOW(), -100, '{"positionIdentifier":[{"identifier":{"value":"POS-22214-IBM"}}],"product":{"security":{"identifier":[{"identifier":{"value":"IBM"}}]}},"quantity":{"amount":-100,"unit":{"currency":{"value":"USD"}}},"party":[{"partyId":[{"identifier":{"value":"ACCOUNT-22214"}}]}],"cdmVersion":"6.0.0","positionType":"SHORT"}');