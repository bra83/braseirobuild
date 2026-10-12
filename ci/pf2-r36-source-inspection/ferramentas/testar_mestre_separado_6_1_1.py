from pathlib import Path
import subprocess, tempfile, sys

ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(src/'MainActivity.java').read_text(encoding='utf-8')
director=(src/'BraseiroNarrativeDirector.java').read_text(encoding='utf-8')
protocol=(src/'MasterProtocolV1.java').read_text(encoding='utf-8')
options=(src/'SceneOptionDiversifier.java').read_text(encoding='utf-8')
support=(src/'MasterSupportPanelEngine.java').read_text(encoding='utf-8')
version=(ROOT/'version.properties').read_text(encoding='utf-8')

checks={
 'versão 6.2.1 preserva Mestre 6.1.1':'VERSION_NAME=6.2.1' in version and 'VERSION_CODE=602100' in version,
 'card auxiliar separado':'AJUDA DO MESTRE — FORA DA NARRATIVA' in main,
 'campo exclusivo de pergunta':'PERGUNTAR AO MESTRE SEM AGIR' in main and 'PERGUNTAR SEM AVANÇAR A CENA' in main,
 'atalho de presentes':'QUEM ESTÁ AQUI?' in main,
 'atalho de geografia':'POSIÇÃO E DISTÂNCIAS' in main,
 'consulta lateral isolada':'sendMasterSideQuestion' in main and 'masterSideQueryActive' in main,
 'stream auxiliar não escreve na narrativa':'if(masterSideQueryActive)' in main and 'Respondendo no painel auxiliar' in main,
 'estado anterior é restaurado':'masterSidePreviousAiMode' in main and 'lastAiMode=masterSidePreviousAiMode' in main and 'activeRuleRetrieval=copyJsonObject(masterSidePreviousRuleRetrieval)' in main,
 'pergunta errada no campo principal é desviada':'BraseiroNarrativeDirector.isNonAdvancing(q)' in main and 'sendMasterSideQuestion(q)' in main,
 'modo RAW/RAI usa painel':'"Regras RAW/RAI".equals(chosenMode)' in main and 'Consulte os manuais e explique' in main,
 'consulta auxiliar reconhecida':'consulta auxiliar fora da ficcao' in director and 'pergunta ao mestre fora da ficcao' in director,
 'várias perguntas respondidas':'Responda diretamente cada pergunta feita' in director and 'itens separados quando houver mais de uma' in director,
 'distância não é inventada':'use somente o estado espacial disponível' in director and 'medida exata não está estabelecida' in director,
 'painel mostra busca de regras':'rules_searched' in support and 'evidence_count' in support and 'mechanics_allowed' in support,
 'painel oferece perícias':'skill_suggestions' in support and 'automatic_roll' in support,
 'painel mostra fontes':'compactSources' in support and 'sources' in support,
 'teste sem evidência é bloqueado':'Sem prova específica, o teste deve permanecer bloqueado' in support,
 'sugestões sempre reconstruídas':'ensureAlways' in options and main.count('SceneOptionDiversifier.ensureAlways')>=4,
 'sugestões continuam em teste':'SUGESTÕES OPCIONAIS — PARA DEPOIS DO TESTE' in main,
 'protocolo aceita suporte':'"master_support"' in protocol,
 'recuperação também mostra card':'addMasterSupportCard(b)' in main and main.count('addMasterSupportCard(b)')>=2,
}
for name,value in checks.items():
    print(('OK  ' if value else 'FALHA ')+name)
if not all(checks.values()):
    sys.exit(1)

with tempfile.TemporaryDirectory(prefix='braseiro-support-611-') as td:
    out=Path(td)/'out';out.mkdir()
    sources=[
        ROOT/'tools/runtime_json/org/json/JSONException.java',
        ROOT/'tools/runtime_json/org/json/JSONArray.java',
        ROOT/'tools/runtime_json/org/json/JSONObject.java',
        ROOT/'tools/appdb_semantic_stubs/com/braseiro/pathfinder2e/AppLog.java',
        src/'LiveTableNarrativeDirector.java',
        src/'BraseiroNarrativeDirector.java',
        src/'MasterSupportPanelEngine.java',
        ROOT/'ferramentas/MasterSupportPanelEngineSelfTest.java',
    ]
    subprocess.run(['javac','-encoding','UTF-8','-d',str(out),*[str(x) for x in sources]],check=True,cwd=ROOT)
    subprocess.run(['java','-cp',str(out),'com.braseiro.dnd.MasterSupportPanelEngineSelfTest'],check=True,cwd=ROOT)
print(f'RESULTADO ESTÁTICO MESTRE PRESERVADO EM 6.2.1: {len(checks)}/{len(checks)}')
