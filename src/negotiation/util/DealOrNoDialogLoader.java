package negotiation.util;

import negotiation.models.*;
import negotiation.strategy.MoveType;
import org.json.*;
import java.io.*;
import java.util.*;

/**
 * Loader for Deal or No Dialog dataset (Facebook/Meta).
 * Dataset: https://huggingface.co/datasets/mikelewis0/deal_or_no_dialog
 * 
 * Format:
 * - Multi-issue negotiation over a set of items (books, hats, balls)
 * - Each item has different values for buyer and seller
 * - Dialogues include natural language and structured outputs
 * - Goal: divide items to maximize individual utility
 */
public class DealOrNoDialogLoader implements IDatasetLoader {
    private String dataPath;
    private String split; // "train", "validation", or "test"
    
    public DealOrNoDialogLoader(String dataPath, String split) {
        this.dataPath = dataPath;
        this.split = split;
    }
    
    @Override
    public List<NegotiationTranscript> loadDialogues() {
        List<NegotiationTranscript> transcripts = new ArrayList<>();
        
        try {
            // Load JSON file
            String filePath = dataPath + "/" + split + ".json";
            File file = new File(filePath);
            
            if (!file.exists()) {
                System.err.println("Dataset file not found: " + filePath);
                System.err.println("Please download from: https://huggingface.co/datasets/mikelewis0/deal_or_no_dialog");
                return transcripts;
            }
            
            // Read and parse JSON
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();
            
            // Parse JSON array
            JSONArray dialogues = new JSONArray(jsonContent.toString());
            
            for (int i = 0; i < dialogues.length(); i++) {
                JSONObject dialogue = dialogues.getJSONObject(i);
                NegotiationTranscript transcript = parseDialogue(dialogue, i);
                transcripts.add(transcript);
            }
            
            System.out.println("Loaded " + transcripts.size() + " dialogues from Deal or No Dialog (" + split + ")");
            
        } catch (Exception e) {
            System.err.println("Error loading Deal or No Dialog dataset: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transcripts;
    }
    
    private NegotiationTranscript parseDialogue(JSONObject dialogue, int index) {
        NegotiationTranscript transcript = new NegotiationTranscript("deal_" + split + "_" + index);
        
        // Parse input (item context)
        if (dialogue.has("input")) {
            JSONArray input = dialogue.getJSONArray("input");
            
            // Input format: [count_book, count_hat, count_ball, value_book_agent1, value_hat_agent1, ...]
            if (input.length() >= 6) {
                int bookCount = input.getInt(0);
                int hatCount = input.getInt(1);
                int ballCount = input.getInt(2);
                
                int bookValue1 = input.getInt(3);
                int hatValue1 = input.getInt(4);
                int ballValue1 = input.getInt(5);
                
                int bookValue2 = input.length() > 6 ? input.getInt(6) : 0;
                int hatValue2 = input.length() > 7 ? input.getInt(7) : 0;
                int ballValue2 = input.length() > 8 ? input.getInt(8) : 0;
                
                // Create multi-issue items
                if (bookCount > 0) {
                    Item book = new Item("book", bookCount, bookValue1, bookValue2);
                    transcript.setItem(book); // Store first item as main
                }
            }
        }
        
        // Parse dialogue utterances
        if (dialogue.has("dialogue")) {
            JSONArray dialogueArray = dialogue.getJSONArray("dialogue");
            
            for (int i = 0; i < dialogueArray.length(); i++) {
                String utterance = dialogueArray.getString(i);
                transcript.addUtterance(utterance);
                transcript.addAgentTurn(i % 2); // Alternate between agents
            }
        }
        
        // Parse output (final agreement)
        if (dialogue.has("output")) {
            // Output can be a string or structured data about the agreement
            try {
                String output = dialogue.getString("output");
                transcript.setAgreed(!output.equals("no_agreement"));
            } catch (Exception e) {
                // Handle different output formats
                transcript.setAgreed(false);
            }
        }
        
        // Create agent info (Deal or No Dialog doesn't provide explicit targets)
        NegotiationTranscript.AgentInfo buyer = new NegotiationTranscript.AgentInfo("buyer", 0, 0);
        NegotiationTranscript.AgentInfo seller = new NegotiationTranscript.AgentInfo("seller", 0, 0);
        transcript.setBuyerInfo(buyer);
        transcript.setSellerInfo(seller);
        
        return transcript;
    }
    
    @Override
    public List<Item> loadItems() {
        List<Item> items = new ArrayList<>();
        List<NegotiationTranscript> transcripts = loadDialogues();
        
        for (NegotiationTranscript transcript : transcripts) {
            if (transcript.getItem() != null) {
                items.add(transcript.getItem());
            }
        }
        
        return items;
    }
    
    @Override
    public String getDatasetName() {
        return "Deal or No Dialog (Facebook/Meta)";
    }
    
    /**
     * Get statistics about the loaded dataset
     */
    public Map<String, Object> getDatasetStatistics(List<NegotiationTranscript> transcripts) {
        Map<String, Object> stats = new HashMap<>();
        
        if (transcripts.isEmpty()) {
            return stats;
        }
        
        int totalUtterances = 0;
        int agreedCount = 0;
        
        for (NegotiationTranscript transcript : transcripts) {
            totalUtterances += transcript.getLength();
            if (transcript.isAgreed()) {
                agreedCount++;
            }
        }
        
        stats.put("total_dialogues", transcripts.size());
        stats.put("average_length", (double) totalUtterances / transcripts.size());
        stats.put("agreement_rate", (double) agreedCount / transcripts.size());
        
        return stats;
    }
}

