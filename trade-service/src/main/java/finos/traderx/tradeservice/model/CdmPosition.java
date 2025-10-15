package finos.traderx.tradeservice.model;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * CDM Native Position Entity following FINOS CDM Position model
 */
@Entity
@Table(name = "CDMPOSITIONS")
@IdClass(CdmPositionId.class)
public class CdmPosition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ACCOUNTID")
    private Integer accountId;

    @Id
    @Column(name = "SECURITY", length = 15)
    private String security;

    @Column(name = "UPDATED")
    private Date updated;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "CDMPOSITIONOBJ", length = 2000)
    private String cdmPositionObj;

    // Constructors
    public CdmPosition() {}

    public CdmPosition(Integer accountId, String security, Date updated, Integer quantity, String cdmPositionObj) {
        this.accountId = accountId;
        this.security = security;
        this.updated = updated;
        this.quantity = quantity;
        this.cdmPositionObj = cdmPositionObj;
    }

    // Getters and Setters
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

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCdmPositionObj() {
        return cdmPositionObj;
    }

    public void setCdmPositionObj(String cdmPositionObj) {
        this.cdmPositionObj = cdmPositionObj;
    }
}