package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Locale;

/** Estado estruturado mínimo de Dados de Vida, espaços de magia e recursos recarregáveis. */
public final class CharacterResourceStateEngine {
    private CharacterResourceStateEngine(){}

    public static JSONObject ensure(JSONObject input,String className,int level,String profile){
        JSONObject s=copy(input);try{
            int lvl=Math.max(1,Math.min(20,level));DndRulesEngine rules=new DndRulesEngine();
            s.put("level",lvl);
            if(className!=null&&!className.isEmpty()&&!s.has("class_name"))s.put("class_name",className);
            s.put("hit_dice_max",lvl);s.put("hit_dice_spent",Math.max(0,Math.min(lvl,s.optInt("hit_dice_spent",0))));
            // RUN224: derive Spellcasting slots from the verified per-class history,
            // never from the primary class multiplied by total character level.
            // Reconciliation preserves spent slots and grants only newly earned capacity.
            if(!s.has("class_levels") &&
                (HomebrewClassRuntimeEngine.fullCaster(className)||HomebrewClassRuntimeEngine.halfCaster(className))){
                // Preserve the preexisting homebrew progression until its per-class
                // spellcasting ledger has been compiled and validated.
                JSONArray legacyMax=spellSlots(className,lvl,profile);
                if(legacyMax!=null){JSONArray old=s.optJSONArray("spell_slots_current");
                    s.put("spell_slots_max",legacyMax);
                    if(old==null||old.length()!=legacyMax.length())s.put("spell_slots_current",new JSONArray(legacyMax.toString()));
                    else s.put("spell_slots_current",boundedSlots(old,legacyMax));}
            }else s=MulticlassSpellSlotRules.reconcile(s,className,lvl,profile);
            if(!s.has("class_resources"))s.put("class_resources",new JSONObject());
            // RUN216: PHB 2014 monk Ki was missing from the persisted resource ledger.
            // Never replenish existing Ki during a UI preview/reconciliation; only rest may do that.
            if(!rules.is2024(profile)&&TashaQuickenedHealingCommitGuard.isMonk(className)&&lvl>=2){
                JSONObject resources=s.optJSONObject("class_resources");
                JSONObject ki=resources.optJSONObject("ki");
                if(ki==null){ki=new JSONObject().put("current",lvl);}
                ki.put("max",lvl).put("recharge","short_or_long_rest");
                ki.put("current",Math.max(0,Math.min(lvl,ki.optInt("current",lvl))));
                resources.put("ki",ki);s.put("class_resources",resources);
            }
            if(rules.is2024(profile))s=Core2024ClassRuntimeEngine.ensure(s,className,lvl);
            if(HomebrewClassRuntimeEngine.supports(className))s=HomebrewClassRuntimeEngine.ensure(s,className,lvl);
            HomebrewSubclassRuntimeEngine.applyToSheet(s);Core2024SubclassRuntimeEngine.applyToSheet(s);Run141CompactCharacterOptionEngine.applySubclassMechanics(s);Run143ForgottenSecretsEngine.applyPatronMechanics(s);ArcanaUnleashedRun143Engine.applySubclass(s);applyArcanaUnleashedFeatsAndBackground(s);s=ArcanaUnleashedRun145ItemEngine.applyInventoryPassives(s);s=ArcanaUnleashedRun147EvolvingItemsEngine.applyInventoryPassives(s);
            if(!s.has("inventory_ledger"))s.put("inventory_ledger",new JSONArray());
            if(!s.has("inventory_ledger_status"))s.put("inventory_ledger_status","structured_entries_only");
            s.put("resource_state_version",1).put("rules_profile",DndRulesEngine.canonicalProfile(profile));
        }catch(Exception ignored){AppLog.ignored("CharacterResourceStateEngine","Falha ao estruturar recursos.",ignored);}return s;
    }

    public static JSONObject spendHitDice(JSONObject input,String className,int level,String profile,int count,int healingRoll){JSONObject s=ensure(input,className,level,profile);try{int max=s.optInt("hit_dice_max",Math.max(1,level)),spent=s.optInt("hit_dice_spent",0);String blocked="";if(!s.optBoolean("short_rest_hit_dice_choice_pending",false))blocked="no_completed_short_rest_pending";else if(s.optInt("hp",0)<=0)blocked="requires_positive_hp";DndRulesEngine rules=new DndRulesEngine();int die=rules.classHitDie(className),con=rules.modifier(s.optInt("con",10));if(blocked.isEmpty())blocked=CharacterHitDiceRules.spendBlock(max,spent,count,die,con,healingRoll);if(!blocked.isEmpty())return s.put("short_rest_hit_dice_blocked",blocked).put("short_rest_hit_dice_spent_now",0).put("short_rest_healing",0);int hp=Math.max(0,s.optInt("hp",0)),hpMax=Math.max(1,s.optInt("max_hp",hp));int rolledHealing=CharacterHitDiceRules.healing(healingRoll,count,rules.is2024(profile));int healed=Math.max(0,Math.min(hpMax-hp,rolledHealing));s.remove("short_rest_hit_dice_blocked");s.put("hit_dice_spent",spent+count).put("hp",hp+healed).put("short_rest_hit_dice_spent_now",count).put("short_rest_healing",healed);}catch(Exception ignored){}return s;}

    public static JSONObject shortRest(JSONObject input,String className,int level,String profile){JSONObject s=ensure(input,className,level,profile);try{rechargeResources(s,"short_rest");JSONObject pact=s.optJSONObject("pact_magic");if(pact!=null)pact.put("current",pact.optInt("max",pact.optInt("current",0)));if(new DndRulesEngine().is2024(profile))s=Core2024ClassRuntimeEngine.shortRest(s,className,level);if(HomebrewClassRuntimeEngine.supports(className))s=HomebrewClassRuntimeEngine.shortRest(s,className,level);HomebrewSubclassRuntimeEngine.shortRest(s);Run141CompactCharacterOptionEngine.shortRest(s);run142Rest(s,"shortRest");Run149ForgottenSecretsSpellEngine.shortRest(s);EberronForgeCompleteEngine.shortRest(s);ForgottenRealmsHeroesEngine.shortRest(s);ArcanaUnleashedRun143Engine.shortRest(s);s.put("short_rest_hit_dice_choice_pending",Math.max(0,s.optInt("hit_dice_max",level)-s.optInt("hit_dice_spent",0))>0);}catch(Exception ignored){}return s;}

    public static JSONObject longRest(JSONObject input,String className,int level,String profile){JSONObject s=ensure(input,className,level,profile);try{int lvl=Math.max(1,Math.min(20,level)),spent=Math.max(0,s.optInt("hit_dice_spent",0));boolean revised=new DndRulesEngine().is2024(profile);spent-=CharacterHitDiceRules.recoveredOnLongRest(lvl,spent,revised);s.put("hit_dice_spent",spent).put("short_rest_hit_dice_choice_pending",false);JSONArray max=s.optJSONArray("spell_slots_max");if(max!=null)s.put("spell_slots_current",new JSONArray(max.toString()));JSONObject pact=s.optJSONObject("pact_magic");if(pact!=null)pact.put("current",pact.optInt("max",0));rechargeResources(s,"long_rest");if(revised)s=Core2024ClassRuntimeEngine.longRest(s,className,level);if(HomebrewClassRuntimeEngine.supports(className))s=HomebrewClassRuntimeEngine.longRest(s,className,level);HomebrewSubclassRuntimeEngine.longRest(s);Run141CompactCharacterOptionEngine.longRest(s);run142Rest(s,"longRest");Run149ForgottenSecretsSpellEngine.longRest(s);EberronForgeCompleteEngine.longRest(s);ForgottenRealmsHeroesEngine.longRest(s);ArcanaUnleashedRun143Engine.longRest(s);s=ArcanaUnleashedRun147EvolvingItemsEngine.longRest(s);}catch(Exception ignored){}return s;}

    public static JSONObject spendSpellSlot(JSONObject input,int level){JSONObject s=copy(input);try{if(s.has("multiclass_slot_validation_error"))return s.put("spell_slot_spend_blocked",true).put("reason","invalid_multiclass_slot_ledger");JSONArray cur=s.optJSONArray("spell_slots_current");if(cur==null||level<1||level>cur.length())return s.put("spell_slot_spend_blocked",true);int idx=level-1,n=cur.optInt(idx,0);if(n<=0)return s.put("spell_slot_spend_blocked",true);cur.put(idx,n-1);s.put("spell_slots_current",cur).put("spell_slot_spent_level",level);}catch(Exception ignored){}return s;}

    public static JSONObject spendPactSlot(JSONObject input){JSONObject s=copy(input);try{if(s.has("multiclass_slot_validation_error"))return s.put("pact_slot_spend_blocked",true).put("reason","invalid_multiclass_slot_ledger");JSONObject pact=s.optJSONObject("pact_magic");if(pact==null)return s.put("pact_slot_spend_blocked",true);int current=Math.max(0,pact.optInt("current",0));if(current<=0)return s.put("pact_slot_spend_blocked",true);pact.put("current",current-1);s.put("pact_magic",pact).put("pact_slot_spent_level",pact.optInt("slot_level",1));}catch(Exception ignored){}return s;}
    public static JSONObject spendClassResource(JSONObject input,String resourceKey,int amount){JSONObject s=copy(input);try{JSONObject all=s.optJSONObject("class_resources");if(all==null)return s.put("class_resource_spend_blocked",true);JSONObject r=all.optJSONObject(resourceKey);if(r==null)return s.put("class_resource_spend_blocked",true);int use=Math.max(1,amount),current=Math.max(0,r.optInt("current",0));if(current<use)return s.put("class_resource_spend_blocked",true).put("class_resource",resourceKey);r.put("current",current-use);all.put(resourceKey,r);s.put("class_resources",all).put("class_resource_spent",resourceKey).put("class_resource_spent_amount",use);}catch(Exception ignored){}return s;}

    private static void rechargeResources(JSONObject s,String rest){
        rechargeResourceMap(s.optJSONObject("class_resources"),rest);
        rechargeResourceMap(s.optJSONObject("subclass_resources"),rest);
    }
    private static void rechargeResourceMap(JSONObject r,String rest){
        if(r==null)return;Iterator<String>it=r.keys();while(it.hasNext()){String k=it.next();JSONObject x=r.optJSONObject(k);if(x==null)continue;String recharge=x.optString("recharge","");boolean ok="long_rest".equals(rest)&&(recharge.contains("long")||recharge.contains("short_or_long"))||"short_rest".equals(rest)&&(recharge.contains("short")||recharge.contains("short_or_long"));if(ok&&x.has("max"))try{x.put("current",x.optInt("max",x.optInt("current",0)));}catch(Exception ignored){}}
    }
    private static JSONArray boundedSlots(JSONArray cur,JSONArray max){JSONArray out=new JSONArray();for(int i=0;i<max.length();i++)out.put(Math.max(0,Math.min(max.optInt(i),cur.optInt(i))));return out;}
    private static JSONArray spellSlots(String cls,int level,String profile){String c=norm(cls);DndRulesEngine r=new DndRulesEngine();boolean full=c.contains("bardo")||c.contains("clerigo")||c.contains("druida")||c.contains("feiticeiro")||c.contains("mago")||HomebrewClassRuntimeEngine.fullCaster(cls);if(full)return r.fullCasterSlots(level);boolean half=c.contains("paladino")||c.contains("patrulheiro")||c.contains("guardiao")||HomebrewClassRuntimeEngine.halfCaster(cls);if(half){int effective;if(HomebrewClassRuntimeEngine.halfCaster(cls)){if(level<2)return zeros();effective=Math.max(1,level/2);}else if(r.is2024(profile))effective=Math.max(1,(level+1)/2);else{if(level<2)return zeros();effective=Math.max(1,level/2);}return r.fullCasterSlots(effective);}return null;}
    private static JSONObject pactSlots(String cls,int level){if(!norm(cls).contains("bruxo")&&!norm(cls).contains("warlock"))return null;int max=level==1?1:level<=10?2:level<=16?3:4;int slotLevel=level<=2?1:level<=4?2:level<=6?3:level<=8?4:5;try{return new JSONObject().put("max",max).put("slot_level",slotLevel);}catch(Exception e){return null;}}
    private static JSONArray zeros(){JSONArray a=new JSONArray();for(int i=0;i<9;i++)a.put(0);return a;}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
    private static String norm(String v){if(v==null)return "";String n=Normalizer.normalize(v.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}


    private static void applyArcanaUnleashedFeatsAndBackground(JSONObject s){try{String bg=s.optString("background_name",s.optString("background",""));if(ArcanaUnleashedRun143Engine.supportsBackground(bg))ArcanaUnleashedRun143Engine.applyBackground(s,bg);String raw=s.optString("selected_feats","");JSONArray feats=ArcanaUnleashedRun143Engine.allFeats();for(int i=0;i<feats.length();i++){String f=feats.optString(i);if(norm(raw).contains(norm(f)))ArcanaUnleashedRun143Engine.applyFeat(s,f);}}catch(Throwable ignored){}}
    private static void applyRun141Subclasses(JSONObject s){try{Object x=Class.forName("com.braseiro.dnd5e.AncestralStancesRuntimeEngine").getMethod("apply",JSONObject.class).invoke(null,s);if(x instanceof JSONObject){JSONObject y=(JSONObject)x;for(java.util.Iterator<String>it=y.keys();it.hasNext();){String k=it.next();s.put(k,y.opt(k));}}}catch(Throwable ignored){}try{Class.forName("com.braseiro.dnd5e.ForgottenSecretsRun141Engine").getMethod("applyToSheet",JSONObject.class).invoke(null,s);}catch(Throwable ignored){}}
    private static void run141Rest(JSONObject s,String method){for(String c:new String[]{"com.braseiro.dnd5e.AncestralStancesRuntimeEngine","com.braseiro.dnd5e.ForgottenSecretsRun141Engine"})try{Class.forName(c).getMethod(method,JSONObject.class).invoke(null,s);}catch(Throwable ignored){}}
    private static void run142Rest(JSONObject s,String method){try{Class<?> c=Class.forName("com.braseiro.dnd5e.Run143ForgottenSecretsEngine");c.getMethod(method,JSONObject.class).invoke(null,s);}catch(Throwable ignored){}}

}
