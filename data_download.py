import requests
import gzip
import json
import io
import os

folder_path = "data/craigslist_bargains"

URL = "https://storageclwsprod1.blob.core.windows.net/bundles/0xda2bae7241044dbaa4e8ebb02c280d8f/contents.gz?se=2025-11-28T22%3A58%3A21Z&sp=rt&sv=2019-12-12&sr=b&rscd=inline%3B%20filename%3D%22train.json%22&rsce=gzip&rsct=application/json&sig=tkTQUcjQYMwFQNWlUSH5N%2BWYgJ3%2BIy9IQSijv2Mvr%2Bk%3D"

resp = requests.get(URL)
resp.raise_for_status()

content = resp.content
if content[:2] == b'\x1f\x8b':
    with gzip.GzipFile(fileobj=io.BytesIO(content)) as gz:
        data = gz.read().decode('utf-8')
else:
    data = content.decode('utf-8')

records = json.loads(data)

if not os.path.exists(folder_path):
    os.makedirs(folder_path)

with open(os.path.join(folder_path, "train.json"), "w") as f:
    json.dump(records, f)


print(f"Loaded {len(records)} items")
print(records[0])  
