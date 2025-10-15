package finos.traderx.tradeservice.model;

import java.io.Serializable;

/**
 * Composite key for CdmPosition
 */
public class CdmPositionId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer accountId;
    private String security;

    public CdmPositionId() {}

    public CdmPositionId(Integer accountId, String security) {
        this.accountId = accountId;
        this.security = security;
    }

    // Getters and setters
    public Integer getAccountId() { 
        return accountId; 
    }
    
    public void setAccountId(Integer accountId) { 
        this.accountId = accountId; 
    }
    
    public String getSecurity() { 
        return security; 
    }
    
    public void setSecurity(String security) { 
        this.security = security; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CdmPositionId)) return false;
        CdmPositionId that = (CdmPositionId) o;
        return accountId.equals(that.accountId) && security.equals(that.security);
    }

    @Override
    public int hashCode() {
        return accountId.hashCode() + security.hashCode();
    }
}