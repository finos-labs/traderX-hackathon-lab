package finos.traderx.tradeprocessor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/cdm", produces = "application/json")
public class CDMController {

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/migrate")
    @Transactional
    public ResponseEntity<Map<String, Object>> runCDMMigration() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Create CDM table
            entityManager.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS CdmTrades ( " +
                "ID VARCHAR(50) PRIMARY KEY, " +
                "AccountID INTEGER, " +
                "Created TIMESTAMP, " +
                "Updated TIMESTAMP, " +
                "Security VARCHAR(15), " +
                "Side VARCHAR(10), " +
                "Quantity INTEGER, " +
                "State VARCHAR(20), " +
                "CdmTradeObj TEXT" +
                ")"
            ).executeUpdate();
            
            result.put("status", "success");
            result.put("message", "CDM tables created successfully");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}