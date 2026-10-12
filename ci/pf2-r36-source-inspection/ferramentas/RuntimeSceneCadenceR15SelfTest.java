package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public final class RuntimeSceneCadenceR15SelfTest {
    private static void ok(boolean value,String msg){if(!value)throw new AssertionError(msg);}
    public static void main(String[] args)throws Exception{
        testCache();testWorldBeat();testPacing();
        System.out.println("R15_RUNTIME_SCENE_CADENCE_PASS");
    }
    private static void testCache()throws Exception{
        SceneRuleCache cache=new SceneRuleCache();
        JSONObject scene=new JSONObject().put("location","Bosque").put("day_number",1).put("time_of_day","manhã");
        JSONObject map=new JSONObject().put("map_package_id","atlas").put("current_hex_id","AA01");
        JSONObject combat=new JSONObject().put("running",0);
        String scope=SceneRuleCache.scope(scene,map,combat);
        JSONObject b=new JSONObject().put("status","ready").put("evidence_count",1).put("evidence",new JSONArray().put(new JSONObject().put("rule_id","x"))).put("structured_rules",new JSONArray());
        ArrayList<JSONObject> hits=new ArrayList<JSONObject>();hits.add(new JSONObject().put("rule_id","x"));
        cache.put(scope,"rastrear",new RuleRetrievalEngine.Result(b,hits));
        RuleRetrievalEngine.Result got=cache.get(scope,"rastrear");
        ok(got!=null,"cache miss");ok("scene_rule_cache_hit".equals(got.bundle.optString("status")),"status cache");
        scene.put("location","Ruínas");String other=SceneRuleCache.scope(scene,map,combat);ok(cache.get(other,"rastrear")==null,"cache deve girar por cena");
    }
    private static void testWorldBeat()throws Exception{
        JSONObject scene=new JSONObject().put("location","Vale").put("time_of_day","tarde").put("day_number",4);
        JSONObject world=new JSONObject().put("events_due_or_upcoming",new JSONArray().put(new JSONObject().put("title","Patrulha chega").put("description","Movimento na estrada").put("due_day",4).put("status","due"))).put("clocks",new JSONArray());
        JSONObject environment=new JSONObject().put("perceptible_signs",new JSONArray().put("vento muda").put("aves levantam voo"));
        JSONObject wb=WorldBeatDirector.preview("observo a estrada",scene,world,new JSONObject().put("enabled",true),environment,new JSONObject(),new JSONObject(),2,false,false);
        ok(wb.optBoolean("active"),"evento devido deve ativar beat");ok("acontecimento".equals(wb.optString("intensity")),"intensidade evento");
        JSONObject frozen=WorldBeatDirector.preview("quem está aqui?",scene,world,new JSONObject(),environment,new JSONObject(),new JSONObject(),3,false,true);
        ok(!frozen.optBoolean("active"),"GM help não avança mundo");
        JSONObject periodic=WorldBeatDirector.preview("continuo pela trilha",scene,new JSONObject().put("events_due_or_upcoming",new JSONArray()).put("clocks",new JSONArray()),new JSONObject(),new JSONObject(),new JSONObject(),new JSONObject().put("checkpoint",new JSONObject().put("location","Vale").put("time_of_day","tarde")),3,false,false);
        ok(periodic.optBoolean("active"),"terceira batida deve permitir pulso autônomo");
    }
    private static void testPacing()throws Exception{
        JSONObject scene=new JSONObject();
        JSONObject quiet=new JSONObject().put("active",false);
        JSONObject event=new JSONObject().put("active",true).put("intensity","acontecimento");
        JSONObject p1=NarrativePacingDirector.plan("continuo pela trilha","exploracao",scene,1,false,quiet,false,false);
        JSONObject p2=NarrativePacingDirector.plan("continuo pela trilha","exploracao",scene,2,false,quiet,false,false);
        ok(p1.optInt("max_words")!=p2.optInt("max_words"),"cadência deve variar entre batidas");
        JSONObject pe=NarrativePacingDirector.plan("chego ao vale","exploracao",scene,2,false,event,false,false);
        ok(pe.optInt("max_words")>p2.optInt("max_words"),"acontecimento deve permitir expansão");
        JSONObject opening=NarrativePacingDirector.plan("INÍCIO AUTOMÁTICO — ABERTURA DE HISTÓRIA","exploracao",scene,0,false,quiet,true,false);
        ok(opening.optInt("min_words")>=300,"abertura precisa ser substancial");
        JSONObject help=NarrativePacingDirector.plan("quem está aqui?","exploracao",scene,0,false,quiet,false,true);
        ok("clarification".equals(help.optString("kind")),"ajuda deve ser esclarecimento");
    }
}
