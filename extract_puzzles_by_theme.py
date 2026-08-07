import os
from collections import Counter
import zstandard as zstd

zst_path = "lichess_db_puzzle.csv.zst"
out_path = "app/src/main/assets/puzzles.csv"

if not os.path.exists(zst_path):
    print(f"Error: {zst_path} not found.")
    exit(1)

# Step 1: Pass 1 to find the maximum possible puzzles for each theme in the DB
print("Pass 1: Counting total theme frequencies in database...")
theme_totals = Counter()
dctx = zstd.ZstdDecompressor()

with open(zst_path, 'rb') as fh:
    with dctx.stream_reader(fh) as reader:
        # Wrap in text stream
        import io
        text_stream = io.TextIOWrapper(reader, encoding='utf-8')
        
        # Read header
        header_line = text_stream.readline()
        header = header_line.strip().split(",")
        try:
            themes_idx = header.index("Themes")
        except ValueError:
            themes_idx = 7
            
        for line in text_stream:
            parts = line.strip().split(",")
            if len(parts) > themes_idx:
                themes = parts[themes_idx].split(" ")
                for t in themes:
                    if t:
                        theme_totals[t] += 1

print(f"Discovered {len(theme_totals)} distinct themes.")
# Target: At least 200 puzzles for each theme, or the maximum available if < 200
targets = {theme: min(200, total) for theme, total in theme_totals.items()}

# Step 2: Pass 2 to select the minimal set of puzzles
print("Pass 2: Extracting puzzles to satisfy theme targets...")
selected_puzzles = []
selected_counts = Counter()

with open(zst_path, 'rb') as fh:
    with dctx.stream_reader(fh) as reader:
        import io
        text_stream = io.TextIOWrapper(reader, encoding='utf-8')
        
        header_line = text_stream.readline()
        
        for line in text_stream:
            parts = line.strip().split(",")
            if len(parts) > themes_idx:
                themes = [t for t in parts[themes_idx].split(" ") if t]
                
                # Check if this puzzle helps satisfy any target
                needs_this = False
                for t in themes:
                    if selected_counts[t] < targets[t]:
                        needs_this = True
                        break
                        
                if needs_this:
                    selected_puzzles.append(line.strip())
                    for t in themes:
                        selected_counts[t] += 1
            
            # Optional: check if all targets are fully satisfied to stop early
            all_satisfied = True
            for t, target in targets.items():
                if selected_counts[t] < target:
                    all_satisfied = False
                    break
            if all_satisfied:
                print("All theme targets successfully satisfied early!")
                break

print(f"Selected {len(selected_puzzles)} puzzles in total.")
print("Verifying theme coverage:")
unsatisfied = 0
for t, target in targets.items():
    actual = selected_counts[t]
    if actual < target:
        print(f"  Warning: Theme '{t}' has only {actual}/{target}")
        unsatisfied += 1

if unsatisfied == 0:
    print("All theme targets fully met!")
else:
    print(f"{unsatisfied} themes could not meet their targets.")

# Write to puzzles.csv
os.makedirs(os.path.dirname(out_path), exist_ok=True)
with open(out_path, "w", encoding="utf-8") as f:
    f.write(header_line)  # Write header
    for p in selected_puzzles:
        f.write(p + "\n")

print(f"Saved to {out_path} ({os.path.getsize(out_path)} bytes).")
