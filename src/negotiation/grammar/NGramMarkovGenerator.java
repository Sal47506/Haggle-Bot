package negotiation.grammar;

import negotiation.models.NegotiationTranscript;
import negotiation.models.DialogueAct;
import negotiation.strategy.MoveType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * N-gram Markov chain generator for negotiation language.
 * Learns from dataset conversations using n-grams and generates sentences probabilistically.
 */
public class NGramMarkovGenerator {
    
    // N-gram storage: context → {next_word: count}
    private Map<String, Map<String, Integer>> bigrams;    // word → {next: count}
    private Map<String, Map<String, Integer>> trigrams;    // "word1 word2" → {next: count}
    private Map<String, Map<String, Integer>> fourgrams;   // "w1 w2 w3" → {next: count}
    
    private static final String PRICE_TOKEN = "<PRICE>";
    private Random random;
    
    // Intent-specific models
    private Map<String, NGramMarkovGenerator> intentModels;
    
    public NGramMarkovGenerator() {
        this.bigrams = new HashMap<>();
        this.trigrams = new HashMap<>();
        this.fourgrams = new HashMap<>();
        this.intentModels = new HashMap<>();
        this.random = new Random();
    }
    
    /**
     * Train the model from dataset transcripts
     */
    public void train(List<NegotiationTranscript> transcripts) {
        // Group utterances by intent
        Map<String, List<String>> intentUtterances = new HashMap<>();
        List<String> allUtterances = new ArrayList<>();
        
        for (NegotiationTranscript t : transcripts) {
            List<String> utterances = t.getUtterances();
            List<DialogueAct> acts = t.getDialogueActs();
            
            for (int i = 0; i < utterances.size(); i++) {
                String utterance = utterances.get(i);
                if (utterance == null || utterance.trim().isEmpty()) continue;
                
                allUtterances.add(utterance);
                
                // Group by intent if available
                if (i < acts.size()) {
                    String intent = mapIntent(acts.get(i));
                    intentUtterances.computeIfAbsent(intent, k -> new ArrayList<>())
                                   .add(utterance);
                }
            }
        }
        
        // Train general model
        trainFromUtterances(allUtterances);
        
        // Train intent-specific models
        for (Map.Entry<String, List<String>> entry : intentUtterances.entrySet()) {
            NGramMarkovGenerator model = new NGramMarkovGenerator();
            model.trainFromUtterances(entry.getValue());
            intentModels.put(entry.getKey(), model);
        }
        
        System.out.println("Trained model with " + allUtterances.size() + " utterances");
        System.out.println("Intent models: " + intentModels.keySet());
    }
    
    /**
     * Train from list of utterances
     */
    private void trainFromUtterances(List<String> utterances) {
        for (String utterance : utterances) {
            // Normalize: replace prices with token
            String normalized = normalizePrice(utterance);
            
            // Tokenize
            String[] words = normalized.split("\\s+");
            if (words.length < 2) continue;
            
            // Build n-grams
            buildNGrams(words);
        }
    }
    
    /**
     * Build n-grams from word array
     */
    private void buildNGrams(String[] words) {
        // Bigrams: word[i] → word[i+1]
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            
            bigrams.computeIfAbsent(word1, k -> new HashMap<>())
                   .merge(word2, 1, Integer::sum);
        }
        
        // Trigrams: "word[i] word[i+1]" → word[i+2]
        for (int i = 0; i < words.length - 2; i++) {
            String key = words[i] + " " + words[i + 1];
            String next = words[i + 2];
            
            trigrams.computeIfAbsent(key, k -> new HashMap<>())
                    .merge(next, 1, Integer::sum);
        }
        
        // Fourgrams: "w[i] w[i+1] w[i+2]" → w[i+3]
        for (int i = 0; i < words.length - 3; i++) {
            String key = words[i] + " " + words[i + 1] + " " + words[i + 2];
            String next = words[i + 3];
            
            fourgrams.computeIfAbsent(key, k -> new HashMap<>())
                     .merge(next, 1, Integer::sum);
        }
    }
    
    /**
     * Normalize prices to token
     */
    private String normalizePrice(String utterance) {
        return utterance.replaceAll("\\$?\\d+(\\.\\d{1,2})?", PRICE_TOKEN);
    }
    
    /**
     * Generate sentence using Markov chain
     */
    public String generate(String intent, double price) {
        // Try intent-specific model first
        if (intentModels.containsKey(intent)) {
            String result = intentModels.get(intent).generateFromModel(intent, price);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        
        // Fallback to general model
        return generateFromModel(intent, price);
    }
    
    /**
     * Generate from this model
     */
    private String generateFromModel(String intent, double price) {
        // Start with seed words based on intent
        List<String> sentence = getSeedWords(intent);
        
        // Generate using Markov chain
        int maxLength = 20;
        while (!isComplete(sentence) && sentence.size() < maxLength) {
            String next = predictNext(sentence);
            if (next == null) break;
            sentence.add(next);
        }
        
        // Replace price token
        String result = String.join(" ", sentence);
        result = result.replace(PRICE_TOKEN, "$" + String.format("%.2f", price));
        
        // Post-process
        return postProcess(result);
    }
    
    /**
     * Predict next word using Markov chain
     */
    private String predictNext(List<String> sentence) {
        int size = sentence.size();
        
        // Try 4-gram first (most context)
        if (size >= 3) {
            String key = sentence.get(size-3) + " " + 
                        sentence.get(size-2) + " " + 
                        sentence.get(size-1);
            String next = sampleFrom(fourgrams.get(key));
            if (next != null) return next;
        }
        
        // Fallback to trigram
        if (size >= 2) {
            String key = sentence.get(size-2) + " " + sentence.get(size-1);
            String next = sampleFrom(trigrams.get(key));
            if (next != null) return next;
        }
        
        // Fallback to bigram
        if (size >= 1) {
            String next = sampleFrom(bigrams.get(sentence.get(size-1)));
            if (next != null) return next;
        }
        
        return null;
    }
    
    /**
     * Weighted random sampling (Markov chain transition)
     */
    private String sampleFrom(Map<String, Integer> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        
        // Calculate total count
        int total = candidates.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return null;
        
        // Weighted random selection
        int randomValue = random.nextInt(total);
        int cumulative = 0;
        
        for (Map.Entry<String, Integer> entry : candidates.entrySet()) {
            cumulative += entry.getValue();
            if (randomValue < cumulative) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Get seed words based on intent
     */
    private List<String> getSeedWords(String intent) {
        // Try to find common starting words from bigrams
        if (bigrams.containsKey("I")) {
            Map<String, Integer> nextWords = bigrams.get("I");
            String mostCommon = "can";
            int maxCount = 0;
            for (Map.Entry<String, Integer> entry : nextWords.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    mostCommon = entry.getKey();
                }
            }
            List<String> seeds = new ArrayList<>();
            seeds.add("I");
            seeds.add(mostCommon);
            return seeds;
        }
        
        // Fallback to intent-based seeds (use ArrayList for mutability)
        List<String> seeds = new ArrayList<>();
        switch (intent.toUpperCase()) {
            case "OFFER":
            case "INIT_PRICE":
                seeds.add("I");
                seeds.add("can");
                break;
            case "COUNTER":
            case "COUNTER_OFFER":
                seeds.add("How");
                seeds.add("about");
                break;
            case "ACCEPT":
                seeds.add("Deal");
                break;
            case "REJECT":
                seeds.add("I");
                seeds.add("can't");
                break;
            case "JUSTIFY":
                seeds.add("It's");
                break;
            case "THREATEN":
                seeds.add("I");
                seeds.add("might");
                break;
            default:
                seeds.add("I");
                break;
        }
        return seeds;
    }
    
    /**
     * Map DialogueAct intent to string
     */
    private String mapIntent(DialogueAct act) {
        if (act == null) return "UNKNOWN";
        MoveType moveType = act.getIntent();
        
        switch (moveType) {
            case INIT_PRICE:
                return "INIT_PRICE";
            case COUNTER_OFFER:
                return "COUNTER";
            case ACCEPT:
                return "ACCEPT";
            case REJECT:
                return "REJECT";
            case INQUIRE:
                return "JUSTIFY";
            case INSIST:
                return "THREATEN";
            default:
                return "OFFER";
        }
    }
    
    /**
     * Check if sentence is complete
     */
    private boolean isComplete(List<String> sentence) {
        if (sentence.isEmpty()) return false;
        String last = sentence.get(sentence.size() - 1);
        return last.endsWith("?") || last.endsWith(".") || last.endsWith("!");
    }
    
    /**
     * Post-process generated sentence
     */
    private String postProcess(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return "I can do it.";
        }
        
        // Capitalize first letter
        if (sentence.length() > 0) {
            sentence = sentence.substring(0, 1).toUpperCase() + 
                      sentence.substring(1);
        }
        
        // Add punctuation if missing
        if (!sentence.matches(".*[.!?]$")) {
            sentence += ".";
        }
        
        return sentence;
    }
    
    /**
     * Get statistics about the model
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== N-Gram Model Statistics ===\n");
        sb.append("Bigrams: ").append(bigrams.size()).append("\n");
        sb.append("Trigrams: ").append(trigrams.size()).append("\n");
        sb.append("Fourgrams: ").append(fourgrams.size()).append("\n");
        sb.append("Intent models: ").append(intentModels.size()).append("\n");
        
        // Show some example bigrams
        sb.append("\nExample bigrams:\n");
        int count = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : bigrams.entrySet()) {
            if (count++ >= 5) break;
            sb.append("  ").append(entry.getKey()).append(" → ")
              .append(entry.getValue().keySet()).append("\n");
        }
        
        return sb.toString();
    }
}

