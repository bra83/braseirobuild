package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;

/** Importação narrativa v1: somente o banco do host pode persistir fatos e regras. */
public final class BraseiroAdventureStore {
    private final AppDb db;
    public BraseiroAdventureStore(AppDb appDb){this.db=appDb;}
    private void ensureSchema(SQLiteDatabase sql){
        sql.execSQL("CREATE TABLE IF NOT EXISTS braseiro_adventure_packages (scope TEXT PRIMARY KEY, world_id TEXT NOT NULL, timeline_id TEXT NOT NULL, active_json TEXT NOT NULL, history_json TEXT NOT NULL, updated_at INTEGER NOT NULL)");
        sql.execSQL("CREATE TABLE IF NOT EXISTS braseiro_adventure_npc_ledger (scope TEXT NOT NULL, npc_id TEXT NOT NULL, data_json TEXT NOT NULL, PRIMARY KEY(scope, npc_id))");
    }
    private String scope(){return db.getActiveMapProfile()+":"+db.getCampaign().optString("name", "D&D Normal");}
    public synchronized JSONObject importJson(String source)throws Exception {
        if(source==null || source.length()==0 || source.getBytes("UTF-8").length>2_000_000)throw new IllegalArgumentException("A aventura deve ter menos de 2 MB.");
        JSONObject pkg=new JSONObject(source);
        validatePackage(pkg);
        String id=pkg.getString("id");
        SQLiteDatabase sql=db.getWritableDatabase(); ensureSchema(sql);
        String key=scope();JSONObject previous=db.queryOne("SELECT * FROM braseiro_adventure_packages WHERE scope=?",new String[]{key});
        String world=pkg.getString("worldId"),timeline=pkg.optString("timelineId","principal");
        if(previous.length()>0){
            if(!world.equals(previous.optString("world_id")) || !timeline.equals(previous.optString("timeline_id")))throw new IllegalArgumentException("Mundo ou cronologia incompatível com o histórico da campanha.");
            JSONObject existing=new JSONObject(previous.optString("active_json","{}"));
            if(existing.length()>0 && !existing.optString("id").equals(id))throw new IllegalArgumentException("Conclua a aventura atual antes de importar outra.");
        }
        ContentValues values=new ContentValues();values.put("scope",key);values.put("world_id",world);values.put("timeline_id",timeline);values.put("active_json",pkg.toString());values.put("history_json",previous.optString("history_json","[]"));values.put("updated_at",System.currentTimeMillis());
        // Transação atômica: o pacote e o cadastro de NPCs sobrevivem a reabertura juntos.
        // Nunca criar NPC público só pela importação de arquivo GM privado.
        sql.beginTransaction();
        try {
            if(sql.insertWithOnConflict("braseiro_adventure_packages",null,values,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("Não foi possível salvar o JSON no banco.");
            JSONArray importedNpcs=pkg.optJSONArray("npcs");
            if(importedNpcs!=null)for(int i=0;i<importedNpcs.length()&&i<500;i++){
                JSONObject npc=importedNpcs.optJSONObject(i);if(npc==null)continue;
                String npcId=npc.optString("id","");
                if(!npcId.matches("[a-zA-Z0-9][a-zA-Z0-9._:-]{1,119}"))continue;
                ContentValues row=new ContentValues();row.put("scope",key);row.put("npc_id",npcId);row.put("data_json",npc.toString());
                // preserve earlier NPC data unless absent; the active game owns all confirmed changes
                sql.insertWithOnConflict("braseiro_adventure_npc_ledger",null,row,SQLiteDatabase.CONFLICT_IGNORE);
            }
            sql.setTransactionSuccessful();
        }finally{sql.endTransaction();}
        return new JSONObject().put("ok",true).put("id",id).put("title",pkg.optString("title"));
    }
    /** Confere o pacote v1 sem executar nenhuma regra ou alterar o banco.
     * Paridade com BRASEIRO_CONTRATO_AVENTURA_V1_CORRIGIDO.schema.json.
     */
    static void validatePackage(JSONObject pkg) throws Exception {
        if (!"braseiro.adventure/v1".equals(pkg.optString("schema"))) {
            throw new IllegalArgumentException("Contrato de aventura JSON inválido.");
        }
        requireId(pkg.opt("id"), "ID de aventura");
        String system=requireId(pkg.opt("system"),"system").toLowerCase(java.util.Locale.ROOT);
        if(!"dnd5e".equals(system)&&!"dnd-5e".equals(system)&&!"dnd5e-2024".equals(system)) {
            throw new IllegalArgumentException("Esta aventura não pertence a D&D 5e.");
        }
        requireId(pkg.opt("worldId"), "worldId");
        requireText(pkg.opt("title"), "Título da aventura");
        JSONObject start=pkg.optJSONObject("start");
        if(start==null)throw new IllegalArgumentException("start deve ser objeto com locationId.");
        String locationId=requireId(start.opt("locationId"),"start.locationId");
        checkOptionalText(start.opt("hook"),"start.hook");
        JSONObject story=pkg.optJSONObject("story");
        if(story==null)throw new IllegalArgumentException("story deve ser objeto.");
        requireText(story.opt("premise"),"story.premise");
        requireText(story.opt("background"),"story.background");
        checkOptionalText(story.opt("playerIntroduction"),"story.playerIntroduction");

        JSONArray locations=validateCollection(pkg,"locations",true);
        boolean found=false;
        for(int i=0;i<locations.length();i++)if(locationId.equals(locations.getJSONObject(i).optString("id")))found=true;
        if(!found)throw new IllegalArgumentException("start.locationId não existe em locations.");
        for(String field:new String[]{"npcs","objectives","encounters"})validateCollection(pkg,field,false);
        JSONArray enemies=validateCollection(pkg,"enemies",false);
        for(int i=0;i<enemies.length();i++){
            JSONObject rulesRef=enemies.getJSONObject(i).optJSONObject("rulesRef");
            if(rulesRef==null)throw new IllegalArgumentException("enemies["+i+"].rulesRef.source ausente.");
            requireText(rulesRef.opt("source"),"enemies["+i+"].rulesRef.source");
        }
        JSONArray secrets=validateCollection(pkg,"secrets",false);
        for(int i=0;i<secrets.length();i++){
            JSONObject secret=secrets.getJSONObject(i);
            if(!"gm_only".equals(secret.opt("visibility")))throw new IllegalArgumentException("secrets["+i+"]: visibility deve ser gm_only.");
            requireText(secret.opt("truth"),"secrets["+i+"].truth");
        }
        checkOptionalText(pkg.opt("description"),"description");
        checkOptionalText(pkg.opt("author"),"author");
        if(pkg.has("timelineId"))requireId(pkg.opt("timelineId"),"timelineId");
    }
    private static String requireId(Object raw,String field){
        if(!(raw instanceof String))throw new IllegalArgumentException(field+": ID inválido.");
        String id=(String)raw;
        if(id.length()>160||!id.matches("[a-zA-Z0-9][a-zA-Z0-9._:-]*"))throw new IllegalArgumentException(field+": ID inválido.");
        return id;
    }
    private static void requireText(Object raw,String field){
        if(!(raw instanceof String)||((String)raw).trim().isEmpty())throw new IllegalArgumentException(field+": texto obrigatório ausente.");
    }
    private static void checkOptionalText(Object raw,String field){
        if(raw!=null&&raw!=JSONObject.NULL&&!(raw instanceof String))throw new IllegalArgumentException(field+": deve ser texto.");
    }
    private static JSONArray validateCollection(JSONObject pkg,String field,boolean required)throws Exception{
        if(!pkg.has(field)&&!required)return new JSONArray();
        JSONArray array=pkg.optJSONArray(field);
        if(array==null)throw new IllegalArgumentException(field+": lista inválida.");
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(int i=0;i<array.length();i++){
            JSONObject item=array.optJSONObject(i);
            if(item==null)throw new IllegalArgumentException(field+"["+i+"]: objeto inválido.");
            String id=requireId(item.opt("id"),field+"["+i+"].id");
            if(!seen.add(id))throw new IllegalArgumentException(field+": ID duplicado ("+id+").");
        }
        return array;
    }
    public synchronized boolean finish(){
        SQLiteDatabase sql=db.getWritableDatabase();ensureSchema(sql);String key=scope();
        JSONObject row=db.queryOne("SELECT * FROM braseiro_adventure_packages WHERE scope=?",new String[]{key});
        if(row.length()==0)return false;
        try{
            JSONObject active=new JSONObject(row.optString("active_json","{}"));if(active.length()==0)return false;
            JSONArray history=new JSONArray(row.optString("history_json","[]"));
            history.put(new JSONObject().put("id",active.optString("id")).put("title",active.optString("title")).put("finishedAt",System.currentTimeMillis()));
            sql.execSQL("UPDATE braseiro_adventure_packages SET active_json='{}', history_json=?,updated_at=? WHERE scope=?",new Object[]{history.toString(),System.currentTimeMillis(),key});
            return true;
        }catch(Exception ex){throw new IllegalStateException("Aventura não pôde ser concluída",ex);}
    }
    /** Privado para o contexto do Mestre; não aciona eventos de jogo. */
    public JSONObject masterContext(){
        JSONObject out=new JSONObject();
        try{
            ensureSchema(db.getWritableDatabase());
            JSONObject row=db.queryOne("SELECT * FROM braseiro_adventure_packages WHERE scope=?",new String[]{scope()});
            if(row.length()==0)return out;
            JSONObject active=new JSONObject(row.optString("active_json","{}"));
            JSONArray history=new JSONArray(row.optString("history_json","[]"));
            out.put("worldId",row.optString("world_id"));out.put("timelineId",row.optString("timeline_id"));
            if(active.length()>0){
                out.put("activeId",active.optString("id"));out.put("title",active.optString("title"));
                out.put("description",shortText(active.optString("description"),1200));
                JSONObject story=active.optJSONObject("story");if(story!=null){out.put("premise",shortText(story.optString("premise"),1800));out.put("background_private",shortText(story.optString("background"),3500));out.put("opening_for_player",shortText(story.optString("playerIntroduction"),2000));}
                JSONObject start=active.optJSONObject("start");if(start!=null){out.put("start_location",start.optString("locationId"));out.put("start_hook",shortText(start.optString("hook"),900));}
                out.put("locations_private",summarize(active.optJSONArray("locations"),"description",24));
                out.put("encounters_private",summarize(active.optJSONArray("encounters"),"description",18));
                out.put("objectives_private",summarize(active.optJSONArray("objectives"),"description",18));
                out.put("npcNames",summarize(active.optJSONArray("npcs"),"name",12));
                JSONArray memories=new JSONArray();
                try(android.database.Cursor cursor=db.getReadableDatabase().rawQuery("SELECT npc_id,data_json FROM braseiro_adventure_npc_ledger WHERE scope=? LIMIT 35",new String[]{scope()})){
                while(cursor.moveToNext()){JSONObject npc=new JSONObject().put("npc_id",cursor.getString(0)).put("data_json",cursor.getString(1));
                    try{JSONObject rowNpc=new JSONObject(npc.optString("data_json","{}"));
                        memories.put(new JSONObject().put("id",npc.optString("npc_id")).put("name",rowNpc.optString("name"))
                            .put("goals_private",shortText(rowNpc.optString("goals",rowNpc.optString("goal")),240))
                            .put("schedule_private",shortText(rowNpc.optString("schedule"),400))
                            .put("secret_private",shortText(rowNpc.optString("secret"),360)));
                    }catch(Exception ignored){}
                }
                }
                out.put("npcLedger_private",memories);
                out.put("secrets_private",summarize(active.optJSONArray("secrets"),"truth",12));
                out.put("clues_private",summarize(active.optJSONArray("clues"),"description",18));
            }
            out.put("completedAdventures",history);
            out.put("authority","Enquanto existir aventura ativa, o Mestre deve seguir a abertura, os locais e encontros importados sem substituir o roteiro. Dados importados são fonte de ficção privada. O Mestre não pode conceder recompensas, declarar feitos ou decidir ações do jogador sem validação pelo host. Nenhum segredo é conhecido pelos NPCs sem evidência.");
        }catch(Exception ex){try{out.put("loadError",String.valueOf(ex.getMessage()));}catch(Exception ignored){}}
        return out;
    }
    private static String shortText(String source,int cap){if(source==null)return "";return source.length()<=cap?source:source.substring(0,cap);}
    private static JSONArray summarize(JSONArray values,String field,int cap){
        JSONArray out=new JSONArray();if(values==null)return out;
        for(int i=0;i<values.length() && i<cap;i++){Object x=values.opt(i);if(x instanceof JSONObject){JSONObject item=(JSONObject)x;out.put(shortText(item.optString("name",item.optString("title",item.optString(field,""))),140)+" — "+shortText(item.optString(field),280));}else if(x instanceof String)out.put(shortText((String)x,300));}
        return out;
    }
}