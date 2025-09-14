package uk.gov.companieshouse.paymentprocessed.consumer.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import payments.payment_processed;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

@Component
@Aspect
class LoggingKafkaListenerAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String LOG_MESSAGE_RECEIVED = "Processing message";
    private static final String LOG_MESSAGE_PROCESSED = "Processed message";
    private static final String EXCEPTION_MESSAGE = "%s exception thrown";

    private final int maxAttempts;

    LoggingKafkaListenerAspect(@Value("${maximum.retry.attempts}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object manageStructuredLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        int retryCount = 0;
        try {
            Message<?> message = (Message<?>) joinPoint.getArgs()[0];
            MessageHeaders headers = message.getHeaders();
            if (headers.get(DEFAULT_HEADER_ATTEMPTS) != null) {
                retryCount = Optional.of(headers.get(DEFAULT_HEADER_ATTEMPTS))
                        .map(attempts -> ByteBuffer.wrap((byte[]) attempts).getInt())
                        .orElse(1) - 1;
            }
            DataMapHolder.initialise(
                    Optional.ofNullable(extractContextId(message.getPayload()))
                            .orElse(UUID.randomUUID().toString()));

            DataMapHolder.get()
                    .retryCount(retryCount)
                    .topic((String) headers.get(RECEIVED_TOPIC))
                    .partition((Integer) headers.get(RECEIVED_PARTITION))
                    .offset((Long) headers.get(OFFSET));

            LOGGER.info(LOG_MESSAGE_RECEIVED, DataMapHolder.getLogMap());

            Object result = joinPoint.proceed();

            LOGGER.info(LOG_MESSAGE_PROCESSED, DataMapHolder.getLogMap());

            return result;
        } catch (RetryableException ex) {
            // maxAttempts includes first attempt which is not a retry
            if (retryCount >= maxAttempts - 1) {
                LOGGER.error("Max retry attempts reached", ex, DataMapHolder.getLogMap());
            } else {
                LOGGER.info(EXCEPTION_MESSAGE.formatted(ex.getClass().getSimpleName()), DataMapHolder.getLogMap());
            }
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Exception thrown", ex, DataMapHolder.getLogMap());
            throw ex;
        } finally {
            DataMapHolder.clear();
        }
    }

    private String extractContextId(Object payload) {
        if (payload instanceof payment_processed data) {
            Map<String, Object> logmap = DataMapHolder.getLogMap();
            logmap.put("payment_resource_id", data.getPaymentResourceId());
            return data.getPaymentResourceId();
        }
        String errorMessage = "Invalid payload type, payload: [%s]".formatted(payload.toString());
        LOGGER.error(errorMessage, DataMapHolder.getLogMap());
        throw new NonRetryableException(errorMessage);
    }
}