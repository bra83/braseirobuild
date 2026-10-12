package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Seleciona sinais autônomos do mundo para a narração sem criar fatos novos.
 * O resultado é somente contexto para o Mestre: nenhum relógio, NPC, clima ou
 * evento é persistido por esta classe.
 */
public final class WorldBeatDirector {
    private WorldBeatDirector(){}

    public static JSONObject preview(String action,JSONObject scene,JSONObject world,JSONObject livingWorld,JSONObject environment,JSONObject npcPreview,JSONObject narrativeMemory,int beatNo,boolean opening,boolean nonAdvancing){
        JSONObject out=new JSONObject();
        try{
            out.put("active",false).put("read_only",true).put("beat_no",Math.max(0,beatNo));
            if(nonAdvancing){out.put("reason","fiction_frozen");return out;}

            JSONObject checkpoint=narrativeMemory==null?null:narrativeMemory.optJSONObject("checkpoint");
            String location=scene==null?"":scene.optString("location","");
            String time=scene==null?"":scene.optString("time_of_day",scene.optString("period",""));
            boolean changedScene=opening||checkpoint==null||checkpoint.length()==0||
                    !location.equals(checkpoint.optString("location",""))||!time.equals(checkpoint.optString("time_of_day",""));
            String function=BraseiroNarrativeDirector.sceneFunction(action,scene);
            if("chegada".equals(function)||"viagem".equals(function))changedScene=true;

            JSONArray perceptible=environment==null?null:environment.optJSONArray("perceptible_signs");
            JSONObject due=firstObject(world==null?null:world.optJSONArray("events_due_or_upcoming"));
            JSONObject clock=nearestClock(world==null?null:world.optJSONArray("clocks"));
            JSONObject npcPressure=firstObject(npcPreview==null?null:npcPreview.optJSONArray("npc_pressure_changes"));
            boolean environmental=perceptible!=null&&perceptible.length()>0;
            boolean urgentEvent=due!=null&&eventDueNow(due,scene);
            boolean clockPressure=clock!=null&&clockPressure(clock)>=0.70d;
            boolean periodic=beatNo>0&&beatNo%3==0;
            boolean active=changedScene||urgentEvent||clockPressure||npcPressure!=null||periodic;
            if(!active){out.put("reason","cadence_quiet");return out;}

            String intensity="ambiental";String source="scene";
            if(urgentEvent){intensity="acontecimento";source="world_event";}
            else if(clockPressure){intensity="sinal";source="world_clock";}
            else if(npcPressure!=null){intensity="sinal";source="npc_offscreen";}
            else if(environmental){intensity=changedScene?"ambiental":"sinal";source="environment";}
            else if(changedScene){intensity="ambiental";source="scene_transition";}

            out.put("active",true).put("intensity",intensity).put("source",source).put("new_scene",changedScene);
            JSONObject grounding=new JSONObject();if(scene!=null){for(String key:new String[]{"location","time_of_day","weather","lighting","danger","objective"})if(scene.has(key))grounding.put(key,scene.opt(key));}out.put("scene_grounding",grounding);
            JSONArray signals=new JSONArray();
            if(perceptible!=null)for(int i=0;i<perceptible.length()&&signals.length()<3;i++){String s=perceptible.optString(i).trim();if(s.length()>0)signals.put(s);}
            out.put("perceptible_signs",signals);
            JSONObject privateCausality=new JSONObject();
            if(clock!=null){JSONObject compact=compactClock(clock);out.put("clock_pressure",compact);privateCausality.put("clock",compact);}
            if(due!=null){JSONObject compact=compactEvent(due);out.put("event_cause_private",compact);privateCausality.put("event",compact);}
            if(npcPressure!=null){JSONObject compact=compactNpc(npcPressure);out.put("npc_pressure_private",compact);privateCausality.put("npc",compact);}
            out.put("private_causality",privateCausality);
            if(livingWorld!=null&&livingWorld.length()>0)out.put("living_world_available",livingWorld.optInt("enabled",livingWorld.optBoolean("enabled",true)?1:0)==1);
            boolean evidenceBacked=signals.length()>0||urgentEvent||clockPressure||npcPressure!=null;out.put("allows_new_world_fact",evidenceBacked);
            out.put("instruction",
                    "Inclua no máximo uma batida autônoma do mundo antes das opções quando ela couber organicamente. " +
                    "Ela NÃO precisa ser consequência da ação do jogador: pode ser rotina, um sinal ambiental, notícia, movimento de NPC, efeito distante de relógio/evento ou mudança perceptível. " +
                    "Use apenas sinais sustentados pelo contexto persistido. event_cause_private, npc_pressure_private e clock_pressure são causas do Mestre: revele somente o que os personagens poderiam perceber ou receber como notícia; nunca exponha rótulo interno, segredo, verdade oculta ou plano fora de cena. " +
                    "Batida ambiental não interrompe a agência. Só um acontecimento já vencido/ativo pode interromper a cena, e mesmo assim preserve uma escolha real. " +
                    "Se allows_new_world_fact=false, use apenas continuidade sensorial de scene_grounding (tempo, clima, luz, espaço já conhecido): NÃO crie NPC, objeto, fumaça, som distante, notícia ou evento novo. Não invente crise só para produzir movimento.");
        }catch(Exception failure){try{out=new JSONObject().put("active",false).put("read_only",true).put("reason","director_error");}catch(Exception ignored){out=new JSONObject();}}
        return out;
    }

    private static JSONObject firstObject(JSONArray array){if(array==null)return null;for(int i=0;i<array.length();i++){JSONObject x=array.optJSONObject(i);if(x!=null)return x;}return null;}
    private static boolean eventDueNow(JSONObject event,JSONObject scene){int due=event.optInt("due_day",0),day=scene==null?0:scene.optInt("day_number",0);String status=event.optString("status","");return (due>0&&day>0&&due<=day)||"due".equalsIgnoreCase(status)||"active".equalsIgnoreCase(status);}
    private static double clockPressure(JSONObject c){double max=Math.max(1d,c.optDouble("max_value",c.optDouble("max",0d)));double now=Math.max(0d,c.optDouble("current_value",c.optDouble("current",0d)));return Math.min(1d,now/max);}
    private static JSONObject nearestClock(JSONArray clocks){JSONObject best=null;double score=-1d;if(clocks!=null)for(int i=0;i<clocks.length();i++){JSONObject c=clocks.optJSONObject(i);if(c==null)continue;double p=clockPressure(c);if(p>score){score=p;best=c;}}return best;}
    private static JSONObject compactClock(JSONObject c)throws Exception{return new JSONObject().put("name",c.optString("name")).put("current",c.optInt("current_value",c.optInt("current",0))).put("max",c.optInt("max_value",c.optInt("max",0))).put("consequence",cut(c.optString("consequence"),220));}
    private static JSONObject compactEvent(JSONObject e)throws Exception{return new JSONObject().put("id",e.optLong("id",0)).put("title",cut(e.optString("title"),120)).put("description",cut(e.optString("description"),260)).put("due_day",e.optInt("due_day",0)).put("status",e.optString("status"));}
    private static JSONObject compactNpc(JSONObject n)throws Exception{return new JSONObject().put("name",cut(n.optString("name"),100)).put("urgency_before",n.optInt("urgency_before",0)).put("urgency_after",n.optInt("urgency_after",0)).put("next_action",cut(n.optString("next_action"),220));}
    private static String cut(String s,int max){String x=s==null?"":s.trim();return x.length()<=max?x:x.substring(0,max)+"…";}
}
