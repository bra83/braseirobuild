package com.braseiro.pathfinder2e;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;

/** Identidade estável para eventos criados antes de event_key/transaction_key. */
public final class LegacyWorldEventIdentity {
    private LegacyWorldEventIdentity() {}

    public static String key(String world,long legacyId,String title,String description,long dueAt,int dueDay,
                             String consequence,String trigger,String procedure,long createdAt) {
        String material=clean(world)+"\n"+Math.max(0L,legacyId)+"\n"+clean(title)+"\n"+clean(description)+"\n"
                +Math.max(0L,dueAt)+"\n"+Math.max(0,dueDay)+"\n"+clean(consequence)+"\n"+clean(trigger)+"\n"
                +clean(procedure)+"\n"+Math.max(0L,createdAt);
        try{
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(material.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder("legacy-event:");
            for(int i=0;i<16;i++)out.append(String.format(Locale.US,"%02x",hash[i]&255));
            return out.toString();
        }catch(Exception impossible){throw new IllegalStateException("SHA-256 indisponível",impossible);}
    }

    /** Calculada sobre o registro portátil antes de qualquer remapeamento de IDs. */
    public static String keyForBackupRow(JSONObject row) {
        if(row==null)return key("",0L,"","",0L,0,"","","",0L);
        return key(row.optString("world_profile"),row.optLong("id",0L),row.optString("title"),
                row.optString("description"),row.optLong("due_at",0L),row.optInt("due_day",0),
                row.optString("consequence_json"),row.optString("trigger_json"),row.optString("procedure_json"),
                row.optLong("created_at",0L));
    }

    public static String transactionKey(String eventKey) {
        String safe=clean(eventKey);
        if(safe.length()==0)throw new IllegalArgumentException("event_key ausente");
        return "world_event:"+safe;
    }

    private static String clean(String value){return value==null?"":value.trim();}
}
