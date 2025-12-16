package dialogue;

import data.DatasetParser;
import models.NegotiationState;
import java.util.*;
import java.util.regex.Pattern;

public class ContextualDialogueGenerator implements DialogueGenerator {
    
    private Map<String, List<String>> utterancesByIntent;
    private Random random;
    private Pattern pricePattern;
    private List<String> conversationHistory;
    private NegotiationState currentState;
    private Map<String, Integer> vocab;
    private List<String> corpus;
    private String itemContext;
    private Set<String> itemContextTokens;
    private Set<String> likelyItemWords;

    // Minimal stopword list to avoid treating generic words as "items"
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the","a","an","and","or","but","if","then","else","this","that","these","those",
        "i","me","my","mine","you","your","yours","we","us","our","ours","they","them","their","theirs",
        "it","its","is","are","was","were","be","been","being","to","of","in","on","at","for","with","from","as",
        "have","has","had","do","does","did","can","could","will","would","should","might","may","must",
        "not","no","yes","ok","okay","sure","thanks","thank","please",
        "price","money","cash","deal","offer","offers","offering","buy","sell","selling","purchase",
        "dollar","dollars","buck","bucks","usd",
        "still","available","interested","question","questions","today","tomorrow","now"
    ));
    
    public ContextualDialogueGenerator(String datasetPath) throws Exception {
        this.random = new Random();
        this.pricePattern = Pattern.compile("\\$?\\s*(\\d+(\\.\\d{1,2})?)");
        this.conversationHistory = new ArrayList<>();
        this.vocab = new HashMap<>();
        this.corpus = new ArrayList<>();
        this.itemContext = null;
        this.itemContextTokens = new HashSet<>();
        this.likelyItemWords = new HashSet<>();
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

        // Build a lightweight list of "likely item words" from common determiner patterns in the corpus.
        // Example: "the stereo", "this couch", "your bike" -> stereo/couch/bike are likely item tokens.
        buildLikelyItemWords();
    }

    private void buildLikelyItemWords() {
        Map<String, Integer> counts = new HashMap<>();
        for (String doc : corpus) {
            if (doc == null) continue;
            String[] toks = tokenize(doc);
            for (int i = 0; i + 1 < toks.length; i++) {
                String t = toks[i];
                if (t == null) continue;
                if (t.equals("the") || t.equals("this") || t.equals("that") || t.equals("your") || t.equals("my") || t.equals("a") || t.equals("an")) {
                    String next = toks[i + 1];
                    if (next == null || next.length() < 3) continue;
                    if (STOPWORDS.contains(next)) continue;
                    if (next.matches("\\d+")) continue;
                    counts.put(next, counts.getOrDefault(next, 0) + 1);
                }
            }
        }

        // Keep the most common N candidates to avoid an unbounded set.
        final int MAX_WORDS = 600;
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(MAX_WORDS, entries.size()); i++) {
            likelyItemWords.add(entries.get(i).getKey());
        }
    }

    private String[] tokenize(String text) {
        if (text == null) return new String[0];
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        if (cleaned.isEmpty()) return new String[0];
        return cleaned.split("\\s+");
    }

    private boolean candidateMentionsContextItem(String candidateLower) {
        if (itemContextTokens == null || itemContextTokens.isEmpty()) return false;
        String[] toks = tokenize(candidateLower);
        Set<String> tokSet = new HashSet<>(Arrays.asList(toks));
        for (String ctx : itemContextTokens) {
            if (tokSet.contains(ctx)) return true;
        }
        return false;
    }

    private boolean candidateMentionsOtherLikelyItem(String candidateLower) {
        if (likelyItemWords == null || likelyItemWords.isEmpty()) return false;
        String[] toks = tokenize(candidateLower);
        for (String t : toks) {
            if (t.length() < 3) continue;
            if (likelyItemWords.contains(t)) return true;
        }
        return false;
    }

    private boolean shouldExcludeCandidateForItemMismatch(String candidate) {
        if (itemContextTokens == null || itemContextTokens.isEmpty()) return false;
        if (candidate == null || candidate.trim().isEmpty()) return false;
        String lower = candidate.toLowerCase();

        // If a candidate clearly mentions *some* likely item token but doesn't mention our context item,
        // drop it to prevent "stereo" showing up in a "soda" negotiation, etc.
        boolean mentionsContext = candidateMentionsContextItem(lower);
        boolean mentionsSomeItem = candidateMentionsOtherLikelyItem(lower);
        return mentionsSomeItem && !mentionsContext;
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
            // No opponent text to match against; still avoid obvious item-mismatch candidates if we have an item context.
            if (itemContextTokens != null && !itemContextTokens.isEmpty()) {
                for (String candidate : candidates) {
                    if (!shouldExcludeCandidateForItemMismatch(candidate)) {
                        filtered.add(candidate);
                    }
                }
                return filtered.isEmpty() ? candidates : filtered;
            }
            return candidates;
        }
        
        // Anchor similarity on both the opponent message and the item context (when available)
        String query = opponentMessage;
        if (itemContext != null && !itemContext.trim().isEmpty()) {
            query = query + " " + itemContext;
        }
        double[] opponentVec = computeTFIDF(query);
        
        for (String candidate : candidates) {
            if (shouldExcludeCandidateForItemMismatch(candidate)) {
                continue;
            }
            double[] candidateVec = computeTFIDF(candidate);
            double similarity = cosine(candidateVec, opponentVec);
            
            if (similarity > 0.3) {
                filtered.add(candidate);
            }
        }
        
        if (filtered.isEmpty()) {
            // If similarity filtering yields nothing, prefer "safe" candidates (no obvious other-item mentions)
            // over falling back to the full pool (which is how off-topic items leak in).
            List<String> safe = new ArrayList<>();
            for (String candidate : candidates) {
                if (!shouldExcludeCandidateForItemMismatch(candidate)) {
                    safe.add(candidate);
                }
            }
            return safe.isEmpty() ? candidates : safe;
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
    
    @Override
    public void setItemContext(String item) {
        if (item == null) {
            this.itemContext = null;
            this.itemContextTokens = new HashSet<>();
            return;
        }
        this.itemContext = item.trim().toLowerCase();
        this.itemContextTokens = new HashSet<>();
        for (String t : tokenize(this.itemContext)) {
            if (t.length() < 2) continue;
            if (STOPWORDS.contains(t)) continue;
            this.itemContextTokens.add(t);
        }
    }
}


