package negotiation.learning;

import negotiation.agents.IAgent;
import negotiation.models.NegotiationResult;
import negotiation.strategy.StrategyProfile;
import negotiation.grammar.RuleExpansion;
import java.util.*;

/**
 * Learning module for adaptive strategy and grammar improvement.
 * Updates agent strategies and grammar weights based on negotiation outcomes.
 */
public class LearningModule {
    private Map<String, StrategyAdjustment> strategyHistory;
    private Map<String, GrammarAdjustment> grammarHistory;
    private double learningRate;
    private int episodeCount;
    
    public LearningModule() {
        this(0.1); // Default learning rate
    }
    
    public LearningModule(double learningRate) {
        this.learningRate = learningRate;
        this.strategyHistory = new HashMap<>();
        this.grammarHistory = new HashMap<>();
        this.episodeCount = 0;
    }
    
    /**
     * Update agent strategies based on negotiation outcome
     * @param buyer The buyer agent
     * @param seller The seller agent
     * @param result The negotiation result
     */
    public void updateAfterNegotiation(IAgent buyer, IAgent seller, NegotiationResult result) {
        episodeCount++;
        
        boolean success = result.isAgreed();
        double buyerUtility = result.getBuyerUtility();
        double sellerUtility = result.getSellerUtility();
        
        // Update buyer strategy
        updateAgentStrategy(buyer, success, buyerUtility);
        
        // Update seller strategy
        updateAgentStrategy(seller, success, sellerUtility);
        
        // Log learning progress
        if (episodeCount % 10 == 0) {
            System.out.println("Learning Episode " + episodeCount + 
                             " - Success Rate: " + calculateSuccessRate());
        }
    }
    
    /**
     * Update individual agent's strategy
     */
    private void updateAgentStrategy(IAgent agent, boolean success, double utility) {
        if (agent.getStrategy() instanceof StrategyProfile) {
            StrategyProfile strategy = (StrategyProfile) agent.getStrategy();
            
            // Record outcome
            String strategyId = agent.getName() + "_" + strategy.getStrategyName();
            StrategyAdjustment adj = strategyHistory.getOrDefault(
                strategyId, 
                new StrategyAdjustment(strategy.getStrategyName())
            );
            
            adj.recordOutcome(success, utility);
            strategyHistory.put(strategyId, adj);
            
            // Update strategy parameters
            strategy.updateParameters(success, utility);
        }
    }
    
    /**
     * Adjust grammar rule probabilities based on successful negotiations
     * @param rulesToReinforce List of rules that led to success
     */
    public void adjustGrammar(List<RuleExpansion> rulesToReinforce) {
        for (RuleExpansion rule : rulesToReinforce) {
            String ruleId = rule.getLhs() + "->" + String.join(",", rule.getRhs());
            
            GrammarAdjustment adj = grammarHistory.getOrDefault(
                ruleId,
                new GrammarAdjustment(ruleId)
            );
            
            adj.reinforceRule(learningRate);
            grammarHistory.put(ruleId, adj);
            
            // Update rule weight
            double currentWeight = rule.getWeight();
            double newWeight = currentWeight + (learningRate * (1.0 - currentWeight));
            rule.setWeight(newWeight);
        }
    }
    
    /**
     * Calculate success rate across all negotiations
     */
    public double calculateSuccessRate() {
        if (strategyHistory.isEmpty()) return 0.0;
        
        int totalSuccess = 0;
        int totalNegotiations = 0;
        
        for (StrategyAdjustment adj : strategyHistory.values()) {
            totalSuccess += adj.getSuccessCount();
            totalNegotiations += adj.getTotalCount();
        }
        
        return totalNegotiations > 0 ? (double) totalSuccess / totalNegotiations : 0.0;
    }
    
    /**
     * Get the best performing strategy
     */
    public String getBestStrategy() {
        String bestStrategy = null;
        double bestPerformance = -1.0;
        
        for (Map.Entry<String, StrategyAdjustment> entry : strategyHistory.entrySet()) {
            double performance = entry.getValue().getAverageUtility();
            if (performance > bestPerformance) {
                bestPerformance = performance;
                bestStrategy = entry.getValue().getStrategyName();
            }
        }
        
        return bestStrategy;
    }
    
    /**
     * Get performance statistics
     */
    public Map<String, Double> getPerformanceStats() {
        Map<String, Double> stats = new HashMap<>();
        
        stats.put("episode_count", (double) episodeCount);
        stats.put("success_rate", calculateSuccessRate());
        stats.put("learning_rate", learningRate);
        stats.put("strategies_tracked", (double) strategyHistory.size());
        stats.put("grammar_rules_tracked", (double) grammarHistory.size());
        
        return stats;
    }
    
    /**
     * Print learning summary
     */
    public void printSummary() {
        System.out.println("\n=== LEARNING MODULE SUMMARY ===");
        System.out.println("Episodes: " + episodeCount);
        System.out.println("Success Rate: " + String.format("%.2f%%", calculateSuccessRate() * 100));
        System.out.println("Best Strategy: " + getBestStrategy());
        
        System.out.println("\nStrategy Performance:");
        for (Map.Entry<String, StrategyAdjustment> entry : strategyHistory.entrySet()) {
            StrategyAdjustment adj = entry.getValue();
            System.out.println(String.format("  %s: %.2f avg utility, %d/%d success",
                adj.getStrategyName(),
                adj.getAverageUtility(),
                adj.getSuccessCount(),
                adj.getTotalCount()
            ));
        }
    }
    
    /**
     * Reset learning history
     */
    public void reset() {
        strategyHistory.clear();
        grammarHistory.clear();
        episodeCount = 0;
    }
    
    // Getters and setters
    public double getLearningRate() {
        return learningRate;
    }
    
    public void setLearningRate(double learningRate) {
        this.learningRate = Math.max(0.0, Math.min(1.0, learningRate));
    }
    
    public int getEpisodeCount() {
        return episodeCount;
    }
    
    /**
     * Inner class to track strategy performance
     */
    private static class StrategyAdjustment {
        private String strategyName;
        private int successCount;
        private int totalCount;
        private double totalUtility;
        
        public StrategyAdjustment(String strategyName) {
            this.strategyName = strategyName;
            this.successCount = 0;
            this.totalCount = 0;
            this.totalUtility = 0.0;
        }
        
        public void recordOutcome(boolean success, double utility) {
            totalCount++;
            totalUtility += utility;
            if (success) {
                successCount++;
            }
        }
        
        public String getStrategyName() {
            return strategyName;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public double getAverageUtility() {
            return totalCount > 0 ? totalUtility / totalCount : 0.0;
        }
    }
    
    /**
     * Inner class to track grammar rule adjustments
     */
    private static class GrammarAdjustment {
        private String ruleId;
        private int reinforcementCount;
        private double cumulativeWeight;
        
        public GrammarAdjustment(String ruleId) {
            this.ruleId = ruleId;
            this.reinforcementCount = 0;
            this.cumulativeWeight = 0.0;
        }
        
        public void reinforceRule(double amount) {
            reinforcementCount++;
            cumulativeWeight += amount;
        }
        
        public String getRuleId() {
            return ruleId;
        }
        
        public int getReinforcementCount() {
            return reinforcementCount;
        }
        
        public double getAverageWeight() {
            return reinforcementCount > 0 ? cumulativeWeight / reinforcementCount : 0.0;
        }
    }
}
