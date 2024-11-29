package com.Challenge.ReceiptProcessor.Service;

import com.Challenge.ReceiptProcessor.Entity.Item;
import com.Challenge.ReceiptProcessor.Entity.Receipt;
import com.Challenge.ReceiptProcessor.Exception.InvalidReceiptException;
import com.Challenge.ReceiptProcessor.Exception.ReceiptNotFoundException;
import com.Challenge.ReceiptProcessor.Repository.ReceiptRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service

public class ReceiptService {

    @Autowired
    private ReceiptRepository receiptRepository;

    // Process receipt and return the generated ID
    @Transactional
    public UUID processReceipt(Receipt receipt) {
        // Check for standard mistakes in the receipt
        if (receipt == null) {
            throw new InvalidReceiptException("The receipt is invalid : Receipt cannot be null");
        }
        if (receipt.getItems() == null || receipt.getItems().isEmpty()) {
            throw new InvalidReceiptException("The receipt is invalid : Receipt must have at least one item");
        }
        if (receipt.getTotal() == null || receipt.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReceiptException("The receipt is invalid : Total amount must be greater than zero");
        }
        for(Item item:receipt.getItems()){
            if(item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0){
                throw new InvalidReceiptException("The receipt is invalid : Price of item must be greater than zero");
            }
        }


        // If valid, store receipt in the repository
        Receipt savedReceipt = receiptRepository.save(receipt);

        // Return the unique ID of the saved receipt
        return savedReceipt.getId();
    }

    // Get points based on receipt ID
    public int getPoints(UUID id) {
        // Check if the receipt exists
        Optional<Receipt> optionalReceipt = receiptRepository.findById(id);
        if (optionalReceipt.isEmpty()) {
            throw new ReceiptNotFoundException("No receipt found for that id:"+id);
        }

        // Calculate points
        Receipt receipt = optionalReceipt.get();
        return calculatePoints(receipt);
    }

    // Helper method to calculate points
    private int calculatePoints(Receipt receipt) {
        int points = 0;

        // 1. One point for every alphanumeric character in the retailer name.
        String retailer = receipt.getRetailer();

        if (retailer != null) {
            points += retailer.replaceAll("[^a-zA-Z0-9]", "").length();

        }

        // 2. 50 points if the total is a round dollar amount with no cents.
        BigDecimal total = receipt.getTotal();

        if (total != null && total.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            points += 50;

        }
        // 3. 25 points if the total is a multiple of 0.25.
        if (total != null && total.remainder(new BigDecimal("0.25")).compareTo(BigDecimal.ZERO) == 0) {
            points += 25;

        }
        // 4. 5 points for every two items on the receipt.
        List<Item> items = receipt.getItems();
        if (items != null) {
            points += (items.size() / 2) * 5;
        }
        // 5. If the trimmed length of the item description is a multiple of 3,
        // multiply the price by 0.2 and round up to the nearest integer.
        if (items != null) {
            for (Item item : items) {
                String description = item.getShortDescription();
                if (description != null && description.trim().length() % 3 == 0) {
                    BigDecimal price = item.getPrice();
                    if (price != null) {
                        //BigDecimal bonus = price.multiply(new BigDecimal("0.2")).setScale(0, BigDecimal.ROUND_UP);
                        BigDecimal bonus = price.multiply(new BigDecimal("0.2")).setScale(0, RoundingMode.UP);
                        points += bonus.intValue();
                    }

                }
            }
        }
        // 6. 6 points if the day in the purchase date is odd.
        LocalDate purchaseDate = receipt.getPurchaseDate();
        if (purchaseDate != null && purchaseDate.getDayOfMonth() % 2 != 0) {
            points += 6;
        }

        // 7. 10 points if the time of purchase is after 2:00 pm and before 4:00 pm.
        LocalTime purchaseTime = receipt.getPurchaseTime();
        if (purchaseTime != null &&
                purchaseTime.isAfter(LocalTime.of(14, 0)) &&
                purchaseTime.isBefore(LocalTime.of(16, 0))) {
            points += 10;
        }
        return points;
    }

}
