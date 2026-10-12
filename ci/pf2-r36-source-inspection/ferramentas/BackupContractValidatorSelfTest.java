package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
public final class BackupContractValidatorSelfTest {
  private static void ok(boolean c,String m){if(!c)throw new AssertionError(m);}
  private static JSONObject base(int version)throws Exception{
    JSONObject b=new JSONObject().put("format","braseiro-pathfinder2e-backup").put("version",version).put("systemId","pathfinder_2e").put("worldProfile","pf2");
    if(version>=12)b.put("backupScope","campaign");
    b.put("campaign",new JSONObject().put("world_profile","pf2").put("name","Teste"));
    b.put("characters",new JSONArray().put(new JSONObject().put("world_profile","pf2").put("name","Heroi")));
    b.put("mapState",new JSONObject().put("logical_map_id",""));
    b.put("derivedIndexesExcluded",true);
    b.put("externalLibrariesExcluded",new JSONArray().put("manuals").put("tokens").put("audio_library").put("map_package_files").put("manual_rag_parts"));
    if(version>=21){b.put("campaignPoiMeta",new JSONObject().put("world_profile","pf2").put("campaign_key","campaign_test"));b.put("campaignPois",new JSONArray());b.put("campaignPoiHistory",new JSONArray());b.put("campaignPoiProposals",new JSONArray());}
    if(version>=22){
      b.put("kingmakerState",new JSONObject().put("world_profile","pf2").put("level",1).put("party_level_cap",1).put("size",1).put("consumption",0).put("consumption_modifier",0).put("turn_phase","COMPLETE").put("active_turn_id",0));
      JSONArray skills=new JSONArray();String[] skillKeys={"agriculture","arts","boating","defense","engineering","exploration","folklore","industry","intrigue","magic","politics","scholarship","statecraft","trade","warfare","wilderness"};for(String key:skillKeys)skills.put(new JSONObject().put("world_profile","pf2").put("skill_key",key));b.put("kingmakerSkills",skills);
      JSONArray leaders=new JSONArray();String[] roleKeys={"ruler","counselor","general","emissary","magister","treasurer","viceroy","warden"};for(String key:roleKeys)leaders.put(new JSONObject().put("world_profile","pf2").put("role_key",key).put("vacant",1).put("invested",0));b.put("kingmakerLeaders",leaders);
      for(String key:new String[]{"kingmakerFeatures","kingmakerHexes","kingmakerSettlements","kingmakerStructures","kingmakerTurns","kingmakerTurnActivities","kingmakerEvents","kingmakerArmies","kingmakerWarEncounters","kingmakerCompanions","kingmakerCamps","kingmakerTradeAgreements"})b.put(key,new JSONArray());
    }
    return b;
  }
  public static void main(String[]args)throws Exception{
    JSONObject v21=BackupContractValidator.validate(base(22));
    ok(v21.optBoolean("valid",false),"backup atual v22 foi rejeitado: "+v21);
    ok(v21.optJSONArray("warnings").toString().contains("mapa sem identificador lógico"),"campanha mapless deve ser aviso, não falha");
    BackupContractValidator.requireValid(base(22));
    JSONObject future=BackupContractValidator.validate(base(23));
    ok(!future.optBoolean("valid",true),"versão futura deve ser recusada");
    ok(future.optJSONArray("errors").toString().contains("versão não suportada"),"erro de versão futura ausente");
    JSONObject withPoi=base(22);withPoi.put("campaignPois",new JSONArray().put(new JSONObject().put("world_profile","pf2").put("campaign_key","campaign_test").put("poi_id","poi_campaign_1").put("name","Taverna do Carvalho Negro").put("hex_id","27.29").put("source","campaign").put("canonical",0).put("hidden",0).put("discovered",1)));
    ok(BackupContractValidator.validate(withPoi).optBoolean("valid",false),"POI de campanha válido foi rejeitado");
    JSONObject leakedCanonical=base(22);leakedCanonical.put("campaignPois",new JSONArray().put(new JSONObject().put("world_profile","pf2").put("campaign_key","campaign_test").put("poi_id","poi_bad").put("name","Oficial indevido").put("hex_id","27.29").put("source","campaign").put("canonical",1)));
    ok(!BackupContractValidator.validate(leakedCanonical).optBoolean("valid",true),"POI canônico vazou para camada de campanha");
    JSONObject badParent=base(22);badParent.put("campaignPois",new JSONArray().put(new JSONObject().put("world_profile","pf2").put("campaign_key","campaign_test").put("poi_id","child").put("name","Ferreiro").put("hex_id","27.29").put("source","campaign").put("canonical",0).put("parent_poi_id","missing")));
    ok(!BackupContractValidator.validate(badParent).optBoolean("valid",true),"parent_poi_id ausente não foi recusado");
    JSONObject pathLeak=base(22);pathLeak.optJSONObject("mapState").put("image_uri","content://arquivo");
    ok(!BackupContractValidator.validate(pathLeak).optBoolean("valid",true),"backup atual não pode carregar caminho físico");
    JSONObject legacy=base(11);legacy.optJSONObject("mapState").put("map_package_id","pacote-antigo");
    JSONObject lr=BackupContractValidator.validate(legacy);ok(lr.optBoolean("valid",false),"backup legado suportado deve migrar/avisar: "+lr);ok(lr.optJSONArray("warnings").length()>0,"legado deveria produzir avisos");
    System.out.println("BACKUP_CONTRACT_V22_PASS current=22 future_rejected=23 mapless_nonblocking=true poi_contract=true legacy_supported=11");
  }
}
