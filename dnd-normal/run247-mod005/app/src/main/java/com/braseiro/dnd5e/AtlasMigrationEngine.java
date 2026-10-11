package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Migração de posição dirigida exclusivamente pelos perfis do pacote. */
public final class AtlasMigrationEngine {
    private AtlasMigrationEngine() {}

    public static final class PositionResolution {
        public final String hexId;
        public final double u;
        public final double v;
        public final boolean hasPosition;
        public final String status;
        public final String legacyJson;
        PositionResolution(String hexId,double u,double v,boolean hasPosition,String status,String legacyJson){this.hexId=hexId==null?"":hexId;this.u=u;this.v=v;this.hasPosition=hasPosition;this.status=status==null?"ready":status;this.legacyJson=legacyJson==null?"{}":legacyJson;}
    }

    public static PositionResolution resolveCampaignPosition(AppDb db,JSONObject previous,JSONObject previousPackage,AtlasMapProfile target,JSONObject migrationProfiles,String targetPackageId,MapManifestGeometry geometry) throws Exception {/*BR_JSON_WRAP*/try{
        if(previous==null)previous=new JSONObject();if(previousPackage==null)previousPackage=new JSONObject();
        String previousPackageId=previous.optString("map_package_id",previousPackage.optString("package_id",""));
        String previousLogical=previous.optString("logical_map_id",previousPackage.optString("logical_map_id",previousPackage.optString("map_id","")));
        String previousFrame=previous.optString("coordinate_frame_id",previousPackage.optString("coordinate_frame_id",""));
        String previousHex=previous.optString("current_hex_id","");
        JSONObject legacy=new JSONObject();legacy.put("logical_map_id",previousLogical).put("map_package_id",previousPackageId).put("coordinate_frame_id",previousFrame).put("hex_id",previousHex).put("token_x",previous.optDouble("token_x",.5)).put("token_y",previous.optDouble("token_y",.5));

        if(previousPackageId.length()==0&&previousHex.length()==0)return new PositionResolution("",.5,.5,false,"needs_initial_position",legacy.toString());
        if(previousLogical.length()>0&&!previousLogical.equals(target.logicalMapId)&&!sourceDeclaredForTarget(migrationProfiles,previousPackageId,previousFrame,target.mapPackageId,target.coordinateFrameId))return new PositionResolution("",.5,.5,false,"different_logical_map",legacy.toString());

        if(target.coordinateFrameId.equals(previousFrame)&&previous.has("position_u")&&!previous.isNull("position_u")&&previous.has("position_v")&&!previous.isNull("position_v")){
            double u=clamp(previous.optDouble("position_u")),v=clamp(previous.optDouble("position_v"));MapManifestGeometry.ManifestHex h=geometry.nearest(target.uToX(u),target.vToY(v));return h==null?pending(legacy,"migration_pending"):new PositionResolution(h.id,u,v,true,"ready",legacy.toString());
        }

        String previousDeclaredPackage=previousPackage.optString("map_package_id",previousPackage.optString("map_id",previousPackageId));
        JSONObject migration=findMigration(migrationProfiles,previousDeclaredPackage,previousFrame,target.mapPackageId,target.coordinateFrameId);
        if(migration!=null&&migration.optString("status","").startsWith("VALIDADO")&&migration.optJSONObject("transform")!=null){
            JSONObject transform=migration.optJSONObject("transform");JSONArray matrix=transform.optJSONArray("matrix");if(matrix!=null&&matrix.length()==3){double width=previousPackage.optDouble("image_width",0),height=previousPackage.optDouble("image_height",0);if(width<=0||height<=0)throw new Exception("Pacote legado sem dimensões para transformação validada.");double x=clamp(previous.optDouble("token_x",.5))*width,y=clamp(previous.optDouble("token_y",.5))*height;double[] xy=homography(matrix,x,y);MapManifestGeometry.ManifestHex h=geometry.nearest((float)xy[0],(float)xy[1]);if(h!=null&&xy[0]>=0&&xy[1]>=0&&xy[0]<=target.imageWidth&&xy[1]<=target.imageHeight)return new PositionResolution(h.id,target.xToU(xy[0]),target.yToV(xy[1]),true,"ready",legacy.toString());}
        }

        // Fonte sem transformação validada: somente identidade canônica comprovada.
        if(isExplicitlyUnvalidated(migration)){
            String sceneLocation=db.getSceneState().optString("location","");JSONObject canonical=db.findCanonicalLocationByIdentity(targetPackageId,sceneLocation);if(canonical.length()>0&&canonical.optString("hex_id","").length()>0){String h=canonical.optString("hex_id");double u=canonical.optDouble("position_u",db.getRawMapHex(targetPackageId,h).optDouble("center_u",.5));double v=canonical.optDouble("position_v",db.getRawMapHex(targetPackageId,h).optDouble("center_v",.5));return new PositionResolution(h,u,v,true,"ready_identity_reconciled",legacy.toString());}
            return pending(legacy,"migration_pending");
        }
        return pending(legacy,"migration_pending");
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Migra POIs mutáveis da campanha somente por evidência cartográfica explícita.
     * Nunca usa nome/identidade de CampaignPoi como substituto de coordenada.
     */
    public static JSONObject migrateCampaignPois(AppDb db,AtlasMapProfile target,JSONObject migrationProfiles,String targetPackageId) throws Exception {/*BR_JSON_WRAP*/try{
        JSONObject report=new JSONObject();int migrated=0,pending=0,unchanged=0;JSONArray pendingIds=new JSONArray();
        for(JSONObject poi:db.listCampaignPois(target.logicalMapId,true,5000)){
            String id=poi.optString("id","");if(id.length()==0)continue;String sourcePackage=poi.optString("map_package_id","");String sourceFrame=poi.optString("coordinate_frame_id","");String oldHex=poi.optString("hex_id","");
            Double u=nullableDouble(poi,"position_u"),v=nullableDouble(poi,"position_v");
            if(targetPackageId.equals(sourcePackage)&&db.isValidMapHex(targetPackageId,oldHex)){unchanged++;continue;}
            JSONObject legacy=new JSONObject().put("map_package_id",sourcePackage).put("coordinate_frame_id",sourceFrame).put("hex_id",oldHex);if(u!=null)legacy.put("u",u);if(v!=null)legacy.put("v",v);String existingLegacy=poi.optString("legacy_position_json","");if(existingLegacy.length()>2)legacy.put("previous_legacy",existingLegacy);
            if(target.coordinateFrameId.equals(sourceFrame)&&u!=null&&v!=null){JSONObject h=db.nearestMapHexByNormalized(targetPackageId,u,v);String newHex=h.optString("hex_id","");if(newHex.length()>0){db.updateCampaignPoiPlacement(id,targetPackageId,target.coordinateFrameId,newHex,u,v,"ready_uv_same_frame",legacy.toString());migrated++;continue;}}
            JSONObject migration=findMigration(migrationProfiles,sourcePackage,sourceFrame,target.mapPackageId,target.coordinateFrameId);
            if(migration!=null&&migration.optString("status","").startsWith("VALIDADO")&&migration.optJSONObject("transform")!=null){
                double[] sourceXY=sourcePixel(db,sourcePackage,oldHex,u,v);JSONArray matrix=migration.optJSONObject("transform").optJSONArray("matrix");if(sourceXY!=null&&matrix!=null&&matrix.length()==3){double[] xy=homography(matrix,sourceXY[0],sourceXY[1]);if(xy[0]>=0&&xy[1]>=0&&xy[0]<=target.imageWidth&&xy[1]<=target.imageHeight){double nu=target.xToU(xy[0]),nv=target.yToV(xy[1]);JSONObject h=db.nearestMapHexByNormalized(targetPackageId,nu,nv);String newHex=h.optString("hex_id","");if(newHex.length()>0){db.updateCampaignPoiPlacement(id,targetPackageId,target.coordinateFrameId,newHex,nu,nv,"ready_validated_transform",legacy.toString());migrated++;continue;}}}
            }
            db.updateCampaignPoiPlacement(id,targetPackageId,target.coordinateFrameId,"",null,null,"migration_pending",legacy.toString());pending++;pendingIds.put(id);
        }
        return report.put("migrated",migrated).put("pending",pending).put("unchanged",unchanged).put("pending_ids",pendingIds);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static Double nullableDouble(JSONObject o,String key){return o.has(key)&&!o.isNull(key)?Double.valueOf(o.optDouble(key,Double.NaN)):null;}
    private static double[] sourcePixel(AppDb db,String sourcePackage,String oldHex,Double u,Double v){JSONObject p=db.getMapPackageAny(sourcePackage);double w=p.optDouble("image_width",0),h=p.optDouble("image_height",0);if(w<=0||h<=0)return null;if(u!=null&&v!=null&&!Double.isNaN(u)&&!Double.isNaN(v))return new double[]{clamp(u)*w,clamp(v)*h};JSONObject old=db.getRawMapHex(sourcePackage,oldHex);if(old.length()>0)return new double[]{old.optDouble("center_x"),old.optDouble("center_y")};return null;}

    public static JSONObject findMigration(JSONObject root,String sourcePackage,String sourceFrame,String targetPackage,String targetFrame){JSONArray profiles=root==null?null:root.optJSONArray("profiles");if(profiles==null)return null;for(int i=0;i<profiles.length();i++){JSONObject p=profiles.optJSONObject(i);if(p==null)continue;boolean source=(sourcePackage.length()>0&&sourcePackage.equals(p.optString("source_map_package_id")))||(sourceFrame.length()>0&&sourceFrame.equals(p.optString("source_coordinate_frame_id")));boolean target=targetPackage.equals(p.optString("target_map_package_id"))&&targetFrame.equals(p.optString("target_coordinate_frame_id"));if(source&&target)return p;}return null;}
    private static boolean isExplicitlyUnvalidated(JSONObject migration){if(migration==null)return false;String status=migration.optString("status","").toUpperCase(java.util.Locale.ROOT);return (status.contains("NAO_VALIDADO")||status.contains("NÃO_VALIDADO")||status.contains("UNVALIDATED"))&&migration.optJSONObject("transform")==null;}
    public static boolean sourceDeclaredForTarget(JSONObject root,String sourcePackage,String sourceFrame,String targetPackage,String targetFrame){return findMigration(root,sourcePackage==null?"":sourcePackage,sourceFrame==null?"":sourceFrame,targetPackage,targetFrame)!=null;}
    private static PositionResolution pending(JSONObject legacy,String status){return new PositionResolution("",.5,.5,false,status,legacy.toString());}
    private static double[] homography(JSONArray m,double x,double y)throws Exception{double m00=m.getJSONArray(0).getDouble(0),m01=m.getJSONArray(0).getDouble(1),m02=m.getJSONArray(0).getDouble(2),m10=m.getJSONArray(1).getDouble(0),m11=m.getJSONArray(1).getDouble(1),m12=m.getJSONArray(1).getDouble(2),m20=m.getJSONArray(2).getDouble(0),m21=m.getJSONArray(2).getDouble(1),m22=m.getJSONArray(2).getDouble(2);double w=m20*x+m21*y+m22;if(Math.abs(w)<1e-12)throw new Exception("Homografia inválida: divisor zero.");return new double[]{(m00*x+m01*y+m02)/w,(m10*x+m11*y+m12)/w};}
    private static double clamp(double v){return v<0?0:(v>1?1:v);}
}
