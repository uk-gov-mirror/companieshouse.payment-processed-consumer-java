package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import payments.payment_processed;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.RetryableException;
import uk.gov.companieshouse.paymentprocessed.consumer.service.PaymentProcessedServiceRouter;

@Component
public class Consumer {
    private final PaymentProcessedServiceRouter paymentProcessedServiceRouter;

    private final MessageFlags messageFlags;

    public Consumer(PaymentProcessedServiceRouter paymentProcessedServiceRouter, MessageFlags messageFlags) {
        this.paymentProcessedServiceRouter = paymentProcessedServiceRouter;
        this.messageFlags = messageFlags;
    }

    @KafkaListener(
            id = "${payment.processed.topic}-consumer",
            containerFactory = "paymentProcessedKafkaListenerContainerFactory",
            topics = "${payment.processed.topic}",
            groupId = "${payment.processed.group.name}"
    )
    public void consume(Message<payment_processed> message) {
        try {
            paymentProcessedServiceRouter.route(message);
        } catch (RetryableException ex) {
            messageFlags.setRetryable(true);
            throw ex;
        }
    }
}

