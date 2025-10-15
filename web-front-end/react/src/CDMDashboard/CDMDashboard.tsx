import React, { useCallback, useEffect, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { Box, Tab, Tabs, Typography, Card, CardContent, Chip } from '@mui/material';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { SelectChangeEvent } from '@mui/material';
import { socket } from '../socket';
import { GetPositions, GetTrades } from '../hooks';
import { CreateCDMTradeButton, UpdateCDMTradeButton } from '../ActionButtons';
import { AccountsDropdown } from '../AccountsDropdown';
import { ColDef } from 'ag-grid-community';
import { PositionData, TradeData } from '../Datatable/types';
import { CDMTradeData } from './types';
import { CDMTradeDetails } from './CDMTradeDetails';
import { Environment } from '../env';

const PUBLISH = 'publish';
const SUBSCRIBE = 'subscribe';
const UNSUBSCRIBE = 'unsubscribe';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`cdm-tabpanel-${index}`}
      aria-labelledby={`cdm-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

export const CDMDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [tradeRowData, setTradeRowData] = useState<TradeData[]>([]);
  const [cdmTradeRowData, setCdmTradeRowData] = useState<CDMTradeData[]>([]);
  const [tradeColumnDefs, setTradeColumnDefs] = useState<ColDef[]>([]);
  const [cdmTradeColumnDefs, setCdmTradeColumnDefs] = useState<ColDef[]>([]);
  const [positionRowData, setPositionRowData] = useState<PositionData[]>([]);
  const [positionColumnDefs, setPositionColumnDefs] = useState<ColDef[]>([]);
  const [selectedId, setSelectedId] = useState<number>(0);
  const [currentAccount, setCurrentAccount] = useState<string>('');
  const [selectedTrade, setSelectedTrade] = useState<CDMTradeData | null>(null);

  const positionData = GetPositions(selectedId);
  const tradeData = GetTrades(selectedId);

  // Fetch CDM trades
  const fetchCDMTrades = useCallback(async () => {
    if (selectedId === 0) return;
    try {
      const response = await fetch(`${Environment.trade_service_url}/cdm/trades/${selectedId}`);
      if (response.ok) {
        const cdmTrades = await response.json();
        setCdmTradeRowData(cdmTrades);
      }
    } catch (error) {
      console.error('Error fetching CDM trades:', error);
    }
  }, [selectedId]);

  const handleChange = useCallback((event: SelectChangeEvent<any>) => {
    socket.off(PUBLISH);
    if (selectedId !== 0) {
      socket.emit(UNSUBSCRIBE, `/accounts/${selectedId}/trades`);
      socket.emit(UNSUBSCRIBE, `/accounts/${selectedId}/positions`);
    }
    setSelectedId(event.target.value);
    setCurrentAccount(event.target.value);
    socket.emit(SUBSCRIBE, `/accounts/${event.target.value}/trades`);
    socket.emit(SUBSCRIBE, `/accounts/${event.target.value}/positions`);
    socket.on(PUBLISH, (data: any) => {
      if (data.topic === `/accounts/${event.target.value}/trades`) {
        console.log("INCOMING TRADE DATA: ", data);
        setTradeRowData((current: TradeData[]) => [...current, data.payload]);
        // Refresh CDM trades when new trade comes in
        fetchCDMTrades();
      }
      if (data.topic === `/accounts/${event.target.value}/positions`) {
        console.log("INCOMING POSITION DATA: ", data);
        setPositionRowData((current: PositionData[]) => [...current, data.payload]);
      }
    });
  }, [selectedId, fetchCDMTrades]);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleTradeSelect = (trade: CDMTradeData) => {
    setSelectedTrade(trade);
  };

  useEffect(() => {
    const positionKeys = ['security', 'quantity', 'updated'];
    const tradeKeys = ['security', 'quantity', 'side', 'state', 'updated'];
    const cdmTradeKeys = ['id', 'security', 'quantity', 'side', 'state', 'cdmVersion', 'businessEventType', 'updated'];
    
    setPositionRowData(positionData);
    setTradeRowData(tradeData);
    setPositionColumnDefs([]);
    setTradeColumnDefs([]);
    setCdmTradeColumnDefs([]);
    
    positionKeys.forEach((key: string) => 
      setPositionColumnDefs((current: ColDef<PositionData>[]) => [...current, { field: key }])
    );
    tradeKeys.forEach((key: string) => 
      setTradeColumnDefs((current: ColDef<TradeData>[]) => [...current, { field: key }])
    );
    cdmTradeKeys.forEach((key: string) => 
      setCdmTradeColumnDefs((current: ColDef<CDMTradeData>[]) => [...current, { 
        field: key,
        cellRenderer: key === 'cdmVersion' ? (params: any) => 
          <Chip label={`CDM ${params.value}`} color="primary" size="small" /> : undefined,
        cellRendererParams: key === 'businessEventType' ? (params: any) => 
          <Chip label={params.value} color="secondary" size="small" /> : undefined
      }])
    );
    
    fetchCDMTrades();
  }, [positionData, tradeData, selectedId, currentAccount, fetchCDMTrades]);

  return (
    <>
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h4" component="h1" gutterBottom>
            FINOS CDM Integration Dashboard
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            TraderX with Common Domain Model Support
          </Typography>
        </CardContent>
      </Card>

      <div className="accounts-dropdown">
        <AccountsDropdown currentAccount={currentAccount} handleChange={handleChange} />
      </div>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="CDM dashboard tabs">
          <Tab label="Traditional Trades" />
          <Tab label="CDM Trades" />
          <Tab label="Positions" />
          <Tab label="CDM Details" />
        </Tabs>
      </Box>

      <TabPanel value={tabValue} index={0}>
        <div className="action-buttons" style={{ width: "100%", display: "flex", marginBottom: "16px" }}>
          <CreateCDMTradeButton accountId={selectedId} onTradeCreated={fetchCDMTrades} />
        </div>
        <div className="ag-theme-alpine" style={{ height: "70vh", width: "100%" }}>
          <AgGridReact
            rowData={tradeRowData}
            columnDefs={tradeColumnDefs}
          />
        </div>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <div className="action-buttons" style={{ width: "100%", display: "flex", marginBottom: "16px" }}>
          <CreateCDMTradeButton accountId={selectedId} onTradeCreated={fetchCDMTrades} />
          {selectedTrade && (
            <UpdateCDMTradeButton 
              trade={selectedTrade} 
              onTradeUpdated={fetchCDMTrades} 
            />
          )}
        </div>
        <div className="ag-theme-alpine" style={{ height: "70vh", width: "100%" }}>
          <AgGridReact
            rowData={cdmTradeRowData}
            columnDefs={cdmTradeColumnDefs}
            onRowClicked={(event) => handleTradeSelect(event.data)}
            rowSelection="single"
          />
        </div>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <div className="ag-theme-alpine" style={{ height: "70vh", width: "100%" }}>
          <AgGridReact
            rowData={positionRowData}
            columnDefs={positionColumnDefs}
          />
        </div>
      </TabPanel>

      <TabPanel value={tabValue} index={3}>
        {selectedTrade ? (
          <CDMTradeDetails trade={selectedTrade} />
        ) : (
          <Typography variant="body1" color="text.secondary">
            Select a CDM trade from the CDM Trades tab to view detailed CDM structure
          </Typography>
        )}
      </TabPanel>
    </>
  );
};