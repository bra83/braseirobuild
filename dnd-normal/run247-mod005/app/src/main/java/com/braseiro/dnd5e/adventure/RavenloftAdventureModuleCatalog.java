package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RHW campaign arcs compiled into deterministic AdventureModules.
 * Source: 5e.tools RHW book-rhw.json blob 1e2f87f2c79d99ac9d989fe80d61bc1bfbec6cd5.
 * Campaign arcs remain separate from the source-bound short-adventure catalog.
 */
public final class RavenloftAdventureModuleCatalog {
    private RavenloftAdventureModuleCatalog() {}
    public static final String SOURCE_BOOK="Ravenloft: The Horrors Within";
    public static final String SOURCE_BLOB="1e2f87f2c79d99ac9d989fe80d61bc1bfbec6cd5";

    private static final class Spec {
        final String id,domain; final int page; final String[] stages;
        Spec(String id,String domain,int page,String...stages){this.id=id;this.domain=domain;this.page=page;this.stages=stages;}
    }

    private static final Spec[] SPECS=new Spec[]{
        new Spec("barovia","Barovia",47,"Chegada pelas Brumas e abrigo local","Pressão dos agentes de Strahd","Busca por conhecimento e relíquias","Confronto em Castle Ravenloft"),
        new Spec("borca","Borca",58,"Armadilha de um nobre manipulador","Missão de fuga, confronto ou infiltração","Envolvimento na rivalidade dos Darklords"),
        new Spec("darkon","Darkon",68,"Chegada a uma região fraturada de Darkon","Confronto com um pretendente ao poder","Expedição a Castle Avernus e destino de Azalin"),
        new Spec("dementlieu","Dementlieu",75,"Investigação de desaparecimentos em Port-a-Lucine","Infiltração do Grand Masquerade","Exposição da conspiração por trás dos desaparecimentos"),
        new Spec("falkovnia","Falkovnia",84,"Resgate de sobreviventes e contato com os Talons","Escolha entre proteção civil e repressão de Lekar","Defesa do Gate of Torches durante o cerco zumbi"),
        new Spec("har_akir","Har'Akir",94,"Exploração de um complexo soterrado e canopic jars","Missão para recuperar os supostos tesouros do faraó","Revelação do conflito em torno dos scrolls e do ka de Ankhtepot"),
        new Spec("hazlan","Hazlan",102,"Resgate de vítimas dos experimentos de Khrinitov","Coleta de recurso mágico no Mound of the Worm","Armadilha e confronto ligados ao plano de Eleni contra Hazlik"),
        new Spec("innsmouth","Innsmouth",112,"Naufrágio, sonhos e primeiro contato com cultos","Missões entre cultos e relíquias","Escalada de influência, Dark Gifts e Vaults","Confronto final no Cthulhu's Vault"),
        new Spec("kalakeri","Kalakeri",120,"Contato com a facção rebelde e resgate político","Busca pelo símbolo heroico no Forest of the Still","Escolha de lado no conflito pelo Sapphire Throne"),
        new Spec("kartakass","Kartakass",128,"Busca por Frantosh e viagem a Harmonia","Missão ligada a Harkon e Argent Falls","Confronto com a influência de Harkon em Skald"),
        new Spec("lamordia","Lamordia",135,"Escolta rumo a Schloss Mordenheim","Missão para recuperar o Unbreakable Heart","Descoberta do conflito entre Viktra e Elise","Escolha: apoiar Viktra","Escolha: apoiar Elise","Consequências no Schloss Mordenheim"),
        new Spec("mordent","Mordent",146,"Mistério dos espíritos mutilados de Mordentshire","Haunting no herbalist shop","Busca pelo Rod of Rastinon e manipulação de Godefroy"),
        new Spec("shadowlands","Shadowlands",154,"Auxílio ao Circle e recuperação de um forte","Pedido de Kateri Shadowborn e busca pelas Four Keys","Reforja da espada no Stone Circle","Confronto em Shadowborn Manor"),
        new Spec("sithicus","Sithicus",162,"Caçada às sombras para Azrael Dax","Missão de Inza por chamas cataclísmicas","Descoberta do destino de Caradoc e infiltração em Nedragaard Keep"),
        new Spec("tepest","Tepest",169,"Participação no Tithe e ruptura do sacrifício","Aliança com vítimas feéricas e investigação de Lorinda","Infiltração do Gurgyl","Consequências da queda ou retorno de Lorinda"),
        new Spec("valachan","Valachan",180,"Naufrágio e sobrevivência na selva","Chegada a Oselo ou Shuaran","Convite para a Trial of Hearts","Decisão final sobre Chakuna e o destino do domínio")
    };

    public static List<AdventureModule> all(){
        List<AdventureModule> out=new ArrayList<AdventureModule>();
        for(Spec s:SPECS)out.add(build(s));
        return out;
    }

    public static AdventureModule byId(String moduleId){
        if(moduleId==null)return null;
        for(AdventureModule m:all())if(m.getId().equalsIgnoreCase(moduleId.trim()))return m;
        return null;
    }

    public static JSONArray catalog(){
        JSONArray a=new JSONArray();
        for(Spec s:SPECS)try{a.put(new JSONObject().put("module_id",moduleId(s)).put("domain",s.domain).put("source_page",s.page).put("stages",s.stages.length).put("source_book",SOURCE_BOOK).put("runtime_tier","campaign_arc_runtime"));}catch(Exception ignored){}
        return a;
    }

    public static JSONObject coverage(){
        JSONObject o=new JSONObject();
        try{return o.put("source","RHW").put("campaign_arcs_expected",16).put("campaign_arc_modules",SPECS.length)
                .put("campaign_arc_complete",SPECS.length==16).put("runtime_tier","campaign_arc_runtime")
                .put("short_adventures_expected",17).put("short_adventures_complete",RavenloftShortAdventureCatalog.size()==17).put("short_adventures_pending",RavenloftShortAdventureCatalog.size()!=17)
                .put("auto_rolls",false).put("source_blob",SOURCE_BLOB);}
        catch(Exception e){return o;}
    }

    private static AdventureModule build(Spec s){
        Map<String,AdventureNode> nodes=new LinkedHashMap<String,AdventureNode>();
        for(int i=0;i<s.stages.length;i++){
            String nodeId="stage_"+(i+1);
            List<AdventureTransition> transitions=new ArrayList<AdventureTransition>();
            if(i+1<s.stages.length){
                String next="stage_"+(i+2);
                JSONObject effects=new JSONObject();
                try{effects.put("add_flags",new JSONArray().put("rhw_"+s.id+"_stage_"+(i+1)+"_complete"));}
                catch(Exception ignored){}
                transitions.add(new AdventureTransition("advance_"+(i+1),next,"Avançar o arco","","",false,new JSONObject(),effects));
            }
            JSONObject rewards=new JSONObject();
            try{rewards.put("source_bound",true).put("auto_award",false);}
            catch(Exception ignored){}
            AdventureNode node=new AdventureNode(nodeId,
                    s.domain+" — Marco "+(i+1),
                    s.stages[i],
                    "Use apenas fatos confirmados pelo módulo e pelo estado persistido. Qualquer teste, ataque, save, dano ou gasto de recurso deve ser resolvido pelo Rules Engine com entrada explícita do jogador/host; nunca role automaticamente.",
                    "rhw_"+s.id+"_stage_"+(i+1),
                    null,transitions,new JSONObject(),rewards);
            nodes.put(nodeId,node);
        }
        JSONObject meta=new JSONObject();
        try{meta.put("source_book",SOURCE_BOOK).put("source_code","RHW").put("source_page",s.page)
                .put("source_locator","5e.tools data/book/book-rhw.json p."+s.page+" blob "+SOURCE_BLOB)
                .put("domain",s.domain).put("runtime_tier","campaign_arc_runtime")
                .put("source_fidelity","paraphrased_milestones_no_raw_prose")
                .put("rules_authority","rules_engine").put("world_authority","world_engine")
                .put("ai_authority","narration_only").put("auto_rolls",false)
                .put("short_adventure_detail_complete",RavenloftShortAdventureCatalog.size()==17);}
        catch(Exception ignored){}
        return new AdventureModule(moduleId(s),s.domain+" — Campaign Arc (RHW)","1.0.0","stage_1",nodes,meta);
    }
    private static String moduleId(Spec s){return "rhw_campaign_"+s.id;}
}
