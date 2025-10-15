import React, { useEffect, useState } from 'react';
import { Environment } from '../env';

interface CDMTrade {
  id: string;
  security: string;
  quantity: number;
  side: string;
  state: string;
  updated: string;
  cdmTrade: string;
}

interface CDMViewerProps {
  accountId: number;
}

export const CDMViewer: React.FC<CDMViewerProps> = ({ accountId }) => {
  const [cdmTrades, setCdmTrades] = useState<CDMTrade[]>([]);
  const [cdmStatus, setCdmStatus] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const fetchCDMData = async () => {
    if (accountId === 0) return;
    
    setLoading(true);
    try {
      // Fetch CDM status
      const statusResponse = await fetch(`${Environment.trade_service_url}/cdm/status`);
      if (statusResponse.ok) {
        const status = await statusResponse.json();
        setCdmStatus(status);
      }

      // Fetch CDM trades for account
      const tradesResponse = await fetch(`${Environment.trade_service_url}/cdm/trades/${accountId}`);
      if (tradesResponse.ok) {
        const trades = await tradesResponse.json();
        setCdmTrades(trades);
      }
    } catch (error) {
      console.error('Error fetching CDM data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCDMData();
    // Refresh every 5 seconds
    const interval = setInterval(fetchCDMData, 5000);
    return () => clearInterval(interval);
  }, [accountId]);

  if (accountId === 0) {
    return (
      <div style={{ padding: '20px', backgroundColor: '#f5f5f5', margin: '10px', borderRadius: '5px' }}>
        <h3>🏛️ FINOS CDM Integration</h3>
        <p>Select an account to view CDM-converted trades</p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', backgroundColor: '#f5f5f5', margin: '10px', borderRadius: '5px' }}>
      <h3>🏛️ FINOS CDM Integration</h3>
      
      {cdmStatus && (
        <div style={{ marginBottom: '15px' }}>
          <p><strong>CDM Status:</strong> 
            <span style={{ 
              color: cdmStatus.cdmEnabled ? 'green' : 'red',
              marginLeft: '10px',
              fontWeight: 'bold'
            }}>
              {cdmStatus.status}
            </span>
          </p>
          <p><strong>CDM Version:</strong> {cdmStatus.cdmVersion}</p>
          <p><strong>Total CDM Trades:</strong> {cdmStatus.cdmTradesStored}</p>
        </div>
      )}

      <h4>CDM Trades for Account {accountId}:</h4>
      {loading ? (
        <p>Loading CDM trades...</p>
      ) : cdmTrades.length > 0 ? (
        <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
          {cdmTrades.map((trade, index) => (
            <div key={index} style={{ 
              border: '1px solid #ddd', 
              padding: '10px', 
              margin: '5px 0', 
              backgroundColor: 'white',
              borderRadius: '3px'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <strong>{trade.security}</strong> - {trade.quantity} shares ({trade.side})
                  <br />
                  <small>CDM ID: {trade.id}</small>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span style={{ 
                    backgroundColor: '#1976d2', 
                    color: 'white', 
                    padding: '2px 8px', 
                    borderRadius: '12px',
                    fontSize: '12px'
                  }}>
                    CDM 6.0.0
                  </span>
                </div>
              </div>
              <details style={{ marginTop: '10px' }}>
                <summary style={{ cursor: 'pointer', color: '#1976d2' }}>
                  View CDM BusinessEvent JSON
                </summary>
                <pre style={{ 
                  backgroundColor: '#f8f8f8', 
                  padding: '10px', 
                  fontSize: '12px',
                  overflow: 'auto',
                  maxHeight: '200px',
                  marginTop: '5px'
                }}>
                  {JSON.stringify(JSON.parse(trade.cdmTrade || '{}'), null, 2)}
                </pre>
              </details>
            </div>
          ))}
        </div>
      ) : (
        <p>No CDM trades found for this account. Create a trade to see CDM conversion!</p>
      )}
      
      <div style={{ marginTop: '15px', fontSize: '12px', color: '#666' }}>
        <p>💡 <strong>How it works:</strong> When you create a trade above, it's automatically converted to FINOS CDM format and stored in the CDMTRADES table.</p>
      </div>
    </div>
  );
};