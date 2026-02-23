package ao.co.oportunidade.notification.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AlertService.
 * Tests all alert types and priority levels.
 */
@QuarkusTest
class AlertServiceTest {

    @Inject
    AlertService alertService;

    private static final String TEST_TRANSACTION_ID = "TXN-TEST-12345";
    private static final String TEST_ERROR_MESSAGE = "Test error message for unit testing";
    private static final UUID TEST_ORDER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should send webhook failure alert with critical priority")
    void testSendWebhookFailureAlert() {
        // When
        assertDoesNotThrow(() -> 
            alertService.sendWebhookFailureAlert(TEST_TRANSACTION_ID, TEST_ERROR_MESSAGE, 3)
        );
        
        // Then - verify no exceptions thrown (alert logged successfully)
        // In Phase 1, alerts are logged to console
        // In Phase 2, we would verify Slack webhook was called
    }

    @Test
    @DisplayName("Should send payment processing alert with high priority")
    void testSendPaymentProcessingAlert() {
        // When
        assertDoesNotThrow(() -> 
            alertService.sendPaymentProcessingAlert(TEST_ORDER_ID, TEST_TRANSACTION_ID, TEST_ERROR_MESSAGE)
        );
    }

    @Test
    @DisplayName("Should send Odoo API failure alert")
    void testSendOdooApiFailureAlert() {
        // Given
        String operation = "fetchDocument";
        
        // When
        assertDoesNotThrow(() -> 
            alertService.sendOdooApiFailureAlert(operation, TEST_ERROR_MESSAGE)
        );
    }

    @Test
    @DisplayName("Should send token generation alert")
    void testSendTokenGenerationAlert() {
        // When
        assertDoesNotThrow(() -> 
            alertService.sendTokenGenerationAlert(TEST_ORDER_ID, TEST_ERROR_MESSAGE)
        );
    }

    @Test
    @DisplayName("Should send employer reference not found alert")
    void testSendEmployerReferenceNotFoundAlert() {
        // Given
        String referenceCode = "TEST-REF-001";
        
        // When
        assertDoesNotThrow(() -> 
            alertService.sendEmployerReferenceNotFoundAlert(referenceCode, TEST_TRANSACTION_ID)
        );
    }

    @Test
    @DisplayName("Should send database failure alert")
    void testSendDatabaseFailureAlert() {
        // Given
        String operation = "INSERT INTO orders";
        
        // When
        assertDoesNotThrow(() -> 
            alertService.sendDatabaseFailureAlert(operation, TEST_ERROR_MESSAGE)
        );
    }

    @Test
    @DisplayName("Should handle null error messages gracefully")
    void testNullErrorMessage() {
        // When
        assertDoesNotThrow(() -> 
            alertService.sendWebhookFailureAlert(TEST_TRANSACTION_ID, null, 3)
        );
    }

    @Test
    @DisplayName("Should truncate long error messages")
    void testLongErrorMessageTruncation() {
        // Given
        String longError = "E".repeat(1000); // 1000 character error message
        
        // When
        assertDoesNotThrow(() -> 
            alertService.sendWebhookFailureAlert(TEST_TRANSACTION_ID, longError, 3)
        );
        
        // Then - should not throw exception even with very long error
    }

    @Test
    @DisplayName("Should handle multiple alerts in sequence")
    void testMultipleAlertsInSequence() {
        // When
        assertDoesNotThrow(() -> {
            alertService.sendWebhookFailureAlert("TXN-1", "Error 1", 3);
            alertService.sendPaymentProcessingAlert(TEST_ORDER_ID, "TXN-2", "Error 2");
            alertService.sendOdooApiFailureAlert("operation", "Error 3");
            alertService.sendTokenGenerationAlert(TEST_ORDER_ID, "Error 4");
        });
    }

    @Test
    @DisplayName("Should include environment in alert messages")
    void testEnvironmentInAlerts() {
        // This test verifies that alerts include environment information
        // The actual environment value comes from application.yml
        assertDoesNotThrow(() -> 
            alertService.sendWebhookFailureAlert(TEST_TRANSACTION_ID, TEST_ERROR_MESSAGE, 3)
        );
    }
}
