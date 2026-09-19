#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_compile_hotfix.py <project-root>")

root = Path(sys.argv[1])
main = root / "android/app/src/main/java/com/braseiro/dnd5e/MainActivity.java"
text = main.read_text(encoding="utf-8")

old_norm = "Normalizer.normalize(raw,Normalizer.Form.NFD)"
new_norm = "java.text.Normalizer.normalize(raw,java.text.Normalizer.Form.NFD)"
if old_norm in text:
    text = text.replace(old_norm, new_norm, 1)

if "private void runOfflineMaster(" not in text:
    marker = "    private void acceptOrRepairNarration("
    if marker not in text:
        raise SystemExit("RUN109 hotfix insertion marker not found")
    method = """    private void runOfflineMaster(final String mode,final String userText,final List<JSONObject> localHits,final String reason){
        try{
            if(offlineMaster==null)offlineMaster=new OfflineMasterEngine();
            List<JSONObject> hits=localHits==null?new ArrayList<JSONObject>():localHits;
            JSONObject offline=offlineMaster.generate(mode,userText,activeMasterPlan,hits);
            if(offline==null)offline=new JSONObject();
            JSONObject marker=offline.optJSONObject("_offline_master");
            if(marker==null){marker=new JSONObject();offline.put("_offline_master",marker);}
            marker.put("enabled",true).put("reason",reason==null?"contingency":reason);
            pendingAiInputChars=0;aiBusy=false;streamingNarration="";
            handleAiResponse(offline.toString(),userText);
            setStatus("Mestre local ativo • "+(reason==null?"contingência remota":reason));
        }catch(Throwable failure){
            aiBusy=false;streamingNarration="";
            AppLog.e("OfflineMaster","Falha no Mestre local.",failure);
            setStatus("O Mestre local falhou sem alterar a campanha: "+safeMessage(failure));
            toast("Não foi possível concluir a cena offline.");
            if("Mestre IA".equals(currentScreen))showNarrator();
        }
    }

"""
    text = text.replace(marker, method + marker, 1)

main.write_text(text, encoding="utf-8")

checks = [
    "java.text.Normalizer.normalize(raw,java.text.Normalizer.Form.NFD)",
    "private void runOfflineMaster(",
    "offlineMaster.generate(mode,userText,activeMasterPlan,hits)",
]
for check in checks:
    if check not in text:
        raise SystemExit("RUN109 compile hotfix missing: " + check)

print("RUN109_COMPILE_HOTFIX_APPLIED")
