// --- File: src/main/java/com/example/DroolsStressTest.java ---
package org.example;

import org.example.model.Customer;
import org.example.model.SecurityAlert;
import org.example.model.ScoreHolder;
import org.example.model.Transaction;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DroolsStressTest {

    // --- Configuration ---
    private static final int NUM_CUSTOMERS = 10_000;
    private static final int NUM_TRANSACTIONS = 10_000_000;
    private static final int NUM_LOCATIONS = 1_000;
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("### Starting Rule Engine Stress Test (Java Drools) ###");

        // 1. Setup Phase: Build the KieSession
        long startTime = System.nanoTime();
        KieSession kSession = createKieSession();
        long setupDuration = System.nanoTime() - startTime;

        // 2. Data Generation Phase
        System.out.println("Generating test data...");
        startTime = System.nanoTime();
        List<Object> allFacts = generateData(NUM_CUSTOMERS, NUM_TRANSACTIONS, NUM_LOCATIONS);
        long dataGenDuration = System.nanoTime() - startTime;

        // 3. Processing Phase
        System.out.println("Inserting facts and processing rules...");
        ScoreHolder scoreHolder = new ScoreHolder();
        kSession.setGlobal("scoreHolder", scoreHolder);

        startTime = System.nanoTime();
        for (Object fact : allFacts) {
            kSession.insert(fact);
        }
        kSession.fireAllRules();
        long processingDuration = System.nanoTime() - startTime;

        // Clean up the session
        kSession.dispose();

        // 4. Get Memory Snapshot
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection to get a more stable memory reading
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();

        // 5. Reporting
        System.out.println("\n--- Stress Test Results ---");
        System.out.println("\n#### Performance Summary");
        System.out.printf("| %-30s | %-20s |\n", "Metric", "Value");
        System.out.printf("|-%-30s-|-%-20s-|\n", "-".repeat(30), "-".repeat(20));
        System.out.printf("| %-30s | %,d facts\n", "Total Facts Processed", allFacts.size());
        System.out.printf("| %-30s | %.4f s\n", "Setup Time (Build Session)", TimeUnit.NANOSECONDS.toMillis(setupDuration) / 1000.0);
        System.out.printf("| %-30s | %.4f s\n", "Data Generation Time", TimeUnit.NANOSECONDS.toMillis(dataGenDuration) / 1000.0);
        System.out.printf("| %-30s | %.4f s\n", "Processing Time (Insert+Fire)", TimeUnit.NANOSECONDS.toMillis(processingDuration) / 1000.0);

        double throughput = allFacts.size() / (processingDuration / 1_000_000_000.0);
        System.out.printf("| %-30s | %,.2f facts/sec\n", "Throughput", throughput);

        System.out.println("\n#### Memory Usage Summary");
        System.out.printf("| %-30s | %-20s |\n", "Metric", "Value");
        System.out.printf("|-%-30s-|-%-20s-|\n", "-".repeat(30), "-".repeat(20));
        System.out.printf("| %-30s | %.2f MB\n", "Final Memory Usage", memoryUsed / (1024.0 * 1024.0));

        System.out.println("\n#### Engine Output");
        System.out.printf("- Final Score: %,.2f\n", scoreHolder.getScore());
        System.out.printf("- Total Rule Activations: %,d\n", scoreHolder.getRulesFired());
    }

    /**
     * Creates and configures a KieSession from our DRL file.
     */
    private static KieSession createKieSession() {
        KieServices ks = KieServices.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        // Add the DRL file to the virtual file system
        Resource drlResource = ks.getResources().newClassPathResource("rules/StressTestRules.drl");
        kfs.write(drlResource);

        // Build the KieModule
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();

        // Check for errors
        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }

        KieModule kModule = kb.getKieModule();
        KieContainer kContainer = ks.newKieContainer(kModule.getReleaseId());

        return kContainer.newKieSession();
    }

    /**
     * Generates a large, randomized dataset for testing.
     */
    private static List<Object> generateData(int numCustomers, int numTransactions, int numLocations) {
        List<Object> facts = new ArrayList<>(numCustomers + numTransactions + numLocations);
        String[] riskLevels = {"low", "medium", "high"};
        String[] statuses = {"active", "inactive"};
        double[] statusWeights = {0.95, 0.05};

        List<String> locations = new ArrayList<>(numLocations);
        for (int i = 0; i < numLocations; i++) {
            locations.add("location_" + i);
        }

        for (int i = 0; i < numCustomers; i++) {
            facts.add(new Customer(
                    i,
                    riskLevels[random.nextInt(riskLevels.length)],
                    weightedRandomChoice(statuses, statusWeights)
            ));
        }

        for (int i = 0; i < numTransactions; i++) {
            facts.add(new Transaction(
                    i,
                    random.nextInt(numCustomers),
                    random.nextDouble() * 50000.0,
                    locations.get(random.nextInt(numLocations))
            ));
        }

        // Create alerts for a subset of locations
        Collections.shuffle(locations);
        int alertCount = Math.max(1, numLocations / 4);
        for (int i = 0; i < alertCount; i++) {
            facts.add(new SecurityAlert(
                    locations.get(i),
                    random.nextInt(5) + 1
            ));
        }
        return facts;
    }

    private static String weightedRandomChoice(String[] items, double[] weights) {
        double totalWeight = 0.0;
        for (double w : weights) {
            totalWeight += w;
        }
        double r = random.nextDouble() * totalWeight;
        for (int i = 0; i < items.length; i++) {
            r -= weights[i];
            if (r <= 0.0) {
                return items[i];
            }
        }
        return items[items.length - 1]; // Fallback
    }
}
