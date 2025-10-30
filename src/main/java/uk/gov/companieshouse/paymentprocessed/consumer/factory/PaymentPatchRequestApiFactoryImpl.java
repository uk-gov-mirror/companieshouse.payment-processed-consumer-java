package uk.gov.companieshouse.paymentprocessed.consumer.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;

import java.time.OffsetDateTime;

@Component
public class PaymentPatchRequestApiFactoryImpl implements PaymentPatchRequestApiFactory {
    @Override
    public PaymentPatchRequestApi createPaymentPatchRequest(String status, OffsetDateTime paidAt, String paymentReference) {
        PaymentPatchRequestApi paymentPatchRequestApi = new PaymentPatchRequestApi();
        paymentPatchRequestApi.setPaidAt(paidAt);
        paymentPatchRequestApi.setStatus(status);
        paymentPatchRequestApi.setPaymentReference(paymentReference);
        return paymentPatchRequestApi;
    }
}
