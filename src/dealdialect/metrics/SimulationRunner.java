package dealdialect.metrics;

import dealdialect.engine.*;
import dealdialect.language.*;
import dealdialect.strategies.*;
import negotiation.models.Item;
import java.io.*;
import java.util.*;

/**
 * Runs batch AI vs AI simulations.
 * Tests multiple strategies and collects statistics.
 */
public class SimulationRunner {
    
    private int iterations;
    private Map<String, StrategyConfig> strategies;
    private List<SimulationResult> results;
    private LanguageModel languageModel;
    
    public SimulationRunner() {
        this.iterations = 100;
        this.strategies = new HashMap<>();
        this.results = new ArrayList<>();
        this.languageModel = new TemplateLanguageModel();
    }
    
    /**
     * Add a strategy configuration to test
     */
    public void addStrategy(String name, StrategyConfig config) {
        strategies.put(name, config);
    }
    
    /**
     * Run the simulation
     */
    public void run() {
        System.out.println("Starting simulation with " + iterations + " iterations...");
        
        // Test all strategy combinations
        for (Map.Entry<String, StrategyConfig> buyerEntry : strategies.entrySet()) {
            for (Map.Entry<String, StrategyConfig> sellerEntry : strategies.entrySet()) {
                String buyerStrategy = buyerEntry.getKey();
                String sellerStrategy = sellerEntry.getKey();
                
                System.out.printf("Testing: %s (Buyer) vs %s (Seller)%n", 
                                buyerStrategy, sellerStrategy);
                
                runMatchup(buyerStrategy, buyerEntry.getValue(), 
                          sellerStrategy, sellerEntry.getValue());
            }
        }
        
        System.out.println("Simulation complete!");
    }
    
    private void runMatchup(String buyerStratName, StrategyConfig buyerStrat,
                           String sellerStratName, StrategyConfig sellerStrat) {
        int agreements = 0;
        double totalBuyerSurplus = 0;
        double totalSellerSurplus = 0;
        double totalRounds = 0;
        
        for (int i = 0; i < iterations; i++) {
            // Create random deal
            DealContext context = createRandomDeal();
            
            // Create engine
            NegotiationEngine engine = new NegotiationEngine(context, languageModel);
            
            // Set up buyer strategy
            engine.setMovePolicy(Role.BUYER, buyerStrat.movePolicy);
            engine.setBluffPolicy(Role.BUYER, buyerStrat.bluffPolicy);
            engine.setConcessionPolicy(Role.BUYER, buyerStrat.concessionPolicy);
            
            // Set up seller strategy
            engine.setMovePolicy(Role.SELLER, sellerStrat.movePolicy);
            engine.setBluffPolicy(Role.SELLER, sellerStrat.bluffPolicy);
            engine.setConcessionPolicy(Role.SELLER, sellerStrat.concessionPolicy);
            
            // Set truthfulness policy
            engine.setTruthfulnessPolicy(new DefaultTruthfulnessPolicy(0.7, 0.02));
            
            // Run negotiation
            while (!engine.getState().isTerminal()) {
                engine.step(Role.BUYER);
                if (engine.getState().isTerminal()) break;
                engine.step(Role.SELLER);
            }
            
            // Collect results
            DialogueState state = engine.getState();
            if (state.hasAgreement()) {
                agreements++;
                Metrics metrics = new Metrics(context, state);
                Map<String, Object> allMetrics = metrics.calculateAll();
                
                totalBuyerSurplus += (Double) allMetrics.get("buyer_surplus");
                totalSellerSurplus += (Double) allMetrics.get("seller_surplus");
            }
            totalRounds += state.getCurrentRound();
        }
        
        // Store result
        SimulationResult result = new SimulationResult();
        result.buyerStrategy = buyerStratName;
        result.sellerStrategy = sellerStratName;
        result.agreementRate = (double) agreements / iterations;
        result.avgBuyerSurplus = agreements > 0 ? totalBuyerSurplus / agreements : 0;
        result.avgSellerSurplus = agreements > 0 ? totalSellerSurplus / agreements : 0;
        result.avgRounds = totalRounds / iterations;
        results.add(result);
        
        System.out.printf("  Agreement Rate: %.2f%%, Avg Rounds: %.1f%n", 
                         result.agreementRate * 100, result.avgRounds);
    }
    
    private DealContext createRandomDeal() {
        Random rand = new Random();
        double msrp = 500 + rand.nextDouble() * 1500;  // $500-$2000
        double buyerValue = msrp * (0.8 + rand.nextDouble() * 0.3);  // 80-110% of MSRP
        double sellerCost = msrp * (0.6 + rand.nextDouble() * 0.3);   // 60-90% of MSRP
        
        Item item = new Item("electronics", "Product", "Description", msrp);
        return new DealContext(item, msrp, buyerValue, sellerCost, 20);
    }
    
    /**
     * Export results to CSV
     */
    public void exportToCSV(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("Buyer Strategy,Seller Strategy,Agreement Rate,Avg Buyer Surplus,Avg Seller Surplus,Avg Rounds");
            
            // Data
            for (SimulationResult result : results) {
                writer.printf("%s,%s,%.4f,%.2f,%.2f,%.2f%n",
                    result.buyerStrategy,
                    result.sellerStrategy,
                    result.agreementRate,
                    result.avgBuyerSurplus,
                    result.avgSellerSurplus,
                    result.avgRounds);
            }
        }
        
        System.out.println("Results exported to " + filename);
    }
    
    /**
     * Get win rates by strategy
     */
    public Map<String, Double> getWinRates() {
        Map<String, Integer> wins = new HashMap<>();
        Map<String, Integer> games = new HashMap<>();
        
        for (SimulationResult result : results) {
            games.merge(result.buyerStrategy, 1, Integer::sum);
            games.merge(result.sellerStrategy, 1, Integer::sum);
            
            if (result.avgBuyerSurplus > result.avgSellerSurplus) {
                wins.merge(result.buyerStrategy, 1, Integer::sum);
            } else if (result.avgSellerSurplus > result.avgBuyerSurplus) {
                wins.merge(result.sellerStrategy, 1, Integer::sum);
            }
        }
        
        Map<String, Double> winRates = new HashMap<>();
        for (String strategy : games.keySet()) {
            winRates.put(strategy, (double) wins.getOrDefault(strategy, 0) / games.get(strategy));
        }
        
        return winRates;
    }
    
    // Getters and setters
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
    
    public List<SimulationResult> getResults() {
        return results;
    }
    
    /**
     * Strategy configuration bundle
     */
    public static class StrategyConfig {
        public MovePolicy movePolicy;
        public BluffPolicy bluffPolicy;
        public ConcessionPolicy concessionPolicy;
        
        public StrategyConfig(MovePolicy movePolicy, BluffPolicy bluffPolicy, 
                            ConcessionPolicy concessionPolicy) {
            this.movePolicy = movePolicy;
            this.bluffPolicy = bluffPolicy;
            this.concessionPolicy = concessionPolicy;
        }
    }
    
    /**
     * Simulation result for one matchup
     */
    public static class SimulationResult {
        public String buyerStrategy;
        public String sellerStrategy;
        public double agreementRate;
        public double avgBuyerSurplus;
        public double avgSellerSurplus;
        public double avgRounds;
    }
}

