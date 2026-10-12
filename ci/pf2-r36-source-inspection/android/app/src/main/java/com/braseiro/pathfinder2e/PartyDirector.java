package com.braseiro.pathfinder2e;

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
        JSONObject hero=db.getCharacter(characterId);if(hero.length()==0||action==null||action.trim().length()==0)return -1;
        String clean=action.trim();if(clean.length()>1200)clean=clean.substring(0,1200);return db.addPartyAction(characterId,hero.optString("name","Aventureiro"),clean,inferIntent(clean));
    }

    public JSONArray queuedActions(){return new JSONArray(db.listPartyActions("queued",40));}
    public boolean hasQueuedActions(){return db.countPartyActions("queued")>0;}
    public void clearResolved(){db.deletePartyActionsByStatus("resolved");}
    public void clearQueue(){db.deletePartyActionsByStatus("queued");}

    private static boolean gmControlled(JSONObject control){return control!=null&&"gm".equalsIgnoreCase(control.optString("control_mode","player"));}
    public boolean hasGmControlledHeroes(){for(JSONObject h:db.listActiveCharacters())if(gmControlled(db.getCharacterControl(h.optLong("id"))))return true;return false;}
    public JSONArray gmControlledHeroes(){JSONArray out=new JSONArray();try{for(JSONObject h:db.listActiveCharacters()){JSONObject c=db.getCharacterControl(h.optLong("id"));if(!gmControlled(c))continue;JSONObject x=new JSONObject().put("id",h.optLong("id")).put("name",h.optString("name")).put("class",h.optString("class_name")).put("level",h.optInt("level",1)).put("control_mode","gm").put("tactics",parseObject(c.optString("tactics_json","{}"))).put("personal_goal",c.optString("personal_goal")).put("arc_status",c.optString("arc_status","active"));out.put(x);}}catch(Exception ignored){AppLog.ignored("PartyDirector","Falha ao projetar heróis do Mestre.",ignored);}return out;}
    public JSONObject controlPolicyForPrompt(){JSONObject out=new JSONObject();try{JSONArray player=new JSONArray(),gm=gmControlledHeroes();for(JSONObject h:db.listActiveCharacters()){JSONObject c=db.getCharacterControl(h.optLong("id"));if(!gmControlled(c))player.put(new JSONObject().put("id",h.optLong("id")).put("name",h.optString("name")));}out.put("mode","solo_multi_character").put("human_players",1).put("player_controlled",player).put("gm_controlled",gm).put("player_rule","O jogador decide integralmente intenção, fala e ações voluntárias destes personagens. Barbara nunca as escolhe.").put("gm_rule","Barbara decide intenção, diálogo e ação voluntária somente dos personagens explicitamente marcados MESTRE, respeitando personalidade, tática, conhecimento e estado.").put("mechanics_rule","Nenhuma decisão de Barbara altera PV, condições, inventário, recursos, spell slots, posição ou outro estado diretamente. Quando houver incerteza ou efeito mecânico, use o protocolo/motor PF2e existente (pending_test/combat/transação estruturada) e aguarde o resultado antes de narrar a consequência.");}catch(Exception ignored){AppLog.ignored("PartyDirector","Falha ao montar política de controle.",ignored);}return out;}

    public String buildActionBatch(String fallback){
        JSONArray actions=queuedActions(),gm=gmControlledHeroes();if(actions.length()==0&&gm.length()==0)return fallback==null?"":fallback.trim();
        StringBuilder b=new StringBuilder();
        if(actions.length()>0){b.append("DECLARAÇÕES DO JOGADOR JÁ REGISTRADAS:\n");for(int i=0;i<actions.length()&&i<20;i++){JSONObject a=actions.optJSONObject(i);if(a==null)continue;String declared=a.optString("action_text");if(declared.length()>900)declared=declared.substring(0,900);b.append(i+1).append(". ").append(a.optString("character_name")).append(": ").append(declared).append("\n");}}
        if(gm.length()>0){b.append("PERSONAGENS EXPLICITAMENTE SOB CONTROLE DO MESTRE:\n");for(int i=0;i<gm.length();i++){JSONObject h=gm.optJSONObject(i);if(h==null)continue;b.append("• ").append(h.optString("name")).append(" — ").append(h.optString("class")).append(" nível ").append(h.optInt("level",1));JSONObject tactics=h.optJSONObject("tactics");if(tactics!=null&&tactics.optString("instruction").trim().length()>0)b.append(" | orientação: ").append(tactics.optString("instruction").trim());if(h.optString("personal_goal").trim().length()>0)b.append(" | objetivo: ").append(h.optString("personal_goal").trim());b.append("\n");}b.append("Para estes heróis, Barbara pode escolher intenção, fala e ação voluntária coerente com a cena. Se a escolha exigir teste, gasto, dano, condição, movimento tático ou qualquer mutação, NÃO invente o resultado: encaminhe pelo mesmo protocolo/motor PF2e usado para o personagem do jogador e narre somente após o resultado.\n");}
        if(fallback!=null&&fallback.trim().length()>0)b.append("ORIENTAÇÃO / AÇÃO DO JOGADOR: ").append(fallback.trim()).append("\n");
        b.append("Resolva na ordem causal correta. Personagens marcados JOGADOR continuam sob agência integral do humano; nunca decida novas ações importantes por eles.");
        return b.toString();
    }

    public void markQueuedResolved(){db.markPartyActionsResolved();}

    public JSONObject compactState(){
        JSONObject out=new JSONObject();try{
            out.put("party",db.getPartyState());
            JSONArray heroes=new JSONArray();for(JSONObject h:db.listActiveCharacters()){
                JSONObject x=new JSONObject();x.put("id",h.optLong("id"));x.put("name",h.optString("name"));x.put("class",h.optString("class_name"));x.put("level",h.optInt("level",1));x.put("hp",h.optInt("hp"));x.put("max_hp",h.optInt("max_hp"));x.put("ac",h.optInt("ac"));x.put("control",db.getCharacterControl(h.optLong("id")));heroes.put(x);
            }
            out.put("heroes",heroes);out.put("queued_actions",queuedActions());out.put("company_resources",parseObject(db.getPartyState().optString("company_resources_json","{}")));out.put("subgroups",parseArray(db.getPartyState().optString("subgroups_json","[]")));
        }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha tolerada; a execução continuou.",ignored);}return out;
    }

    public String compactPrompt(){return compactStateForPrompt(5600).toString();}

    public JSONObject compactStateForPrompt(int maxChars){
        JSONObject out=new JSONObject();try{
            JSONObject state=db.getPartyState();JSONObject party=new JSONObject();party.put("marching_order",parseArray(state.optString("marching_order_json","[]")));party.put("roles",parseObject(state.optString("roles_json","{}")));party.put("company_resources",parseObject(state.optString("company_resources_json","{}")));party.put("subgroups",parseArray(state.optString("subgroups_json","[]")));party.put("relations",parseArray(state.optString("relations_json","[]")));party.put("session_preferences",parseObject(state.optString("session_preferences_json","{}")));party.put("knowledge_mode",state.optString("knowledge_mode","jogador_total"));party.put("default_control_mode",state.optString("default_control_mode","manual"));out.put("party",party);
            JSONArray heroes=new JSONArray();for(JSONObject h:db.listActiveCharacters()){
                JSONObject control=db.getCharacterControl(h.optLong("id"));JSONObject x=new JSONObject();x.put("id",h.optLong("id"));x.put("name",h.optString("name"));x.put("class",h.optString("class_name"));x.put("level",h.optInt("level",1));x.put("hp",h.optInt("hp"));x.put("max_hp",h.optInt("max_hp"));x.put("ac",h.optInt("ac"));x.put("control_mode",control.optString("control_mode","manual"));x.put("tactics",parseObject(control.optString("tactics_json","{}")));x.put("personal_goal",control.optString("personal_goal"));x.put("arc_status",control.optString("arc_status","active"));x.put("arc_notes",control.optString("arc_notes"));x.put("last_major_choice",control.optString("last_major_choice"));heroes.put(x);
            }
            out.put("heroes",heroes);JSONArray actions=new JSONArray();for(JSONObject a:db.listPartyActions("queued",20)){JSONObject q=new JSONObject();q.put("character_name",a.optString("character_name"));q.put("action_text",a.optString("action_text").length()>700?a.optString("action_text").substring(0,700):a.optString("action_text"));q.put("intent_type",a.optString("intent_type"));actions.put(q);}out.put("queued_actions",actions);
            int limit=Math.max(2500,maxChars);String raw=out.toString();if(raw.length()>limit){party.remove("relations");party.remove("session_preferences");out.put("queued_actions",firstJson(actions,10));out.put("context_reduced",true);raw=out.toString();}if(raw.length()>limit){for(int i=0;i<heroes.length();i++){JSONObject hero=heroes.optJSONObject(i);if(hero!=null)hero.remove("tactics");}out.put("queued_actions",firstJson(actions,6));out.put("context_reduced",true);}
        }catch(Exception ignored){AppLog.ignored("PartyDirector","Falha tolerada; a execução continuou.",ignored);}return out;
    }


    public JSONObject createLocalSessionMemory(){
        JSONObject result=new JSONObject();try{
            int session=db.nextSessionNumber();JSONObject scene=db.getSceneState();JSONArray threads=new JSONArray();for(JSONObject q:db.listQuests("active",20))threads.put(q.optString("title")+": "+q.optString("objective"));for(JSONObject c:db.listWorldClocks(true,20))threads.put(c.optString("name")+" "+c.optInt("current_value")+"/"+c.optInt("max_value"));
            JSONObject beats=new JSONObject();StringBuilder heroes=new StringBuilder();for(JSONObject h:db.listActiveCharacters()){if(heroes.length()>0)heroes.append("; ");heroes.append(h.optString("name")).append(" PV ").append(h.optInt("hp")).append("/").append(h.optInt("max_hp"));JSONObject control=db.getCharacterControl(h.optLong("id"));beats.put(h.optString("name"),new JSONObject().put("spotlight",control.optInt("spotlight_score",0)).put("control",control.optString("control_mode","manual")).put("personal_goal",control.optString("personal_goal")).put("arc_status",control.optString("arc_status","active")).put("last_major_choice",control.optString("last_major_choice")));}
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
        if(narration==null)return;String lower=narration.toLowerCase(new Locale("pt","BR"));Set<Long> mentioned=new HashSet<Long>();List<JSONObject> heroes=db.listActiveCharacters();
        for(JSONObject h:heroes){String name=h.optString("name").trim();if(name.length()>0&&lower.contains(name.toLowerCase(new Locale("pt","BR"))))mentioned.add(h.optLong("id"));}
        db.updateSpotlight(mentioned);
    }

    public String nextSpotlightHint(){
        JSONObject h=db.lowestSpotlightCharacter();if(h.length()==0)return "";String goal=h.optString("personal_goal").trim();String arc=h.optString("arc_status","active");return "O herói há mais tempo sem protagonismo relevante é "+h.optString("name")+". Ofereça uma oportunidade natural ligada às suas capacidades ou história, sem forçar a cena."+(goal.length()>0?" Objetivo pessoal: "+goal+" (arco "+arc+").":"");
    }

    private static String inferIntent(String action){String n=action.toLowerCase(Locale.ROOT);if(contains(n,"atac","golpe","dispar","lanço","conjuro"))return "combate";if(contains(n,"procuro","examino","investigo","observo","farejo"))return "exploracao";if(contains(n,"falo","pergunto","convers","negocio","ameaço","persu"))return "social";if(contains(n,"viajo","avanço","caminho","sigo","movo"))return "movimento";if(contains(n,"descanso","acampo","durmo"))return "descanso";return "livre";}
    private static boolean contains(String x,String... values){for(String v:values)if(x.contains(v))return true;return false;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw);}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw);}catch(Exception e){return new JSONArray();}}
    private static JSONArray firstJson(JSONArray source,int max){JSONArray out=new JSONArray();if(source!=null)for(int i=0;i<source.length()&&i<max;i++)out.put(source.opt(i));return out;}
}
