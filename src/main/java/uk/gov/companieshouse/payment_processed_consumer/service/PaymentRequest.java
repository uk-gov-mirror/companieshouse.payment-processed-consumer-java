package uk.gov.companieshouse.payment_processed_consumer.service;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.payment.request.PaymentGet;
import uk.gov.companieshouse.api.model.payment.PaymentApi;
import uk.gov.companieshouse.payment_processed_consumer.apiclient.ApiClientServiceImpl;
import uk.gov.companieshouse.payment_processed_consumer.model.PaymentPatchRequest;

@Component
public class PaymentRequest{

    private final ApiClientServiceImpl apiClientServiceImpl;


    public PaymentRequest(ApiClientServiceImpl apiClientServiceImpl) {
        this.apiClientServiceImpl = apiClientServiceImpl;

    }

    public PaymentApi getPaymentSession(String resourceId){
        try {
            InternalApiClient client = apiClientServiceImpl.getPaymentsApiClient();
            PaymentGet paymentGet = client.payment().get("/payments/" + resourceId);
            return paymentGet.execute().getData();
        } catch (ApiErrorResponseException | URIValidationException e) {
            throw new RuntimeException("Invalid URI for payment resource: " + resourceId, e);
        }
    }


    public void patchTransaction(String transactionUri, PaymentPatchRequest paymentPatch){

    }

//    public void updateTransaction(String transactionURI, PaymentPatchRequest patchRequest){
//        try{
//            String requestBody = objectMapper.writeValueAsString(patchRequest);
//            HttpRequest request  = HttpRequest.newBuilder()
//                    .uri(URI.create(transactionURI))
//                    .header("Content-type", "application/json")
//                    .header("Authorisation", "Bearer" + apiClientServiceImpl.getChsApiKey())
//                    .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
//                    .build();
//
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//        }catch (Exception e){
//            throw new RuntimeException("Error updating refund status", e);
//        }
//    }

}

