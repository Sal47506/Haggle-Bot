package dialogue;

import data.DatasetParser;
import models.NegotiationState;
import java.util.*;
import java.util.regex.Pattern;

public class MarkovDialogueGenerator implements DialogueGenerator {
    
    private Map<String, Map<String, List<String>>> markovModels;
    private Map<String, List<String>> seedUtterances;
    private Random random;
    private Pattern pricePattern;
    private List<String> conversationHistory;
    private NegotiationState currentState;
    private int order;
    private String itemContext;
    
    public MarkovDialogueGenerator(String datasetPath, int order) throws Exception {
        this.random = new Random();
        this.pricePattern = Pattern.compile("\\$?\\s*(\\d+(\\.\\d{1,2})?)");
        this.conversationHistory = new ArrayList<>();
        this.order = order;
        this.itemContext = "";
        buildMarkovModels(datasetPath);
    }
    
    public void setItemContext(String item) {
        this.itemContext = item != null ? item.toLowerCase() : "";
    }
    
    private void buildMarkovModels(String datasetPath) throws Exception {
        DatasetParser parser = new DatasetParser();
        List<DatasetParser.NegotiationExample> examples = parser.parseBuyerExamples(datasetPath);
        
        Map<String, List<String>> utterancesByIntent = new HashMap<>();
        seedUtterances = new HashMap<>();
        
        for (DatasetParser.NegotiationExample ex : examples) {
            if (!ex.utterances.isEmpty() && !ex.intents.isEmpty()) {
                String intent = normalizeIntent(ex.intents.get(0));
                String utterance = ex.utterances.get(0).trim();
                
                if (utterance.length() > 10 && utterance.length() < 200) {
                    utterancesByIntent.computeIfAbsent(intent, k -> new ArrayList<>()).add(utterance);
                }
            }
        }
        
        markovModels = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : utterancesByIntent.entrySet()) {
            String intent = entry.getKey();
            List<String> utterances = entry.getValue();
            
            seedUtterances.put(intent, new ArrayList<>(utterances));
            
            Map<String, List<String>> transitions = buildNGrams(utterances, order);
            markovModels.put(intent, transitions);
        }
        
        System.out.println("Built Markov models by intent:");
        for (Map.Entry<String, Map<String, List<String>>> entry : markovModels.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size() + " transitions, " + 
                             seedUtterances.get(entry.getKey()).size() + " seed utterances");
        }
    }
    
    private Map<String, List<String>> buildNGrams(List<String> utterances, int n) {
        Map<String, List<String>> transitions = new HashMap<>();
        
        for (String utterance : utterances) {
            String normalized = normalizePrice(utterance);
            List<String> tokens = tokenize(normalized);
            
            if (tokens.size() < n) continue;
            
            for (int i = 0; i <= tokens.size() - n; i++) {
                List<String> context = new ArrayList<>();
                for (int j = 0; j < n - 1; j++) {
                    context.add(tokens.get(i + j));
                }
                String key = String.join(" ", context);
                String next = tokens.get(i + n - 1);
                
                transitions.computeIfAbsent(key, k -> new ArrayList<>()).add(next);
            }
        }
        
        return transitions;
    }
    
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\s+");
        
        for (String word : words) {
            if (word.isEmpty()) continue;
            
            if (word.matches(".*[.,!?;:]+$")) {
                String base = word.replaceAll("[.,!?;:]+$", "");
                if (!base.isEmpty()) {
                    tokens.add(base);
                }
                String punct = word.substring(base.length());
                tokens.add(punct);
            } else {
                tokens.add(word);
            }
        }
        
        return tokens;
    }
    
    private String normalizePrice(String utterance) {
        return utterance.replaceAll("\\$?\\s*\\d+(\\.\\d{1,2})?", "<PRICE>");
    }
    
    private String normalizeIntent(String intent) {
        if (intent == null) return "OTHER";
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
    
    public void updateContext(NegotiationState state, String lastMessage) {
        this.currentState = state;
        if (lastMessage != null && !lastMessage.trim().isEmpty()) {
            conversationHistory.add(lastMessage);
            if (conversationHistory.size() > 10) {
                conversationHistory.remove(0);
            }
        }
    }
    
    public void resetConversation() {
        conversationHistory.clear();
        currentState = null;
    }
    
    public String generate(String intent, double price) {
        return generate(intent, price, null);
    }
    
    public String generate(String intent, double price, String opponentMessage) {
        String normalizedIntent = normalizeIntent(intent);
        
        List<String> seedCandidates = seedUtterances.get(normalizedIntent);
        
        if (seedCandidates == null || seedCandidates.isEmpty()) {
            return getFallbackDialogue(normalizedIntent, price);
        }
        
        List<String> filteredSeeds = filterSeedsByContext(seedCandidates, normalizedIntent, price, opponentMessage);
        
        if (filteredSeeds.isEmpty()) {
            filteredSeeds = seedCandidates;
        }
        
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String seed = filteredSeeds.get(random.nextInt(Math.min(50, filteredSeeds.size())));
            String generated = generateFromSeed(null, seed, normalizedIntent, price);
            
            if (isValidGeneration(generated, price)) {
                return generated;
            }
        }
        
        return getFallbackDialogue(normalizedIntent, price);
    }
    
    private boolean isValidGeneration(String text, double price) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        if (text.contains("<PRICE>") || text.contains("<price>")) {
            return false;
        }
        
        if (text.length() < 10) {
            return false;
        }
        
        String priceStr = String.format("%.2f", price);
        if (!text.contains(priceStr) && !text.contains("$")) {
            if (text.toLowerCase().contains("offer") || text.toLowerCase().contains("how about")) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<String> filterSeedsByContext(List<String> seeds, String intent, double price, String opponentMessage) {
        if (seeds == null || seeds.isEmpty()) return new ArrayList<>();
        List<String> itemsFiltered = new ArrayList<>(); 
        
        for (String seed : seeds) {
            if (calculateSeedRelevance(seed, intent, price, opponentMessage) > 0.5) {
                itemsFiltered.add(seed);
            }
        }
        return itemsFiltered;

    }
    
    private double calculateSeedRelevance(String seed, String intent, double price, String opponentMessage) {
        double score = 0.5;
        String lowerSeed = seed.toLowerCase();
        
        boolean isBluff = lowerSeed.contains("budget") || lowerSeed.contains("can't afford") || 
                         lowerSeed.contains("other") || lowerSeed.contains("elsewhere") ||
                         lowerSeed.contains("firm") || lowerSeed.contains("final");
        
        if (isBluff && random.nextDouble() < 0.4) {
            score += 0.3;
        }
        
        if (currentState != null) {
            double lastOffer = currentState.getLastOfferPrice();
            double opponentOffer = currentState.getOpponentLastOffer();
            
            if (intent.equals("COUNTER") || intent.equals("OFFER")) {
                if (price > lastOffer) {
                    if (lowerSeed.contains("higher") || lowerSeed.contains("more") || lowerSeed.contains("increase")) {
                        score += 0.2;
                    }
                    if (lowerSeed.contains("meet") || lowerSeed.contains("halfway") || lowerSeed.contains("middle")) {
                        score += 0.15;
                    }
                } else if (price < lastOffer && lowerSeed.contains("lower")) {
                    score += 0.2;
                }
                
                if (currentState.getRound() > 5) {
                    if (lowerSeed.contains("final") || lowerSeed.contains("best") || lowerSeed.contains("last")) {
                        score += 0.2;
                    }
                }
                
                if (currentState.getRound() < 3 && isBluff) {
                    score += 0.2;
                }
            }
            
            if (intent.equals("REJECT")) {
                if (opponentOffer > lastOffer * 1.5) {
                    if (lowerSeed.contains("high") || lowerSeed.contains("too much") || lowerSeed.contains("expensive")) {
                        score += 0.3;
                    }
                }
            }
        }
        
        if (opponentMessage != null) {
            String lowerOpponent = opponentMessage.toLowerCase();
            if (lowerOpponent.contains("?") && lowerSeed.contains("?")) {
                score += 0.1;
            }
            if (lowerOpponent.contains("final") && lowerSeed.contains("final")) {
                score += 0.15;
            }
            if (lowerOpponent.contains("no") || lowerOpponent.contains("can't")) {
                if (isBluff) {
                    score += 0.2;
                }
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    private String generateFromSeed(Map<String, List<String>> model, String seedUtterance, String intent, double price) {
        String directReplacement = replacePrice(seedUtterance, price);
        
        if (!directReplacement.contains("<PRICE>") && directReplacement.length() > 10) {
            return directReplacement;
        }
        
        return directReplacement;
    }
    
    private String replacePrice(String utterance, double targetPrice) {
        java.util.regex.Matcher matcher = pricePattern.matcher(utterance);
        String priceStr = "$" + String.format("%.2f", targetPrice);
        String result = matcher.replaceAll(java.util.regex.Matcher.quoteReplacement(priceStr));
        
        if (!result.contains("$")) {
            if (utterance.toLowerCase().contains("how about") || utterance.toLowerCase().contains("offer")) {
                result = result.trim();
                if (!result.endsWith(".") && !result.endsWith("?") && !result.endsWith("!")) {
                    result += " " + priceStr + "?";
                } else {
                    result = result.substring(0, result.length() - 1) + " " + priceStr + result.substring(result.length() - 1);
                }
            } else if (utterance.toLowerCase().contains("deal") || utterance.toLowerCase().contains("accept")) {
                result = result.trim();
                if (!result.endsWith(".") && !result.endsWith("?") && !result.endsWith("!")) {
                    result += " for " + priceStr + ".";
                }
            }
        }
        
        return result.trim();
    }
    
    private String getFallbackDialogue(String intent, double price) {
        String item = !itemContext.isEmpty() ? itemContext : "this";
        
        switch (intent) {
            case "OFFER":
                if (currentState != null && currentState.getRound() == 1) {
                    return "I'm interested in " + item + ". I can offer $" + String.format("%.2f", price) + ".";
                }
                return "I can offer you $" + String.format("%.2f", price) + " for " + item + ".";
            case "COUNTER":
                if (currentState != null && currentState.getRound() > 5) {
                    return "I really can't go higher than $" + String.format("%.2f", price) + ". That's my final offer.";
                }
                return "How about $" + String.format("%.2f", price) + "?";
            case "REJECT":
                if (currentState != null && currentState.getConsecutiveRejects() > 1) {
                    return "I found " + item + " elsewhere for less. I'll stick with $" + String.format("%.2f", price) + ".";
                }
                return "That's too high. I can only do $" + String.format("%.2f", price) + ".";
            case "ACCEPT":
                return "Deal! $" + String.format("%.2f", price) + " works for me.";
            default:
                return "I'm interested in " + item + ".";
        }
    }
    
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
}

