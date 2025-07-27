package org.acme.vehiclerouting.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.count;

public class Stress_1 {

    // --- Data Definitions ---

    public record Customer(Integer id, String riskLevel, String status) {
    }

    public record Transaction(Integer id, Integer customerId, double amount, String location) {
    }

    public record SecurityAlert(String location, int severity) {
    }

    // --- Dummy Entity to satisfy SolverFactory validation ---
    @PlanningEntity
    public class DummyEntity {

        @PlanningVariable(valueRangeProviderRefs = {"some_value_provider"})
        Integer dummy_variable;
        public DummyEntity() {
        }

        public Integer getDummy_variable() {
            return this.dummy_variable;
        }
    }

    // A dummy solution class to hold the facts for the SolutionManager
    @PlanningSolution
    public static class EmptySolution {

        @ProblemFactCollectionProperty
        private List<Object> facts;

        @PlanningEntityCollectionProperty
        private List<DummyEntity> dummyEntityList;

        @PlanningScore
        private SimpleBigDecimalScore score;

        // No-arg constructor required by Timefold
        public EmptySolution() {
        }

        public EmptySolution(List<Object> facts) {
            this.facts = facts;
            this.dummyEntityList = Collections.emptyList();
        }

        @ValueRangeProvider(id = "some_value_provider")
        public List<Integer> some_value_provider() {
            return Arrays.asList(1, 2);
        }

        public List<Object> getFacts() {
            return facts;
        }

        public List<DummyEntity> getDummyEntityList() {
            return dummyEntityList;
        }

        public SimpleBigDecimalScore getScore() {
            return score;
        }

        public void setScore(SimpleBigDecimalScore score) {
            this.score = score;
        }
    }


    // --- Constraint Definitions (Updated API Style) ---

    public static class StressTestConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[]{
                    highValueTransaction(constraintFactory),
                    excessiveTransactionsPerCustomer(constraintFactory),
                    transactionInAlertedLocation(constraintFactory),
                    inactiveCustomerTransaction(constraintFactory),
                    highRiskTransactionWithoutAlert(constraintFactory)
            };
        }

        // Constraint 1: Simple filter for high-value transactions.
        Constraint highValueTransaction(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Transaction.class)
                    .filter(tx -> tx.amount() > 45000)
                    .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                            tx -> BigDecimal.valueOf(tx.amount() / 1000.0))
                    .asConstraint("high_value_transaction");
        }

        // Constraint 2: Group transactions by customer and check for excessive activity.
        Constraint excessiveTransactionsPerCustomer(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Transaction.class)
                    .groupBy(Transaction::customerId, count())
                    .filter((cid, count) -> count > 25)
                    .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                            (cid, count) -> BigDecimal.valueOf((count - 25) * 10))
                    .asConstraint("excessive_transactions_per_customer");
        }

        // Constraint 3: Join transactions with security alerts on location.
        Constraint transactionInAlertedLocation(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Transaction.class)
                    .join(SecurityAlert.class,
                            Joiners.equal(Transaction::location, SecurityAlert::location))
                    .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                            (tx, alert) -> BigDecimal.valueOf(100 * alert.severity()))
                    .asConstraint("transaction_in_alerted_location");
        }

        // Constraint 4: Join to find transactions from inactive customers.
        Constraint inactiveCustomerTransaction(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Customer.class)
                    .filter(c -> "inactive".equals(c.status()))
                    .join(Transaction.class,
                            Joiners.equal(Customer::id, Transaction::customerId))
                    .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                            (c, tx) -> BigDecimal.valueOf(500))
                    .asConstraint("inactive_customer_transaction");
        }

        // Constraint 5: Complex rule using ifNotExists.
        Constraint highRiskTransactionWithoutAlert(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(Customer.class)
                    .filter(c -> "high".equals(c.riskLevel()))
                    .join(Transaction.class,
                            Joiners.equal(Customer::id, Transaction::customerId))
                    .ifNotExists(SecurityAlert.class,
                            Joiners.equal((c, tx) -> tx.location(), SecurityAlert::location))
                    .penalizeBigDecimal(SimpleBigDecimalScore.ONE,
                            (c, tx) -> BigDecimal.valueOf(1000))
                    .asConstraint("high_risk_transaction_without_alert");
        }
    }

    // --- Data Generation ---

    public static List<Object> generateData(int numCustomers, int numTransactions, int numLocations) {
        System.out.println("Generating test data...");
        Random random = new Random(0); // Use a fixed seed for reproducibility
        List<String> locations = IntStream.range(0, numLocations)
                .mapToObj(i -> "location_" + i)
                .collect(Collectors.toList());

        List<Object> allFacts = new ArrayList<>(numCustomers + numTransactions + (numLocations / 4));

        // Generate Customers
        for (int i = 0; i < numCustomers; i++) {
            String riskLevel = List.of("low", "medium", "high").get(random.nextInt(3));
            String status = random.nextDouble() < 0.05 ? "inactive" : "active";
            allFacts.add(new Customer(i, riskLevel, status));
        }

        // Generate Transactions
        for (int i = 0; i < numTransactions; i++) {
            allFacts.add(new Transaction(
                    i,
                    random.nextInt(numCustomers),
                    random.nextDouble() * 49999.0 + 1.0,
                    locations.get(random.nextInt(numLocations))
            ));
        }

        // Generate Security Alerts
        Collections.shuffle(locations, random);
        int alertCount = Math.max(1, numLocations / 4);
        for (int i = 0; i < alertCount; i++) {
            allFacts.add(new SecurityAlert(locations.get(i), random.nextInt(5) + 1));
        }

        return allFacts;
    }

    // --- Main Test Runner ---

    public static void main(String[] args) {
        // --- Configuration ---
        int numCustomers = 10_000;
        int numTransactions = 10_000_000; // Reduced for reasonable heap size
        int numLocations = 1_000;

        System.out.println("### Starting Rule Engine Stress Test (Java/Bavet) ###");

        // 1. Setup Phase & Initial State
        long timeStartSetup = System.nanoTime();
        SolverConfig solverConfig = new SolverConfig()
                .withConstraintProviderClass(StressTestConstraintProvider.class)
                .withSolutionClass(EmptySolution.class)
                .withEntityClasses(DummyEntity.class)
                .withMoveThreadCount("1");
        SolverFactory<EmptySolution> solverFactory = SolverFactory.create(solverConfig);
        SolutionManager<EmptySolution, SimpleBigDecimalScore> solutionManager = SolutionManager.create(solverFactory);
        long timeEndSetup = System.nanoTime();

        // 2. Data Generation Phase
        long timeStartData = System.nanoTime();
        List<Object> allFacts = generateData(numCustomers, numTransactions, numLocations);
        long timeEndData = System.nanoTime();


        // 3. Processing Phase
        System.out.println("Inserting facts and processing rules...");
        long timeStartProcessing = System.nanoTime();
        var solution = new EmptySolution(allFacts);
        var scoreExplanation = solutionManager.explain(solution);
        long timeEndProcessing = System.nanoTime();

        // 4. Get Memory Snapshot (Approximation)
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection to get a cleaner reading
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();


        // 5. Reporting
        System.out.println("\n--- Stress Test Results ---");

        // Time Metrics
        double setupDuration = (timeEndSetup - timeStartSetup) / 1_000_000_000.0;
        double dataGenDuration = (timeEndData - timeStartData) / 1_000_000_000.0;
        double processingDuration = (timeEndProcessing - timeStartProcessing) / 1_000_000_000.0;
        double totalDuration = (timeEndProcessing - timeStartSetup) / 1_000_000_000.0;

        // Performance Metrics
        long totalFacts = allFacts.size();
        double factsPerSecond = (processingDuration > 0) ? totalFacts / processingDuration : Double.POSITIVE_INFINITY;

        // Display Report using Markdown
        System.out.println("\n#### Performance Summary");
        System.out.println("| Metric                         | Value               |");
        System.out.println("|--------------------------------|---------------------|");
        System.out.printf("| Total Facts Processed          | %,d         |%n", totalFacts);
        System.out.printf("| Setup Time (Build Network)     | %.4f s      |%n", setupDuration);
        System.out.printf("| Data Generation Time           | %.4f s      |%n", dataGenDuration);
        System.out.printf("| **Processing Time (Insert+Flush)** | **%.4f s** |%n", processingDuration);
        System.out.printf("| Total Time                     | %.4f s      |%n", totalDuration);
        System.out.printf("| **Throughput** | **%,.2f facts/sec** |%n", factsPerSecond);

        System.out.println("\n#### Memory Usage Summary (Approximation)");
        System.out.println("| Metric                         | Value               |");
        System.out.println("|--------------------------------|---------------------|");
        System.out.printf("| **Final Memory Usage** | **%.2f MB** |%n", memoryUsed / (1024.0 * 1024.0));

        System.out.println("\n#### Engine Output");
        System.out.println("- **Final Score:** " + scoreExplanation.getScore());
        System.out.println("- **Total Constraint Matches:** " + scoreExplanation.getConstraintMatchTotalMap().values().stream().mapToLong(cm -> cm.getConstraintMatchCount()).sum());
        scoreExplanation.getConstraintMatchTotalMap().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        System.out.printf("  - `%s`: %d matches%n",
                                entry.getKey(), entry.getValue().getConstraintMatchCount()));
    }
}
