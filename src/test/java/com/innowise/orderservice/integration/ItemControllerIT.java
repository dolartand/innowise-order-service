package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.repository.ItemRepository;
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

import java.math.BigDecimal;

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
            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("1500.00"))
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/items")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
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
            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("1500.00"))
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "2")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when invalid data")
        void shouldReturn400_WhenInvalidData() throws Exception {
            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("L") // Too short name
                    .price(new BigDecimal("-100.00")) // Negative price
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
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

            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("2000.00"))
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")));
        }

        @Test
        @DisplayName("should return 403 when no service key provided")
        void shouldReturn403_WhenNoServiceKey() throws Exception {
            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Laptop")
                    .price(new BigDecimal("1500.00"))
                    .build();

            mockMvc.perform(post("/api/v1/items")
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
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
                            .header("X-Service-Key", TEST_SERVICE_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(item.getId()))
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.price").value(1500.00));
        }

        @Test
        @DisplayName("should return 404 when item doesn't exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            mockMvc.perform(get("/api/v1/items/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY))
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
                            .header("X-Service-Key", TEST_SERVICE_KEY)
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
                            .header("X-Service-Key", TEST_SERVICE_KEY))
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
                            .header("X-Service-Key", TEST_SERVICE_KEY)
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
                            .header("X-Service-Key", TEST_SERVICE_KEY)
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

            ItemRequestDto updateDto = ItemRequestDto.builder()
                    .name("Laptop ASUS")
                    .price(new BigDecimal("2500.00"))
                    .build();

            mockMvc.perform(put("/api/v1/items/{id}", existingItem.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
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

            ItemRequestDto updateDto = ItemRequestDto.builder()
                    .name("Laptop Pro")
                    .price(new BigDecimal("2500.00"))
                    .build();

            mockMvc.perform(put("/api/v1/items/{id}", existingItem.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "2")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when item doesnt exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            ItemRequestDto updateDto = ItemRequestDto.builder()
                    .name("Laptop Pro")
                    .price(new BigDecimal("2500.00"))
                    .build();

            mockMvc.perform(put("/api/v1/items/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN")
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

            mockMvc.perform(delete("/api/v1/items/{id}", item.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "admin@example.com")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());

            assertThat(itemRepository.findById(item.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return 403 when regular user tries to delete item")
        void shouldReturn403_WhenRegularUserTriesToDelete() throws Exception {
            Item item = createAndSaveItem("Laptop", new BigDecimal("1500.00"));

            mockMvc.perform(delete("/api/v1/items/{id}", item.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "2")
                            .header("X-User-Email", "user@example.com")
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when item doesn't exist")
        void shouldReturn404_WhenItemDoesntExist() throws Exception {
            mockMvc.perform(delete("/api/v1/items/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
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
}