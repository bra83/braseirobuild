package com.braseiro.pathfinder2e;
import org.json.*;
public final class RuleProcedureCompilerSelfTest {
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static boolean arrayContains(JSONArray a,String value){if(a==null)return false;for(int i=0;i<a.length();i++)if(value.equalsIgnoreCase(a.optString(i)))return true;return false;}
 public static void main(String[] args)throws Exception{
  JSONObject source=new JSONObject().put("rule_id","manual:1:page:10").put("book","Livro do Jogador").put("page",10)
   .put("excerpt","Quando uma criatura entra no alcance de 30 pés, ela deve fazer um teste de resistência de Destreza CD 15. Em uma falha, sofre 2d6+3 de dano e fica Caída por 1 rodada. Em um sucesso, sofre metade do dano. Exceto se estiver imune a fogo.");
  JSONObject compiled=RuleProcedureCompiler.compile(source,new JSONArray().put("teste_salvaguarda").put("ataque_dano"));
  require(compiled.optBoolean("executable",false),"regra deveria ser executável");
  require(compiled.optJSONArray("atomic_procedures").length()>=1,"procedimento atômico não criado");
  require(compiled.optJSONArray("dice_expressions").optString(0).equalsIgnoreCase("2d6+3"),"fórmula incorreta");

  JSONObject reaction=RuleProcedureCompiler.compile(new JSONObject().put("rule_id","r2").put("excerpt","Como reação, quando uma criatura sair do seu alcance, faça um ataque corpo a corpo contra ela. Em um acerto, ela sofre 1d8 de dano."),new JSONArray());
  JSONArray economy=reaction.optJSONArray("action_economy");
  require(arrayContains(economy,"reaction"),"reação não reconhecida");
  require(!arrayContains(economy,"action"),"a palavra reaction não pode gerar ação normal por substring");

  JSONObject disconnected=RuleProcedureCompiler.compile(new JSONObject().put("rule_id","r3").put("excerpt","O salão mede 2d6 metros de largura. Muitos anos depois, um alvo fica Atordoado."),new JSONArray());
  require(!disconnected.optBoolean("executable",false),"número e efeito de frases desconectadas não podem formar procedimento executável");

  JSONObject multiple=RuleProcedureCompiler.compile(new JSONObject().put("rule_id","r4").put("excerpt","Em um acerto, o alvo sofre 1d6 de dano. Em um acerto diferente, outra criatura sofre 2d8 de dano."),new JSONArray());
  if(multiple.optInt("executable_procedure_count",0)>1)require(multiple.optBoolean("requires_procedure_id",false),"múltiplos procedimentos exigem procedure_id");
  System.out.println("OK: compilador preservou coerência atômica, distinguiu reação de ação e bloqueou combinação desconectada.");
 }
}
