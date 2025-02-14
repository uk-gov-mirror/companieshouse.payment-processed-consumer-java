package uk.gov.companieshouse.payment_processed_consumer.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.payment_processed_consumer.model.PaymentPatchRequest;



@Component
public class PaymentProcessorService implements Service {

    private final PaymentRequest paymentRequest;
    private final Logger logger;

    public PaymentProcessorService(PaymentRequest paymentRequest, Logger logger) {
        this.paymentRequest = paymentRequest;
        this.logger = logger;
    }

    // Processes the incoming message, and retrieves the payment information from payments API. Will also determine whether payment is a refund or not, the transactionURI is amended if so.
    @Override
    public void processMessage(ServiceParameters parameters){
        final var message = parameters.getData();
        final var resourceId = message.getPaymentResourceId();
        final var refundId = message.getRefundId();


        logger.info("Processing message " + message + " for resource ID " + resourceId + ".");

        PaymentApi paymentSession = paymentRequest.getPaymentSession(resourceId);
        String transactionURI = paymentSession.getLinks().get("Resource");
        if (!refundId.isEmpty()) {
            transactionURI += "/refunds";
        }
        PaymentPatchRequest patchRequest = paymentPatchRequest(refundId, resourceId, paymentSession);
        paymentRequest.patchTransaction(transactionURI, patchRequest);
    }

    // Builds the PatchRequest to send to the transaction API
    @Override
    public PaymentPatchRequest paymentPatchRequest(String refundId, String resourceId, PaymentApi paymentSession) {
        PaymentPatchRequest paymentPatchRequest = new PaymentPatchRequest();
        if (!refundId.isEmpty()) {
            paymentSession.getRefunds().stream()
                    .filter(refund -> refund.getId().equals(refundId))
                    .findFirst()
                    .ifPresent(refund -> {
                        paymentPatchRequest.setRefundReference(refund.getId());
                        paymentPatchRequest.setRefundStatus(String.valueOf(refund.getStatus()));
                        paymentPatchRequest.setRefundProcessedAt(
                                LocalDateTime.parse(refund.getCreatedAt()));
                    });
        } else {
            paymentPatchRequest.setStatus(paymentSession.getStatus());
            paymentPatchRequest.setPaymentReference(resourceId);
            paymentPatchRequest.setPaidAt(LocalDateTime.parse(paymentSession.getCompletedAt()));
        }
        logger.info("Sending PATCH request for resourceId: " + resourceId);
        return paymentPatchRequest;
    }
}
