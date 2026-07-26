import re

pattern = re.compile(r"([\d\.,]+)(?:\s*/\s*100?|%)?")
for s in ["4/5", "3.5/4", "A+"]:
    m = pattern.search(s)
    if m:
        print(f"String: {s}, group(0): {m.group(0)}, group(1): {m.group(1)}")
    else:
        print(f"String: {s} NOT FOUND")
