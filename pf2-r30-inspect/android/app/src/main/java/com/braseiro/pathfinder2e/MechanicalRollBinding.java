package com.braseiro.pathfinder2e;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Confere se uma rolagem pertence exatamente ao teste, ator, cena e regra que
 * originaram a consequência. Não aceita mais a aproximação "mesma fórmula e
 * mesmo total em até 30 minutos".
 */
public final class MechanicalRollBinding {
    public static final class Result {
        public final boolean valid;
        public final String reason;
        private Result(boolean ok,String why){valid=ok;reason=why==null?"":why;}
    }

    private MechanicalRollBinding(){}

    public static Result verify(JSONObject roll,JSONObject calculation,JSONObject hero,long now){
        if(roll==null||roll.length()==0)return fail("Rolagem vinculada não encontrada.");
        if(calculation==null)return fail("Cálculo mecânico ausente.");
        long expectedId=calculation.optLong("roll_id",0);
        if(expectedId<=0||roll.optLong("id",0)!=expectedId)return fail("roll_id obrigatório ou divergente.");
        if(roll.optLong("consumed_at",0)>0)return fail("A rolagem já foi consumida por outra consequência.");
        if(roll.optLong("cancelled_at",0)>0)return fail("A rolagem foi cancelada.");
        long created=roll.optLong("created_at",0);String boundTestKey=roll.optString("test_key","").trim();boolean persistentActivity=boundTestKey.startsWith("activity:");
        if(created<=0||created-now>60_000L||(!persistentActivity&&now-created>30L*60L*1000L))return fail(persistentActivity?"A data da rolagem vinculada é inválida.":"A rolagem não pertence à janela da cena atual.");

        String formula=compact(calculation.optString("roll_formula",calculation.optString("formula")));
        if(formula.length()==0||!formula.equals(compact(roll.optString("formula"))))return fail("A fórmula da rolagem diverge do cálculo.");
        int total=calculation.optInt("roll_total",calculation.optInt("total",Integer.MIN_VALUE));
        if(total==Integer.MIN_VALUE||roll.optInt("total",Integer.MIN_VALUE)!=total)return fail("O total da rolagem diverge do cálculo.");

        String testKey=calculation.optString("test_key","").trim();
        if(testKey.length()==0||!testKey.equals(roll.optString("test_key","").trim()))return fail("A rolagem não está vinculada ao teste atual.");
        String action=calculation.optString("action_signature","").trim();
        if(action.length()==0||!action.equals(roll.optString("action_signature","").trim()))return fail("A rolagem pertence a outra ação.");
        String scene=calculation.optString("scene_fingerprint","").trim();
        if(scene.length()==0||!scene.equals(roll.optString("scene_fingerprint","").trim()))return fail("A rolagem pertence a outra cena.");
        String source=calculation.optString("source_rule_id",calculation.optString("rule_id","")).trim();
        if(source.length()==0||!source.equals(roll.optString("source_rule_id","").trim()))return fail("A rolagem não está vinculada à regra citada.");
        String procedure=calculation.optString("procedure_id","").trim();
        if(procedure.length()==0||!procedure.equals(roll.optString("procedure_id","").trim()))return fail("A rolagem não está vinculada ao procedimento executado.");
        String campaign=calculation.optString("campaign_fingerprint","").trim();
        if(campaign.length()==0||!campaign.equals(roll.optString("campaign_fingerprint","").trim()))return fail("A rolagem pertence a outra campanha.");
        String target=calculation.optString("target_signature","").trim();
        if(target.length()==0||!target.equals(roll.optString("target_signature","").trim()))return fail("A rolagem pertence a outro alvo.");
        String consequence=calculation.optString("consequence_key","").trim();
        if(consequence.length()==0||!consequence.equals(roll.optString("consequence_key","").trim()))return fail("A rolagem não está reservada para esta consequência.");

        int modifier=calculation.optInt("modifier",calculation.optInt("flat_modifier",Integer.MIN_VALUE));
        if(modifier==Integer.MIN_VALUE||modifier!=roll.optInt("modifier",Integer.MAX_VALUE))return fail("O modificador diverge do valor vinculado à ficha.");
        String modifierSource=norm(calculation.optString("modifier_source"));if(modifierSource.length()==0||!modifierSource.equals(norm(roll.optString("modifier_source"))))return fail("A origem do modificador diverge da ficha.");
        String rollMode=norm(calculation.optString("roll_mode",calculation.optString("mode")));if(rollMode.length()==0||!rollMode.equals(norm(roll.optString("roll_mode",roll.optString("mode")))))return fail("Vantagem/desvantagem diverge da rolagem local.");
        int natural=calculation.optInt("natural_roll",Integer.MIN_VALUE);if(natural==Integer.MIN_VALUE||natural!=roll.optInt("natural_roll",Integer.MAX_VALUE))return fail("O resultado natural diverge da rolagem.");
        boolean critical=natural==20,criticalFailure=natural==1;
        if(calculation.optBoolean("critical",critical)!=critical||roll.optBoolean("critical",critical)!=critical)return fail("O crítico foi classificado incorretamente.");
        if(calculation.optBoolean("critical_failure",criticalFailure)!=criticalFailure||roll.optBoolean("critical_failure",criticalFailure)!=criticalFailure)return fail("A falha crítica foi classificada incorretamente.");
        String result=norm(calculation.optString("result",calculation.optString("outcome")));if(result.length()==0||!result.equals(norm(roll.optString("outcome"))))return fail("O resultado/consequência diverge da rolagem vinculada.");

        long heroId=hero==null?0:hero.optLong("id",hero.optLong("character_id",0));
        long rollHeroId=roll.optLong("actor_character_id",0);
        if(heroId>0&&rollHeroId!=heroId)return fail("A rolagem pertence a outro personagem.");
        String heroName=norm(hero==null?"":hero.optString("name"));
        String rollName=norm(roll.optString("actor_name"));
        if(heroName.length()>0&&rollName.length()>0&&!heroName.equals(rollName))return fail("O nome do ator da rolagem diverge da ficha.");
        return ok();
    }

    private static Result ok(){return new Result(true,"");}
    private static Result fail(String reason){return new Result(false,reason);}
    private static String compact(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("\\s+","").replace("−","-");}
    private static String norm(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
