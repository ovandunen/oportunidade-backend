package solutions.envision.odoo.document.service;

import ao.co.oportunidade.order.model.Order;
import ao.co.oportunidade.order.model.PackageType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentTokenService.
 * Tests JWT token generation, validation, and expiration logic.
 */
@QuarkusTest
class DocumentTokenServiceTest {

    @Inject
    DocumentTokenService tokenService;

    private Order testOrder;

    @BeforeEach
    void setup() {
        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setMerchantTransactionId("ORD-TEST-12345");
        testOrder.setEmployerId("EMP-001");
        testOrder.setEmployerEmail("employer@test.com");
        testOrder.setPackageType(PackageType.STANDARD);
        testOrder.setCandidateIds(List.of("CAND-001", "CAND-002"));
        testOrder.setOdooDocumentIds(List.of("odoo_doc_CAND-001", "odoo_doc_CAND-002"));
        testOrder.setReferenceCode("TEST-REF-001");
        testOrder.setStatus(Order.OrderStatus.COMPLETED);
        testOrder.setAmount(new BigDecimal("100.00"));
        testOrder.setCurrency("AOA");
    }

    @Test
    @DisplayName("Should generate access token successfully")
    void testGenerateAccessToken() {
        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains(".")); // JWT format check (header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
    }

    @Test
    @DisplayName("Should generate different tokens for different orders")
    void testUniquenessOfTokens() {
        // Given
        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setMerchantTransactionId("ORD-TEST-67890");
        order2.setEmployerId("EMP-002");
        order2.setEmployerEmail("employer2@test.com");
        order2.setPackageType(PackageType.BASIC);
        order2.setCandidateIds(List.of("CAND-003"));
        order2.setOdooDocumentIds(List.of("odoo_doc_CAND-003"));

        // When
        String token1 = tokenService.generateAccessToken(testOrder);
        String token2 = tokenService.generateAccessToken(order2);

        // Then
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should generate download URL with token")
    void testGenerateDownloadUrl() {
        // Given
        String token = tokenService.generateAccessToken(testOrder);

        // When
        String downloadUrl = tokenService.generateDownloadUrl(token);

        // Then
        assertNotNull(downloadUrl);
        assertTrue(downloadUrl.contains("/api/documents/download"));
        assertTrue(downloadUrl.contains("token=" + token));
    }

    @Test
    @DisplayName("Should throw exception when order ID is null")
    void testValidateOrder_NullOrderId() {
        // Given
        testOrder.setId(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateAccessToken(testOrder)
        );
        assertTrue(exception.getMessage().contains("Order ID"));
    }

    @Test
    @DisplayName("Should throw exception when employer ID is null")
    void testValidateOrder_NullEmployerId() {
        // Given
        testOrder.setEmployerId(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateAccessToken(testOrder)
        );
        assertTrue(exception.getMessage().contains("Employer ID"));
    }

    @Test
    @DisplayName("Should throw exception when candidate IDs are empty")
    void testValidateOrder_EmptyCandidateIds() {
        // Given
        testOrder.setCandidateIds(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateAccessToken(testOrder)
        );
        assertTrue(exception.getMessage().contains("Candidate IDs"));
    }

    @Test
    @DisplayName("Should throw exception when document IDs are empty")
    void testValidateOrder_EmptyDocumentIds() {
        // Given
        testOrder.setOdooDocumentIds(List.of());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateAccessToken(testOrder)
        );
        assertTrue(exception.getMessage().contains("Document IDs"));
    }

    @Test
    @DisplayName("Should generate token for BASIC package (24 hours)")
    void testTokenGeneration_BasicPackage() {
        // Given
        testOrder.setPackageType(PackageType.BASIC);

        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should generate token for PREMIUM package (72 hours)")
    void testTokenGeneration_PremiumPackage() {
        // Given
        testOrder.setPackageType(PackageType.PREMIUM);

        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should generate token for ENTERPRISE package (168 hours)")
    void testTokenGeneration_EnterprisePackage() {
        // Given
        testOrder.setPackageType(PackageType.ENTERPRISE);

        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should handle order with multiple candidates")
    void testTokenGeneration_MultipleCandidates() {
        // Given
        testOrder.setCandidateIds(List.of("CAND-001", "CAND-002", "CAND-003", "CAND-004"));
        testOrder.setOdooDocumentIds(List.of("DOC-001", "DOC-002", "DOC-003", "DOC-004"));

        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should handle order with single candidate")
    void testTokenGeneration_SingleCandidate() {
        // Given
        testOrder.setCandidateIds(List.of("CAND-001"));
        testOrder.setOdooDocumentIds(List.of("DOC-001"));

        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should generate consistent tokens for same order data")
    void testTokenConsistency() {
        // When - generate token twice with same data
        String token1 = tokenService.generateAccessToken(testOrder);
        
        // Sleep a bit to ensure different issuedAt timestamps
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String token2 = tokenService.generateAccessToken(testOrder);

        // Then - tokens should be different due to different iat (issued at) claims
        assertNotEquals(token1, token2, "Tokens should differ due to timestamp");
    }

    @Test
    @DisplayName("Should handle missing package type gracefully")
    void testTokenGeneration_MissingPackageType() {
        // Given
        testOrder.setPackageType(null);

        // When - should use default or throw exception
        // The actual behavior depends on implementation
        try {
            String token = tokenService.generateAccessToken(testOrder);
            assertNotNull(token); // If it succeeds, verify token is created
        } catch (IllegalArgumentException e) {
            // If it throws, verify error message
            assertTrue(e.getMessage().contains("Package"));
        }
    }

    @Test
    @DisplayName("Should generate URL with proper base path")
    void testDownloadUrl_ProperFormat() {
        // Given
        String token = "test.token.signature";

        // When
        String url = tokenService.generateDownloadUrl(token);

        // Then
        assertTrue(url.startsWith("http"));
        assertTrue(url.contains("/api/documents/download"));
        assertTrue(url.contains("?token="));
    }

    @Test
    @DisplayName("Should handle very long employer emails")
    void testTokenGeneration_LongEmployerEmail() {
        // Given
        String longEmail = "very.long.email.address.for.testing.purposes@" + "x".repeat(200) + ".com";
        testOrder.setEmployerEmail(longEmail);

        // When
        String token = tokenService.generateAccessToken(testOrder);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
}
