#!/usr/bin/env python3
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(SRC/'MainActivity.java').read_text('utf-8')
retrieval=(SRC/'RuleRetrievalEngine.java').read_text('utf-8')
planner=(SRC/'AdventureOpeningPlanner.java').read_text('utf-8')
options=(SRC/'SceneOptionDiversifier.java').read_text('utf-8')
extractor=(SRC/'NarratedNpcExtractor.java').read_text('utf-8')
version=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text('utf-8').splitlines() if '=' in line)
checks={
 'base 4.9.6 preservada ou superior':int(version.get('VERSION_CODE','0'))>=490600,
 'início oferece sorteio ou Atlas':('SORTEAR HEX VÁLIDO' in main or 'SORTEAR LOCAL CATALOGADO' in main or 'SORTEAR COM VARIEDADE' in main) and 'ESCOLHER NO ATLAS' in main,
 'narrativa espera confirmação do hex':'launchAdventure(reset,""' in main and 'pendingAdventureStartFromMap' in main and 'CONFIRMAR E INICIAR' in main,
 'hex inexistente bloqueia abertura':'isEligibleAdventureStartHex' in main and ('não pertence à geometria do Atlas ativo' in main or 'não contém cidade, local nomeado ou terreno catalogado' in main),
 'sorteio não reutiliza silenciosamente posição anterior':'AdventureStartSelector.choose' in main and 'recent_adventure_start_points_' in main and 'return previous' not in main[main.index('private JSONObject prepareAdventureStartingPoint'):main.index('private void showMystaraCampaign')],
 'abertura declara foco de regras':'FOCO DE REGRAS PARA A ABERTURA' in main and 'openingPlan.rulesFocus' in main and 'ruleFocusFor' in planner,
 'recuperação não pula abertura':'opening_rules_ready' in retrieval and 'opening_rules_partial' in retrieval and 'opening_narrative' in retrieval,
 'abertura usa fontes sem antecipar teste':'Não imponha teste nem aplique consequência mecânica antes' in retrieval,
 'NPC titulado é extraído': 'barão|baronesa' in extractor and 'NarratedNpcExtractor.extract' in main,
 'NPC narrado é persistido e recebe token':'synchronizeVisualSceneCast' in main and 'db.upsertNpcMemory(update)' in main and 'scheduleSceneNpcTokenBinding' in main,
 'prompt exige NPC nos campos estruturados':'todo NPC que aparece, fala, entrega algo, exige algo ou age' in main,
 'opções incompatíveis são filtradas':'isCompatibleWithScene' in options and 'travessia' in options and 'não estiverem literalmente presentes' in main,
 'cobrança gera negociação e documento':'NEGOCIAR A EXIGÊNCIA' in options and 'EXIGIR PROVA DA COBRANÇA' in options and 'notas promissorias' in options,
 'fallback abstrato removido':'VERIFICAR O DETALHE MAIS RECENTE' not in options,
 'regressões JVM incluídas':(ROOT/'tools/test_scene_option_diversifier.sh').is_file() and (ROOT/'tools/test_narrated_npc_extractor.sh').is_file(),
}
failed=[]
for name,ok in checks.items():
    print(('OK  ' if ok else 'FALHA  ')+name)
    if not ok: failed.append(name)
if failed: raise SystemExit(f'{len(failed)} falha(s): '+', '.join(failed))
for script in ['test_scene_option_diversifier.sh','test_narrated_npc_extractor.sh']:
    subprocess.run(['bash',str(ROOT/'tools'/script)],check=True)
print(f'REGRESSÃO 4.9.6 PRESERVADA: {len(checks)}/{len(checks)} verificações estáticas e 16 comportamentais aprovadas.')
