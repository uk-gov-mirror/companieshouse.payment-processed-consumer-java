package uk.gov.companieshouse.payment_processed_consumer.service;

import uk.gov.companieshouse.api.model.payment.PaymentApi;

public interface Service {

    public void processMessage(ServiceParameters parameters);
    public PaymentApi paymentPatchRequest(String refundId, String resourceId, PaymentApi
            paymentResponse);

}
