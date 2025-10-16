package ao.co.oportunidade.webhook.service;

import io.quarkus.test.junit.QuarkusTest;


/**
 * Unit tests for WebhookEventService.
 */
@QuarkusTest
public class WebhookEventServiceTest {/*

    @Inject
    WebhookEventService webhookEventService;

    @InjectMock
    WebhookEventRepository webhookEventRepository;

    @Inject
    ObjectMapper objectMapper;

    private AppyPayWebhookPayload testPayload;

    @BeforeEach
    void setUp() {
        testPayload = AppyPayWebhookPayload.builder()
                .id("test-transaction-123")
                .merchantTransactionId("ORDER-12345")
                .type("Charge")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status("Success")
                .paymentMethod("REF")
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    @Test
    void testIsAlreadyProcessed_NotProcessed() {
        // Given
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.empty());

        // When
        boolean result = webhookEventService.isAlreadyProcessed("test-transaction-123");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testIsAlreadyProcessed_AlreadyProcessed() {
        // Given
        final WebhookEvent existingEvent = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.PROCESSED)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(existingEvent));

        // When
        boolean result = webhookEventService.isAlreadyProcessed("test-transaction-123");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsAlreadyProcessed_CurrentlyProcessing() {
        // Given
        WebhookEvent existingEvent = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.PROCESSING)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(existingEvent));

        // When
        boolean result = webhookEventService.isAlreadyProcessed("test-transaction-123");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsAlreadyProcessed_Failed_CanRetry() {
        // Given
        WebhookEvent existingEvent = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.FAILED)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(existingEvent));

        // When
        boolean result = webhookEventService.isAlreadyProcessed("test-transaction-123");

        // Then
        assertThat(result).isFalse(); // Failed events can be retried
    }

    @Test
    void testCreateWebhookEvent() {
        // Given
        doAnswer(invocation -> {
            WebhookEvent event = invocation.getArgument(0);
            event.setId(java.util.UUID.randomUUID());
            return null;
        }).when(webhookEventRepository).persist(any(WebhookEvent.class));

        // When
        WebhookEvent result = webhookEventService.createWebhookEvent(testPayload);

        // Then
        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).persist(captor.capture());
        
        final WebhookEvent captured = captor.getValue();
        assertThat(captured.getAppypayTransactionId()).isEqualTo("test-transaction-123");
        assertThat(captured.getMerchantTransactionId()).isEqualTo("ORDER-12345");
        assertThat(captured.getWebhookType()).isEqualTo("Charge");
        assertThat(captured.getProcessingStatus()).isEqualTo(WebhookEvent.ProcessingStatus.RECEIVED);
        assertThat(captured.getRetryCount()).isEqualTo(0);
        assertThat(captured.getPayload()).isNotNull();
    }

    @Test
    void testMarkAsProcessing() {
        // Given
        WebhookEvent event = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.RECEIVED)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(event));

        // When
        webhookEventService.markAsProcessing("test-transaction-123");

        // Then
        verify(webhookEventRepository).persist(any(WebhookEvent.class).);
        assertThat(event.getProcessingStatus()).isEqualTo(WebhookEvent.ProcessingStatus.PROCESSING);
    }

    @Test
    void testMarkAsProcessed() {
        // Given
        WebhookEvent event = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.PROCESSING)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(event));

        // When
        webhookEventService.markAsProcessed("test-transaction-123");

        // Then
        verify(webhookEventRepository).persist(any(WebhookEvent.class));
        assertThat(event.getProcessingStatus()).isEqualTo(WebhookEvent.ProcessingStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void testMarkAsFailed() {
        // Given
        WebhookEvent event = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.PROCESSING)
                .retryCount(0)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(event));

        // When
        webhookEventService.markAsFailed("test-transaction-123", "Test error");

        // Then
        verify(webhookEventRepository).persist(any(WebhookEvent.class));
        assertThat(event.getProcessingStatus()).isEqualTo(WebhookEvent.ProcessingStatus.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("Test error");
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    void testMoveToDeadLetter() {
        // Given
        WebhookEvent event = WebhookEvent.builder()
                .appypayTransactionId("test-transaction-123")
                .processingStatus(WebhookEvent.ProcessingStatus.FAILED)
                .retryCount(3)
                .build();
        
        when(webhookEventRepository.findByAppyPayTransactionId("test-transaction-123"))
                .thenReturn(Optional.of(event));

        // When
        webhookEventService.moveToDeadLetter("test-transaction-123");

        // Then
        verify(webhookEventRepository).persist(any(WebhookEvent.class));
        assertThat(event.getProcessingStatus()).isEqualTo(WebhookEvent.ProcessingStatus.DEAD_LETTER);
    }**/
}
