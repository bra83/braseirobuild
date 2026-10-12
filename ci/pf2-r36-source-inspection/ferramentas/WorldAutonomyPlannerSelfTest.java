package com.braseiro.pathfinder2e;
import org.json.*;
public final class WorldAutonomyPlannerSelfTest {
 private static void require(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 private static JSONObject guidance()throws Exception{
  JSONObject source=new JSONObject().put("book","Guia de Domínios").put("score",92).put("page",12).put("excerpt","A cada 6 horas, uma facção mercante pode fazer uma negociação de mercado.");
  JSONObject procedure=new JSONObject().put("procedure_id","rule:trade:procedure:1").put("executable",true).put("coherence_score",96)
   .put("scope_text","A cada 6 horas, uma facção mercante pode fazer uma negociação de mercado.")
   .put("durations",new JSONArray().put("6 horas")).put("mechanic_keys",new JSONArray().put("trade").put("cost"))
   .put("effects",new JSONArray().put(new JSONObject().put("kind","resource_cost")));
  JSONObject rule=new JSONObject().put("confidence",95).put("source",source).put("atomic_procedures",new JSONArray().put(procedure));
  return new JSONObject().put("structured_rules",new JSONArray().put(rule));
 }
 public static void main(String[] args)throws Exception{
  AppDb db=new AppDb();db.scene.put("day",5);
  db.events.add(new JSONObject().put("id",21).put("title","Escassez na rota da caravana").put("description","A Guilda de Ferro perdeu suprimentos").put("status","scheduled").put("due_day",5));
  db.clocks.add(new JSONObject().put("id",9).put("name","Pressão comercial").put("current_value",3).put("max_value",4).put("consequence","mercado fecha"));
  db.quests.add(new JSONObject().put("id",7).put("title","Reabrir a rota").put("objective","Proteger a caravana").put("stakes","abastecimento").put("faction_id",3));
  JSONObject faction=new JSONObject().put("id",3).put("name","Guilda de Ferro").put("agenda","controlar o comércio de caravanas").put("resources","mercadores e guardas").put("relationship",5).put("location","Mercado");
  db.factions.add(new JSONObject().put("id",3).put("name","Guilda de Ferro").put("current_plan","Reabrir a rota comercial"));
  WorldAutonomyPlanner planner=new WorldAutonomyPlanner(db);
  JSONObject plan=planner.factionPlan(faction,5,guidance());
  require(plan.optJSONArray("plan_steps_json").length()==4,"plano sem etapas");
  require(plan.optInt("plan_cycle",1)==0,"plano autônomo deveria reavaliar ao terminar");
  require(plan.optJSONObject("decision_basis").optLong("target_id",0)>0,"plano sem alvo persistente");
  require(Math.abs(plan.optDouble("plan_interval_hours",99)-6d)<0.01,"a duração recuperada do manual não guiou a cadência");
  require("manual_rule_guidance".equals(plan.optString("decision_source")),"o manual não guiou a decisão antes do plano");
  JSONObject npc=new JSONObject().put("npc_name","Mara").put("faction","Guilda de Ferro").put("current_location","Mercado").put("urgency",88).put("trust",20).put("fear",10).put("suspicion",30);
  require(WorldAutonomyPlanner.needsNewFactionPlan("Plano encerrado",4,4,false),"plano não cíclico concluído precisa ser recriado");
  JSONObject action=planner.npcAction(npc,5,guidance());
  require(action.optString("summary").contains("Mara"),"ação sem NPC");
  require("manual_rule_guidance".equals(action.optString("decision_source")),"NPC não recebeu orientação do manual antes de agir");
  require(action.optJSONObject("decision_rule_basis").optString("procedure_id").length()>0,"ação sem procedimento de decisão");
  System.out.println("OK: manuais executáveis guiaram categoria, cadência e ação antes da decisão autônoma.");
 }
}
