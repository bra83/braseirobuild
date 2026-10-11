package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/** RUN133 - runtime executável dos talentos de Strixhaven. Nenhuma escolha é automática. */
public final class CrossBookFeatRuntimeEngine {
    private static final String BOOK="Strixhaven: A Curriculum of Chaos";
    private static final Map<String,String[]> CANTRIPS=new LinkedHashMap<String,String[]>();
    static{
        CANTRIPS.put("lorehold",new String[]{"Luz","Chama Sagrada","Taumaturgia"});
        CANTRIPS.put("prismari",new String[]{"Raio de Fogo","Prestidigitação","Raio de Gelo"});
        CANTRIPS.put("quandrix",new String[]{"Druidismo","Orientação","Mãos Mágicas"});
        CANTRIPS.put("silverquill",new String[]{"Chama Sagrada","Taumaturgia","Zombaria Viciosa"});
        CANTRIPS.put("witherbloom",new String[]{"Toque Arrepiante","Druidismo","Estabilizar"});
    }
    private CrossBookFeatRuntimeEngine(){}
    public static JSONArray allFeats(){return new JSONArray().put("Strixhaven Initiate").put("Strixhaven Mascot");}
    public static JSONArray cantripsForCollege(String college){JSONArray a=new JSONArray();String[] xs=CANTRIPS.get(norm(college));if(xs!=null)for(String x:xs)a.put(x);return a;}
    public static JSONObject lookup(String feat){JSONObject o=new JSONObject();try{String f=norm(feat);if(f.equals("strixhaven initiate"))return o.put("known",true).put("feat","Strixhaven Initiate").put("source",BOOK).put("page",36).put("mechanics_compiled",true);if(f.equals("strixhaven mascot"))return o.put("known",true).put("feat","Strixhaven Mascot").put("source",BOOK).put("page",37).put("mechanics_compiled",true);return o.put("known",false);}catch(Exception e){return o;}}

    public static JSONObject applyInitiate(JSONObject state,String college,JSONArray chosenCantrips,String firstLevelSpell,String ability){
        JSONObject out=result(state);try{
            String c=norm(college);String[] allowed=CANTRIPS.get(c);if(allowed==null)return fail(out,"COLLEGE_REQUIRED","Escolha Lorehold, Prismari, Quandrix, Silverquill ou Witherbloom.");
            if(chosenCantrips==null||chosenCantrips.length()!=2)return fail(out,"TWO_CANTRIPS_REQUIRED","Escolha exatamente 2 truques permitidos pelo colégio.");
            LinkedHashSet<String> picked=new LinkedHashSet<String>();for(int i=0;i<chosenCantrips.length();i++){String x=canonical(chosenCantrips.optString(i),allowed);if(x.length()==0)return fail(out,"INVALID_CANTRIP","Truque não permitido para "+college+": "+chosenCantrips.optString(i));picked.add(x);}if(picked.size()!=2)return fail(out,"DISTINCT_CANTRIPS_REQUIRED","Os 2 truques devem ser diferentes.");
            if(firstLevelSpell==null||firstLevelSpell.trim().isEmpty())return fail(out,"FIRST_LEVEL_SPELL_REQUIRED","Escolha explicitamente uma magia de 1º círculo permitida pela lista indicada pelo colégio.");
            String a=norm(ability);if(!(a.equals("inteligencia")||a.equals("sabedoria")||a.equals("carisma")||a.equals("intelligence")||a.equals("wisdom")||a.equals("charisma")))return fail(out,"SPELLCASTING_ABILITY_REQUIRED","Escolha Inteligência, Sabedoria ou Carisma.");
            state.put("strixhaven_college",displayCollege(c));state.put("strixhaven_initiate_cantrips",new JSONArray(picked));state.put("strixhaven_initiate_first_level_spell",firstLevelSpell.trim());state.put("strixhaven_initiate_spellcasting_ability",ability.trim());state.put("strixhaven_initiate_free_cast_available",true);appendToken(state,"selected_feats","Strixhaven Initiate");appendSpells(state,picked);appendSpell(state,firstLevelSpell.trim());clearPending(state,"feat_choice_pending","Strixhaven Initiate:");state.put("feat_choices_complete",state.optJSONArray("feat_choice_pending")==null||state.optJSONArray("feat_choice_pending").length()==0);
            return ok(out.put("state",state),"Strixhaven Initiate aplicado com escolhas explícitas.");
        }catch(Exception e){return fail(out,"RUNTIME_ERROR",e.getMessage());}
    }

    public static JSONObject useInitiateFreeCast(JSONObject state){JSONObject out=result(state);try{if(!hasFeat(state,"Strixhaven Initiate"))return fail(out,"FEAT_REQUIRED","Strixhaven Initiate não está aplicado.");if(!state.optBoolean("strixhaven_initiate_free_cast_available",false))return fail(out,"FREE_CAST_SPENT","A conjuração gratuita já foi usada desde o último descanso longo.");String spell=state.optString("strixhaven_initiate_first_level_spell","");if(spell.length()==0)return fail(out,"SPELL_CHOICE_PENDING","A magia de 1º círculo ainda não foi escolhida.");state.put("strixhaven_initiate_free_cast_available",false);return ok(out.put("state",state).put("spell",spell),"Conjuração gratuita consumida; recupera no descanso longo.");}catch(Exception e){return fail(out,"RUNTIME_ERROR",e.getMessage());}}

    public static JSONObject applyMascot(JSONObject state){JSONObject out=result(state);try{if(state.optInt("level",1)<4)return fail(out,"LEVEL_4_REQUIRED","Strixhaven Mascot exige 4º nível.");if(!hasFeat(state,"Strixhaven Initiate"))return fail(out,"INITIATE_REQUIRED","Strixhaven Mascot exige Strixhaven Initiate.");String c=norm(state.optString("strixhaven_college",state.optString("strixhaven_initiate_college_locked","")));String form=mascot(c);if(form.length()==0)return fail(out,"COLLEGE_REQUIRED","O colégio de Strixhaven precisa estar definido.");appendToken(state,"selected_feats","Strixhaven Mascot");state.put("strixhaven_mascot_form",form).put("strixhaven_mascot_find_familiar_ritual",true).put("strixhaven_mascot_attack_reaction_enabled",true).put("strixhaven_mascot_swap_free_available",true);return ok(out.put("state",state),"Strixhaven Mascot aplicado.");}catch(Exception e){return fail(out,"RUNTIME_ERROR",e.getMessage());}}

    public static JSONObject useMascotSwap(JSONObject state,int familiarDistanceMeters,boolean destinationFits,Integer explicitSpellSlotLevel){JSONObject out=result(state);try{if(!hasFeat(state,"Strixhaven Mascot"))return fail(out,"MASCOT_FEAT_REQUIRED","Strixhaven Mascot não está aplicado.");if(familiarDistanceMeters<0||familiarDistanceMeters>18)return fail(out,"OUT_OF_RANGE","O familiar precisa estar a até 18 m (60 pés).");if(!destinationFits)return fail(out,"DESTINATION_TOO_SMALL","A troca falha se o espaço de destino for pequeno demais.");if(state.optBoolean("strixhaven_mascot_swap_free_available",false)){state.put("strixhaven_mascot_swap_free_available",false);return ok(out.put("state",state).put("cost","free_long_rest_use"),"Troca de posição realizada; uso gratuito consumido.");}if(explicitSpellSlotLevel==null||explicitSpellSlotLevel.intValue()<2)return fail(out,"SLOT_2_PLUS_REQUIRED","Após o uso gratuito, o jogador deve fornecer explicitamente um espaço de magia de 2º círculo ou maior.");state.put("strixhaven_mascot_last_paid_slot_level",explicitSpellSlotLevel.intValue());return ok(out.put("state",state).put("cost","spell_slot_level_"+explicitSpellSlotLevel),"Troca de posição autorizada com gasto explícito de espaço.");}catch(Exception e){return fail(out,"RUNTIME_ERROR",e.getMessage());}}

    public static JSONObject onLongRest(JSONObject state){JSONObject out=result(state);try{if(hasFeat(state,"Strixhaven Initiate"))state.put("strixhaven_initiate_free_cast_available",true);if(hasFeat(state,"Strixhaven Mascot"))state.put("strixhaven_mascot_swap_free_available",true);return ok(out.put("state",state),"Recursos dos talentos de Strixhaven restaurados no descanso longo.");}catch(Exception e){return fail(out,"RUNTIME_ERROR",e.getMessage());}}

    private static JSONObject result(JSONObject state){JSONObject o=new JSONObject();try{o.put("ok",false).put("auto_rolls",false).put("runtime","CrossBookFeatRuntimeEngine").put("state",state==null?new JSONObject():state);}catch(Exception ignored){}return o;}
    private static JSONObject ok(JSONObject o,String m)throws Exception{return o.put("ok",true).put("message",m);}
    private static JSONObject fail(JSONObject o,String c,String m){try{o.put("ok",false).put("code",c).put("message",m==null?c:m);}catch(Exception ignored){}return o;}
    private static boolean hasFeat(JSONObject s,String feat){String f=norm(feat);for(String p:s.optString("selected_feats",s.optString("granted_feats","")).split("[|,;•]"))if(norm(p).contains(f))return true;return false;}
    private static String canonical(String value,String[] allowed){String n=norm(value);for(String a:allowed)if(norm(a).equals(n))return a;return "";}
    private static String displayCollege(String c){if(c.equals("lorehold"))return "Lorehold";if(c.equals("prismari"))return "Prismari";if(c.equals("quandrix"))return "Quandrix";if(c.equals("silverquill"))return "Silverquill";if(c.equals("witherbloom"))return "Witherbloom";return "";}
    private static String mascot(String c){if(c.equals("lorehold"))return "Spirit Statue Mascot";if(c.equals("prismari"))return "Art Elemental Mascot";if(c.equals("quandrix"))return "Fractal Mascot";if(c.equals("silverquill"))return "Inkling Mascot";if(c.equals("witherbloom"))return "Pest Mascot";return "";}
    private static void appendSpells(JSONObject s,Collection<String> xs)throws Exception{for(String x:xs)appendSpell(s,x);}private static void appendSpell(JSONObject s,String x)throws Exception{appendToken(s,"spells",x);}
    private static void appendToken(JSONObject o,String key,String value)throws Exception{String raw=o.optString(key,"");String nv=norm(value);for(String p:raw.split("[|,;•]") )if(norm(p).equals(nv))return;o.put(key,raw.trim().isEmpty()?value:raw+" | "+value);}
    private static void clearPending(JSONObject s,String key,String prefix)throws Exception{JSONArray a=s.optJSONArray(key);if(a==null)return;JSONArray n=new JSONArray();String p=norm(prefix);for(int i=0;i<a.length();i++)if(!norm(a.optString(i)).startsWith(p))n.put(a.optString(i));s.put(key,n);}
    private static String norm(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
