package com.bankify.transactionservice;

import com.bankify.transactionservice.entity.TransactionType;
import com.bankify.transactionservice.repository.TransactionTypeRepository;
import com.bankify.transactionservice.service.TransactionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import java.util.List;

@SpringBootApplication
public class TransactionserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionserviceApplication.class, args);
    }

    /** Makes sure the three built-in transaction types exist. */
    @Bean
    ApplicationRunner seedTransactionTypes(TransactionTypeRepository types) {
        return args -> List.of(TransactionService.DEPOSIT, TransactionService.WITHDRAWAL, TransactionService.TRANSFER)
                .forEach(name -> {
                    if (types.findByTypeName(name).isEmpty()) {
                        types.save(new TransactionType(name, name.charAt(0) + name.substring(1).toLowerCase()));
                    }
                });
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.connectTimeout(Duration.ofSeconds(2)).readTimeout(Duration.ofSeconds(5)).build();
    }
}
