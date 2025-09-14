package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import payments.payment_processed;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.payments.PaymentResponse;
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.companieshouse.paymentprocessed.consumer.kafka.KafkaUtils.ERROR_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.kafka.KafkaUtils.INVALID_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.kafka.KafkaUtils.MAIN_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.kafka.KafkaUtils.RETRY_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.GET_URI;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.RESOURCE_LINK;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentProcessed;

@SpringBootTest(properties = {
        "payments.api.url=http://localhost:8889",
})
@WireMockTest(httpPort = 8889)
class ConsumerPositiveIT extends AbstractKafkaIT {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;
    @Autowired
    private KafkaProducer<String, byte[]> testProducer;
    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void shouldConsumePaymentProcessMessageSuccessfully() throws Exception {

        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = new ReflectDatumWriter<>(payment_processed.class);
        writer.write(getPaymentProcessed(), encoder);
        ApiResponse<PaymentResponse> apiResponse = TestUtils.getPaymentResponse();
        ObjectMapper objectMapper = TestUtils.getObjectMapper();
        apiResponse.getData().getLinks().setResource("http://localhost" + ":8889" + RESOURCE_LINK);
        String apiResponseJson = objectMapper.writeValueAsString(apiResponse.getData());
        stubFor(get(GET_URI)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(apiResponseJson)
                ));

        stubFor(patch(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                ));

        // when
        testProducer.send(new ProducerRecord<>(MAIN_TOPIC, 0, System.currentTimeMillis(),
                "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(10L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 1);
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, RETRY_TOPIC)).isZero();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, ERROR_TOPIC)).isZero();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, INVALID_TOPIC)).isZero();
        verify(getRequestedFor(urlEqualTo(GET_URI)));
        verify(patchRequestedFor(urlEqualTo(RESOURCE_LINK)));
    }

    @Test
    void shouldConsumePaymentProcessMessageRefundSuccessfully() throws Exception {

        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = new ReflectDatumWriter<>(payment_processed.class);
        payment_processed paymentProcessed = getPaymentProcessed();
        paymentProcessed.setRefundId("R123");
        writer.write(paymentProcessed, encoder);
        ApiResponse<PaymentResponse> apiResponse = TestUtils.getPaymentResponseRefund();
        ObjectMapper objectMapper = TestUtils.getObjectMapper();
        apiResponse.getData().getLinks().setResource("http://localhost" + ":8889" + RESOURCE_LINK);
        String apiResponseJson = objectMapper.writeValueAsString(apiResponse.getData());
        stubFor(get(GET_URI)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(apiResponseJson)
                ));

        stubFor(patch(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                ));

        // when
        testProducer.send(new ProducerRecord<>(MAIN_TOPIC, 0, System.currentTimeMillis(),
                "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(10, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 1);
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, RETRY_TOPIC)).isZero();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, ERROR_TOPIC)).isZero();
        assertThat(KafkaUtils.noOfRecordsForTopic(consumerRecords, INVALID_TOPIC)).isZero();
        verify(getRequestedFor(urlEqualTo(GET_URI)));
        verify(patchRequestedFor(urlEqualTo(RESOURCE_LINK + "/refunds")));
    }
}