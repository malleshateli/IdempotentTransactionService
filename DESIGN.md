# Design Notes — FDE L2 Assessment

## Architecture
`SampleTransactionData` represents a replaceable input adapter. `TransactionProcessor` owns validation, deduplication, sequence handling, retry, and business effects. The in-memory collections represent the state store, account balances, and ledger.

## Idempotency and duplicate detection
The processor records transaction IDs and request IDs when a valid request is first accepted. A later request containing either identifier is returned as DUPLICATE. The duplicate path does not update the account balance or append a ledger entry. The business transaction ID is the primary idempotency key in this exercise.

## Out-of-order strategy
Sequence numbers are tracked independently per account. The expected sequence defaults to 1. A future sequence is stored in a sorted pending map and marked PENDING. When the expected sequence arrives and completes, the processor drains the next contiguous pending sequence. An older sequence is rejected as stale. ACC-203 is configured with initial sequence 5 solely to make the supplied retry example independently executable.

## Retry and failure handling
The sample can be configured to simulate a transient failure for a transaction a specified number of times. The processor retries up to `maxAttempts`. The balance and ledger are changed only after the simulated failure check succeeds. Retry exhaustion marks the transaction FAILED and produces no business effect.

## Consistency and concurrency
The processor's public batch method is synchronized, so two threads in this JVM cannot interleave processing through this processor instance. The balance update and ledger append happen in one synchronized processing flow. This is not a substitute for a database transaction or cross-instance lock.

## Observability
The application logs accepted, duplicate, pending, retry, and processed events. It prints a status summary, account balances, and ledger entry count after the sample batch.

## AI-assisted development usage
AI assistance was used to structure the project, draft implementation and tests, and identify edge cases. The code should be reviewed and executed locally; the candidate should be prepared to explain each design choice and validate test results independently.

## Production hardening
- Persist transaction state, idempotency keys, balances, and ledger entries in a transactional database.
- Enforce a unique constraint on the business idempotency key.
- Use durable queue ingestion and acknowledge messages only after commit.
- Add bounded exponential backoff, dead-letter queue, and manual replay tooling.
- Add structured logs, correlation IDs, metrics, tracing, and alerting.
- Define reversal semantics, currency conversion rules, account sequence initialization, and debit/credit business constraints with the customer.
- Add authentication, authorization, encryption, and sensitive-data controls.
