package negotiation.grammar;

import negotiation.models.NegotiationTranscript;
import java.util.*;

/**
 * Automated conversation demo - simulates a full negotiation
 * between a human buyer and AI seller without requiring input.
 */
public class AutoConversationDemo {
    
    private NGramMarkovGenerator generator;
    private double sellerPrice;
    private double buyerPrice;
    private int round;
    private List<String> conversation;
    private Random random;
    
    // Simulated buyer responses
    private String[] buyerOffers = {
        "Hi! I'm interested in the MacBook. Would you take $1000 for it?",
        "That's still high. How about $1100?",
        "I can go up to $1200, but that's my limit.",
        "Can you do $1250?",
        "I'll pay $1300 if you can include the charger.",
        "Deal! $1300 works for me."
    };
    
    public AutoConversationDemo() {
        this.generator = new NGramMarkovGenerator();
        this.conversation = new ArrayList<>();
        this.round = 0;
        this.random = new Random();
    }
    
    public void initialize() {
        System.out.println("=== Automated Negotiation Conversation Demo ===\n");
        
        // Train generator
        System.out.println("Training AI seller from dataset...");
        List<NegotiationTranscript> transcripts = createSampleData();
        generator.train(transcripts);
        System.out.println("Training complete!\n");
        
        // Set initial prices
        sellerPrice = 1500.0;  // Seller starts high
        buyerPrice = 0.0;
        
        System.out.println("Scenario: You (Buyer) are negotiating to buy a MacBook Pro");
        System.out.println("Seller's initial asking price: $" + String.format("%.2f", sellerPrice));
        System.out.println("Your goal: Get the best price possible!\n");
        System.out.println("============================================================\n");
    }
    
    public void runConversation() {
        int buyerOfferIndex = 0;
        boolean dealReached = false;
        
        while (!dealReached && round < 10 && buyerOfferIndex < buyerOffers.length) {
            round++;
            System.out.println("--- Round " + round + " ---\n");
            
            // Buyer's turn
            String buyerInput = buyerOffers[buyerOfferIndex++];
            System.out.println("YOU (Buyer): " + buyerInput);
            conversation.add("BUYER: " + buyerInput);
            
            // Extract price from buyer input
            double extractedPrice = extractPrice(buyerInput);
            if (extractedPrice > 0) {
                buyerPrice = extractedPrice;
            }
            
            // Check for acceptance
            if (buyerInput.toLowerCase().contains("deal") || 
                buyerInput.toLowerCase().contains("accept")) {
                System.out.println("\n✅ DEAL REACHED!");
                System.out.println("Final price: $" + String.format("%.2f", buyerPrice));
                conversation.add("DEAL: $" + String.format("%.2f", buyerPrice));
                dealReached = true;
                break;
            }
            
            // Check if buyer's offer is acceptable to seller
            double gap = sellerPrice - buyerPrice;
            double gapPercent = gap / sellerPrice;
            
            if (gapPercent <= 0.1 && buyerPrice > 0) {
                // Within 10% - seller accepts
                String acceptResponse = generator.generate("ACCEPT", sellerPrice);
                System.out.println("SELLER: " + acceptResponse);
                conversation.add("SELLER: " + acceptResponse);
                System.out.println("\n✅ DEAL REACHED at $" + String.format("%.2f", buyerPrice));
                dealReached = true;
                break;
            }
            
            // Seller responds
            String sellerResponse = generateSellerResponse(buyerPrice, gapPercent);
            System.out.println("SELLER: " + sellerResponse);
            conversation.add("SELLER: " + sellerResponse);
            
            // Update seller's price (make concession)
            if (buyerPrice > 0) {
                double concession = gap * 0.15;  // 15% concession
                sellerPrice = Math.max(sellerPrice - concession, buyerPrice + 20);
                System.out.println("(Seller's new asking price: $" + String.format("%.2f", sellerPrice) + ")");
            }
            
            System.out.println();
            
            // Small delay for readability
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        if (!dealReached && round >= 10) {
            System.out.println("\n⚠ Maximum rounds reached. No deal.");
        }
        
        printConversationSummary();
    }
    
    private String generateSellerResponse(double buyerOffer, double gapPercent) {
        if (buyerOffer <= 0) {
            // No price mentioned
            return generator.generate("INQUIRE", sellerPrice);
        }
        
        if (gapPercent > 0.3) {
            // Large gap - reject or justify
            if (random.nextDouble() < 0.6) {
                return generator.generate("REJECT", sellerPrice);
            } else {
                return generator.generate("JUSTIFY", sellerPrice);
            }
        } else if (gapPercent > 0.15) {
            // Medium gap - counter offer
            return generator.generate("COUNTER", sellerPrice);
        } else {
            // Small gap - might accept or final counter
            if (random.nextDouble() < 0.4) {
                return generator.generate("ACCEPT", sellerPrice);
            } else {
                return generator.generate("COUNTER", sellerPrice);
            }
        }
    }
    
    private double extractPrice(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$?\\s*(\\d+(\\.\\d{1,2})?)");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    private void printConversationSummary() {
        System.out.println("\n============================================================");
        System.out.println("=== Conversation Summary ===");
        System.out.println("============================================================");
        System.out.println("Total rounds: " + round);
        System.out.println("Final seller price: $" + String.format("%.2f", sellerPrice));
        System.out.println("Final buyer offer: $" + String.format("%.2f", buyerPrice));
        
        System.out.println("\n--- Full Conversation Transcript ---");
        for (int i = 0; i < conversation.size(); i++) {
            System.out.println((i + 1) + ". " + conversation.get(i));
        }
    }
    
    private List<NegotiationTranscript> createSampleData() {
        List<NegotiationTranscript> samples = new ArrayList<>();
        
        // Sample 1
        NegotiationTranscript t1 = new NegotiationTranscript("sample_1");
        t1.addUtterance("Hi! I'm interested in the item. Would you take $50 for it?");
        t1.addUtterance("That's quite low. The item is brand new and works perfectly.");
        t1.addUtterance("I understand. How about $60? I need it for my commute.");
        t1.addUtterance("I could do $80. It's a great deal.");
        t1.addUtterance("Meet me in the middle at $70?");
        t1.addUtterance("Deal! $70 works for me.");
        samples.add(t1);
        
        // Sample 2
        NegotiationTranscript t2 = new NegotiationTranscript("sample_2");
        t2.addUtterance("I can pay $100 for this.");
        t2.addUtterance("I'm looking for $150. It's worth more than that.");
        t2.addUtterance("How about $120? That's my best offer.");
        t2.addUtterance("I can't go lower than $140.");
        t2.addUtterance("I'll accept $130.");
        t2.addUtterance("Deal!");
        samples.add(t2);
        
        // Sample 3
        NegotiationTranscript t3 = new NegotiationTranscript("sample_3");
        t3.addUtterance("What's your best price?");
        t3.addUtterance("I can do $200.");
        t3.addUtterance("That's too high for me. Can you do $150?");
        t3.addUtterance("I could go down to $175.");
        t3.addUtterance("I'm afraid $150 is my limit.");
        t3.addUtterance("I'll have to pass then.");
        samples.add(t3);
        
        // Sample 4
        NegotiationTranscript t4 = new NegotiationTranscript("sample_4");
        t4.addUtterance("Would you take $500?");
        t4.addUtterance("I could do $600.");
        t4.addUtterance("How about $550?");
        t4.addUtterance("Deal! $550 it is.");
        samples.add(t4);
        
        // Sample 5
        NegotiationTranscript t5 = new NegotiationTranscript("sample_5");
        t5.addUtterance("It's brand new and in perfect condition.");
        t5.addUtterance("I can offer $300.");
        t5.addUtterance("It's worth at least $400.");
        t5.addUtterance("I'll go up to $350.");
        t5.addUtterance("I can do $375.");
        t5.addUtterance("Agreed!");
        samples.add(t5);
        
        // Sample 6 - High value negotiation
        NegotiationTranscript t6 = new NegotiationTranscript("sample_6");
        t6.addUtterance("I'm interested. What's your price?");
        t6.addUtterance("I'm asking $1200.");
        t6.addUtterance("That's more than I can afford. How about $1000?");
        t6.addUtterance("I could come down to $1100.");
        t6.addUtterance("I can do $1050.");
        t6.addUtterance("Meet me at $1075?");
        t6.addUtterance("Deal!");
        samples.add(t6);
        
        // Sample 7 - More seller responses
        NegotiationTranscript t7 = new NegotiationTranscript("sample_7");
        t7.addUtterance("I'll pay $800.");
        t7.addUtterance("That's too low. I need at least $900.");
        t7.addUtterance("How about $850?");
        t7.addUtterance("I can do $875.");
        t7.addUtterance("Deal!");
        samples.add(t7);
        
        return samples;
    }
    
    public static void main(String[] args) {
        AutoConversationDemo demo = new AutoConversationDemo();
        demo.initialize();
        demo.runConversation();
    }
}

