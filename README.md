# Idempotent Background Transaction Processing Service

Java 17 + Maven + TestNG implementation of the FDE L2 case study.

## Requirements
- JDK 17 or later
- Maven 3.8 or later
- Eclipse IDE (or another Java IDE)

## Import into Eclipse
1. Extract this ZIP file.
2. Open Eclipse.
3. Select **File -> Import -> Maven -> Existing Maven Projects**.
4. Browse to the extracted `IdempotentTransactionService` folder.
5. Select the project and click **Finish**.
6. Wait for Maven dependencies to download.

## Run the application
- Open `src/main/java/com/assessment/Main.java`.
- Right-click -> **Run As -> Java Application**.

Or from a terminal opened in the project folder:
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.assessment.Main"
```
If you prefer not to configure the Maven exec plugin, run `Main.java` directly from Eclipse.

## Run TestNG tests
From the project folder:
```bash
mvn clean test
```
In Eclipse, right-click `TransactionProcessorTest.java` -> **Run As -> TestNG Test**.

## Implemented scenarios
1. Duplicate transaction is detected and its balance/ledger effect is not repeated.
2. Sequence 2 waits until sequence 1 is available; then both are processed in order.
3. Invalid amount is rejected without stopping the batch.
4. Simulated transient failure is retried and succeeds without a duplicate effect.
5. Retry exhaustion results in FAILED with no balance/ledger update.

## Design limitations / assumptions
- In-memory storage is used; data is lost when the application stops.
- This is a single-process demonstration. `synchronized` protects operations inside this JVM, not multiple service instances.
- Sequence numbers are tracked per account and default to 1. The sample configures ACC-203 to start at sequence 5 because the supplied sample has no earlier events for that account.
- The sample supports INR and CREDIT/DEBIT. REVERSAL semantics were not defined in the problem statement, so reversal processing is not guessed or implemented.
- A future production implementation should use a durable database with a unique idempotency constraint, database transactions, durable queue offsets, dead-letter handling, metrics, authentication, and multi-instance concurrency control.
