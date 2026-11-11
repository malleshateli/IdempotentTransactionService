package com.assessment;

import com.assessment.model.TransactionRequest;
import com.assessment.model.TransactionStatus;
import com.assessment.model.TransactionType;
import com.assessment.processor.TransactionProcessor;
import com.assessment.processor.TransactionResult;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionProcessorTest {

    private TransactionRequest txn(String txnId, String reqId, String account,
                                   TransactionType type, String amount, long sequence) {
        return new TransactionRequest(txnId, reqId, account, type,
                new BigDecimal(amount), "INR", sequence);
    }

    @Test
    public void shouldNotApplySameTransactionTwice() {
        TransactionProcessor processor = new TransactionProcessor();
        TransactionRequest original = txn("T-1", "R-1", "A-1", TransactionType.CREDIT, "100", 1);
        TransactionRequest duplicate = txn("T-1", "R-1-REPLAY", "A-1", TransactionType.CREDIT, "100", 1);

        List<TransactionResult> results = processor.processBatch(Arrays.asList(original, duplicate));

        Assert.assertEquals(results.get(0).getStatus(), TransactionStatus.PROCESSED);
        Assert.assertEquals(results.get(1).getStatus(), TransactionStatus.DUPLICATE);
        Assert.assertEquals(processor.getBalance("A-1"), new BigDecimal("100"));
        Assert.assertEquals(processor.getLedgerEntryCount(), 1);
    }

    @Test
    public void shouldProcessOutOfOrderTransactionsInSequence() {
        TransactionProcessor processor = new TransactionProcessor();
        TransactionRequest sequence2 = txn("T-2", "R-2", "A-2", TransactionType.DEBIT, "20", 2);
        TransactionRequest sequence1 = txn("T-1", "R-1", "A-2", TransactionType.CREDIT, "100", 1);

        List<TransactionResult> results = processor.processBatch(Arrays.asList(sequence2, sequence1));

        Assert.assertEquals(results.get(0).getStatus(), TransactionStatus.PENDING);
        Assert.assertEquals(results.get(1).getStatus(), TransactionStatus.PROCESSED);
        Assert.assertEquals(processor.getTransactionStatus("T-2"), TransactionStatus.PROCESSED);
        Assert.assertEquals(processor.getBalance("A-2"), new BigDecimal("80"));
        Assert.assertEquals(processor.getLedgerEntryCount(), 2);
    }

    @Test
    public void shouldRejectInvalidAmountAndContinueBatch() {
        TransactionProcessor processor = new TransactionProcessor();
        TransactionRequest invalid = txn("T-BAD", "R-BAD", "A-3", TransactionType.CREDIT, "-10", 1);
        TransactionRequest valid = txn("T-GOOD", "R-GOOD", "A-4", TransactionType.CREDIT, "50", 1);

        List<TransactionResult> results = processor.processBatch(Arrays.asList(invalid, valid));

        Assert.assertEquals(results.get(0).getStatus(), TransactionStatus.FAILED);
        Assert.assertEquals(results.get(1).getStatus(), TransactionStatus.PROCESSED);
        Assert.assertEquals(processor.getBalance("A-4"), new BigDecimal("50"));
    }

    @Test
    public void shouldRetryTransientFailureWithoutDuplicateEffect() {
        Map<String, Integer> failOnce = new HashMap<>();
        failOnce.put("T-RETRY", 1);
        TransactionProcessor processor = new TransactionProcessor(3, Collections.emptyMap(), failOnce);
        TransactionRequest request = txn("T-RETRY", "R-RETRY", "A-5",
                TransactionType.CREDIT, "75", 1);

        TransactionResult result = processor.processBatch(Collections.singletonList(request)).get(0);

        Assert.assertEquals(result.getStatus(), TransactionStatus.PROCESSED);
        Assert.assertEquals(result.getAttempts(), 2);
        Assert.assertEquals(processor.getBalance("A-5"), new BigDecimal("75"));
        Assert.assertEquals(processor.getLedgerEntryCount(), 1);
    }

    @Test
    public void shouldFailWhenRetryLimitIsExhausted() {
        Map<String, Integer> failThreeTimes = new HashMap<>();
        failThreeTimes.put("T-FAIL", 3);
        TransactionProcessor processor = new TransactionProcessor(2, Collections.emptyMap(), failThreeTimes);
        TransactionRequest request = txn("T-FAIL", "R-FAIL", "A-6",
                TransactionType.CREDIT, "75", 1);

        TransactionResult result = processor.processBatch(Collections.singletonList(request)).get(0);

        Assert.assertEquals(result.getStatus(), TransactionStatus.FAILED);
        Assert.assertEquals(processor.getBalance("A-6"), BigDecimal.ZERO);
        Assert.assertEquals(processor.getLedgerEntryCount(), 0);
    }
}
