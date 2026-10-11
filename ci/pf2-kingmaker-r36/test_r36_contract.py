from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"

def test_official_kingdom_action_contract():
    source=SOURCE.read_text(encoding="utf-8")
    assert "taxesCircumstanceBonus" in source
    assert "claimHexStabilityPenalty" in source
    assert "resolveQuellUnrest" in source
