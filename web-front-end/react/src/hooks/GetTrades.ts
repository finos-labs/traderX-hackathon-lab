import { SetStateAction, useEffect, useState } from "react";
import { TradeData } from "../Datatable/types";
import { Environment } from '../env';

export const GetTrades = (accountId:number) => {
	const [tradesData, setTradesData] = useState<TradeData[]>([]);
	type data = () => Promise<unknown>;

	useEffect(() => {
		const fetchData: data = async () => {
			try {
				console.log('🔍 Fetching trades for account:', accountId);
				const response = await fetch(`${Environment.trade_service_url}/trade/trades`);
				console.log('📡 Response status:', response.status);
				
				if (response.ok) {
					const result = await response.json();
					console.log('📊 Raw trades response:', result);
					
					// Handle the response format from our new endpoint
					let trades = [];
					if (result.trades && Array.isArray(result.trades)) {
						trades = result.trades;
					} else if (Array.isArray(result)) {
						trades = result;
					}
					
					console.log('📋 All trades before filtering:', trades);
					
					// Filter by account ID if specified
					if (accountId && accountId !== 0) {
						trades = trades.filter((trade: any) => trade.accountId === accountId);
					}
					
					console.log('✅ Filtered trades for account', accountId, ':', trades);
					setTradesData(trades);
				} else {
					console.error('❌ Failed to fetch trades, status:', response.status);
				}
			} catch (error) {
				console.error('❌ Error fetching trades:', error);
				setTradesData([]);
			}
		};
		
		if (accountId !== 0) {
			fetchData();
		} else {
			console.log('⏸️ Skipping trades fetch - no account selected');
		}
	}, [accountId]);
	return tradesData;
}
