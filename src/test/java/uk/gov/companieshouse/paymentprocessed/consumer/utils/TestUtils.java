package uk.gov.companieshouse.paymentprocessed.consumer.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import payments.payment_processed;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.payments.PaymentResponse;

import java.time.OffsetDateTime;

import static uk.gov.companieshouse.paymentprocessed.consumer.client.PaymentsProcessedApiClientTest.getAPIResponse;

public class TestUtils {

    public static final String RESOURCE_LINK = "/transactions/174365-968117-586962/payment";

    public static final String BASE_URL = "http://api-payments.chs.local";

    public static final String GET_URI = "/payments/P9hl8PrKRBk1Zmc";


    public static ApiResponse<PaymentResponse> getPaymentResponse() throws JsonProcessingException {
        String json = "{\"amount\":\"55.00\",\"completed_at\":\"2025-09-24T06:44:32.354Z\",\"created_at\":\"2025-09-24T06:44:27.854Z\",\"description\":\"Application to register a Companies House authorised agent\",\"links\":{\"journey\":\"https://payments.cidev.aws.chdev.org/payments/Bq286FEk6xzSfXk/pay\",\"resource\":\"" + RESOURCE_LINK + "\",\"self\":\"payments/Bq286FEk6xzSfXk\"},\"payment_method\":\"credit-card\",\"reference\":\"Register_ACSP_174365-968117-586962\",\"status\":\"paid\",\"etag\":\"34e92e90a981a9686b45a56204e98d7d1fef86bbb446bf0c2cf5c679\",\"kind\":\"payment-session#payment-session\"}}";
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        PaymentResponse paymentResponse = objectMapper.readValue(json, PaymentResponse.class);
        return getAPIResponse(paymentResponse);
    }

    public static PaymentPatchRequestApi getPaymentPatchRequestApi() throws JsonProcessingException {
        String json = "{\"completed_at\":\"2025-09-24T06:44:32.354Z\",\"status\":\"paid\",\"reference\":\"Register_ACSP_174365-968117-586962\"}";
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        JsonNode rootNode = objectMapper.readTree(json);
        String status = rootNode.get("status").asText();
        String reference = rootNode.get("reference").asText();
        String paidAt = rootNode.get("completed_at").asText();
        PaymentPatchRequestApi paymentPatchRequestApi = new PaymentPatchRequestApi();
        paymentPatchRequestApi.setPaymentReference(reference);
        paymentPatchRequestApi.setStatus(status);
        paymentPatchRequestApi.setPaidAt(OffsetDateTime.parse(paidAt));
        return paymentPatchRequestApi;
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @NotNull
    public static payment_processed getPaymentProcessed() {
        payment_processed paymentProcessed = new payment_processed();
        paymentProcessed.setAttempt(1);
        paymentProcessed.setPaymentResourceId("P9hl8PrKRBk1Zmc");
        return paymentProcessed;
    }

    public static ApiResponse<PaymentResponse> getPaymentResponseRefund() throws JsonProcessingException {
        String json = "{"
                + "\"amount\":\"55.00\","
                + "\"completed_at\":\"2025-09-24T06:44:32.354Z\","
                + "\"created_at\":\"2025-09-24T06:44:27.854Z\","
                + "\"description\":\"Application to register a Companies House authorised agent\","
                + "\"links\":{"
                + "\"journey\":\"https://payments.cidev.aws.chdev.org/payments/Bq286FEk6xzSfXk/pay\","
                + "\"resource\":\"" + RESOURCE_LINK + "\","
                + "\"self\":\"payments/Bq286FEk6xzSfXk\""
                + "},"
                + "\"payment_method\":\"credit-card\","
                + "\"reference\":\"Register_ACSP_174365-968117-586962\","
                + "\"status\":\"paid\","
                + "\"etag\":\"34e92e90a981a9686b45a56204e98d7d1fef86bbb446bf0c2cf5c679\","
                + "\"kind\":\"payment-session#payment-session\","
                + "\"refunds\":["
                + "{"
                + "\"refund_id\":\"R123\","
                + "\"created_at\":\"2025-09-23T10:15:30.000Z\","
                + "\"amount\":1000,"
                + "\"status\":\"approved\","
                + "\"external_refund_url\":\"https://example.com/refund/R123\","
                + "\"refund_reference\":\"REF123\""
                + "},"
                + "{"
                + "\"refund_id\":\"R124\","
                + "\"created_at\":\"2025-09-22T09:10:25.000Z\","
                + "\"amount\":500,"
                + "\"status\":\"pending\","
                + "\"external_refund_url\":\"https://example.com/refund/R124\","
                + "\"refund_reference\":\"REF124\""
                + "}"
                + "]"
                + "}";
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        PaymentResponse paymentResponse = objectMapper.readValue(json, PaymentResponse.class);
        return getAPIResponse(paymentResponse);
    }
}
