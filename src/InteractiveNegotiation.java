import agents.BuyerAgent;
import dialogue.DialogueGenerator;
import dialogue.MarkovDialogueGenerator;
import dialogue.MarkovTransformerDialogueGenerator;
import java.util.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class InteractiveNegotiation {
    
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static Pattern pricePattern = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d{1,2})?)|" +
                                                          "(\\d+(?:\\.\\d{1,2})?)\\s*(?:dollars?|bucks?|usd)");
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Interactive Negotiation ===");
            System.out.println();
            
            String datasetPath = "data/craigslist_bargains/train.json";
            
            System.out.print("Enter item name: ");
            System.out.flush();
            String itemName = reader.readLine();
            if (itemName == null) {
                System.err.println("\nError: No input received (stdin closed or EOF). Make sure you're running this interactively.");
                System.err.println("If running from a script, ensure stdin is not redirected.");
                return;
            }
            itemName = itemName.trim();
            
            System.out.print("Enter your asking price: $");
            String askingPriceInput = reader.readLine();
            if (askingPriceInput == null) {
                System.out.println("Error: No input received. Exiting.");
                return;
            }
            double askingPrice = Double.parseDouble(askingPriceInput.trim());
            
            System.out.print("Enter buyer's reservation price (max they'll pay): $");
            String resInput = reader.readLine();
            if (resInput == null) {
                System.out.println("Error: No input received. Exiting.");
                return;
            }
            double buyerReservation = Double.parseDouble(resInput.trim());
            
            System.out.print("Enter buyer's target price (ideal price): $");
            String targetInput = reader.readLine();
            if (targetInput == null) {
                System.out.println("Error: No input received. Exiting.");
                return;
            }
            double buyerTarget = Double.parseDouble(targetInput.trim());

            System.out.print("Use transformer reranker for buyer dialogue? (y/N): ");
            String rerankInput = reader.readLine();
            if (rerankInput == null) rerankInput = "n";
            boolean useTransformerReranker = rerankInput.trim().toLowerCase().startsWith("y");
            
            System.out.println("\n=== Negotiation Started ===");
            System.out.println("Item: " + itemName);
            System.out.println("Your asking price: $" + String.format("%.2f", askingPrice));
            System.out.println("\nTip: Type 'quit' or 'exit' to end negotiation");
            System.out.println("Disclaimer: Buyer responses are auto-generated and may contain artifacts or off-topic phrases. Deals are marked heuristically when price gaps narrow—treat them as approximations.");
            System.out.println("----------------------------------------\n");
            
            DialogueGenerator generator;
            if (useTransformerReranker) {
                generator = new MarkovTransformerDialogueGenerator(datasetPath, 3, true);
            } else {
                generator = new MarkovDialogueGenerator(datasetPath, 3);
            }
            
            BuyerAgent buyer = new BuyerAgent(generator, buyerReservation, buyerTarget);
            buyer.setItemContext(itemName);
            
            System.out.println("Q-Learning enabled:");
            System.out.println("  Exploration rate: " + buyer.getEpsilon());
            System.out.println("  Agent will learn from this negotiation");
            System.out.println("  Item context: " + itemName);
            System.out.println("  Transformer reranker: " + (useTransformerReranker ? "enabled" : "disabled (Markov only)"));
            System.out.println();
            
            String buyerMessage = buyer.makeInitialOffer();
            System.out.println("Buyer: " + buyerMessage);
            System.out.println("Buyer's offer: $" + String.format("%.2f", buyer.getCurrentOffer()));
            System.out.println();
            
            double currentSellerPrice = askingPrice;
            int round = 1;
            
            while (round < 20) {
                System.out.print("You (Seller): ");
                String sellerMessage = reader.readLine().trim();
                
                if (sellerMessage.isEmpty()) {
                    System.out.println("Please enter a message.");
                    continue;
                }
                
                if (sellerMessage.toLowerCase().equals("quit") || sellerMessage.toLowerCase().equals("exit")) {
                    System.out.println("\nNegotiation ended by seller.");
                    break;
                }
                
                double inferredPrice = inferPriceFromMessage(sellerMessage);
                boolean isRejecting = isRejectingMessage(sellerMessage);
                
                if (inferredPrice > 0) {
                    currentSellerPrice = inferredPrice;
                    System.out.println("  [Inferred price: $" + String.format("%.2f", currentSellerPrice) + "]");
                } else if (!isRejecting) {
                    System.out.print("  Your price: $");
                    String priceInput = reader.readLine().trim();
                    
                    if (priceInput.toLowerCase().equals("quit") || priceInput.toLowerCase().equals("exit")) {
                        System.out.println("\nNegotiation ended by seller.");
                        break;
                    }
                    
                    try {
                        currentSellerPrice = Double.parseDouble(priceInput);
                    } catch (NumberFormatException e) {
                        System.out.println("  Invalid price, keeping previous: $" + String.format("%.2f", currentSellerPrice));
                    }
                } else {
                    System.out.println("  [Keeping price: $" + String.format("%.2f", currentSellerPrice) + "]");
                }
                
                System.out.println();
                
                String buyerResponse = buyer.respondToSeller(sellerMessage, currentSellerPrice);
                
                System.out.println("Buyer: " + buyerResponse);
                System.out.println("Buyer's offer: $" + String.format("%.2f", buyer.getCurrentOffer()));
                
                double priceGap = Math.abs(buyer.getCurrentOffer() - currentSellerPrice);
                System.out.println("Round: " + buyer.getState().getRound() + 
                                 " | Price gap: $" + String.format("%.2f", priceGap) +
                                 " | Exploration: " + String.format("%.1f%%", buyer.getEpsilon() * 100));
                
                if (buyer.getState().getOpponentLastOffer() <= buyer.getReservationPrice() && 
                    buyer.getState().getOpponentLastOffer() > 0) {
                    System.out.println("\n=== Deal Reached! ===");
                    System.out.println("Buyer accepts your price: $" + String.format("%.2f", currentSellerPrice));
                    break;
                }
                
                if (priceGap < 2.0) {
                    System.out.println("\n=== Deal Reached! ===");
                    double finalPrice = (buyer.getCurrentOffer() + currentSellerPrice) / 2.0;
                    System.out.println("Agreed price: $" + String.format("%.2f", finalPrice));
                    break;
                }
                
                if (buyerResponse.toLowerCase().contains("deal") && 
                    (buyerResponse.toLowerCase().contains("accept") || 
                     buyerResponse.toLowerCase().contains("agreed"))) {
                    System.out.println("\n=== Deal Reached! ===");
                    System.out.println("Buyer accepts: $" + String.format("%.2f", currentSellerPrice));
                    break;
                }
                
                if (buyer.getState().getRound() > 15) {
                    System.out.println("\nNegotiation ended after " + buyer.getState().getRound() + " rounds.");
                    break;
                }
                
                System.out.println();
                round++;
            }
            
            System.out.println("\n=== Negotiation Summary ===");
            System.out.println("Total rounds: " + buyer.getState().getRound());
            System.out.println("Final seller price: $" + String.format("%.2f", currentSellerPrice));
            System.out.println("Final buyer offer: $" + String.format("%.2f", buyer.getCurrentOffer()));
            System.out.println("\nOffer history:");
            List<Double> history = buyer.getOfferHistory();
            for (int i = 0; i < history.size(); i++) {
                System.out.println("  Round " + (i+1) + ": $" + String.format("%.2f", history.get(i)));
            }
            
            buyer.saveQTable("q_table.txt");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static double inferPriceFromMessage(String message) {
        Matcher matcher = pricePattern.matcher(message.toLowerCase());
        if (matcher.find()) {
            try {
                String priceStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                return Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    private static boolean isRejectingMessage(String message) {
        String lower = message.toLowerCase();
        
        String[] rejectKeywords = {
            "no", "nope", "can't", "cannot", "won't", "wouldn't",
            "too low", "too high", "impossible", "refuse", 
            "not accepting", "not interested", "pass"
        };
        
        for (String keyword : rejectKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        
        if (lower.contains("deal") && (lower.contains("no") || lower.contains("not"))) {
            return true;
        }
        
        return false;
    }
}

