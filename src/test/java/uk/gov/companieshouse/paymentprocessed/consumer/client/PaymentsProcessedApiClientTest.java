package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.payments.PrivatePaymentResourceHandler;
import uk.gov.companieshouse.api.handler.payments.request.PaymentProcessedConsumerGet;
import uk.gov.companieshouse.api.handler.payments.request.PaymentProcessedConsumerPatch;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.payments.PaymentResponse;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.BASE_URL;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentPatchRequestApi;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentResponse;

@ExtendWith(MockitoExtension.class)
public class PaymentsProcessedApiClientTest {
    @Mock
    private Supplier<InternalApiClient> internalApiClientFactory;
    @Mock
    private InternalApiClient internalApiClient;
    @Mock
    private PrivatePaymentResourceHandler privatePaymentResourceHandler;
    @Mock
    HttpClient httpClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PaymentProcessedConsumerGet paymentProcessedConsumerGet;
    @Mock
    private PaymentProcessedConsumerPatch paymentProcessedConsumerPatch;
    @Mock
    private ResponseHandler responseHandler;
    @InjectMocks
    private PaymentsProcessedApiClient paymentsProcessedApiClient;
    public static final String RESOURCE_ID = "P9hl8PrKRBk1Zmc";
    @Mock
    private ApiResponse<Void> apiResponseVoid;

    @BeforeEach
    void setUp(TestInfo info) {
        if (info.getDisplayName().equals("shouldThrowNonRetryableExceptionForInvalidPaymentsPatchUri()"))
            return;
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privatePayment()).thenReturn(privatePaymentResourceHandler);
    }

    @Test
    void shouldSendSuccessfulGetRequest() throws Exception {
        when(privatePaymentResourceHandler.paymentProcessedConsumerGet(anyString())).thenReturn(paymentProcessedConsumerGet);
        when(paymentProcessedConsumerGet.execute()).thenReturn(getPaymentResponse());
        Optional<PaymentResponse> paymentResponse = paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        Assertions.assertTrue(paymentResponse.isPresent());
        Assertions.assertEquals(TestUtils.RESOURCE_LINK, paymentResponse.get().getLinks().getResource());
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerGet("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldSendSuccessfulPatchRequest() throws Exception {
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        when(privatePaymentResourceHandler.paymentProcessedConsumerPatch(TestUtils.RESOURCE_LINK, paymentPatchRequestApi)).thenReturn(paymentProcessedConsumerPatch);
        doReturn(getAPIResponse(null)).when(paymentProcessedConsumerPatch).execute();
        paymentsProcessedApiClient.patchPayment(BASE_URL + TestUtils.RESOURCE_LINK, paymentPatchRequestApi);
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerPatch(TestUtils.RESOURCE_LINK, paymentPatchRequestApi);
    }

    @Test
    void shouldHandleApiErrorExceptionWhenSendingGetRequest() throws Exception {
        Class<ApiErrorResponseException> exceptionClass = ApiErrorResponseException.class;
        when(privatePaymentResourceHandler.paymentProcessedConsumerGet(anyString())).thenReturn(paymentProcessedConsumerGet);
        when(paymentProcessedConsumerGet.execute()).thenThrow(ApiErrorResponseException.class);
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(responseHandler).handle(anyString(), anyString(), any(exceptionClass));
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerGet("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleApiErrorExceptionWhenSendingPatchRequest() throws Exception {
        Class<ApiErrorResponseException> exceptionClass = ApiErrorResponseException.class;
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        when(privatePaymentResourceHandler.paymentProcessedConsumerPatch(TestUtils.RESOURCE_LINK, paymentPatchRequestApi)).thenReturn(paymentProcessedConsumerPatch);
        when(paymentProcessedConsumerPatch.execute()).thenThrow(exceptionClass);
        paymentsProcessedApiClient.patchPayment(BASE_URL + TestUtils.RESOURCE_LINK, paymentPatchRequestApi);
        verify(responseHandler).handle(anyString(), anyString(), any(exceptionClass));
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerPatch(TestUtils.RESOURCE_LINK, paymentPatchRequestApi);
    }

    @Test
    void shouldHandleURIValidationExceptionWhenSendingGetRequest() throws Exception {
        Class<URIValidationException> uriValidationException = URIValidationException.class;
        when(privatePaymentResourceHandler.paymentProcessedConsumerGet(anyString())).thenReturn(paymentProcessedConsumerGet);
        when(paymentProcessedConsumerGet.execute()).thenThrow(uriValidationException);
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(responseHandler).handle(anyString(), any(uriValidationException));
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerGet("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleJsonProcessingExceptionWhenSendingGetRequest() throws Exception {
        Class<JsonProcessingException> exceptionClass = JsonProcessingException.class;
        when(privatePaymentResourceHandler.paymentProcessedConsumerGet(anyString())).thenReturn(paymentProcessedConsumerGet);
        when(paymentProcessedConsumerGet.execute()).thenReturn(getPaymentResponse());
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(responseHandler).handle(anyString(), anyString(), any(exceptionClass));
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerGet("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleJsonProcessingExceptionWhenSendingPatchRequest() throws Exception {
        Class<JsonProcessingException> exceptionClass = JsonProcessingException.class;
        PaymentPatchRequestApi paymentPatchRequestApi = getPaymentPatchRequestApi();
        when(privatePaymentResourceHandler.paymentProcessedConsumerPatch(TestUtils.RESOURCE_LINK, paymentPatchRequestApi)).thenReturn(paymentProcessedConsumerPatch);
        doReturn(getAPIResponse(null)).when(paymentProcessedConsumerPatch).execute();
        doAnswer(invocation -> {
                    Object arg = invocation.getArgument(0);
                    if (arg == null)
                        throw new JsonProcessingException("Mocked exception for PaymentResponse") {
                        };
                    return paymentPatchRequestApi.toString();
                }
        ).when(objectMapper).writeValueAsString(any());
        paymentsProcessedApiClient.patchPayment(BASE_URL + TestUtils.RESOURCE_LINK, paymentPatchRequestApi);
        verify(responseHandler).handle(anyString(), anyString(), any(exceptionClass));
    }

    @Test
    void shouldHandleGoneResourceWhenSendingGetRequest() throws Exception {
        HttpResponseException.Builder builder = new HttpResponseException.Builder(
                HttpStatus.GONE.value(),
                "Resource Gone",
                new HttpHeaders() // Ensure headers are not null
        );
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);
        when(privatePaymentResourceHandler.paymentProcessedConsumerGet(anyString())).thenReturn(paymentProcessedConsumerGet);
        when(paymentProcessedConsumerGet.execute()).thenThrow(apiErrorResponseException);
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResource", true
        );
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResourceId", RESOURCE_ID
        );
        paymentsProcessedApiClient.getPayment(RESOURCE_ID);
        verify(privatePaymentResourceHandler, times(1)).paymentProcessedConsumerGet("/payments/" + RESOURCE_ID);
    }

    @Test
    void shouldHandleGoneResourceDifferentPaymentIdWhenSendingGetRequest() throws Exception {
        HttpResponseException.Builder builder = new HttpResponseException.Builder(
                HttpStatus.GONE.value(),
                "Resource Gone",
                new HttpHeaders() // Ensure headers are not null
        );
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);
        when(privatePaymentResourceHandler.paymentProcessedConsumerGet(anyString())).thenReturn(paymentProcessedConsumerGet);
        when(paymentProcessedConsumerGet.execute()).thenThrow(apiErrorResponseException);
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResource", true
        );
        ReflectionTestUtils.setField(paymentsProcessedApiClient, "skipGoneResourceId", RESOURCE_ID);
        PaymentsProcessedApiClient spyClient = spy(paymentsProcessedApiClient);
        spyClient.getPayment(RESOURCE_ID + "1");
        verify(spyClient, times(1)).checkSkipGoneResource(RESOURCE_ID + "1", true);
        Assertions.assertFalse(spyClient.checkSkipGoneResource(RESOURCE_ID + "1", true), "Expected checkSkipGoneResource to return false");
    }

    @Test
    void shouldThrowNonRetryableExceptionForInvalidPaymentsPatchUri() {
        // Arrange
        String invalidPaymentsPatchUri = "invalid_uri";
        PaymentPatchRequestApi paymentPatchRequestApi = mock(PaymentPatchRequestApi.class);

        // Act & Assert
        NonRetryableException exception = Assertions.assertThrows(NonRetryableException.class, () ->
                paymentsProcessedApiClient.patchPayment(invalidPaymentsPatchUri, paymentPatchRequestApi)
        );

        // Verify
        Assertions.assertTrue(exception.getMessage().contains("Payments Consumer API Patch Payment failed due to being unable to create URL for resource ID: invalid_uri"));
    }

    public static <T> ApiResponse<T> getAPIResponse(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), null, data);
    }
}
