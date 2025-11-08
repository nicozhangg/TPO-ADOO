import sys
import PyPDF2
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

path = Path("TPOFIN~1.PDF")
reader = PyPDF2.PdfReader(path.open("rb"))
for i, page in enumerate(reader.pages, 1):
    text = page.extract_text() or ""
    print(f"--- Page {i} ---")
    print(text)
    print()
