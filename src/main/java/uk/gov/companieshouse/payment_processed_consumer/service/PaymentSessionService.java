package uk.gov.companieshouse.payment_processed_consumer.service;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.payment.request.PaymentGet;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.payment_processed_consumer.apiclient.ApiClientServiceImpl;

@Component
public class PaymentSessionService {

    private final ApiClientServiceImpl apiClientServiceImpl;

    public PaymentSessionService(ApiClientServiceImpl apiClientServiceImpl) {
        this.apiClientServiceImpl = apiClientServiceImpl;
    }

    public PaymentApi getPaymentSession(String resourceId) {
        try {
            InternalApiClient client = apiClientServiceImpl.getPaymentsApiClient();
            PaymentGet paymentGet = client.payment().get("/payments/" + resourceId);
            return paymentGet.execute().getData();
        } catch (ApiErrorResponseException | URIValidationException e) {
            throw new RuntimeException("Invalid URI for payment resource: " + resourceId, e);
        }
    }
}