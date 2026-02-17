package com.resumebackend.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentOrchestrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestrationServiceApplication.class, args);
    }
}
