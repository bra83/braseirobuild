package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/** One-time adapter for the exact historical bundled V2 ZIP, not arbitrary legacy maps. */
final class LegacyFaerunV2Atlas {
    static final String PACKAGE_ID = "faerun-v2-legacy-adapted-960ac6a9";
    private static final String SOURCE_BLOB = "960ac6a9195ca26d0dce45e0480b0e73feeddd6b";
    private static final String LOGICAL = "forgotten_realms:faerun";
    private static final String FRAME = "faerun_v2_source_960ac6a9";
    private LegacyFaerunV2Atlas() {}

    static boolean matchesSource(Uri uri) throws Exception {
        if (uri == null || !"file".equals(uri.getScheme()) || uri.getPath() == null) return false;
        File file = new File(uri.getPath());
        if (!file.isFile() || file.length() != 102373336L) return false;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + file.length() + "\0").getBytes(StandardCharsets.UTF_8));
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] b = new byte[131072]; int n;
            while ((n = in.read(b)) != -1) digest.update(b, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format(Locale.US,"%02x",b & 255));
        return SOURCE_BLOB.equals(hex.toString());
    }

    private static final class Row {
        final String id; final double x,y; final String neighbors;
        Row(String id,double x,double y,String neighbors){this.id=id;this.x=x;this.y=y;this.neighbors=neighbors;}
    }

    static AtlasMapProfile adapt(SQLiteDatabase db,String id,JSONObject manifest,MapManifestGeometry g,String image,int expected) throws Exception {
        if (!PACKAGE_ID.equals(id) || !"dnd".equals(g.getWorldProfile()) || !"flat-top".equals(g.getOrientation())
                || expected != g.size() || g.getImageWidth() <= 0 || g.getImageHeight() <= 0)
            throw new IllegalStateException("Geometria V2 não corresponde ao contrato de migração.");
        ArrayList<Row> rows = new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT hex_id,center_x,center_y,neighbors_json FROM map_hex_atlas WHERE package_id=?",new String[]{id})){
            while(c.moveToNext()) rows.add(new Row(c.getString(0),c.getDouble(1),c.getDouble(2),c.getString(3)));
        }
        if(rows.size()!=expected) throw new IllegalStateException("Índice V2 incompleto.");
        double miles=g.getHexScaleMiles();
        if(!(miles>0)){
            JSONObject layout=manifest.optJSONObject("layout");
            JSONObject distance=layout==null?null:layout.optJSONObject("distancePerHex");
            if(distance!=null && distance.optString("unit").toLowerCase(Locale.ROOT).contains("milha")) miles=distance.optDouble("value");
        }
        if(!(miles>0)) throw new IllegalStateException("Distância por hex ausente no manifesto V2.");
        int locations=count(db,"SELECT COUNT(*) FROM map_locations WHERE package_id=?",id);
        int positioned=count(db,"SELECT COUNT(*) FROM map_locations l WHERE l.package_id=? AND EXISTS (SELECT 1 FROM map_hex_atlas h WHERE h.package_id=l.package_id AND h.hex_id=l.hex_id)",id);
        int areas=count(db,"SELECT COUNT(*) FROM map_areas WHERE package_id=?",id);
        JSONObject profile=new JSONObject()
            .put("schemaVersion","1").put("logical_map_id",LOGICAL).put("map_package_id",id)
            .put("atlas_namespace","faerun").put("coordinate_system",new JSONObject().put("id",FRAME))
            .put("world_profile","dnd:forgotten_realms").put("setting",g.getSetting())
            .put("image",new JSONObject().put("file",image).put("width",Math.round(g.getImageWidth())).put("height",Math.round(g.getImageHeight())))
            .put("grid",new JSONObject().put("type","hex").put("orientation","flat-top").put("offset_layout","odd-q")
                .put("hex_id_policy","excel-column-row").put("origin",new JSONObject().put("x",g.getOriginX()).put("y",g.getOriginY()))
                .put("side_length",g.getHexWidth()/2).put("hex_width",g.getHexWidth()).put("hex_height",g.getHexHeight())
                .put("column_step_x",g.getStepX()).put("row_step_y",g.getStepY()).put("odd_column_y_offset",g.getAlternateColumnOffsetY())
                .put("columns",g.getColumns()).put("hex_count",expected).put("distance_per_hex",new JSONObject().put("value",miles).put("unit","milhas")))
            .put("canonical_catalog",new JSONObject().put("version","legacy-v2-960ac6a9").put("read_only",true).put("locations",locations).put("positioned",positioned).put("areas_multihex",areas))
            .put("position_semantics",new JSONObject().put("empty_hex_is_valid",true))
            .put("compatibility",new JSONObject().put("same_coordinate_frame_can_use_uv_directly",true).put("different_coordinate_frame_requires_validated_transform",true))
            .put("provenance",new JSONObject().put("source_git_blob",SOURCE_BLOB).put("adapter","legacy_v2_verified_manifest_and_index"));
        AtlasMapProfile result=AtlasMapProfile.parse(profile.toString());
        manifest.put("logical_map_id",LOGICAL).put("map_package_id",id).put("coordinate_frame_id",FRAME).put("canonical_catalog_version",result.catalogVersion);
        JSONObject contract=new JSONObject().put("layers",new JSONObject().put("CANONICAL",new JSONObject().put("read_only",true))).put("source_git_blob",SOURCE_BLOB);
        JSONObject index=new JSONObject().put("logical_map_id",LOGICAL).put("map_package_id",id).put("canonical_catalog_version",result.catalogVersion)
            .put("counts",new JSONObject().put("hexes",expected).put("locations",locations).put("positionedLocations",positioned));
        result.validateAgainst(manifest,index,contract);
        db.beginTransaction();
        try(SQLiteStatement update=db.compileStatement("UPDATE map_hex_atlas SET center_u=?,center_v=?,neighbors_json=? WHERE package_id=? AND hex_id=?")){
            for(Row row:rows){
                MapManifestGeometry.ManifestHex hex=g.byId(row.id);
                if(hex==null || !Double.isFinite(row.x) || !Double.isFinite(row.y)
                        || Math.abs(row.x-hex.centerX)>.02 || Math.abs(row.y-hex.centerY)>.02
                        || row.x<0 || row.y<0 || row.x>g.getImageWidth() || row.y>g.getImageHeight())
                    throw new IllegalStateException("Centro V2 diverge do manifesto: "+row.id);
                JSONObject neighbors=new JSONObject();
                HashSet<String> valid=new HashSet<>(g.neighbors(row.id));
                for(String other:valid){
                    MapManifestGeometry.ManifestHex n=g.byId(other);
                    String direction=n.column==hex.column?(n.centerY<hex.centerY?"N":"S"):
                            (n.centerX>hex.centerX?(n.centerY<hex.centerY?"NE":"SE"):(n.centerY<hex.centerY?"NW":"SW"));
                    if(neighbors.has(direction))throw new IllegalStateException("Vizinhança V2 ambígua: "+row.id);
                    neighbors.put(direction,other);
                }
                JSONObject original=new JSONObject(row.neighbors==null?"{}":row.neighbors);
                java.util.Iterator<String> keys=original.keys();
                while(keys.hasNext()){
                    String key=keys.next();String neighbor=original.optString(key,"");
                    if(!neighbor.isEmpty()&&!"null".equalsIgnoreCase(neighbor)&&!valid.contains(neighbor))
                        throw new IllegalStateException("Vizinhança declarada diverge do manifesto: "+row.id);
                }
                update.bindDouble(1,row.x/g.getImageWidth());update.bindDouble(2,row.y/g.getImageHeight());
                update.bindString(3,neighbors.toString());update.bindString(4,id);update.bindString(5,row.id);
                if(update.executeUpdateDelete()!=1)throw new IllegalStateException("Hex V2 não persistido: "+row.id);
            }
            ContentValues v=new ContentValues();v.put("logical_map_id",LOGICAL);v.put("map_package_id",id);
            v.put("coordinate_frame_id",FRAME);v.put("atlas_namespace",result.atlasNamespace);v.put("canonical_catalog_version",result.catalogVersion);
            v.put("atlas_profile_json",profile.toString());v.put("atlas_contract_json",contract.toString());v.put("canonical_index_json",index.toString());v.put("manifest_json",manifest.toString());
            if(db.update("map_packages",v,"package_id=?",new String[]{id})!=1)throw new IllegalStateException("Metadados V2 não persistidos.");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return result;
    }
    private static int count(SQLiteDatabase db,String sql,String id){
        try(Cursor c=db.rawQuery(sql,new String[]{id})){if(!c.moveToFirst())throw new IllegalStateException("Contagem indisponível");return c.getInt(0);}
    }
}
