package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/**
 * Projeção do Atlas para narrativa/Barbara. O banco continua autoridade.
 * A projeção pública nunca inclui POIs ocultos/não descobertos.
 */
public final class AtlasContextProjector {
    private AtlasContextProjector() {}

    public static JSONObject publicProjection(AppDb db) {
        return projection(db,false);
    }

    /** Uso local do Mestre/Mundo Vivo. Nunca deve ser enviado como contexto público do Barbara. */
    public static JSONObject masterProjection(AppDb db) {
        return projection(db,true);
    }

    private static JSONObject projection(AppDb db,boolean includeHidden) {/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try {
            JSONObject state=db.getMapState();
            String packageId=state.optString("map_package_id","");
            JSONObject pack=db.getMapPackage(packageId);
            String logical=pack.optString("logical_map_id",state.optString("logical_map_id",""));
            String frame=pack.optString("coordinate_frame_id",state.optString("coordinate_frame_id",""));
            String hex=state.optString("current_hex_id","");
            out.put("ready",db.isAtlasContractPackage(packageId));
            out.put("logical_map_id",logical);
            out.put("map_package_id",packageId);
            out.put("atlas_namespace",pack.optString("atlas_namespace",""));
            out.put("coordinate_frame_id",frame);
            out.put("canonical_catalog_version",pack.optString("canonical_catalog_version",""));
            JSONObject position=new JSONObject();
            position.put("hex_id",hex);
            if(hex.length()>0&&state.has("position_u")&&!state.isNull("position_u")&&state.has("position_v")&&!state.isNull("position_v")){
                position.put("u",state.optDouble("position_u")).put("v",state.optDouble("position_v"));
            }
            position.put("migration_status",state.optString("position_migration_status",""));
            out.put("campaign_current_position",position);
            // Camera é deliberadamente um bloco separado e jamais entra em campaign_current_position.
            out.put("camera_position",new JSONObject().put("scale",state.optDouble("scale",1)).put("offset_x",state.optDouble("offset_x",0)).put("offset_y",state.optDouble("offset_y",0)));
            if(hex.length()==0||packageId.length()==0)return out;
            JSONObject raw=db.getMapHex(packageId,hex);
            JSONObject hexInfo=new JSONObject();
            hexInfo.put("hex_id",hex).put("terrain",raw.optString("terrain_primary","")).put("road",raw.optInt("road_present",0)==1);
            try{hexInfo.put("neighbors",new JSONObject(raw.optString("neighbors_json","{}")));}catch(Exception ignored){hexInfo.put("neighbors",new JSONObject());}
            out.put("hex",hexInfo);
            JSONArray regions=new JSONArray();for(JSONObject area:db.getMapAreasForHex(packageId,hex)){regions.put(new JSONObject().put("id",area.optString("area_id")).put("name",area.optString("name")).put("type",area.optString("type")));}out.put("regions",regions);
            JSONArray pois=new JSONArray();
            for(JSONObject c:db.getMapLocationsForHex(packageId,hex)){
                if(c.optString("hex_id","").length()==0)continue;
                pois.put(new JSONObject().put("id",c.optString("location_id")).put("name",c.optString("name")).put("type",c.optString("type")).put("source","CANONICAL").put("read_only",true));
            }
            for(JSONObject p:db.getCampaignPoisForHex(logical,hex,includeHidden)){
                boolean hidden=p.optInt("hidden",0)==1,discovered=p.optInt("discovered",0)==1;
                if(!includeHidden&&(hidden||!discovered))continue;
                JSONObject q=new JSONObject().put("id",p.optString("id")).put("name",p.optString("name")).put("type",p.optString("type")).put("source","procedural".equalsIgnoreCase(p.optString("origin"))?"PROCEDURAL":"CAMPAIGN").put("status",p.optString("status")).put("parent_poi_id",p.optString("parent_poi_id"));
                if(includeHidden)q.put("hidden",hidden).put("discovered",discovered);
                pois.put(q);
            }
            out.put("pois",pois);
            out.put("visibility",includeHidden?"master":"public");
        } catch(Exception e){AppLog.w("AtlasContextProjector","Falha ao projetar Atlas.",e);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
