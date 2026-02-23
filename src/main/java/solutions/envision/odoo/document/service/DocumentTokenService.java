package solutions.envision.odoo.document.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import ao.co.oportunidade.order.model.Order;
import ao.co.oportunidade.order.model.PackageType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for generating JWT-based document access tokens.
 * 
 * Tokens are multi-use and include:
 * - Employer identification
 * - Authorized candidate IDs
 * - Authorized Odoo document IDs
 * - Expiration based on package type
 * - Audit metadata
 * 
 * Business Rules:
 * - One token per order
 * - Token can be used multiple times until expiration
 * - All access attempts are logged for compliance
 * - Tokens are signed with RSA private key
 * 
 * @author Oportunidade Team
 * @since Phase 1 - Critical Fixes
 */
@ApplicationScoped
public class DocumentTokenService {
    
    private static final Logger LOG = Logger.getLogger(DocumentTokenService.class);
    
    @ConfigProperty(name = "jwt.issuer")
    String jwtIssuer;
    
    @ConfigProperty(name = "app.base-url")
    String baseUrl;
    
    /**
     * Generate JWT access token for document download based on order details.
     * 
     * The token includes claims for:
     * - Order ID (for audit trail)
     * - Employer ID (who purchased access)
     * - Package type (access level)
     * - Candidate IDs (which candidates' documents)
     * - Odoo document IDs (specific documents authorized)
     * 
     * @param order The order for which to generate the token
     * @return JWT token string (signed)
     * @throws IllegalArgumentException if order is missing required fields
     */
    public String generateAccessToken(Order order) {
        
        validateOrder(order);
        
        int expirationHours = order.getPackageType().getExpirationHours();
        
        LOG.infof("Generating access token for order %s, employer %s, package %s (%dh expiration)",
            order.getId(), order.getEmployerId(), order.getPackageType(), expirationHours);
        
        try {
            String token = Jwt.issuer(jwtIssuer)
                .subject(order.getEmployerEmail())
                .claim("orderId", order.getId().toString())
                .claim("employerId", order.getEmployerId())
                .claim("employerEmail", order.getEmployerEmail())
                .claim("packageType", order.getPackageType().name())
                .claim("candidateIds", order.getCandidateIds())
                .claim("odooDocumentIds", order.getOdooDocumentIds())
                .claim("tokenType", "document-access")
                .claim("multiUse", true)  // Token can be used multiple times
                .claim("merchantTransactionId", order.getMerchantTransactionId())
                .expiresIn(Duration.ofHours(expirationHours))
                .issuedAt(Instant.now())
                .sign();
            
            LOG.infof("Token generated successfully for order %s", order.getId());
            return token;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate token for order %s", order.getId());
            throw new RuntimeException("Token generation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate download URL with embedded JWT token.
     * 
     * Format: {baseUrl}/api/documents/download?token={jwt}
     * 
     * @param token JWT token string
     * @return Full URL for document download
     */
    public String generateDownloadUrl(String token) {
        return String.format("%s/api/documents/download?token=%s", baseUrl, token);
    }
    
    /**
     * Generate download URL directly from order (convenience method).
     * 
     * @param order The order containing document access details
     * @return Full URL for document download
     */
    public String generateDownloadUrl(Order order) {
        String token = generateAccessToken(order);
        return generateDownloadUrl(token);
    }
    
    /**
     * Validate order has all required fields for token generation.
     * 
     * @param order Order to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        if (order.getId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        
        if (order.getEmployerId() == null || order.getEmployerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Employer ID is required");
        }
        
        if (order.getEmployerEmail() == null || order.getEmployerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Employer email is required");
        }
        
        if (order.getPackageType() == null) {
            throw new IllegalArgumentException("Package type is required");
        }
        
        if (order.getCandidateIds() == null || order.getCandidateIds().isEmpty()) {
            throw new IllegalArgumentException("At least one candidate ID is required");
        }
        
        LOG.debugf("Order validation passed for order %s", order.getId());
    }
    
    /**
     * Calculate expiration timestamp for a given package type.
     * 
     * @param packageType Package type
     * @return Instant when token will expire
     */
    public Instant calculateExpiration(PackageType packageType) {
        return Instant.now().plus(Duration.ofHours(packageType.getExpirationHours()));
    }
}
