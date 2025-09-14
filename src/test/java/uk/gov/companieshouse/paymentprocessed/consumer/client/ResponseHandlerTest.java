package uk.gov.companieshouse.paymentprocessed.consumer.client;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ResponseHandlerTest {

    private final ResponseHandler responseHandler = new ResponseHandler();

    private ApiErrorResponseException apiErrorResponseException;

    public static final String RESOURCE_ID = "P9hl8PrKRBk1Zmc";

    private static final String GET_PAYMENT_CALL = "Payments Consumer API GET Payment";
    private static final String PATCH_PAYMENT_CALL = "Payments Consumer API Patch Payment";


    @ParameterizedTest()
    @MethodSource("scenarios")
    void shouldHandleApiErrorResponseGetScenarios(HttpStatus apiResponseStatus, Class<RuntimeException> expectedException) {
        // given
        apiErrorResponseException = new ApiErrorResponseException(
                new HttpResponseException.Builder(apiResponseStatus.value(), "", new HttpHeaders()));

        // when
        Executable executable = () -> responseHandler.handle(GET_PAYMENT_CALL, RESOURCE_ID, apiErrorResponseException);

        // then
        assertThrows(expectedException, executable);
    }

    @ParameterizedTest()
    @MethodSource("scenarios")
    void shouldHandleApiErrorPatchResponseScenarios(HttpStatus apiResponseStatus, Class<RuntimeException> expectedException) {
        // given
        apiErrorResponseException = new ApiErrorResponseException(
                new HttpResponseException.Builder(apiResponseStatus.value(), "", new HttpHeaders()));

        // when
        Executable executable = () -> responseHandler.handle(PATCH_PAYMENT_CALL, RESOURCE_ID, apiErrorResponseException);

        // then
        assertThrows(expectedException, executable);
    }

    @Test
    void shouldHandleURIValidationGetException() {
        // given
        URIValidationException exception = new URIValidationException("Invalid URI");

        // when
        Executable executable = () -> responseHandler.handle(GET_PAYMENT_CALL, exception);

        // then
        assertThrows(NonRetryableException.class, executable);
    }

    @Test
    void shouldHandleURIValidationPatchException() {
        // given
        URIValidationException exception = new URIValidationException("Invalid URI");

        // when
        Executable executable = () -> responseHandler.handle(PATCH_PAYMENT_CALL, exception);

        // then
        assertThrows(NonRetryableException.class, executable);
    }

    private static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, NonRetryableException.class),
                Arguments.of(HttpStatus.CONFLICT, NonRetryableException.class),
                Arguments.of(HttpStatus.UNAUTHORIZED, RetryableException.class),
                Arguments.of(HttpStatus.FORBIDDEN, RetryableException.class),
                Arguments.of(HttpStatus.NOT_FOUND, RetryableException.class),
                Arguments.of(HttpStatus.METHOD_NOT_ALLOWED, RetryableException.class),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, RetryableException.class),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, RetryableException.class)
        );
    }
}
