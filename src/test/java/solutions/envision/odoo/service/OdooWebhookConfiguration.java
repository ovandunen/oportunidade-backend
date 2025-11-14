package solutions.envision.odoo.service;


import jakarta.inject.Singleton;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;




@Getter
@Singleton
public class OdooWebhookConfiguration {

    @ConfigProperty(name = "odoo.webhook.key")
    private String odooWebhookKey;

    @ConfigProperty(name = "odoo.webhook.url")
    private String odooWebhookUrl;
}
