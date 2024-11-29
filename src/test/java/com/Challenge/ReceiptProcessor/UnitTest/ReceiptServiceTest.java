package com.Challenge.ReceiptProcessor.UnitTest;

import com.Challenge.ReceiptProcessor.Entity.Item;
import com.Challenge.ReceiptProcessor.Entity.Receipt;
import com.Challenge.ReceiptProcessor.Exception.InvalidReceiptException;
import com.Challenge.ReceiptProcessor.Exception.ReceiptNotFoundException;
import com.Challenge.ReceiptProcessor.Repository.ReceiptRepository;
import com.Challenge.ReceiptProcessor.Service.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @InjectMocks
    private ReceiptService receiptService;

    private Receipt validReceipt;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize a valid receipt for testing
        Item item1 = new Item(1L, "Test Item 1", BigDecimal.valueOf(10.00), null);
        Item item2 = new Item(2L, "Test Item 2", BigDecimal.valueOf(5.00), null);

        validReceipt = new Receipt();
        validReceipt.setId(UUID.randomUUID());
        validReceipt.setRetailer("Test Retailer");
        validReceipt.setPurchaseDate(LocalDate.of(2022, 11, 27));
        validReceipt.setPurchaseTime(LocalTime.of(15, 30));
        validReceipt.setTotal(BigDecimal.valueOf(15.00));
        validReceipt.setItems(Arrays.asList(item1, item2));
    }

    @Test
    void testProcessReceipt_ValidReceipt() {
        // Arrange
        when(receiptRepository.save(any(Receipt.class))).thenReturn(validReceipt);

        // Act
        UUID result = receiptService.processReceipt(validReceipt);

        // Assert
        assertNotNull(result);
        verify(receiptRepository, times(1)).save(validReceipt);
    }

    @Test
    void testProcessReceipt_NullReceipt() {
        // Act & Assert
        Exception exception = assertThrows(InvalidReceiptException.class, () -> {
            receiptService.processReceipt(null);
        });

        assertEquals("The receipt is invalid : Receipt cannot be null", exception.getMessage());
        verifyNoInteractions(receiptRepository);
    }

    @Test
    void testProcessReceipt_NoItems() {
        // Arrange
        validReceipt.setItems(null);

        // Act & Assert
        Exception exception = assertThrows(InvalidReceiptException.class, () -> {
            receiptService.processReceipt(validReceipt);
        });

        assertEquals("The receipt is invalid : Receipt must have at least one item", exception.getMessage());
        verifyNoInteractions(receiptRepository);
    }

    @Test
    void testProcessReceipt_InvalidTotal() {
        // Arrange
        validReceipt.setTotal(BigDecimal.ZERO);

        // Act & Assert
        Exception exception = assertThrows(InvalidReceiptException.class, () -> {
            receiptService.processReceipt(validReceipt);
        });

        assertEquals("The receipt is invalid : Total amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(receiptRepository);
    }

    @Test
    void testGetPoints_ValidId() {
        // Arrange
        UUID receiptId = validReceipt.getId();
        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(validReceipt));

        // Act
        int points = receiptService.getPoints(receiptId);

        // Assert
        assertTrue(points > 0); // Points calculation logic is already covered in the service
        verify(receiptRepository, times(1)).findById(receiptId);
    }

    @Test
    void testGetPoints_InvalidId() {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(receiptRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ReceiptNotFoundException.class, () -> {
            receiptService.getPoints(invalidId);
        });

        assertEquals("No receipt found for that id:" + invalidId, exception.getMessage());
        verify(receiptRepository, times(1)).findById(invalidId);
    }

    @Test
    void testCalculatePoints_SpecificCase() {
        // Arrange
        // Adjust receipt fields to test specific point rules
        validReceipt.setRetailer("Test123"); // 7 points (alphanumeric)
        validReceipt.setTotal(BigDecimal.valueOf(20.00)); // 50 points (round number)
        validReceipt.setItems(Arrays.asList(
                new Item(1L, "ABC", BigDecimal.valueOf(1.00), validReceipt), // Description length % 3 = 0, 0.2 x price = 1
                new Item(2L, "DEF", BigDecimal.valueOf(2.00), validReceipt)  // Another matching item
        ));
        validReceipt.setPurchaseDate(LocalDate.of(2022, 11, 27)); // Odd day = 6 points
        validReceipt.setPurchaseTime(LocalTime.of(15, 00)); // Within 2-4 pm = 10 points

        when(receiptRepository.findById(validReceipt.getId())).thenReturn(Optional.of(validReceipt));

        // Act
        int points = receiptService.getPoints(validReceipt.getId());

        // Assert
        assertEquals(7 + 50 + 25 + 5 + 1 + 1 + 6 + 10, points); // Summing all expected points
    }
}
