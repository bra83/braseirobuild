package com.braseiro.pathfinder2e;
import org.json.*;
/** Regressões fail-closed do verificador mecânico no host PF2e. */
public final class RuleResolutionVerifierSelfTest {
 private static int checks=0;
 public static void main(String[] args)throws Exception{
  java.lang.reflect.Method grounded=RuleResolutionVerifier.class.getDeclaredMethod("signedNumberGrounded",String.class,int.class,String[].class);grounded.setAccessible(true);
  String[] labels={"para atingir","to hit","bonus de ataque","attack bonus","ataque corpo a corpo","ataque a distancia"};
  check((Boolean)grounded.invoke(null,"Ataque corpo a corpo: +17 para atingir",17,(Object)labels),"bônus local é fundamentado");
  check(!(Boolean)grounded.invoke(null,"Dano 17; CA 29; CD 26",17,(Object)labels),"número solto não fundamenta ataque");
  RuleResolutionVerifier verifier=new RuleResolutionVerifier(new AppDb());
  JSONObject hero=new JSONObject().put("id",4).put("name","Ayla").put("hp",20).put("max_hp",20).put("level",5);
  RuleResolutionVerifier.Result noBasis=verifier.verifyStateUpdate(null,new JSONObject().put("hp_delta",-5),hero,new JSONObject());
  check(!noBasis.valid,"mutação sem base é bloqueada");
  RuleResolutionVerifier.Result noCalc=verifier.verifyStateUpdate(new JSONObject(),new JSONObject().put("hp_delta",-5),hero,new JSONObject());
  check(!noCalc.valid,"mutação sem cálculo é bloqueada");
  JSONObject basis=new JSONObject().put("rule_id","pf2:test:1").put("calculation",new JSONObject().put("operation","damage").put("fixed_value",5));
  RuleResolutionVerifier.Result noEvidence=verifier.verifyStateUpdate(basis,new JSONObject().put("hp_delta",-5),hero,new JSONObject().put("evidence",new JSONArray()));
  check(!noEvidence.valid,"cálculo sem evidência recuperada é bloqueado");
  JSONObject source=new JSONObject().put("rule_id","pf2:test:1").put("book","Player Core").put("page",42).put("excerpt","O alvo sofre 5 de dano.");
  JSONObject retrieval=new JSONObject().put("evidence",new JSONArray().put(source)).put("structured_rules",new JSONArray());
  RuleResolutionVerifier.Result noProcedure=verifier.verifyStateUpdate(basis,new JSONObject().put("hp_delta",-5),hero,retrieval);
  check(!noProcedure.valid,"evidência sem procedimento executável é bloqueada");
  check(noProcedure.reason.toLowerCase().contains("procedimento"),"razão de bloqueio explica ausência de procedimento");
  System.out.println("OK: "+checks+" verificações fail-closed do RuleResolutionVerifier PF2e.");
 }
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
