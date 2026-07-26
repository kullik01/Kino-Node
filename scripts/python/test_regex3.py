import re

pattern = re.compile(r"([\d\.,]+)(?:\s*/\s*([\d\.,]+)|%)?")
for s in ["8.8/10", "4/5", "87%"]:
    m = pattern.search(s)
    if m:
        print(f"String: {s}, group(1): {m.group(1)}, group(2): {m.group(2)}")
    else:
        print(f"String: {s} NOT FOUND")
