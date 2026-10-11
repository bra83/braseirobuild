package com.braseiro.dnd5e;

import android.content.Context;
import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only catalog for the complete embedded Strixhaven VTT asset package. */
public final class StrixhavenAssetCatalog {
    public static final String ROOT="strixhaven/vtt_pack/";
    private final AssetManager assets;
    private JSONObject manifest;

    public StrixhavenAssetCatalog(Context context){assets=context.getApplicationContext().getAssets();}

    private synchronized JSONObject manifest(){
        if(manifest!=null)return manifest;
        try(InputStream in=assets.open(ROOT+"manifest.json")){manifest=new JSONObject(readText(in));}
        catch(Exception e){AppLog.e("StrixhavenAssets","Falha ao abrir manifest embutido.",e);manifest=new JSONObject();}
        return manifest;
    }

    public int assetCount(){return manifest().optInt("individual_assets_count",0);}
    public int mapCount(){JSONArray a=manifest().optJSONArray("maps");return a==null?0:a.length();}
    public int sourceSheetCount(){return manifest().optInt("source_sheets_count",0);}
    public boolean isComplete(){return assetCount()==586&&mapCount()==7&&sourceSheetCount()==28;}

    public int matchingCount(String filter){String q=filter==null?"":filter.trim().toLowerCase(Locale.US);JSONArray a=manifest().optJSONArray("assets");if(a==null)return 0;int count=0;for(int i=0;i<a.length();i++){JSONObject src=a.optJSONObject(i);if(src==null)continue;String hay=(src.optString("asset_id")+" "+src.optString("file")+" "+src.optString("source_sheet")).toLowerCase(Locale.US);if(q.length()==0||hay.contains(q))count++;}return count;}

    public List<JSONObject> list(String filter,int limit){/*BR_JSON_WRAP*/try{
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();String q=filter==null?"":filter.trim().toLowerCase(Locale.US);JSONArray a=manifest().optJSONArray("assets");if(a==null)return out;
        int max=Math.max(1,Math.min(200,limit));for(int i=0;i<a.length()&&out.size()<max;i++){JSONObject src=a.optJSONObject(i);if(src==null)continue;String id=src.optString("asset_id");String file=src.optString("file");String sheet=src.optString("source_sheet");String hay=(id+" "+file+" "+sheet).toLowerCase(Locale.US);if(q.length()>0&&!hay.contains(q))continue;try{JSONObject row=new JSONObject(src.toString());row.put("asset_path",ROOT+file);out.add(row);}catch(Exception ignored){AppLog.ignored("StrixhavenAssets","Entrada inválida ignorada.",ignored);}}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public InputStream open(JSONObject row)throws Exception{String path=row==null?"":row.optString("asset_path");if(path.length()==0)throw new Exception("Asset Strixhaven sem caminho.");return assets.open(path);}

    public JSONObject summary(){/*BR_JSON_WRAP*/try{JSONObject out=new JSONObject();try{out.put("root",ROOT).put("assets",assetCount()).put("maps",mapCount()).put("source_sheets",sourceSheetCount()).put("complete",isComplete());}catch(Exception ignored){AppLog.ignored("StrixhavenAssets","Resumo parcial.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String readText(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return new String(out.toByteArray(),"UTF-8");}
}
