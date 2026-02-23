package ao.co.oportunidade.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock test resource for mocking external services (Odoo API, Slack, etc.).
 * Automatically starts WireMock server before tests and stops it after.
 * 
 * Usage:
 * <pre>
 * {@code
 * @QuarkusTest
 * @QuarkusTestResource(WireMockTestBase.class)
 * public class MyIntegrationTest {
 *     // Tests here will have WireMock running
 * }
 * }
 * </pre>
 */
public class WireMockTestBase implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(WireMockTestBase.class);
    private WireMockServer wireMockServer;
    public static final int WIREMOCK_PORT = 9999;

    @Override
    public Map<String, String> start() {
        LOG.info("Starting WireMock server for integration tests...");
        
        wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(WIREMOCK_PORT)
                .dynamicHttpsPort()
        );
        
        wireMockServer.start();
        WireMock.configureFor("localhost", WIREMOCK_PORT);
        
        // Set up default stubs
        setupDefaultOdooStubs();
        setupDefaultSlackStubs();
        
        LOG.infof("WireMock server started on port %d", WIREMOCK_PORT);
        
        // Override configuration to point to WireMock
        return Map.of(
            "odoo.url", "http://localhost:" + WIREMOCK_PORT + "/odoo",
            "slack.webhook-url", "http://localhost:" + WIREMOCK_PORT + "/slack/webhook",
            "slack.enabled", "false"
        );
    }

    @Override
    public void stop() {
        if (wireMockServer != null) {
            LOG.info("Stopping WireMock server...");
            wireMockServer.stop();
        }
    }

    /**
     * Set up default Odoo API stubs.
     * These are successful responses that work for most tests.
     * Individual tests can override with more specific stubs.
     */
    private void setupDefaultOdooStubs() {
        // Mock Odoo authenticate endpoint
        wireMockServer.stubFor(post(urlPathEqualTo("/odoo/web/session/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "jsonrpc": "2.0",
                        "id": 1,
                        "result": {
                            "uid": 2,
                            "session_id": "mock-session-id-123",
                            "username": "test_user",
                            "company_id": 1,
                            "partner_id": 3
                        }
                    }
                    """)));

        // Mock Odoo payment webhook endpoint
        wireMockServer.stubFor(post(urlPathMatching("/odoo/webhook/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "status": "success",
                        "message": "Payment received",
                        "payment_id": 12345
                    }
                    """)));

        // Mock Odoo document fetch
        wireMockServer.stubFor(get(urlPathMatching("/odoo/api/documents/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/pdf")
                .withBody("Mock PDF content")));

        // Mock Odoo XML-RPC call for documents
        wireMockServer.stubFor(post(urlPathEqualTo("/odoo/xmlrpc/2/object"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/xml")
                .withBody("""
                    <?xml version="1.0"?>
                    <methodResponse>
                        <params>
                            <param>
                                <value><array><data>
                                    <value><struct>
                                        <member>
                                            <name>id</name>
                                            <value><int>1</int></value>
                                        </member>
                                        <member>
                                            <name>name</name>
                                            <value><string>test-document.pdf</string></value>
                                        </member>
                                    </struct></value>
                                </data></array></value>
                            </param>
                        </params>
                    </methodResponse>
                    """)));
    }

    /**
     * Set up default Slack webhook stubs.
     */
    private void setupDefaultSlackStubs() {
        wireMockServer.stubFor(post(urlPathEqualTo("/slack/webhook"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("ok")));
    }

    /**
     * Get the WireMock server instance (for test-specific stubs).
     * Note: This is static access, use carefully.
     */
    public static void resetToDefault() {
        WireMock.reset();
    }
}
