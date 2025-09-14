package uk.gov.companieshouse.paymentprocessed.consumer.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import payments.payment_processed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProcessedServiceRouterTest {

    private PaymentProcessedService paymentProcessedService;
    private PaymentProcessedServiceRouter paymentProcessedServiceRouter;

    @BeforeEach
    void setUp() {
        paymentProcessedService = mock(PaymentProcessedService.class);
        paymentProcessedServiceRouter = new PaymentProcessedServiceRouter(paymentProcessedService);
    }

    @Test
    void route_shouldCallCreatePaymentProcessedService() {
        Message<payment_processed> message = mock(Message.class);
        payment_processed mockPayload = mock(payment_processed.class);
        when(message.getPayload()).thenReturn(mockPayload);
        MessageHeaders mockHeaders = mock(MessageHeaders.class);

        when(message.getHeaders()).thenReturn(mockHeaders);
        paymentProcessedServiceRouter.route(message);

        verify(paymentProcessedService, times(1)).processMessage(mockPayload);
    }
}