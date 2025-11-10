package dealdialect.export;

import dealdialect.engine.*;
import dealdialect.metrics.Metrics;
import org.json.*;
import java.io.*;
import java.util.*;

/**
 * Exports negotiation to JSON format.
 */
public class JSONWriter {
    
    public static void saveNegotiation(DealContext context, DialogueState state, 
                                      String filename) throws IOException {
        JSONObject json = new JSONObject();
        
        // Context
        JSONObject contextJson = new JSONObject();
        contextJson.put("msrp", context.getMsrp());
        contextJson.put("buyer_value", context.getBuyerValue());
        contextJson.put("seller_cost", context.getSellerCost());
        contextJson.put("time_limit", context.getTimeLimit());
        contextJson.put("zopa_size", context.getZOPASize());
        if (context.getItem() != null) {
            contextJson.put("item", context.getItem().getTitle());
            contextJson.put("category", context.getItem().getCategory());
        }
        json.put("context", contextJson);
        
        // Offers
        JSONArray offersArray = new JSONArray();
        for (Offer offer : state.getHistory()) {
            JSONObject offerJson = new JSONObject();
            offerJson.put("round", offer.getRoundNumber());
            offerJson.put("role", offer.getRole().toString());
            offerJson.put("price", offer.getPrice());
            offerJson.put("utterance", offer.getUtterance());
            offerJson.put("intent", offer.getIntent().toString());
            offerJson.put("is_bluff", offer.isBluff());
            offerJson.put("bluff_strength", offer.getBluffStrength());
            offerJson.put("trust_after", offer.getTrustAfter());
            offersArray.put(offerJson);
        }
        json.put("offers", offersArray);
        
        // Outcome
        JSONObject outcome = new JSONObject();
        outcome.put("agreed", state.hasAgreement());
        outcome.put("final_price", state.getDealPrice());
        outcome.put("rounds", state.getCurrentRound());
        outcome.put("buyer_trust", state.getBuyerTrust());
        outcome.put("seller_trust", state.getSellerTrust());
        json.put("outcome", outcome);
        
        // Metrics
        Metrics metrics = new Metrics(context, state);
        Map<String, Object> allMetrics = metrics.calculateAll();
        JSONObject metricsJson = new JSONObject(allMetrics);
        json.put("metrics", metricsJson);
        
        // Write to file
        try (FileWriter file = new FileWriter(filename)) {
            file.write(json.toString(2));  // Pretty print with 2-space indent
        }
    }
}

