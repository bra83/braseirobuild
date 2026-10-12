#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(SRC/'MainActivity.java').read_text('utf-8')
opts=(SRC/'SceneOptionDiversifier.java').read_text('utf-8')
version=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text('utf-8').splitlines() if '=' in line)
checks={
 'base 4.9.5 ou posterior':tuple(map(int,version.get('VERSION_NAME','0.0.0').split('.'))) >= (4,9,5) and int(version.get('VERSION_CODE','0'))>=490500,
 'estado novo normaliza NPCs':'normalizeProjectedSceneArray(projected,proposed,"present_npcs","present_npcs_json")' in main,
 'estado novo normaliza ameaças':'normalizeProjectedSceneArray(projected,proposed,"threats","threats_json")' in main,
 'estado novo normaliza pistas':'normalizeProjectedSceneArray(projected,proposed,"clues","clues_json")' in main,
 'histórico evita bloco duplicado':'tailMatches(values, additions)' in opts,
 'aliases de cena são lidos':'scene.has(alias)?scene.opt(alias):scene.opt(key)' in opts,
 'objetivo orienta fallback':'AVANÇAR EM ' in opts,
 'perigo orienta fallback':'REDUZIR O RISCO DE ' in opts,
 'sem fallback abstrato':'VERIFICAR O DETALHE MAIS RECENTE' not in opts and 'é melhor\n        // deixar o campo de ação livre' in opts,
 'teste JVM incluído':(ROOT/'tools/test_scene_option_diversifier.sh').is_file() and (ROOT/'ferramentas/SceneOptionDiversifierSelfTest.java').is_file(),
}
failed=[]
for name,ok in checks.items():
 print(('OK  ' if ok else 'FALHA  ')+name)
 if not ok: failed.append(name)
if failed: raise SystemExit(f'{len(failed)} falha(s): '+', '.join(failed))
print(f'RESULTADO 4.9.5+: {len(checks)}/{len(checks)} verificações aprovadas.')
