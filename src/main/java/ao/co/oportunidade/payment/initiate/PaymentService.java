package ao.co.oportunidade.payment.initiate;

import ao.co.oportunidade.payment.initiate.client.AppyPayClient;
import ao.co.oportunidade.payment.initiate.client.AppyPaySessionRequest;
import ao.co.oportunidade.payment.initiate.client.AppyPaySessionResponse;
import ao.co.oportunidade.payment.initiate.dto.PaymentInitiateRequest;
import ao.co.oportunidade.payment.initiate.dto.PaymentInitiateResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    @RestClient
    AppyPayClient appyPayClient;

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "payment.initiation.session-expiry-hours", defaultValue = "24")
    int sessionExpiryHours;

    @Transactional
    public PaymentInitiateResponse initiate(String userId, PaymentInitiateRequest request) {
        BillableResource catalog = findCatalog(request.resourceId(), request.resourceType());
        if (catalog == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Unknown resourceId or resourceType"))
                            .build());
        }

        if (catalog.singleBuyer
                && PaymentInitiationOrder.existsPaidForResourceDifferentUser(request.resourceId(), userId)) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", "Resource already purchased by another user"))
                            .build());
        }

        Instant now = Instant.now();
        Optional<PaymentInitiationOrder> existingOpt =
                PaymentInitiationOrder.findByUserAndResource(userId, request.resourceId());

        if (existingOpt.isPresent()) {
            PaymentInitiationOrder existing = existingOpt.get();
            if (existing.status == OrderStatus.PAID) {
                return new PaymentInitiateResponse(existing.merchantTransactionId, existing.paymentUrl);
            }
            if (existing.status == OrderStatus.PENDING && existing.paymentSessionExpiresAt.isAfter(now)) {
                return new PaymentInitiateResponse(existing.merchantTransactionId, existing.paymentUrl);
            }
            if (existing.status == OrderStatus.PENDING && !existing.paymentSessionExpiresAt.isAfter(now)) {
                return refreshSession(existing, request, now);
            }
            if (existing.status == OrderStatus.FAILED || existing.status == OrderStatus.EXPIRED) {
                return refreshSession(existing, request, now);
            }
        }

        return createNewOrder(userId, request, now);
    }

    private PaymentInitiateResponse refreshSession(
            PaymentInitiationOrder row,
            PaymentInitiateRequest request,
            Instant now) {
        String merchantTransactionId = newMerchantTransactionId();
        AppyPaySessionResponse session = callAppyPay(merchantTransactionId, request);
        row.merchantTransactionId = merchantTransactionId;
        row.paymentUrl = session.paymentUrl();
        row.status = OrderStatus.PENDING;
        row.paymentSessionExpiresAt = now.plus(sessionExpiryHours, ChronoUnit.HOURS);
        row.updatedAt = now;
        return new PaymentInitiateResponse(row.merchantTransactionId, row.paymentUrl);
    }

    private PaymentInitiateResponse createNewOrder(String userId, PaymentInitiateRequest request, Instant now) {
        String merchantTransactionId = newMerchantTransactionId();
        AppyPaySessionResponse session = callAppyPay(merchantTransactionId, request);

        PaymentInitiationOrder order = new PaymentInitiationOrder();
        order.id = UUID.randomUUID();
        order.userId = userId;
        order.resourceId = request.resourceId();
        order.resourceType = request.resourceType();
        order.merchantTransactionId = merchantTransactionId;
        order.paymentUrl = session.paymentUrl();
        order.status = OrderStatus.PENDING;
        order.paymentSessionExpiresAt = now.plus(sessionExpiryHours, ChronoUnit.HOURS);
        order.createdAt = now;
        order.updatedAt = now;
        order.persist();

        return new PaymentInitiateResponse(order.merchantTransactionId, order.paymentUrl);
    }

    private static String newMerchantTransactionId() {
        return "txn-" + UUID.randomUUID();
    }

    private AppyPaySessionResponse callAppyPay(String merchantTransactionId, PaymentInitiateRequest request) {
        try {
            AppyPaySessionResponse response = appyPayClient.createPaymentSession(
                    new AppyPaySessionRequest(
                            merchantTransactionId,
                            request.resourceId(),
                            request.resourceType().name()));
            if (response == null || response.paymentUrl() == null || response.paymentUrl().isBlank()) {
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_GATEWAY)
                                .entity(Map.of("error", "Invalid response from payment provider"))
                                .build());
            }
            return response;
        } catch (ProcessingException e) {
            LOG.errorf(e, "AppyPay emulator unreachable");
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_GATEWAY)
                            .entity(Map.of("error", "Payment provider unavailable"))
                            .build());
        } catch (WebApplicationException e) {
            Response r = e.getResponse();
            int status = r != null ? r.getStatus() : 0;
            if (status == Response.Status.CONFLICT.getStatusCode()
                    || (status >= 400 && status < 500)) {
                throw e;
            }
            LOG.errorf(e, "AppyPay emulator error");
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_GATEWAY)
                            .entity(Map.of("error", "Payment provider unavailable"))
                            .build());
        }
    }

    private BillableResource findCatalog(String resourceId, PaymentResourceType resourceType) {
        return entityManager.createQuery(
                        "select b from BillableResource b where b.resourceId = :r and b.resourceType = :t",
                        BillableResource.class)
                .setParameter("r", resourceId)
                .setParameter("t", resourceType)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
