package dialogue;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import models.NegotiationState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wraps the MarkovDialogueGenerator and optionally re-ranks multiple Markov
 * samples using a Hugging Face transformer text-embedding model for better
 * contextual awareness. If the transformer is unavailable, it silently
 * falls back to plain Markov generation.
 */
public class MarkovTransformerDialogueGenerator implements DialogueGenerator {

    private final MarkovDialogueGenerator markov;
    private boolean useTransformer;
    private Predictor<String, float[]> embeddingPredictor;
    private ZooModel<String, float[]> embeddingModel;
    private int rerankSamples = 5;

    public MarkovTransformerDialogueGenerator(String datasetPath, int order, boolean useTransformer) throws Exception {
        this.markov = new MarkovDialogueGenerator(datasetPath, order);
        this.useTransformer = useTransformer;
        if (useTransformer) {
            initTransformer();
        }
    }

    private void initTransformer() {
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .optFilter("backbone", "sentence-transformers/all-MiniLM-L6-v2")
                    .optEngine("PyTorch")
                    .build();
            embeddingModel = criteria.loadModel();
            embeddingPredictor = embeddingModel.newPredictor();
            System.out.println("Loaded Hugging Face transformer reranker (all-MiniLM-L6-v2).");
        } catch (IOException | ModelException e) {
            System.err.println("Transformer reranker unavailable, falling back to Markov only: " + e.getMessage());
            useTransformer = false;
        }
    }

    @Override
    public String generate(String intent, double price) {
        return generate(intent, price, null);
    }

    @Override
    public String generate(String intent, double price, String opponentMessage) {
        // If transformer is disabled or we have no context to compare, just use Markov.
        if (!useTransformer || embeddingPredictor == null || opponentMessage == null || opponentMessage.trim().isEmpty()) {
            return markov.generate(intent, price, opponentMessage);
        }

        try {
            List<String> candidates = sampleCandidates(intent, price, opponentMessage, rerankSamples);
            if (candidates.isEmpty()) {
                return markov.generate(intent, price, opponentMessage);
            }

            float[] opponentVec = embeddingPredictor.predict(opponentMessage);
            String best = candidates.get(0);
            double bestScore = -1.0;

            for (String candidate : candidates) {
                float[] candidateVec = embeddingPredictor.predict(candidate);
                double score = cosine(candidateVec, opponentVec);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            return best;
        } catch (TranslateException e) {
            System.err.println("Transformer reranking failed, using Markov only: " + e.getMessage());
            return markov.generate(intent, price, opponentMessage);
        }
    }

    private List<String> sampleCandidates(String intent, double price, String opponentMessage, int count) {
        Set<String> unique = new HashSet<>();
        // Always include the first generation
        unique.add(markov.generate(intent, price, opponentMessage));

        int attempts = 0;
        int maxAttempts = count * 5; // prevent endless loops if generations repeat
        while (unique.size() < count && attempts < maxAttempts) {
            unique.add(markov.generate(intent, price, opponentMessage));
            attempts++;
        }
        return new ArrayList<>(unique);
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public void updateContext(NegotiationState state, String lastMessage) {
        markov.updateContext(state, lastMessage);
    }

    @Override
    public void resetConversation() {
        markov.resetConversation();
    }

    @Override
    public void setItemContext(String item) {
        markov.setItemContext(item);
    }
}


