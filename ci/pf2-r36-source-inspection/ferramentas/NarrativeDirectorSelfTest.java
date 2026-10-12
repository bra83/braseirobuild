package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public final class NarrativeDirectorSelfTest {
    private static int ok=0;
    private static void check(String name, boolean value){
        if(!value)throw new AssertionError(name);
        ok++; System.out.println("OK  "+name);
    }
    private static boolean empty(JSONArray a){return a!=null&&a.length()==0;}
    public static void main(String[] args)throws Exception{
        check("consulta de contexto congela", BraseiroNarrativeDirector.classify("Essa cidade permite andar armado?")==BraseiroNarrativeDirector.Intent.NARRATIVE_CONTEXT_QUERY);
        check("fala diegética vence a interrogação", BraseiroNarrativeDirector.classify("Pergunto ao guarda se posso andar armado.")==BraseiroNarrativeDirector.Intent.DIEGETIC_SPEECH);
        check("planejamento não executa", BraseiroNarrativeDirector.classify("Talvez eu espere anoitecer")==BraseiroNarrativeDirector.Intent.PLANNING);
        check("execução altera ficção", BraseiroNarrativeDirector.classify("Eu espero até anoitecer")==BraseiroNarrativeDirector.Intent.DIEGETIC_ACTION);
        check("regra é consulta", BraseiroNarrativeDirector.classify("Como funciona vantagem em D&D 5e?")==BraseiroNarrativeDirector.Intent.RULE_QUERY);
        check("espaço é clarificação", BraseiroNarrativeDirector.classify("Consigo ver a janela daqui?")==BraseiroNarrativeDirector.Intent.SPATIAL_CLARIFICATION);
        check("continua retoma", BraseiroNarrativeDirector.classify("continua")==BraseiroNarrativeDirector.Intent.CONTINUE);

        JSONObject dangerous=new JSONObject()
            .put("narration","A resposta de contexto é curta e direta.")
            .put("visual_prompt","explosão")
            .put("options",new JSONArray().put("Atacar"))
            .put("pending_test",new JSONObject().put("required",true))
            .put("state_updates",new JSONArray().put(new JSONObject().put("hp_delta",-7)))
            .put("npc_updates",new JSONArray().put(new JSONObject().put("name","Guarda")))
            .put("world_events",new JSONArray().put(new JSONObject().put("title","Incêndio")))
            .put("map_updates",new JSONArray().put(new JSONObject()))
            .put("scene_state",new JSONObject().put("location","outro lugar"))
            .put("combat",new JSONObject().put("action","start").put("enemies",new JSONArray().put(new JSONObject())));
        JSONObject frozen=BraseiroNarrativeDirector.freezeEnvelope(dangerous,"Essa cidade permite andar armado?");
        check("freeze limpa dano", empty(frozen.optJSONArray("state_updates")));
        check("freeze limpa NPC", empty(frozen.optJSONArray("npc_updates")));
        check("freeze limpa eventos", empty(frozen.optJSONArray("world_events")));
        check("freeze limpa mapa", empty(frozen.optJSONArray("map_updates")));
        check("freeze limpa opções", empty(frozen.optJSONArray("options")));
        check("freeze limpa visual", frozen.optString("visual_prompt").isEmpty());
        check("freeze mantém combate inerte", "none".equals(frozen.optJSONObject("combat").optString("action")));
        check("freeze marcado", frozen.optBoolean("_non_advancing",false));

        JSONObject narrativeState=new JSONObject()
            .put("checkpoint_summary","Alda está diante da janela quebrada.")
            .put("scene_function","investigação").put("temperature",2).put("focal_point","vidro do lado de fora")
            .put("motifs",new JSONArray().put(new JSONObject().put("key","tres_circulos").put("observation","símbolo branco").put("status","recorrente")))
            .put("clues",new JSONArray().put(new JSONObject().put("key","vidro_externo").put("evidence","cacos no jardim").put("status","percebida")))
            .put("topology",new JSONArray().put(new JSONObject().put("from","quarto").put("to","jardim").put("relation","janela")))
            .put("open_questions",new JSONArray().put("Quem abriu a janela?"));
        JSONObject scene=new JSONObject().put("location","Quarto de Aldren").put("time_of_day","noite").put("weather","seco");
        JSONObject env=new JSONObject().put("narrative_state",narrativeState);
        JSONObject memory=BraseiroNarrativeDirector.recordAdvancingMemory(new JSONObject(),"Examino a janela", "Os cacos estão do lado de fora; a madeira foi lascada junto ao fecho.",env,scene);
        check("checkpoint persistido", memory.optJSONObject("checkpoint")!=null&&"Quarto de Aldren".equals(memory.optJSONObject("checkpoint").optString("location")));
        check("pista persistida", memory.optJSONArray("clues")!=null&&memory.optJSONArray("clues").length()==1);
        check("motivo persistido", memory.optJSONArray("motifs")!=null&&memory.optJSONArray("motifs").length()==1);
        check("topologia persistida", memory.optJSONArray("topology")!=null&&memory.optJSONArray("topology").length()==1);
        check("DNA iniciado", memory.optJSONObject("style_dna")!=null&&memory.optJSONObject("style_dna").optInt("beats",0)==1);
        String oldCheckpoint=memory.optJSONObject("checkpoint").toString();
        JSONObject clarified=BraseiroNarrativeDirector.recordClarification(memory,"Consigo ver a porta daqui?","Sim. A porta fica à sua direita e permanece aberta.");
        check("clarificação preserva checkpoint", clarified.optJSONObject("checkpoint")!=null && memory.optJSONObject("checkpoint").optString("location").equals(clarified.optJSONObject("checkpoint").optString("location")) && memory.optJSONObject("checkpoint").optString("narration").equals(clarified.optJSONObject("checkpoint").optString("narration")));
        check("clarificação registrada", clarified.optJSONArray("clarifications")!=null&&clarified.optJSONArray("clarifications").length()==1);

        JSONArray recent=new JSONArray().put("Do alto da estrada, Valedouro ocupa os dois lados do rio. Uma fila de carroças está parada diante do portão oeste.");
        String repeated="Do alto da estrada, Valedouro ocupa os dois lados do rio. Uma fila de carroças está parada diante do portão oeste. Os guardas discutem junto aos barris.";
        check("anti-repetição pega abertura idêntica", BraseiroNarrativeDirector.qualityProblem(repeated,recent,"Eu entro na cidade").length()>0);
        String fresh="A carroça emperra no arco do portão. O cocheiro desce para soltar uma correia enquanto dois guardas abrem espaço entre os pedestres. No terceiro barril há um símbolo branco que nenhum deles toca.";
        check("cena concreta inédita passa QA", BraseiroNarrativeDirector.qualityProblem(fresh,recent,"Eu entro na cidade").isEmpty());
        check("função investigação", "investigação".equals(BraseiroNarrativeDirector.sceneFunction("Examino as marcas no chão",scene)));
        check("temperatura investigação", BraseiroNarrativeDirector.sceneTemperature(scene,"Examino as marcas no chão")==2);
        String contract=BraseiroNarrativeDirector.contract(new JSONObject().put("tone","aventura"),scene,recent);
        check("contrato inclui camadas", contract.contains("PERCEPTÍVEL")&&contract.contains("INFERÍVEL")&&contract.contains("OCULTO"));
        check("contrato inclui 24 funções", contract.contains("chegada")&&contract.contains("encerramento")&&contract.contains("aftermath"));
        check("contrato inclui TTS invisível", contract.contains("TTS INVISÍVEL"));
        System.out.println("RESULTADO NARRATIVE DIRECTOR 2.0: "+ok+"/"+ok);
    }
}
