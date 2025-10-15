package finos.traderx.tradeprocessor.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Component
public class CDMDatabaseConfig implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(CDMDatabaseConfig.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🚀 Initializing CDM database schema...");
        
        try {
            // Create CDMTRADES table with exact column names matching entity
            entityManager.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS CDMTRADES ( " +
                "ID VARCHAR(50) PRIMARY KEY, " +
                "ACCOUNTID INTEGER, " +
                "CREATED TIMESTAMP, " +
                "UPDATED TIMESTAMP, " +
                "SECURITY VARCHAR(15), " +
                "SIDE VARCHAR(10), " +
                "QUANTITY INTEGER, " +
                "STATE VARCHAR(20), " +
                "CDMTRADEOBJ VARCHAR(2000) " +
                ")"
            ).executeUpdate();
            
            log.info("✅ CDM database schema initialized successfully");
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize CDM database schema: {}", e.getMessage());
            // Don't fail startup if table creation fails
        }
    }
}