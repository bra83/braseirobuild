package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Planejador determinístico e persistente para facções e NPCs.
 *
 * As decisões são derivadas do estado real da campanha (missões, relógios,
 * eventos, facções e cena) e nunca aplicam dano, ouro, XP ou ficha por conta
 * própria. Consequências mecânicas continuam obrigatoriamente no fluxo de
 * regras recuperadas, rolagem local, revisão e transação.
 */
public final class WorldAutonomyPlanner {
    private final AppDb db;

    public WorldAutonomyPlanner(){this(null);}
    public WorldAutonomyPlanner(AppDb database){db=database;}

    /** Decide se a facção precisa abandonar o ciclo anterior e produzir um novo plano. */
    public static boolean needsNewFactionPlan(String currentPlan,int stepCount,int progress,boolean cycle){
        return currentPlan==null||currentPlan.trim().length()==0||stepCount<=0||(!cycle&&Math.max(0,progress)>=stepCount);
    }

    public JSONObject factionPlan(JSONObject faction,int day){return factionPlan(faction,day,new JSONObject());}

    public JSONObject factionPlan(JSONObject faction,int day,JSONObject ruleBundle){
        JSONObject out=new JSONObject();
        try{
            String name=clean(faction.optString("name","A facção"),80);
            String agenda=clean(faction.optString("agenda","")+" "+faction.optString("objectives_json")+" "+faction.optString("success_conditions_json")+" "+faction.optString("failure_conditions_json"),520);
            String location=clean(faction.optString("location",""),120);
            String resources=clean(faction.optString("resources_json",faction.optString("resources","")),320);
            String strategicContext=clean(faction.optString("territories_json")+" "+faction.optString("relations_json")+" "+faction.optString("allies_json")+" "+faction.optString("enemies_json")+" "+faction.optString("information_json")+" "+faction.optString("costs_json")+" "+faction.optString("risks_json")+" "+faction.optString("memory_json"),620);
            String current=clean(faction.optString("current_plan",""),220);
            int relationship=clamp(faction.optInt("relationship",0),-100,100);
            WorldSignal signal=bestSignal(name,agenda,location,faction.optLong("id",0));
            WorldRuleGuidance guidance=new WorldRuleGuidance(ruleBundle);
            String category=guidance.recommendedCategory(planCategory(agenda+" "+current+" "+resources+" "+strategicContext+" "+signal.text));
            String goal=goalFor(agenda,relationship,signal);
            String ruleArchetype=guidance.actionArchetype("");if(ruleArchetype.length()>0)goal=goal+"; método: "+ruleArchetype;
            String plan="Plano autônomo de "+category+": "+goal;
            JSONArray steps=stepsFor(category,name,goal,location,resources,relationship,Math.max(1,day),signal);
            double interval=guidance.recommendedIntervalHours(dynamicInterval(category,signal.pressure));
            JSONObject basis=new JSONObject().put("source","estado_persistente")
                    .put("faction_id",faction.optLong("id",0)).put("relationship",relationship)
                    .put("agenda",agenda).put("resources",resources).put("strategic_context",strategicContext)
                    .put("target_kind",signal.kind).put("target_id",signal.id)
                    .put("target",signal.text).put("pressure",signal.pressure)
                    .put("reason",signal.reason).put("rule_guidance",guidance.decisionBasis());
            out.put("current_plan",plan).put("plan_steps_json",steps)
                    .put("plan_interval_hours",interval).put("plan_cycle",0)
                    .put("generated_day",Math.max(1,day)).put("plan_category",category)
                    .put("decision_basis",basis).put("decision_rule_basis",guidance.decisionBasis()).put("decision_source",guidance.hasExecutableProcedure()?"manual_rule_guidance":"estado_persistente").put("target_ref",signal.toJson());
        }catch(Exception error){AppLog.w("WorldAutonomyPlanner","Falha ao montar plano autônomo seguro.",error);}
        return out;
    }

    public JSONObject npcAction(JSONObject state,int day){return npcAction(state,day,new JSONObject());}

    public JSONObject npcAction(JSONObject state,int day,JSONObject ruleBundle){
        JSONObject out=new JSONObject();
        try{
            String name=clean(state.optString("npc_name",state.optString("name","O NPC")),80);
            String location=clean(state.optString("current_location",""),120);
            String emotion=clean(state.optString("emotion",""),80);
            String beliefs=clean(state.optString("beliefs_json",state.optString("beliefs",""))+" "+state.optString("knowledge_json")+" "+state.optString("goals_json")+" "+state.optString("personality_json")+" "+state.optString("recent_events_json"),420);
            String faction=clean(state.optString("faction",""),100);
            String declared=clean(state.optString("next_action",""),220);
            int urgency=clamp(state.optInt("urgency",0),0,100),trust=clamp(state.optInt("trust",0),-100,100);
            int fear=clamp(state.optInt("fear",0),0,100),suspicion=clamp(state.optInt("suspicion",0),0,100);
            WorldSignal signal=bestSignal(faction.length()>0?faction:name,beliefs,location,0);
            JSONObject factionState=findFaction(faction);
            String factionPlan=clean(factionState.optString("current_plan"),220);
            WorldRuleGuidance guidance=new WorldRuleGuidance(ruleBundle);
            String action;
            if(declared.length()>0)action=pastTense(declared);
            else if(urgency>=85)action="tomou uma iniciativa imediata para "+signalObjective(signal,"proteger seu interesse mais urgente");
            else if(fear>=65||contains(emotion,"medo","temor","ansiedade"))action="procurou proteção, informação e uma rota de retirada diante de "+signalObjective(signal,"uma ameaça percebida");
            else if(suspicion>=65)action="testou a lealdade de um contato e investigou "+signalObjective(signal,"uma possível traição");
            else if(trust>=55)action="procurou um aliado e compartilhou informação sobre "+signalObjective(signal,"seu plano atual");
            else if(contains(emotion,"raiva","hostil","ressent"))action="pressionou um contato e tornou pública sua oposição a "+signalObjective(signal,"seu rival");
            else if(factionPlan.length()>0)action="executou uma etapa verificável do plano de "+faction+": "+factionPlan;
            else if(faction.length()>0)action="reuniu informação e recursos em favor de "+faction+" para "+signalObjective(signal,"seu objetivo atual");
            else if(beliefs.length()>0)action="agiu de acordo com suas convicções diante de "+signalObjective(signal,"o conflito atual");
            else action="buscou uma testemunha, recurso ou notícia ligada a "+signalObjective(signal,"o conflito atual");

            if(guidance.hasExecutableProcedure())action=guidance.actionArchetype(action)+" em resposta a "+signalObjective(signal,"seu objetivo atual");
            String summary=name+" "+action+locationSuffix(location)+".";
            JSONObject consequence=safeNarrativeConsequence(name,summary,urgency,day,signal);
            JSONObject basis=new JSONObject().put("source","estado_persistente")
                    .put("urgency",urgency).put("trust",trust).put("fear",fear)
                    .put("suspicion",suspicion).put("faction",faction).put("knowledge",state.optString("knowledge_json")).put("goals",state.optString("goals_json")).put("personality",state.optString("personality_json")).put("resources",state.optString("resources_json"))
                    .put("faction_plan",factionPlan).put("target_kind",signal.kind)
                    .put("target_id",signal.id).put("target",signal.text)
                    .put("pressure",signal.pressure).put("reason",signal.reason).put("rule_guidance",guidance.decisionBasis());
            out.put("action",action).put("location",location).put("summary",summary)
                    .put("consequence_json",consequence).put("generated_day",Math.max(1,day))
                    .put("rule_query","NPC agenda reação moral facção "+action+" "+location+" "+signal.text)
                    .put("decision_basis",basis).put("decision_rule_basis",guidance.decisionBasis()).put("decision_source",guidance.hasExecutableProcedure()?"manual_rule_guidance":"estado_persistente").put("target_ref",signal.toJson());
        }catch(Exception error){AppLog.w("WorldAutonomyPlanner","Falha ao montar ação autônoma segura.",error);}
        return out;
    }

    private JSONObject safeNarrativeConsequence(String name,String summary,int urgency,int day,WorldSignal signal)throws Exception{
        JSONObject consequence=new JSONObject();
        JSONArray knowledge=new JSONArray().put(new JSONObject().put("subject",name).put("content",summary)
                .put("truth_status","verdade").put("visibility","mestre").put("known_by_json",new JSONArray())
                .put("reveal_condition","Quando a ação deixar sinais ou chegar ao conhecimento da companhia.")
                .put("source","Agenda autônoma de NPC"));
        consequence.put("knowledge_updates",knowledge);
        if(urgency>=85||signal.pressure>=80){
            String target=signal.text.length()>0?signal.text:"a iniciativa de "+name;
            JSONObject eventConsequence=new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject()
                    .put("title","Consequência da iniciativa de "+name)
                    .put("body","A iniciativa de "+name+" ligada a "+target+" produziu uma reação observável no mundo.")
                    .put("kind","Evento")));
            consequence.put("world_events",new JSONArray().put(new JSONObject()
                    .put("title","Reação à iniciativa de "+name)
                    .put("description","O mundo responde à ação de "+name+" relacionada a "+target+".")
                    .put("due_day",Math.max(1,day)+1).put("consequence_json",eventConsequence)));
        }
        return consequence;
    }

    private WorldSignal bestSignal(String actor,String agenda,String location,long factionId){
        WorldSignal best=new WorldSignal("context",0,clean(agenda,180),20,"agenda persistente");
        if(db==null)return best;
        try{
            for(JSONObject event:db.listWorldEventsForMaster(80)){
                int pressure="awaiting_review".equals(event.optString("status"))?100:duePressure(event.optInt("due_day",0));
                int relevance=relevance(actor+" "+agenda+" "+location,event.optString("title")+" "+event.optString("description"));
                WorldSignal s=new WorldSignal("event",event.optLong("id"),clean(event.optString("title")+": "+event.optString("description"),220),Math.min(100,pressure+relevance),"evento pendente ou próximo");
                if(s.betterThan(best))best=s;
            }
            for(JSONObject clock:db.listWorldClocks(true,80)){
                int max=Math.max(1,clock.optInt("max_value",4)),current=Math.max(0,clock.optInt("current_value",0));
                int pressure=clamp((int)Math.round(current*100d/max),0,100);
                int relevance=relevance(actor+" "+agenda+" "+location,clock.optString("name")+" "+clock.optString("consequence"));
                WorldSignal s=new WorldSignal("clock",clock.optLong("id"),clean(clock.optString("name")+": "+clock.optString("consequence"),220),Math.min(100,pressure+relevance),"relógio ativo "+current+"/"+max);
                if(s.betterThan(best))best=s;
            }
            for(JSONObject quest:db.listQuests("active",100)){
                if(factionId>0&&quest.optLong("faction_id",0)>0&&quest.optLong("faction_id")!=factionId)continue;
                String text=quest.optString("title")+": "+quest.optString("objective")+" "+quest.optString("stakes");
                int relevance=relevance(actor+" "+agenda+" "+location,text)+(quest.optLong("faction_id",0)==factionId&&factionId>0?35:0);
                WorldSignal s=new WorldSignal("quest",quest.optLong("id"),clean(text,220),clamp(35+relevance,0,90),"missão ativa relacionada");
                if(s.betterThan(best))best=s;
            }
        }catch(Exception error){AppLog.w("WorldAutonomyPlanner","Contexto do mundo indisponível; usando agenda local.",error);}
        return best;
    }

    private JSONObject findFaction(String name){
        if(db==null||name==null||name.trim().length()==0)return new JSONObject();
        try{for(JSONObject row:db.listFactions(200))if(norm(row.optString("name")).equals(norm(name)))return row;}catch(Exception ignored){AppLog.ignored("WorldAutonomyPlanner","Facção não localizada; a ação seguirá sem o plano dela.",ignored);}return new JSONObject();
    }

    private int duePressure(int dueDay){
        if(dueDay<=0||db==null)return 35;
        int today=1;try{today=Math.max(1,db.getSceneState().optInt("day",1));}catch(Exception ignored){AppLog.ignored("WorldAutonomyPlanner","Dia da cena indisponível.",ignored);}
        int remaining=dueDay-today;if(remaining<=0)return 95;if(remaining==1)return 80;if(remaining<=3)return 60;return 35;
    }

    private static int relevance(String context,String target){
        String a=norm(context),b=norm(target);if(a.length()==0||b.length()==0)return 0;int hits=0,total=0;
        for(String term:b.split("\\s+")){if(term.length()<4)continue;total++;if(a.contains(term))hits++;}
        return total==0?0:Math.min(35,(int)Math.round(35d*hits/Math.min(total,8)));
    }

    private static String goalFor(String agenda,int relationship,WorldSignal signal){
        if(signal.text.length()>0&&signal.pressure>=45)return "intervir em "+signal.text;
        if(agenda.length()>0)return agenda;
        return relationship<-25?"reduzir a influência da companhia":relationship>25?"aproximar-se da companhia":"ampliar sua posição na região";
    }

    private static String signalObjective(WorldSignal signal,String fallback){return signal!=null&&signal.text.length()>0?signal.text:fallback;}
    private static double dynamicInterval(String category,int pressure){
        double base="guerra".equals(category)?18d:"intriga".equals(category)?24d:"comercio".equals(category)?30d:36d;
        if(pressure>=85)base*=0.45d;else if(pressure>=65)base*=0.65d;else if(pressure<=25)base*=1.25d;
        return Math.max(6d,Math.min(72d,Math.round(base*2d)/2d));
    }

    private static JSONArray stepsFor(String category,String name,String goal,String location,String resources,int relationship,int day,WorldSignal signal)throws Exception{
        JSONArray out=new JSONArray();String targetRef=signal.kind+":"+signal.id;
        if("guerra".equals(category)){
            out.put(step(name,"Reconhecimento","Mapear posições, rotas e vulnerabilidades ligadas a "+goal,location,resources,0,day,false,targetRef));
            out.put(step(name,"Mobilização","Mover agentes, suprimentos e aliados para pressionar "+goal,location,resources,relationship<0?-2:0,day,false,targetRef));
            out.put(step(name,"Ação de pressão","Executar uma demonstração pública, bloqueio, incursão ou ultimato relacionado a "+goal,location,resources,relationship<0?-4:1,day,true,targetRef));
            out.put(step(name,"Reavaliação","Medir perdas, reação pública e resposta dos rivais antes do próximo movimento em "+goal,location,resources,0,day,false,targetRef));
        }else if("comercio".equals(category)){
            out.put(step(name,"Levantamento de mercado","Identificar escassez, compradores e concorrentes ligados a "+goal,location,resources,0,day,false,targetRef));
            out.put(step(name,"Negociação","Firmar compromissos, preços e rotas para avançar "+goal,location,resources,relationship>0?1:0,day,false,targetRef));
            out.put(step(name,"Movimentação de recursos","Transferir bens, pessoal ou crédito para executar "+goal,location,resources,relationship<0?-1:1,day,true,targetRef));
            out.put(step(name,"Consolidação","Registrar lucros, perdas e novos riscos em "+goal,location,resources,0,day,false,targetRef));
        }else if("religiao".equals(category)){
            out.put(step(name,"Proclamação","Espalhar uma mensagem ou doutrina ligada a "+goal,location,resources,0,day,false,targetRef));
            out.put(step(name,"Reunião de fiéis","Reunir apoio, ofertas e testemunhos para "+goal,location,resources,relationship>0?1:0,day,false,targetRef));
            out.put(step(name,"Cerimônia pública","Realizar um rito ou ato público que torne "+goal+" visível",location,resources,relationship<0?-2:1,day,true,targetRef));
            out.put(step(name,"Leitura dos sinais","Interpretar reações, dissidências e oportunidades em "+goal,location,resources,0,day,false,targetRef));
        }else if("pesquisa".equals(category)){
            out.put(step(name,"Coleta de fontes","Reunir mapas, relatos, componentes ou especialistas para "+goal,location,resources,0,day,false,targetRef));
            out.put(step(name,"Investigação","Comparar evidências e testar hipóteses relacionadas a "+goal,location,resources,0,day,false,targetRef));
            out.put(step(name,"Experimento controlado","Produzir uma demonstração limitada e reversível de "+goal,location,resources,0,day,true,targetRef));
            out.put(step(name,"Contenção e revisão","Registrar riscos e ajustar o método em "+goal,location,resources,0,day,false,targetRef));
        }else{
            out.put(step(name,"Observar","Reunir sinais, contatos e obstáculos sobre "+goal,location,resources,0,day,false,targetRef));
            out.put(step(name,"Articular","Reposicionar contatos e recursos para "+goal,location,resources,relationship<0?-1:0,day,false,targetRef));
            out.put(step(name,"Agir","Executar uma ação pública e reversível para "+goal,location,resources,relationship<0?-2:1,day,true,targetRef));
            out.put(step(name,"Reavaliar","Medir a reação do mundo e atualizar prioridades em "+goal,location,resources,0,day,false,targetRef));
        }
        return out;
    }

    private static JSONObject step(String faction,String title,String summary,String location,String resources,int relationshipDelta,int day,boolean createsEvent,String targetRef)throws Exception{
        String full=summary+locationSuffix(location)+".";JSONObject consequence=new JSONObject();
        consequence.put("knowledge_updates",new JSONArray().put(new JSONObject().put("subject",faction).put("content",full)
                .put("truth_status","verdade").put("visibility","mestre").put("known_by_json",new JSONArray())
                .put("reveal_condition","Quando personagens presenciarem sinais, ouvirem rumores ou investigarem a facção.")
                .put("source","Plano autônomo de facção")));
        if(createsEvent){
            JSONObject eventConsequence=new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject()
                    .put("title","Reação ao movimento de "+faction).put("body","Rivais, aliados e habitantes reagem ao movimento recente da facção.").put("kind","Evento")));
            consequence.put("world_events",new JSONArray().put(new JSONObject().put("title","Reação ao movimento de "+faction)
                    .put("description","O movimento da facção produz consequências visíveis.").put("due_day",Math.max(1,day)+1).put("consequence_json",eventConsequence)));
        }
        return new JSONObject().put("title",title).put("summary",full).put("location",location).put("resources",resources)
                .put("agenda",summary).put("relationship_delta",relationshipDelta).put("next_plan","")
                .put("rule_query",ruleQueryFor(title,summary)).put("target_ref",targetRef).put("consequence_json",consequence);
    }

    private static String planCategory(String value){String n=norm(value);if(contains(n,"guerra","atacar","invas","militar","conquista","cerco","hostil"))return "guerra";if(contains(n,"comerc","mercado","lucro","rota","caravana","guilda"))return "comercio";if(contains(n,"templo","relig","culto","fe","doutrina","sagrado"))return "religiao";if(contains(n,"pesquis","magia","arcano","experimento","biblioteca","segredo"))return "pesquisa";if(contains(n,"espiao","intriga","chantag","politic","nobre","coroa"))return "intriga";return "influencia";}
    private static String ruleQueryFor(String title,String summary){return title+" "+summary+" facção npc reação moral domínio atividade tempo livre custos eventos";}
    private static boolean contains(String source,String... values){String n=norm(source);for(String value:values)if(n.contains(norm(value)))return true;return false;}
    private static String pastTense(String action){String a=clean(action,220);String n=norm(a);if(n.startsWith("vai "))return a.substring(Math.min(4,a.length()));if(n.startsWith("ira "))return a.substring(Math.min(4,a.length()));if(n.startsWith("pretende "))return a.substring(Math.min(9,a.length()));return a;}
    private static String locationSuffix(String location){return location.length()==0?"":" em "+location;}
    private static String clean(String value,int max){String out=value==null?"":value.replaceAll("\\s+"," ").trim();return out.length()<=max?out:out.substring(0,max-1)+"…";}
    private static String norm(String value){if(value==null)return "";return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}

    private static final class WorldSignal {
        final String kind;final long id;final String text;final int pressure;final String reason;
        WorldSignal(String k,long i,String t,int p,String r){kind=k==null?"context":k;id=i;text=t==null?"":t;pressure=clamp(p,0,100);reason=r==null?"":r;}
        boolean betterThan(WorldSignal other){return other==null||pressure>other.pressure||(pressure==other.pressure&&text.length()>other.text.length());}
        JSONObject toJson(){try{return new JSONObject().put("kind",kind).put("id",id).put("text",text).put("pressure",pressure).put("reason",reason);}catch(Exception ignored){return new JSONObject();}}
    }
}
