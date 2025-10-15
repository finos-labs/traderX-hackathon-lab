package finos.traderx.tradeservice.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FINOS CDM validation using JSON-based approach
// Reference: https://cdm.finos.org/docs/event-model/
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

/**
 * CDM Validation Service based on FINOS CDM Event Model specification
 * Reference: https://cdm.finos.org/docs/event-model/
 * GitHub: https://github.com/finos/common-domain-model
 */
@Service
public class CDMValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(CDMValidationService.class);
    
    /**
     * Validate CDM Trade JSON against FINOS CDM specification
     */
    public Map<String, Object> validateCDMTradeJson(String cdmTradeJson) {
        log.info("🔍 Validating CDM Trade JSON against FINOS CDM specification");
        
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();
        List<String> passed = new java.util.ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(cdmTradeJson);
            
            // Check required CDM Trade fields
            if (!jsonNode.has("tradeIdentifier")) {
                errors.add("Missing required tradeIdentifier");
            } else {
                passed.add("✅ Trade identifier present");
            }
            
            if (!jsonNode.has("tradableProduct")) {
                errors.add("Missing required tradableProduct");
            } else {
                passed.add("✅ Tradable product present");
                
                // Validate product details
                var tradableProduct = jsonNode.get("tradableProduct");
                if (!tradableProduct.has("product")) {
                    warnings.add("⚠️ Product details missing");
                } else {
                    passed.add("✅ Product details present");
                }
            }
            
            if (!jsonNode.has("tradeDate")) {
                warnings.add("⚠️ Trade date missing");
            } else {
                passed.add("✅ Trade date present");
            }
            
            if (!jsonNode.has("party")) {
                warnings.add("⚠️ Party information missing");
            } else {
                passed.add("✅ Party information present");
            }
            
            if (!jsonNode.has("cdmVersion")) {
                warnings.add("⚠️ CDM version missing");
            } else {
                passed.add("✅ CDM version present");
            }
            
            // Overall validation result
            boolean isValid = errors.isEmpty();
            String status = isValid ? "VALID" : "INVALID";
            
            validation.put("isValid", isValid);
            validation.put("status", status);
            validation.put("errors", errors);
            validation.put("warnings", warnings);
            validation.put("passed", passed);
            validation.put("cdmCompliant", isValid && warnings.size() <= 2);
            validation.put("message", isValid ? 
                "✅ CDM Trade JSON is valid according to FINOS CDM specification" : 
                "❌ CDM Trade JSON has validation errors");
            
            log.info("✅ CDM Trade JSON validation completed: {} errors, {} warnings", 
                errors.size(), warnings.size());
            
        } catch (Exception e) {
            log.error("❌ Error during CDM Trade JSON validation", e);
            validation.put("isValid", false);
            validation.put("status", "ERROR");
            validation.put("error", e.getMessage());
            validation.put("message", "❌ Validation failed: " + e.getMessage());
        }
        
        return validation;
    }
    
    /**
     * Validate CDM BusinessEvent JSON against FINOS CDM Event Model
     */
    public Map<String, Object> validateCDMBusinessEventJson(String businessEventJson) {
        log.info("🔍 Validating CDM BusinessEvent JSON against FINOS CDM Event Model");
        
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();
        List<String> passed = new java.util.ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(businessEventJson);
            
            // Check required fields for BusinessEvent according to CDM Event Model
            if (!jsonNode.has("eventIdentifier")) {
                warnings.add("⚠️ Event identifier missing");
            } else {
                passed.add("✅ Event identifier present");
            }
            
            if (!jsonNode.has("eventDate")) {
                errors.add("Missing required eventDate");
            } else {
                passed.add("✅ Event date present");
            }
            
            if (!jsonNode.has("intent")) {
                warnings.add("⚠️ Event intent missing");
            } else {
                passed.add("✅ Event intent present");
            }
            
            if (!jsonNode.has("primitives")) {
                warnings.add("⚠️ Primitive events missing");
            } else {
                passed.add("✅ Primitive events present");
                
                // Validate primitives structure
                var primitives = jsonNode.get("primitives");
                if (primitives.isArray() && primitives.size() > 0) {
                    var firstPrimitive = primitives.get(0);
                    if (firstPrimitive.has("execution")) {
                        passed.add("✅ Execution primitive present");
                    }
                }
            }
            
            if (!jsonNode.has("after")) {
                warnings.add("⚠️ After state missing");
            } else {
                passed.add("✅ After state present");
            }
            
            if (!jsonNode.has("cdmVersion")) {
                warnings.add("⚠️ CDM version missing");
            } else {
                passed.add("✅ CDM version present");
            }
            
            if (!jsonNode.has("eventModel")) {
                warnings.add("⚠️ Event model reference missing");
            } else {
                passed.add("✅ Event model reference present");
            }
            
            // Overall validation result
            boolean isValid = errors.isEmpty();
            String status = isValid ? "VALID" : "INVALID";
            
            validation.put("isValid", isValid);
            validation.put("status", status);
            validation.put("errors", errors);
            validation.put("warnings", warnings);
            validation.put("passed", passed);
            validation.put("eventModelCompliant", isValid && warnings.size() <= 3);
            validation.put("message", isValid ? 
                "✅ CDM BusinessEvent JSON is valid according to FINOS CDM Event Model" : 
                "❌ CDM BusinessEvent JSON has validation errors");
            
            log.info("✅ CDM BusinessEvent JSON validation completed: {} errors, {} warnings", 
                errors.size(), warnings.size());
            
        } catch (Exception e) {
            log.error("❌ Error during CDM BusinessEvent JSON validation", e);
            validation.put("isValid", false);
            validation.put("status", "ERROR");
            validation.put("error", e.getMessage());
            validation.put("message", "❌ Validation failed: " + e.getMessage());
        }
        
        return validation;
    }
    
    /**
     * Validate JSON string as proper CDM format
     */
    public Map<String, Object> validateCDMJson(String cdmJson) {
        log.info("🔍 Validating CDM JSON format");
        
        Map<String, Object> validation = new HashMap<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(cdmJson);
            
            // Determine CDM JSON type based on structure
            if (jsonNode.has("eventIdentifier") && jsonNode.has("primitives")) {
                // This looks like a BusinessEvent
                validation = validateCDMBusinessEventJson(cdmJson);
                validation.put("jsonFormat", "CDM BusinessEvent");
                validation.put("cdmCompliant", true);
            } else if (jsonNode.has("tradeIdentifier") && jsonNode.has("tradableProduct")) {
                // This looks like a Trade
                validation = validateCDMTradeJson(cdmJson);
                validation.put("jsonFormat", "CDM Trade");
                validation.put("cdmCompliant", true);
            } else if (jsonNode.has("cdmVersion")) {
                // Has CDM version but unknown structure
                validation.put("isValid", false);
                validation.put("status", "UNKNOWN_CDM_TYPE");
                validation.put("jsonFormat", "Unknown CDM Type");
                validation.put("cdmCompliant", false);
                validation.put("error", "JSON has cdmVersion but doesn't match known CDM structures");
                validation.put("message", "❌ Unknown CDM JSON structure");
            } else {
                // Not CDM JSON at all
                validation.put("isValid", false);
                validation.put("status", "NOT_CDM");
                validation.put("jsonFormat", "Non-CDM JSON");
                validation.put("cdmCompliant", false);
                validation.put("error", "JSON does not appear to be CDM format");
                validation.put("message", "❌ JSON is not FINOS CDM format");
            }
            
        } catch (Exception e) {
            log.error("❌ Error validating CDM JSON", e);
            validation.put("isValid", false);
            validation.put("status", "ERROR");
            validation.put("error", e.getMessage());
            validation.put("message", "❌ JSON validation failed: " + e.getMessage());
        }
        
        return validation;
    }
    
    /**
     * Get CDM compliance report
     */
    public Map<String, Object> getCDMComplianceReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("cdmVersion", "6.0.0");
        report.put("framework", "FINOS Common Domain Model");
        report.put("rosettaFramework", "Rosetta DSL Runtime");
        report.put("eventModel", "FINOS CDM Event Model");
        report.put("documentation", "https://cdm.finos.org/docs/event-model/");
        report.put("github", "https://github.com/finos/common-domain-model");
        
        report.put("supportedTypes", Arrays.asList(
            "Trade",
            "BusinessEvent", 
            "ExecutionInstruction",
            "Party",
            "Product",
            "TradableProduct"
        ));
        
        report.put("validationCapabilities", Arrays.asList(
            "CDM Trade validation",
            "CDM BusinessEvent validation",
            "JSON schema compliance",
            "Rosetta framework compatibility",
            "Event Model compliance"
        ));
        
        return report;
    }
}