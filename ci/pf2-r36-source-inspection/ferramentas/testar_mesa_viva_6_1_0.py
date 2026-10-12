from pathlib import Path
import subprocess,tempfile,sys,re
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(src/'MainActivity.java').read_text(encoding='utf-8')
master=(src/'MasterOrchestrator.java').read_text(encoding='utf-8')
base=(src/'BraseiroNarrativeDirector.java').read_text(encoding='utf-8')
live=(src/'LiveTableNarrativeDirector.java').read_text(encoding='utf-8')
version=(ROOT/'version.properties').read_text(encoding='utf-8')
checks={
 'classe Mesa Viva existe':'class LiveTableNarrativeDirector' in live,
 'contrato em prompt rapido':'LiveTableNarrativeDirector.contract' in main and main.count('LiveTableNarrativeDirector.contract')>=2,
 'QA Mesa Viva antes de aceitar':'LiveTableNarrativeDirector.qualityProblem' in main,
 'contexto operacional live_table':'root.put("live_table"' in master,
 'memoria gravada pelo diretor base':'LiveTableNarrativeDirector.record' in base,
 'memoria reentra no prompt':'LiveTableNarrativeDirector.memoryPrompt' in base,
 'JSON aceita live_table':'\\"live_table\\"' in main,
 'abertura de elenco':'ABERTURA DE ELENCO' in live,
 'corte paralelo':'SUBGRUPOS E CORTE PARALELO' in live,
 'sandbox faccoes':'MUNDO ABERTO' in live and 'Facções continuam agindo' in live,
 'aventura focada':'AVENTURA FOCADA' in live,
 'investigacao tatil':'INVESTIGAÇÃO TÁTIL' in live,
 'firewall dramatic irony':'DRAMATIC IRONY COM FIREWALL' in live,
 'humor e respiracao':'HUMOR E RESPIRAÇÃO' in live,
 'promessa e pagamento':'PROMESSA E PAGAMENTO' in live,
 'Mesa Viva 6.1.x preservada em 6.2.1':'VERSION_NAME=6.2.1' in version and 'VERSION_CODE=602100' in version,
}
for n,v in checks.items():print(('OK  ' if v else 'FALHA ')+n)
if not all(checks.values()):sys.exit(1)
with tempfile.TemporaryDirectory(prefix='braseiro-mesa-viva-') as td:
 out=Path(td)/'out';out.mkdir()
 sources=[ROOT/'tools/runtime_json/org/json/JSONException.java',ROOT/'tools/runtime_json/org/json/JSONArray.java',ROOT/'tools/runtime_json/org/json/JSONObject.java',ROOT/'tools/appdb_semantic_stubs/com/braseiro/pathfinder2e/AppLog.java',src/'LiveTableNarrativeDirector.java',ROOT/'ferramentas/LiveTableNarrativeDirectorSelfTest.java']
 subprocess.run(['javac','-encoding','UTF-8','-d',str(out),*[str(x) for x in sources]],check=True,cwd=ROOT)
 subprocess.run(['java','-cp',str(out),'com.braseiro.dnd.LiveTableNarrativeDirectorSelfTest'],check=True,cwd=ROOT)
print(f'RESULTADO ESTATICO MESA VIVA: {len(checks)}/{len(checks)}')
