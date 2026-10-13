package com.braseiro.pathfinder2e;
import org.json.JSONObject;
import java.util.Arrays;
/** Kingmaker AP p. 520; Companion Guide companion leadership entries. */
public final class KingmakerLeadershipSemantics {
 private KingmakerLeadershipSemantics(){}
 public static boolean validSkill(String skill){
  return Arrays.asList("intrigue","politics","statecraft","warfare").contains(skill);
 }
 public static int skillCircumstance(String role,String skill){
  if(!validSkill(skill))throw new IllegalArgumentException("Nova Liderança exige Intriga, Política, Estadismo ou Guerra.");
  int bonus=(("intrigue".equals(skill)&&Arrays.asList("emissary","treasurer").contains(role))||
   ("politics".equals(skill)&&Arrays.asList("counselor","ruler").contains(role))||
   ("statecraft".equals(skill)&&Arrays.asList("magister","viceroy").contains(role))||
   ("warfare".equals(skill)&&Arrays.asList("general","warden").contains(role)))?2:0;
  return bonus+("ruler".equals(role)?-4:0);
 }
 public static String improved(String degree,boolean preferred){
  if(!Arrays.asList("CRITICAL_SUCCESS","SUCCESS","FAILURE","CRITICAL_FAILURE").contains(degree))
   throw new IllegalArgumentException("Grau de sucesso inválido.");
  if(!preferred)return degree;
  if("CRITICAL_FAILURE".equals(degree))return "FAILURE";
  if("FAILURE".equals(degree))return "SUCCESS";
  return "CRITICAL_SUCCESS";
 }
 public static JSONObject appointment(String degree,String role,int turn,boolean jaethal,boolean famous,int d3)throws Exception{
  if(!Arrays.asList("CRITICAL_SUCCESS","SUCCESS","FAILURE","CRITICAL_FAILURE").contains(degree))
   throw new IllegalArgumentException("Grau de Nova Liderança inválido.");
  if(jaethal&&famous&&(d3<1||d3>3))throw new IllegalArgumentException("Jaethal exige 1d3 real.");
  boolean accepted=!"CRITICAL_FAILURE".equals(degree);
  int unrest=("ruler".equals(role)&&!"CRITICAL_SUCCESS".equals(degree)?1:0)+("CRITICAL_FAILURE".equals(degree)?1:0);
  if(accepted&&jaethal&&famous)unrest+=d3;
  return new JSONObject().put("role",role).put("degree",degree).put("vacant",!accepted)
   .put("new_leadership_unrest",unrest).put("jaethal_unrest_d3",accepted&&jaethal&&famous?d3:0)
   .put("approval_pending","FAILURE".equals(degree)).put("appointed_turn",turn)
   .put("leader_activity_modifier","CRITICAL_SUCCESS".equals(degree)?1:("FAILURE".equals(degree)?-1:0))
   .put("source_ref","Kingmaker Adventure Path p. 520; Kingmaker Companion Guide");
 }
 public static int leaderActivityModifier(JSONObject appointment,int turn){
  if(appointment==null||appointment.optBoolean("vacant"))return 0;
  if(appointment.optBoolean("approval_pending"))return -1;
  return "CRITICAL_SUCCESS".equals(appointment.optString("degree"))&&turn<=appointment.optInt("appointed_turn")+1?1:0;
 }
}
