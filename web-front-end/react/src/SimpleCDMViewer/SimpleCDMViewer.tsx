import React, { useEffect, useState, useCallback, useRef } from 'react';
import { Environment } from '../env';

// Stable CDM JSON Viewer Component
const CDMJsonViewer: React.FC<{ 
  cdmJson: any; 
  tradeId: string; 
  expandedStatesRef: React.MutableRefObject<{[key: string]: boolean}>;
}> = ({ cdmJson, tradeId, expandedStatesRef }) => {
  
  // Get initial state from ref, default to false
  const [isExpanded, setIsExpanded] = useState(() => {
    return expandedStatesRef.current[tradeId] || false;
  });
  
  const toggleExpanded = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const newState = !isExpanded;
    setIsExpanded(newState);
    // Store state in ref to persist across re-renders
    expandedStatesRef.current[tradeId] = newState;
  };
  
  const formatJson = (json: any) => {
    try {
      if (typeof json === 'string') {
        return JSON.stringify(JSON.parse(json), null, 2);
      }
      return JSON.stringify(json, null, 2);
    } catch (e) {
      return json.toString();
    }
  };
  
  // Sync state with ref on mount
  useEffect(() => {
    const savedState = expandedStatesRef.current[tradeId];
    if (savedState !== undefined && savedState !== isExpanded) {
      setIsExpanded(savedState);
    }
  }, [tradeId, expandedStatesRef, isExpanded]);
  
  return (
    <div style={{ marginTop: '8px' }}>
      <button 
        onClick={toggleExpanded}
        style={{ 
          cursor: 'pointer', 
          color: '#1976d2', 
          fontSize: '12px',
          background: 'none',
          border: '1px solid #1976d2',
          textDecoration: 'none',
          padding: '4px 8px',
          borderRadius: '4px',
          fontWeight: 'bold'
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.backgroundColor = '#1976d2';
          e.currentTarget.style.color = 'white';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.backgroundColor = 'transparent';
          e.currentTarget.style.color = '#1976d2';
        }}
      >
        {isExpanded ? '▼ Hide CDM JSON' : '▶ View CDM JSON'}
      </button>
      
      {isExpanded && (
        <div style={{ 
          backgroundColor: '#f8f8f8', 
          padding: '12px', 
          fontSize: '10px',
          overflow: 'auto',
          maxHeight: '400px',
          marginTop: '8px',
          borderRadius: '4px',
          border: '2px solid #1976d2',
          fontFamily: 'Monaco, Consolas, "Courier New", monospace',
          position: 'relative'
        }}>
          <div style={{ 
            marginBottom: '8px', 
            fontSize: '11px', 
            color: '#1976d2',
            borderBottom: '1px solid #1976d2',
            paddingBottom: '4px',
            fontWeight: 'bold',
            position: 'sticky',
            top: 0,
            backgroundColor: '#f8f8f8'
          }}>
            🏛️ FINOS CDM Event Model JSON (Trade: {tradeId})
          </div>
          <pre style={{ 
            margin: 0, 
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            lineHeight: '1.4',
            color: '#333'
          }}>
            {formatJson(cdmJson)}
          </pre>
        </div>
      )}
    </div>
  );
};

interface SimpleCDMViewerProps {
  accountId: number;
}

export const SimpleCDMViewer: React.FC<SimpleCDMViewerProps> = ({ accountId }) => {
  const [cdmTrades, setCdmTrades] = useState<any[]>([]);
  const [cdmStatus, setCdmStatus] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  
  // Keep track of expanded states to preserve them across re-renders
  const expandedStatesRef = useRef<{[key: string]: boolean}>({});

  const fetchCDMData = useCallback(async () => {
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
  }, [accountId]);

  useEffect(() => {
    fetchCDMData();
    // Refresh every 15 seconds (even less frequent)
    const interval = setInterval(fetchCDMData, 15000);
    return () => clearInterval(interval);
  }, [fetchCDMData]);

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
                <CDMJsonViewer 
                  cdmJson={trade.cdmTrade} 
                  tradeId={trade.id} 
                  expandedStatesRef={expandedStatesRef}
                />
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