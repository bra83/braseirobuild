package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-world, locally deterministic activity receipts for D&D (never Mystara).
 * A planned action is NOT a payout. A signed resolution commits time, receipts,
 * inventory/provisions and campaign wallet in the same SQLite transaction.
 *
 * Forage mechanics: DMG 2024 "Running Exploration", Travel Terrain/Foraging:
 * Wisdom (Survival) vs terrain DC, success 1d6+Wis modifier lb food AND
 * a separate 1d6+Wis modifier gallons of water. If the party records days
 * of resources, fractional supplies carry until one full party-day is found.
 * One attempt per character per campaign day is an explicit local
 * "one journey stage or one day" anti-farming guard, not a universal RAW hour.
 */
public final class PostStrixhavenActivitiesEngine {
    private static final String TABLE="sandbox_activity_receipts";
    private final AppDb db;
    private final DndRulesEngine rules=new DndRulesEngine();

    public PostStrixhavenActivitiesEngine(AppDb database){db=database;ensureSchema();}

    private void requireDnd(){
        if("mystara".equalsIgnoreCase(db.getActiveMapProfile()))
            throw new IllegalStateException("MYSTARA_FROZEN");
        if(db.isStrixhavenAdventureActive())
            throw new IllegalStateException("STRIXHAVEN_ACTIVE");
        if(db.getCombatState().optInt("running",0)==1)
            throw new IllegalStateException("A atividade não pode iniciar durante o combate.");
    }
    private SQLiteDatabase sql(){return db.getWritableDatabase();}
    private String profile(){return db.getActiveMapProfile();}
    private void ensureSchema(){
        sql().execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+" ("+
                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "world_profile TEXT NOT NULL,kind TEXT NOT NULL,character_id INTEGER NOT NULL,"+
                "status TEXT NOT NULL,day_number INTEGER NOT NULL,location TEXT NOT NULL,"+
                "terrain TEXT NOT NULL,dc INTEGER NOT NULL,employer TEXT NOT NULL,"+
                "agreed_gold INTEGER NOT NULL,duration_hours REAL NOT NULL,"+
                "payload_json TEXT NOT NULL,result_json TEXT NOT NULL,"+
                "created_at INTEGER NOT NULL,completed_at INTEGER NOT NULL DEFAULT 0)");
        sql().execSQL("CREATE INDEX IF NOT EXISTS idx_sandbox_activity_profile_day ON "+
                TABLE+"(world_profile,character_id,day_number,kind,status)");
    }
    private int day(){return Math.max(1,db.getSceneState().optInt("day_number",1));}
    private static JSONObject mapBinding(JSONObject map)throws Exception{
        return new JSONObject().put("map_package_id",map.optString("map_package_id",""))
                .put("map_hex_id",map.optString("current_hex_id",""));
    }
    private static void requireSameForageHex(JSONObject payload,JSONObject now){
        String packageId=payload.optString("map_package_id","").trim();
        String hex=payload.optString("map_hex_id","").trim();
        if(packageId.isEmpty()||hex.isEmpty())return; // manual scenes without a bound Atlas hex
        if(!packageId.equals(now.optString("map_package_id","").trim())
                ||!hex.equals(now.optString("current_hex_id","").trim()))
            throw new IllegalStateException("A posição no Atlas mudou desde o início da caça/coleta; cancele e planeje novamente.");
    }
    private JSONObject hero(long id){
        JSONObject h=db.getCharacter(id);
        if(id<=0||h.length()==0||h.optInt("active",0)!=1)
            throw new IllegalArgumentException("Herói ativo não encontrado.");
        return h;
    }
    private static JSONObject parse(String raw){
        try{return new JSONObject(raw==null?"{}":raw);}catch(Exception ignored){return new JSONObject();}
    }
    private static JSONObject row(Cursor c)throws Exception{
        JSONObject o=new JSONObject();
        for(int i=0;i<c.getColumnCount();i++){
            String key=c.getColumnName(i);
            switch(c.getType(i)){
                case Cursor.FIELD_TYPE_INTEGER:o.put(key,c.getLong(i));break;
                case Cursor.FIELD_TYPE_FLOAT:o.put(key,c.getDouble(i));break;
                case Cursor.FIELD_TYPE_NULL:o.put(key,JSONObject.NULL);break;
                default:o.put(key,c.getString(i));break;
            }
        }
        return o;
    }
    public JSONObject get(long id){
        ensureSchema();
        try(Cursor c=sql().rawQuery("SELECT * FROM "+TABLE+" WHERE id=? AND world_profile=?",
                new String[]{String.valueOf(id),profile()})){
            return c.moveToFirst()?row(c):new JSONObject();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public List<JSONObject> recent(int limit){
        ensureSchema();List<JSONObject> a=new ArrayList<>();
        try(Cursor c=sql().rawQuery("SELECT * FROM "+TABLE+" WHERE world_profile=? ORDER BY id DESC LIMIT "+Math.max(1,Math.min(100,limit)),new String[]{profile()})){
            while(c.moveToNext())a.add(row(c));
        }catch(Exception e){throw new IllegalStateException(e);}
        return a;
    }
    private int countSameDay(String kind,long heroId,int onDay){
        try(Cursor c=sql().rawQuery("SELECT COUNT(*) FROM "+TABLE+
                        " WHERE world_profile=? AND character_id=? AND day_number=? AND kind=? AND status!='cancelled'",
                new String[]{profile(),String.valueOf(heroId),String.valueOf(onDay),kind})){
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    /**
     * A verified Atlas hex takes priority over a terrain selected in the UI.
     * Unverified/absent hex data keeps the manual terrain workflow available.
     * This prevents a forest DC being used while the confirmed Atlas position
     * identifies a mountain or desert.
     */
    static String effectiveForageTerrain(String requested,JSONObject map,JSONObject hex){
        if(map==null||hex==null||map.optString("map_package_id","").trim().isEmpty()
                ||map.optString("current_hex_id","").trim().isEmpty())return requested;
        String canonical=hex.optString("terrain_primary","").trim();
        String status=hex.optString("review_status","").trim().toLowerCase(Locale.ROOT);
        double confidence=hex.optDouble("terrain_confidence",0d);
        boolean confirmed="verified".equals(status)
                ||("automatic".equals(status)&&Double.isFinite(confidence)&&confidence>=0.80d);
        if(!confirmed||canonical.isEmpty()||"unassigned".equalsIgnoreCase(canonical)
                ||"null".equalsIgnoreCase(canonical))return requested;
        return canonical;
    }
    private String effectiveForageTerrain(String requested){
        JSONObject map=db.getMapState();
        String packageId=map.optString("map_package_id","").trim();
        String hexId=map.optString("current_hex_id","").trim();
        if(packageId.isEmpty()||hexId.isEmpty())return requested;
        return effectiveForageTerrain(requested,map,db.getMapHex(packageId,hexId));
    }
    public static int foragingDc(String terrain){
        String t=(terrain==null?"":terrain).toLowerCase(Locale.ROOT);
        if(t.contains("floresta")||t.contains("forest")||t.contains("bosque")||t.contains("pântano")||
                t.contains("pantano")||t.contains("swamp")||t.contains("costa")||t.contains("coast"))return 10;
        if(t.contains("colina")||t.contains("hill")||t.contains("pradaria")||t.contains("grass")||
                t.contains("planície")||t.contains("planicie")||t.contains("urbano")||t.contains("urban"))return 15;
        if(t.contains("deserto")||t.contains("desert")||t.contains("ártico")||t.contains("artico")||
                t.contains("arctic")||t.contains("montanha")||t.contains("mountain")||
                t.contains("subterr")||t.contains("underdark"))return 20;
        throw new IllegalArgumentException("Terreno não identificado: confirme o tipo antes de rolar.");
    }
    private long insert(String kind,long heroId,String location,String terrain,int dc,String employer,
                        int agreedGold,double hours,JSONObject payload,String status)throws Exception{
        ContentValues v=new ContentValues();
        v.put("world_profile",profile());v.put("kind",kind);v.put("character_id",heroId);
        v.put("status",status);v.put("day_number",day());
        v.put("location",location==null?"":location.trim());v.put("terrain",terrain==null?"":terrain.trim());
        v.put("dc",dc);v.put("employer",employer==null?"":employer.trim());
        v.put("agreed_gold",agreedGold);v.put("duration_hours",hours);
        v.put("payload_json",payload==null?"{}":payload.toString());v.put("result_json","{}");
        v.put("created_at",System.currentTimeMillis());v.put("completed_at",0L);
        long id=sql().insertOrThrow(TABLE,null,v);
        if(id<=0)throw new IllegalStateException("Atividade não persistida.");
        return id;
    }
    /** Plan a local two-hour stage; its duration is a UI scheduling assumption. */
    public synchronized JSONObject planForage(long characterId,String terrain,boolean hunting){
        try{
            requireDnd();JSONObject h=hero(characterId);terrain=effectiveForageTerrain(terrain);int dc=foragingDc(terrain);
            int d=day();if(countSameDay("forage",characterId,d)>0)
                throw new IllegalStateException("Já existe tentativa de caça/coleta neste dia de campanha para este herói.");
            JSONObject scene=db.getSceneState();
            long id=insert("forage",characterId,scene.optString("location",""),terrain,dc,"",0,2d,
                    new JSONObject().put("hunting",hunting).put("source","DMG 2024 · Running Exploration · Foraging")
                         .put("rule_family","survival_foraging")
                         .put("map_package_id",db.getMapState().optString("map_package_id",""))
                         .put("map_hex_id",db.getMapState().optString("current_hex_id","")),"planned");
            return new JSONObject().put("ok",true).put("activity_id",id).put("dc",dc)
                    .put("hero",h.optString("name")).put("ability","Sabedoria (Sobrevivência)")
                    .put("hours",2d).put("rule","DMG 2024 Foraging");
        }catch(Exception e){return error(e);}
    }
    /**
     * Only the app dice callback (not Gemini) must supply d20, food d6, water d6.
     * Failed checks ignore food/water d6; all three rolls still have strict bounds.
     */
    public synchronized JSONObject resolveForage(long id,int d20,int foodD6,int waterD6){
        try{
            requireDnd();
            if(d20<1||d20>20||foodD6<1||foodD6>6||waterD6<1||waterD6>6)
                throw new IllegalArgumentException("Rolagem fora do intervalo do dado físico.");
            JSONObject task=get(id);
            if(!"forage".equals(task.optString("kind")))throw new IllegalArgumentException("Não é caça/coleta.");
            if("completed".equals(task.optString("status")))
                return new JSONObject().put("ok",true).put("idempotent",true)
                        .put("receipt",parse(task.optString("result_json")));
            if(!"planned".equals(task.optString("status")))
                throw new IllegalStateException("A caça/coleta não está pendente.");
            if(task.optInt("day_number")!=day())
                throw new IllegalStateException("O dia mudou: reagende para preservar o limite diário.");
            requireSameForageHex(parse(task.optString("payload_json","{}")),db.getMapState());
            JSONObject h=hero(task.optLong("character_id"));
            int wis=rules.modifier(h.optInt("wis",10));
            int modifier=rules.d20CheckBonus(h,"wisdom","survival");
            int total=d20+modifier;boolean success=total>=task.optInt("dc");
            int food=success?Math.max(0,foodD6+wis):0;
            int water=success?Math.max(0,waterD6+wis):0;
            JSONObject result=new JSONObject().put("kind","forage").put("activity_id",id)
                    .put("character_id",h.optLong("id")).put("character",h.optString("name"))
                    .put("terrain",task.optString("terrain")).put("dc",task.optInt("dc"))
                    .put("d20",d20).put("modifier",modifier).put("total",total)
                    .put("success",success).put("food_d6",foodD6).put("water_d6",waterD6)
                    .put("food_lb",food).put("water_gal",water).put("source","DMG 2024 · Running Exploration")
                    .put("activity_hours",task.optDouble("duration_hours",2d));
            WorldStateEngine clock=new WorldStateEngine(db);
            JSONObject tick=clock.preview("Passo 2 horas caçando e forrageando na região");
            if(tick.optString("error","").length()>0||Math.abs(tick.optDouble("hours_advanced")-2d)>.001d)
                throw new IllegalStateException("Prévia temporal inválida: "+tick.optString("error"));
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){
                @Override public void run()throws Exception{
                    JSONObject now=get(id);
                    if(!"planned".equals(now.optString("status")))throw new IllegalStateException("A atividade foi alterada.");
                    if(!clock.commit(tick))throw new IllegalStateException("World tick: "+tick.optString("commit_error"));
                    JSONObject p=db.getPartyState();
                    JSONObject supplies=parse(p.optString("company_resources_json","{}"));
                    int members=Math.max(1,db.listActiveCharacters().size());
                    double foodCarry=Math.max(0d,supplies.optDouble("foraged_food_lb_carry",0d))+food;
                    double waterCarry=Math.max(0d,supplies.optDouble("foraged_water_gal_carry",0d))+water;
                    int foodDays=(int)Math.floor(foodCarry/members+1e-9);
                    int waterDays=(int)Math.floor(waterCarry/members+1e-9);
                    supplies.put("foraged_food_lb_carry",foodCarry-foodDays*members);
                    supplies.put("foraged_water_gal_carry",waterCarry-waterDays*members);
                    supplies.put("provisions_days",Math.max(0,supplies.optInt("provisions_days",0)+foodDays));
                    supplies.put("water_days",Math.max(0,supplies.optInt("water_days",0)+waterDays));
                    p.put("company_resources_json",supplies.toString());
                    db.savePartyStateChecked(p);
                    result.put("party_size",members).put("provisions_days_added",foodDays)
                          .put("water_days_added",waterDays)
                          .put("provisions_days_after",supplies.optInt("provisions_days"))
                          .put("water_days_after",supplies.optInt("water_days"))
                          .put("calendar_day_after",db.getSceneState().optInt("day_number"));
                    ContentValues cv=new ContentValues();cv.put("status","completed");cv.put("result_json",result.toString());cv.put("completed_at",System.currentTimeMillis());
                    if(sql().update(TABLE,cv,"id=? AND world_profile=? AND status='planned'",
                            new String[]{String.valueOf(id),profile()})!=1)
                        throw new IllegalStateException("Falha no recibo de caça/coleta.");
                    JSONObject plan=new EnvironmentalDirector(db).preview("Caço e procuro água na região");
                    if(plan.optString("error","").length()>0)
                        throw new IllegalStateException("Atualização climática inválida: "+plan.optString("error"));
                    new EnvironmentalDirector(db).commitChecked(plan,"");
                    db.addAuditEvent("sandbox_exploracao","Forrageamento resolvido",
                            task.optString("location"),"{}",result.toString(),true);
                    db.addJournal(success?"Caça/coleta bem-sucedida":"Caça/coleta sem recursos",
                            "Teste "+d20+" + "+modifier+" = "+total+" contra CD "+task.optInt("dc")+
                            ". Comida "+food+" lb; água "+water+" gal. Dias completos para a companhia: +"+
                            foodDays+" comida, +"+waterDays+" água.","Mundo Vivo");
                }
            });
            return new JSONObject().put("ok",true).put("receipt",result);
        }catch(Exception e){return error(e);}
    }
    /** A wage is a signed in-game local agreement, NOT D&D RAW money. */
    public synchronized JSONObject offerJob(long characterId,String employer,int agreedGold,double hours){
        try{
            requireDnd();JSONObject h=hero(characterId);
            if(employer==null||employer.trim().length()<3||employer.length()>120)
                throw new IllegalArgumentException("Informe o contratante confirmado na ficção.");
            if(agreedGold<0||agreedGold>1000)
                throw new IllegalArgumentException("Acordo salarial em PO inválido.");
            if(!Double.isFinite(hours)||hours<1d||hours>12d||Math.abs(hours-Math.rint(hours))>.001d)
                throw new IllegalArgumentException("Turno deve durar de 1 a 12 horas.");
            if(countSameDay("job",characterId,day())>0)
                throw new IllegalStateException("Já existe turno contratado para este herói neste dia.");
            long id=insert("job",characterId,db.getSceneState().optString("location",""),
                    "",0,employer,agreedGold,hours,
                    new JSONObject().put("wage_is_local_agreement",true)
                        .put("authorization","Acordo explícito de remuneração, não tabela do D&D."),
                    "offered");
            return new JSONObject().put("ok",true).put("activity_id",id)
                    .put("employer",employer.trim()).put("agreed_gold",agreedGold)
                    .put("hours",hours).put("hero",h.optString("name"));
        }catch(Exception e){return error(e);}
    }
    public synchronized JSONObject acceptJob(long id){
        try{
            requireDnd();JSONObject task=get(id);
            if(!"job".equals(task.optString("kind")))throw new IllegalArgumentException("Contrato inexistente.");
            if("accepted".equals(task.optString("status")))return new JSONObject().put("ok",true).put("idempotent",true);
            if(!"offered".equals(task.optString("status")))throw new IllegalStateException("Contrato já decidido.");
            ContentValues cv=new ContentValues();cv.put("status","accepted");
            if(sql().update(TABLE,cv,"id=? AND world_profile=? AND status='offered'",new String[]{String.valueOf(id),profile()})!=1)
                throw new IllegalStateException("O contrato foi alterado.");
            return new JSONObject().put("ok",true).put("activity_id",id);
        }catch(Exception e){return error(e);}
    }
    public synchronized JSONObject finishJob(long id){
        try{
            requireDnd();JSONObject task=get(id);
            if(!"job".equals(task.optString("kind")))throw new IllegalArgumentException("Não é trabalho.");
            if("completed".equals(task.optString("status")))
                return new JSONObject().put("ok",true).put("idempotent",true)
                        .put("receipt",parse(task.optString("result_json")));
            if(!"accepted".equals(task.optString("status")))
                throw new IllegalStateException("O trabalho exige acordo aceito.");
            if(task.optInt("day_number")!=day())
                throw new IllegalStateException("Contrato é de outro dia; resolva o atraso com o Mestre.");
            final long heroId=task.optLong("character_id");
            hero(heroId);
            final int wage=task.optInt("agreed_gold");
            final double hours=task.optDouble("duration_hours");
            int wholeHours=(int)Math.round(hours);
            WorldStateEngine clock=new WorldStateEngine(db);
            JSONObject tick=clock.preview("Trabalho por "+wholeHours+" horas para "+task.optString("employer"));
            // WorldStateEngine has integer-hour parsing; fractional shifts are
            // intentionally prohibited until its parser supports decimal hours.
            if(Math.abs(hours-Math.rint(hours))>.001d)
                throw new IllegalArgumentException("Este motor exige turnos em horas inteiras.");
            if(tick.optString("error","").length()>0||Math.abs(tick.optDouble("hours_advanced")-hours)>.001d)
                throw new IllegalStateException("Duração não corresponde ao turno contratado.");
            JSONObject result=new JSONObject().put("kind","job").put("activity_id",id)
                    .put("character_id",heroId).put("employer",task.optString("employer"))
                    .put("hours",hours).put("gold_earned",wage).put("source","acordo_local_confirmado");
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){
                @Override public void run() throws Exception {
                    JSONObject now=get(id);
                    if(!"accepted".equals(now.optString("status")))throw new IllegalStateException("Turno já resolvido.");
                    if(!clock.commit(tick))throw new IllegalStateException("World tick: "+tick.optString("commit_error"));
                    JSONObject h=db.getCharacter(heroId);
                    JSONObject sheet=parse(h.optString("sheet_json","{}"));
                    JSONObject resources=sheet.optJSONObject("resources");if(resources==null)resources=new JSONObject();
                    int before=Math.max(0,sheet.optInt("gold",resources.optInt("gold",0)));
                    if((long)before+wage>Integer.MAX_VALUE)throw new IllegalStateException("Saldo excede limite.");
                    sheet.put("gold",before+wage);resources.put("gold",before+wage);sheet.put("resources",resources);
                    db.updateCharacterSheetChecked(heroId,sheet.toString());
                    result.put("gold_before",before).put("gold_after",before+wage)
                          .put("day_after",db.getSceneState().optInt("day_number"));
                    ContentValues cv=new ContentValues();cv.put("status","completed");
                    cv.put("completed_at",System.currentTimeMillis());cv.put("result_json",result.toString());
                    if(sql().update(TABLE,cv,"id=? AND world_profile=? AND status='accepted'",
                            new String[]{String.valueOf(id),profile()})!=1)throw new IllegalStateException("Recibo de salário duplicado.");
                    JSONObject weather=new EnvironmentalDirector(db).preview("Trabalho durante o turno");
                    if(weather.optString("error","").length()>0)throw new IllegalStateException("Clima indisponível");
                    new EnvironmentalDirector(db).commitChecked(weather,"");
                    db.addAuditEvent("sandbox_economia","Pagamento de trabalho confirmado",
                            task.optString("employer"),"{}",result.toString(),true);
                    db.addJournal("Trabalho — "+task.optString("employer"),
                            "Turno de "+hours+" h concluído. Pagamento acordado +"+wage+
                            " PO registrado uma única vez.","Mundo Vivo");
                }
            });
            return new JSONObject().put("ok",true).put("receipt",result);
        }catch(Exception e){return error(e);}
    }
    public synchronized JSONObject cancel(long id){
        try{
            requireDnd();JSONObject task=get(id);
            if(task.length()==0)throw new IllegalArgumentException("Atividade inexistente.");
            if(!"planned".equals(task.optString("status"))&&!"offered".equals(task.optString("status"))&&
                    !"accepted".equals(task.optString("status")))
                throw new IllegalStateException("A atividade já tem resultado.");
            ContentValues v=new ContentValues();v.put("status","cancelled");
            if(sql().update(TABLE,v,"id=? AND world_profile=? AND status IN ('planned','offered','accepted')",
                    new String[]{String.valueOf(id),profile()})!=1)throw new IllegalStateException("Cancelamento falhou.");
            return new JSONObject().put("ok",true).put("cancelled",true);
        }catch(Exception e){return error(e);}
    }
    private static JSONObject error(Exception e){
        JSONObject o=new JSONObject();
        try{o.put("ok",false).put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
        catch(Exception ignored){}
        return o;
    }
}