#!/usr/bin/env python3
"""Download canonical public Drive RUN230; verify nine part hashes and ZIP before extraction."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

FOLDER = "https://drive.google.com/drive/folders/1T7ZD9lNBiqHjRKaRrOO1_1sZoIyke_N5"

def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as inp:
        for block in iter(lambda: inp.read(2 * 1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--local-parts", default="")
    args = parser.parse_args()
    out = Path(args.output).resolve()
    incoming = out / "incoming"
    incoming.mkdir(parents=True, exist_ok=True)
    proof = out / "out"
    proof.mkdir(exist_ok=True)
    meta = json.loads(Path(__file__).with_name("pinned-source.json").read_text("utf-8"))
    if not args.local_parts:
        import gdown
        files = gdown.download_folder(url=FOLDER, output=str(incoming), quiet=False, remaining_ok=True)
        if not files:
            raise RuntimeError("RUN230_PUBLIC_DRIVE_DOWNLOAD_FAILED")
        where = incoming
    else:
        where = Path(args.local_parts).resolve()
    parts = meta["parts"]
    if len(parts) != 9 or [x["index"] for x in parts] != list(range(9)):
        raise RuntimeError("RUN230_PART_MANIFEST_INVALID")
    archive_path = out / meta["name"]
    with archive_path.open("wb") as writer:
        for part in parts:
            found = list(where.rglob(part["name"]))
            if len(found) != 1:
                raise RuntimeError("RUN230_PART_MISSING_OR_DUPLICATE_" + str(part["index"]))
            p = found[0]
            if p.stat().st_size != part["size"] or digest(p) != part["sha256"]:
                raise RuntimeError("RUN230_PART_SHA256_MISMATCH_" + str(part["index"]))
            print("RUN230_PART_" + str(part["index"]) + "_SHA256=PASS", flush=True)
            with p.open("rb") as source:
                shutil.copyfileobj(source, writer, 4 * 1024 * 1024)
    got = digest(archive_path)
    if archive_path.stat().st_size != meta["total_size"] or got != meta["zip_sha256"]:
        raise RuntimeError("RUN230_FULL_SOURCE_SHA256_MISMATCH: " + got)
    project = out / "source"
    with zipfile.ZipFile(archive_path) as z:
        if len(z.infolist()) < 1500:
            raise RuntimeError("RUN230_SOURCE_ZIP_INCOMPLETE")
        if z.testzip() is not None:
            raise RuntimeError("RUN230_SOURCE_ZIP_CRC_ERROR")
        z.extractall(project)
    version = (project / "version.properties").read_text("utf-8")
    for expected in ("VERSION_CODE=606530", "DB_SCHEMA=61", "BACKUP_FORMAT=21"):
        if expected not in version.splitlines():
            raise RuntimeError("RUN230_VERSION_CONTRACT_FAILED_" + expected)
    app = (project / "android" / "app" / "build.gradle.kts").read_text("utf-8")
    if 'applicationId = "com.braseiro.dnd5e"' not in app or "compileSdk = 35" not in app:
        raise RuntimeError("RUN230_APPLICATION_OR_SDK_MISMATCH")
    if not (project / "android" / "gradlew").is_file():
        raise RuntimeError("RUN230_WRAPPER_MISSING")
    (proof / "SOURCE_PROOF.txt").write_text(
        "RUN230_SHA256=" + got + "\nNINE_PARTS_SHA256=PASS\nZIP_CRC=PASS\n"
        "ANDROID_COMPILE=NOT_RUN\nDEVICE_TEST=NOT_RUN\nG05_COMPLETE=NO\n",
        encoding="utf-8"
    )
    print("RUN230_FULL_SOURCE_SHA256=PASS: " + got)

if __name__ == "__main__":
    main()
