package com.example.flutto.service;

import com.example.flutto.model.SamlProviderConfig;
import com.example.flutto.model.SamlProvidersConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SamlConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(SamlConfigurationService.class);
    
    @Value("classpath:saml-providers.yaml")
    private Resource yamlResource;
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    private SamlProvidersConfig providersConfig;
    
    /**
     * Loads all SAML providers from the YAML configuration file
     */
    public List<SamlProviderConfig> getAllProviders() {
        try {
            if (providersConfig == null) {
                loadProviders();
            }
            return providersConfig.getProviders();
        } catch (Exception e) {
            logger.error("Error loading SAML providers", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Loads only enabled SAML providers
     */
    public List<SamlProviderConfig> getEnabledProviders() {
        return getAllProviders().stream()
                .filter(SamlProviderConfig::isEnabled)  // Use method reference
                .collect(Collectors.toList());
    }
    
    /**
     * Find a specific provider by ID
     */
    public Optional<SamlProviderConfig> getProviderById(String id) {
        return getAllProviders().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }
    
    /**
     * Saves a provider to the configuration
     */
    public synchronized SamlProviderConfig saveProvider(SamlProviderConfig provider) throws IOException {
        if (providersConfig == null) {
            loadProviders();
        }
        
        // Remove existing provider with same ID if it exists
        providersConfig.getProviders().removeIf(p -> p.getId().equals(provider.getId()));
        
        // Add the new/updated provider
        providersConfig.getProviders().add(provider);
        
        // Save to YAML file
        saveProvidersToYaml();
        
        return provider;
    }
    
    /**
     * Deletes a provider from the configuration
     */
    public synchronized boolean deleteProvider(String id) throws IOException {
        if (providersConfig == null) {
            loadProviders();
        }
        
        boolean removed = providersConfig.getProviders().removeIf(p -> p.getId().equals(id));
        
        if (removed) {
            saveProvidersToYaml();
        }
        
        return removed;
    }
    
    /**
     * Loads providers from the YAML file
     */
    private synchronized void loadProviders() throws IOException {
        File file = yamlResource.getFile();
        if (!file.exists()) {
            providersConfig = new SamlProvidersConfig();
            return;
        }
        
        providersConfig = yamlMapper.readValue(file, SamlProvidersConfig.class);
        if (providersConfig == null) {
            providersConfig = new SamlProvidersConfig();
        }
    }
    
    /**
     * Saves providers to the YAML file
     */
    private synchronized void saveProvidersToYaml() throws IOException {
        //Get the runtime file (in target/classes)
        File runtimeFile = yamlResource.getFile();
        
        // Identify the source file location
        String sourceFilePath = System.getProperty("user.dir") + 
            "/src/main/resources/saml-providers.yaml";
        File sourceFile = new File(sourceFilePath);
        
        // Log both locations
        logger.info("Runtime YAML file path: {}", runtimeFile.getAbsolutePath());
        logger.info("Source YAML file path: {}", sourceFile.getAbsolutePath());
        
        // Save to both locations
        yamlMapper.writeValue(runtimeFile, providersConfig);
        yamlMapper.writeValue(sourceFile, providersConfig);
        
        logger.info("SAML provider configuration saved to both runtime and source locations");
    }
    
    /**
     * Initializes the service, loading providers and logging the configuration
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing SamlConfigurationService");
        try {
            List<SamlProviderConfig> providers = getAllProviders();
            logger.info("Loaded {} providers from configuration", providers.size());
            providers.forEach(p -> logger.info("Provider: {}, Enabled: {}", p.getId(), p.isEnabled()));
        } catch (Exception e) {
            logger.error("Failed to load SAML providers", e);
        }
    }
}