package com.sajal.outbox_pattern.service;

import com.sajal.outbox_pattern.model.OutboxEvent;
import com.sajal.outbox_pattern.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessorService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.kafka.topic}")
    private String topicName;

    @Scheduled(fixedRateString = "${app.outbox.polling-interval-ms:5000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> unprocessedEvents = outboxEventRepository.findUnprocessedEvents();
        
        if (!unprocessedEvents.isEmpty()) {
            log.info("Found {} unprocessed outbox events", unprocessedEvents.size());
            
            for (OutboxEvent event : unprocessedEvents) {
                try {
                    // Send to Kafka
                    kafkaTemplate.send(topicName, event.getAggregateId(), event.getPayload());
                    
                    // Mark as processed
                    event.setProcessedAt(LocalDateTime.now());
                    outboxEventRepository.save(event);
                    
                    log.info("Processed outbox event: {}", event.getId());
                } catch (Exception e) {
                    log.error("Failed to process outbox event: {}", event.getId(), e);
                    // Don't update processedAt so it will be retried next time
                }
            }
        }
    }
}