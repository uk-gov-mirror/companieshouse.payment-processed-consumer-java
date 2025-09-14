package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

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
import uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.companieshouse.paymentprocessed.consumer.kafka.KafkaUtils.MAIN_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.kafka.KafkaUtils.RETRY_TOPIC;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.GET_URI;

@SpringBootTest(properties = {
        "consumer.max-attempts=2",
        "consumer.backoff-delay=50",
        "payments.api.url=http://localhost:8889"

})
@WireMockTest(httpPort = 8889)
class ConsumerRetryableException500ErrorIT extends AbstractKafkaIT {

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
    void shouldFailConsumePaymentProcessMessage() throws Exception {

        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = new ReflectDatumWriter<>(payment_processed.class);
        writer.write(TestUtils.getPaymentProcessed(), encoder);

        stubFor(get(urlEqualTo(GET_URI))
                .willReturn(aResponse()
                        .withStatus(500)));

        // when
        testProducer.send(new ProducerRecord<>(MAIN_TOPIC, 0, System.currentTimeMillis(),
                "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(10, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        // then
        verify(getRequestedFor(urlEqualTo(GET_URI)));

        int totalCount = 0;
        long timeout = System.currentTimeMillis() + 2000; // 2 seconds
        while (System.currentTimeMillis() < timeout) {
            ConsumerRecords<?, ?> records = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(200), 1);
            totalCount += KafkaUtils.noOfRecordsForTopic(records, RETRY_TOPIC);
            if (totalCount >= 1) break;
        }
        assertThat(totalCount).isEqualTo(1);
    }
}
