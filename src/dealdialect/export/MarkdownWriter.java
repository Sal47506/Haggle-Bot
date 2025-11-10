package dealdialect.export;

import dealdialect.engine.*;
import dealdialect.metrics.Metrics;
import java.io.*;
import java.util.Map;

/**
 * Exports negotiation to Markdown format for human reading.
 */
public class MarkdownWriter {
    
    public static void saveNegotiation(DealContext context, DialogueState state, 
                                      String filename) throws IOException {
        StringBuilder md = new StringBuilder();
        
        // Title
        md.append("# Negotiation Transcript\n\n");
        
        // Context
        md.append("## Deal Context\n\n");
        md.append("- **Item**: ").append(context.getItem() != null ? context.getItem().getTitle() : "N/A").append("\n");
        md.append("- **MSRP**: $").append(String.format("%.2f", context.getMsrp())).append("\n");
        md.append("- **Buyer Maximum**: $").append(String.format("%.2f", context.getBuyerValue())).append("\n");
        md.append("- **Seller Minimum**: $").append(String.format("%.2f", context.getSellerCost())).append("\n");
        md.append("- **ZOPA Size**: $").append(String.format("%.2f", context.getZOPASize())).append("\n");
        md.append("- **Time Limit**: ").append(context.getTimeLimit()).append(" rounds\n\n");
        
        // Dialogue
        md.append("## Negotiation Dialogue\n\n");
        for (Offer offer : state.getHistory()) {
            md.append("### Round ").append(offer.getRoundNumber()).append(" - ").append(offer.getRole()).append("\n\n");
            md.append("**").append(offer.getRole()).append("**: ").append(offer.getUtterance()).append("\n\n");
            md.append("- Price: $").append(String.format("%.2f", offer.getPrice())).append("\n");
            md.append("- Intent: ").append(offer.getIntent()).append("\n");
            if (offer.isBluff()) {
                md.append("- ⚠️ **Bluff** (strength: ").append(String.format("%.2f", offer.getBluffStrength())).append(")\n");
            }
            md.append("\n");
        }
        
        // Outcome
        md.append("## Outcome\n\n");
        md.append("- **Status**: ").append(state.hasAgreement() ? "✅ AGREEMENT REACHED" : "❌ NO DEAL").append("\n");
        if (state.hasAgreement()) {
            md.append("- **Final Price**: $").append(String.format("%.2f", state.getDealPrice())).append("\n");
        }
        md.append("- **Total Rounds**: ").append(state.getCurrentRound()).append("\n");
        md.append("- **Buyer Final Trust**: ").append(String.format("%.2f", state.getBuyerTrust())).append("\n");
        md.append("- **Seller Final Trust**: ").append(String.format("%.2f", state.getSellerTrust())).append("\n\n");
        
        // Metrics
        md.append("## Metrics\n\n");
        Metrics metrics = new Metrics(context, state);
        Map<String, Object> allMetrics = metrics.calculateAll();
        
        md.append("### Economic Metrics\n\n");
        if (state.hasAgreement()) {
            md.append("- **Buyer Surplus**: $").append(String.format("%.2f", allMetrics.get("buyer_surplus"))).append("\n");
            md.append("- **Seller Surplus**: $").append(String.format("%.2f", allMetrics.get("seller_surplus"))).append("\n");
            md.append("- **Total Welfare**: $").append(String.format("%.2f", allMetrics.get("welfare"))).append("\n");
            md.append("- **Pareto Efficiency**: ").append(String.format("%.2f%%", (Double)allMetrics.get("pareto_efficiency") * 100)).append("\n");
            md.append("- **Fairness**: ").append(String.format("%.2f", allMetrics.get("fairness"))).append("\n\n");
        }
        
        md.append("### Behavioral Metrics\n\n");
        md.append("- **Buyer Bluffs**: ").append(allMetrics.get("buyer_bluff_count"));
        md.append(" (").append(String.format("%.0f%% success", (Double)allMetrics.get("buyer_bluff_success_rate") * 100)).append(")\n");
        md.append("- **Seller Bluffs**: ").append(allMetrics.get("seller_bluff_count"));
        md.append(" (").append(String.format("%.0f%% success", (Double)allMetrics.get("seller_bluff_success_rate") * 100)).append(")\n");
        
        // Write to file
        try (FileWriter file = new FileWriter(filename)) {
            file.write(md.toString());
        }
    }
}

