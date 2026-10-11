from pathlib import Path
import sys
p=Path(sys.argv[1])/"version.properties"
s=p.read_text()
assert "VERSION_NAME=9.0.37" in s
print("Version verified")
