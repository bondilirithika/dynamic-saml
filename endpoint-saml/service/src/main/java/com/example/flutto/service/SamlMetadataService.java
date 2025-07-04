package com.example.flutto.service;

import com.example.flutto.model.SamlProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SamlMetadataService {
    private static final Logger logger = LoggerFactory.getLogger(SamlMetadataService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Parses SAML metadata from a URL and pre-fills a SamlProviderConfig
     */
    public SamlProviderConfig parseMetadataFromUrl(String url) {
        try {
            String metadataXml = restTemplate.getForObject(url, String.class);
            SamlProviderConfig config = new SamlProviderConfig();
            config.setMetadataSource("url");
            config.setMetadataUrl(url);
            
            return parseMetadataXml(metadataXml, config);
        } catch (Exception e) {
            logger.error("Error fetching or parsing metadata from URL: " + url, e);
            throw new RuntimeException("Failed to parse metadata from URL: " + e.getMessage());
        }
    }

    /**
     * Parses SAML metadata XML and pre-fills a SamlProviderConfig
     */
    public SamlProviderConfig parseMetadataXml(String metadataXml, SamlProviderConfig config) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(metadataXml)));
            
            // Set metadataXml if the source is 'xml'
            if ("xml".equals(config.getMetadataSource())) {
                config.setMetadataXml(metadataXml);
            }
            
            // Extract EntityID
            Element entityDescriptor = document.getDocumentElement();
            String entityId = entityDescriptor.getAttribute("entityID");
            if (entityId != null && !entityId.isEmpty()) {
                config.setSpEntityId(entityId);
            }
            
            // Extract SSO URL
            NodeList ssoElements = document.getElementsByTagNameNS(
                    "urn:oasis:names:tc:SAML:2.0:metadata", 
                    "SingleSignOnService");
            for (int i = 0; i < ssoElements.getLength(); i++) {
                Element ssoElement = (Element) ssoElements.item(i);
                String binding = ssoElement.getAttribute("Binding");
                if (binding.contains("HTTP-Redirect")) {
                    config.setIdpLoginUrl(ssoElement.getAttribute("Location"));
                    break;
                }
            }
            
            // Extract SLO URL
            NodeList sloElements = document.getElementsByTagNameNS(
                    "urn:oasis:names:tc:SAML:2.0:metadata", 
                    "SingleLogoutService");
            for (int i = 0; i < sloElements.getLength(); i++) {
                Element sloElement = (Element) sloElements.item(i);
                String binding = sloElement.getAttribute("Binding");
                if (binding.contains("HTTP-Redirect")) {
                    config.setIdpLogoutUrl(sloElement.getAttribute("Location"));
                    break;
                }
            }
            
            // Extract certificate
            NodeList certElements = document.getElementsByTagNameNS(
                    "http://www.w3.org/2000/09/xmldsig#", 
                    "X509Certificate");
            if (certElements.getLength() > 0) {
                String certValue = certElements.item(0).getTextContent();
                String formattedCert = "-----BEGIN CERTIFICATE-----\n" + 
                        certValue + "\n-----END CERTIFICATE-----";
                config.setIdpCertificate(formattedCert);
            }
            
            // Set default values
            if (config.getNameIdFormat() == null) {
                config.setNameIdFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
            }
            
            // Set attribute mappings if not already set
            if (config.getAttributeMappings() == null) {
                Map<String, List<String>> attributeMappings = new HashMap<>();
                attributeMappings.put("username", List.of("mail", "email", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
                attributeMappings.put("email", List.of("mail", "email", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
                attributeMappings.put("firstName", List.of("givenName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"));
                attributeMappings.put("lastName", List.of("sn", "surname", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
                config.setAttributeMappings(attributeMappings);
            }
            
            // Set requested attributes if not already set
            if (config.getRequestedAttributes() == null) {
                Map<String, Map<String, String>> requestedAttributes = new HashMap<>();
                Map<String, String> emailAttr = new HashMap<>();
                emailAttr.put("name", "mail");
                emailAttr.put("format", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic");
                requestedAttributes.put("email", emailAttr);
                
                Map<String, String> firstNameAttr = new HashMap<>();
                firstNameAttr.put("name", "givenName");
                firstNameAttr.put("format", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic");
                requestedAttributes.put("firstName", firstNameAttr);
                
                Map<String, String> lastNameAttr = new HashMap<>();
                lastNameAttr.put("name", "sn");
                lastNameAttr.put("format", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic");
                requestedAttributes.put("lastName", lastNameAttr);
                
                config.setRequestedAttributes(requestedAttributes);
            }
            
            return config;
        } catch (Exception e) {
            logger.error("Error parsing metadata XML", e);
            throw new RuntimeException("Failed to parse metadata XML: " + e.getMessage());
        }
    }
}