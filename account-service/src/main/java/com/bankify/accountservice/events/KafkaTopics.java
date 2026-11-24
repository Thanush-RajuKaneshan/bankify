package com.bankify.accountservice.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {

    /** Created on startup if missing, so the listener never subscribes to a topic that does not exist yet. */
    @Bean
    NewTopic userEvents() {
        return TopicBuilder.name("user-events").partitions(3).replicas(1).build();
    }
}
