import gzip
import os

assets_dir = r"app/src/main/assets"
gz_path = os.path.join(assets_dir, "puzzles_subset.csv.gz")
out_csv = os.path.join(assets_dir, "puzzles.csv")

if not os.path.exists(gz_path):
    # Try absolute path
    gz_path = r"w:\Mobile Application\Chess\app\src\main\assets\puzzles_subset.csv.gz"
    out_csv = r"w:\Mobile Application\Chess\app\src\main\assets\puzzles.csv"

print("Decompressing puzzles...")
puzzles = []
try:
    with gzip.open(gz_path, "rt", encoding="utf-8") as f:
        # Read header
        header = f.readline()
        puzzles.append(header.strip())
        
        for idx, line in enumerate(f):
            puzzles.append(line.strip())
            if len(puzzles) >= 1001:  # 1000 + 1 header
                break
except Exception as e:
    print(f"Failed to read gz: {e}")
    exit(1)

print(f"Extracted {len(puzzles) - 1} puzzles.")

with open(out_csv, "w", encoding="utf-8") as out:
    for p in puzzles:
        out.write(p + "\n")

print(f"Saved to {out_csv}.")
