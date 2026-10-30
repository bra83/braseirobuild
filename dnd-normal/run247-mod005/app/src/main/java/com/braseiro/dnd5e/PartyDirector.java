package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Coordena vários protagonistas sem gastar chamadas de IA. */
public final class PartyDirector {
    private final AppDb db;
    public PartyDirector(AppDb database){db=database;}

    public long queueAction(long characterId,String action){
        JSONObject hero=db.getCharacter(characterId);if(hero.length()==0||hero.optInt("active",1)!=1||action==null||action.trim().length()==0)return -1;
        String clean=action.trim();if(clean.length()>1200)clean=clean.substring(0,1200);return db.addPartyAction(characterId,hero.optString("name","Aventureiro"),clean,inferIntent(clean));
    }

    public JSONArray queuedActions(){return new JSONArray(db.listPartyActions("queued",40));}
    /** Heróis que estão fisicamente na companhia agora; personagens afastados continuam ativos no mundo. */
    public List<JSONObject> activeHeroRoster(){return new java.util.ArrayList<JSONObject>(rosterHeroes());}
    public boolean hasQueuedActions(){return db.countPartyActions("queued")>0;}
    public void clearResolved(){db.deletePartyActionsByStatus("resolved");}
    public void clearQueue(){db.deletePartyActionsByStatus("queued");}

    /** Roster persistente: a companhia pode ser formada depois de Strixhaven sem recriar a campanha. */
    public JSONArray activeRoster(){
        ensureFounderMembership();
        JSONArray out=new JSONArray();
        for(JSONObject row:db.listPartyMemberships("active",80)){
            try{
                JSONObject x=new JSONObject(row.toString());
                if("hero".equalsIgnoreCase(row.optString("actor_kind"))){
                    JSONObject hero=db.getCharacter(row.optLong("character_id"));
                    if(hero.length()==0)continue;
                    x.put("name",hero.optString("name","Aventureiro")).put("class_name",hero.optString("class_name")).put("level",hero.optInt("level",1)).put("hp",hero.optInt("hp")).put("max_hp",hero.optInt("max_hp"));
                }else{
                    JSONObject npc=db.findNpcMemoryByName(row.optString("npc_name"));
                    x.put("name",row.optString("npc_name")).put("npc_active",npc.optInt("active",1)==1).put("location",npc.optString("last_seen",row.optString("home_location")));
                }
                out.put(x);
            }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha ao montar membro da companhia.",ignored);}
        }
        return out;
    }

    /** Explicit invitation: player and NPC must both opt in while present on campus. */
    public JSONObject inviteStrixhavenClassmate(String npcName,boolean playerInvited,
                                                 boolean npcAccepted)throws Exception{
        return new KvotheCampaignPartyBridge(db).inviteCampusNpc(npcName,playerInvited,npcAccepted);
    }

    /** Player-selected three existing character sheets at D040; not a scripted teleport. */
    public JSONObject recruitKvotheFriendsAtMarco(long wilemId,long simmonId,long auriId,
                                                   boolean playerConfirmed,
                                                   boolean allAccepted)throws Exception{
        return new KvotheCampaignPartyBridge(db).recruitAtMarco(
            wilemId,simmonId,auriId,playerConfirmed,allAccepted);
    }

    /** Canonical NPC-to-hero transfer; only possible after a consented D040 meeting. */
    public JSONObject recruitKvotheFriendsAtMarco(boolean playerConfirmed,
                                                  boolean allAccepted)throws Exception{
        return new KvotheCampaignPartyBridge(db).recruitAtMarco(playerConfirmed,allAccepted);
    }

    /** Unique in-world feat: XP is awarded to one recruited companion only,
     * never to Kvothe or other party members by association. */
    public JSONObject awardKvotheFriendIndividualXp(long characterId,int xp,
            String confirmedFeat,String receipt)throws Exception{
        return StrixhavenFriends2024.awardIndependentHeroXp(
            db,characterId,xp,confirmedFeat,receipt);
    }

    public JSONObject kvotheCampaignPolicy(){
        return new KvotheCampaignPartyBridge(db).context();
    }

    public long recruitHero(long characterId,String recruitmentType)throws Exception{
        ensureFounderMembership();
        JSONObject hero=db.getCharacter(characterId);if(hero.length()==0)throw new IllegalArgumentException("Herói inexistente.");
        long membership=db.upsertPartyMembership(new JSONObject().put("actor_kind","hero").put("character_id",characterId).put("status","active")
                .put("party_role","companion").put("recruitment_type",cleanRecruitmentType(recruitmentType)).put("joined_day",currentDay())
                .put("home_location",db.getSceneState().optString("location")).put("loyalty",100).put("reason","Entrou para a companhia."));
        // The existing user-facing recruitment action must honor player control.
        if(membership>0 && !KvotheCampaignPartyBridge.inCampus(db.getSceneState())
                && KvotheCampaignPartyBridge.atMarco(db.getSceneState(),db.getMapState())
                && ("Wilem".equalsIgnoreCase(hero.optString("name"))
                    || "Simmon".equalsIgnoreCase(hero.optString("name"))
                    || "Auri".equalsIgnoreCase(hero.optString("name")))){
            db.saveCharacterControl(characterId,"manual","{}");
        }
        return membership;
    }

    public long recruitNpc(String npcName,String recruitmentType,JSONObject compensation)throws Exception{
        ensureFounderMembership();
        String name=npcName==null?"":npcName.trim();if(name.length()==0)throw new IllegalArgumentException("NPC inválido.");
        JSONObject npc=db.findNpcMemoryByName(name);if(npc.length()==0)throw new IllegalArgumentException("O NPC precisa existir no Mundo Vivo antes de ser recrutado.");
        JSONObject pay=compensation==null?new JSONObject():new JSONObject(compensation.toString());
        long id=db.upsertPartyMembership(new JSONObject().put("actor_kind","npc").put("npc_name",npc.optString("name",name)).put("status","active")
                .put("party_role","companion").put("recruitment_type",cleanRecruitmentType(recruitmentType)).put("joined_day",currentDay())
                .put("home_location",npc.optString("last_seen",db.getSceneState().optString("location"))).put("compensation_json",pay.toString())
                .put("loyalty",initialNpcLoyalty(recruitmentType)).put("reason","Recrutado dentro do mundo persistente."));
        return id;
    }

    public boolean releaseMember(long membershipId,String homeLocation,String reason){
        for(JSONObject row:db.listPartyMemberships(null,200)){if(row.optLong("id")!=membershipId)continue;
            db.updatePartyMembershipStatus(membershipId,"away",homeLocation==null?row.optString("home_location"):homeLocation,reason==null?"Saiu da companhia, mas continua existindo no Mundo Vivo.":reason);return true;}
        return false;
    }

    public boolean releaseHero(long characterId,String homeLocation,String reason){
        if(characterId<=0)return false;
        for(JSONObject row:db.listPartyMemberships(null,200)){
            if(!"hero".equalsIgnoreCase(row.optString("actor_kind"))||row.optLong("character_id")!=characterId)continue;
            long membershipId=row.optLong("id");
            db.updatePartyMembershipStatus(membershipId,"away",homeLocation==null?row.optString("home_location"):homeLocation,reason==null?"Saiu da companhia, mas continua existindo no Mundo Vivo.":reason);
            return true;
        }
        return false;
    }

    public boolean reactivateMember(long membershipId)throws Exception{
        for(JSONObject row:db.listPartyMemberships(null,200)){if(row.optLong("id")!=membershipId)continue;db.updatePartyMembershipStatus(membershipId,"active",row.optString("home_location"),"Retornou à companhia.");return true;}return false;
    }

    public boolean reactivateHero(long characterId)throws Exception{
        if(characterId<=0)return false;
        for(JSONObject row:db.listPartyMemberships(null,200)){
            if(!"hero".equalsIgnoreCase(row.optString("actor_kind"))||row.optLong("character_id")!=characterId)continue;
            db.updatePartyMembershipStatus(row.optLong("id"),"active",row.optString("home_location"),"Retornou à companhia.");
            return true;
        }
        return false;
    }

    public JSONArray availableHeroesToRecruit(){
        ensureFounderMembership();JSONArray out=new JSONArray();for(JSONObject hero:db.listActiveCharacters())if(db.findActivePartyHero(hero.optLong("id")).length()==0)out.put(hero);return out;
    }

    public JSONArray availableNpcsToRecruit(int limit){
        JSONArray out=new JSONArray();JSONArray known=db.listKnownNpcs(Math.max(1,Math.min(300,limit)));for(int i=0;i<known.length()&&out.length()<limit;i++){JSONObject npc=known.optJSONObject(i);if(npc==null||npc.optInt("active",1)!=1)continue;String name=npc.optString("name");if(name.trim().length()==0||db.findActivePartyNpc(name).length()>0)continue;out.put(npc);}return out;
    }

    public void ensureFounderMembership(){
        if(!db.listPartyMemberships(null,1).isEmpty())return;List<JSONObject> heroes=db.listActiveCharacters();if(heroes.isEmpty())return;
        try{
            JSONObject first=heroes.get(0);
            for(JSONObject candidate:heroes){
                long cid=candidate.optLong("id",Long.MAX_VALUE);
                long fid=first.optLong("id",Long.MAX_VALUE);
                if(cid>0&&cid<fid)first=candidate;
            }
            db.upsertPartyMembership(new JSONObject().put("actor_kind","hero").put("character_id",first.optLong("id")).put("status","active").put("party_role","protagonist").put("recruitment_type","founder").put("joined_day",currentDay()).put("loyalty",100).put("reason","Protagonista inicial da campanha."));
        }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha ao registrar protagonista fundador.",ignored);}
    }

    public String buildActionBatch(String fallback){
        JSONArray actions=queuedActions();if(actions.length()==0)return fallback==null?"":fallback.trim();
        StringBuilder b=new StringBuilder("DECLARAÇÕES SIMULTÂNEAS DA COMPANHIA:\n");
        for(int i=0;i<actions.length()&&i<20;i++){JSONObject a=actions.optJSONObject(i);if(a==null)continue;String declared=a.optString("action_text");if(declared.length()>900)declared=declared.substring(0,900);b.append(i+1).append(". ").append(a.optString("character_name")).append(": ").append(declared).append("\n");}
        if(fallback!=null&&fallback.trim().length()>0)b.append("ORIENTAÇÃO GERAL DO JOGADOR: ").append(fallback.trim()).append("\n");
        b.append("Resolva as ações na ordem causal correta. Ações compatíveis podem ocorrer juntas; ações dependentes aguardam o resultado anterior. Não decida novas ações importantes pelos heróis.");
        return b.toString();
    }

    public JSONArray queuedActionIds(){JSONArray ids=new JSONArray();JSONArray rows=queuedActions();for(int i=0;i<Math.min(20,rows.length());i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&row.optLong("id",0)>0)ids.put(row.optLong("id"));}return ids;}
    public void markQueuedNarrated(JSONArray actionIds){db.markPartyActionsNarrated(actionIds);}

    public JSONObject compactState(){
        JSONObject out=new JSONObject();try{
            out.put("party",db.getPartyState());out.put("roster",activeRoster());
            JSONArray heroes=new JSONArray();for(JSONObject h:rosterHeroes()){
                JSONObject x=new JSONObject();x.put("id",h.optLong("id"));x.put("name",h.optString("name"));x.put("class",h.optString("class_name"));x.put("level",h.optInt("level",1));x.put("hp",h.optInt("hp"));x.put("max_hp",h.optInt("max_hp"));x.put("ac",h.optInt("ac"));x.put("control",db.getCharacterControl(h.optLong("id")));heroes.put(x);
            }
            out.put("heroes",heroes);out.put("queued_actions",queuedActions());out.put("company_resources",parseObject(db.getPartyState().optString("company_resources_json","{}")));out.put("subgroups",parseArray(db.getPartyState().optString("subgroups_json","[]")));
        }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha tolerada; a execução continuou.",ignored);}return out;
    }

    public String compactPrompt(){return compactStateForPrompt(5600).toString();}

    /** Contrato de iniciativa derivado dos modos persistidos de cada herói. */
    public JSONObject controlContract(){
        JSONObject out=new JSONObject();JSONArray heroes=new JSONArray();
        try{
            for(JSONObject h:rosterHeroes()){
                long id=h.optLong("id");String mode=CompanionAgencyEngine.mode(db.getCharacterControl(id).optString("control_mode","manual"));
                heroes.put(new JSONObject().put("character_id",id).put("name",h.optString("name")).put("mode",mode)
                        .put("initiative", "companion".equals(mode)?"motivada_pela_ficha":"curated".equals(mode)?"proposta_para_aprovacao":"somente_comando_do_jogador"));
            }
            out.put("heroes",heroes).put("curated_requires_approval",true).put("manual_requires_player_command",true)
                    .put("limited_resources_require_confirmation",true);
        }catch(Exception failure){AppLog.e("PartyDirector","Falha ao montar contrato de controle.",failure);}
        return out;
    }

    public JSONObject compactStateForPrompt(int maxChars){
        JSONObject out=new JSONObject();try{
            JSONObject state=db.getPartyState();JSONObject party=new JSONObject();party.put("marching_order",parseArray(state.optString("marching_order_json","[]")));party.put("roles",parseObject(state.optString("roles_json","{}")));party.put("company_resources",parseObject(state.optString("company_resources_json","{}")));party.put("subgroups",parseArray(state.optString("subgroups_json","[]")));party.put("relations",parseArray(state.optString("relations_json","[]")));party.put("session_preferences",parseObject(state.optString("session_preferences_json","{}")));party.put("knowledge_mode",state.optString("knowledge_mode","jogador_total"));party.put("default_control_mode",CompanionAgencyEngine.mode(state.optString("default_control_mode","manual")));out.put("party",party);
            out.put("kvothe_strixhaven_party_policy",kvotheCampaignPolicy());
            out.put("roster",activeRoster());JSONArray heroes=new JSONArray();for(JSONObject h:rosterHeroes()){
                JSONObject control=db.getCharacterControl(h.optLong("id"));JSONObject x=new JSONObject();x.put("id",h.optLong("id"));x.put("name",h.optString("name"));x.put("class",h.optString("class_name"));x.put("level",h.optInt("level",1));x.put("hp",h.optInt("hp"));x.put("max_hp",h.optInt("max_hp"));x.put("ac",h.optInt("ac"));x.put("control_mode",CompanionAgencyEngine.mode(control.optString("control_mode","manual")));x.put("tactics",parseObject(control.optString("tactics_json","{}")));x.put("personal_goal",control.optString("personal_goal"));x.put("arc_status",control.optString("arc_status","active"));x.put("arc_notes",control.optString("arc_notes"));x.put("last_major_choice",control.optString("last_major_choice"));heroes.put(x);
            }
            out.put("heroes",heroes);JSONArray actions=new JSONArray();for(JSONObject a:db.listPartyActions("queued",20)){JSONObject q=new JSONObject();q.put("character_name",a.optString("character_name"));q.put("action_text",a.optString("action_text").length()>700?a.optString("action_text").substring(0,700):a.optString("action_text"));q.put("intent_type",a.optString("intent_type"));actions.put(q);}out.put("queued_actions",actions);
            int limit=Math.max(2500,maxChars);String raw=out.toString();if(raw.length()>limit){party.remove("relations");party.remove("session_preferences");out.put("queued_actions",firstJson(actions,10));out.put("context_reduced",true);raw=out.toString();}if(raw.length()>limit){for(int i=0;i<heroes.length();i++){JSONObject hero=heroes.optJSONObject(i);if(hero!=null)hero.remove("tactics");}out.put("queued_actions",firstJson(actions,6));out.put("context_reduced",true);}
        }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha tolerada; a execução continuou.",ignored);}return out;
    }


    public JSONObject createLocalSessionMemory(){
        JSONObject result=new JSONObject();try{
            int session=db.nextSessionNumber();JSONObject scene=db.getSceneState();JSONArray threads=new JSONArray();for(JSONObject q:db.listQuests("active",20))threads.put(q.optString("title")+": "+q.optString("objective"));for(JSONObject c:db.listWorldClocks(true,20))threads.put(c.optString("name")+" "+c.optInt("current_value")+"/"+c.optInt("max_value"));
            JSONObject beats=new JSONObject();StringBuilder heroes=new StringBuilder();for(JSONObject h:rosterHeroes()){if(heroes.length()>0)heroes.append("; ");heroes.append(h.optString("name")).append(" PV ").append(h.optInt("hp")).append("/").append(h.optInt("max_hp"));JSONObject control=db.getCharacterControl(h.optLong("id"));beats.put(h.optString("name"),new JSONObject().put("spotlight",control.optInt("spotlight_score",0)).put("control",control.optString("control_mode","manual")).put("personal_goal",control.optString("personal_goal")).put("arc_status",control.optString("arc_status","active")).put("last_major_choice",control.optString("last_major_choice")));}
            String summary="A companhia encerrou a sessão em "+scene.optString("location","local não registrado")+", no dia "+Math.max(1,scene.optInt("day_number",1))+", durante "+scene.optString("time_of_day","horário indefinido")+". Estado do grupo: "+heroes+". Objetivo atual: "+scene.optString("objective","não registrado")+". Última ação: "+db.getStoryState().optString("last_action","não registrada")+".";
            long id=db.addSessionMemory(session,"Sessão "+session,summary,threads.toString(),beats.toString());result.put("id",id).put("session_no",session).put("summary",summary).put("open_threads",threads).put("character_beats",beats);
        }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha tolerada; a execução continuou.",ignored);}return result;
    }

    public String buildLocalNextSessionPrep(){
        StringBuilder b=new StringBuilder();JSONObject scene=db.getSceneState();b.append("Retomada: dia ").append(Math.max(1,scene.optInt("day_number",1))).append(", ").append(scene.optString("time_of_day","horário indefinido")).append(", em ").append(scene.optString("location","local não registrado")).append(".\n");
        List<JSONObject> quests=db.listQuests("active",8);if(!quests.isEmpty()){b.append("Missões abertas:\n");for(JSONObject q:quests)b.append("• ").append(q.optString("title")).append(": ").append(q.optString("objective")).append("\n");}
        List<JSONObject> clocks=db.listWorldClocks(true,8);if(!clocks.isEmpty()){b.append("Relógios ativos:\n");for(JSONObject c:clocks)b.append("• ").append(c.optString("name")).append(" ").append(c.optInt("current_value")).append("/").append(c.optInt("max_value")).append(" — ").append(c.optString("consequence")).append("\n");}
        List<JSONObject> factions=db.listFactions(6);if(!factions.isEmpty()){b.append("Movimentos ocultos das facções:\n");for(JSONObject f:factions)b.append("• ").append(f.optString("name")).append(": ").append(f.optString("current_plan")).append("\n");}
        String hint=nextSpotlightHint();if(hint.length()>0)b.append("Equilíbrio de protagonismo: ").append(hint).append("\n");return b.toString();
    }

    public void updateSpotlightFromNarration(String narration){
        if(narration==null)return;String lower=narration.toLowerCase(new Locale("pt","BR"));Set<Long> mentioned=new HashSet<Long>();List<JSONObject> heroes=rosterHeroes();
        for(JSONObject h:heroes){String name=h.optString("name").trim();if(name.length()>0&&lower.contains(name.toLowerCase(new Locale("pt","BR"))))mentioned.add(h.optLong("id"));}
        Set<Long> rosterIds=new HashSet<Long>();for(JSONObject h:heroes)rosterIds.add(h.optLong("id"));db.updateSpotlight(mentioned,rosterIds);
    }

    public String nextSpotlightHint(){
        JSONObject chosen=null;long oldest=Long.MAX_VALUE;int lowest=Integer.MAX_VALUE;
        for(JSONObject hero:rosterHeroes()){JSONObject control=db.getCharacterControl(hero.optLong("id"));long at=control.optLong("last_spotlight_at",0L);int score=control.optInt("spotlight_score",0);if(chosen==null||at<oldest||(at==oldest&&score<lowest)){chosen=hero;oldest=at;lowest=score;}}
        if(chosen==null)return "";JSONObject control=db.getCharacterControl(chosen.optLong("id"));String goal=control.optString("personal_goal").trim();String arc=control.optString("arc_status","active");return "O herói há mais tempo sem protagonismo relevante é "+chosen.optString("name")+". Ofereça uma oportunidade natural ligada às suas capacidades ou história, sem forçar a cena."+(goal.length()>0?" Objetivo pessoal: "+goal+" (arco "+arc+").":"");
    }

    private List<JSONObject> rosterHeroes(){
        ensureFounderMembership();List<JSONObject> all=db.listActiveCharacters();List<JSONObject> selected=new java.util.ArrayList<JSONObject>();
        List<JSONObject> memberships=db.listPartyMemberships("active",100);if(memberships.isEmpty())return all;
        Set<Long> ids=new HashSet<Long>();for(JSONObject row:memberships)if("hero".equalsIgnoreCase(row.optString("actor_kind")))ids.add(row.optLong("character_id"));
        for(JSONObject hero:all)if(ids.contains(hero.optLong("id")))selected.add(hero);return selected.isEmpty()?all:selected;
    }
    private int currentDay(){return Math.max(1,db.getSceneState().optInt("day_number",1));}
    private static int initialNpcLoyalty(String type){String n=type==null?"":type.toLowerCase(Locale.ROOT);if(n.contains("amig")||n.contains("aliad"))return 70;if(n.contains("contrat")||n.contains("merc"))return 45;return 55;}
    private static String cleanRecruitmentType(String type){String n=type==null?"joined":type.trim();return n.length()==0?"joined":(n.length()>40?n.substring(0,40):n);}

    private static String inferIntent(String action){String n=action.toLowerCase(Locale.ROOT);if(contains(n,"atac","golpe","dispar","lanço","conjuro"))return "combate";if(contains(n,"procuro","examino","investigo","observo","farejo"))return "exploracao";if(contains(n,"falo","pergunto","convers","negocio","ameaço","persu"))return "social";if(contains(n,"viajo","avanço","caminho","sigo","movo"))return "movimento";if(contains(n,"descanso","acampo","durmo"))return "descanso";return "livre";}
    private static boolean contains(String x,String... values){for(String v:values)if(x.contains(v))return true;return false;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw);}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw);}catch(Exception e){return new JSONArray();}}
    private static JSONArray firstJson(JSONArray source,int max){JSONArray out=new JSONArray();if(source!=null)for(int i=0;i<source.length()&&i<max;i++)out.put(source.opt(i));return out;}
}
