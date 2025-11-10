package dealdialect.language;

import dealdialect.engine.*;
import java.util.*;
import java.util.regex.*;

/**
 * Template-based language model for negotiation.
 * Uses predefined templates with placeholders.
 */
public class TemplateLanguageModel implements LanguageModel {
    private Random random;
    private Map<Offer.Intent, List<String>> templates;
    private Map<Offer.Intent, List<String>> bluffTemplates;
    
    public TemplateLanguageModel() {
        this.random = new Random();
        this.templates = new HashMap<>();
        this.bluffTemplates = new HashMap<>();
        initializeTemplates();
    }
    
    @Override
    public String generateUtterance(Offer.Intent intent, double price, DealContext context, 
                                   DialogueState state, Role role) {
        List<String> templates = this.templates.get(intent);
        if (templates == null || templates.isEmpty()) {
            return String.format("$%.2f", price);
        }
        
        String template = templates.get(random.nextInt(templates.size()));
        
        // Replace placeholders
        String result = template
            .replace("{price}", String.format("$%.2f", price))
            .replace("{item}", context.getItem() != null ? context.getItem().getTitle() : "item")
            .replace("{role}", role == Role.BUYER ? "buy" : "sell")
            .replace("{msrp}", String.format("$%.2f", context.getMsrp()));
        
        return result;
    }
    
    @Override
    public String generateBluffText(Offer.Intent intent, double bluffStrength, 
                                   DealContext context, Role role) {
        List<String> templates = bluffTemplates.get(intent);
        if (templates == null || templates.isEmpty()) {
            return "This is my final offer.";
        }
        
        String template = templates.get(random.nextInt(templates.size()));
        
        // Adjust template based on bluff strength
        String intensifier = bluffStrength > 0.7 ? "absolutely" : 
                            bluffStrength > 0.4 ? "really" : "";
        
        return template
            .replace("{intensifier}", intensifier)
            .replace("{item}", context.getItem() != null ? context.getItem().getTitle() : "item")
            .trim();
    }
    
    @Override
    public Offer parseHumanInput(String humanInput, Role humanRole) {
        if (humanInput == null || humanInput.trim().isEmpty()) {
            return null;
        }
        
        humanInput = humanInput.trim().toLowerCase();
        
        // Try to extract price
        Double price = extractPrice(humanInput);
        
        // Determine intent
        Offer.Intent intent = detectIntent(humanInput);
        
        if (price != null && intent != Offer.Intent.WALK_AWAY) {
            return new Offer(humanRole, price, humanInput, intent);
        } else if (intent == Offer.Intent.ACCEPT || intent == Offer.Intent.REJECT || 
                   intent == Offer.Intent.WALK_AWAY) {
            return new Offer(humanRole, 0.0, humanInput, intent);
        }
        
        return null;
    }
    
    @Override
    public String getModelName() {
        return "TemplateLanguageModel";
    }
    
    /**
     * Initialize template libraries
     */
    private void initializeTemplates() {
        // OFFER templates
        templates.put(Offer.Intent.OFFER, Arrays.asList(
            "I can {role} for {price}.",
            "How about {price}?",
            "I'm thinking {price} for the {item}.",
            "Would you accept {price}?",
            "My offer is {price}."
        ));
        
        // COUNTER templates
        templates.put(Offer.Intent.COUNTER, Arrays.asList(
            "I could do {price}.",
            "What about {price} instead?",
            "How about we meet at {price}?",
            "I can go up to {price}.",
            "Let's try {price}."
        ));
        
        // JUSTIFY templates
        templates.put(Offer.Intent.JUSTIFY, Arrays.asList(
            "That's fair because the MSRP is {msrp}.",
            "I think {price} is reasonable for this {item}.",
            "Given the condition, {price} makes sense.",
            "The market value is around {price}."
        ));
        
        // THREATEN templates
        templates.put(Offer.Intent.THREATEN, Arrays.asList(
            "I might have to walk away if we can't agree.",
            "I have other options if this doesn't work out.",
            "This needs to work or I'll look elsewhere.",
            "I'm not sure we can make a deal here."
        ));
        
        // ACCEPT templates
        templates.put(Offer.Intent.ACCEPT, Arrays.asList(
            "Deal! I accept {price}.",
            "You've got a deal at {price}.",
            "Agreed. {price} it is.",
            "I'll take it for {price}."
        ));
        
        // REJECT templates
        templates.put(Offer.Intent.REJECT, Arrays.asList(
            "I can't do {price}, sorry.",
            "That's too far from what I had in mind.",
            "I'm afraid {price} doesn't work for me.",
            "I'll have to pass on {price}."
        ));
        
        // WALK_AWAY templates
        templates.put(Offer.Intent.WALK_AWAY, Arrays.asList(
            "I don't think we can reach an agreement.",
            "This isn't going to work out. Thanks anyway.",
            "I'm going to look at other options.",
            "Let's call it here."
        ));
        
        // BLUFF_PUFF templates
        bluffTemplates.put(Offer.Intent.BLUFF_PUFF, Arrays.asList(
            "This is {intensifier} my absolute limit.",
            "I {intensifier} can't go any further.",
            "I have another buyer/seller lined up.",
            "This {item} is worth way more than that.",
            "I'm taking a loss at this price already."
        ));
    }
    
    /**
     * Extract price from text using regex
     */
    private Double extractPrice(String text) {
        // Look for patterns like $100, $100.50, 100, etc.
        Pattern pattern = Pattern.compile("\\$?\\s*(\\d+(?:\\.\\d{1,2})?)");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Detect intent from text
     */
    private Offer.Intent detectIntent(String text) {
        text = text.toLowerCase();
        
        // Check for accept keywords
        if (text.contains("accept") || text.contains("deal") || text.contains("agreed") || 
            text.matches(".*\\b(yes|ok|sure|fine)\\b.*")) {
            return Offer.Intent.ACCEPT;
        }
        
        // Check for reject keywords
        if (text.contains("reject") || text.contains("no way") || text.contains("can't do") ||
            text.contains("too") && (text.contains("high") || text.contains("low"))) {
            return Offer.Intent.REJECT;
        }
        
        // Check for walk away keywords
        if (text.contains("walk") || text.contains("goodbye") || text.contains("nevermind") ||
            text.contains("forget it")) {
            return Offer.Intent.WALK_AWAY;
        }
        
        // Check for threaten keywords
        if (text.contains("other options") || text.contains("might walk") || 
            text.contains("have to leave")) {
            return Offer.Intent.THREATEN;
        }
        
        // Check for bluff keywords
        if (text.contains("final offer") || text.contains("absolute limit") || 
            text.contains("best price")) {
            return Offer.Intent.BLUFF_PUFF;
        }
        
        // Check for counter/offer
        if (extractPrice(text) != null) {
            if (text.contains("how about") || text.contains("what about") || 
                text.contains("instead")) {
                return Offer.Intent.COUNTER;
            }
            return Offer.Intent.OFFER;
        }
        
        // Default to inquire
        return Offer.Intent.INQUIRE;
    }
}

