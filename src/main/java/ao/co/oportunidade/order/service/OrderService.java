package ao.co.oportunidade.order.service;

import ao.co.oportunidade.order.model.Order;
import ao.co.oportunidade.order.entity.OrderRepository;
import solutions.envision.service.DomainService;
import ao.co.oportunidade.reference.model.Reference;
import ao.co.oportunidade.reference.service.ReferenceService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import solutions.envision.model.DomainNotCreatedException;

import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Order domain following DDD principles.
 */
@ApplicationScoped
public class OrderService extends DomainService<Order, OrderRepository> {


    @Inject
    ReferenceService referenceService;


    public  Order find(AppyPayWebhookPayload payload) {

        final Optional<Order> existingOrder =
                findByMerchantTransactionId(payload.getMerchantTransactionId());

        return existingOrder.orElse(null);
    }

    public  Order create(AppyPayWebhookPayload payload, Order.OrderStatus defaultStatus) {

        // Create new order
        final Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setMerchantTransactionId(payload.getMerchantTransactionId());
        order.setAmount(payload.getAmount());
        order.setCurrency(payload.getCurrency());
        order.setStatus(defaultStatus);
        order.setCreatedDate(Instant.now());
        order.setUpdatedDate(Instant.now());

        // Set customer info if available
        if (payload.getCustomer() != null) {
            final CustomerInfo customer = payload.getCustomer();
            order.setCustomerName(customer.getName());
            order.setCustomerEmail(customer.getEmail());
            order.setCustomerPhone(customer.getPhone());
        }

        // Link to reference if available
        if (payload.getReference() != null) {
            ReferenceInfo refInfo = payload.getReference();

             final Reference reference = referenceService.getReferenceByNumber(refInfo.getReferenceNumber());
             if(reference != null) {
                 order.setReferenceId(reference.getId());
             }else  {
                 throw new NoSuchElementException("Reference not found"); //TODO different Exception
             }
        }

        return order;
    }

    @Override
    public Collection<Order> getAllDomains() {
        return getRepository().findDomains();
    }

    @Transactional
    @Override
    public void saveDomain(Order order) {
        try {
            validateDomain(order);
        } catch (DomainNotCreatedException e) {
            throw new RuntimeException("Failed to create invalidated order", e);
        }
        getRepository().save(order);
    }

    /**
     * Find order by merchant transaction ID.
     *
     * @param merchantTransactionId the merchant transaction ID
     * @return Optional containing the order if found
     */
    public java.util.Optional<Order> findByMerchantTransactionId(String merchantTransactionId) {
        return getRepository().findByMerchantTransactionId(merchantTransactionId);
    }

}
