package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai orientação operacional somente de procedimentos atômicos executáveis
 * recuperados dos manuais. É usada antes da decisão autônoma, não como uma
 * citação decorativa anexada depois.
 */
public final class WorldRuleGuidance {
    private static final Pattern DURATION=Pattern.compile("(?iu)(\\d+(?:[.,]\\d+)?)\\s*(minutos?|minutes?|horas?|hours?|dias?|days?|rodadas?|rounds?|turnos?|turns?)");
    private final JSONObject best;
    private final JSONObject source;
    private final int score;

    public WorldRuleGuidance(JSONObject retrievalBundle){
        JSONObject found=null,foundSource=null;int foundScore=-1;
        JSONArray rules=retrievalBundle==null?null:retrievalBundle.optJSONArray("structured_rules");
        if(rules!=null)for(int i=0;i<rules.length();i++){
            JSONObject rule=rules.optJSONObject(i);if(rule==null)continue;
            JSONArray procedures=rule.optJSONArray("atomic_procedures");
            if(procedures==null)continue;
            for(int j=0;j<procedures.length();j++){
                JSONObject procedure=procedures.optJSONObject(j);if(procedure==null||!procedure.optBoolean("executable",false))continue;
                JSONObject candidateSource=rule.optJSONObject("source");if(candidateSource==null)candidateSource=new JSONObject();int retrievalScore=candidateSource.optInt("score",0);
                if(retrievalScore<35)continue;
                int candidate=procedure.optInt("coherence_score",0)+rule.optInt("confidence",0)/4+Math.min(30,retrievalScore/4);
                if(candidate>foundScore){foundScore=candidate;found=procedure;foundSource=candidateSource;}
            }
        }
        best=found==null?new JSONObject():found;source=foundSource==null?new JSONObject():foundSource;score=Math.max(0,foundScore);
    }

    public boolean hasExecutableProcedure(){return best.length()>0&&best.optBoolean("executable",false)&&source.optInt("score",0)>=35;}
    public JSONObject bestProcedure(){return copy(best);}

    public double recommendedIntervalHours(double fallback){
        double candidate=durationHours(best.optJSONArray("durations"));
        if(candidate<=0d)candidate=durationHours(best.optJSONArray("numeric_terms"));
        if(candidate<=0d)return clamp(fallback,1d,24d*30d);
        return clamp(candidate,1d,24d*30d);
    }

    public String recommendedCategory(String fallback){
        String all=norm(best.optJSONArray("mechanic_keys")+" "+best.optString("scope_text")+" "+best.optJSONArray("triggers")+" "+best.optJSONArray("effects"));
        if(contains(all,"morale","moral","retreat","flee","cerco","siege","war","guerra","combat"))return "guerra";
        if(contains(all,"price","cost","trade","market","commerce","preco","custo","comerc","mercado"))return "comercio";
        if(contains(all,"ritual","divine","religion","temple","religiao","templo","culto"))return "religiao";
        if(contains(all,"research","craft","study","downtime","pesquis","fabric","estudo","tempo livre"))return "pesquisa";
        if(contains(all,"reaction","attitude","negotiation","social","reacao","atitude","persuas","intriga"))return "intriga";
        return fallback==null?"influencia":fallback;
    }

    public String actionArchetype(String fallback){
        String all=norm(best.optString("scope_text")+" "+best.optJSONArray("triggers")+" "+best.optJSONArray("effects"));
        if(contains(all,"morale","retreat","flee","moral","recu"))return "avaliar moral e posição antes de recuar, negociar ou pressionar";
        if(contains(all,"reaction","attitude","reacao","atitude"))return "realizar uma abordagem social e registrar a reação conforme a regra";
        if(contains(all,"travel","journey","march","viagem","marcha","terrain","terreno"))return "executar deslocamento mensurável respeitando terreno, ritmo e tempo";
        if(contains(all,"cost","price","trade","custo","preco","comerc"))return "negociar e movimentar recursos com custos verificáveis";
        if(contains(all,"downtime","craft","research","tempo livre","fabric","pesquis"))return "executar uma etapa de atividade com duração e requisito verificáveis";
        return fallback==null?"agir conforme o estado persistente":fallback;
    }

    public JSONObject decisionBasis(){
        try{return new JSONObject().put("source","manual_rule_guidance").put("procedure_id",best.optString("procedure_id"))
                .put("coherence_score",best.optInt("coherence_score",score)).put("retrieval_score",source.optInt("score",0)).put("book",source.optString("book"))
                .put("page",source.optInt("page",0)).put("excerpt",source.optString("excerpt",best.optString("scope_text")))
                .put("mechanic_keys",best.optJSONArray("mechanic_keys")!=null?best.optJSONArray("mechanic_keys"):new JSONArray());}
        catch(Exception error){return new JSONObject();}
    }

    private static double durationHours(JSONArray values){
        if(values==null)return 0d;
        for(int i=0;i<values.length();i++){
            String text=String.valueOf(values.opt(i));Matcher matcher=DURATION.matcher(text);
            if(!matcher.find())continue;double value;
            try{value=Double.parseDouble(matcher.group(1).replace(',','.'));}catch(Exception error){if(error.getMessage()==null){}continue;}
            String unit=norm(matcher.group(2));
            if(unit.startsWith("dia")||unit.startsWith("day"))return value*24d;
            if(unit.startsWith("hora")||unit.startsWith("hour"))return value;
            if(unit.startsWith("min")||unit.startsWith("minute"))return value/60d;
            if(unit.startsWith("rod")||unit.startsWith("round")||unit.startsWith("turn"))return value/10d;
        }
        return 0d;
    }

    private static JSONObject copy(JSONObject value){JSONObject out=new JSONObject();if(value==null)return out;try{java.util.Iterator<String> keys=value.keys();while(keys.hasNext()){String key=keys.next();out.put(key,value.opt(key));}}catch(Exception error){if(error.getMessage()==null)return new JSONObject();}return out;}
    private static boolean contains(String source,String... values){for(String value:values)if(source.contains(norm(value)))return true;return false;}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
    private static String norm(String value){if(value==null)return "";return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
}
