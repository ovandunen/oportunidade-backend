package ao.co.oportunidade.document.service;

import ao.co.oportunidade.document.entity.DocumentAccessAuditLog;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentAccessAuditLogService.
 * Tests CRUD operations and query methods.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentAccessLogServiceTest {

    @Inject
    DocumentAccessLogService logService;

    private static UUID testOrderId;
    private static final String TEST_EMPLOYER_ID = "EMP-TEST-001";
    private static final String TEST_EMPLOYER_EMAIL = "test@employer.com";
    private static final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
    private static final String TEST_CANDIDATE_ID = "CAND-001";
    private static final String TEST_DOCUMENT_ID = "DOC-001";
    private static final String TEST_DOCUMENT_NAME = "Resume.pdf";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Test Browser)";

    @BeforeEach
    @Transactional
    void setup() {
        testOrderId = UUID.randomUUID();
        // Clean up test data
        DocumentAccessAuditLog.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should log successful document access")
    void testLogSuccessfulAccess() {
        // When
        DocumentAccessAuditLog log = logService.logSuccessfulAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                TEST_EMPLOYER_EMAIL,
                TEST_TOKEN,
                DocumentAccessAuditLog.TokenType.MULTI_USE,
                TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID,
                TEST_DOCUMENT_NAME,
                TEST_IP_ADDRESS,
                TEST_USER_AGENT,
                1024L
        );

        // Then
        assertNotNull(log);
        assertNotNull(log.getId());
        assertEquals(testOrderId, log.getOrderId());
        assertEquals(TEST_EMPLOYER_ID, log.getEmployerId());
        assertEquals(TEST_EMPLOYER_EMAIL, log.getEmployerEmail());
        assertEquals(TEST_CANDIDATE_ID, log.getCandidateId());
        assertEquals(TEST_DOCUMENT_ID, log.getDocumentId());
        assertEquals(TEST_DOCUMENT_NAME, log.getDocumentName());
        assertEquals(TEST_IP_ADDRESS, log.getIpAddress());
        assertEquals(DocumentAccessAuditLog.TokenType.MULTI_USE, log.getTokenType());
        assertTrue(log.getSuccess());
        assertEquals(200, log.getHttpStatusCode());
        assertEquals(1024L, log.getDocumentSizeBytes());
        assertNotNull(log.getAccessedAt());
    }

    @Test
    @Order(2)
    @DisplayName("Should log failed document access")
    void testLogFailedAccess() {
        // When
        DocumentAccessAuditLog log = logService.logFailedAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                TEST_EMPLOYER_EMAIL,
                TEST_TOKEN,
                DocumentAccessAuditLog.TokenType.MULTI_USE,
                TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID,
                TEST_IP_ADDRESS,
                TEST_USER_AGENT,
                403,
                "Access denied - invalid token"
        );

        // Then
        assertNotNull(log);
        assertFalse(log.getSuccess());
        assertEquals(403, log.getHttpStatusCode());
        assertEquals("Access denied - invalid token", log.getErrorMessage());
        assertNull(log.getDocumentSizeBytes());
    }

    @Test
    @Order(3)
    @DisplayName("Should truncate very long tokens")
    void testTokenTruncation() {
        // Given
        String veryLongToken = "a".repeat(500);

        // When
        DocumentAccessAuditLog log = logService.logSuccessfulAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                TEST_EMPLOYER_EMAIL,
                veryLongToken,
                DocumentAccessAuditLog.TokenType.MULTI_USE,
                TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID,
                TEST_DOCUMENT_NAME,
                TEST_IP_ADDRESS,
                TEST_USER_AGENT,
                1024L
        );

        // Then
        assertNotNull(log.getTokenUsed());
        assertTrue(log.getTokenUsed().length() <= 150); // Truncated to ~100 chars
    }

    @Test
    @Order(4)
    @DisplayName("Should truncate very long user agents")
    void testUserAgentTruncation() {
        // Given
        String veryLongUserAgent = "Mozilla/5.0 " + "x".repeat(1500);

        // When
        DocumentAccessAuditLog log = logService.logSuccessfulAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                TEST_EMPLOYER_EMAIL,
                TEST_TOKEN,
                DocumentAccessAuditLog.TokenType.MULTI_USE,
                TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID,
                TEST_DOCUMENT_NAME,
                TEST_IP_ADDRESS,
                veryLongUserAgent,
                1024L
        );

        // Then
        assertNotNull(log.getUserAgent());
        assertTrue(log.getUserAgent().length() <= 1000);
    }

    @Test
    @Order(5)
    @DisplayName("Should truncate very long error messages")
    void testErrorMessageTruncation() {
        // Given
        String veryLongError = "Error: " + "x".repeat(1500);

        // When
        DocumentAccessAuditLog log = logService.logFailedAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                TEST_EMPLOYER_EMAIL,
                TEST_TOKEN,
                DocumentAccessAuditLog.TokenType.MULTI_USE,
                TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID,
                TEST_IP_ADDRESS,
                TEST_USER_AGENT,
                500,
                veryLongError
        );

        // Then
        assertNotNull(log.getErrorMessage());
        assertTrue(log.getErrorMessage().length() <= 1000);
    }

    @Test
    @Order(6)
    @DisplayName("Should retrieve access logs by order ID")
    void testGetAccessLogsForOrder() {
        // Given
        logService.logSuccessfulAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID, TEST_DOCUMENT_NAME, TEST_IP_ADDRESS, TEST_USER_AGENT, 1024L);

        logService.logSuccessfulAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, "CAND-002",
                "DOC-002", "CV.pdf", TEST_IP_ADDRESS, TEST_USER_AGENT, 2048L);

        // When
        List<DocumentAccessAuditLog> logs = logService.getAccessLogsForOrder(testOrderId);

        // Then
        assertNotNull(logs);
        assertEquals(2, logs.size());
    }

    @Test
    @Order(7)
    @DisplayName("Should count successful downloads for an order")
    void testCountSuccessfulDownloads() {
        // Given
        logService.logSuccessfulAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID, TEST_DOCUMENT_NAME, TEST_IP_ADDRESS, TEST_USER_AGENT, 1024L);

        logService.logFailedAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, "CAND-002",
                "DOC-002", TEST_IP_ADDRESS, TEST_USER_AGENT, 403, "Access denied");

        // When
        long successfulCount = logService.countSuccessfulDownloads(testOrderId);

        // Then
        assertEquals(1, successfulCount);
    }

    @Test
    @Order(8)
    @DisplayName("Should retrieve access logs by employer ID")
    void testGetAccessLogsForEmployer() {
        // Given
        logService.logSuccessfulAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID, TEST_DOCUMENT_NAME, TEST_IP_ADDRESS, TEST_USER_AGENT, 1024L);

        // When
        List<DocumentAccessAuditLog> logs = logService.getAccessLogsForEmployer(TEST_EMPLOYER_ID);

        // Then
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().allMatch(log -> TEST_EMPLOYER_ID.equals(log.getEmployerId())));
    }

    @Test
    @Order(9)
    @DisplayName("Should retrieve failed access attempts")
    void testGetFailedAccessAttempts() {
        // Given
        logService.logSuccessfulAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID, TEST_DOCUMENT_NAME, TEST_IP_ADDRESS, TEST_USER_AGENT, 1024L);

        logService.logFailedAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, "CAND-002",
                "DOC-002", TEST_IP_ADDRESS, TEST_USER_AGENT, 403, "Access denied");

        // When
        List<DocumentAccessAuditLog> failedLogs = logService.getFailedAccessAttempts();

        // Then
        assertNotNull(failedLogs);
        assertFalse(failedLogs.isEmpty());
        assertTrue(failedLogs.stream().allMatch(log -> !log.getSuccess()));
    }

    @Test
    @Order(10)
    @DisplayName("Should retrieve access logs by IP address")
    void testGetAccessLogsByIp() {
        // Given
        logService.logSuccessfulAccess(testOrderId, TEST_EMPLOYER_ID, TEST_EMPLOYER_EMAIL,
                TEST_TOKEN, DocumentAccessAuditLog.TokenType.MULTI_USE, TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID, TEST_DOCUMENT_NAME, TEST_IP_ADDRESS, TEST_USER_AGENT, 1024L);

        // When
        List<DocumentAccessAuditLog> logs = logService.getAccessLogsByIp(TEST_IP_ADDRESS);

        // Then
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().allMatch(log -> TEST_IP_ADDRESS.equals(log.getIpAddress())));
    }

    @Test
    @Order(11)
    @DisplayName("Should handle null values gracefully")
    void testNullValues() {
        // When
        DocumentAccessAuditLog log = logService.logSuccessfulAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                null, // null email
                TEST_TOKEN,
                DocumentAccessAuditLog.TokenType.MULTI_USE,
                null, // null candidate
                null, // null document
                null, // null document name
                null, // null IP
                null, // null user agent
                null  // null size
        );

        // Then
        assertNotNull(log);
        assertTrue(log.getSuccess());
    }

    @Test
    @Order(12)
    @DisplayName("Should log single-use token type")
    void testSingleUseTokenType() {
        // When
        DocumentAccessAuditLog log = logService.logSuccessfulAccess(
                testOrderId,
                TEST_EMPLOYER_ID,
                TEST_EMPLOYER_EMAIL,
                "single-use-token-123",
                DocumentAccessAuditLog.TokenType.SINGLE_USE,
                TEST_CANDIDATE_ID,
                TEST_DOCUMENT_ID,
                TEST_DOCUMENT_NAME,
                TEST_IP_ADDRESS,
                TEST_USER_AGENT,
                1024L
        );

        // Then
        assertNotNull(log);
        assertEquals(DocumentAccessAuditLog.TokenType.SINGLE_USE, log.getTokenType());
    }
}
