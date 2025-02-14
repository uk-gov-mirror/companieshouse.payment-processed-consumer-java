package uk.gov.companieshouse.payment_processed_consumer;

import static uk.gov.companieshouse.payment_processed_consumer.environment.EnvironmentVariablesChecker.allRequiredEnvironmentVariablesPresent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentProcessedConsumerApplication {

	public static final String APPLICATION_NAME_SPACE = "payment-processed-consumer";

	public static void main(String[] args) {
			SpringApplication.run(PaymentProcessedConsumerApplication.class, args);
	}

}
