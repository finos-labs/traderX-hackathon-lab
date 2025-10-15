package finos.traderx.tradeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import finos.traderx.tradeservice.model.CdmPosition;
import finos.traderx.tradeservice.model.CdmPositionId;
import java.util.List;

/**
 * Repository for CDM Native Positions following FINOS CDM Position model
 */
@Repository
public interface CdmPositionRepository extends JpaRepository<CdmPosition, CdmPositionId> {
    
    List<CdmPosition> findByAccountId(Integer accountId);
    
    CdmPosition findByAccountIdAndSecurity(Integer accountId, String security);
}