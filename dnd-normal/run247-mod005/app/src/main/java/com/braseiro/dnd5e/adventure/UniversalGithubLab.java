package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

/**
 * Same production AdventureEngine + SQLite journal, isolated sample module/party.
 * Reuses Tome -> Year Zero -> Loreweaver and Tome -> Star Wars -> Loreweaver,
 * verifies persistence after reopening and rejects duplicate results.
 * Doesn't switch the active D&D campaign or invoke Gemini.
 */
public final class UniversalGithubLab {
    public static final String MODULE_ID="braseiro-universal-github-integration-v1";
    public static final long LAB_PARTY_ID=0L;
    private UniversalGithubLab() {}

    private static JSONObject cmd(String action){return new JSONObject().put("action", action);}
    private static JSONObject resultError(String at,JSONObject result){
        return new JSONObject().put("ok",false).put("stage",at).put("detail",result);
    }
    private static JSONObject checked(AdventureEngine engine,JSONObject cmd,String status){
        JSONObject result=engine.processUniversal(cmd);
        if(!status.equals(result.optString("status")))
            throw new IllegalStateException(cmd.optString("action")+": "+result.toString());
        return result;
    }
    private static JSONArray jfaces(int... values){JSONArray arr=new JSONArray();for(int value:values)arr.put(value);return arr;}
    public interface EngineFactory { AdventureEngine newEngine(); }
    public static JSONObject run(EngineFactory factory) {
        String stage="create";
        try {
            AdventureEngine engine=factory.newEngine();
            if(engine==null)return resultError(stage,new JSONObject().put("error","Engine indisponível"));
            AdventureModule module=AdventureModule.createSampleModule(MODULE_ID,"Integração de bibliotecas GitHub");
            stage="load_or_resume";
            JSONObject start=engine.loadOrStartAdventure(module,LAB_PARTY_ID);
            if(engine.getActiveState()==null)return resultError(stage,start);
            stage="register_tome_entities";
            JSONObject current=checked(engine,cmd("status"),"ok").getJSONObject("state");
            JSONObject entities=current.optJSONObject("entities");
            if(entities==null||entities.optJSONObject("yze-hero")==null) {
                checked(engine,cmd("register").put("character_id","yze-hero")
                    .put("system_id",UniversalRpgEngine.YZE).put("name","Exploradora YZE")
                    .put("attributes",new JSONObject().put("strength",3).put("agility",4))
                    .put("resources",new JSONObject().put("health",new JSONObject().put("current",4).put("maximum",5))),"registered");
            }
            if(entities==null||entities.optJSONObject("sw-pilot")==null) {
                checked(engine,cmd("register").put("character_id","sw-pilot")
                    .put("system_id",UniversalRpgEngine.STAR_WARS).put("name","Piloto SW")
                    .put("attributes",new JSONObject().put("agility",3))
                    .put("resources",new JSONObject().put("strain",new JSONObject().put("current",2).put("maximum",10))),"registered");
            }
            if(entities==null||entities.optJSONObject("dnd-hero")==null) {
                checked(engine,cmd("register").put("character_id","dnd-hero")
                    .put("system_id",UniversalRpgEngine.DND).put("name","Herói D&D")
                    .put("attributes",new JSONObject().put("strength",14))
                    .put("resources",new JSONObject().put("hp",new JSONObject().put("current",12).put("maximum",12))),"registered");
            }
            // YZE: Tome sheet, player supplied actual d6 values, Loreweaver outcome.
            stage="year_zero_check";
            JSONObject yzePending=checked(engine,cmd("begin_check")
                .put("character_id","yze-hero").put("system_id",UniversalRpgEngine.YZE)
                .put("situation","A ponte começa a ceder")
                .put("spec",new JSONObject().put("dice_pool",new JSONObject()
                    .put("attribute",2).put("skill",1).put("gear",1)))
                .put("on_success","Alcança o trecho seguro")
                .put("on_failure","Fica agarrada à ponte"),"requires_roll").getJSONObject("pending");
            JSONObject yzeResult=checked(engine,cmd("resolve_check")
                .put("request_id",yzePending.getString("request_id"))
                .put("submission",new JSONObject().put("dice",new JSONObject()
                    .put("attribute",jfaces(6,2)).put("skill",jfaces(3)).put("gear",jfaces(5)))),"resolved");
            if(!yzeResult.getJSONObject("receipt").getJSONObject("resolution").getJSONObject("outcome").getBoolean("success"))
                throw new IllegalStateException("Year Zero não confirmou sucesso");
            // SW narrative: ability 4=2 successes, difficulty 2=1 failure.
            stage="star_wars_check";
            JSONObject swPending=checked(engine,cmd("begin_check")
                .put("character_id","sw-pilot").put("system_id",UniversalRpgEngine.STAR_WARS)
                .put("situation","O piloto foge de um bloqueio imperial")
                .put("spec",new JSONObject().put("dice_pool",new JSONObject().put("ability",1).put("difficulty",1)))
                .put("on_success","A nave escapa").put("on_failure","A patrulha alcança a nave"),"requires_roll").getJSONObject("pending");
            JSONArray swDice=new JSONArray().put(new JSONObject().put("die","ability").put("face",4))
                .put(new JSONObject().put("die","difficulty").put("face",2));
            JSONObject swResult=checked(engine,cmd("resolve_check")
                .put("request_id",swPending.getString("request_id"))
                .put("submission",new JSONObject().put("dice",swDice)),"resolved");
            if(swResult.getJSONObject("receipt").getJSONObject("resolution").optInt("net_success")!=1)
                throw new IllegalStateException("Cancelamento de sucessos SW incorreto");
            // D&D: no dice code required; true manual adjudication in same journal.
            stage="manual_dnd_check";
            JSONObject dndPending=checked(engine,cmd("ai_roll_tag")
                .put("character_id","dnd-hero").put("system_id",UniversalRpgEngine.DND)
                .put("situation","O herói salta uma fenda")
                .put("narration","Atenção: [[ROLL:1d20+2]]").put("dc",12)
                .put("on_success","Salto completo").put("on_failure","Preso à beirada"),"requires_roll").getJSONObject("pending");
            JSONObject dndResult=checked(engine,cmd("resolve_check").put("request_id",dndPending.getString("request_id"))
                .put("mode","manual").put("outcome","success"),"resolved");
            stage="resource_sqlite";
            JSONObject before=checked(engine,cmd("status"),"ok").getJSONObject("state");
            JSONObject hero=before.getJSONObject("entities").getJSONObject("yze-hero");
            int revision=hero.optInt("braseiro_revision",0);
            String event="demo."+UUID.randomUUID().toString();
            int startHealth=hero.getJSONObject("resources").getJSONObject("health").optInt("current",-1);
            int delta=startHealth>0?-1:1;
            JSONObject change=checked(engine,cmd("resource").put("event_id",event)
                .put("character_id","yze-hero").put("expected_revision",revision)
                .put("resource","health").put("delta",delta),"resource_updated");
            JSONObject retry=engine.processUniversal(cmd("resource").put("event_id",event)
                .put("character_id","yze-hero").put("expected_revision",revision)
                .put("resource","health").put("delta",delta));
            if(retry.optBoolean("committed"))throw new IllegalStateException("Evento duplicado aceito");
            // Multi-repository state effect transaction: Tome item, world time, persistent fact.
            stage="multi_module_effects";
            JSONObject fresh=checked(engine,cmd("status"),"ok").getJSONObject("state");
            JSONObject edited=fresh.getJSONObject("entities").getJSONObject("yze-hero");
            String token=UUID.randomUUID().toString().substring(0,8);
            JSONObject applied=checked(engine,cmd("apply_effects")
                .put("event_id","lab.effects."+token).put("character_id","yze-hero")
                .put("expected_revision",edited.optInt("braseiro_revision"))
                .put("effects",new JSONArray()
                    .put(new JSONObject().put("kind","item").put("item_id","torch")
                        .put("name","Tocha").put("delta",1))
                    .put(new JSONObject().put("kind","time").put("minutes",10))
                    .put(new JSONObject().put("kind","world_fact")
                        .put("fact_id","lab."+token).put("text","A ponte está danificada."))),"effects_applied");
            stage="reload_sqlite";
            AdventureEngine reopened=factory.newEngine();
            reopened.loadOrStartAdventure(module,LAB_PARTY_ID);
            JSONObject saved=checked(reopened,cmd("status"),"ok").getJSONObject("state");
            JSONObject savedSheet=saved.getJSONObject("entities").getJSONObject("yze-hero");
            int health=savedSheet.getJSONObject("resources").getJSONObject("health").optInt("current",-1);
            if(health!=startHealth+delta)throw new IllegalStateException("A reabertura não refletiu a atualização de recurso; atual="+health);
            JSONObject restoredItem=savedSheet.getJSONObject("inventory").getJSONArray("items").getJSONObject(0);
            if(restoredItem.optInt("quantity",0)<1)throw new IllegalStateException("Item não salvo no journal");
            if(saved.optInt("elapsed_minutes",0)<10)throw new IllegalStateException("Relógio não salvo no journal");
            if(saved.optJSONArray("world_facts")==null||saved.optJSONArray("world_facts").length()<1)
                throw new IllegalStateException("Fato do mundo não salvo no journal");
            // Replay must be refused after reload: no resource or story duplication.
            JSONObject replay=reopened.processUniversal(cmd("resolve_check")
                .put("request_id",swPending.getString("request_id")).put("mode","manual").put("outcome","success"));
            if(replay.optBoolean("committed"))throw new IllegalStateException("Resultado de cena duplicado");
            return new JSONObject().put("ok",true).put("status","passed")
                .put("project","Braseiro D&D NORMAL").put("sqlite_reload",true)
                .put("modules",UniversalGithubModules.catalog())
                .put("yze_outcome",yzeResult.getJSONObject("receipt").getJSONObject("resolution").getJSONObject("outcome"))
                .put("sw_resolution",swResult.getJSONObject("receipt").getJSONObject("resolution"))
                .put("dnd_manual",dndResult.getJSONObject("receipt").getJSONObject("resolution"))
                .put("resource_change",change.getJSONObject("receipt"))
                .put("effects_change",applied.getJSONObject("receipt"))
                .put("world_facts_saved",saved.getJSONArray("world_facts").length())
                .put("saved_history",saved.getJSONArray("history").length())
                .put("sqlite_hero_health",health)
                .put("no_gemini_invocation",true).put("no_apk_claim",true);
        } catch(Exception e){return resultError(stage,new JSONObject().put("error",String.valueOf(e)));}
    }
}
