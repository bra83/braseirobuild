package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Persistent, source-bound runtime state for the Cozinheiro supplement. */
public final class CookingStateEngine {
    private CookingStateEngine(){}

    public static JSONObject empty(){
        JSONObject o=new JSONObject();
        try{
            o.put("schema",1).put("configured",false).put("cook_level",1).put("proficiency_bonus",2).put("constitution_modifier",0)
             .put("archetype","").put("known_snacks",new JSONArray()).put("prepared_snacks",new JSONArray())
             .put("active_snacks",new JSONObject()).put("pouch_ready",true).put("last_restock_day",1)
             .put("next_restock_due_day",31).put("last_prepare_day",0).put("last_feed_day",0).put("version",1);
        }catch(Exception ignored){}
        return o;
    }

    public static JSONObject configure(JSONObject state,int cookLevel,int proficiencyBonus,int constitutionModifier,String archetype,JSONArray knownSnacks){
        JSONObject s=copy(state);JSONArray known=new JSONArray();int level=Math.max(1,Math.min(20,cookLevel));String arch=text(archetype);
        if(knownSnacks!=null)for(int i=0;i<knownSnacks.length();i++){
            String snack=text(knownSnacks.optString(i));if(snack.isEmpty())continue;
            if(!CookingRulesEngine.canPrepareSnack(snack,level,arch))throw new IllegalArgumentException("Petisco não disponível para nível/arquétipo: "+snack);
            if(!contains(known,snack))known.put(CookingRulesEngine.snackByName(snack).optString("name",snack));
        }
        int maxKnown=snacksKnownAtLevel(level);if(known.length()>maxKnown)throw new IllegalArgumentException("Petiscos conhecidos excedem o limite do nível: "+maxKnown);
        try{s.put("configured",true).put("cook_level",level).put("proficiency_bonus",Math.max(0,proficiencyBonus)).put("constitution_modifier",constitutionModifier)
                .put("archetype",arch).put("known_snacks",known).put("snack_save_dc",CookingRulesEngine.snackSaveDc(proficiencyBonus,constitutionModifier));}catch(Exception ignored){}
        return s;
    }

    public static JSONObject restock(JSONObject state,String method,double goldAvailable,double hoursAvailable,int day){
        JSONObject s=copy(state),inputs=new JSONObject();try{inputs.put("method",text(method)).put("gold_available",Math.max(0,goldAvailable)).put("hours_available",Math.max(0,hoursAvailable));}catch(Exception ignored){}
        JSONObject run=AuthorizedMechanicalMutationEngine.execute("cozinheiro.pouch.restock",inputs),r=run.optJSONObject("result");
        if(!run.optBoolean("executed")||r==null||!r.optBoolean("eligible"))throw new IllegalArgumentException("Reabastecimento da Bolsa de Cozinheiro não elegível.");
        int d=Math.max(1,day);try{s.put("pouch_ready",true).put("last_restock_day",d).put("next_restock_due_day",d+Math.max(1,r.optInt("restock_interval_days",30)))
                .put("last_restock_method",text(method)).put("last_restock_gold_cost",r.optDouble("gold_cost",0)).put("last_restock_hours_cost",r.optDouble("hours_cost",0));}catch(Exception ignored){}
        return s;
    }

    public static JSONObject prepare(JSONObject state,String restType,JSONArray requested,boolean sharedLongRest,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject s=copy(state);requireConfigured(s);if(!s.optBoolean("pouch_ready",true))throw new IllegalStateException("Bolsa de Cozinheiro indisponível.");
        int level=s.optInt("cook_level",1),con=s.optInt("constitution_modifier",0);String arch=s.optString("archetype","");
        JSONObject baseRun=AuthorizedMechanicalMutationEngine.execute("cozinheiro.snack.prepare",new JSONObject().put("constitution_modifier",con).put("rest_type",text(restType)));
        if(!baseRun.optBoolean("executed"))throw new IllegalArgumentException("Descanso inválido para preparar petiscos.");
        int base=Math.max(0,baseRun.optJSONObject("result").optInt("snacks_prepared",0));
        JSONObject extraRun=AuthorizedMechanicalMutationEngine.execute("cozinheiro.accepting_orders",new JSONObject().put("cook_level",level).put("constitution_modifier",con).put("rest_type",text(restType)));
        int extra=extraRun.optBoolean("executed")&&extraRun.optJSONObject("result")!=null?Math.max(0,extraRun.optJSONObject("result").optInt("extra_snacks",0)):0;
        JSONArray req=requested==null?new JSONArray():requested;if(req.length()>base+extra)throw new IllegalArgumentException("Quantidade preparada excede o limite autorizado: "+(base+extra));
        JSONArray known=s.optJSONArray("known_snacks");JSONArray prepared=new JSONArray();
        for(int i=0;i<req.length();i++){
            String name=text(req.optString(i));if(!contains(known,name))throw new IllegalArgumentException("Petisco não conhecido: "+name);
            if(!CookingRulesEngine.canPrepareSnack(name,level,arch))throw new IllegalArgumentException("Petisco não preparável: "+name);
            JSONObject snack=CookingRulesEngine.snackByName(name);if(i>=base&&text(snack.optString("archetype")).length()>0)throw new IllegalArgumentException("Petisco extra de Aceitando Pedidos não pode exigir pré-requisito: "+name);
            JSONObject item=copy(snack);try{item.put("prepared_day",Math.max(1,day)).put("source_book",CookingRulesEngine.BOOK);}catch(Exception ignored){}
            prepared.put(item);
        }
        JSONObject comfortRun=AuthorizedMechanicalMutationEngine.execute("cozinheiro.comfort_meal",new JSONObject().put("cook_level",level).put("shared_long_rest",sharedLongRest&&isLongRest(restType)));boolean comfort=comfortRun.optBoolean("executed")&&comfortRun.optJSONObject("result")!=null&&comfortRun.optJSONObject("result").optBoolean("eligible",false);
        try{s.put("prepared_snacks",prepared).put("last_prepare_day",Math.max(1,day)).put("last_rest_type",text(restType)).put("comfort_meal_ready",comfort)
                .put("prepared_capacity_base",base).put("prepared_capacity_extra",extra);}catch(Exception ignored){}
        return s;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject feed(JSONObject state,String target,String snackName,boolean withinTouch,boolean freeHand,boolean actionAvailable,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject s=copy(state);requireConfigured(s);String who=text(target);if(who.isEmpty())throw new IllegalArgumentException("Alvo do petisco ausente.");
        JSONObject active=s.optJSONObject("active_snacks");if(active==null){active=new JSONObject();try{s.put("active_snacks",active);}catch(Exception ignored){}}
        if(active.optJSONObject(key(who))!=null)throw new IllegalStateException("A criatura já possui um efeito de petisco ativo.");
        JSONObject feed=AuthorizedMechanicalMutationEngine.execute("cozinheiro.snack.feed",new JSONObject().put("within_touch",withinTouch).put("free_hand",freeHand).put("action_available",actionAvailable));
        if(!feed.optBoolean("executed")||feed.optJSONObject("result")==null||!feed.optJSONObject("result").optBoolean("allowed"))throw new IllegalArgumentException("Condições para alimentar com petisco não satisfeitas.");
        JSONArray prepared=s.optJSONArray("prepared_snacks");int idx=indexOf(prepared,snackName);if(idx<0)throw new IllegalArgumentException("Petisco não está preparado: "+snackName);
        JSONObject snack=prepared.optJSONObject(idx);JSONArray next=new JSONArray();for(int i=0;i<prepared.length();i++)if(i!=idx)next.put(prepared.opt(i));
        int mult=s.optBoolean("comfort_meal_ready",false)?2:1;JSONObject effect=copy(snack);try{
            effect.put("target",who).put("started_day",Math.max(1,day)).put("save_dc",CookingRulesEngine.snackSaveDc(s.optInt("proficiency_bonus",0),s.optInt("constitution_modifier",0)))
                  .put("duration_multiplier",mult).put("effective_duration",Math.max(1,effect.optInt("duration",1))*mult)
                  .put("requires_save_resolution",effect.optString("effect_key","").contains("_save_")||effect.optString("effect_key","").startsWith("save_"))
                  .put("authority","Cozinheiro.pdf compiled snack catalog");
            active.put(key(who),effect);s.put("prepared_snacks",next).put("active_snacks",active).put("last_feed_day",Math.max(1,day));
        }catch(Exception ignored){}
        return s;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject endActiveSnack(JSONObject state,String target){JSONObject s=copy(state);JSONObject a=s.optJSONObject("active_snacks");if(a!=null)a.remove(key(target));return s;}

    public static JSONObject leftovers(JSONObject state,String requestedSnack,boolean initiativeRolled,int day){/*BR_JSON_WRAP_106*/try{
        JSONObject s=copy(state);requireConfigured(s);JSONArray prepared=s.optJSONArray("prepared_snacks");int remaining=prepared==null?0:prepared.length();
        String snack=text(requestedSnack);JSONObject run=AuthorizedMechanicalMutationEngine.execute("cozinheiro.leftovers",new JSONObject().put("cook_level",s.optInt("cook_level",1)).put("initiative_rolled",initiativeRolled).put("snacks_remaining",remaining).put("known_snack_available",contains(s.optJSONArray("known_snacks"),snack)));
        JSONObject rr=run.optJSONObject("result");if(!run.optBoolean("executed")||rr==null||!rr.optBoolean("eligible"))return s;
        if(!contains(s.optJSONArray("known_snacks"),snack)||!CookingRulesEngine.canPrepareSnack(snack,s.optInt("cook_level",1),s.optString("archetype", "")))throw new IllegalArgumentException("Petisco de Sobras precisa ser conhecido e preparável.");
        if(prepared==null)prepared=new JSONArray();prepared.put(copy(CookingRulesEngine.snackByName(snack)).put("prepared_day",Math.max(1,day)).put("generated_by","cozinheiro.leftovers"));
        try{s.put("prepared_snacks",prepared).put("last_leftovers_day",Math.max(1,day));}catch(Exception ignored){}return s;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject advanceDay(JSONObject state,int day){JSONObject s=copy(state);try{s.put("current_day",Math.max(1,day)).put("restock_due",Math.max(1,day)>=s.optInt("next_restock_due_day",31));}catch(Exception ignored){}return s;}

    public static int snacksKnownAtLevel(int level){int result=0;JSONArray p=CookingRulesEngine.classProgression();for(int i=0;i<p.length();i++){JSONObject row=p.optJSONObject(i);if(row!=null&&row.optInt("level",0)<=level)result=Math.max(result,row.optInt("snacks_known",0));}return result;}
    private static void requireConfigured(JSONObject s){if(s==null||!s.optBoolean("configured",false))throw new IllegalStateException("Estado do Cozinheiro não configurado.");}
    private static boolean isLongRest(String rest){String n=norm(rest);return "long".equals(n)||"longo".equals(n);}
    private static int indexOf(JSONArray a,String name){if(a==null)return -1;String n=norm(name);for(int i=0;i<a.length();i++){Object v=a.opt(i);String x=v instanceof JSONObject?((JSONObject)v).optString("name"):String.valueOf(v);if(norm(x).equals(n))return i;}return -1;}
    private static boolean contains(JSONArray a,String value){return indexOf(a,value)>=0;}
    private static String key(String s){return norm(s).replace(' ','_');}
    private static String norm(String s){return java.text.Normalizer.normalize(text(s).toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}
    private static String text(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
    private static JSONObject copy(JSONObject src){try{return src==null?empty():new JSONObject(src.toString());}catch(Exception e){return empty();}}
}
