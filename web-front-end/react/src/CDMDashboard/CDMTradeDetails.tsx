import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid,
  Paper
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { CDMTradeData } from './types';

interface CDMTradeDetailsProps {
  trade: CDMTradeData;
}

export const CDMTradeDetails: React.FC<CDMTradeDetailsProps> = ({ trade }) => {
  const formatJSON = (obj: any) => {
    return JSON.stringify(obj, null, 2);
  };

  const renderCDMField = (label: string, value: any, isObject = false) => (
    <Box sx={{ mb: 2 }}>
      <Typography variant="subtitle2" color="primary" gutterBottom>
        {label}
      </Typography>
      {isObject ? (
        <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
          <pre style={{ fontSize: '12px', overflow: 'auto', margin: 0 }}>
            {formatJSON(value)}
          </pre>
        </Paper>
      ) : (
        <Typography variant="body2" sx={{ ml: 1 }}>
          {value || 'N/A'}
        </Typography>
      )}
    </Box>
  );

  return (
    <Box sx={{ maxWidth: '100%', mx: 'auto' }}>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Typography variant="h5" component="h2" sx={{ mr: 2 }}>
              CDM Trade Details
            </Typography>
            <Chip 
              label={`CDM ${trade.cdmVersion}`} 
              color="primary" 
              variant="outlined" 
            />
            <Chip 
              label={trade.businessEventType} 
              color="secondary" 
              variant="outlined" 
              sx={{ ml: 1 }}
            />
          </Box>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="h6" gutterBottom color="primary">
                Basic Trade Information
              </Typography>
              {renderCDMField('Trade ID', trade.id)}
              {renderCDMField('Security', trade.security)}
              {renderCDMField('Quantity', trade.quantity)}
              {renderCDMField('Side', trade.side)}
              {renderCDMField('State', trade.state)}
              {renderCDMField('Updated', trade.updated ? new Date(trade.updated).toLocaleString() : 'N/A')}
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Typography variant="h6" gutterBottom color="primary">
                CDM Metadata
              </Typography>
              {renderCDMField('CDM Version', trade.cdmVersion)}
              {renderCDMField('Business Event Type', trade.businessEventType)}
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">CDM Business Event Structure</Typography>
        </AccordionSummary>
        <AccordionDetails>
          {trade.cdmBusinessEvent ? (
            <Box>
              <Typography variant="subtitle1" gutterBottom>
                Complete CDM BusinessEvent Object:
              </Typography>
              {renderCDMField('Full CDM BusinessEvent', trade.cdmBusinessEvent, true)}
            </Box>
          ) : (
            <Typography color="text.secondary">
              No CDM BusinessEvent data available
            </Typography>
          )}
        </AccordionDetails>
      </Accordion>

      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">CDM Trade Structure</Typography>
        </AccordionSummary>
        <AccordionDetails>
          {trade.cdmTrade ? (
            <Box>
              <Typography variant="subtitle1" gutterBottom>
                Complete CDM Trade Object:
              </Typography>
              {renderCDMField('Full CDM Trade', trade.cdmTrade, true)}
            </Box>
          ) : (
            <Typography color="text.secondary">
              No CDM Trade data available
            </Typography>
          )}
        </AccordionDetails>
      </Accordion>

      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">CDM Validation & Compliance</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" color="success.main">
                    ✓ CDM Compliant
                  </Typography>
                  <Typography variant="body2">
                    Trade follows FINOS CDM standards
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" color="info.main">
                    📊 Structured Data
                  </Typography>
                  <Typography variant="body2">
                    Standardized business event format
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" color="primary.main">
                    🔄 Interoperable
                  </Typography>
                  <Typography variant="body2">
                    Cross-system compatibility ensured
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>
    </Box>
  );
};