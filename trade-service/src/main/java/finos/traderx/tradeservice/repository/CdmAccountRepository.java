package finos.traderx.tradeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import finos.traderx.tradeservice.model.CdmAccount;

/**
 * Repository for CDM Native Accounts following FINOS CDM Party model
 */
@Repository
public interface CdmAccountRepository extends JpaRepository<CdmAccount, Integer> {
    
    CdmAccount findByDisplayName(String displayName);
}