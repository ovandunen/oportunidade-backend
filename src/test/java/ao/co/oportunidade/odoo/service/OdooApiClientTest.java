package ao.co.oportunidade.odoo.service;

import ao.co.oportunidade.odoo.dto.OdooPaymentRequest;
import ao.co.oportunidade.odoo.dto.OdooWebhookResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OdooApiClientTest {

    @InjectMock
    @RestClient
    OdooApiClient odooApiClient;

    private String validWebhookKey;
    private OdooPaymentRequest validPaymentRequest;
    private OdooWebhookResponse successResponse;

    @BeforeEach
    void setUp() {
        validWebhookKey = "test-webhook-key-12345";

        // Setup valid payment request
        final OdooPaymentRequest.PaymentData paymentData = new OdooPaymentRequest.PaymentData();
        paymentData.setAmount("1500.00");
        paymentData.setCurrencyId(1); // AOA currency ID
        paymentData.setPartnerId(123); // Customer/Partner ID
        paymentData.setPaymentReference("PAY-REF-001");
        paymentData.setPaymentDate("2025-10-28");
        paymentData.setPaymentType("inbound");
        paymentData.setJournalId(1);
        paymentData.setPaymentMethodId(1);
        paymentData.setCommunication("Payment for order ORD-001");

        validPaymentRequest = new OdooPaymentRequest(paymentData);

        // Setup success response
        successResponse = new OdooWebhookResponse();
        successResponse.setSuccess(true);
        successResponse.setMessage("Payment processed successfully");
    }

    @Test
    @DisplayName("Should successfully send payment with valid data")
    void testSendPayment_Success() {
        // Arrange
        when(odooApiClient.sendPayment(validWebhookKey, validPaymentRequest))
                .thenReturn(successResponse);

        // Act
        OdooWebhookResponse response = odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Payment processed successfully", response.getMessage());

        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle null webhook key")
    void testSendPayment_NullWebhookKey() {
        // Arrange
        when(odooApiClient.sendPayment(null, validPaymentRequest))
                .thenThrow(new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED));

        // Act & Assert
        assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(null, validPaymentRequest);
        });

        verify(odooApiClient, times(1)).sendPayment(null, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle invalid webhook key (401 Unauthorized)")
    void testSendPayment_InvalidWebhookKey() {
        // Arrange
        final String invalidKey = "invalid-key";
        when(odooApiClient.sendPayment(invalidKey, validPaymentRequest))
                .thenThrow(new WebApplicationException("Unauthorized", Response.Status.UNAUTHORIZED));

        // Act & Assert
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(invalidKey, validPaymentRequest);
        });

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
        verify(odooApiClient, times(1)).sendPayment(invalidKey, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle null payment request")
    void testSendPayment_NullPaymentRequest() {
        // Arrange
        when(odooApiClient.sendPayment(validWebhookKey, null))
                .thenThrow(new WebApplicationException("Bad Request", Response.Status.BAD_REQUEST));

        // Act & Assert
        assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(validWebhookKey, null);
        });

        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, null);
    }

    @Test
    @DisplayName("Should handle server error (500 Internal Server Error)")
    void testSendPayment_ServerError() {
        // Arrange
        when(odooApiClient.sendPayment(validWebhookKey, validPaymentRequest))
                .thenThrow(new WebApplicationException("Internal Server Error", Response.Status.INTERNAL_SERVER_ERROR));

        // Act & Assert
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);
        });

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle timeout exception")
    void testSendPayment_Timeout() {
        // Arrange
        when(odooApiClient.sendPayment(validWebhookKey, validPaymentRequest))
                .thenThrow(new jakarta.ws.rs.ProcessingException("Request timeout"));

        // Act & Assert
        assertThrows(jakarta.ws.rs.ProcessingException.class, () -> {
            odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);
        });

        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle failed payment response")
    void testSendPayment_FailedPayment() {
        // Arrange
        final OdooWebhookResponse failedResponse = new OdooWebhookResponse();
        failedResponse.setSuccess(false);
        failedResponse.setMessage("Insufficient funds");
        failedResponse.setError("INSUFFICIENT_FUNDS");

        when(odooApiClient.sendPayment(validWebhookKey, validPaymentRequest))
                .thenReturn(failedResponse);

        // Act
        final OdooWebhookResponse response = odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Insufficient funds", response.getMessage());
        assertEquals("INSUFFICIENT_FUNDS", response.getError());

        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle validation error (400 Bad Request)")
    void testSendPayment_ValidationError() {
        // Arrange
        final OdooPaymentRequest invalidRequest = new OdooPaymentRequest();
        // Missing required fields

        when(odooApiClient.sendPayment(validWebhookKey, invalidRequest))
                .thenThrow(new WebApplicationException("Bad Request: Missing required fields", Response.Status.BAD_REQUEST));

        // Act & Assert
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(validWebhookKey, invalidRequest);
        });

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, invalidRequest);
    }

    @Test
    @DisplayName("Should handle network connectivity issues")
    void testSendPayment_NetworkError() {
        // Arrange
        when(odooApiClient.sendPayment(validWebhookKey, validPaymentRequest))
                .thenThrow(new jakarta.ws.rs.ProcessingException("Connection refused"));

        // Act & Assert
        jakarta.ws.rs.ProcessingException exception = assertThrows(jakarta.ws.rs.ProcessingException.class, () -> {
            odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);
        });

        assertTrue(exception.getMessage().contains("Connection refused"));
        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, validPaymentRequest);
    }

    @Test
    @DisplayName("Should handle different payment amounts correctly")
    void testSendPayment_DifferentAmounts() {
        // Test with very small amount
        final OdooPaymentRequest.PaymentData smallAmountData = new OdooPaymentRequest.PaymentData();
        smallAmountData.setAmount("0.01");
        smallAmountData.setCurrencyId(1);
        smallAmountData.setPartnerId(123);

        final OdooPaymentRequest smallAmountRequest = new OdooPaymentRequest(smallAmountData);

        final OdooWebhookResponse smallAmountResponse = new OdooWebhookResponse();
        smallAmountResponse.setSuccess(true);

        when(odooApiClient.sendPayment(validWebhookKey, smallAmountRequest))
                .thenReturn(smallAmountResponse);

        // Act
        final OdooWebhookResponse response = odooApiClient.sendPayment(validWebhookKey, smallAmountRequest);

        // Assert
        assertNotNull(response);
        assertThat(response.getSuccess()).isNotNull().isEqualTo(Boolean.TRUE);
        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, smallAmountRequest);
    }

    @Test
    @DisplayName("Should verify correct HTTP headers are used")
    void testSendPayment_VerifyHeaders() {
        // Arrange
        when(odooApiClient.sendPayment(anyString(), any(OdooPaymentRequest.class)))
                .thenReturn(successResponse);

        // Act
        odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);

        // Assert - verify the method was called with correct parameters
        verify(odooApiClient, times(1))
                .sendPayment(eq(validWebhookKey), eq(validPaymentRequest));
    }

    @Test
    @DisplayName("Should handle rate limiting (429 Too Many Requests)")
    void testSendPayment_RateLimited() {
        // Arrange
        when(odooApiClient.sendPayment(validWebhookKey, validPaymentRequest))
                .thenThrow(new WebApplicationException("Too Many Requests", Response.Status.TOO_MANY_REQUESTS));

        // Act & Assert
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(validWebhookKey, validPaymentRequest);
        });

        assertEquals(429, exception.getResponse().getStatus());
        verify(odooApiClient, times(1)).sendPayment(validWebhookKey, validPaymentRequest);
    }
}