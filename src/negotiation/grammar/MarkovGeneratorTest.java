package negotiation.grammar;

import negotiation.models.NegotiationTranscript;
import java.util.*;

/**
 * Test class for NGramMarkovGenerator
 */
public class MarkovGeneratorTest {
    
    public static void main(String[] args) {
        System.out.println("=== N-Gram Markov Chain Generator Test ===\n");
        
        // Create generator
        NGramMarkovGenerator generator = new NGramMarkovGenerator();
        
        // Load datasets (using sample data for now)
        System.out.println("Loading sample data for testing...");
        List<NegotiationTranscript> transcripts = createSampleData();
        
        // Train model
        System.out.println("\nTraining model...");
        generator.train(transcripts);
        
        // Show statistics
        System.out.println("\n" + generator.getStatistics());
        
        // Test generation
        System.out.println("\n=== Generating Test Sentences ===\n");
        
        // Test different intents
        String[] intents = {"OFFER", "COUNTER", "ACCEPT", "REJECT", "JUSTIFY"};
        double[] prices = {50.0, 150.0, 500.0, 1350.0};
        
        for (String intent : intents) {
            System.out.println("Intent: " + intent);
            for (double price : prices) {
                String generated = generator.generate(intent, price);
                System.out.println("  Price $" + String.format("%.2f", price) + ": " + generated);
            }
            System.out.println();
        }
        
        // Generate multiple samples for same intent/price
        System.out.println("=== Multiple Samples (COUNTER, $100) ===\n");
        for (int i = 0; i < 10; i++) {
            String generated = generator.generate("COUNTER", 100.0);
            System.out.println((i+1) + ". " + generated);
        }
        
        System.out.println("\n=== Test Complete ===");
    }
    
    /**
     * Create sample data for testing
     */
    private static List<NegotiationTranscript> createSampleData() {
        List<NegotiationTranscript> samples = new ArrayList<>();
        
        // Sample 1 - Successful negotiation
        NegotiationTranscript t1 = new NegotiationTranscript("sample_1");
        t1.addUtterance("Hi! I'm interested in the item. Would you take $50 for it?");
        t1.addUtterance("That's quite low. The item is brand new and works perfectly.");
        t1.addUtterance("I understand. How about $60? I need it for my commute.");
        t1.addUtterance("I could do $80. It's a great deal.");
        t1.addUtterance("Meet me in the middle at $70?");
        t1.addUtterance("Deal! $70 works for me.");
        samples.add(t1);
        
        // Sample 2 - Counter offers
        NegotiationTranscript t2 = new NegotiationTranscript("sample_2");
        t2.addUtterance("I can pay $100 for this.");
        t2.addUtterance("I'm looking for $150. It's worth more than that.");
        t2.addUtterance("How about $120? That's my best offer.");
        t2.addUtterance("I can't go lower than $140.");
        t2.addUtterance("I'll accept $130.");
        t2.addUtterance("Deal!");
        samples.add(t2);
        
        // Sample 3 - Rejection
        NegotiationTranscript t3 = new NegotiationTranscript("sample_3");
        t3.addUtterance("What's your best price?");
        t3.addUtterance("I can do $200.");
        t3.addUtterance("That's too high for me. Can you do $150?");
        t3.addUtterance("I could go down to $175.");
        t3.addUtterance("I'm afraid $150 is my limit.");
        t3.addUtterance("I'll have to pass then.");
        samples.add(t3);
        
        // Sample 4 - More variety
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
        
        return samples;
    }
}

