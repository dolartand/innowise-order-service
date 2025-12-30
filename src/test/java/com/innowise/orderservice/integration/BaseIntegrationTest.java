package com.innowise.orderservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseIntegrationTest {

    public static final String TEST_SERVICE_KEY = "test-service-key";

    // Postgres
    protected static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    // Kafka (используем версию, совместимую с Kraft, чтобы не тянуть Zookeeper)
    protected static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    // WireMock для user-service
    protected static final WireMockServer wireMockServer;

    static {
        // Запуск контейнеров
        postgreSQLContainer.start();
        kafkaContainer.start();

        // Запуск WireMock
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // === DATASOURCE (из вашего yaml: spring.datasource) ===
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

        // === LIQUIBASE ===
        registry.add("spring.liquibase.enabled", () -> "true");

        // === KAFKA (из вашего yaml: spring.kafka) ===
        // 1. Адрес брокера (вместо ${KAFKA_BOOTSTRAP_SERVERS})
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);

        // 2. Group ID (вместо ${KAFKA_CONSUMER_GROUP_ID}) - Это исправит вашу ошибку!
        registry.add("spring.kafka.consumer.group-id", () -> "test-order-group");

        // 3. Читать сообщения с начала (важно для тестов)
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        // === KAFKA TOPICS (из вашего yaml: kafka.topics) ===
        // Чтобы резолвились плейсхолдеры ${KAFKA_TOPIC_...}
        registry.add("kafka.topics.order-events", () -> "test-order-events");
        registry.add("kafka.topics.payment-events", () -> "test-payment-events");

        // === USER SERVICE (из вашего yaml: user.service.url) ===
        registry.add("user.service.url", () -> "http://localhost:" + wireMockServer.port());

        // === RESILIENCE4J & FEIGN ===
        registry.add("resilience4j.circuitbreaker.instances.user-service.registerHealthIndicator", () -> "true");
        registry.add("feign.circuitbreaker.enabled", () -> "true");

        // === SERVICE KEY (из вашего yaml: service.api.key) ===
        registry.add("service.api.key", () -> TEST_SERVICE_KEY);
    }
}