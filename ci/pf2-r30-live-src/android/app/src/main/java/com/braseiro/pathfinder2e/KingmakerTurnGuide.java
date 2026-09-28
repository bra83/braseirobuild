package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/**
 * Projeção didática do Turno do Reino.
 *
 * Não resolve ações, não altera estado e não mantém um segundo catálogo.
 * Toda opção vem do KingmakerRulesEngine e todo bloqueio/configuração vem do
 * KingmakerRepository/AppDb atuais.
 */
public final class KingmakerTurnGuide {
    private final AppDb db;
    private final KingmakerRepository repository;
    private final KingmakerRulesEngine rules;

    public KingmakerTurnGuide(AppDb db, KingmakerRepository repository, KingmakerRulesEngine rules) {
        this.db = db;
        this.repository = repository;
        this.rules = rules;
    }

    public JSONObject snapshot() {
        JSONObject out = new JSONObject();
        try {
            JSONObject state = repository.state();
            JSONObject turn = repository.activeTurn();
            JSONObject control = repository.controlSummary();
            String phase = turn.length() > 0 ? turn.optString("phase", "UPKEEP") : "COMPLETE";
            int unrest = state.optInt("unrest", 0);

            out.put("title", "Guia completo do Turno do Reino");
            out.put("turn_active", turn.length() > 0 && "active".equals(turn.optString("status")));
            out.put("turn_number", turn.length() > 0 ? turn.optInt("turn_number") : state.optInt("turn_number", 0));
            out.put("current_phase", phase);
            out.put("current_phase_label", KingmakerService.phaseLabel(phase));
            out.put("sequence", rules.turnProcedureGuide());
            out.put("configuration", configuration(state, control));
            out.put("current_options", currentOptions(state, turn, unrest));
            out.put("activity_catalog", rules.activityGuideCatalog());
            out.put("authority", "read_only_projection_of_KingmakerRulesEngine_and_KingmakerRepository");
            out.put("decision_rule", "O guia informa; a escolha continua sendo do jogador e é executada pelo resolver oficial.");
        } catch (Exception e) {
            try { out.put("error", String.valueOf(e.getMessage())); } catch (Exception ignored) {}
        }
        return out;
    }

    public String renderText() {
        JSONObject s = snapshot();
        StringBuilder b = new StringBuilder();
        b.append("GUIA COMPLETO DO TURNO DO REINO\n\n");
        if (s.has("error")) return b.append("Não foi possível montar o guia: ").append(s.optString("error")).toString();

        if (s.optBoolean("turn_active")) {
            b.append("AGORA\nTurno ").append(s.optInt("turn_number"))
                    .append(" • ").append(s.optString("current_phase_label")).append("\n\n");
        } else {
            b.append("AGORA\nNenhum turno ativo. Ao iniciar, o motor rola/valida os Recursos e abre a primeira decisão humana.\n\n");
        }

        JSONArray config = s.optJSONArray("configuration");
        if (config != null && config.length() > 0) {
            b.append("CONFIGURAÇÃO QUE EXIGE SUA ATENÇÃO\n");
            for (int i=0;i<config.length();i++) {
                JSONObject x=config.optJSONObject(i); if(x==null)continue;
                b.append("• ").append(x.optString("title")).append("\n  ").append(x.optString("text")).append("\n");
                JSONArray choices=x.optJSONArray("choices");
                if(choices!=null&&choices.length()>0)b.append("  Você pode escolher: ").append(join(choices)).append("\n");
            }
            b.append("\n");
        }

        b.append("SEQUÊNCIA COMPLETA\n");
        JSONArray sequence=s.optJSONArray("sequence");
        if(sequence!=null)for(int i=0;i<sequence.length();i++){
            JSONObject phase=sequence.optJSONObject(i); if(phase==null)continue;
            boolean current=phase.optString("key").equals(s.optString("current_phase"));
            b.append(current?"▶ ":"  ").append(i+1).append(". ").append(phase.optString("name")).append("\n");
            JSONArray steps=phase.optJSONArray("steps");
            if(steps!=null)for(int j=0;j<steps.length();j++){
                JSONObject step=steps.optJSONObject(j); if(step==null)continue;
                b.append("   ").append(j+1).append(") ").append(step.optString("name"));
                if(step.optBoolean("required"))b.append(" • obrigatório");
                else b.append(" • opcional/condicional");
                b.append("\n      ").append(step.optString("explanation")).append("\n");
                String changes=step.optString("changes");
                if(changes.length()>0)b.append("      Pode mudar: ").append(changes).append("\n");
            }
            b.append("\n");
        }

        JSONArray options=s.optJSONArray("current_options");
        if(options!=null&&options.length()>0){
            b.append("OPÇÕES VÁLIDAS NESTE MOMENTO\n");
            for(int i=0;i<options.length();i++){
                JSONObject o=options.optJSONObject(i); if(o==null)continue;
                b.append("• ").append(o.optString("name")).append("\n");
                if(o.optString("description").length()>0)b.append("  ").append(o.optString("description")).append("\n");
                if(o.optString("requirements").length()>0)b.append("  Requisitos: ").append(o.optString("requirements")).append("\n");
                if(o.optString("check").length()>0)b.append("  Teste: ").append(o.optString("check")).append("\n");
                if(o.optString("changes").length()>0)b.append("  Pode mudar: ").append(o.optString("changes")).append("\n");
                JSONArray params=o.optJSONArray("parameters");
                if(params!=null&&params.length()>0)b.append("  Você escolhe: ").append(join(params)).append("\n");
            }
            b.append("\n");
        }

        b.append("COMO USAR\n");
        b.append("Este guia não toma decisões por você. Feche-o e use “Resolver próxima decisão”. ");
        b.append("O botão executa o mesmo KingmakerRepository/KingmakerRulesEngine descrito acima; não há uma segunda regra escondida.");
        return b.toString();
    }

    private JSONArray configuration(JSONObject state, JSONObject control) throws Exception {
        JSONArray out=new JSONArray();

        int vacancies=repository.vacantLeadershipRoles();
        if(vacancies>0){
            JSONArray vacantNames=new JSONArray();
            for(JSONObject leader:repository.leaders())if(leader.optInt("vacant",0)==1)vacantNames.put(leader.optString("role_name",leader.optString("role_key")));
            JSONArray candidates=new JSONArray();
            for(JSONObject h:db.listActiveCharacters())candidates.put(h.optString("name")+" (PJ)");
            for(JSONObject c:repository.companions())if(c.optInt("available",1)==1)candidates.put(c.optString("name")+" (companheiro)");
            JSONObject x=new JSONObject().put("key","leadership").put("title","Há "+vacancies+" cargo(s) de liderança vago(s)")
                    .put("text","Cargos vagos podem alterar CDs/pressões do Reino. Preencha quando a ficção e a fase permitirem. Vagos: "+join(vacantNames)+".")
                    .put("choices",candidates);
            out.put(x);
        }

        List<JSONObject> settlements=repository.settlements();
        String capital=state.optString("capital_name","").trim();
        if(capital.length()==0){
            JSONArray choices=new JSONArray();for(JSONObject town:settlements)choices.put(town.optString("name"));
            out.put(new JSONObject().put("key","capital").put("title","Capital ainda não definida")
                    .put("text",settlements.isEmpty()?"Primeiro estabeleça um assentamento; depois escolha qual será a capital.":"Escolha um assentamento existente como capital quando o fluxo pedir.")
                    .put("choices",choices));
        }

        if(settlements.isEmpty()){
            out.put(new JSONObject().put("key","settlement").put("title","Nenhum assentamento registrado")
                    .put("text","Atividades de estrutura/comércio que exigem assentamento ficam indisponíveis até estabelecer um."));
        }

        if(control.optBoolean("anarchy")){
            out.put(new JSONObject().put("key","anarchy").put("title","Reino em Anarquia")
                    .put("text","Unrest "+state.optInt("unrest")+" restringe as atividades. O guia filtra as opções pelo mesmo canPerformActivity() do motor.")
                    .put("choices",new JSONArray().put("Acalmar Agitação")));
        }

        return out;
    }

    private JSONArray currentOptions(JSONObject state, JSONObject turn, int unrest) throws Exception {
        JSONArray out=new JSONArray();
        if(turn==null||turn.length()==0||!"active".equals(turn.optString("status"))){
            return out.put(option("start_turn","Iniciar próximo turno","Cria o turno e resolve a geração de Recursos usando o motor local.","","Recursos, turno ativo",new JSONArray()));
        }

        String phase=turn.optString("phase","UPKEEP").toUpperCase(Locale.US);
        if("UPKEEP".equals(phase)){
            JSONObject resource=parse(turn.optString("resource_roll_json","{}"));
            if(!resource.optBoolean("consumption_resolved")){
                return out.put(option("resolve_consumption","Resolver Consumo","Escolha como o Reino cobre o Consumo atual.","Fase de Manutenção","Alimento, RP ou Unrest",new JSONArray().put("Alimento").put("RP").put("Assumir Unrest")));
            }
            return out.put(option("advance_phase","Avançar para Comércio","O Consumo já foi resolvido.","Consumo resolvido","Fase atual",new JSONArray()));
        }

        if("COMMERCE".equals(phase)){
            String[] keys={"collect_taxes","improve_lifestyle","tap_treasury","trade_commodities","manage_trade_agreements","establish_trade_agreement","capital_investment"};
            addActivities(out,keys,unrest);
            return out;
        }

        if("ACTIVITY".equals(phase)){
            JSONArray cat=rules.activityGuideCatalog();
            for(int i=0;i<cat.length();i++){
                JSONObject a=cat.optJSONObject(i);if(a==null)continue;
                if(!"ACTIVITY".equalsIgnoreCase(a.optString("phase")))continue;
                if(!rules.canPerformActivity(a.optString("key"),unrest))continue;
                out.put(fromActivity(a));
            }
            return out;
        }

        if("EVENT".equals(phase)){
            List<JSONObject> pending=repository.events("pending");
            if(!pending.isEmpty()){
                for(JSONObject e:pending)out.put(option("resolve_event","Resolver evento: "+e.optString("title"),e.optString("notes"),"Evento pendente","Unrest/Ruin/recursos/estado conforme o evento",new JSONArray().put(e.optString("skill_key","perícia apropriada"))));
            }else{
                out.put(option("event_check","Teste simples de Evento","Faça o teste simples usando a CD de Evento atual ("+state.optInt("event_dc",16)+").","Fase de Evento","Cria evento ou reduz a próxima CD; evento ocorrido restaura CD 16",new JSONArray().put("resultado do d20")));
            }
            return out;
        }

        return out.put(option("advance_phase","Avançar fase","Nenhuma decisão adicional foi identificada pelo estado atual.","","Fase atual",new JSONArray()));
    }

    private void addActivities(JSONArray out,String[] keys,int unrest)throws Exception{
        for(String key:keys){
            if(!rules.canPerformActivity(key,unrest))continue;
            JSONObject a=rules.activityGuide(key);
            if(a!=null&&a.length()>0)out.put(fromActivity(a));
        }
    }

    private JSONObject fromActivity(JSONObject a)throws Exception{
        String skill=a.optString("skill");
        String check=skill.length()==0?"Sem teste genérico; o resolver específico conduz a atividade.":KingmakerRulesEngine.skillLabel(skill)+" / "+KingmakerRulesEngine.abilityLabel(a.optString("ability"))+" contra a CD aplicável";
        return option(a.optString("key"),a.optString("name"),a.optString("description"),a.optString("requirements"),a.optString("changes"),a.optJSONArray("parameters")==null?new JSONArray():a.optJSONArray("parameters"))
                .put("check",check).put("resolver","KingmakerRepository.resolveKingdomActivity");
    }

    private static JSONObject option(String key,String name,String description,String requirements,String changes,JSONArray parameters)throws Exception{
        return new JSONObject().put("key",key).put("name",name).put("description",description==null?"":description)
                .put("requirements",requirements==null?"":requirements).put("changes",changes==null?"":changes)
                .put("parameters",parameters==null?new JSONArray():parameters);
    }

    private static JSONObject parse(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static String join(JSONArray a){if(a==null||a.length()==0)return "nenhuma";StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){String v=a.optString(i);if(v.length()==0)continue;if(b.length()>0)b.append(", ");b.append(v);}return b.length()==0?"nenhuma":b.toString();}
}
