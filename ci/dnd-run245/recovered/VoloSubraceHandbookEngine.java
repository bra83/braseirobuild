package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** RUN138 — compilador incremental de Volo's Complete Subrace Handbook (v1.0).
 * Apenas opções cujo fluxo efeito→estado está completo são expostas à criação.
 * O restante permanece explicitamente PENDENTE, nunca contado como cobertura.
 */
public final class VoloSubraceHandbookEngine {
    public static final String SOURCE="D&D5e - Volo's Complete Subrace Handbook (v1.0)";
    public static final String SOURCE_SHA="d80d23aa5c7e0ae0d1b2105471e5b9f34cea558ae0940659d017f2b5e9f6f0ab";
    public static final String AZER="Anão — Azerblood (Volo Subrace Handbook)";
    public static final String DREAM="Anão — Dream Dwarf (Volo Subrace Handbook)";
    public static final String BLACKSCALE="Povo Lagarto — Blackscale (Volo Subrace Handbook)";
    public static final String POISON_DUSK="Povo Lagarto — Poison Dusk (Volo Subrace Handbook)";
    public static final String PRIDEHEAD="Tabaxi — Pridehead (Volo Subrace Handbook)";
    public static final String STRIPEBACK="Tabaxi — Stripeback (Volo Subrace Handbook)";
    private static final String[] READY={AZER,DREAM,BLACKSCALE,POISON_DUSK,PRIDEHEAD,STRIPEBACK};
    private static final String[] PENDING={
      "Elfo — Variant Sea Elf",
      "Golias — Cloudborn","Golias — Fireborn","Golias — Frostborn","Golias — Hillborn","Golias — Stoneborn","Golias — Stormborn",
      "Halfling — Ghostwise (Revised)","Halfling — Lightfoot (Revised)","Halfling — Stout (Revised)","Halfling — Tallfellow",
      "Hobgoblin — Brightnose","Hobgoblin — Sunscorch","Hobgoblin — Koalinth (Sea Hobgoblin)",
      "Kenku — Harrowfeather","Kenku — Shroudeye","Kenku — Snatchbeak",
      "Kobold — Dragonwrought","Kobold — Lickspittle","Kobold — Pitstalker",
      "Orc — Gray Orc","Orc — Mountain Orc","Orc — Orog (Underdark Orc)","Orc — Neo-Orog (Thayan Red Orc)",
      "Yuan-Ti — Malison","Yuan-Ti — Pureblood (Revised)","Grung revisado + cores",
      "Aasimar — Angelic Heritage","Aasimar — Alternate Appearance","Bugbear — Chameleon","Bugbear — Strangleclaw","Bugbear — Worghest",
      "Dragonborn — variantes de sopro","Firbolg — Bloodbear","Firbolg — Ghostraven","Firbolg — Primalstag","Human — Variant Human 2","Tiefling — Legacies demoníacos"
    };    private VoloSubraceHandbookEngine(){}

    public static JSONArray allReadySpecies(){JSONArray a=new JSONArray();for(String x:READY)a.put(x);return a;}
    public static JSONArray pendingDocumentOptions(){JSONArray a=new JSONArray();for(String x:PENDING)a.put(x);return a;}
    public static boolean isReady(String species){String q=n(species);for(String x:READY)if(q.equals(n(x)))return true;return false;}
    public static JSONObject descriptor(){JSONObject o=new JSONObject();try{return o.put("source",SOURCE).put("source_sha256",SOURCE_SHA).put("selection_ready",READY.length).put("pending_groups",PENDING.length).put("ambiguous",0).put("effect_pipeline_complete",false).put("status","PARTIAL_COMPILED_DO_NOT_COUNT_AS_BOOK_CLOSED").put("auto_rolls",false);}catch(Exception e){return o;}}
    public static JSONObject lookup(String species){JSONObject o=new JSONObject();try{boolean ready=isReady(species);return o.put("known",ready).put("species",species).put("source",SOURCE).put("runtime","VoloSubraceHandbookEngine").put("mechanics_compiled",ready).put("effect_pipeline_complete",ready);}catch(Exception e){return o;}}

    public static JSONObject install(){JSONObject out=new JSONObject();JSONArray failed=new JSONArray();int ok=0;try{for(String sp:READY){String eid="run138.volo_species."+n(sp).replace(' ','_');JSONObject ev=new JSONObject().put("evidence_id",eid).put("content_kind","species").put("content_name",sp).put("source_book",SOURCE).put("source_locator","subrace/variant rule block").put("source_sha256",SOURCE_SHA).put("revision",1);JSONObject er=AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev);if(!er.optBoolean("registered",false)){failed.put(sp);continue;}JSONObject basis=new JSONObject().put("source_type","reviewed_evidence").put("evidence_id",eid).put("content_kind","species").put("content_name",sp).put("source_book",SOURCE).put("source_locator","subrace/variant rule block").put("source_sha256",SOURCE_SHA).put("revision",1);JSONObject mech=new JSONObject().put("effect_pipeline_complete",true).put("execution_status","compiled_effect_pipeline").put("runtime_resolver","VoloSubraceHandbookEngine.resolve").put("auto_rolls",false).put("auto_mutation_allowed",false);JSONObject rr=CompiledContentMechanicsEngine.register("species",sp,mech,basis);if(rr.optBoolean("registered",false))ok++;else failed.put(sp);}return out.put("installed",ok).put("expected",READY.length).put("failed",failed).put("complete",ok==READY.length).put("book_complete",false).put("pending_document_options",pendingDocumentOptions());}catch(Exception e){try{return out.put("installed",ok).put("expected",READY.length).put("complete",false).put("book_complete",false).put("failed",failed);}catch(Exception ignored){return out;}}}

    public static boolean apply2014AbilityBonuses(JSONObject s){try{String q=n(s.optString("species",""));if(q.equals(n(AZER))){add(s,"con",2);add(s,"wis",1);return true;}if(q.equals(n(DREAM))){add(s,"con",2);pending(s,"dream_dwarf_asi_choose_wis_or_int_plus_1");return true;}if(q.equals(n(BLACKSCALE))){add(s,"con",2);add(s,"str",1);return true;}if(q.equals(n(POISON_DUSK))){add(s,"con",1);add(s,"dex",1);add(s,"int_score",1);return true;}if(q.equals(n(PRIDEHEAD))){add(s,"con",1);add(s,"cha",1);add(s,"str",1);return true;}if(q.equals(n(STRIPEBACK))){add(s,"dex",2);add(s,"str",1);return true;}}catch(Exception ignored){}return false;}

    public static boolean applySpeciesMechanics(JSONObject s){try{String q=n(s.optString("species",""));if(!isReady(s.optString("species","")))return false;s.put("volo_subrace_source",SOURCE).put("volo_subrace_runtime","VoloSubraceHandbookEngine").put("volo_subrace_auto_rolls",false);JSONObject r=s.optJSONObject("species_resources");if(r==null)r=new JSONObject();if(q.equals(n(AZER))){res(r,"inner_fire_heat_metal",1,"long_rest");append(s,"damage_resistances","fogo");s.put("inner_fire_min_level",5).put("inner_fire_spell","heat metal, 2º nível").put("inner_fire_ability","Sabedoria");}
        else if(q.equals(n(DREAM))){res(r,"earth_dream",1,"long_rest");s.put("ethereal_sight_range_ft",30).put("ethereal_sight_bonus_action",true).put("earth_dream_natural_environment_only",true).put("earth_dream_radius_ft",500);}
        else if(q.equals(n(BLACKSCALE))){s.put("bite_damage","1d8 + Força").put("bite_grapple_bonus_action_on_hit",true).put("powerful_build",true).put("swim_speed_removed",true);}
        else if(q.equals(n(POISON_DUSK))){s.put("size","Pequeno").put("bite_damage","1d4 + Destreza").put("bite_poison_dice",poisonDice(s.optInt("level",1))).put("bite_poison_save","Constituição").put("bite_poison_dc_formula","8 + CON + proficiência").put("color_shift_bonus_action",true).put("color_shift_stealth_advantage_if_stationary_or_half_speed",true);}
        else if(q.equals(n(PRIDEHEAD))){res(r,"lion_roar",1,"short_or_long_rest");s.put("powerful_build",true);append(s,"skill_proficiencies","Atletismo");append(s,"skill_proficiencies","Intimidação");}
        else if(q.equals(n(STRIPEBACK))){res(r,"pouncing_strike",2,"short_or_long_rest");s.put("claws_damage","1d6 + Força").put("jump_distance_doubled",true);}
        s.put("species_resources",r);return true;}catch(Exception e){return false;}}

    public static JSONArray actionsFor(String species,int level){JSONArray a=new JSONArray();String q=n(species);if(q.equals(n(AZER))&&level>=5)a.put("Fogo Interior");else if(q.equals(n(DREAM))){a.put("Escolher Atributo Dream Dwarf");a.put("Visão Etérea");a.put("Sonho da Terra");}else if(q.equals(n(BLACKSCALE)))a.put("Mordida Blackscale");else if(q.equals(n(POISON_DUSK))){a.put("Mordida Venenosa");a.put("Mudança de Cor");}else if(q.equals(n(PRIDEHEAD)))a.put("Rugido do Leão");else if(q.equals(n(STRIPEBACK)))a.put("Ataque Saltitante");return a;}
    public static JSONObject resolve(JSONObject input,String species,int level,String action,JSONObject payload){JSONObject s=copy(input);try{applySpeciesMechanics(s);String q=n(species),a=n(action);if(payload==null)payload=new JSONObject();if(q.equals(n(AZER))&&a.equals("fogo interior")){if(level<5)return block(s,"min_level_5");if(!spend(s,"inner_fire_heat_metal",1))return block(s,"inner_fire_heat_metal");String target=payload.optString("target_id","");if(target.isEmpty()){restore(s,"inner_fire_heat_metal");return need(s,"target_id");}return mark(s,"volo_azerblood_inner_fire",new JSONObject().put("spell","heat metal").put("spell_level",2).put("ability","Sabedoria").put("target_id",target));}
        if(q.equals(n(DREAM))&&a.equals("escolher atributo dream dwarf")){if(s.optBoolean("dream_dwarf_asi_applied",false))return block(s,"dream_dwarf_asi_already_applied");String choice=n(payload.optString("ability",""));if(!(choice.equals("sabedoria")||choice.equals("inteligencia")))return need(s,"ability=Sabedoria|Inteligência");String key=choice.equals("sabedoria")?"wis":"int_score";add(s,key,1);s.put("dream_dwarf_asi_applied",true);removePending(s,"dream_dwarf_asi_choose_wis_or_int_plus_1");return mark(s,"volo_dream_dwarf_asi_choice",new JSONObject().put("ability",payload.optString("ability")).put("bonus",1));}
        if(q.equals(n(DREAM))&&a.equals("visao eterea")){boolean active=payload.optBoolean("active",true);s.put("ethereal_sight_active",active);return mark(s,"volo_dream_dwarf_ethereal_sight",new JSONObject().put("active",active).put("range_ft",30));}
        if(q.equals(n(DREAM))&&a.equals("sonho da terra")){if(!payload.optBoolean("natural_environment_confirmed",false))return need(s,"natural_environment_confirmed=true + subject");String subject=payload.optString("subject","");if(subject.isEmpty())return need(s,"subject");if(!spend(s,"earth_dream",1))return block(s,"earth_dream");return mark(s,"volo_dream_dwarf_earth_dream",new JSONObject().put("subject",subject).put("radius_ft",500).put("gm_fact_required",true));}
        if(q.equals(n(BLACKSCALE))&&a.equals("mordida blackscale")){if(!payload.optBoolean("hit_confirmed",false))return need(s,"hit_confirmed=true + target_size");String size=payload.optString("target_size","");return mark(s,"volo_blackscale_bite",new JSONObject().put("damage","1d8 + Força").put("target_size",size).put("bonus_action_grapple_available",smallOrMedium(size)));}
        if(q.equals(n(POISON_DUSK))&&a.equals("mordida venenosa")){if(!payload.optBoolean("hit_confirmed",false))return need(s,"hit_confirmed=true");int dc=8+new DndRulesEngine().proficiencyBonus(Math.max(1,level))+new DndRulesEngine().modifier(s.optInt("con",10));return mark(s,"volo_poison_dusk_bite",new JSONObject().put("base_damage","1d4 + Destreza").put("poison_damage",poisonDice(level)).put("save","Constituição").put("save_dc",dc).put("target_save_pending",true));}
        if(q.equals(n(POISON_DUSK))&&a.equals("mudanca de cor")){boolean active=payload.optBoolean("active",true);s.put("color_shift_active",active);return mark(s,"volo_poison_dusk_color_shift",new JSONObject().put("active",active).put("stealth_advantage_if_stationary_or_half_speed",true));}
        if(q.equals(n(PRIDEHEAD))&&a.equals("rugido do leao")){if(!spend(s,"lion_roar",1))return block(s,"lion_roar");int dc=8+new DndRulesEngine().proficiencyBonus(Math.max(1,level))+new DndRulesEngine().modifier(s.optInt("cha",10));return mark(s,"volo_pridehead_lion_roar",new JSONObject().put("range_ft",30).put("save","Carisma").put("save_dc",dc).put("condition","amedrontado").put("until_end_of_next_turn",true).put("targets_chosen_manually",true));}
        if(q.equals(n(STRIPEBACK))&&a.equals("ataque saltitante")){if(!spend(s,"pouncing_strike",1))return block(s,"pouncing_strike");if(!payload.optBoolean("hit_confirmed",false)||payload.optString("target_size","").isEmpty()){restore(s,"pouncing_strike");return need(s,"hit_confirmed=true + target_size");}int dc=8+new DndRulesEngine().proficiencyBonus(Math.max(1,level))+new DndRulesEngine().modifier(s.optInt("dex",10));return mark(s,"volo_stripeback_pouncing_strike",new JSONObject().put("unarmed_attack",true).put("save","Força").put("save_dc",dc).put("prone_on_fail",smallOrMediumOrLarge(payload.optString("target_size",""))).put("target_save_pending",true));}
        return block(s,"unknown_volo_species_action");}catch(Exception e){return s;}}

    public static void shortRest(JSONObject s){try{applySpeciesMechanics(s);restoreToMax(s,"lion_roar");restoreToMax(s,"pouncing_strike");}catch(Exception ignored){}}
    public static void longRest(JSONObject s){try{applySpeciesMechanics(s);JSONObject r=s.optJSONObject("species_resources");if(r==null)return;java.util.Iterator<String> it=r.keys();while(it.hasNext()){JSONObject x=r.optJSONObject(it.next());if(x!=null)x.put("current",x.optInt("max",0));}}catch(Exception ignored){}}
    public static String speedOverride(String species){String q=n(species);if(q.equals(n(POISON_DUSK))||q.equals(n(BLACKSCALE))||q.equals(n(PRIDEHEAD))||q.equals(n(STRIPEBACK))||q.equals(n(AZER))||q.equals(n(DREAM)))return "9 m";return "";}

    private static void pending(JSONObject s,String value)throws Exception{JSONArray a=s.optJSONArray("species_choices_pending");if(a==null)a=new JSONArray();for(int i=0;i<a.length();i++)if(value.equals(a.optString(i)))return;a.put(value);s.put("species_choices_pending",a);}
    private static void removePending(JSONObject s,String value)throws Exception{JSONArray a=s.optJSONArray("species_choices_pending");if(a==null)return;JSONArray n=new JSONArray();for(int i=0;i<a.length();i++)if(!value.equals(a.optString(i)))n.put(a.optString(i));s.put("species_choices_pending",n);}
    private static void add(JSONObject s,String key,int v)throws Exception{s.put(key,Math.min(20,s.optInt(key,10)+v));}
    private static void append(JSONObject s,String key,String v)throws Exception{JSONArray a=s.optJSONArray(key);if(a==null)a=new JSONArray();for(int i=0;i<a.length();i++)if(v.equals(a.optString(i)))return;a.put(v);s.put(key,a);}
    private static JSONObject res(JSONObject all,String k,int max,String recharge)throws Exception{JSONObject r=all.optJSONObject(k);if(r==null)r=new JSONObject().put("current",max);r.put("max",max).put("current",Math.min(max,Math.max(0,r.optInt("current",max)))).put("recharge",recharge);all.put(k,r);return r;}
    private static JSONObject speciesResources(JSONObject s)throws Exception{JSONObject r=s.optJSONObject("species_resources");if(r==null){r=new JSONObject();s.put("species_resources",r);}return r;}
    private static boolean spend(JSONObject s,String k,int v)throws Exception{JSONObject x=speciesResources(s).optJSONObject(k);if(x==null||x.optInt("current",0)<v)return false;x.put("current",x.optInt("current",0)-v);return true;}
    private static void restore(JSONObject s,String k)throws Exception{JSONObject x=speciesResources(s).optJSONObject(k);if(x!=null)x.put("current",Math.min(x.optInt("max",0),x.optInt("current",0)+1));}
    private static void restoreToMax(JSONObject s,String k)throws Exception{JSONObject x=speciesResources(s).optJSONObject(k);if(x!=null)x.put("current",x.optInt("max",0));}
    private static JSONObject mark(JSONObject s,String event,JSONObject effect)throws Exception{return s.put("species_action_resolved",true).put("species_action_event",event).put("species_action_effect",effect).put("species_action_auto_rolls",false);}
    private static JSONObject need(JSONObject s,String input)throws Exception{return s.put("species_action_resolved",false).put("species_action_status","requires_player_or_gm_resolution").put("species_action_required_input",input).put("species_action_auto_rolls",false);}
    private static JSONObject block(JSONObject s,String reason)throws Exception{return s.put("species_action_resolved",false).put("species_action_status","blocked").put("species_action_reason",reason).put("species_action_auto_rolls",false);}
    private static String poisonDice(int level){return level>=16?"5d4":level>=11?"4d4":level>=6?"3d4":"2d4";}
    private static boolean smallOrMedium(String size){String q=n(size);return q.contains("pequeno")||q.contains("medio")||q.equals("small")||q.equals("medium");}
    private static boolean smallOrMediumOrLarge(String size){String q=n(size);return smallOrMedium(size)||q.contains("grande")||q.equals("large");}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
    private static String n(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
