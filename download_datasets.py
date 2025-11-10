#!/usr/bin/env python3
"""
Download negotiation datasets from HuggingFace
Updated to handle new HuggingFace dataset format (without loading scripts)
"""

from datasets import load_dataset
import os
import json

def download_craigslist_bargains():
    """Download Craigslist Bargains dataset"""
    print('=' * 60)
    print('Downloading Craigslist Bargains dataset...')
    print('=' * 60)
    
    try:
        # Create directory
        os.makedirs('./data/craigslist_bargains', exist_ok=True)
        
        # Load dataset with trust_remote_code=True (required for datasets with scripts)
        print('Loading dataset from HuggingFace...')
        dataset = load_dataset('stanfordnlp/craigslist_bargains', trust_remote_code=True)
        
        # Save each split as JSON
        for split in ['train', 'validation', 'test']:
            if split in dataset:
                print(f'Processing {split} split ({len(dataset[split])} examples)...')
                
                # Convert to list of dicts
                data = []
                for example in dataset[split]:
                    data.append(example)
                
                # Save as JSON
                output_file = f'./data/craigslist_bargains/{split}.json'
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(data, f, indent=2, ensure_ascii=False)
                
                print(f'✓ Saved to {output_file}')
        
        print('✓ Craigslist Bargains downloaded successfully\n')
        return True
        
    except Exception as e:
        print(f'✗ Error downloading Craigslist Bargains: {e}')
        print('Trying alternative method...\n')
        
        # Alternative: Download directly from GitHub
        try:
            import requests
            print('Attempting to download from GitHub repository...')
            
            base_url = "https://raw.githubusercontent.com/stanfordnlp/cocoa/master/craigslistbargain/data/"
            files = {
                'train': 'train.json',
                'validation': 'dev.json', 
                'test': 'test.json'
            }
            
            for split, filename in files.items():
                url = base_url + filename
                print(f'Downloading {split}...')
                response = requests.get(url)
                
                if response.status_code == 200:
                    output_file = f'./data/craigslist_bargains/{split}.json'
                    with open(output_file, 'w', encoding='utf-8') as f:
                        f.write(response.text)
                    print(f'✓ Downloaded {split}')
                else:
                    print(f'✗ Failed to download {split} (HTTP {response.status_code})')
            
            return True
            
        except Exception as e2:
            print(f'✗ Alternative method failed: {e2}\n')
            return False

def download_deal_or_no_dialog():
    """Download Deal or No Dialog dataset"""
    print('=' * 60)
    print('Downloading Deal or No Dialog dataset...')
    print('=' * 60)
    
    try:
        # Create directory
        os.makedirs('./data/deal_or_no_dialog', exist_ok=True)
        
        # Load dataset with trust_remote_code=True
        print('Loading dataset from HuggingFace...')
        dataset = load_dataset('mikelewis0/deal_or_no_dialog', trust_remote_code=True)
        
        # Save each split as JSON
        for split in ['train', 'validation', 'test']:
            if split in dataset:
                print(f'Processing {split} split ({len(dataset[split])} examples)...')
                
                # Convert to list of dicts
                data = []
                for example in dataset[split]:
                    data.append(example)
                
                # Save as JSON
                output_file = f'./data/deal_or_no_dialog/{split}.json'
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(data, f, indent=2, ensure_ascii=False)
                
                print(f'✓ Saved to {output_file}')
        
        print('✓ Deal or No Dialog downloaded successfully\n')
        return True
        
    except Exception as e:
        print(f'✗ Error downloading Deal or No Dialog: {e}')
        print('Trying alternative method...\n')
        
        # Alternative: Download from GitHub
        try:
            import requests
            print('Attempting to download from GitHub repository...')
            
            base_url = "https://raw.githubusercontent.com/facebookresearch/end-to-end-negotiator/master/data/"
            files = {
                'train': 'negotiate/train.txt',
                'validation': 'negotiate/val.txt',
                'test': 'negotiate/test.txt'
            }
            
            for split, filename in files.items():
                url = base_url + filename
                print(f'Downloading {split}...')
                response = requests.get(url)
                
                if response.status_code == 200:
                    # Parse the text format and convert to JSON
                    lines = response.text.strip().split('\n')
                    data = []
                    for line in lines:
                        if line.strip():
                            data.append(json.loads(line))
                    
                    output_file = f'./data/deal_or_no_dialog/{split}.json'
                    with open(output_file, 'w', encoding='utf-8') as f:
                        json.dump(data, f, indent=2, ensure_ascii=False)
                    print(f'✓ Downloaded {split}')
                else:
                    print(f'✗ Failed to download {split} (HTTP {response.status_code})')
            
            return True
            
        except Exception as e2:
            print(f'✗ Alternative method failed: {e2}\n')
            return False

def main():
    print('\n' + '=' * 60)
    print('Negotiation Datasets Downloader')
    print('=' * 60 + '\n')
    
    # Create data directory
    os.makedirs('./data', exist_ok=True)
    
    # Download datasets
    craigslist_success = download_craigslist_bargains()
    deal_success = download_deal_or_no_dialog()
    
    # Summary
    print('=' * 60)
    print('DOWNLOAD SUMMARY')
    print('=' * 60)
    print(f'Craigslist Bargains: {"✓ SUCCESS" if craigslist_success else "✗ FAILED"}')
    print(f'Deal or No Dialog:   {"✓ SUCCESS" if deal_success else "✗ FAILED"}')
    print('=' * 60)
    
    if craigslist_success or deal_success:
        print('\n✓ Dataset(s) downloaded successfully!')
        print('Location: ./data/')
        
        if craigslist_success:
            print('\nCraigslist Bargains files:')
            print('  - data/craigslist_bargains/train.json')
            print('  - data/craigslist_bargains/validation.json')
            print('  - data/craigslist_bargains/test.json')
        
        if deal_success:
            print('\nDeal or No Dialog files:')
            print('  - data/deal_or_no_dialog/train.json')
            print('  - data/deal_or_no_dialog/validation.json')
            print('  - data/deal_or_no_dialog/test.json')
    else:
        print('\n⚠ All datasets failed to download.')
        print('Please try manual download from:')
        print('  - https://github.com/stanfordnlp/cocoa')
        print('  - https://github.com/facebookresearch/end-to-end-negotiator')
    
    print()

if __name__ == '__main__':
    main()
