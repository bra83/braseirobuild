package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fail-closed dependency audit for Monster Manual (2025/XMM) statblocks referenced by
 * already-compiled official-book content. This class never fabricates a statblock: it
 * only exposes an XMM dependency when CompiledStatblockEngine already has an authorized
 * source-bound record for it.
 */
public final class XmmDependencyRuntimeEngine {
    public static final String SOURCE_ID="XMM";
    private XmmDependencyRuntimeEngine(){}

    /* Verified from 5etools XMM referenceSources entries pointing at NF. The registry is
       intentionally dependency-scoped, not a claim that the whole XMM is implemented. */
    private static final String[] NF_REFERENCED={
        "Archpriest","Basilisk","Beholder Zombie","Black Pudding","Ettin","Flameskull",
        "Flumph","Ghost","Griffon","Guard","Guard Captain","Helmed Horror",
        "Invisible Stalker","Lizardfolk Sovereign","Mage","Mage Apprentice","Noble",
        "Noble Prodigy","Otyugh","Saber-Toothed Tiger","Spy","Tough Boss","Troll","Yeti"
    };

    public static JSONObject dependencyCoverage(){
        JSONObject out=new JSONObject(); JSONArray rows=new JSONArray(); int ready=0;
        try{
            for(String name:NF_REFERENCED){
                JSONObject profile=CompiledStatblockEngine.runtimeProfile(name);
                boolean found=profile.optBoolean("found",false);
                boolean authorized=found && profile.optJSONObject("completeness")!=null;
                if(authorized)ready++;
                rows.put(new JSONObject().put("name",name).put("source",SOURCE_ID)
                    .put("required_by","NF").put("runtime_present",found)
                    .put("authorized_compiled_record",authorized)
                    .put("status",authorized?"ready":"blocked_missing_authorized_xmm_statblock"));
            }
            return out.put("source",SOURCE_ID).put("scope","NF-referenced XMM dependencies")
                .put("expected",NF_REFERENCED.length).put("ready",ready)
                .put("blocked",NF_REFERENCED.length-ready).put("complete",ready==NF_REFERENCED.length)
                .put("auto_rolls",false).put("dependencies",rows);
        }catch(Exception e){try{return out.put("source",SOURCE_ID).put("complete",false)
            .put("status","blocked").put("reason","dependency_audit_error");}catch(Exception ignored){return out;}}
    }

    public static JSONObject statblock(String name){
        JSONObject out=new JSONObject();
        try{
            JSONObject p=CompiledStatblockEngine.runtimeProfile(name);
            if(!p.optBoolean("found",false))return out.put("source",SOURCE_ID).put("name",name)
                .put("resolved",false).put("status","blocked_missing_authorized_xmm_statblock")
                .put("auto_rolls",false);
            return out.put("source",SOURCE_ID).put("name",name).put("resolved",true)
                .put("auto_rolls",false).put("result",p);
        }catch(Exception e){try{return out.put("source",SOURCE_ID).put("name",name)
            .put("resolved",false).put("status","blocked_runtime_error").put("auto_rolls",false);}
            catch(Exception ignored){return out;}}
    }
}
