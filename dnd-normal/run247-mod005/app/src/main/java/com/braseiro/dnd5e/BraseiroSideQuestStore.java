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
                "imported_at INTEGER NOT NULL, location_index TEXT NOT NULL DEFAULT '', " +
                "PRIMARY KEY(scope,module_id))");
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
        // Independent optional library: the principal package's worldId/timelineId
        // describe THAT adventure, not the live D&D atlas or all future modules.
        // A Strixhaven principal may remain active while the same campaign
        // continues in Faerûn. Comparing its identifiers here incorrectly
        // rejects a valid Faerûn module. The current D&D profile and the
        // module's Faerûn world guard are checked below; geography is checked
        // at RUMOR/SCENE exposure, never by mutating the main adventure.
        // Preserve the module's own world/timeline metadata for backup/restore.
        if (!world.toLowerCase(Locale.ROOT).contains("faerun"))
            throw new IllegalArgumentException("Este pacote não é uma aventura do mundo atual de Faerûn.");
        JSONObject previous = db.queryOne("SELECT package_json FROM braseiro_sidequest_modules WHERE scope=? AND module_id=?",
                new String[]{scope(),id});
        if (previous.length() > 0) {
            if (!new JSONObject(previous.optString("package_json","{}")).toString().equals(pkg.toString()))
                throw new IllegalArgumentException("ID já instalado com conteúdo diferente. Preserve a versão da missão em andamento.");
            return new JSONObject().put("ok",true).put("alreadyInstalled",true).put("id",id).put("title",pkg.getString("title"));
        }
        ContentValues row = new ContentValues();
        row.put("scope",scope()); row.put("module_id",id); row.put("title",pkg.getString("title"));
        row.put("world_id",world); row.put("timeline_id",timeline);
        row.put("package_json",pkg.toString()); row.put("location_index",locationIndex(pkg)); row.put("enabled",1);
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
    /** Explicit library opt-in; never moves the player or alters the main story. */
    public synchronized void setFollowing(String id,boolean following) {
        ensureSchema(db.getWritableDatabase());
        ContentValues values=new ContentValues();
        values.put("state",following?"following":"unaccepted");
        if(db.getWritableDatabase().update("braseiro_sidequest_modules",values,
                "scope=? AND module_id=?",new String[]{scope(),id})!=1)
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

    /**
     * Tiny non-secret index so each scene never reparses every 2 MB module.
     * A positive match is only a prefilter; eligibleRumors still verifies place.
     */
    private static String locationIndex(JSONObject pkg) {
        StringBuilder index=new StringBuilder("|");
        JSONArray locs=pkg.optJSONArray("locations");
        for(int i=0;locs!=null&&i<locs.length();i++){
            JSONObject location=locs.optJSONObject(i);if(location==null)continue;
            for(String key:new String[]{"id","name","atlasHex"}){
                String value=location.optString(key,"");
                if(!value.isEmpty()&&!value.equals("null"))
                    index.append(normalized(value).trim()).append("|");
            }
        }
        JSONObject geo=pkg.optJSONObject("canonicalGeography");
        JSONArray anchors=geo==null?null:geo.optJSONArray("canonicalAnchors");
        for(int i=0;anchors!=null&&i<anchors.length();i++){
            JSONObject place=anchors.optJSONObject(i);
            if(place!=null)index.append(normalized(place.optString("name")).trim()).append("|");
        }
        return index.toString();
    }
    private static boolean indexCouldMatch(String index,String scene,String hex){
        if(index==null||index.trim().isEmpty())return true; // legacy row
        if(scene==null||scene.trim().isEmpty())return false;
        for(String candidate:index.split("\\|")){
            if(candidate.trim().length()>=5&&containsPlace(scene,candidate))return true;
            if(!hex.isEmpty()&&candidate.trim().equalsIgnoreCase(hex))return true;
        }
        return false;
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

    /**
     * Scene tools are only available at a real, host-confirmed place. They are
     * suggestions for investigation, NEVER a forced encounter or a state commit.
     * No global truth, secret, hidden-NPC identity or unvisited map is exposed.
     */
    static JSONObject localSceneMaterial(JSONObject pkg,String label,String hex) {
        JSONObject out=new JSONObject();
        try {
            JSONArray encounterFeed=new JSONArray(),clueFeed=new JSONArray();
            JSONArray arcFeed=new JSONArray(),npcFeed=new JSONArray();
            JSONArray encounters=pkg.optJSONArray("encounters");
            for(int i=0;encounters!=null&&i<encounters.length()&&encounterFeed.length()<3;i++){
                JSONObject event=encounters.optJSONObject(i);
                if(event==null||!matchesPlace(pkg,event.optString("locationId"),label,hex))continue;
                encounterFeed.put(new JSONObject().put("id",event.optString("id"))
                    .put("type",event.optString("type")).put("setup_private",event.optString("description"))
                    .put("trigger_required",event.optString("trigger"))
                    .put("auto_resolve",false));
            }
            JSONArray clues=pkg.optJSONArray("clues");
            for(int i=0;clues!=null&&i<clues.length()&&clueFeed.length()<3;i++){
                JSONObject clue=clues.optJSONObject(i);
                if(clue==null||!matchesPlace(pkg,clue.optString("locationId"),label,hex))continue;
                clueFeed.put(new JSONObject().put("id",clue.optString("id"))
                    .put("possible_evidence_private",clue.optString("description"))
                    .put("reveal_condition",clue.optString("revealCondition"))
                    .put("known_at_import",false));
            }
            JSONObject story=pkg.optJSONObject("story");
            JSONArray arcs=story==null?null:story.optJSONArray("arcs");
            for(int i=0;arcs!=null&&i<arcs.length()&&arcFeed.length()<2;i++){
                JSONObject arc=arcs.optJSONObject(i);
                if(arc==null||!matchesPlace(pkg,arc.optString("entry"),label,hex))continue;
                arcFeed.put(new JSONObject().put("id",arc.optString("id"))
                    .put("name_private",arc.optString("name"))
                    .put("dramatic_question_private",arc.optString("dramatic_question"))
                    .put("optional_scenes_private",arc.optJSONArray("scenes")==null?new JSONArray():arc.optJSONArray("scenes")));
            }
            JSONArray npcs=pkg.optJSONArray("npcs");
            for(int i=0;npcs!=null&&i<npcs.length()&&npcFeed.length()<3;i++){
                JSONObject npc=npcs.optJSONObject(i);if(npc==null)continue;
                JSONArray places=npc.optJSONArray("places");boolean near=false;
                for(int k=0;places!=null&&k<places.length();k++)
                    if(matchesPlace(pkg,places.optString(k),label,hex))near=true;
                if(!near)continue;
                npcFeed.put(new JSONObject().put("name_private_candidate",npc.optString("name"))
                    .put("public_role_if_present",npc.optString("publicRole"))
                    .put("appearance_if_present",npc.optString("appearance"))
                    .put("presence_must_be_verified_by_host",true));
            }
            out.put("optional_encounters",encounterFeed).put("conditional_clues_private",clueFeed)
                .put("local_arc_material_private",arcFeed)
                .put("possible_npcs_NOT_spawned",npcFeed);
        }catch(Exception ignored){return new JSONObject();}
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

    /** Conservative evidence recognition: only after the player's scene actually committed. */
    static boolean narrationContainsRumor(String narration,String hook) {
        if(narration==null||hook==null||narration.trim().isEmpty()||hook.trim().isEmpty())return false;
        String actual=normalized(narration),expected=normalized(hook).trim();
        if(expected.length()>=18&&actual.contains(" "+expected+" "))return true;
        java.util.HashSet<String> keywords=new java.util.HashSet<>();
        for(String word:expected.split(" "))if(word.length()>=5)keywords.add(word);
        if(keywords.size()<4)return false; // Tiny hints require an exact quote.
        int found=0;
        for(String word:keywords)if(actual.contains(" "+word+" "))found++;
        return found>=4&&found*100>=keywords.size()*75;
    }
    /**
     * Invoked ONLY at the accepted narration boundary; repeated descriptions of
     * the same rumor are then suppressed by the persisted ledger. Never marks
     * imported hooks heard merely because the Master considered them.
     */
    public synchronized int recordAcceptedNarration(String narration,String confirmedLocation,int day) {
        if(narration==null||narration.trim().isEmpty()||
                confirmedLocation==null||confirmedLocation.trim().isEmpty())return 0;
        try{
            ensureSchema(db.getWritableDatabase());
            String hex=db.getMapState().optString("current_hex_id","");
            int recorded=0;
            for(JSONObject candidate:db.queryList(
                    "SELECT module_id,location_index FROM braseiro_sidequest_modules WHERE scope=? AND enabled=1 LIMIT 200",
                    new String[]{scope()})){
                if(!indexCouldMatch(candidate.optString("location_index"),confirmedLocation,hex))continue;
                String id=candidate.optString("module_id");
                JSONObject row=db.queryOne("SELECT package_json FROM braseiro_sidequest_modules WHERE scope=? AND module_id=?",
                        new String[]{scope(),id});
                if(row.length()==0)continue;
                JSONArray rumors=eligibleRumors(new JSONObject(row.optString("package_json","{}")),confirmedLocation,hex);
                for(int i=0;i<rumors.length();i++){
                    JSONObject rumor=rumors.optJSONObject(i);
                    if(rumor!=null&&narrationContainsRumor(narration,rumor.optString("rumor"))
                            &&recordConfirmedRumor(id,rumor.optString("id"),"narracao_confirmada",
                                confirmedLocation,day))recorded++;
                }
            }
            return recorded;
        }catch(Exception error){
            AppLog.e("SideQuestStore","Não foi possível registrar boatos confirmados.",error);
            return 0; // Never cause a verified scene to fail because of rumor bookkeeping.
        }
    }

    /** Read-only, small, spoiler-safe candidate feed for the Master. */
    public JSONObject masterContext() {
        JSONObject out=new JSONObject();
        try {
            ensureSchema(db.getWritableDatabase());
            JSONObject scene=AtlasScenePositionReconciler.authoritativeSceneSnapshot(db);
            JSONObject map=db.getMapState();
            String location=scene.optString("location","");
            String hex=map.optString("current_hex_id","");
            JSONArray nearby=new JSONArray();
            for(JSONObject row:db.queryList("SELECT module_id,title,location_index,state FROM braseiro_sidequest_modules " +
                    "WHERE scope=? AND enabled=1 ORDER BY imported_at LIMIT 200",new String[]{scope()})) {
                if(!indexCouldMatch(row.optString("location_index"),location,hex))continue;
                JSONObject raw=db.queryOne("SELECT package_json FROM braseiro_sidequest_modules WHERE scope=? AND module_id=?",
                    new String[]{scope(),row.optString("module_id")});
                if(raw.length()==0)continue;
                JSONObject pkg=new JSONObject(raw.optString("package_json","{}"));
                JSONArray candidates=eligibleRumors(pkg,location,hex),unheard=new JSONArray();
                for(int i=0;i<candidates.length();i++){
                    JSONObject candidate=candidates.optJSONObject(i);
                    if(candidate==null)continue;
                    String rumorId=candidate.optString("id");
                    JSONObject heard=db.queryOne("SELECT rumor_id FROM braseiro_sidequest_rumors " +
                            "WHERE scope=? AND module_id=? AND rumor_id=?",new String[]{scope(),row.optString("module_id"),rumorId});
                    if(heard.length()==0)unheard.put(candidate);
                }
                JSONObject localMaterial=localSceneMaterial(pkg,location,hex);
                boolean localLead=false;
                java.util.Iterator<String> localKeys=localMaterial.keys();
                while(localKeys.hasNext()){Object payload=localMaterial.opt(localKeys.next());
                    if(payload instanceof JSONArray && ((JSONArray)payload).length()>0)localLead=true;}
                if(unheard.length()==0&&!localLead)continue;
                boolean following="following".equals(row.optString("state"));
                JSONObject exposure=new JSONObject().put("id",row.optString("module_id"))
                    .put("title_private",row.optString("title"))
                    .put("following",following)
                    .put("accepted","accepted".equals(row.optString("state")))
                    .put("localRumors",unheard)
                    .put("localAdventureMaterial_private",localMaterial);
                if(following){
                    JSONObject story=pkg.optJSONObject("story");
                    if(story!=null)exposure.put("background_private_GM_ONLY",
                        story.optString("background","").substring(0,
                            Math.min(3200,story.optString("background","").length())));
                }
                nearby.put(exposure);
                if(nearby.length()>=5)break;
            }
            out.put("optional_modules_nearby",nearby)
               .put("current_location_verified_by_host",location)
               .put("policy","Missões importadas são oportunidades opcionais e NÃO substituem a aventura principal. " +
                    "O módulo secundário NUNCA fornece posição inicial, elenco obrigatório ou cena de abertura, mesmo quando seu JSON contém start ou playerIntroduction. " +
                    "A cena persistida e a identidade dos NPCs presentes têm prioridade absoluta; mantenha nomes, relações e diálogos já estabelecidos. " +
                    "Ofereça NO MÁXIMO UM rumor natural por cena, apenas se o local atual permitir, " +
                    "sem inventar NPC presente, evento, teste, recompensa ou viagem. " +
                    "Não revele segredo, pano de fundo privado, solução, nome oculto ou destino " +
                    "que a cena não tenha comprovado. Um rumor é boato, não fato. " +
                    "A importação não representa aceitação nem conclusão. Mesmo com material local, só introduza NPCs quando realmente presentes e pistas após observação ou investigação confirmada. O modo acompanhar não presume viagem, sucesso ou resolução.");
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
