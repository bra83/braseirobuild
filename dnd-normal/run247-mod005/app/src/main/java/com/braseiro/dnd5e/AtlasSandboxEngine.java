package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Camada sandbox do Atlas. Não toca no catálogo CANONICAL e não decide regras D&D.
 * Geração procedural só ocorre quando sinais persistidos do mundo dão suporte.
 */
public final class AtlasSandboxEngine {
    private final AppDb db;
    public AtlasSandboxEngine(AppDb database){db=database;}

    public JSONObject createPlayerPoi(String hexId,String name,String type,String description,String parentPoiId) throws Exception {/*BR_JSON_WRAP*/try{
        JSONObject state=db.getMapState();String packageId=state.optString("map_package_id","");
        if(!db.isAtlasContractPackage(packageId))throw new IllegalStateException("O Atlas ativo não usa o contrato sandbox.");
        if(!db.isValidMapHex(packageId,hexId))throw new IllegalArgumentException("Hex inválido: "+hexId);
        JSONObject spec=new JSONObject().put("world_profile",db.getActiveMapProfile()).put("logical_map_id",db.getLogicalMapIdForPackage(packageId)).put("map_package_id",packageId).put("coordinate_frame_id",db.getCoordinateFrameIdForPackage(packageId)).put("hex_id",hexId).put("name",name).put("type",normalizeType(type)).put("description",description==null?"":description).put("origin","campaign").put("creator","player").put("hidden",false).put("discovered",true).put("parent_poi_id",parentPoiId==null?"":parentPoiId).put("status","active");
        JSONObject created=db.createCampaignPoi(spec);
        if(isSettlementType(created.optString("type")))db.createOrUpdateSettlementForPoi(created.optString("id"),new JSONObject().put("population",0).put("population_band","fundação").put("government","").put("economy","").put("security",25).put("prosperity",20));
        return created;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Executado depois de avanço de calendário confirmado. Retorna vazio quando não há razão para gerar algo. */
    public JSONObject afterWorldAdvance(long worldTick){/*BR_JSON_WRAP*/try{
        try{
            JSONObject state=db.getMapState();String packageId=state.optString("map_package_id","");String current=state.optString("current_hex_id","");
            if(!db.isAtlasContractPackage(packageId)||current.length()==0)return new JSONObject();
            List<String> neighbors=db.getMapHexNeighbors(packageId,current);String target=current;if(!neighbors.isEmpty())target=neighbors.get((int)Math.floorMod(stableLong(current+"|"+worldTick),neighbors.size()));
            if(!db.getCampaignPoisForHex(db.getLogicalMapIdForPackage(packageId),target,true).isEmpty())return evolveExisting(target,worldTick);
            JSONObject h=db.getRawMapHex(packageId,target);String terrain=h.optString("terrain_primary","").toLowerCase(Locale.ROOT);boolean road=h.optInt("road_present",0)==1;List<JSONObject> areas=db.getMapAreasForHex(packageId,target);String areasText=areas.toString().toLowerCase(Locale.ROOT);String sandboxFocus=target+" "+terrain+" "+areasText;String events=WorldContextSelector.select(new JSONArray(db.listWorldEventsForMaster(1000)),sandboxFocus,40,"title","description","event_key").toString().toLowerCase(Locale.ROOT);String factions=WorldContextSelector.select(db.listFactions(1000),sandboxFocus,40,"name","agenda","current_plan","location").toString().toLowerCase(Locale.ROOT);String climate=db.getEnvironmentalDirectorState().toString().toLowerCase(Locale.ROOT);
            long gate=Math.floorMod(stableLong(target+"|"+worldTick+"|"+events.hashCode()),100);
            String type="",name="",reason="";
            if((events.contains("guerra")||events.contains("cerco")||events.contains("invas"))&&gate<18){type="forte";name="Fortim de campanha";reason="fronteira pressionada por conflito persistido";}
            else if(road&&(events.contains("comerc")||factions.contains("merc")||gate<12)){type="taverna";name="Estalagem de estrada";reason="hex de estrada com circulação/comércio";}
            else if((terrain.contains("mont")||terrain.contains("rocha")||terrain.contains("colina"))&&gate<10){type="mina";name="Acampamento de prospecção";reason="terreno mineral e atividade de prospecção";}
            else if((terrain.contains("florest")||areasText.contains("floresta"))&&(events.contains("bandid")||events.contains("monstr"))&&gate<15){type="acampamento";name="Esconderijo na mata";reason="floresta associada a ameaça persistida";}
            else if((climate.contains("seca")||climate.contains("tempest"))&&road&&gate<7){type="posto";name="Abrigo de viajantes";reason="estrada sob pressão climática persistente";}
            if(type.length()==0)return evolveExisting(target,worldTick);
            JSONObject spec=new JSONObject().put("logical_map_id",db.getLogicalMapIdForPackage(packageId)).put("map_package_id",packageId).put("coordinate_frame_id",db.getCoordinateFrameIdForPackage(packageId)).put("hex_id",target).put("name",name).put("type",type).put("description","Local procedural criado pelo Mundo Vivo: "+reason+".").put("origin","procedural").put("creator","living_world").put("hidden",true).put("discovered",false).put("status","active").put("updated_tick",worldTick).put("tags",new JSONArray().put("procedural").put("contextual").put(reason));
            return db.createCampaignPoi(spec);
        }catch(Exception e){AppLog.w("AtlasSandboxEngine","Geração procedural do Atlas foi ignorada com segurança.",e);return new JSONObject();}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject evolveExisting(String hex,long tick){/*BR_JSON_WRAP*/try{
        try{String logical=db.getLogicalMapIdForPackage(db.getMapState().optString("map_package_id",""));for(JSONObject p:db.getCampaignPoisForHex(logical,hex,true)){if(!"procedural".equalsIgnoreCase(p.optString("origin")))continue;long age=Math.max(0,tick-p.optLong("created_tick",tick));int dev=p.optInt("development_level",0);if(age>=30&&Math.floorMod(stableLong(p.optString("id")+"|"+tick),7)==0){String type=p.optString("type");String status=p.optString("status","active");if("acampamento".equals(type)&&dev>=1)status="developing_settlement";db.evolveCampaignPoi(p.optString("id"),status,p.optString("owner"),p.optString("faction"),dev+1,tick,new JSONObject().put("reason","desenvolvimento do Mundo Vivo").put("preserve_poi_id",true));return db.getCampaignPoi(p.optString("id"));}}}catch(Exception e){AppLog.w("AtlasSandboxEngine","Evolução procedural não pôde ser aplicada.",e);}return new JSONObject();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static boolean isSettlementType(String type){String t=normalizeType(type);return t.equals("assentamento")||t.equals("cidade")||t.equals("vila");}
    public static String normalizeType(String type){String t=type==null?"outro":type.trim().toLowerCase(Locale.ROOT);String allowed="taverna casa fazenda posto mina templo torre forte castelo guilda porto acampamento assentamento cidade dungeon outro";return (" "+allowed+" ").contains(" "+t+" ")?t:"outro";}
    private static long stableLong(String s){UUID u=UUID.nameUUIDFromBytes((s==null?"":s).getBytes(StandardCharsets.UTF_8));return u.getMostSignificantBits()^u.getLeastSignificantBits();}
}
