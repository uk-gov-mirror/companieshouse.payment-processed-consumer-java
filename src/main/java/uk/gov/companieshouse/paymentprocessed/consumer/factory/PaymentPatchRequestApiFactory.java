package uk.gov.companieshouse.paymentprocessed.consumer.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;

import java.time.OffsetDateTime;

@Component
public interface PaymentPatchRequestApiFactory {
    PaymentPatchRequestApi createPaymentPatchRequest(String status, OffsetDateTime paidAt, String paymentReference);
}
