package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.orderservice.dto.order.OrderItemRequestDto;
import com.innowise.orderservice.dto.order.OrderRequestDto;
import com.innowise.orderservice.dto.order.OrderResponseDto;
import com.innowise.orderservice.dto.order.OrderUpdateDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.enums.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("OrderController integration tests")
@Transactional
public class OrderControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        wireMockServer.resetAll();
    }

    @Nested
    @DisplayName("Test POST /api/v1/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("should successfully create order")
        void shouldCreateOrder_Success() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            stubUserServiceGetUserById(1L, true);

            OrderItemRequestDto itemDto = new OrderItemRequestDto(item.getId(), 2);
            OrderRequestDto requestDto = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(itemDto))
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalPrice").value(3000.00))
                    .andExpect(jsonPath("$.user").exists())
                    .andExpect(jsonPath("$.user.id").value(1))
                    .andExpect(jsonPath("$.user.name").value("John"))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andReturn();

            entityManager.flush();
            entityManager.clear();

            String responseBody = result.getResponse().getContentAsString();
            OrderResponseDto createdOrder = objectMapper.readValue(responseBody, OrderResponseDto.class);

            Order orderInDb = orderRepository.findById(createdOrder.id()).orElseThrow();
            assertThat(orderInDb.getUserId()).isEqualTo(1L);
            assertThat(orderInDb.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(orderInDb.getTotalPrice()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(orderInDb.getItems()).hasSize(1);

            wireMockServer.verify(WireMock.getRequestedFor(urlEqualTo("/api/v1/users/1")));
        }

        @Test
        @DisplayName("should return 400 when user is inactive")
        void shouldReturn400_WhenUserInactive() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            stubUserServiceGetUserById(1L, false);

            OrderItemRequestDto itemDto = new OrderItemRequestDto(item.getId(), 2);
            OrderRequestDto requestDto = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(itemDto))
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("inactive user")));
        }

        @Test
        @DisplayName("should return 404 when item doesn't exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            stubUserServiceGetUserById(1L, true);

            OrderItemRequestDto itemDto = new OrderItemRequestDto(999L, 2);
            OrderRequestDto requestDto = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(itemDto))
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("Item not found")));
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void shouldReturn400_WhenValidationFails() throws Exception {
            OrderRequestDto requestDto = OrderRequestDto.builder()
                    .userId(null)
                    .items(List.of())
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("should handle user service unavailable")
        void shouldHandleUserServiceUnavailable() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/1"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")));

            OrderItemRequestDto itemDto = new OrderItemRequestDto(item.getId(), 2);
            OrderRequestDto requestDto = OrderRequestDto.builder()
                    .userId(1L)
                    .items(List.of(itemDto))
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.name").value("Unavailable"));
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/orders/{id}")
    class GetOrderByIdTests {

        @Test
        @DisplayName("should return order by id when authenticated as owner")
        void shouldGetOrderById_WhenAuthenticatedAsOwner() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.user").exists());
        }

        @Test
        @DisplayName("should return order by id when authenticated as admin")
        void shouldGetOrderById_WhenAuthenticatedAsAdmin() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "2")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()));
        }

        @Test
        @DisplayName("should return 403 when user tries to access another user's order")
        void shouldReturn403_WhenUserTriesToAccessAnotherUsersOrder() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "2")
                            .header("X-User-Email", "another@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when order doesn't exist")
        void shouldReturn404_WhenOrderDoesntExist() throws Exception {
            mockMvc.perform(get("/api/v1/orders/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("Order not found")));
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/orders")
    class GetAllOrdersTests {

        @Test
        @DisplayName("should return all orders when authenticated as admin")
        void shouldGetAllOrders_WhenAuthenticatedAsAdmin() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            createAndSaveOrder(1L, item, 2);
            createAndSaveOrder(2L, item, 1);
            createAndSaveOrder(3L, item, 3);

            stubUserServiceGetUserById(1L, true);
            stubUserServiceGetUserById(2L, true);
            stubUserServiceGetUserById(3L, true);

            mockMvc.perform(get("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should return 403 when regular user tries to get all orders")
        void shouldReturn403_WhenRegularUserTriesToGetAllOrders() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should filter orders by status when authenticated as admin")
        void shouldFilterOrdersByStatus_WhenAuthenticatedAsAdmin() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order1 = createAndSaveOrder(1L, item, 2);
            order1.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(order1);

            Order order2 = createAndSaveOrder(2L, item, 1);
            order2.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(order2);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(get("/api/v1/orders")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("statuses", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/orders/user/{userId}")
    class GetOrdersByUserIdTests {

        @Test
        @DisplayName("should return user's orders when authenticated as owner")
        void shouldGetOrdersByUserId_WhenAuthenticatedAsOwner() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            createAndSaveOrder(1L, item, 2);
            createAndSaveOrder(1L, item, 1);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(get("/api/v1/orders/user/{userId}", 1L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].userId").value(1))
                    .andExpect(jsonPath("$.content[1].userId").value(1));
        }

        @Test
        @DisplayName("should return user's orders when authenticated as admin")
        void shouldGetOrdersByUserId_WhenAuthenticatedAsAdmin() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            createAndSaveOrder(1L, item, 2);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(get("/api/v1/orders/user/{userId}", 1L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("should return 403 when user tries to access another user's orders")
        void shouldReturn403_WhenUserTriesToAccessAnotherUsersOrders() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            createAndSaveOrder(1L, item, 2);

            mockMvc.perform(get("/api/v1/orders/user/{userId}", 1L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "2")
                            .header("X-User-Email", "another@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Test PUT /api/v1/orders/{id}")
    class UpdateOrderTests {

        @Test
        @DisplayName("should successfully update order when authenticated as admin")
        void shouldUpdateOrder_WhenAuthenticatedAsAdmin() throws Exception {
            Item item1 = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Item item2 = createAndSaveItem("Mouse", new BigDecimal("25.00"));
            Order order = createAndSaveOrder(1L, item1, 2);

            stubUserServiceGetUserById(1L, true);

            OrderItemRequestDto newItemDto = new OrderItemRequestDto(item2.getId(), 3);
            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .items(List.of(newItemDto))
                    .build();

            mockMvc.perform(put("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()))
                    .andExpect(jsonPath("$.status").value("PROCESSING"))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.totalPrice").value(75.00));

            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(updatedOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        }

        @Test
        @DisplayName("should return 403 when regular user tries to update order")
        void shouldReturn403_WhenRegularUserTriesToUpdate() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);

            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .items(List.of(new OrderItemRequestDto(item.getId(), 1)))
                    .build();

            mockMvc.perform(put("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when trying to update delivered order")
        void shouldReturn400_WhenTryingToUpdateDeliveredOrder() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .items(List.of(new OrderItemRequestDto(item.getId(), 1)))
                    .build();

            mockMvc.perform(put("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("Cannot update order")));
        }

        @Test
        @DisplayName("should return 404 when order doesn't exist")
        void shouldReturn404_WhenOrderDoesntExist() throws Exception {
            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .items(List.of(new OrderItemRequestDto(1L, 1)))
                    .build();

            mockMvc.perform(put("/api/v1/orders/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Test PATCH /api/v1/orders/{id}/status")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("should successfully update order status from PENDING to PROCESSING")
        void shouldUpdateStatus_FromPendingToProcessing() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(order);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("status", "PROCESSING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()))
                    .andExpect(jsonPath("$.status").value("PROCESSING"));

            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        }

        @Test
        @DisplayName("should successfully update order status from PROCESSING to SHIPPED")
        void shouldUpdateStatus_FromProcessingToShipped() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("status", "SHIPPED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));
        }

        @Test
        @DisplayName("should successfully update order status from SHIPPED to DELIVERED")
        void shouldUpdateStatus_FromShippedToDelivered() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);

            stubUserServiceGetUserById(1L, true);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("status", "DELIVERED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DELIVERED"));
        }

        @Test
        @DisplayName("should return 400 for invalid status transition")
        void shouldReturn400_ForInvalidStatusTransition() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(order);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("status", "DELIVERED"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("Invalid status transition")));
        }

        @Test
        @DisplayName("should return 400 when trying to change status of delivered order")
        void shouldReturn400_WhenChangingStatusOfDeliveredOrder() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .param("status", "PROCESSING"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when regular user tries to update status")
        void shouldReturn403_WhenRegularUserTriesToUpdateStatus() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .param("status", "PROCESSING"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Test DELETE /api/v1/orders/{id}")
    class DeleteOrderTests {

        @Test
        @DisplayName("should successfully soft delete order with PENDING status")
        void shouldSoftDeleteOrder_WithPendingStatus() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(order);

            mockMvc.perform(delete("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();

            Order deletedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(deletedOrder.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("should successfully soft delete order with CANCELLED status")
        void shouldSoftDeleteOrder_WithCancelledStatus() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            mockMvc.perform(delete("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when trying to delete order with PROCESSING status")
        void shouldReturn400_WhenDeletingProcessingOrder() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);
            order.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            mockMvc.perform(delete("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("Cannot delete order")));
        }

        @Test
        @DisplayName("should return 403 when regular user tries to delete order")
        void shouldReturn403_WhenRegularUserTriesToDelete() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            Order order = createAndSaveOrder(1L, item, 2);

            mockMvc.perform(delete("/api/v1/orders/{id}", order.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when order doesn't exist")
        void shouldReturn404_WhenOrderDoesntExist() throws Exception {
            mockMvc.perform(delete("/api/v1/orders/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "999")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNotFound());
        }
    }

    private Item createAndSaveItem(String name, BigDecimal price) {
        Item item = Item.builder()
                .name(name)
                .price(price)
                .build();
        return itemRepository.save(item);
    }

    private Order createAndSaveOrder(Long userId, Item item, int quantity) {
        Order order = Order.builder()
                .userId(userId)
                .orderStatus(OrderStatus.PENDING)
                .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .deleted(false)
                .build();

        Order savedOrder = orderRepository.save(order);

        com.innowise.orderservice.entity.OrderItem orderItem =
                com.innowise.orderservice.entity.OrderItem.builder()
                        .order(savedOrder)
                        .item(item)
                        .quantity(quantity)
                        .price(item.getPrice())
                        .build();

        savedOrder.getItems().add(orderItem);
        return orderRepository.save(savedOrder);
    }

    private void stubUserServiceGetUserById(Long userId, boolean active) {
        String userJson = String.format("""
                {
                    "id": %d,
                    "name": "John",
                    "surname": "Doe",
                    "birthDate": "1990-01-01",
                    "email": "john@example.com",
                    "active": %s,
                    "createdAt": "2024-01-01T10:00:00",
                    "updatedAt": "2024-01-01T10:00:00",
                    "cards": []
                }
                """, userId, active);

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(userJson)));
    }
}