package com.example.flutto.controller;

import com.example.flutto.config.DynamicSamlConfig;
import com.example.flutto.model.SamlProviderConfig;
import com.example.flutto.service.SamlConfigurationService;
import com.example.flutto.service.SamlMetadataService;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Validator;

@RestController
@RequestMapping("/api/admin/saml")
public class SamlAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SamlAdminController.class);
    
    private final SamlConfigurationService configService;
    private final SamlMetadataService metadataService;
    private final DynamicSamlConfig samlConfig;
    private final Environment environment;
    
    @Autowired
    private Validator validator;
    
    @Autowired
    public SamlAdminController(
            SamlConfigurationService configService,
            SamlMetadataService metadataService,
            DynamicSamlConfig samlConfig,
            Environment environment) {
        this.configService = configService;
        this.metadataService = metadataService;
        this.samlConfig = samlConfig;
        this.environment = environment;
    }
    
    @GetMapping("/providers")
    public ResponseEntity<List<SamlProviderConfig>> getAllProviders() {
        return ResponseEntity.ok(configService.getAllProviders());
    }
    
    @GetMapping("/providers/{id}")
    public ResponseEntity<SamlProviderConfig> getProviderById(@PathVariable String id) {
        return configService.getProviderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/providers")
    public ResponseEntity<SamlProviderConfig> createProvider(@RequestBody SamlProviderConfig provider) {
        try {
            logger.info("Creating new SAML provider: {}", provider.getDisplayName());
            
            // Generate an ID if none provided
            if (provider.getId() == null || provider.getId().isEmpty()) {
                provider.setId(generateProviderId(provider.getDisplayName()));
                logger.info("Generated provider ID: {}", provider.getId());
            }
            
            // Set proper Entity ID based on application base URL
            String baseUrl = environment.getProperty("saml.service-provider.base-url");
            if (baseUrl == null) {
                baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                logger.info("Using current context URL as base: {}", baseUrl);
            }
            
            // Set entity ID BEFORE validation
            provider.setSpEntityId(baseUrl + "/saml2/service-provider-metadata/" + provider.getId());
            logger.info("Set entity ID to: {}", provider.getSpEntityId());
            
            // Manual validation AFTER setting required fields
            Set<ConstraintViolation<SamlProviderConfig>> violations = validator.validate(provider);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<SamlProviderConfig> violation : violations) {
                    sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
                }
                logger.error("Validation errors: {}", sb.toString());
                return ResponseEntity.badRequest().body(null);
            }
            
            SamlProviderConfig savedProvider = configService.saveProvider(provider);
            logger.info("Provider saved successfully");
            
            // Refresh SAML configurations
            samlConfig.refreshRegistrations();
            logger.info("SAML registrations refreshed");
            
            return ResponseEntity.ok(savedProvider);
        } catch (Exception e) {
            logger.error("Error creating SAML provider: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @PutMapping("/providers/{id}")
    public ResponseEntity<SamlProviderConfig> updateProvider(
            @PathVariable String id, 
            @RequestBody SamlProviderConfig provider) {
        
        try {
            if (!id.equals(provider.getId())) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!configService.getProviderById(id).isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Ensure we're using the exact same ID
            provider.setId(id);
            
            // Update the Entity ID with the current backend URL
            String baseUrl = environment.getProperty("saml.service-provider.base-url");
            if (baseUrl == null) {
                baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                logger.info("Using current context URL as base: {}", baseUrl);
            }
            
            // Set the Entity ID to use the current backend URL
            provider.setSpEntityId(baseUrl + "/saml2/service-provider-metadata/" + provider.getId());
            logger.info("Updated entity ID to: {}", provider.getSpEntityId());
            
            // Manual validation AFTER setting required fields
            Set<ConstraintViolation<SamlProviderConfig>> violations = validator.validate(provider);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<SamlProviderConfig> violation : violations) {
                    sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
                }
                logger.error("Validation errors: {}", sb.toString());
                return ResponseEntity.badRequest().body(null);
            }
            
            SamlProviderConfig savedProvider = configService.saveProvider(provider);
            
            // Refresh SAML configurations
            samlConfig.refreshRegistrations();
            
            return ResponseEntity.ok(savedProvider);
        } catch (Exception e) {
            logger.error("Error updating SAML provider", e);
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    @DeleteMapping("/providers/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable String id) {
        try {
            boolean deleted = configService.deleteProvider(id);
            
            if (deleted) {
                // Refresh SAML configurations
                samlConfig.refreshRegistrations();
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error deleting SAML provider", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/parse-metadata-url")
    public ResponseEntity<SamlProviderConfig> parseMetadataUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            SamlProviderConfig config = metadataService.parseMetadataFromUrl(url);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("Error parsing metadata URL", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/parse-metadata-xml")
    public ResponseEntity<SamlProviderConfig> parseMetadataXml(@RequestBody Map<String, String> request) {
        String xml = request.get("xml");
        if (xml == null || xml.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            SamlProviderConfig config = new SamlProviderConfig();
            config.setMetadataSource("xml");
            
            SamlProviderConfig parsedConfig = metadataService.parseMetadataXml(xml, config);
            return ResponseEntity.ok(parsedConfig);
        } catch (Exception e) {
            logger.error("Error parsing metadata XML", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshProviders() {
        samlConfig.refreshRegistrations();
        return ResponseEntity.ok().build();
    }
    
    private String generateProviderId(String displayName) {
        // Create a URL-friendly ID from the display name
        String baseId = displayName.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        // Ensure uniqueness by adding a random suffix if needed
        if (configService.getProviderById(baseId).isPresent()) {
            return baseId + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        return baseId;
    }
}