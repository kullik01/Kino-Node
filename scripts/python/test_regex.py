import re

pattern = re.compile(r"([\d\.,]+)(?:\s*/\s*100?|%)?")
for s in ["8.8/10", "87%", "74/100", "88", "4.9"]:
    m = pattern.search(s)
    if m:
        print(f"String: {s}, group(0): {m.group(0)}, group(1): {m.group(1)}")
