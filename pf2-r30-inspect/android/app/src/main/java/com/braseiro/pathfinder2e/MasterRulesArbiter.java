package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Firewall mecânico do Mestre para Pathfinder 2e Remaster.
 *
 * A IA pode propor uma jogada ou citar uma regra recuperada, mas nunca é a
 * autoridade numérica. Regras externas (D&D, Mystara e PF1) não têm fallback.
 */
public final class MasterRulesArbiter {
    private static final String SYSTEM_ID="pathfinder_2e";
    private final AppDb db;
    private final Pathfinder2eRulesEngine rules=new Pathfinder2eRulesEngine();
    private final RuleResolutionVerifier resolutionVerifier;
    private JSONObject activeRuleRetrieval=new JSONObject();

    public MasterRulesArbiter(AppDb database){
        db=database;
        resolutionVerifier=new RuleResolutionVerifier(database);
    }

    public void setActiveRuleRetrieval(JSONObject retrieval){
        activeRuleRetrieval=retrieval==null?new JSONObject():retrieval;
    }

    public JSONObject rulesFrame(String action){
        JSONObject out=new JSONObject();
        try{
            JSONArray actors=new JSONArray();
            List<JSONObject> active=db.listActiveCharacters();
            for(JSONObject hero:active){
                JSONObject a=new JSONObject();
                a.put("name",hero.optString("name"));
                a.put("level",Math.max(1,hero.optInt("level",1)));
                a.put("hp",hero.optInt("hp",0));
                a.put("max_hp",hero.optInt("max_hp",0));
                a.put("ac",hero.optInt("ac",10));
                a.put("initiative_perception",rules.initiativeBonus(hero,"Percepção"));
                a.put("turn_budget",rules.turnBudget(0,false,false,false,0));
                actors.put(a);
            }
            out.put("system_id",SYSTEM_ID);
            out.put("rules_profile",Pathfinder2eRulesEngine.PROFILE);
            out.put("authority",rules.authority());
            out.put("action_economy","3 ações + 1 reação; atividades podem consumir 1–3 ações conforme a regra recuperada.");
            out.put("degree_policy","total vs CD: +10 sucesso crítico; >= CD sucesso; <= CD-10 falha crítica; 20 natural melhora um grau; 1 natural piora um grau.");
            out.put("map_policy","MAP local: -5/-10, ou -4/-8 para arma ágil, quando aplicável.");
            out.put("condition_policy","Somente condições PF2e; dying/wounded/doomed e dano persistente são estados PF2e.");
            out.put("test_policy","Sem risco, oposição, custo ou incerteza relevante, não peça teste. Nunca escolha um personagem silenciosamente quando o ator estiver ambíguo.");
            out.put("external_system_fallback",false);
            out.put("actors",actors);
            out.put("scene_has_risk",hasRisk(action));
            out.put("opposition_present",oppositionPresent(action));
            out.put("rule_retrieval_status",activeRuleRetrieval.optString("status","not_attached"));
            JSONArray ev=activeRuleRetrieval.optJSONArray("evidence");
            out.put("rule_evidence_count",ev==null?0:ev.length());
            out.put("suggested_check",suggestedPf2Check(action));
            out.put("travel_rules_state",db.getTravelRulesState());
        }catch(Exception error){AppLog.w("MasterRulesArbiter","Falha ao montar quadro PF2e.",error);}
        return out;
    }

    public JSONObject validate(JSONObject envelope,String action){
        if(envelope==null)envelope=new JSONObject();
        JSONArray warnings=new JSONArray();
        int verifiedMechanics=0,blockedMechanics=0;
        try{
            Map<String,JSONObject> heroes=heroesByName();
            JSONObject test=envelope.optJSONObject("pending_test");
            if(test!=null&&test.optBoolean("required",true)){
                if(!hasRisk(action)&&!oppositionPresent(action)){
                    envelope.put("pending_test",JSONObject.NULL);
                    warnings.put("Teste removido: não há risco, oposição, custo ou incerteza mecânica suficiente.");
                }else{
                    sanitizePf2Test(test,heroes,warnings);
                    JSONObject best=resolutionVerifier.bestEvidenceForTest(test,activeRuleRetrieval);
                    if(best!=null){
                        JSONObject support=new JSONObject();
                        support.put("book",best.optString("book"));
                        support.put("page",best.optInt("page",0));
                        support.put("rule_id",best.optString("rule_id"));
                        support.put("procedure_id",best.optString("_matched_procedure_id"));
                        support.put("source_type","retrieved_pf2_manual");
                        test.put("rule_support",support);
                    }else if(activeRuleRetrieval.optBoolean("required",false)){
                        envelope.put("pending_test",JSONObject.NULL);
                        warnings.put("Teste bloqueado: a busca de regra era obrigatória e não encontrou procedimento PF2e compatível.");
                        blockedMechanics++;
                    }
                }
            }

            JSONArray updates=envelope.optJSONArray("state_updates");
            JSONArray safeUpdates=new JSONArray();
            if(updates!=null)for(int i=0;i<updates.length();i++){
                JSONObject update=updates.optJSONObject(i);if(update==null)continue;
                String who=normalize(update.optString("character"));
                JSONObject hero=heroes.get(who);
                if(hero==null){warnings.put("Alteração mecânica rejeitada: personagem inexistente.");blockedMechanics++;continue;}
                if(!containsMechanicalMutation(update)){
                    safeUpdates.put(copyNonMechanicalUpdate(update));
                    continue;
                }
                JSONObject basis=update.optJSONObject("mechanical_basis");
                RuleResolutionVerifier.Result verified=resolutionVerifier.verifyStateUpdate(basis,update,hero,activeRuleRetrieval);
                if(verified.valid){
                    JSONObject clean=new JSONObject(update.toString());
                    if(basis!=null)basis.put("calculation",verified.normalizedCalculation).put("calculation_verified",true);
                    safeUpdates.put(clean);verifiedMechanics++;
                }else{
                    blockedMechanics++;
                    warnings.put("Alteração mecânica de "+hero.optString("name")+" bloqueada: "+verified.reason);
                }
            }
            envelope.put("state_updates",safeUpdates);

            JSONObject combat=envelope.optJSONObject("combat");
            if(combat==null)combat=new JSONObject().put("action","none");
            String combatAction=normalize(combat.optString("action","none"));
            if(!combatAction.matches("none|start|update|end"))combatAction="none";
            combat.put("action",combatAction);
            if("start".equals(combatAction)){
                JSONArray enemies=combat.optJSONArray("enemies");
                if(enemies!=null&&enemies.length()>0){
                    JSONObject basis=combat.optJSONObject("mechanical_basis");
                    RuleResolutionVerifier.Result verified=resolutionVerifier.verifyEnemyStatBlocks(basis,enemies,activeRuleRetrieval,false);
                    if(verified.valid){
                        if(basis!=null)basis.put("calculation",verified.normalizedCalculation).put("calculation_verified",true);
                        verifiedMechanics++;
                    }else{
                        combat.put("action","none").put("enemies",new JSONArray()).put("updates",new JSONArray());
                        blockedMechanics++;
                        warnings.put("Encontro mecânico bloqueado: "+verified.reason);
                    }
                }
            }
            if("none".equals(combat.optString("action"))||"end".equals(combat.optString("action"))){
                combat.remove("enemies");
                if("none".equals(combat.optString("action")))combat.remove("updates");
            }
            envelope.put("combat",combat);

            JSONObject report=new JSONObject();
            report.put("valid",true);
            report.put("fail_closed",true);
            report.put("system_id",SYSTEM_ID);
            report.put("rules_profile",Pathfinder2eRulesEngine.PROFILE);
            report.put("verified_mechanics",verifiedMechanics);
            report.put("blocked_mechanics",blockedMechanics);
            report.put("external_system_fallback",false);
            report.put("warnings",warnings);
            envelope.put("_rules_report",report);
        }catch(Exception error){
            AppLog.e("MasterRulesArbiter","Falha fechada na validação PF2e.",error);
            try{
                envelope.put("pending_test",JSONObject.NULL);
                envelope.put("state_updates",new JSONArray());
                envelope.put("combat",new JSONObject().put("action","none"));
                warnings.put("Mecânica bloqueada por falha do árbitro local: "+error.getClass().getSimpleName());
                envelope.put("_rules_report",new JSONObject().put("valid",true).put("fail_closed",true).put("system_id",SYSTEM_ID).put("verified_mechanics",0).put("blocked_mechanics",blockedMechanics+1).put("warnings",warnings));
            }catch(Exception ignored){AppLog.ignored("MasterRulesArbiter","Falha ao fechar relatório PF2e.",ignored);}
        }
        return envelope;
    }

    private void sanitizePf2Test(JSONObject test,Map<String,JSONObject> heroes,JSONArray warnings)throws Exception{
        test.put("system_id",SYSTEM_ID).put("rules_profile",Pathfinder2eRulesEngine.PROFILE).put("required",true);
        String actor=test.optString("character","").trim();
        if(actor.length()>0&&!heroes.containsKey(normalize(actor))){
            test.remove("character");test.put("actor_selection_required",true);
            warnings.put("Ator do teste não existe na companhia; a seleção ficará para o aplicativo.");
        }
        String type=normalize(test.optString("type","check"));
        if(type.contains("attack")||type.contains("ataq"))type="attack";
        else if(type.contains("save")||type.contains("salv")||type.contains("fort")||type.contains("reflex")||type.contains("will")||type.contains("vont"))type="save";
        else type="check";
        test.put("type",type);
        if("save".equals(type)){
            test.put("save_type",validSave(test.optString("save_type",test.optString("skill","Fortitude"))));
            test.remove("skill");
        }else if("check".equals(type)){
            String skill=validSkill(test.optString("skill",test.optString("ability","Percepção")));
            test.put("skill",skill);test.remove("ability");
        }
        int dc=test.optInt("dc",15);test.put("dc",Math.max(5,Math.min(60,dc)));
        String mode=normalize(test.optString("mode","Normal"));
        if(mode.contains("fortuna")||mode.contains("fortune"))test.put("mode","Fortuna");
        else if(mode.contains("infortun")||mode.contains("misfortune"))test.put("mode","Infortúnio");
        else test.put("mode","Normal");
        test.remove("advantage");test.remove("disadvantage");test.remove("target_number");
    }

    private JSONObject suggestedPf2Check(String action){
        JSONObject o=new JSONObject();
        try{
            String n=normalize(action);
            if(n.contains("esgueir")||n.contains("furtiv")||n.contains("stealth"))return o.put("type","check").put("skill","Furtividade");
            if(n.contains("escalar")||n.contains("nadar")||n.contains("saltar")||n.contains("forcar")||n.contains("athletics"))return o.put("type","check").put("skill","Atletismo");
            if(n.contains("engan")||n.contains("mentir")||n.contains("deception"))return o.put("type","check").put("skill","Enganação");
            if(n.contains("convencer")||n.contains("diplom")||n.contains("negoci"))return o.put("type","check").put("skill","Diplomacia");
            if(n.contains("intimid"))return o.put("type","check").put("skill","Intimidação");
            if(n.contains("investig")||n.contains("procur")||n.contains("observar")||n.contains("percep"))return o.put("type","check").put("skill","Percepção");
            if(n.contains("rastre")||n.contains("sobreviv"))return o.put("type","check").put("skill","Sobrevivência");
        }catch(Exception ignored){}
        return o;
    }

    private Map<String,JSONObject> heroesByName(){
        Map<String,JSONObject> out=new HashMap<>();
        for(JSONObject h:db.listActiveCharacters())out.put(normalize(h.optString("name")),h);
        return out;
    }

    private static boolean containsMechanicalMutation(JSONObject u){
        if(u==null)return false;
        return u.has("hp_delta")||u.has("hp_set")||u.has("temp_hp")||nonEmpty(u.optJSONObject("resource_changes"))||arrayLen(u.optJSONArray("condition_add"))>0||arrayLen(u.optJSONArray("condition_remove"))>0||arrayLen(u.optJSONArray("add_items"))>0||arrayLen(u.optJSONArray("remove_items"))>0;
    }

    private static JSONObject copyNonMechanicalUpdate(JSONObject u){
        JSONObject out=new JSONObject();
        try{
            out.put("character",u.optString("character"));
            if(u.optString("notes_append").trim().length()>0)out.put("notes_append",u.optString("notes_append"));
            if(u.optString("reason").trim().length()>0)out.put("reason",u.optString("reason"));
        }catch(Exception ignored){}
        return out;
    }

    private static String validSkill(String raw){
        String n=normalize(raw);
        String[][] skills={
            {"Acrobacia","acrob"},{"Arcanismo","arcan"},{"Atletismo","atlet","athlet"},{"Manufatura","manuf","craft"},
            {"Enganação","engan","decept"},{"Diplomacia","diplom"},{"Intimidação","intimid"},{"Medicina","medic"},
            {"Natureza","natur"},{"Ocultismo","ocult","occult"},{"Performance","perform"},{"Religião","religi"},
            {"Sociedade","socied","society"},{"Furtividade","furtiv","stealth"},{"Sobrevivência","sobreviv","survival"},
            {"Ladinagem","ladin","thiever"},{"Percepção","percep"}
        };
        for(String[] row:skills)for(int i=1;i<row.length;i++)if(n.contains(row[i]))return row[0];
        return "Percepção";
    }

    private static String validSave(String raw){
        String n=normalize(raw);
        if(n.contains("reflex"))return "Reflexos";
        if(n.contains("will")||n.contains("vont"))return "Vontade";
        return "Fortitude";
    }

    private static boolean hasRisk(String action){
        String n=normalize(action);
        if(n.trim().isEmpty())return false;
        String[] terms={"atac","luta","combate","perigo","ameaca","armad","escalar","saltar","nadar","furtiv","esgueir","roub","forcar","veneno","doenca","perseguir","fugir","rastre","desarm","conjurar","magia","intimid","engan","negoci","investig"};
        for(String t:terms)if(n.contains(t))return true;
        return false;
    }

    private static boolean oppositionPresent(String action){
        String n=normalize(action);
        String[] terms={"inimig","criatura","monstro","guarda","rival","oponent","hostil","armadilha","dc ","cd "};
        for(String t:terms)if(n.contains(t))return true;
        return false;
    }

    private static int arrayLen(JSONArray a){return a==null?0:a.length();}
    private static boolean nonEmpty(JSONObject o){return o!=null&&o.length()>0;}
    private static String normalize(String s){
        String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9_+\\- ]"," ").replaceAll("\\s+"," ").trim();
    }
}
