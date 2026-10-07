from pathlib import Path
import re, sys, json

root=Path(sys.argv[1])
pkg=root/"android"/"app"/"src"/"main"/"java"/"com"/"braseiro"/"dnd5e"
if not pkg.exists():
    raise SystemExit("SOURCE_NOT_FOUND")

def read(name):
    return (pkg/name).read_text(encoding="utf-8")
def write(name,text):
    (pkg/name).write_text(text,encoding="utf-8",newline="\n")
def once(text,old,new,label):
    n=text.count(old)
    if n!=1: raise SystemExit(f"{label}: expected 1, got {n}")
    return text.replace(old,new,1)

# ------------------------------------------------------------------
# Human Master runtime contract, derived from the canonical user profile.
# ------------------------------------------------------------------
human=r'''package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Runtime narrative direction. Mechanics/state remain authoritative. */
public final class HumanMasterNarrativeProfile {
    private HumanMasterNarrativeProfile(){}

    public static String systemInstruction(){
        return "DIREÇÃO NARRATIVA CANÔNICA — MESTRE HUMANO IMERSIVO: "
            +"A ficção vem antes da mecânica e volta imediatamente depois dela. "
            +"Consulte o estado persistente antes de afirmar clima, hora, data, posição, recursos, feridas, NPC presente, objetivo ou viagem. "
            +"Nunca sacrifique consistência por texto bonito e nunca revele fatos secretos sem descoberta. "
            +"Narre como um Mestre humano em tempo real: concreto, visual, sensorial, reativo e proporcional à importância da cena; não como relatório, tutorial, HUD, banco de dados ou romance. "
            +"NPCs existem fora do diálogo: têm objetivos, memória, rotina e reagem ao tempo e ao mundo. Preserve identidade, relações, promessas, feridas, posição, clima, portas, cadáveres e consequências entre cenas e sessões. "
            +"Mudanças de clima e horário precisam aparecer no mundo quando relevantes, não apenas na interface. "
            +"Não decida emoções nem escolhas importantes do personagem do jogador. Peça teste apenas quando houver incerteza/consequência/regra e transforme o resultado de volta em ficção. "
            +"Sugestões opcionais: zero a duas; somente ações concretas ancoradas em pessoa, objeto, ameaça, pista, saída, lugar ou objetivo realmente presentes. Nunca crie opções genéricas só para preencher a tela. "
            +"CONTROLE DOS HERÓIS: modo manual nunca age sozinho. Modo sistema pode executar autonomamente apenas ações rotineiras, reversíveis, de baixo risco e sem gastar recurso limitado ou assumir compromisso importante. Em combate, modo sistema é assistido: propõe uma ação legal e aguarda confirmação. Se o usuário escrever explicitamente fala ou ação de qualquer herói, essa declaração é autoridade para aquele momento, mesmo que o herói esteja em modo sistema. "
            +"ORDEM DE AÇÃO: fora de combate não force todos os heróis a agir em toda cena; dê foco conforme causalidade, posição, relevância e fala do usuário. Em combate, todos os combatentes capazes recebem seu turno na ordem real de iniciativa; nunca invente rolagem oculta. Empates seguem a autoridade do perfil de regras/controle e permanecem auditáveis. "
            +"MORTE DE NPC: quando a ficção e a mecânica confirmarem morte, envie atualização explícita dead=true/status=dead/active=0; não reutilize essa identidade como NPC vivo sem ressurreição explícita. "
            +"Pare quando houver decisão real. O jogador deve sentir um lugar vivo, não o resultado de um sistema.";
    }

    public static JSONObject contract(){
        JSONObject o=new JSONObject();
        try{
            o.put("id","human_master_immersive_v1");
            o.put("fiction_before_mechanics",true);
            o.put("state_authority",true);
            o.put("persistent_weather_time",true);
            o.put("persistent_npc_identity",true);
            o.put("grounded_optional_actions_max",2);
            o.put("allow_zero_optional_actions",true);
            o.put("manual_hero_autonomy",false);
            o.put("system_hero_routine_only",true);
            o.put("system_hero_combat_assisted",true);
            o.put("explicit_user_hero_action_overrides",true);
            o.put("combat_order","initiative");
            o.put("auto_rolls",false);
        }catch(Exception ignored){}
        return o;
    }
}
'''
write("HumanMasterNarrativeProfile.java",human)

# ------------------------------------------------------------------
# AppDb: preserve NPC tombstones; a dead identity cannot respawn by accident.
# Portrait reference is cleared on confirmed death while identity remains.
# ------------------------------------------------------------------
t=read("AppDb.java")
old='''        JSONObject previous=findNpcMemoryByName(name);
        ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("stable_id",stableId);v.put("name",name);
'''
new='''        JSONObject previous=findNpcMemoryByName(name);
        boolean deadTombstone=npcDeathTombstone(normalized,previous);
        ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("stable_id",stableId);v.put("name",name);
'''
t=once(t,old,new,"npc-death-anchor")
old='''        v.put("active",normalized.has("active")?normalized.optInt("active",1):previous.optInt("active",1));v.put("updated_at",System.currentTimeMillis());
'''
new='''        v.put("active",deadTombstone?0:(normalized.has("active")?normalized.optInt("active",1):previous.optInt("active",1)));if(deadTombstone){v.put("token_uri","");v.put("stats_json",npcStatsWithDeath(normalized,previous));}v.put("updated_at",System.currentTimeMillis());
'''
t=once(t,old,new,"npc-death-values")
anchor='''    private static void putNpcField(ContentValues v,JSONObject source,JSONObject previous,String key,String fallback){v.put(key,source.has(key)?source.optString(key,fallback):previous.optString(key,fallback));}
'''
helpers='''    private static boolean npcDeathTombstone(JSONObject current,JSONObject previous){
        try{
            if(current!=null&&current.optBoolean("resurrected",false))return false;
            if(current!=null&&(current.optBoolean("dead",false)||"dead".equalsIgnoreCase(current.optString("status"))||"morto".equalsIgnoreCase(current.optString("status"))))return true;
            JSONObject cs=current==null?new JSONObject():new JSONObject(current.optString("stats_json","{}"));
            if(cs.optBoolean("dead",false)||"dead".equalsIgnoreCase(cs.optString("status"))||"morto".equalsIgnoreCase(cs.optString("status")))return true;
            JSONObject ps=previous==null?new JSONObject():new JSONObject(previous.optString("stats_json","{}"));
            return ps.optBoolean("dead",false)||"dead".equalsIgnoreCase(ps.optString("status"))||"morto".equalsIgnoreCase(ps.optString("status"));
        }catch(Exception ignored){return false;}
    }
    private static String npcStatsWithDeath(JSONObject current,JSONObject previous){
        try{
            JSONObject s=new JSONObject(current!=null&&current.has("stats_json")?current.optString("stats_json","{}"):previous==null?"{}":previous.optString("stats_json","{}"));
            s.put("dead",true).put("status","dead");return s.toString();
        }catch(Exception ignored){return "{\\"dead\\":true,\\"status\\":\\"dead\\"}";}
    }
    public void markNpcDead(long id){
        JSONObject npc=getNpcMemory(id);if(npc.length()==0)return;
        ContentValues v=new ContentValues();v.put("active",0);v.put("token_uri","");v.put("stats_json",npcStatsWithDeath(new JSONObject(),npc));v.put("updated_at",System.currentTimeMillis());
        getWritableDatabase().update("npc_memory_profiles",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});
        ContentValues combat=new ContentValues();combat.put("token_uri","");getWritableDatabase().update("combatants",combat,"world_profile=? AND source_kind='npc' AND source_id=?",new String[]{getActiveMapProfile(),String.valueOf(id)});
    }
''' + anchor
t=once(t,anchor,helpers,"npc-death-helpers")
write("AppDb.java",t)

# ------------------------------------------------------------------
# MainActivity
# ------------------------------------------------------------------
t=read("MainActivity.java")

# Immediate visible startup surface instead of gray activity; defer TTS and catalog work.
old='''            prefs=getSharedPreferences("settings",MODE_PRIVATE);if(!prefs.getBoolean("rag_master_2_2_migrated",false))prefs.edit().putString("master_pace","cinematic").putBoolean("rag_master_2_2_migrated",true).apply();if(!prefs.getBoolean("narrator_2_7_cinematic_restored",false)){SharedPreferences.Editor paceEdit=prefs.edit().putBoolean("narrator_2_7_cinematic_restored",true);if(prefs.getBoolean("narrator_2_6_speed_migrated",false)&&"balanced".equals(prefs.getString("master_pace","cinematic")))paceEdit.putString("master_pace","cinematic");paceEdit.apply();}reconcileWorldProfilePreferences();applyTheme();ui=new Handler(Looper.getMainLooper());
'''
new='''            prefs=getSharedPreferences("settings",MODE_PRIVATE);if(!prefs.getBoolean("rag_master_2_2_migrated",false))prefs.edit().putString("master_pace","cinematic").putBoolean("rag_master_2_2_migrated",true).apply();if(!prefs.getBoolean("narrator_2_7_cinematic_restored",false)){SharedPreferences.Editor paceEdit=prefs.edit().putBoolean("narrator_2_7_cinematic_restored",true);if(prefs.getBoolean("narrator_2_6_speed_migrated",false)&&"balanced".equals(prefs.getString("master_pace","cinematic")))paceEdit.putString("master_pace","cinematic");paceEdit.apply();}reconcileWorldProfilePreferences();applyTheme();ui=new Handler(Looper.getMainLooper());showImmediateStartupSurface();
'''
t=once(t,old,new,"startup-visible")
t=t.replace('''accountManager=AccountManager.get(this);ensureNpcSourceManualCatalog();if(!prefs.getBoolean("npc_token_folder_47_migrated",false))''','''accountManager=AccountManager.get(this);if(!prefs.getBoolean("npc_token_folder_47_migrated",false))''',1)
t=t.replace('''            ensureNativeTtsInitialized();
''','',1)
t=t.replace('''ImageStorageManager.cleanupLegacySceneImages(this,sharedImageUri);
''','',1)
old='''            setStatus(safetyMessage);handleIncomingIntent(getIntent());warmManualSearchIndexAsync();
            AppExecutors.io().execute(new Runnable(){public void run(){final JSONObject report=db.backfillNpcPersonalities();ui.post(new Runnable(){public void run(){if(report.optInt("updated",0)>0)setBackgroundStatus("NPCs: "+report.optInt("updated")+" personalidade(s) aplicadas do catálogo de "+report.optInt("catalog_profiles",300)+" perfis.");}});}});
'''
new='''            setStatus(safetyMessage);handleIncomingIntent(getIntent());warmManualSearchIndexAsync();
            ui.postDelayed(new Runnable(){public void run(){ensureNativeTtsInitialized();}},350L);
            final Uri cleanupSceneImage=sharedImageUri;AppExecutors.io().execute(new Runnable(){public void run(){try{ensureNpcSourceManualCatalog();ImageStorageManager.cleanupLegacySceneImages(MainActivity.this,cleanupSceneImage);final JSONObject report=db.backfillNpcPersonalities();ui.post(new Runnable(){public void run(){if(report.optInt("updated",0)>0)setBackgroundStatus("NPCs: "+report.optInt("updated")+" personalidade(s) aplicadas do catálogo de "+report.optInt("catalog_profiles",300)+" perfis.");}});}catch(Exception e){AppLog.w("StartupAsync","Preparação secundária continuou sem bloquear a interface.",e);}}});
'''
t=once(t,old,new,"startup-defer")

# Add startup surface before showStartupFailure.
anchor='''    private void showStartupFailure(Throwable fatal){
'''
startup=r'''    private void showImmediateStartupSurface(){
        try{
            LinearLayout splash=new LinearLayout(this);splash.setOrientation(LinearLayout.VERTICAL);splash.setGravity(Gravity.CENTER);splash.setPadding(dp(24),dp(32),dp(24),dp(32));splash.setBackgroundColor(Color.rgb(24,18,14));
            TextView title=new TextView(this);title.setText("BRASEIRO D&D");title.setTextColor(Color.rgb(228,177,79));title.setTextSize(28);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);splash.addView(title);
            TextView sub=new TextView(this);sub.setText("Preparando a campanha…");sub.setTextColor(Color.rgb(232,222,211));sub.setTextSize(15);sub.setPadding(0,dp(12),0,0);sub.setGravity(Gravity.CENTER);splash.addView(sub);
            setContentView(splash);
        }catch(Throwable ignored){/* o recovery de startup continua sendo a autoridade */}
    }

''' + anchor
t=once(t,anchor,startup,"startup-method")

# Gemini save button directly under key fields. TESTAR also saves atomically first.
# Gemini key controls: put an explicit save control directly under the key fields.
gemini_anchor='''ai.addView(key);ai.addView(paidKey);ai.addView(text("A chave alternativa só é usada pelo Charon quando a principal recebe 429, esgota a cota ou entra no circuito de espera de cinco minutos.",12,MUTED,false));'''
gemini_new='''ai.addView(key);ai.addView(paidKey);Button saveGeminiKeys=button("SALVAR CHAVES GEMINI",GREEN);saveGeminiKeys.setOnClickListener(new View.OnClickListener(){public void onClick(View v){String primary=key.getText().toString().trim(),alternate=paidKey.getText().toString().trim();if(primary.length()<20){toast("Informe uma chave principal Gemini válida.");return;}if(saveGeminiSecretsAtomically(primary,alternate)){toast("Chaves Gemini salvas no Android Keystore.");setStatus("Chave Gemini salva e aplicada.");}else toast("A chave não foi salva; o valor anterior foi preservado.");}});ai.addView(saveGeminiKeys);ai.addView(text("A chave alternativa só é usada pelo Charon quando a principal recebe 429, esgota a cota ou entra no circuito de espera de cinco minutos.",12,MUTED,false));'''
t=once(t,gemini_anchor,gemini_new,"gemini-save-button")
gemini_test='''String k=key.getText().toString().trim();if(k.length()<20){toast("Informe a chave Gemini.");return;}setStatus("Testando o modelo...");'''
gemini_test_new='''String k=key.getText().toString().trim();if(k.length()<20){toast("Informe a chave Gemini.");return;}if(!saveGeminiSecretsAtomically(k,paidKey.getText().toString().trim())){toast("Não foi possível salvar a chave antes do teste.");return;}setStatus("Testando o modelo...");'''
t=once(t,gemini_test,gemini_test_new,"gemini-test-saves-first")

# Character controller UI using the already-existing character_control_profiles backend.
old='''        f.active=check("Personagem em jogo",edit?existing.optInt("active",1)==1:initial.optBoolean("active",true));form.addView(f.active);configureSelectableMechanicFields(f,rulesProfile);bindCharacterDependencyListeners(f,rulesProfile);scroll.addView(form);
'''
new='''        JSONObject savedControl=edit?db.getCharacterControl(existing.optLong("id",0)):new JSONObject();String initialControl=savedControl.optString("control_mode",initial.optString("control_mode","manual"));f.controller=spinner(new String[]{"Você","Sistema — rotina / combate assistido"},"system".equals(initialControl)?"Sistema — rotina / combate assistido":"Você");LinearLayout controlCard=card();controlCard.addView(text("CONTROLE DO PERSONAGEM",14,GOLD,true));controlCard.addView(f.controller);controlCard.addView(text("Sistema: somente rotina reversível e de baixo risco. Em combate, o sistema sugere e você confirma. Uma fala ou ação que você escrever para este herói sempre prevalece naquele momento.",11,MUTED,false));form.addView(controlCard);f.active=check("Personagem em jogo",edit?existing.optInt("active",1)==1:initial.optBoolean("active",true));form.addView(f.active);configureSelectableMechanicFields(f,rulesProfile);bindCharacterDependencyListeners(f,rulesProfile);scroll.addView(form);
'''
t=once(t,old,new,"character-control-ui")

old='''db.updateCharacterRuleDetails(id,sheet.optString("background"),sheet.optString("appearance"),sheet.optString("proficiencies")+"\\n"+sheet.optString("skills"),sheet.optString("inventory"),sheet.optString("spellcasting")+"\\n"+sheet.optString("spells")+"\\n"+sheet.optString("features"),rulesProfile);db.updateCharacterSheet(id,sheet.toString());db.addAuditEvent'''
new='''db.updateCharacterRuleDetails(id,sheet.optString("background"),sheet.optString("appearance"),sheet.optString("proficiencies")+"\\n"+sheet.optString("skills"),sheet.optString("inventory"),sheet.optString("spellcasting")+"\\n"+sheet.optString("spells")+"\\n"+sheet.optString("features"),rulesProfile);db.updateCharacterSheet(id,sheet.toString());String controlMode=f.controller!=null&&f.controller.getSelectedItemPosition()==1?"system":"manual";db.saveCharacterControl(id,controlMode,"{\\"routine_only\\":true,\\"combat\\":\\"assisted\\",\\"user_override\\":true,\\"limited_resources\\":\\"confirm\\"}");db.addAuditEvent'''
t=once(t,old,new,"character-control-save")

old='''o.put("life_path",value(f.lifePath));o.put("rules_source",profile);'''
new='''o.put("life_path",value(f.lifePath));o.put("rules_source",profile);o.put("control_mode",f.controller!=null&&f.controller.getSelectedItemPosition()==1?"system":"manual");'''
t=once(t,old,new,"character-control-sheet")

old='''if(f.active!=null)f.active.setChecked(s.optBoolean("active",true));if(f!=null){'''
new='''if(f.controller!=null&&s.has("control_mode"))selectSpinner(f.controller,"system".equals(s.optString("control_mode"))?"Sistema — rotina / combate assistido":"Você");if(f.active!=null)f.active.setChecked(s.optBoolean("active",true));if(f!=null){'''
t=once(t,old,new,"character-control-seed")

old='''Spinner species,cls,background,alignment,origin,career,terms;CheckBox auto,active;'''
new='''Spinner species,cls,background,alignment,origin,career,terms,controller;CheckBox auto,active;'''
t=once(t,old,new,"character-control-ref")

# Unified portrait pipeline: clicking a hero or NPC token -> canonical prompt -> Gemini -> exact persistent target.
old='''        tile.addView(circle,new LinearLayout.LayoutParams(dp(58),dp(58)));
'''
new='''        if(!entry.optBoolean("group",false)&&!entry.optBoolean("transient",false)){final JSONObject portraitEntry=entry;circle.setClickable(true);if("AVENTUREIRO".equalsIgnoreCase(kind)){circle.setContentDescription("Gerar ou atualizar retrato persistente do herói "+name+" no Gemini");circle.setOnClickListener(new View.OnClickListener(){public void onClick(View v){beginHeroPortrait(portraitEntry);}});}else{circle.setContentDescription("Gerar ou atualizar retrato persistente de "+name+" no Gemini");circle.setOnClickListener(new View.OnClickListener(){public void onClick(View v){beginNpcPortrait(portraitEntry);}});}}
        tile.addView(circle,new LinearLayout.LayoutParams(dp(58),dp(58)));
'''
t=once(t,old,new,"npc-portrait-click")

anchor='''    private Uri storeNpcAvatar(Uri source,long npcId){try{return ImageStorageManager.importImage(this,source,"npc_avatars","npc_"+npcId,80);}catch(Exception e){AppLog.e("ImageImport","Falha ao armazenar avatar do NPC "+npcId,e);return null;}}
'''
portrait=r'''    private void setPendingPortraitTarget(String type,long id,String stable,String name){
        if(prefs==null)return;SharedPreferences.Editor e=prefs.edit()
            .putString("pending_image_target_type",type==null?"":type)
            .putLong("pending_image_target_id",id)
            .putString("pending_image_target_stable_id",stable==null?"":stable)
            .putString("pending_image_target_name",name==null?"":name);
        if("NPC".equals(type))e.putLong("pending_npc_portrait_id",id).putString("pending_npc_portrait_stable_id",stable==null?"":stable).putString("pending_npc_portrait_name",name==null?"":name);
        else e.remove("pending_npc_portrait_id").remove("pending_npc_portrait_stable_id").remove("pending_npc_portrait_name");
        e.apply();
    }
    private String circularTokenPortraitRules(){
        return "FORMATO OBRIGATÓRIO: retrato quadrado 1:1 para TOKEN CIRCULAR de VTT; enquadramento EXCLUSIVAMENTE dos ombros para cima; rosto centralizado; cabeça inteira visível; cabelo, orelhas, chifres, capuz ou elmo totalmente dentro do quadro; margem de segurança generosa em todos os lados; olhos próximos ao centro; todos os traços importantes dentro dos 70% centrais para sobreviver ao recorte redondo. Fundo simples, discreto e de baixo contraste. Sem texto, nome, moldura, ficha, corpo inteiro, pose aberta ou cenário dominante. Não cortar topo da cabeça, queixo ou ombros. Equipamentos/armas só podem aparecer parcialmente se couberem naturalmente no enquadramento sem empurrar o rosto para fora do centro.";
    }
    private void sharePortraitPromptWithGemini(String prompt,String name){
        Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,prompt);share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{Intent direct=new Intent(share);direct.setPackage("com.google.android.apps.bard");startActivity(direct);}catch(Exception noGemini){startActivity(Intent.createChooser(share,"Gerar retrato de "+name));}
    }
    private void beginHeroPortrait(JSONObject entry){
        try{
            String name=entry==null?"":entry.optString("name","").trim();if(name.length()==0)return;
            JSONObject hero=db.findCharacterByName(name);if(hero.length()==0){toast("Herói não encontrado na ficha persistente.");return;}
            long id=hero.optLong("id",0);if(id<=0){toast("Identidade persistente do herói inválida.");return;}
            JSONObject sheet;try{sheet=new JSONObject(hero.optString("sheet_json","{}"));}catch(Exception e){sheet=new JSONObject();}
            String species=hero.optString("species",sheet.optString("species",sheet.optString("race",""))).trim();
            String cls=hero.optString("class_name",sheet.optString("class_name",sheet.optString("class",""))).trim();
            String subclass=sheet.optString("subclass_name",sheet.optString("subclass","")).trim();
            String background=hero.optString("background",sheet.optString("background_name",sheet.optString("background",""))).trim();
            String visual=hero.optString("visual_description",sheet.optString("appearance",sheet.optString("visual_description",""))).trim();
            String gender=sheet.optString("gender",sheet.optString("sex","")).trim();
            String age=sheet.optString("age",sheet.optString("age_range","")).trim();
            String equipment=hero.optString("equipment",sheet.optString("inventory",sheet.optString("equipment",""))).trim();
            setPendingPortraitTarget("HERO",id,String.valueOf(id),name);
            StringBuilder p=new StringBuilder();p.append("Crie o retrato CANÔNICO deste herói de D&D para uso como token no Braseiro VTT. Preserve a mesma identidade em futuras imagens. ").append(circularTokenPortraitRules()).append("\n\n");
            p.append("HERÓI: ").append(name).append(".\n");if(species.length()>0)p.append("Espécie/raça: ").append(species).append(".\n");if(cls.length()>0)p.append("Classe: ").append(cls).append(".\n");if(subclass.length()>0)p.append("Subclasse: ").append(subclass).append(".\n");if(hero.optInt("level",0)>0)p.append("Nível: ").append(hero.optInt("level")).append(".\n");if(background.length()>0)p.append("Antecedente: ").append(background).append(".\n");if(gender.length()>0)p.append("Sexo/gênero definido na ficha: ").append(gender).append(".\n");if(age.length()>0)p.append("Idade/faixa: ").append(age).append(".\n");if(visual.length()>0)p.append("Aparência canônica: ").append(visual).append(".\n");if(equipment.length()>0)p.append("Equipamento visual relevante: ").append(equipment).append(".\n");
            p.append("Priorize rosto, espécie, classe, aparência, cabelo/barba, olhos, pele/pelagem/escamas, roupas/armadura, marcas e expressão já definidas. Não invente mudança de raça, classe ou aparência que contradiga a ficha.");
            sharePortraitPromptWithGemini(p.toString(),name);setStatus("Prompt de retrato do herói enviado ao Gemini para "+name+". Compartilhe a imagem de volta ao Braseiro.");
        }catch(Exception e){AppLog.e("HeroPortrait","Falha ao preparar retrato persistente do herói.",e);toast("Retrato do herói: "+e.getMessage());}
    }
    private void beginNpcPortrait(JSONObject entry){
        try{
            String name=entry==null?"":entry.optString("name","").trim();if(name.length()==0)return;
            JSONObject npc=db.findNpcMemoryByName(name);
            if(npc.length()==0){db.upsertNpcMemory(new JSONObject().put("name",name).put("stable_id",NpcIdentityNormalizer.stableId(activeWorldProfile(),name)).put("visual_description",entry.optString("hint","")).put("active",1));npc=db.findNpcMemoryByName(name);}
            if(npc.length()==0){toast("Não foi possível criar a identidade persistente deste NPC.");return;}
            JSONObject stats;try{stats=new JSONObject(npc.optString("stats_json","{}"));}catch(Exception e){stats=new JSONObject();}
            if(stats.optBoolean("dead",false)||"dead".equalsIgnoreCase(stats.optString("status"))||"morto".equalsIgnoreCase(stats.optString("status"))){toast(name+" está registrado como morto; o retrato não será recriado.");return;}
            long id=npc.optLong("id",0);String stable=npc.optString("stable_id",NpcIdentityNormalizer.stableId(activeWorldProfile(),name));
            setPendingPortraitTarget("NPC",id,stable,name);
            String species=stats.optString("species",stats.optString("race",stats.optString("creature_type",entry.optString("hint",""))));
            String visual=npc.optString("visual_description","").trim();String summary=npc.optString("summary","").trim();
            StringBuilder p=new StringBuilder();p.append("Crie o retrato CANÔNICO deste NPC de D&D para uso como token no Braseiro VTT. Identidade única e persistente; não crie uma pessoa diferente. ").append(circularTokenPortraitRules()).append("\n\n");
            p.append("NPC: ").append(name).append(".\n");if(species.length()>0)p.append("Espécie/raça/tipo: ").append(species).append(".\n");if(visual.length()>0)p.append("Aparência canônica: ").append(visual).append(".\n");if(summary.length()>0)p.append("Papel/contexto visual: ").append(summary).append(".\n");if(npc.optString("attitude").length()>0)p.append("Postura/atitude visível: ").append(npc.optString("attitude")).append(".\n");p.append("Mantenha idade aparente, traços, pele/pelagem/escamas, cabelo, barba, olhos, roupas, equipamento e marcas distintivas consistentes em qualquer futura imagem deste mesmo NPC.");
            sharePortraitPromptWithGemini(p.toString(),name);setStatus("Prompt de retrato enviado ao Gemini para "+name+". Compartilhe a imagem de volta ao Braseiro.");
        }catch(Exception e){AppLog.e("NpcPortrait","Falha ao preparar retrato persistente.",e);toast("Retrato do NPC: "+e.getMessage());}
    }
    private boolean hasPendingPortraitTarget(){
        if(prefs==null)return false;String type=prefs.getString("pending_image_target_type","");long id=prefs.getLong("pending_image_target_id",0);
        if(id>0&&("HERO".equals(type)||"NPC".equals(type)))return true;
        return prefs.getLong("pending_npc_portrait_id",0)>0;
    }
    private void clearPendingPortraitTarget(){if(prefs!=null)prefs.edit().remove("pending_image_target_type").remove("pending_image_target_id").remove("pending_image_target_stable_id").remove("pending_image_target_name").remove("pending_npc_portrait_id").remove("pending_npc_portrait_stable_id").remove("pending_npc_portrait_name").apply();}
    private Uri storeHeroAvatar(Uri source,long characterId){try{return ImageStorageManager.importImage(this,source,"character_avatars","hero_"+characterId,80);}catch(Exception e){AppLog.e("ImageImport","Falha ao armazenar avatar do herói "+characterId,e);return null;}}
    private void importReceivedPortraitAsync(final Uri source){
        String type=prefs.getString("pending_image_target_type","");long genericId=prefs.getLong("pending_image_target_id",0);
        if((type==null||type.length()==0||genericId<=0)&&prefs.getLong("pending_npc_portrait_id",0)>0){type="NPC";genericId=prefs.getLong("pending_npc_portrait_id",0);}
        final String targetType=type;final long id=genericId;final String expectedStable=prefs.getString("pending_image_target_stable_id",prefs.getString("pending_npc_portrait_stable_id",""));final String name=prefs.getString("pending_image_target_name",prefs.getString("pending_npc_portrait_name","personagem"));
        if(id<=0||(!"HERO".equals(targetType)&&!"NPC".equals(targetType))){clearPendingPortraitTarget();return;}
        setStatus("Salvando retrato persistente de "+name+"…");
        AppExecutors.io().execute(new Runnable(){public void run(){String failure="";Uri stored=null;try{
            if("HERO".equals(targetType)){JSONObject hero=db.getCharacter(id);if(hero.length()==0)throw new Exception("A ficha do herói não existe mais.");stored=storeHeroAvatar(source,id);if(stored==null)throw new Exception("A imagem do herói não pôde ser armazenada.");db.setCharacterToken(id,stored.toString());}
            else{JSONObject npc=db.getNpcMemory(id);if(npc.length()==0||!expectedStable.equals(npc.optString("stable_id"))||npc.optInt("active",1)==0)throw new Exception("A identidade do NPC mudou ou não está mais ativa.");JSONObject stats;try{stats=new JSONObject(npc.optString("stats_json","{}"));}catch(Exception parse){stats=new JSONObject();}if(stats.optBoolean("dead",false)||"dead".equalsIgnoreCase(stats.optString("status"))||"morto".equalsIgnoreCase(stats.optString("status")))throw new Exception("O NPC morreu antes da importação; o retrato foi recusado.");stored=storeNpcAvatar(source,id);if(stored==null)throw new Exception("A imagem do NPC não pôde ser armazenada.");db.setNpcToken(id,stored.toString());}
        }catch(Exception e){failure=String.valueOf(e.getMessage());}final String error=failure;ui.post(new Runnable(){public void run(){if(error.length()>0){toast("Retrato: "+error);setStatus("Retrato não importado.");return;}clearPendingPortraitTarget();screenCache.remove("Mestre IA");synchronizeVisualSceneCast();toast("Retrato de "+name+" salvo e vinculado somente a este "+("HERO".equals(targetType)?"herói":"NPC")+".");setStatus("Retrato persistente de "+name+" atualizado.");if("Mestre IA".equals(currentScreen))showNarrator();}});}});
    }

''' + anchor
t=once(t,anchor,portrait,"portrait-methods")

# Route any returned shared image to the exact pending HERO/NPC target before scene image.
t=t.replace('''if(stored!=null){applyReceivedSceneImage(stored);return;}''','''if(stored!=null){if(hasPendingPortraitTarget()){importReceivedPortraitAsync(stored);return;}applyReceivedSceneImage(stored);return;}''',1)
t=t.replace('''if(!Intent.ACTION_SEND.equals(action)){Uri recovered=PendingImageImportStore.claim(this,"");if(recovered!=null){applyReceivedSceneImage(recovered);return;}}''','''if(!Intent.ACTION_SEND.equals(action)){Uri recovered=PendingImageImportStore.claim(this,"");if(recovered!=null){if(hasPendingPortraitTarget()){importReceivedPortraitAsync(recovered);return;}applyReceivedSceneImage(recovered);return;}}''',1)
old='''        final Uri source=incomingImageUri(intent);consumeIncomingIntent(intent);if(source==null)return;
        // Compatibilidade com remetentes que contornam a Activity dedicada.
        setStatus("Validando e salvando a imagem recebida…");'''
new='''        final Uri source=incomingImageUri(intent);consumeIncomingIntent(intent);if(source==null)return;
        if(hasPendingPortraitTarget()){importReceivedPortraitAsync(source);return;}
        // Compatibilidade com remetentes que contornam a Activity dedicada.
        setStatus("Validando e salvando a imagem recebida…");'''
t=once(t,old,new,"portrait-route-direct")

write("MainActivity.java",t)

# ------------------------------------------------------------------
# Suggestions: never force generic filler. Empty is valid.
# ------------------------------------------------------------------
t=read("SceneOptionDiversifier.java")
pat=r'public static JSONArray ensureAlways\(JSONArray source, JSONArray recentHistory,\s*String narration, String declaredAction,\s*JSONObject scene, int beatNumber\) \{/\*BR_JSON_WRAP\*/try\{.*?return out;\s*\}catch\(RuntimeException e\)\{throw e;\}catch\(Exception e\)\{throw new IllegalStateException\(e\);\}\}'
m=re.search(pat,t,re.S)
if not m: raise SystemExit("suggestion-ensureAlways-anchor-missing")
replacement='''public static JSONArray ensureAlways(JSONArray source, JSONArray recentHistory,
                                         String narration, String declaredAction,
                                         JSONObject scene, int beatNumber) {/*BR_JSON_WRAP*/try{
        // RUN182: zero opções é um resultado válido. Nunca preencha a UI com ações
        // universais que não nasceram de entidade/fato realmente presente.
        return diversify(source,recentHistory,narration,declaredAction,scene,beatNumber);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}'''
t=t[:m.start()]+replacement+t[m.end():]
write("SceneOptionDiversifier.java",t)

# ------------------------------------------------------------------
# Offline master: compact, concrete, scene-bound; no essay about game-mastering.
# ------------------------------------------------------------------
t=read("OfflineMasterEngine.java")
pat=r'if\(presentation\)\{.*?\}\s*else\{'
m=re.search(pat,t,re.S)
if not m: raise SystemExit("offline-presentation-anchor-missing")
presentation=r'''if(presentation){
            if(opening)text.append("A chegada a ").append(location).append(" ganha forma pelo que já pode ser percebido agora. ");
            else text.append("Ao alcançar ").append(location).append(", a companhia encontra o lugar já em movimento. ");
            if(weather.length()>0)text.append("O tempo está ").append(lowerFirst(weather)).append(", e isso altera a luz, os sons e o uso do espaço. ");
            if(catalog.length()>0){String fact=naturalizeCatalog(catalog,localWater);if(fact.length()>0)text.append(clean(fact,320)).append(' ');}
            if(npc.length()>0)text.append(npc).append(" está entre as pessoas que podem ser individualizadas na cena. ");
            if(danger.length()>0)text.append("Há um sinal concreto de perigo: ").append(lowerFirst(danger)).append(". ");
            if(objective.length()>0)text.append("O objetivo já estabelecido continua sendo ").append(lowerFirst(objective)).append(". ");
            text.append("O restante permanece para ser descoberto pelas ações da companhia.");
        }else{'''
t=t[:m.start()]+presentation+t[m.end():]

pat=r'private static JSONArray options\(String location,String npc,String danger,String objective,String action\)throws Exception\{/\*BR_JSON_WRAP\*/try\{.*?\}catch\(RuntimeException e\)\{throw e;\}catch\(Exception e\)\{throw new IllegalStateException\(e\);\}\}'
m=re.search(pat,t,re.S)
if not m: raise SystemExit("offline-options-anchor-missing")
options=r'''private static JSONArray options(String location,String npc,String danger,String objective,String action)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        if(npc.length()>0)out.put(option("FALAR COM "+npc.toUpperCase(Locale.ROOT),"Falo com "+npc+" sobre o que está acontecendo agora."));
        if(danger.length()>0&&out.length()<2)out.put(option("REAGIR A "+clean(danger,60).toUpperCase(Locale.ROOT),"Mudo minha posição ou preparo uma resposta ao perigo já visível."));
        if(objective.length()>0&&out.length()<2)out.put(option("AVANÇAR NO OBJETIVO","Dou um passo concreto em direção a "+objective+"."));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}'''
t=t[:m.start()]+options+t[m.end():]
write("OfflineMasterEngine.java",t)

# ------------------------------------------------------------------
# Master Orchestrator: inject canonical runtime direction + hero-control/order contract.
# ------------------------------------------------------------------
t=read("MasterOrchestrator.java")
# Put contract in the game bus / compact context when possible.
if 'HumanMasterNarrativeProfile.contract()' not in t:
    candidates=['root.put("game_bus",bus);','context.put("game_bus",bus);','out.put("game_bus",bus);']
    injected=False
    for c in candidates:
        if c in t:
            t=t.replace(c,c[:-1]+'.put("human_master_profile",HumanMasterNarrativeProfile.contract());',1)
            injected=True
            break
    if not injected:
        # Safe fallback: append instruction to plan text/prompt if bus assembly uses another variable name.
        m=re.search(r'(String prompt\s*=\s*)(action\s*\+)',t)
        if m:
            t=t[:m.start()]+m.group(1)+'HumanMasterNarrativeProfile.systemInstruction()+"\\n\\n"+'+m.group(2)+t[m.end():]
        else:
            print("WARN: master bus/prompt injection fallback unavailable; HumanMaster class remains used by offline/UI contracts")
write("MasterOrchestrator.java",t)

# ------------------------------------------------------------------
# Volo PT-BR label if RUN181 cached source missed the earlier patch.
# ------------------------------------------------------------------
t=read("DndPtBrLocalizer.java")
if "Manual de Sub-raças de Volo" not in t:
    anchor='''        add("INVENTORY AND EQUIPMENT", "INVENTÁRIO E EQUIPAMENTO");
'''
    if anchor in t:
        t=t.replace(anchor,'''        add("Volo's Complete Subrace Handbook", "Manual Completo de Sub-raças de Volo");
        add("Volo Subrace Handbook", "Manual de Sub-raças de Volo");
        add("Subrace Handbook", "Manual de Sub-raças");
'''+anchor,1)
write("DndPtBrLocalizer.java",t)

# ------------------------------------------------------------------
# Gemini remote-first connectivity: a saved key must reach the real API.
# Local bookkeeping must never silently pretend that Gemini is offline.
# ------------------------------------------------------------------
t=read("GeminiService.java")
old='''        if(kind==Kind.TEXT){ordered.add("gemini-3.5-flash-lite");ordered.add("gemini-3.6-flash");ordered.add("gemini-3.5-flash");ordered.add("gemini-3.1-flash-lite");}'''
new='''        if(kind==Kind.TEXT){ordered.add("gemini-3.8-flash");ordered.add("gemini-3.7-flash");ordered.add("gemini-3.6-flash");ordered.add("gemini-3.5-flash-lite");ordered.add("gemini-3.5-flash");ordered.add("gemini-3.1-flash-lite");}'''
if old in t:
    t=t.replace(old,new,1)
write("GeminiService.java",t)

t=read("MainActivity.java")

# The adventure must try Gemini whenever a real key exists. These local quota
# counters remain diagnostic only; real 429/auth/network errors come from Google.
hard_block='''                    if(masterOrchestrator!=null&&!masterOrchestrator.canCall()){runOfflineMaster(mode,userText,localHits,"limite remoto diário atingido");return;}
                    if(aiQuotaGovernor!=null&&!aiQuotaGovernor.canCall(pendingAiInputChars)){runOfflineMaster(mode,userText,localHits,"orçamento remoto insuficiente para esta cena");return;}
'''
if hard_block in t:
    t=t.replace(hard_block,'''                    // RUN182 REMOTE-FIRST: não bloquear a tentativa real por orçamento local.
                    // Telemetria/cota local continua sendo registrada, mas somente a API confirma 429/auth/rede.
''',1)

# The explicit connection test must also reach the network instead of being
# rejected by a stale local counter.
test_budget='''if(aiQuotaGovernor!=null&&!aiQuotaGovernor.canCall(testSystem.length()+testPrompt.length())){toast("O orçamento local de hoje foi preservado.");return;}'''
if test_budget in t:
    t=t.replace(test_budget,'',1)

# Persist a truthful ONLINE/OFFLINE state for the narration path.
success_old='''                        public void onSuccess(final String text){ui.post(new Runnable(){public void run(){if(requestId!=aiRequestGeneration)return;acceptOrRepairNarration(mode,userText,text,sys,key,model,requestId,0);}});}'''
success_new='''                        public void onSuccess(final String text){ui.post(new Runnable(){public void run(){if(requestId!=aiRequestGeneration)return;prefs.edit().putString("gemini_connection_state","online").putLong("gemini_last_ok_at",System.currentTimeMillis()).remove("gemini_last_error").apply();acceptOrRepairNarration(mode,userText,text,sys,key,model,requestId,0);}});}'''
if success_old in t:
    t=t.replace(success_old,success_new,1)

error_old='''                        public void onError(final String error){ui.post(new Runnable(){public void run(){
                            if(requestId!=aiRequestGeneration)return;
                            String streamProblem=narrationQualityProblem(streamingNarration,userText);'''
error_new='''                        public void onError(final String error){ui.post(new Runnable(){public void run(){
                            if(requestId!=aiRequestGeneration)return;
                            prefs.edit().putString("gemini_connection_state","offline").putString("gemini_last_error",error==null?"erro desconhecido":error).apply();
                            String streamProblem=narrationQualityProblem(streamingNarration,userText);'''
if error_old in t:
    t=t.replace(error_old,error_new,1)

missing_old='''                    if(key.trim().length()<20){runOfflineMaster(mode,userText,localHits,"chave Gemini ausente");return;}'''
missing_new='''                    if(key.trim().length()<20){prefs.edit().putString("gemini_connection_state","offline").putString("gemini_last_error","chave Gemini não salva").apply();runOfflineMaster(mode,userText,localHits,"chave Gemini não salva; abra CONFIG e use SALVAR E TESTAR GEMINI");return;}'''
if missing_old in t:
    t=t.replace(missing_old,missing_new,1)

offline_old='''            try{handleAiResponse(response.toString(),action);setStatus("Resposta preparada pelo Mestre local, sem depender de conexão externa.");}'''
offline_new='''            try{handleAiResponse(response.toString(),action);setStatus("Mestre local usado porque o Gemini não respondeu: "+(reason==null||reason.trim().length()==0?"motivo não informado":reason));}'''
if offline_old in t:
    t=t.replace(offline_old,offline_new,1)

status_old='''    private String textModelStatus(){String configured=prefs==null?"":prefs.getString("text_model","gemini-3.5-flash-lite").trim();String actual=gemini==null?"":gemini.getLastWorkingTextModel().trim();if(actual.length()==0)actual=configured;if(configured.length()==0||actual.equalsIgnoreCase(configured))return "Gemini "+actual;return "Gemini "+actual+" (fallback; configurado: "+configured+")";}'''
status_new='''    private String textModelStatus(){String configured=prefs==null?"":prefs.getString("text_model","gemini-3.5-flash-lite").trim();String state=prefs==null?"unknown":prefs.getString("gemini_connection_state","unknown");if(!"online".equals(state))return "Gemini OFFLINE • configurado: "+configured;String actual=gemini==null?"":gemini.getLastWorkingTextModel().trim();if(actual.length()==0)actual=configured;if(configured.length()==0||actual.equalsIgnoreCase(configured))return "Gemini ONLINE • "+actual;return "Gemini ONLINE • "+actual+" (fallback; configurado: "+configured+")";}'''
if status_old in t:
    t=t.replace(status_old,status_new,1)

# The existing RUN182 save control is now explicitly save+test aware.
t=t.replace('button("TESTAR IA — 1 CHAMADA",PANEL2)','button("SALVAR E TESTAR GEMINI",PANEL2)',1)

save_msg='''if(saveGeminiSecretsAtomically(primary,alternate)){toast("Chaves Gemini salvas no Android Keystore.");setStatus("Chave Gemini salva e aplicada.");}'''
save_msg_new='''if(saveGeminiSecretsAtomically(primary,alternate)){prefs.edit().putString("gemini_connection_state","saved_not_tested").remove("gemini_last_error").apply();toast("Chaves Gemini salvas no Android Keystore.");setStatus("Chave Gemini salva. Toque em SALVAR E TESTAR GEMINI para confirmar a conexão.");}'''
if save_msg in t:
    t=t.replace(save_msg,save_msg_new,1)

# Add a visible connection line just below the save button.
save_add='''ai.addView(saveGeminiKeys);ai.addView(text("A chave alternativa só é usada pelo Charon'''
if save_add in t:
    t=t.replace(save_add,'''ai.addView(saveGeminiKeys);ai.addView(text(geminiConnectionStatusText(),12,"online".equals(prefs.getString("gemini_connection_state",""))?GREEN:GOLD,true));ai.addView(text("A chave alternativa só é usada pelo Charon''',1)

# Mark the dedicated test result as authoritative.
test_success='''public void onSuccess(final String value){ui.post(new Runnable(){public void run(){if(aiQuotaGovernor!=null)aiQuotaGovernor.recordCall(testSystem.length()+testPrompt.length(),value==null?0:value.length());toast(value);setStatus("IA conectada • "+gemini.getLastWorkingTextModel());}});}'''
test_success_new='''public void onSuccess(final String value){ui.post(new Runnable(){public void run(){if(aiQuotaGovernor!=null)aiQuotaGovernor.recordCall(testSystem.length()+testPrompt.length(),value==null?0:value.length());prefs.edit().putString("gemini_connection_state","online").putLong("gemini_last_ok_at",System.currentTimeMillis()).remove("gemini_last_error").apply();toast(value);setStatus("GEMINI ONLINE • "+gemini.getLastWorkingTextModel());showSettings();}});}'''
if test_success in t:
    t=t.replace(test_success,test_success_new,1)

test_error='''public void onError(final String error){ui.post(new Runnable(){public void run(){if(aiQuotaGovernor!=null)aiQuotaGovernor.recordCall(testSystem.length()+testPrompt.length(),0);showFriendlyAiError(error,false,null);setStatus("Teste da IA falhou.");}});}'''
test_error_new='''public void onError(final String error){ui.post(new Runnable(){public void run(){if(aiQuotaGovernor!=null)aiQuotaGovernor.recordCall(testSystem.length()+testPrompt.length(),0);prefs.edit().putString("gemini_connection_state","offline").putString("gemini_last_error",error==null?"erro desconhecido":error).apply();showFriendlyAiError(error,false,null);setStatus("GEMINI OFFLINE • "+(error==null?"erro desconhecido":error));showSettings();}});}'''
if test_error in t:
    t=t.replace(test_error,test_error_new,1)

# Helper for the settings card.
settings_anchor='''    private void showSettings(){
'''
if 'private String geminiConnectionStatusText()' not in t and settings_anchor in t:
    helper='''    private String geminiConnectionStatusText(){
        String state=prefs==null?"unknown":prefs.getString("gemini_connection_state","unknown");
        String configured=prefs==null?"":prefs.getString("text_model","gemini-3.5-flash-lite").trim();
        String actual=gemini==null?"":gemini.getLastWorkingTextModel().trim();
        String error=prefs==null?"":prefs.getString("gemini_last_error","");
        if("online".equals(state))return "● GEMINI ONLINE • "+(actual.length()>0?actual:configured);
        if("saved_not_tested".equals(state))return "● CHAVE SALVA • conexão ainda não testada";
        return "● GEMINI OFFLINE"+(error==null||error.trim().length()==0?"":" • "+error.trim());
    }

'''
    t=t.replace(settings_anchor,helper+settings_anchor,1)

write("MainActivity.java",t)

# ------------------------------------------------------------------
# Static acceptance contracts.
# ------------------------------------------------------------------
checks={
 "human_master_class":(pkg/"HumanMasterNarrativeProfile.java").exists(),
 "startup_surface":"showImmediateStartupSurface();" in read("MainActivity.java"),
 "tts_deferred":"postDelayed(new Runnable(){public void run(){ensureNativeTtsInitialized();}},350L)" in read("MainActivity.java"),
 "gemini_save_button":"SALVAR CHAVES GEMINI" in read("MainActivity.java"),
 "gemini_test_saves":"saveGeminiSecretsAtomically(k,paidKey.getText().toString().trim())" in read("MainActivity.java"),
 "gemini_remote_first":'limite remoto diário atingido' not in read("MainActivity.java") and 'orçamento remoto insuficiente para esta cena' not in read("MainActivity.java"),
 "gemini_online_status":'gemini_connection_state' in read("MainActivity.java") and 'GEMINI ONLINE' in read("MainActivity.java"),
 "gemini_explicit_fallback_reason":'Mestre local usado porque o Gemini não respondeu' in read("MainActivity.java"),
 "character_control_ui":"Sistema — rotina / combate assistido" in read("MainActivity.java"),
 "character_control_persist":"db.saveCharacterControl(id,controlMode" in read("MainActivity.java"),
 "npc_portrait_click":"beginNpcPortrait(portraitEntry)" in read("MainActivity.java"),
 "hero_portrait_click":"beginHeroPortrait(portraitEntry)" in read("MainActivity.java"),
 "portrait_generic_pending":'pending_image_target_type' in read("MainActivity.java") and 'pending_image_target_id' in read("MainActivity.java"),
 "hero_portrait_persist":"db.setCharacterToken(id,stored.toString())" in read("MainActivity.java"),
 "npc_portrait_persist":"db.setNpcToken(id,stored.toString())" in read("MainActivity.java"),
 "portrait_not_scene":"if(hasPendingPortraitTarget()){importReceivedPortraitAsync(source);return;}" in read("MainActivity.java"),
 "portrait_token_safe":"ombros para cima" in read("MainActivity.java") and "TOKEN CIRCULAR" in read("MainActivity.java"),
 "npc_tombstone":"deadTombstone" in read("AppDb.java") and "markNpcDead" in read("AppDb.java"),
 "zero_suggestions_allowed":"zero opções é um resultado válido" in read("SceneOptionDiversifier.java"),
 "offline_no_generic_fill":'CRIAR OUTRA ABORDAGEM' not in read("OfflineMasterEngine.java"),
 "volo_ptbr":"Manual de Sub-raças de Volo" in read("DndPtBrLocalizer.java"),
}
print(json.dumps(checks,ensure_ascii=False,indent=2))
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("RUN182_STATIC_FAIL::"+",".join(bad))
print("RUN182_PATCH_OK")
