package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Mantém NPCs como atores persistentes, inclusive fora da cena. */
public final class NpcAgencyEngine {
    private final AppDb db;
    private final NpcPersonalityCatalog personalities;
    private final GuildEngine guilds;
    public NpcAgencyEngine(AppDb database){this(database,null);}
    public NpcAgencyEngine(AppDb database,NpcPersonalityCatalog catalog){db=database;personalities=catalog;guilds=new GuildEngine(database);}

    public JSONArray promptView(String action,int max){
        JSONArray out=new JSONArray();try{
            String focus=normalize((action==null?"":action)+" "+db.getSceneState().optString("present_npcs_json","[]")+" "+db.getSceneState().optString("location"));
            Map<String,JSONObject> cognition=new HashMap<String,JSONObject>();for(JSONObject c:db.listNpcCognitiveStates(200))cognition.put(normalize(c.optString("npc_name")),c);
            JSONArray relevant=new JSONArray(),others=new JSONArray();JSONArray memories=db.listNpcMemories(160);
            for(int i=0;i<memories.length();i++){
                JSONObject npc=memories.optJSONObject(i);if(npc==null)continue;JSONObject c=cognition.get(normalize(npc.optString("name")));JSONObject x=new JSONObject();x.put("name",npc.optString("name"));x.put("summary",trim(npc.optString("summary"),260));x.put("goals",trim(npc.optString("goals"),220));x.put("attitude",npc.optString("attitude","neutra"));x.put("relationship",trim(npc.optString("relationship"),180));x.put("knowledge",trim(npc.optString("knowledge"),240));x.put("debts",trim(npc.optString("debts"),160));x.put("location",npc.optString("last_seen"));x.put("hex_id",npc.optString("hex_id"));x.put("voice_profile",npc.optString("voice_profile"));if(c!=null){x.put("emotion",c.optString("emotion","neutro"));x.put("trust",c.optInt("trust"));x.put("fear",c.optInt("fear"));x.put("suspicion",c.optInt("suspicion"));x.put("urgency",c.optInt("urgency"));x.put("next_action",trim(c.optString("next_action"),220));x.put("beliefs_json",trim(c.optString("beliefs_json","[]"),260));x.put("faction",c.optString("faction"));}
                String name=normalize(npc.optString("name"));if(name.length()>0&&focus.contains(name))relevant.put(x);else others.put(x);
            }
            for(int i=0;i<relevant.length()&&out.length()<max;i++)out.put(relevant.opt(i));for(int i=0;i<others.length()&&out.length()<max;i++)out.put(others.opt(i));
        }catch(Exception ignored){AppLog.ignored("NpcAgencyEngine","Falha tolerada; a execução continuou.",ignored);}return out;
    }

    public JSONObject previewOffscreenTick(JSONObject localTick){
        JSONObject out=new JSONObject();try{int hours=localTick==null?0:localTick.optInt("hours_advanced",0);JSONArray changes=new JSONArray();if(hours>0)for(JSONObject c:db.listNpcCognitiveStates(120)){
            int urgency=Math.min(100,c.optInt("urgency",0)+Math.max(0,hours/2));if(urgency!=c.optInt("urgency",0))changes.put(new JSONObject().put("name",c.optString("npc_name")).put("urgency_before",c.optInt("urgency")).put("urgency_after",urgency).put("next_action",c.optString("next_action")));
        }out.put("hours",hours).put("npc_pressure_changes",changes).put("preview_only",true);}catch(Exception ignored){AppLog.ignored("NpcAgencyEngine","Falha tolerada; a execução continuou.",ignored);}return out;
    }

    public void applyConfirmed(JSONObject envelope,JSONObject localTick,String narration) throws Exception{
        JSONArray memories=envelope==null?null:envelope.optJSONArray("npc_updates");
        if(memories!=null)for(int i=0;i<memories.length();i++){
            JSONObject u=memories.optJSONObject(i);
            if(u!=null)db.upsertNpcMemoryChecked(u);
        }
        JSONArray updates=envelope==null?null:envelope.optJSONArray("npc_cognition_updates");
        if(updates!=null)for(int i=0;i<updates.length();i++){
            JSONObject u=updates.optJSONObject(i);
            if(u!=null){db.upsertNpcCognitiveStateChecked(u);guilds.ensureNpcMembership(u.optString("name",u.optString("npc_name")),u.optString("faction"));}
        }
        // A pressão causada pela passagem do tempo é aplicada exclusivamente por
        // LivingWorldSimulationEngine. Reaplicá-la aqui duplicava a urgência.
        initializePresentNpcs(narration);
    }

    public JSONObject validateUpdates(JSONObject envelope){
        if(envelope==null)envelope=new JSONObject();
        try{
            JSONArray source=envelope.optJSONArray("npc_cognition_updates"),clean=new JSONArray();
            Set<String> known=new HashSet<String>();
            JSONArray memories=db.listNpcMemories(300);
            for(int i=0;i<memories.length();i++){
                JSONObject n=memories.optJSONObject(i);
                if(n!=null)known.add(normalize(n.optString("name")));
            }
            if(source!=null)for(int i=0;i<source.length();i++){
                JSONObject u=source.optJSONObject(i);if(u==null)continue;
                String name=u.optString("name",u.optString("npc_name")).trim();
                if(name.length()==0||!known.contains(normalize(name)))continue;
                JSONObject x=new JSONObject();x.put("name",name);
                copyText(u,x,"emotion",60);copyInt(u,x,"trust",-100,100);
                copyInt(u,x,"fear",0,100);copyInt(u,x,"suspicion",0,100);
                copyInt(u,x,"urgency",0,100);copyText(u,x,"next_action",300);
                copyJsonArray(u,x,"schedule_json");copyJsonArray(u,x,"beliefs_json");copyJsonArray(u,x,"goals_json");copyJsonArray(u,x,"knowledge_json");copyJsonArray(u,x,"recent_events_json");copyJsonObject(u,x,"personality_json");copyJsonObject(u,x,"resources_json");
                copyText(u,x,"faction",120);copyText(u,x,"current_location",160);
                copyText(u,x,"last_interaction_summary",320);clean.put(x);
            }
            envelope.put("npc_cognition_updates",clean);
            envelope.put("_npc_validation",new JSONObject().put("valid",true).put("accepted",clean.length()));
        }catch(Exception failure){
            try{
                envelope.put("npc_cognition_updates",new JSONArray());
                envelope.put("npc_updates",new JSONArray());
                envelope.put("_npc_validation",new JSONObject().put("valid",false).put("error",failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage())));
            }catch(Exception ignored){AppLog.ignored("NpcAgencyEngine","Falha ao fechar a validação de NPCs.",ignored);}
            AppLog.e("NpcAgencyEngine","Validação de NPCs bloqueada.",failure);
        }
        return envelope;
    }

    public JSONObject diagnostic(){JSONObject out=new JSONObject();try{JSONObject personalitiesReport=db.backfillNpcPersonalities();out.put("npc_memories",db.listNpcMemories(1000).length());out.put("npc_cognitive_states",db.listNpcCognitiveStates(1000).size());out.put("personality_catalog",personalitiesReport).put("guild_membership_connected",true).put("active",true);}catch(Exception ignored){AppLog.ignored("NpcAgencyEngine","Falha tolerada; a execução continuou.",ignored);}return out;}

    private void initializePresentNpcs(String narration)throws Exception{
        JSONObject scene=db.getSceneState();JSONArray present=parseArray(scene.optString("present_npcs_json","[]"));for(int i=0;i<present.length();i++){Object value=present.opt(i);JSONObject visible=value instanceof JSONObject?(JSONObject)value:new JSONObject();String name=value instanceof JSONObject?visible.optString("name"):String.valueOf(value);if(name.trim().length()==0)continue;if(visible.optBoolean("transient",false)||visible.optBoolean("anonymous",false))continue;JSONObject memory=db.findNpcMemoryByName(name);if(memory.length()==0){memory=new JSONObject().put("name",name).put("summary",visible.optString("summary",visible.optString("role"))).put("visual_description",visible.optString("visual_description")).put("last_seen",scene.optString("location")).put("hex_id",scene.optString("hex_id")).put("active",1);db.upsertNpcMemoryChecked(memory);memory=db.findNpcMemoryByName(name);}JSONObject state=db.getNpcCognitiveState(name);state.put("name",name);state.put("current_location",scene.optString("location"));state.put("last_interaction_summary",trim(narration,320));state.put("last_action_at",System.currentTimeMillis());if(personalities!=null){JSONObject selected=personalities.initializeIfBlank(name,memory,state);if(selected.length()>0)state.put("personality_json",selected.toString());}db.upsertNpcCognitiveStateChecked(state);guilds.ensureNpcMembership(name,state.optString("faction"));}
    }
    private static void copyText(JSONObject from,JSONObject to,String key,int max)throws Exception{if(from.has(key))to.put(key,trim(from.optString(key),max));}
    private static void copyInt(JSONObject from,JSONObject to,String key,int min,int max)throws Exception{if(from.has(key))to.put(key,Math.max(min,Math.min(max,from.optInt(key))));}
    private static void copyJsonArray(JSONObject from,JSONObject to,String key)throws Exception{if(!from.has(key))return;Object v=from.opt(key);if(v instanceof JSONArray)to.put(key,v.toString());else to.put(key,new JSONArray(String.valueOf(v)).toString());}
    private static void copyJsonObject(JSONObject from,JSONObject to,String key)throws Exception{if(!from.has(key))return;Object v=from.opt(key);if(v instanceof JSONObject)to.put(key,v.toString());else to.put(key,new JSONObject(String.valueOf(v)).toString());}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private static String trim(String v,int max){String s=v==null?"":v.trim();return s.length()<=max?s:s.substring(0,max)+"…";}
    private static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
