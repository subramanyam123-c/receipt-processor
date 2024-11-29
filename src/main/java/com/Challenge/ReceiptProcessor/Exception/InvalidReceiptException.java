package com.Challenge.ReceiptProcessor.Exception;

public class InvalidReceiptException extends RuntimeException {
    public InvalidReceiptException(String message) {
        super(message);
    }
}