package com.innowise.orderservice.kafka.producer;

import com.innowise.orderservice.dto.order.OrderCreatedEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEventDto> kafkaTemplate;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    public void sendOrderCreatedEvent(OrderCreatedEventDto event) {
        log.info("Sending ORDER_CREATED event to Kafka: orderId={}, userId={}, amount={}",
                event.orderId(), event.userId(), event.totalAmount());

        try {
            String key = event.orderId().toString();

            CompletableFuture<SendResult<String, OrderCreatedEventDto>> future =
                    kafkaTemplate.send(orderEventsTopic, key, event);

            SendResult<String, OrderCreatedEventDto> result = future.get(10, TimeUnit.SECONDS);

            log.info("ORDER_CREATED event sent successfully: orderId={}, partition={}, offset={}",
                    event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Failed to send ORDER_CREATED event for orderId={}: {}",
                  event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send order event to Kafka", e);
        }
    }
}
