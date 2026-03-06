package ao.co.oportunidade.openapi;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * OpenAPI specification for third-party REST consumers.
 * <ul>
 *   <li>OpenAPI JSON: GET /api-docs/openapi</li>
 *   <li>OpenAPI YAML: GET /api-docs/openapi?format=yaml</li>
 *   <li>Swagger UI: GET /q/swagger-ui</li>
 * </ul>
 */
@ApplicationScoped
@OpenAPIDefinition(
        info = @Info(
                title = "Recruiting Agency API",
                version = "1.0.0",
                description = """
                        REST API for recruiting agency payment and document access integration.
                        
                        **Payment Flow:**
                        1. Issue a payment reference via POST /api/v1/payment-references/text or /qr
                        2. Customer pays at MultiCaixa (ATM/agent) using the reference
                        3. AppyPay sends webhook to /webhooks/appypay on payment status change
                        4. Query order status via GET /api/v1/orders/merchant/{merchantTransactionId}
                        5. Document access tokens are sent via email after successful payment
                        """,
                contact = @Contact(
                        name = "API Support",
                        email = "api-support@example.com",
                        url = "https://example.com/support"
                ),
                license = @License(
                        name = "Proprietary",
                        url = "https://example.com/license"
                )
        ),
        servers = {
                @Server(url = "/", description = "Current server (base URL from request)")
        },
        tags = {
                @Tag(name = "Payment References", description = "Issue MultiCaixa payment references (text or QR)"),
                @Tag(name = "Orders", description = "Order management and status lookup"),
                @Tag(name = "Payment Transactions", description = "Payment transaction history"),
                @Tag(name = "Document Access", description = "Secure document download for employers"),
                @Tag(name = "References", description = "Reference management"),
                @Tag(name = "Health", description = "Health and readiness checks")
        }
)
public class OpenApiConfig {
}
