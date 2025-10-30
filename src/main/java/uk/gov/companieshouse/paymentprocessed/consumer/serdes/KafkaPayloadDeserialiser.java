package uk.gov.companieshouse.paymentprocessed.consumer.serdes;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import payments.payment_processed;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.paymentprocessed.consumer.exception.InvalidPayloadException;

import java.io.IOException;

import static uk.gov.companieshouse.paymentprocessed.consumer.Application.NAMESPACE;

public class KafkaPayloadDeserialiser implements Deserializer<payment_processed> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);


    @Override
    public payment_processed deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<payment_processed> reader = new ReflectDatumReader<>(payment_processed.class);
            return reader.read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            LOGGER.error("Error deserialising message payload.", e);
            throw new InvalidPayloadException(String.format("Invalid payload: [%s] was provided.", new String(data)), e);
        }
    }
}
