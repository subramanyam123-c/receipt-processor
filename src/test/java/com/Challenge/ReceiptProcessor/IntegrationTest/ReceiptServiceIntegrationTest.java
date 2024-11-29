package com.Challenge.ReceiptProcessor.IntegrationTest;

import com.Challenge.ReceiptProcessor.Entity.Item;
import com.Challenge.ReceiptProcessor.Entity.Receipt;
import com.Challenge.ReceiptProcessor.Exception.ReceiptNotFoundException;
import com.Challenge.ReceiptProcessor.Repository.ReceiptRepository;
import com.Challenge.ReceiptProcessor.Service.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class ReceiptServiceIntegrationTest {

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private ReceiptRepository receiptRepository;

    private Receipt validReceipt;

    @BeforeEach
    void setUp() {
        // Prepare a valid receipt object
        Item item1 = new Item(null, "Milk", BigDecimal.valueOf(3.50), null);
        Item item2 = new Item(null, "Bread", BigDecimal.valueOf(2.00), null);

        validReceipt = new Receipt();
        validReceipt.setRetailer("GroceryStore");
        validReceipt.setPurchaseDate(LocalDate.of(2022, 11, 27));
        validReceipt.setPurchaseTime(LocalTime.of(15, 0)); // 3:00 PM
        validReceipt.setTotal(BigDecimal.valueOf(5.50));
        validReceipt.setItems(Arrays.asList(item1, item2));
    }

    @Test
    void testProcessReceipt_Success() {
        // Act: Process the receipt and retrieve its ID
        UUID receiptId = receiptService.processReceipt(validReceipt);

        // Assert: Verify the receipt is saved in the database
        assertNotNull(receiptId);
        assertTrue(receiptRepository.findById(receiptId).isPresent());
    }

    @Test
    void testProcessReceipt_InvalidTotal() {
        // Arrange: Set an invalid total
        validReceipt.setTotal(BigDecimal.ZERO);

        // Act & Assert: Ensure an InvalidReceiptException is thrown
        Exception exception = assertThrows(RuntimeException.class, () -> {
            receiptService.processReceipt(validReceipt);
        });

        assertEquals("The receipt is invalid : Total amount must be greater than zero", exception.getMessage());
    }

    @Test
    void testGetPoints_ValidReceipt() {
        // Arrange: Save the receipt
        UUID receiptId = receiptService.processReceipt(validReceipt);

        // Act: Retrieve points for the saved receipt
        int points = receiptService.getPoints(receiptId);

        assertEquals(58, points); // Total = 58
    }

    @Test
    void testGetPoints_InvalidReceiptId() {
        // Arrange: Use a non-existent receipt ID
        UUID invalidId = UUID.randomUUID();

        // Act & Assert: Ensure ReceiptNotFoundException is thrown
        Exception exception = assertThrows(ReceiptNotFoundException.class, () -> {
            receiptService.getPoints(invalidId);
        });

        assertEquals("No receipt found for that id:" + invalidId, exception.getMessage());
    }

    @Test
    void testCalculatePoints_EdgeCase() {
        // Arrange: Set up edge case data
        Item item1 = new Item(null, "Chocolate", BigDecimal.valueOf(2.25), validReceipt); // Bonus = 1 (length % 3)
        validReceipt.setItems(Arrays.asList(item1));
        validReceipt.setPurchaseDate(LocalDate.of(2022, 11, 25)); // Odd day
        validReceipt.setTotal(BigDecimal.valueOf(10.00)); // Round dollar total

        UUID receiptId = receiptService.processReceipt(validReceipt);

        // Act: Calculate points
        int points = receiptService.getPoints(receiptId);

        // Assert: Verify edge case points

        assertEquals(104, points); // Total = 104
    }
}
