package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
/** Kingmaker AP pp. 514, 520, 537; Companion Guide companion leadership entries. */
public final class KingmakerLeadershipSemantics {
 private KingmakerLeadershipSemantics(){}
 public static final List<String> ALL_ROLES=Collections.unmodifiableList(Arrays.asList(
  "ruler","counselor","general","emissary","magister","treasurer","viceroy","warden"
 ));
 public static boolean validRole(String role){
  return role!=null&&ALL_ROLES.contains(role.trim().toLowerCase(Locale.ROOT));
 }
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
  return leaderActivityModifier(appointment,turn,"ACTIVITY");
 }
 /** The failure penalty applies only to activities in the Activity phase (Kingmaker p.520). */
 public static int leaderActivityModifier(JSONObject appointment,int turn,String phase){
  if(appointment==null||appointment.optBoolean("vacant"))return 0;
  if(appointment.optBoolean("approval_pending"))return "ACTIVITY".equalsIgnoreCase(phase)?-1:0;
  return "CRITICAL_SUCCESS".equals(appointment.optString("degree"))&&turn<=appointment.optInt("appointed_turn")+1?1:0;
 }
 /**
  * Validates when New Leadership or leadership assignment may occur (Kingmaker pp. 514, 520, 537):
  * - Turn 0 (Founding): free assignment, no check allowed.
  * - Turn 1+ during UPKEEP (Upkeep Step 1) or ACTIVITY: normal in-turn New Leadership check.
  * - Turn 1+ outside a Kingdom turn (or in any phase) when roleVacant, incumbentUnavailable, or emergencyReplacement:
  *   extraordinary immediate replacement check allowed per pp. 514 & 520.
  */
 public static JSONObject validateTiming(int turnNumber,String activePhase,boolean roleVacant,boolean incumbentUnavailable,boolean emergencyReplacement)throws Exception{
  if(turnNumber<=0)throw new IllegalStateException("Durante a fundação, nomeie líderes gratuitamente, sem teste.");
  String phase=activePhase==null?"":activePhase.trim().toUpperCase(Locale.ROOT);
  boolean extraordinary=roleVacant||incumbentUnavailable||emergencyReplacement;
  if(phase.isEmpty()){
   if(!extraordinary)throw new IllegalStateException("Abra o turno do Reino para realizar Nova Liderança.");
   return new JSONObject().put("allowed",true).put("mode","EXTRAORDINARY_OUT_OF_TURN").put("requires_check",true).put("phase_recorded","OUT_OF_TURN");
  }
  if("UPKEEP".equals(phase)){
   return new JSONObject().put("allowed",true).put("mode",extraordinary?"UPKEEP_VACANCY_OR_REPLACEMENT":"UPKEEP_STEP_1").put("requires_check",true).put("phase_recorded","UPKEEP");
  }
  if(extraordinary){
   return new JSONObject().put("allowed",true).put("mode","EXTRAORDINARY_REPLACEMENT").put("requires_check",true).put("phase_recorded",phase);
  }
  if("ACTIVITY".equals(phase)){
   return new JSONObject().put("allowed",true).put("mode","ACTIVITY_LEADERSHIP").put("requires_check",true).put("phase_recorded","ACTIVITY");
  }
  throw new IllegalStateException("A troca ordinária de líder disponível deve ocorrer na Etapa 1 da Manutenção (Upkeep Step 1); fora dela, apenas vagas ou baixas inesperadas permitem substituição imediata.");
 }
 /**
  * Validates that a single person (PC, NPC, or Companion) never holds two leadership roles simultaneously.
  */
 public static void validateSingleRolePerLeader(JSONArray leaders,String targetRoleKey,String entityKind,long entityId,String leaderName){
  String cleanName=leaderName==null?"":leaderName.trim();
  if(cleanName.isEmpty())return;
  String cleanRole=targetRoleKey==null?"":targetRoleKey.trim().toLowerCase(Locale.ROOT);
  String cleanKind=entityKind==null?"":entityKind.trim().toLowerCase(Locale.ROOT);
  if(leaders==null)return;
  for(int i=0;i<leaders.length();i++){
   JSONObject occupied=leaders.optJSONObject(i);
   if(occupied==null||occupied.optInt("vacant",1)==1)continue;
   String occRole=occupied.optString("role_key","").trim().toLowerCase(Locale.ROOT);
   if(cleanRole.equals(occRole))continue;
   long occId=occupied.optLong("entity_id",0);
   String occKind=occupied.optString("entity_kind","").trim().toLowerCase(Locale.ROOT);
   String occName=occupied.optString("leader_name","").trim();
   boolean sameEntity=entityId>0&&occId==entityId&&cleanKind.equals(occKind);
   boolean sameName=!occName.isEmpty()&&cleanName.equalsIgnoreCase(occName);
   if(sameEntity||sameName){
    if("companion".equals(cleanKind)||"npc".equals(cleanKind)||"companion".equals(occKind)||"npc".equals(occKind)){
     throw new IllegalStateException("Este companheiro já ocupa outro cargo de liderança.");
    }
    throw new IllegalStateException("A mesma pessoa só pode ocupar um cargo de liderança.");
   }
  }
 }
 /**
  * Validates independent reselection of up to 4 manual invested roles at Founding or Upkeep Step 1 (pp. 514, 537).
  * Companions in their preferred role provide automatic investment in addition to the 4 manual roles.
  */
 public static Set<String> validateInvestedReselection(JSONArray leaders,JSONArray requestedRoles,Set<String> autoInvestedRoles){
  if(requestedRoles==null)throw new IllegalArgumentException("Informe os cargos investidos.");
  Set<String> auto=autoInvestedRoles==null?Collections.<String>emptySet():autoInvestedRoles;
  Set<String> manual=new LinkedHashSet<String>();
  for(int i=0;i<requestedRoles.length();i++){
   String role=requestedRoles.optString(i,"").trim().toLowerCase(Locale.ROOT);
   if(!validRole(role))throw new IllegalArgumentException("Cargo de liderança inválido para investimento: "+role);
   JSONObject row=null;
   if(leaders!=null){
    for(int j=0;j<leaders.length();j++){
     JSONObject cand=leaders.optJSONObject(j);
     if(cand!=null&&role.equals(cand.optString("role_key","").trim().toLowerCase(Locale.ROOT))){row=cand;break;}
    }
   }
   if(row==null||row.optInt("vacant",1)==1||row.optString("leader_name","").trim().isEmpty())
    throw new IllegalStateException("Não é possível investir em um cargo vago: "+role);
   if(!auto.contains(role))manual.add(role);
  }
  if(manual.size()>4)throw new IllegalStateException("Escolha no máximo quatro cargos investidos.");
  Set<String> effective=new LinkedHashSet<String>(manual);
  effective.addAll(auto);
  return effective;
 }
}
