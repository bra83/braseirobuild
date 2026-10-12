from pathlib import Path
R=Path(__file__).resolve().parents[1]
def read(p): return (R/p).read_text(encoding='utf-8', errors='ignore')
def test_forbidden_maps_removed():
 a=R/'android/app/src/main/assets/maps'; names=' '.join(x.name.lower() for x in a.glob('*')); assert 'faerun' not in names and 'mystara' not in names
def test_pf2_rules():
 s=read('android/app/src/main/java/com/braseiro/pathfinder2e/Pathfinder2eRulesEngine.java');
 for x in ['CRITICAL_SUCCESS','mapPenalty','actions_max','dying','perception_bonus','nenhuma regra D&D/PF1']: assert x in s
def test_living_world_preserved():
 for f in ['LivingWorldEngine.java','LivingWorldSimulationEngine.java','LivingWorldCatchUpEngine.java','NpcAgencyEngine.java','WorldStateEngine.java']: assert (R/'android/app/src/main/java/com/braseiro/pathfinder2e'/f).exists()
def test_narrative_is_table_not_calculator():
 s=read('android/app/src/main/java/com/braseiro/pathfinder2e/PathfinderNarrativeContract.java'); assert 'Não responda como calculadora' in s and 'batidas curtas' in s and 'Mundo Vivo' in s
def test_token_prompt():
 s=read('android/app/src/main/java/com/braseiro/pathfinder2e/PathfinderTokenPromptBuilder.java'); assert 'Ancestralidade' in s and 'Equipamento visível' in s and 'Gemini' in s
def test_manifest():
 s=read('android/app/src/main/assets/pathfinder/system_manifest.json'); assert 'Pathfinder 2e Remaster' in s and 'multi-campaign' in s
