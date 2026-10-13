package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source-pinned 2025 recharge thresholds 4-6, 5-6, 6-only; d6 is supplied explicitly. */
public final class Xmm2025RechargeDecisionTest {
 private static JSONObject query(String monster,String cat,String name,Integer roll)throws Exception {
   JSONObject in=new JSONObject().put("monster",monster).put("category",cat).put("action",name);
   if(roll!=null)in.put("roll_d6",roll);
   return Xmm2025SourceIndex.rechargeDecision(in);
 }
 @Test public void rechargeFourRequiresAtLeastFourAndNoSilentRoll()throws Exception {
   JSONObject waiting=query("Basilisk","bonus_action","Petrifying Gaze",null);
   assertEquals("manual_roll_required",waiting.getString("status"));
   assertFalse(waiting.getBoolean("auto_rolls"));
   assertFalse(query("Basilisk","bonus_action","Petrifying Gaze",3).getBoolean("recharged"));
   assertTrue(query("Basilisk","bonus_action","Petrifying Gaze",4).getBoolean("recharged"));
 }
 @Test public void rechargeFiveRequiresAtLeastFive()throws Exception {
   assertFalse(query("Adult Black Dragon","action","Acid Breath",4).getBoolean("recharged"));
   assertTrue(query("Adult Black Dragon","action","Acid Breath",5).getBoolean("recharged"));
 }
 @Test public void rechargeSixRequiresSix()throws Exception {
   assertFalse(query("Abominable Yeti","action","Cold Breath",5).getBoolean("recharged"));
   assertTrue(query("Abominable Yeti","action","Cold Breath",6).getBoolean("recharged"));
 }
 @Test public void invalidActionOrDieMustFailClosed()throws Exception {
   assertFalse(query("Basilisk","bonus_action","Imaginary Gaze",6).getBoolean("resolved"));
   assertFalse(query("Basilisk","bonus_action","Petrifying Gaze",0).getBoolean("resolved"));
   assertFalse(query("Basilisk","bonus_action","Petrifying Gaze",7).getBoolean("resolved"));
   assertFalse(query("No such monster","action","Cold Breath",6).getBoolean("resolved"));
 }
 @Test public void userInputIsNeverMutatedByExplicitRechargeEvaluation()throws Exception {
   JSONObject original=new JSONObject().put("monster","Basilisk")
     .put("category","bonus_action").put("action","Petrifying Gaze")
     .put("roll_d6",4).put("creature_hp",71);
   String before=original.toString();
   JSONObject result=Xmm2025SourceIndex.rechargeDecision(original);
   assertEquals(before,original.toString());
   assertFalse(result.getBoolean("auto_mutation_allowed"));
   assertTrue(result.getBoolean("recharged"));
 }
 @Test public void androidGatewayRoutesRechargeWithoutAutoMutation()throws Exception {
   JSONObject in=new JSONObject().put("monster","Adult Black Dragon")
       .put("category","action").put("action","Acid Breath").put("roll_d6",5);
   JSONObject routed=new OfficialBookRuntimeGateway(null).execute("xmm","recharge",in);
   assertTrue(routed.getBoolean("executed"));
   assertTrue(routed.getJSONObject("result").getBoolean("recharged"));
   assertFalse(routed.getBoolean("auto_mutation_allowed"));
 }
}
