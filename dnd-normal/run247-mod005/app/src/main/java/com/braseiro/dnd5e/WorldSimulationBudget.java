package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;

/** Orçamento fixo e previsível por ciclo; o excedente permanece pendente para o próximo avanço. */
public final class WorldSimulationBudget {
    public final int factions=8,npcs=12,activities=8,events=8,guilds=8,stepsPerFaction=4;
    public JSONArray trim(JSONArray in,int max){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();if(in!=null)for(int i=0;i<in.length()&&i<max;i++)out.put(in.opt(i));return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject metadata(int fTotal,int nTotal,int aTotal,int eTotal,int gTotal){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{
        o.put("factions",entry(fTotal,factions)).put("npcs",entry(nTotal,npcs)).put("activities",entry(aTotal,activities)).put("events",entry(eTotal,events)).put("guilds",entry(gTotal,guilds)).put("steps_per_faction",stepsPerFaction);
    }catch(Exception ignored){AppLog.ignored("WorldSimulationBudget","Falha tolerada registrada na consolidação 5.0.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private JSONObject entry(int total,int cap)throws Exception{/*BR_JSON_WRAP*/try{return new JSONObject().put("available",Math.max(0,total)).put("processed",Math.min(Math.max(0,total),cap)).put("deferred",Math.max(0,total-cap)).put("cap",cap);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
