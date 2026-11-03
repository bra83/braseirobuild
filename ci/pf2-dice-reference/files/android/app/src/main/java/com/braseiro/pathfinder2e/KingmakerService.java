package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;

/**
 * Fachada transacional do modo Kingmaker.
 * Liga Reino, Atlas de campanha, Mundo Vivo, Diário e o motor PF2e sem criar
 * um segundo banco ou permitir que narrativa gerada grave mecânica.
 */
public final class KingmakerService {
    private final AppDb db;
    private final KingmakerRepository repository;
    private final CampaignPoiService campaignPois;
    private final KingmakerRulesEngine rules;
    private final KingmakerSoloDirector solo;

    public KingmakerService(AppDb database){
        if(database==null)throw new IllegalArgumentException("AppDb ausente.");
        db=database;repository=new KingmakerRepository(db);campaignPois=new CampaignPoiService(db);rules=new KingmakerRulesEngine();solo=new KingmakerSoloDirector(db,repository);
    }

    public KingmakerRepository repository(){return repository;}
    public KingmakerRulesEngine rules(){return rules;}
    public boolean enabled(){return repository.enabled();}
    public JSONObject soloSnapshot(){return solo.snapshot();}
    public String soloMasterBrief(){return solo.masterBrief();}

    public JSONObject activate(JSONObject setup)throws Exception{
        JSONObject spec=setup==null?new JSONObject():new JSONObject(setup.toString());String name=spec.optString("kingdom_name").trim();if(name.length()==0)throw new IllegalArgumentException("Informe o nome do Reino.");if(!spec.has("party_level_cap")){int partyLevel=1;for(JSONObject hero:db.listActiveCharacters())partyLevel=Math.max(partyLevel,hero.optInt("level",1));spec.put("party_level_cap",partyLevel);}spec.put("enabled",true).put("campaign_mode","kingmaker");JSONObject state=repository.configure(spec);persistFoundationChoice("foundation:charter","charter",state.optString("charter"));persistFoundationChoice("foundation:heartland","heartland",state.optString("heartland"));persistFoundationChoice("foundation:government","government",state.optString("government"));String startingHex=spec.optString("starting_hex").trim();if(startingHex.length()>0&&repository.claimedHexCount()==0){repository.initializeFoundingHex(startingHex,"Hex fundador definido na ativação Kingmaker.");String capital=state.optString("capital_name").trim();if(capital.length()>0&&repository.settlements().isEmpty())foundSettlement(capital,"village",startingHex,"Capital fundadora do Reino.");}state=repository.state();db.addJournalChecked("Fundação de "+name,"O modo Kingmaker foi ativado. Capital: "+state.optString("capital_name","a definir")+". Carta: "+state.optString("charter","a definir")+". Região natal: "+state.optString("heartland","a definir")+". Governo: "+state.optString("government","a definir")+".","Kingmaker");return state;
    }

    public JSONObject deactivate()throws Exception{return repository.configure(new JSONObject().put("enabled",false));}

    /** Funda assentamento e POI mutável no mesmo commit. O Atlas oficial não é escrito. */
    public JSONObject foundSettlement(final String name,final String type,final String hexId,final String notes)throws Exception{
        if(!enabled())throw new IllegalStateException("Ative Kingmaker primeiro.");final JSONObject[] result={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            String settlementType="village",settlementKey="kingdom-settlement:"+UUID.randomUUID().toString().replace("-","");JSONObject settlement=repository.addSettlement(settlementKey,name,settlementType,hexId,"",notes);JSONObject spec=new JSONObject().put("name",name).put("type","settlement").put("description","Aldeia fundada pelo Reino; a categoria pode evoluir pelas regras de assentamento.").put("origin","kingmaker_found_settlement").put("created_by","kingdom").put("settlement_key",settlementKey).put("settlement",new JSONObject().put("size",settlementType).put("level",1).put("population",settlement.optInt("population")).put("government",repository.state().optString("government"))).put("tags",new JSONArray().put("kingmaker").put("settlement"));JSONObject poi=campaignPois.createManual(spec,hexId);repository.linkSettlementPoi(settlementKey,poi.optString("poi_id"));result[0]=new JSONObject().put("settlement",repository.settlement(settlementKey)).put("campaign_poi",poi);
        }});db.addJournalChecked("Assentamento fundado — "+name,"O Reino fundou uma aldeia no território "+hexId+".","Kingmaker");return result[0];
    }

    public JSONObject importCompanionFromNpc(long npcId,String assignment)throws Exception{
        JSONObject npc=db.queryOne("SELECT * FROM npc_memory_profiles WHERE id=? AND world_profile=?",new String[]{String.valueOf(npcId),db.getActiveMapProfile()});if(npc.length()==0)throw new IllegalArgumentException("NPC persistente inexistente.");for(JSONObject row:repository.companions())if(row.optLong("npc_id")==npcId)return row;JSONObject companion=repository.addCompanion(npc.optString("name"),npcId,assignment,npc.optString("summary"));db.addJournalChecked("Companheiro recrutado — "+npc.optString("name"),npc.optString("name")+" passou a integrar o elenco Kingmaker como "+assignment+".","Kingmaker");return companion;
    }

    /** Proposta contextual: não inventa CDs; usa somente dados já persistidos e deixa a resolução para as regras existentes. */
    public JSONObject contextualCampProposal()throws Exception{
        JSONObject map=db.getMapState(),scene=db.getSceneState(),out=new JSONObject();
        String hex=map.optString("current_hex_id",scene.optString("hex_id"));
        JSONObject atlas=new JSONObject();String packageId=map.optString("map_package_id");if(packageId.length()>0&&hex.length()>0)atlas=db.getMapHex(packageId,hex);
        out.put("hex",hex).put("hex_id",hex).put("day",scene.optInt("day_number",1)).put("day_number",scene.optInt("day_number",1)).put("weather",scene.optString("weather",map.optString("weather")))
                .put("period",scene.optString("time_of_day",scene.optString("period"))).put("terrain",atlas.optString("terrain_primary"))
                .put("zone_name",atlas.optString("name",atlas.optString("terrain_primary","Ermos")))
                .put("companions",new JSONArray(repository.companions())).put("policy","CDs e efeitos não são inferidos: recupere a autoridade Kingmaker quando a zona não tiver valores persistidos.");
        return out;
    }

    public JSONObject startCampAtCurrentPosition(String zoneName,int zoneDc,int encounterDc,String prepareDegree,boolean reused,int specialIngredients,String notes)throws Exception{
        JSONObject map=db.getMapState(),scene=db.getSceneState();String hex=map.optString("current_hex_id",scene.optString("hex_id"));return repository.startCamp(hex,scene.optInt("day_number",1),zoneName,zoneDc,encounterDc,prepareDegree,reused,specialIngredients,scene.optString("weather"),notes);
    }

    public JSONObject dashboard(){
        JSONObject out=repository.compactContext();try{JSONObject state=repository.state(),turn=repository.activeTurn();out.put("profile",KingmakerRulesEngine.PROFILE).put("turn_phase_label",phaseLabel(turn.length()>0?turn.optString("phase"):state.optString("turn_phase"))).put("resource_formula",state.optInt("resource_die_count",1)+"d"+state.optInt("resource_die_size",4)).put("claimed_hexes",repository.claimedHexCount()).put("settlement_count",repository.settlements().size()).put("army_count",repository.armies().size()).put("companion_count",repository.companions().size());}catch(Exception e){try{out.put("dashboard_error",String.valueOf(e.getMessage()));}catch(Exception ignored){}}return out;
    }

    /** Contexto completo para o Mestre, explicitamente read-only para a IA. */
    public JSONObject masterContext(){JSONObject out=repository.compactContext();try{out.put("mechanical_authority","KingmakerRulesEngine + Pathfinder2eRulesEngine").put("mutation_policy","proposal_only; mudanças são feitas pela interface Kingmaker e revisadas pelo jogador").put("manual_retrieval_hint","Para efeitos de atividades, estruturas, eventos, táticas ou estatísticas não presentes no catálogo local, recupere a passagem do manual Kingmaker/Companion Guide antes de narrar; nunca invente a mecânica.").put("solo_snapshot",solo.snapshot()).put("solo_play_brief",solo.masterBrief());}catch(Exception ignored){}return out;}

    public String ruleSearchQuery(String topic){String q=topic==null?"":topic.trim();return "Pathfinder 2e Kingmaker kingdom turn leadership kingdom skill activity settlement structure hexploration army warfare camping companion influence "+q;}
    public String authority(){return rules.authority();}

    private void persistFoundationChoice(String featureKey,String kind,String name)throws Exception{if(name==null||name.trim().length()==0)return;repository.upsertFeature(featureKey,kind,name,1,"{}","Kingmaker — criação do Reino","Escolha de fundação; efeitos específicos dependem da opção confirmada no manual.");}

    public static String phaseLabel(String phase){String p=phase==null?"":phase.toUpperCase(Locale.US);if("UPKEEP".equals(p))return "Manutenção";if("COMMERCE".equals(p))return "Comércio";if("ACTIVITY".equals(p))return "Atividades";if("EVENT".equals(p))return "Evento";return "Concluído";}
}
