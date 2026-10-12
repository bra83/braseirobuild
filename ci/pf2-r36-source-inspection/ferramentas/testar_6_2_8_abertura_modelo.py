from pathlib import Path
import sys, re
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
GEMINI=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java').read_text(encoding='utf-8')
checks=[]
def check(name, cond):
    checks.append((name,bool(cond)))

check('Narrador usa modelo de texto visível configurado', 'final String model=prefs.getString("text_model","gemini-3.5-flash-lite").trim();pendingAiModel=model;' in MAIN)
check('preferência oculta do Narrador não decide modelo', 'prefs.getString("narrator_text_model"' not in MAIN)
check('upgrade remove preferência oculta antiga', 'prefs.contains("narrator_text_model")' in MAIN and 'edit.remove("narrator_text_model")' in MAIN)
check('salvar configuração remove override oculto', '.putString("text_model",textModel.getText().toString().trim()).remove("narrator_text_model")' in MAIN)
check('modelo configurado permanece primeiro candidato', 'if(configuredClean.length()>0&&!isLegacyModel(configuredClean))ordered.add(configuredClean);' in GEMINI)
check('status explica fallback em vez de modelo misterioso', '(fallback; configurado: ' in MAIN and 'textModelStatus()' in MAIN)
check('abertura longa é requisito explícito do prompt', 'ABERTURA LONGA DO MESTRE — REQUISITO DE JOGO' in MAIN)
check('abertura longa exige 3 a 5 parágrafos', 'Escreva 3 a 5 parágrafos substanciais' in MAIN)
check('abertura longa tem faixa 360 a 700 palavras', 'normalmente entre 360 e 700 palavras' in MAIN)
check('abertura longa ocorre uma vez', 'A longa apresentação acontece UMA VEZ na abertura' in MAIN)
check('abertura não recebe ritmo curto comum', 'else if(opening){' in MAIN and 'append(narrationPaceInstruction())' in MAIN)
check('QA rejeita abertura de um parágrafo', 'if(paragraphs<3)return "a abertura veio em "+paragraphs+" parágrafo(s)' in MAIN)
check('QA rejeita abertura abaixo de 300 palavras', 'if(words<300)return "a abertura tem apenas "+words+" palavras' in MAIN)
check('contador de parágrafo usa quebras explícitas, não frases', 'clean.split("\\\\r?\\\\n\\\\s*")' in MAIN[MAIN.index('private int countNarrationParagraphs'):MAIN.index('private void streamNarrationPartial')] and 'sentences=' not in MAIN[MAIN.index('private int countNarrationParagraphs'):MAIN.index('private void streamNarrationPartial')])
check('reparo recebe alvo formal da abertura', 'ALVO DE FORMA: "+narrationQualityTarget(originalUserText)' in MAIN)
check('alvo formal também registra abertura longa', 'ABERTURA LONGA: 3 a 5 parágrafos substanciais, normalmente 360 a 700 palavras' in MAIN)

for name,ok in checks:
    print(('PASS' if ok else 'FAIL')+' - '+name)
if not all(ok for _,ok in checks):
    sys.exit(1)
print(f'OK - {sum(ok for _,ok in checks)}/{len(checks)} verificações 6.2.8.')
