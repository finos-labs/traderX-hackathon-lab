package finos.traderx.tradeservice.model;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * CDM Native Account Entity following FINOS CDM Party model
 */
@Entity
@Table(name = "CDMACCOUNTS")
public class CdmAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "DISPLAYNAME", length = 50)
    private String displayName;

    @Column(name = "CDMACCOUNTOBJ", length = 2000)
    private String cdmAccountObj;

    // Constructors
    public CdmAccount() {}

    public CdmAccount(Integer id, String displayName, String cdmAccountObj) {
        this.id = id;
        this.displayName = displayName;
        this.cdmAccountObj = cdmAccountObj;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCdmAccountObj() {
        return cdmAccountObj;
    }

    public void setCdmAccountObj(String cdmAccountObj) {
        this.cdmAccountObj = cdmAccountObj;
    }
}