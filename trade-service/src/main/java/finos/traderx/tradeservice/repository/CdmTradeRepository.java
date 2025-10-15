package finos.traderx.tradeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import finos.traderx.tradeservice.model.CdmTrade;
import java.util.List;

@Repository
public interface CdmTradeRepository extends JpaRepository<CdmTrade, String> {
    List<CdmTrade> findByAccountId(Integer accountId);
}