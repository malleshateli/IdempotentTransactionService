package com.assessment.processor;

import com.assessment.model.TransactionStatus;

public class TransactionResult {
    private final String transactionId;
    private final TransactionStatus status;
    private final String message;
    private final int attempts;

    public TransactionResult(String transactionId, TransactionStatus status, String message, int attempts) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.attempts = attempts;
    }

    public String getTransactionId() { return transactionId; }
    public TransactionStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public int getAttempts() { return attempts; }

    @Override
    public String toString() {
        return transactionId + " | " + status + " | attempts=" + attempts + " | " + message;
    }
}
