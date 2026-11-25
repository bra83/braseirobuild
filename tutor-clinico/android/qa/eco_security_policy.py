"""Fail closed on accidental changes to the ECO Android privacy policy."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main"
A = "{http://schemas.android.com/apk/res/android}"
DOMAINS = {"root", "file", "database", "sharedpref", "external",
           "device_root", "device_file", "device_database", "device_sharedpref"}


def require(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)


def validate_manifest(manifest):
    application = manifest.find("application")
    require(application is not None, "application missing")
    for key in ("allowBackup", "fullBackupContent", "usesCleartextTraffic"):
        require(application.get(A + key) == "false", key + " must be false")
    require(application.get(A + "dataExtractionRules") ==
            "@xml/eco_data_extraction_rules", "backup exclusions not linked")
    permissions = {node.get(A + "name") for node in manifest
                   if node.tag.startswith("uses-permission")}
    require("android.permission.INTERNET" not in permissions, "network permission enabled")
    exported = {(node.tag, node.get(A + "name")) for node in application
                if node.tag in {"activity", "activity-alias", "provider", "receiver", "service"}
                and node.get(A + "exported") == "true"}
    require(exported == {("activity", ".MainActivity")}, "unexpected exported component")
    for node in application:
        if node.tag in {"activity", "activity-alias", "provider", "receiver", "service"}:
            require(node.get(A + "exported") in {"true", "false"}, "missing explicit exported policy")


def validate_rules(rules):
    require(rules.tag == "data-extraction-rules", "incorrect rule root")
    require(len(rules) == 2 and {node.tag for node in rules} ==
            {"cloud-backup", "device-transfer"}, "missing or duplicate transfer policy")
    for mode in rules:
        require(len(mode) == len(DOMAINS), "unexpected backup rule count")
        require(all(node.tag == "exclude" and node.get("path") == "." for node in mode),
                "backup policy must exclude entire domains")
        require({node.get("domain") for node in mode} == DOMAINS,
                "incomplete backup exclusion: " + mode.tag)
    require(rules.find("cloud-backup").get("disableIfNoEncryptionCapabilities") == "true",
            "cloud encryption policy missing")


validate_manifest(ET.parse(ROOT / "AndroidManifest.xml").getroot())
validate_rules(ET.parse(ROOT / "res/xml/eco_data_extraction_rules.xml").getroot())
print("PASS: no automatic cloud/device transfer, no network permission, explicit component policy")
