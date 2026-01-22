package solutions.envision.odoo.document.service;



import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import solutions.envision.odoo.document.DocumentAccessAudit;
import solutions.envision.odoo.document.DocumentAccessLog;
import solutions.envision.odoo.document.entity.DocumentAccessTokenEntity;
import solutions.envision.odoo.document.entity.TokenValidationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class DocumentAccessTokenService {

    @Inject
    EntityManager em;


    @ConfigProperty(name = "jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "document.access.duration.hours", defaultValue = "72")
    int accessDurationHours;

    /**
     * Generate access token for specific documents
     */
    @Transactional
    public DocumentAccessTokenEntity generateToken(
            final String employerId,
            final List<Integer> candidateIds,
            final String packageType) {

       final  Instant expiresAt = Instant.now().plus(Duration.ofHours(accessDurationHours));

        // Create JWT with claims
        final String token = Jwt.issuer("document-service")
                .subject(employerId)
                .claim("employer_id", employerId)
                .claim("candidate_ids", candidateIds)
                .claim("package_type", packageType)
                .claim("max_downloads", getMaxDownloadsForPackage(packageType))
                .expiresAt(expiresAt)
                .sign();

        // Persist access record
        final DocumentAccessTokenEntity accessToken = new DocumentAccessTokenEntity();
        accessToken.setToken(token);
        accessToken.setEmployerId(employerId);
        accessToken.setCandidateIds(candidateIds);
        accessToken.setPackageType(packageType);
        accessToken.setExpiresAt(expiresAt);
        accessToken.setCreatedAt(Instant.now());
        accessToken.setDownloadCount(0);
        accessToken.setMaxDownloads(getMaxDownloadsForPackage(packageType));

        em.persist(accessToken);

        return accessToken;
    }

    /**
     * Generate individual document access token
     */
    @Transactional
    public String generateDocumentToken(
            final String employerId,
            final Integer candidateId,
            final Integer documentId) {

        final Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

        final String token = Jwt.issuer("document-service")
                .subject(employerId)
                .claim("employer_id", employerId)
                .claim("candidate_id", candidateId)
                .claim("document_id", documentId)
                .claim("type", "single_document")
                .expiresAt(expiresAt)
                .sign();

        // Store in access log
        final DocumentAccessLog log = new DocumentAccessLog();
        log.setToken(token);
        log.setEmployerId(employerId);
        log.setCandidateId(candidateId);
        log.setDocumentId(documentId);
        log.setExpiresAt(expiresAt);
        log.setAccessed(false);

        em.persist(log);

        return token;
    }

    /**
     * Validate and consume token
     */
    @Transactional
    public TokenValidationResult validateToken(String token) {
        final DocumentAccessTokenEntity accessToken = em.createQuery(
                        "SELECT t FROM DocumentAccessToken t WHERE t.token = :token",
                        DocumentAccessTokenEntity.class)
                .setParameter("token", token)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (accessToken == null) {
            return new TokenValidationResult(false, "Token not found", null);
        }

        if (accessToken.getExpiresAt().isBefore(Instant.now())) {
            return new TokenValidationResult(false, "Token expired", null);
        }

        if (accessToken.getDownloadCount() >= accessToken.getMaxDownloads()) {
            return new TokenValidationResult(false, "Download limit exceeded", null);
        }

        // Increment download counter
        accessToken.setDownloadCount(accessToken.getDownloadCount() + 1);
        accessToken.setLastAccessedAt(Instant.now());
        em.merge(accessToken);

        return new TokenValidationResult(true, "Valid", accessToken);
    }

    /**
     * Record document access for audit
     */
    @Transactional
    public void recordAccess(String token, Integer candidateId, Integer documentId, String ipAddress) {
        final DocumentAccessAudit audit = new DocumentAccessAudit();
        audit.setToken(token);
        audit.setCandidateId(candidateId);
        audit.setDocumentId(documentId);
        audit.setAccessedAt(Instant.now());
        audit.setIpAddress(ipAddress);

        em.persist(audit);
    }

    private int getMaxDownloadsForPackage(final String packageType) {
        return switch (packageType) {
            case "basic" -> 10;
            case "standard" -> 50;
            case "premium" -> 200;
            case "unlimited" -> Integer.MAX_VALUE;
            default -> 25;
        };
    }
}

