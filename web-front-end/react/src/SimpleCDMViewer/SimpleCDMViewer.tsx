import React, { useEffect, useState } from 'react';
import { Environment } from '../env';

interface SimpleCDMViewerProps {
  accountId: number;
}

export const SimpleCDMViewer: React.FC<SimpleCDMViewerProps> = ({ accountId }) => {
  const [cdmTrades, setCdmTrades] = useState<any[]>([]);
  const [cdmStatus, setCdmStatus] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  const fetchCDMData = async () => {
    if (accountId === 0) return;
    
    setLoading(true);
    setError('');
    
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
        setCdmTrades(Array.isArray(trades) ? trades : []);
      }
    } catch (err) {
      console.error('Error fetching CDM data:', err);
      setError('Failed to load CDM data');
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
      <div style={{ 
        padding: '15px', 
        backgroundColor: '#f0f8ff', 
        margin: '10px 0', 
        borderRadius: '5px',
        border: '1px solid #1976d2'
      }}>
        <h3 style={{ margin: '0 0 10px 0', color: '#1976d2' }}>
          🏛️ FINOS CDM Integration
        </h3>
        <p style={{ margin: 0, color: '#666' }}>
          Select an account to view CDM-converted trades
        </p>
      </div>
    );
  }

  return (
    <div style={{ 
      padding: '15px', 
      backgroundColor: '#f0f8ff', 
      margin: '10px 0', 
      borderRadius: '5px',
      border: '1px solid #1976d2'
    }}>
      <h3 style={{ margin: '0 0 15px 0', color: '#1976d2' }}>
        🏛️ FINOS CDM Integration
      </h3>
      
      {cdmStatus && (
        <div style={{ marginBottom: '15px', fontSize: '14px' }}>
          <span style={{ 
            backgroundColor: cdmStatus.cdmEnabled ? '#4caf50' : '#f44336',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '12px',
            marginRight: '10px'
          }}>
            {cdmStatus.status}
          </span>
          <span style={{ marginRight: '15px' }}>
            <strong>CDM Version:</strong> {cdmStatus.cdmVersion}
          </span>
          <span>
            <strong>Total CDM Trades:</strong> {cdmStatus.cdmTradesStored || 0}
          </span>
        </div>
      )}

      <h4 style={{ margin: '0 0 10px 0', color: '#333' }}>
        CDM Trades for Account {accountId}:
      </h4>
      
      {loading && <p>Loading CDM trades...</p>}
      
      {error && (
        <p style={{ color: '#f44336', margin: '10px 0' }}>
          {error}
        </p>
      )}
      
      {!loading && !error && cdmTrades.length > 0 ? (
        <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
          {cdmTrades.map((trade, index) => (
            <div key={index} style={{ 
              border: '1px solid #ddd', 
              padding: '10px', 
              margin: '5px 0', 
              backgroundColor: 'white',
              borderRadius: '3px',
              fontSize: '13px'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <strong>{trade.security || 'N/A'}</strong> - {trade.quantity || 0} shares ({trade.side || 'N/A'})
                  <br />
                  <small style={{ color: '#666' }}>CDM ID: {trade.id || 'N/A'}</small>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span style={{ 
                    backgroundColor: '#1976d2', 
                    color: 'white', 
                    padding: '2px 6px', 
                    borderRadius: '10px',
                    fontSize: '11px'
                  }}>
                    CDM 6.0.0
                  </span>
                </div>
              </div>
              
              {trade.cdmTrade && (
                <details style={{ marginTop: '8px' }}>
                  <summary style={{ cursor: 'pointer', color: '#1976d2', fontSize: '12px' }}>
                    View CDM JSON
                  </summary>
                  <pre style={{ 
                    backgroundColor: '#f8f8f8', 
                    padding: '8px', 
                    fontSize: '11px',
                    overflow: 'auto',
                    maxHeight: '150px',
                    marginTop: '5px',
                    borderRadius: '3px'
                  }}>
                    {typeof trade.cdmTrade === 'string' 
                      ? JSON.stringify(JSON.parse(trade.cdmTrade), null, 2)
                      : JSON.stringify(trade.cdmTrade, null, 2)
                    }
                  </pre>
                </details>
              )}
            </div>
          ))}
        </div>
      ) : !loading && !error ? (
        <p style={{ color: '#666', fontStyle: 'italic' }}>
          No CDM trades found. Create a trade to see CDM conversion!
        </p>
      ) : null}
      
      <div style={{ marginTop: '10px', fontSize: '11px', color: '#666', fontStyle: 'italic' }}>
        💡 Trades are automatically converted to FINOS CDM format and stored in the CDMTRADES table.
      </div>
    </div>
  );
};