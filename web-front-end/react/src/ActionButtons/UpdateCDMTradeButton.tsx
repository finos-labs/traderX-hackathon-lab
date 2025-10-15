import { 
  Box, 
  Button, 
  Modal, 
  TextField, 
  ToggleButton, 
  ToggleButtonGroup,
  Typography,
  Chip,
  Alert,
  MenuItem
} from "@mui/material";
import { ChangeEvent, MouseEvent, useCallback, useState } from "react";
import { style } from "../style";
import { Side } from "./types";
import { Environment } from '../env';
import { CDMTradeData } from '../CDMDashboard/types';

interface UpdateCDMTradeButtonProps {
  trade: CDMTradeData;
  onTradeUpdated?: () => void;
}

export const UpdateCDMTradeButton = ({ trade, onTradeUpdated }: UpdateCDMTradeButtonProps) => {
  const delay = (ms: number) => new Promise(
    resolve => setTimeout(resolve, ms)
  );

  const handleSubmit = async () => {
    try {
      // Update the trade
      const updateResponse = await fetch(`${Environment.trade_service_url}/trade/${trade.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: trade.id,
          security: security,
          quantity: quantity,
          side: side,
          state: tradeState,
        }),
      });

      if (!updateResponse.ok) {
        throw new Error('Failed to update trade');
      }

      // Update the CDM representation
      const cdmResponse = await fetch(`${Environment.trade_service_url}/cdm/update-trade`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tradeId: trade.id,
          businessEventType: businessEventType
        }),
      });

      if (cdmResponse.ok) {
        setUpdateSuccess(true);
        setCdmSuccess(true);
        await delay(2000);
        setUpdateSuccess(false);
        setCdmSuccess(false);
        setOpen(false);
        onTradeUpdated?.();
        console.log('CDM trade updated successfully');
        return;
      } else {
        // Trade updated but CDM update failed
        setUpdateSuccess(true);
        setCdmError('CDM update failed, but trade was updated');
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
  
  const handleOpen = () => {
    setOpen(true);
    // Initialize form with current trade values
    setSecurity(trade.security);
    setQuantity(trade.quantity);
    setSide(trade.side as Side);
    setTradeState(trade.state || 'New');
  };

  const [side, setSide] = useState<Side>(trade.side as Side);
  const [security, setSecurity] = useState<string>(trade.security);
  const [quantity, setQuantity] = useState<number>(trade.quantity);
  const [tradeState, setTradeState] = useState<string>(trade.state || 'New');
  const [businessEventType, setBusinessEventType] = useState<string>('Amendment');
  const [updateSuccess, setUpdateSuccess] = useState<boolean>(false);
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

  const handleStateChange = useCallback(
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setTradeState(event.target.value);
    }, []);

  const handleBusinessEventTypeChange = useCallback(
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setBusinessEventType(event.target.value);
    }, []);

  const tradeStates = ['New', 'Pending', 'Executed', 'Settled', 'Cancelled'];
  const businessEventTypes = ['Amendment', 'Cancellation', 'Correction', 'PartialTermination', 'FullTermination'];

  return (
    <div className="modal-container">
      <Button 
        onClick={handleOpen} 
        variant="outlined" 
        color="secondary"
        startIcon={<Chip label="CDM" size="small" />}
        disabled={!trade}
      >
        Update CDM Trade
      </Button>
      <Modal
        open={open}
        onClose={handleClose}
        aria-labelledby="update-modal-title"
        aria-describedby="update-modal-description"
      >
        <Box className="modal-components" sx={style}>
          <Typography variant="h6" component="h2" gutterBottom>
            Update CDM Trade: {trade.id}
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Update trade details and create new CDM BusinessEvent
          </Typography>
          
          <div className="form-container">
            <TextField
              label="Security"
              variant="outlined"
              style={{ width: "8em" }}
              onChange={handleSecurityChange}
              value={security}
            />
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
            
            <TextField
              select
              label="Trade State"
              variant="outlined"
              style={{ width: "8em" }}
              onChange={handleStateChange}
              value={tradeState}
            >
              {tradeStates.map((state) => (
                <MenuItem key={state} value={state}>
                  {state}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label="CDM Business Event"
              variant="outlined"
              style={{ width: "10em" }}
              onChange={handleBusinessEventTypeChange}
              value={businessEventType}
            >
              {businessEventTypes.map((eventType) => (
                <MenuItem key={eventType} value={eventType}>
                  {eventType}
                </MenuItem>
              ))}
            </TextField>
            
            {!updateSuccess && !cdmSuccess && (
              <div style={{ float: 'left' }} className="submit-button-container">
                <Button 
                  variant="contained" 
                  color="success" 
                  onClick={handleSubmit}
                  disabled={!security || !quantity || !side}
                >
                  Update CDM Trade
                </Button>
              </div>
            )}
            
            {updateSuccess && cdmSuccess && (
              <Alert severity="success">
                CDM Trade Updated Successfully!
              </Alert>
            )}
            
            {updateSuccess && !cdmSuccess && cdmError && (
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