package com.Challenge.ReceiptProcessor.Controller;

import com.Challenge.ReceiptProcessor.Entity.Receipt;
import com.Challenge.ReceiptProcessor.Service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/receipts")
@Validated
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    // Endpoint to process receipts
    @PostMapping("/process")
    public ResponseEntity<Map<String, UUID>> processReceipt(@Valid @RequestBody Receipt receipt) {
        UUID id = receiptService.processReceipt(receipt);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // Endpoint to get points for a receipt
    @GetMapping("/{id}/points")
    public ResponseEntity<Map<String, Integer>> getPoints(@PathVariable UUID id) {
        int points = receiptService.getPoints(id);
        return ResponseEntity.ok(Map.of("points", points));
    }
}
