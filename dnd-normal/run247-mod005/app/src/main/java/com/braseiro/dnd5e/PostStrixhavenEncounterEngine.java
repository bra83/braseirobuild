package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Non-railroading emergent encounter receipts driven by canonical game time.
 * Offers are optional; no NPC HP, combat, money, quests or conditions are
 * invented by the randomizer. A hostile encounter goes through the ordinary
 * Master/Combat gateway before any mechanical outcome.
 *
 * The chance and flavor table are Braseiro local pacing policy, NOT D&D RAW.
 */
public final class PostStrixhavenEncounterEngine {
    private static final String TABLE="sandbox_encounter_receipts";
    private final AppDb db;
    public PostStrixhavenEncounterEngine(AppDb database){db=database;ensureSchema();}
    private SQLiteDatabase sql(){return db.getWritableDatabase();}
    private String profile(){return db.getActiveMapProfile();}
    private void ensureSchema(){
        sql().execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+" ("+
                "id INTEGER PRIMARY KEY AUTOINCREMENT,world_profile TEXT NOT NULL,"+
                "trigger_key TEXT NOT NULL,category TEXT NOT NULL,status TEXT NOT NULL,"+
                "title TEXT NOT NULL,description TEXT NOT NULL,"+
                "location TEXT NOT NULL,terrain TEXT NOT NULL,weather TEXT NOT NULL,"+
                "day_number INTEGER NOT NULL,clock_hour REAL NOT NULL,"+
                "quest_id INTEGER NOT NULL DEFAULT 0,"+
                "outcome_json TEXT NOT NULL DEFAULT '{}',created_at INTEGER NOT NULL,"+
                "resolved_at INTEGER NOT NULL DEFAULT 0,"+
                "UNIQUE(world_profile,trigger_key))");
        sql().execSQL("CREATE INDEX IF NOT EXISTS idx_sandbox_encounters_status ON "+
                TABLE+"(world_profile,status,id)");
    }
    private boolean applicable(){
        if("mystara".equalsIgnoreCase(profile()))return false;
        String campaign=db.getCampaign().optString("adventure_profile","").toLowerCase(Locale.ROOT);
        return !campaign.contains("strixhaven");
    }
    private static JSONObject row(Cursor c)throws Exception{
        JSONObject o=new JSONObject();for(int i=0;i<c.getColumnCount();i++){
            String key=c.getColumnName(i);
            switch(c.getType(i)){
                case Cursor.FIELD_TYPE_INTEGER:o.put(key,c.getLong(i));break;
                case Cursor.FIELD_TYPE_FLOAT:o.put(key,c.getDouble(i));break;
                case Cursor.FIELD_TYPE_NULL:o.put(key,JSONObject.NULL);break;
                default:o.put(key,c.getString(i));break;
            }
        }return o;
    }
    public JSONObject get(long id){
        try(Cursor c=sql().rawQuery("SELECT * FROM "+TABLE+" WHERE world_profile=? AND id=?",
                new String[]{profile(),String.valueOf(id)})){return c.moveToFirst()?row(c):new JSONObject();}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public List<JSONObject> recent(int limit){
        List<JSONObject> list=new ArrayList<>();
        try(Cursor c=sql().rawQuery("SELECT * FROM "+TABLE+" WHERE world_profile=? ORDER BY id DESC LIMIT "+
                Math.max(1,Math.min(50,limit)),new String[]{profile()})){
            while(c.moveToNext())list.add(row(c));
        }catch(Exception e){throw new IllegalStateException(e);}
        return list;
    }
    private int unresolvedCount(){
        try(Cursor c=sql().rawQuery("SELECT COUNT(*) FROM "+TABLE+
                " WHERE world_profile=? AND status IN ('offered','engaged','combat_pending')",
                new String[]{profile()})){return c.moveToFirst()?c.getInt(0):0;}
    }
    private String confirmedTerrain(){
        JSONObject map=db.getMapState();
        String packageId=map.optString("map_package_id","");
        String hex=map.optString("current_hex_id","");
        if(packageId.length()==0||hex.length()==0)return "";
        JSONObject h=db.getMapHex(packageId,hex);
        return h.optString("terrain_primary","");
    }
    private static String[] encounter(String terrain,boolean night,int index){
        String t=(terrain==null?"":terrain).toLowerCase(Locale.ROOT);
        boolean urban=t.contains("urban")||t.contains("cidade")||t.contains("vila");
        boolean forest=t.contains("floresta")||t.contains("forest")||t.contains("bosque");
        boolean mountain=t.contains("montanha")||t.contains("mountain")||t.contains("colina");
        boolean coast=t.contains("costa")||t.contains("coast")||t.contains("rio");
        String[][] options;
        if(urban)options=new String[][]{
            {"social","Confusão na praça","Dois moradores disputam a passagem e pedem testemunhas; ninguém exige a intervenção do grupo."},
            {"oportunidade","Pedido de auxílio","Um trabalhador procura ajuda para resolver um contratempo local; termos e recompensas ainda precisam ser negociados."},
            {"mistério","Mensagem extraviada","Um bilhete sem destinatário chega às mãos de um mensageiro; quem quiser pode investigar a origem."},
            {"cotidiano","Mudança de turno","Um serviço local encerra suas atividades enquanto outro começa; os habitantes continuam a própria rotina."}
        };
        else if(forest)options=new String[][]{
            {"fauna","Rastros recentes","Sinais de animais atravessam a trilha; aproximar-se ou seguir os rastros é escolha do grupo."},
            {"mistério","Acampamento abandonado","As marcas de uma antiga fogueira aparecem no terreno; não há prova de perigo iminente."},
            {"social","Viajante discreto","Uma pessoa observa a rota à distância; ela não ataca sem motivo."},
            {"risco","Sons além das árvores","Ruídos distantes sugerem movimento, sem confirmar a identidade ou hostilidade de qualquer criatura."}
        };
        else if(mountain)options=new String[][]{
            {"ambiente","Passagem instável","Pedras soltas dificultam uma trilha secundária; o grupo pode contornar ou examinar com cuidado."},
            {"social","Guia na trilha","Um viajante experiente oferece informações sobre passagens; nenhum pagamento é presumido."},
            {"mistério","Marcas na rocha","Inscrições gastas pelo tempo se destacam no caminho; investigar é opcional."},
            {"risco","Sinal de movimento","Uma silhueta surge em uma crista distante; ainda não se sabe se representa perigo."}
        };
        else if(coast)options=new String[][]{
            {"social","Barco em dificuldade","Marinheiros discutem como solucionar um problema no cais; o grupo pode passar adiante."},
            {"oportunidade","Carga atrasada","Uma carga aguarda transporte e alguém procura ajuda sob condições ainda não negociadas."},
            {"ambiente","Variação da corrente","A água altera a segurança da passagem; a rota pode ser revista."},
            {"mistério","Objeto na margem","Algo foi deixado pela corrente; sua natureza e procedência permanecem desconhecidas."}
        };
        else options=new String[][]{
            {"social","Viajantes à distância","Um pequeno grupo segue sua própria rota; ambos podem prosseguir sem interação."},
            {"ambiente","Alteração no caminho","O relevo apresenta um obstáculo menor que pode ser contornado."},
            {"mistério","Vestígios desconhecidos","Marcas antigas sugerem passagem anterior; o jogador decide investigar ou ignorar."},
            {"oportunidade","Trabalho eventual","Alguém procura ajuda, sem obrigação de aceitar ou recompensa automática."}
        };
        String[] result=options[Math.floorMod(index,options.length)];
        if(night&&"cotidiano".equals(result[0]))
            return new String[]{"ambiente","Luzes depois de escurecer","A atividade de moradores diminui com a noite; luzes isoladas indicam locais habitados."};
        return result;
    }
    /**
     * Called ONCE inside WorldStateEngine's SQL transaction after canonical
     * day/hour was written. A 6-hour game-time boundary triggers a bounded,
     * seeded possibility; queries, zero-time actions and protected campaigns
     * never generate encounters. No forced attack.
     */
    public JSONObject onWorldTick(JSONObject tick)throws Exception{
        if(tick==null||!applicable()||!tick.optBoolean("living_world_enabled",true))
            return new JSONObject().put("offered",false).put("reason","inactive_world_profile");
        double hours=tick.optDouble("hours_advanced",0d);
        if(!Double.isFinite(hours)||hours<=0d||db.getCombatState().optInt("running",0)==1)
            return new JSONObject().put("offered",false).put("reason","no_world_time");
        double before=(Math.max(1,tick.optInt("day_before",1))-1)*24d+tick.optDouble("clock_hour_before",9d);
        double after=(Math.max(1,tick.optInt("day_after",1))-1)*24d+tick.optDouble("clock_hour_after",9d);
        long beforeBucket=(long)Math.floor(before/6d);
        long afterBucket=(long)Math.floor(after/6d);
        if(afterBucket<=beforeBucket)
            return new JSONObject().put("offered",false).put("reason","not_due");
        if(unresolvedCount()>=2)
            return new JSONObject().put("offered",false).put("reason","pending_player_choices");
        JSONObject scene=db.getSceneState(),map=db.getMapState();
        String terrain=confirmedTerrain();
        String location=scene.optString("location","");
        String weather=scene.optString("weather","");
        String hex=map.optString("current_hex_id","");
        String trigger=profile()+"|"+afterBucket+"|"+hex+"|"+location;
        long seed=(long)trigger.hashCode()*1103515245L+12345L;
        Random rng=new Random(seed);
        String t=terrain.toLowerCase(Locale.ROOT);
        boolean urban=t.contains("urban")||t.contains("cidade")||t.contains("vila");
        int chance=urban?16:27;
        String w=weather.toLowerCase(Locale.ROOT);
        if(w.contains("tempest")||w.contains("nevasca")||w.contains("forte"))chance+=7;
        if(rng.nextInt(100)>=chance)
            return new JSONObject().put("offered",false).put("reason","quiet_period").put("probability_percent",chance);
        boolean night=scene.optDouble("clock_hour",9d)<6d||scene.optDouble("clock_hour",9d)>=20d;
        String[] selection=encounter(terrain,night,rng.nextInt(1000));
        ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("trigger_key",trigger);
        v.put("category",selection[0]);v.put("status","offered");v.put("title",selection[1]);
        v.put("description",selection[2]);v.put("location",location);v.put("terrain",terrain);
        v.put("weather",weather);v.put("day_number",scene.optInt("day_number",1));
        v.put("clock_hour",scene.optDouble("clock_hour",9d));v.put("created_at",System.currentTimeMillis());
        long id=sql().insertWithOnConflict(TABLE,null,v,SQLiteDatabase.CONFLICT_IGNORE);
        JSONObject out=new JSONObject().put("offered",id>0).put("encounter_id",id)
                .put("category",selection[0]).put("title",selection[1])
                .put("description",selection[2]).put("probability_percent",chance)
                .put("requires_player_choice",true).put("combat_started",false)
                .put("automatic_rewards",false);
        if(id>0)db.addAuditEvent("mundo_vivo","Encontro opcional",
                selection[1], "{}",out.toString(),true);
        return out;
    }
    public synchronized JSONObject choose(long id,String choice,String outcome){
        JSONObject response=new JSONObject();
        try{
            if(!applicable())throw new IllegalStateException("O encontro não pertence à campanha aberta.");
            String key=(choice==null?"":choice).trim().toLowerCase(Locale.ROOT);
            if(!"ignore".equals(key)&&!"investigate".equals(key)&&!"combat".equals(key)&&!"resolve".equals(key))
                throw new IllegalArgumentException("Escolha inválida.");
            JSONObject before=get(id);
            if(before.length()==0)throw new IllegalArgumentException("Encontro inexistente.");
            String old=before.optString("status");
            if("ignored".equals(old)||"resolved".equals(old))
                return response.put("ok",true).put("idempotent",true).put("status",old);
            if("combat_pending".equals(old)&&!"resolve".equals(key))
                throw new IllegalStateException("Combate já encaminhado ao motor de regras.");
            if("resolve".equals(key)&&!"engaged".equals(old))
                throw new IllegalStateException("Investigue o encontro antes de concluir.");
            if(!"offered".equals(old)&&!"engaged".equals(old)&&!"combat_pending".equals(old))
                throw new IllegalStateException("Estado do encontro inválido.");
            if("resolve".equals(key)&&"combat_pending".equals(old))
                throw new IllegalStateException("O combate exige resultado verificado pelo motor de combate.");
            final String next="ignore".equals(key)?"ignored":
                    "investigate".equals(key)?"engaged":"combat".equals(key)?"combat_pending":"resolved";
            final JSONObject receipt=new JSONObject().put("encounter_id",id).put("choice",key)
                    .put("status",next).put("outcome",outcome==null?"":outcome.trim())
                    .put("requires_mechanics","combat".equals(key))
                    .put("gold_delta",0).put("xp_delta",0)
                    .put("items_granted",0).put("damage_applied",0);
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){
                @Override public void run()throws Exception{
                    ContentValues cv=new ContentValues();cv.put("status",next);
                    cv.put("outcome_json",receipt.toString());
                    if("ignored".equals(next)||"resolved".equals(next))cv.put("resolved_at",System.currentTimeMillis());
                    if(sql().update(TABLE,cv,"id=? AND world_profile=? AND status=?",
                            new String[]{String.valueOf(id),profile(),old})!=1)
                        throw new IllegalStateException("O encontro foi alterado.");
                    db.addJournal("Encontro — "+before.optString("title"),
                            before.optString("description")+"\n\nEscolha: "+key+". "+
                            (outcome==null?"":outcome.trim())+
                            "\nSem dano, XP ou recompensa concedidos por este registro.",
                            "Mundo Vivo");
                    db.addAuditEvent("sandbox_encontro","Encontro — "+next,
                            before.optString("title"),before.toString(),receipt.toString(),true);
                }
            });
            return response.put("ok",true).put("status",next).put("receipt",receipt)
                    .put("requires_combat_gateway","combat".equals(key));
        }catch(Exception e){
            try{return response.put("ok",false).put("error",e.getMessage()==null?"Falha":e.getMessage());}
            catch(Exception ignored){return new JSONObject();}
        }
    }
}