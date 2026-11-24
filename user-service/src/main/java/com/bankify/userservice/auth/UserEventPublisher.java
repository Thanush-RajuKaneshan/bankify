package com.bankify.userservice.auth;

import com.bankify.userservice.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Publishes user changes so other services can keep their own copy in their own database. */
@Component
public class UserEventPublisher {

    public static final String TOPIC = "user-events";
    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public UserEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void userRegistered(User user) {
        Map<String, Object> event = Map.of("type", "USER_REGISTERED",
                "userId", user.getUserId(), "username", user.getUsername());
        try {
            kafka.send(TOPIC, String.valueOf(user.getUserId()), event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Could not publish USER_REGISTERED for user {}", user.getUserId(), error);
                        }
                    });
        } catch (RuntimeException e) {
            // Kafka unreachable (Spring and the Kafka client throw unrelated types): sign-up and login still work, and the next login publishes again.
            log.error("Could not publish USER_REGISTERED for user {}", user.getUserId(), e);
        }
    }
}
