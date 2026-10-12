package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class MasterSupportPanelEngineSelfTest {
    private static int ok=0;
    private static void check(boolean value,String name){
        if(!value)throw new AssertionError(name);
        ok++;
        System.out.println("OK  "+name);
    }

    public static void main(String[] args)throws Exception{
        JSONObject env=new JSONObject()
                .put("_non_advancing",true)
                .put("narration","A testemunha interrompe o movimento ao ouvir o nome do cais.");
        JSONObject retrieval=new JSONObject()
                .put("required",true)
                .put("status","ready")
                .put("evidence_count",2)
                .put("sufficient_for_mechanics",true);
        JSONArray citations=new JSONArray()
                .put(new JSONObject().put("manual_title","Livro do Jogador").put("page_no",174).put("excerpt","Testes de atributo e perícias."));
        JSONObject scene=new JSONObject()
                .put("location","Taverna do Cervo")
                .put("time_of_day","noite")
                .put("present_npcs",new JSONArray().put(new JSONObject().put("name","Mara")));
        List<JSONObject> heroes=new ArrayList<>();
        heroes.add(new JSONObject().put("name","Lysa").put("wis",16).put("cha",10).put("int_score",12));
        JSONArray options=new JSONArray().put(new JSONObject().put("label","Examinar o selo"));

        JSONObject support=MasterSupportPanelEngine.build(env,retrieval,citations,scene,heroes,options,new JSONObject(),"Quem está na cena e ela parece nervosa?",false);
        check(support.optBoolean("outside_fiction"),"painel fica fora da ficção");
        check(support.optBoolean("rules_searched"),"consulta aos manuais fica visível");
        check(support.optInt("evidence_count")==2,"quantidade de evidências é preservada");
        check(support.optString("what_happened").contains("congelada"),"consulta auxiliar declara congelamento");
        check(support.optString("scene_summary").contains("Mara"),"presentes entram no resumo da cena");
        check(support.optJSONArray("skill_suggestions").length()>0,"perícias opcionais são sugeridas");
        check(!support.optJSONArray("skill_suggestions").optJSONObject(0).optBoolean("automatic_roll",true),"sugestão não rola automaticamente");
        check(support.optJSONArray("sources").length()==1,"fontes recuperadas aparecem no painel");
        check(support.optBoolean("options_visible"),"estado das sugestões opcionais é reportado");

        JSONObject answered=MasterSupportPanelEngine.withAnswer(support,"Qual a distância até Mara?","A distância exata ainda não foi fixada.",retrieval,citations);
        check(answered.optString("last_answer").contains("não foi fixada"),"distância não estabelecida não é inventada");
        check(answered.optBoolean("outside_fiction"),"resposta auxiliar permanece fora da ficção");

        JSONObject blocked=MasterSupportPanelEngine.build(new JSONObject(),new JSONObject().put("required",true).put("status","index_incomplete").put("sufficient_for_mechanics",false),new JSONArray(),scene,heroes,new JSONArray(),new JSONObject().put("required",true).put("ability","Sabedoria").put("skill","Percepção").put("dc",14),"Procuro uma armadilha",false);
        check(blocked.optString("rules_explanation").contains("bloqueado"),"teste sem prova fica bloqueado");

        System.out.println("RESULTADO PAINEL AUXILIAR: "+ok+"/"+ok);
    }
}
