package finos.traderx.tradeservice.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * CDM Native Trade Entity following FINOS CDM Trade model
 * Based on: https://github.com/tomhealey-icma/traderXcdm/blob/main/database/initialSchema.sql
 */
@Entity
@Table(name = "CDMTRADES")
public class CdmTrade implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 50, name = "ID")
    private String id;

    @Column(name = "ACCOUNTID")
    private Integer accountId;

    @Column(name = "CREATED")
    private Date created;

    @Column(name = "UPDATED")
    private Date updated;

    @Column(length = 15, name = "SECURITY")
    private String security;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, name = "SIDE")
    private TradeSide side;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(length = 20, name = "STATE")
    private String state = "New";

    @Column(length = 10000, name = "CDMTRADEOBJ")
    private String cdmTradeObj;

    // Constructors
    public CdmTrade() {}

    public CdmTrade(String id, Integer accountId, String security, TradeSide side, Integer quantity, String state, String cdmTradeObj) {
        this.id = id;
        this.accountId = accountId;
        this.security = security;
        this.side = side;
        this.quantity = quantity;
        this.state = state;
        this.cdmTradeObj = cdmTradeObj;
        this.created = new Date();
        this.updated = new Date();
    }

    // Getters and Setters
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public Integer getAccountId() {
        return this.accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getSecurity() {
        return this.security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public TradeSide getSide() {
        return this.side;
    }

    public void setSide(TradeSide side) {
        this.side = side;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Date getUpdated() {
        return this.updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCdmTradeObj() {
        return this.cdmTradeObj;
    }

    public void setCdmTradeObj(String cdmTradeObj) {
        this.cdmTradeObj = cdmTradeObj;
    }

    // Legacy compatibility methods
    public String getCdmTrade() {
        return this.cdmTradeObj;
    }

    public void setCdmTrade(String cdmTrade) {
        this.cdmTradeObj = cdmTrade;
    }
}