from pathlib import Path
import subprocess, tempfile, sys, re
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(src/'MainActivity.java').read_text(encoding='utf-8')
orc=(src/'MasterOrchestrator.java').read_text(encoding='utf-8')
proto=(src/'MasterProtocolV1.java').read_text(encoding='utf-8')
director=(src/'BraseiroNarrativeDirector.java').read_text(encoding='utf-8')
checks={
 'director integrado ao prompt':'BraseiroNarrativeDirector.contract' in main and 'BraseiroNarrativeDirector.interactionContract' in main,
 'QA local antes da resposta':'BraseiroNarrativeDirector.qualityProblem' in main,
 'freeze antes da validação':'candidate=BraseiroNarrativeDirector.freezeEnvelope' in orc,
 'mundo não prevê tick em consulta':'narrative&&!nonAdvancing?world.preview(action):new JSONObject()' in orc,
 'NPC offscreen não avança em consulta':'narrative&&!nonAdvancing?npcAgency.previewOffscreenTick(tick):new JSONObject()' in orc,
 'protocolo aceita narrative_state':'"narrative_state"' in proto,
 'pistas no envelope':'\\"clues\\":[]' in main,
 'memória no backup portátil':'session_preferences_json' in main and 'braseiro_narrative_memory' in main,
 'histórico anti-repetição portátil':'braseiro_recent_narrations' in main,
 'TTS recebe delivery':'narrativeSpeechRate()' in main and 'applyNativeNarrativeDelivery()' in main,
 'TTS recebe pausa':'narrativePauseMs' in main,
 '24 funções de cena':'FUNÇÃO DA CENA' in director and 'aftermath' in director and 'encerramento' in director,
 'camadas de informação':'PERCEPTÍVEL' in director and 'OBSERVÁVEL' in director and 'INFERÍVEL' in director and 'OCULTO' in director,
 'planejador de batidas':'PLANEJADOR DE BATIDAS' in director,
 'temperatura 0 a 5':'TEMPERATURA NARRATIVA' in director,
 'subtexto de NPC':'SABE, ACREDITA, SUSPEITA, OUVIU, INVENTOU, ESCONDE ou RECUSA' in director,
 'três microexemplos no contrato':'TRÊS MICROEXEMPLOS DE COMPORTAMENTO' in director and 'CHEGADA:' in director and 'INVESTIGAÇÃO/DIÁLOGO:' in director and 'AÇÃO:' in director,
 'revisão única só de prosa':'BRAS_NARRATIVE_EDITOR' in main and 'repairAttempt<1' in main and 'stableEnvelope' in main,
 'diagnóstico de revisão permitido no protocolo':'narrative_quality_repair' in proto,
}
for name,value in checks.items():
 print(('OK  ' if value else 'FALHA ')+name)
if not all(checks.values()): sys.exit(1)
with tempfile.TemporaryDirectory(prefix='braseiro-narrative-test-') as td:
 out=Path(td)/'out'; out.mkdir()
 sources=[
   ROOT/'tools/runtime_json/org/json/JSONException.java',
   ROOT/'tools/runtime_json/org/json/JSONArray.java',
   ROOT/'tools/runtime_json/org/json/JSONObject.java',
   ROOT/'tools/appdb_semantic_stubs/com/braseiro/pathfinder2e/AppLog.java',
   src/'LiveTableNarrativeDirector.java',
   src/'BraseiroNarrativeDirector.java',
   ROOT/'ferramentas/NarrativeDirectorSelfTest.java',
 ]
 cmd=['javac','-encoding','UTF-8','-d',str(out),*[str(x) for x in sources]]
 subprocess.run(cmd,check=True,cwd=ROOT)
 subprocess.run(['java','-cp',str(out),'com.braseiro.dnd.NarrativeDirectorSelfTest'],check=True,cwd=ROOT)
print(f'RESULTADO ESTÁTICO 6.0.2: {len(checks)}/{len(checks)}')
