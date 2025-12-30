package com.innowise.orderservice.kafka.producer;

import com.innowise.orderservice.dto.order.OrderCreatedEventDto;
import com.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

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

            future.whenComplete((r, e) -> {
                if (e != null) {
                    log.error("Failed to send ORDER_CREATED event for orderId={}: {}",
                            event.orderId(), e.getMessage() ,e);
                } else {
                    log.info("ORDER_CREATED event sent successfully: orderId={}, partition={}, offset={}",
                            event.orderId(),
                            r.getRecordMetadata().partition(),
                            r.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error sending ORDER_CREATED event for orderId={}: {}", event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send order event", e);
        }
    }
}
