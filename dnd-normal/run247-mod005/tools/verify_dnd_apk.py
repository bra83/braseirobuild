#!/usr/bin/env python3
"""P4 Braseiro D&D: prove the *built APK* embeds the unmodified real Faerûn atlas.

This is NOT an Android-on-device or comprehensive editorial certification.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile

ASSET_NAME = "PACOTE_MAPA_FAERUN_2025_PTBR_MAX_V2.zip"
APK_PATH = "assets/maps/" + ASSET_NAME
CANONICAL_BYTES = 102_373_336


def sha256_stream(fileobj):
    digest = hashlib.sha256()
    count = 0
    while True:
        chunk = fileobj.read(1024 * 1024)
        if not chunk:
            break
        count += len(chunk)
        digest.update(chunk)
    return digest.hexdigest(), count


def verify(apk: Path, project: Path) -> dict:
    source_asset = project / "app/src/main/assets/maps" / ASSET_NAME
    if not apk.is_file():
        raise ValueError("DND_APK_MISSING: " + str(apk))
    if not source_asset.is_file():
        raise ValueError("FAERUN_CANONICAL_SOURCE_ASSET_MISSING")
    source_size = source_asset.stat().st_size
    if source_size != CANONICAL_BYTES:
        raise ValueError(f"FAERUN_SOURCE_SIZE_MISMATCH:{source_size}!={CANONICAL_BYTES}")
    with source_asset.open("rb") as stream:
        source_sha, bytes_read = sha256_stream(stream)
    if bytes_read != CANONICAL_BYTES:
        raise ValueError("FAERUN_SOURCE_READ_TRUNCATED")

    with zipfile.ZipFile(apk) as package:
        all_entries = package.namelist()
        matching = [n for n in all_entries if n == APK_PATH]
        if len(matching) != 1:
            raise ValueError(f"FAERUN_APK_ENTRY_COUNT:{len(matching)}")
        entry = package.getinfo(APK_PATH)
        if entry.file_size != CANONICAL_BYTES:
            raise ValueError(f"FAERUN_EMBEDDED_SIZE_MISMATCH:{entry.file_size}")
        # A nested ZIP must not be destroyed by Gradle's QA lite mode.
        with package.open(entry, "r") as source:
            packaged_sha, packaged_count = sha256_stream(source)
        if packaged_count != CANONICAL_BYTES or packaged_sha != source_sha:
            raise ValueError("FAERUN_EMBEDDED_HASH_MISMATCH")
        with package.open(entry, "r") as source:
            if source.read(4) != b"PK\x03\x04":
                raise ValueError("FAERUN_EMBEDDED_NOT_A_REAL_ZIP")
        # Uniqueness protects against an alternate or test-only map overriding V2.
        other_atlases = [name for name in all_entries
                         if name.startswith("assets/maps/PACOTE_MAPA_FAERUN")
                         and name != APK_PATH]
        if other_atlases:
            raise ValueError("UNEXPECTED_OTHER_FAERUN_ASSETS:" + ",".join(other_atlases[:5]))

    with apk.open("rb") as stream:
        apk_sha, apk_bytes = sha256_stream(stream)
    if apk_bytes != apk.stat().st_size:
        raise ValueError("APK_SHA256_READ_TRUNCATED")
    return {
        "format": "BRASEIRO_DND_P4_APK_EVIDENCE_V1",
        "status": "APK_ATLAS_BYTES_VERIFIED",
        "apk": apk.name,
        "apk_bytes": apk_bytes,
        "apk_sha256": apk_sha,
        "map_entry": APK_PATH,
        "faerun_zip_bytes": CANONICAL_BYTES,
        "faerun_sha256": source_sha,
        "mapped_in_apk": True,
        "apk_signed": "verify with apksigner separately",
        "physical_device": "NOT_TESTED",
        "editorial_integral": "NOT_CERTIFIED",
        "oauth_cloud_config": "NOT_VERIFIED",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--project", type=Path, default=Path("."))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        report = verify(args.apk.resolve(), args.project.resolve())
    except (OSError, ValueError, zipfile.BadZipFile, RuntimeError) as error:
        print("DND_P4_APK_CHECK_FAILED=" + str(error), file=sys.stderr)
        return 1
    serialized = json.dumps(report, ensure_ascii=False, indent=2)
    print(serialized)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(serialized + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
