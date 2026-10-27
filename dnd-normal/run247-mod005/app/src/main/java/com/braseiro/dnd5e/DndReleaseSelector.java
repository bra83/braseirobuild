package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Pure release filtering for the multi-VTT GitHub repository. */
final class DndReleaseSelector {
    private DndReleaseSelector(){}

    static JSONObject firstInstallable(JSONArray releases){
        if(releases==null)return null;
        for(int i=0;i<releases.length();i++){
            JSONObject release=releases.optJSONObject(i);
            if(release==null||release.optBoolean("draft",false)||release.optBoolean("prerelease",false))continue;
            if(!release.optString("tag_name","").startsWith("dnd-"))continue;
            JSONArray assets=release.optJSONArray("assets");
            if(assets==null)continue;
            String apkName="";
            for(int j=0;j<assets.length();j++){
                JSONObject asset=assets.optJSONObject(j);
                if(asset==null)continue;
                String name=asset.optString("name","");
                if(name.startsWith("BRASEIRO_DND_")&&name.endsWith(".apk")){
                    apkName=name;
                    break;
                }
            }
            if(apkName.isEmpty())continue;
            for(int j=0;j<assets.length();j++){
                JSONObject asset=assets.optJSONObject(j);
                if(asset!=null&&(apkName+".sha256").equals(asset.optString("name",""))){
                    return release;
                }
            }
        }
        return null;
    }
}
