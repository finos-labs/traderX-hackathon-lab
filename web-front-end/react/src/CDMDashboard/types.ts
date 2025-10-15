import { TradeData } from '../Datatable/types';

export interface CDMTradeData extends TradeData {
  cdmVersion: string;
  businessEventType: string;
  cdmBusinessEvent?: any; // Full CDM BusinessEvent object
  cdmTrade?: any; // Full CDM Trade object
}

export interface CDMBusinessEvent {
  eventIdentifier: Array<{
    identifier: {
      value: string;
    };
  }>;
  eventDate: string;
  effectiveDate: string;
  primitives: {
    execution: Array<{
      after: {
        trade: CDMTrade;
      };
    }>;
  };
}

export interface CDMTrade {
  tradeIdentifier: Array<{
    identifier: {
      value: string;
    };
  }>;
  tradeDate: {
    value: string;
  };
  tradableProduct: {
    product: {
      security: {
        identifier: Array<{
          identifier: {
            value: string;
          };
        }>;
      };
    };
  };
  quantity: Array<{
    value: number;
  }>;
  counterparty: Array<{
    role: string;
    partyReference: {
      value: string;
    };
  }>;
}