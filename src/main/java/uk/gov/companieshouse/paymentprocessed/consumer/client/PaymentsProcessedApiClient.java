package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.payments.PaymentResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

@Component
public class PaymentsProcessedApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private static final String GET_PAYMENT_CALL = "Payments Consumer API GET Payment";
    private static final String PATCH_PAYMENT_CALL = "Payments Consumer API Patch Payment";
    private static final String URL_CREATE_EXCEPTION_MESSAGE = "%s failed due to being unable to create URL for resource ID: %s";
    @Value("${payments.api.url}")
    String paymentsApiUrl;
    @Value("${skip.gone.resource.id}")
    String skipGoneResourceId;
    @Value("${skip.gone.resource}")
    Boolean skipGoneResource;

    PaymentsProcessedApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler, ObjectMapper objectMapper) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.objectMapper = objectMapper;
        this.responseHandler = responseHandler;
    }

    public Optional<PaymentResponse> getPayment(String resourceID) {
        InternalApiClient apiClient = internalApiClientFactory.get();
        apiClient.setBasePaymentsPath(paymentsApiUrl);
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        String resourceUri = String.format("/payments/%s", resourceID);
        Optional<PaymentResponse> response = Optional.empty();
        LOGGER.info(String.format("Initiating %s resource ID: %s and resource URI: %s", GET_PAYMENT_CALL, resourceID, resourceUri));
        try {
            response = Optional.ofNullable(apiClient.privatePayment().paymentProcessedConsumerGet(resourceUri)
                    .execute()
                    .getData());
            if (response.isPresent()) {
                String jsonResponse = objectMapper.writeValueAsString(response.get());
                LOGGER.info(String.format("Successfully called %s for resource ID: %s and response: %s", GET_PAYMENT_CALL, resourceID, jsonResponse));
            }
        } catch (ApiErrorResponseException ex) {
            LOGGER.error(String.format("Unable to obtain response from %s for resource ID: %s", GET_PAYMENT_CALL, resourceID));
            if (ex.getStatusCode() == HttpStatus.GONE.value() && checkSkipGoneResource(resourceID, skipGoneResource)) {
                return Optional.empty();
            }
            responseHandler.handle(GET_PAYMENT_CALL, resourceID, ex);
        } catch (JsonProcessingException ex) {
            responseHandler.handle(GET_PAYMENT_CALL, resourceID, ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(GET_PAYMENT_CALL, ex);
        }
        return response;
    }

    public void patchPayment(String paymentsPatchUri, PaymentPatchRequestApi paymentPatchRequestApi) {
        InternalApiClient apiClient = internalApiClientFactory.get();
        URL url;
        try {
            url = new URI(paymentsPatchUri).toURL();
        } catch (URISyntaxException | MalformedURLException | RuntimeException ex) {
            LOGGER.error(String.format(URL_CREATE_EXCEPTION_MESSAGE, PATCH_PAYMENT_CALL, paymentsPatchUri),
                    ex, DataMapHolder.getLogMap());
            throw new NonRetryableException(String.format(URL_CREATE_EXCEPTION_MESSAGE, PATCH_PAYMENT_CALL, paymentsPatchUri), ex);
        }
        LOGGER.info(String.format("URL: %s for PaymentsPatchUri: %s", url, paymentsPatchUri));
        String protocol = url.getProtocol() + "://" + url.getHost();
        apiClient.setBasePaymentsPath(protocol);
        apiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        paymentsPatchUri = paymentsPatchUri.substring(protocol.length());
        LOGGER.info(String.format("PaymentsPatchUri after substring: %s", paymentsPatchUri));
        Void response;
        try {
            LOGGER.info(String.format("Initiating %s for resource URI: %s and PaymentPatchRequestApi: %s",
                    PATCH_PAYMENT_CALL, paymentsPatchUri, objectMapper.writeValueAsString(paymentPatchRequestApi)));
            response = apiClient.privatePayment().paymentProcessedConsumerPatch(paymentsPatchUri, paymentPatchRequestApi)
                    .execute()
                    .getData();
            LOGGER.info(String.format("Successfully called %s for resource URI: %s and response: %s",
                    PATCH_PAYMENT_CALL, paymentsPatchUri, objectMapper.writeValueAsString(response)));
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(PATCH_PAYMENT_CALL, paymentsPatchUri, ex);
        } catch (JsonProcessingException ex) {
            responseHandler.handle(PATCH_PAYMENT_CALL, paymentsPatchUri, ex);
        }
    }

    public boolean checkSkipGoneResource(String paymentId, boolean skipGoneResource) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("payment_id", paymentId);

        if (skipGoneResource) {
            LOGGER.info(String.format("SKIP_GONE_RESOURCE is true - checking if message should be skipped for Payment ID [%s]", paymentId), logData);
            if (skipGoneResourceId != null && !skipGoneResourceId.isEmpty() && !skipGoneResourceId.equals(paymentId)) {
                LOGGER.info(String.format("SKIP_GONE_RESOURCE_ID [%s] does not match Payment ID [%s] - not skipping message", skipGoneResourceId, paymentId), logData);
                return false;
            }
            LOGGER.info(String.format("Message for Payment ID [%s] meets criteria and will be skipped", paymentId), logData);
            return true;
        }
        LOGGER.info(String.format("SKIP_GONE_RESOURCE is false - not skipping message for Payment ID [%s]", paymentId), logData);
        return false;
    }

}
