package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Pure release filtering for the multi-VTT GitHub repository. */
final class DndReleaseSelector {
    private DndReleaseSelector(){}

    /** A missing or malformed proof is NOT evidence that a release is newer. */
    static long verifiedCode(String proof, String packageName) {
        if(proof==null||packageName==null)return -1;
        long code=-1;
        boolean packageMatches=false;
        String[] lines=proof.split("\\r?\\n");
        for(String line:lines){
            if(line.startsWith("PACKAGE="))packageMatches=packageName.equals(line.substring(8).trim());
            if(line.startsWith("VERSION_CODE=")||line.startsWith("NEW_CODE=")){
                String digits=line.substring(line.indexOf('=')+1).trim();
                if(!digits.matches("[0-9]{1,10}"))return -1;
                try{
                    code=Long.parseLong(digits);
                }catch(NumberFormatException ignored){return -1;}
            }
        }
        return packageMatches&&code>0&&code<=2100000000L?code:-1;
    }

    static String proofUrl(JSONObject release) {
        if(release==null)return "";
        JSONArray assets=release.optJSONArray("assets");
        for(int i=0;assets!=null&&i<assets.length();i++){
            JSONObject asset=assets.optJSONObject(i);
            if(asset!=null&&"BUILD_PROOF.txt".equals(asset.optString("name","")))
                return asset.optString("browser_download_url","");
        }
        return "";
    }

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
