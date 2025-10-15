import { SetStateAction, useEffect, useState } from "react";
import { PositionData } from "../Datatable/types";
import { Environment } from '../env';

export const GetPositions = (accountId:number) => {
	const [positionsData, setPositionsData] = useState<PositionData[]>([]);
	type data = () => Promise<unknown>;
	useEffect(() => {
		const fetchData: data = async () => {
			try {
				console.log('🔍 Fetching positions for account:', accountId);
				const response = await fetch(`${Environment.trade_service_url}/trade/positions/${accountId}`);
				console.log('📡 Positions response status:', response.status);
				
				if (response.ok) {
					const json = await response.json();
					console.log('📊 Positions data:', json);
					setPositionsData(json);
				} else {
					console.error('❌ Failed to fetch positions, status:', response.status);
				}
			} catch (error) {
				console.error('❌ Error fetching positions:', error);
				setPositionsData([]);
			}
		};
		
		if (accountId !== 0) {
			fetchData();
		} else {
			console.log('⏸️ Skipping positions fetch - no account selected');
		}
	}, [accountId]);
	return positionsData;
}
