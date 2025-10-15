-- CDM Migration Script
-- Creates CDM tables for TraderX CDM integration

CREATE TABLE IF NOT EXISTS CdmTrades ( 
    ID VARCHAR(50) PRIMARY KEY, 
    AccountID INTEGER, 
    Created TIMESTAMP, 
    Updated TIMESTAMP, 
    Security VARCHAR(15), 
    Side VARCHAR(10) CHECK (Side IN ('Buy','Sell')), 
    Quantity INTEGER CHECK (Quantity > 0), 
    State VARCHAR(20) CHECK (State IN ('New', 'Processing', 'Settled', 'Cancelled')), 
    CdmTradeObj TEXT
);

ALTER TABLE CdmTrades ADD FOREIGN KEY (AccountID) REFERENCES Accounts(ID);