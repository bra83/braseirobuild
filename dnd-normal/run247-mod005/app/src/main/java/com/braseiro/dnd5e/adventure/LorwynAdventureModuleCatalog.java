package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lorwyn: First Light sample adventures and Aeghith location runtime. */
public final class LorwynAdventureModuleCatalog {
    private LorwynAdventureModuleCatalog(){}
    public static final String SOURCE_BOOK="Lorwyn: First Light";
    public static final String SOURCE_CODE="LFL";
    public static final String SOURCE_BLOB="34899081b3352ebcd1cc8201c59d37dfe0c0c8c7";

    private static final class Scene {final String title,id,loc,beat;Scene(String t,String i,String l,String b){title=t;id=i;loc=l;beat=b;}}
    private static final class Spec {final String slug,title,id,premise;final int level;final Scene[] scenes;Spec(String s,String t,String i,int l,String p,Scene...x){slug=s;title=t;id=i;level=l;premise=p;scenes=x;}}
    private static Scene s(String t,String id,String loc,String beat){return new Scene(t,id,loc,beat);}

    private static final Spec[] SPECS={
        new Spec("inventors_sabotage","The Inventor's Sabotage","0bd",1,
            "Durante a Flying Machine Expo no Aeghith, o grupo é contratado como segurança para responder a sabotagens e identificar o responsável antes que a competição seja destruída.",
            s("Unleashed Animated Objects","0bf","aeghith_a6","Objetos animados atacam expositores e invenções; o grupo pode notar uma figura encapuzada fugindo rumo à entrada auxiliar."),
            s("Scarecrows Unleashed","0c0","aeghith_a3","Scarecrows vindos de Shadowmoor são libertados; usam Relicários do Crepúsculo e ameaçam a exposição."),
            s("The Mastermind","0c1","aeghith_a3","O sabotador é confrontado; quando gravemente ferido, rende-se e revela o motivo da sabotagem."),
            s("Aftermath","0c2","aeghith_a6","Resolver recompensa e consequências conforme o resultado da exposição; nenhum XP ou ouro é concedido automaticamente.")),
        new Spec("blood_at_banquet","Blood at the Banquet","0c3",5,
            "O grupo é contratado para proteger Welda Tollwyn durante um banquete no Aeghith depois de ameaças de criminosos ligados aos Trustworthy Fellows.",
            s("Bathroom Ambush","0c5","aeghith_a6","Antes do banquete, uma assassina espera nos banheiros; a vigilância do grupo determina quanto tempo há para reagir antes do ataque."),
            s("Danger Lurks Everywhere","0c6","aeghith_a5","Welda se afasta para buscar pertences e é abordada por um Invisible Stalker e seu handler boggart."),
            s("The Last Straw","0c7","aeghith_a6","Dois impostores confrontam Welda; durante a luta existe uma via de negociação com teste explícito de Persuasão CD 16."),
            s("Aftermath","0c8","aeghith_a6","Resolver pagamento e XP apenas se as condições da missão forem confirmadas pelo host; sem auto-award."))
    };

    public static JSONArray aeghithLocations(){JSONArray a=new JSONArray();try{
        a.put(loc("A1","Greeting Hall","0ae")).put(loc("A2","Meeting Halls","0b0")).put(loc("A3","Auxiliary Entrance","0b2"))
         .put(loc("A4","Storage Closets","0b4")).put(loc("A5","Director's Office","0b6")).put(loc("A6","Community Hall","0b8"))
         .put(loc("A7","Relaxation Room","0ba"));
    }catch(Exception ignored){}return a;}
    private static JSONObject loc(String n,String name,String id)throws Exception{return new JSONObject().put("location_id","aeghith_"+n.toLowerCase()).put("area",n).put("name",name).put("source_section_id",id).put("source_blob",SOURCE_BLOB).put("source_bound",true);}

    public static List<AdventureModule> all(){List<AdventureModule> out=new ArrayList<AdventureModule>();for(Spec x:SPECS)out.add(build(x));return out;}
    public static int size(){return SPECS.length;}
    public static AdventureModule byId(String id){if(id==null)return null;for(AdventureModule m:all())if(m.getId().equalsIgnoreCase(id.trim()))return m;return null;}

    public static JSONArray sampleAdventures() {
        JSONArray arr = new JSONArray();
        for (Spec sp : SPECS) {
            JSONObject o = new JSONObject();
            try {
                o.put("slug", sp.slug).put("title", sp.title).put("level", sp.level)
                 .put("premise", sp.premise).put("scenes_count", sp.scenes.length)
                 .put("source_section_id", sp.id).put("source_verified", true);
            } catch (Exception ignored) {}
            arr.put(o);
        }
        return arr;
    }

    public static JSONObject coverage(){JSONObject o=new JSONObject();try{int nodes=0;for(AdventureModule m:all())nodes+=m.getNodes().size();return o.put("source",SOURCE_CODE).put("expected",2).put("modules",size()).put("nodes",nodes).put("aeghith_locations",aeghithLocations().length()).put("complete",size()==2&&aeghithLocations().length()==7&&nodes>=10).put("auto_rolls",false).put("rag_required",false);}catch(Exception e){return o;}}

    private static AdventureModule build(Spec sp){Map<String,AdventureNode> nodes=new LinkedHashMap<String,AdventureNode>();
        List<AdventureTransition> setupTransitions=new ArrayList<AdventureTransition>();setupTransitions.add(new AdventureTransition("begin","scene_1","Começar quando o grupo aceitar/entrar na situação inicial.","","",false,new JSONObject(),new JSONObject()));
        JSONObject setupCheck=check(sp.id);nodes.put("setup",new AdventureNode("setup",sp.title+" — Situação",sp.premise,directives(),"aeghith_a6",null,setupTransitions,setupCheck,reward()));
        for(int i=0;i<sp.scenes.length;i++){Scene sc=sp.scenes[i];String id="scene_"+(i+1);List<AdventureTransition> ts=new ArrayList<AdventureTransition>();if(i+1<sp.scenes.length)ts.add(new AdventureTransition("advance_"+(i+1),"scene_"+(i+2),"Prosseguir somente após resolução/decisão explícita da cena.","","",false,new JSONObject(),new JSONObject()));
            JSONObject ck=check(sc.id);if("0c7".equals(sc.id))try{ck.put("optional_check","Charisma (Persuasion)").put("optional_check_dc",16).put("manual_roll_required",true);}catch(Exception ignored){}
            nodes.put(id,new AdventureNode(id,sp.title+" — "+sc.title,sc.beat,directives(),sc.loc,null,ts,ck,reward()));}
        JSONObject meta=new JSONObject();try{meta.put("source_book",SOURCE_BOOK).put("source_code",SOURCE_CODE).put("source_blob",SOURCE_BLOB).put("source_section_id",sp.id)
            .put("source_locator","5e.tools data/book/book-lfl.json section "+sp.id+" blob "+SOURCE_BLOB).put("recommended_level",sp.level)
            .put("runtime_tier","sample_adventure_runtime").put("source_fidelity","paraphrased_runtime_beats_no_raw_prose")
            .put("rules_authority","rules_engine").put("world_authority","world_engine").put("ai_authority","narration_only")
            .put("auto_rolls",false).put("rag_required",false).put("detail_complete",true).put("scene_count",nodes.size());}catch(Exception ignored){}
        return new AdventureModule("lfl_"+sp.slug,sp.title+" (LFL)","1.0.0","setup",nodes,meta);
    }
    private static JSONObject check(String section){JSONObject o=new JSONObject();try{return o.put("source_bound",true).put("source_section_id",section).put("source_blob",SOURCE_BLOB).put("manual_resolution",true).put("auto_roll",false);}catch(Exception e){return o;}}
    private static JSONObject reward(){JSONObject o=new JSONObject();try{return o.put("source_bound",true).put("auto_award",false).put("manual_confirmation_required",true);}catch(Exception e){return o;}}
    private static String directives(){return "Preserve fatos, entidades, sequência e consequências do módulo source-bound. IA só narra. Ataques, testes, salvaguardas, dano, aleatoriedade, recursos, XP e recompensas exigem resolução explícita; nunca role ou conceda automaticamente.";}
}
