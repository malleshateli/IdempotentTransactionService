package com.assessment.processor;

import com.assessment.model.TransactionRequest;
import com.assessment.model.TransactionStatus;
import com.assessment.model.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * In-memory demonstration processor.
 * Sequence numbers are tracked per account. The default expected sequence is 1.
 * All public processing operations are synchronized to prevent concurrent duplicate effects
 * in this single-process demonstration.
 */
public class TransactionProcessor {
    private static final Logger LOGGER = Logger.getLogger(TransactionProcessor.class.getName());

    private final int maxAttempts;
    private final Map<String, Long> nextSequenceByAccount = new HashMap<>();
    private final Map<String, TreeMap<Long, TransactionRequest>> pendingByAccount = new HashMap<>();
    private final Set<String> seenTransactionIds = new HashSet<>();
    private final Set<String> seenRequestIds = new HashSet<>();
    private final Map<String, TransactionStatus> transactionStates = new HashMap<>();
    private final Map<String, BigDecimal> balances = new HashMap<>();
    private final List<String> ledger = new ArrayList<>();
    private final Map<String, Integer> simulatedFailuresRemaining = new HashMap<>();
    private final List<TransactionResult> results = new ArrayList<>();
    private long duplicateRequestCount = 0;

    public TransactionProcessor(int maxAttempts,
                                Map<String, Long> initialSequences,
                                Map<String, Integer> failFirstAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.maxAttempts = maxAttempts;
        if (initialSequences != null) {
            nextSequenceByAccount.putAll(initialSequences);
        }
        if (failFirstAttempts != null) {
            simulatedFailuresRemaining.putAll(failFirstAttempts);
        }
    }

    public TransactionProcessor() {
        this(2, Collections.emptyMap(), Collections.emptyMap());
    }

    public synchronized List<TransactionResult> processBatch(List<TransactionRequest> requests) {
        List<TransactionResult> batchResults = new ArrayList<>();
        for (TransactionRequest request : requests) {
            TransactionResult result = accept(request);
            batchResults.add(result);
            results.add(result);
        }
        return batchResults;
    }

    private TransactionResult accept(TransactionRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            String id = request == null ? "UNKNOWN" : safe(request.getTransactionId());
            if (request != null && notBlank(request.getTransactionId())) {
                transactionStates.put(request.getTransactionId(), TransactionStatus.FAILED);
            }
            LOGGER.warning("Rejected transaction " + id + ": " + validationError);
            return new TransactionResult(id, TransactionStatus.FAILED, validationError, 0);
        }

        String transactionId = request.getTransactionId();
        String requestId = request.getRequestId();

        if (seenTransactionIds.contains(transactionId) || seenRequestIds.contains(requestId)) {
            duplicateRequestCount++;
            LOGGER.info("Duplicate request detected for " + transactionId);
            return new TransactionResult(transactionId, TransactionStatus.DUPLICATE,
                    "Transaction ID or request ID was already received", 0);
        }

        seenTransactionIds.add(transactionId);
        seenRequestIds.add(requestId);
        transactionStates.put(transactionId, TransactionStatus.RECEIVED);

        long expected = nextSequenceByAccount.getOrDefault(request.getAccountId(), 1L);
        if (request.getSequenceNumber() < expected) {
            transactionStates.put(transactionId, TransactionStatus.FAILED);
            return new TransactionResult(transactionId, TransactionStatus.FAILED,
                    "Stale sequence. Expected " + expected, 0);
        }

        if (request.getSequenceNumber() > expected) {
            pendingByAccount.computeIfAbsent(request.getAccountId(), key -> new TreeMap<>())
                    .put(request.getSequenceNumber(), request);
            transactionStates.put(transactionId, TransactionStatus.PENDING);
            LOGGER.info("Transaction " + transactionId + " is waiting for sequence " + expected);
            return new TransactionResult(transactionId, TransactionStatus.PENDING,
                    "Waiting for sequence " + expected, 0);
        }

        TransactionResult current = processInSequence(request);
        drainPending(request.getAccountId());
        return current;
    }

    private void drainPending(String accountId) {
        TreeMap<Long, TransactionRequest> pending = pendingByAccount.get(accountId);
        while (pending != null) {
            long expected = nextSequenceByAccount.getOrDefault(accountId, 1L);
            TransactionRequest next = pending.remove(expected);
            if (next == null) {
                break;
            }
            processInSequence(next);
        }
        if (pending != null && pending.isEmpty()) {
            pendingByAccount.remove(accountId);
        }
    }

    private TransactionResult processInSequence(TransactionRequest request) {
        String id = request.getTransactionId();
        transactionStates.put(id, TransactionStatus.PROCESSING);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (consumeSimulatedFailure(id)) {
                transactionStates.put(id, TransactionStatus.RETRY_PENDING);
                LOGGER.warning("Simulated transient failure for " + id + " on attempt " + attempt);
                if (attempt == maxAttempts) {
                    transactionStates.put(id, TransactionStatus.FAILED);
                    advanceSequence(request);
                    return new TransactionResult(id, TransactionStatus.FAILED,
                            "Retry limit exhausted", attempt);
                }
                continue;
            }

            String businessError = applyBusinessEffect(request);
            if (businessError != null) {
                transactionStates.put(id, TransactionStatus.FAILED);
                advanceSequence(request);
                return new TransactionResult(id, TransactionStatus.FAILED, businessError, attempt);
            }

            transactionStates.put(id, TransactionStatus.PROCESSED);
            advanceSequence(request);
            LOGGER.info("Processed transaction " + id + " after " + attempt + " attempt(s)");
            return new TransactionResult(id, TransactionStatus.PROCESSED, "Transaction processed", attempt);
        }

        transactionStates.put(id, TransactionStatus.FAILED);
        advanceSequence(request);
        return new TransactionResult(id, TransactionStatus.FAILED, "Processing failed", maxAttempts);
    }

    private boolean consumeSimulatedFailure(String transactionId) {
        int remaining = simulatedFailuresRemaining.getOrDefault(transactionId, 0);
        if (remaining <= 0) {
            return false;
        }
        simulatedFailuresRemaining.put(transactionId, remaining - 1);
        return true;
    }

    private String applyBusinessEffect(TransactionRequest request) {
        BigDecimal current = balances.getOrDefault(request.getAccountId(), BigDecimal.ZERO);
        BigDecimal updated;
        if (request.getTransactionType() == TransactionType.CREDIT) {
            updated = current.add(request.getAmount());
        } else {
            if (current.compareTo(request.getAmount()) < 0) {
                return "Insufficient account balance";
            }
            updated = current.subtract(request.getAmount());
        }

        // The balance and ledger are changed together only after validation/retry checks pass.
        balances.put(request.getAccountId(), updated);
        ledger.add(request.getTransactionId() + "|" + request.getAccountId() + "|" +
                request.getTransactionType() + "|" + request.getAmount() + "|" + updated);
        return null;
    }

    private void advanceSequence(TransactionRequest request) {
        nextSequenceByAccount.put(request.getAccountId(), request.getSequenceNumber() + 1);
    }

    private String validate(TransactionRequest request) {
        if (request == null) return "Request must not be null";
        if (!notBlank(request.getTransactionId())) return "transactionId is mandatory";
        if (!notBlank(request.getRequestId())) return "requestId is mandatory";
        if (!notBlank(request.getAccountId())) return "accountId is mandatory";
        if (request.getTransactionType() == null) return "transactionType is mandatory";
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            return "amount must be greater than zero";
        if (!notBlank(request.getCurrency())) return "currency is mandatory";
        if (request.getSequenceNumber() < 1) return "sequenceNumber must be positive";
        if (!"INR".equalsIgnoreCase(request.getCurrency()))
            return "This demonstration supports INR only";
        return null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }

    public synchronized Map<TransactionStatus, Long> getSummary() {
        Map<TransactionStatus, Long> summary = new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus status : TransactionStatus.values()) {
            summary.put(status, 0L);
        }
        for (TransactionStatus status : transactionStates.values()) {
            summary.put(status, summary.get(status) + 1);
        }
        // Duplicate deliveries are request attempts, not a replacement state for the original transaction.
        summary.put(TransactionStatus.DUPLICATE, duplicateRequestCount);
        return summary;
    }

    public synchronized BigDecimal getBalance(String accountId) {
        return balances.getOrDefault(accountId, BigDecimal.ZERO);
    }

    public synchronized int getLedgerEntryCount() {
        return ledger.size();
    }

    public synchronized TransactionStatus getTransactionStatus(String transactionId) {
        return transactionStates.get(transactionId);
    }

    public synchronized List<String> getLedger() {
        return new ArrayList<>(ledger);
    }

    public synchronized List<TransactionResult> getAllResults() {
        return new ArrayList<>(results);
    }
}
