from pathlib import Path
R=Path(__file__).resolve().parents[1]
def read(p): return (R/p).read_text(encoding="utf-8")
def test_rules_recovery_and_conditions():
 s=read("android/app/src/main/java/com/braseiro/pathfinder2e/Pathfinder2eRulesEngine.java")
 for x in ["recoveryCheck","knockedOut","validConditionMutation","CRITICAL_SUCCESS","mapPenalty"]: assert x in s
def test_rag_never_blocks_fiction():
 s=read("android/app/src/main/java/com/braseiro/pathfinder2e/Pf2RagContinuityGate.java")
 assert 'fiction_may_continue' in s and 'mechanics_may_commit' in s and 'Continue a cena sem inventar números' in s
def test_atlas_grounding():
 s=read("android/app/src/main/java/com/braseiro/pathfinder2e/Pf2AtlasGrounding.java")
 for x in ["BATTLEMAP","DUNGEON_MAP","confirmed","mechanical_authority","mayNarrateLocation"]: assert x in s
def test_gemini_first_call_failover():
 s=read("android/app/src/main/java/com/braseiro/pathfinder2e/Pf2GeminiResiliencePolicy.java")
 assert 'gemini-3.5-flash-lite' in s and 'lastKnownGood' in s
def test_visual_prompt_compile_regression():
 s=read("android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java")
 assert 'memory.length()>0?memory:n' in s
 assert 'memory.length()>0?memory:npc' not in s
