package negotiation.grammar;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a weighted grammar rule expansion.
 * Used for probabilistic grammar generation.
 */
public class RuleExpansion {
    private String lhs;  // Left-hand side (category)
    private List<String> rhs;  // Right-hand side (expansion)
    private double weight;  // Probability weight
    
    public RuleExpansion(String lhs, String rhs, double weight) {
        this.lhs = lhs;
        this.rhs = Arrays.asList(rhs.split(","));
        this.weight = weight;
    }
    
    public RuleExpansion(String lhs, List<String> rhs, double weight) {
        this.lhs = lhs;
        this.rhs = new ArrayList<>(rhs);
        this.weight = weight;
    }
    
    public String getLhs() {
        return lhs;
    }
    
    public List<String> getRhs() {
        return rhs;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    /**
     * Adjust weight based on feedback (for learning)
     * @param adjustment Amount to adjust (+/-)
     */
    public void adjustWeight(double adjustment) {
        this.weight = Math.max(0.01, Math.min(1.0, this.weight + adjustment));
    }
    
    @Override
    public String toString() {
        return String.format("%s -> %s (%.2f)", lhs, String.join(" ", rhs), weight);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RuleExpansion)) return false;
        RuleExpansion other = (RuleExpansion) obj;
        return this.lhs.equals(other.lhs) && this.rhs.equals(other.rhs);
    }
    
    @Override
    public int hashCode() {
        return lhs.hashCode() * 31 + rhs.hashCode();
    }
}
