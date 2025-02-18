package uk.gov.companieshouse.payment_processed_consumer.service;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.payment.PrivatePaymentResourceHandler;
import uk.gov.companieshouse.api.handler.payment.request.PaymentPatch;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.logging.Logger;


@Component
public class PaymentPatchService {

    public final PrivatePaymentResourceHandler resourceHandler;
    private final Logger logger;

    public PaymentPatchService(PrivatePaymentResourceHandler resourceHandler, Logger logger) {
        this.resourceHandler = resourceHandler;
        this.logger = logger;
    }

    public void patchTransaction(String transactionUri, PaymentApi paymentPatchRequest) {
        try {
           PaymentPatch response = resourceHandler.patch(transactionUri, paymentPatchRequest);
            if (response.execute().getStatusCode() == 200) {
                logger.info("PATCH request sent to " + transactionUri + " transaction closed: " + paymentPatchRequest);
            } else {
                throw new RuntimeException("Failed to patch transaction at URI: " + transactionUri + ". Status code: " + response.execute().getStatusCode());
            }
        } catch (URIValidationException | ApiErrorResponseException e) {
            throw new RuntimeException("Failed to patch transaction at URI: " + transactionUri, e);
        }
    }
}