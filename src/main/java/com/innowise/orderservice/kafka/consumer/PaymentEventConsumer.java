package com.innowise.orderservice.kafka.consumer;

import com.innowise.orderservice.dto.payment.PaymentEventDto;
import com.innowise.orderservice.enums.OrderStatus;
import com.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            @Payload PaymentEventDto event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received PAYMENT_CREATED event: orderId={}, userId={}, partition={}, offset={}",
                event.orderId(), event.userId(), partition, offset);

        try {
            OrderStatus newOrderStatus = switch (event.status()) {
                case SUCCESS -> {
                    log.info("Payment successful for orderId={}, updating order to PROCESSING status",
                            event.orderId());
                    yield OrderStatus.PROCESSING;
                }
                case FAILED -> {
                    log.warn("Payment failed for orderId={}, updating order to CANCELLED status",
                            event.orderId());
                    yield OrderStatus.CANCELLED;
                }
                default -> {
                    log.warn("Unexpected order status {} for orderId={}, skipping update",
                            event.status(), event.orderId());
                    acknowledgment.acknowledge();
                    yield null;
                }
            };

            if (newOrderStatus != null) {
                orderService.updateOrderStatus(event.orderId(), newOrderStatus);
                log.info("Order status updated successfully: orderId={}, new status={}",
                        event.orderId(), newOrderStatus);
            }

            acknowledgment.acknowledge();
            log.info("PAYMENT_CREATED event processed successfully for orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Error processing PAYMENT_CREATED event for orderId={}", event.orderId(), e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }
}
