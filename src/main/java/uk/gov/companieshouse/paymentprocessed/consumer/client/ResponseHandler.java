package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;

import java.util.Arrays;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

@Component
public class ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String API_INFO_RESPONSE_MESSAGE = "%s failed, resource URI: %s, status code: %d. %s";
    private static final String API_ERROR_RESPONSE_MESSAGE = "%s failed, resource URI: %s, status code: %d.";
    private static final String URI_VALIDATION_EXCEPTION_MESSAGE = "%s failed due to invalid URI";
    private static final String JSON_PARSE_EXCEPTION_MESSAGE = "%s failed due  to unable to parse response for resource ID: %s";

    public void handle(String apiCall, URIValidationException ex) {
        LOGGER.error(String.format(URI_VALIDATION_EXCEPTION_MESSAGE, apiCall), ex, DataMapHolder.getLogMap());
        throw new NonRetryableException(String.format(URI_VALIDATION_EXCEPTION_MESSAGE, apiCall), ex);
    }

    public void handle(String apiCall, String resourceUri, ApiErrorResponseException ex) {
        final int statusCode = ex.getStatusCode();
        if (HttpStatus.BAD_REQUEST.value() == statusCode || HttpStatus.CONFLICT.value() == statusCode) {
            LOGGER.error(String.format(API_ERROR_RESPONSE_MESSAGE, apiCall, resourceUri, statusCode),
                    ex, DataMapHolder.getLogMap());
            throw new NonRetryableException(String.format(API_ERROR_RESPONSE_MESSAGE, apiCall, resourceUri, statusCode), ex);
        } else {
            LOGGER.info(
                    String.format(API_INFO_RESPONSE_MESSAGE, apiCall, resourceUri, ex.getStatusCode(),
                            Arrays.toString(ex.getStackTrace())),
                    DataMapHolder.getLogMap());
            throw new RetryableException(String.format(API_ERROR_RESPONSE_MESSAGE, apiCall, resourceUri, statusCode), ex);
        }
    }

    public void handle(String apiCall, String resourceUri, JsonProcessingException ex) {
        LOGGER.error(String.format(JSON_PARSE_EXCEPTION_MESSAGE, apiCall, resourceUri),
                ex, DataMapHolder.getLogMap());
        throw new NonRetryableException(String.format(JSON_PARSE_EXCEPTION_MESSAGE, apiCall, resourceUri), ex);
    }

}
