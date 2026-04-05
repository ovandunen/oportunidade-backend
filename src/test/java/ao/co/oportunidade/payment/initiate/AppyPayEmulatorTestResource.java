package ao.co.oportunidade.payment.initiate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Stubs the AppyPay emulator {@code POST /v1/payment-sessions} for {@link PaymentInitiateIT}.
 */
public class AppyPayEmulatorTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(AppyPayEmulatorTestResource.class);

    public static volatile WireMockServer SERVER;

    @Override
    public Map<String, String> start() {
        SERVER = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        SERVER.start();
        WireMock.configureFor("localhost", SERVER.port());
        stubSuccessfulSession();
        String base = "http://localhost:" + SERVER.port();
        LOG.infof("AppyPay WireMock at %s", base);
        return Map.of("quarkus.rest-client.appypay-emulator.url", base);
    }

    static void stubSuccessfulSession() {
        SERVER.stubFor(post(urlEqualTo("/v1/payment-sessions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentUrl\":\"https://emulator.test/checkout/session-1\"}")));
    }

    static void stubConnectionReset() {
        SERVER.stubFor(post(urlEqualTo("/v1/payment-sessions"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    @Override
    public void stop() {
        if (SERVER != null) {
            SERVER.stop();
            SERVER = null;
        }
    }
}
