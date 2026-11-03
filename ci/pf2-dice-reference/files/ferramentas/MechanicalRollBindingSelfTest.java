package com.braseiro.pathfinder2e;
import org.json.JSONObject;
public final class MechanicalRollBindingSelfTest {
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static JSONObject roll()throws Exception{return new JSONObject().put("id",71).put("formula","1d20+5").put("total",17).put("created_at",System.currentTimeMillis()).put("consumed_at",0).put("cancelled_at",0).put("actor_character_id",3).put("actor_name","Lia").put("test_key","test:scene:1").put("action_signature","action:push").put("scene_fingerprint","scene:abc").put("source_rule_id","manual:2:p:40").put("procedure_id","manual:2:p:40:procedure:1").put("campaign_fingerprint","campaign:9").put("target_signature","target:ogre").put("consequence_key","consequence:push:1").put("modifier",5).put("modifier_source","strength_mod").put("roll_mode","normal").put("natural_roll",12).put("critical",false).put("critical_failure",false).put("outcome","success");}
 private static JSONObject calc()throws Exception{return new JSONObject().put("roll_id",71).put("roll_formula","1d20+5").put("roll_total",17).put("test_key","test:scene:1").put("action_signature","action:push").put("scene_fingerprint","scene:abc").put("source_rule_id","manual:2:p:40").put("procedure_id","manual:2:p:40:procedure:1").put("campaign_fingerprint","campaign:9").put("target_signature","target:ogre").put("consequence_key","consequence:push:1").put("modifier",5).put("modifier_source","strength_mod").put("roll_mode","normal").put("natural_roll",12).put("critical",false).put("critical_failure",false).put("result","success");}
 public static void main(String[] args)throws Exception{
  JSONObject hero=new JSONObject().put("id",3).put("name","Lia");
  require(MechanicalRollBinding.verify(roll(),calc(),hero,System.currentTimeMillis()).valid,"vínculo exato deveria passar");
  JSONObject other=calc();other.put("action_signature","action:other");require(!MechanicalRollBinding.verify(roll(),other,hero,System.currentTimeMillis()).valid,"outra ação não pode reutilizar a rolagem");
  JSONObject consumed=roll();consumed.put("consumed_at",System.currentTimeMillis());require(!MechanicalRollBinding.verify(consumed,calc(),hero,System.currentTimeMillis()).valid,"rolagem consumida não pode ser reutilizada");
  JSONObject otherHero=new JSONObject().put("id",9).put("name","Nara");require(!MechanicalRollBinding.verify(roll(),calc(),otherHero,System.currentTimeMillis()).valid,"outro personagem não pode usar a rolagem");
  JSONObject source=calc();source.put("source_rule_id","manual:other");require(!MechanicalRollBinding.verify(roll(),source,hero,System.currentTimeMillis()).valid,"outra regra não pode usar a rolagem");
  JSONObject target=calc();target.put("target_signature","target:dragon");require(!MechanicalRollBinding.verify(roll(),target,hero,System.currentTimeMillis()).valid,"outro alvo não pode usar a rolagem");
  JSONObject mode=calc();mode.put("roll_mode","advantage");require(!MechanicalRollBinding.verify(roll(),mode,hero,System.currentTimeMillis()).valid,"vantagem não pode ser inventada depois da rolagem");
  JSONObject cancelled=roll();cancelled.put("cancelled_at",System.currentTimeMillis());require(!MechanicalRollBinding.verify(cancelled,calc(),hero,System.currentTimeMillis()).valid,"rolagem cancelada não pode ser aplicada");
  JSONObject activityRoll=roll();activityRoll.put("test_key","activity:dnd:90").put("created_at",System.currentTimeMillis()-5L*24L*60L*60L*1000L);JSONObject activityCalc=calc();activityCalc.put("test_key","activity:dnd:90");require(MechanicalRollBinding.verify(activityRoll,activityCalc,hero,System.currentTimeMillis()).valid,"atividade vinculada deve continuar válida até ser consumida");
  System.out.println("OK: rolagens são exclusivas do teste, ação, cena, ator e regra; atividades vinculadas persistem até o consumo.");
 }
}
