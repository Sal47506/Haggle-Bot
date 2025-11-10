package negotiation.util;

import java.io.*;
import java.net.*;

/**
 * Utility class to download negotiation datasets from HuggingFace.
 * Provides instructions and helper methods for dataset acquisition.
 */
public class DatasetDownloader {
    
    /**
     * Print instructions for downloading Craigslist Bargains dataset
     */
    public static void printCraigslistInstructions() {
        System.out.println("=== Craigslist Bargains Dataset ===");
        System.out.println("Source: Stanford NLP");
        System.out.println("URL: https://huggingface.co/datasets/stanfordnlp/craigslist_bargains");
        System.out.println();
        System.out.println("To download:");
        System.out.println("1. Install HuggingFace datasets library:");
        System.out.println("   pip install datasets");
        System.out.println();
        System.out.println("2. Download using Python:");
        System.out.println("   from datasets import load_dataset");
        System.out.println("   dataset = load_dataset('stanfordnlp/craigslist_bargains')");
        System.out.println("   dataset.save_to_disk('./data/craigslist_bargains')");
        System.out.println();
        System.out.println("3. Or download manually from:");
        System.out.println("   https://github.com/stanfordnlp/cocoa");
        System.out.println();
        System.out.println("Dataset Statistics:");
        System.out.println("- 6682 human-human dialogues");
        System.out.println("- Average dialogue length: 9.2 turns");
        System.out.println("- Categories: housing, furniture, cars, bikes, phones, electronics");
        System.out.println();
    }
    
    /**
     * Print instructions for downloading Deal or No Dialog dataset
     */
    public static void printDealOrNoDialogInstructions() {
        System.out.println("=== Deal or No Dialog Dataset ===");
        System.out.println("Source: Facebook AI Research / Meta");
        System.out.println("URL: https://huggingface.co/datasets/mikelewis0/deal_or_no_dialog");
        System.out.println();
        System.out.println("To download:");
        System.out.println("1. Install HuggingFace datasets library:");
        System.out.println("   pip install datasets");
        System.out.println();
        System.out.println("2. Download using Python:");
        System.out.println("   from datasets import load_dataset");
        System.out.println("   dataset = load_dataset('mikelewis0/deal_or_no_dialog')");
        System.out.println("   dataset.save_to_disk('./data/deal_or_no_dialog')");
        System.out.println();
        System.out.println("3. Or download manually from:");
        System.out.println("   https://github.com/facebookresearch/end-to-end-negotiator");
        System.out.println();
        System.out.println("Dataset Statistics:");
        System.out.println("- Multi-issue bargaining (books, hats, balls)");
        System.out.println("- Different values for each agent");
        System.out.println("- Includes strategic reasoning");
        System.out.println();
    }
    
    /**
     * Create a Python script to download both datasets
     */
    public static void generateDownloadScript(String outputPath) {
        try {
            PrintWriter writer = new PrintWriter(new File(outputPath + "/download_datasets.py"));
            
            writer.println("#!/usr/bin/env python3");
            writer.println("\"\"\"");
            writer.println("Download negotiation datasets from HuggingFace");
            writer.println("\"\"\"");
            writer.println();
            writer.println("from datasets import load_dataset");
            writer.println("import os");
            writer.println();
            writer.println("# Create data directory");
            writer.println("os.makedirs('./data', exist_ok=True)");
            writer.println();
            writer.println("print('Downloading Craigslist Bargains dataset...')");
            writer.println("try:");
            writer.println("    craigslist = load_dataset('stanfordnlp/craigslist_bargains')");
            writer.println("    craigslist.save_to_disk('./data/craigslist_bargains')");
            writer.println("    print('✓ Craigslist Bargains downloaded successfully')");
            writer.println("except Exception as e:");
            writer.println("    print(f'✗ Error downloading Craigslist Bargains: {e}')");
            writer.println();
            writer.println("print('\\nDownloading Deal or No Dialog dataset...')");
            writer.println("try:");
            writer.println("    deal = load_dataset('mikelewis0/deal_or_no_dialog')");
            writer.println("    deal.save_to_disk('./data/deal_or_no_dialog')");
            writer.println("    print('✓ Deal or No Dialog downloaded successfully')");
            writer.println("except Exception as e:");
            writer.println("    print(f'✗ Error downloading Deal or No Dialog: {e}')");
            writer.println();
            writer.println("print('\\nDatasets downloaded to ./data/')");
            
            writer.close();
            
            System.out.println("Download script created: " + outputPath + "/download_datasets.py");
            System.out.println("Run with: python3 download_datasets.py");
            
        } catch (FileNotFoundException e) {
            System.err.println("Error creating download script: " + e.getMessage());
        }
    }
    
    /**
     * Main method to print instructions
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   Negotiation Datasets - Download Instructions            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        printCraigslistInstructions();
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println();
        printDealOrNoDialogInstructions();
        
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Generating download script...");
        generateDownloadScript(".");
    }
}

