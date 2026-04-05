package ao.co.oportunidade.payment.initiate;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@QuarkusTestResource(value = AppyPayEmulatorTestResource.class, restrictToAnnotatedClass = true)
class PaymentInitiateIT {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void resetWireMock() {
        WireMockServer s = AppyPayEmulatorTestResource.SERVER;
        if (s != null) {
            s.resetAll();
            AppyPayEmulatorTestResource.stubSuccessfulSession();
        }
    }

    @AfterEach
    void flushContext() {
        entityManager.clear();
    }

    @Test
    @TestSecurity(user = "user-alice", roles = {"user"})
    void initiate_happyPath_persistsOrder() {
        String body = """
                {"resourceId":"job-123","resourceType":"JOB_DOCUMENT"}
                """;
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .statusCode(200)
                .body("merchantTransactionId", startsWith("txn-"))
                .body("paymentUrl", equalTo("https://emulator.test/checkout/session-1"));

        long rows = QuarkusTransaction.requiringNew()
                .call(() -> entityManager.createQuery(
                                "select count(o) from PaymentInitiationOrder o where o.userId = :u and o.resourceId = :r",
                                Long.class)
                        .setParameter("u", "user-alice")
                        .setParameter("r", "job-123")
                        .getSingleResult());
        assertThat(rows).isEqualTo(1L);
    }

    @Test
    @TestSecurity(user = "user-bob", roles = {"user"})
    void initiate_duplicatePending_returnsSameTransaction() {
        String body = """
                {"resourceId":"job-123","resourceType":"JOB_DOCUMENT"}
                """;
        String first = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .statusCode(200)
                .extract()
                .path("merchantTransactionId");

        String second = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .statusCode(200)
                .extract()
                .path("merchantTransactionId");

        assertThat(second).isEqualTo(first);
    }

    @Test
    @TestSecurity(user = "user-appypay-down", roles = {"user"})
    void initiate_appypayUnreachable_returns502() {
        WireMockServer s = AppyPayEmulatorTestResource.SERVER;
        s.resetAll();
        AppyPayEmulatorTestResource.stubConnectionReset();

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"resourceId":"job-123","resourceType":"JOB_DOCUMENT"}
                        """)
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .statusCode(502);
    }

    @Test
    @TestSecurity(user = "buyer-b", roles = {"user"})
    void initiate_exclusiveResourceConflict_returns409() {
        String exclusiveResourceId = "job-exclusive-" + UUID.randomUUID();
        QuarkusTransaction.requiringNew().run(() -> {
            BillableResource br = new BillableResource();
            br.resourceId = exclusiveResourceId;
            br.resourceType = PaymentResourceType.JOB_OFFER;
            br.singleBuyer = true;
            entityManager.persist(br);

            PaymentInitiationOrder paid = new PaymentInitiationOrder();
            paid.id = UUID.randomUUID();
            paid.userId = "buyer-a";
            paid.resourceId = exclusiveResourceId;
            paid.resourceType = PaymentResourceType.JOB_OFFER;
            paid.merchantTransactionId = "txn-seed-" + UUID.randomUUID();
            paid.paymentUrl = "https://emulator.test/done";
            paid.status = OrderStatus.PAID;
            paid.paymentSessionExpiresAt = Instant.now().plusSeconds(3600);
            paid.createdAt = Instant.now();
            paid.updatedAt = Instant.now();
            entityManager.persist(paid);
        });

        given()
                .contentType(ContentType.JSON)
                .body(String.format(
                        "{\"resourceId\":\"%s\",\"resourceType\":\"JOB_OFFER\"}", exclusiveResourceId))
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "user-c", roles = {"user"})
    void initiate_unknownResource_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"resourceId":"unknown-job","resourceType":"JOB_DOCUMENT"}
                        """)
                .when()
                .post("/api/v1/payments/initiate")
                .then()
                .statusCode(400);
    }
}
