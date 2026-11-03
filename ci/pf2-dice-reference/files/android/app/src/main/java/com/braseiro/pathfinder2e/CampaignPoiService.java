package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Camada de locais mutáveis da campanha. O catálogo canônico permanece somente leitura.
 * Esta classe nunca escreve em map_locations/map_hex_atlas/map_packages.
 */
public final class CampaignPoiService {
    public static final String SOURCE_CAMPAIGN="campaign";
    public static final String SOURCE_PROCEDURAL="procedural";
    public static final int EXPORT_VERSION=1;
    private static final Set<String> STATUS=new HashSet<String>();
    private static final Pattern PERSISTENT_PLACE=Pattern.compile("(?iu)\\b(taverna|estalagem|aldeia|acampamento|templo|santu[aá]rio|caverna|gruta|mina|forte|fortim|torre|loja|casa|esconderijo|dungeon|masmorra|fazenda|posto comercial|posto de guarda|mosteiro|ru[ií]na|ponte|mercado|quartel|ferreiro|sede)\\b");
    static { for(String s:new String[]{"active","abandoned","ruined","destroyed","occupied","rebuilding"})STATUS.add(s); }

    private final AppDb db;
    public CampaignPoiService(AppDb database){
        if(database==null)throw new IllegalArgumentException("AppDb ausente.");
        db=database;AppDb.ensureCampaignPoiSchema(db.getWritableDatabase());
    }

    public String campaignKey(){
        AppDb.ensureCampaignPoiSchema(db.getWritableDatabase());
        String profile=db.getActiveMapProfile();
        JSONObject row=db.queryOne("SELECT campaign_key FROM campaign_poi_meta WHERE world_profile=?",new String[]{profile});
        String key=row.optString("campaign_key").trim();
        if(key.length()>0)return key;
        key="campaign_"+UUID.randomUUID().toString().replace("-","");
        ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("campaign_key",key);v.put("last_generation_tick",0);long now=System.currentTimeMillis();v.put("created_at",now);v.put("updated_at",now);
        db.getWritableDatabase().insertWithOnConflict("campaign_poi_meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);
        return key;
    }

    public long currentTick(){
        JSONObject scene=db.getSceneState();long day=Math.max(1,scene.optLong("day_number",1));double hour=scene.optDouble("clock_hour",9d);return day*24L+Math.max(0,Math.min(23,(int)Math.floor(hour)));
    }

    public JSONObject createManual(JSONObject spec,String hexId)throws Exception{
        JSONObject copy=spec==null?new JSONObject():new JSONObject(spec.toString());
        copy.put("hex_id",hexId).put("source",SOURCE_CAMPAIGN).put("origin",copy.optString("origin","player_construction")).put("hidden",copy.optBoolean("hidden",false)).put("discovered",copy.has("discovered")?copy.optBoolean("discovered"):true).put("created_by",copy.optString("created_by","player"));
        return create(copy,SOURCE_CAMPAIGN);
    }

    public JSONObject createProcedural(JSONObject spec)throws Exception{
        JSONObject copy=spec==null?new JSONObject():new JSONObject(spec.toString());
        copy.put("source",SOURCE_PROCEDURAL).put("origin",copy.optString("origin","living_world")).put("created_by",copy.optString("created_by","gm"));
        if(!copy.has("hidden"))copy.put("hidden",true);if(!copy.has("discovered"))copy.put("discovered",false);
        return create(copy,SOURCE_PROCEDURAL);
    }

    private JSONObject create(JSONObject spec,String source)throws Exception{
        AppDb.ensureCampaignPoiSchema(db.getWritableDatabase());
        String profile=db.getActiveMapProfile(),campaign=campaignKey();JSONObject map=db.getMapState();String logicalMap=map.optString("logical_map_id").trim(),packageId=map.optString("map_package_id").trim();
        String name=clean(spec.optString("name"));if(name.length()==0)throw new IllegalArgumentException("O local precisa de nome.");
        String type=normalizeType(spec.optString("type","point_of_interest"));String hex=clean(spec.optString("hex_id",map.optString("current_hex_id"))).toUpperCase(Locale.US);if(hex.length()==0)throw new IllegalArgumentException("O local precisa estar ligado a um hex.");
        if(packageId.length()>0&&db.getMapHex(packageId,hex).length()==0)throw new IllegalArgumentException("Hex "+hex+" não existe no Atlas ativo.");
        String parent=clean(spec.optString("parent_poi_id"));if(parent.length()>0){JSONObject p=get(parent);if(p.length()==0)throw new IllegalArgumentException("POI pai inexistente.");if(!campaign.equals(p.optString("campaign_key")))throw new IllegalArgumentException("POI pai pertence a outra campanha.");}
        String poiId=clean(spec.optString("poi_id"));if(poiId.length()==0)poiId="poi_"+source+"_"+UUID.randomUUID().toString().replace("-","");
        if(get(poiId).length()>0)throw new IllegalArgumentException("ID de POI duplicado: "+poiId);
        boolean hidden=spec.optBoolean("hidden",false),discovered=spec.has("discovered")?spec.optBoolean("discovered"):!hidden;if(hidden&&!spec.has("discovered"))discovered=false;
        String status=safeStatus(spec.optString("status","active"));long tick=spec.optLong("created_tick",currentTick()),now=System.currentTimeMillis();
        int[] qr=hexCoordinates(hex);int q=spec.has("q")?spec.optInt("q"):qr[0],r=spec.has("r")?spec.optInt("r"):qr[1];String createdBy=clean(spec.optString("created_by",SOURCE_PROCEDURAL.equals(source)?"gm":"player"));
        ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("campaign_key",campaign);v.put("poi_id",poiId);v.put("map_id",logicalMap);v.put("name",name);v.put("name_key",normalize(name));v.put("type",type);v.put("hex_id",hex);v.put("location_id",nullIfEmpty(spec.optString("location_id")));v.put("q",q);v.put("r",r);v.put("source",source);v.put("origin",clean(spec.optString("origin",SOURCE_PROCEDURAL.equals(source)?"living_world":"campaign_discovery")));v.put("canonical",0);v.put("hidden",hidden?1:0);v.put("discovered",discovered?1:0);v.put("created_by",createdBy);v.put("created_tick",tick);v.put("status",status);v.put("description",clean(spec.optString("description")));v.put("tags_json",jsonArrayString(spec.opt("tags"),spec.optString("tags_json","[]")));v.put("faction_id",nullIfEmpty(spec.optString("faction_id")));v.put("owner_npc_id",nullIfEmpty(spec.optString("owner_npc_id")));v.put("parent_poi_id",nullIfEmpty(parent));v.put("development_level",Math.max(1,spec.optInt("development_level",1)));v.put("icon",clean(spec.optString("icon",iconFor(type))));v.put("notes",clean(spec.optString("notes")));v.put("private_notes",clean(spec.optString("private_notes")));v.put("settlement_key",nullIfEmpty(spec.optString("settlement_key")));v.put("settlement_json",jsonObjectString(spec.opt("settlement"),spec.optString("settlement_json","{}")));v.put("state_json",jsonObjectString(spec.opt("state"),spec.optString("state_json","{}")));v.put("created_at",now);v.put("updated_at",now);
        long inserted=db.getWritableDatabase().insertOrThrow("campaign_pois",null,v);if(inserted<0)throw new IllegalStateException("O POI não foi persistido.");JSONObject created=get(poiId);recordHistory(created,new JSONObject(),created,"created",createdBy,tick);if(isSettlementType(type))created=linkSettlement(created);db.addAuditEvent("atlas_campaign","POI de campanha criado",name+" • "+hex,"{}",playerSafe(created).toString(),true);return created;
    }

    public JSONObject get(String poiId){return db.queryOne("SELECT * FROM campaign_pois WHERE world_profile=? AND campaign_key=? AND poi_id=?",new String[]{db.getActiveMapProfile(),campaignKey(),clean(poiId)});}
    public List<JSONObject> listAll(){return db.queryList("SELECT * FROM campaign_pois WHERE world_profile=? AND campaign_key=? ORDER BY updated_at DESC,name COLLATE NOCASE",new String[]{db.getActiveMapProfile(),campaignKey()});}
    public List<JSONObject> listForHex(String hexId,boolean includeSecrets){String extra=includeSecrets?"":" AND hidden=0 AND discovered=1";return db.queryList("SELECT * FROM campaign_pois WHERE world_profile=? AND campaign_key=? AND hex_id=?"+extra+" ORDER BY parent_poi_id,name COLLATE NOCASE",new String[]{db.getActiveMapProfile(),campaignKey(),clean(hexId).toUpperCase(Locale.US)});}
    public List<JSONObject> visibleForAtlas(String mapId){return db.queryList("SELECT * FROM campaign_pois WHERE world_profile=? AND campaign_key=? AND canonical=0 AND hidden=0 AND discovered=1 AND (?='' OR map_id='' OR map_id=?) ORDER BY updated_at DESC,name COLLATE NOCASE",new String[]{db.getActiveMapProfile(),campaignKey(),clean(mapId),clean(mapId)});}
    public List<JSONObject> findVisibleByQuery(String query,int limit){String key=normalize(query);if(key.length()<1)return new ArrayList<JSONObject>();String like="%"+key+"%";return db.queryList("SELECT * FROM campaign_pois WHERE world_profile=? AND campaign_key=? AND hidden=0 AND discovered=1 AND (name_key LIKE ? OR lower(type) LIKE ? OR lower(tags_json) LIKE ?) ORDER BY CASE WHEN name_key=? THEN 0 WHEN name_key LIKE ? THEN 1 ELSE 2 END,name LIMIT "+Math.max(1,Math.min(100,limit)),new String[]{db.getActiveMapProfile(),campaignKey(),like,"%"+key+"%","%"+key+"%",key,key+"%"});}
    public JSONObject resolveVisibleByName(String raw){String q=normalize(raw);if(q.length()==0)return new JSONObject();List<JSONObject> all=visibleForAtlas(db.getMapState().optString("logical_map_id",""));JSONObject best=new JSONObject();int bestLen=0;for(JSONObject row:all){String key=row.optString("name_key");if(key.length()==0)continue;if(q.equals(key))return row;if(q.contains(key)&&key.length()>bestLen){best=row;bestLen=key.length();}}if(best.length()>0)return best;List<JSONObject> rows=findVisibleByQuery(raw,20);return rows.isEmpty()?new JSONObject():rows.get(0);}

    public JSONObject discover(String poiId,String reason,String actor)throws Exception{JSONObject before=requirePoi(poiId);JSONObject change=new JSONObject().put("hidden",false).put("discovered",true);return evolve(poiId,change,clean(reason).length()==0?"discovery":reason,actor);}
    public JSONObject changeStatus(String poiId,String status,String reason,String actor)throws Exception{return evolve(poiId,new JSONObject().put("status",safeStatus(status)),reason,actor);}
    public JSONObject evolve(String poiId,JSONObject changes,String reason,String actor)throws Exception{
        JSONObject before=requirePoi(poiId);ContentValues v=new ContentValues();JSONObject c=changes==null?new JSONObject():changes;String[] textKeys={"name","type","hex_id","location_id","description","faction_id","owner_npc_id","parent_poi_id","icon","notes","private_notes","settlement_key"};for(String key:textKeys)if(c.has(key)){String value=clean(c.optString(key));if("name".equals(key)){if(value.length()==0)throw new IllegalArgumentException("Nome vazio.");v.put("name",value);v.put("name_key",normalize(value));}else if("type".equals(key))v.put("type",normalizeType(value));else if("hex_id".equals(key)){String hex=value.toUpperCase(Locale.US);validateHex(hex);v.put("hex_id",hex);}else if("parent_poi_id".equals(key)){if(value.length()>0&&get(value).length()==0)throw new IllegalArgumentException("POI pai inexistente.");v.put(key,nullIfEmpty(value));}else v.put(key,nullIfEmpty(value));}
        if(c.has("hidden"))v.put("hidden",c.optBoolean("hidden")?1:0);if(c.has("discovered"))v.put("discovered",c.optBoolean("discovered")?1:0);if(c.has("status"))v.put("status",safeStatus(c.optString("status")));if(c.has("development_level"))v.put("development_level",Math.max(1,c.optInt("development_level",1)));if(c.has("tags")||c.has("tags_json"))v.put("tags_json",jsonArrayString(c.opt("tags"),c.optString("tags_json","[]")));if(c.has("settlement")||c.has("settlement_json"))v.put("settlement_json",jsonObjectString(c.opt("settlement"),c.optString("settlement_json","{}")));if(c.has("state")||c.has("state_json"))v.put("state_json",jsonObjectString(c.opt("state"),c.optString("state_json","{}")));
        if(!hasSupportedChange(c))return before;long tick=currentTick();v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("campaign_pois",v,"poi_id=? AND world_profile=? AND campaign_key=?",new String[]{poiId,db.getActiveMapProfile(),campaignKey()});JSONObject after=get(poiId);recordHistory(after,before,after,clean(reason).length()==0?"state_change":reason,clean(actor).length()==0?"gm":actor,tick);if(isSettlementType(after.optString("type")))after=linkSettlement(after);db.addAuditEvent("atlas_campaign","POI atualizado",after.optString("name")+" • "+clean(reason),playerSafe(before).toString(),playerSafe(after).toString(),true);return after;
    }

    private JSONObject linkSettlement(JSONObject poi)throws Exception{
        String key=clean(poi.optString("settlement_key"));if(key.length()==0)key="campaign-poi:"+poi.optString("poi_id");JSONObject settlement=parseObject(poi.optString("settlement_json","{}"));JSONObject existing=db.queryOne("SELECT * FROM settlement_states WHERE world_profile=? AND settlement_key=?",new String[]{db.getActiveMapProfile(),key});ContentValues s=new ContentValues();s.put("world_profile",db.getActiveMapProfile());s.put("settlement_key",key);s.put("name",poi.optString("name"));s.put("kind",poi.optString("type","settlement"));s.put("region",settlement.optString("region",existing.optString("region","")));s.put("hex_id",poi.optString("hex_id"));
        String population=clean(settlement.optString("population"));String size=clean(settlement.optString("size"));s.put("population_band",population.length()>0?population:(size.length()>0?size:existing.optString("population_band","desconhecida")));
        for(String k:new String[]{"prosperity","security"})if(settlement.has(k))s.put(k,clamp100(settlement.optInt(k,50)));if(settlement.has("market_strength"))s.put("trade",clamp100(settlement.optInt("market_strength",50)));
        s.put("resources_json",objectOrArrayString(settlement.opt("resources"),existing.optString("resources_json","{}")));s.put("tags_json",jsonArrayString(settlement.opt("tags"),poi.optString("tags_json","[]")));
        StringBuilder settlementNotes=new StringBuilder();for(String field:new String[]{"size","level","government","faction","loyalty","market","services","defense","buildings","important_npcs"}){Object raw=settlement.opt(field);String value=raw instanceof JSONArray||raw instanceof JSONObject?String.valueOf(raw):clean(settlement.optString(field));if(value.length()>0){if(settlementNotes.length()>0)settlementNotes.append(" • ");settlementNotes.append(field).append(": ").append(value);}}s.put("notes",settlementNotes.toString());s.put("last_simulated_day",Math.max(1,db.getSceneState().optInt("day_number",1)));s.put("updated_at",System.currentTimeMillis());
        if(existing.length()>0)db.getWritableDatabase().update("settlement_states",s,"id=?",new String[]{String.valueOf(existing.optLong("id"))});else db.getWritableDatabase().insertOrThrow("settlement_states",null,s);ContentValues p=new ContentValues();p.put("settlement_key",key);db.getWritableDatabase().update("campaign_pois",p,"poi_id=?",new String[]{poi.optString("poi_id")});return get(poi.optString("poi_id"));
    }

    public JSONObject exportJson()throws Exception{
        JSONObject map=db.getMapState(),out=new JSONObject();out.put("format","braseiro-pathfinder2e-campaign-pois").put("version",EXPORT_VERSION).put("system_id","pathfinder_2e").put("world_profile",db.getActiveMapProfile()).put("campaign_key",campaignKey()).put("logical_map_id",map.optString("logical_map_id")).put("exported_at",System.currentTimeMillis());out.put("pois",new JSONArray(db.queryList("SELECT * FROM campaign_pois WHERE world_profile=? AND campaign_key=? AND canonical=0 ORDER BY created_at,poi_id",new String[]{db.getActiveMapProfile(),campaignKey()})));out.put("history",new JSONArray(db.queryList("SELECT h.* FROM campaign_poi_history h JOIN campaign_pois p ON p.poi_id=h.poi_id WHERE p.world_profile=? AND p.campaign_key=? ORDER BY h.id",new String[]{db.getActiveMapProfile(),campaignKey()})));return out;
    }

    public int importJson(JSONObject root)throws Exception{
        if(root==null||!"braseiro-pathfinder2e-campaign-pois".equals(root.optString("format"))||root.optInt("version",0)!=EXPORT_VERSION)throw new IllegalArgumentException("Arquivo de POIs incompatível.");if(!"pathfinder_2e".equals(root.optString("system_id","pathfinder_2e")))throw new IllegalArgumentException("POIs de outro sistema.");
        JSONArray rows=root.optJSONArray("pois");if(rows==null)return 0;String profile=db.getActiveMapProfile(),campaign=campaignKey();JSONObject map=db.getMapState();String mapId=map.optString("logical_map_id"),packageId=map.optString("map_package_id");Set<String> ids=new HashSet<String>();for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)throw new IllegalArgumentException("Linha de POI inválida em "+i);String id=clean(row.optString("poi_id"));if(id.length()==0||!ids.add(id))throw new IllegalArgumentException("ID ausente ou duplicado em "+i);if(get(id).length()>0)throw new IllegalArgumentException("ID de POI já existe nesta campanha: "+id);if(row.optBoolean("canonical",false)||row.optInt("canonical",0)==1)throw new IllegalArgumentException("Importação recusada: POI canônico não pode entrar na camada de campanha.");String hex=clean(row.optString("hex_id")).toUpperCase(Locale.US);if(hex.length()==0||(packageId.length()>0&&db.getMapHex(packageId,hex).length()==0))throw new IllegalArgumentException("Hex inválido no POI "+id+": "+hex);String source=row.optString("source");if(!SOURCE_CAMPAIGN.equals(source)&&!SOURCE_PROCEDURAL.equals(source))throw new IllegalArgumentException("Fonte inválida no POI "+id);}
        for(int i=0;i<rows.length();i++){String parent=clean(rows.optJSONObject(i).optString("parent_poi_id"));if(parent.length()>0&&!ids.contains(parent)&&get(parent).length()==0)throw new IllegalArgumentException("parent_poi_id inexistente: "+parent);}
        SQLiteDatabase sql=db.getWritableDatabase();sql.beginTransaction();int count=0;try{for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);ContentValues v=valuesForImport(row);v.put("world_profile",profile);v.put("campaign_key",campaign);v.put("map_id",mapId);v.put("canonical",0);v.put("updated_at",System.currentTimeMillis());long inserted=sql.insertWithOnConflict("campaign_pois",null,v,SQLiteDatabase.CONFLICT_ABORT);if(inserted<0)throw new IllegalStateException("Falha ao importar POI "+row.optString("poi_id"));count++;}JSONArray history=root.optJSONArray("history");if(history!=null){for(int i=0;i<history.length();i++){JSONObject h=history.optJSONObject(i);if(h==null||!ids.contains(h.optString("poi_id")))continue;ContentValues v=valuesForImport(h);v.remove("id");sql.insert("campaign_poi_history",null,v);}}sql.setTransactionSuccessful();}finally{sql.endTransaction();}db.addAuditEvent("atlas_campaign","POIs importados",String.valueOf(count),"{}",new JSONObject().put("logical_map_id",mapId).put("count",count).toString(),true);return count;
    }

    public List<JSONObject> history(String poiId){return db.queryList("SELECT * FROM campaign_poi_history WHERE poi_id=? ORDER BY id DESC",new String[]{clean(poiId)});}

    public boolean proposalHandled(String fingerprint){return db.queryOne("SELECT proposal_key FROM campaign_poi_proposals WHERE world_profile=? AND campaign_key=? AND proposal_key=? AND status IN ('saved','ignored')",new String[]{db.getActiveMapProfile(),campaignKey(),clean(fingerprint)}).length()>0;}
    public void rememberProposal(String fingerprint,JSONObject candidate,String status){ContentValues v=new ContentValues();v.put("proposal_key",clean(fingerprint));v.put("world_profile",db.getActiveMapProfile());v.put("campaign_key",campaignKey());v.put("hex_id",candidate==null?"":clean(candidate.optString("hex_id")));v.put("source_text",candidate==null?"":clean(candidate.optString("source_text")));v.put("candidate_json",candidate==null?"{}":candidate.toString());v.put("status",clean(status));v.put("created_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().insertWithOnConflict("campaign_poi_proposals",null,v,SQLiteDatabase.CONFLICT_REPLACE);}

    public JSONObject playerProjection(){JSONObject out=new JSONObject();try{JSONArray rows=new JSONArray();for(JSONObject p:visibleForAtlas(db.getMapState().optString("logical_map_id")))rows.put(playerSafe(p));out.put("layer","CAMPAIGN+PROCEDURAL_DISCOVERED").put("pois",rows).put("secret_count_excluded",Math.max(0,listAll().size()-rows.length()));}catch(Exception ignored){AppLog.ignored("CampaignPoi","Falha ao montar projeção pública.",ignored);}return out;}
    public JSONObject masterProjection(){JSONObject out=new JSONObject();try{JSONArray pub=new JSONArray(),secret=new JSONArray();for(JSONObject p:listAll()){if(p.optInt("hidden",0)==1||p.optInt("discovered",0)==0)secret.put(p);else pub.put(playerSafe(p));}out.put("public_pois",pub).put("private_pois",secret).put("instruction","private_pois são conhecimento do Mestre; não revelar ao jogador sem descoberta ficcional válida.");}catch(Exception ignored){AppLog.ignored("CampaignPoi","Falha ao montar projeção do Mestre.",ignored);}return out;}

    public JSONObject proposeFromNarration(String narration,String hexId){
        JSONObject out=new JSONObject();
        try{
            String text=clean(narration),hex=clean(hexId).toUpperCase(Locale.US);
            if(text.length()<30||hex.length()==0)return out;
            Matcher m=PERSISTENT_PLACE.matcher(text);
            while(m.find()){
                String sentence=extractSentence(text,m.start(),m.end());
                String lower=sentence.toLowerCase(new Locale("pt","BR"));
                boolean locative=lower.matches("(?s).*(encontram|encontra|encontrou|descobrem|descobre|descobriu|chegam a|chega a|avistam|avista|há (?:uma|um)|ha (?:uma|um)|existe|ergue-se|ergue se|fica (?:na|no|à|a)|localizad[oa]|situad[oa]|no cruzamento|na estrada|à margem|a margem|diante de|ao lado de).*");
                if(!locative)continue;
                String type=normalizeType(m.group(1));
                String name=extractCandidateName(sentence,m.group(1));
                if(name.length()==0)name=extractDescriptivePlaceName(sentence,m.group(1));
                if(name.length()==0)continue;
                int score=2;
                if(lower.matches("(?s).*(chamad[oa]|conhecid[oa] como|pertence a|administrad[oa]|dono|facção|faccao|abrigo|serviço|servico|mercado|cruzamento|estrada|margem).*"))score++;
                String key=normalize(name);boolean duplicate=false;
                for(JSONObject existing:listForHex(hex,true))if(key.equals(existing.optString("name_key"))){duplicate=true;break;}
                if(duplicate)continue;
                out.put("proposed",true).put("score",score).put("name",name).put("type",type).put("hex_id",hex).put("description",sentence).put("source_text",sentence).put("origin","narrative_discovery").put("source",SOURCE_CAMPAIGN).put("hidden",false).put("discovered",true).put("created_by","gm");
                return out;
            }
        }catch(Exception ignored){AppLog.ignored("CampaignPoi","Falha ao detectar POI narrativo.",ignored);}
        return out;
    }

    /**
     * Geração consequencial: só reage a eventos/facções com sinal espacial e causal.
     * Não há sorteio cego e no máximo um novo POI por janela mínima de 72 horas de jogo.
     */
    public JSONObject applyWorldAdvance(int dayBefore,int dayAfter){
        JSONObject report=new JSONObject();
        try{
            JSONArray evolved=applyWorldEvolution(dayBefore,dayAfter);report.put("evolved",evolved).put("evolved_count",evolved.length());
            long tick=Math.max(currentTick(),Math.max(1,dayAfter)*24L);JSONObject meta=db.queryOne("SELECT * FROM campaign_poi_meta WHERE world_profile=?",new String[]{db.getActiveMapProfile()});long last=meta.optLong("last_generation_tick",0);
            if(tick-last<72)return report.put("created",false).put("reason","generation_clock_not_due");
            JSONObject candidate=worldDrivenCandidate(dayBefore,dayAfter);if(!candidate.optBoolean("eligible",false))return report.put("created",false).put("reason",candidate.optString("reason","no_world_causal_signal"));
            JSONObject created=createProcedural(candidate);ContentValues v=new ContentValues();v.put("last_generation_tick",tick);v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("campaign_poi_meta",v,"world_profile=?",new String[]{db.getActiveMapProfile()});
            return report.put("created",true).put("poi",created).put("cause",candidate.optString("cause")).put("world_context",candidate.optJSONObject("world_context"));
        }catch(Exception e){try{report.put("created",false).put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("CampaignPoi","Falha ao registrar geração do Mundo Vivo.",ignored);}return report;}
    }

    /**
     * Evolução causal de locais já persistidos. O evento muda o mesmo poi_id e deixa
     * trilha em campaign_poi_history. Eventos já aplicados são idempotentes.
     */
    private JSONArray applyWorldEvolution(int dayBefore,int dayAfter)throws Exception{
        JSONArray changed=new JSONArray();String profile=db.getActiveMapProfile(),current=db.getMapState().optString("current_hex_id");
        List<JSONObject> events=db.queryList("SELECT * FROM world_events WHERE world_profile=? AND (due_day BETWEEN ? AND ? OR resolved_at>0) ORDER BY resolved_at DESC,created_at DESC LIMIT 60",new String[]{profile,String.valueOf(Math.max(1,dayBefore)),String.valueOf(Math.max(dayBefore,dayAfter))});
        for(JSONObject event:events){
            String eventKey="world_event:"+event.optLong("id");if(historyHasReason(eventKey))continue;
            String text=(event.optString("title")+" "+event.optString("description")+" "+event.optString("consequence_json")).toLowerCase(new Locale("pt","BR"));
            if(!hasEvolutionSignal(text))continue;String hex=hexFromWorldSignal(event,current);if(hex.length()==0)continue;
            List<JSONObject> candidates=listForHex(hex,true);JSONObject target=targetPoiForEvent(event,text,candidates);if(target.length()==0)continue;
            JSONObject mutation=worldEvolutionMutation(text,target);if(mutation.length()==0)continue;
            JSONObject after=evolve(target.optString("poi_id"),mutation,eventKey+":"+evolutionReason(text),"living_world");changed.put(playerSafe(after));
            if(changed.length()>=4)break;
        }
        return changed;
    }

    private boolean historyHasReason(String eventKey){return db.queryOne("SELECT id FROM campaign_poi_history WHERE reason LIKE ? LIMIT 1",new String[]{clean(eventKey)+"%"}).length()>0;}
    private static boolean hasEvolutionSignal(String text){return text.matches("(?s).*(destru|arrasad|inc[eê]ndio|abandon|evacu|ru[ií]na|desmoron|ocup|conquist|reconstru|reergu|crescimento|expans[aã]o|prosper|rota comercial|mercado|bandid|necromant|fortific).*" );}
    private JSONObject targetPoiForEvent(JSONObject event,String text,List<JSONObject> candidates){
        try{JSONObject consequence=parseObject(event.optString("consequence_json","{}"));String explicit=clean(consequence.optString("poi_id"));if(explicit.length()>0){JSONObject p=get(explicit);if(p.length()>0)return p;}}catch(Exception ignored){AppLog.ignored("CampaignPoi","Evento sem poi_id explícito.",ignored);}
        JSONObject best=new JSONObject();int bestScore=0;for(JSONObject p:candidates){int score=0;String key=normalize(p.optString("name"));if(key.length()>3&&normalize(text).contains(key))score+=5;String type=normalize(p.optString("type"));if(type.length()>2&&normalize(text).contains(type))score+=1;if(p.optString("status","active").equals("active"))score++;if(score>bestScore){best=p;bestScore=score;}}
        if(bestScore>0)return best;return candidates.size()==1?candidates.get(0):new JSONObject();
    }
    private JSONObject worldEvolutionMutation(String text,JSONObject target)throws Exception{
        JSONObject c=new JSONObject();String type=normalizeType(target.optString("type"));int level=Math.max(1,target.optInt("development_level",1));
        if(text.matches("(?s).*(destru[ií]d|arrasad|inc[eê]ndio.*destru|aniquilad).*"))return c.put("status","destroyed");
        if(text.matches("(?s).*(abandon|evacuad|desertad).*"))return c.put("status","abandoned");
        if(text.matches("(?s).*(virou ru[ií]na|tornou-se ru[ií]na|desmoron|em ru[ií]nas).*"))return c.put("status","ruined").put("type","ruin");
        if(text.matches("(?s).*(reconstru|reergu|restaur).*"))return c.put("status",target.optString("status").equals("rebuilding")?"active":"rebuilding").put("development_level",level+1);
        if(text.matches("(?s).*(bandid.*ocup|ocupad.*bandid).*"))return c.put("status","occupied").put("type","hideout");
        if(text.matches("(?s).*(necromant.*(ocup|transform)|mausol[eé]u.*necromant).*"))return c.put("status","occupied").put("type","mausoleum");
        if(text.matches("(?s).*(ocup|conquist).*"))return c.put("status","occupied");
        if(text.matches("(?s).*(crescimento|expans[aã]o|prosper|rota comercial|mercado).*")){
            c.put("development_level",level+1).put("status","active");if(type.equals("camp")||type.equals("farm")||type.equals("cabin"))c.put("type","trading_post");else if(type.equals("trading_post")&&level>=2)c.put("type","settlement");return c;
        }
        return c;
    }
    private static String evolutionReason(String text){if(text.contains("destru")||text.contains("arrasad"))return "destroyed";if(text.contains("abandon")||text.contains("evacu"))return "abandoned";if(text.contains("reconstru")||text.contains("reergu"))return "rebuilding";if(text.contains("ocup")||text.contains("conquist"))return "occupied";if(text.contains("crescimento")||text.contains("expans")||text.contains("prosper"))return "development";return "state_change";}

    private JSONObject worldDrivenCandidate(int dayBefore,int dayAfter)throws Exception{
        JSONObject out=new JSONObject();String profile=db.getActiveMapProfile(),packageId=db.getMapState().optString("map_package_id"),current=db.getMapState().optString("current_hex_id");List<JSONObject> events=db.queryList("SELECT * FROM world_events WHERE world_profile=? AND (due_day BETWEEN ? AND ? OR resolved_at>0) ORDER BY resolved_at DESC,created_at DESC LIMIT 40",new String[]{profile,String.valueOf(Math.max(1,dayBefore)),String.valueOf(Math.max(dayBefore,dayAfter))});
        for(JSONObject event:events){String text=(event.optString("title")+" "+event.optString("description")+" "+event.optString("consequence_json")).toLowerCase(new Locale("pt","BR"));String type=worldTypeFor(text);if(type.length()==0)continue;String hex=hexFromWorldSignal(event,current);if(hex.length()==0||packageId.length()>0&&db.getMapHex(packageId,hex).length()==0)continue;String name=worldNameFor(type,event.optString("title"),hex);if(hasEquivalent(type,hex,name))continue;JSONObject candidate=out.put("eligible",true).put("name",name).put("type",type).put("hex_id",hex).put("description","Local surgido como consequência de: "+event.optString("title","evento regional")+". "+event.optString("description")).put("hidden",true).put("discovered",false).put("created_by","living_world").put("origin","world_event").put("cause","world_event:"+event.optLong("id")).put("tags",new JSONArray().put("world_generated").put("persistent"));candidate=enrichWorldCandidate(candidate,text);if(candidate.optBoolean("eligible",false))return candidate;out=new JSONObject();}
        for(JSONObject faction:db.queryList("SELECT * FROM factions WHERE world_profile=? AND active=1 AND TRIM(COALESCE(current_plan,''))<>'' ORDER BY updated_at DESC LIMIT 30",new String[]{profile})){String text=(faction.optString("current_plan")+" "+faction.optString("agenda")).toLowerCase(new Locale("pt","BR"));String type=worldTypeFor(text);String location=clean(faction.optString("location"));String hex=location.matches("(?i)[A-Z]?\\d+[._-]\\d+")?location.toUpperCase(Locale.US):"";if(type.length()==0||hex.length()==0||packageId.length()>0&&db.getMapHex(packageId,hex).length()==0)continue;String name=worldNameFor(type,faction.optString("name"),hex);if(hasEquivalent(type,hex,name))continue;JSONObject candidate=out.put("eligible",true).put("name",name).put("type",type).put("hex_id",hex).put("description","Construído/ocupado pela facção "+faction.optString("name")+" em execução do plano: "+faction.optString("current_plan")).put("hidden",true).put("discovered",false).put("created_by","living_world").put("origin","faction_plan").put("faction_id",String.valueOf(faction.optLong("id"))).put("cause","faction:"+faction.optLong("id")).put("tags",new JSONArray().put("faction").put("world_generated"));candidate=enrichWorldCandidate(candidate,text);if(candidate.optBoolean("eligible",false))return candidate;out=new JSONObject();}
        return out.put("eligible",false).put("reason","no_world_causal_signal");
    }

    /** Deterministic context gate: terrain, routes, waterways, weather, danger, population and factions. */
    private JSONObject enrichWorldCandidate(JSONObject candidate,String causalText)throws Exception{
        String packageId=db.getMapState().optString("map_package_id"),hexId=candidate.optString("hex_id"),type=normalizeType(candidate.optString("type"));JSONObject hex=db.getMapHex(packageId,hexId),scene=db.getSceneState();String terrain=normalize(hex.optString("terrain_primary")),weather=normalize(scene.optString("weather")),danger=normalize(scene.optString("danger"));boolean road=hasJsonEntries(hex.optString("roads_json")),waterway=hasJsonEntries(hex.optString("waterways_json"));int canonicalDensity=jsonCount(hex.optString("location_ids"))+jsonCount(hex.optString("nearby_location_ids")),campaignDensity=listForHex(hexId,true).size(),threatCount=jsonCount(scene.optString("threats_json"));String political=hex.optString("political_json","{}");JSONObject economy=db.queryOne("SELECT trade,prosperity,security FROM settlement_states WHERE world_profile=? AND hex_id=? ORDER BY updated_at DESC LIMIT 1",new String[]{db.getActiveMapProfile(),hexId});int regionalTrade=economy.length()==0?50:economy.optInt("trade",50),regionalProsperity=economy.length()==0?50:economy.optInt("prosperity",50);JSONObject factionPressure=db.queryOne("SELECT COUNT(*) AS n FROM factions WHERE world_profile=? AND active=1 AND location LIKE ?",new String[]{db.getActiveMapProfile(),"%"+hexId+"%"});int factionCount=factionPressure.optInt("n",0);int score=2;JSONArray reasons=new JSONArray();
        if(road){score+=2;reasons.put("road");}if(waterway){score+=1;reasons.put("waterway");}if(canonicalDensity>0){score+=Math.min(2,canonicalDensity);reasons.put("settlement_proximity");}if(campaignDensity>0)score+=1;if(factionCount>0){score++;reasons.put("faction_presence");}if(regionalTrade>=65&&(type.equals("tavern")||type.equals("trading_post")||type.equals("settlement"))){score+=2;reasons.put("regional_trade");}if(regionalProsperity>=65&&type.equals("settlement")){score++;reasons.put("regional_prosperity");}if(threatCount>0&&(type.equals("fort")||type.equals("hideout"))){score+=Math.min(2,threatCount);reasons.put("monster_or_threat_pressure");}if(political!=null&&political.trim().length()>2&&!"{}".equals(political.trim())&&!"[]".equals(political.trim())){score++;reasons.put("political_context");}if(danger.matches(".*(alto|high|perigo|hostil|amea[cç]).*")){if(type.equals("fort")||type.equals("hideout"))score+=2;else score--;}if(weather.matches(".*(tempestad|storm|neve|snow|inunda|flood).*")){if(type.equals("camp")||type.equals("farm"))score-=2;reasons.put("severe_weather");}
        if(type.equals("tavern")||type.equals("trading_post")){score+=road?3:-2;if(terrain.matches(".*(mountain|montanh|swamp|pantano|pântano).*"))score--;}
        else if(type.equals("mine")){if(terrain.matches(".*(mountain|montanh|hill|colina).*"))score+=4;else score-=4;}
        else if(type.equals("fort")){if(road)score++;if(danger.length()>0)score++;if(political.length()>2)score+=2;}
        else if(type.equals("settlement")){if(road)score+=2;if(waterway)score+=2;if(terrain.matches(".*(water|ocean|sea|mar).*"))score-=6;else if(terrain.matches(".*(mountain|montanh|swamp|pantano|pântano|desert).*"))score-=2;}
        else if(type.equals("hideout")){if(terrain.matches(".*(forest|florest|mountain|montanh|swamp|pantano|pântano|ruin).*"))score+=3;if(canonicalDensity>2)score--;}
        else if(type.equals("shrine")||type.equals("temple")){if(road||waterway||canonicalDensity>0)score+=2;}
        JSONObject context=new JSONObject().put("biome",terrain).put("terrain",terrain).put("has_road",road).put("has_waterway",waterway).put("weather",weather).put("danger",danger).put("threat_count",threatCount).put("canonical_density",canonicalDensity).put("campaign_density",campaignDensity).put("faction_count",factionCount).put("regional_trade",regionalTrade).put("regional_prosperity",regionalProsperity).put("political",political).put("causal_signal",clean(causalText)).put("suitability_score",score).put("reasons",reasons);candidate.put("world_context",context);JSONObject state=parseObject(candidate.optString("state_json","{}"));state.put("generation_context",context);candidate.put("state",state);
        if(score<3)return candidate.put("eligible",false).put("reason","world_context_incompatible");return candidate.put("eligible",true);
    }
    private static boolean hasJsonEntries(String raw){return jsonCount(raw)>0;}
    private static int jsonCount(String raw){try{String s=raw==null?"":raw.trim();if(s.startsWith("["))return new JSONArray(s).length();if(s.startsWith("{"))return new JSONObject(s).length();}catch(Exception ignored){}return 0;}

    private boolean hasEquivalent(String type,String hex,String name){String key=normalize(name);for(JSONObject p:listForHex(hex,true))if(type.equals(p.optString("type"))&&key.equals(p.optString("name_key")))return true;return false;}
    private String hexFromWorldSignal(JSONObject event,String current){String raw=event.optString("consequence_json","{}");try{JSONObject c=new JSONObject(raw);for(String key:new String[]{"hex_id","location_hex_id","current_hex_id","target_hex_id"}){String hex=clean(c.optString(key));if(hex.length()>0)return hex.toUpperCase(Locale.US);}}catch(Exception ignored){AppLog.ignored("CampaignPoi","Consequência sem JSON espacial.",ignored);}String text=event.optString("title")+" "+event.optString("description");Matcher m=Pattern.compile("(?i)\\b([A-Z]?\\d{1,3}[._-]\\d{1,3})\\b").matcher(text);if(m.find())return m.group(1).replace('_','.').replace('-','.').toUpperCase(Locale.US);return "";}
    private static String worldTypeFor(String text){if(text.matches("(?s).*(rota comercial|caravana|com[eé]rcio|mercado|estalagem).*"))return "tavern";if(text.matches("(?s).*(guerra|fronteira|invas[aã]o|guarni[cç][aã]o|militar|fortifica).*"))return "fort";if(text.matches("(?s).*(minera[cç][aã]o|mina|garimpo).*"))return "mine";if(text.matches("(?s).*(peregrina[cç][aã]o|santu[aá]rio|templo|culto|ritual).*"))return "shrine";if(text.matches("(?s).*(crescimento populacional|colonos|assentamento|aldeia|funda[cç][aã]o de cidade).*"))return "settlement";if(text.matches("(?s).*(bandido|bandidos|fora da lei).*"))return "hideout";if(text.matches("(?s).*(necromante|mausol[eé]u|mortos-vivos).*"))return "mausoleum";if(text.matches("(?s).*(ru[ií]na antiga|ru[ií]nas descobertas|local em ru[ií]nas).*"))return "ruin";if(text.matches("(?s).*(caverna descoberta|gruta descoberta|nova passagem subterr[aâ]nea).*"))return "cave";if(text.matches("(?s).*(torre abandonada|torre erguida|nova torre).*"))return "tower";if(text.matches("(?s).*(acampamento permanente|acampamento mercante).*"))return "camp";if(text.matches("(?s).*(fazenda|colheita|agricultur).*"))return "farm";return "";}
    private static String worldNameFor(String type,String cause,String hex){String base=displayType(type);String c=clean(cause);if(c.length()>36)c=c.substring(0,36).trim();return c.length()>0?base+" — "+c:base+" de "+hex;}

    private JSONObject requirePoi(String id){JSONObject p=get(id);if(p.length()==0)throw new IllegalArgumentException("POI inexistente: "+id);return p;}
    private void validateHex(String hex){String packageId=db.getMapState().optString("map_package_id");if(hex.length()==0||packageId.length()>0&&db.getMapHex(packageId,hex).length()==0)throw new IllegalArgumentException("Hex inexistente: "+hex);}
    private void recordHistory(JSONObject identity,JSONObject before,JSONObject after,String reason,String actor,long tick){try{ContentValues v=new ContentValues();v.put("poi_id",identity.optString("poi_id"));v.put("tick",tick);v.put("reason",clean(reason));v.put("responsible",clean(actor));v.put("previous_json",before==null?"{}":before.toString());v.put("current_json",after==null?"{}":after.toString());v.put("created_at",System.currentTimeMillis());db.getWritableDatabase().insertOrThrow("campaign_poi_history",null,v);}catch(Exception e){throw new IllegalStateException("Histórico do POI não foi persistido.",e);}}
    private static JSONObject playerSafe(JSONObject raw)throws Exception{JSONObject out=new JSONObject(raw.toString());out.remove("private_notes");out.remove("state_json");out.remove("campaign_key");out.put("canonical",false);return out;}
    private static ContentValues valuesForImport(JSONObject row)throws Exception{ContentValues v=new ContentValues();Iterator<String> keys=row.keys();while(keys.hasNext()){String k=keys.next();Object value=row.opt(k);if(value==null||value==JSONObject.NULL)v.putNull(k);else if(value instanceof Boolean)v.put(k,(Boolean)value?1:0);else if(value instanceof Integer)v.put(k,(Integer)value);else if(value instanceof Long)v.put(k,(Long)value);else if(value instanceof Double)v.put(k,(Double)value);else v.put(k,String.valueOf(value));}v.remove("id");return v;}
    private static String jsonArrayString(Object value,String fallback){try{if(value instanceof JSONArray)return ((JSONArray)value).toString();if(value instanceof String&&((String)value).trim().startsWith("["))return new JSONArray((String)value).toString();return new JSONArray(fallback==null?"[]":fallback).toString();}catch(Exception e){return "[]";}}
    private static String jsonObjectString(Object value,String fallback){try{if(value instanceof JSONObject)return ((JSONObject)value).toString();if(value instanceof String&&((String)value).trim().startsWith("{"))return new JSONObject((String)value).toString();return new JSONObject(fallback==null?"{}":fallback).toString();}catch(Exception e){return "{}";}}
    private static String objectOrArrayString(Object value,String fallback){if(value instanceof JSONObject||value instanceof JSONArray)return String.valueOf(value);String s=value==null?"":String.valueOf(value).trim();if(s.startsWith("{")||s.startsWith("["))return s;return fallback==null?"{}":fallback;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null||raw.trim().isEmpty()?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static boolean hasSupportedChange(JSONObject c){if(c==null)return false;String[] keys={"name","type","hex_id","location_id","description","faction_id","owner_npc_id","parent_poi_id","icon","notes","private_notes","settlement_key","hidden","discovered","status","development_level","tags","tags_json","settlement","settlement_json","state","state_json"};for(String key:keys)if(c.has(key))return true;return false;}
    private static int[] hexCoordinates(String hex){int[] out={0,0};String h=clean(hex).toUpperCase(Locale.US);Matcher m=Pattern.compile("(?i)^([A-Z]?\\d{1,4})[._-](\\d{1,4})$").matcher(h);if(!m.matches())return out;try{String first=m.group(1);String digits=first.replaceAll("[^0-9]","");out[0]=digits.length()==0?0:Integer.parseInt(digits);out[1]=Integer.parseInt(m.group(2));}catch(Exception ignored){}return out;}
    private static String nullIfEmpty(String s){String c=clean(s);return c.length()==0?null:c;}
    private static String clean(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
    private static String normalize(String s){String n=Normalizer.normalize(clean(s).toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static String normalizeType(String raw){String t=normalize(raw).replace(' ','_');if(t.length()==0)return "point_of_interest";if(t.equals("taverna")||t.equals("estalagem"))return "tavern";if(t.equals("aldeia")||t.equals("cidade")||t.equals("assentamento")||t.equals("vila"))return "settlement";if(t.equals("templo"))return "temple";if(t.equals("santuario"))return "shrine";if(t.equals("caverna")||t.equals("gruta"))return "cave";if(t.equals("forte")||t.equals("fortim"))return "fort";if(t.equals("torre"))return "tower";if(t.equals("mina"))return "mine";if(t.equals("ruina")||t.equals("ruinas"))return "ruin";if(t.equals("esconderijo"))return "hideout";if(t.equals("posto_comercial"))return "trading_post";return t;}
    private static String safeStatus(String raw){String s=normalize(raw).replace(' ','_');return STATUS.contains(s)?s:"active";}
    private static boolean isSettlementType(String type){String t=normalizeType(type);return "settlement".equals(t)||"town".equals(t)||"city".equals(t)||"village".equals(t);}
    private static int clamp100(int v){return Math.max(0,Math.min(100,v));}
    public static String iconFor(String type){String t=normalizeType(type);if(t.equals("tavern"))return "⌂";if(t.equals("settlement"))return "◆";if(t.equals("fort"))return "♜";if(t.equals("temple")||t.equals("shrine"))return "✦";if(t.equals("cave")||t.equals("mine"))return "⬟";if(t.equals("tower"))return "▲";if(t.equals("ruin"))return "⌘";if(t.equals("trading_post")||t.equals("market"))return "¤";if(t.equals("hideout"))return "●";return "•";}
    public static String displayType(String type){String t=normalizeType(type);if(t.equals("tavern"))return "Taverna";if(t.equals("settlement"))return "Assentamento";if(t.equals("fort"))return "Forte";if(t.equals("temple"))return "Templo";if(t.equals("shrine"))return "Santuário";if(t.equals("cave"))return "Caverna";if(t.equals("mine"))return "Mina";if(t.equals("tower"))return "Torre";if(t.equals("ruin"))return "Ruína";if(t.equals("trading_post"))return "Posto comercial";if(t.equals("hideout"))return "Esconderijo";if(t.equals("mausoleum"))return "Mausoléu";return type==null?"Local":type.replace('_',' ');}
    private static String extractSentence(String text,int start,int end){int a=Math.max(text.lastIndexOf('.',Math.max(0,start-1)),Math.max(text.lastIndexOf('!',Math.max(0,start-1)),text.lastIndexOf('?',Math.max(0,start-1))));int b=text.indexOf('.',end);if(b<0)b=text.indexOf('!',end);if(b<0)b=text.indexOf('?',end);if(b<0)b=Math.min(text.length(),end+220);return clean(text.substring(Math.min(text.length(),a+1),Math.min(text.length(),b+1)));}
    private static String extractDescriptivePlaceName(String sentence,String rawType){
        if(sentence==null)return "";
        String noun=clean(rawType);
        Pattern p=Pattern.compile("(?iu)\\b("+Pattern.quote(noun)+")\\b\\s+((?:no|na|nos|nas|do|da|dos|das|de|à|ao)\\s+[\\p{L}][\\p{L}'-]*(?:\\s+(?:do|da|dos|das|de|e|velha|velho|antiga|antigo|[\\p{L}][\\p{L}'-]*)){0,5})");
        Matcher m=p.matcher(sentence);
        if(!m.find())return "";
        String tail=clean(m.group(2)).replaceAll("[,;:.!?].*$","");
        if(tail.length()<4)return "";
        String base=displayType(normalizeType(noun));
        String descriptive=base+" "+tail;
        if(descriptive.length()>72)descriptive=descriptive.substring(0,72).trim();
        return descriptive;
    }
    private static String extractCandidateName(String sentence,String rawType){if(sentence==null)return "";Matcher named=Pattern.compile("(?iu)\\b(?:chamad[oa]|conhecid[oa] como)\\s+([\\p{L}][\\p{L}'-]*(?:\\s+(?:do|da|dos|das|de|e|[\\p{L}][\\p{L}'-]*)){0,5})").matcher(sentence);if(named.find())return clean(named.group(1)).replaceAll("[,.!?;:].*$","");Matcher title=Pattern.compile("(?u)\\b([A-ZÁÉÍÓÚÂÊÔÃÕÇ][\\p{L}'-]+(?:\\s+(?:do|da|dos|das|de|e|[A-ZÁÉÍÓÚÂÊÔÃÕÇ][\\p{L}'-]+)){1,4})\\b").matcher(sentence);while(title.find()){String candidate=clean(title.group(1));if(!normalize(candidate).contains(normalize(rawType))&&candidate.length()>4)return candidate;}return "";}
}
