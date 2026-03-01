package solutions.envision.odoo.document.service;

import static org.junit.jupiter.api.Assertions.*;


import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import solutions.envision.odoo.document.entity.DocumentAccessTokenEntity;
import solutions.envision.odoo.document.entity.TokenValidationResult;
import solutions.envision.odoo.document.DocumentAccessLog;
import solutions.envision.odoo.document.DocumentAccessAudit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@QuarkusTest
public class DocumentAccessTokenServiceTest {

    @Inject
    DocumentAccessTokenService service;

    @Inject
    EntityManager em;

    @BeforeEach
    @TestTransaction
    public void cleanDatabase() {
        em.createQuery("DELETE FROM DocumentAccessAudit").executeUpdate();
        em.createQuery("DELETE FROM DocumentAccessLog").executeUpdate();
        em.createQuery("DELETE FROM DocumentAccessTokenEntity").executeUpdate();
    }

    @Test
    @TestTransaction
    public void testGenerateToken_CreatesValidToken() {
        // Given
        String employerId = "EMP-001";
        List<Integer> candidateIds = List.of(1, 2, 3);
        String packageType = "standard";

        // When
        DocumentAccessTokenEntity result = service.generateToken(
                employerId,
                candidateIds,
                packageType
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getToken());
        assertEquals(employerId, result.getEmployerId());
        assertEquals(candidateIds, result.getCandidateIds());
        assertEquals(packageType, result.getPackageType());
        assertEquals(0, result.getDownloadCount());
        assertEquals(50, result.getMaxDownloads()); // standard = 50
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiresAt());

        // Verify it's persisted
        em.flush();
        em.clear();

        DocumentAccessTokenEntity persisted = em.find(
                DocumentAccessTokenEntity.class,
                result.getId()
        );
        assertNotNull(persisted);
        assertEquals(result.getToken(), persisted.getToken());
    }

    @Test
    @TestTransaction
    public void testGenerateToken_DifferentPackageTypes() {
        // Test all package types
        assertEquals(10, service.generateToken("EMP1", List.of(1), "basic")
                .getMaxDownloads());
        assertEquals(50, service.generateToken("EMP2", List.of(1), "standard")
                .getMaxDownloads());
        assertEquals(200, service.generateToken("EMP3", List.of(1), "premium")
                .getMaxDownloads());
        assertEquals(Integer.MAX_VALUE, service.generateToken("EMP4", List.of(1), "unlimited")
                .getMaxDownloads());
        assertEquals(10, service.generateToken("EMP5", List.of(1), "unknown")
                .getMaxDownloads()); // default
    }

    @Test
    @TestTransaction
    public void testGenerateDocumentToken_CreatesTokenAndLog() {
        // Given
        final String employerId = "EMP-001";
        final Integer candidateId = 123;
        final Integer documentId = 456;

        // When
        final String token = service.generateDocumentToken(
                employerId,
                candidateId,
                documentId
        );

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify log was created
        em.flush();
        List<DocumentAccessLog> logs = em.createQuery(
                        "SELECT l FROM DocumentAccessLog l WHERE l.token = :token",
                        DocumentAccessLog.class
                )
                .setParameter("token", token)
                .getResultList();

        assertEquals(1, logs.size());
        DocumentAccessLog log = logs.getFirst();
        assertEquals(employerId, log.getEmployerId());
        assertEquals(candidateId, log.getCandidateId());
        assertEquals(documentId, log.getDocumentId());
        assertFalse(log.isAccessed());
        assertNotNull(log.getExpiresAt());
    }

    @Test
    @TestTransaction
    public void testValidateToken_ValidToken_ReturnsSuccess() {
        // Given - create a valid token
        DocumentAccessTokenEntity accessToken = service.generateToken(
                "EMP-001",
                List.of(1, 2),
                "basic"
        );
        em.flush();

        // When
        TokenValidationResult result = service.validateToken(accessToken.getToken());

        // Then
        assertTrue(result.valid());
        assertEquals("Valid", result.message());
        assertNotNull(result.token());
        assertEquals(1, result.token().getDownloadCount());
        assertNotNull(result.token().getLastAccessedAt());
    }

    @Test
    @TestTransaction
    public void testValidateToken_NonExistentToken_ReturnsNotFound() {
        // When
        TokenValidationResult result = service.validateToken("non-existent-token");

        // Then
        assertFalse(result.valid());
        assertEquals("Token not found", result.message());
        assertNull(result.token());
    }

    @Test
    @TestTransaction
    public void testValidateToken_ExpiredToken_ReturnsExpired() {
        // Given - create token and manually expire it
        DocumentAccessTokenEntity accessToken = service.generateToken(
                "EMP-001",
                List.of(1),
                "basic"
        );
        accessToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        em.merge(accessToken);
        em.flush();

        // When
        TokenValidationResult result = service.validateToken(accessToken.getToken());

        // Then
        assertFalse(result.valid());
        assertEquals("Token expired", result.message());
    }

    @Test
    @TestTransaction
    public void testValidateToken_DownloadLimitExceeded_ReturnsLimitExceeded() {
        // Given - create token and exhaust download limit
        DocumentAccessTokenEntity accessToken = service.generateToken(
                "EMP-001",
                List.of(1),
                "basic"
        );
        accessToken.setDownloadCount(10); // basic = max 10
        em.merge(accessToken);
        em.flush();

        // When
        TokenValidationResult result = service.validateToken(accessToken.getToken());

        // Then
        assertFalse(result.valid());
        assertEquals("Download limit exceeded", result.message());
    }

    @Test
    @TestTransaction
    public void testValidateToken_IncrementsDownloadCount() {
        // Given
        DocumentAccessTokenEntity accessToken = service.generateToken(
                "EMP-001",
                List.of(1),
                "standard"
        );
        em.flush();
        String token = accessToken.getToken();

        // When - validate multiple times
        service.validateToken(token);
        service.validateToken(token);
        service.validateToken(token);
        em.flush();
        em.clear();

        // Then
        DocumentAccessTokenEntity updated = em.createQuery(
                        "SELECT t FROM DocumentAccessTokenEntity t WHERE t.token = :token",
                        DocumentAccessTokenEntity.class
                )
                .setParameter("token", token)
                .getSingleResult();

        assertEquals(3, updated.getDownloadCount());
    }

    @Test
    @TestTransaction
    public void testRecordAccess_CreatesAuditRecord() {
        // Given
        String token = "test-token-123";
        Integer candidateId = 100;
        Integer documentId = 200;
        String ipAddress = "192.168.1.1";

        // When
        service.recordAccess(token, candidateId, documentId, ipAddress);
        em.flush();

        // Then
        List<DocumentAccessAudit> audits = em.createQuery(
                        "SELECT a FROM DocumentAccessAudit a WHERE a.token = :token",
                        DocumentAccessAudit.class
                )
                .setParameter("token", token)
                .getResultList();

        assertEquals(1, audits.size());
        final DocumentAccessAudit audit = audits.getFirst();
        assertEquals(token, audit.getToken());
        assertEquals(candidateId, audit.getCandidateId());
        assertEquals(documentId, audit.getDocumentId());
        assertEquals(ipAddress, audit.getIpAddress());
        assertNotNull(audit.getAccessedAt());
    }

    @Test
    @TestTransaction
    public void testTokenExpirationTimeIsCorrect() {
        // Given
        Instant before = Instant.now();

        // When
        DocumentAccessTokenEntity accessToken = service.generateToken(
                "EMP-001",
                List.of(1),
                "basic"
        );

        Instant after = Instant.now();

        // Then - should expire in ~72 hours (configured duration)
        long hoursUntilExpiration = ChronoUnit.HOURS.between(
                accessToken.getCreatedAt(),
                accessToken.getExpiresAt()
        );

        assertTrue(hoursUntilExpiration >= 71 && hoursUntilExpiration <= 73,
                "Expected ~72h expiration, got " + hoursUntilExpiration + "h");
        assertTrue(accessToken.getExpiresAt().isAfter(before.plus(71, ChronoUnit.HOURS)));
        assertTrue(accessToken.getExpiresAt().isBefore(after.plus(73, ChronoUnit.HOURS)));
    }
}