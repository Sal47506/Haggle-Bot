package negotiation.grammar;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Grammar-based sentence generator for negotiation dialogue.
 * Extends the SentenceGenerator concept with phrase structure rules
 * for natural language generation in haggling scenarios.
 */
public class GrammarEngine {
    
    // Non-terminal categories (phrasal + lexical)
    public final static List<String> nonTerminalCategories =
        Arrays.asList("S", "NP", "VP", "AP", "PP", "N", "V", "A", "D", "ADV", "CONJ", "PRON");
    
    // Word categories only
    public final static List<String> wordCategories =
        Arrays.asList("N", "V", "A", "D", "ADV", "CONJ", "PRON");
    
    private Map<String, List<String>> rules;
    private Map<String, List<String>> words;
    private Map<String, List<RuleExpansion>> weightedRules;
    private Random random;
    
    public GrammarEngine() {
        this.rules = new LinkedHashMap<>();
        this.words = new LinkedHashMap<>();
        this.weightedRules = new LinkedHashMap<>();
        this.random = new Random();
    }
    
    /**
     * Read phrase structure rules from file and store in map.
     * Format: "NP D,N" represents NP -> D N
     * @param s Scanner for reading rules file
     * @return Map of category to list of expansions
     */
    public Map<String, List<String>> getRules(Scanner s) {
        while (s.hasNextLine()) {
            String line = s.nextLine().trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            String[] parts = line.split("\\s+", 2);
            if (parts.length < 2) continue;
            
            String lhs = parts[0];
            String[] rhs = parts[1].split(",");
            
            if (!rules.containsKey(lhs)) {
                rules.put(lhs, new ArrayList<>());
            }
            rules.get(lhs).add(parts[1]);
        }
        return rules;
    }
    
    /**
     * Read dictionary of tagged words from file.
     * Format: "price,N" stores "price" as a noun
     * @param s Scanner for reading dictionary file
     * @return Map of category to list of words
     */
    public Map<String, List<String>> getWords(Scanner s) {
        while (s.hasNextLine()) {
            String line = s.nextLine().trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            String[] parts = line.split(",");
            if (parts.length < 2) continue;
            
            String word = parts[0].trim();
            String category = parts[1].trim();
            
            if (!words.containsKey(category)) {
                words.put(category, new ArrayList<>());
            }
            words.get(category).add(word);
        }
        return words;
    }
    
    /**
     * Load rules from file path
     */
    public void loadRules(File file) throws FileNotFoundException {
        Scanner s = new Scanner(file);
        getRules(s);
        s.close();
    }
    
    /**
     * Load words from file path
     */
    public void loadWords(File file) throws FileNotFoundException {
        Scanner s = new Scanner(file);
        getWords(s);
        s.close();
    }
    
    /**
     * Check if parse contains any non-terminal categories
     * @param parse Current parse list
     * @return true if contains non-terminal
     */
    public boolean containsNonTerminal(List<String> parse) {
        for (String item : parse) {
            if (nonTerminalCategories.contains(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Select a random word of specified category and replace it in parse
     * @param category The word category (N, V, A, etc.)
     * @param words Dictionary of words
     * @param parse Current parse
     * @return Updated parse with word replaced
     */
    public List<String> selectWord(String category, Map<String, List<String>> words, 
                                   List<String> parse) {
        List<String> newParse = new ArrayList<>(parse);
        
        if (!words.containsKey(category) || words.get(category).isEmpty()) {
            return newParse;
        }
        
        // Randomly select a word
        List<String> wordList = words.get(category);
        String selectedWord = wordList.get(random.nextInt(wordList.size()));
        
        // Find and replace the category in parse
        int index = newParse.indexOf(category);
        if (index >= 0) {
            newParse.set(index, selectedWord);
        }
        
        return newParse;
    }
    
    /**
     * Select a random rule expansion and replace category in parse
     * @param category The category to expand (NP, VP, etc.)
     * @param rules Map of rules
     * @param parse Current parse
     * @return Updated parse with rule expanded
     */
    public List<String> selectRule(String category, Map<String, List<String>> rules, 
                                   List<String> parse) {
        List<String> newParse = new ArrayList<>();
        
        if (!rules.containsKey(category) || rules.get(category).isEmpty()) {
            return new ArrayList<>(parse);
        }
        
        // Randomly select a rule expansion
        List<String> expansions = rules.get(category);
        String selectedExpansion = expansions.get(random.nextInt(expansions.size()));
        String[] expandedParts = selectedExpansion.split(",");
        
        // Find the category in parse and replace it
        int index = parse.indexOf(category);
        if (index < 0) {
            return new ArrayList<>(parse);
        }
        
        // Build new parse: before + expansion + after
        newParse.addAll(parse.subList(0, index));
        newParse.addAll(Arrays.asList(expandedParts));
        if (index + 1 < parse.size()) {
            newParse.addAll(parse.subList(index + 1, parse.size()));
        }
        
        return newParse;
    }
    
    /**
     * Generate a complete sentence using rules and words.
     * Parses top-down, left-to-right.
     * @param rules Phrase structure rules
     * @param words Dictionary of words
     * @return List of parses showing derivation steps
     */
    public List<List<String>> generateSentence(Map<String, List<String>> rules, 
                                                Map<String, List<String>> words) {
        List<List<String>> parses = new ArrayList<>();
        List<String> currentParse = new ArrayList<>();
        currentParse.add("S");
        parses.add(new ArrayList<>(currentParse));
        
        while (containsNonTerminal(currentParse)) {
            // Find leftmost non-terminal
            for (int i = 0; i < currentParse.size(); i++) {
                String item = currentParse.get(i);
                
                if (wordCategories.contains(item)) {
                    // It's a lexical category - select a word
                    currentParse = selectWord(item, words, currentParse);
                    parses.add(new ArrayList<>(currentParse));
                    break;
                } else if (nonTerminalCategories.contains(item)) {
                    // It's a phrasal category - select a rule
                    currentParse = selectRule(item, rules, currentParse);
                    parses.add(new ArrayList<>(currentParse));
                    break;
                }
            }
        }
        
        return parses;
    }
    
    /**
     * Generate a sentence starting from a specific category
     * @param category Starting category (e.g., "S", "NP", "VP")
     * @return Generated sentence as string
     */
    public String generateSentence(String category) {
        List<String> parse = new ArrayList<>();
        parse.add(category);
        
        while (containsNonTerminal(parse)) {
            for (int i = 0; i < parse.size(); i++) {
                String item = parse.get(i);
                
                if (wordCategories.contains(item)) {
                    parse = selectWord(item, this.words, parse);
                    break;
                } else if (nonTerminalCategories.contains(item)) {
                    parse = selectRule(item, this.rules, parse);
                    break;
                }
            }
        }
        
        return String.join(" ", parse);
    }
    
    /**
     * Store parse derivation steps to file
     * @param parses List of parse steps
     * @param p PrintWriter for output
     */
    public void storeParses(List<List<String>> parses, PrintWriter p) {
        for (List<String> parse : parses) {
            p.println(parse);
        }
    }
    
    /**
     * Expand a category into all possible derivations
     * @param category Category to expand
     * @return List of possible expansions
     */
    public List<String> expand(String category) {
        if (rules.containsKey(category)) {
            return new ArrayList<>(rules.get(category));
        }
        return new ArrayList<>();
    }
    
    // Getters
    public Map<String, List<String>> getRules() {
        return rules;
    }
    
    public Map<String, List<String>> getWords() {
        return words;
    }
    
    public void setRules(Map<String, List<String>> rules) {
        this.rules = rules;
    }
    
    public void setWords(Map<String, List<String>> words) {
        this.words = words;
    }
}
