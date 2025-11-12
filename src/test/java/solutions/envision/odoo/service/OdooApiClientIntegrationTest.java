package solutions.envision.odoo.service;

import solutions.envision.odoo.dto.OdooPaymentRequest;
import solutions.envision.odoo.dto.OdooWebhookResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDate;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;


@QuarkusTest
@TestProfile(OdooApiClientIntegrationTest.OdooTestProfile.class)
public class OdooApiClientIntegrationTest {

    private static WireMockServer wireMockServer;

    @Inject
    @RestClient
    OdooApiClient odooApiClient;

    private static final String VALID_WEBHOOK_KEY = "test-webhook-key-12345";
    private static final String WEBHOOK_PATH = "/api/webhook/payment";

    public static class OdooTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.rest-client.odoo-api.url", "http://localhost:8088",
                    "odoo.webhook.key", VALID_WEBHOOK_KEY
            );
        }
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8088);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8088);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("Integration: Should successfully send payment to Odoo")
    void testSendPayment_Integration_Success() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .withHeader("X-Odoo-Webhook-Key", equalTo(VALID_WEBHOOK_KEY))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                        "success": true,
                        "message": "Payment processed successfully",
                        "transactionId": "TXN-123456",
                        "odooOrderId": "SO-001"
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act
        OdooWebhookResponse response = odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("Payment processed successfully", response.getMessage());

        // Verify the request was made
        verify(postRequestedFor(urlEqualTo(WEBHOOK_PATH))
                .withHeader("X-Odoo-Webhook-Key", equalTo(VALID_WEBHOOK_KEY)));
    }

    @Test
    @DisplayName("Integration: Should handle 401 Unauthorized with invalid key")
    void testSendPayment_Integration_Unauthorized() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .withHeader("X-Odoo-Webhook-Key", equalTo("invalid-key"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("""
                    {
                        "error": "Unauthorized",
                        "message": "Invalid webhook key"
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act & Assert
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment("invalid-key", request.getPayment());
        });

        assertEquals(401, exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("Integration: Should handle 400 Bad Request for invalid data")
    void testSendPayment_Integration_BadRequest() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("""
                    {
                        "error": "Bad Request",
                        "message": "Missing required field: amount"
                    }
                    """)));

        OdooPaymentRequest request = new OdooPaymentRequest();
        // Missing required fields

        // Act & Assert
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());
        });

        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("Integration: Should handle 500 Internal Server Error")
    void testSendPayment_Integration_ServerError() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("""
                    {
                        "error": "Internal Server Error",
                        "message": "Database connection failed"
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act & Assert
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());
        });

        assertEquals(500, exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("Integration: Should handle timeout")
    void testSendPayment_Integration_Timeout() {
        // Arrange - simulate a slow response
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000) // 5 second delay
                        .withBody("""
                    {
                        "success": true
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act & Assert
        // This should timeout based on your client configuration
        assertThrows(jakarta.ws.rs.ProcessingException.class, () -> {
            odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());
        });
    }

    @Test
    @DisplayName("Integration: Should handle failed payment response")
    void testSendPayment_Integration_PaymentFailed() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                        "success": false,
                        "message": "Payment declined",
                        "errorCode": "INSUFFICIENT_FUNDS",
                        "transactionId": null
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act
        final OdooWebhookResponse response = odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());

        // Assert
        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals("Payment declined", response.getMessage());
        //assertEquals("INSUFFICIENT_FUNDS", response.getError());
    }

    @Test
    @DisplayName("Integration: Should verify request body is sent correctly")
    void testSendPayment_Integration_VerifyRequestBody() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                        "success": true,
                        "transactionId": "TXN-999"
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act
        odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());

        // Assert - verify the request body was sent correctly
        verify(postRequestedFor(urlEqualTo(WEBHOOK_PATH))
                .withRequestBody(containing("1500.00"))
                .withRequestBody(containing("PAY-REF-001"))
                .withRequestBody(containing("inbound")));
    }

    @Test
    @DisplayName("Integration: Should handle rate limiting (429)")
    void testSendPayment_Integration_RateLimited() {
        // Arrange
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "60")
                        .withBody("""
                    {
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded. Try again in 60 seconds."
                    }
                    """)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act & Assert
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());
        });

        assertEquals(429, exception.getResponse().getStatus());
    }


    @Test
    @DisplayName("Integration: Should handle connection failures")
    void testSendPayment_Integration_ConnectionFailure() {
        // Simulate various connection failures
        stubFor(post(urlEqualTo(WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        OdooPaymentRequest request = createValidPaymentRequest();

        // Act & Assert
        assertThrows(ProcessingException.class, () -> {
            odooApiClient.sendPayment(VALID_WEBHOOK_KEY, request.getPayment());
        });
    }


    private OdooPaymentRequest createValidPaymentRequest() {
        OdooPaymentRequest.PaymentData paymentData = new OdooPaymentRequest.PaymentData();
        paymentData.setAmount("1500.00");
        paymentData.setCurrencyId(1); // AOA currency ID
        paymentData.setPartnerId(123); // Customer/Partner ID
        paymentData.setPaymentReference("PAY-REF-001");
        paymentData.setPaymentDate(LocalDate.parse("2025-10-28"));
        paymentData.setPaymentType("inbound");
        paymentData.setPaymentMethodId(1);
        paymentData.setCommunication("Payment for order ORD-001");

        return new OdooPaymentRequest(paymentData);
    }
}
