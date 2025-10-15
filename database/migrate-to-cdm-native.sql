-- Migration script to CDM Native schema
-- Run this to migrate from legacy CDMTRADES to proper CDM Native tables

-- Create CDM Native tables if they don't exist
CREATE TABLE IF NOT EXISTS CdmAccounts ( 
    ID INTEGER PRIMARY KEY, 
    DisplayName VARCHAR(50), 
    CdmAccountObj JSON(2000) 
);

CREATE TABLE IF NOT EXISTS CdmAccountUsers ( 
    AccountID INTEGER NOT NULL, 
    Username VARCHAR(15) NOT NULL, 
    CdmAccountUserObj JSON(2000), 
    PRIMARY KEY (AccountID, Username)
);

CREATE TABLE IF NOT EXISTS CdmPositions ( 
    AccountID INTEGER, 
    Security VARCHAR(15), 
    Updated TIMESTAMP, 
    Quantity INTEGER, 
    CdmPositionObj JSON(2000), 
    PRIMARY KEY (AccountID, Security) 
);

-- Migrate existing CDMTRADES data to new CDM Native structure
-- Update CDMTRADES table to match CDM Native schema
ALTER TABLE CDMTRADES ADD COLUMN IF NOT EXISTS CdmTradeObj JSON(10000);

-- Copy existing CDM data to new column if it exists
UPDATE CDMTRADES 
SET CdmTradeObj = CDMTRADEOBJ 
WHERE CdmTradeObj IS NULL AND CDMTRADEOBJ IS NOT NULL;

-- Create CDM Accounts from existing trade data
INSERT INTO CdmAccounts (ID, DisplayName, CdmAccountObj)
SELECT DISTINCT 
    ACCOUNTID,
    CONCAT('Account ', ACCOUNTID),
    CONCAT('{"name":{"value":"Account ', ACCOUNTID, '"},"partyId":[{"identifier":{"value":"', ACCOUNTID, '"}}]}')
FROM CDMTRADES 
WHERE ACCOUNTID NOT IN (SELECT ID FROM CdmAccounts);

-- Create CDM Positions from existing trades
INSERT INTO CdmPositions (AccountID, Security, Updated, Quantity, CdmPositionObj)
SELECT 
    ACCOUNTID,
    SECURITY,
    MAX(UPDATED) as Updated,
    SUM(CASE WHEN SIDE = 'Buy' THEN QUANTITY ELSE -QUANTITY END) as Quantity,
    CONCAT('{"positionIdentifier":[{"identifier":{"value":"POS-', ACCOUNTID, '-', SECURITY, '"}}],"product":{"security":{"identifier":[{"identifier":{"value":"', SECURITY, '"}}]}},"quantity":{"amount":', SUM(CASE WHEN SIDE = 'Buy' THEN QUANTITY ELSE -QUANTITY END), ',"unit":{"currency":{"value":"USD"}}},"party":[{"partyId":[{"identifier":{"value":"ACCOUNT-', ACCOUNTID, '"}}]}],"cdmVersion":"6.0.0","positionType":"', CASE WHEN SUM(CASE WHEN SIDE = 'Buy' THEN QUANTITY ELSE -QUANTITY END) > 0 THEN 'LONG' ELSE 'SHORT' END, '"}') as CdmPositionObj
FROM CDMTRADES 
GROUP BY ACCOUNTID, SECURITY
HAVING SUM(CASE WHEN SIDE = 'Buy' THEN QUANTITY ELSE -QUANTITY END) != 0
ON DUPLICATE KEY UPDATE 
    Updated = VALUES(Updated),
    Quantity = VALUES(Quantity),
    CdmPositionObj = VALUES(CdmPositionObj);

-- Add foreign key constraints if they don't exist
ALTER TABLE CdmAccountUsers ADD CONSTRAINT IF NOT EXISTS fk_cdm_account_users 
    FOREIGN KEY (AccountID) REFERENCES CdmAccounts(ID);

ALTER TABLE CdmPositions ADD CONSTRAINT IF NOT EXISTS fk_cdm_positions 
    FOREIGN KEY (AccountID) REFERENCES CdmAccounts(ID);

ALTER TABLE CDMTRADES ADD CONSTRAINT IF NOT EXISTS fk_cdm_trades 
    FOREIGN KEY (ACCOUNTID) REFERENCES CdmAccounts(ID);

-- Verify migration
SELECT 'CDM Accounts' as TableName, COUNT(*) as RecordCount FROM CdmAccounts
UNION ALL
SELECT 'CDM Positions' as TableName, COUNT(*) as RecordCount FROM CdmPositions  
UNION ALL
SELECT 'CDM Trades' as TableName, COUNT(*) as RecordCount FROM CDMTRADES;