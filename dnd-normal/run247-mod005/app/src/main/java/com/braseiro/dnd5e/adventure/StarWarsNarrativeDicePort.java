package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Java port of the exact dice-face tables from swrpg-online/dice/src/diceFaces.ts (MIT).
 * https://github.com/swrpg-online/dice - preserved license under docs/THIRD_PARTY_NOTICES.md
 * Faces are one-based, as in the original library. Requires player-supplied results.
 */
public final class StarWarsNarrativeDicePort {
    private StarWarsNarrativeDicePort() {}
    // Per-face vectors: successes, failures, advantages, threats, triumphs, despairs, light, dark.
    private static final int[][] BOOST = {{},{},{},{1,0,0,0,0,0,0,0},{1,0,1,0,0,0,0,0},{0,0,2,0,0,0,0,0},{0,0,1,0,0,0,0,0}};
    private static final int[][] SETBACK = {{},{},{},{0,1,0,0,0,0,0,0},{0,1,0,0,0,0,0,0},{0,0,0,1,0,0,0,0},{0,0,0,1,0,0,0,0}};
    private static final int[][] ABILITY = {{},{},{1,0,0,0,0,0,0,0},{1,0,0,0,0,0,0,0},{2,0,0,0,0,0,0,0},{0,0,1,0,0,0,0,0},{0,0,1,0,0,0,0,0},{1,0,1,0,0,0,0,0},{0,0,2,0,0,0,0,0}};
    private static final int[][] DIFFICULTY = {{},{},{0,1,0,0,0,0,0,0},{0,2,0,0,0,0,0,0},{0,0,0,1,0,0,0,0},{0,0,0,1,0,0,0,0},{0,0,0,1,0,0,0,0},{0,0,0,2,0,0,0,0},{0,1,0,1,0,0,0,0}};
    private static final int[][] PROFICIENCY = {{},{},{1,0,0,0,0,0,0,0},{1,0,0,0,0,0,0,0},{2,0,0,0,0,0,0,0},{2,0,0,0,0,0,0,0},{0,0,1,0,0,0,0,0},{1,0,1,0,0,0,0,0},{1,0,1,0,0,0,0},{1,0,1,0,0,0,0},{0,0,2,0,0,0,0,0},{0,0,2,0,0,0,0,0},{0,0,0,0,1,0,0,0}};
    private static final int[][] CHALLENGE = {{},{},{0,1,0,0,0,0,0,0},{0,1,0,0,0,0,0,0},{0,2,0,0,0,0,0,0},{0,2,0,0,0,0,0,0},{0,0,0,1,0,0,0,0},{0,0,0,1,0,0,0,0},{0,1,0,1,0,0,0,0},{0,1,0,1,0,0,0,0},{0,0,0,2,0,0,0,0},{0,0,0,2,0,0,0,0},{0,0,0,0,0,1,0,0}};
    private static final int[][] FORCE = {{},{0,0,0,0,0,0,0,1},{0,0,0,0,0,0,0,1},{0,0,0,0,0,0,0,1},{0,0,0,0,0,0,0,1},{0,0,0,0,0,0,0,1},{0,0,0,0,0,0,0,1},{0,0,0,0,0,0,0,2},{0,0,0,0,0,0,1,0},{0,0,0,0,0,0,1,0},{0,0,0,0,0,0,2,0},{0,0,0,0,0,0,2,0},{0,0,0,0,0,0,2,0}};
    private static int[][] faces(String name) {
        if("boost".equals(name))return BOOST; if("setback".equals(name))return SETBACK;
        if("ability".equals(name))return ABILITY; if("difficulty".equals(name))return DIFFICULTY;
        if("proficiency".equals(name))return PROFICIENCY; if("challenge".equals(name))return CHALLENGE;
        if("force".equals(name))return FORCE; return null;
    }
    public static JSONObject resolve(JSONObject spec, JSONObject submission) {
        JSONObject pool = spec == null ? null : spec.optJSONObject("dice_pool");
        JSONArray results = submission == null ? null : submission.optJSONArray("dice");
        if(pool == null || results == null) return error("Pool e faces de dados são obrigatórios.");
        String[] names={"ability","proficiency","boost","difficulty","challenge","setback","force"};
        int expected=0;
        for(String name:names){int count=pool.optInt(name,0); if(count<0||count>20)return error("Quantidade inválida de "+name);expected+=count;}
        if(expected<1||expected>60||expected!=results.length())return error("Quantidade de dados informada difere do pool definido.");
        int[] totals=new int[8];int[] actual=new int[names.length];
        for(int i=0;i<results.length();i++){
            JSONObject item=results.optJSONObject(i);if(item==null)return error("Face narrativa inválida.");
            String kind=item.optString("die",""); int face=item.optInt("face",0);int pos=-1;
            for(int j=0;j<names.length;j++)if(names[j].equals(kind)){pos=j;break;}
            int[][] table=faces(kind);
            if(pos<0||table==null||face<1||face>=table.length)return error("Tipo ou face narrativa inválida: "+kind+"/"+face);
            actual[pos]++;
            for(int j=0;j<8;j++)totals[j]+= j<table[face].length?table[face][j]:0;
        }
        for(int j=0;j<names.length;j++)if(actual[j]!=pool.optInt(names[j],0))return error("Tipo de dado não corresponde ao pool: "+names[j]);
        if ("force".equals(spec.optString("check_kind", ""))) {
            for(int j=0;j<names.length-1;j++)if(pool.optInt(names[j],0)>0)return error("Teste de Força não mistura dados narrativos normais.");
            JSONObject outcome=LoreweaverOutcomePort.create("force","Dados da Força",false,false,false,0,null);
            return new JSONObject().put("resolved",true).put("force_pool",true)
                .put("light_side",totals[6]).put("dark_side",totals[7])
                .put("outcome",outcome);
        }
        if(pool.optInt("force",0)>0)return error("Dados da Força exigem check_kind=force; não podem ser cancelados por sucessos narrativos.");
        int netSuccess=totals[0]+totals[4]-totals[1]-totals[5];
        int netAdvantage=totals[2]-totals[3];
        JSONObject outcome=new JSONObject();
        outcome.put("resolved",true).put("success",netSuccess>0).put("net_success",netSuccess)
          .put("net_advantage",netAdvantage).put("triumph",totals[4]).put("despair",totals[5])
          .put("light_side",totals[6]).put("dark_side",totals[7]);
        outcome.put("narrative_hint", netSuccess>0 ? (netAdvantage>0?"Sucesso com vantagem":netAdvantage<0?"Sucesso com ameaça":"Sucesso")
                  :(netAdvantage>0?"Fracasso com vantagem":netAdvantage<0?"Fracasso com ameaça":"Fracasso"));
        outcome.put("outcome",LoreweaverOutcomePort.create(netSuccess>0?"success":"failure",netSuccess>0?"Sucesso":"Fracasso",
          netSuccess>0,totals[4]>0,totals[5]>0,netSuccess,Integer.valueOf(netSuccess)));
        return outcome;
    }
    private static JSONObject error(String message){return new JSONObject().put("resolved",false).put("error",message);}
}
