package com.braseiro.dnd5e;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.List;

/** World-backed commercial and ecological stock; never grants ingredients from scene prose. */
public final class WilemWorldSupply {
    private WilemWorldSupply(){}
    public static void ensureSchema(SQLiteDatabase sql){
        sql.execSQL("CREATE TABLE IF NOT EXISTS wilem_vendor_sales ("+
            "world_profile TEXT NOT NULL, source_key TEXT NOT NULL, stock_revision TEXT NOT NULL,"+
            "ingredient_id TEXT NOT NULL, sold INTEGER NOT NULL DEFAULT 0,"+
            "PRIMARY KEY(world_profile,source_key,stock_revision,ingredient_id))");
    }
    private static JSONObject obj(Object raw){
        if(raw instanceof JSONObject)return (JSONObject)raw;
        try{return new JSONObject(raw==null?"{}":String.valueOf(raw));}
        catch(Exception ignored){return new JSONObject();}
    }
    private static JSONArray arr(Object raw){
        if(raw instanceof JSONArray)return (JSONArray)raw;
        try{return new JSONArray(raw==null?"[]":String.valueOf(raw));}
        catch(Exception ignored){return new JSONArray();}
    }
    private static String normalize(String raw){
        String t=Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
        return t.replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static boolean containsWord(String text,String value){
        String t=" "+normalize(text)+" ",v=" "+normalize(value)+" ";
        return !v.trim().isEmpty()&&t.contains(v);
    }
    private static JSONObject mapCell(AppDb db){
        JSONObject map=db.getMapState(),scene=db.getSceneState();
        String pkg=map.optString("map_package_id","");
        String hex=scene.optString("hex_id","");
        if(hex.isEmpty())hex=map.optString("current_hex_id","");
        if(pkg.isEmpty()||hex.isEmpty())return new JSONObject();
        JSONObject cell=db.getMapHex(pkg,hex);
        if(cell.length()==0)return new JSONObject();
        try{return new JSONObject().put("package_id",pkg).put("hex_id",hex)
            .put("terrain",cell.optString("terrain_primary","")+" "+
                cell.optString("terrain_secondary",""))
            .put("weather",scene.optString("weather",""));}
        catch(Exception e){return new JSONObject();}
    }
    private static String sourceRevision(JSONObject stock){
        String revision=stock.optString("revision","");
        if(revision.trim().isEmpty())revision="initial";
        if(revision.length()>128)throw new IllegalArgumentException("Revisao de estoque invalida");
        return revision;
    }
    private static void addOffers(AppDb db,JSONArray out,String sourceKey,String seller,
        JSONObject manifest,JSONObject npc){
        JSONArray rows=manifest.optJSONArray("wilem_market_stock");
        if(rows==null)return;
        String revision=sourceRevision(manifest);
        SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String ing=row.optString("ingredient_id","");
            JSONObject ingredient=WilemCookbookCatalog.ingredientById(ing);
            if(ingredient.length()==0||"monstro".equals(ingredient.optString("category"))
                ||"magico".equals(ingredient.optString("category")))continue;
            int capacity=row.optInt("quantity",-1),cost=row.optInt("price_cp",-1);
            if(capacity<=0||capacity>10000||cost<0||cost>1000000)continue;
            int sold=sold(sql,db.getActiveMapProfile(),sourceKey,revision,ing);
            int left=Math.max(0,capacity-sold);
            if(left==0)continue;
            try{out.put(new JSONObject().put("source_key",sourceKey)
                .put("seller",seller).put("stock_revision",revision)
                .put("ingredient_id",ing).put("name",ingredient.optString("name"))
                .put("price_cp",cost).put("quantity",left));}
            catch(Exception e){throw new IllegalStateException("Oferta de comercio invalida",e);}
        }
    }
    public static JSONArray marketOffers(AppDb db){
        JSONArray out=new JSONArray();
        JSONArray npcs=db.listSceneNpcs(200);
        for(int i=0;i<npcs.length();i++){
            JSONObject seen=npcs.optJSONObject(i);if(seen==null)continue;
            JSONObject npc=db.findNpcMemoryByName(seen.optString("name"));
            if(npc.length()==0||npc.optInt("active",0)!=1)continue;
            JSONObject stock=obj(npc.optString("stats_json","{}"));
            // The merchant catalog is an explicit persistent NPC stat, never inferred from a profession.
            String key=npc.optString("stable_id","").trim();
            if(key.isEmpty())key="npc-id-"+npc.optLong("id");
            addOffers(db,out,"npc:"+key,npc.optString("name"),stock,npc);
        }
        JSONObject cell=mapCell(db);
        if(cell.length()>0){
            // A location shares a hex with many other POIs. A shop in the same
            // region is NOT a shop the character is currently visiting.
            String currentPlace=NpcIdentityNormalizer.key(db.getSceneState().optString("location",""));
            if(!currentPlace.isEmpty())for(JSONObject poi:db.getMapLocationsForHex(
                    cell.optString("package_id"),cell.optString("hex_id"))){
                String placement=poi.optString("placement_status","").toLowerCase(Locale.ROOT);
                if(placement.contains("review")||placement.contains("approx"))continue;
                String id=poi.optString("location_id","").trim();
                String registeredName=NpcIdentityNormalizer.key(poi.optString("name",""));
                // Exact named location or its stable POI ID. A sub-string, the
                // map hex alone, or an unrelated merchant never authorizes sale.
                if(id.isEmpty()||(!currentPlace.equals(registeredName)&&
                    !currentPlace.equals(NpcIdentityNormalizer.key(id))))continue;
                JSONObject raw=obj(poi.optString("raw_json","{}"));
                addOffers(db,out,"poi:"+cell.optString("package_id")+":"+id,
                    poi.optString("name",id),raw,null);
            }
        }
        return out;
    }
    public static JSONObject offer(AppDb db,String sourceKey,String ingredientId){
        JSONArray a=marketOffers(db);
        JSONObject found=new JSONObject();
        for(int i=0;i<a.length();i++){
            JSONObject row=a.optJSONObject(i);
            if(row!=null&&sourceKey.equals(row.optString("source_key"))
                &&ingredientId.equals(row.optString("ingredient_id"))){
                if(found.length()>0)throw new IllegalStateException("Catalogo do vendedor duplica ingrediente");
                found=row;
            }
        }
        return found;
    }
    public static int sold(SQLiteDatabase sql,String profile,String source,String revision,String ing){
        ensureSchema(sql);
        try(Cursor c=sql.rawQuery("SELECT sold FROM wilem_vendor_sales "+
            "WHERE world_profile=? AND source_key=? AND stock_revision=? AND ingredient_id=?",
            new String[]{profile,source,revision,ing})){
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    public static void sell(SQLiteDatabase sql,String profile,JSONObject offer,int qty)throws Exception{
        if(offer.length()==0||qty<=0||qty>offer.optInt("quantity",0))
            throw new IllegalStateException("Oferta inexistente ou sem estoque suficiente");
        ensureSchema(sql);
        String source=offer.getString("source_key"),rev=offer.getString("stock_revision"),
            ing=offer.getString("ingredient_id");
        int current=sold(sql,profile,source,rev,ing);
        sql.execSQL("INSERT OR IGNORE INTO wilem_vendor_sales "+
            "(world_profile,source_key,stock_revision,ingredient_id,sold) VALUES(?,?,?,?,0)",
            new Object[]{profile,source,rev,ing});
        sql.execSQL("UPDATE wilem_vendor_sales SET sold=? WHERE "+
            "world_profile=? AND source_key=? AND stock_revision=? AND ingredient_id=? AND sold=?",
            new Object[]{current+qty,profile,source,rev,ing,current});
        if(sold(sql,profile,source,rev,ing)!=current+qty)
            throw new IllegalStateException("Estoque do vendedor nao foi debitado");
    }
    private static boolean rawHabitat(String raw,String desired){
        String hay=normalize(raw),want=normalize(desired);
        if(want.isEmpty()||want.equals("mercado")||want.equals("feira")||
            want.equals("mercador")||want.equals("moinho")||want.equals("apicultor"))return false;
        String[] aliases;
        switch(want){
            case "floresta":aliases=new String[]{"floresta","forest","woods","woodland"};break;
            case "prado":aliases=new String[]{"prado","grassland","meadow","planicie"};break;
            case "rio":aliases=new String[]{"rio","river","stream"};break;
            case "lago":aliases=new String[]{"lago","lake"};break;
            case "horta":aliases=new String[]{"horta","garden","vegetable garden"};break;
            case "pomar":aliases=new String[]{"pomar","orchard"};break;
            case "fazenda":aliases=new String[]{"fazenda","farm","farmland"};break;
            case "cacada":aliases=new String[]{"floresta","forest","grassland","prado"};break;
            default:aliases=new String[]{want};
        }
        for(String alias:aliases)if(containsWord(hay,alias))return true;
        return false;
    }
    private static JSONObject sceneForageNode(AppDb db,String ingredientId){
        JSONObject scene=db.getSceneState();
        JSONArray nodes=arr(scene.optString("clues_json","[]"));
        for(int i=0;i<nodes.length();i++){
            JSONObject n=nodes.optJSONObject(i);
            if(n!=null&&"wilem_forage_node".equals(n.optString("type"))&&
                ingredientId.equals(n.optString("ingredient_id"))&&
                n.optInt("quantity",0)>0&&n.optString("id").trim().length()>=3&&
                (n.optString("location").isEmpty()||n.optString("location").equals(scene.optString("location"))))
                return n;
        }
        return new JSONObject();
    }
    public static JSONObject forageOffer(AppDb db,JSONObject ing){
        if(ing==null||ing.length()==0||ing.optInt("tier",1)>0||
            "monstro".equals(ing.optString("category"))||
            "magico".equals(ing.optString("category")))return new JSONObject();
        JSONObject scene=db.getSceneState(),cell=mapCell(db);
        int day=scene.optInt("day_number",1);
        String id=ing.optString("id");
        JSONObject explicit=sceneForageNode(db,id);
        boolean fromNode=explicit.length()>0;
        if(!fromNode&&cell.length()==0)return new JSONObject();
        String terrain=cell.optString("terrain","");
        boolean habitat=false;
        if(!fromNode){
            for(String raw:ing.optString("source_locations","").split(","))
                if(rawHabitat(terrain,raw)){habitat=true;break;}
            if(!habitat)return new JSONObject();
        }
        // Extreme weather closes normal foraging; structured encounter rewards remain distinct.
        String weather=normalize(scene.optString("weather",""));
        if(!fromNode&&(containsWord(weather,"nevasca")||containsWord(weather,"blizzard")||
            containsWord(weather,"tempestade")||containsWord(weather,"storm")))return new JSONObject();
        String source=fromNode?"node:"+explicit.optString("id"):
            "atlas:"+cell.optString("package_id")+":"+cell.optString("hex_id");
        int available=fromNode?explicit.optInt("quantity",0):3;
        // The node's lifetime is its current scene; atlas ecology renews once per world day.
        String revision=fromNode?explicit.optString("revision","initial"):"day:"+day;
        SQLiteDatabase sql=db.getWritableDatabase();
        int already=sold(sql,db.getActiveMapProfile(),source,revision,"forage:"+id);
        int remaining=Math.max(0,available-already);
        if(remaining==0)return new JSONObject();
        try{return new JSONObject().put("ingredient_id",id).put("name",ing.optString("name"))
            .put("source_key",source).put("stock_revision",revision)
            .put("quantity",remaining).put("dc",fromNode?explicit.optInt("dc",12):12)
            .put("source_type",fromNode?"scene_node":"mapped_habitat")
            .put("location",scene.optString("location"));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void consumeForage(AppDb db,JSONObject offer,int amount)throws Exception{
        if(amount<=0||amount>offer.optInt("quantity",0))throw new IllegalStateException("Recurso natural esgotado");
        JSONObject sale=new JSONObject(offer.toString()).put("ingredient_id",
            "forage:"+offer.getString("ingredient_id"));
        sell(db.getWritableDatabase(),db.getActiveMapProfile(),sale,amount);
    }
}
