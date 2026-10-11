package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RHW short adventures compiled as deterministic, source-bound AdventureModules.
 * No book prose is embedded. Nodes are paraphrased runtime beats tied to source section IDs/pages.
 * Source: 5e.tools RHW book-rhw.json blob 1e2f87f2c79d99ac9d989fe80d61bc1bfbec6cd5.
 */
public final class RavenloftShortAdventureCatalog {
    private RavenloftShortAdventureCatalog() {}
    public static final String SOURCE_BOOK = RavenloftAdventureModuleCatalog.SOURCE_BOOK;
    public static final String SOURCE_BLOB = RavenloftAdventureModuleCatalog.SOURCE_BLOB;

    private static final class SceneSpec {
        final String title, sourceId; final int page;
        SceneSpec(String title,int page,String sourceId){this.title=title;this.page=page;this.sourceId=sourceId;}
    }
    private static final class Spec {
        final String slug,domain,title,sourceId,premise; final int page; final SceneSpec[] scenes;
        Spec(String slug,String domain,String title,int page,String sourceId,String premise,SceneSpec...scenes){
            this.slug=slug;this.domain=domain;this.title=title;this.page=page;this.sourceId=sourceId;this.premise=premise;this.scenes=scenes;
        }
    }
    private static SceneSpec s(String title,int page,String id){return new SceneSpec(title,page,id);}

    private static final Spec[] SPECS = new Spec[]{
        new Spec("blood_o_the_vine","Barovia","Blood o' the Vine",47,"0e2",
            "Investigar o fechamento da taverna Blood of the Vine depois que uma descoberta subterrânea liberou magia sinistra e corrompeu seus proprietários; Urwin Martikov oferece pagamento pela investigação.",
            s("Corrupted Owners",47,"0e4"),s("Taproom",47,"0e5"),s("Office",48,"0e6"),s("Private Dining Room",48,"0e7"),s("Temple of Ravenkind",48,"0e8"),s("Cursed Remains",48,"0e9"),s("Returning to Urwin",48,"0ea")),
        new Spec("treacherous_blood","Barovia","Treacherous Blood",48,"0eb",
            "Investigar escavações no cemitério de Berez enquanto Lyssa von Zarovich trama enfraquecer Strahd e soltar seus servos monstruosos sobre Barovia.",
            s("Berez Churchyard",48,"0ed"),s("Lyssa's Plot",48,"0ee"),s("Crypt",48,"0ef"),s("Hidden Chamber",48,"0f0"),s("Elder Evil",48,"0f1"),s("Conclusion",48,"0f2")),
        new Spec("lethal_lavender","Borca","Lethal Lavender",58,"109",
            "Os irmãos Eroso e Siobhan Manduchi tentam usar o grupo como bode expiatório em uma falsa morte depois de roubarem uma planta rara ligada aos Boritsi.",
            s("Dig Your Vibe",58,"10b"),s("Exquisite Corpse",58,"10c"),s("Wanted",58,"10d"),s("Dead Walking",58,"10e"),s("Flawed Perfection",58,"10f"),s("Lethal Lavender",58,"110"),s("Ivana's Favor",58,"111")),
        new Spec("secrets_of_avernus","Darkon","Secrets of Avernus",68,"12c",
            "Entrar em Castle Avernus para descobrir o segredo do desaparecimento de Azalin enquanto a Shroud e os pretendentes ao trono ameaçam Darkon.",
            s("Lychgate Encounters",68,"12e"),s("Into the Maw",68,"12f"),s("The Archive",68,"130"),s("Crypt of Irik Zal'honan",68,"131"),s("Hall of Life",68,"132"),s("Ebbasheyth's Lair",68,"133"),s("Conclusion",68,"134")),
        new Spec("grand_masquerade","Dementlieu","The Grand Masquerade",76,"145",
            "Preparar disfarces e convites, entrar no Grand Masquerade e usar o evento para obter acesso a nobres e informações normalmente inalcançáveis.",
            s("Dressed to Kill",76,"147"),s("Arrival",76,"148"),s("Masquerade",76,"149"),s("Uncouth Exchange",76,"14a"),s("The Duchess Appears",76,"14b"),s("Conclusion",76,"14c")),
        new Spec("siege_of_lekar","Falkovnia","Siege of Lekar",84,"15f",
            "Defender o Gate of Torches durante o ataque zumbi a Lekar e sobreviver ao plano de Vladeska Drakov de sacrificar o grupo e parte da cidade.",
            s("Lekar Gates",84,"160"),s("Zombie Attack",84,"162"),s("Drakov's Talons",85,"163"),s("Talon Wagon",85,"164"),s("Siege Climax",85,"165"),s("Loyalist's Gate",85,"166"),s("Drakov's Defeat",85,"167")),
        new Spec("scrolls_of_the_dead","Har'Akir","Scrolls of the Dead",94,"17a",
            "Impedir o ritual de Isu Rehkotep, que pretende controlar Senmet para derrubar Ankhtepot, antes que a retaliação do faraó atinja Muhar.",
            s("Exploring the Pyramid",94,"17c"),s("Pyramid Heights",94,"17d"),s("Ritual Site",94,"17e"),s("The Mummy",94,"17f"),s("The Scroll",94,"180"),s("The Pharaoh",94,"181")),
        new Spec("eyes_of_the_darklord","Hazlan","Eyes of the Darklord",102,"193",
            "Ajudar vítimas de experimentos mágicos a escapar de Hazlan destruindo a orrery em Veneficus, usada pelos Eyes of Hazlik para localizar fugitivos.",
            s("Greeting at Veneficus",102,"195"),s("Galleries of Suffering",102,"196"),s("Flawed Magic",102,"197"),s("Red Tower",102,"198"),s("Orrery Chamber",102,"199"),s("Hazlik",102,"19a")),
        new Spec("vault_of_the_elder","Innsmouth","Vault of the Elder",112,"1b5",
            "Seguir instruções de um culto até um vault cujo elder conhece segredos para escapar da ilha, enquanto o patrono exige uma relíquia do local.",
            s("Vault Ambush",112,"1b7"),s("Frigid Spires",112,"1b8"),s("Hollow City",112,"1b9"),s("Encampment",112,"1ba"),s("Elder Temple",112,"1bb"),s("Frozen Warning",112,"1bc"),s("Elder's Reliquary",112,"1bd"),s("Ice Tombs",112,"1be")),
        new Spec("tower_of_traitors","Kalakeri","Tower of Traitors",120,"1d0",
            "Recuperar dois crânios na Tower of Traitors para Arijani ou Reeva, sem receber inicialmente a verdade sobre a identidade e o poder ligados aos restos.",
            s("Stolen Skulls",120,"1d2"),s("Learning About the Skulls",120,"1d3"),s("Tower of Traitors",120,"1d4"),s("Searching the Tower",120,"1d5"),s("Loyalty or Treachery",120,"1d6")),
        new Spec("hungry_for_more","Kartakass","Hungry for More",128,"1eb",
            "Trazer o cantor Frantosh de volta à família antes que a influência de Harkon Lukas e a apresentação na noite de lua cheia o consumam.",
            s("Frantosh's Debut",128,"1ed"),s("Full-Moon",128,"1ee"),s("The Old Kartakan",128,"1ef"),s("The Show",128,"1f0"),s("After the Show",128,"1f1"),s("Conclusion",128,"1f2")),
        new Spec("isles_of_agony","Lamordia","Isles of Agony",138,"205",
            "Recuperar para Viktra o Unbreakable Heart, sem saber no início que ele está dentro de Elise, e decidir como lidar com a suposta criatura responsável.",
            s("Shipwreck",138,"207"),s("The Monster",138,"208"),s("Track of the Monster",138,"209"),s("Hope's Heart",138,"20a"),s("Fighting Elise",138,"20b"),s("Negotiating with Elise",138,"20c"),s("The Crew",138,"20d"),s("Aftermath",138,"20e")),
        new Spec("ghost_hunters_haunting","Mordent","The Ghost Hunter's Haunting",146,"220",
            "Encerrar a assombração na loja de ervas de Rudolph van Richten após um acidente quebrar uma relíquia e libertar um espírito felino mortal.",
            s("Beatrice Polk's Aid",146,"222"),s("Ghost Cat",146,"223"),s("Laboratory",146,"224"),s("Stairs",146,"225"),s("Dumbwaiter",146,"226"),s("Upstairs Hall",146,"227"),s("Library",146,"228"),s("Guest Room",146,"229"),s("Cellar",146,"22a")),
        new Spec("shadowborn_sword","Shadowlands","The Shadowborn Sword",154,"244",
            "Acompanhar Lucia Lambert na missão de recuperar a espada perdida de Kateri Shadowborn e enfrentar as forças que cercam Shadowborn Manor.",
            s("Phantasmal Forest",154,"246"),s("Fallen Knight",154,"247"),s("Shadowborn Manor",154,"248"),s("Shadowborn's Ghost",154,"249"),s("Ebonbane",154,"24a"),s("Conclusion",154,"24b")),
        new Spec("shadow_of_the_black_rose","Sithicus","Shadow of the Black Rose",162,"25f",
            "Buscar para Inza Kulchevich uma gema mágica ligada a Azrael Dax em troca de informação sobre como escapar de Sithicus.",
            s("Inza's Quest",162,"261"),s("Inza's Secret",162,"262"),s("Searching the Ruins",162,"263"),s("Kender Nosferatu",162,"264"),s("Shadow Highlord",162,"265"),s("Azrael Dax",162,"266"),s("Repairing the Pendant",162,"267"),s("Soth Appears",162,"268"),s("Escape",162,"269")),
        new Spec("the_tithe","Tepest","The Tithe",170,"27a",
            "Participar do Tithe em Viktal e descobrir que a celebração religiosa conduz os participantes selecionados ao sacrifício para Mother.",
            s("Tithe Competitions",170,"27c"),s("Competition Winners",170,"27d"),s("Feast",170,"27e"),s("The Sacrifice",170,"27f"),s("Conclusion",170,"280")),
        new Spec("trial_of_hearts","Valachan","Trial of Hearts",180,"295",
            "Aceitar a convocação de Chakuna em Pantara Lodge e enfrentar a Trial of Hearts como caminho para tentar deixar Valachan.",
            s("Pantara Lodge",180,"297"),s("Chakuna's Guests",180,"298"),s("Trial Rules",180,"299"),s("Cheating",180,"29a"),s("Choosing a Route",180,"29b"),s("Trial Encounters",180,"29c"),s("Chakuna During the Trial",181,"29d"),s("Kiru Island Event",181,"29e"),s("Scars Event",181,"29f"),s("Trial Shrine",181,"2a0"),s("Rival",181,"2a1"),s("Conclusion",181,"2a2"))
    };

    public static List<AdventureModule> all(){
        List<AdventureModule> out=new ArrayList<AdventureModule>();
        for(Spec spec:SPECS)out.add(build(spec));
        return out;
    }
    public static int size(){return SPECS.length;}
    public static AdventureModule byId(String moduleId){if(moduleId==null)return null;for(AdventureModule m:all())if(m.getId().equalsIgnoreCase(moduleId.trim()))return m;return null;}
    public static JSONArray catalog(){JSONArray a=new JSONArray();for(Spec sp:SPECS)try{a.put(new JSONObject().put("module_id",moduleId(sp)).put("domain",sp.domain).put("title",sp.title).put("source_page",sp.page).put("source_section_id",sp.sourceId).put("scenes",sp.scenes.length).put("runtime_tier","short_adventure_runtime"));}catch(Exception ignored){}return a;}
    public static JSONObject coverage(){JSONObject o=new JSONObject();try{return o.put("source","RHW").put("short_adventures_expected",17).put("short_adventure_modules",SPECS.length).put("short_adventures_complete",SPECS.length==17).put("runtime_tier","short_adventure_runtime").put("auto_rolls",false).put("source_blob",SOURCE_BLOB);}catch(Exception e){return o;}}

    private static AdventureModule build(Spec sp){
        Map<String,AdventureNode> nodes=new LinkedHashMap<String,AdventureNode>();
        List<SceneSpec> runtime=new ArrayList<SceneSpec>();
        runtime.add(new SceneSpec("Situação e gancho",sp.page,sp.sourceId));
        for(SceneSpec scene:sp.scenes)runtime.add(scene);
        for(int i=0;i<runtime.size();i++){
            SceneSpec sc=runtime.get(i); String id=i==0?"setup":"scene_"+i;
            List<AdventureTransition> transitions=new ArrayList<AdventureTransition>();
            if(i+1<runtime.size()){
                String target="scene_"+(i+1); JSONObject effects=new JSONObject();
                try{effects.put("add_flags",new JSONArray().put("rhw_short_"+sp.slug+"_beat_"+i+"_complete"));}catch(Exception ignored){}
                transitions.add(new AdventureTransition("advance_"+i,target,"Prosseguir quando a cena estiver resolvida","","",false,new JSONObject(),effects));
            }
            JSONObject check=new JSONObject(); JSONObject rewards=new JSONObject();
            try{check.put("source_bound",true).put("source_section_id",sc.sourceId).put("source_page",sc.page).put("manual_resolution",true).put("auto_roll",false);
                rewards.put("source_bound",true).put("auto_award",false); }catch(Exception ignored){}
            String desc=i==0?sp.premise:("Cena source-bound: "+sc.title+". Preserve a ordem, fatos, entidades e consequências definidos para esta seção; não invente resolução mecânica fora do Rules Engine.");
            String directives="Narração pode adaptar forma, nunca fatos mecânicos. Testes, ataques, saves, dano, tabelas aleatórias, recursos e recompensas exigem resolução explícita pelo Rules Engine/host; jamais role automaticamente. Não avance antes da resolução/decisão do jogador.";
            nodes.put(id,new AdventureNode(id,sp.title+" — "+sc.title,desc,directives,"rhw_short_"+sp.slug+"_"+sc.sourceId,null,transitions,check,rewards));
        }
        JSONObject meta=new JSONObject(); try{meta.put("source_book",SOURCE_BOOK).put("source_code","RHW").put("source_blob",SOURCE_BLOB).put("source_page",sp.page).put("source_section_id",sp.sourceId)
            .put("source_locator","5e.tools data/book/book-rhw.json section "+sp.sourceId+" p."+sp.page+" blob "+SOURCE_BLOB).put("domain",sp.domain).put("runtime_tier","short_adventure_runtime")
            .put("source_fidelity","paraphrased_runtime_beats_no_raw_prose").put("rules_authority","rules_engine").put("world_authority","world_engine").put("ai_authority","narration_only")
            .put("auto_rolls",false).put("short_adventure_detail_complete",true).put("scene_count",nodes.size());}catch(Exception ignored){}
        return new AdventureModule(moduleId(sp),sp.title+" (RHW)","1.0.0","setup",nodes,meta);
    }
    private static String moduleId(Spec sp){return "rhw_short_"+sp.slug;}
}
