#!/usr/bin/env python3
"""Fetch and verify the exact recovered RUN245 source archive for BRELGA CI."""
import argparse
import hashlib
import pathlib
import sys
import zipfile
import gdown

PARTS = [
    ("14qAFvO13zA_Qa99aQ_oSLtMWEr0c2L2S", 31457280),
    ("1u_5HSsz6stWYu5DEVKLMk7gC4Uwowt2B", 31457280),
    ("17SUKYVjT136oWYfH-5Tzs66U10nw-Arf", 31457280),
    ("1f8M5YGKAwJr8WcvdLo6_tJx_WfyUCgT4", 31457280),
    ("1MKAMtn2Gn1BGAzadERocXuv5j5ihNlaS", 14488681),
]
EXPECTED_SIZE = 140317801
EXPECTED_SHA256 = "7f69f02e5b77a100cfd517b80c4de96c10d0b33cf3284d78112ebd3755e0222b"
EXPECTED_ENTRIES = 1213


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    root = pathlib.Path(args.output)
    parts_dir = root / "parts"
    project = root / "project"
    parts_dir.mkdir(parents=True, exist_ok=True)
    project.mkdir(parents=True, exist_ok=True)
    archive = root / "BRASEIRO_DND_NORMAL_RUN245_G05_SOURCE_WIP.zip"

    for index, (file_id, expected_size) in enumerate(PARTS):
        part = parts_dir / f"part{index:02d}"
        result = gdown.download(id=file_id, output=str(part), quiet=False)
        if result is None or not part.is_file():
            raise RuntimeError(f"DRIVE_PART_{index:02d}_DOWNLOAD_FAILED")
        actual_size = part.stat().st_size
        if actual_size != expected_size:
            raise RuntimeError(
                f"DRIVE_PART_{index:02d}_SIZE_MISMATCH:{actual_size}!={expected_size}"
            )

    digest = hashlib.sha256()
    total = 0
    with archive.open("wb") as out:
        for index, _ in enumerate(PARTS):
            part = parts_dir / f"part{index:02d}"
            with part.open("rb") as src:
                while chunk := src.read(1024 * 1024):
                    out.write(chunk)
                    digest.update(chunk)
                    total += len(chunk)

    if total != EXPECTED_SIZE:
        raise RuntimeError(f"ARCHIVE_SIZE_MISMATCH:{total}!={EXPECTED_SIZE}")
    if digest.hexdigest() != EXPECTED_SHA256:
        raise RuntimeError("ARCHIVE_SHA256_MISMATCH")

    with zipfile.ZipFile(archive) as zf:
        if len(zf.infolist()) != EXPECTED_ENTRIES:
            raise RuntimeError(f"ZIP_ENTRY_COUNT_MISMATCH:{len(zf.infolist())}")
        bad = zf.testzip()
        if bad is not None:
            raise RuntimeError(f"ZIP_CRC_FAILURE:{bad}")
        zf.extractall(project)

    props = project / "version.properties"
    if not props.is_file():
        raise RuntimeError("RUN245_VERSION_PROPERTIES_MISSING")
    parsed = {}
    for line in props.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            parsed[key.strip()] = value.strip()
    if parsed.get("VERSION_CODE") != "606545":
        raise RuntimeError("RUN245_VERSION_CODE_MISMATCH")
    app_gradle = project / "app" / "build.gradle.kts"
    if not app_gradle.is_file() or "com.braseiro.dnd5e" not in app_gradle.read_text(encoding="utf-8"):
        raise RuntimeError("RUN245_APPLICATION_ID_MISSING")
    if not (project / "gradlew.bat").is_file():
        raise RuntimeError("RUN245_GRADLE_WRAPPER_MISSING")

    (root / "SOURCE_VERIFY.txt").write_text(
        f"RUN245_SOURCE=PASS\n"
        f"ARCHIVE_SHA256={EXPECTED_SHA256}\n"
        f"ARCHIVE_BYTES={EXPECTED_SIZE}\n"
        f"ZIP_ENTRIES={EXPECTED_ENTRIES}\n"
        f"APPLICATION_ID=com.braseiro.dnd5e\n"
        f"VERSION_CODE={parsed['VERSION_CODE']}\n",
        encoding="utf-8",
    )
    print((root / "SOURCE_VERIFY.txt").read_text(encoding="utf-8"), end="")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"RUN245_SOURCE_FETCH_FAILED={exc}", file=sys.stderr)
        raise
