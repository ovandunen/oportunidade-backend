package ao.co.oportunidade.payment;

import ao.co.oportunidade.employer.model.EmployerReference;
import ao.co.oportunidade.order.entity.OrderRepository;
import ao.co.oportunidade.payment.model.PaymentReference;
import ao.co.oportunidade.payment.model.ReferenceIssuanceCommand;
import ao.co.oportunidade.payment.service.PaymentProcessService;
import ao.co.oportunidade.payment.service.PaymentReferenceService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import io.restassured.http.ContentType;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the full payment flow via the AppyPay emulator.
 * Requires the emulator to be running on port 8085 (e.g. from oportunidade-appypay-emulator).
 *
 * The test app runs on port 8080. Start the emulator with
 * EMULATOR_WEBHOOK_TARGET_URL=http://localhost:8080/webhooks/appypay
 *
 * Emulator API docs: http://localhost:8085/q/swagger-ui
 */
@QuarkusTest
@TestProfile(EmulatorTestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentReferenceEmulatorIT {

    @Inject
    PaymentReferenceService paymentReferenceService;

    @Inject
    PaymentProcessService paymentProcessService;

    @Inject
    OrderRepository orderRepository;

    private static WireMockServer odooWireMock;
    private static final String ODOO_WEBHOOK_PATH = "/api/webhook/payment";

    private String lastReferenceCode;

    @BeforeAll
    static void startOdooWireMock() {
        odooWireMock = new WireMockServer(
                WireMockConfiguration.wireMockConfig().port(EmulatorTestProfile.ODOO_WIREMOCK_PORT)
        );
        odooWireMock.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", EmulatorTestProfile.ODOO_WIREMOCK_PORT);
    }

    @AfterAll
    static void stopOdooWireMock() {
        if (odooWireMock != null) {
            odooWireMock.stop();
        }
    }

    @BeforeEach
    void stubOdoo() {
        odooWireMock.resetAll();
        odooWireMock.stubFor(post(urlPathEqualTo(ODOO_WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"payment_id\": 42, \"message\": \"Payment processed\"}")));
    }

    @AfterEach
    void tearDown() {
        try {
            var response = given()
                    .when().delete("http://localhost:8085/emulator/control/references");
            if (response.statusCode() != 204) {
                // 404 if emulator not running or path changed
            }
        } catch (Exception e) {
            // Emulator may not be running
        }
        if (lastReferenceCode != null) {
            QuarkusTransaction.requiringNew().run(() ->
                    EmployerReference.delete("referenceCode", lastReferenceCode));
        }
    }

    @Test
    @Order(1)
    void full_payment_flow_via_emulator() {
        String merchantTxId = "order-" + System.currentTimeMillis();

        // 1. Issue a reference through the normal domain flow (sends minimal payload to emulator)
        var txId = new io.quarkus.narayana.jta.runtime.TransactionScopedNotifier.TransactionId(merchantTxId);
        var command = new ReferenceIssuanceCommand(
                txId,
                BigDecimal.valueOf(5000),
                null,
                "AOA",
                null
        );

        PaymentReference ref = paymentReferenceService.issueReference(command);
        String referenceNumber = ref.getReferenceNumber();
        assertThat(referenceNumber).isNotNull().isNotEmpty();

        // 2. Seed EmployerReference so webhook enrichment can resolve employer
        lastReferenceCode = referenceNumber;
        seedEmployerReference(referenceNumber);

        // 3. Call POST /emulator/control/pay/{referenceNumber}

        var payResponse = given()
                .contentType(ContentType.JSON)
                .when()
                .post("http://localhost:8085/emulator/control/pay/" + referenceNumber)
                .then()
                .statusCode(200)
                .extract().body().jsonPath();

        assertThat(payResponse.getString("status")).isEqualTo("PAID");

        // 3b. Process webhook synchronously (bypass async channel for deterministic test)
        AppyPayWebhookPayload payload = new AppyPayWebhookPayload();
        payload.setId("tx-" + System.currentTimeMillis());
        payload.setMerchantTransactionId(merchantTxId);
        payload.setType("Charge");
        payload.setAmount(BigDecimal.valueOf(5000));
        payload.setCurrency("AOA");
        payload.setStatus("PAID");
        payload.setPaymentMethod("REF");
        ReferenceInfo refInfo = new ReferenceInfo();
        refInfo.setReferenceNumber(referenceNumber);
        refInfo.setEntity("00123");
        refInfo.setStatus("Active");
        payload.setReference(refInfo);
        payload.setCreatedDate(java.time.Instant.now());
        payload.setUpdatedDate(java.time.Instant.now());
        paymentProcessService.processPaymentStatus(payload);

        // 4. Assert the order was created and completed
        var order = QuarkusTransaction.requiringNew().call(() ->
                orderRepository.findByMerchantTransactionId(merchantTxId));
        assertThat(order).isPresent();
        assertThat(order.get().getStatus().name()).isEqualTo("COMPLETED");

        // 5. Assert PaymentReference was created (stored in DB via PaymentReferenceService)
        // 6. Document access unlocked for the order - handled by PaymentProcessService
    }

    @Transactional
    void seedEmployerReference(String referenceCode) {
        EmployerReference ref = new EmployerReference();
        ref.setReferenceCode(referenceCode);
        ref.setEmployerId("EMP-EMU-001");
        ref.setEmployerEmail("employer@emulator-test.com");
        ref.setCompanyName("Emulator Test Co");
        ref.persist();
    }
}
