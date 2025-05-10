package com.sajal.outbox_pattern.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajal.outbox_pattern.dto.OrderRequest;
import com.sajal.outbox_pattern.model.Order;
import com.sajal.outbox_pattern.model.OutboxEvent;
import com.sajal.outbox_pattern.repository.OrderRepository;
import com.sajal.outbox_pattern.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(OrderRequest orderRequest) {
        // 1. Create and save the order
        Order order = Order.builder()
                .customerName(orderRequest.customerName())
                .totalAmount(orderRequest.totalAmount())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 2. Create an outbox event in the same transaction
        createOutboxEvent("ORDER", savedOrder.getId().toString(), "ORDER_CREATED", savedOrder);

        log.info("Order created with ID: {} and outbox event recorded", savedOrder.getId());
        return savedOrder;
    }

    @Transactional
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private void createOutboxEvent(String aggregateType, String aggregateId, String eventType, Object  payload) {
        try {
            String payLoad = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payLoad)
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for outbox event", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}
