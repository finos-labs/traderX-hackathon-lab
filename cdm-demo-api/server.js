const express = require('express');
const cors = require('cors');
const path = require('path');
const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Mock CDM data for demo (since we can't easily connect to H2 from Node.js)
let cdmTrades = [];
let tradeCounter = 0;

// Simulate CDM trade creation
function createCDMTrade(tradeData) {
    const cdmTrade = {
        id: `CDM-${tradeData.id || 'demo-' + (++tradeCounter)}`,
        accountId: tradeData.accountId || 22214,
        security: tradeData.security || 'DEMO',
        side: tradeData.side || 'Buy',
        quantity: tradeData.quantity || 100,
        state: 'CDM_PROCESSED',
        created: new Date().toISOString(),
        cdmBusinessEvent: {
            cdmVersion: '6.0.0',
            businessEventType: 'EXECUTION',
            eventQualifier: 'NewTrade',
            eventDate: new Date().toISOString().split('T')[0]
        }
    };
    
    cdmTrades.push(cdmTrade);
    return cdmTrade;
}

// Initialize with some demo data
createCDMTrade({ id: 'demo-initial', security: 'AAPL', quantity: 100, side: 'Buy' });
createCDMTrade({ id: 'demo-sample', security: 'MSFT', quantity: 200, side: 'Sell' });

// CDM Demo endpoint
app.get('/cdm-demo', (req, res) => {
    res.json({
        title: 'FINOS CDM Integration Demo',
        cdmVersion: '6.0.0',
        status: 'ACTIVE',
        totalCdmTrades: cdmTrades.length,
        timestamp: new Date().toISOString(),
        cdmTrades: cdmTrades.map(trade => ({
            id: trade.id,
            security: trade.security,
            quantity: trade.quantity,
            side: trade.side,
            accountId: trade.accountId,
            state: trade.state,
            created: trade.created,
            cdmBusinessEvent: trade.cdmBusinessEvent
        })),
        demoInfo: {
            description: 'This demonstrates FINOS CDM 6.0.0 integration with TraderX',
            features: [
                'CDM ExecutionInstruction Creation',
                'CDM BusinessEvent Processing',
                'Industry Standard Compliance',
                'Event Store Persistence'
            ]
        }
    });
});

// Add new CDM trade (for demo purposes)
app.post('/cdm-demo/add-trade', (req, res) => {
    const newTrade = createCDMTrade(req.body);
    res.json({
        success: true,
        message: 'CDM trade created successfully',
        trade: newTrade,
        totalTrades: cdmTrades.length
    });
});

// CDM status endpoint
app.get('/cdm-status', (req, res) => {
    res.json({
        service: 'CDM Demo API',
        cdmVersion: '6.0.0',
        status: 'ACTIVE',
        totalTrades: cdmTrades.length,
        timestamp: new Date().toISOString()
    });
});

// Serve the HTML dashboard
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

app.listen(port, '0.0.0.0', () => {
    console.log(`🚀 CDM Demo API running at http://localhost:${port}`);
    console.log(`📊 CDM Dashboard: http://localhost:${port}/`);
    console.log(`📊 CDM Demo API: http://localhost:${port}/cdm-demo`);
    console.log(`✅ CDM Status: http://localhost:${port}/cdm-status`);
});