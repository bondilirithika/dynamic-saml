package com.example.flutto.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlProviderConfig {
    
    @NotBlank(message = "Provider ID is required")
    private String id;
    
    @NotBlank(message = "Display name is required")
    private String displayName;
    
    private String metadataSource = "manual"; // "manual", "url", or "xml"
    private String metadataUrl;
    private String metadataXml;
    
    @NotBlank(message = "Service Provider entity ID is required")
    private String spEntityId;
    
    @NotBlank(message = "Identity Provider login URL is required")
    private String idpLoginUrl;
    
    private String idpLogoutUrl;
    
    @NotBlank(message = "Identity Provider certificate is required")
    private String idpCertificate;
    
    private String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
    private boolean limitSelfRegistration = false;
    private String customIconUrl;
    
    // Signature and encryption settings
    private boolean signAuthnRequests = false;
    private boolean requireSignedResponses = true;
    private boolean requireEncryptedResponses = false;
    private String spCertificate;
    private String spPrivateKey;
    private String digestAlgorithm = "SHA-1";
    private String signatureAlgorithm = "RSA-SHA1";
    
    // Attribute mappings and requested attributes
    private Map<String, List<String>> attributeMappings;
    private Map<String, Map<String, String>> requestedAttributes;

    private Boolean enabled = true; // Default to enabled

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(String metadataSource) {
        this.metadataSource = metadataSource;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getMetadataXml() {
        return metadataXml;
    }

    public void setMetadataXml(String metadataXml) {
        this.metadataXml = metadataXml;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    public String getIdpLoginUrl() {
        return idpLoginUrl;
    }

    public void setIdpLoginUrl(String idpLoginUrl) {
        this.idpLoginUrl = idpLoginUrl;
    }

    public String getIdpLogoutUrl() {
        return idpLogoutUrl;
    }

    public void setIdpLogoutUrl(String idpLogoutUrl) {
        this.idpLogoutUrl = idpLogoutUrl;
    }

    public String getIdpCertificate() {
        return idpCertificate;
    }

    public void setIdpCertificate(String idpCertificate) {
        this.idpCertificate = idpCertificate;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public void setNameIdFormat(String nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    public boolean isLimitSelfRegistration() {
        return limitSelfRegistration;
    }

    public void setLimitSelfRegistration(boolean limitSelfRegistration) {
        this.limitSelfRegistration = limitSelfRegistration;
    }

    public String getCustomIconUrl() {
        return customIconUrl;
    }

    public void setCustomIconUrl(String customIconUrl) {
        this.customIconUrl = customIconUrl;
    }

    public boolean isSignAuthnRequests() {
        return signAuthnRequests;
    }

    public void setSignAuthnRequests(boolean signAuthnRequests) {
        this.signAuthnRequests = signAuthnRequests;
    }

    public boolean isRequireSignedResponses() {
        return requireSignedResponses;
    }

    public void setRequireSignedResponses(boolean requireSignedResponses) {
        this.requireSignedResponses = requireSignedResponses;
    }

    public boolean isRequireEncryptedResponses() {
        return requireEncryptedResponses;
    }

    public void setRequireEncryptedResponses(boolean requireEncryptedResponses) {
        this.requireEncryptedResponses = requireEncryptedResponses;
    }

    public String getSpCertificate() {
        return spCertificate;
    }

    public void setSpCertificate(String spCertificate) {
        this.spCertificate = spCertificate;
    }

    public String getSpPrivateKey() {
        return spPrivateKey;
    }

    public void setSpPrivateKey(String spPrivateKey) {
        this.spPrivateKey = spPrivateKey;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public Map<String, List<String>> getAttributeMappings() {
        return attributeMappings;
    }

    public void setAttributeMappings(Map<String, List<String>> attributeMappings) {
        this.attributeMappings = attributeMappings;
    }

    public Map<String, Map<String, String>> getRequestedAttributes() {
        return requestedAttributes;
    }

    public void setRequestedAttributes(Map<String, Map<String, String>> requestedAttributes) {
        this.requestedAttributes = requestedAttributes;
    }

    public boolean isEnabled() {
        return enabled == null || enabled; // If null, default to true
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
