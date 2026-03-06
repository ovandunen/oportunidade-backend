package ao.co.oportunidade.payment;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for PaymentReferenceEmulatorIT.
 * Sets appypay-api URL to point at the emulator (must be running on 8085).
 */
public class EmulatorTestProfile implements QuarkusTestProfile {

    /** WireMock port for Odoo stub (must match PaymentReferenceEmulatorIT) */
    public static final int ODOO_WIREMOCK_PORT = 9092;

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.rest-client.appypay-api.url", "http://localhost:8085/emulator/appypay",
                // Odoo stub - PaymentProcessService sends to Odoo; must succeed or transaction rolls back
                "quarkus.rest-client.odoo-api.url", "http://localhost:" + ODOO_WIREMOCK_PORT,
                "odoo.webhook.key", "test-webhook-key",
                // Run on 8080 so emulator webhook (set EMULATOR_WEBHOOK_TARGET_URL=http://localhost:8080/webhooks/appypay) reaches us.
                "quarkus.http.test-port", "8080",
                // Ensure webhook channel links producer to consumer
                "mp.messaging.outgoing.webhook-events-out.destination", "webhook-events-in"
        );
    }

    @Override
    public String getConfigProfile() {
        return "dev";
    }
}
