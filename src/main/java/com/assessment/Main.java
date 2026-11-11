package com.assessment;

import com.assessment.input.SampleTransactionData;
import com.assessment.model.TransactionStatus;
import com.assessment.processor.TransactionProcessor;
import com.assessment.processor.TransactionResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, Long> initialSequences = new HashMap<>();
        initialSequences.put("ACC-203", 5L); // Sample data starts this account at sequence 5.

        Map<String, Integer> simulatedFailures = new HashMap<>();
        simulatedFailures.put("TXN-2005", 1); // Fail once, then succeed on retry.

        TransactionProcessor processor = new TransactionProcessor(3, initialSequences, simulatedFailures);
        List<TransactionResult> results = processor.processBatch(SampleTransactionData.create());

        System.out.println("\n========== INPUT RESULTS ==========");
        for (TransactionResult result : results) {
            System.out.println(result);
        }

        System.out.println("\n========== PROCESSING SUMMARY ==========");
        Map<TransactionStatus, Long> summary = processor.getSummary();
        for (TransactionStatus status : TransactionStatus.values()) {
            System.out.println(status + " = " + summary.get(status));
        }

        System.out.println("\n========== ACCOUNT BALANCES ==========");
        System.out.println("ACC-201 = " + processor.getBalance("ACC-201"));
        System.out.println("ACC-202 = " + processor.getBalance("ACC-202"));
        System.out.println("ACC-203 = " + processor.getBalance("ACC-203"));
        System.out.println("Ledger entries = " + processor.getLedgerEntryCount());
    }
}
