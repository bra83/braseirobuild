"""Kingmaker R36 source-contract checks against the extracted Android project."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"

def test_r36_official_outcomes_are_persisted():
    text=SRC.read_text(encoding="utf-8")
    assert "taxesCircumstanceBonus" in text
    assert '"taxes_circumstance"' in text
    assert "critical_success_d6" in text
    assert "critical_failure_d4" in text
    assert "stability_penalty_through_turn" in text

def test_r36_no_legacy_tax_resource_grants():
    text=SRC.read_text(encoding="utf-8")
    section=text.split("public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes)")[1].split("\n    public ")[0]
    assert 'put("resource_points", 2)' not in section
    assert 'put("resource_points", 1)' not in section

def test_r36_version():
    assert "VERSION_NAME=9.0.37" in (ROOT/"version.properties").read_text(encoding="utf-8")
