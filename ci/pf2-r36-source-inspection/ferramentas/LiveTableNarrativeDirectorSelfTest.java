package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public final class LiveTableNarrativeDirectorSelfTest {
    private static int ok=0;
    private static void check(String name,boolean value){if(!value)throw new AssertionError(name);ok++;System.out.println("OK  "+name);}
    private static JSONObject party(int heroes,int subgroups,int queued)throws Exception{
        JSONObject p=new JSONObject();JSONArray hs=new JSONArray();for(int i=0;i<heroes;i++)hs.put(new JSONObject().put("name","Heroi "+(i+1)));p.put("heroes",hs);
        JSONArray sg=new JSONArray();for(int i=0;i<subgroups;i++)sg.put(new JSONObject().put("key","g"+i));p.put("party",new JSONObject().put("subgroups",sg));
        JSONArray qa=new JSONArray();for(int i=0;i<queued;i++)qa.put(new JSONObject().put("action_text","acao "+i));p.put("queued_actions",qa);return p;
    }
    private static JSONObject world(int quests,int factions,int clocks)throws Exception{
        JSONObject w=new JSONObject();JSONArray q=new JSONArray(),f=new JSONArray(),c=new JSONArray();for(int i=0;i<quests;i++)q.put(new JSONObject());for(int i=0;i<factions;i++)f.put(new JSONObject());for(int i=0;i<clocks;i++)c.put(new JSONObject());return w.put("quests",q).put("factions",f).put("clocks",c).put("events_due_or_upcoming",new JSONArray());
    }
    public static void main(String[] args)throws Exception{
        JSONObject empty=new JSONObject();JSONObject scene=new JSONObject().put("location","Portao");
        check("mundo aberto por faccoes",LiveTableNarrativeDirector.inferMode(empty,scene,party(4,0,0),world(3,3,2),"")==LiveTableNarrativeDirector.CampaignMode.OPEN_WORLD);
        check("aventura focada por objetivo",LiveTableNarrativeDirector.inferMode(empty,new JSONObject().put("objective","Investigar a mansao"),party(4,0,0),world(1,0,0),"")==LiveTableNarrativeDirector.CampaignMode.FOCUSED_ADVENTURE);
        check("prologo de elenco",LiveTableNarrativeDirector.choosePattern(empty,scene,party(4,0,0),world(0,0,0),new JSONObject(),"Inicio da campanha")==LiveTableNarrativeDirector.TablePattern.ENSEMBLE_PROLOGUE);
        check("corte paralelo",LiveTableNarrativeDirector.choosePattern(empty,scene,party(4,2,0),world(0,0,0),new JSONObject().put("checkpoint",new JSONObject().put("x",1)),"Enquanto isso")==LiveTableNarrativeDirector.TablePattern.PARALLEL_CROSSCUT);
        check("investigacao tatil",LiveTableNarrativeDirector.choosePattern(empty,scene,party(4,0,0),world(1,0,0),new JSONObject().put("checkpoint",new JSONObject().put("x",1)),"Examino a fechadura")==LiveTableNarrativeDirector.TablePattern.TACTILE_INVESTIGATION);
        String contract=LiveTableNarrativeDirector.contract(empty,scene,party(4,0,0),world(2,2,1),new JSONObject(),"Inicio da campanha");
        check("contrato abertura elenco",contract.contains("ABERTURA DE ELENCO"));
        check("contrato mundo aberto",contract.contains("MUNDO ABERTO"));
        check("contrato aventura focada",contract.contains("AVENTURA FOCADA"));
        check("contrato investigacao tatil",contract.contains("INVESTIGAÇÃO TÁTIL"));
        check("contrato dramatic irony",contract.contains("DRAMATIC IRONY"));
        JSONObject liveState=new JSONObject().put("mode","OPEN_WORLD").put("pattern","FACTION_SANDBOX")
            .put("threads",new JSONArray().put(new JSONObject().put("key","porto")))
            .put("scene_promises",new JSONArray().put(new JSONObject().put("key","sino")))
            .put("spotlight",new JSONObject().put("focus","Mira"));
        JSONObject env=new JSONObject().put("narrative_state",new JSONObject().put("live_table",liveState));
        JSONObject memory=LiveTableNarrativeDirector.record(new JSONObject(),"Sigo para o porto","O sino toca.",env,new JSONObject().put("location","Cais"));
        check("memoria live_table",memory.optJSONObject("live_table")!=null);
        check("threads persistidas",memory.optJSONObject("live_table").optJSONArray("threads").length()==1);
        check("promessas persistidas",memory.optJSONObject("live_table").optJSONArray("scene_promises").length()==1);
        check("historico de handoff",memory.optJSONObject("live_table").optJSONArray("handoff_history").length()==1);
        check("memory prompt",LiveTableNarrativeDirector.memoryPrompt(memory).contains("MEMÓRIA DE MESA VIVA"));
        check("qa ironia vazada",LiveTableNarrativeDirector.qualityProblem("Vocês não sabem, mas o duque já fugiu.",memory,scene,"").length()>0);
        check("qa emocao forçada",LiveTableNarrativeDirector.qualityProblem("Você sente medo e decide fugir.",memory,scene,"").length()>0);
        check("qa menu",LiveTableNarrativeDirector.qualityProblem("- Atacar\n- Fugir\n- Negociar",memory,scene,"").length()>0);
        check("qa cena concreta",LiveTableNarrativeDirector.qualityProblem("O sino do cais toca duas vezes. Mira fecha a mão sobre a carta; do outro lado da água, as lanternas da patrulha mudam de direção.",memory,scene,"Olho o cais").isEmpty());
        JSONObject ps=LiveTableNarrativeDirector.promptState(empty,scene,party(4,2,1),world(3,2,2),new JSONObject(memory.toString()).put("checkpoint",new JSONObject().put("x",1)),"Enquanto isso");
        check("prompt state pattern",ps.optString("pattern").equals("PARALLEL_CROSSCUT"));
        check("prompt state contagens",ps.optInt("heroes")==4&&ps.optInt("subgroups")==2&&ps.optInt("active_factions")==2);
        System.out.println("RESULTADO MESA VIVA 6.1.0: "+ok+"/"+ok);
    }
}
