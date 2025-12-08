# HaggleBot: An AI-Powered Negotiation Agent

HaggleBot is an intelligent negotiation agent that uses **Reinforcement Learning (Q-Learning)** and **advanced dialogue generation techniques** to simulate realistic buyer behavior in price negotiations. The agent learns optimal negotiation strategies through interaction and generates contextually appropriate responses using multiple dialogue generation methods.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Dialogue Generation](#dialogue-generation)
  - [Markov Chain Dialogue Generator](#markov-chain-dialogue-generator)
  - [Contextual Dialogue Generator](#contextual-dialogue-generator)
- [Reinforcement Learning](#reinforcement-learning)
- [Installation & Usage](#installation--usage)
- [Code Walkthrough](#code-walkthrough)
- [Technical Details](#technical-details)

## Features

### 🤖 **Intelligent Negotiation Agent**
- **Q-Learning based decision making**: Learns optimal negotiation strategies through trial and error
- **Tactical behavior**: Employs different negotiation tactics (Hard Ball, Opportunistic, Sneaky)
- **Context-aware responses**: Generates dialogue that adapts to conversation history
- **Price inference**: Automatically extracts prices from natural language messages

### 💬 **Advanced Dialogue Generation**
- **Markov Chain Model**: Uses n-gram models to generate natural-sounding dialogue
- **Contextual Similarity**: TF-IDF based semantic matching for contextually relevant responses
- **Placeholder system**: Supports `<ITEM>`, `<CONTEXT>`, and `<PRICE>` placeholders for dynamic content
- **Intent-based generation**: Generates responses based on negotiation intents (OFFER, COUNTER, REJECT, ACCEPT)

### 📊 **State Management**
- Tracks negotiation rounds, price gaps, and offer history
- Maintains conversation context for coherent dialogue
- Monitors consecutive rejects and deal status

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    InteractiveNegotiation                   │
│                  (Main Application Loop)                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      BuyerAgent                             │
│  ┌──────────────────┐  ┌──────────────────────────────────┐ │
│  │  Q-Learning      │  │  Dialogue Generation            │ │
│  │  - Q-Table       │  │  - Intent Selection              │ │
│  │  - Epsilon-Greedy│  │  - Price Decision                │ │
│  │  - Reward Func   │  │  - Tactical Modifiers            │ │
│  └──────────────────┘  └──────────────────────────────────┘ │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
┌──────────────────┐        ┌──────────────────────┐
│ MarkovDialogue   │        │ ContextualDialogue    │
│ Generator        │        │ Generator             │
│ - N-gram models  │        │ - TF-IDF similarity   │
│ - Seed utterances│        │ - Semantic matching    │
└──────────────────┘        └──────────────────────┘
```

## Dialogue Generation

HaggleBot supports two dialogue generation approaches, each with unique strengths:

### Markov Chain Dialogue Generator

The **MarkovDialogueGenerator** uses n-gram Markov models to generate natural dialogue by learning word transition patterns from training data.

#### How It Works

1. **Training Phase**: Builds n-gram models from negotiation examples
   ```java
   private Map<String, Map<String, List<String>>> buildNGrams(List<String> utterances, int n) {
       Map<String, List<String>> transitions = new HashMap<>();
       
       for (String utterance : utterances) {
           String normalized = normalizePrice(utterance);
           normalized = normalizeItem(normalized);
           normalized = normalizeContext(normalized);
           List<String> tokens = tokenize(normalized);
           
           // Create n-gram transitions
           for (int i = 0; i <= tokens.size() - n; i++) {
               List<String> context = new ArrayList<>();
               for (int j = 0; j < n - 1; j++) {
                   context.add(tokens.get(i + j));
               }
               String key = String.join(" ", context);
               String next = tokens.get(i + n - 1);
               
               transitions.computeIfAbsent(key, k -> new ArrayList<>()).add(next);
           }
       }
       return transitions;
   }
   ```

2. **Seed Utterances**: Stores real negotiation examples as templates
   ```java
   seedUtterances.put(intent, new ArrayList<>(utterances));
   ```

3. **Generation Process**:
   - Selects a seed utterance based on intent and context relevance
   - Replaces placeholders (`<PRICE>`, `<ITEM>`, `<CONTEXT>`)
   - Validates the generated response

#### Placeholder System

The generator supports dynamic placeholders that are replaced during generation:

- **`<PRICE>`**: Replaced with the actual offer price
- **`<ITEM>`**: Replaced with the item name from seller's input
- **`<CONTEXT>`**: Replaced with meaningful words from previous seller messages

```java
private String replacePlaceholders(String utterance, double targetPrice) {
    String result = utterance;
    
    // Replace price placeholders
    java.util.regex.Matcher matcher = pricePattern.matcher(result);
    String priceStr = "$" + String.format("%.2f", targetPrice);
    result = matcher.replaceAll(java.util.regex.Matcher.quoteReplacement(priceStr));
    
    // Replace <ITEM> placeholder with actual item name
    String itemName = !itemContext.isEmpty() ? itemContext : "this";
    result = result.replaceAll("\\b<ITEM>\\b", itemName);
    
    // Replace <CONTEXT> placeholder with relevant portions from previous seller messages
    String contextText = extractContextFromHistory();
    if (!contextText.isEmpty()) {
        result = result.replaceAll("\\b<CONTEXT>\\b", contextText);
    }
    
    return result.trim();
}
```

#### Context Extraction

The system extracts meaningful context from conversation history:

```java
private String extractContextFromHistory() {
    // Get the most recent seller messages (last 2-3 messages)
    int numMessages = Math.min(3, conversationHistory.size());
    List<String> recentMessages = conversationHistory.subList(
        conversationHistory.size() - numMessages, 
        conversationHistory.size()
    );
    
    // Extract meaningful words (filtering out stop words and prices)
    List<String> contextPhrases = new ArrayList<>();
    Set<String> stopWords = new HashSet<>(Arrays.asList(
        "the", "and", "for", "can", "will", "this", "that", ...
    ));
    
    // Process messages and extract key phrases
    // Returns up to 10 meaningful words
    return String.join(" ", contextPhrases.subList(0, Math.min(10, contextPhrases.size())));
}
```

#### Seed Relevance Scoring

Seeds are filtered and scored based on relevance to the current negotiation context:

```java
private double calculateSeedRelevance(String seed, String intent, double price, String opponentMessage) {
    double score = 0.5;
    String lowerSeed = seed.toLowerCase();
    
    // Detect bluff indicators
    boolean isBluff = lowerSeed.contains("budget") || lowerSeed.contains("can't afford") || 
                     lowerSeed.contains("other") || lowerSeed.contains("elsewhere") ||
                     lowerSeed.contains("firm") || lowerSeed.contains("final");
    
    // Adjust score based on negotiation state
    if (currentState != null) {
        double lastOffer = currentState.getLastOfferPrice();
        
        if (intent.equals("COUNTER") || intent.equals("OFFER")) {
            if (price > lastOffer) {
                if (lowerSeed.contains("higher") || lowerSeed.contains("more")) {
                    score += 0.2;
                }
            }
            
            // Late-round final offers get higher scores
            if (currentState.getRound() > 5) {
                if (lowerSeed.contains("final") || lowerSeed.contains("best")) {
                    score += 0.2;
                }
            }
        }
    }
    
    return Math.min(score, 1.0);
}
```

### Contextual Dialogue Generator

The **ContextualDialogueGenerator** uses **TF-IDF (Term Frequency-Inverse Document Frequency)** to find semantically similar responses based on the opponent's message.

#### How It Works

1. **Vocabulary Building**: Creates a vocabulary from all training utterances
   ```java
   private void buildVocab(String datasetPath) throws Exception {
       for (String intent : utterancesByIntent.keySet()) {
           List<String> utterances = utterancesByIntent.get(intent);
           corpus.addAll(utterances);
           
           for (String utterance : utterances) {
               String[] words = utterance.toLowerCase().split("\\s+");
               for (String word : words) {
                   if (!vocab.containsKey(word)) {
                       vocab.put(word, idx++);
                   }
               }
           }
       }
   }
   ```

2. **TF-IDF Vectorization**: Converts messages into TF-IDF vectors
   ```java
   private double[] computeTFIDF(String sentence) {
       double[] vec = new double[vocab.size()];
       String[] words = sentence.toLowerCase().split("\\s+");
       
       // Calculate term frequency
       for (String word : words) {
           if (vocab.containsKey(word)) {
               vec[vocab.get(word)] += 1;
           }
       }
       
       // Apply inverse document frequency
       for (String word : words) {
           if (!vocab.containsKey(word)) continue;
           int idx = vocab.get(word);
           int docCount = 0;
           for (String doc : corpus) {
               if (doc.toLowerCase().contains(word)) {
                   docCount++;
               }
           }
           double idf = Math.log((double) corpus.size() / (docCount + 1));
           vec[idx] *= idf;
       }
       
       return vec;
   }
   ```

3. **Semantic Filtering**: Filters candidates by cosine similarity
   ```java
   private List<String> filterByContext(List<String> candidates, String intent, 
                                        double price, String opponentMessage) {
       List<String> filtered = new ArrayList<>();
       double[] opponentVec = computeTFIDF(opponentMessage);
       
       for (String candidate : candidates) {
           double[] candidateVec = computeTFIDF(candidate);
           double similarity = cosine(candidateVec, opponentVec);
           
           // Only include candidates with similarity > 0.3
           if (similarity > 0.3) {
               filtered.add(candidate);
           }
       }
       
       return filtered.isEmpty() ? candidates : filtered;
   }
   ```

## Reinforcement Learning

HaggleBot uses **Q-Learning**, a model-free reinforcement learning algorithm, to learn optimal negotiation strategies.

### Q-Learning Components

#### 1. State Representation

States are discretized into buckets based on:
- **Round number**: Current negotiation round (bucketed up to 10)
- **Price gap**: Difference between buyer and seller prices (bucketed by $10 increments)
- **Consecutive rejects**: Number of consecutive rejections (bucketed up to 5)

```java
private String getStateKey(double sellerPrice) {
    double priceGap = Math.abs(sellerPrice - currentOffer);
    int roundBucket = Math.min(state.getRound(), 10);
    int gapBucket = (int) Math.floor(priceGap / 10.0);
    int rejectsBucket = Math.min(consecutiveRejects, 5);
    return roundBucket + "_" + gapBucket + "_" + rejectsBucket;
}
```

#### 2. Action Space

The agent can choose from three actions:
- **COUNTER**: Make a counter-offer
- **REJECT**: Reject the seller's offer
- **ACCEPT**: Accept the seller's offer

```java
private static final String[] ACTIONS = {"COUNTER", "REJECT", "ACCEPT"};
```

#### 3. Q-Table

The Q-table stores Q-values for each state-action pair:

```java
private Map<String, double[]> qTable;  // State -> [Q(COUNTER), Q(REJECT), Q(ACCEPT)]
```

#### 4. Epsilon-Greedy Exploration

The agent uses epsilon-greedy exploration to balance exploitation and exploration:

```java
private int chooseAction(String stateKey) {
    if (!qTable.containsKey(stateKey)) {
        qTable.put(stateKey, new double[]{0.0, 0.0, 0.0});
    }

    // Explore: 20% chance of random action
    if (random.nextDouble() < epsilon) {
        return random.nextInt(3);
    }

    // Exploit: Choose action with highest Q-value
    double[] qValues = qTable.get(stateKey);
    return argmax(qValues);
}
```

#### 5. Reward Function

Rewards are designed to encourage favorable deals:

```java
double reward = 0.0;
if (intent.equals("ACCEPT")) {
    if (sellerPrice <= reservationPrice) {
        // Higher reward for better deals
        reward = (reservationPrice - sellerPrice) * 2.0;
    } else {
        reward = -10.0;  // Penalty for accepting bad deals
    }
    consecutiveRejects = 0;
} else if (intent.equals("REJECT")) {
    reward = -2.0;  // Small penalty for rejecting
    consecutiveRejects++;
} else {
    // Reward based on gap reduction
    double gapReduction = Math.abs(sellerPrice - currentOffer);
    reward = -gapReduction / 10.0;
    consecutiveRejects = 0;
}
```

#### 6. Q-Value Update

Q-values are updated using the Bellman equation:

```java
private void updateQTable(String stateKey, int action, double reward, String nextStateKey) {
    double[] qValues = qTable.get(stateKey);
    double[] nextQValues = qTable.getOrDefault(nextStateKey, new double[]{0.0, 0.0, 0.0});

    // Bellman equation: Q(s,a) = (1-α)Q(s,a) + α[r + γ*max(Q(s',a'))]
    double bestNextQ = Math.max(nextQValues[0], Math.max(nextQValues[1], nextQValues[2]));
    qValues[action] = (1 - alpha) * qValues[action] +
                    alpha * (reward + gamma * bestNextQ);

    qTable.put(stateKey, qValues);
}
```

**Parameters**:
- **α (alpha) = 0.1**: Learning rate - controls how quickly the agent updates Q-values
- **γ (gamma) = 0.95**: Discount factor - determines importance of future rewards
- **ε (epsilon) = 0.2**: Exploration rate - probability of taking a random action

### Price Decision Logic

The agent uses sophisticated logic to decide counter-offer prices:

```java
private double decidePrice(String intent, double sellerPrice, int stubbornnessLevel) {
    switch (intent) {
        case "ACCEPT":
            return sellerPrice;
            
        case "COUNTER":
            if (stubbornnessLevel >= 2) {
                // Stubborn: only small increase
                double stubbornIncrease = currentOffer * 0.02;
                return Math.round((currentOffer + stubbornIncrease) * 100.0) / 100.0;
            }
            
            // Progressive counter-offering based on round
            double progressFactor = Math.min(1.0, state.getRound() / 8.0);
            double minOffer = targetPrice;
            double maxOffer = reservationPrice;
            
            double baseCounter = minOffer + (maxOffer - minOffer) * progressFactor;
            
            // Bridge 30% of the gap
            double gapToBridge = (sellerPrice - currentOffer) * 0.3;
            double newOffer = currentOffer + gapToBridge;
            
            // Constrain within bounds
            newOffer = Math.max(newOffer, baseCounter);
            newOffer = Math.min(newOffer, reservationPrice); 
            newOffer = Math.max(newOffer, currentOffer * 1.05);  // At least 5% increase
            
            return Math.round(newOffer * 100.0) / 100.0;
            
        case "REJECT":
            return currentOffer;  // Maintain current offer
    }
}
```

### Tactical Behavior

The agent employs different negotiation tactics:

```java
private enum Tactic {
    HARD_BALL,      // Aggressive, firm stance
    OPPORTUNISTIC,  // Quick to accept good deals
    SNEAKY,         // Uses deception (budget constraints, alternatives)
    DEFAULT
}
```

Tactics are selected based on negotiation state:

```java
private Tactic pickTactic(String intent, double sellerPrice) {
    double gap = sellerPrice - currentOffer;
    int round = state.getRound();
    
    if (intent.equals("REJECT")) {
        if (gap > sellerPrice * 0.5) {
            return Tactic.HARD_BALL;  // Large gap: be aggressive
        }
        return random.nextDouble() < 0.7 ? Tactic.HARD_BALL : Tactic.SNEAKY;
    }
    
    if (intent.equals("COUNTER")) {
        if (round >= 6) {
            // Late rounds: mix of sneaky and hard ball
            if (gap > sellerPrice * 0.2) {
                return random.nextDouble() < 0.6 ? Tactic.SNEAKY : Tactic.HARD_BALL;
            }
            return Tactic.OPPORTUNISTIC;
        }
        // Early rounds: varied tactics
        double r = random.nextDouble();
        if (r < 0.33) return Tactic.HARD_BALL;
        if (r < 0.66) return Tactic.OPPORTUNISTIC;
        return Tactic.SNEAKY;
    }
    
    return Tactic.DEFAULT;
}
```

Tactics modify generated responses:

```java
private String generateTacticalResponse(String intent, double price, 
                                        String sellerMessage, Tactic tactic) {
    String baseResponse = dialogueGen.generate(intent, price, sellerMessage);
    
    switch (tactic) {
        case HARD_BALL:
            if (!baseResponse.toLowerCase().contains("final") && state.getRound() > 3) {
                baseResponse = addTacticalModifier(baseResponse, "This is my final offer.");
            }
            break;
        case SNEAKY:
            if (!baseResponse.toLowerCase().contains("budget")) {
                baseResponse = addTacticalModifier(baseResponse, "I saw similar ones for less.");
            }
            break;
        case OPPORTUNISTIC:
            if (intent.equals("ACCEPT")) {
                baseResponse = addTacticalModifier(baseResponse, "I can pay cash right now.");
            }
            break;
    }
    
    return baseResponse;
}
```

## Installation & Usage

### Prerequisites

- Java 8 or higher
- Maven 3.6+
- Training dataset in JSON format (Craigslist Bargains format)

### Building the Project

```bash
mvn clean compile
```

### Running the Interactive Negotiation

```bash
# Windows
run_interactive.ps1

# Or using Maven
run_interactive_maven.bat
```

### Usage Example

```
=== Interactive Negotiation ===

Enter item name: laptop
Enter your asking price: $500
Enter buyer's reservation price (max they'll pay): $400
Enter buyer's target price (ideal price): $300

=== Negotiation Started ===
Item: laptop
Your asking price: $500.00

Buyer: I'm interested in laptop. I can offer $300.00.
Buyer's offer: $300.00

You (Seller): That's too low. I need at least $450.
  [Inferred price: $450.00]

Buyer: How about $320.00?
Buyer's offer: $320.00
Round: 2 | Price gap: $130.00 | Exploration: 20.0%

...
```

## Code Walkthrough

### Main Negotiation Loop

The `InteractiveNegotiation` class manages the negotiation session:

```java
public static void main(String[] args) {
    // Initialize buyer agent
    BuyerAgent buyer = new BuyerAgent(datasetPath, buyerReservation, buyerTarget);
    buyer.setItemContext(itemName);
    
    // Get initial offer
    String buyerMessage = buyer.makeInitialOffer();
    System.out.println("Buyer: " + buyerMessage);
    
    // Negotiation loop
    while (round < 20) {
        // Get seller input
        String sellerMessage = reader.readLine().trim();
        double inferredPrice = inferPriceFromMessage(sellerMessage);
        
        // Get buyer response
        String buyerResponse = buyer.respondToSeller(sellerMessage, currentSellerPrice);
        
        // Check for deal
        if (buyer.getState().getOpponentLastOffer() <= buyer.getReservationPrice()) {
            System.out.println("\n=== Deal Reached! ===");
            break;
        }
    }
}
```

### Buyer Agent Response Generation

The `respondToSeller` method orchestrates the entire response generation:

```java
public String respondToSeller(String sellerMessage, double sellerPrice) {
    // 1. Get current state
    String currentStateKey = getStateKey(sellerPrice);
    
    // 2. Choose action using Q-Learning
    int action = chooseAction(currentStateKey);
    String intent = actionToIntent(action);
    
    // 3. Calculate reward for previous action
    double reward = calculateReward(intent, sellerPrice);
    
    // 4. Update Q-table
    if (previousStateKey != null && previousAction >= 0) {
        updateQTable(previousStateKey, previousAction, reward, currentStateKey);
    }
    
    // 5. Decide price and intent
    double offerPrice = decidePrice(intent, sellerPrice, consecutiveRejects);
    intent = decideIntent(sellerPrice);
    
    // 6. Update state
    state = new NegotiationState(...);
    dialogueGen.updateContext(state, sellerMessage);
    
    // 7. Generate tactical response
    currentTactic = pickTactic(intent, sellerPrice);
    String response = generateTacticalResponse(intent, offerPrice, sellerMessage, currentTactic);
    
    return response;
}
```

## Technical Details

### Dataset Format

The system expects JSON data in the Craigslist Bargains format:

```json
[
  {
    "agents": [
      {"id": "0", "role": "buyer"},
      {"id": "1", "role": "seller"}
    ],
    "events": [
      {
        "agent": "0",
        "action": "message",
        "data": "I can offer $50 for this item."
      }
    ]
  }
]
```

### Intent Classification

Intents are inferred from utterance content:

- **OFFER**: Contains price, "offer", "how about"
- **COUNTER**: Contains "counter", "how about"
- **REJECT**: Contains "reject", "can't", "won't", "no deal"
- **ACCEPT**: Contains "accept", "deal", "agreed"

### State Management

The `NegotiationState` class tracks:

```java
public class NegotiationState {
    private int round;                    // Current round number
    private double lastOfferPrice;        // Buyer's last offer
    private double myReservationPrice;    // Maximum buyer will pay
    private double myTargetPrice;         // Ideal price for buyer
    private double opponentLastOffer;      // Seller's last offer
    private double priceGap;              // Absolute price difference
    private double priceGapPercentage;    // Relative price gap
    private int consecutiveRejects;       // Number of consecutive rejects
    private boolean dealReached;          // Whether deal was reached
    private List<Double> offerHistory;   // History of all offers
}
```

## Future Enhancements

- [ ] Transformer-based dialogue generation
- [ ] Multi-item negotiations
- [ ] Emotional state modeling
- [ ] Advanced reward shaping
- [ ] Deep Q-Network (DQN) implementation
- [ ] Multi-agent negotiation scenarios

## License

This project is provided as-is for educational and research purposes.

## Acknowledgments

- Training data from Craigslist Bargains dataset
- Q-Learning algorithm based on standard RL principles
- Dialogue generation inspired by natural language processing techniques
