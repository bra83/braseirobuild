package com.braseiro.dnd5e;
import org.json.*;
/** Atomic turn-aware bridge between compiled attacks and persistent encounter state. */
public final class StatblockCombatFlowEngine{
 private StatblockCombatFlowEngine(){}
 public static synchronized JSONObject attack(String attacker,String target,String action,int d20){return attack(attacker,target,action,d20,d20,0d);}
 public static synchronized JSONObject attack(String attacker,String target,String action,int firstD20,int secondD20,double distanceFt){return attackResolved(attacker,target,action,firstD20,secondD20,distanceFt,null);}
 public static synchronized JSONObject legendaryAction(String attacker,String target,String action,int d20){return legendaryAction(attacker,target,action,d20,d20);}
 public static synchronized JSONObject legendaryAction(String attacker,String target,String action,int firstD20,int secondD20){/*BR_JSON_WRAP_106*/try{
  JSONObject av=StatblockEncounterStateEngine.view(attacker),tv=StatblockEncounterStateEngine.view(target);
  if(!av.optBoolean("found")||!tv.optBoolean("found"))return block("token_not_found");
  if(av.optInt("hp",0)<=0)return block("attacker_defeated");
  if(tv.optInt("hp",0)<=0)return block("target_defeated");
  JSONObject ts=StatblockTurnEngine.state();
  if(ts.optInt("round",0)<1)return block("encounter_not_started");
  if(attacker.equals(ts.optString("current_token")))return block("legendary_action_forbidden_on_own_turn");
  JSONObject ae=economy(ts,attacker);
  if(ae==null)return block("token_not_in_initiative");
  JSONObject declaredAction=findAction(av.optString("statblock_id"),action);
  if(declaredAction==null)return block("action_not_found");
  if("Monster Manual (2025)".equals(CompiledStatblockEngine.get(av.optString("statblock_id")).optString("source_book"))&&
      declaredAction.optBoolean("manual_unmodeled_source_effect_required",false))
   return block("manual_source_action_resolution_required");
  if(!"legendary_action".equalsIgnoreCase(declaredAction.optString("category","")))return block("not_a_legendary_action");
  int cost=declaredAction.optInt("legendary_cost",1);if(cost<=0)cost=1;
  int remainingLeg=ae.optInt("legendary_points_remaining",0);
  if(remainingLeg<cost)return block("legendary_points_exhausted");

  JSONObject tp=CompiledStatblockEngine.runtimeProfile(tv.optString("statblock_id"));
  JSONObject r;
  if(declaredAction.has("spells")){
   JSONArray spells=declaredAction.optJSONArray("spells");
   String chosenSpell=resolveDeclaredSpellName(action,spells,declaredAction);
   if(!isSupportedLegendarySpell(chosenSpell,declaredAction))return block("unsupported_legendary_spell");
   r=new JSONObject().put("resolved",true).put("resolution_type","spellcasting_action")
     .put("statblock_id",av.optString("statblock_id")).put("action",action)
     .put("spells",spells).put("selected_spell",chosenSpell)
     .put("effect_on_fail",declaredAction.optString("effect_on_fail",""));
   if(declaredAction.has("damage")){
    r.put("damage_expression",declaredAction.optString("damage")).put("damage_type",declaredAction.optString("damage_type",""));
   }
  }else if(declaredAction.has("to_hit")){
   int ac=tp.optInt("armor_class",10);
   r=StatblockCombatRuntimeEngine.attack(av.optString("statblock_id"),action,firstD20,ac,av.optInt("hp",-1),av.optInt("max_hp",-1));
  }else if(declaredAction.has("save_dc")||declaredAction.has("save_ability")){
   int saveBonus=calculateTargetSaveBonus(tp,declaredAction.optString("save_ability",""));
   int effectiveSaveRoll=firstD20+saveBonus;
   r=StatblockCombatRuntimeEngine.savingThrowAction(av.optString("statblock_id"),action,effectiveSaveRoll);
   r.put("natural_d20",firstD20).put("target_save_bonus",saveBonus);
  }else{
   r=new JSONObject().put("resolved",true).put("resolution_type","special_legendary_action")
     .put("statblock_id",av.optString("statblock_id")).put("action",action)
     .put("effect_on_fail",declaredAction.optString("effect_on_fail",""));
   if(declaredAction.has("damage")){
    r.put("damage_expression",declaredAction.optString("damage")).put("damage_type",declaredAction.optString("damage_type",""));
   }
  }
  if(r.optBoolean("blocked"))return r;

  JSONObject encounterBefore=StatblockEncounterStateEngine.snapshot(),turnBefore=StatblockTurnEngine.snapshot();
  try{
   JSONObject spent=StatblockTurnEngine.spendLegendary(attacker,cost);
   if(spent.optBoolean("blocked"))return spent;

   if("attack_roll".equals(r.optString("resolution_type"))){
    if(r.optBoolean("hit")){
     JSONObject damage=applyAttackDamage(target,r,r.optBoolean("critical"),1.0d);
     r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",damage.optInt("applied_damage"))
      .put("damage_components",damage.optJSONArray("components")).put("target_defeated",damage.optBoolean("target_defeated"));
     String onHitCond=r.optString("rider_on_hit_condition","");
     if(!onHitCond.isEmpty()){
      for(String c:onHitCond.split("[,;]")){String tc=c.trim();if(!tc.isEmpty())StatblockEncounterStateEngine.condition(target,tc,true);}
      int durRounds=r.optInt("rider_on_hit_duration_rounds",0);
      if(durRounds>0)StatblockEncounterStateEngine.setConditionRounds(target,onHitCond,durRounds);
      r.put("rider_on_hit_applied",true);
     }
     if(r.optBoolean("rider_save_required",false)){
      int saveDc=r.optInt("rider_save_dc",10);
      int riderSaveBonus=calculateTargetSaveBonus(tp,r.optString("rider_save_ability",declaredAction.optString("save_ability","str")));
      int saveRoll=secondD20+riderSaveBonus;
      boolean pass=saveRoll>=saveDc;
      r.put("rider_save_bonus",riderSaveBonus).put("rider_save_total",saveRoll).put("rider_save_success",pass);
      if(!pass){
       String failCond=r.optString("rider_condition_on_fail","");
       if(!failCond.isEmpty()){
        for(String c:failCond.split("[,;]")){String tc=c.trim();if(!tc.isEmpty())StatblockEncounterStateEngine.condition(target,tc,true);}
        r.put("rider_condition_applied",failCond);
       }
       String failEff=r.optString("rider_effect_on_fail","");
       if(!failEff.isEmpty())applySpecialEffect(attacker,target,failEff,r);
      }
     }
    }
   }else if("saving_throw".equals(r.optString("resolution_type"))){
    boolean saveSuccess=r.optBoolean("save_success",false);
    String dmgExpr=r.optString("damage_expression","");
    double mult=r.optDouble("damage_multiplier",1d);
    if(!dmgExpr.isEmpty()&&mult>0d){
     JSONObject damage=applyAttackDamage(target,r,false,mult);
     r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",damage.optInt("applied_damage"))
      .put("damage_components",damage.optJSONArray("components")).put("target_defeated",damage.optBoolean("target_defeated"));
    }else{
     r.put("raw_damage",0).put("applied_damage",0).put("damage_components",new JSONArray()).put("target_defeated",false);
    }
    if(!saveSuccess){
     String cond=r.optString("condition_on_fail","");
     if(!cond.isEmpty())StatblockEncounterStateEngine.condition(target,cond,true);
     String eff=r.optString("effect_on_fail","");
     if(!eff.isEmpty())applySpecialEffect(attacker,target,eff,r);
    }
   }else if("spellcasting_action".equals(r.optString("resolution_type"))){
    executeLegendarySpell(attacker,target,r.optJSONArray("spells"),firstD20,declaredAction,r);
    String eff=declaredAction.optString("effect_on_fail","");
    if(!eff.isEmpty())applySpecialEffect(attacker,target,eff,r);
   }else if("special_legendary_action".equals(r.optString("resolution_type"))){
    if(r.has("damage_expression")&&!r.optString("damage_expression","").isEmpty()){
     JSONObject damage=applyAttackDamage(target,r,false,1.0d);
     r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",damage.optInt("applied_damage"))
      .put("damage_components",damage.optJSONArray("components")).put("target_defeated",damage.optBoolean("target_defeated"));
    }
    String cond=declaredAction.optString("condition_on_fail","");
    if(!cond.isEmpty())StatblockEncounterStateEngine.condition(target,cond,true);
    String eff=declaredAction.optString("effect_on_fail","");
    if(!eff.isEmpty())applySpecialEffect(attacker,target,eff,r);
   }

   return r.put("attacker",attacker).put("target",target).put("legendary_action",true)
     .put("cost",cost).put("target_state",StatblockEncounterStateEngine.view(target)).put("turn_state",StatblockTurnEngine.state());
  }catch(Exception mutationFailure){
   StatblockEncounterStateEngine.restore(encounterBefore);
   StatblockTurnEngine.restore(turnBefore);
   throw mutationFailure;
  }
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 /** Resolves the same turn attack after validating its live 3D tactical scene. */
 public static synchronized JSONObject attackWithGeometry(String attacker,String target,String action,
  int firstD20,int secondD20,int width,int height,int levels,
  TacticalCreatureGeometryRules.Creature attackerVolume,TacticalCreatureGeometryRules.Creature targetVolume,
  boolean[][][] opaque,int[][][] cover,boolean[][][] occupied,String kind,int reach,int normal,int far,
  boolean targetLocationKnown,boolean targetVisible,int blindsightFeet){
  TacticalCreatureGeometryRules.Result geometry=TacticalCreatureGeometryRules.evaluate(width,height,levels,
   attackerVolume,targetVolume,opaque,cover,occupied,kind,reach,normal,far,targetLocationKnown,targetVisible,blindsightFeet);
  if(!geometry.legal())return block(geometry.blocked);
  return attackResolved(attacker,target,action,firstD20,secondD20,geometry.distanceFeet,geometry);
 }
 private static JSONObject attackResolved(String attacker,String target,String action,int firstD20,int secondD20,
  double distanceFt,TacticalCreatureGeometryRules.Result geometry){/*BR_JSON_WRAP_106*/try{
  JSONObject ts=StatblockTurnEngine.state(); if(!attacker.equals(ts.optString("current_token")))return block("not_current_turn");
  JSONObject ae=economy(ts,attacker);if(ae==null||!ae.optBoolean("action"))return block("action_resource_spent");
  JSONObject av=StatblockEncounterStateEngine.view(attacker),tv=StatblockEncounterStateEngine.view(target);if(!av.optBoolean("found")||!tv.optBoolean("found"))return block("token_not_found");if(av.optInt("hp",0)<=0)return block("attacker_defeated");if(tv.optInt("hp",0)<=0)return block("target_defeated");
  JSONObject declaredAction=findAction(av.optString("statblock_id"),action);if(declaredAction==null)return block("action_not_found");// The legacy action path spends only a normal action; it must never
  // execute reactions, bonus actions, lair or legendary abilities using this budget.
  if(!"action".equals(MonsterActionEconomyGuard.standaloneCost(
      declaredAction.optString("category",""))))return block("unsupported_non_action_economy_in_legacy_flow");
  String riderBlock=MonsterActionEconomyGuard.attackRiderBlock(declaredAction,false);if("unsupported_multiattack_on_hit_rider".equals(riderBlock))return block("unsupported_on_hit_rider_in_legacy_flow");if(!riderBlock.isEmpty())return block(riderBlock);
  JSONObject tp=CompiledStatblockEngine.runtimeProfile(tv.optString("statblock_id"));DndRulesEngine rules=new DndRulesEngine();String cover=tv.optString("cover","");if(rules.hasTotalCover(cover))return block("target_has_total_cover");int geometryCover=geometry==null?0:geometry.coverBonus;int ac=tp.optInt("armor_class",10)+Math.max(rules.coverAcBonus(cover),geometryCover);JSONObject range=StatblockCombatRuntimeEngine.rangeProfile(av.optString("statblock_id"),action,distanceFt);if(range.optBoolean("blocked"))return range;if(range.optBoolean("out_of_range"))return block("attack_out_of_range").put("range",range);
  XmmConditionalAttackRollRules.Result sourceRollMode=XmmConditionalAttackRollRules.advantage(
      declaredAction,tv.optInt("hp",-1),tv.optInt("max_hp",-1));
  if(!sourceRollMode.legal())return block(sourceRollMode.blocked);
  JSONObject cp=ConditionRulesEngine.attackProfile(StatblockEncounterStateEngine.conditionText(attacker),StatblockEncounterStateEngine.conditionText(target),distanceFt,true,true,false,false,DndRulesEngine.PROFILE_2014);if(cp.optBoolean("blocked",false))return block("attacker_incapacitated");if(sourceRollMode.advantage)cp.put("advantage",true);if(range.optBoolean("long_range_disadvantage",false)||(range.optBoolean("ranged",false)&&distanceFt>0d&&distanceFt<=5d&&!ConditionRulesEngine.incapacitated(StatblockEncounterStateEngine.conditionText(target)))||(geometry!=null&&(geometry.longRangeDisadvantage||geometry.unseenDisadvantage)))cp.put("disadvantage",true);int used=rules.chooseD20(firstD20,secondD20,cp.optBoolean("advantage",false),cp.optBoolean("disadvantage",false));
  JSONObject r=StatblockCombatRuntimeEngine.attack(av.optString("statblock_id"),action,used,ac,
      av.optInt("hp",-1),av.optInt("max_hp",-1),false,
      cp.optBoolean("advantage",false)&&!cp.optBoolean("disadvantage",false));if(r.optBoolean("blocked"))return r;r.put("first_d20",firstD20).put("second_d20",secondD20).put("roll_mode",cp.optBoolean("advantage",false)&&!cp.optBoolean("disadvantage",false)?"advantage":cp.optBoolean("disadvantage",false)&&!cp.optBoolean("advantage",false)?"disadvantage":"normal").put("target_cover",cover).put("range",range);if(geometry!=null)r.put("attack_geometry_receipt",TacticalCreatureGeometryRules.receipt(geometry)).put("geometry_cover_bonus",geometryCover).put("unseen_disadvantage",geometry.unseenDisadvantage).put("blindsight_used",geometry.usedBlindsight);if(r.optBoolean("hit")&&cp.optBoolean("critical_on_hit",false))r.put("critical",true).put("critical_from_condition",true);
  if(declaredAction.optBoolean("returns_to_attacker_after_ranged_attack",false)&&range.optBoolean("ranged",false))r.put("weapon_returned",true);
  // Preflight passed. Commit the action and damage as one transaction; a bad
  // source-bound damage expression must never spend the action or alter HP.
  JSONObject encounterBefore=StatblockEncounterStateEngine.snapshot(),turnBefore=StatblockTurnEngine.snapshot();
  try{
   JSONObject spent=StatblockTurnEngine.spend(attacker,"action");if(spent.optBoolean("blocked"))return spent;
   if(r.optBoolean("hit")){JSONObject damage=applyAttackDamage(target,r,r.optBoolean("critical"));r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",damage.optInt("applied_damage")).put("damage_components",damage.optJSONArray("components")).put("target_defeated",damage.optBoolean("target_defeated"));}
   return r.put("attacker",attacker).put("target",target).put("target_state",StatblockEncounterStateEngine.view(target)).put("turn_state",StatblockTurnEngine.state());
  }catch(Exception failedMutation){
   StatblockEncounterStateEngine.restore(encounterBefore);
   StatblockTurnEngine.restore(turnBefore);
   throw failedMutation;
  }
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
 public static synchronized JSONObject multiattack(String attacker,String target,String[] actions,int[] d20s){
  if(d20s==null)return block("invalid_multiattack_input");int[] second=new int[d20s.length];System.arraycopy(d20s,0,second,0,d20s.length);
  JSONObject preview=ConditionRulesEngine.attackProfile(StatblockEncounterStateEngine.conditionText(attacker),StatblockEncounterStateEngine.conditionText(target),0d,true,true,false,false,DndRulesEngine.PROFILE_2014);
  if(preview.optBoolean("advantage",false)!=preview.optBoolean("disadvantage",false))return block("multiattack_requires_paired_rolls");
  return multiattack(attacker,target,actions,d20s,second,0d);
 }
 public static synchronized JSONObject multiattack(String attacker,String target,String[] actions,int[] firstD20s,int[] secondD20s,double distanceFt){/*BR_JSON_WRAP_106*/try{
  if(actions==null||firstD20s==null||secondD20s==null||actions.length==0||actions.length!=firstD20s.length||actions.length!=secondD20s.length)return block("invalid_multiattack_input");
  JSONObject ts=StatblockTurnEngine.state();if(!attacker.equals(ts.optString("current_token")))return block("not_current_turn");JSONObject ae=economy(ts,attacker);if(ae==null||!ae.optBoolean("action"))return block("action_resource_spent");
  JSONObject av=StatblockEncounterStateEngine.view(attacker),tv=StatblockEncounterStateEngine.view(target);if(!av.optBoolean("found")||!tv.optBoolean("found"))return block("token_not_found");if(av.optInt("hp",0)<=0)return block("attacker_defeated");if(tv.optInt("hp",0)<=0)return block("target_defeated");
  JSONObject attackerProfile=CompiledStatblockEngine.runtimeProfile(av.optString("statblock_id"));JSONObject declared=attackerProfile.optJSONObject("multiattack");if(declared==null)return block("multiattack_not_compiled");JSONArray declaredSequence=declared.optJSONArray("sequence");String[] sequence=null;if(declaredSequence!=null){sequence=new String[declaredSequence.length()];for(int i=0;i<sequence.length;i++)sequence[i]=declaredSequence.optString(i);}
  int declaredCount=declared.optInt("count",sequence==null?0:sequence.length);
  String declarationBlock=MultiattackActionRules.declarationBlock(sequence,declaredCount,
      declared.optString("action",""),declared.optString("options",""));
  if(!declarationBlock.isEmpty())return block(declarationBlock);
  if(!MultiattackActionRules.matches(actions,sequence,declaredCount,
      declared.optString("action",""),declared.optString("options","")))return block("multiattack_sequence_not_authorized");
  for(int i=0;i<firstD20s.length;i++)if(firstD20s[i]<1||firstD20s[i]>20||secondD20s[i]<1||secondD20s[i]>20)
      return block("multiattack_invalid_d20").put("component",i);
  for(int i=0;i<actions.length;i++){JSONObject componentSource=findAction(av.optString("statblock_id"),actions[i]);if(componentSource==null)return block("multiattack_component_blocked").put("component",i).put("detail",block("action_not_found"));String categoryBlock=MonsterActionEconomyGuard.componentBlock(
      componentSource.optString("category",""));if(!categoryBlock.isEmpty())
      return block("multiattack_component_blocked").put("component",i).put("detail",block(categoryBlock));
      String riderBlock=MonsterActionEconomyGuard.attackRiderBlock(componentSource,false);if(!riderBlock.isEmpty())return block("multiattack_component_blocked").put("component",i).put("detail",block(riderBlock));}
  DndRulesEngine rules=new DndRulesEngine();String cover=tv.optString("cover","");if(rules.hasTotalCover(cover))return block("target_has_total_cover");JSONObject tp=CompiledStatblockEngine.runtimeProfile(tv.optString("statblock_id"));int ac=tp.optInt("armor_class",10)+rules.coverAcBonus(cover);
  // Preflight every component before touching HP or the action resource. A
  // later out-of-range/invalid component must not leave earlier damage behind.
  JSONObject[] planned=new JSONObject[actions.length];
  for(int i=0;i<actions.length;i++){
   JSONObject componentSource=findAction(av.optString("statblock_id"),actions[i]);
   if(componentSource==null)return block("multiattack_component_blocked").put("component",i)
       .put("detail",block("action_not_found"));
   JSONObject range=StatblockCombatRuntimeEngine.rangeProfile(av.optString("statblock_id"),actions[i],distanceFt);
   if(range.optBoolean("blocked")||range.optBoolean("out_of_range"))return block("multiattack_component_out_of_range").put("component",i).put("detail",range);
   XmmConditionalAttackRollRules.Result sourceRollMode=XmmConditionalAttackRollRules.advantage(
       componentSource,tv.optInt("hp",-1),tv.optInt("max_hp",-1));
   if(!sourceRollMode.legal())return block("multiattack_component_blocked").put("component",i)
       .put("detail",block(sourceRollMode.blocked));
   JSONObject cp=ConditionRulesEngine.attackProfile(StatblockEncounterStateEngine.conditionText(attacker),StatblockEncounterStateEngine.conditionText(target),distanceFt,true,true,false,false,DndRulesEngine.PROFILE_2014);
   if(cp.optBoolean("blocked",false))return block("attacker_incapacitated").put("component",i);
   if(sourceRollMode.advantage)cp.put("advantage",true);
   if(range.optBoolean("long_range_disadvantage",false)||(range.optBoolean("ranged",false)&&distanceFt>0d&&distanceFt<=5d&&!ConditionRulesEngine.incapacitated(StatblockEncounterStateEngine.conditionText(target))))cp.put("disadvantage",true);
   int used=rules.chooseD20(firstD20s[i],secondD20s[i],cp.optBoolean("advantage",false),cp.optBoolean("disadvantage",false));
   JSONObject r=StatblockCombatRuntimeEngine.attack(av.optString("statblock_id"),actions[i],used,ac,
       av.optInt("hp",-1),av.optInt("max_hp",-1),false,
       cp.optBoolean("advantage",false)&&!cp.optBoolean("disadvantage",false));
   if(r.optBoolean("blocked"))return block("multiattack_component_blocked").put("component",i).put("detail",r);
   r.put("first_d20",firstD20s[i]).put("second_d20",secondD20s[i]).put("target_cover",cover).put("range",range);
   if(r.optBoolean("hit")&&cp.optBoolean("critical_on_hit",false))r.put("critical",true).put("critical_from_condition",true);
   planned[i]=r;
  }
  JSONObject encounterBefore=StatblockEncounterStateEngine.snapshot(),turnBefore=StatblockTurnEngine.snapshot();
  JSONArray results=new JSONArray();int total=0;
  try{
   JSONObject spent=StatblockTurnEngine.spend(attacker,"action");if(spent.optBoolean("blocked"))throw new IllegalStateException("multiattack_action_spend_failed");
   for(JSONObject r:planned){
    if(r.optBoolean("hit")){JSONObject damage=applyAttackDamage(target,r,r.optBoolean("critical"));int applied=damage.optInt("applied_damage");total=Math.addExact(total,applied);r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",applied).put("damage_components",damage.optJSONArray("components"));results.put(r);if(damage.optBoolean("target_defeated"))break;}
    else results.put(r);
   }
  }catch(Exception mutationFailure){
   StatblockEncounterStateEngine.restore(encounterBefore);StatblockTurnEngine.restore(turnBefore);
   throw mutationFailure;
  }
  JSONObject ds=StatblockEncounterStateEngine.view(target);
  return new JSONObject().put("resolved",true).put("multiattack",true).put("attacker",attacker).put("target",target).put("total_damage",total).put("target_defeated",ds.optBoolean("defeated")).put("components",results).put("target_state",ds).put("turn_state",StatblockTurnEngine.state());
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

 private static JSONObject applyAttackDamage(String target,JSONObject attack,boolean critical){return applyAttackDamage(target,attack,critical,1.0d);}
 private static JSONObject applyAttackDamage(String target,JSONObject attack,boolean critical,double multiplier){/*BR_JSON_WRAP_106*/try{JSONArray specs=damageSpecs(attack.optString("damage_expression",""),attack.optString("damage_type",""),attack.optString("extra_damage_expression","")),parts=new JSONArray();int rawTotal=0,appliedTotal=0;for(int i=0;i<specs.length();i++){JSONObject spec=specs.optJSONObject(i);if(spec==null)continue;String expr=spec.optString("expression"),type=spec.optString("type");int raw=rollAverage(expr,critical);if(multiplier>=0d&&multiplier!=1.0d){raw=(int)Math.floor(raw*multiplier);}if(raw<=0)continue;JSONObject ds=StatblockEncounterStateEngine.damage(target,raw,type);int applied=ds.optInt("applied_damage",raw);rawTotal=Math.addExact(rawTotal,raw);appliedTotal=Math.addExact(appliedTotal,applied);parts.put(new JSONObject().put("expression",expr).put("type",type).put("raw_damage",raw).put("applied_damage",applied));}JSONObject tv=StatblockEncounterStateEngine.view(target);return new JSONObject().put("raw_damage",rawTotal).put("applied_damage",appliedTotal).put("components",parts).put("target_defeated",tv.optBoolean("defeated",tv.optInt("hp",1)<=0));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

 private static void applySpecialEffect(String attacker,String target,String effect,JSONObject receipt){
  if(effect==null||effect.trim().isEmpty())return;
  String clean=effect.trim().toLowerCase(java.util.Locale.ROOT);
  try{receipt.put("effect_applied",clean);}catch(Exception ignored){}
  String[] parts=clean.split("[;,/]");
  for(String p:parts){
   p=p.trim();if(p.isEmpty())continue;
   if(p.startsWith("teleport:")){
    int feet=parseFeet(p.substring(9));
    try{receipt.put("teleport_applied",true).put("teleport_feet",feet);}catch(Exception ignored){}
    StatblockEncounterStateEngine.setEffectProperty(attacker,"last_teleport_ft",feet);
    JSONObject posAttacker=StatblockEncounterStateEngine.getPosition(attacker);
    if(posAttacker.optBoolean("found")){
     int ox=posAttacker.optInt("x",0),oy=posAttacker.optInt("y",0),oz=posAttacker.optInt("z",0);
     int squares=Math.max(1,feet/5);
     JSONObject posTarget=StatblockEncounterStateEngine.getPosition(target);
     int tx=posTarget.optBoolean("found")?posTarget.optInt("x",0):ox;
     int ty=posTarget.optBoolean("found")?posTarget.optInt("y",0):oy;
     int dx=ox-tx,dy=oy-ty;
     int sx=Integer.compare(dx,0),sy=Integer.compare(dy,0);
     if(sx==0&&sy==0){sx=1;}
     int nx=Math.min(99,Math.max(0,ox+sx*squares));
     int ny=Math.min(99,Math.max(0,oy+sy*squares));
     StatblockEncounterStateEngine.setPosition(attacker,nx,ny,oz);
     try{receipt.put("origin_x",ox).put("origin_y",oy).put("dest_x",nx).put("dest_y",ny);}catch(Exception ignored){}
    }
   }else if(p.startsWith("push:")){
    int feet=parseFeet(p.substring(5));
    try{receipt.put("forced_movement_applied",true).put("forced_movement_type","push").put("forced_movement_feet",feet);}catch(Exception ignored){}
    StatblockEncounterStateEngine.setEffectProperty(target,"forced_movement_ft",feet);
    JSONObject posTarget=StatblockEncounterStateEngine.getPosition(target);
    JSONObject posAttacker=StatblockEncounterStateEngine.getPosition(attacker);
    if(posTarget.optBoolean("found")&&posAttacker.optBoolean("found")){
     int tx=posTarget.optInt("x",0),ty=posTarget.optInt("y",0),tz=posTarget.optInt("z",0);
     int ax=posAttacker.optInt("x",0),ay=posAttacker.optInt("y",0);
     int squares=Math.max(1,feet/5);
     int dx=tx-ax,dy=ty-ay;
     int sx=Integer.compare(dx,0),sy=Integer.compare(dy,0);
     if(sx==0&&sy==0){sx=1;}
     int nx=Math.min(99,Math.max(0,tx+sx*squares));
     int ny=Math.min(99,Math.max(0,ty+sy*squares));
     if(nx==tx&&ny==ty&&squares>0){
      try{receipt.put("forced_movement_blocked","destination_blocked_or_boundary");}catch(Exception ignored){}
     }else{
      StatblockEncounterStateEngine.setPosition(target,nx,ny,tz);
      try{receipt.put("origin_x",tx).put("origin_y",ty).put("dest_x",nx).put("dest_y",ny);}catch(Exception ignored){}
     }
    }
   }else if(p.startsWith("pull:")){
    int feet=parseFeet(p.substring(5));
    try{receipt.put("forced_movement_applied",true).put("forced_movement_type","pull").put("forced_movement_feet",feet);}catch(Exception ignored){}
    StatblockEncounterStateEngine.setEffectProperty(target,"forced_movement_ft",feet);
    JSONObject posTarget=StatblockEncounterStateEngine.getPosition(target);
    JSONObject posAttacker=StatblockEncounterStateEngine.getPosition(attacker);
    if(posTarget.optBoolean("found")&&posAttacker.optBoolean("found")){
     int tx=posTarget.optInt("x",0),ty=posTarget.optInt("y",0),tz=posTarget.optInt("z",0);
     int ax=posAttacker.optInt("x",0),ay=posAttacker.optInt("y",0);
     int squares=Math.max(1,feet/5);
     int dx=ax-tx,dy=ay-ty;
     int sx=Integer.compare(dx,0),sy=Integer.compare(dy,0);
     int nx=tx,ny=ty;
     for(int step=1;step<=squares;step++){
      int nextX=tx+sx*step,nextY=ty+sy*step;
      if(nextX==ax&&nextY==ay)break;
      if(nextX<0||nextX>99||nextY<0||nextY>99)break;
      nx=nextX;ny=nextY;
     }
     if(nx==tx&&ny==ty&&squares>0){
      try{receipt.put("forced_movement_blocked","destination_blocked_or_boundary");}catch(Exception ignored){}
     }else{
      StatblockEncounterStateEngine.setPosition(target,nx,ny,tz);
      try{receipt.put("origin_x",tx).put("origin_y",ty).put("dest_x",nx).put("dest_y",ny);}catch(Exception ignored){}
     }
    }
   }else if(isConditionName(p)){
    StatblockEncounterStateEngine.condition(target,p,true);
    try{receipt.put("condition_from_effect",p);}catch(Exception ignored){}
   }
  }
 }
 private static int parseFeet(String s){try{String clean=s.replaceAll("[^0-9]","");return clean.isEmpty()?0:Integer.parseInt(clean);}catch(Exception e){return 0;}}
 private static boolean isConditionName(String s){return "prone".equals(s)||"blinded".equals(s)||"frightened".equals(s)||"charmed".equals(s)||"poisoned".equals(s)||"restrained".equals(s)||"incapacitated".equals(s)||"paralyzed".equals(s)||"stunned".equals(s)||"deafened".equals(s);}

 private static void executeLegendarySpell(String attacker,String target,JSONArray spells,int d20,JSONObject declaredAction,JSONObject r){
  String spellName=r.optString("selected_spell","");
  if(spellName.isEmpty()){
   spellName=resolveDeclaredSpellName(r.optString("action",""),spells,declaredAction);
  }
  if(spellName.isEmpty())return;
  try{r.put("spell_executed",spellName);}catch(Exception ignored){}
  String sLower=spellName.toLowerCase(java.util.Locale.ROOT);
  JSONObject tv=StatblockEncounterStateEngine.view(target);
  JSONObject tp=CompiledStatblockEngine.runtimeProfile(tv.optString("statblock_id"));
  int targetAc=tp.optInt("armor_class",10);
  int saveDc=declaredAction.optInt("save_dc",15);
  if(sLower.contains("invisibility")){
   StatblockEncounterStateEngine.condition(attacker,"invisible",true);
   StatblockEncounterStateEngine.setEffectProperty(attacker,"concentration",spellName);
   try{r.put("applied_condition","invisible").put("affected_token",attacker).put("concentration_spell",spellName);}catch(Exception ignored){}
  }else if(sLower.contains("scorching ray")){
   int attackBonus=declaredAction.optInt("to_hit",7);
   boolean hit=(d20+attackBonus)>=targetAc||d20==20;
   try{r.put("spell_attack_roll",d20+attackBonus).put("hit",hit);}catch(Exception ignored){}
   if(hit){
    JSONObject fakeAtk=new JSONObject();
    try{fakeAtk.put("damage_expression","6d6").put("damage_type","fire");}catch(Exception ignored){}
    JSONObject dmg=applyAttackDamage(target,fakeAtk,d20==20,1.0d);
    try{r.put("raw_damage",dmg.optInt("raw_damage")).put("applied_damage",dmg.optInt("applied_damage")).put("damage_components",dmg.optJSONArray("components")).put("target_defeated",dmg.optBoolean("target_defeated"));}catch(Exception ignored){}
   }
  }else if(sLower.contains("guiding bolt")){
   int attackBonus=declaredAction.optInt("to_hit",7);
   boolean hit=(d20+attackBonus)>=targetAc||d20==20;
   try{r.put("spell_attack_roll",d20+attackBonus).put("hit",hit);}catch(Exception ignored){}
   if(hit){
    JSONObject fakeAtk=new JSONObject();
    try{fakeAtk.put("damage_expression","4d6").put("damage_type","radiant");}catch(Exception ignored){}
    JSONObject dmg=applyAttackDamage(target,fakeAtk,d20==20,1.0d);
    try{r.put("raw_damage",dmg.optInt("raw_damage")).put("applied_damage",dmg.optInt("applied_damage")).put("damage_components",dmg.optJSONArray("components")).put("target_defeated",dmg.optBoolean("target_defeated"));}catch(Exception ignored){}
   }
  }else if(sLower.contains("ray of sickness")){
   int attackBonus=declaredAction.optInt("to_hit",7);
   boolean hit=(d20+attackBonus)>=targetAc||d20==20;
   try{r.put("spell_attack_roll",d20+attackBonus).put("hit",hit);}catch(Exception ignored){}
   if(hit){
    JSONObject fakeAtk=new JSONObject();
    try{fakeAtk.put("damage_expression","2d8").put("damage_type","poison");}catch(Exception ignored){}
    JSONObject dmg=applyAttackDamage(target,fakeAtk,d20==20,1.0d);
    try{r.put("raw_damage",dmg.optInt("raw_damage")).put("applied_damage",dmg.optInt("applied_damage")).put("damage_components",dmg.optJSONArray("components")).put("target_defeated",dmg.optBoolean("target_defeated"));}catch(Exception ignored){}
    int conBonus=calculateTargetSaveBonus(tp,"con");
    int conTotal=d20+conBonus;
    boolean savePass=conTotal>=saveDc;
    try{r.put("save_ability","con").put("save_bonus",conBonus).put("save_roll",conTotal).put("save_dc",saveDc).put("save_success",savePass);}catch(Exception ignored){}
    if(!savePass){StatblockEncounterStateEngine.condition(target,"poisoned",true);try{r.put("applied_condition","poisoned");}catch(Exception ignored){}}
   }
  }else if(sLower.contains("shatter")){
   int conBonus=calculateTargetSaveBonus(tp,"con");
   int conTotal=d20+conBonus;
   boolean savePass=conTotal>=saveDc;
   double mult=savePass?0.5d:1.0d;
   JSONObject fakeAtk=new JSONObject();
   try{fakeAtk.put("damage_expression","3d8").put("damage_type","thunder");}catch(Exception ignored){}
   JSONObject dmg=applyAttackDamage(target,fakeAtk,false,mult);
   try{r.put("save_ability","con").put("save_bonus",conBonus).put("save_roll",conTotal).put("save_dc",saveDc).put("save_success",savePass).put("raw_damage",dmg.optInt("raw_damage")).put("applied_damage",dmg.optInt("applied_damage")).put("damage_components",dmg.optJSONArray("components")).put("target_defeated",dmg.optBoolean("target_defeated"));}catch(Exception ignored){}
  }else if(sLower.contains("command")){
   int wisBonus=calculateTargetSaveBonus(tp,"wis");
   int wisTotal=d20+wisBonus;
   boolean savePass=wisTotal>=saveDc;
   try{r.put("save_ability","wis").put("save_bonus",wisBonus).put("save_roll",wisTotal).put("save_dc",saveDc).put("save_success",savePass);}catch(Exception ignored){}
   if(!savePass){StatblockEncounterStateEngine.condition(target,"prone",true);try{r.put("applied_condition","prone");}catch(Exception ignored){}}
  }else if(sLower.contains("mind spike")){
   int wisBonus=calculateTargetSaveBonus(tp,"wis");
   int wisTotal=d20+wisBonus;
   boolean savePass=wisTotal>=saveDc;
   double mult=savePass?0.5d:1.0d;
   JSONObject fakeAtk=new JSONObject();
   try{fakeAtk.put("damage_expression","3d8").put("damage_type","psychic");}catch(Exception ignored){}
   JSONObject dmg=applyAttackDamage(target,fakeAtk,false,mult);
   try{r.put("save_ability","wis").put("save_bonus",wisBonus).put("save_roll",wisTotal).put("save_dc",saveDc).put("save_success",savePass).put("raw_damage",dmg.optInt("raw_damage")).put("applied_damage",dmg.optInt("applied_damage")).put("damage_components",dmg.optJSONArray("components")).put("target_defeated",dmg.optBoolean("target_defeated"));}catch(Exception ignored){}
  }else if(sLower.contains("hold monster")){
   int wisBonus=calculateTargetSaveBonus(tp,"wis");
   int wisTotal=d20+wisBonus;
   boolean savePass=wisTotal>=saveDc;
   StatblockEncounterStateEngine.setEffectProperty(attacker,"concentration",spellName);
   try{r.put("save_ability","wis").put("save_bonus",wisBonus).put("save_roll",wisTotal).put("save_dc",saveDc).put("save_success",savePass).put("concentration_spell",spellName);}catch(Exception ignored){}
   if(!savePass){StatblockEncounterStateEngine.condition(target,"paralyzed",true);try{r.put("applied_condition","paralyzed");}catch(Exception ignored){}}
  }else if(sLower.contains("power word stun")){
   StatblockEncounterStateEngine.condition(target,"stunned",true);
   try{r.put("applied_condition","stunned");}catch(Exception ignored){}
  }else{
   if(declaredAction.has("damage")&&!declaredAction.optString("damage","").isEmpty()){
    JSONObject dmg=applyAttackDamage(target,declaredAction,false,1.0d);
    try{r.put("raw_damage",dmg.optInt("raw_damage")).put("applied_damage",dmg.optInt("applied_damage")).put("damage_components",dmg.optJSONArray("components")).put("target_defeated",dmg.optBoolean("target_defeated"));}catch(Exception ignored){}
   }
   if(declaredAction.has("condition_on_fail")&&!declaredAction.optString("condition_on_fail","").isEmpty()){
    String c=declaredAction.optString("condition_on_fail");
    StatblockEncounterStateEngine.condition(target,c,true);
    try{r.put("applied_condition",c);}catch(Exception ignored){}
   }
  }
 }

 private static String resolveDeclaredSpellName(String actionName,JSONArray spells,JSONObject declaredAction){
  if(declaredAction!=null&&declaredAction.has("spell")&&!declaredAction.optString("spell").trim().isEmpty()){
   return declaredAction.optString("spell").trim();
  }
  if(spells!=null){
   for(int i=0;i<spells.length();i++){
    String sp=spells.optString(i,"").trim();
    if(!sp.isEmpty()&&actionName!=null&&actionName.trim().equalsIgnoreCase(sp))return sp;
   }
   if(spells.length()>0)return spells.optString(0,"").trim();
  }
  return actionName==null?"":actionName.trim();
 }

 private static boolean isSupportedLegendarySpell(String spellName,JSONObject declaredAction){
  if(spellName==null||spellName.trim().isEmpty())return false;
  String s=spellName.trim().toLowerCase(java.util.Locale.ROOT);
  if(s.contains("invisibility")||s.contains("scorching ray")||s.contains("guiding bolt")
   ||s.contains("ray of sickness")||s.contains("shatter")||s.contains("command")
   ||s.contains("mind spike")||s.contains("hold monster")||s.contains("power word stun")){
   return true;
  }
  if(declaredAction!=null&&(declaredAction.has("damage")||declaredAction.has("condition_on_fail"))){
   return true;
  }
  return false;
 }

 private static int calculateTargetSaveBonus(JSONObject targetProfile,String ability){
  if(targetProfile==null||ability==null||ability.trim().isEmpty())return 0;
  String ab=ability.trim().toLowerCase(java.util.Locale.ROOT);
  if(ab.length()>3)ab=ab.substring(0,3);
  JSONObject saves=targetProfile.optJSONObject("saving_throws");
  if(saves!=null&&saves.has(ab)){
   Object val=saves.opt(ab);
   if(val instanceof Number)return ((Number)val).intValue();
   if(val!=null){
    try{
     String clean=String.valueOf(val).replaceAll("[^0-9+-]","");
     if(!clean.isEmpty())return Integer.parseInt(clean);
    }catch(Exception ignored){}
   }
  }
  JSONObject abilities=targetProfile.optJSONObject("abilities");
  if(abilities!=null&&abilities.has(ab)){
   int score=abilities.optInt(ab,10);
   return (int)Math.floor((score-10)/2.0);
  }
  return 0;
 }

 static JSONArray damageSpecs(String expression,String typeSpec,String extra){return damageSpecs(expression,typeSpec,extra,"");}
 static JSONArray damageSpecs(String expression,String typeSpec,String extra,String extraType){/*BR_JSON_WRAP_106*/try{JSONArray out=new JSONArray();String e=expression==null?"":expression.trim(),t=typeSpec==null?"":typeSpec.trim();java.util.ArrayList<String> types=new java.util.ArrayList<String>();for(String x:t.split("\\s*\\+\\s*")){String q=x.trim();if(q.length()>0)types.add(q);}java.util.ArrayList<String> dice=new java.util.ArrayList<String>();
if(types.size()>1&&!e.isEmpty()){
String clean=e.replaceAll("\\s+","");
java.util.regex.Matcher m=java.util.regex.Pattern.compile("([+-]?)(\\d+d\\d+|\\d+)").matcher(clean);
int cursor=0;
while(m.find()){
 if(m.start()!=cursor)throw new IllegalArgumentException("unparsed_typed_damage_source");
 cursor=m.end();
 String sign=m.group(1),item=m.group(2);
 if(item.contains("d")){
  if("-".equals(sign))throw new IllegalArgumentException("negative_damage_dice_unsupported");
  dice.add(item);
 }else{
  if(dice.isEmpty()||sign.isEmpty())throw new IllegalArgumentException("unbound_damage_modifier");
  int last=dice.size()-1;dice.set(last,dice.get(last)+" "+sign+" "+item);
 }
}
if(cursor!=clean.length())throw new IllegalArgumentException("incomplete_typed_damage_source");
}
if(e.length()>0){
 if(types.size()>1){
  if(types.size()!=dice.size())throw new IllegalArgumentException("typed_damage_count_mismatch");
  for(int i=0;i<dice.size();i++)out.put(new JSONObject().put("expression",dice.get(i)).put("type",types.get(i)));
 }else out.put(new JSONObject().put("expression",e).put("type",types.size()==1?types.get(0):""));
}String ex=extra==null?"":extra.trim();if(ex.length()>0){String xt=extraType==null?"":extraType.trim();if(xt.isEmpty())xt=damageTypeInText(ex);String xe=ex.replaceAll("(?i)\\b(acid|bludgeoning|cold|fire|force|lightning|necrotic|piercing|poison|psychic|radiant|slashing|thunder|ácido|contundente|frio|fogo|força|elétrico|eletrico|necrótico|necrotico|perfurante|veneno|psíquico|psiquico|radiante|cortante|trovejante)\\b","").trim();if(xe.length()==0)xe=ex;if(xt.length()==0&&types.size()==1)xt=types.get(0);out.put(new JSONObject().put("expression",xe).put("type",xt));}return coalesceDamageTypes(out);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
 private static JSONArray coalesceDamageTypes(JSONArray input)throws Exception{JSONArray out=new JSONArray();java.util.LinkedHashMap<String,JSONObject> byType=new java.util.LinkedHashMap<String,JSONObject>();for(int i=0;i<input.length();i++){JSONObject part=input.optJSONObject(i);if(part==null)continue;String type=DamageTypeNormalizer.canonical(part.optString("type",""));if(type.isEmpty()){out.put(part);continue;}JSONObject previous=byType.get(type);if(previous==null)byType.put(type,new JSONObject(part.toString()));else previous.put("expression",previous.optString("expression")+" + "+part.optString("expression"));}for(JSONObject part:byType.values())out.put(part);return out;}
 private static String damageTypeInText(String v){String s=v==null?"":v.toLowerCase(java.util.Locale.ROOT);String[][] pairs={{"acid","acid"},{"ácido","acid"},{"bludgeoning","bludgeoning"},{"contundente","bludgeoning"},{"cold","cold"},{"frio","cold"},{"fire","fire"},{"fogo","fire"},{"force","force"},{"força","force"},{"lightning","lightning"},{"elétrico","lightning"},{"eletrico","lightning"},{"necrotic","necrotic"},{"necrótico","necrotic"},{"necrotico","necrotic"},{"piercing","piercing"},{"perfurante","piercing"},{"poison","poison"},{"veneno","poison"},{"psychic","psychic"},{"psíquico","psychic"},{"psiquico","psychic"},{"radiant","radiant"},{"radiante","radiant"},{"slashing","slashing"},{"cortante","slashing"},{"thunder","thunder"},{"trovejante","thunder"}};for(String[]p:pairs)if(s.matches(".*\\b"+java.util.regex.Pattern.quote(p[0])+"\\b.*"))return p[1];return "";}
 private static JSONObject economy(JSONObject s,String token){JSONArray a=s.optJSONArray("order");if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&token.equals(x.optString("token")))return x.optJSONObject("economy");}return null;}
 private static JSONObject findAction(String statblockId,String actionName){JSONObject prof=CompiledStatblockEngine.runtimeProfile(statblockId);JSONArray a=prof.optJSONArray("actions");if(a!=null)for(int i=0;i<a.length();i++){JSONObject candidate=a.optJSONObject(i);if(candidate!=null&&actionName!=null&&actionName.trim().equalsIgnoreCase(candidate.optString("name","")))return candidate;}JSONArray sp=prof.optJSONArray("special_actions");if(sp!=null)for(int i=0;i<sp.length();i++){JSONObject candidate=sp.optJSONObject(i);if(candidate!=null&&actionName!=null&&actionName.trim().equalsIgnoreCase(candidate.optString("name","")))return candidate;}return null;}
 private static int rollAverage(String expr){return rollAverage(expr,false);}
 static int rollAverage(String expr,boolean critical){
  if(expr==null||expr.trim().isEmpty())return 0;
  String clean=expr.replaceAll("\\s+","").toLowerCase(java.util.Locale.ROOT);
  if(!clean.matches("[+-]?(?:\\d+d\\d+|\\d+)(?:[+-](?:\\d+d\\d+|\\d+))*"))
   throw new IllegalArgumentException("unsupported_source_damage_expression");
  java.util.regex.Matcher term=java.util.regex.Pattern.compile("([+-]?)(\\d+)(?:d(\\d+))?").matcher(clean);
  long total=0L;int cursor=0;
  while(term.find()){
   if(term.start()!=cursor)throw new IllegalArgumentException("damage_expression_not_fully_parsed");
   cursor=term.end();
   long amount=Long.parseLong(term.group(2)),contribution;
   if(term.group(3)!=null){
    long faces=Long.parseLong(term.group(3));
    if(amount<1L||faces<1L)throw new IllegalArgumentException("invalid_source_damage_dice");
    long dice=critical?Math.multiplyExact(amount,2L):amount;
    contribution=Math.multiplyExact(dice,Math.addExact(faces,1L))/2L;
   }else contribution=amount;
   total=Math.addExact(total,"-".equals(term.group(1))?-contribution:contribution);
  }
  if(cursor!=clean.length()||total>Integer.MAX_VALUE||total<Integer.MIN_VALUE)
   throw new IllegalArgumentException("damage_expression_out_of_range");
  return Math.max(0,(int)total);
 }
 private static JSONObject block(String r){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("blocked",true).put("reason",r);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
}
