#!/usr/bin/env python3
import argparse
import hashlib
import json
import struct
import sys
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"

def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Braseiro OSE Visual Lock pinned PNG assets.")
    parser.add_argument("--assets-root", default="src/assets")
    parser.add_argument("--pin", default=None)
    args = parser.parse_args()

    root = Path(args.assets_root)
    pin_path = Path(args.pin) if args.pin else root / "VISUAL_LOCK_BINARY_PIN.json"
    pin = json.loads(pin_path.read_text(encoding="utf-8"))

    missing = 0
    invalid_sha = 0
    invalid_png = 0
    invalid_dimensions = 0

    for entry in pin["files"]:
        path = root / entry["filename"]
        if not path.is_file() or path.stat().st_size <= 0:
            missing += 1
            print(f"MISSING={entry['filename']}")
            continue

        data = path.read_bytes()
        sha = hashlib.sha256(data).hexdigest()
        if sha != entry["sha256"]:
            invalid_sha += 1
            print(f"INVALID_SHA_FILE={entry['filename']} got={sha} expected={entry['sha256']}")

        png_ok = len(data) >= 24 and data[:8] == PNG_SIGNATURE and data[12:16] == b"IHDR"
        if not png_ok:
            invalid_png += 1
            print(f"INVALID_PNG_FILE={entry['filename']}")
            continue

        width, height = struct.unpack(">II", data[16:24])
        expected = (entry["width"], entry["height"])
        if (width, height) != expected:
            invalid_dimensions += 1
            print(
                f"INVALID_DIMENSIONS_FILE={entry['filename']} "
                f"got={width}x{height} expected={expected[0]}x{expected[1]}"
            )

    print(f"PINNED_FILES={len(pin['files'])}")
    print(f"MISSING_PINNED_ASSETS={missing}")
    print(f"INVALID_SHA={invalid_sha}")
    print(f"INVALID_PNG={invalid_png}")
    print(f"INVALID_DIMENSIONS={invalid_dimensions}")

    errors = missing + invalid_sha + invalid_png + invalid_dimensions
    if errors == 0:
        print("ALL_IMAGES_DECODE=PASS")
        print("BINARY_ASSET_INTEGRITY=PASS")
        return 0

    print("ALL_IMAGES_DECODE=FAIL")
    print("BINARY_ASSET_INTEGRITY=FAIL")
    return 1

if __name__ == "__main__":
    sys.exit(main())
