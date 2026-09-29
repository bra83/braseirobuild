from pathlib import Path
import json
import re
import shutil
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
ASSETS = ROOT / "android/app/src/main/assets/pathfinder/kingmaker"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 anchor, found {count}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# 1) Canonical staged Restov / Jamandi prelude
# ---------------------------------------------------------------------------
shutil.copy2(HERE / "KingmakerPreludePolicy.java", JAVA / "KingmakerPreludePolicy.java")

repo_path = JAVA / "KingmakerRepository.java"
repo = read(repo_path)
old_campaign_phase = r'''    public String campaignStoryPhase(){
        String phase=state().optString("campaign_story_phase",KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL).trim().toLowerCase(Locale.ROOT);
        if(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(phase)
                ||KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX.equals(phase)
                ||KingmakerPreludePolicy.PHASE_JAMANDI_MANSION.equals(phase)
                ||KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)
                ||KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION.equals(phase))return phase;
        return KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL;
    }

    public boolean mainAdventureStarted(){return state().optInt("main_adventure_started",0)==1;}

    public JSONObject resetCampaignPrelude(){
        return setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL,false);
    }

    public JSONObject setCampaignStoryPhase(String phase,boolean mainStarted){
        String next=phase==null?"":phase.trim().toLowerCase(Locale.ROOT);
        if(!(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(next)
                ||KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX.equals(next)
                ||KingmakerPreludePolicy.PHASE_JAMANDI_MANSION.equals(next)
                ||KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(next)
                ||KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION.equals(next)))
            throw new IllegalArgumentException("Fase narrativa Kingmaker inválida: "+phase);
        ContentValues v=new ContentValues();
        v.put("campaign_story_phase",next);
        v.put("main_adventure_started",mainStarted?1:0);
        v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});
        return state();
    }

'''
new_campaign_phase = r'''    public String campaignStoryPhase(){
        String phase=state().optString("campaign_story_phase",KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL).trim().toLowerCase(Locale.ROOT);
        // Migração da R33: o estado antigo "jamandi_mansion" passa a representar
        // apenas a aproximação/entrada, nunca uma mansão inteira já resolvida.
        if("jamandi_mansion".equals(phase)){
            ContentValues migration=new ContentValues();
            migration.put("campaign_story_phase",KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH);
            migration.put("main_adventure_started",1);
            migration.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_state",migration,"world_profile=?",new String[]{profile()});
            phase=KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH;
        }
        return KingmakerPreludePolicy.isKnownPhase(phase)?phase:KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL;
    }

    public boolean mainAdventureStarted(){return state().optInt("main_adventure_started",0)==1;}

    public JSONObject resetCampaignPrelude(){
        JSONObject out=setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL,false);
        ensurePreludeQuestState(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL);
        return out;
    }

    public JSONObject setCampaignStoryPhase(String phase,boolean mainStarted){
        String next=phase==null?"":phase.trim().toLowerCase(Locale.ROOT);
        if("jamandi_mansion".equals(next))next=KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH;
        if(!KingmakerPreludePolicy.isKnownPhase(next))
            throw new IllegalArgumentException("Fase narrativa Kingmaker inválida: "+phase);
        ContentValues v=new ContentValues();
        v.put("campaign_story_phase",next);
        v.put("main_adventure_started",mainStarted?1:0);
        v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});
        ensurePreludeQuestState(next);
        return state();
    }

    private void ensurePreludeQuestState(String phase){
        try{
            String p=phase==null?KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL:phase;
            boolean completed=KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(p)||KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION.equals(p);
            JSONObject progress=new JSONObject()
                    .put("quest_key","q_call_for_heroes")
                    .put("chapter_key","chapter_00_call_for_heroes")
                    .put("campaign_story_phase",p)
                    .put("sandbox_before_mansion",true)
                    .put("canonical_gate","charter_before_stolen_lands");
            db.applyQuestUpdate(new JSONObject()
                    .put("title","Convocação de Heróis — Restov")
                    .put("objective",completed?"A convocação foi respondida e a carta de exploração foi aceita.":"Viver Restov livremente e responder à convocação de Lady Jamandi quando o grupo decidir.")
                    .put("stakes","A campanha principal de Kingmaker só começa por escolha explícita do jogador e segue seus marcos antes da expedição.")
                    .put("status",completed?"completed":"active")
                    .put("stage",KingmakerPreludePolicy.titleForPhase(p))
                    .put("progress_json",progress.toString()));
        }catch(Exception error){
            AppLog.w("KingmakerPrelude","Não foi possível sincronizar a missão estruturada do prólogo.",error);
        }
    }

'''
repo = replace_once(repo, old_campaign_phase, new_campaign_phase, "repository staged prelude")

old_default_companions = "        ensureDefaultCompanions();\n"
new_default_companions = (
    "        ensureDefaultCompanions();\n"
    "        try{JSONObject preludeState=db.queryOne(\"SELECT campaign_story_phase FROM kingmaker_state WHERE world_profile=?\",new String[]{profile()});"
    "ensurePreludeQuestState(preludeState.optString(\"campaign_story_phase\",KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL));}"
    "catch(Exception error){AppLog.w(\"KingmakerPrelude\",\"Prólogo será sincronizado na próxima atualização de fase.\",error);}\n"
)
repo = replace_once(repo, old_default_companions, new_default_companions, "ensure structured prelude quest")
write(repo_path, repo)

# ---------------------------------------------------------------------------
# 2) MainActivity: persistent Restov arrival, staged mansion, Atlas anchor,
#    major scenes, exact FL Kokoro flow, and character-token return routing.
# ---------------------------------------------------------------------------
main_path = JAVA / "MainActivity.java"
main = read(main_path)

main = replace_once(
    main,
    '    private static final String PREF_PENDING_NPC_PROMPT="pending_image_npc_prompt_v1";',
    '    private static final String PREF_PENDING_NPC_PROMPT="pending_image_npc_prompt_v1";\n'
    '    private static final String PREF_PENDING_CHARACTER_ID="pending_image_character_id_v1";',
    "character image preference",
)

# Character Gemini buttons must mark a character target, never a scene target.
main = main.replace(
    'pendingTokenCharacterId=String.valueOf(id);sharePromptToGemini(PathfinderTokenPromptBuilder.build(mergeVisualCharacter(ch,sheet),NarrativeLocationNaturalizer.currentNarrativeLabel(db)));',
    'pendingTokenCharacterId=String.valueOf(id);sharePromptToGeminiForCharacter(PathfinderTokenPromptBuilder.build(mergeVisualCharacter(ch,sheet),NarrativeLocationNaturalizer.currentNarrativeLabel(db)),id);'
)
main = main.replace(
    'prompt.setOnClickListener(new View.OnClickListener(){public void onClick(View v){sharePromptToGemini(PathfinderTokenPromptBuilder.build(merged,NarrativeLocationNaturalizer.currentNarrativeLabel(db)));}});',
    'prompt.setOnClickListener(new View.OnClickListener(){public void onClick(View v){sharePromptToGeminiForCharacter(PathfinderTokenPromptBuilder.build(merged,NarrativeLocationNaturalizer.currentNarrativeLabel(db)),character.optLong("id"));}});'
)

old_share = r'''    private void sharePromptToGemini(String prompt){sharePromptToGeminiTarget(prompt,"scene",0L,"");}
    private void sharePromptToGeminiForNpc(String prompt,long npcId,String npcName){sharePromptToGeminiTarget(prompt,"npc",npcId,npcName);}
    private void sharePromptToGeminiTarget(String prompt,String target,long npcId,String npcName){
        lastPrompt=prompt;SharedPreferences.Editor pending=prefs.edit().putString(PREF_PENDING_IMAGE_TARGET,target==null?"scene":target);
        if("npc".equals(target)){pending.remove(PREF_PENDING_IMAGE_FINGERPRINT).putLong(PREF_PENDING_NPC_ID,npcId).putString(PREF_PENDING_NPC_NAME,npcName==null?"":npcName).putString(PREF_PENDING_NPC_PROMPT,prompt==null?"":prompt);}
        else pending.putString(PREF_PENDING_IMAGE_FINGERPRINT,currentSceneVisualFingerprint()).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT);
        pending.apply();Intent x=new Intent(Intent.ACTION_SEND);x.setType("text/plain");x.putExtra(Intent.EXTRA_TEXT,prompt);x.setPackage("com.google.android.apps.bard");try{startActivity(x);setStatus("Gemini aberto. Depois compartilhe a imagem de volta ao Braseiro Pathfinder 2e.");}catch(ActivityNotFoundException e){Intent fallback=new Intent(Intent.ACTION_SEND);fallback.setType("text/plain");fallback.putExtra(Intent.EXTRA_TEXT,prompt);startActivity(Intent.createChooser(fallback,"Escolha o Gemini"));setStatus("Escolha o Gemini e compartilhe a imagem de volta depois.");}}
'''
new_share = r'''    private void sharePromptToGemini(String prompt){sharePromptToGeminiTarget(prompt,"scene",0L,"");}
    private void sharePromptToGeminiForNpc(String prompt,long npcId,String npcName){sharePromptToGeminiTarget(prompt,"npc",npcId,npcName);}
    private void sharePromptToGeminiForCharacter(String prompt,long characterId){sharePromptToGeminiTarget(prompt,"character",characterId,"");}
    private void sharePromptToGeminiTarget(String prompt,String target,long targetId,String npcName){
        String resolvedTarget=("npc".equals(target)||"character".equals(target))?target:"scene";
        lastPrompt=prompt;SharedPreferences.Editor pending=prefs.edit().putString(PREF_PENDING_IMAGE_TARGET,resolvedTarget);
        if("npc".equals(resolvedTarget)){
            pending.remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_CHARACTER_ID)
                    .putLong(PREF_PENDING_NPC_ID,targetId).putString(PREF_PENDING_NPC_NAME,npcName==null?"":npcName)
                    .putString(PREF_PENDING_NPC_PROMPT,prompt==null?"":prompt);
        }else if("character".equals(resolvedTarget)){
            pending.remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT)
                    .putLong(PREF_PENDING_CHARACTER_ID,targetId);
        }else{
            pending.putString(PREF_PENDING_IMAGE_FINGERPRINT,currentSceneVisualFingerprint())
                    .remove(PREF_PENDING_CHARACTER_ID).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT);
        }
        pending.apply();Intent x=new Intent(Intent.ACTION_SEND);x.setType("text/plain");x.putExtra(Intent.EXTRA_TEXT,prompt);x.setPackage("com.google.android.apps.bard");
        try{startActivity(x);setStatus("Gemini aberto. A imagem compartilhada de volta será vinculada exclusivamente a "+("character".equals(resolvedTarget)?"este personagem":("npc".equals(resolvedTarget)?"este NPC":"esta cena"))+".");}
        catch(ActivityNotFoundException e){Intent fallback=new Intent(Intent.ACTION_SEND);fallback.setType("text/plain");fallback.putExtra(Intent.EXTRA_TEXT,prompt);startActivity(Intent.createChooser(fallback,"Escolha o Gemini"));setStatus("Escolha o Gemini e compartilhe a imagem de volta depois.");}
    }
'''
main = replace_once(main, old_share, new_share, "separate Gemini image targets")

old_received = '    private void applyReceivedImage(Uri stored){String target=prefs.getString(PREF_PENDING_IMAGE_TARGET,"scene");if("npc".equals(target)){applyReceivedNpcImage(stored);return;}applyReceivedSceneImage(stored);}'
new_received = r'''    private void applyReceivedImage(Uri stored){
        String target=prefs.getString(PREF_PENDING_IMAGE_TARGET,"scene");
        if("npc".equals(target)){applyReceivedNpcImage(stored);return;}
        if("character".equals(target)){applyReceivedCharacterImage(stored);return;}
        applyReceivedSceneImage(stored);
    }
    private void applyReceivedCharacterImage(Uri stored){
        if(stored==null||!ImageStorageManager.isReadable(this,stored)){setStatus("A imagem recebida não está mais disponível.");return;}
        final long characterId=prefs.getLong(PREF_PENDING_CHARACTER_ID,0L);
        final JSONObject character=characterId>0?db.getCharacter(characterId):new JSONObject();
        if(character.length()==0){setStatus("O personagem associado à imagem não existe mais.");clearPendingImageTarget();toast("Imagem não vinculada à cena.");return;}
        final Uri source=stored;final String name=character.optString("name","Personagem");
        setStatus("Salvando token de "+name+"…");
        AppExecutors.io().execute(new Runnable(){public void run(){try{
            final Uri avatar=ImageStorageManager.importImage(MainActivity.this,source,"hero_avatars","hero_"+characterId+"_gemini",80);
            db.setCharacterToken(characterId,avatar.toString());
            ui.post(new Runnable(){public void run(){pendingTokenCharacterId="";clearPendingImageTarget();screenCache.remove("Personagens");screenCache.remove("Combate");screenCache.remove("Mestre IA");setStatus("Imagem salva exclusivamente como token de "+name+".");toast("Token de "+name+" atualizado.");if("Personagens".equals(currentScreen))showHeroes();}});
        }catch(final Exception error){AppLog.e("CharacterPortrait","Falha ao persistir token recebido.",error);ui.post(new Runnable(){public void run(){setStatus("A imagem do personagem não pôde ser salva.");toast(String.valueOf(error.getMessage()));}});}}});
    }'''
main = replace_once(main, old_received, new_received, "route received character image")

main = replace_once(
    main,
    '    private void clearPendingImageTarget(){prefs.edit().remove(PREF_PENDING_IMAGE_TARGET).remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT).apply();}',
    '    private void clearPendingImageTarget(){prefs.edit().remove(PREF_PENDING_IMAGE_TARGET).remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_CHARACTER_ID).remove(PREF_PENDING_NPC_ID).remove(PREF_PENDING_NPC_NAME).remove(PREF_PENDING_NPC_PROMPT).apply();}',
    "clear character image target",
)

old_restov = r'''    private JSONObject prepareKingmakerRestovStart(boolean reset)throws Exception{
        if(kingmakerRepository!=null&&(reset||!KingmakerPreludePolicy.isPreExpeditionPhase(kingmakerRepository.campaignStoryPhase())))
            kingmakerRepository.resetCampaignPrelude();
        JSONObject map=db.getMapState();
        String packageId=map.optString("map_package_id","");
        if(packageId.length()>0&&db.isMapPackageUsable(packageId)){
            db.saveMapPackageState(map.optString("image_uri"),map.optString("overlay_uri"),map.optString("manifest_json"),packageId,"",
                    map.optInt("overlay_visible",1)==1,map.optString("reveals_json","{}"),map.optString("grid_type","Quadrada"),
                    (float)map.optDouble("grid_size",64),map.optInt("fog_enabled",0)==1,(float)map.optDouble("token_x",.5),
                    (float)map.optDouble("token_y",.5),(float)map.optDouble("scale",1),(float)map.optDouble("offset_x",0),(float)map.optDouble("offset_y",0));
            db.saveMapTravelState(false,"","[]","Kingmaker — prólogo em Restov");
        }
        long seed=System.nanoTime()^System.currentTimeMillis()^activeWorldProfile().hashCode();
        String daypart=KingmakerPreludePolicy.randomDaypart(seed);
        JSONObject scene=new JSONObject();
        scene.put("day_number",1);
        scene.put("location","Restov, Rostland, Brevoy");
        scene.put("hex_id","");
        scene.put("time_of_day",daypart);
        scene.put("clock_hour",KingmakerPreludePolicy.hourForDaypart(daypart));
        scene.put("weather",KingmakerPreludePolicy.randomWeather(seed));
        scene.put("lighting",KingmakerPreludePolicy.lightingForDaypart(daypart));
        scene.put("danger","");
        scene.put("objective","Entrar em Restov e decidir livremente o que fazer na cidade.");
        scene.put("present_npcs",new JSONArray());
        scene.put("threats",new JSONArray());
        scene.put("clues",new JSONArray());
        scene.put("pending_consequences",new JSONArray());
        db.saveSceneState(scene);
        db.addAuditEvent("kingmaker_prologo","Chegada preparada em Restov",daypart+" • "+scene.optString("weather"),"{}",scene.toString(),true);
        return new JSONObject().put("hex_id","").put("start_location_name","Restov, Rostland, Brevoy").put("atlas_optional",true).put("map_grounded",false).put("prelude","restov");
    }
'''
new_restov = r'''    private JSONObject prepareKingmakerRestovStart(boolean reset)throws Exception{
        if(kingmakerRepository!=null&&(reset||!KingmakerPreludePolicy.isPreExpeditionPhase(kingmakerRepository.campaignStoryPhase())))
            kingmakerRepository.resetCampaignPrelude();
        JSONObject scene=db.getSceneState();
        boolean alreadyInRestov=!reset&&scene.optString("location","").toLowerCase(Locale.ROOT).contains("restov")
                &&scene.has("clock_hour")&&scene.optString("weather","").trim().length()>0;
        long seed=prefs.getLong("kingmaker_restov_arrival_seed",0L);
        if(seed==0L||reset){seed=System.nanoTime()^System.currentTimeMillis()^activeWorldProfile().hashCode();prefs.edit().putLong("kingmaker_restov_arrival_seed",seed).apply();}
        if(!alreadyInRestov){
            String daypart=KingmakerPreludePolicy.randomDaypart(seed);
            scene=new JSONObject();
            scene.put("day_number",1);
            scene.put("location","Restov, Rostland, Brevoy");
            scene.put("hex_id","");
            scene.put("time_of_day",daypart);
            scene.put("clock_hour",KingmakerPreludePolicy.hourForDaypart(daypart));
            scene.put("weather",KingmakerPreludePolicy.randomWeather(seed));
            scene.put("lighting",KingmakerPreludePolicy.lightingForDaypart(daypart));
            scene.put("danger","");
            scene.put("objective","Entrar em Restov e decidir livremente o que fazer na cidade.");
            scene.put("present_npcs",new JSONArray());
            scene.put("threats",new JSONArray());
            scene.put("clues",new JSONArray());
            scene.put("pending_consequences",new JSONArray());
            db.saveSceneState(scene);
            db.addAuditEvent("kingmaker_prologo","Chegada preparada em Restov",daypart+" • "+scene.optString("weather"),"{}",scene.toString(),true);
        }else{
            initializeSceneClock(); // retoma o relógio persistido; nunca sorteia novamente clima/horário.
        }
        JSONObject map=db.getMapState();String packageId=map.optString("map_package_id","");
        if(packageId.length()>0&&db.isMapPackageUsable(packageId))db.saveMapTravelState(false,"","[]","Kingmaker — Restov (âncora urbana; hexploração ainda não iniciada)");
        prefs.edit().putString("kingmaker_atlas_mode","restov_city").putString("kingmaker_atlas_anchor_name","Restov, Rostland, Brevoy").apply();
        return new JSONObject().put("hex_id","").put("start_location_name","Restov, Rostland, Brevoy").put("atlas_optional",false).put("map_grounded",true).put("atlas_mode","urban_anchor").put("prelude","restov");
    }
'''
main = replace_once(main, old_restov, new_restov, "persistent Restov arrival")

old_action_start = main.index('    private String prepareKingmakerPreludeAction(String raw){')
old_action_end = main.index('    private void prepareStolenLandsDepartureAtlas(){', old_action_start)
new_action = r'''    private String prepareKingmakerPreludeAction(String raw){
        if(kingmakerRepository==null)return raw==null?"":raw;
        String text=raw==null?"":raw;
        try{
            String phase=kingmakerRepository.campaignStoryPhase();
            if(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(phase)&&db.getStoryState().optInt("started",0)==1){
                kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX,false);
                phase=KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX;
            }
            if(KingmakerPreludePolicy.isRestovPhase(phase)&&KingmakerPreludePolicy.isMansionIntent(text)){
                phase=KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH;
                kingmakerRepository.setCampaignStoryPhase(phase,true);
                JSONObject scene=db.getSceneState();
                scene.put("location","Restov — caminho para a Mansão de Lady Jamandi Aldori");
                scene.put("hex_id","");
                scene.put("objective","Chegar à mansão de Lady Jamandi e responder à Convocação de Heróis.");
                db.saveSceneState(scene);
                prefs.edit().putString("kingmaker_atlas_anchor_name","Mansão de Lady Jamandi Aldori, Restov").apply();
                db.addAuditEvent("kingmaker_prologo","Campanha principal iniciada","Jogador escolheu ir à mansão de Lady Jamandi","{}",scene.toString(),true);
                return text+"\n\n"+KingmakerPreludePolicy.directiveForPhase(phase);
            }
            if(KingmakerPreludePolicy.isMansionPhase(phase)){
                String next=KingmakerPreludePolicy.nextMansionPhaseForAction(phase,text);
                if(!next.equals(phase)){
                    boolean expedition=KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION.equals(next);
                    kingmakerRepository.setCampaignStoryPhase(next,true);
                    phase=next;
                    if(expedition)prepareStolenLandsDepartureAtlas();
                }
            }
            String directive=KingmakerPreludePolicy.directiveForPhase(phase);
            return directive.length()==0?text:(text+"\n\n"+directive);
        }catch(Exception error){AppLog.w("KingmakerPrelude","Falha ao atualizar fase narrativa; preservando ação original.",error);return text;}
    }

    private void continueKingmakerMansionFromUi(){
        if(kingmakerRepository==null){navigate("Mestre IA");return;}
        try{
            String phase=kingmakerRepository.campaignStoryPhase();
            if(KingmakerPreludePolicy.PHASE_JAMANDI_ATTACK.equals(phase)){
                if(db.getCombatState().optInt("running",0)==1){toast("Conclua o encontro atual antes de avançar o ataque à mansão.");navigate("Combate");return;}
                if(!prefs.getBoolean("kingmaker_mansion_attack_combat_finished",false)){
                    navigate("Mestre IA");sendAi("Narrador","Continue o ataque à mansão a partir do ponto atual. Ainda não avance para as consequências finais: resolva primeiro as ameaças e encontros que ainda estiverem em curso.","PLAYER_ACTION");return;
                }
                kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_JAMANDI_AFTER_ATTACK,true);
                phase=KingmakerPreludePolicy.PHASE_JAMANDI_AFTER_ATTACK;
            }else{
                String next=KingmakerPreludePolicy.nextMansionPhaseForContinue(phase);
                if(!next.equals(phase)){kingmakerRepository.setCampaignStoryPhase(next,true);phase=next;}
            }
            navigate("Mestre IA");
            sendAi("Narrador","Continue o arco da Convocação de Heróis exatamente do marco persistido agora. Não resuma nem salte acontecimentos futuros.\n\n"+KingmakerPreludePolicy.directiveForPhase(phase),"PLAYER_ACTION");
        }catch(Exception error){toast("Kingmaker: "+safeMessage(error));}
    }

    private void markKingmakerMansionCombatStartedIfNeeded(){
        try{if(kingmakerRepository!=null&&KingmakerPreludePolicy.PHASE_JAMANDI_ATTACK.equals(kingmakerRepository.campaignStoryPhase()))
            prefs.edit().putBoolean("kingmaker_mansion_attack_combat_started",true).putBoolean("kingmaker_mansion_attack_combat_finished",false).apply();}
        catch(Exception ignored){}
    }
    private void markKingmakerMansionCombatFinishedIfNeeded(){
        try{if(kingmakerRepository!=null&&KingmakerPreludePolicy.PHASE_JAMANDI_ATTACK.equals(kingmakerRepository.campaignStoryPhase())
                &&prefs.getBoolean("kingmaker_mansion_attack_combat_started",false))
            prefs.edit().putBoolean("kingmaker_mansion_attack_combat_finished",true).apply();}
        catch(Exception ignored){}
    }

'''
main = main[:old_action_start] + new_action + main[old_action_end:]

main = replace_once(
    main,
    '            db.addAuditEvent("kingmaker_prologo","Expedição iniciada","Restov → Terras Roubadas; H_OLEG é destino canônico, não teleporte","{}",scene.toString(),true);',
    '            prefs.edit().putString("kingmaker_atlas_mode","stolen_lands").putString("kingmaker_atlas_anchor_name","Estrada ao sul de Restov — rumo às Terras Roubadas").apply();\n'
    '            db.addAuditEvent("kingmaker_prologo","Expedição iniciada","Restov → Terras Roubadas; H_OLEG é destino canônico, não teleporte","{}",scene.toString(),true);',
    "departure Atlas mode",
)

old_start_mansion = r'''    private void startJamandiMansionFromUi(){
        navigate("Mestre IA");
        sendAi("Narrador","O jogador decide ir agora à mansão de Lady Jamandi Aldori para responder à convocação. Narre a aproximação e a entrada naturalmente; não resuma acontecimentos futuros.","PLAYER_ACTION");
    }
'''
new_start_mansion = r'''    private void startJamandiMansionFromUi(){
        try{
            if(kingmakerRepository!=null&&KingmakerPreludePolicy.isRestovPhase(kingmakerRepository.campaignStoryPhase()))
                kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH,true);
            prefs.edit().putString("kingmaker_atlas_anchor_name","Mansão de Lady Jamandi Aldori, Restov").apply();
        }catch(Exception error){AppLog.w("KingmakerPrelude","Não foi possível persistir a ida à mansão antes da narração.",error);}
        navigate("Mestre IA");
        sendAi("Narrador","O jogador decide ir agora à mansão de Lady Jamandi Aldori para responder à convocação. Narre a aproximação e a entrada naturalmente; não resuma acontecimentos futuros.\n\n"+KingmakerPreludePolicy.directiveForPhase(KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH),"PLAYER_ACTION");
    }
'''
main = replace_once(main, old_start_mansion, new_start_mansion, "explicit mansion start")

main = replace_once(
    main,
    'if("jamandi_mansion".equals(key)){navigate("Mestre IA");sendAi("Narrador","Continue a sequência atual da Convocação de Heróis na mansão de Lady Jamandi a partir do ponto exato persistido; não salte para Oleg ou para um acontecimento futuro.","PLAYER_ACTION");return;}',
    'if("jamandi_mansion".equals(key)){continueKingmakerMansionFromUi();return;}',
    "staged mansion UI",
)

# Solo display uses the actual persisted stage name.
main = main.replace(
    '"Continuar a aventura na mansão de Lady Jamandi"',
    '"Continuar a aventura na mansão de Lady Jamandi"'
)

# Atlas: show the campaign anchor instead of pretending Restov is an empty/random hex.
atlas_anchor = r'''        try{
            if(kingmakerRepository!=null&&KingmakerPreludePolicy.isPreExpeditionPhase(kingmakerRepository.campaignStoryPhase())){
                LinearLayout anchorCard=card();anchorCard.setBackground(makeBg(Color.rgb(35,31,24),GOLD,2));
                String anchor=prefs.getString("kingmaker_atlas_anchor_name","Restov, Rostland, Brevoy");
                anchorCard.addView(text("LOCAL DA CAMPANHA • "+anchor,15,GOLD,true));
                anchorCard.addView(text("Kingmaker está no prólogo urbano de Restov. O Atlas permanece sincronizado com a ficção sem inventar um hex das Terras Roubadas. A hexploração e as rotas do Cinturão Verde só são ativadas quando o grupo aceitar a carta e decidir partir.",12,TEXT,false));
                root.addView(anchorCard);
            }
        }catch(Exception error){AppLog.w("KingmakerAtlas","Não foi possível desenhar a âncora urbana do prólogo.",error);}
'''
anchor_point = '        worldBar.addView(text("ATLAS • PATHFINDER 2E",15,GOLD,true));worldBar.addView(text("Um único perfil cartográfico. Imagem, hexes, manifesto, POIs, rotas e névoa compartilham a mesma identidade Pathfinder.",12,TEXT,false));root.addView(worldBar);\n'
main = replace_once(main, anchor_point, anchor_point + atlas_anchor, "Restov Atlas anchor card")

main = replace_once(
    main,
    '        final TextView currentHexInfo=text("Hex atual: —",14,GOLD,true);currentHexInfo.setPadding(dp(6),0,dp(6),dp(5));root.addView(currentHexInfo);',
    '        String atlasCurrentLabel="Hex atual: —";try{if(kingmakerRepository!=null&&KingmakerPreludePolicy.isPreExpeditionPhase(kingmakerRepository.campaignStoryPhase()))atlasCurrentLabel="Local atual: "+prefs.getString("kingmaker_atlas_anchor_name","Restov, Rostland, Brevoy")+" • modo urbano";}catch(Exception ignored){}\n'
    '        final TextView currentHexInfo=text(atlasCurrentLabel,14,GOLD,true);currentHexInfo.setPadding(dp(6),0,dp(6),dp(5));root.addView(currentHexInfo);',
    "Atlas current Restov label",
)

# Major Kingmaker scenes override ordinary exploration length, but never inflate combat beats.
old_pace = '    private String narrationPaceInstruction(){String mode=currentMasterSceneMode();boolean economy=masterOrchestrator!=null&&masterOrchestrator.economyMode();if("conversa".equals(mode))return '
new_pace = '    private String narrationPaceInstruction(){String mode=currentMasterSceneMode();boolean economy=masterOrchestrator!=null&&masterOrchestrator.economyMode();try{if(!"combate".equals(mode)&&kingmakerRepository!=null&&KingmakerPreludePolicy.isMajorScenePhase(kingmakerRepository.campaignStoryPhase()))return "CENA PRINCIPAL KINGMAKER: narre com aproximadamente 5.000 a 6.000 caracteres quando a cena sustentar essa densidade; use câmera narrativa, sensorialidade funcional, pessoas agindo e geografia jogável; preserve agência e pare em uma decisão real. Esta regra sobrescreve o limite comum de exploração."; }catch(Exception ignored){}if("conversa".equals(mode))return '
if main.count(old_pace) != 1:
    raise RuntimeError("major scene pace anchor missing")
main = main.replace(old_pace, new_pace, 1)

# Forbidden Lands runtime behavior: each short Kokoro segment is an isolated
# QUEUE_FLUSH speak call. The next segment starts only from onDone.
main = replace_once(
    main,
    '            int queueMode=(neural&&nativeNarrationQueueIndex>0)?TextToSpeech.QUEUE_ADD:TextToSpeech.QUEUE_FLUSH;',
    '            int queueMode=TextToSpeech.QUEUE_FLUSH; // Forbidden Lands: cada segmento curto é uma chamada isolada; onDone dispara o próximo.',
    "Kokoro FL queue semantics",
)

# Mark mansion-attack combat lifecycle without changing generic PF2 combat behavior.
main = main.replace(
    'db.setCombatRunningChecked(true);}',
    'db.setCombatRunningChecked(true);markKingmakerMansionCombatStartedIfNeeded();}',
    1
)
main = replace_once(
    main,
    'db.resetCombatChecked();summary.append(summary.length()>0?"\\n":"").append("Combate encerrado automaticamente — ").append(reason);',
    'db.resetCombatChecked();markKingmakerMansionCombatFinishedIfNeeded();summary.append(summary.length()>0?"\\n":"").append("Combate encerrado automaticamente — ").append(reason);',
    "mansion combat completion",
)
# Manual end button also counts as a resolved encounter while the staged attack is active.
main = main.replace('db.resetCombat();prefs.edit().remove("last_combat_narration")', 'db.resetCombat();markKingmakerMansionCombatFinishedIfNeeded();prefs.edit().remove("last_combat_narration")', 1)

# Settings text describes actual isolated FL calls.
main = main.replace(
    'Primeiro segmento QUEUE_FLUSH, seguintes QUEUE_ADD, avanço somente após onDone real.',
    'Cada segmento Android é enviado isoladamente com QUEUE_FLUSH; o próximo só começa após onDone real, como no Forbidden Lands.'
)

# New adventures must clear every pending image target and Restov arrival seed.
main = main.replace(
    '.remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_RECENT_SCENE_OPTIONS)',
    '.remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_PENDING_IMAGE_TARGET).remove(PREF_PENDING_CHARACTER_ID).remove(PREF_PENDING_NPC_ID).remove("kingmaker_restov_arrival_seed").remove("kingmaker_mansion_attack_combat_started").remove("kingmaker_mansion_attack_combat_finished").remove(PREF_RECENT_SCENE_OPTIONS)',
    1
)

write(main_path, main)

# ---------------------------------------------------------------------------
# 3) Solo director: expose the exact staged phase, never a generic whole mansion.
# ---------------------------------------------------------------------------
solo_path = JAVA / "KingmakerSoloDirector.java"
solo = read(solo_path)
solo = solo.replace(
    '.put("title",KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)?"Preparar a partida de Restov":"Continuar a Convocação de Heróis")',
    '.put("title",KingmakerPreludePolicy.titleForPhase(phase))'
)
solo = solo.replace(
    '.put("chapter_title", "Convocação de Heróis — Mansão de Lady Jamandi")',
    '.put("chapter_title", KingmakerPreludePolicy.titleForPhase(storyPhase))'
)
solo = solo.replace(
    'steps.put(step("jamandi_mansion",KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)?"Preparar a saída de Restov":"Continuar a aventura na mansão de Lady Jamandi",',
    'steps.put(step("jamandi_mansion",KingmakerPreludePolicy.titleForPhase(phase),'
)
write(solo_path, solo)

# ---------------------------------------------------------------------------
# 4) Structured Chapter 0 metadata + campaign structural audit hooks.
# ---------------------------------------------------------------------------
prelude = {
    "schema": "braseiro-pathfinder2e-chapter-v1",
    "chapter_key": "chapter_00_call_for_heroes",
    "chapterKey": "chapter_00_call_for_heroes",
    "chapter_number": 0,
    "chapterNumber": 0,
    "title": "Prólogo — Convocação de Heróis em Restov",
    "locale": "pt-BR",
    "level_min": 1,
    "levelMin": 1,
    "level_max": 1,
    "levelMax": 1,
    "summary": "Prólogo estruturado que mantém Restov como sandbox até a escolha explícita de ir à mansão de Lady Jamandi e então bloqueia saltos até a carta e a partida para as Terras Roubadas.",
    "metadata": {
        "urban_anchor": "Restov, Rostland, Brevoy",
        "sandbox_before_mansion": True,
        "canonical_gate": "charter_before_stolen_lands",
        "runtime_authority": "KingmakerPreludePolicy"
    },
    "quests": [{
        "quest_key": "q_call_for_heroes",
        "questKey": "q_call_for_heroes",
        "title": "Convocação de Heróis — Restov",
        "status": "active",
        "stage": "restov_sandbox",
        "objective": "Explorar Restov livremente e responder à convocação de Lady Jamandi apenas quando o grupo decidir.",
        "stakes": "Ir à mansão inicia a campanha principal; a expedição não pode começar antes da carta.",
        "owner_character_id": 0,
        "faction_id": 0,
        "milestone_xp": 0,
        "milestoneXp": 0,
        "prerequisites": [],
        "objectives": [
            {"id": "obj_call_1", "desc": "Chegar a Restov", "description": "Chegar a Restov", "status": "pending", "completed": False},
            {"id": "obj_call_2", "desc": "Escolher quando ir à mansão de Lady Jamandi", "description": "Escolher quando ir à mansão de Lady Jamandi", "status": "pending", "completed": False},
            {"id": "obj_call_3", "desc": "Concluir os marcos da mansão e aceitar ou recusar a carta", "description": "Concluir os marcos da mansão e aceitar ou recusar a carta", "status": "pending", "completed": False}
        ],
        "rewards": {},
        "progress_json": {"runtime_phase_authority": "kingmaker_state.campaign_story_phase"}
    }],
    "hazards": [], "pois": [], "tactical_scenes": [], "npcs": [], "factions": [], "clocks": [], "events": [], "milestones": []
}
write(ASSETS / "chapters/chapter_00_call_for_heroes.json", json.dumps(prelude, ensure_ascii=False, indent=2) + "\n")

catalog_path = ASSETS / "kingmaker_catalog.json"
catalog = json.loads(read(catalog_path))
catalog["campaign_prelude"] = {
    "chapter": "pathfinder/kingmaker/chapters/chapter_00_call_for_heroes.json",
    "urban_anchor": "Restov, Rostland, Brevoy",
    "runtime_authority": "KingmakerPreludePolicy",
    "stolen_lands_unlock_phase": "stolen_lands_expedition"
}
write(catalog_path, json.dumps(catalog, ensure_ascii=False, indent=2) + "\n")

# Chapter model permits the explicit zero-numbered prologue.
models_path = JAVA / "KingmakerCampaignModels.java"
models = read(models_path)
models = replace_once(
    models,
    '            if (levelMin < 1 || levelMax > 20 || levelMin > levelMax) {',
    '            if (chapterNumber < 0) throw new IllegalArgumentException("chapter_number inválido: " + chapterNumber);\n'
    '            if (levelMin < 1 || levelMax > 20 || levelMin > levelMax) {',
    "allow chapter zero while rejecting negative chapters",
)
write(models_path, models)

# ---------------------------------------------------------------------------
# 5) Version identity
# ---------------------------------------------------------------------------
version_path = ROOT / "version.properties"
version = read(version_path).replace("VERSION_NAME=9.0.34", "VERSION_NAME=9.0.35").replace("VERSION_CODE=90034", "VERSION_CODE=90035")
write(version_path, version)

# Keep historical identity tests behavioral, not pinned to R33.
tests_dir = ROOT / "tests_pf2"
for path in tests_dir.glob("test_*.py"):
    legacy = read(path)
    updated = legacy.replace("VERSION_NAME=9.0.34", "VERSION_NAME=9.0.35").replace("VERSION_CODE=90034", "VERSION_CODE=90035")
    if updated != legacy:
        write(path, updated)

print("PF2 R34 closure patch applied successfully.")
