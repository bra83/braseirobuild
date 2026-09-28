from pathlib import Path
import shutil, sys, re

root=Path(sys.argv[1]).resolve()
bundle=Path(__file__).resolve().parent

def read(rel):
    return (root/rel).read_text(encoding="utf-8")
def write(rel,s):
    p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding="utf-8")
def req(s,old,new,label,count=1):
    n=s.count(old)
    if n < count:
        raise SystemExit(f"{label}: esperado >= {count}, encontrado {n}")
    return s.replace(old,new,count)

java="android/app/src/main/java/com/braseiro/pathfinder2e"
assets="android/app/src/main/assets/pathfinder/master"

# New integrated layers.
shutil.copy2(bundle/"HumanGmNarrativeProfile.java",root/java/"HumanGmNarrativeProfile.java")
shutil.copy2(bundle/"KingdomTurnGuideEngine.java",root/java/"KingdomTurnGuideEngine.java")
(root/assets).mkdir(parents=True,exist_ok=True)
shutil.copy2(bundle/"PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md",root/assets/"PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md")

# Narrative persona: extend existing director, never replace it.
rel=f"{java}/BraseiroNarrativeDirector.java"
s=read(rel)
s=req(s,
'        p.append(PathfinderNarrativeContract.prompt()).append("\\n");',
'        p.append(PathfinderNarrativeContract.prompt()).append("\\n");\n        p.append(HumanGmNarrativeProfile.prompt()).append("\\n");',
"persona runtime hook")
write(rel,s)

# Exact TTS Tom+Tags crash: Java regex received \\\\p{M}+ instead of Unicode class \\p{M}+.
rel=f"{java}/NarrationPlaybackPolicy.java"
s=read(rel)
bad='.replaceAll("\\\\\\\\p{M}+","")'
good='.replaceAll("\\\\p{M}+","")'
if bad not in s:
    # Defensive literal fallback against line-ending/formatter differences.
    bad='replaceAll("\\\\\\\\p{M}+","")'
    good='replaceAll("\\\\p{M}+","")'
s=req(s,bad,good,"tts unicode regex")
write(rel,s)

# Existing character-control persistence: normalize defaults to player, keep legacy rows readable.
rel=f"{java}/AppDb.java"
s=read(rel)
s=s.replace('getPartyState().optString("default_control_mode","manual")','getPartyState().optString("default_control_mode","player")')
s=s.replace('v.put("control_mode",mode==null?"manual":mode);','v.put("control_mode",mode==null?"player":mode);')
write(rel,s)

# Master cognitive policy: use existing control profiles, with explicit PLAYER/GM semantics.
rel=f"{java}/MasterCognitiveEngine.java"
s=read(rel)
old='out.put("agency_guard","Nunca decida ação, fala decisiva, gasto de recurso limitado ou escolha moral por herói em modo manual. Em modo assistido, apenas sugira. Em modo companheiro/automático, obedeça tactics e pare antes de recurso importante.");'
new='out.put("agency_guard","Controle PLAYER/Jogador: nunca decida ação, fala decisiva, gasto ou escolha moral pelo herói. Controle GM/Mestre: você pode escolher intenção, fala e ação voluntária coerentes com ficha/tactics, mas NUNCA aplique PV, condição, inventário, spell slot, recurso ou outro efeito diretamente; toda consequência mecânica passa pelo mesmo motor PF2e/Kingmaker e pelos mesmos gates do jogador.");'
s=req(s,old,new,"gm/player agency guard")
old2='unit.put("resource_guard",control.optString("control_mode","manual").equals("manual")?"não gastar recurso limitado sem declaração explícita":"seguir tática e pedir confirmação para recurso limitado");'
new2='String cm=control.optString("control_mode","player");boolean playerControlled="player".equals(cm)||"manual".equals(cm);unit.put("resource_guard",playerControlled?"não gastar recurso limitado sem declaração explícita":"Mestre pode escolher intenção/ação conforme tactics, mas recurso limitado e toda mutação mecânica continuam sujeitos aos gates do motor");'
s=req(s,old2,new2,"control resource guard")
write(rel,s)

# Main UI / image routing / guide.
rel=f"{java}/MainActivity.java"
s=read(rel)

# Pending character target.
s=req(s,
'    private static final String PREF_PENDING_NPC_PROMPT="pending_image_npc_prompt_v1";',
'    private static final String PREF_PENDING_NPC_PROMPT="pending_image_npc_prompt_v1";\n    private static final String PREF_PENDING_CHARACTER_ID="pending_image_character_id_v1";',
"pending character pref")

# Gemini character button must target character, not scene.
old='Button gemini=button("DESCREVER PERSONAGEM PARA O GEMINI",PANEL2);gemini.setOnClickListener(new View.OnClickListener(){public void onClick(View v){pendingTokenCharacterId=String.valueOf(id);sharePromptToGemini(PathfinderTokenPromptBuilder.build(mergeVisualCharacter(ch,sheet),NarrativeLocationNaturalizer.currentNarrativeLabel(db)));}});card.addView(gemini);'
new='Button gemini=button("DESCREVER PERSONAGEM PARA O GEMINI",PANEL2);gemini.setOnClickListener(new View.OnClickListener(){public void onClick(View v){pendingTokenCharacterId=String.valueOf(id);sharePromptToGeminiForCharacter(PathfinderTokenPromptBuilder.build(mergeVisualCharacter(ch,sheet),NarrativeLocationNaturalizer.currentNarrativeLabel(db)),id);}});card.addView(gemini);'
s=req(s,old,new,"character Gemini target")

# Add direct per-character control choice to Hero card, reusing character_control_profiles.
anchor='actions.addView(view);actions.addView(edit);actions.addView(token);actions.addView(del);card.addView(actions);'
insert=anchor+'\n            JSONObject heroControl=db.getCharacterControl(id);Button controlButton=button("CONTROLE: "+controlLabel(heroControl.optString("control_mode","player")).toUpperCase(Locale.ROOT),PANEL2);controlButton.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showCharacterControlChoice(id,ch.optString("name"));}});card.addView(controlButton);'
s=req(s,anchor,insert,"hero control button")

# Share-target helpers and persistence.
s=req(s,
'    private void sharePromptToGeminiForNpc(String prompt,long npcId,String npcName){sharePromptToGeminiTarget(prompt,"npc",npcId,npcName);}',
'    private void sharePromptToGeminiForNpc(String prompt,long npcId,String npcName){sharePromptToGeminiTarget(prompt,"npc",npcId,npcName);}\n    private void sharePromptToGeminiForCharacter(String prompt,long characterId){sharePromptToGeminiTarget(prompt,"character",characterId,"");}',
"character Gemini helper")

old='if("npc".equals(target)){pending.remove(PREF_PENDING_IMAGE_FINGERPRINT).putLong(PREF_PENDING_NPC_ID,npcId).putString(PREF_PENDING_NPC_NAME,npcName==null?"":npcName).putString(PREF_PENDING_NPC_PROMPT,prompt==null?"":prompt);}\n        else pending.putString(PREF_PENDING_IMAGE_FINGERPRINT,currentSceneVisualFingerprint()).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT);'
new='if("npc".equals(target)){pending.remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_CHARACTER_ID).putLong(PREF_PENDING_NPC_ID,npcId).putString(PREF_PENDING_NPC_NAME,npcName==null?"":npcName).putString(PREF_PENDING_NPC_PROMPT,prompt==null?"":prompt);}\n        else if("character".equals(target)){pending.remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT).putLong(PREF_PENDING_CHARACTER_ID,npcId);}\n        else pending.putString(PREF_PENDING_IMAGE_FINGERPRINT,currentSceneVisualFingerprint()).remove(PREF_PENDING_CHARACTER_ID).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT);'
s=req(s,old,new,"image target persistence")

# Route return image by pending target.
s=req(s,
'    private void applyReceivedImage(Uri stored){String target=prefs.getString(PREF_PENDING_IMAGE_TARGET,"scene");if("npc".equals(target)){applyReceivedNpcImage(stored);return;}applyReceivedSceneImage(stored);}',
'''    private void applyReceivedImage(Uri stored){String target=prefs.getString(PREF_PENDING_IMAGE_TARGET,"scene");if("npc".equals(target)){applyReceivedNpcImage(stored);return;}if("character".equals(target)){applyReceivedCharacterImage(stored);return;}applyReceivedSceneImage(stored);}
    private void applyReceivedCharacterImage(Uri stored){
        if(stored==null||!ImageStorageManager.isReadable(this,stored)){setStatus("A cópia recebida não está mais disponível.");clearPendingImageTarget();return;}
        final long characterId=prefs.getLong(PREF_PENDING_CHARACTER_ID,0L);final JSONObject character=characterId>0?db.getCharacter(characterId):new JSONObject();
        if(character.length()==0){setStatus("O personagem associado ao token não existe mais nesta campanha.");clearPendingImageTarget();toast("Token não vinculado: personagem não encontrado.");return;}
        final Uri source=stored;final String name=character.optString("name","personagem");setStatus("Salvando token persistente de "+name+"…");
        AppExecutors.io().execute(new Runnable(){public void run(){try{final Uri portrait=storeCharacterAvatar(source,characterId);if(portrait==null)throw new Exception("A imagem não pôde ser copiada com segurança.");db.setCharacterToken(characterId,portrait.toString());ui.post(new Runnable(){public void run(){clearPendingImageTarget();pendingTokenCharacterId="";setStatus("Token salvo exclusivamente para "+name+".");toast("Token de "+name+" atualizado.");navigate("Heróis");}});}catch(final Exception error){AppLog.e("CharacterPortrait","Falha ao persistir token recebido.",error);ui.post(new Runnable(){public void run(){setStatus("O token recebido não pôde ser salvo.");toast(String.valueOf(error.getMessage()));}});}}});
    }''',
"received character image route")

s=req(s,
'private void clearPendingImageTarget(){prefs.edit().remove(PREF_PENDING_IMAGE_TARGET).remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT).apply();}',
'private void clearPendingImageTarget(){prefs.edit().remove(PREF_PENDING_IMAGE_TARGET).remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_CHARACTER_ID).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT).apply();}',
"clear character image target")

# Simplify existing sophisticated control UI to the requested human-facing binary choice.
s=s.replace('new String[]{"Manual","Assistido","Companheiro","Automático em combate"}','new String[]{"Jogador","Mestre"}')
old='private static String controlLabel(String mode){if("assisted".equals(mode))return "Assistido";if("companion".equals(mode))return "Companheiro";if("combat_auto".equals(mode))return "Automático em combate";return "Manual";}\n    private static String controlMode(String label){if("Assistido".equals(label))return "assisted";if("Companheiro".equals(label))return "companion";if("Automático em combate".equals(label))return "combat_auto";return "manual";}'
new='private static String controlLabel(String mode){return ("gm".equals(mode)||"assisted".equals(mode)||"companion".equals(mode)||"combat_auto".equals(mode))?"Mestre":"Jogador";}\n    private static String controlMode(String label){return "Mestre".equals(label)?"gm":"player";}\n    private void showCharacterControlChoice(final long characterId,String name){final JSONObject current=db.getCharacterControl(characterId);final String tactics=current.optString("tactics_json","{}");new AlertDialog.Builder(this).setTitle("Quem controla "+name+"?").setMessage("Jogador: você decide todas as ações voluntárias.\\n\\nMestre: Barbara pode decidir intenção, fala e ação deste personagem, mas PV, condições, recursos, inventário, magia e qualquer outra consequência continuam passando pelo mesmo motor PF2e/Kingmaker.").setNegativeButton("Cancelar",null).setNeutralButton("MESTRE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){db.saveCharacterControl(characterId,"gm",tactics);toast("Controle entregue ao Mestre.");showHeroes();}}).setPositiveButton("JOGADOR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){db.saveCharacterControl(characterId,"player",tactics);toast("Você assumiu o controle deste personagem.");showHeroes();}}).show();}'
s=req(s,old,new,"binary player/gm control")

# Add dynamic guide button to existing Solo Kingdom card.
anchor='guided.addView(text(kingdomGuide.optString("text","O motor apresenta somente a próxima decisão humana."),12,MUTED,false));'
insert=anchor+'Button kingdomHelp=smallButton("?  GUIA COMPLETO DO TURNO",PANEL2);kingdomHelp.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showKingdomTurnGuideDialog();}});guided.addView(kingdomHelp);'
s=req(s,anchor,insert,"kingdom guide button")

# Add guide dialog immediately before current guided decision method.
anchor='    private void runGuidedKingdomDecision(){'
method='''    private void showKingdomTurnGuideDialog(){
        JSONObject snapshot=new KingdomTurnGuideEngine(kingmakerRepository,kingmakerRules).snapshot();
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("SEQUÊNCIA DO TURNO",16,GOLD,true));
        JSONArray sequence=snapshot.optJSONArray("sequence");if(sequence!=null)for(int i=0;i<sequence.length();i++){JSONObject step=sequence.optJSONObject(i);if(step==null)continue;String mark=step.optBoolean("current")?"▶ ":"";form.addView(text(mark+step.optInt("index")+". "+step.optString("label")+" — "+step.optString("summary"),12,step.optBoolean("current")?TEXT:MUTED,step.optBoolean("current")));}
        JSONObject state=snapshot.optJSONObject("state");if(state!=null){form.addView(text("ESTADO QUE IMPORTA AGORA",15,GOLD,true));form.addView(text("Fase: "+snapshot.optString("phase_label")+" • Reino nível "+state.optInt("level")+" • CD de Controle "+state.optInt("control_dc")+" • RP "+state.optInt("resource_points")+" • Consumo "+state.optInt("consumption")+" • Unrest "+state.optInt("unrest")+"\\nCommodities: alimento "+state.optInt("food")+" • madeira "+state.optInt("lumber")+" • luxos "+state.optInt("luxuries")+" • minério "+state.optInt("ore")+" • pedra "+state.optInt("stone"),12,TEXT,false));}
        JSONArray alerts=snapshot.optJSONArray("configuration_alerts");if(alerts!=null&&alerts.length()>0){form.addView(text("CONFIGURAÇÕES / DECISÕES QUE PODEM SER NECESSÁRIAS",15,GOLD,true));for(int i=0;i<alerts.length();i++){JSONObject a=alerts.optJSONObject(i);if(a!=null)form.addView(text("• "+a.optString("title")+" — "+a.optString("text"),12,MUTED,false));}}
        form.addView(text("O QUE VOCÊ PODE ESCOLHER NESTA FASE",15,GOLD,true));JSONArray options=snapshot.optJSONArray("options");if(options!=null)for(int i=0;i<options.length();i++){JSONObject o=options.optJSONObject(i);if(o==null)continue;boolean valid=!o.has("valid_now")||o.optBoolean("valid_now");LinearLayout c=card();c.addView(text((valid?"":"INDISPONÍVEL • ")+o.optString("name"),14,valid?TEXT:MUTED,true));String skill=o.optString("skill"),ability=o.optString("ability");String meta=(skill.length()>0?("Perícia: "+skill):"")+(ability.length()>0?(" • atributo: "+ability):"");if(meta.length()>0)c.addView(text(meta,11,HINT,false));c.addView(text(o.optString("description"),12,TEXT,false));if(o.optString("requirements").length()>0)c.addView(text("Antes de escolher: "+o.optString("requirements"),11,MUTED,false));if(o.optString("changes").length()>0)c.addView(text("O que pode mudar: "+o.optString("changes"),11,MUTED,false));if(o.optString("next").length()>0)c.addView(text("Depois: "+o.optString("next"),11,HINT,false));form.addView(c);}
        form.addView(text("Este guia não mantém uma segunda cópia das regras: ele lê o mesmo estado e o mesmo catálogo usados pelo motor do Reino. A escolha continua sendo sua.",11,HINT,false));
        new AlertDialog.Builder(this).setTitle("Guia do Turno do Reino").setView(scrollableDialogView(form)).setNegativeButton("Fechar",null).setPositiveButton("IR PARA A PRÓXIMA DECISÃO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){runGuidedKingdomDecision();}}).show();
    }

'''
s=req(s,anchor,method+anchor,"kingdom guide dialog")

write(rel,s)

# MasterOrchestrator: make GM-controlled PCs explicit while preserving mechanical arbiter.
rel=f"{java}/MasterOrchestrator.java"
s=read(rel)
anchor='live_table não autoriza ação, emoção ou conhecimento fora do personagem; apenas organiza foco, cortes, convergência, promessas e pressão causal.'
extra=anchor+' PERSONAGENS DO GRUPO: game_bus/party contém control_mode. PLAYER/Jogador/manual significa que você jamais escolhe a ação voluntária do herói. GM/Mestre (e valores legados assisted/companion/combat_auto) autoriza você a escolher intenção, fala e ação voluntária daquele herói coerentemente com ficha/tactics, como um companheiro conduzido pelo Mestre. Mesmo nesses personagens, você NÃO altera diretamente PV, condições, recursos, inventário, spell slots ou estado mecânico: toda consequência deve passar pelas autoridades mecânicas, pending tests e validação local.'
s=req(s,anchor,extra,"orchestrator gm-controlled party policy")
write(rel,s)

# Preserve Kokoro: fail patch if key architecture disappeared.
main=s if False else read(f"{java}/MainActivity.java")
required=[
 'com.k2fsa.sherpa.onnx.tts.engine',
 'com.k2fsa.sherpa.onnx.tts.engine.debug',
 'Braseiro Kokoro PT-BR · neural',
 'availableNativeTtsVoices()',
 'nativeTts.setVoice(chosen)'
]
for token in required:
    if token not in main: raise SystemExit("Kokoro regression: missing "+token)

print("PATCH_OK")
