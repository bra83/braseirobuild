package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.List;

/**
 * Parallel, private library of optional D&D adventures. Import never replaces
 * the principal adventure, starts a quest, spawns an NPC or changes world time.
 * Only place-validated, non-secret rumor hooks are passed to the narrator.
 */
public final class BraseiroSideQuestStore {
    private static final int MAX_BYTES = 2_000_000;
    private final AppDb db;

    public BraseiroSideQuestStore(AppDb database) { db = database; }
    private String scope() {
        return db.getActiveMapProfile() + ":" + db.getCampaign().optString("name", "D&D Normal");
    }
    static void ensureSchema(SQLiteDatabase sql) {
        sql.execSQL("CREATE TABLE IF NOT EXISTS braseiro_sidequest_modules (" +
                "scope TEXT NOT NULL, module_id TEXT NOT NULL, title TEXT NOT NULL, " +
                "world_id TEXT NOT NULL, timeline_id TEXT NOT NULL, package_json TEXT NOT NULL, " +
                "enabled INTEGER NOT NULL DEFAULT 1, state TEXT NOT NULL DEFAULT 'unaccepted', " +
                "imported_at INTEGER NOT NULL, PRIMARY KEY(scope,module_id))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS braseiro_sidequest_rumors (" +
                "scope TEXT NOT NULL, module_id TEXT NOT NULL, rumor_id TEXT NOT NULL, " +
                "source TEXT NOT NULL, location TEXT NOT NULL, day_number INTEGER NOT NULL, " +
                "PRIMARY KEY(scope,module_id,rumor_id))");
    }
    public synchronized JSONObject importJson(String source) throws Exception {
        if (source == null || source.getBytes("UTF-8").length > MAX_BYTES || source.trim().isEmpty())
            throw new IllegalArgumentException("A aventura JSON deve ter no máximo 2 MB.");
        JSONObject pkg = new JSONObject(source);
        BraseiroAdventureStore.validatePackage(pkg);
        String id = pkg.getString("id"), world = pkg.getString("worldId");
        String timeline = pkg.optString("timelineId","principal");
        if (!"dnd".equalsIgnoreCase(db.getActiveMapProfile()))
            throw new IllegalArgumentException("Missões JSON disponíveis apenas no mundo D&D.");
        SQLiteDatabase sql = db.getWritableDatabase();
        ensureSchema(sql);
        // If there is a main imported package, the optional package must share
        // its world and timeline. Otherwise the host map position remains authority.
        JSONObject hasMain = db.queryOne("SELECT name FROM sqlite_master WHERE type='table' AND name='braseiro_adventure_packages'", null);
        if (hasMain.length() > 0) {
            JSONObject main = db.queryOne("SELECT world_id,timeline_id,active_json FROM braseiro_adventure_packages WHERE scope=?",
                    new String[]{scope()});
            if (main.length() > 0 && new JSONObject(main.optString("active_json","{}")).length() > 0
                    && (!world.equals(main.optString("world_id")) || !timeline.equals(main.optString("timeline_id"))))
                throw new IllegalArgumentException("A missão secundária pertence a outro mundo ou cronologia.");
        }
        if (!world.toLowerCase(Locale.ROOT).contains("faerun"))
            throw new IllegalArgumentException("Este pacote não é uma aventura do mundo atual de Faerûn.");
        JSONObject previous = db.queryOne("SELECT package_json FROM braseiro_sidequest_modules WHERE scope=? AND module_id=?",
                new String[]{scope(),id});
        if (previous.length() > 0) {
            if (!new JSONObject(previous.optString("package_json","{}")).similar(pkg))
                throw new IllegalArgumentException("ID já instalado com conteúdo diferente. Preserve a versão da missão em andamento.");
            return new JSONObject().put("ok",true).put("alreadyInstalled",true).put("id",id).put("title",pkg.getString("title"));
        }
        ContentValues row = new ContentValues();
        row.put("scope",scope()); row.put("module_id",id); row.put("title",pkg.getString("title"));
        row.put("world_id",world); row.put("timeline_id",timeline);
        row.put("package_json",pkg.toString()); row.put("enabled",1);
        row.put("state","unaccepted"); row.put("imported_at",System.currentTimeMillis());
        if (sql.insertOrThrow("braseiro_sidequest_modules",null,row) < 0)
            throw new IllegalStateException("Não foi possível registrar a missão secundária.");
        return new JSONObject().put("ok",true).put("id",id).put("title",pkg.getString("title"))
                .put("alreadyInstalled",false).put("started",false);
    }

    /** Player's library screen: never returns the story's GM-only material. */
    public JSONArray listInstalled() {
        ensureSchema(db.getWritableDatabase());
        JSONArray out = new JSONArray();
        List<JSONObject> rows = db.queryList("SELECT module_id,title,enabled,state FROM braseiro_sidequest_modules " +
                "WHERE scope=? ORDER BY imported_at,module_id LIMIT 120",new String[]{scope()});
        for (JSONObject row : rows) {
            JSONObject item = new JSONObject();
            try {item.put("id",row.optString("module_id")).put("title",row.optString("title"))
                    .put("enabled",row.optInt("enabled") == 1).put("state",row.optString("state"));}
            catch (Exception ignored) { }
            out.put(item);
        }
        return out;
    }
    public synchronized void setEnabled(String id,boolean enabled) {
        ensureSchema(db.getWritableDatabase());
        ContentValues values = new ContentValues();values.put("enabled",enabled ? 1 : 0);
        if (db.getWritableDatabase().update("braseiro_sidequest_modules",values,
                "scope=? AND module_id=?",new String[]{scope(),id}) != 1)
            throw new IllegalArgumentException("Missão secundária não encontrada.");
    }
    public synchronized void remove(String id) {
        SQLiteDatabase sql = db.getWritableDatabase();ensureSchema(sql);
        sql.beginTransaction();
        try {
            sql.delete("braseiro_sidequest_rumors","scope=? AND module_id=?",new String[]{scope(),id});
            sql.delete("braseiro_sidequest_modules","scope=? AND module_id=?",new String[]{scope(),id});
            sql.setTransactionSuccessful();
        } finally { sql.endTransaction(); }
        // Never delete committed canonical events, journal, NPCs or main story.
    }

    private static String normalized(String text) {
        if(text == null) return "";
        String lowered = Normalizer.normalize(text,Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replace('_',' ').replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
        return " " + lowered + " ";
    }
    private static boolean containsPlace(String actual,String expected) {
        String name = normalized(expected).trim();
        return name.length() >= 5 && normalized(actual).contains(" " + name + " ");
    }
    private static JSONObject findLocation(JSONObject pkg,String id) {
        JSONArray locations = pkg.optJSONArray("locations");
        for(int i=0;locations!=null&&i<locations.length();i++) {
            JSONObject loc = locations.optJSONObject(i);
            if(loc!=null && id.equals(loc.optString("id")))return loc;
        }
        return new JSONObject();
    }
    private static boolean matchesPlace(JSONObject pkg,String place,String sceneLabel,String hex) {
        if(place == null || place.isEmpty())return false;
        JSONObject loc = findLocation(pkg,place);
        if(loc.optString("locationType").contains("virtual"))return false;
        if(containsPlace(sceneLabel,loc.optString("name"))
                || containsPlace(sceneLabel,place))return true;
        String atlasHex=loc.optString("atlasHex","");
        if(!atlasHex.isEmpty() && !"null".equals(atlasHex)
                && !hex.isEmpty() && atlasHex.equalsIgnoreCase(hex))return true;
        // A named confirmed local site inherits its real parent region, but a
        // parent city cannot imply that the party visited a specific building.
        JSONArray locations=pkg.optJSONArray("locations");
        for(int i=0;locations!=null&&i<locations.length();i++) {
            JSONObject child=locations.optJSONObject(i);
            if(child!=null && place.equals(child.optString("parentPlaceId"))
                    && containsPlace(sceneLabel,child.optString("name")))return true;
        }
        JSONObject geo=pkg.optJSONObject("canonicalGeography");
        JSONArray anchors=geo==null?null:geo.optJSONArray("canonicalAnchors");
        for(int i=0;anchors!=null&&i<anchors.length();i++) {
            JSONObject anchor=anchors.optJSONObject(i);
            if(anchor!=null && place.equals(anchor.optString("id"))
                    && containsPlace(sceneLabel,anchor.optString("name")))return true;
        }
        return false;
    }
    private static void appendRumor(JSONArray out,String id,String hook,String place,String stage) {
        if(hook == null || hook.trim().isEmpty() || out.length() >= 4)return;
        JSONObject item=new JSONObject();
        try {item.put("id",id).put("locationId",place).put("stage",stage)
                .put("rumor",hook.trim().substring(0,Math.min(400,hook.trim().length())));}
        catch(Exception ignored) {return;}
        out.put(item);
    }
    /** Pure location gate. Null/unknown atlas hex does not invent distances. */
    static JSONArray eligibleRumors(JSONObject pkg,String sceneLabel,String atlasHex) {
        JSONArray out=new JSONArray();
        if(sceneLabel==null || sceneLabel.trim().isEmpty())return out;
        JSONObject start=pkg.optJSONObject("start");
        if(start==null)return out;
        String entry=start.optString("locationId");
        JSONObject entryLoc=findLocation(pkg,entry);
        boolean atEntry=matchesPlace(pkg,entry,sceneLabel,atlasHex);
        String parent=entryLoc.optString("parentPlaceId");
        if(atEntry)appendRumor(out,"opening",start.optString("hook"),entry,"site");
        else if(!parent.isEmpty() && matchesPlace(pkg,parent,sceneLabel,atlasHex)) {
            JSONObject story=pkg.optJSONObject("story");
            appendRumor(out,"regional",story==null?"":story.optString("oralHistory"),parent,"region");
        }
        // Hooks are private until the player really reaches the stated region.
        // No NPC is spawned or attributed speech simply because a hook exists.
        JSONArray side=pkg.optJSONArray("sideMissionsPrivate");
        for(int i=0;side!=null&&i<side.length();i++){
            JSONObject mission=side.optJSONObject(i);
            if(mission==null || !matchesPlace(pkg,mission.optString("place"),sceneLabel,atlasHex))continue;
            appendRumor(out,"side:"+mission.optString("id"),mission.optString("hook"),
                    mission.optString("place"),"local");
        }
        return out;
    }

    /** Mark as heard only from a CONFIRMED scene or explicit player record. */
    public synchronized boolean recordConfirmedRumor(String moduleId,String rumorId,String source,
            String confirmedLocation,int dayNumber) throws Exception {
        if(moduleId==null||rumorId==null||source==null||source.trim().isEmpty()
                ||confirmedLocation==null||confirmedLocation.trim().isEmpty())
            throw new IllegalArgumentException("Rumor requer fonte e local confirmados.");
        SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);
        JSONObject stored=db.queryOne("SELECT package_json FROM braseiro_sidequest_modules WHERE scope=? AND module_id=? AND enabled=1",
                new String[]{scope(),moduleId});
        if(stored.length()==0)return false;
        JSONObject pkg=new JSONObject(stored.optString("package_json","{}"));
        JSONArray eligible=eligibleRumors(pkg,confirmedLocation,db.getMapState().optString("current_hex_id"));
        boolean valid=false;
        for(int i=0;i<eligible.length();i++)if(rumorId.equals(eligible.optJSONObject(i).optString("id")))valid=true;
        if(!valid)return false;
        ContentValues row=new ContentValues();
        row.put("scope",scope());row.put("module_id",moduleId);row.put("rumor_id",rumorId);
        row.put("source",source);row.put("location",confirmedLocation);
        row.put("day_number",Math.max(1,dayNumber));
        return sql.insertWithOnConflict("braseiro_sidequest_rumors",null,row,SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    /** Read-only, small, spoiler-safe candidate feed for the Master. */
    public JSONObject masterContext() {
        JSONObject out=new JSONObject();
        try {
            ensureSchema(db.getWritableDatabase());
            JSONObject scene=db.getSceneState();
            JSONObject map=db.getMapState();
            String location=scene.optString("location","");
            String hex=map.optString("current_hex_id","");
            JSONArray nearby=new JSONArray();
            for(JSONObject row:db.queryList("SELECT module_id,title,package_json,state FROM braseiro_sidequest_modules " +
                    "WHERE scope=? AND enabled=1 ORDER BY imported_at LIMIT 100",new String[]{scope()})) {
                JSONObject pkg=new JSONObject(row.optString("package_json","{}"));
                JSONArray candidates=eligibleRumors(pkg,location,hex),unheard=new JSONArray();
                for(int i=0;i<candidates.length();i++){
                    JSONObject candidate=candidates.optJSONObject(i);
                    if(candidate==null)continue;
                    String rumorId=candidate.optString("id");
                    JSONObject heard=db.queryOne("SELECT rumor_id FROM braseiro_sidequest_rumors " +
                            "WHERE scope=? AND module_id=? AND rumor_id=?",new String[]{scope(),row.optString("module_id"),rumorId});
                    if(heard.length()==0)unheard.put(candidate);
                }
                if(unheard.length()==0)continue;
                JSONObject exposure=new JSONObject().put("id",row.optString("module_id"))
                    .put("title_private",row.optString("title"))
                    .put("accepted",!"unaccepted".equals(row.optString("state")))
                    .put("localRumors",unheard);
                nearby.put(exposure);
                if(nearby.length()>=3)break;
            }
            out.put("optional_modules_nearby",nearby)
               .put("current_location_verified_by_host",location)
               .put("policy","Missões importadas são oportunidades opcionais e NÃO substituem a aventura principal. " +
                    "Ofereça NO MÁXIMO UM rumor natural por cena, apenas se o local atual permitir, " +
                    "sem inventar NPC presente, evento, teste, recompensa ou viagem. " +
                    "Não revele segredo, pano de fundo privado, solução, nome oculto ou destino " +
                    "que a cena não tenha comprovado. Um rumor é boato, não fato. " +
                    "A importação não representa aceitação nem conclusão.");
        }catch(Exception error){
            try{out.put("loadError","Missões secundárias indisponíveis; preserve a campanha principal.");}
            catch(Exception ignored){}
            AppLog.e("SideQuestStore","Contexto de missões secundárias indisponível.",error);
        }
        return out;
    }

    public void attachPortableBackup(JSONObject root) throws Exception {
        ensureSchema(db.getWritableDatabase());
        String prefix=db.getActiveMapProfile()+":%";
        root.put("braseiroSideQuestModules",new JSONArray(db.queryList(
                "SELECT * FROM braseiro_sidequest_modules WHERE scope LIKE ?",new String[]{prefix})));
        root.put("braseiroSideQuestRumors",new JSONArray(db.queryList(
                "SELECT * FROM braseiro_sidequest_rumors WHERE scope LIKE ?",new String[]{prefix})));
    }
    /** Invoked inside the host's existing atomic backup restoration transaction. */
    public void restorePortableBackup(SQLiteDatabase sql,JSONObject root,String profile) throws Exception {
        ensureSchema(sql);
        String[] names={"braseiroSideQuestModules","braseiroSideQuestRumors"};
        String[] tables={"braseiro_sidequest_modules","braseiro_sidequest_rumors"};
        for(int group=0;group<names.length;group++){
            JSONArray rows=root.optJSONArray(names[group]);
            if(rows==null)continue; // Older BACKUP21 files did not contain this extension.
            sql.delete(tables[group],"scope LIKE ?",new String[]{profile+":%"});
            for(int i=0;i<rows.length();i++){
                JSONObject item=rows.optJSONObject(i);
                if(item==null)throw new IllegalArgumentException("Backup com registro de missão inválido.");
                String rowScope=item.optString("scope");
                if(!rowScope.startsWith(profile+":"))throw new SecurityException("Backup de outro mundo.");
                ContentValues data=new ContentValues();
                java.util.Iterator<String> keys=item.keys();
                while(keys.hasNext()){
                    String key=keys.next();Object v=item.opt(key);
                    if(v==JSONObject.NULL)data.putNull(key);
                    else if(v instanceof Integer)data.put(key,(Integer)v);
                    else if(v instanceof Long)data.put(key,(Long)v);
                    else data.put(key,String.valueOf(v));
                }
                if(sql.insertWithOnConflict(tables[group],null,data,SQLiteDatabase.CONFLICT_REPLACE)<0)
                    throw new IllegalStateException("Falha na restauração das missões secundárias.");
            }
        }
    }
}
