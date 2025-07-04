package com.example.flutto.config;

import com.example.flutto.model.SamlProviderConfig;
import com.example.flutto.service.SamlConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.math.BigInteger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

@Configuration
@ConditionalOnBean(SamlConfigurationService.class)
@EnableScheduling
public class DynamicSamlConfig {
    private static final Logger logger = LoggerFactory.getLogger(DynamicSamlConfig.class);
    
    private final SamlConfigurationService configService;
    private final ConcurrentHashMap<String, RelyingPartyRegistration> registrations = new ConcurrentHashMap<>();
    
    @Autowired
    public DynamicSamlConfig(SamlConfigurationService configService) {
        this.configService = configService;
    }
    
    // Add to the top of the class as a static initializer
    static {
        // Register Bouncy Castle if not already registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    @Bean
    @Primary
    @ConditionalOnProperty(name = "saml.enabled", havingValue = "true", matchIfMissing = true)
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        List<SamlProviderConfig> providers = configService.getEnabledProviders();
        logger.info("Found {} enabled SAML providers", providers.size());
        
        List<RelyingPartyRegistration> registrations = new ArrayList<>();
        
        // Try to build registrations from enabled providers
        for (SamlProviderConfig provider : providers) {
            try {
                RelyingPartyRegistration registration = buildRegistration(provider);
                registrations.add(registration);
                logger.info("Successfully built registration for provider: {}", provider.getId());
            } catch (Exception e) {
                logger.error("Failed to build registration for provider {}: {}", provider.getId(), e.getMessage(), e);
            }
        }
        
        // If no valid registrations, create a dummy one
        if (registrations.isEmpty()) {
            logger.warn("No valid SAML providers found. Creating a dummy registration for startup.");
            
            RelyingPartyRegistration dummyRegistration = RelyingPartyRegistration.withRegistrationId("dummy")
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/dummy")
                .entityId("{baseUrl}/saml2/service-provider-metadata/dummy")
                .assertingPartyDetails(party -> party
                    .entityId("https://example.com/dummy")
                    .singleSignOnServiceLocation("https://example.com/dummy")
                    .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT)
                )
                .build();
            
            registrations.add(dummyRegistration);
        }
        
        return new InMemoryRelyingPartyRegistrationRepository(
            registrations.toArray(new RelyingPartyRegistration[0])
        );
    }
    
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        refreshRegistrations();
    }
    
    @Scheduled(fixedDelayString = "${saml.config.refresh-interval-ms:300000}")
    public void scheduledRefresh() {
        refreshRegistrations();
    }
    
    public synchronized void refreshRegistrations() {
        logger.info("Refreshing SAML provider configurations");
        List<SamlProviderConfig> providers = configService.getEnabledProviders();
        
        // Clear existing registrations that aren't in the updated list
        List<String> activeProviderIds = providers.stream()
                .map(SamlProviderConfig::getId)
                .toList();
        
        registrations.keySet().stream()
                .filter(id -> !activeProviderIds.contains(id))
                .forEach(registrations::remove);
        
        // Add/update registrations
        for (SamlProviderConfig provider : providers) {
            try {
                RelyingPartyRegistration registration = buildRegistration(provider);
                registrations.put(provider.getId(), registration);
                logger.info("Configured SAML provider: {}", provider.getId());
            } catch (Exception e) {
                logger.error("Error configuring SAML provider {}: {}", provider.getId(), e.getMessage(), e);
            }
        }
    }
    
    private RelyingPartyRegistration buildRegistration(SamlProviderConfig provider) {
        try {
            RelyingPartyRegistration.Builder builder = RelyingPartyRegistration
                .withRegistrationId(provider.getId())
                .entityId(provider.getSpEntityId())
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/" + provider.getId())
                .assertionConsumerServiceBinding(Saml2MessageBinding.POST);
            
            // Configure IdP details
            builder.assertingPartyDetails(party -> {
                // Set entity ID and SSO endpoint
                party.entityId(provider.getIdpLoginUrl().contains("idpid=") ? 
                    "https://accounts.google.com/o/saml2?idpid=" + provider.getIdpLoginUrl().split("idpid=")[1] : 
                    provider.getIdpLoginUrl())
                    .singleSignOnServiceLocation(provider.getIdpLoginUrl())
                    .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT);
                
                // Add IdP certificate for response verification
                if (provider.getIdpCertificate() != null && !provider.getIdpCertificate().isEmpty()) {
                    party.verificationX509Credentials(c -> 
                        c.add(loadCertificate(provider.getIdpCertificate())));
                }
            });
            
            // SIGNING STRATEGY:
            // 1. If user explicitly wants to sign (signAuthnRequests=true) and provides credentials, use those
            // 2. If user wants to sign but doesn't provide credentials, generate self-signed ones and warn
            // 3. If user doesn't want to sign (signAuthnRequests=false), still provide self-signed credentials
            //    to satisfy Spring Security, but the framework won't actually sign the requests if properly configured
            
            if (provider.isSignAuthnRequests() && 
                provider.getSpCertificate() != null && 
                provider.getSpPrivateKey() != null) {
                // Case 1: User-provided credentials
                Saml2X509Credential signingCredential = getSigningCredential(
                    provider.getSpCertificate(), 
                    provider.getSpPrivateKey()
                );
                builder.signingX509Credentials(c -> c.add(signingCredential));
                logger.info("Using user-provided signing credentials for provider: {}", provider.getId());
            } else if (provider.isSignAuthnRequests()) {
                // Case 2: Auto-generate credentials for signing
                generateAndAddSigningCredentials(builder);
                logger.warn("User requested signed AuthnRequests but didn't provide credentials for provider: {}. Using auto-generated credentials.", provider.getId());
            } else {
                // Case 3: Provide credentials but don't actually sign
                generateAndAddSigningCredentials(builder);
                logger.info("Provider {} doesn't require signed AuthnRequests, but providing default credentials to satisfy Spring Security", provider.getId());
            }
            
            return builder.build();
        } catch (Exception e) {
            logger.error("Error building SAML registration for provider " + provider.getId(), e);
            throw new RuntimeException("Failed to build SAML registration: " + e.getMessage(), e);
        }
    }
    
    private Saml2X509Credential loadCertificate(String certificateData) {
        try {
            String cleanCert = certificateData
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
                
            byte[] certBytes = Base64.getDecoder().decode(cleanCert);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certBytes));
                
            return Saml2X509Credential.verification(certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load certificate: " + e.getMessage(), e);
        }
    }
    
    private Saml2X509Credential getSigningCredential(String certificateData, String privateKeyData) {
        try {
            // Load certificate
            String cleanCert = certificateData
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
                
            byte[] certBytes = Base64.getDecoder().decode(cleanCert);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certBytes));
            
            // Load private key
            String cleanKey = privateKeyData
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
                
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            
            return Saml2X509Credential.signing(privateKey, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create signing credential: " + e.getMessage(), e);
        }
    }
    
    private void generateAndAddSigningCredentials(RelyingPartyRegistration.Builder builder) {
        try {
            // Generate a temporary self-signed certificate
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            // Simple X509 certificate creation (in production, use proper libraries like Bouncy Castle)
            X509Certificate certificate = generateSelfSignedCertificate(keyPair);
            
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        // Create the credential and explicitly add it
        Saml2X509Credential signingCredential = Saml2X509Credential.signing(privateKey, certificate);
        
        // Add to builder - IMPORTANT: must use this exact format
        builder.signingX509Credentials(c -> c.add(signingCredential));
        
        logger.info("Successfully generated and added signing credentials");
    } catch (Exception e) {
        logger.error("Failed to generate signing credentials", e);
        throw new RuntimeException("Failed to generate signing credentials", e);
    }
}

    /**
     * Generates a self-signed X509Certificate for the given KeyPair.
     * NOTE: This is a minimal implementation for demonstration purposes.
     * For production, use a library like Bouncy Castle for certificate creation.
     */
    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair) {
        try {
            // Create a valid from/to date for the certificate
            long now = System.currentTimeMillis();
            Date startDate = new Date(now);
            Date endDate = new Date(now + 365L * 24 * 60 * 60 * 1000); // 1 year validity

            // Generate a simple self-signed certificate using plain JDK
            return new SelfSignedCertificateGenerator().generate(keyPair, "CN=SelfSigned", startDate, endDate);
        } catch (Exception e) {
            logger.error("Failed to generate self-signed certificate", e);
            throw new RuntimeException("Failed to generate self-signed certificate: " + e.getMessage(), e);
        }
    }

    // Add this helper class
    private static class SelfSignedCertificateGenerator {
        public X509Certificate generate(KeyPair keyPair, String subjectDN, Date startDate, Date endDate) throws Exception {
            // Create certificate serial number
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
            
            // Create X500Name for subject
            X500Name name = new X500Name(subjectDN);
            
            // Create certificate generator
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    name,                       // Issuer = Subject for self-signed
                    serialNumber,               // Serial number
                    startDate,                  // Start date
                    endDate,                    // End date
                    name,                       // Subject name
                    keyPair.getPublic()         // Public key
            );
            
            // Create the signature
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(keyPair.getPrivate());
            
            // Build and return the certificate
            return new JcaX509CertificateConverter().getCertificate(
                    certBuilder.build(signer));
        }
    }
}