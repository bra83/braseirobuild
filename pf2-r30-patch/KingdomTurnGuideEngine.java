package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/**
 * Guia de alfabetização mecânica do Turno do Reino.
 * Lê o mesmo KingmakerRulesEngine/Repository usados pelo runtime; não executa
 * regras próprias e não persiste estado paralelo.
 */
public final class KingdomTurnGuideEngine {
    private final KingmakerRepository repository;
    private final KingmakerRulesEngine rules;
    public KingdomTurnGuideEngine(KingmakerRepository repository,KingmakerRulesEngine rules){this.repository=repository;this.rules=rules;}

    public JSONObject snapshot(){
        JSONObject out=new JSONObject();
        try{
            JSONObject state=repository.state(),turn=repository.activeTurn(),control=repository.controlSummary();
            String phase=turn.length()==0?"COMPLETE":turn.optString("phase","UPKEEP").toUpperCase(Locale.US);
            out.put("turn_active",turn.length()>0&&"active".equals(turn.optString("status")));
            out.put("turn_number",turn.optInt("turn_number",state.optInt("turn_number",0)));
            out.put("phase",phase).put("phase_label",KingmakerService.phaseLabel(phase));
            out.put("sequence",sequence(phase));
            out.put("state",new JSONObject()
                    .put("level",state.optInt("level",1)).put("control_dc",control.optInt("control_dc",0))
                    .put("resource_points",state.optInt("resource_points",0)).put("consumption",state.optInt("consumption",0))
                    .put("unrest",state.optInt("unrest",0)).put("anarchy",state.optInt("anarchy",0)==1||rules.isAnarchy(state.optInt("unrest",0)))
                    .put("food",state.optInt("food",0)).put("lumber",state.optInt("lumber",0)).put("luxuries",state.optInt("luxuries",0))
                    .put("ore",state.optInt("ore",0)).put("stone",state.optInt("stone",0))
                    .put("kingdom_name",state.optString("kingdom_name")).put("capital_name",state.optString("capital_name")));
            out.put("configuration_alerts",configurationAlerts(state,control));
            out.put("options",optionsFor(phase,state));
            out.put("policy","Este guia é somente leitura. Toda escolha chama o resolver oficial do Reino; nenhuma regra é duplicada aqui.");
        }catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){}}
        return out;
    }

    private JSONArray sequence(String current)throws Exception{
        JSONArray a=new JSONArray();
        String[] phases={"UPKEEP","COMMERCE","ACTIVITY","EVENT"};
        String[] labels={"Manutenção","Comércio","Atividades","Eventos"};
        for(int i=0;i<phases.length;i++)a.put(new JSONObject().put("index",i+1).put("key",phases[i]).put("label",labels[i]).put("current",phases[i].equals(current))
                .put("summary",summary(phases[i])));
        return a;
    }

    private String summary(String p){
        if("UPKEEP".equals(p))return "Atualize o Reino, gere recursos e resolva Consumo e consequências obrigatórias antes de avançar.";
        if("COMMERCE".equals(p))return "Resolva decisões econômicas e comerciais válidas para o estado atual do Reino.";
        if("ACTIVITY".equals(p))return "Escolha atividades de liderança, região e civismo; requisitos e alvos vêm do motor.";
        if("EVENT".equals(p))return "Verifique e resolva eventos pendentes; o turno só termina quando o procedimento da fase permitir.";
        return "Turno concluído.";
    }

    private JSONArray configurationAlerts(JSONObject state,JSONObject control)throws Exception{
        JSONArray a=new JSONArray();
        if(state.optString("capital_name","").trim().isEmpty())a.put(alert("capital","Capital ainda não definida","Quando uma atividade exigir capital/assentamento, escolha uma opção válida antes de resolver."));
        if(control.optInt("vacant_roles",0)>0)a.put(alert("leadership","Há cargos de liderança vagos",control.optInt("vacant_roles")+" cargo(s) vago(s) podem impor penalidades; o guia mostrará Nova Liderança quando aplicável."));
        if(repository.settlements().isEmpty())a.put(alert("settlement","Nenhum assentamento registrado","Atividades cívicas que exigem assentamento ficam indisponíveis até a campanha fundar um."));
        if(repository.hexes().isEmpty())a.put(alert("hex","Nenhum hex de Reino registrado","Atividades regionais dependem do estado do Atlas/hexes e só aparecem quando fizerem sentido."));
        if(repository.armies().isEmpty())a.put(alert("army","Nenhum exército registrado","Ações de exército não são exigidas enquanto o Reino não possuir exército."));
        return a;
    }

    private JSONObject alert(String key,String title,String text)throws Exception{return new JSONObject().put("key",key).put("title",title).put("text",text);}

    private JSONArray optionsFor(String phase,JSONObject state)throws Exception{
        JSONArray out=new JSONArray();
        if("UPKEEP".equals(phase)){
            JSONObject turn=repository.activeTurn(),resource=parse(turn.optString("resource_roll_json","{}"));
            if(!resource.optBoolean("consumption_resolved"))out.put(new JSONObject().put("key","resolve_consumption").put("name","Resolver Consumo").put("required",true)
                    .put("description","Escolha como o Reino cobrirá o Consumo atual.").put("changes","Alimento, RP ou Unrest, conforme sua escolha e o resultado.").put("next","Depois, concluir Manutenção."));
            else out.put(new JSONObject().put("key","advance_phase").put("name","Concluir Manutenção").put("required",true).put("description","As obrigações atuais de Manutenção estão resolvidas.").put("changes","Avança o turno para Comércio.").put("next","Comércio."));
            return out;
        }
        if("EVENT".equals(phase)){
            List<JSONObject> events=repository.events("pending");
            if(!events.isEmpty())for(JSONObject e:events)if(!"companion".equalsIgnoreCase(e.optString("event_type"))&&!"exploration".equalsIgnoreCase(e.optString("event_type"))){out.put(new JSONObject().put("key","resolve_event").put("name",e.optString("title","Resolver evento")).put("required",true).put("description","Evento do Reino pendente.").put("changes","Somente os efeitos confirmados pelo resolver/fonte do evento.").put("next","Reavaliar a fase de Evento."));break;}
            if(out.length()==0)out.put(new JSONObject().put("key","advance_phase").put("name","Concluir fase de Evento").put("required",true).put("description","Nenhum evento do Reino está pendente.").put("changes","Conclui o turno quando o motor permitir.").put("next","Voltar à aventura."));
            return out;
        }
        JSONArray catalog=rules.activityCatalog();
        int unrest=state.optInt("unrest",0);
        for(int i=0;i<catalog.length();i++){
            JSONObject a=catalog.optJSONObject(i);if(a==null||!phase.equalsIgnoreCase(a.optString("phase")))continue;
            String key=a.optString("key");boolean valid=rules.canPerformActivity(key,unrest)&&requirementsAvailable(key);
            JSONObject item=new JSONObject().put("key",key).put("name",a.optString("name")).put("category",a.optString("category"))
                    .put("required",false).put("valid_now",valid).put("skill",a.optString("skill")).put("ability",a.optString("ability"))
                    .put("description",a.optString("description")).put("requirements",requirementText(key,valid))
                    .put("changes",changeText(key)).put("next","Depois da resolução, o motor atualiza o estado e o guia recalcula as opções.");
            out.put(item);
        }
        out.put(new JSONObject().put("key","advance_phase").put("name","Concluir esta fase").put("required",false).put("valid_now",true)
                .put("description","Encerra a fase somente se os gates do motor permitirem.").put("changes","Avança para a próxima fase; não altera resultados de atividades.").put("next",nextPhaseLabel(phase)));
        return out;
    }

    private boolean requirementsAvailable(String key){
        if("build_structure".equals(key)||"demolish".equals(key)||"repair_structure".equals(key)||"upgrade_settlement".equals(key))return !repository.settlements().isEmpty();
        if("claim_hex".equals(key)||"abandon_hex".equals(key)||"clear_hex".equals(key)||"reconnoiter_hex".equals(key)||"build_roads".equals(key)||"irrigate".equals(key)||"establish_settlement".equals(key)||"work_sites".equals(key)||"fortify_hex".equals(key))return !repository.hexes().isEmpty();
        return true;
    }

    private String requirementText(String key,boolean valid){
        if(!valid)return "Indisponível no estado atual do Reino; o motor bloqueará a execução.";
        if("build_structure".equals(key)||"demolish".equals(key)||"repair_structure".equals(key)||"upgrade_settlement".equals(key))return "Escolher assentamento/estrutura válida; custos, lotes e requisitos são validados pelo resolver.";
        if(key.contains("hex")||"build_roads".equals(key)||"irrigate".equals(key)||"work_sites".equals(key)||"establish_settlement".equals(key))return "Escolher hex válido no estado persistido do Atlas/Reino.";
        if(key.contains("trade"))return "Usar somente parceiros/acordos/commodities realmente disponíveis no estado persistido.";
        if("new_leadership".equals(key))return "Escolher o cargo e a liderança; nenhuma nomeação é automática.";
        return "O resolver oficial valida fase, perícia, CD, frequência, alvo e demais requisitos.";
    }

    private String changeText(String key){
        if("collect_taxes".equals(key))return "Pode alterar RP/Economia e, conforme o grau, Unrest/Ruin previstos pelo resolver.";
        if("improve_lifestyle".equals(key)||"rest_and_relax".equals(key)||"quell_unrest".equals(key))return "Pode alterar Unrest e efeitos associados conforme o grau de sucesso.";
        if("tap_treasury".equals(key)||"capital_investment".equals(key))return "Pode alterar RP/tesouro e efeitos adiados definidos pelo resolver.";
        if(key.contains("trade"))return "Pode alterar commodities, RP e/ou estado de acordos comerciais.";
        if("build_structure".equals(key)||"demolish".equals(key)||"repair_structure".equals(key))return "Pode alterar estruturas, lotes, RP, commodities e bônus do assentamento.";
        if("upgrade_settlement".equals(key)||"establish_settlement".equals(key))return "Pode alterar categoria/estado do assentamento e seus limites.";
        if(key.contains("hex")||"build_roads".equals(key)||"irrigate".equals(key)||"work_sites".equals(key))return "Pode alterar posse, melhorias, recursos ou Consumo associados ao hex.";
        if("new_leadership".equals(key))return "Altera ocupação de cargo e os modificadores derivados da liderança.";
        return "Os campos alterados dependem do grau de sucesso e são aplicados pelo resolver oficial desta atividade.";
    }

    private String nextPhaseLabel(String phase){String n=rules.nextPhase(phase);return "COMPLETE".equals(n)?"Concluir o turno":KingmakerService.phaseLabel(n);}
    private static JSONObject parse(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
}
