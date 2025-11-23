package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.repository.ItemRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.hamcrest.Matchers;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("ItemController integration tests")
public class ItemControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${service.api.key}")
    private String serviceApiKey;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Test POST /api/v1/items")
    class CreateItemTests {
        @Test
        @DisplayName("should successfully create item when authenticated as admin")
        void shouldCreateItem_WhenAuthenticatedAsAdmin() throws Exception {
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("1500.00"))
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.price").value(1500.00))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ItemResponseDto createdItem = objectMapper.readValue(responseBody, ItemResponseDto.class);

            Item itemInDb = itemRepository.findById(createdItem.id()).orElseThrow();

            assertThat(itemInDb.getName()).isEqualTo("Laptop");
            assertThat(itemInDb.getPrice()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("should return 403 when user tries to create item")
        void shouldReturn403_WhenUserTriesToCreateItem() throws Exception {
            String userToken = generateToken(2L, "user@example.com", "USER");

            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("1500.00"))
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + userToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when invalid data")
        void shouldReturn400_WhenInvalidData() throws Exception {
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("L") // Too short name
                    .price(new BigDecimal("-100.00")) // Negative price
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("should return 409 when item name already exists")
        void shouldReturn409_WhenItemNameExists() throws Exception {
            createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("2000.00"))
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")));
        }

        @Test
        @DisplayName("should return 403 when no service key provided")
        void shouldReturn403_WhenNoServiceKey() throws Exception {
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("1500.00"))
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/items/{id}")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item by id without authentication")
        void shouldGetItemById_WithoutAuthentication() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            mockMvc.perform(get("/api/v1/items/{id}", item.getId())
                            .header("X-Service-Key", serviceApiKey))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(item.getId()))
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.price").value(1500.00));
        }

        @Test
        @DisplayName("should return 404 when item doesn't exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            mockMvc.perform(get("/api/v1/items/{id}", 999L)
                            .header("X-Service-Key", serviceApiKey))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("Item")));
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/items")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items without authentication")
        void shouldGetAllItems_WithoutAuthentication() throws Exception {
            createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            createAndSaveItem("Mouse", new BigDecimal("25.00"));
            createAndSaveItem("Keyboard", new BigDecimal("75.00"));

            mockMvc.perform(get("/api/v1/items")
                            .header("X-Service-Key", serviceApiKey)
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should return empty page when no items exist")
        void shouldReturnEmptyPage_WhenNoItems() throws Exception {
            mockMvc.perform(get("/api/v1/items")
                            .header("X-Service-Key", serviceApiKey))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/items/search")
    class SearchItemsTests {

        @Test
        @DisplayName("should search items by name without authentication")
        void shouldSearchItemsByName_WithoutAuthentication() throws Exception {
            createAndSaveItem("Laptop ASUS", new BigDecimal("1500.00"));
            createAndSaveItem("Laptop Lenovo", new BigDecimal("1200.00"));
            createAndSaveItem("Mouse", new BigDecimal("25.00"));

            mockMvc.perform(get("/api/v1/items/search")
                            .header("X-Service-Key", serviceApiKey)
                            .param("name", "Laptop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value(Matchers.containsString("Laptop")))
                    .andExpect(jsonPath("$[1].name").value(Matchers.containsString("Laptop")));
        }

        @Test
        @DisplayName("should return empty list when no matching items")
        void shouldReturnEmptyList_WhenNoMatchingItems() throws Exception {
            createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            mockMvc.perform(get("/api/v1/items/search")
                            .header("X-Service-Key", serviceApiKey)
                            .param("name", "Phone"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Test PUT /api/v1/items/{id}")
    class UpdateItemTests {

        @Test
        @DisplayName("should successfully update item when authenticated as admin")
        void shouldUpdateItem_WhenAuthenticatedAsAdmin() throws Exception {
            Item existingItem = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            ItemRequestDto updateDto = ItemRequestDto.builder()
                    .name("Laptop ASUS")
                    .price(new BigDecimal("2500.00"))
                    .build();

            mockMvc.perform(put("/api/v1/items/{id}", existingItem.getId())
                            // ИСПРАВЛЕНО: добавлен пробел после Bearer
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(existingItem.getId()))
                    .andExpect(jsonPath("$.name").value("Laptop ASUS"))
                    .andExpect(jsonPath("$.price").value(2500.00));

            Item updatedItem = itemRepository.findById(existingItem.getId()).orElseThrow();
            assertThat(updatedItem.getName()).isEqualTo("Laptop ASUS");
            assertThat(updatedItem.getPrice()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }

        @Test
        @DisplayName("should return 403 when user tries to update item")
        void shouldReturn403_WhenRegularUserTriesToUpdate() throws Exception {
            Item existingItem = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            String userToken = generateToken(2L, "user@example.com", "USER");

            ItemRequestDto updateDto = ItemRequestDto.builder()
                    .name("Laptop Pro")
                    .price(new BigDecimal("2500.00"))
                    .build();

            mockMvc.perform(put("/api/v1/items/{id}", existingItem.getId())
                            // ИСПРАВЛЕНО: добавлен пробел после Bearer
                            .header("Authorization", "Bearer " + userToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when item doesnt exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            ItemRequestDto updateDto = ItemRequestDto.builder()
                    .name("Laptop Pro")
                    .price(new BigDecimal("2500.00"))
                    .build();

            mockMvc.perform(put("/api/v1/items/{id}", 999L)
                            // ИСПРАВЛЕНО: добавлен пробел после Bearer
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Test DELETE /api/v1/items/{id}")
    class DeleteItemTests {

        @Test
        @DisplayName("should successfully delete item when authenticated as admin")
        void shouldDeleteItem_WhenAuthenticatedAsAdmin() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            mockMvc.perform(delete("/api/v1/items/{id}", item.getId())
                            // ИСПРАВЛЕНО: добавлен пробел после Bearer
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey))
                    .andExpect(status().isNoContent());

            assertThat(itemRepository.findById(item.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return 403 when regular user tries to delete item")
        void shouldReturn403_WhenRegularUserTriesToDelete() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));
            String userToken = generateToken(2L, "user@example.com", "USER");

            mockMvc.perform(delete("/api/v1/items/{id}", item.getId())
                            // ИСПРАВЛЕНО: добавлен пробел после Bearer
                            .header("Authorization", "Bearer " + userToken)
                            .header("X-Service-Key", serviceApiKey))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when item doesn't exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            String adminToken = generateToken(1L, "admin@example.com", "ADMIN");

            mockMvc.perform(delete("/api/v1/items/{id}", 999L)
                            // ИСПРАВЛЕНО: добавлен пробел после Bearer
                            .header("Authorization", "Bearer " + adminToken)
                            .header("X-Service-Key", serviceApiKey))
                    .andExpect(status().isNotFound());
        }
    }

    private String generateToken(Long userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        long expirationTime = 1000 * 60 * 60;

        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    private Item createAndSaveItem(String name, BigDecimal price) {
        Item item = Item.builder()
                .name(name)
                .price(price)
                .build();
        return itemRepository.save(item);
    }
}