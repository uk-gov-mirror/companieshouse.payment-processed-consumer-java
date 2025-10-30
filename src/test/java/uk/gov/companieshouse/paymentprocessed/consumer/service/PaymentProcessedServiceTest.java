package uk.gov.companieshouse.paymentprocessed.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import payments.payment_processed;
import uk.gov.companieshouse.api.payments.PaymentLinks;
import uk.gov.companieshouse.api.payments.PaymentPatchRequestApi;
import uk.gov.companieshouse.api.payments.PaymentResponse;
import uk.gov.companieshouse.api.payments.Refund;
import uk.gov.companieshouse.paymentprocessed.consumer.client.PaymentsProcessedApiClient;
import uk.gov.companieshouse.paymentprocessed.consumer.factory.PaymentPatchRequestApiFactoryImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProcessedServiceTest {

    @Mock
    private PaymentsProcessedApiClient paymentsProcessedApiClient;

    @Mock
    private PaymentPatchRequestApiFactoryImpl paymentPatchRequestApiFactoryImpl;

    @InjectMocks
    private PaymentProcessedService paymentProcessedService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProcessMessageSuccessfully() {
        // Arrange
        payment_processed paymentProcessed = mock(payment_processed.class);
        when(paymentProcessed.getPaymentResourceId()).thenReturn("payment123");
        when(paymentProcessed.getRefundId()).thenReturn(null);

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getStatus()).thenReturn("paid");
        when(paymentResponse.getCompletedAt()).thenReturn(OffsetDateTime.now());
        when(paymentResponse.getLinks()).thenReturn(mock(PaymentLinks.class));
        when(paymentResponse.getLinks().getResource()).thenReturn("http://example.com/resource");

        when(paymentsProcessedApiClient.getPayment("payment123")).thenReturn(Optional.of(paymentResponse));

        PaymentPatchRequestApi patchRequest = mock(PaymentPatchRequestApi.class);
        when(paymentPatchRequestApiFactoryImpl.createPaymentPatchRequest(anyString(), any(OffsetDateTime.class), anyString()))
                .thenReturn(patchRequest);

        // Act
        paymentProcessedService.processMessage(paymentProcessed);

        // Assert
        verify(paymentsProcessedApiClient).patchPayment("http://example.com/resource", patchRequest);
    }

    @Test
    void shouldProcessRefundSuccessfully() {
        // Arrange
        payment_processed paymentProcessed = mock(payment_processed.class);
        when(paymentProcessed.getPaymentResourceId()).thenReturn("payment123");
        when(paymentProcessed.getRefundId()).thenReturn("refund123");

        Refund refund = mock(Refund.class);
        when(refund.getRefundId()).thenReturn("refund123");
        when(refund.getStatus()).thenReturn("approved");
        when(refund.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(refund.getRefundReference()).thenReturn("REF123");

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getRefunds()).thenReturn(List.of(refund));
        when(paymentResponse.getLinks()).thenReturn(mock(PaymentLinks.class));
        when(paymentResponse.getLinks().getResource()).thenReturn("http://example.com/resource");

        when(paymentsProcessedApiClient.getPayment("payment123")).thenReturn(Optional.of(paymentResponse));

        PaymentPatchRequestApi patchRequest = mock(PaymentPatchRequestApi.class);
        when(paymentPatchRequestApiFactoryImpl.createPaymentPatchRequest(anyString(), any(OffsetDateTime.class), anyString()))
                .thenReturn(patchRequest);

        // Act
        paymentProcessedService.processMessage(paymentProcessed);

        // Assert
        verify(paymentsProcessedApiClient).patchPayment("http://example.com/resource/refunds", patchRequest);
    }

    @Test
    void shouldLogAndSkipWhenPaymentNotFound() {
        // Arrange
        payment_processed paymentProcessed = mock(payment_processed.class);
        when(paymentProcessed.getPaymentResourceId()).thenReturn("payment123");

        when(paymentsProcessedApiClient.getPayment("payment123")).thenReturn(Optional.empty());

        // Act
        paymentProcessedService.processMessage(paymentProcessed);

        // Assert
        verify(paymentsProcessedApiClient, never()).patchPayment(anyString(), any());
    }
}