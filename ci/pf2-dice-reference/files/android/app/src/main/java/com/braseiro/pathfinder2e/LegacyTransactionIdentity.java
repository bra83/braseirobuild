package com.braseiro.pathfinder2e;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;

/** Gera uma identidade estável para pendências anteriores ao effect_key obrigatório. */
public final class LegacyTransactionIdentity {
    private LegacyTransactionIdentity() {}

    public static String key(String world,String title,String summary,String payload,long createdAt) {
        String material=clean(world)+"\n"+clean(title)+"\n"+clean(summary)+"\n"+clean(payload)+"\n"+Math.max(0L,createdAt);
        try{
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(material.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder("legacy-transaction:");
            for(int i=0;i<16;i++)out.append(String.format(Locale.US,"%02x",hash[i]&255));
            return out.toString();
        }catch(Exception impossible){throw new IllegalStateException("SHA-256 indisponível",impossible);}
    }

    /** A identidade nasce do registro portátil original, antes de remapear IDs SQLite. */
    public static String keyForBackupRow(JSONObject row) {
        if(row==null)return key("","","","",0L);
        return key(row.optString("world_profile"),row.optString("title"),row.optString("summary"),
                row.optString("payload_json"),row.optLong("created_at",0L));
    }

    private static String clean(String value){return value==null?"":value.trim();}
}
