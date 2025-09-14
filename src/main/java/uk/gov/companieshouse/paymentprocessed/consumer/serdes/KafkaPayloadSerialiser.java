package uk.gov.companieshouse.paymentprocessed.consumer.serdes;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import payments.payment_processed;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.NonRetryableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class KafkaPayloadSerialiser implements Serializer<payment_processed> {


    @Override
    public byte[] serialize(String topic, payment_processed data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<payment_processed> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException e) {
            throw new NonRetryableException("Error serialising message payload", e);
        }
        return outputStream.toByteArray();
    }

    public DatumWriter<payment_processed> getDatumWriter() {
        return new ReflectDatumWriter<>(payment_processed.class);
    }
}
