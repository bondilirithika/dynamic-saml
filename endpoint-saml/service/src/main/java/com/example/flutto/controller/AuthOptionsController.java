package com.example.flutto.controller;

import com.example.flutto.model.SamlProviderConfig;
import com.example.flutto.service.SamlConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth/options")
public class AuthOptionsController {

    private final SamlConfigurationService configService;
    
    @Autowired
    public AuthOptionsController(SamlConfigurationService configService) {
        this.configService = configService;
    }
    
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getLoginOptions() {
        Logger logger = LoggerFactory.getLogger(AuthOptionsController.class);
        logger.info("Fetching login options");
        
        List<SamlProviderConfig> providers = configService.getEnabledProviders();
        logger.info("Found {} enabled providers", providers.size());
        
        // If no providers, add a default one for testing
        // /*
        // if (providers.isEmpty()) {
        //     logger.warn("No providers found, adding test Google provider");
        //     Map<String, String> googleProvider = new HashMap<>();
        //     googleProvider.put("id", "google");
        //     googleProvider.put("displayName", "Google Workspace");
        //     googleProvider.put("type", "saml");
        //     googleProvider.put("iconUrl", "https://upload.wikimedia.org/wikipedia/commons/5/53/Google_%22G%22_Logo.svg");
            
        //     return ResponseEntity.ok(Collections.singletonList(googleProvider));
        // }
        // */
        
        // Original code
        List<Map<String, String>> options = providers.stream()
                .map(provider -> Map.of(
                    "id", provider.getId(),
                    "displayName", provider.getDisplayName(),
                    "type", "saml",
                    "iconUrl", provider.getCustomIconUrl() != null ? provider.getCustomIconUrl() : ""
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(options);
    }
}