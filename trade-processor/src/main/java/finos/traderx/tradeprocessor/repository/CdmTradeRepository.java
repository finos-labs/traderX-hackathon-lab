package finos.traderx.tradeprocessor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import finos.traderx.tradeprocessor.model.CdmTrade;

@Repository
public interface CdmTradeRepository extends JpaRepository<CdmTrade, String> {
}