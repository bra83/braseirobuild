from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"


def test_final_router_covers_every_adventure_guide_branch():
    router = (SRC / "KingmakerSoloFlowRouter.java").read_text(encoding="utf-8")
    main = (SRC / "MainActivity.java").read_text(encoding="utf-8")
    for token in (
        "adventure_start", "camp_encounter", "camp", "war",
        "exploration_resolution", "companion", "kingdom_bridge",
        "travel", "free_exploration", "MASTER_FALLBACK",
    ):
        assert token in router
    assert "continueKingmakerCampaign()" in main
    assert "KingmakerSoloFlowRouter.actionFor" in main


def test_final_checkpoint_contract_is_wired_into_transactional_restore():
    contract = (SRC / "KingmakerContinuityContract.java").read_text(encoding="utf-8")
    db = (SRC / "AppDb.java").read_text(encoding="utf-8")
    director = (SRC / "KingmakerSoloDirector.java").read_text(encoding="utf-8")
    for token in ("stable", "compare", "ignored_fields", "turn_id"):
        assert token in contract
    assert "kingmakerContinuityCheckpoint" in db
    assert "KingmakerContinuityContract.compare" in db
    assert '"continuity_checkpoint"' in director


def test_final_harness_preserves_long_window_and_does_not_replace_r7():
    final_harness = (ROOT / "tests_pf2/java/KingmakerSoloCampaignFinalHarness.java").read_text(encoding="utf-8")
    final_script = (ROOT / "tools/test_kingmaker_solo_campaign_final.sh").read_text(encoding="utf-8")
    r7_script = (ROOT / "tools/test_kingmaker_solo_campaign_r7.sh").read_text(encoding="utf-8")
    assert "weeks=72" in final_harness
    assert "KingmakerSoloCampaignFinalHarness" in final_script
    assert "KingmakerSoloCampaignR7Harness" in r7_script
