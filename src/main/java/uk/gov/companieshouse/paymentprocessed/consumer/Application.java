package uk.gov.companieshouse.paymentprocessed.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static final String NAMESPACE = "payment-processed-consumer-java";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
