package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Simula centenas de cenas, serializações e retomadas sem depender de busca textual no código. */
public final class LongCampaignStateSelfTest {
    private static int checks=0;
    public static void main(String[] args)throws Exception{
        JSONArray history=new JSONArray(),previous=new JSONArray();
        int scenesWithOptions=0,scenesWithoutOptions=0,totalOptions=0;
        for(int beat=1;beat<=500;beat++){
            JSONObject scene=new JSONObject()
                    .put("location","Distrito "+(beat%23))
                    .put("present_npcs_json",new JSONArray().put(new JSONObject().put("name","NPC "+beat)).toString())
                    .put("clues_json",new JSONArray().put("pista "+beat).toString())
                    .put("threats_json",new JSONArray().put("ameaça "+(beat%17)).toString());
            String narration="Cena "+beat+": NPC "+beat+" aponta a pista "+beat+" enquanto a ameaça "+(beat%17)+" altera o Distrito "+(beat%23)+".";
            JSONArray options=SceneOptionDiversifier.diversify(new JSONArray(),history,narration,"",scene,beat);
            check(options.length()>=0&&options.length()<=6,"cena "+beat+" excedeu política 0–6");
            if(options.length()==0)scenesWithoutOptions++;else scenesWithOptions++;
            totalOptions+=options.length();
            for(int i=0;i<options.length();i++){
                JSONObject option=options.optJSONObject(i);
                check(option!=null,"cena "+beat+" retornou opção inválida");
                String action=option.optString("action",option.optString("description",option.optString("label","")));
                check(SceneOptionDiversifier.isGroundedInScene(action,narration,scene),"cena "+beat+" retornou sugestão sem âncora real");
            }
            if(previous.length()>0&&options.length()>0&&previous.length()==options.length())
                check(!previous.toString().equals(options.toString()),"cena "+beat+" repetiu literalmente o conjunto anterior");
            history=SceneOptionDiversifier.appendHistory(history,options);
            check(history.length()<=60,"histórico cresceu sem limite na cena "+beat);
            if(beat%37==0)history=SceneOptionDiversifier.parseHistory(history.toString());
            previous=new JSONArray(options.toString());
        }
        check(scenesWithOptions>0,"campanha longa nunca produziu uma sugestão ancorada");
        check(totalOptions>0,"campanha longa perdeu todas as sugestões");
        check(history.length()>0&&history.length()<=60,"retomada final preserva janela limitada");

        JSONObject otherScene=new JSONObject().put("location","Porto de outra campanha")
                .put("present_npcs_json",new JSONArray().put(new JSONObject().put("name","Mira")).toString())
                .put("clues_json",new JSONArray().put("chave de bronze").toString());
        String otherNarration="Mira fecha o portão do Porto de outra campanha e ergue uma chave de bronze, pedindo uma resposta.";
        JSONArray isolated=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),otherNarration,"",otherScene,1);
        check(isolated.length()>=0&&isolated.length()<=6,"nova campanha violou política 0–6");
        for(int i=0;i<isolated.length();i++){
            JSONObject option=isolated.optJSONObject(i);
            String action=option==null?"":option.optString("action",option.optString("description",option.optString("label","")));
            check(SceneOptionDiversifier.isGroundedInScene(action,otherNarration,otherScene),"nova campanha recebeu sugestão sem âncora");
        }
        check(!isolated.toString().contains("NPC 500")&&!isolated.toString().contains("Distrito 17"),"histórico de outra campanha contaminou a nova campanha");
        System.out.println("OK: 500 cenas, política 0–6 ancorada, retomadas seriadas e isolamento entre campanhas. Cenas com opções="+scenesWithOptions+", sem opções="+scenesWithoutOptions+".");
    }
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
