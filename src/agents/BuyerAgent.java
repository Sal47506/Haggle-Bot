package agents;

import dialogue.DialogueGenerator;
import dialogue.MarkovDialogueGenerator;
import models.NegotiationState;
import java.util.*;

public class BuyerAgent {
    
    private DialogueGenerator dialogueGen;
    private double reservationPrice;
    private double targetPrice;
    private double currentOffer;
    private NegotiationState state;
    private Random random;
    private List<Double> offerHistory;
    private Map<String, double[]> qTable;
    private double alpha = 0.1;
    private double gamma = 0.95;
    private double epsilon = 0.2;
    private static final String[] ACTIONS = {"COUNTER", "REJECT", "ACCEPT"};
    private int consecutiveRejects = 0;
    private String previousStateKey = null;
    private int previousAction = -1;
    private enum Tactic {
        HARD_BALL,
        OPPORTUNISTIC,
        SNEAKY,
        DEFAULT
    }
    private Tactic currentTactic;

    public BuyerAgent(String datasetPath, double reservationPrice, double targetPrice) throws Exception {
        this.dialogueGen = new MarkovDialogueGenerator(datasetPath, 3);
        this.reservationPrice = reservationPrice;
        this.targetPrice = targetPrice;
        this.currentOffer = targetPrice;
        this.random = new Random();
        this.offerHistory = new ArrayList<>();
        this.qTable = new HashMap<>();
        this.state = new NegotiationState(0, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
    }

    public BuyerAgent(DialogueGenerator dialogueGen, double reservationPrice, double targetPrice) {
        this.dialogueGen = dialogueGen;
        this.reservationPrice = reservationPrice;
        this.targetPrice = targetPrice;
        this.currentOffer = targetPrice;
        this.random = new Random();
        this.offerHistory = new ArrayList<>();
        this.qTable = new HashMap<>();
        this.state = new NegotiationState(0, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
    }
    
    public void setItemContext(String itemName) {
        dialogueGen.setItemContext(itemName);
    }
    
    private Tactic pickTactic(String intent, double sellerPrice) {
        double gap = sellerPrice - currentOffer;
        int round = state.getRound();
        
        if (intent.equals("ACCEPT")) {
            return Tactic.OPPORTUNISTIC;
        }
        
        if (intent.equals("REJECT")) {
            if (gap > sellerPrice * 0.5) {
                return Tactic.HARD_BALL;
            }
            return random.nextDouble() < 0.7 ? Tactic.HARD_BALL : Tactic.SNEAKY;
        }
        
        if (intent.equals("COUNTER")) {
            if (round >= 6) {
                if (gap > sellerPrice * 0.2) {
                    return random.nextDouble() < 0.6 ? Tactic.SNEAKY : Tactic.HARD_BALL;
                }
                return Tactic.OPPORTUNISTIC;
            }
            
            if (gap > sellerPrice * 0.3) {
                return random.nextDouble() < 0.5 ? Tactic.SNEAKY : Tactic.HARD_BALL;
            }
            
            if (round >= 2 && gap <= sellerPrice * 0.2) {
                return Tactic.OPPORTUNISTIC;
            }
            
            double r = random.nextDouble();
            if (r < 0.33) return Tactic.HARD_BALL;
            if (r < 0.66) return Tactic.OPPORTUNISTIC;
            return Tactic.SNEAKY;
        }
        
        return Tactic.DEFAULT;
    }

    private String getStateKey(double sellerPrice) {
        double priceGap = Math.abs(sellerPrice - currentOffer);
        int roundBucket = Math.min(state.getRound(), 10);
        int gapBucket = (int) Math.floor(priceGap / 10.0);
        int rejectsBucket = Math.min(consecutiveRejects, 5);
        return roundBucket + "_" + gapBucket + "_" + rejectsBucket;
    }

    private int chooseAction(String stateKey) {
        if (!qTable.containsKey(stateKey)) {
            qTable.put(stateKey, new double[]{0.0, 0.0, 0.0});
        }

        if (random.nextDouble() < epsilon) {
            return random.nextInt(3);
        }

        double[] qValues = qTable.get(stateKey);
        return argmax(qValues);
    }
    
    private int argmax(double[] values) {
        int maxIdx = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private String actionToIntent(int action) {
        return ACTIONS[action];
    } 

    private void updateQTable(String stateKey, int action, double reward, String nextStateKey) {
        if (!qTable.containsKey(stateKey)) {
            qTable.put(stateKey, new double[]{0.0, 0.0, 0.0});
        }
        
        double[] qValues = qTable.get(stateKey);
        double[] nextQValues = qTable.getOrDefault(nextStateKey, new double[]{0.0, 0.0, 0.0});

        double bestNextQ = Math.max(nextQValues[0], Math.max(nextQValues[1], nextQValues[2]));
        qValues[action] = (1 - alpha) * qValues[action] +
                        alpha * (reward + gamma * bestNextQ);

        qTable.put(stateKey, qValues);
    }
    
    public String respondToSeller(String sellerMessage, double sellerPrice) {
        String currentStateKey = getStateKey(sellerPrice);
        
        int action = chooseAction(currentStateKey);
        String intent = actionToIntent(action);
        
        if (intent.equals("ACCEPT") && sellerPrice > reservationPrice) {
            intent = "COUNTER";
            action = 0;
        }
        
        if (intent.equals("COUNTER") && sellerPrice <= reservationPrice) {
            intent = "ACCEPT";
            action = 2;
        }
        
        double reward = 0.0;
        if (intent.equals("ACCEPT")) {
            if (sellerPrice <= reservationPrice) {
                reward = (reservationPrice - sellerPrice) * 2.0;
            } else {
                reward = -10.0;
            }
            consecutiveRejects = 0;
        } else if (intent.equals("REJECT")) {
            reward = -2.0;
            consecutiveRejects++;
        } else {
            double gapReduction = Math.abs(sellerPrice - currentOffer);
            reward = -gapReduction / 10.0;
            consecutiveRejects = 0;
        }
        
        currentTactic = pickTactic(intent, sellerPrice);
        
        if (consecutiveRejects >= 2) {
            intent = "REJECT";
            action = 1;
        }
        
        double offerPrice = decidePrice(intent, sellerPrice, consecutiveRejects);
        intent = decideIntent(sellerPrice);
        
        currentOffer = offerPrice;
        offerHistory.add(offerPrice);
        
        state = new NegotiationState(
            state.getRound() + 1,
            currentOffer,
            reservationPrice,
            targetPrice,
            sellerPrice,
            offerHistory
        );
        state.setConsecutiveRejects(consecutiveRejects);
        
        String nextStateKey = getStateKey(sellerPrice);
        
        if (previousStateKey != null && previousAction >= 0) {
            updateQTable(previousStateKey, previousAction, reward, nextStateKey);
        }
        
        previousStateKey = currentStateKey;
        previousAction = action;
        
        dialogueGen.updateContext(state, sellerMessage);
        
        String response = generateTacticalResponse(intent, offerPrice, sellerMessage, currentTactic);
        
        return response;
    }
    
    private String generateTacticalResponse(String intent, double price, String sellerMessage, Tactic tactic) {
        String baseResponse = dialogueGen.generate(intent, price, sellerMessage);
        
        if (consecutiveRejects >= 3) {
            String[] stubbornResponses = {
                "I really can't go higher than $" + String.format("%.2f", price) + ". That's my absolute max.",
                "I'm being serious - $" + String.format("%.2f", price) + " is the best I can do.",
                "I've got another seller offering it for less. $" + String.format("%.2f", price) + " is fair.",
                "My budget is $" + String.format("%.2f", price) + ". I can't change that."
            };
            return stubbornResponses[random.nextInt(stubbornResponses.length)];
        }
        
        switch (tactic) {
            case HARD_BALL:
                if (!baseResponse.toLowerCase().contains("final") && state.getRound() > 3) {
                    baseResponse = addTacticalModifier(baseResponse, "This is my final offer.");
                }
                break;
            case SNEAKY:
                if (!baseResponse.toLowerCase().contains("budget") && !baseResponse.toLowerCase().contains("found")) {
                    baseResponse = addTacticalModifier(baseResponse, "I saw similar ones for less.");
                }
                break;
            case OPPORTUNISTIC:
                if (intent.equals("ACCEPT") && !baseResponse.toLowerCase().contains("cash")) {
                    baseResponse = addTacticalModifier(baseResponse, "I can pay cash right now.");
                }
                break;
            default:
                break;
        }
        
        return baseResponse;
    }
    
    private String addTacticalModifier(String response, String modifier) {
        if (response.endsWith(".") || response.endsWith("!") || response.endsWith("?")) {
            return response.substring(0, response.length() - 1) + ". " + modifier;
        }
        return response + " " + modifier;
    }
    
    public String makeInitialOffer() {
        currentOffer = targetPrice;
        offerHistory.clear();
        offerHistory.add(currentOffer);
        
        state = new NegotiationState(1, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
        dialogueGen.updateContext(state, null);
        
        return dialogueGen.generate("OFFER", currentOffer);
    }
    
    private String decideIntent(double sellerPrice) {
        if (sellerPrice <= reservationPrice) {
            state.setDealReached(true);
            return "ACCEPT";
        }
        
        double priceGap = Math.abs(sellerPrice - currentOffer);
        double relativegGap = priceGap / sellerPrice;
        
        if (state.getRound() > 7 && sellerPrice <= reservationPrice * 1.2) {
            state.setDealReached(true);
            return "ACCEPT";
        }
        
        if (relativegGap < 0.15 && state.getRound() > 4) {
            state.setDealReached(true);
            return "ACCEPT";
        }
        
        if (state.getRound() > 10) {
            if (sellerPrice <= reservationPrice * 1.3) {
                state.setDealReached(true);
                return "ACCEPT";
            }
            return "REJECT";
        }
        
        if (sellerPrice > reservationPrice * 2.0 && state.getRound() < 3) {
            return "REJECT";
        }
        
        return "COUNTER";
    }
    
    private double decidePrice(String intent, double sellerPrice, int stubbornnessLevel) {
        switch (intent) {
            case "ACCEPT":
                return sellerPrice;
                
            case "COUNTER":
                if (stubbornnessLevel >= 2) {
                    double stubbornIncrease = currentOffer * 0.02;
                    return Math.round((currentOffer + stubbornIncrease) * 100.0) / 100.0;
                }
                
                double progressFactor = Math.min(1.0, state.getRound() / 8.0);
                double minOffer = targetPrice;
                double maxOffer = reservationPrice;
                
                double baseCounter = minOffer + (maxOffer - minOffer) * progressFactor;
                
                double gapToBridge = (sellerPrice - currentOffer) * 0.3;
                double newOffer = currentOffer + gapToBridge;
                
                newOffer = Math.max(newOffer, baseCounter);
                newOffer = Math.min(newOffer, reservationPrice); 
                newOffer = Math.max(newOffer, currentOffer * 1.05);
                
                return Math.round(newOffer * 100.0) / 100.0;
                
            case "REJECT":
                return currentOffer;
                
            default:
                return currentOffer;
        }
    }
    
    public boolean isDealReached() {
        return state != null && state.isDealReached();
    }
    
    public double getCurrentOffer() {
        return currentOffer;
    }
    
    public double getReservationPrice() {
        return reservationPrice;
    }
    
    public NegotiationState getState() {
        return state;
    }
    
    public void reset() {
        this.currentOffer = targetPrice;
        this.offerHistory.clear();
        this.consecutiveRejects = 0;
        this.previousStateKey = null;
        this.previousAction = -1;
        this.state = new NegotiationState(0, 0.0, reservationPrice, targetPrice, 0.0, offerHistory);
        dialogueGen.resetConversation();
    }
    
    public void saveQTable(String filename) {
        System.out.println("\n=== Q-Table Statistics ===");
        System.out.println("Total states learned: " + qTable.size());
        System.out.println("Exploration rate (epsilon): " + epsilon);
        System.out.println("Learning rate (alpha): " + alpha);
    }
    
    public void setEpsilon(double epsilon) {
        this.epsilon = Math.max(0.0, Math.min(1.0, epsilon));
    }
    
    public double getEpsilon() {
        return epsilon;
    }
    
    public List<Double> getOfferHistory() {
        return new ArrayList<>(offerHistory);
    }
}

