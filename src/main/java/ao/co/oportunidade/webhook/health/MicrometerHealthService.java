package ao.co.oportunidade.webhook.health;


import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class MicrometerHealthService{

    private static final Logger LOG = Logger.getLogger(MicrometerHealthService.class);

    @Inject
    MeterRegistry registry;

    @Inject
    DataSource dataSource;

    private final AtomicInteger databaseHealthStatus = new AtomicInteger(1); // 1 = UP, 0 = DOWN
    private final AtomicInteger applicationHealthStatus = new AtomicInteger(1);
    private final AtomicInteger webhookHealthStatus = new AtomicInteger(1);

    public void init() {
        // Database Health Gauge
        Gauge.builder("health.database", databaseHealthStatus, AtomicInteger::get)
                .description("Database health status (1=UP, 0=DOWN)")
                .tag("type", "database")
                .tag("service", "postgresql")
                .register(registry);

        // Application Health Gauge
        Gauge.builder("health.application", applicationHealthStatus, AtomicInteger::get)
                .description("Application health status (1=UP, 0=DOWN)")
                .tag("type", "application")
                .register(registry);

        // Webhook Service Health Gauge
        Gauge.builder("health.webhook.service", webhookHealthStatus, AtomicInteger::get)
                .description("Webhook service health status (1=UP, 0=DOWN)")
                .tag("type", "webhook")
                .register(registry);

        LOG.info("Micrometer health metrics initialized");
    }

    public boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            databaseHealthStatus.set(isValid ? 1 : 0);

            registry.counter("health.database.checks.total",
                            "status", isValid ? "success" : "failure")
                    .increment();

            return isValid;
        } catch (SQLException e) {
            LOG.error("Database health check failed", e);
            databaseHealthStatus.set(0);

            registry.counter("health.database.checks.total",
                            "status", "error",
                            "error", e.getClass().getSimpleName())
                    .increment();

            return false;
        }
    }

    public boolean checkWebhookServiceHealth() {
        try {
            // Ihre Webhook Service Logik hier
            boolean isHealthy = true; // Implementieren Sie Ihre Logik
            webhookHealthStatus.set(isHealthy ? 1 : 0);

            registry.counter("health.webhook.checks.total",
                            "status", isHealthy ? "success" : "failure")
                    .increment();

            return isHealthy;
        } catch (Exception e) {
            LOG.error("Webhook health check failed", e);
            webhookHealthStatus.set(0);

            registry.counter("health.webhook.checks.total",
                            "status", "error")
                    .increment();

            return false;
        }
    }

    public void updateApplicationHealth(boolean isHealthy) {
        applicationHealthStatus.set(isHealthy ? 1 : 0);
    }
}