package dialogue;

import data.DatasetParser;
import models.NegotiationState;
import java.util.*;
import java.util.regex.Pattern;

public class ContextualDialogueGenerator {
    
    private Map<String, List<String>> utterancesByIntent;
    private Random random;
    private Pattern pricePattern;
    private List<String> conversationHistory;
    private NegotiationState currentState;
    private Map<String, Integer> vocab;
    private List<String> corpus;
    
    public ContextualDialogueGenerator(String datasetPath) throws Exception {
        this.random = new Random();
        this.pricePattern = Pattern.compile("\\$?\\s*(\\d+(\\.\\d{1,2})?)");
        this.conversationHistory = new ArrayList<>();
        this.vocab = new HashMap<>();
        this.corpus = new ArrayList<>();
        loadUtterances(datasetPath);
        buildVocab(datasetPath);
    }
    
    private void loadUtterances(String datasetPath) throws Exception {
        DatasetParser parser = new DatasetParser();
        List<DatasetParser.NegotiationExample> examples = parser.parseBuyerExamples(datasetPath);
        
        utterancesByIntent = new HashMap<>();
        for (DatasetParser.NegotiationExample ex : examples) {
            if (!ex.utterances.isEmpty() && !ex.intents.isEmpty()) {
                String intent = normalizeIntent(ex.intents.get(0));
                String utterance = ex.utterances.get(0).trim();
                
                if (utterance.length() > 10 && utterance.length() < 200) {
                    utterancesByIntent.computeIfAbsent(intent, k -> new ArrayList<>()).add(utterance);
                }
            }
        }
        
        System.out.println("Loaded utterances by intent:");
        for (Map.Entry<String, List<String>> entry : utterancesByIntent.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size() + " utterances");
        }
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

    private void buildVocab(String datasetPath) throws Exception {
        int idx = 0;
        for (String intent : utterancesByIntent.keySet()) {
            List<String> utterances = utterancesByIntent.get(intent);
            corpus.addAll(utterances);
            
            for (String utterance : utterances) {
                String[] words = utterance.toLowerCase().split("\\s+");
                for (String word : words) {
                    if (!vocab.containsKey(word)) {
                        vocab.put(word, idx++);
                    }
                }
            }
        }
    }

    private double[] computeTFIDF(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return new double[vocab.size()];
        }
        
        String[] words = sentence.toLowerCase().split("\\s+");
        double[] vec = new double[vocab.size()];
        
        for (String word : words) {
            if (vocab.containsKey(word)) {
                vec[vocab.get(word)] += 1;
            }
        }
        
        for (String word : words) {
            if (!vocab.containsKey(word)) continue;
            int idx = vocab.get(word);
            int docCount = 0;
            for (String doc : corpus) {
                if (doc.toLowerCase().contains(word)) {
                    docCount++;
                }
            }
            double idf = Math.log((double) corpus.size() / (docCount + 1));
            vec[idx] *= idf;
        }
        
        return vec;
    }
    
    public void resetConversation() {
        conversationHistory.clear();
        currentState = null;
    }
    
    public String generate(String intent, double price) {
        return generate(intent, price, null);
    }
    
    public String generate(String intent, double price, String opponentLastMessage) {
        String normalizedIntent = normalizeIntent(intent);
        List<String> candidates = utterancesByIntent.get(normalizedIntent);
        
        if (candidates == null || candidates.isEmpty()) {
            return getFallbackDialogue(normalizedIntent, price);
        }
        
        List<String> filteredCandidates = filterByContext(candidates, normalizedIntent, price, opponentLastMessage);
        
        if (filteredCandidates.isEmpty()) {
            filteredCandidates = candidates;
        }
        
        String template = filteredCandidates.get(random.nextInt(filteredCandidates.size()));
        return replacePrice(template, price);
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
 
    private List<String> filterByContext(List<String> candidates, String intent, double price, String opponentMessage) {
        List<String> filtered = new ArrayList<>();
        
        if (opponentMessage == null || opponentMessage.trim().isEmpty()) {
            return candidates;
        }
        
        double[] opponentVec = computeTFIDF(opponentMessage);
        
        for (String candidate : candidates) {
            double[] candidateVec = computeTFIDF(candidate);
            double similarity = cosine(candidateVec, opponentVec);
            
            if (similarity > 0.3) {
                filtered.add(candidate);
            }
        }
        
        if (filtered.isEmpty()) {
            return candidates;
        }
        
        return filtered;
    }
    
    private double calculateRelevance(String utterance, String intent, double price, String opponentMessage) {
        double score = 0.5;
        
        if (currentState != null) {
            double lastOffer = currentState.getLastOfferPrice();
            double opponentOffer = currentState.getOpponentLastOffer();
            
            if (intent.equals("COUNTER") || intent.equals("OFFER")) {
                double priceDiff = Math.abs(extractPriceFromUtterance(utterance) - price);
                double expectedDiff = Math.abs(price - lastOffer);
                
                if (priceDiff < expectedDiff * 1.5) {
                    score += 0.2;
                }
                
                if (price > lastOffer && utterance.toLowerCase().contains("higher")) {
                    score += 0.1;
                } else if (price < lastOffer && utterance.toLowerCase().contains("lower")) {
                    score += 0.1;
                }
            }
            
            if (intent.equals("REJECT")) {
                if (opponentOffer > lastOffer && utterance.toLowerCase().contains("high")) {
                    score += 0.2;
                } else if (opponentOffer < lastOffer && utterance.toLowerCase().contains("low")) {
                    score += 0.2;
                }
            }
            
            if (currentState.getRound() > 5 && utterance.toLowerCase().contains("final")) {
                score += 0.1;
            }
        }
        
        if (opponentMessage != null) {
            String lowerOpponent = opponentMessage.toLowerCase();
            String lowerUtterance = utterance.toLowerCase();
            
            if (lowerOpponent.contains("how about") && lowerUtterance.contains("how about")) {
                score += 0.1;
            }
            
            if (lowerOpponent.contains("deal") && lowerUtterance.contains("deal")) {
                score += 0.1;
            }
            
            if (lowerOpponent.contains("?") && lowerUtterance.contains("?")) {
                score += 0.05;
            }
        }
        
        if (!conversationHistory.isEmpty()) {
            String lastUtterance = conversationHistory.get(conversationHistory.size() - 1).toLowerCase();
            if (lastUtterance.contains("final") && utterance.toLowerCase().contains("final")) {
                score += 0.1;
            }
        }
        
        return Math.min(score, 1.0);
    }
    
    private double extractPriceFromUtterance(String utterance) {
        java.util.regex.Matcher matcher = pricePattern.matcher(utterance);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
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

   // hopefully i dont have to use this lmao, only used to start the project up for running, ignore this
    private String getFallbackDialogue(String intent, double price) {
        switch (intent) {
            case "OFFER":
                if (currentState != null && currentState.getRound() == 1) {
                    return "I'm interested in buying this. I can offer $" + String.format("%.2f", price) + ".";
                }
                return "I can offer you $" + String.format("%.2f", price) + ".";
            case "COUNTER":
                if (currentState != null && currentState.getRound() > 3) {
                    return "My final offer is $" + String.format("%.2f", price) + ".";
                }
                return "How about $" + String.format("%.2f", price) + "?";
            case "REJECT":
                if (currentState != null && currentState.getOpponentLastOffer() > currentState.getLastOfferPrice()) {
                    return "That's too high for me. I can't do $" + String.format("%.2f", price) + ".";
                }
                return "I can't do $" + String.format("%.2f", price) + ", that's too high.";
            case "ACCEPT":
                return "Deal! $" + String.format("%.2f", price) + " works for me.";
            default:
                return "I'm interested in this item.";
        }
    }
    
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
}


