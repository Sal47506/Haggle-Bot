package negotiation.util;

import negotiation.models.*;
import java.util.List;

/**
 * Interface for loading negotiation datasets.
 */
public interface IDatasetLoader {
    
    /**
     * Load negotiation dialogues from the dataset
     * @return List of negotiation transcripts
     */
    List<NegotiationTranscript> loadDialogues();
    
    /**
     * Load items being negotiated
     * @return List of items
     */
    List<Item> loadItems();
    
    /**
     * Get dataset name
     * @return dataset name
     */
    String getDatasetName();
}

