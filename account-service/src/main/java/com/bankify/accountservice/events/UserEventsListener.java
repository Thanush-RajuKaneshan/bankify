package com.bankify.accountservice.events;

import com.bankify.accountservice.entity.AccountOwner;
import com.bankify.accountservice.repository.AccountOwnerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Each service owns its database, so account-service cannot join against user-service's tables.
 * Instead it keeps its own copy of registered users, updated from the user-events topic.
 */
@Component
public class UserEventsListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventsListener.class);

    private final AccountOwnerRepository owners;
    private final ObjectMapper json;

    public UserEventsListener(AccountOwnerRepository owners, ObjectMapper json) {
        this.owners = owners;
        this.json = json;
    }

    @KafkaListener(topics = "user-events")
    public void onUserEvent(String message) throws Exception {
        JsonNode event = json.readTree(message);
        if ("USER_REGISTERED".equals(event.path("type").asText())) {
            owners.save(new AccountOwner(event.path("userId").asLong(), event.path("username").asText()));
            log.info("Synced user {} from user-events", event.path("userId").asLong());
        }
    }
}
