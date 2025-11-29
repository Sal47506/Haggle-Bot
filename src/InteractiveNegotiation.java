import agents.BuyerAgent;
import java.util.*;
import java.io.*;

public class InteractiveNegotiation {
    
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Interactive Negotiation ===");
            System.out.println();
            
            String datasetPath = "data/craigslist_bargains/train.json";
            
            System.out.print("Enter item name: ");
            String itemName = reader.readLine().trim();
            
            System.out.print("Enter your asking price: $");
            double askingPrice = Double.parseDouble(reader.readLine().trim());
            
            System.out.print("Enter buyer's reservation price (max they'll pay): $");
            double buyerReservation = Double.parseDouble(reader.readLine().trim());
            
            System.out.print("Enter buyer's target price (ideal price): $");
            double buyerTarget = Double.parseDouble(reader.readLine().trim());
            
            System.out.println("\n=== Negotiation Started ===");
            System.out.println("Item: " + itemName);
            System.out.println("Your asking price: $" + String.format("%.2f", askingPrice));
            System.out.println("\nTip: Type 'quit' or 'exit' to end negotiation");
            System.out.println("----------------------------------------\n");
            
            BuyerAgent buyer = new BuyerAgent(datasetPath, buyerReservation, buyerTarget);
            
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
                
                System.out.print("Your price: $");
                String priceInput = reader.readLine().trim();
                
                if (priceInput.toLowerCase().equals("quit") || priceInput.toLowerCase().equals("exit")) {
                    System.out.println("\nNegotiation ended by seller.");
                    break;
                }
                
                try {
                    currentSellerPrice = Double.parseDouble(priceInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price, using previous price: $" + String.format("%.2f", currentSellerPrice));
                }
                
                System.out.println();
                
                String buyerResponse = buyer.respondToSeller(sellerMessage, currentSellerPrice);
                
                System.out.println("Buyer: " + buyerResponse);
                System.out.println("Buyer's offer: $" + String.format("%.2f", buyer.getCurrentOffer()));
                
                double priceGap = Math.abs(buyer.getCurrentOffer() - currentSellerPrice);
                System.out.println("Round: " + buyer.getState().getRound() + 
                                 " | Price gap: $" + String.format("%.2f", priceGap));
                
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
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

