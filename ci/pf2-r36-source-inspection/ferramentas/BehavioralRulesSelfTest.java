package com.braseiro.pathfinder2e;
import org.json.JSONObject;
/** Regressões mecânicas PF2e Remaster executáveis. */
public final class BehavioralRulesSelfTest {
 private static int checks=0;
 public static void main(String[] args)throws Exception{
  Pathfinder2eRulesEngine pf=new Pathfinder2eRulesEngine();
  check(pf.proficiency(5,"trained")==7,"treinado = nível + 2");
  check(pf.proficiency(5,"expert")==9,"especialista = nível + 4");
  check(pf.proficiency(5,"master")==11,"mestre = nível + 6");
  check(pf.proficiency(5,"legendary")==13,"lendário = nível + 8");
  check(pf.degree(10,25,15)==Pathfinder2eRulesEngine.Degree.CRITICAL_SUCCESS,"10 acima = sucesso crítico");
  check(pf.degree(20,15,15)==Pathfinder2eRulesEngine.Degree.CRITICAL_SUCCESS,"20 natural melhora sucesso em um grau");
  check(pf.degree(1,15,15)==Pathfinder2eRulesEngine.Degree.FAILURE,"1 natural piora sucesso em um grau");
  check(pf.degree(1,5,15)==Pathfinder2eRulesEngine.Degree.CRITICAL_FAILURE,"falha por 10 + 1 permanece crítica");
  check(pf.mapPenalty(1,false)==0&&pf.mapPenalty(2,false)==-5&&pf.mapPenalty(3,false)==-10,"MAP normal");
  check(pf.mapPenalty(2,true)==-4&&pf.mapPenalty(3,true)==-8,"MAP ágil");
  JSONObject hero=new JSONObject().put("level",5).put("sheet_json",new JSONObject()
    .put("level",5).put("attribute_modifiers",new JSONObject().put("str",4).put("dex",3).put("con",2).put("wis",1))
    .put("skill_ranks",new JSONObject().put("athletics","expert").put("stealth","trained"))
    .put("perception_rank","trained").put("fortitude_rank","expert").put("reflex_rank","trained").put("will_rank","trained").toString());
  check(pf.checkBonus(hero,"","Atletismo")==13,"Atletismo usa FOR + proficiência");
  check(pf.checkBonus(hero,"","Furtividade")==10,"Furtividade usa DES + proficiência");
  check(pf.initiativeBonus(hero,"Percepção")==8,"iniciativa por Percepção");
  check(pf.savingThrowBonus(hero,"Fortitude")==11,"Fortitude usa CON + proficiência");
  check(pf.savingThrowBonus(hero,"Reflexos")==10,"Reflexos usa DES + proficiência");
  JSONObject budget=pf.turnBudget(2,false,false,false,0);check(budget.optInt("actions_remaining")==1&&budget.optBoolean("reaction_available"),"3 ações + reação");
  JSONObject quick=pf.turnBudget(1,false,true,false,0);check(quick.optInt("actions_max")==4,"quickened expõe ação restrita adicional");
  JSONObject ko=pf.knockedOut(2,0,false);check(ko.optInt("dying")==3&&!ko.optBoolean("dead"),"dying inicial = 1 + wounded");
  JSONObject rec=pf.recoveryCheck(20,2,0,0);check(rec.optInt("dying")==0&&rec.optInt("wounded")==1&&rec.optBoolean("stabilized"),"recuperação crítica estabiliza e aumenta wounded");
  check(pf.validConditionMutation("Amedrontado",2),"condição valorada aceita valor");
  check(!pf.validConditionMutation("Amedrontado",0),"condição valorada rejeita zero");
  check(pf.authority().contains("nenhuma mecânica D&D, Mystara ou PF1 é fallback"),"firewall explícito");
  System.out.println("OK: "+checks+" cenários comportamentais Pathfinder 2e Remaster.");
 }
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
