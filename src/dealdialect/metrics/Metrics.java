package dealdialect.metrics;

import dealdialect.engine.*;
import java.util.*;

/**
 * Comprehensive metrics calculator for negotiation outcomes.
 * Tracks surplus, efficiency, bluffs, trust, and performance.
 */
public class Metrics {
    private DealContext context;
    private DialogueState state;
    private Map<String, Object> calculatedMetrics;
    
    public Metrics(DealContext context, DialogueState state) {
        this.context = context;
        this.state = state;
        this.calculatedMetrics = new HashMap<>();
    }
    
    /**
     * Calculate all metrics
     */
    public Map<String, Object> calculateAll() {
        calculatedMetrics.clear();
        
        // Basic outcome metrics
        calculatedMetrics.put("agreed", state.hasAgreement());
        calculatedMetrics.put("rounds", state.getCurrentRound());
        calculatedMetrics.put("deal_price", state.getDealPrice());
        
        if (state.hasAgreement() && state.getDealPrice() != null) {
            double dealPrice = state.getDealPrice();
            
            // Surplus metrics
            calculatedMetrics.putAll(calculateSurplusMetrics(dealPrice));
            
            // Efficiency metrics
            calculatedMetrics.putAll(calculateEfficiencyMetrics(dealPrice));
            
            // Concession metrics
            calculatedMetrics.putAll(calculateConcessionMetrics());
        }
        
        // Trust metrics
        calculatedMetrics.putAll(calculateTrustMetrics());
        
        // Bluff metrics
        calculatedMetrics.putAll(calculateBluffMetrics());
        
        // Negotiation dynamics
        calculatedMetrics.putAll(calculateDynamicsMetrics());
        
        return calculatedMetrics;
    }
    
    /**
     * Calculate surplus-related metrics
     */
    private Map<String, Object> calculateSurplusMetrics(double dealPrice) {
        Map<String, Object> metrics = new HashMap<>();
        
        double buyerSurplus = context.getBuyerValue() - dealPrice;
        double sellerSurplus = dealPrice - context.getSellerCost();
        double totalSurplus = buyerSurplus + sellerSurplus;
        
        metrics.put("buyer_surplus", buyerSurplus);
        metrics.put("seller_surplus", sellerSurplus);
        metrics.put("total_surplus", totalSurplus);
        metrics.put("welfare", totalSurplus);
        
        // Surplus Share Compromise (SSC) - how surplus is divided
        if (totalSurplus > 0) {
            double buyerShare = buyerSurplus / totalSurplus;
            double sellerShare = sellerSurplus / totalSurplus;
            metrics.put("buyer_surplus_share", buyerShare);
            metrics.put("seller_surplus_share", sellerShare);
            
            // Fairness metric (1.0 = perfectly fair, 0.0 = completely unfair)
            double fairness = 1.0 - Math.abs(buyerShare - 0.5) * 2;
            metrics.put("fairness", fairness);
        }
        
        return metrics;
    }
    
    /**
     * Calculate efficiency metrics
     */
    private Map<String, Object> calculateEfficiencyMetrics(double dealPrice) {
        Map<String, Object> metrics = new HashMap<>();
        
        double maxPossibleSurplus = context.getZOPASize();
        double actualSurplus = (context.getBuyerValue() - dealPrice) + 
                              (dealPrice - context.getSellerCost());
        
        // Pareto efficiency
        double paretoEfficiency = maxPossibleSurplus > 0 ? 
            actualSurplus / maxPossibleSurplus : 1.0;
        metrics.put("pareto_efficiency", paretoEfficiency);
        
        // Deadweight loss
        double deadweightLoss = Math.max(0, maxPossibleSurplus - actualSurplus);
        metrics.put("deadweight_loss", deadweightLoss);
        
        // Exploitability (how much one side dominated)
        double buyerSurplus = context.getBuyerValue() - dealPrice;
        double sellerSurplus = dealPrice - context.getSellerCost();
        double exploitability = Math.abs(buyerSurplus - sellerSurplus) / 
                               Math.max(buyerSurplus + sellerSurplus, 1.0);
        metrics.put("exploitability", exploitability);
        
        return metrics;
    }
    
    /**
     * Calculate concession metrics
     */
    private Map<String, Object> calculateConcessionMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        for (Role role : Role.values()) {
            List<Offer> offers = state.getOffersFrom(role);
            if (offers.size() < 2) continue;
            
            double totalConcession = 0.0;
            int concessionCount = 0;
            List<Double> concessions = new ArrayList<>();
            
            for (int i = 1; i < offers.size(); i++) {
                double concession = offers.get(i).getConcessionAmount(offers.get(i-1));
                if (concession > 0) {
                    totalConcession += concession;
                    concessionCount++;
                    concessions.add(concession);
                }
            }
            
            String prefix = role.name().toLowerCase();
            metrics.put(prefix + "_total_concession", totalConcession);
            metrics.put(prefix + "_avg_concession", 
                       concessionCount > 0 ? totalConcession / concessionCount : 0.0);
            
            // Concession efficiency (how quickly they reached agreement)
            double concessionEfficiency = concessionCount > 0 ? 
                totalConcession / concessionCount / state.getCurrentRound() : 0.0;
            metrics.put(prefix + "_concession_efficiency", concessionEfficiency);
            
            // Concession variance (consistency of concessions)
            if (concessions.size() > 1) {
                double avg = totalConcession / concessionCount;
                double variance = concessions.stream()
                    .mapToDouble(c -> Math.pow(c - avg, 2))
                    .average().orElse(0.0);
                metrics.put(prefix + "_concession_variance", variance);
            }
        }
        
        return metrics;
    }
    
    /**
     * Calculate trust trajectory metrics
     */
    private Map<String, Object> calculateTrustMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("buyer_final_trust", state.getBuyerTrust());
        metrics.put("seller_final_trust", state.getSellerTrust());
        metrics.put("avg_trust", (state.getBuyerTrust() + state.getSellerTrust()) / 2.0);
        
        // Trust degradation (from initial 1.0)
        metrics.put("buyer_trust_degradation", 1.0 - state.getBuyerTrust());
        metrics.put("seller_trust_degradation", 1.0 - state.getSellerTrust());
        
        return metrics;
    }
    
    /**
     * Calculate bluff-related metrics
     */
    private Map<String, Object> calculateBluffMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        for (Role role : Role.values()) {
            String prefix = role.name().toLowerCase();
            
            int totalBluffs = role == Role.BUYER ? 
                state.getBuyerBluffCount() : state.getSellerBluffCount();
            int detected = role == Role.BUYER ? 
                state.getBuyerBluffDetected() : state.getSellerBluffDetected();
            
            metrics.put(prefix + "_bluff_count", totalBluffs);
            metrics.put(prefix + "_bluff_detected", detected);
            
            double successRate = totalBluffs > 0 ? 
                1.0 - ((double) detected / totalBluffs) : 1.0;
            metrics.put(prefix + "_bluff_success_rate", successRate);
        }
        
        return metrics;
    }
    
    /**
     * Calculate negotiation dynamics metrics
     */
    private Map<String, Object> calculateDynamicsMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Negotiation length
        metrics.put("negotiation_duration", state.getCurrentRound());
        
        // Offer counts
        List<Offer> buyerOffers = state.getOffersFrom(Role.BUYER);
        List<Offer> sellerOffers = state.getOffersFrom(Role.SELLER);
        metrics.put("buyer_offer_count", buyerOffers.size());
        metrics.put("seller_offer_count", sellerOffers.size());
        
        // Price convergence (how close final offers were)
        if (!buyerOffers.isEmpty() && !sellerOffers.isEmpty()) {
            double lastBuyerPrice = buyerOffers.get(buyerOffers.size() - 1).getPrice();
            double lastSellerPrice = sellerOffers.get(sellerOffers.size() - 1).getPrice();
            double convergence = Math.abs(lastBuyerPrice - lastSellerPrice);
            metrics.put("price_convergence", convergence);
            
            // Convergence rate
            if (state.getCurrentRound() > 0) {
                metrics.put("convergence_rate", convergence / state.getCurrentRound());
            }
        }
        
        // Time pressure utilization
        double timePressure = (double) state.getCurrentRound() / context.getTimeLimit();
        metrics.put("time_pressure", timePressure);
        
        return metrics;
    }
    
    /**
     * Get a specific metric
     */
    public Object getMetric(String name) {
        return calculatedMetrics.get(name);
    }
    
    /**
     * Get all calculated metrics
     */
    public Map<String, Object> getAllMetrics() {
        return new HashMap<>(calculatedMetrics);
    }
    
    /**
     * Generate a summary string
     */
    public String generateSummary() {
        calculateAll();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== NEGOTIATION METRICS ===\n\n");
        
        sb.append("Outcome: ").append(state.hasAgreement() ? "AGREEMENT" : "NO DEAL").append("\n");
        if (state.hasAgreement()) {
            sb.append(String.format("Deal Price: $%.2f\n", state.getDealPrice()));
            sb.append(String.format("Buyer Surplus: $%.2f\n", getMetric("buyer_surplus")));
            sb.append(String.format("Seller Surplus: $%.2f\n", getMetric("seller_surplus")));
            sb.append(String.format("Total Welfare: $%.2f\n", getMetric("welfare")));
            sb.append(String.format("Pareto Efficiency: %.2f%%\n", 
                (Double)getMetric("pareto_efficiency") * 100));
            sb.append(String.format("Fairness: %.2f\n", getMetric("fairness")));
        }
        
        sb.append(String.format("\nRounds: %d\n", state.getCurrentRound()));
        sb.append(String.format("Buyer Trust: %.2f\n", state.getBuyerTrust()));
        sb.append(String.format("Seller Trust: %.2f\n", state.getSellerTrust()));
        
        sb.append(String.format("\nBuyer Bluffs: %d (%.0f%% success)\n",
            getMetric("buyer_bluff_count"),
            (Double)getMetric("buyer_bluff_success_rate") * 100));
        sb.append(String.format("Seller Bluffs: %d (%.0f%% success)\n",
            getMetric("seller_bluff_count"),
            (Double)getMetric("seller_bluff_success_rate") * 100));
        
        return sb.toString();
    }
}

