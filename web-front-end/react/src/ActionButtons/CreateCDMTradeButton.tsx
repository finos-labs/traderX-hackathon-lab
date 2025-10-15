import { 
  Box, 
  Button, 
  MenuItem, 
  Modal, 
  TextField, 
  ToggleButton, 
  ToggleButtonGroup,
  Typography,
  Chip,
  Alert
} from "@mui/material";
import { ChangeEvent, MouseEvent, useCallback, useState } from "react";
import { style } from "../style";
import { ActionButtonsProps, Side } from "./types";
import { Environment } from '../env';

interface CreateCDMTradeButtonProps extends ActionButtonsProps {
  onTradeCreated?: () => void;
}

export const CreateCDMTradeButton = ({ accountId, onTradeCreated }: CreateCDMTradeButtonProps) => {
  const [refData, setRefData] = useState<any>([]);
  const tradeId = Math.floor(Math.random() * 1000000);

  const delay = (ms: number) => new Promise(
    resolve => setTimeout(resolve, ms)
  );

  const handleSubmit = async () => {
    try {
      // First create the regular trade
      const tradeResponse = await fetch(`${Environment.trade_service_url}/trade/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: `TRADE-${tradeId}`,
          security: security,
          quantity: quantity,
          accountId: accountId,
          side: side,
        }),
      });

      if (!tradeResponse.ok) {
        throw new Error('Failed to create trade');
      }

      // Then create the CDM representation
      const cdmResponse = await fetch(`${Environment.trade_service_url}/cdm/convert-trade`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tradeId: `TRADE-${tradeId}`,
          accountId: accountId
        }),
      });

      if (cdmResponse.ok) {
        setTradeSuccess(true);
        setCdmSuccess(true);
        await delay(2000);
        setTradeSuccess(false);
        setCdmSuccess(false);
        setOpen(false);
        onTradeCreated?.();
        console.log('CDM trade created successfully');
        return;
      } else {
        // Trade created but CDM conversion failed
        setTradeSuccess(true);
        setCdmError('CDM conversion failed, but trade was created');
      }
    } catch (error) {
      console.log(error);
      setError(error);
      return error;
    }
  };

  const [open, setOpen] = useState<boolean>(false);
  const [error, setError] = useState<any>('');
  const [cdmError, setCdmError] = useState<string>('');
  const handleClose = () => {
    setOpen(false);
    setError('');
    setCdmError('');
  };
  
  const handleOpen = async () => {
    setOpen(true);
    try {
      const response = await fetch(`${Environment.reference_data_url}/stocks`);
      const data = await response.json();
      setRefData(data);
    } catch (error) {
      return error;
    }
  };

  const tickerItem = refData.map((option: any) => (
    <MenuItem key={option.ticker} value={option.ticker}>
      {option.ticker}
    </MenuItem>
  ));

  const [side, setSide] = useState<Side>();
  const [security, setSecurity] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(0);
  const [tradeSuccess, setTradeSuccess] = useState<boolean>(false);
  const [cdmSuccess, setCdmSuccess] = useState<boolean>(false);

  const handleToggleChange = useCallback((
    _event: MouseEvent<HTMLElement>,
    newSide: Side,
  ) => {
    setSide(newSide);
  }, []);

  const handleSecurityChange = useCallback(
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setSecurity(event.target.value);
    }, []);

  const handleQuantityChange = useCallback(
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setQuantity(parseInt(event.target.value));
    }, []);

  return (
    <div className="modal-container">
      <Button 
        onClick={handleOpen} 
        variant="contained" 
        color="primary"
        startIcon={<Chip label="CDM" size="small" />}
      >
        Create CDM Trade
      </Button>
      <Modal
        open={open}
        onClose={handleClose}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description"
      >
        <Box className="modal-components" sx={style}>
          <Typography variant="h6" component="h2" gutterBottom>
            Create New CDM-Compliant Trade
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            This will create a trade and convert it to FINOS CDM format
          </Typography>
          
          <div className="form-container">
            <TextField
              select
              label="Security"
              variant="outlined"
              style={{ width: "8em" }}
              onChange={handleSecurityChange}
              value={security}
            >
              {tickerItem}
            </TextField>
            <TextField
              type="number"
              style={{ width: "6em" }}
              label="Quantity"
              onChange={handleQuantityChange}
              value={quantity || ''}
            />
            <ToggleButtonGroup
              color="primary"
              size="medium"
              style={{ height: "3.5em" }}
              value={side}
              exclusive
              onChange={handleToggleChange}
              aria-label="tradeSide"
            >
              <ToggleButton value="Buy">Buy</ToggleButton>
              <ToggleButton value="Sell">Sell</ToggleButton>
            </ToggleButtonGroup>
            
            {!tradeSuccess && !cdmSuccess && (
              <div style={{ float: 'left' }} className="submit-button-container">
                <Button 
                  variant="contained" 
                  color="success" 
                  onClick={handleSubmit}
                  disabled={!security || !quantity || !side || !accountId || accountId === 0}
                >
                  Create CDM Trade
                </Button>
              </div>
            )}
            
            {(!accountId || accountId === 0) && (
              <Alert severity="info">
                Please select an account first to create trades
              </Alert>
            )}
            
            {tradeSuccess && cdmSuccess && (
              <Alert severity="success">
                CDM Trade Created Successfully!
              </Alert>
            )}
            
            {tradeSuccess && !cdmSuccess && cdmError && (
              <Alert severity="warning">
                {cdmError}
              </Alert>
            )}
            
            {error && (
              <Alert severity="error">
                Error: {error.toString()}
              </Alert>
            )}
          </div>
        </Box>
      </Modal>
    </div>
  );
};