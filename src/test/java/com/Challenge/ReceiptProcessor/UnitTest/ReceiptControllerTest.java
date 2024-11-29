package com.Challenge.ReceiptProcessor.UnitTest;

import com.Challenge.ReceiptProcessor.Controller.ReceiptController;
import com.Challenge.ReceiptProcessor.Entity.Receipt;
import com.Challenge.ReceiptProcessor.Service.ReceiptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ReceiptControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private ReceiptController receiptController;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(receiptController).build();
    }

    @Test
    void testProcessReceipt_ValidReceipt() throws Exception {
        // Arrange
        String receiptJson = "{\n" +
                "  \"retailer\": \"Target\",\n" +
                "  \"purchaseDate\": \"2022-01-01\",\n" +
                "  \"purchaseTime\": \"13:01\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shortDescription\": \"Mountain Dew 12PK\",\n" +
                "      \"price\": \"6.49\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"shortDescription\": \"Emils Cheese Pizza\",\n" +
                "      \"price\": \"12.25\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"shortDescription\": \"Knorr Creamy Chicken\",\n" +
                "      \"price\": \"1.26\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"shortDescription\": \"Doritos Nacho Cheese\",\n" +
                "      \"price\": \"3.35\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"shortDescription\": \"Klarbrunn 12-PK 12 FL OZ\",\n" +
                "      \"price\": \"12.00\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": \"35.35\"\n" +
                "}";

        UUID receiptId = UUID.randomUUID();

        when(receiptService.processReceipt(any(Receipt.class))).thenReturn(receiptId);

        // Act & Assert
        mockMvc.perform(post("/receipts/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receiptJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(receiptId.toString()));

        verify(receiptService, times(1)).processReceipt(any(Receipt.class));
    }

    @Test
    void testProcessReceipt_InvalidReceipt() throws Exception {
        // Arrange
        String invalidReceiptJson = "{\n" +
                "  \"retailer\": \"\",\n" +
                "  \"items\": [],\n" +
                "  \"total\": \"\"\n" +
                "}"; // Missing required fields or invalid values

        // Act & Assert
        mockMvc.perform(post("/receipts/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReceiptJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(receiptService);
    }

    @Test
    void testGetPoints_ValidId() throws Exception {
        // Arrange
        UUID receiptId = UUID.randomUUID();
        int points = 32;

        when(receiptService.getPoints(receiptId)).thenReturn(points);

        // Act & Assert
        mockMvc.perform(get("/receipts/{id}/points", receiptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(points));

        verify(receiptService, times(1)).getPoints(receiptId);
    }

    @Test
    void testGetPoints_InvalidId() throws Exception {
        // Arrange
        String invalidId = "invalid-uuid";

        // Act & Assert
        mockMvc.perform(get("/receipts/{id}/points", invalidId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(receiptService);
    }
}
