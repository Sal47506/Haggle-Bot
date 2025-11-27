package negotiation.grammar;

import negotiation.models.NegotiationTranscript;
import java.util.*;

/**
 * Simulates a full negotiation conversation between human buyer and AI seller.
 * Uses NGramMarkovGenerator for seller's responses.
 */
public class ConversationSimulator {
    
    private NGramMarkovGenerator generator;
    private Scanner scanner;
    private double sellerPrice;
    private double buyerPrice;
    private int round;
    private List<String> conversation;
    
    public ConversationSimulator() {
        this.generator = new NGramMarkovGenerator();
        this.scanner = new Scanner(System.in);
        this.conversation = new ArrayList<>();
        this.round = 0;
    }
    
    public void initialize() {
        System.out.println("=== Negotiation Conversation Simulator ===\n");
        
        // Train generator
        System.out.println("Training AI seller from dataset...");
        List<NegotiationTranscript> transcripts = createSampleData();
        generator.train(transcripts);
        System.out.println("Training complete!\n");
        
        // Set initial prices
        sellerPrice = 1500.0;  // Seller starts high
        buyerPrice = 0.0;      // Buyer hasn't made offer yet
        
        System.out.println("You are negotiating to buy a MacBook Pro.");
        System.out.println("Seller's initial asking price: $" + String.format("%.2f", sellerPrice));
        System.out.println("Your goal: Get the best price possible!\n");
        System.out.println("Type your offers (e.g., 'I'll pay $1200' or 'How about $1100?')");
        System.out.println("Type 'quit' to walk away, 'accept' to accept seller's last offer\n");
    }
    
    public void runConversation() {
        boolean continueNegotiation = true;
        
        while (continueNegotiation && round < 15) {
            round++;
            System.out.println("\n--- Round " + round + " ---");
            
            // Buyer's turn
            System.out.print("YOU (Buyer): ");
            String buyerInput = scanner.nextLine().trim();
            
            if (buyerInput.isEmpty()) {
                System.out.println("Please enter an offer.");
                round--;  // Don't count empty input
                continue;
            }
            
            if (buyerInput.equalsIgnoreCase("quit") || buyerInput.equalsIgnoreCase("walk away")) {
                System.out.println("\nYou walked away from the negotiation.");
                conversation.add("BUYER: " + buyerInput);
                break;
            }
            
            if (buyerInput.equalsIgnoreCase("accept")) {
                System.out.println("\n✅ DEAL REACHED!");
                System.out.println("Final price: $" + String.format("%.2f", sellerPrice));
                conversation.add("BUYER: " + buyerInput);
                conversation.add("DEAL: $" + String.format("%.2f", sellerPrice));
                break;
            }
            
            // Extract price from buyer input
            double extractedPrice = extractPrice(buyerInput);
            if (extractedPrice > 0) {
                buyerPrice = extractedPrice;
            }
            
            conversation.add("BUYER: " + buyerInput);
            
            // Check if buyer's offer is acceptable
            if (buyerPrice >= sellerPrice * 0.9) {  // Within 10% of seller's price
                String acceptResponse = generator.generate("ACCEPT", sellerPrice);
                System.out.println("SELLER: " + acceptResponse);
                conversation.add("SELLER: " + acceptResponse);
                System.out.println("\n✅ DEAL REACHED at $" + String.format("%.2f", buyerPrice));
                break;
            }
            
            // Seller responds
            String sellerResponse = generateSellerResponse(buyerPrice);
            System.out.println("SELLER: " + sellerResponse);
            conversation.add("SELLER: " + sellerResponse);
            
            // Update seller's price (concession)
            if (buyerPrice > 0) {
                double concession = (sellerPrice - buyerPrice) * 0.2;  // 20% concession
                sellerPrice = Math.max(sellerPrice - concession, buyerPrice + 50);
            }
        }
        
        if (round >= 15) {
            System.out.println("\n⚠ Maximum rounds reached. No deal.");
        }
        
        printConversationSummary();
    }
    
    private String generateSellerResponse(double buyerOffer) {
        if (buyerOffer <= 0) {
            // No price mentioned, ask for offer
            return generator.generate("INQUIRE", sellerPrice);
        }
        
        double gap = sellerPrice - buyerOffer;
        double gapPercent = gap / sellerPrice;
        
        if (gapPercent > 0.3) {
            // Large gap - reject or justify
            if (Math.random() < 0.5) {
                return generator.generate("REJECT", sellerPrice);
            } else {
                return generator.generate("JUSTIFY", sellerPrice);
            }
        } else if (gapPercent > 0.1) {
            // Medium gap - counter offer
            return generator.generate("COUNTER", sellerPrice);
        } else {
            // Small gap - might accept
            if (Math.random() < 0.3) {
                return generator.generate("ACCEPT", sellerPrice);
            } else {
                return generator.generate("COUNTER", sellerPrice);
            }
        }
    }
    
    private double extractPrice(String input) {
        // Try to extract price from input
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
        System.out.println("\n=== Conversation Summary ===");
        System.out.println("Total rounds: " + round);
        System.out.println("\nFull conversation:");
        for (String line : conversation) {
            System.out.println(line);
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
        
        // Sample 4 - More seller responses
        NegotiationTranscript t4 = new NegotiationTranscript("sample_4");
        t4.addUtterance("Would you take $500?");
        t4.addUtterance("I could do $600.");
        t4.addUtterance("How about $550?");
        t4.addUtterance("Deal! $550 it is.");
        samples.add(t4);
        
        // Sample 5 - Justification
        NegotiationTranscript t5 = new NegotiationTranscript("sample_5");
        t5.addUtterance("It's brand new and in perfect condition.");
        t5.addUtterance("I can offer $300.");
        t5.addUtterance("It's worth at least $400.");
        t5.addUtterance("I'll go up to $350.");
        t5.addUtterance("I can do $375.");
        t5.addUtterance("Agreed!");
        samples.add(t5);
        
        // Sample 6 - More variety
        NegotiationTranscript t6 = new NegotiationTranscript("sample_6");
        t6.addUtterance("I'm interested. What's your price?");
        t6.addUtterance("I'm asking $1200.");
        t6.addUtterance("That's more than I can afford. How about $1000?");
        t6.addUtterance("I could come down to $1100.");
        t6.addUtterance("I can do $1050.");
        t6.addUtterance("Meet me at $1075?");
        t6.addUtterance("Deal!");
        samples.add(t6);
        
        return samples;
    }
    
    public static void main(String[] args) {
        ConversationSimulator simulator = new ConversationSimulator();
        simulator.initialize();
        simulator.runConversation();
    }
}

