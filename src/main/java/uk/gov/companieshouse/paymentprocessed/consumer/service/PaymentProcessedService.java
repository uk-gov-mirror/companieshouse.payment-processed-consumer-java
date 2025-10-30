package uk.gov.companieshouse.paymentprocessed.consumer.service;

import org.springframework.stereotype.Service;
import payments.payment_processed;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.payments.PaymentResponse;
import uk.gov.companieshouse.api.payments.Refund;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.client.PaymentsProcessedApiClient;
import uk.gov.companieshouse.paymentprocessed.consumer.factory.PaymentPatchRequestApiFactoryImpl;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

@Service
public class PaymentProcessedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final PaymentsProcessedApiClient paymentsProcessedApiClient;

    private final PaymentPatchRequestApiFactoryImpl paymentPatchRequestApiFactoryImpl;

    public PaymentProcessedService(PaymentsProcessedApiClient paymentsProcessedApiClient, PaymentPatchRequestApiFactoryImpl paymentPatchRequestApiFactoryImpl) {
        this.paymentsProcessedApiClient = paymentsProcessedApiClient;
        this.paymentPatchRequestApiFactoryImpl = paymentPatchRequestApiFactoryImpl;
    }

    public void processMessage(payment_processed paymentProcessed) {
        //Naming convention to be fixed as per https://companieshouse.atlassian.net/browse/KAF-99
        String paymentResourceId = paymentProcessed.getPaymentResourceId();
        DataMapHolder.get().resourceId(paymentResourceId);
        Optional<PaymentResponse> paymentResponseOptional = paymentsProcessedApiClient.getPayment(paymentResourceId);
        if (paymentResponseOptional.isEmpty()) {
            LOGGER.info("Payment not found or skipped for PaymentResourceId" + paymentResourceId);
            return;
        }
        PaymentResponse paymentResponse = paymentResponseOptional.get();
        String patchUri = paymentResponse.getLinks().getResource();
        PaymentPatchRequestApi paymentPatchRequestApi = null;
        if (paymentProcessed.getRefundId() != null && paymentResponse.getRefunds() != null) {
            LOGGER.info("Refund ID present in message, for PaymentResourceId" + paymentResourceId);
            Optional<Refund> refundOptional = paymentResponse.getRefunds().stream().filter(refund -> refund.getRefundId().equals(paymentProcessed.getRefundId())).findFirst();
            if (refundOptional.isPresent()) {
                Refund refund = refundOptional.get();
                paymentPatchRequestApi = paymentPatchRequestApiFactoryImpl.createPaymentPatchRequest(refund.getStatus(), OffsetDateTime.from(refund.getCreatedAt()), refund.getRefundReference());
                patchUri += "/refunds";
            }
        } else {
            paymentPatchRequestApi = paymentPatchRequestApiFactoryImpl.createPaymentPatchRequest(paymentResponse.getStatus(), OffsetDateTime.from(paymentResponse.getCompletedAt()), paymentProcessed.getPaymentResourceId());
        }
        paymentsProcessedApiClient.patchPayment(patchUri, paymentPatchRequestApi);
        LOGGER.info("Payment Patched Successfully, for PaymentResourceId " + paymentResourceId);
    }
}


