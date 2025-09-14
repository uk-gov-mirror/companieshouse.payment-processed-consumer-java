package uk.gov.companieshouse.paymentprocessed.consumer.kafka;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class TestConsumerAspect {

    private final int steps;
    private CountDownLatch latch;

    public TestConsumerAspect(@Value("${steps:1}") int steps) {
        this.steps = steps;
        this.latch = new CountDownLatch(steps);
    }

    @After("execution(* Consumer.consume(..))")
    void afterConsume(JoinPoint joinPoint) {
        System.out.println("TestConsumerAspect: afterConsume called");
        System.out.println("  Thread: " + Thread.currentThread().getName());
        System.out.println("  Arguments: " + java.util.Arrays.toString(joinPoint.getArgs()));
        System.out.println("  Latch count before: " + latch.getCount());
        latch.countDown();
        System.out.println("  Latch count after: " + latch.getCount());
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void resetLatch() {
        latch = new CountDownLatch(steps);
    }
}
