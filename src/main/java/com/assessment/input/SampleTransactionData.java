package com.assessment.input;

import com.assessment.model.TransactionRequest;
import com.assessment.model.TransactionType;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class SampleTransactionData {
    private SampleTransactionData() { }

    public static List<TransactionRequest> create() {
        return Arrays.asList(
                new TransactionRequest("TXN-2002", "REQ-2", "ACC-201",
                        TransactionType.DEBIT, new BigDecimal("40"), "INR", 2),
                new TransactionRequest("TXN-2001", "REQ-1", "ACC-201",
                        TransactionType.CREDIT, new BigDecimal("100"), "INR", 1),
                new TransactionRequest("TXN-2002", "REQ-2-DUP", "ACC-201",
                        TransactionType.DEBIT, new BigDecimal("40"), "INR", 2),
                new TransactionRequest("TXN-2003", "REQ-3", "ACC-201",
                        TransactionType.CREDIT, new BigDecimal("25"), "INR", 3),
                new TransactionRequest("TXN-2004", "REQ-4", "ACC-202",
                        TransactionType.DEBIT, new BigDecimal("-10"), "INR", 4),
                new TransactionRequest("TXN-2005", "REQ-5", "ACC-203",
                        TransactionType.CREDIT, new BigDecimal("500"), "INR", 5)
        );
    }
}
