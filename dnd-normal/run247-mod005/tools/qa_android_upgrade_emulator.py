#!/usr/bin/env python3
"""Physical-Android-like emulator upgrade probe; no user data, no Gemini token.
Installs the older QA APK, creates a DB-backed save canary, installs the new
APK in place, verifies SQLite state and application-private marker survive.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import tarfile
import time

PKG = "com.braseiro.dnd5e"
DB = "braseiro_dnd.db"
LOCATION = "QA_UPGRADE_PERSISTENCE_20261114"
FILE_TOKEN = "BRASEIRO_UPGRADE_CANARY_DO_NOT_DELETE"


def adb(*parts, input_bytes=None, timeout=150):
    command = ["adb", *parts]
    result = subprocess.run(command, input=input_bytes, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=timeout)
    if result.returncode:
        raise RuntimeError(f"adb failed {command}: {result.stderr.decode(errors='replace')[-1200:]}")
    return result.stdout


def app_start():
    adb("shell", "am", "start", "-W", "-n", PKG + "/.MainActivity", timeout=150)
    # Do not assume app UI initialized just because ActivityManager returned.
    for _ in range(90):
        check = subprocess.run(["adb", "shell", "run-as", PKG,
                                "test", "-s", "databases/" + DB],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if check.returncode == 0:
            time.sleep(8)
            return
        time.sleep(2)
    raise RuntimeError("App did not create the expected SQLite database after launch")


def stop():
    adb("shell", "am", "force-stop", PKG)
    time.sleep(2)


def database_snapshot(target_dir):
    target_dir.mkdir(exist_ok=True, parents=True)
    raw = adb("exec-out", "run-as", PKG, "sh", "-c",
              "tar -cf - databases", timeout=120)
    with tarfile.open(fileobj=io.BytesIO(raw), mode="r:") as archive:
        for member in archive.getmembers():
            if not member.isfile() or Path(member.name).name not in (
                DB, DB + "-wal", DB + "-shm"):
                continue
            stream = archive.extractfile(member)
            if stream:
                (target_dir / Path(member.name).name).write_bytes(stream.read())
    dbfile = target_dir / DB
    if not dbfile.is_file() or dbfile.stat().st_size < 4096:
        raise RuntimeError("SQLite snapshot missing or too small")
    return dbfile


def inspect(dbfile):
    with sqlite3.connect(str(dbfile)) as connection:
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        schema_version = connection.execute("PRAGMA user_version").fetchone()[0]
        tables = {x[0] for x in connection.execute(
            "SELECT name FROM sqlite_master WHERE type='table'")}
        if integrity != "ok" or "scene_state_profiles" not in tables:
            raise RuntimeError(f"Corrupt or missing canonical DB: {integrity}, {tables}")
        row = connection.execute(
            "SELECT location,day_number,clock_hour FROM scene_state_profiles "
            "WHERE world_profile='dnd'").fetchone()
        characters = connection.execute("SELECT COUNT(*) FROM characters").fetchone()[0] \
            if "characters" in tables else None
        return dict(integrity=integrity, schema_version=schema_version,
                    table_count=len(tables), scene=row, characters=characters)


def insert_canary(dbfile):
    with sqlite3.connect(str(dbfile)) as connection:
        connection.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        row = connection.execute(
            "SELECT COUNT(*) FROM scene_state_profiles WHERE world_profile='dnd'").fetchone()[0]
        if row != 1:
            raise RuntimeError("Expected D&D scene row is missing")
        connection.execute(
            "UPDATE scene_state_profiles SET location=?,day_number=?,clock_hour=? "
            "WHERE world_profile='dnd'", (LOCATION, 17, 18.75))
        connection.commit()
        connection.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    assert inspect(dbfile)["scene"] == (LOCATION, 17, 18.75)


def restore_synthetic_db(dbfile):
    adb("shell", "run-as", PKG, "sh", "-c",
        "rm -f databases/" + DB + "-wal databases/" + DB + "-shm")
    adb("shell", "-T", "run-as", PKG, "sh", "-c",
        "cat > databases/" + DB, input_bytes=dbfile.read_bytes(), timeout=180)


def log_and_assert(path, payload):
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous", required=True)
    parser.add_argument("--current", required=True)
    parser.add_argument("--evidence", default="qa-emulator-evidence")
    args = parser.parse_args()
    old, new = Path(args.previous), Path(args.current)
    evidence = Path(args.evidence)
    evidence.mkdir(parents=True, exist_ok=True)
    for item in (old, new):
        if not item.is_file() or item.stat().st_size < 100_000_000:
            raise RuntimeError(f"Missing full-asset APK: {item}")
    adb("wait-for-device", timeout=240)
    adb("install", "-r", str(old), timeout=240)
    app_start()
    stop()
    pre = database_snapshot(evidence / "before")
    before = inspect(pre)
    insert_canary(pre)
    before_with_canary = inspect(pre)
    restore_synthetic_db(pre)
    adb("shell", "run-as", PKG, "sh", "-c",
        "mkdir -p files && printf '" + FILE_TOKEN + "' > files/qa_upgrade_canary.txt")
    # Upgrade in place: NO uninstall and NO -d downgrade flag.
    adb("install", "-r", str(new), timeout=240)
    app_start()
    stop()
    after_db = database_snapshot(evidence / "after")
    after = inspect(after_db)
    marker = adb("exec-out", "run-as", PKG, "cat",
                 "files/qa_upgrade_canary.txt").decode(errors="replace").strip()
    passed = (after["scene"] == (LOCATION, 17, 18.75)
              and before_with_canary["characters"] == after["characters"]
              and marker == FILE_TOKEN
              and after["schema_version"] >= before["schema_version"])
    summary = {
        "workflow": "ANDROID_EMULATOR_UPGRADE_QA_V1",
        "source_apks": {
            "previous_sha256": hashlib.sha256(old.read_bytes()).hexdigest(),
            "current_sha256": hashlib.sha256(new.read_bytes()).hexdigest()},
        "old_original": before,
        "old_with_synthetic_save": before_with_canary,
        "new_after_upgrade": after,
        "private_file_canary_preserved": marker == FILE_TOKEN,
        "upgrade_command": "adb install -r (without uninstall)",
        "passed": passed,
        "physical_galaxy_a57_certified": False,
        "gemini_real_certified": False,
    }
    log_and_assert(evidence / "result.json", summary)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    if not passed:
        raise RuntimeError("ANDROID_UPGRADE_SAVE_INTEGRITY_FAILED")


if __name__ == "__main__":
    main()
