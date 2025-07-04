package com.example.flutto.controller;

import com.example.flutto.config.DynamicSamlConfig;
import com.example.flutto.model.SamlProviderConfig;
import com.example.flutto.service.SamlConfigurationService;
import com.example.flutto.service.SamlMetadataService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/saml")
@PreAuthorize("hasRole('ADMIN')")  // Ensure only admins can access these endpoints
public class SamlAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SamlAdminController.class);
    
    private final SamlConfigurationService configService;
    private final SamlMetadataService metadataService;
    private final DynamicSamlConfig samlConfig;
    
    @Autowired
    public SamlAdminController(
            SamlConfigurationService configService,
            SamlMetadataService metadataService,
            DynamicSamlConfig samlConfig) {
        this.configService = configService;
        this.metadataService = metadataService;
        this.samlConfig = samlConfig;
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
    public ResponseEntity<SamlProviderConfig> createProvider(@Valid @RequestBody SamlProviderConfig provider) {
        try {
            // Generate an ID if none provided
            if (provider.getId() == null || provider.getId().isEmpty()) {
                provider.setId(generateProviderId(provider.getDisplayName()));
            }
            
            SamlProviderConfig savedProvider = configService.saveProvider(provider);
            
            // Refresh SAML configurations
            samlConfig.refreshRegistrations();
            
            return ResponseEntity.ok(savedProvider);
        } catch (Exception e) {
            logger.error("Error creating SAML provider", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/providers/{id}")
    public ResponseEntity<SamlProviderConfig> updateProvider(
            @PathVariable String id, 
            @Valid @RequestBody SamlProviderConfig provider) {
        
        try {
            if (!id.equals(provider.getId())) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!configService.getProviderById(id).isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            SamlProviderConfig savedProvider = configService.saveProvider(provider);
            
            // Refresh SAML configurations
            samlConfig.refreshRegistrations();
            
            return ResponseEntity.ok(savedProvider);
        } catch (Exception e) {
            logger.error("Error updating SAML provider", e);
            return ResponseEntity.badRequest().build();
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