package uk.gov.companieshouse.paymentprocessed.consumer.service;


import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import payments.payment_processed;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.logging.DataMapHolder;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

@Component
public class PaymentProcessedServiceRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final PaymentProcessedService paymentProcessedService;

    public PaymentProcessedServiceRouter(PaymentProcessedService paymentProcessedService) {
        this.paymentProcessedService = paymentProcessedService;
    }

    public void route(Message<payment_processed> message) {
        payment_processed paymentProcessed = message.getPayload();
        LOGGER.info("Received payment processed message for PaymentResourceId" + paymentProcessed.getPaymentResourceId(), DataMapHolder.getLogMap());
        message.getHeaders().forEach((key, value) -> LOGGER.info(" '%s' = %s".formatted(key, value), DataMapHolder.getLogMap()));
        paymentProcessedService.processMessage(paymentProcessed);
    }
}
