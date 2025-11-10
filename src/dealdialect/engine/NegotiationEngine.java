package dealdialect.engine;

import dealdialect.strategies.*;
import dealdialect.language.LanguageModel;
import java.util.*;

/**
 * Core negotiation engine for DealDialect.
 * Manages turn-by-turn negotiation with bluffing, trust, and detection.
 */
public class NegotiationEngine {
    private DealContext context;
    private DialogueState state;
    private LanguageModel languageModel;
    
    // Policies for each role
    private Map<Role, MovePolicy> movePolicies;
    private Map<Role, BluffPolicy> bluffPolicies;
    private Map<Role, ConcessionPolicy> concessionPolicies;
    private TruthfulnessPolicy truthfulnessPolicy;
    
    // Opponent models
    private Map<Role, OpponentModel> opponentModels;
    
    // Event listeners
    private List<NegotiationListener> listeners;
    
    public NegotiationEngine(DealContext context, LanguageModel languageModel) {
        this.context = context;
        this.state = new DialogueState();
        this.languageModel = languageModel;
        this.movePolicies = new EnumMap<>(Role.class);
        this.bluffPolicies = new EnumMap<>(Role.class);
        this.concessionPolicies = new EnumMap<>(Role.class);
        this.opponentModels = new EnumMap<>(Role.class);
        this.listeners = new ArrayList<>();
        
        // Initialize opponent models
        opponentModels.put(Role.BUYER, new OpponentModel(Role.SELLER));
        opponentModels.put(Role.SELLER, new OpponentModel(Role.BUYER));
    }
    
    /**
     * Execute one negotiation step
     * @param role The role making this move
     * @return The offer made, or null if negotiation ended
     */
    public Offer step(Role role) {
        if (state.isTerminal()) {
            return null;
        }
        
        // Check if should walk away
        MovePolicy movePolicy = movePolicies.get(role);
        if (movePolicy != null && movePolicy.shouldWalkAway(context, state, role)) {
            state.setTerminal(true);
            Offer walkAway = new Offer(role, 0.0, "I'm walking away.", Offer.Intent.WALK_AWAY);
            state.addOffer(walkAway);
            notifyOfferMade(walkAway);
            return walkAway;
        }
        
        // Check if should accept opponent's offer
        if (movePolicy != null && movePolicy.shouldAccept(context, state, role)) {
            Offer oppOffer = state.getLastOfferFrom(role.opposite());
            if (oppOffer != null) {
                state.setDealPrice(oppOffer.getPrice());
                Offer accept = new Offer(role, oppOffer.getPrice(), 
                    languageModel.generateUtterance(Offer.Intent.ACCEPT, oppOffer.getPrice(), 
                                                   context, state, role),
                    Offer.Intent.ACCEPT);
                state.addOffer(accept);
                notifyOfferMade(accept);
                return accept;
            }
        }
        
        // Decide next offer
        Offer nextOffer = null;
        if (movePolicy != null) {
            nextOffer = movePolicy.decideNextOffer(context, state, role);
        }
        
        if (nextOffer == null) {
            // Can't make offer - walk away
            state.setTerminal(true);
            return null;
        }
        
        // Decide if should bluff
        BluffPolicy bluffPolicy = bluffPolicies.get(role);
        if (bluffPolicy != null && bluffPolicy.shouldBluff(context, state, role)) {
            double bluffStrength = bluffPolicy.getBluffStrength(context, state, role);
            double bluffedPrice = bluffPolicy.generateBluffedPrice(nextOffer.getPrice(), 
                                                                   bluffStrength, role);
            String bluffText = languageModel.generateBluffText(Offer.Intent.BLUFF_PUFF, 
                                                              bluffStrength, context, role);
            
            nextOffer = Offer.createBluff(role, bluffedPrice, bluffText, bluffStrength);
        } else {
            // Generate regular utterance
            String utterance = languageModel.generateUtterance(nextOffer.getIntent(), 
                                                              nextOffer.getPrice(),
                                                              context, state, role);
            nextOffer.setUtterance(utterance);
        }
        
        // Add offer to state
        state.addOffer(nextOffer);
        state.nextRound();
        
        // Update opponent model
        OpponentModel oppModel = opponentModels.get(role.opposite());
        if (oppModel != null) {
            oppModel.updateFromOffer(nextOffer, context, state);
        }
        
        // Detect bluffs
        detectBluff(nextOffer);
        
        // Update trust naturally
        updateTrust();
        
        // Check terminal conditions
        detectTerminal();
        
        // Notify listeners
        notifyOfferMade(nextOffer);
        
        return nextOffer;
    }
    
    /**
     * Detect if an offer is a bluff
     */
    public boolean detectBluff(Offer offer) {
        if (!offer.isBluff()) return false;
        
        Role detector = offer.getRole().opposite();
        
        if (truthfulnessPolicy == null) return false;
        
        double detectionProb = truthfulnessPolicy.detectBluffProbability(
            offer, context, state, detector
        );
        
        // Probabilistic detection
        boolean detected = new Random().nextDouble() < detectionProb;
        
        if (detected) {
            truthfulnessPolicy.updateTrustOnBluff(state, offer.getRole(), detectionProb);
            notifyBluffDetected(offer, detectionProb);
            
            // Update opponent model
            OpponentModel oppModel = opponentModels.get(detector);
            if (oppModel != null) {
                oppModel.recordBluffDetection();
            }
        }
        
        return detected;
    }
    
    /**
     * Update trust levels
     */
    public void updateTrust() {
        if (truthfulnessPolicy == null) return;
        
        int round = state.getCurrentRound();
        
        // Natural trust decay
        double buyerTrust = state.getBuyerTrust();
        double sellerTrust = state.getSellerTrust();
        
        state.setBuyerTrust(truthfulnessPolicy.calculateTrustDecay(buyerTrust, round));
        state.setSellerTrust(truthfulnessPolicy.calculateTrustDecay(sellerTrust, round));
    }
    
    /**
     * Check for terminal conditions
     */
    public void detectTerminal() {
        // Check time limit
        if (state.getCurrentRound() >= context.getTimeLimit()) {
            state.setTerminal(true);
        }
        
        // Check if trust is too low
        if (state.getBuyerTrust() < 0.1 || state.getSellerTrust() < 0.1) {
            state.setTerminal(true);
        }
    }
    
    /**
     * Compute surplus for a given deal price
     */
    public Map<String, Double> computeSurplus(double dealPrice) {
        Map<String, Double> surplus = new HashMap<>();
        
        double buyerSurplus = context.getBuyerValue() - dealPrice;
        double sellerSurplus = dealPrice - context.getSellerCost();
        double totalSurplus = buyerSurplus + sellerSurplus;
        
        surplus.put("buyer", buyerSurplus);
        surplus.put("seller", sellerSurplus);
        surplus.put("total", totalSurplus);
        surplus.put("welfare", totalSurplus);
        
        // Calculate deadweight loss (compared to efficient outcome)
        double efficientSurplus = context.getZOPASize();
        double deadweightLoss = Math.max(0, efficientSurplus - totalSurplus);
        surplus.put("deadweight_loss", deadweightLoss);
        
        return surplus;
    }
    
    /**
     * Process human input
     */
    public Offer processHumanInput(String input, Role humanRole) {
        Offer humanOffer = languageModel.parseHumanInput(input, humanRole);
        
        if (humanOffer != null) {
            // Handle special intents
            if (humanOffer.getIntent() == Offer.Intent.ACCEPT) {
                Offer oppOffer = state.getLastOfferFrom(humanRole.opposite());
                if (oppOffer != null) {
                    state.setDealPrice(oppOffer.getPrice());
                    humanOffer.setPrice(oppOffer.getPrice());
                }
            } else if (humanOffer.getIntent() == Offer.Intent.WALK_AWAY) {
                state.setTerminal(true);
            }
            
            // Add to state
            state.addOffer(humanOffer);
            state.nextRound();
            
            // Update opponent model
            OpponentModel oppModel = opponentModels.get(humanRole.opposite());
            if (oppModel != null) {
                oppModel.updateFromOffer(humanOffer, context, state);
            }
            
            // Notify listeners
            notifyOfferMade(humanOffer);
        }
        
        return humanOffer;
    }
    
    // Policy setters
    public void setMovePolicy(Role role, MovePolicy policy) {
        movePolicies.put(role, policy);
    }
    
    public void setBluffPolicy(Role role, BluffPolicy policy) {
        bluffPolicies.put(role, policy);
    }
    
    public void setConcessionPolicy(Role role, ConcessionPolicy policy) {
        concessionPolicies.put(role, policy);
    }
    
    public void setTruthfulnessPolicy(TruthfulnessPolicy policy) {
        this.truthfulnessPolicy = policy;
    }
    
    // Getters
    public DealContext getContext() { return context; }
    public DialogueState getState() { return state; }
    public OpponentModel getOpponentModel(Role role) { return opponentModels.get(role.opposite()); }
    
    // Listener management
    public void addListener(NegotiationListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(NegotiationListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyOfferMade(Offer offer) {
        for (NegotiationListener listener : listeners) {
            listener.onOfferMade(offer, state);
        }
    }
    
    private void notifyBluffDetected(Offer offer, double confidence) {
        for (NegotiationListener listener : listeners) {
            listener.onBluffDetected(offer, confidence, state);
        }
    }
    
    /**
     * Interface for listening to negotiation events
     */
    public interface NegotiationListener {
        void onOfferMade(Offer offer, DialogueState state);
        void onBluffDetected(Offer offer, double confidence, DialogueState state);
    }
}

