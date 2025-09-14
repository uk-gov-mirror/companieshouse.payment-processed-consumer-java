package uk.gov.companieshouse.paymentprocessed.consumer.serdes;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import payments.payment_processed;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.InvalidPayloadException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.paymentprocessed.consumer.utils.TestUtils.getPaymentProcessed;

class PaymentProcessedDeserializerTest {


    @Test
    void testShouldSuccessfullyDeserializePaymentProcessed() throws IOException {
        // given
        payment_processed paymentProcessed = getPaymentProcessed();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = new ReflectDatumWriter<>(payment_processed.class);
        writer.write(paymentProcessed, encoder);
        try (KafkaPayloadDeserialiser deserialiser = new KafkaPayloadDeserialiser()) {
            payment_processed actual = deserialiser.deserialize("topic", outputStream.toByteArray());
            assertThat(actual, is(equalTo(paymentProcessed)));
        }
    }

    @Test
    void testDeserializeThrowsInvalidException() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        try (KafkaPayloadDeserialiser deserialiser = new KafkaPayloadDeserialiser()) {
            Executable actual = () -> deserialiser.deserialize("topic", outputStream.toByteArray());
            InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
            assertThat(exception.getMessage(), is(equalTo("Invalid payload: [\nhello] was provided.")));
            assertThat(exception.getCause(), is(CoreMatchers.instanceOf(IOException.class)));
        }
    }
}
