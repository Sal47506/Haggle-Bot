package dealdialect.export;

import dealdialect.engine.*;
import org.json.*;
import java.io.*;

/**
 * Exports negotiation to Jupyter Notebook format (.ipynb).
 */
public class NotebookExporter {
    
    public static void exportToNotebook(DealContext context, DialogueState state, 
                                       String filename) throws IOException {
        JSONObject notebook = new JSONObject();
        
        // Notebook metadata
        JSONObject metadata = new JSONObject();
        metadata.put("kernelspec", new JSONObject()
            .put("display_name", "Python 3")
            .put("language", "python")
            .put("name", "python3"));
        notebook.put("metadata", metadata);
        notebook.put("nbformat", 4);
        notebook.put("nbformat_minor", 4);
        
        // Cells
        JSONArray cells = new JSONArray();
        
        // Title cell
        cells.put(createMarkdownCell("# Negotiation Analysis\n\n" +
            "Analysis of negotiation between buyer and seller."));
        
        // Context cell
        String contextMd = String.format("## Deal Context\n\n" +
            "- Item: %s\n" +
            "- MSRP: $%.2f\n" +
            "- ZOPA: $%.2f\n",
            context.getItem() != null ? context.getItem().getTitle() : "N/A",
            context.getMsrp(),
            context.getZOPASize());
        cells.put(createMarkdownCell(contextMd));
        
        // Python code to load data
        String pythonCode = "import json\nimport pandas as pd\nimport matplotlib.pyplot as plt\n\n" +
            "# Load negotiation data\n" +
            "with open('negotiation.json') as f:\n" +
            "    data = json.load(f)\n\n" +
            "# Create dataframe\n" +
            "offers = pd.DataFrame(data['offers'])\n" +
            "offers.head()";
        cells.put(createCodeCell(pythonCode));
        
        // Visualization code
        String vizCode = "# Plot offer progression\n" +
            "plt.figure(figsize=(10, 6))\n" +
            "buyer_offers = offers[offers['role'] == 'BUYER']\n" +
            "seller_offers = offers[offers['role'] == 'SELLER']\n" +
            "plt.plot(buyer_offers['round'], buyer_offers['price'], 'o-', label='Buyer')\n" +
            "plt.plot(seller_offers['round'], seller_offers['price'], 's-', label='Seller')\n" +
            "plt.xlabel('Round')\n" +
            "plt.ylabel('Price ($)')\n" +
            "plt.title('Negotiation Offer Progression')\n" +
            "plt.legend()\n" +
            "plt.grid(True)\n" +
            "plt.show()";
        cells.put(createCodeCell(vizCode));
        
        notebook.put("cells", cells);
        
        // Write to file
        try (FileWriter file = new FileWriter(filename)) {
            file.write(notebook.toString(2));
        }
    }
    
    private static JSONObject createMarkdownCell(String content) {
        JSONObject cell = new JSONObject();
        cell.put("cell_type", "markdown");
        cell.put("metadata", new JSONObject());
        cell.put("source", new JSONArray().put(content));
        return cell;
    }
    
    private static JSONObject createCodeCell(String code) {
        JSONObject cell = new JSONObject();
        cell.put("cell_type", "code");
        cell.put("execution_count", JSONObject.NULL);
        cell.put("metadata", new JSONObject());
        cell.put("outputs", new JSONArray());
        cell.put("source", new JSONArray().put(code));
        return cell;
    }
}

