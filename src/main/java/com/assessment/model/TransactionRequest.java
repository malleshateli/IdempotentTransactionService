package com.assessment.model;

import java.math.BigDecimal;

public class TransactionRequest {
    private final String transactionId;
    private final String requestId;
    private final String accountId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final String currency;
    private final long sequenceNumber;

    public TransactionRequest(String transactionId, String requestId, String accountId,
                              TransactionType transactionType, BigDecimal amount,
                              String currency, long sequenceNumber) {
        this.transactionId = transactionId;
        this.requestId = requestId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.sequenceNumber = sequenceNumber;
    }

    public String getTransactionId() { return transactionId; }
    public String getRequestId() { return requestId; }
    public String getAccountId() { return accountId; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public long getSequenceNumber() { return sequenceNumber; }

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "transactionId='" + transactionId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", type=" + transactionType +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
