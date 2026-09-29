package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Simulação causal persistente para campanhas de longa duração.
 *
 * Nenhum acontecimento fica isolado: altera demografia/economia/saúde/política,
 * pode gerar pedidos de ajuda, afeta NPCs locais e propaga choques comerciais
 * por até três ondas. Tudo é determinístico a partir do estado persistido e do
 * dia da campanha; não depende de uma resposta de IA para existir.
 */
public final class WorldCausalityEngine {
    private static final int MODEL_VERSION=1;
    private static final String[] COMMODITIES={"alimentos","bebidas","madeira","minérios","ferramentas","tecidos","medicamentos","luxos","cavalos","sal"};
    private final AppDb db;

    public WorldCausalityEngine(AppDb database){db=database;ensure();}

    public synchronized JSONObject tick(){
        int day=Math.max(1,db.getSceneState().optInt("day_number",1));
        return simulateToDay(day);
    }

    public synchronized JSONObject simulateToDay(int targetDay){
        JSONObject out=new JSONObject();
        try{
            ensure();
            LivingSettlementEngine settlements=new LivingSettlementEngine(db);
            settlements.seedFromAtlas();
            WorldCausalCatalog.seed(db.getWritableDatabase());
            List<JSONObject> rows=db.queryList("SELECT * FROM settlement_states WHERE world_profile=? ORDER BY settlement_key",new String[]{profile()});
            bootstrap(rows);
            rows=db.queryList("SELECT * FROM settlement_states WHERE world_profile=? ORDER BY settlement_key",new String[]{profile()});
            JSONObject living=new LivingWorldEngine(db).state();
            int patterns=db.queryOne("SELECT COUNT(*) n FROM world_causal_patterns",null).optInt("n",0);
            int changed=0,events=0,propagated=0,npcs=0;
            ArrayDeque<Shock> shocks=new ArrayDeque<Shock>();
            for(JSONObject row:rows){
                int last=Math.max(1,row.optInt("last_causal_day",row.optInt("last_simulated_day",1)));
                if(targetDay<=last)continue;
                RegionResult rr=advanceRegion(row,last,targetDay,living,shocks);
                changed++;events+=rr.events;npcs+=rr.npcs;
            }
            propagated=propagate(shocks,targetDay,3);
            int calendarEvents=applyCalendarMilestones(rows,targetDay,living);
            JSONObject audit=audit();
            out.put("target_day",targetDay).put("calendar",WorldCalendarEngine.snapshot(targetDay,living))
                    .put("catalog_patterns",patterns).put("regions_changed",changed).put("events_generated",events)
                    .put("calendar_events",calendarEvents).put("propagated_shocks",propagated).put("npc_reactions",npcs)
                    .put("audit",audit).put("healthy",audit.optBoolean("healthy",false));
            db.addAuditEvent("causalidade_mundo","Mundo causal atualizado","dia "+targetDay+" • "+events+" evento(s) • "+propagated+" propagação(ões)","{}",out.toString(),true);
        }catch(Exception error){
            try{out.put("healthy",false).put("error",error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());}catch(Exception ignored){}
            AppLog.e("WorldCausality","Falha na simulação causal.",error);
        }
        return out;
    }

    public JSONObject fingerprint(){
        JSONObject out=new JSONObject();
        try{
            List<JSONObject> rows=db.queryList("SELECT settlement_key,population_total,adult_men,adult_women,children,elderly,displaced,soldiers,food_stock,food_production,food_demand,drink_stock,medicine_stock,price_index,health_index,disease_pressure,migration_pressure,war_exposure,trade_capacity,infrastructure,political_stability,last_causal_day,causal_state_json FROM settlement_states WHERE world_profile=? ORDER BY settlement_key",new String[]{profile()});
            StringBuilder b=new StringBuilder();
            for(JSONObject r:rows)b.append(r.toString()).append('\n');
            out.put("regions",rows.size()).put("sha256",sha256(b.toString())).put("day",db.getSceneState().optInt("day_number",1));
        }catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){}}
        return out;
    }

    public JSONObject audit(){
        JSONObject out=new JSONObject();JSONArray problems=new JSONArray();
        try{
            int patterns=db.queryOne("SELECT COUNT(*) n FROM world_causal_patterns",null).optInt("n",0);
            if(patterns<WorldCausalCatalog.EXPECTED_PATTERN_COUNT)problems.put("catálogo causal incompleto: "+patterns+"/"+WorldCausalCatalog.EXPECTED_PATTERN_COUNT);
            for(JSONObject r:db.queryList("SELECT * FROM settlement_states WHERE world_profile=?",new String[]{profile()})){
                int pop=r.optInt("population_total",0),men=r.optInt("adult_men",0),women=r.optInt("adult_women",0),children=r.optInt("children",0),elderly=r.optInt("elderly",0);
                if(pop<0||men<0||women<0||children<0||elderly<0)problems.put(r.optString("name")+": demografia negativa");
                if(men+women+children+elderly>pop+Math.max(5,pop/100))problems.put(r.optString("name")+": grupos demográficos excedem população total");
                for(String k:new String[]{"food_stock","food_production","food_demand","drink_stock","medicine_stock","price_index","trade_capacity","infrastructure"})
                    if(r.optDouble(k,0d)<0d)problems.put(r.optString("name")+": "+k+" negativo");
                for(String k:new String[]{"health_index","disease_pressure","migration_pressure","war_exposure","political_stability"})
                    if(r.optInt(k,0)<0||r.optInt(k,0)>100)problems.put(r.optString("name")+": "+k+" fora de 0..100");
            }
            out.put("healthy",problems.length()==0).put("problems",problems).put("catalog_patterns",patterns).put("fingerprint",fingerprint());
        }catch(Exception e){try{problems.put(String.valueOf(e.getMessage()));out.put("healthy",false).put("problems",problems);}catch(Exception ignored){}}
        return out;
    }

    /** Permite a eventos estruturados aplicarem consequência causal futura. */
    public static void applySettlementUpdate(AppDb db,JSONObject u)throws Exception{
        if(db==null||u==null)return;String key=u.optString("settlement_key").trim();if(key.length()==0)return;
        JSONObject row=db.queryOne("SELECT * FROM settlement_states WHERE world_profile=? AND settlement_key=?",new String[]{db.getActiveMapProfile(),key});
        if(row.length()==0)return;
        ContentValues v=new ContentValues();
        for(String field:new String[]{"prosperity","security","scarcity","unrest","criminality","political_pressure","trade","faith_influence","population_total","adult_men","adult_women","children","elderly","displaced","soldiers","labor_force","health_index","disease_pressure","migration_pressure","war_exposure","political_stability","last_causal_day"})
            if(u.has(field))v.put(field,Math.max(0,u.optInt(field)));
        for(String field:new String[]{"food_stock","food_production","food_demand","drink_stock","medicine_stock","price_index","trade_capacity","infrastructure"})
            if(u.has(field))v.put(field,Math.max(0d,u.optDouble(field)));
        if(u.has("causal_state_json"))v.put("causal_state_json",u.opt("causal_state_json") instanceof JSONObject?u.optJSONObject("causal_state_json").toString():u.optString("causal_state_json","{}"));
        v.put("updated_at",System.currentTimeMillis());
        if(v.size()>1)db.getWritableDatabase().update("settlement_states",v,"world_profile=? AND settlement_key=?",new String[]{db.getActiveMapProfile(),key});
    }

    private RegionResult advanceRegion(JSONObject row,int fromDay,int toDay,JSONObject living,ArrayDeque<Shock> shocks)throws Exception{
        JSONObject state=new JSONObject(row.toString());
        int events=0,npcs=0;
        int cursor=fromDay;
        int eventBudget=Math.min(32,Math.max(1,(toDay-fromDay+13)/14));
        while(cursor<toDay){
            int next=Math.min(toDay,cursor+30);
            applyBaseline(state,cursor,next,living);
            int eventDay=next;
            if(events<eventBudget&&shouldEvent(state,eventDay)){
                JSONObject p=pressure(state);
                JSONObject pattern=WorldCausalCatalog.choose(state.optString("settlement_key"),contextFor(state),eventDay,p);
                if(pattern.length()>0){
                    applyEffects(state,pattern.optJSONObject("effects"),eventDay);
                    recordEvent(state,pattern,eventDay);
                    enqueueShocks(state,pattern,shocks,0);
                    npcs+=nudgeNpcs(state,pattern,eventDay);
                    maybeScheduleAid(state,pattern,eventDay);
                    events++;
                }
            }
            cursor=next;
        }
        state.put("last_causal_day",toDay);
        persist(state);
        return new RegionResult(events,npcs);
    }

    private void applyBaseline(JSONObject s,int fromDay,int toDay,JSONObject living)throws Exception{
        int days=Math.max(0,toDay-fromDay);if(days<=0)return;
        int mid=fromDay+Math.max(1,days/2);
        String season=WorldCalendarEngine.seasonForDay(mid,living);
        double agricultural=WorldCalendarEngine.agriculturalMultiplier(season);
        double diseaseSeason=WorldCalendarEngine.diseaseMultiplier(season);
        int pop=Math.max(1,s.optInt("population_total",1000));
        double prod=Math.max(0d,s.optDouble("food_production",pop*1.03d));
        double demand=Math.max(1d,pop);
        double infrastructure=clamp100(s.optDouble("infrastructure",50d))/100d;
        double war=clamp100(s.optDouble("war_exposure",0d))/100d;
        double disease=clamp100(s.optDouble("disease_pressure",5d))/100d;
        double trade=clamp100(s.optDouble("trade_capacity",s.optInt("trade",50)))/100d;
        double effectiveProd=prod*agricultural*(.65d+.35d*infrastructure)*(1d-.55d*war);
        double stock=Math.max(0d,s.optDouble("food_stock",demand*45d))+effectiveProd*days-demand*days;
        double desired=demand*90d;
        stock=Math.max(0d,Math.min(desired*2d,stock));
        s.put("food_stock",round2(stock)).put("food_demand",round2(demand)).put("food_production",round2(prod));
        double coverage=stock/Math.max(1d,demand);
        int scarcity=clamp((int)Math.round(70d-coverage),0,100);
        s.put("scarcity",clamp((s.optInt("scarcity",20)*3+scarcity)/4,0,100));
        double health=clamp100(s.optDouble("health_index",70d));
        double naturalAnnual=.010d*((health-50d)/50d+1d);
        double naturalDelta=pop*naturalAnnual*days/Math.max(1,new LivingWorldEngine(db).daysPerYear());
        double famineRate=coverage<7d?(7d-coverage)/7d*.025d:0d;
        double diseaseRate=Math.max(0d,(disease*diseaseSeason-.35d))*.020d;
        double warRate=war*.014d;
        int deaths=(int)Math.round(pop*(famineRate+diseaseRate+warRate)*days/30d);
        int births=(int)Math.max(0,Math.round(naturalDelta));
        int nextPop=Math.max(20,pop+births-deaths);
        distributePopulationChange(s,pop,nextPop,deaths,war);
        int diseasePressure=clamp((int)Math.round(s.optInt("disease_pressure",5)+(diseaseSeason-1d)*8d+(coverage<10d?2d:0d)-(health>70?1d:0d)),0,100);
        s.put("disease_pressure",diseasePressure);
        s.put("health_index",clamp((int)Math.round(health-(diseasePressure>60?1:0)+(coverage>45?1:0)),0,100));
        double price=Math.max(25d,s.optDouble("price_index",100d));
        double target=100d+(s.optInt("scarcity",20)-20d)*.9d+(1d-trade)*25d+war*35d;
        s.put("price_index",round2(price*.75d+target*.25d));
        s.put("migration_pressure",clamp(s.optInt("migration_pressure",10)+(coverage<12?2:0)+(war>.35?3:0)+(diseasePressure>60?2:0)-(s.optInt("prosperity",50)>65?1:0),0,100));
        s.put("war_exposure",clamp((int)Math.round(war*100d)-(war>0?1:0),0,100));
    }

    private void applyEffects(JSONObject s,JSONObject e,int day)throws Exception{
        if(e==null)return;
        for(String k:new String[]{"prosperity","security","scarcity","unrest","criminality","political_pressure","trade","faith_influence"}){
            String deltaKey=k+"_delta";if(e.has(deltaKey))s.put(k,clamp(s.optInt(k,50)+e.optInt(deltaKey),0,100));
        }
        if(e.has("crime_delta"))s.put("criminality",clamp(s.optInt("criminality",15)+e.optInt("crime_delta"),0,100));
        if(e.has("health_delta"))s.put("health_index",clamp(s.optInt("health_index",70)+e.optInt("health_delta"),0,100));
        if(e.has("disease_delta"))s.put("disease_pressure",clamp(s.optInt("disease_pressure",5)+e.optInt("disease_delta"),0,100));
        if(e.has("migration_delta"))s.put("migration_pressure",clamp(s.optInt("migration_pressure",10)+e.optInt("migration_delta"),0,100));
        if(e.has("war_delta"))s.put("war_exposure",clamp(s.optInt("war_exposure",0)+e.optInt("war_delta"),0,100));
        if(e.has("infrastructure_delta"))s.put("infrastructure",clamp(s.optInt("infrastructure",55)+e.optInt("infrastructure_delta"),0,100));
        s.put("trade_capacity",clamp(s.optInt("trade_capacity",s.optInt("trade",50))+e.optInt("trade_delta",0),0,100));
        if(e.has("political_delta"))s.put("political_stability",clamp(s.optInt("political_stability",65)-e.optInt("political_delta"),0,100));
        scale(s,"food_production",e.optDouble("food_production_delta_pct",0d));
        scale(s,"food_stock",e.optDouble("food_stock_delta_pct",0d));
        scale(s,"drink_stock",e.optDouble("drink_stock_delta_pct",0d));
        scale(s,"medicine_stock",e.optDouble("medicine_stock_delta_pct",0d));
        s.put("price_index",Math.max(10d,round2(s.optDouble("price_index",100d)*(1d+e.optDouble("price_delta_pct",0d)/100d))));
        int pop=Math.max(1,s.optInt("population_total",1000));
        int popDelta=(int)Math.round(pop*e.optInt("population_delta_ppm",0)/1000000d);
        int nextPop=Math.max(20,pop+popDelta);
        distributePopulationChange(s,pop,nextPop,Math.max(0,-popDelta),s.optInt("war_exposure",0)/100d);
        int men=s.optInt("adult_men",Math.max(1,nextPop*27/100));
        men=Math.max(0,men+(int)Math.round(men*e.optInt("adult_men_extra_loss_ppm",0)/1000000d));
        s.put("adult_men",men);
        int soldiers=s.optInt("soldiers",Math.max(0,nextPop/80));
        soldiers=Math.max(0,soldiers+(int)Math.round(soldiers*e.optInt("soldier_loss_ppm",0)/1000000d));
        s.put("soldiers",soldiers);
        int displaced=s.optInt("displaced",0)+(int)Math.round(nextPop*e.optInt("displaced_delta_ppm",0)/1000000d);
        s.put("displaced",Math.max(0,displaced));
        rebalance(s);
        s.put("last_causal_day",day);
    }

    private void enqueueShocks(JSONObject s,JSONObject pattern,ArrayDeque<Shock> queue,int wave){
        JSONObject e=pattern.optJSONObject("effects");if(e==null)return;JSONObject c=e.optJSONObject("commodity_shock");if(c==null)return;
        java.util.Iterator<String> it=c.keys();while(it.hasNext()){String commodity=it.next();double magnitude=c.optDouble(commodity,0d);if(Math.abs(magnitude)>=.04d)queue.add(new Shock(s.optString("settlement_key"),commodity,magnitude,wave,pattern.optString("event_name")));}
    }

    private int propagate(ArrayDeque<Shock> queue,int day,int maxWave)throws Exception{
        int applied=0;Set<String> receipts=new HashSet<String>();
        List<JSONObject> regions=db.queryList("SELECT * FROM settlement_states WHERE world_profile=? ORDER BY settlement_key",new String[]{profile()});
        Map<String,List<JSONObject>> exporters=exporters(regions);
        while(!queue.isEmpty()){
            Shock shock=queue.removeFirst();if(shock.wave>=maxWave)continue;
            for(JSONObject dest:regions){
                String dk=dest.optString("settlement_key");if(dk.equals(shock.source))continue;
                JSONObject state=parse(dest.optString("causal_state_json","{}"));
                if(!arrayContains(state.optJSONArray("imports"),shock.commodity))continue;
                List<JSONObject> sources=exporters.get(shock.commodity);if(sources==null||sources.isEmpty())continue;
                JSONObject supplier=sources.get(Math.floorMod((dk+"|"+shock.commodity).hashCode(),sources.size()));
                if(!shock.source.equals(supplier.optString("settlement_key")))continue;
                String receipt="supply:"+day+":"+shock.wave+":"+shock.source+":"+dk+":"+shock.commodity;
                if(receipts.contains(receipt)||db.hasEffectReceipt(receipt))continue;
                receipts.add(receipt);
                double damp=Math.pow(.55d,shock.wave+1),impact=shock.magnitude*damp;
                ContentValues v=new ContentValues();
                int scarcity=clamp(dest.optInt("scarcity",20)+(int)Math.round(Math.max(0d,impact)*28d),0,100);
                double price=Math.max(10d,dest.optDouble("price_index",100d)*(1d+Math.max(-.20d,Math.min(.35d,impact))));
                v.put("scarcity",scarcity);v.put("price_index",round2(price));
                v.put("trade_capacity",clamp(dest.optInt("trade_capacity",dest.optInt("trade",50))-(int)Math.round(Math.max(0d,impact)*15d),0,100));
                v.put("updated_at",System.currentTimeMillis());
                db.getWritableDatabase().update("settlement_states",v,"world_profile=? AND settlement_key=?",new String[]{profile(),dk});
                db.recordEffectReceipt(receipt,"Choque de abastecimento — "+shock.commodity);
                recordSupplyHistory(shock,dest,impact,day);
                applied++;
                if(impact>.10d){
                    JSONObject destState=parse(dest.optString("causal_state_json","{}"));JSONArray exp=destState.optJSONArray("exports");
                    if(exp!=null&&exp.length()>0){String secondary=exp.optString(Math.floorMod((shock.commodity+dk).hashCode(),exp.length()));queue.add(new Shock(dk,secondary,impact*.65d,shock.wave+1,"desdobramento de "+shock.cause));}
                }
            }
        }
        return applied;
    }

    private int applyCalendarMilestones(List<JSONObject> rows,int targetDay,JSONObject living)throws Exception{
        int added=0;
        int before=Math.max(1,targetDay-35);
        JSONArray milestones=WorldCalendarEngine.milestonesBetween(before,targetDay,living);
        for(int i=0;i<milestones.length();i++){
            JSONObject m=milestones.optJSONObject(i);if(m==null)continue;int day=m.optInt("day");
            for(JSONObject r:rows){
                String key="calendar:"+profile()+":"+r.optString("settlement_key")+":"+day+":"+(m.optBoolean("year_started")?"year":m.optBoolean("season_started")?"season":"month");
                if(db.hasEffectReceipt(key))continue;
                int selector=Math.floorMod((key+"|social").hashCode(),100);
                if(m.optBoolean("month_started")&&selector>22)continue; // alguns centros celebram/recebem feira a cada mês
                String title=m.optBoolean("year_started")?"Celebrações e decisões de Ano Novo":m.optBoolean("season_started")?"Festival de "+m.optString("season_name"):"Feira de "+m.optString("month_name");
                JSONObject p=new JSONObject().put("event_name",title).put("family",m.optBoolean("year_started")?"politica":"cultura").put("severity","leve").put("severity_level",1).put("polarity",1)
                        .put("effects",WorldCausalCatalog.effectVector(m.optBoolean("year_started")?"politica":"cultura",title,1,contextFor(r)));
                JSONObject current=db.queryOne("SELECT * FROM settlement_states WHERE world_profile=? AND settlement_key=?",new String[]{profile(),r.optString("settlement_key")});
                applyEffects(current,p.optJSONObject("effects"),day);persist(current);recordEvent(current,p,day);nudgeNpcs(current,p,day);
                db.recordEffectReceipt(key,title);added++;
            }
        }
        return added;
    }

    private void recordEvent(JSONObject s,JSONObject p,int day)throws Exception{
        String eventKey="causal:"+uuid(profile()+"|"+s.optString("settlement_key")+"|"+day+"|"+p.optString("pattern_key",p.optString("event_name")));
        JSONObject effects=p.optJSONObject("effects");
        String summary=p.optString("event_name")+" em "+s.optString("name")+". O acontecimento alterou condições materiais e sociais locais; seus efeitos podem se propagar por comércio, migração, doença, política e relações entre facções.";
        ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("event_key",eventKey);v.put("day_number",day);v.put("era_label",new WorldHistoryEngine(db).worldSeed().optString("era_label","Era atual"));v.put("scope","regional");v.put("location",s.optString("name"));v.put("title",p.optString("event_name"));v.put("summary",summary);v.put("actors_json","[]");v.put("causes_json",new JSONArray().put(new JSONObject().put("pattern_key",p.optString("pattern_key")).put("family",p.optString("family")).put("context",p.optString("context"))).toString());v.put("consequences_json",new JSONArray().put(effects==null?new JSONObject():effects).toString());v.put("tags_json",new JSONArray().put("causal").put(p.optString("family")).put(p.optString("severity")).toString());v.put("significance",Math.min(100,20+p.optInt("severity_level",1)*18));v.put("source_kind","causal_simulation");v.put("source_id",s.optString("settlement_key"));v.put("canonical",1);v.put("created_at",System.currentTimeMillis());
        db.getWritableDatabase().insertWithOnConflict("world_history_events",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        db.addJournalFact(p.optString("event_name"),summary,"Mundo Vivo",eventKey,"causal_simulation");
    }

    private void recordSupplyHistory(Shock shock,JSONObject dest,double impact,int day)throws Exception{
        String key="supply_history:"+day+":"+shock.wave+":"+shock.source+":"+dest.optString("settlement_key")+":"+shock.commodity;
        String title=impact>=0?"Pressão de abastecimento de "+shock.commodity:"Alívio de abastecimento de "+shock.commodity;
        String body=(impact>=0?"A redução":"A melhora")+" do fornecimento ligado a "+shock.source+" afetou "+dest.optString("name")+", alterando disponibilidade e preços de "+shock.commodity+".";
        db.addJournalFact(title,body,"Mundo Vivo",key,"causal_supply");
    }

    private int nudgeNpcs(JSONObject s,JSONObject pattern,int day)throws Exception{
        int changed=0;String location=s.optString("name"),region=s.optString("region"),event=pattern.optString("event_name");
        List<JSONObject> npcs=db.queryList("SELECT * FROM npc_cognitive_state WHERE world_profile=? AND (current_location=? COLLATE NOCASE OR current_location=? COLLATE NOCASE) LIMIT 80",new String[]{profile(),location,region});
        for(JSONObject npc:npcs){
            int severity=pattern.optInt("severity_level",1),urgency=clamp(npc.optInt("urgency",0)+severity*8,0,100);
            ContentValues v=new ContentValues();v.put("urgency",urgency);v.put("last_action_day",Math.min(npc.optInt("last_action_day",0),day-1));
            String current=npc.optString("next_action").trim();if(current.length()==0)v.put("next_action","responder às consequências de "+event+" em "+location);
            JSONArray recent=parseArray(npc.optString("recent_events_json","[]"));recent.put(new JSONObject().put("day",day).put("event",event).put("location",location));while(recent.length()>12){JSONArray trimmed=new JSONArray();for(int i=Math.max(0,recent.length()-12);i<recent.length();i++)trimmed.put(recent.opt(i));recent=trimmed;}
            v.put("recent_events_json",recent.toString());v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("npc_cognitive_state",v,"id=? AND world_profile=?",new String[]{String.valueOf(npc.optLong("id")),profile()});changed++;
        }
        return changed;
    }

    private void maybeScheduleAid(JSONObject s,JSONObject p,int day)throws Exception{
        JSONObject e=p.optJSONObject("effects");if(e==null||e.optInt("aid_pressure",0)<35)return;
        String title="Pedido de ajuda de "+s.optString("name");
        JSONObject consequence=new JSONObject().put("knowledge_updates",new JSONArray().put(new JSONObject().put("subject",s.optString("name")).put("content","Autoridades, templos, guildas ou famílias de "+s.optString("name")+" procuram auxílio após "+p.optString("event_name")+".").put("truth_status","verdade").put("visibility","mestre").put("known_by_json",new JSONArray()).put("reveal_condition","Quando mensageiros, rumores ou contatos alcançarem a companhia.").put("source","Mundo causal")))
                .put("journal_updates",new JSONArray().put(new JSONObject().put("title",title).put("body","A crise criou necessidade concreta de alimento, cura, proteção, transporte, recursos ou mediação.").put("kind","Mundo Vivo")));
        db.addWorldEventChecked(new JSONObject().put("title",title).put("description","A população reage à crise e procura auxílio.").put("due_day",day+1+Math.floorMod((s.optString("settlement_key")+"|aid|"+day).hashCode(),3)).put("consequence_json",consequence).put("procedure_json",new JSONObject().put("executable",true).put("kind","narrative_world_consequence")));
    }

    private void bootstrap(List<JSONObject> rows)throws Exception{
        for(JSONObject s:rows){
            if(s.optInt("causal_model_version",0)>=MODEL_VERSION)continue;
            String kind=(s.optString("kind")+" "+s.optString("name")).toLowerCase(Locale.ROOT);
            int base=kind.contains("capital")?24000:kind.contains("city")||kind.contains("cidade")?9000:kind.contains("town")||kind.contains("vila")?2600:kind.contains("fort")?550:kind.contains("village")||kind.contains("aldeia")?700:1300;
            int pop=Math.max(100,(int)Math.round(base*(.86d+Math.floorMod(s.optString("settlement_key").hashCode(),29)/100d)));
            JSONObject causal=initialTradeProfile(s);
            ContentValues v=new ContentValues();v.put("population_total",pop);v.put("adult_men",(int)Math.round(pop*.27d));v.put("adult_women",(int)Math.round(pop*.28d));v.put("children",(int)Math.round(pop*.32d));v.put("elderly",Math.max(0,pop-(int)Math.round(pop*.87d)));v.put("displaced",0);v.put("soldiers",Math.max(2,pop/80));v.put("labor_force",(int)Math.round(pop*.46d));v.put("food_demand",(double)pop);v.put("food_production",pop*(.92d+s.optInt("prosperity",50)/500d));v.put("food_stock",pop*45d);v.put("drink_stock",pop*30d);v.put("medicine_stock",pop*.08d);v.put("price_index",100d);v.put("health_index",70);v.put("disease_pressure",5);v.put("migration_pressure",10);v.put("war_exposure",0);v.put("trade_capacity",s.optInt("trade",50));v.put("infrastructure",Math.max(25,Math.min(85,(s.optInt("prosperity",50)+s.optInt("security",50))/2)));v.put("political_stability",Math.max(20,100-s.optInt("political_pressure",15)));v.put("last_causal_day",Math.max(1,s.optInt("last_simulated_day",db.getSceneState().optInt("day_number",1))));v.put("causal_model_version",MODEL_VERSION);v.put("causal_state_json",causal.toString());v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("settlement_states",v,"id=? AND world_profile=?",new String[]{String.valueOf(s.optLong("id")),profile()});
        }
    }

    private JSONObject initialTradeProfile(JSONObject s)throws Exception{
        String context=contextFor(s);JSONArray exports=new JSONArray(),imports=new JSONArray();
        if("rural".equals(context)){exports.put("alimentos").put("bebidas").put("cavalos");imports.put("ferramentas").put("tecidos").put("sal");}
        else if("rio".equals(context)){exports.put("alimentos").put("bebidas").put("madeira");imports.put("ferramentas").put("tecidos").put("medicamentos");}
        else if("floresta".equals(context)){exports.put("madeira").put("medicamentos").put("alimentos");imports.put("ferramentas").put("sal").put("tecidos");}
        else if("montanha".equals(context)){exports.put("minérios").put("ferramentas").put("sal");imports.put("alimentos").put("bebidas").put("tecidos");}
        else if("capital".equals(context)||"urbano".equals(context)){exports.put("ferramentas").put("tecidos").put("luxos");imports.put("alimentos").put("bebidas").put("madeira");}
        else{exports.put("madeira").put("cavalos").put("alimentos");imports.put("ferramentas").put("tecidos").put("medicamentos");}
        return new JSONObject().put("version",MODEL_VERSION).put("context",context).put("exports",exports).put("imports",imports);
    }

    private Map<String,List<JSONObject>> exporters(List<JSONObject> regions){
        Map<String,List<JSONObject>> out=new HashMap<String,List<JSONObject>>();
        for(JSONObject r:regions){JSONObject state=parse(r.optString("causal_state_json","{}"));JSONArray exp=state.optJSONArray("exports");if(exp==null)continue;for(int i=0;i<exp.length();i++){String c=exp.optString(i);if(c.length()==0)continue;List<JSONObject> list=out.get(c);if(list==null){list=new ArrayList<JSONObject>();out.put(c,list);}list.add(r);}}
        return out;
    }

    private JSONObject pressure(JSONObject s)throws Exception{
        return new JSONObject().put("war_exposure",s.optInt("war_exposure")).put("disease_pressure",s.optInt("disease_pressure")).put("scarcity",s.optInt("scarcity")).put("unrest",s.optInt("unrest")).put("political_pressure",s.optInt("political_pressure")).put("criminality",s.optInt("criminality"));
    }

    private boolean shouldEvent(JSONObject s,int day){
        int pressure=Math.max(Math.max(s.optInt("war_exposure"),s.optInt("disease_pressure")),Math.max(s.optInt("scarcity"),s.optInt("unrest")));
        int threshold=pressure>=65?82:pressure>=40?70:58;
        return Math.floorMod((s.optString("settlement_key")+"|"+day+"|causal_event").hashCode(),100)<threshold;
    }

    private String contextFor(JSONObject s){
        String t=(s.optString("kind")+" "+s.optString("tags_json")+" "+s.optString("region")+" "+s.optString("name")).toLowerCase(Locale.ROOT);
        if(t.matches(".*(capital|restov|new stetven).*"))return "capital";
        if(t.matches(".*(river|rio|porto|port).*"))return "rio";
        if(t.matches(".*(forest|floresta|wood).*"))return "floresta";
        if(t.matches(".*(mountain|montanha|hill|colina).*"))return "montanha";
        if(t.matches(".*(farm|rural|village|aldeia|fazenda).*"))return "rural";
        if(t.matches(".*(border|fronteira|fort|posto).*"))return "fronteira";
        if(t.matches(".*(city|cidade|town|vila).*"))return "urbano";
        return "rota_comercial";
    }

    private void persist(JSONObject s)throws Exception{
        ContentValues v=new ContentValues();
        for(String k:new String[]{"prosperity","security","scarcity","unrest","criminality","political_pressure","trade","faith_influence","population_total","adult_men","adult_women","children","elderly","displaced","soldiers","labor_force","health_index","disease_pressure","migration_pressure","war_exposure","trade_capacity","infrastructure","political_stability","last_causal_day","causal_model_version"})v.put(k,s.optInt(k));
        for(String k:new String[]{"food_stock","food_production","food_demand","drink_stock","medicine_stock","price_index"})v.put(k,s.optDouble(k));
        v.put("causal_state_json",s.optString("causal_state_json","{}"));v.put("updated_at",System.currentTimeMillis());
        if(db.getWritableDatabase().update("settlement_states",v,"id=? AND world_profile=?",new String[]{String.valueOf(s.optLong("id")),profile()})!=1)throw new IllegalStateException("Falha ao persistir região causal "+s.optString("name"));
    }

    private void distributePopulationChange(JSONObject s,int oldPop,int newPop,int deaths,double war)throws Exception{
        if(oldPop<=0){s.put("population_total",newPop);return;}
        int men=Math.max(0,s.optInt("adult_men",(int)(oldPop*.27))),women=Math.max(0,s.optInt("adult_women",(int)(oldPop*.28))),children=Math.max(0,s.optInt("children",(int)(oldPop*.32))),elderly=Math.max(0,s.optInt("elderly",oldPop-men-women-children));
        if(deaths>0){
            double maleShare=Math.min(.82d,.30d+.52d*war);int maleDeaths=Math.min(men,(int)Math.round(deaths*maleShare));int remaining=Math.max(0,deaths-maleDeaths);
            men-=maleDeaths;women=Math.max(0,women-(int)Math.round(remaining*.30));children=Math.max(0,children-(int)Math.round(remaining*.35));elderly=Math.max(0,elderly-(remaining-(int)Math.round(remaining*.30)-(int)Math.round(remaining*.35)));
        }
        int known=men+women+children+elderly,delta=newPop-known;
        if(delta>0){children+=delta;}else if(delta<0){int loss=Math.min(children,-delta);children-=loss;delta+=loss;if(delta<0){loss=Math.min(elderly,-delta);elderly-=loss;delta+=loss;}}
        s.put("population_total",newPop).put("adult_men",men).put("adult_women",women).put("children",children).put("elderly",elderly);rebalance(s);
    }

    private void rebalance(JSONObject s)throws Exception{
        int pop=Math.max(0,s.optInt("population_total")),men=Math.max(0,s.optInt("adult_men")),women=Math.max(0,s.optInt("adult_women")),children=Math.max(0,s.optInt("children")),elderly=Math.max(0,s.optInt("elderly"));
        int sum=men+women+children+elderly;
        if(sum>pop){int over=sum-pop;int take=Math.min(children,over);children-=take;over-=take;if(over>0){take=Math.min(elderly,over);elderly-=take;over-=take;}if(over>0){take=Math.min(women,over);women-=take;over-=take;}if(over>0)men=Math.max(0,men-over);}
        else if(sum<pop)children+=pop-sum;
        s.put("adult_men",men).put("adult_women",women).put("children",children).put("elderly",elderly);
        s.put("labor_force",Math.max(0,(int)Math.round(men*.82d+women*.72d)));
        s.put("soldiers",Math.min(Math.max(0,s.optInt("soldiers")),men+women));
        s.put("displaced",Math.min(pop,Math.max(0,s.optInt("displaced"))));
        s.put("food_demand",Math.max(1d,pop));
    }

    private void scale(JSONObject s,String key,double pct)throws Exception{s.put(key,Math.max(0d,round2(s.optDouble(key,0d)*(1d+pct/100d))));}
    private static boolean arrayContains(JSONArray a,String v){if(a==null)return false;for(int i=0;i<a.length();i++)if(v.equals(a.optString(i)))return true;return false;}
    private static JSONObject parse(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private String profile(){return db.getActiveMapProfile();}
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private static double clamp100(double v){return Math.max(0d,Math.min(100d,v));}
    private static double round2(double v){return Math.round(v*100d)/100d;}
    private static String uuid(String raw){return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();}
    private static String sha256(String raw){try{java.security.MessageDigest d=java.security.MessageDigest.getInstance("SHA-256");byte[] b=d.digest(raw.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){return Integer.toHexString(raw.hashCode());}}

    private void ensure(){
        AppDb.ensureSixZeroSchema(db.getWritableDatabase());
        WorldCausalCatalog.seed(db.getWritableDatabase());
    }

    private static final class RegionResult {final int events,npcs;RegionResult(int e,int n){events=e;npcs=n;}}
    private static final class Shock {final String source,commodity,cause;final double magnitude;final int wave;Shock(String s,String c,double m,int w,String why){source=s;commodity=c;magnitude=m;wave=w;cause=why;}}
}
