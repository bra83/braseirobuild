package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Portable integrity seal for NEW BACKUP21 exports; checks before any SQL mutation.
 * Old version-21 backups without a seal remain importable for backwards compatibility.
 * The hash prevents accidental truncation/corruption; it is not an authentication signature.
 */
public final class Backup21IntegrityEnvelope {
    public static final String KEY="backup21_content_sha256";
    private Backup21IntegrityEnvelope() {}
    public static JSONObject seal(JSONObject root)throws Exception {
        if(root==null||root.optInt("version",0)!=21)
            throw new IllegalArgumentException("backup21_seal_requires_current_version");
        String required=required(root);
        if(!required.isEmpty())throw new IllegalArgumentException(required);
        // Hash the same JSON-parsed value that check() will verify. SQLite/Android
        // JSON objects can carry Number implementations whose toString() changes
        // after a serialization round-trip (for example integral Doubles).
        JSONObject canonicalInput=new JSONObject(root.toString());
        canonicalInput.remove(KEY);
        root.put(KEY,CanonicalJson.sha256(canonicalInput));
        return root;
    }
    public static String check(JSONObject root){
        if(root==null)return "blocked_missing_backup";
        if(root.optInt("version",0)!=21)return root.has(KEY)?"unexpected_integrity_envelope_version":"";
        if(!root.has(KEY))return ""; // legacy version 21, prior to RUN217
        String present=root.optString(KEY,"");
        if(!validDigest(present))return "invalid_integrity_digest";
        String required=required(root);
        if(!required.isEmpty())return required;
        try {
            JSONObject without=new JSONObject(root.toString());without.remove(KEY);
            return present.equals(CanonicalJson.sha256(without))?"":"integrity_digest_mismatch";
        } catch(Exception e){return "integrity_digest_validation_failed";}
    }
    private static String required(final JSONObject backup){
        return Backup21RequiredSections.missing(backup.optInt("version",0),
            new Backup21RequiredSections.Presence(){
                public boolean object(String key){return backup.optJSONObject(key)!=null;}
                public boolean array(String key){return backup.optJSONArray(key)!=null;}
            });
    }
    private static boolean validDigest(String value){
        if(value==null||value.length()!=64)return false;
        for(int i=0;i<64;i++){
            char c=value.charAt(i);
            if(!((c>='0'&&c<='9')||(c>='a'&&c<='f')))return false;
        }
        return true;
    }
}
