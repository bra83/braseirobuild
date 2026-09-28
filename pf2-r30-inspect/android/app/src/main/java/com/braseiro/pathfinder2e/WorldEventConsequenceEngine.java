package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Aplica consequências estruturadas de eventos agendados de forma idempotente.
 * Consequências mecânicas de herói/combate permanecem bloqueadas: precisam
 * passar pela transação normal do Mestre e pela confirmação do jogador.
 */
public final class WorldEventConsequenceEngine {
    private final AppDb db;
    public WorldEventConsequenceEngine(AppDb database){db=database;}

    public JSONArray preview(JSONArray events){
        JSONArray out=new JSONArray();if(events==null)return out;
        for(int i=0;i<events.length();i++){
            JSONObject event=events.optJSONObject(i);if(event==null)continue;
            JSONObject consequence=parseObject(event.optString("consequence_json","{}"));
            JSONObject item=new JSONObject();try{
                item.put("id",event.optLong("id")).put("title",event.optString("title"));
                JSONArray blocked=blockedKeys(consequence);item.put("blocked_keys",blocked);
                item.put("applicable",blocked.length()==0);
                item.put("summary",summary(consequence));
            }catch(Exception ignored){AppLog.ignored("WorldEventConsequence","Falha ao preparar prévia.",ignored);}
            out.put(item);
        }
        return out;
    }

    public boolean applyTriggered(JSONArray events) throws Exception{
        if(events==null)return true;boolean all=true;
        for(int i=0;i<events.length();i++){
            JSONObject event=events.optJSONObject(i);if(event==null)continue;final long id=event.optLong("id",0);if(id<=0)continue;
            try{
                JSONObject fresh=db.getWorldEvent(id);
                if(fresh.length()==0)continue;
                String eventKey=fresh.optString("event_key").trim();if(eventKey.length()==0)throw new IllegalStateException("Evento sem event_key obrigatório: "+id);
                final String effectKey=LegacyWorldEventIdentity.transactionKey(eventKey);
                if("applied".equalsIgnoreCase(fresh.optString("consequence_status"))||db.hasEffectReceipt(effectKey))continue;
                String eventStatus=fresh.optString("status");if("awaiting_review".equalsIgnoreCase(eventStatus)){all=false;continue;}if(!"scheduled".equalsIgnoreCase(eventStatus))continue;
                final JSONObject consequence=parseObject(fresh.optString("consequence_json","{}"));
                JSONArray blocked=blockedKeys(consequence);
                if(blocked.length()>0){
                    String error="Consequência mecânica exige revisão do Mestre: "+blocked.toString();
                    JSONObject pending=new JSONObject().put("_world_event_id",id).put("_world_event_title",fresh.optString("title")).put("requires_master_review",true).put("proposed_consequence",consequence).put("resolution_key",effectKey);
                    db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{
                        db.addPendingTransactionUnique("Evento pendente — "+fresh.optString("title"),"Consequência mecânica exige revisão do Mestre.",pending,effectKey);
                        db.markWorldEventPendingReview(id,"Consequência mecânica exige revisão do Mestre.");
                        if(!"awaiting_review".equalsIgnoreCase(db.getWorldEvent(id).optString("status")))throw new IllegalStateException("O evento não entrou em revisão.");
                        db.addAuditEvent("evento_mundo","Evento enviado para revisão",fresh.optString("title"),fresh.toString(),pending.toString(),false);
                    }});
                    all=false;continue;
                }
                db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{
                    apply(consequence,fresh);
                    db.recordEffectReceipt(effectKey,"Evento — "+fresh.optString("title"));
                    db.markWorldEventConsequenceChecked(id,true,"");
                    db.addAuditEvent("evento_mundo","Consequência de evento aplicada",fresh.optString("title"),fresh.toString(),consequence.toString(),true);
                }});
            }catch(Exception e){
                String error=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
                throw new Exception("Falha na consequência do evento "+event.optString("title")+": "+error,e);
            }
        }
        return all;
    }

    public boolean canApplyStructured(JSONObject consequence){return blockedKeys(consequence).length()==0;}

    /** Reutilizado por planos autônomos. Só executa efeitos estruturais
     * e recusa qualquer mutação mecânica de personagem ou combate. */
    public void applyStructured(JSONObject consequence,String sourceTitle)throws Exception{
        applyStructured(consequence,sourceTitle,"");
    }

    public void applyStructured(final JSONObject consequence,final String sourceTitle,final String effectKey)throws Exception{
        final JSONObject safe=consequence==null?new JSONObject():consequence;
        JSONArray blocked=blockedKeys(safe);
        if(blocked.length()>0)throw new IllegalArgumentException("Efeito mecânico exige revisão: "+blocked.toString());
        if(effectKey!=null&&effectKey.trim().length()>0&&db.hasEffectReceipt(effectKey))return;
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{
            JSONObject source=new JSONObject().put("title",sourceTitle==null?"Mundo Vivo":sourceTitle).put("description",sourceTitle==null?"":sourceTitle);
            apply(safe,source);
            if(effectKey!=null&&effectKey.trim().length()>0)db.recordEffectReceipt(effectKey,sourceTitle);
        }});
    }

    private void apply(JSONObject c,JSONObject event)throws Exception{
        JSONArray facts=c.optJSONArray("knowledge_updates");if(facts!=null)for(int i=0;i<facts.length();i++){JSONObject f=facts.optJSONObject(i);if(f==null)throw new IllegalArgumentException("Conhecimento inválido na consequência.");db.addKnowledgeFactChecked(f.optString("subject"),f.optString("content"),f.optString("truth_status","verdade"),f.optString("visibility","grupo"),arrayText(f.opt("known_by_json")),f.optString("reveal_condition"),f.optString("source",event.optString("title")));}
        JSONArray quests=c.optJSONArray("quest_updates");if(quests!=null)for(int i=0;i<quests.length();i++)db.applyQuestUpdateChecked(quests.optJSONObject(i));
        JSONArray factions=c.optJSONArray("faction_updates");if(factions!=null)for(int i=0;i<factions.length();i++)db.applyFactionUpdateChecked(factions.optJSONObject(i));
        new GuildEngine(db).applyUpdates(c);
        JSONArray clocks=c.optJSONArray("clock_updates");if(clocks!=null)for(int i=0;i<clocks.length();i++)db.applyWorldClockUpdateChecked(clocks.optJSONObject(i));
        JSONArray events=c.optJSONArray("world_events");if(events!=null)for(int i=0;i<events.length();i++){JSONObject e=events.optJSONObject(i);if(e==null)throw new IllegalArgumentException("Evento encadeado inválido.");db.addWorldEventChecked(e);}
        JSONArray memories=c.optJSONArray("npc_updates");if(memories!=null)for(int i=0;i<memories.length();i++)db.upsertNpcMemoryChecked(memories.optJSONObject(i));
        JSONArray cognition=c.optJSONArray("npc_cognition_updates");if(cognition!=null)for(int i=0;i<cognition.length();i++)db.upsertNpcCognitiveStateChecked(cognition.optJSONObject(i));
        JSONArray journals=c.optJSONArray("journal_updates");if(journals!=null)for(int i=0;i<journals.length();i++){JSONObject j=journals.optJSONObject(i);if(j==null)throw new IllegalArgumentException("Diário inválido na consequência.");if(j.optString("body").trim().length()>0)db.addJournalChecked(j.optString("title",event.optString("title")),j.optString("body"),j.optString("kind","Evento"));}
        JSONArray maps=c.optJSONArray("map_updates");if(maps!=null)for(int i=0;i<maps.length();i++)db.applyMapDirectorUpdateChecked(maps.optJSONObject(i));
        JSONArray tactical=c.optJSONArray("tactical_updates");if(tactical!=null)for(int i=0;i<tactical.length();i++)db.applyTacticalDirectorUpdateChecked(tactical.optJSONObject(i));
        JSONObject scene=c.optJSONObject("scene_state");if(scene!=null&&scene.length()>0){JSONObject merged=new JSONObject(db.getSceneState().toString());java.util.Iterator<String> keys=scene.keys();while(keys.hasNext()){String key=keys.next();if(!scene.isNull(key))merged.put(key,scene.opt(key));}db.saveSceneStateChecked(merged);}
        if(c.length()==0||summary(c).equals("sem alterações estruturadas"))db.addJournalChecked("Evento — "+event.optString("title"),event.optString("description","O evento ocorreu."),"Evento");
    }

    private static JSONArray blockedKeys(JSONObject c){JSONArray out=new JSONArray();if(c==null)return out;for(String key:new String[]{"state_updates","combat","character_updates","xp","gold","damage","healing"}){Object value=c.opt(key);boolean present=value!=null&&value!=JSONObject.NULL;if(value instanceof JSONArray)present=((JSONArray)value).length()>0;if(value instanceof JSONObject)present=((JSONObject)value).length()>0;if(present)out.put(key);}return out;}
    private static String summary(JSONObject c){if(c==null||c.length()==0)return "sem alterações estruturadas";StringBuilder b=new StringBuilder();append(b,c,"knowledge_updates","fato(s)");append(b,c,"quest_updates","missão(ões)");append(b,c,"faction_updates","facção(ões)");append(b,c,"guild_updates","guilda(s)");append(b,c,"clock_updates","relógio(s)");append(b,c,"world_events","novo(s) evento(s)");append(b,c,"npc_updates","NPC(s)");append(b,c,"npc_cognition_updates","ação(ões) de NPC");append(b,c,"journal_updates","registro(s) de diário");append(b,c,"map_updates","mudança(s) de mapa");append(b,c,"tactical_updates","movimento(s) tático(s)");if(c.optJSONObject("scene_state")!=null&&c.optJSONObject("scene_state").length()>0){if(b.length()>0)b.append(" • ");b.append("estado da cena");}return b.length()==0?"sem alterações estruturadas":b.toString();}
    private static void append(StringBuilder b,JSONObject c,String key,String label){JSONArray a=c.optJSONArray(key);if(a==null||a.length()==0)return;if(b.length()>0)b.append(" • ");b.append(a.length()).append(' ').append(label);}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static String arrayText(Object value){if(value instanceof JSONArray)return value.toString();try{return new JSONArray(String.valueOf(value)).toString();}catch(Exception e){return "[]";}}
    private static String objectText(Object value){if(value instanceof JSONObject)return value.toString();try{return new JSONObject(String.valueOf(value)).toString();}catch(Exception e){return "{}";}}
}
