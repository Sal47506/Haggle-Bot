package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DatasetParser {
    
    public static class NegotiationExample {
        public String role;
        public List<String> utterances;
        public List<String> intents;
        public List<Double> prices;
        
        public NegotiationExample() {
            utterances = new ArrayList<>();
            intents = new ArrayList<>();
            prices = new ArrayList<>();
        }
    }
    
    public List<NegotiationExample> parseBuyerExamples(String filePath) throws IOException {
        List<NegotiationExample> examples = new ArrayList<>();
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        
        JsonElement root = JsonParser.parseString(content);
        JsonArray dataArray;
        
        if (root.isJsonArray()) {
            dataArray = root.getAsJsonArray();
            System.out.println("Found JSON array with " + dataArray.size() + " elements");
        } else if (root.isJsonObject()) {
            dataArray = new JsonArray();
            dataArray.add(root.getAsJsonObject());
            System.out.println("Found JSON object, converted to array");
        } else {
            System.out.println("Unknown JSON type");
            return examples;
        }
        
        int processed = 0;
        for (JsonElement element : dataArray) {
            processed++;
            if (element == null || !element.isJsonObject()) continue;
            
            JsonObject example = element.getAsJsonObject();
            if (example == null) continue;
            
            JsonArray events = example.has("events") ? example.getAsJsonArray("events") : null;
            JsonArray actions = example.has("actions") ? example.getAsJsonArray("actions") : null;
            JsonElement agentsElement = example.has("agents") ? example.get("agents") : null;
            
            if (events == null || agentsElement == null) {
                if (processed <= 3) {
                    System.out.println("Element " + processed + ": Missing events or agents");
                }
                continue;
            }
            
            Map<String, String> agentRoles = new HashMap<>();
            
            if (agentsElement.isJsonArray()) {
                JsonArray agentsArray = agentsElement.getAsJsonArray();
                for (int j = 0; j < agentsArray.size(); j++) {
                    JsonElement agentElem = agentsArray.get(j);
                    if (agentElem != null && agentElem.isJsonObject()) {
                        JsonObject agentObj = agentElem.getAsJsonObject();
                        if (agentObj.has("id") && agentObj.has("role")) {
                            agentRoles.put(agentObj.get("id").getAsString(), agentObj.get("role").getAsString());
                        }
                    }
                }
            } else if (agentsElement.isJsonObject()) {
                JsonObject agentsObj = agentsElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : agentsObj.entrySet()) {
                    String agentId = entry.getKey();
                    JsonElement agentElem = entry.getValue();
                    if (agentElem != null && agentElem.isJsonObject()) {
                        JsonObject agentObj = agentElem.getAsJsonObject();
                        if (agentObj.has("role")) {
                            agentRoles.put(agentId, agentObj.get("role").getAsString());
                        } else if (agentObj.has("Role")) {
                            agentRoles.put(agentId, agentObj.get("Role").getAsString());
                        }
                    }
                }
            }
            
            if (processed == 1) {
                System.out.println("events size: " + events.size());
                if (events.size() > 0 && events.get(0).isJsonObject()) {
                    System.out.println("First event keys: " + events.get(0).getAsJsonObject().keySet());
                }
                System.out.println("agent roles: " + agentRoles);
            }
            
            for (int i = 0; i < events.size(); i++) {
                JsonElement eventElement = events.get(i);
                if (eventElement == null || !eventElement.isJsonObject()) continue;
                
                JsonObject event = eventElement.getAsJsonObject();
                
                if (processed == 1 && i < 3) {
                    System.out.println("Event " + i + " keys: " + event.keySet());
                }
                
                JsonElement agentElement = event.has("agent") ? event.get("agent") : 
                                         event.has("agent_id") ? event.get("agent_id") : null;
                JsonElement actionElement = event.has("action") ? event.get("action") : 
                                           event.has("type") ? event.get("type") : null;
                JsonElement dataElement = event.has("data") ? event.get("data") :
                                         event.has("text") ? event.get("text") :
                                         event.has("message") ? event.get("message") : null;
                
                if (agentElement == null || actionElement == null || dataElement == null) continue;
                
                String agent = agentElement.isJsonPrimitive() ? agentElement.getAsString() : null;
                String action = actionElement.isJsonPrimitive() ? actionElement.getAsString() : null;
                String data = dataElement.isJsonPrimitive() ? dataElement.getAsString() : null;
                
                if (agent == null || action == null || data == null) continue;
                
                String role = agentRoles.get(agent);
                boolean isBuyer = "buyer".equalsIgnoreCase(role) || 
                                  "buyer".equalsIgnoreCase(agent) || 
                                  "0".equals(agent);
                
                if (isBuyer && 
                    ("message".equalsIgnoreCase(action) || "offer".equalsIgnoreCase(action) || 
                     "utterance".equalsIgnoreCase(action) || "text".equalsIgnoreCase(action))) {
                    
                    NegotiationExample ex = new NegotiationExample();
                    ex.role = "buyer";
                    ex.utterances.add(data);
                    
                    String intent = inferIntent(data);
                    ex.intents.add(intent);
                    ex.prices.add(extractPrice(data));
                    
                    examples.add(ex);
                }
            }
        }
        
        System.out.println("Processed " + processed + " elements, found " + examples.size() + " buyer examples");
        return examples;
    }
    
    public Map<String, List<String>> groupBuyerUtterancesByIntent(List<NegotiationExample> examples) {
        Map<String, List<String>> intentGroups = new HashMap<>();
        
        for (NegotiationExample ex : examples) {
            String intent = normalizeIntent(ex.intents.get(0));
            String utterance = ex.utterances.get(0);
            intentGroups.computeIfAbsent(intent, k -> new ArrayList<>()).add(utterance);
        }
        
        return intentGroups;
    }
    
    private String normalizeIntent(String intent) {
        switch (intent.toLowerCase()) {
            case "init-price":
            case "offer":
                return "OFFER";
            case "counter":
            case "counter-offer":
                return "COUNTER";
            case "reject":
                return "REJECT";
            case "accept":
                return "ACCEPT";
            default:
                return "OTHER";
        }
    }
    
    private String inferIntent(String utterance) {
        String lower = utterance.toLowerCase();
        if (lower.contains("accept") || lower.contains("deal") || lower.contains("agreed")) {
            return "ACCEPT";
        } else if (lower.contains("reject") || lower.contains("can't") || lower.contains("won't") || lower.contains("no deal")) {
            return "REJECT";
        } else if (lower.contains("how about") || lower.contains("counter") || lower.contains("offer")) {
            return "COUNTER";
        } else if (lower.matches(".*\\$\\d+.*")) {
            return "OFFER";
        }
        return "OTHER";
    }
    
    private double extractPrice(String utterance) {
        try {
            String priceStr = utterance.replaceAll(".*\\$([0-9]+(?:\\.[0-9]{1,2})?).*", "$1");
            return Double.parseDouble(priceStr);
        } catch (Exception e) {
            return 0.0;
        }
    }
}

