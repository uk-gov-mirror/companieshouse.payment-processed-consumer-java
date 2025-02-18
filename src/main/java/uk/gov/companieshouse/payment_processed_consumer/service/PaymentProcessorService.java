package uk.gov.companieshouse.payment_processed_consumer.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.logging.Logger;


@Component
public class PaymentProcessorService implements Service {

    private final PaymentSessionService paymentSessionService;
    private final PaymentPatchService paymentPatchService;
    private final Logger logger;

    public PaymentProcessorService(PaymentSessionService paymentSessionService,
            PaymentPatchService paymentPatchService, Logger logger) {
        this.paymentSessionService = paymentSessionService;
        this.paymentPatchService = paymentPatchService;
        this.logger = logger;
    }

    // Processes the incoming message, and retrieves the payment information from payments API. Will also determine whether payment is a refund or not, the transactionURI is amended if so.
    @Override
    public void processMessage(ServiceParameters parameters){
        final var message = parameters.getData();
        final var resourceId = message.getPaymentResourceId();
        final var refundId = message.getRefundId();

        logger.info("Processing message " + message + " for resource ID " + resourceId + ".");

        PaymentApi paymentSession = paymentSessionService.getPaymentSession(resourceId);
        String transactionURI = paymentSession.getLinks().get("Resource");
        if (!refundId.isEmpty()) {
            transactionURI += "/refunds";
        }
        PaymentApi patchRequest = paymentPatchRequest(refundId, resourceId, paymentSession);
        paymentPatchService.patchTransaction(transactionURI, patchRequest);
    }

    // Builds the PatchRequest to send to the transaction API
    @Override
    public PaymentApi paymentPatchRequest(String refundId, String resourceId, PaymentApi paymentSession) {
        PaymentApi paymentPatchRequest = new PaymentApi();
        if (!refundId.isEmpty()) {
            paymentSession.getRefunds().stream()
                    .filter(refund -> refund.getId().equals(refundId))
                    .findFirst()
                    .ifPresent(refund -> {
                        paymentPatchRequest.setReference(refund.getId());
                        paymentPatchRequest.setStatus(String.valueOf(refund.getStatus()));
                        paymentPatchRequest.setCreatedAt(
                                String.valueOf(LocalDateTime.parse(refund.getCreatedAt())));
                    });
        } else {
            paymentPatchRequest.setStatus(paymentSession.getStatus());
            paymentPatchRequest.setReference(resourceId);
            paymentPatchRequest.setCreatedAt(
                    String.valueOf(LocalDateTime.parse(paymentSession.getCompletedAt())));
        }
        logger.info("Sending PATCH request for resourceId: " + resourceId);
        return paymentPatchRequest;
    }
}
