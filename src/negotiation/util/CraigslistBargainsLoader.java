package negotiation.util;

import negotiation.models.*;
import negotiation.strategy.MoveType;
import org.json.*;
import java.io.*;
import java.util.*;

/**
 * Loader for Stanford NLP Craigslist Bargains dataset.
 * Dataset: https://huggingface.co/datasets/stanfordnlp/craigslist_bargains
 * 
 * Format:
 * - 6682 human-human negotiation dialogues
 * - Items from 6 categories: housing, furniture, cars, bikes, phones, electronics
 * - Includes dialogue acts (intent and price)
 * - Agent information (role, target, bottomline)
 */
public class CraigslistBargainsLoader implements IDatasetLoader {
    private String dataPath;
    private String split; // "train", "validation", or "test"
    
    public CraigslistBargainsLoader(String dataPath, String split) {
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
                System.err.println("Please download from: https://huggingface.co/datasets/stanfordnlp/craigslist_bargains");
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
            
            System.out.println("Loaded " + transcripts.size() + " dialogues from Craigslist Bargains (" + split + ")");
            
        } catch (Exception e) {
            System.err.println("Error loading Craigslist Bargains dataset: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transcripts;
    }
    
    private NegotiationTranscript parseDialogue(JSONObject dialogue, int index) {
        NegotiationTranscript transcript = new NegotiationTranscript("craigslist_" + split + "_" + index);
        
        // Parse agent info
        JSONObject agentInfo = dialogue.getJSONObject("agent_info");
        JSONArray roles = agentInfo.getJSONArray("Role");
        JSONArray targets = agentInfo.getJSONArray("Target");
        JSONArray bottomlines = agentInfo.getJSONArray("Bottomline");
        
        for (int i = 0; i < roles.length(); i++) {
            String role = roles.getString(i);
            double target = targets.isNull(i) ? 0.0 : targets.getDouble(i);
            String bottomlineStr = bottomlines.getString(i);
            double bottomline = bottomlineStr.equals("None") ? 0.0 : Double.parseDouble(bottomlineStr);
            
            NegotiationTranscript.AgentInfo info = new NegotiationTranscript.AgentInfo(role, target, bottomline);
            
            if (role.equals("buyer")) {
                transcript.setBuyerInfo(info);
            } else {
                transcript.setSellerInfo(info);
            }
        }
        
        // Parse items
        JSONObject items = dialogue.getJSONObject("items");
        String category = items.getJSONArray("Category").getString(0);
        String title = items.getJSONArray("Title").getString(0);
        String description = items.getJSONArray("Description").getString(0);
        double price = items.getJSONArray("Price").getDouble(0);
        
        Item item = new Item(category, title, description, price);
        
        // Add images
        String images = items.getJSONArray("Images").getString(0);
        if (!images.isEmpty()) {
            item.addImage(images);
        }
        
        transcript.setItem(item);
        
        // Parse utterances and turns
        JSONArray utterances = dialogue.getJSONArray("utterance");
        JSONArray agentTurns = dialogue.getJSONArray("agent_turn");
        
        for (int i = 0; i < utterances.length(); i++) {
            String utterance = utterances.getString(i);
            int turn = agentTurns.getInt(i);
            
            transcript.addUtterance(utterance);
            transcript.addAgentTurn(turn);
        }
        
        // Parse dialogue acts (if available - not in test set)
        if (dialogue.has("dialogue_acts")) {
            JSONObject dialogueActs = dialogue.getJSONObject("dialogue_acts");
            JSONArray intents = dialogueActs.getJSONArray("intent");
            JSONArray prices = dialogueActs.getJSONArray("price");
            
            for (int i = 0; i < intents.length(); i++) {
                String intent = intents.getString(i);
                double actPrice = prices.getDouble(i);
                
                MoveType moveType = MoveType.fromIntent(intent);
                DialogueAct act = new DialogueAct(moveType, actPrice);
                transcript.addDialogueAct(act);
            }
        }
        
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
        return "Craigslist Bargains (Stanford NLP)";
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
        Map<String, Integer> categoryCount = new HashMap<>();
        
        for (NegotiationTranscript transcript : transcripts) {
            totalUtterances += transcript.getLength();
            
            if (transcript.getItem() != null) {
                String category = transcript.getItem().getCategory();
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
            }
        }
        
        stats.put("total_dialogues", transcripts.size());
        stats.put("average_length", (double) totalUtterances / transcripts.size());
        stats.put("category_distribution", categoryCount);
        
        return stats;
    }
}

