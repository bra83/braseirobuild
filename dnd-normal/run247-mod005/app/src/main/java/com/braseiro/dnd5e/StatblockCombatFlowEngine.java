package com.braseiro.dnd5e;
import org.json.*;
/** Atomic turn-aware bridge between compiled attacks and persistent encounter state. */
public final class StatblockCombatFlowEngine{
 private StatblockCombatFlowEngine(){}
 public static synchronized JSONObject attack(String attacker,String target,String action,int d20){return attack(attacker,target,action,d20,d20,0d);}
 public static synchronized JSONObject attack(String attacker,String target,String action,int firstD20,int secondD20,double distanceFt){return attackResolved(attacker,target,action,firstD20,secondD20,distanceFt,null);}
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
  JSONObject declaredAction=findAction(av.optString("statblock_id"),action);if(declaredAction==null)return block("action_not_found");String riderBlock=MonsterActionEconomyGuard.attackRiderBlock(declaredAction,false);if("unsupported_multiattack_on_hit_rider".equals(riderBlock))return block("unsupported_on_hit_rider_in_legacy_flow");if(!riderBlock.isEmpty())return block(riderBlock);
  JSONObject tp=CompiledStatblockEngine.runtimeProfile(tv.optString("statblock_id"));DndRulesEngine rules=new DndRulesEngine();String cover=tv.optString("cover","");if(rules.hasTotalCover(cover))return block("target_has_total_cover");int geometryCover=geometry==null?0:geometry.coverBonus;int ac=tp.optInt("armor_class",10)+Math.max(rules.coverAcBonus(cover),geometryCover);JSONObject range=StatblockCombatRuntimeEngine.rangeProfile(av.optString("statblock_id"),action,distanceFt);if(range.optBoolean("blocked"))return range;if(range.optBoolean("out_of_range"))return block("attack_out_of_range").put("range",range);
  JSONObject cp=ConditionRulesEngine.attackProfile(StatblockEncounterStateEngine.conditionText(attacker),StatblockEncounterStateEngine.conditionText(target),distanceFt,true,true,false,false,DndRulesEngine.PROFILE_2014);if(cp.optBoolean("blocked",false))return block("attacker_incapacitated");if(range.optBoolean("long_range_disadvantage",false)||(range.optBoolean("ranged",false)&&distanceFt>0d&&distanceFt<=5d&&!ConditionRulesEngine.incapacitated(StatblockEncounterStateEngine.conditionText(target)))||(geometry!=null&&(geometry.longRangeDisadvantage||geometry.unseenDisadvantage)))cp.put("disadvantage",true);int used=rules.chooseD20(firstD20,secondD20,cp.optBoolean("advantage",false),cp.optBoolean("disadvantage",false));
  JSONObject r=StatblockCombatRuntimeEngine.attack(av.optString("statblock_id"),action,used,ac);if(r.optBoolean("blocked"))return r;r.put("first_d20",firstD20).put("second_d20",secondD20).put("roll_mode",cp.optBoolean("advantage",false)&&!cp.optBoolean("disadvantage",false)?"advantage":cp.optBoolean("disadvantage",false)&&!cp.optBoolean("advantage",false)?"disadvantage":"normal").put("target_cover",cover).put("range",range);if(geometry!=null)r.put("attack_geometry_receipt",TacticalCreatureGeometryRules.receipt(geometry)).put("geometry_cover_bonus",geometryCover).put("unseen_disadvantage",geometry.unseenDisadvantage).put("blindsight_used",geometry.usedBlindsight);if(r.optBoolean("hit")&&cp.optBoolean("critical_on_hit",false))r.put("critical",true).put("critical_from_condition",true);
  JSONObject spent=StatblockTurnEngine.spend(attacker,"action");if(spent.optBoolean("blocked"))return spent;
  if(r.optBoolean("hit")){JSONObject damage=applyAttackDamage(target,r,r.optBoolean("critical"));r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",damage.optInt("applied_damage")).put("damage_components",damage.optJSONArray("components")).put("target_defeated",damage.optBoolean("target_defeated"));}
  return r.put("attacker",attacker).put("target",target).put("target_state",StatblockEncounterStateEngine.view(target)).put("turn_state",StatblockTurnEngine.state());
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
  for(int i=0;i<actions.length;i++){JSONObject componentSource=findAction(av.optString("statblock_id"),actions[i]);if(componentSource==null)return block("multiattack_component_blocked").put("component",i).put("detail",block("action_not_found"));String riderBlock=MonsterActionEconomyGuard.attackRiderBlock(componentSource,false);if(!riderBlock.isEmpty())return block("multiattack_component_blocked").put("component",i).put("detail",block(riderBlock));}
  DndRulesEngine rules=new DndRulesEngine();String cover=tv.optString("cover","");if(rules.hasTotalCover(cover))return block("target_has_total_cover");JSONObject tp=CompiledStatblockEngine.runtimeProfile(tv.optString("statblock_id"));int ac=tp.optInt("armor_class",10)+rules.coverAcBonus(cover);
  // Preflight every component before touching HP or the action resource. A
  // later out-of-range/invalid component must not leave earlier damage behind.
  JSONObject[] planned=new JSONObject[actions.length];
  for(int i=0;i<actions.length;i++){
   JSONObject range=StatblockCombatRuntimeEngine.rangeProfile(av.optString("statblock_id"),actions[i],distanceFt);
   if(range.optBoolean("blocked")||range.optBoolean("out_of_range"))return block("multiattack_component_out_of_range").put("component",i).put("detail",range);
   JSONObject cp=ConditionRulesEngine.attackProfile(StatblockEncounterStateEngine.conditionText(attacker),StatblockEncounterStateEngine.conditionText(target),distanceFt,true,true,false,false,DndRulesEngine.PROFILE_2014);
   if(cp.optBoolean("blocked",false))return block("attacker_incapacitated").put("component",i);
   if(range.optBoolean("long_range_disadvantage",false)||(range.optBoolean("ranged",false)&&distanceFt>0d&&distanceFt<=5d&&!ConditionRulesEngine.incapacitated(StatblockEncounterStateEngine.conditionText(target))))cp.put("disadvantage",true);
   int used=rules.chooseD20(firstD20s[i],secondD20s[i],cp.optBoolean("advantage",false),cp.optBoolean("disadvantage",false));
   JSONObject r=StatblockCombatRuntimeEngine.attack(av.optString("statblock_id"),actions[i],used,ac);
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
    if(r.optBoolean("hit")){JSONObject damage=applyAttackDamage(target,r,r.optBoolean("critical"));int applied=damage.optInt("applied_damage");total+=applied;r.put("raw_damage",damage.optInt("raw_damage")).put("applied_damage",applied).put("damage_components",damage.optJSONArray("components"));results.put(r);if(damage.optBoolean("target_defeated"))break;}
    else results.put(r);
   }
  }catch(Exception mutationFailure){
   StatblockEncounterStateEngine.restore(encounterBefore);StatblockTurnEngine.restore(turnBefore);
   throw mutationFailure;
  }
  JSONObject ds=StatblockEncounterStateEngine.view(target);
  return new JSONObject().put("resolved",true).put("multiattack",true).put("attacker",attacker).put("target",target).put("total_damage",total).put("target_defeated",ds.optBoolean("defeated")).put("components",results).put("target_state",ds).put("turn_state",StatblockTurnEngine.state());
 }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

 private static JSONObject applyAttackDamage(String target,JSONObject attack,boolean critical){/*BR_JSON_WRAP_106*/try{JSONArray specs=damageSpecs(attack.optString("damage_expression",""),attack.optString("damage_type",""),attack.optString("extra_damage_expression","")),parts=new JSONArray();int rawTotal=0,appliedTotal=0;for(int i=0;i<specs.length();i++){JSONObject spec=specs.optJSONObject(i);if(spec==null)continue;String expr=spec.optString("expression"),type=spec.optString("type");int raw=rollAverage(expr,critical);if(raw<=0)continue;JSONObject ds=StatblockEncounterStateEngine.damage(target,raw,type);int applied=ds.optInt("applied_damage",raw);rawTotal+=raw;appliedTotal+=applied;parts.put(new JSONObject().put("expression",expr).put("type",type).put("raw_damage",raw).put("applied_damage",applied));}JSONObject tv=StatblockEncounterStateEngine.view(target);return new JSONObject().put("raw_damage",rawTotal).put("applied_damage",appliedTotal).put("components",parts).put("target_defeated",tv.optBoolean("defeated",tv.optInt("hp",1)<=0));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
 static JSONArray damageSpecs(String expression,String typeSpec,String extra){/*BR_JSON_WRAP_106*/try{JSONArray out=new JSONArray();String e=expression==null?"":expression.trim(),t=typeSpec==null?"":typeSpec.trim();java.util.ArrayList<String> types=new java.util.ArrayList<String>();for(String x:t.split("\\s*\\+\\s*")){String q=x.trim();if(q.length()>0)types.add(q);}java.util.ArrayList<String> dice=new java.util.ArrayList<String>();java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?").matcher(e);while(m.find())dice.add(m.group());if(e.length()>0){if(types.size()>1&&types.size()==dice.size()){for(int i=0;i<dice.size();i++)out.put(new JSONObject().put("expression",dice.get(i)).put("type",types.get(i)));}else out.put(new JSONObject().put("expression",e).put("type",types.size()==1?types.get(0):""));}String ex=extra==null?"":extra.trim();if(ex.length()>0){String xt=damageTypeInText(ex);String xe=ex.replaceAll("(?i)\\b(acid|bludgeoning|cold|fire|force|lightning|necrotic|piercing|poison|psychic|radiant|slashing|thunder|ácido|contundente|frio|fogo|força|elétrico|eletrico|necrótico|necrotico|perfurante|veneno|psíquico|psiquico|radiante|cortante|trovejante)\\b","").trim();if(xe.length()==0)xe=ex;if(xt.length()==0&&types.size()==1)xt=types.get(0);out.put(new JSONObject().put("expression",xe).put("type",xt));}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
 private static String damageTypeInText(String v){String s=v==null?"":v.toLowerCase(java.util.Locale.ROOT);String[][] pairs={{"acid","acid"},{"ácido","acid"},{"bludgeoning","bludgeoning"},{"contundente","bludgeoning"},{"cold","cold"},{"frio","cold"},{"fire","fire"},{"fogo","fire"},{"force","force"},{"força","force"},{"lightning","lightning"},{"elétrico","lightning"},{"eletrico","lightning"},{"necrotic","necrotic"},{"necrótico","necrotic"},{"necrotico","necrotic"},{"piercing","piercing"},{"perfurante","piercing"},{"poison","poison"},{"veneno","poison"},{"psychic","psychic"},{"psíquico","psychic"},{"psiquico","psychic"},{"radiant","radiant"},{"radiante","radiant"},{"slashing","slashing"},{"cortante","slashing"},{"thunder","thunder"},{"trovejante","thunder"}};for(String[]p:pairs)if(s.matches(".*\\b"+java.util.regex.Pattern.quote(p[0])+"\\b.*"))return p[1];return "";}
 private static JSONObject economy(JSONObject s,String token){JSONArray a=s.optJSONArray("order");if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&token.equals(x.optString("token")))return x.optJSONObject("economy");}return null;}
 private static JSONObject findAction(String statblockId,String actionName){JSONArray a=CompiledStatblockEngine.runtimeProfile(statblockId).optJSONArray("actions");if(a!=null)for(int i=0;i<a.length();i++){JSONObject candidate=a.optJSONObject(i);if(candidate!=null&&actionName!=null&&actionName.trim().equalsIgnoreCase(candidate.optString("name","")))return candidate;}return null;}
 private static int rollAverage(String expr){return rollAverage(expr,false);}
 static int rollAverage(String expr,boolean critical){if(expr==null)return 0;String s=expr.trim();java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)d(\\d+)\\s*([+-]\\s*\\d+)?").matcher(s);int total=0;boolean any=false;while(m.find()){any=true;int n=Integer.parseInt(m.group(1)),d=Integer.parseInt(m.group(2)),mod=m.group(3)==null?0:Integer.parseInt(m.group(3).replaceAll("\\s+",""));if(critical)n*=2;total+=n*(d+1)/2+mod;}if(any)return Math.max(0,total);try{return Math.max(0,Integer.parseInt(s.replaceAll("[^0-9-]","")));}catch(Exception e){return 0;}}
 private static JSONObject block(String r){/*BR_JSON_WRAP_106*/try{return new JSONObject().put("blocked",true).put("reason",r);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
}
