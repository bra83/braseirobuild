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
        if(!"braseiro.adventure/v1".equals(pkg.optString("schema")))throw new IllegalArgumentException("Contrato de aventura JSON inválido.");
        if(!"dnd5e".equalsIgnoreCase(pkg.optString("system")) && !"dnd-5e".equalsIgnoreCase(pkg.optString("system")) && !"dnd5e-2024".equalsIgnoreCase(pkg.optString("system")))throw new IllegalArgumentException("Esta aventura não pertence a D&D 5e.");
        String id=pkg.optString("id");
        if(!id.matches("[a-zA-Z0-9._:-]{3,120}") || pkg.optString("title").trim().length()<3)throw new IllegalArgumentException("ID ou título de aventura inválido.");
        if(!pkg.has("story") && !pkg.has("description") && !pkg.has("start"))throw new IllegalArgumentException("A aventura precisa de uma história ou gancho.");
        SQLiteDatabase sql=db.getWritableDatabase(); ensureSchema(sql);
        String key=scope();JSONObject previous=db.queryOne("SELECT * FROM braseiro_adventure_packages WHERE scope=?",new String[]{key});
        String world=pkg.optString("worldId",key),timeline=pkg.optString("timelineId","principal");
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
                JSONObject story=active.optJSONObject("story");if(story!=null){out.put("premise",shortText(story.optString("premise"),1800));out.put("background_private",shortText(story.optString("background"),3500));}
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
                out.put("secrets_private",summarize(active.optJSONArray("secrets"),"description",12));
                out.put("clues_private",summarize(active.optJSONArray("clues"),"description",18));
            }
            out.put("completedAdventures",history);
            out.put("authority","Dados importados são fonte de ficção privada. O Mestre não pode conceder recompensas, declarar feitos ou decidir ações do jogador sem validação pelo host. Nenhum segredo é conhecido pelos NPCs sem evidência.");
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