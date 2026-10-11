package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/** Actual persistent free-play module, not the GitHub self-test's synthetic Faerun map. */
public final class UniversalLiveSession {
    public static final String MODULE_ID="braseiro-universal-live-v1";
    public static final long PARTY_ID=909041L;
    private UniversalLiveSession(){}
    public static AdventureModule module(){
        JSONObject scene=new JSONObject().put("id","live").put("title","Mesa Universal")
            .put("description","O Mestre estabelece o cenário de acordo com o perfil escolhido.")
            .put("gm_directives","Nenhuma regra de um sistema é aplicada em outro.")
            .put("location_id","live-world");
        return AdventureModule.fromJson(new JSONObject().put("id",MODULE_ID)
            .put("title","Braseiro Universal (mesa persistente)").put("version","1.0.0")
            .put("root_node_id","live").put("nodes",new JSONArray().put(scene))
            .put("metadata",new JSONObject().put("braseiro_universal",true)));
    }
    public static String prompt(String systemId, String characterId, JSONObject state, String playerAction){
        String recent="";
        JSONArray scenes=state==null?null:state.optJSONArray("scenes");
        if(scenes!=null){int start=Math.max(0,scenes.length()-5);
            for(int i=start;i<scenes.length();i++){JSONObject scene=scenes.optJSONObject(i);
                if(scene!=null)recent+="\n[Mestre] "+clip(scene.optString("narration"),700);}}
        String resolved="";
        JSONArray receipts=state==null?null:state.optJSONArray("history");
        if(receipts!=null){int start=Math.max(0,receipts.length()-5);
            for(int i=start;i<receipts.length();i++){JSONObject receipt=receipts.optJSONObject(i);
                if(receipt!=null)resolved+="\n[Resultado confirmado] "+clip(receipt.toString(),550);}}
        String facts="";
        JSONArray worldFacts=state==null?null:state.optJSONArray("world_facts");
        if(worldFacts!=null){int start=Math.max(0,worldFacts.length()-12);
            for(int i=start;i<worldFacts.length();i++){JSONObject fact=worldFacts.optJSONObject(i);
                if(fact!=null)facts+="\n- "+clip(fact.optString("text"),350);}}
        int elapsed=state==null?0:state.optInt("elapsed_minutes",0);
        JSONObject entities=state==null?null:state.optJSONObject("entities");
        JSONObject hero=entities==null?null:entities.optJSONObject(characterId);
        String sheet=hero==null?"{}":clip(hero.toString(),2500);
        return "Você é o Mestre do Braseiro Universal. Sistema selecionado: "+systemId+". "
            +"Jamais misture mecânicas de outros RPGs. Narre em PT-BR com personalidade e consequências. "
            +"Se desconhecer uma regra, prefira um teste assistido com resultado manual; não invente regra oficial. "
            +"O aplicativo é a autoridade sobre ficha/recursos e você NÃO PODE alegar que alterou valores. "
            +"Responda APENAS JSON: {\"narration\":\"texto narrativo\", \"check\":{\"required\":false}} "
            +"ou, quando houver teste, {\"narration\":\"texto narrativo\",\"check\":{\"required\":true,"
            +"\"situation\":\"descrição\",\"on_success\":\"consequência\",\"on_failure\":\"consequência\","
            +"\"spec\":{...},\"on_success_effects\":[],\"on_failure_effects\":[]}}. "
            +"Efeitos opcionais são SUGESTÕES e nunca são aplicados sem confirmação do jogador. "
            +"Tipos: resource {kind,key,delta}, item {kind,item_id,name,delta}, "
            +"time {kind,minutes}, world_fact {kind,fact_id,text}. "
            +"Use nomes de recurso EXATOS da ficha, nunca invente uma chave, no máximo seis por resultado. "
            +"Se não souber a consequência mecânica, forneça arrays vazios. D&D spec={\"dc\":12,\"modifier\":2}; "
            +"Forbidden Lands spec={\"dice_pool\":{\"attribute\":2,\"skill\":1,\"gear\":0}}; "
            +"Star Wars spec={\"dice_pool\":{\"ability\":1,\"difficulty\":1}}. "
            +"Exemplos não são valores obrigatórios. Use o pool adequado à ficha e situação; "
            +"se impossível determinar, não invente uma rolagem. Um único teste por resposta. "
            +"A ação atual vem do jogador. Ficha: "+sheet+". "
            +"Tempo decorrido confirmado (minutos): "+elapsed+". Fatos persistentes do mundo: "+facts+". "
            +"Histórico recente: "+recent+"\nTestes resolvidos e consequências já confirmadas: "+resolved
            +"\n[Ação do jogador] "+clip(playerAction,1500);
    }
    private static String clip(String s,int max){return s==null?"":s.length()>max?s.substring(0,max):s;}
}
