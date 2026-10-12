package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.File;
import java.io.InputStream;

public class AppDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "braseiro_pathfinder2e.db";
    private static final int DB_VERSION = 59;
    public static final String LOCAL_MANUAL_STORE = "local-v4-content-structured";
    private volatile String lastDatabaseError = "";
    private volatile String activeMapProfile = "pf2";
    private final Context appContext;
    private volatile NpcPersonalityCatalog npcPersonalityCatalog;

    public AppDb(Context context) { this(context,DB_NAME); }
    /** Construtor de sandbox usado pelo verificador de round-trip 5.0. */
    AppDb(Context context,String databaseName) {
        super(context,(databaseName==null||databaseName.trim().isEmpty())?DB_NAME:databaseName,null,DB_VERSION);
        appContext=context.getApplicationContext();
        setWriteAheadLoggingEnabled(true);
    }

    /** Trabalho persistente que deve ser concluído por inteiro ou revertido. */
    public interface CheckedTransaction {
        void run() throws Exception;
    }

    /**
     * Executa todas as gravações no mesmo SQLiteDatabase. Chamadas aninhadas
     * reutilizam a transação externa, evitando beginTransaction() aninhado.
     */
    public synchronized void runInTransactionChecked(CheckedTransaction work) throws Exception {
        if(work==null)throw new IllegalArgumentException("Transação ausente.");
        SQLiteDatabase sql=getWritableDatabase();
        boolean owner=!sql.inTransaction();
        if(owner)sql.beginTransaction();
        try{
            work.run();
            if(owner)sql.setTransactionSuccessful();
        }finally{
            if(owner)sql.endTransaction();
        }
    }

    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        try { db.execSQL("PRAGMA busy_timeout=8000"); }
        catch (Exception e) { AppLog.w("AppDb","Não foi possível ampliar a espera por gravações concorrentes.",e); }
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE campaign (id INTEGER PRIMARY KEY, name TEXT, rules_profile TEXT, tone TEXT, feats INTEGER, multiclass INTEGER, flanking INTEGER, notes TEXT, updated_at INTEGER)");
        db.execSQL("INSERT INTO campaign(id,name,rules_profile,tone,feats,multiclass,flanking,notes,updated_at) VALUES(1,'Nova Campanha','Pathfinder 2e Remaster','Exploração e presságios',1,0,1,'',0)");
        db.execSQL("CREATE TABLE characters (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT DEFAULT 'pf2', name TEXT NOT NULL, species TEXT, class_name TEXT, level INTEGER, hp INTEGER, max_hp INTEGER, ac INTEGER, str INTEGER, dex INTEGER, con INTEGER, int_score INTEGER, wis INTEGER, cha INTEGER, active INTEGER DEFAULT 1, notes TEXT, token_uri TEXT, background TEXT, visual_description TEXT, proficiencies TEXT, equipment TEXT, spells TEXT, rules_source TEXT, sheet_json TEXT DEFAULT '{}', created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE journal (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT DEFAULT 'pf2', title TEXT, body TEXT, kind TEXT, canonical_fact INTEGER DEFAULT 1, event_key TEXT DEFAULT '', source_kind TEXT DEFAULT 'system', created_at INTEGER)");
        db.execSQL("CREATE TABLE combatants (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT DEFAULT 'pf2', name TEXT, initiative INTEGER, hp INTEGER, max_hp INTEGER, temp_hp INTEGER DEFAULT 0, ac INTEGER, conditions TEXT, condition_timers_json TEXT DEFAULT '[]', concentration TEXT DEFAULT '', reaction_used INTEGER DEFAULT 0, death_success INTEGER DEFAULT 0, death_fail INTEGER DEFAULT 0, resistances TEXT DEFAULT '', vulnerabilities TEXT DEFAULT '', immunities TEXT DEFAULT '', resources_json TEXT DEFAULT '{}', source_kind TEXT DEFAULT '', source_id INTEGER DEFAULT 0, token_uri TEXT DEFAULT '', is_player INTEGER, active_turn INTEGER DEFAULT 0, sort_order INTEGER, created_at INTEGER)");
        db.execSQL("CREATE TABLE combat_state (id INTEGER PRIMARY KEY, round_no INTEGER, turn_index INTEGER, running INTEGER)");
        db.execSQL("INSERT INTO combat_state(id,round_no,turn_index,running) VALUES(1,1,0,0)");
        db.execSQL("CREATE TABLE manuals (id INTEGER PRIMARY KEY AUTOINCREMENT, drive_id TEXT UNIQUE, title TEXT, category TEXT, rule_profile TEXT, enabled INTEGER DEFAULT 1, mime_type TEXT, size_bytes INTEGER, modified_time TEXT, local_uri TEXT, source TEXT, indexed INTEGER DEFAULT 0, gemini_uri TEXT, gemini_name TEXT, gemini_expires INTEGER DEFAULT 0, ai_selected INTEGER DEFAULT 0, rag_document TEXT DEFAULT '', rag_status TEXT DEFAULT 'not_indexed', rag_indexed_at INTEGER DEFAULT 0, rag_error TEXT DEFAULT '', rag_modified_time TEXT DEFAULT '', rag_store_name TEXT DEFAULT '', rag_part_count INTEGER DEFAULT 0, rag_parts_ready INTEGER DEFAULT 0, rag_page_count INTEGER DEFAULT 0, rag_source_hash TEXT DEFAULT '', rag_progress TEXT DEFAULT '', rag_split_mode TEXT DEFAULT '', source_root TEXT DEFAULT '', last_seen INTEGER DEFAULT 0, availability TEXT DEFAULT 'available', last_verified_at INTEGER DEFAULT 0, created_at INTEGER)");
        createManualRagPartTable(db);
        createManualLocalIndexTables(db);
        db.execSQL("CREATE TABLE dice_history (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL DEFAULT 'pf2', formula TEXT, detail TEXT, total INTEGER, mode TEXT, actor_character_id INTEGER DEFAULT 0, actor_name TEXT DEFAULT '', test_key TEXT DEFAULT '', action_signature TEXT DEFAULT '', source_rule_id TEXT DEFAULT '', procedure_id TEXT DEFAULT '', scene_fingerprint TEXT DEFAULT '', campaign_fingerprint TEXT DEFAULT '', target_signature TEXT DEFAULT '', consequence_key TEXT DEFAULT '', modifier INTEGER DEFAULT 0, modifier_source TEXT DEFAULT '', roll_mode TEXT DEFAULT 'normal', natural_roll INTEGER DEFAULT 0, critical INTEGER DEFAULT 0, critical_failure INTEGER DEFAULT 0, outcome TEXT DEFAULT '', cancelled_at INTEGER DEFAULT 0, consumed_at INTEGER DEFAULT 0, created_at INTEGER)");
        db.execSQL("CREATE TABLE tokens (id INTEGER PRIMARY KEY AUTOINCREMENT, drive_id TEXT UNIQUE, name TEXT, label TEXT, species TEXT, creature_type TEXT, role TEXT, category TEXT, tags TEXT, mime_type TEXT, size_bytes INTEGER, modified_time TEXT, thumbnail_link TEXT, local_uri TEXT, source TEXT, source_root TEXT DEFAULT '', last_seen INTEGER, enabled INTEGER DEFAULT 1)");
        createTokenPdfTables(db);
        db.execSQL("CREATE TABLE map_state (id INTEGER PRIMARY KEY, image_uri TEXT, overlay_uri TEXT, manifest_json TEXT, map_package_id TEXT, current_hex_id TEXT, overlay_visible INTEGER DEFAULT 1, reveals_json TEXT DEFAULT '{}', grid_type TEXT, grid_size REAL, fog_enabled INTEGER, token_x REAL, token_y REAL, scale REAL, offset_x REAL, offset_y REAL, position_locked INTEGER DEFAULT 0, destination_hex_id TEXT DEFAULT '', route_json TEXT DEFAULT '[]', route_owner TEXT DEFAULT 'Jogador', regions_overlay_visible INTEGER DEFAULT 1, political_overlay_visible INTEGER DEFAULT 0)");
        db.execSQL("INSERT INTO map_state(id,image_uri,overlay_uri,manifest_json,map_package_id,current_hex_id,overlay_visible,reveals_json,grid_type,grid_size,fog_enabled,token_x,token_y,scale,offset_x,offset_y,position_locked,destination_hex_id,route_json,route_owner) VALUES(1,'','','','','',1,'{}','Quadrada',64,0,0.5,0.5,1,0,0,0,'','[]','Jogador')");
        createMapAtlasTables(db);
        createMapNoteTables(db);
        ensureCampaignPoiSchema(db);
        db.execSQL("CREATE TABLE story_state (id INTEGER PRIMARY KEY, started INTEGER DEFAULT 0, last_narration TEXT, visual_prompt TEXT, options_json TEXT, pending_test_json TEXT, last_action TEXT, updated_at INTEGER)");
        db.execSQL("INSERT INTO story_state(id,started,last_narration,visual_prompt,options_json,pending_test_json,last_action,updated_at) VALUES(1,0,'','','[]','','',0)");
        createReliabilityAndImmersionTables(db);
        createWorldProfileTables(db);
        createVirtualMasterTables(db);
        createCognitiveMasterTables(db);
        createTravelRulesTables(db);
        ensureLivingWorldTables(db);
        createLibrarySyncTables(db);
        ensureMasterIntegrationColumns(db);
        createGuildTables(db);
        ensureFiveZeroSchema(db);
        ensureSixZeroSchema(db);
        KingmakerRepository.ensureSchema(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE manuals ADD COLUMN indexed INTEGER DEFAULT 0"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE characters ADD COLUMN token_uri TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 4) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS map_state (id INTEGER PRIMARY KEY, image_uri TEXT, grid_type TEXT, grid_size REAL, fog_enabled INTEGER, token_x REAL, token_y REAL, scale REAL, offset_x REAL, offset_y REAL)"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("INSERT OR IGNORE INTO map_state(id,image_uri,grid_type,grid_size,fog_enabled,token_x,token_y,scale,offset_x,offset_y) VALUES(1,'','Quadrada',64,0,0.5,0.5,1,0,0)"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE manuals ADD COLUMN gemini_uri TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE manuals ADD COLUMN gemini_name TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE manuals ADD COLUMN gemini_expires INTEGER DEFAULT 0"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE manuals ADD COLUMN ai_selected INTEGER DEFAULT 0"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 6) {
            try { db.execSQL("ALTER TABLE characters ADD COLUMN background TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE characters ADD COLUMN visual_description TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE characters ADD COLUMN proficiencies TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE characters ADD COLUMN equipment TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE characters ADD COLUMN spells TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE characters ADD COLUMN rules_source TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS story_state (id INTEGER PRIMARY KEY, started INTEGER DEFAULT 0, last_narration TEXT, visual_prompt TEXT, options_json TEXT, pending_test_json TEXT, last_action TEXT, updated_at INTEGER)"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("INSERT OR IGNORE INTO story_state(id,started,last_narration,visual_prompt,options_json,pending_test_json,last_action,updated_at) VALUES(1,0,'','','[]','','',0)"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 7) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS tokens (id INTEGER PRIMARY KEY AUTOINCREMENT, drive_id TEXT UNIQUE, name TEXT, label TEXT, species TEXT, creature_type TEXT, role TEXT, category TEXT, tags TEXT, mime_type TEXT, size_bytes INTEGER, modified_time TEXT, thumbnail_link TEXT, local_uri TEXT, source TEXT, source_root TEXT DEFAULT '', last_seen INTEGER, enabled INTEGER DEFAULT 1)"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 8) {
            try { createTokenPdfTables(db); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 9) {
            try { db.execSQL("ALTER TABLE token_pdfs ADD COLUMN grid_left REAL DEFAULT 0.065"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE token_pdfs ADD COLUMN grid_top REAL DEFAULT 0.055"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE token_pdfs ADD COLUMN grid_right REAL DEFAULT 0.935"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE token_pdfs ADD COLUMN grid_bottom REAL DEFAULT 0.945"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 10) {
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN overlay_uri TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN manifest_json TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN map_package_id TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN current_hex_id TEXT"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN overlay_visible INTEGER DEFAULT 1"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN reveals_json TEXT DEFAULT '{}'"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { createMapAtlasTables(db); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 11) {
            try { createMapCorrectionTables(db); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 12) {
            try { db.execSQL("ALTER TABLE characters ADD COLUMN sheet_json TEXT DEFAULT '{}'"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 13) {
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN position_locked INTEGER DEFAULT 0"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN destination_hex_id TEXT DEFAULT ''"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN route_json TEXT DEFAULT '[]'"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("ALTER TABLE map_state ADD COLUMN route_owner TEXT DEFAULT 'Jogador'"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("UPDATE map_state SET position_locked=1 WHERE current_hex_id IS NOT NULL AND current_hex_id<>''"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 27) {
            try { createMapNoteTables(db); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 28) {
            try { createMapNoteTables(db); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 20) {
            createReliabilityAndImmersionTables(db);
            addColumnIfMissing(db,"combatants","temp_hp","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"combatants","death_success","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"combatants","death_fail","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"combatants","concentration","TEXT DEFAULT ''");
            addColumnIfMissing(db,"combatants","reaction_used","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"combatants","condition_timers_json","TEXT DEFAULT '[]'");
            addColumnIfMissing(db,"combatants","resistances","TEXT DEFAULT ''");
            addColumnIfMissing(db,"combatants","vulnerabilities","TEXT DEFAULT ''");
            addColumnIfMissing(db,"combatants","immunities","TEXT DEFAULT ''");
            addColumnIfMissing(db,"combatants","resources_json","TEXT DEFAULT '{}'");
        }
        if (oldVersion < 21) {
            addColumnIfMissing(db,"manuals","rag_document","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manuals","rag_status","TEXT DEFAULT 'not_indexed'");
            addColumnIfMissing(db,"manuals","rag_indexed_at","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"manuals","rag_error","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manuals","rag_modified_time","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manuals","rag_store_name","TEXT DEFAULT ''");
        }
        if (oldVersion < 22) {
            addColumnIfMissing(db,"manuals","rag_part_count","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"manuals","rag_parts_ready","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"manuals","rag_page_count","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"manuals","rag_source_hash","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manuals","rag_progress","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manuals","rag_split_mode","TEXT DEFAULT ''");
            createManualRagPartTable(db);
        }
        if (oldVersion < 23) {
            createMapAtlasTables(db);
            addColumnIfMissing(db,"map_packages","world_profile","TEXT DEFAULT 'pf2'");
            addColumnIfMissing(db,"map_packages","regions_overlay_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","political_overlay_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","nation_count","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"map_packages","vector_overlay_uri","TEXT DEFAULT ''");
            addColumnIfMissing(db,"map_packages","vector_overlay_gzip_uri","TEXT DEFAULT ''");
            addColumnIfMissing(db,"map_packages","hex_index_uri","TEXT DEFAULT ''");
            addColumnIfMissing(db,"map_packages","overlay_mode","TEXT DEFAULT 'dynamic'");
            addColumnIfMissing(db,"map_packages","distance_miles","REAL DEFAULT 0");
            addColumnIfMissing(db,"map_packages","distance_km","REAL DEFAULT 0");
            addColumnIfMissing(db,"map_packages","min_label_zoom","REAL DEFAULT 1.6");
            addColumnIfMissing(db,"map_hex_atlas","terrain_method","TEXT DEFAULT ''");
            try { db.execSQL("UPDATE map_packages SET world_profile='pf2' WHERE world_profile IS NULL OR world_profile<> 'pf2'"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 24) {
            addColumnIfMissing(db,"characters","world_profile","TEXT DEFAULT 'pf2'");
            addColumnIfMissing(db,"journal","world_profile","TEXT DEFAULT 'pf2'");
            addColumnIfMissing(db,"combatants","world_profile","TEXT DEFAULT 'pf2'");
            try { db.execSQL("UPDATE characters SET world_profile='pf2' WHERE world_profile IS NULL OR world_profile=''"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("UPDATE journal SET world_profile='pf2' WHERE world_profile IS NULL OR world_profile=''"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            try { db.execSQL("UPDATE combatants SET world_profile='pf2' WHERE world_profile IS NULL OR world_profile=''"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
            createWorldProfileTables(db);
        }
        if (oldVersion < 25) {
            addColumnIfMissing(db,"scene_state_profiles","day_number","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"scene_state_profiles","clock_hour","REAL DEFAULT 9");
            addColumnIfMissing(db,"scene_state","day_number","INTEGER DEFAULT 1");
            try { db.execSQL("UPDATE scene_state_profiles SET day_number=1 WHERE day_number IS NULL OR day_number<1"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 26) {
            createManualLocalIndexTables(db);
            try { db.execSQL("UPDATE manuals SET rag_status=CASE WHEN rag_status='disabled' THEN 'disabled' ELSE 'not_indexed' END, rag_store_name='local-v1', rag_document='', rag_error='', rag_progress='Índice local aguardando processamento.', rag_parts_ready=0, rag_part_count=0"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 29) {
            createWorldProfileTables(db);
            addColumnIfMissing(db,"npc_memory_profiles","token_uri","TEXT DEFAULT ''");
            addColumnIfMissing(db,"npc_memory_profiles","visual_description","TEXT DEFAULT ''");
            addColumnIfMissing(db,"npc_memory_profiles","stats_json","TEXT DEFAULT '{}'");
            addColumnIfMissing(db,"npc_memory_profiles","hex_id","TEXT DEFAULT ''");
            addColumnIfMissing(db,"npc_memory_profiles","active","INTEGER DEFAULT 1");
            addColumnIfMissing(db,"npc_memory_profiles","voice_profile","TEXT DEFAULT ''");
        }
        if (oldVersion < 30) {
            createVirtualMasterTables(db);
        }
        if (oldVersion < 31) {
            createVirtualMasterTables(db);
            addColumnIfMissing(db,"party_state_profiles","relations_json","TEXT DEFAULT '[]'");
            addColumnIfMissing(db,"party_state_profiles","session_preferences_json","TEXT DEFAULT '{}'");
        }
        if (oldVersion < 32) {
            createVirtualMasterTables(db);
            addColumnIfMissing(db,"character_control_profiles","personal_goal","TEXT DEFAULT ''");
            addColumnIfMissing(db,"character_control_profiles","arc_status","TEXT DEFAULT 'active'");
            addColumnIfMissing(db,"character_control_profiles","arc_notes","TEXT DEFAULT ''");
            addColumnIfMissing(db,"character_control_profiles","last_major_choice","TEXT DEFAULT ''");
        }
        if (oldVersion < 33) {
            createCognitiveMasterTables(db);
        }
        if (oldVersion < 34) {
            createCognitiveMasterTables(db);
        }
        if (oldVersion < 35) {
            createTravelRulesTables(db);
        }
        if (oldVersion < 36) {
            createWorldProfileTables(db);
            addColumnIfMissing(db,"scene_state_profiles","clock_hour","REAL DEFAULT 9");
            try { db.execSQL("UPDATE scene_state_profiles SET clock_hour=CASE lower(COALESCE(time_of_day,'')) WHEN 'madrugada' THEN 3 WHEN 'amanhecer' THEN 6 WHEN 'manhã' THEN 9 WHEN 'manha' THEN 9 WHEN 'meio-dia' THEN 12 WHEN 'meio dia' THEN 12 WHEN 'tarde' THEN 15 WHEN 'entardecer' THEN 18 WHEN 'noite' THEN 21 ELSE 9 END"); } catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        }
        if (oldVersion < 37) {
            addColumnIfMissing(db,"manuals","source_root","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manuals","last_seen","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"manuals","availability","TEXT DEFAULT 'available'");
            addColumnIfMissing(db,"manuals","last_verified_at","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"tokens","source_root","TEXT DEFAULT ''");
            addColumnIfMissing(db,"token_pdfs","source_root","TEXT DEFAULT ''");
            createLibrarySyncTables(db);
        }
        if (oldVersion < 38) {
            addColumnIfMissing(db,"audit_log","world_profile","TEXT NOT NULL DEFAULT 'pf2'");
            addColumnIfMissing(db,"dice_history","world_profile","TEXT NOT NULL DEFAULT 'pf2'");
            try { db.execSQL("UPDATE audit_log SET world_profile='pf2' WHERE world_profile IS NULL OR world_profile=''"); } catch (Exception e) { AppLog.w("AppDb","Não foi possível perfilar o histórico legado.",e); }
            addColumnIfMissing(db,"map_world_state","logical_map_id","TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(db,"map_world_state","regions_overlay_visible","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"map_world_state","political_overlay_visible","INTEGER DEFAULT 0");
            try { db.execSQL("UPDATE map_world_state SET logical_map_id=COALESCE((SELECT map_id FROM map_packages WHERE package_id=map_world_state.map_package_id),'') WHERE COALESCE(logical_map_id,'')='' AND COALESCE(map_package_id,'')<>''"); } catch (Exception e) { AppLog.w("AppDb","Não foi possível migrar o identificador lógico do mapa.",e); }
            createMapAtlasTables(db);
            createReliabilityAndImmersionTables(db);
        }
        if (oldVersion < 39) {
            ensureLivingWorldTables(db);
        }
        if (oldVersion < 40) {
            ensureLivingWorldTables(db);
            ensureMasterIntegrationColumns(db);
        }
        if (oldVersion < 41) {
            ensureMasterIntegrationColumns(db);
        }
        if (oldVersion < 42) {
            ensureMasterIntegrationColumns(db);
            addColumnIfMissing(db,"dice_history","actor_character_id","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"dice_history","actor_name","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","test_key","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","action_signature","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","source_rule_id","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","scene_fingerprint","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","outcome","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","consumed_at","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"living_troupes","travel_progress_json","TEXT DEFAULT '{}'");
        }
        if (oldVersion < 43) {
            addColumnIfMissing(db,"dice_history","procedure_id","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","campaign_fingerprint","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","target_signature","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","consequence_key","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","modifier","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"dice_history","modifier_source","TEXT DEFAULT ''");
            addColumnIfMissing(db,"dice_history","roll_mode","TEXT DEFAULT 'normal'");
            addColumnIfMissing(db,"dice_history","natural_roll","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"dice_history","critical","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"dice_history","critical_failure","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"dice_history","cancelled_at","INTEGER DEFAULT 0");
        }
        if (oldVersion < 44) {
            createManualLocalIndexTables(db);
            addColumnIfMissing(db,"manual_local_pages","section_title","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manual_local_pages","chapter_title","TEXT DEFAULT ''");
            addColumnIfMissing(db,"manual_local_pages","content_kind","TEXT DEFAULT 'prose'");
            addColumnIfMissing(db,"manual_local_pages","structure_json","TEXT DEFAULT '{}'");
            addColumnIfMissing(db,"manual_local_pages","cross_refs_json","TEXT DEFAULT '[]'");
            addColumnIfMissing(db,"manual_local_pages","page_hash","TEXT DEFAULT ''");
            try { db.execSQL("UPDATE manuals SET rag_status='not_indexed',rag_store_name='',rag_document='',rag_progress='Índice estrutural v4 requer reindexação' WHERE rag_store_name LIKE 'local-%' AND rag_store_name<>?",new Object[]{LOCAL_MANUAL_STORE}); }
            catch(Exception e){AppLog.w("AppDb","Não foi possível marcar todos os índices locais antigos para reindexação.",e);}
        }
        if (oldVersion < 45) ensureMasterIntegrationColumns(db);
        if (oldVersion < 46) {
            ensureMasterIntegrationColumns(db);
            repairLegacyCharacterIdentities(db);
            backfillLegacyTransactionIdentities(db,null);
            try{db.execSQL("UPDATE manuals SET rag_status='not_indexed',rag_progress='Reavaliar duplicata com números preservados' WHERE rag_status='duplicate_skipped'");}
            catch(Exception e){AppLog.w("AppDb","Não foi possível reabrir todos os manuais descartados pela identidade antiga.",e);}
        }
        if (oldVersion < 47) {
            ensureMasterIntegrationColumns(db);
            backfillLegacyWorldEventIdentities(db,null);
        }
        if (oldVersion < 48) createGuildTables(db);
        if (oldVersion < 49) { addColumnIfMissing(db,"guild_members","dues_due_day","INTEGER DEFAULT 0"); addColumnIfMissing(db,"guild_members","last_dues_notice_day","INTEGER DEFAULT 0"); }
        if (oldVersion < 50) {
            addColumnIfMissing(db,"combatants","source_kind","TEXT DEFAULT ''");
            addColumnIfMissing(db,"combatants","source_id","INTEGER DEFAULT 0");
            addColumnIfMissing(db,"combatants","token_uri","TEXT DEFAULT ''");
            try { db.execSQL("UPDATE combatants SET source_kind='character',source_id=COALESCE((SELECT ch.id FROM characters ch WHERE ch.world_profile=combatants.world_profile AND lower(ch.name)=lower(combatants.name) ORDER BY ch.active DESC LIMIT 1),0),token_uri=COALESCE((SELECT ch.token_uri FROM characters ch WHERE ch.world_profile=combatants.world_profile AND lower(ch.name)=lower(combatants.name) ORDER BY ch.active DESC LIMIT 1),'') WHERE is_player=1"); } catch(Exception e){ AppLog.w("AppDb","Não foi possível vincular os retratos legados dos heróis ao combate.",e); }
            try { db.execSQL("UPDATE combatants SET source_kind='npc',source_id=COALESCE((SELECT n.id FROM npc_memory_profiles n WHERE n.world_profile=combatants.world_profile AND lower(n.name)=lower(combatants.name) LIMIT 1),0),token_uri=COALESCE((SELECT n.token_uri FROM npc_memory_profiles n WHERE n.world_profile=combatants.world_profile AND lower(n.name)=lower(combatants.name) LIMIT 1),'') WHERE is_player=0"); } catch(Exception e){ AppLog.w("AppDb","Não foi possível vincular os retratos legados dos NPCs ao combate.",e); }
        }
        if (oldVersion < 51) {
            // tactical_tokens armazena somente posição. Retratos e ficha pertencem a combatants.
            // Backups antigos exportavam por engano o resultado enriquecido do JOIN; a restauração
            // v51 ignora essas colunas e mantém o esquema posicional estável.
            createVirtualMasterTables(db);
        }
        if (oldVersion < 52) {
            // A extração dos PDFs já estava concluída. Esta migração somente cria
            // o índice invertido local uma vez; não reabre, retransmite nem
            // reprocessa os manuais.
            createManualLocalFtsIndex(db,false);
            db.execSQL("UPDATE manual_search_index_state SET status='pending',last_error='',updated_at=0 WHERE id=1");
        }
        if (oldVersion < 53) ensureFiveZeroSchema(db);
        if (oldVersion < 54) ensureSixZeroSchema(db);
        if (oldVersion < 55) {
            createMapAtlasTables(db);
            createCognitiveMasterTables(db);
            createVirtualMasterTables(db);
            createTravelRulesTables(db);
        }
        if (oldVersion < 56) {
            createMapAtlasTables(db);
            // Consolidação Pathfinder-only: todo estado estrutural ativo usa pf2.
            try { db.execSQL("UPDATE master_runtime_state SET world_profile='pf2' WHERE world_profile<>'pf2' AND NOT EXISTS(SELECT 1 FROM master_runtime_state p WHERE p.world_profile='pf2')"); } catch(Exception ignored){AppLog.ignored("AppDb","Migração cognitiva PF2 já satisfeita.",ignored);}
            try { db.execSQL("DELETE FROM master_runtime_state WHERE world_profile<>'pf2'"); } catch(Exception ignored){AppLog.ignored("AppDb","Limpeza de perfis cognitivos legados já satisfeita.",ignored);}
            try { db.execSQL("INSERT OR IGNORE INTO master_runtime_state(world_profile) VALUES('pf2')"); } catch(Exception ignored){AppLog.ignored("AppDb","Perfil cognitivo PF2 já existe.",ignored);}
        }
        if (oldVersion < 57) {
            // Atlas V5 MAX: novas colunas vivem no mesmo AppDb canônico.
            createMapAtlasTables(db);
        }
        if (oldVersion < 58) {
            // Camada sandbox: POIs de campanha/procedurais, sem tocar no Atlas canônico.
            ensureCampaignPoiSchema(db);
        }
        if (oldVersion < 59) {
            // Kingmaker usa o mesmo AppDb e mantém o Atlas oficial somente leitura.
            KingmakerRepository.ensureSchema(db);
        }
    }

    /** Esquema de consolidação 5.0. Todas as operações são idempotentes. */
    private static void ensureFiveZeroSchema(SQLiteDatabase db) {
        addColumnIfMissing(db,"journal","canonical_fact","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"journal","event_key","TEXT DEFAULT ''");
        addColumnIfMissing(db,"journal","source_kind","TEXT DEFAULT 'system'");
        try{db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_journal_event_key ON journal(world_profile,event_key) WHERE event_key<>''");}catch(Exception e){AppLog.w("AppDb","Não foi possível criar índice canônico do Diário.",e);}
        addColumnIfMissing(db,"npc_memory_profiles","stable_id","TEXT DEFAULT ''");
        try{db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_npc_stable_id ON npc_memory_profiles(world_profile,stable_id) WHERE stable_id<>''");}catch(Exception e){AppLog.w("AppDb","Não foi possível criar índice de identidade de NPC.",e);}
        addColumnIfMissing(db,"combat_state_profiles","state","TEXT DEFAULT 'idle'");
        addColumnIfMissing(db,"combat_state_profiles","pending_roll_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"combat_state_profiles","encounter_id","TEXT DEFAULT ''");
        addColumnIfMissing(db,"combat_state_profiles","updated_at","INTEGER DEFAULT 0");
        db.execSQL("CREATE TABLE IF NOT EXISTS pending_action_queue (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, source_kind TEXT NOT NULL, source_id TEXT DEFAULT '', action_kind TEXT DEFAULT '', title TEXT NOT NULL, detail TEXT DEFAULT '', status TEXT DEFAULT 'pending', due_day INTEGER DEFAULT 0, priority INTEGER DEFAULT 0, payload_json TEXT DEFAULT '{}', rule_query TEXT DEFAULT '', effect_key TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,effect_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_action_queue ON pending_action_queue(world_profile,status,priority DESC,due_day,created_at)");
    }


    /** Esquema 6.0 — O Mundo que se Lembra. Idempotente e isolado por perfil. */
    public static void ensureSixZeroSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS world_seeds (world_profile TEXT PRIMARY KEY, seed_id TEXT NOT NULL, lineage_name TEXT DEFAULT '', origin_campaign TEXT DEFAULT '', generation INTEGER DEFAULT 1, era_label TEXT DEFAULT 'Era atual', metadata_json TEXT DEFAULT '{}', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS emergent_arcs (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, arc_key TEXT NOT NULL, title TEXT NOT NULL, premise TEXT DEFAULT '', dramatic_question TEXT DEFAULT '', stage TEXT DEFAULT 'germinating', momentum INTEGER DEFAULT 0, urgency INTEGER DEFAULT 0, actors_json TEXT DEFAULT '[]', evidence_json TEXT DEFAULT '[]', possible_consequences_json TEXT DEFAULT '[]', origin_json TEXT DEFAULT '{}', status TEXT DEFAULT 'active', created_day INTEGER DEFAULT 1, updated_day INTEGER DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,arc_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_emergent_arcs ON emergent_arcs(world_profile,status,urgency DESC,momentum DESC,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS settlement_states (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, settlement_key TEXT NOT NULL, name TEXT NOT NULL, kind TEXT DEFAULT 'local', region TEXT DEFAULT '', hex_id TEXT DEFAULT '', population_band TEXT DEFAULT 'desconhecida', prosperity INTEGER DEFAULT 50, security INTEGER DEFAULT 50, scarcity INTEGER DEFAULT 20, unrest INTEGER DEFAULT 10, criminality INTEGER DEFAULT 15, political_pressure INTEGER DEFAULT 15, trade INTEGER DEFAULT 50, faith_influence INTEGER DEFAULT 30, faction_pressure_json TEXT DEFAULT '{}', resources_json TEXT DEFAULT '{}', tags_json TEXT DEFAULT '[]', notes TEXT DEFAULT '', last_simulated_day INTEGER DEFAULT 1, updated_at INTEGER NOT NULL, UNIQUE(world_profile,settlement_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_settlement_states ON settlement_states(world_profile,region,name)");
        db.execSQL("CREATE TABLE IF NOT EXISTS world_history_events (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, event_key TEXT NOT NULL, day_number INTEGER DEFAULT 1, era_label TEXT DEFAULT 'Era atual', scope TEXT DEFAULT 'local', location TEXT DEFAULT '', title TEXT NOT NULL, summary TEXT NOT NULL, actors_json TEXT DEFAULT '[]', causes_json TEXT DEFAULT '[]', consequences_json TEXT DEFAULT '[]', tags_json TEXT DEFAULT '[]', significance INTEGER DEFAULT 25, source_kind TEXT DEFAULT 'journal', source_id TEXT DEFAULT '', canonical INTEGER DEFAULT 1, created_at INTEGER NOT NULL, UNIQUE(world_profile,event_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_world_history ON world_history_events(world_profile,significance DESC,day_number DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS rumors (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, rumor_key TEXT NOT NULL, source_event_key TEXT DEFAULT '', origin_location TEXT DEFAULT '', current_region TEXT DEFAULT '', statement TEXT NOT NULL, truth_level TEXT DEFAULT 'parcial', distortion INTEGER DEFAULT 15, confidence INTEGER DEFAULT 50, secrecy INTEGER DEFAULT 20, status TEXT DEFAULT 'circulating', born_day INTEGER DEFAULT 1, expires_day INTEGER DEFAULT 0, carriers_json TEXT DEFAULT '[]', mutations_json TEXT DEFAULT '[]', updated_at INTEGER NOT NULL, UNIQUE(world_profile,rumor_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_rumors ON rumors(world_profile,status,current_region,born_day DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS rumor_knowledge (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, rumor_id INTEGER NOT NULL, knower_kind TEXT NOT NULL, knower_id TEXT NOT NULL, knowledge_state TEXT DEFAULT 'heard', believed_confidence INTEGER DEFAULT 50, learned_day INTEGER DEFAULT 1, source_name TEXT DEFAULT '', notes TEXT DEFAULT '', UNIQUE(world_profile,rumor_id,knower_kind,knower_id), FOREIGN KEY(rumor_id) REFERENCES rumors(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS campaign_legacies (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, world_seed TEXT NOT NULL, legacy_key TEXT NOT NULL, source_campaign TEXT DEFAULT '', generation INTEGER DEFAULT 1, category TEXT DEFAULT 'memory', subject TEXT NOT NULL, statement TEXT NOT NULL, location TEXT DEFAULT '', inheritable INTEGER DEFAULT 1, active INTEGER DEFAULT 1, evidence_json TEXT DEFAULT '[]', created_day INTEGER DEFAULT 1, created_at INTEGER NOT NULL, UNIQUE(world_profile,world_seed,legacy_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_legacies ON campaign_legacies(world_profile,world_seed,active,category)");
        db.execSQL("CREATE TABLE IF NOT EXISTS npc_voice_profiles (world_profile TEXT NOT NULL, npc_stable_id TEXT NOT NULL, npc_name TEXT NOT NULL, voice_name TEXT DEFAULT 'Charon', register_name TEXT DEFAULT 'natural', speaking_style TEXT DEFAULT '', tempo REAL DEFAULT 1.0, warmth INTEGER DEFAULT 50, formality INTEGER DEFAULT 50, signature_phrases_json TEXT DEFAULT '[]', updated_at INTEGER NOT NULL, PRIMARY KEY(world_profile,npc_stable_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS tactical_scenes (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, scene_key TEXT NOT NULL, location TEXT DEFAULT '', hex_id TEXT DEFAULT '', environment TEXT DEFAULT 'generic', width_cells INTEGER DEFAULT 12, height_cells INTEGER DEFAULT 8, seed INTEGER DEFAULT 0, zones_json TEXT DEFAULT '[]', obstacles_json TEXT DEFAULT '[]', features_json TEXT DEFAULT '[]', cover_json TEXT DEFAULT '[]', hazards_json TEXT DEFAULT '[]', entry_points_json TEXT DEFAULT '[]', source_context_json TEXT DEFAULT '{}', status TEXT DEFAULT 'ready', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,scene_key))");
        db.execSQL("CREATE TABLE IF NOT EXISTS local_table_sessions (world_profile TEXT PRIMARY KEY, session_code TEXT NOT NULL, port INTEGER DEFAULT 42731, enabled INTEGER DEFAULT 0, bind_address TEXT DEFAULT '', started_at INTEGER DEFAULT 0, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS local_table_players (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, session_code TEXT NOT NULL, character_id INTEGER DEFAULT 0, character_name TEXT DEFAULT '', player_name TEXT DEFAULT '', join_token TEXT NOT NULL, permissions_json TEXT DEFAULT '{}', last_seen INTEGER DEFAULT 0, active INTEGER DEFAULT 1, UNIQUE(world_profile,join_token))");
        db.execSQL("CREATE TABLE IF NOT EXISTS local_table_secrets (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, character_id INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, event_key TEXT DEFAULT '', active INTEGER DEFAULT 1, created_at INTEGER NOT NULL)");
        long now=System.currentTimeMillis();
        db.execSQL("INSERT OR IGNORE INTO world_seeds(world_profile,seed_id,lineage_name,origin_campaign,generation,era_label,metadata_json,created_at,updated_at) VALUES('pf2','pf2-inner-sea-world','Mar Interior persistente','',1,'Era atual','{}',?,?)",new Object[]{now,now});
    }

    public static void createGuildTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS guilds (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, faction_id INTEGER DEFAULT 0, name TEXT NOT NULL COLLATE NOCASE, type TEXT DEFAULT 'Outra', description TEXT DEFAULT '', charter TEXT DEFAULT '', headquarters TEXT DEFAULT '', treasury REAL DEFAULT 0, currency TEXT DEFAULT 'po', party_reputation INTEGER DEFAULT 0, influence INTEGER DEFAULT 25, current_plan TEXT DEFAULT '', dues_policy_json TEXT DEFAULT '[]', admission_json TEXT DEFAULT '[]', benefits_json TEXT DEFAULT '[]', services_json TEXT DEFAULT '[]', rules_json TEXT DEFAULT '[]', stock_json TEXT DEFAULT '{}', active INTEGER DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,name))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guilds_world_active ON guilds(world_profile,active,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_ranks (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, name TEXT NOT NULL, rank_order INTEGER DEFAULT 0, requirements_json TEXT DEFAULT '{}', benefits_json TEXT DEFAULT '[]', permissions_json TEXT DEFAULT '[]', dues_amount REAL DEFAULT 0, active INTEGER DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE, UNIQUE(guild_id,name))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_ranks ON guild_ranks(world_profile,guild_id,rank_order)");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_members (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, member_kind TEXT NOT NULL DEFAULT 'npc', character_id INTEGER DEFAULT 0, npc_name TEXT DEFAULT '', rank_id INTEGER DEFAULT 0, status TEXT DEFAULT 'active', reputation INTEGER DEFAULT 0, contribution REAL DEFAULT 0, joined_day INTEGER DEFAULT 1, dues_paid_day INTEGER DEFAULT 0, dues_due_day INTEGER DEFAULT 0, last_dues_notice_day INTEGER DEFAULT 0, notes TEXT DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_members ON guild_members(world_profile,guild_id,status,updated_at DESC)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_guild_member_hero ON guild_members(world_profile,guild_id,character_id) WHERE member_kind='hero' AND character_id>0");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_guild_member_npc ON guild_members(world_profile,guild_id,npc_name COLLATE NOCASE) WHERE member_kind='npc' AND TRIM(npc_name)<>''");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_contracts (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT DEFAULT '', issuer TEXT DEFAULT '', status TEXT DEFAULT 'open', assignee_kind TEXT DEFAULT '', assignee_id INTEGER DEFAULT 0, assignee_name TEXT DEFAULT '', requirements_json TEXT DEFAULT '{}', reward_json TEXT DEFAULT '{}', penalty_json TEXT DEFAULT '{}', progress_json TEXT DEFAULT '{}', deadline_day INTEGER DEFAULT 0, linked_quest_id INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_contracts ON guild_contracts(world_profile,guild_id,status,deadline_day)");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_projects (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT DEFAULT '', kind TEXT DEFAULT 'general', status TEXT DEFAULT 'active', progress REAL DEFAULT 0, goal REAL DEFAULT 100, progress_per_day REAL DEFAULT 5, cost_json TEXT DEFAULT '{}', result_json TEXT DEFAULT '{}', location TEXT DEFAULT '', started_day INTEGER DEFAULT 1, completed_day INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_projects ON guild_projects(world_profile,guild_id,status,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_sites (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, name TEXT NOT NULL, site_type TEXT DEFAULT 'sede', location TEXT DEFAULT '', hex_id TEXT DEFAULT '', services_json TEXT DEFAULT '[]', defenses_json TEXT DEFAULT '{}', stock_json TEXT DEFAULT '{}', active INTEGER DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_sites ON guild_sites(world_profile,guild_id,active)");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_ledger (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, entry_type TEXT DEFAULT 'adjustment', amount REAL DEFAULT 0, currency TEXT DEFAULT 'po', description TEXT DEFAULT '', actor TEXT DEFAULT '', day_number INTEGER DEFAULT 1, created_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_ledger ON guild_ledger(world_profile,guild_id,created_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS guild_relations (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, guild_id INTEGER NOT NULL, target_kind TEXT DEFAULT 'guild', target_guild_id INTEGER DEFAULT 0, target_faction_id INTEGER DEFAULT 0, target_name TEXT DEFAULT '', relation INTEGER DEFAULT 0, stance TEXT DEFAULT 'neutral', treaties_json TEXT DEFAULT '[]', notes TEXT DEFAULT '', updated_at INTEGER NOT NULL, FOREIGN KEY(guild_id) REFERENCES guilds(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_guild_relations ON guild_relations(world_profile,guild_id,target_kind,updated_at DESC)");
    }

    public void ensureGuildTables(){createGuildTables(getWritableDatabase());}

    public static void ensureLivingWorldTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS living_world_state (world_profile TEXT PRIMARY KEY, enabled INTEGER DEFAULT 1, simulation_mode TEXT DEFAULT 'equilibrado', calendar_name TEXT DEFAULT 'Calendário da campanha', epoch_year INTEGER DEFAULT 1, days_per_year INTEGER DEFAULT 365, months_json TEXT DEFAULT '[]', world_notes TEXT DEFAULT '', last_simulated_day INTEGER DEFAULT 1, updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO living_world_state(world_profile) VALUES('pf2')");
        db.execSQL("INSERT OR IGNORE INTO living_world_state(world_profile) VALUES('pf2')");
        db.execSQL("CREATE TABLE IF NOT EXISTS hero_life_profiles (world_profile TEXT NOT NULL, character_id INTEGER NOT NULL, birth_day INTEGER DEFAULT 1, initial_age_years INTEGER DEFAULT 18, life_stage TEXT DEFAULT 'não definido', residence TEXT DEFAULT '', occupation TEXT DEFAULT 'Aventureiro', family_name TEXT DEFAULT '', relationship_status TEXT DEFAULT '', household_json TEXT DEFAULT '[]', goals_json TEXT DEFAULT '[]', status TEXT DEFAULT 'active', updated_at INTEGER DEFAULT 0, PRIMARY KEY(world_profile,character_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS life_relationships (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, character_id INTEGER NOT NULL, related_character_id INTEGER DEFAULT 0, related_name TEXT NOT NULL, relation_type TEXT DEFAULT 'vínculo', status TEXT DEFAULT 'ativo', established_day INTEGER DEFAULT 1, related_birth_day INTEGER DEFAULT 0, related_initial_age_years INTEGER DEFAULT 0, age_known INTEGER DEFAULT 0, notes TEXT DEFAULT '', mechanical_effects_json TEXT DEFAULT '{}', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_life_relationships_world_hero ON life_relationships(world_profile,character_id,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS life_activities (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, character_id INTEGER NOT NULL, activity_type TEXT DEFAULT 'livre', title TEXT NOT NULL, location TEXT DEFAULT '', start_day INTEGER DEFAULT 1, duration_days INTEGER DEFAULT 1, status TEXT DEFAULT 'planejada', rule_authorized INTEGER DEFAULT 0, rule_source TEXT DEFAULT '', rule_page INTEGER DEFAULT 0, rule_excerpt TEXT DEFAULT '', check_json TEXT DEFAULT '{}', cost_json TEXT DEFAULT '{}', reward_json TEXT DEFAULT '{}', narrative_result TEXT DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_life_activities_world_status ON life_activities(world_profile,status,start_day)");
        db.execSQL("CREATE TABLE IF NOT EXISTS living_troupes (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, name TEXT NOT NULL, troupe_mode TEXT DEFAULT 'itinerante', current_location TEXT DEFAULT '', home_base TEXT DEFAULT '', members_json TEXT DEFAULT '[]', itinerary_json TEXT DEFAULT '[]', resources_json TEXT DEFAULT '{}', travel_progress_json TEXT DEFAULT '{}', notes TEXT DEFAULT '', active INTEGER DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_living_troupes_world ON living_troupes(world_profile,active,updated_at DESC)");
        ensureMasterIntegrationColumns(db);
    }

    /** Colunas do barramento 4.4: simulacao temporal, consequencias idempotentes e auditoria. */
    private static void ensureMasterIntegrationColumns(SQLiteDatabase db) {
        addColumnIfMissing(db,"living_world_state","last_background_sync_at","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"living_world_state","background_simulation_enabled","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"living_world_state","background_game_hours_per_real_day","REAL DEFAULT 24");
        addColumnIfMissing(db,"living_world_state","background_max_hours","INTEGER DEFAULT 72");
        addColumnIfMissing(db,"life_activities","resolution_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"life_activities","applied_at","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"life_activities","procedure_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"life_activities","progress_units","REAL DEFAULT 0");
        addColumnIfMissing(db,"life_activities","total_units","REAL DEFAULT 1");
        addColumnIfMissing(db,"life_activities","check_frequency_days","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"life_activities","next_check_day","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"life_activities","complications_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"living_troupes","itinerary_index","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"living_troupes","last_travel_day","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"living_troupes","travel_progress_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"factions","plan_progress","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"factions","plan_interval_hours","REAL DEFAULT 24");
        addColumnIfMissing(db,"factions","plan_progress_hours","REAL DEFAULT 0");
        addColumnIfMissing(db,"factions","last_action_day","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"factions","plan_steps_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","plan_cycle","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"factions","last_action_summary","TEXT DEFAULT ''");
        addColumnIfMissing(db,"factions","objectives_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"factions","resources_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"factions","territories_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","relations_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","allies_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","enemies_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","information_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","costs_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"factions","risks_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","success_conditions_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","failure_conditions_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"factions","memory_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"world_clocks","interval_hours","REAL DEFAULT 6");
        addColumnIfMissing(db,"world_clocks","accumulated_hours","REAL DEFAULT 0");
        addColumnIfMissing(db,"world_clocks","trigger_condition","TEXT DEFAULT ''");
        addColumnIfMissing(db,"world_clocks","faction_id","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"world_clocks","last_advanced_day","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"world_clocks","paused","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"world_clocks","rate_multiplier","REAL DEFAULT 1");
        addColumnIfMissing(db,"world_clocks","tick_delta","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"world_clocks","regression_allowed","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"world_clocks","owner_entity","TEXT DEFAULT ''");
        addColumnIfMissing(db,"world_clocks","segments_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"world_clocks","completion_receipt","TEXT DEFAULT ''");
        addColumnIfMissing(db,"world_events","consequence_status","TEXT DEFAULT 'pending'");
        addColumnIfMissing(db,"world_events","applied_at","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"world_events","last_error","TEXT DEFAULT ''");
        addColumnIfMissing(db,"world_events","event_key","TEXT DEFAULT ''");
        addColumnIfMissing(db,"world_events","trigger_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"world_events","procedure_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"world_events","state_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"world_events","transaction_key","TEXT DEFAULT ''");
        addColumnIfMissing(db,"world_events","application_receipt","TEXT DEFAULT ''");
        addColumnIfMissing(db,"npc_cognitive_state","last_action_day","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"npc_cognitive_state","goals_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"npc_cognitive_state","personality_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"npc_cognitive_state","knowledge_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"npc_cognitive_state","resources_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"npc_cognitive_state","recent_events_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"story_state_profiles","option_history_json","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"pending_transactions","effect_key","TEXT DEFAULT ''");
        try{db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_effect_key ON pending_transactions(world_profile,effect_key,status)");}
        catch(Exception e){AppLog.w("AppDb","Não foi possível criar o índice de idempotência das pendências.",e);}
        try{db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_world_event_key ON world_events(world_profile,event_key) WHERE event_key<>''");}
        catch(Exception e){AppLog.w("AppDb","Não foi possível criar o índice de identidade dos eventos.",e);}
        db.execSQL("CREATE TABLE IF NOT EXISTS effect_receipts (world_profile TEXT NOT NULL, effect_key TEXT NOT NULL, source TEXT DEFAULT '', applied_at INTEGER NOT NULL, PRIMARY KEY(world_profile,effect_key))");
    }

    private static void createVirtualMasterTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS party_state_profiles (world_profile TEXT PRIMARY KEY, marching_order_json TEXT DEFAULT '[]', roles_json TEXT DEFAULT '{}', company_resources_json TEXT DEFAULT '{}', subgroups_json TEXT DEFAULT '[]', relations_json TEXT DEFAULT '[]', session_preferences_json TEXT DEFAULT '{}', knowledge_mode TEXT DEFAULT 'jogador_total', default_control_mode TEXT DEFAULT 'manual', updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO party_state_profiles(world_profile) VALUES('pf2')");
        db.execSQL("CREATE TABLE IF NOT EXISTS party_action_queue (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, character_id INTEGER NOT NULL, character_name TEXT NOT NULL, action_text TEXT NOT NULL, intent_type TEXT DEFAULT 'livre', priority INTEGER DEFAULT 0, status TEXT DEFAULT 'queued', created_at INTEGER NOT NULL, resolved_at INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_party_actions_world_status ON party_action_queue(world_profile,status,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS character_control_profiles (world_profile TEXT NOT NULL, character_id INTEGER NOT NULL, control_mode TEXT DEFAULT 'manual', tactics_json TEXT DEFAULT '{}', spotlight_score INTEGER DEFAULT 0, last_spotlight_at INTEGER DEFAULT 0, personal_goal TEXT DEFAULT '', arc_status TEXT DEFAULT 'active', arc_notes TEXT DEFAULT '', last_major_choice TEXT DEFAULT '', PRIMARY KEY(world_profile,character_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS knowledge_facts (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, fact_key TEXT DEFAULT '', subject TEXT DEFAULT '', content TEXT NOT NULL, truth_status TEXT DEFAULT 'verdade', visibility TEXT DEFAULT 'grupo', known_by_json TEXT DEFAULT '[]', reveal_condition TEXT DEFAULT '', source TEXT DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_knowledge_world ON knowledge_facts(world_profile,visibility,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS quests (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, title TEXT NOT NULL, status TEXT DEFAULT 'active', stage TEXT DEFAULT '', objective TEXT DEFAULT '', stakes TEXT DEFAULT '', owner_character_id INTEGER DEFAULT 0, faction_id INTEGER DEFAULT 0, progress_json TEXT DEFAULT '{}', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_quests_world_status ON quests(world_profile,status,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS factions (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, name TEXT NOT NULL COLLATE NOCASE, agenda TEXT DEFAULT '', resources TEXT DEFAULT '', relationship INTEGER DEFAULT 0, current_plan TEXT DEFAULT '', location TEXT DEFAULT '', active INTEGER DEFAULT 1, updated_at INTEGER NOT NULL, UNIQUE(world_profile,name))");
        db.execSQL("CREATE TABLE IF NOT EXISTS world_clocks (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, name TEXT NOT NULL, current_value INTEGER DEFAULT 0, max_value INTEGER DEFAULT 4, consequence TEXT DEFAULT '', automatic INTEGER DEFAULT 0, active INTEGER DEFAULT 1, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_world_clocks ON world_clocks(world_profile,active,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS world_events (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, title TEXT NOT NULL, description TEXT DEFAULT '', due_at INTEGER DEFAULT 0, due_day INTEGER DEFAULT 0, status TEXT DEFAULT 'scheduled', consequence_json TEXT DEFAULT '{}', created_at INTEGER NOT NULL, resolved_at INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_world_events_due ON world_events(world_profile,status,due_at,due_day)");
        db.execSQL("CREATE TABLE IF NOT EXISTS pending_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, title TEXT NOT NULL, summary TEXT DEFAULT '', payload_json TEXT NOT NULL, status TEXT DEFAULT 'pending', created_at INTEGER NOT NULL, applied_at INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_transactions ON pending_transactions(world_profile,status,created_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS session_memories (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, session_no INTEGER DEFAULT 1, title TEXT DEFAULT '', summary TEXT DEFAULT '', open_threads_json TEXT DEFAULT '[]', character_beats_json TEXT DEFAULT '{}', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS tactical_tokens (world_profile TEXT NOT NULL, combatant_id INTEGER NOT NULL, name TEXT NOT NULL, is_player INTEGER DEFAULT 0, x REAL DEFAULT 0.5, y REAL DEFAULT 0.5, zone TEXT DEFAULT '', updated_at INTEGER NOT NULL, PRIMARY KEY(world_profile,combatant_id))");
    }

    private static void createTravelRulesTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS travel_rules_state (world_profile TEXT PRIMARY KEY, state_json TEXT DEFAULT '{}', updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO travel_rules_state(world_profile) VALUES('pf2')");
    }

    private static void createCognitiveMasterTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS master_runtime_state (world_profile TEXT PRIMARY KEY, scene_mode TEXT DEFAULT 'exploracao', tension INTEGER DEFAULT 0, beat_no INTEGER DEFAULT 0, last_action_fingerprint TEXT DEFAULT '', last_context_fingerprint TEXT DEFAULT '', last_response_fingerprint TEXT DEFAULT '', last_cognitive_plan_json TEXT DEFAULT '{}', unresolved_threads_json TEXT DEFAULT '[]', last_summary TEXT DEFAULT '', validation_warnings_json TEXT DEFAULT '[]', updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO master_runtime_state(world_profile) VALUES('pf2')");
        db.execSQL("CREATE TABLE IF NOT EXISTS master_episodes (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, action_text TEXT DEFAULT '', summary TEXT DEFAULT '', actors_json TEXT DEFAULT '[]', scene_mode TEXT DEFAULT '', location TEXT DEFAULT '', hex_id TEXT DEFAULT '', tags_json TEXT DEFAULT '[]', significance INTEGER DEFAULT 1, canonical INTEGER DEFAULT 1, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_master_episodes_world ON master_episodes(world_profile,created_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS npc_cognitive_state (world_profile TEXT NOT NULL, npc_name TEXT NOT NULL COLLATE NOCASE, emotion TEXT DEFAULT 'neutro', trust INTEGER DEFAULT 0, fear INTEGER DEFAULT 0, suspicion INTEGER DEFAULT 0, urgency INTEGER DEFAULT 0, next_action TEXT DEFAULT '', schedule_json TEXT DEFAULT '[]', beliefs_json TEXT DEFAULT '[]', faction TEXT DEFAULT '', current_location TEXT DEFAULT '', last_interaction_summary TEXT DEFAULT '', last_action_at INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, PRIMARY KEY(world_profile,npc_name))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_npc_cognition_world ON npc_cognitive_state(world_profile,updated_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS master_response_cache (world_profile TEXT NOT NULL, cache_key TEXT NOT NULL, mode TEXT DEFAULT '', model TEXT DEFAULT '', action_hash TEXT DEFAULT '', state_hash TEXT DEFAULT '', response_json TEXT NOT NULL, created_at INTEGER NOT NULL, last_used_at INTEGER DEFAULT 0, use_count INTEGER DEFAULT 0, PRIMARY KEY(world_profile,cache_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_master_cache_age ON master_response_cache(world_profile,created_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS environmental_director_state (world_profile TEXT PRIMARY KEY, state_json TEXT DEFAULT '{}', updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO environmental_director_state(world_profile) VALUES('pf2')");
        db.execSQL("INSERT OR IGNORE INTO environmental_director_state(world_profile) VALUES('pf2')");
    }

    private static void createWorldProfileTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS campaign_profiles (world_profile TEXT PRIMARY KEY, name TEXT, rules_profile TEXT, tone TEXT, feats INTEGER, multiclass INTEGER, flanking INTEGER, notes TEXT, updated_at INTEGER)");
        db.execSQL("INSERT OR IGNORE INTO campaign_profiles(world_profile,name,rules_profile,tone,feats,multiclass,flanking,notes,updated_at) SELECT 'pf2',name,'Pathfinder 2e Remaster','Exploração e presságios',1,0,1,notes,updated_at FROM campaign WHERE id=1");
        db.execSQL("INSERT OR IGNORE INTO campaign_profiles(world_profile,name,rules_profile,tone,feats,multiclass,flanking,notes,updated_at) VALUES('pf2','Nova Campanha','Pathfinder 2e Remaster','Exploração e presságios',1,0,1,'',0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS story_state_profiles (world_profile TEXT PRIMARY KEY, started INTEGER DEFAULT 0, last_narration TEXT DEFAULT '', visual_prompt TEXT DEFAULT '', options_json TEXT DEFAULT '[]', pending_test_json TEXT DEFAULT '', last_action TEXT DEFAULT '', updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO story_state_profiles(world_profile,started,last_narration,visual_prompt,options_json,pending_test_json,last_action,updated_at) SELECT 'pf2',started,last_narration,visual_prompt,options_json,pending_test_json,last_action,updated_at FROM story_state WHERE id=1");
        db.execSQL("INSERT OR IGNORE INTO story_state_profiles(world_profile) VALUES('pf2')");
        db.execSQL("CREATE TABLE IF NOT EXISTS combat_state_profiles (world_profile TEXT PRIMARY KEY, round_no INTEGER DEFAULT 1, turn_index INTEGER DEFAULT 0, running INTEGER DEFAULT 0, party_initiative INTEGER DEFAULT 0, enemy_initiative INTEGER DEFAULT 0, phase INTEGER DEFAULT 0, state TEXT DEFAULT 'idle', pending_roll_json TEXT DEFAULT '{}', encounter_id TEXT DEFAULT '', updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO combat_state_profiles(world_profile,round_no,turn_index,running) SELECT 'pf2',round_no,turn_index,running FROM combat_state WHERE id=1");
        db.execSQL("INSERT OR IGNORE INTO combat_state_profiles(world_profile) VALUES('pf2')");
        db.execSQL("CREATE TABLE IF NOT EXISTS scene_state_profiles (world_profile TEXT PRIMARY KEY, day_number INTEGER DEFAULT 1, clock_hour REAL DEFAULT 9, location TEXT DEFAULT '', hex_id TEXT DEFAULT '', time_of_day TEXT DEFAULT '', weather TEXT DEFAULT '', lighting TEXT DEFAULT '', danger TEXT DEFAULT '', objective TEXT DEFAULT '', present_npcs_json TEXT DEFAULT '[]', threats_json TEXT DEFAULT '[]', clues_json TEXT DEFAULT '[]', pending_consequences_json TEXT DEFAULT '[]', updated_at INTEGER DEFAULT 0)");
        addColumnIfMissing(db,"scene_state_profiles","day_number","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"scene_state_profiles","clock_hour","REAL DEFAULT 9");
        db.execSQL("INSERT OR IGNORE INTO scene_state_profiles(world_profile,location,hex_id,time_of_day,weather,lighting,danger,objective,present_npcs_json,threats_json,clues_json,pending_consequences_json,updated_at) SELECT 'pf2',location,hex_id,time_of_day,weather,lighting,danger,objective,present_npcs_json,threats_json,clues_json,pending_consequences_json,updated_at FROM scene_state WHERE id=1");
        db.execSQL("INSERT OR IGNORE INTO scene_state_profiles(world_profile) VALUES('pf2')");
        db.execSQL("CREATE TABLE IF NOT EXISTS npc_memory_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, stable_id TEXT DEFAULT '', name TEXT NOT NULL COLLATE NOCASE, summary TEXT DEFAULT '', goals TEXT DEFAULT '', secrets TEXT DEFAULT '', attitude TEXT DEFAULT 'neutra', relationship TEXT DEFAULT '', knowledge TEXT DEFAULT '', debts TEXT DEFAULT '', last_seen TEXT DEFAULT '', token_uri TEXT DEFAULT '', visual_description TEXT DEFAULT '', stats_json TEXT DEFAULT '{}', hex_id TEXT DEFAULT '', active INTEGER DEFAULT 1, voice_profile TEXT DEFAULT '', updated_at INTEGER, UNIQUE(world_profile,name))");
        addColumnIfMissing(db,"npc_memory_profiles","token_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"npc_memory_profiles","visual_description","TEXT DEFAULT ''");
        addColumnIfMissing(db,"npc_memory_profiles","stats_json","TEXT DEFAULT '{}'");
        addColumnIfMissing(db,"npc_memory_profiles","hex_id","TEXT DEFAULT ''");
        addColumnIfMissing(db,"npc_memory_profiles","active","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"npc_memory_profiles","voice_profile","TEXT DEFAULT ''");
        db.execSQL("INSERT OR IGNORE INTO npc_memory_profiles(world_profile,name,summary,goals,secrets,attitude,relationship,knowledge,debts,last_seen,updated_at) SELECT 'pf2',name,summary,goals,secrets,attitude,relationship,knowledge,debts,last_seen,updated_at FROM npc_memory");
    }


    private static void createManualLocalIndexTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS manual_local_pages (id INTEGER PRIMARY KEY AUTOINCREMENT, manual_id INTEGER NOT NULL, source_key TEXT NOT NULL, page_no INTEGER NOT NULL, title TEXT DEFAULT '', content TEXT DEFAULT '', normalized TEXT DEFAULT '', section_title TEXT DEFAULT '', chapter_title TEXT DEFAULT '', content_kind TEXT DEFAULT 'prose', structure_json TEXT DEFAULT '{}', cross_refs_json TEXT DEFAULT '[]', page_hash TEXT DEFAULT '', indexed_at INTEGER DEFAULT 0, UNIQUE(manual_id,source_key,page_no))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_manual_local_pages_manual ON manual_local_pages(manual_id,source_key,page_no)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_manual_local_pages_source ON manual_local_pages(source_key,manual_id)");
        createManualLocalFtsIndex(db,false);
    }

    /**
     * Índice invertido persistente dos textos já extraídos. A tabela FTS4 usa
     * manual_local_pages como conteúdo externo: o texto não é duplicado, mas a
     * busca deixa de executar LIKE '%termo%' sobre todas as páginas de todos os
     * manuais a cada cena. Os gatilhos mantêm o índice sincronizado com novas
     * importações e reindexações.
     */
    private static void createManualLocalFtsIndex(SQLiteDatabase db,boolean rebuild){
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS manual_local_pages_fts USING fts4(content='manual_local_pages', normalized, title, section_title, chapter_title, tokenize=unicode61)");
        db.execSQL("CREATE TABLE IF NOT EXISTS manual_search_index_state (id INTEGER PRIMARY KEY,status TEXT NOT NULL DEFAULT 'pending',source_pages INTEGER DEFAULT 0,indexed_pages INTEGER DEFAULT 0,last_error TEXT DEFAULT '',updated_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT OR IGNORE INTO manual_search_index_state(id,status,source_pages,indexed_pages,last_error,updated_at) VALUES(1,'pending',0,0,'',0)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS manual_local_pages_ai AFTER INSERT ON manual_local_pages BEGIN INSERT INTO manual_local_pages_fts(docid,normalized,title,section_title,chapter_title) VALUES(new.id,new.normalized,new.title,new.section_title,new.chapter_title); END");
        // Em tabelas FTS com conteúdo externo, a remoção precisa ocorrer antes
        // de a linha-base desaparecer; depois disso o FTS já não consegue ler
        // os termos antigos para eliminá-los do índice.
        db.execSQL("CREATE TRIGGER IF NOT EXISTS manual_local_pages_ad BEFORE DELETE ON manual_local_pages BEGIN DELETE FROM manual_local_pages_fts WHERE docid=old.id; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS manual_local_pages_bu BEFORE UPDATE ON manual_local_pages BEGIN DELETE FROM manual_local_pages_fts WHERE docid=old.id; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS manual_local_pages_au AFTER UPDATE ON manual_local_pages BEGIN INSERT INTO manual_local_pages_fts(docid,normalized,title,section_title,chapter_title) VALUES(new.id,new.normalized,new.title,new.section_title,new.chapter_title); END");
        if(rebuild)db.execSQL("INSERT INTO manual_local_pages_fts(manual_local_pages_fts) VALUES('rebuild')");
    }

    public boolean isManualSearchIndexReady(){
        try{return "ready".equals(queryOne("SELECT status FROM manual_search_index_state WHERE id=1",null).optString("status"));}
        catch(Exception e){AppLog.w("AppDb","Estado do índice invertido indisponível.",e);return false;}
    }

    /** Reconstrói apenas o índice de busca, em executor de I/O. Os PDFs e as
     * páginas já extraídas não são reabertos nem reprocessados. */
    public synchronized JSONObject rebuildManualSearchIndexIfNeeded(){
        JSONObject out=new JSONObject();SQLiteDatabase sql=getWritableDatabase();
        try{
            createManualLocalFtsIndex(sql,false);
            JSONObject state=queryOne("SELECT * FROM manual_search_index_state WHERE id=1",null);
            if("ready".equals(state.optString("status"))){out.put("status","ready").put("rebuilt",false).put("pages",state.optInt("indexed_pages",0));return out;}
            ContentValues building=new ContentValues();building.put("status","building");building.put("last_error","");building.put("updated_at",System.currentTimeMillis());sql.update("manual_search_index_state",building,"id=1",null);
            long started=System.currentTimeMillis();sql.beginTransaction();int sourcePages=0;
            try{
                Cursor count=sql.rawQuery("SELECT COUNT(*) FROM manual_local_pages",null);try{sourcePages=count.moveToFirst()?count.getInt(0):0;}finally{count.close();}
                sql.execSQL("INSERT INTO manual_local_pages_fts(manual_local_pages_fts) VALUES('rebuild')");
                ContentValues ready=new ContentValues();ready.put("status","ready");ready.put("source_pages",sourcePages);ready.put("indexed_pages",sourcePages);ready.put("last_error","");ready.put("updated_at",System.currentTimeMillis());sql.update("manual_search_index_state",ready,"id=1",null);sql.setTransactionSuccessful();
            }finally{sql.endTransaction();}
            out.put("status","ready").put("rebuilt",true).put("pages",sourcePages).put("elapsed_ms",System.currentTimeMillis()-started);return out;
        }catch(Exception e){
            try{ContentValues failed=new ContentValues();failed.put("status","error");failed.put("last_error",safeDatabaseError(e));failed.put("updated_at",System.currentTimeMillis());sql.update("manual_search_index_state",failed,"id=1",null);out.put("status","error").put("error",safeDatabaseError(e));}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao registrar erro do índice invertido.",ignored);}AppLog.e("AppDb","Falha ao reconstruir o índice invertido local.",e);return out;
        }
    }

    private static void createManualRagPartTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS manual_rag_parts (id INTEGER PRIMARY KEY AUTOINCREMENT, manual_id INTEGER NOT NULL, source_hash TEXT NOT NULL, part_no INTEGER NOT NULL, total_parts INTEGER NOT NULL, start_page INTEGER NOT NULL, end_page INTEGER NOT NULL, page_count INTEGER NOT NULL, size_bytes INTEGER DEFAULT 0, split_mode TEXT DEFAULT '', local_path TEXT DEFAULT '', rag_document TEXT DEFAULT '', rag_status TEXT DEFAULT 'prepared', rag_error TEXT DEFAULT '', rag_store_name TEXT DEFAULT '', rag_modified_time TEXT DEFAULT '', indexed_at INTEGER DEFAULT 0, UNIQUE(manual_id,source_hash,part_no))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_manual_rag_parts_manual ON manual_rag_parts(manual_id,source_hash,part_no)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_manual_rag_parts_status ON manual_rag_parts(rag_status,rag_store_name)");
    }

    private static void createReliabilityAndImmersionTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL DEFAULT 'pf2', category TEXT NOT NULL, title TEXT NOT NULL, summary TEXT, before_json TEXT DEFAULT '{}', after_json TEXT DEFAULT '{}', reversible INTEGER DEFAULT 0, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS scene_state (id INTEGER PRIMARY KEY, day_number INTEGER DEFAULT 1, location TEXT DEFAULT '', hex_id TEXT DEFAULT '', time_of_day TEXT DEFAULT '', weather TEXT DEFAULT '', lighting TEXT DEFAULT '', danger TEXT DEFAULT '', objective TEXT DEFAULT '', present_npcs_json TEXT DEFAULT '[]', threats_json TEXT DEFAULT '[]', clues_json TEXT DEFAULT '[]', pending_consequences_json TEXT DEFAULT '[]', updated_at INTEGER)");
        addColumnIfMissing(db,"scene_state","day_number","INTEGER DEFAULT 1");
        db.execSQL("INSERT OR IGNORE INTO scene_state(id,updated_at) VALUES(1,0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS npc_memory (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE COLLATE NOCASE, summary TEXT DEFAULT '', goals TEXT DEFAULT '', secrets TEXT DEFAULT '', attitude TEXT DEFAULT 'neutra', relationship TEXT DEFAULT '', knowledge TEXT DEFAULT '', debts TEXT DEFAULT '', last_seen TEXT DEFAULT '', updated_at INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_npc_memory_updated ON npc_memory(updated_at DESC)");
    }

    private static void addColumnIfMissing(SQLiteDatabase db,String table,String column,String declaration) {
        if (hasColumn(db,table,column)) return;
        db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+declaration);
    }

    private void repairLegacyCharacterIdentities(SQLiteDatabase db){
        Cursor c=null;try{
            c=db.rawQuery("SELECT id,species,class_name,background,visual_description,notes,spells,sheet_json FROM characters",null);
            while(c.moveToNext())try{
                JSONObject row=rowToJson(c);if(!CharacterIdentityCoherence.repairLegacyRow(row))continue;
                ContentValues v=new ContentValues();v.put("visual_description",row.optString("visual_description"));v.put("notes",row.optString("notes"));v.put("spells",row.optString("spells"));v.put("sheet_json",row.optString("sheet_json","{}"));v.put("updated_at",System.currentTimeMillis());
                db.update("characters",v,"id=?",new String[]{String.valueOf(row.optLong("id"))});
            }catch(Exception rowFailure){AppLog.w("AppDb","Uma ficha antiga não pôde ter a identidade narrativa reparada.",rowFailure);}
        }catch(Exception failure){AppLog.w("AppDb","Não foi possível auditar a coerência das fichas antigas.",failure);}finally{if(c!=null)c.close();}
    }

    private static void backfillLegacyTransactionIdentities(SQLiteDatabase db,String onlyProfile){
        Cursor c=null;try{
            String sql="SELECT id,world_profile,title,summary,payload_json,status,created_at,applied_at,effect_key FROM pending_transactions"+(onlyProfile==null?"":" WHERE world_profile=?");
            c=db.rawQuery(sql,onlyProfile==null?null:new String[]{onlyProfile});
            while(c.moveToNext())try{
                long id=c.getLong(c.getColumnIndexOrThrow("id"));String world=c.getString(c.getColumnIndexOrThrow("world_profile"));String title=c.getString(c.getColumnIndexOrThrow("title"));String summary=c.getString(c.getColumnIndexOrThrow("summary"));String payloadRaw=c.getString(c.getColumnIndexOrThrow("payload_json"));String status=c.getString(c.getColumnIndexOrThrow("status"));long createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));long appliedAt=c.getLong(c.getColumnIndexOrThrow("applied_at"));String key=c.getString(c.getColumnIndexOrThrow("effect_key"));
                if(key==null||key.trim().length()==0)key=LegacyTransactionIdentity.key(world,title,summary,payloadRaw,createdAt);else key=key.trim();
                JSONObject payload=parseBackupObject(payloadRaw);payload.put("_effect_key",key);
                ContentValues v=new ContentValues();v.put("effect_key",key);v.put("payload_json",payload.toString());db.update("pending_transactions",v,"id=?",new String[]{String.valueOf(id)});
                if("applied".equals(status)){ContentValues receipt=new ContentValues();receipt.put("world_profile",world);receipt.put("effect_key",key);receipt.put("source","migração de transação legada");receipt.put("applied_at",appliedAt>0?appliedAt:createdAt);db.insertWithOnConflict("effect_receipts",null,receipt,SQLiteDatabase.CONFLICT_IGNORE);}
            }catch(Exception rowFailure){AppLog.w("AppDb","Uma transação antiga permaneceu bloqueada para revisão.",rowFailure);}
        }catch(Exception failure){AppLog.w("AppDb","Não foi possível completar as identidades das transações antigas.",failure);}finally{if(c!=null)c.close();}
    }

    private static void backfillLegacyWorldEventIdentities(SQLiteDatabase db,String onlyProfile){
        Cursor c=null;try{
            String sql="SELECT id,world_profile,title,description,due_at,due_day,consequence_json,trigger_json,procedure_json,created_at,status,consequence_status,event_key,transaction_key,application_receipt FROM world_events"+(onlyProfile==null?"":" WHERE world_profile=?");
            c=db.rawQuery(sql,onlyProfile==null?null:new String[]{onlyProfile});
            while(c.moveToNext())try{
                long id=c.getLong(c.getColumnIndexOrThrow("id"));String world=c.getString(c.getColumnIndexOrThrow("world_profile"));String eventKey=c.getString(c.getColumnIndexOrThrow("event_key"));
                if(eventKey==null||eventKey.trim().length()==0)eventKey=LegacyWorldEventIdentity.key(world,id,c.getString(c.getColumnIndexOrThrow("title")),c.getString(c.getColumnIndexOrThrow("description")),c.getLong(c.getColumnIndexOrThrow("due_at")),c.getInt(c.getColumnIndexOrThrow("due_day")),c.getString(c.getColumnIndexOrThrow("consequence_json")),c.getString(c.getColumnIndexOrThrow("trigger_json")),c.getString(c.getColumnIndexOrThrow("procedure_json")),c.getLong(c.getColumnIndexOrThrow("created_at")));else eventKey=eventKey.trim();
                String transactionKey=c.getString(c.getColumnIndexOrThrow("transaction_key"));if(transactionKey==null||transactionKey.trim().length()==0)transactionKey=LegacyWorldEventIdentity.transactionKey(eventKey);else transactionKey=transactionKey.trim();
                String status=c.getString(c.getColumnIndexOrThrow("status")),consequenceStatus=c.getString(c.getColumnIndexOrThrow("consequence_status")),applicationReceipt=c.getString(c.getColumnIndexOrThrow("application_receipt"));boolean applied="resolved".equalsIgnoreCase(status)||"applied".equalsIgnoreCase(consequenceStatus);
                ContentValues v=new ContentValues();v.put("event_key",eventKey);v.put("transaction_key",transactionKey);if(applied&&(applicationReceipt==null||applicationReceipt.trim().length()==0))v.put("application_receipt",transactionKey);db.update("world_events",v,"id=?",new String[]{String.valueOf(id)});
                if(applied){ContentValues receipt=new ContentValues();receipt.put("world_profile",world);receipt.put("effect_key",transactionKey);receipt.put("source","migração de evento legado");receipt.put("applied_at",Math.max(0L,c.getLong(c.getColumnIndexOrThrow("created_at"))));db.insertWithOnConflict("effect_receipts",null,receipt,SQLiteDatabase.CONFLICT_IGNORE);}
            }catch(Exception rowFailure){AppLog.w("AppDb","Um evento antigo permaneceu bloqueado para revisão.",rowFailure);}
        }catch(Exception failure){AppLog.w("AppDb","Não foi possível completar as identidades dos eventos antigos.",failure);}finally{if(c!=null)c.close();}
    }

    private static String safe(String value){return value==null?"":value;}

    private static boolean hasColumn(SQLiteDatabase db,String table,String column) {
        Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null);
        try { while(c.moveToNext()) if(column.equalsIgnoreCase(c.getString(c.getColumnIndexOrThrow("name")))) return true; }
        finally { c.close(); }
        return false;
    }

    private static int countQuery(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try { return c.moveToFirst()?c.getInt(0):0; }
        finally { c.close(); }
    }

    private static void createTokenPdfTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS token_pdfs (id INTEGER PRIMARY KEY AUTOINCREMENT, source_uri TEXT UNIQUE, document_id TEXT, name TEXT, label TEXT, group_key TEXT, category TEXT, class_name TEXT, species TEXT, set_number INTEGER DEFAULT 0, size_bytes INTEGER DEFAULT 0, modified_time INTEGER DEFAULT 0, page_count INTEGER DEFAULT -1, columns_count INTEGER DEFAULT 5, rows_count INTEGER DEFAULT 9, margin_x REAL DEFAULT 0.065, margin_y REAL DEFAULT 0.055, grid_left REAL DEFAULT 0.065, grid_top REAL DEFAULT 0.055, grid_right REAL DEFAULT 0.935, grid_bottom REAL DEFAULT 0.945, processed_pages TEXT DEFAULT '[]', source_root TEXT DEFAULT '', last_seen INTEGER DEFAULT 0, enabled INTEGER DEFAULT 1)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_token_pdfs_group ON token_pdfs(group_key,enabled)");
        db.execSQL("CREATE TABLE IF NOT EXISTS pdf_tokens (id INTEGER PRIMARY KEY AUTOINCREMENT, pdf_id INTEGER NOT NULL, page_no INTEGER NOT NULL, token_index INTEGER NOT NULL, row_no INTEGER NOT NULL, col_no INTEGER NOT NULL, crop_x REAL, crop_y REAL, crop_w REAL, crop_h REAL, thumb_path TEXT, full_path TEXT, favorite INTEGER DEFAULT 0, assigned INTEGER DEFAULT 0, last_used INTEGER DEFAULT 0, UNIQUE(pdf_id,page_no,token_index))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pdf_tokens_page ON pdf_tokens(pdf_id,page_no)");
    }


    private static void createLibrarySyncTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS library_sync_state (source_key TEXT PRIMARY KEY, source_root TEXT DEFAULT '', status TEXT DEFAULT 'never', item_count INTEGER DEFAULT 0, added_count INTEGER DEFAULT 0, changed_count INTEGER DEFAULT 0, missing_count INTEGER DEFAULT 0, last_started INTEGER DEFAULT 0, last_success INTEGER DEFAULT 0, last_error TEXT DEFAULT '', details_json TEXT DEFAULT '{}')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_library_sync_status ON library_sync_state(status,last_success DESC)");
    }


    private static void createMapAtlasTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS map_packages (package_id TEXT PRIMARY KEY, map_id TEXT, world_profile TEXT DEFAULT 'pf2', label TEXT, image_uri TEXT, overlay_uri TEXT, regions_overlay_uri TEXT DEFAULT '', political_overlay_uri TEXT DEFAULT '', vector_overlay_uri TEXT DEFAULT '', vector_overlay_gzip_uri TEXT DEFAULT '', hex_index_uri TEXT DEFAULT '', overlay_mode TEXT DEFAULT 'dynamic', manifest_json TEXT, source_index_json TEXT DEFAULT '{}', image_width INTEGER, image_height INTEGER, hex_count INTEGER DEFAULT 0, location_count INTEGER DEFAULT 0, nation_count INTEGER DEFAULT 0, distance_miles REAL DEFAULT 0, distance_km REAL DEFAULT 0, min_label_zoom REAL DEFAULT 1.6, imported_at INTEGER, ready INTEGER DEFAULT 0)");
        addColumnIfMissing(db,"map_packages","world_profile","TEXT DEFAULT 'pf2'");
        addColumnIfMissing(db,"map_packages","regions_overlay_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","political_overlay_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","nation_count","INTEGER DEFAULT 0");
        addColumnIfMissing(db,"map_packages","vector_overlay_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","vector_overlay_gzip_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","hex_index_uri","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_packages","overlay_mode","TEXT DEFAULT 'dynamic'");
        addColumnIfMissing(db,"map_packages","distance_miles","REAL DEFAULT 0");
        addColumnIfMissing(db,"map_packages","distance_km","REAL DEFAULT 0");
        addColumnIfMissing(db,"map_packages","min_label_zoom","REAL DEFAULT 1.6");
        addColumnIfMissing(db,"map_packages","source_index_json","TEXT DEFAULT '{}'");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_packages_profile ON map_packages(world_profile,ready,imported_at DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS map_hex_atlas (package_id TEXT NOT NULL, hex_id TEXT NOT NULL, center_x REAL, center_y REAL, terrain_primary TEXT, terrain_secondary TEXT, terrain_confidence REAL, terrain_method TEXT DEFAULT '', metaregion_id TEXT DEFAULT '', roads_json TEXT, waterways_json TEXT, political_json TEXT, review_status TEXT, review_reasons TEXT, location_ids TEXT, nearby_location_ids TEXT DEFAULT '[]', raw_json TEXT DEFAULT '{}', PRIMARY KEY(package_id,hex_id))");
        addColumnIfMissing(db,"map_hex_atlas","terrain_method","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_hex_atlas","metaregion_id","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_hex_atlas","nearby_location_ids","TEXT DEFAULT '[]'");
        addColumnIfMissing(db,"map_hex_atlas","raw_json","TEXT DEFAULT '{}'");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_hex_package ON map_hex_atlas(package_id,hex_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS map_locations (package_id TEXT NOT NULL, location_id TEXT NOT NULL, name TEXT, type TEXT, hex_id TEXT, region TEXT, metaregion_id TEXT DEFAULT '', area_or_nation TEXT DEFAULT '', canonical_tier TEXT DEFAULT '', official_5e INTEGER DEFAULT 0, scope TEXT, placement_method TEXT, placement_confidence REAL, placement_status TEXT, source_ids TEXT, tags TEXT, raw_json TEXT, PRIMARY KEY(package_id,location_id))");
        addColumnIfMissing(db,"map_locations","metaregion_id","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_locations","area_or_nation","TEXT DEFAULT ''");
        addColumnIfMissing(db,"map_locations","canonical_tier","TEXT DEFAULT ''");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_locations_hex ON map_locations(package_id,hex_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS map_nations (package_id TEXT NOT NULL, nation_id TEXT NOT NULL, name_en TEXT DEFAULT '', name_ptbr TEXT DEFAULT '', polygon_json TEXT DEFAULT '[]', hex_ids_json TEXT DEFAULT '[]', raw_json TEXT DEFAULT '{}', PRIMARY KEY(package_id,nation_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_nations_package ON map_nations(package_id,nation_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS map_metaregions (package_id TEXT NOT NULL, metaregion_id TEXT NOT NULL, name_en TEXT DEFAULT '', name_ptbr TEXT DEFAULT '', polygon_json TEXT DEFAULT '[]', source_text TEXT DEFAULT '', raw_json TEXT DEFAULT '{}', PRIMARY KEY(package_id,metaregion_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_metaregions_package ON map_metaregions(package_id,metaregion_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS map_hex_locations (package_id TEXT NOT NULL, hex_id TEXT NOT NULL, location_id TEXT NOT NULL, PRIMARY KEY(package_id,hex_id,location_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_hex_locations ON map_hex_locations(package_id,hex_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS map_world_state (world_profile TEXT PRIMARY KEY, logical_map_id TEXT NOT NULL DEFAULT '', image_uri TEXT DEFAULT '', overlay_uri TEXT DEFAULT '', manifest_json TEXT DEFAULT '', map_package_id TEXT DEFAULT '', current_hex_id TEXT DEFAULT '', overlay_visible INTEGER DEFAULT 1, overlay_mode TEXT DEFAULT 'dynamic', min_label_zoom REAL DEFAULT 1.6, reveals_json TEXT DEFAULT '{}', grid_type TEXT DEFAULT 'Quadrada', grid_size REAL DEFAULT 64, fog_enabled INTEGER DEFAULT 0, token_x REAL DEFAULT 0.5, token_y REAL DEFAULT 0.5, scale REAL DEFAULT 1, offset_x REAL DEFAULT 0, offset_y REAL DEFAULT 0, position_locked INTEGER DEFAULT 0, destination_hex_id TEXT DEFAULT '', route_json TEXT DEFAULT '[]', route_owner TEXT DEFAULT 'Jogador', regions_overlay_visible INTEGER DEFAULT 1, political_overlay_visible INTEGER DEFAULT 0)");
        addColumnIfMissing(db,"map_world_state","logical_map_id","TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(db,"map_world_state","regions_overlay_visible","INTEGER DEFAULT 1");
        addColumnIfMissing(db,"map_world_state","political_overlay_visible","INTEGER DEFAULT 0");
        db.execSQL("INSERT OR IGNORE INTO map_world_state(world_profile,image_uri,overlay_uri,manifest_json,map_package_id,current_hex_id,overlay_visible,reveals_json,grid_type,grid_size,fog_enabled,token_x,token_y,scale,offset_x,offset_y,position_locked,destination_hex_id,route_json,route_owner) SELECT 'pf2',COALESCE(image_uri,''),COALESCE(overlay_uri,''),COALESCE(manifest_json,''),COALESCE(map_package_id,''),COALESCE(current_hex_id,''),COALESCE(overlay_visible,1),COALESCE(reveals_json,'{}'),COALESCE(grid_type,'Quadrada'),COALESCE(grid_size,64),COALESCE(fog_enabled,0),COALESCE(token_x,.5),COALESCE(token_y,.5),COALESCE(scale,1),COALESCE(offset_x,0),COALESCE(offset_y,0),COALESCE(position_locked,0),COALESCE(destination_hex_id,''),COALESCE(route_json,'[]'),COALESCE(route_owner,'Jogador') FROM map_state WHERE id=1");
        db.execSQL("INSERT OR IGNORE INTO map_world_state(world_profile) VALUES('pf2')");
        createMapCorrectionTables(db);
    }

    private static void createMapCorrectionTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS map_hex_corrections (map_id TEXT NOT NULL, hex_id TEXT NOT NULL, terrain_primary TEXT, terrain_secondary TEXT, terrain_confidence REAL, roads_json TEXT, waterways_json TEXT, political_json TEXT, review_status TEXT, review_reasons TEXT, custom_locations_json TEXT DEFAULT '[]', hide_unconfirmed_locations INTEGER DEFAULT 0, review_note TEXT, updated_at INTEGER, PRIMARY KEY(map_id,hex_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_corrections_map ON map_hex_corrections(map_id,hex_id)");
    }

    private static void createMapNoteTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS map_hex_notes (world_profile TEXT NOT NULL DEFAULT 'pf2', map_id TEXT NOT NULL, hex_id TEXT NOT NULL, note_text TEXT NOT NULL DEFAULT '', note_title TEXT NOT NULL DEFAULT '', note_details TEXT NOT NULL DEFAULT '', icon TEXT NOT NULL DEFAULT '✎', created_by TEXT NOT NULL DEFAULT 'player', created_at INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(world_profile,map_id,hex_id))");
        addColumnIfMissing(db,"map_hex_notes","note_title","TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(db,"map_hex_notes","note_details","TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(db,"map_hex_notes","icon","TEXT NOT NULL DEFAULT '✎'");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_hex_notes_lookup ON map_hex_notes(world_profile,map_id,hex_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_map_hex_notes_updated ON map_hex_notes(world_profile,updated_at DESC)");
    }

    /**
     * Esquema 8.3: camada mutável de campanha sobre o Atlas canônico.
     * As tabelas oficiais map_locations/map_hex_atlas continuam somente leitura.
     */
    public static void ensureCampaignPoiSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS campaign_poi_meta (world_profile TEXT PRIMARY KEY, campaign_key TEXT NOT NULL, last_generation_tick INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS campaign_pois (poi_id TEXT PRIMARY KEY, world_profile TEXT NOT NULL, campaign_key TEXT NOT NULL, map_id TEXT NOT NULL DEFAULT '', name TEXT NOT NULL, name_key TEXT NOT NULL, type TEXT NOT NULL DEFAULT 'point_of_interest', hex_id TEXT NOT NULL, location_id TEXT, q INTEGER DEFAULT 0, r INTEGER DEFAULT 0, source TEXT NOT NULL DEFAULT 'campaign', origin TEXT NOT NULL DEFAULT 'campaign_discovery', canonical INTEGER NOT NULL DEFAULT 0, hidden INTEGER NOT NULL DEFAULT 0, discovered INTEGER NOT NULL DEFAULT 1, created_by TEXT NOT NULL DEFAULT 'player', created_tick INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'active', description TEXT NOT NULL DEFAULT '', tags_json TEXT NOT NULL DEFAULT '[]', faction_id TEXT, owner_npc_id TEXT, parent_poi_id TEXT, development_level INTEGER NOT NULL DEFAULT 1, icon TEXT NOT NULL DEFAULT '•', notes TEXT NOT NULL DEFAULT '', private_notes TEXT NOT NULL DEFAULT '', settlement_key TEXT, settlement_json TEXT NOT NULL DEFAULT '{}', state_json TEXT NOT NULL DEFAULT '{}', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, CHECK(canonical=0), CHECK(source IN ('campaign','procedural')), CHECK(status IN ('active','abandoned','ruined','destroyed','occupied','rebuilding')))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_pois_atlas ON campaign_pois(world_profile,campaign_key,map_id,hex_id,hidden,discovered)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_pois_name ON campaign_pois(world_profile,campaign_key,name_key)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_pois_parent ON campaign_pois(parent_poi_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS campaign_poi_history (id INTEGER PRIMARY KEY AUTOINCREMENT, poi_id TEXT NOT NULL, tick INTEGER NOT NULL DEFAULT 0, reason TEXT NOT NULL DEFAULT '', responsible TEXT NOT NULL DEFAULT '', previous_json TEXT NOT NULL DEFAULT '{}', current_json TEXT NOT NULL DEFAULT '{}', created_at INTEGER NOT NULL, FOREIGN KEY(poi_id) REFERENCES campaign_pois(poi_id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_poi_history ON campaign_poi_history(poi_id,tick,id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS campaign_poi_proposals (proposal_key TEXT PRIMARY KEY, world_profile TEXT NOT NULL, campaign_key TEXT NOT NULL, hex_id TEXT NOT NULL DEFAULT '', source_text TEXT NOT NULL DEFAULT '', candidate_json TEXT NOT NULL DEFAULT '{}', status TEXT NOT NULL DEFAULT 'suggested', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_campaign_poi_proposals ON campaign_poi_proposals(world_profile,campaign_key,status,updated_at DESC)");
    }

    public JSONObject getCampaign() {
        ensureWorldProfileData(activeMapProfile);
        JSONObject out=queryOne("SELECT * FROM campaign_profiles WHERE world_profile=?",new String[]{getActiveMapProfile()});
        if(out.length()==0)throw new IllegalStateException("Falha ao ler a campanha do mundo ativo; os dados não foram tratados como vazios.");
        return out;
    }

    private static String canonicalCampaignRulesProfile(String profile){return "Pathfinder 2e Remaster";}

    public void saveCampaign(String name, String profile, String tone, boolean feats, boolean multiclass, boolean flanking, String notes) {
        ensureWorldProfileData(activeMapProfile);
        String storedProfile="Pathfinder 2e Remaster";
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("rules_profile", storedProfile); v.put("tone", tone);
        v.put("feats", feats ? 1 : 0); v.put("multiclass", multiclass ? 1 : 0); v.put("flanking", flanking ? 1 : 0);
        v.put("notes", notes); v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("campaign_profiles", v, "world_profile=?", new String[]{getActiveMapProfile()});
        getWritableDatabase().update("campaign",v,"id=1",null);
    }

    public long addCharacter(String name, String species, String className, int level, int hp, int maxHp, int ac, int str, int dex, int con, int intel, int wis, int cha, boolean active, String notes) {
        ContentValues v = new ContentValues();
        v.put("world_profile",getActiveMapProfile()); v.put("name", name); v.put("species", species); v.put("class_name", className); v.put("level", level);
        v.put("hp", hp); v.put("max_hp", maxHp); v.put("ac", ac); v.put("str", str); v.put("dex", dex); v.put("con", con); v.put("int_score", intel); v.put("wis", wis); v.put("cha", cha); v.put("active", active ? 1 : 0); v.put("notes", notes);
        v.put("created_at", System.currentTimeMillis()); v.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insert("characters", null, v);
    }

    public void updateCharacter(long id, String name, String species, String className, int level, int hp, int maxHp, int ac, int str, int dex, int con, int intel, int wis, int cha, boolean active, String notes) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("species", species); v.put("class_name", className); v.put("level", level);
        v.put("hp", hp); v.put("max_hp", maxHp); v.put("ac", ac); v.put("str", str); v.put("dex", dex); v.put("con", con); v.put("int_score", intel); v.put("wis", wis); v.put("cha", cha); v.put("active", active ? 1 : 0); v.put("notes", notes); v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("characters", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void updateCharacterRuleDetails(long id,String background,String visual,String proficiencies,String equipment,String spells,String rulesSource){
        ContentValues v=new ContentValues();v.put("background",background);v.put("visual_description",visual);v.put("proficiencies",proficiencies);v.put("equipment",equipment);v.put("spells",spells);v.put("rules_source",rulesSource);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("characters",v,"id=?",new String[]{String.valueOf(id)});
    }
    public void updateCharacterSheet(long id,String sheetJson){
        ContentValues v=new ContentValues();v.put("sheet_json",sheetJson==null?"{}":sheetJson);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("characters",v,"id=?",new String[]{String.valueOf(id)});
    }
    public void setCharacterToken(long id,String tokenUri){
        String safe=tokenUri==null?"":tokenUri.trim();JSONObject ch=getCharacter(id);ContentValues v=new ContentValues();v.put("token_uri",safe);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("characters",v,"id=?",new String[]{String.valueOf(id)});
        ContentValues combat=new ContentValues();combat.put("source_kind","character");combat.put("source_id",id);combat.put("token_uri",safe);
        getWritableDatabase().update("combatants",combat,"world_profile=? AND (source_kind='character' AND source_id=? OR is_player=1 AND lower(name)=lower(?))",new String[]{getActiveMapProfile(),String.valueOf(id),ch.optString("name","")});
    }

    public List<JSONObject> listCharacters() { return queryList("SELECT * FROM characters WHERE world_profile=? ORDER BY active DESC, name COLLATE NOCASE", new String[]{getActiveMapProfile()}); }
    public List<JSONObject> listActiveCharacters() { return queryList("SELECT * FROM characters WHERE world_profile=? AND active=1 ORDER BY name COLLATE NOCASE", new String[]{getActiveMapProfile()}); }
    public JSONObject getCharacter(long id) { return queryOne("SELECT * FROM characters WHERE id=? AND world_profile=?", new String[]{String.valueOf(id),getActiveMapProfile()}); }
    public JSONObject findCharacterByName(String name) {
        if(name==null||name.trim().length()==0)return new JSONObject();
        JSONObject exact=queryOne("SELECT * FROM characters WHERE world_profile=? AND lower(name)=lower(?) ORDER BY active DESC LIMIT 1",new String[]{getActiveMapProfile(),name.trim()});
        if(exact.length()>0)return exact;
        return queryOne("SELECT * FROM characters WHERE world_profile=? AND (lower(name) LIKE lower(?) OR lower(?) LIKE '%'||lower(name)||'%') ORDER BY active DESC,length(name) DESC LIMIT 1",new String[]{getActiveMapProfile(),"%"+name.trim()+"%",name.trim()});
    }
    public void setCharacterHp(long id,int hp){
        JSONObject ch=getCharacter(id);int max=Math.max(1,ch.optInt("max_hp",1));int safe=Math.max(0,Math.min(max,hp));
        ContentValues v=new ContentValues();v.put("hp",safe);v.put("updated_at",System.currentTimeMillis());
        JSONObject sheet=readCharacterSheet(ch);try{sheet.put("hp",safe);sheet.put("max_hp",max);v.put("sheet_json",sheet.toString());}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        getWritableDatabase().update("characters",v,"id=?",new String[]{String.valueOf(id)});syncPlayerCombatantHp(ch.optString("name"),safe);
    }
    public void setCharacterEquipmentAndNotes(long id,String equipment,String notes){
        String safeEquipment=equipment==null?"":equipment;String safeNotes=notes==null?"":notes;JSONObject ch=getCharacter(id);
        ContentValues v=new ContentValues();v.put("equipment",safeEquipment);v.put("notes",safeNotes);v.put("updated_at",System.currentTimeMillis());
        JSONObject sheet=readCharacterSheet(ch);try{
            sheet.put("inventory",safeEquipment);sheet.put("equipment",safeEquipment);sheet.put("campaign_notes",safeNotes);
            String master=extractMasterNotesForSheet(safeNotes);if(!master.isEmpty()){String extra=sheet.optString("extra_notes").trim();if(!extra.contains(master))sheet.put("extra_notes",extra+(extra.isEmpty()?"":"\n")+master);}
            v.put("sheet_json",sheet.toString());
        }catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        getWritableDatabase().update("characters",v,"id=?",new String[]{String.valueOf(id)});
    }
    private JSONObject readCharacterSheet(JSONObject ch){
        try{String raw=ch==null?"":ch.optString("sheet_json","").trim();if(raw.startsWith("{"))return new JSONObject(raw);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}
        return new JSONObject();
    }
    private String extractMasterNotesForSheet(String notes){
        if(notes==null||notes.trim().isEmpty())return "";StringBuilder b=new StringBuilder();
        for(String line:notes.split("\n"))if(line.trim().startsWith("[Mestre]")){if(b.length()>0)b.append('\n');b.append(line.trim());}
        return b.toString();
    }
    private void syncPlayerCombatantHp(String characterName,int hp){if(characterName==null||characterName.length()==0)return;ContentValues v=new ContentValues();v.put("hp",hp);getWritableDatabase().update("combatants",v,"world_profile=? AND is_player=1 AND lower(name)=lower(?)",new String[]{getActiveMapProfile(),characterName});}
    public void deleteCharacter(long id) { getWritableDatabase().delete("characters", "id=?", new String[]{String.valueOf(id)}); }
    public void setCharacterActive(long id, boolean active) { ContentValues v=new ContentValues(); v.put("active",active?1:0); v.put("updated_at",System.currentTimeMillis()); getWritableDatabase().update("characters",v,"id=?",new String[]{String.valueOf(id)}); }
    public void adjustCharacterHp(long id, int delta) {
        JSONObject ch=getCharacter(id);int max=Math.max(1,ch.optInt("max_hp",1));int hp=Math.max(0,Math.min(max,ch.optInt("hp",0)+delta));setCharacterHp(id,hp);
    }

    public long addJournal(String title,String body,String kind){return addJournalFact(title,body,kind,"","system");}
    public long addJournalFact(String title,String body,String kind,String eventKey,String sourceKind){
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("title",title==null?"":title);v.put("body",body==null?"":body);v.put("kind",kind==null?"":kind);v.put("canonical_fact",1);v.put("event_key",eventKey==null?"":eventKey.trim());v.put("source_kind",sourceKind==null?"system":sourceKind);v.put("created_at",System.currentTimeMillis());
        if(eventKey!=null&&!eventKey.trim().isEmpty())return getWritableDatabase().insertWithOnConflict("journal",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        return getWritableDatabase().insert("journal",null,v);
    }
    public List<JSONObject> listJournal() { return queryList("SELECT * FROM journal WHERE world_profile=? ORDER BY created_at DESC",new String[]{getActiveMapProfile()}); }
    public void deleteJournal(long id) { getWritableDatabase().delete("journal","id=?",new String[]{String.valueOf(id)}); }

    public long addCombatant(String name,int initiative,int hp,int maxHp,int ac,String conditions,boolean player) {
        JSONObject visual=resolveCombatantVisualIdentity(name,player,"",0,"");
        return addCombatantWithVisual(name,initiative,hp,maxHp,ac,conditions,player,visual.optString("source_kind"),visual.optLong("source_id"),visual.optString("token_uri"));
    }
    public long addCombatantWithVisual(String name,int initiative,int hp,int maxHp,int ac,String conditions,boolean player,String sourceKind,long sourceId,String tokenUri) {
        JSONObject visual=resolveCombatantVisualIdentity(name,player,sourceKind,sourceId,tokenUri);
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("name",name);v.put("initiative",initiative);v.put("hp",hp);v.put("max_hp",maxHp);v.put("ac",ac);v.put("conditions",conditions);v.put("source_kind",visual.optString("source_kind"));v.put("source_id",visual.optLong("source_id"));v.put("token_uri",visual.optString("token_uri"));v.put("is_player",player?1:0);v.put("created_at",System.currentTimeMillis());
        long id=getWritableDatabase().insert("combatants",null,v); sortCombat(); return id;
    }
    private void sortCombat() {
        List<JSONObject> rows=queryList("SELECT * FROM combatants WHERE world_profile=? ORDER BY initiative DESC, is_player ASC, name",new String[]{getActiveMapProfile()}); SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction(); try { for(int i=0;i<rows.size();i++){ContentValues v=new ContentValues();v.put("sort_order",i);db.update("combatants",v,"id=?",new String[]{String.valueOf(rows.get(i).optLong("id"))});} db.setTransactionSuccessful(); } finally { db.endTransaction(); }
    }
    public List<JSONObject> listCombatants(){return queryList("SELECT * FROM combatants WHERE world_profile=? ORDER BY sort_order, initiative DESC",new String[]{getActiveMapProfile()});}
    public JSONObject findCombatantByName(String name){if(name==null||name.trim().length()==0)return new JSONObject();JSONObject exact=queryOne("SELECT * FROM combatants WHERE world_profile=? AND lower(name)=lower(?) LIMIT 1",new String[]{getActiveMapProfile(),name.trim()});if(exact.length()>0)return exact;return queryOne("SELECT * FROM combatants WHERE world_profile=? AND (lower(name) LIKE lower(?) OR lower(?) LIKE '%'||lower(name)||'%') ORDER BY length(name) DESC LIMIT 1",new String[]{getActiveMapProfile(),"%"+name.trim()+"%",name.trim()});}
    public JSONObject getCombatant(long id){if(id<=0)return new JSONObject();return queryOne("SELECT * FROM combatants WHERE id=? AND world_profile=? LIMIT 1",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void setCombatRunning(boolean running){ContentValues v=new ContentValues();v.put("running",running?1:0);v.put("updated_at",System.currentTimeMillis());if(running){v.put("round_no",1);v.put("turn_index",-1);v.put("state",CombatStateMachine.PREPARING);v.put("encounter_id","enc-"+java.util.UUID.randomUUID().toString());}else{v.put("turn_index",0);v.put("state",CombatStateMachine.IDLE);v.put("pending_roll_json","{}");v.put("encounter_id","");ContentValues clear=new ContentValues();clear.put("active_turn",0);getWritableDatabase().update("combatants",clear,"world_profile=?",new String[]{getActiveMapProfile()});}getWritableDatabase().update("combat_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});}
    public synchronized boolean transitionCombatState(String next,JSONObject pendingRoll){
        JSONObject current=getCombatState();String from=CombatStateMachine.normalize(current.optString("state"),current.optInt("running",0)==1);String to=CombatStateMachine.normalize(next,current.optInt("running",0)==1);if(!CombatStateMachine.canTransition(from,to))return false;ContentValues v=new ContentValues();v.put("state",to);v.put("pending_roll_json",pendingRoll==null?"{}":pendingRoll.toString());v.put("running",CombatStateMachine.IDLE.equals(to)||CombatStateMachine.ENCOUNTER_END.equals(to)?0:1);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("combat_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});return true;
    }
    public void deleteCombatant(long id){getWritableDatabase().delete("combatants","id=?",new String[]{String.valueOf(id)}); sortCombat();}
    /** Compatibilidade: deltas antigos usam dano/cura PF2e normal (não crítico, letal). */
    public void adjustCombatantHp(long id,int delta){if(delta<0)applyCombatantDamagePf2(id,-delta,false,false);else if(delta>0)applyCombatantHealingPf2(id,delta);}

    public JSONObject applyCombatantDamagePf2(long id,int damage,boolean critical,boolean nonlethal){return applyCombatantHpPf2(id,-Math.max(0,damage),critical,nonlethal);}
    public JSONObject applyCombatantHealingPf2(long id,int healing){return applyCombatantHpPf2(id,Math.max(0,healing),false,false);}

    private JSONObject applyCombatantHpPf2(long id,int delta,boolean critical,boolean nonlethal){
        JSONObject c=queryOne("SELECT * FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});if(c.length()==0)return new JSONObject();
        int beforeHp=Math.max(0,c.optInt("hp")),hp=beforeHp,temp=Math.max(0,c.optInt("temp_hp")),max=Math.max(1,c.optInt("max_hp",1));
        JSONObject state=parseJsonObject(c.optString("resources_json","{}"));int dying=Math.max(0,state.optInt("dying",0)),wounded=Math.max(0,state.optInt("wounded",0)),doomed=Math.max(0,state.optInt("doomed",0));boolean dead=state.optBoolean("dead",false),usesDyingRules=c.optInt("is_player",0)==1||state.optBoolean("uses_dying_rules",false);
        if(dead)return c;
        try{
            if(delta<0){
                int incomingDamage=-delta;if(!nonlethal&&incomingDamage>=2*max){hp=0;temp=0;dying=0;dead=true;state.put("unconscious",false).put("massive_damage_death",true);}
                int damage=dead?0:incomingDamage;if(temp>0&&damage>0){int absorbed=Math.min(temp,damage);temp-=absorbed;damage-=absorbed;}
                if(damage>0){
                    hp=Math.max(0,hp-damage);
                    if(beforeHp>0&&hp==0){
                        if(nonlethal){dying=0;dead=false;state.put("unconscious",true);}
                        else if(usesDyingRules){JSONObject ko=new Pathfinder2eRulesEngine().knockedOut(wounded,doomed,false,critical);dying=ko.optInt("dying",0);dead=ko.optBoolean("dead",false);state.put("unconscious",true);}
                        else{dying=0;dead=true;state.put("unconscious",false);}
                    }else if(beforeHp==0){
                        if(dying>0){dying+=critical?2:1;dead=dying>=Math.max(1,4-doomed);}
                        else if(!nonlethal){JSONObject ko=new Pathfinder2eRulesEngine().knockedOut(wounded,doomed,false,critical);dying=ko.optInt("dying",0);dead=ko.optBoolean("dead",false);state.put("unconscious",true);}
                    }
                }
            }else if(delta>0){
                hp=Math.min(max,hp+delta);
                if(hp>0){if(dying>0){dying=0;wounded+=1;}state.put("unconscious",false);dead=false;}
            }
            state.put("system","pathfinder_2e").put("dying",dying).put("wounded",wounded).put("doomed",doomed).put("dead",dead);
        }catch(Exception error){AppLog.w("AppDb","Falha ao atualizar estado dying/wounded.",error);}
        ContentValues v=new ContentValues();v.put("hp",hp);v.put("temp_hp",temp);v.put("resources_json",state.toString());v.put("conditions",syncUnconsciousCondition(c.optString("conditions",""),state.optBoolean("unconscious",false)));getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});
        if(c.optInt("is_player",0)==1){JSONObject ch=findCharacterByName(c.optString("name"));if(ch.length()>0)setCharacterHp(ch.optLong("id"),hp);}
        return getCombatant(id);
    }

    public JSONObject applyRecoveryCheckPf2(long id,int natural){
        JSONObject c=getCombatant(id);if(c.length()==0)return new JSONObject();JSONObject state=parseJsonObject(c.optString("resources_json","{}"));int dying=Math.max(0,state.optInt("dying",0));if(dying<=0)return c;
        JSONObject result=new Pathfinder2eRulesEngine().recoveryCheck(natural,dying,state.optInt("wounded",0),state.optInt("doomed",0));
        try{state.put("system","pathfinder_2e").put("dying",result.optInt("dying")).put("wounded",result.optInt("wounded")).put("doomed",result.optInt("doomed")).put("dead",result.optBoolean("dead"));if(result.optBoolean("stabilized",false))state.put("unconscious",true);}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao registrar recuperação PF2e.",ignored);}
        ContentValues v=new ContentValues();v.put("resources_json",state.toString());v.put("conditions",syncUnconsciousCondition(c.optString("conditions",""),true));getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});return result;
    }

    public JSONArray applyPf2LongRestNaturalHealing(){
        JSONArray out=new JSONArray();Pathfinder2eRulesEngine rules=new Pathfinder2eRulesEngine();for(JSONObject ch:listActiveCharacters())try{int level=Math.max(1,ch.optInt("level",1)),con=rules.attributeModifier(ch,"Constituição"),healing=Math.max(1,con)*level,before=Math.max(0,ch.optInt("hp",0)),after=Math.min(Math.max(1,ch.optInt("max_hp",1)),before+healing);if(after!=before)setCharacterHp(ch.optLong("id"),after);out.put(new JSONObject().put("character_id",ch.optLong("id")).put("name",ch.optString("name")).put("before",before).put("after",after).put("healed",after-before));}catch(Exception error){AppLog.w("AppDb","Cura natural de descanso não aplicada a "+ch.optString("name"),error);}return out;
    }

    private String syncUnconsciousCondition(String raw,boolean unconscious){
        String base=raw==null?"":raw.trim();java.util.ArrayList<String> kept=new java.util.ArrayList<String>();for(String p:base.split("[,;\n]+")){String x=p.trim();if(x.isEmpty())continue;String n=java.text.Normalizer.normalize(x.toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","");if(n.startsWith("inconsciente")||n.startsWith("unconscious"))continue;kept.add(x);}if(unconscious)kept.add("inconsciente");return android.text.TextUtils.join(", ",kept);
    }

    public void updateCombatantInitiative(long id,int initiative){ContentValues v=new ContentValues();v.put("initiative",initiative);getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});sortCombat();}
    public void updateInitiativeForSide(boolean players,int initiative){ContentValues v=new ContentValues();v.put("initiative",initiative);getWritableDatabase().update("combatants",v,"world_profile=? AND is_player=?",new String[]{getActiveMapProfile(),players?"1":"0"});sortCombat();}
    public void updateConditions(long id,String conditions){ContentValues v=new ContentValues();v.put("conditions",conditions);getWritableDatabase().update("combatants",v,"id=?",new String[]{String.valueOf(id)});}
    public JSONObject getCombatState(){ensureWorldProfileData(activeMapProfile);return queryOne("SELECT * FROM combat_state_profiles WHERE world_profile=?",new String[]{getActiveMapProfile()});}
    public void resetCombat(){SQLiteDatabase db=getWritableDatabase();db.delete("combatants","world_profile=?",new String[]{getActiveMapProfile()});ContentValues v=new ContentValues();v.put("round_no",1);v.put("turn_index",0);v.put("running",0);v.put("party_initiative",0);v.put("enemy_initiative",0);v.put("phase",0);v.put("state",CombatStateMachine.IDLE);v.put("pending_roll_json","{}");v.put("encounter_id","");v.put("updated_at",System.currentTimeMillis());db.update("combat_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});}
    public JSONObject nextTurn(){
        List<JSONObject> list=listCombatants(); if(list.size()==0)return new JSONObject(); JSONObject state=getCombatState(); int idx=state.optInt("turn_index",-1); int round=state.optInt("round_no",1); idx++; if(idx>=list.size()){idx=0;round++;}
        if(state.optInt("turn_index",-1)>=0&&state.optInt("turn_index",-1)<list.size()){long previousId=list.get(state.optInt("turn_index")).optLong("id");tickConditionTimers(previousId,"end");applyPf2EndTurnConditionUpdates(previousId);}
        ContentValues clear=new ContentValues();clear.put("active_turn",0);getWritableDatabase().update("combatants",clear,"world_profile=?",new String[]{getActiveMapProfile()});
        JSONObject active=list.get(idx);ContentValues a=new ContentValues();a.put("active_turn",1);a.put("reaction_used",0);getWritableDatabase().update("combatants",a,"id=?",new String[]{String.valueOf(active.optLong("id"))});tickConditionTimers(active.optLong("id"),"start");
        JSONObject phase=getCombatState();String phaseName=phase.optString("state","");if(CombatStateMachine.PREPARING.equals(phaseName))transitionCombatState(CombatStateMachine.INITIATIVE,new JSONObject());phase=getCombatState();phaseName=phase.optString("state","");if(CombatStateMachine.INITIATIVE.equals(phaseName)||CombatStateMachine.RESOLVING.equals(phaseName)||CombatStateMachine.AWAITING_ROLL.equals(phaseName))transitionCombatState(CombatStateMachine.ACTIVE_ROUND,new JSONObject());
        ContentValues s=new ContentValues();s.put("round_no",round);s.put("turn_index",idx);s.put("running",1);s.put("state",CombatStateMachine.ACTIVE_ROUND);s.put("pending_roll_json","{}");s.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("combat_state_profiles",s,"world_profile=?",new String[]{getActiveMapProfile()}); return active;
    }
    private void tickConditionTimers(long id,String phase){JSONObject c=queryOne("SELECT condition_timers_json FROM combatants WHERE id=?",new String[]{String.valueOf(id)});try{JSONArray timers=new JSONArray(c.optString("condition_timers_json","[]")),next=new JSONArray();for(int i=0;i<timers.length();i++){JSONObject timer=timers.optJSONObject(i);if(timer==null)continue;String tick=timer.optString("tick",timer.optString("phase","manual")).toLowerCase(java.util.Locale.ROOT);int rounds=timer.optInt("rounds",0);if(rounds>0&&phase.equals(tick))timer.put("rounds",rounds-1);if(timer.optInt("rounds",0)>0||rounds<=0)next.put(timer);}ContentValues v=new ContentValues();v.put("condition_timers_json",next.toString());getWritableDatabase().update("combatants",v,"id=?",new String[]{String.valueOf(id)});}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao atualizar duração PF2e.",ignored);}}
    private void applyPf2EndTurnConditionUpdates(long id){
        JSONObject c=queryOne("SELECT conditions FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});String raw=c.optString("conditions","");if(raw.trim().isEmpty())return;java.util.ArrayList<String> out=new java.util.ArrayList<String>();
        for(String part:raw.split("[,;\n]+")){String x=part.trim();if(x.isEmpty())continue;java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)^(frightened|amedrontado)\\s+(\\d+)$").matcher(x);if(m.matches()){int value=Math.max(0,Integer.parseInt(m.group(2))-1);if(value>0)out.add(m.group(1)+" "+value);}else out.add(x);}
        String next=android.text.TextUtils.join(", ",out);if(!next.equals(raw)){ContentValues v=new ContentValues();v.put("conditions",next);getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    }

    public long addDiceHistory(String formula,String detail,int total,String mode){return addDiceHistoryBound(formula,detail,total,mode,new JSONObject());}
    public long addDiceHistoryBound(String formula,String detail,int total,String mode,JSONObject context){
        JSONObject safe=context==null?new JSONObject():context;
        ContentValues v=new ContentValues();
        v.put("world_profile",getActiveMapProfile());v.put("formula",formula==null?"":formula);v.put("detail",detail==null?"":detail);v.put("total",total);v.put("mode",mode==null?"":mode);
        v.put("actor_character_id",safe.optLong("actor_character_id",0));v.put("actor_name",safe.optString("actor_name",""));v.put("test_key",safe.optString("test_key",""));v.put("action_signature",safe.optString("action_signature",""));v.put("source_rule_id",safe.optString("source_rule_id",""));v.put("procedure_id",safe.optString("procedure_id",""));v.put("scene_fingerprint",safe.optString("scene_fingerprint",""));v.put("campaign_fingerprint",safe.optString("campaign_fingerprint",""));v.put("target_signature",safe.optString("target_signature",""));v.put("consequence_key",safe.optString("consequence_key",""));v.put("modifier",safe.optInt("modifier",0));v.put("modifier_source",safe.optString("modifier_source",""));v.put("roll_mode",safe.optString("roll_mode",mode==null?"normal":mode));v.put("natural_roll",safe.optInt("natural_roll",0));v.put("critical",safe.optBoolean("critical",false)?1:0);v.put("critical_failure",safe.optBoolean("critical_failure",false)?1:0);v.put("outcome",safe.optString("outcome",""));v.put("cancelled_at",0);v.put("consumed_at",0);v.put("created_at",System.currentTimeMillis());
        SQLiteDatabase sql=getWritableDatabase();long id=sql.insertOrThrow("dice_history",null,v);
        sql.execSQL("DELETE FROM dice_history WHERE world_profile=? AND id NOT IN (SELECT id FROM dice_history WHERE world_profile=? ORDER BY created_at DESC LIMIT 200)",new Object[]{getActiveMapProfile(),getActiveMapProfile()});
        return id;
    }
    public JSONObject getDiceHistoryById(long id){if(id<=0)return new JSONObject();return queryOne("SELECT * FROM dice_history WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    /** Vincula uma rolagem genérica a um teste específico uma única vez. Uma
     * rolagem já vinculada a outra ação, cena, personagem ou regra não pode ser
     * reaproveitada. */
    public void bindDiceHistoryChecked(long id,JSONObject context)throws Exception{
        JSONObject row=getDiceHistoryById(id);
        if(row.length()==0)throw new IllegalStateException("Rolagem local não encontrada: "+id);
        if(row.optLong("consumed_at",0)>0)throw new IllegalStateException("A rolagem já foi consumida.");
        JSONObject safe=context==null?new JSONObject():context;
        String testKey=safe.optString("test_key","").trim(),action=safe.optString("action_signature","").trim(),scene=safe.optString("scene_fingerprint","").trim(),source=safe.optString("source_rule_id","").trim(),procedure=safe.optString("procedure_id","").trim(),campaign=safe.optString("campaign_fingerprint","").trim(),target=safe.optString("target_signature","").trim(),consequence=safe.optString("consequence_key","").trim(),modifierSource=safe.optString("modifier_source","").trim(),rollMode=safe.optString("roll_mode","").trim();
        if(testKey.length()==0||action.length()==0||scene.length()==0||source.length()==0||procedure.length()==0||campaign.length()==0||target.length()==0||consequence.length()==0||modifierSource.length()==0||rollMode.length()==0||!safe.has("natural_roll"))throw new IllegalArgumentException("O vínculo mecânico absoluto está incompleto.");
        String formula=safe.optString("formula",safe.optString("roll_formula","")).replaceAll("\\s+","");
        if(formula.length()>0&&!formula.equalsIgnoreCase(row.optString("formula","").replaceAll("\\s+","")))throw new IllegalStateException("A fórmula selecionada não corresponde à rolagem.");
        if(safe.has("total")&&safe.optInt("total",Integer.MIN_VALUE)!=row.optInt("total",Integer.MAX_VALUE))throw new IllegalStateException("O total selecionado não corresponde à rolagem.");
        for(String key:new String[]{"test_key","action_signature","scene_fingerprint","source_rule_id","procedure_id","campaign_fingerprint","target_signature","consequence_key"}){String existing=row.optString(key,"").trim(),wanted=safe.optString(key,"").trim();if(existing.length()>0&&!existing.equals(wanted))throw new IllegalStateException("A rolagem já pertence a outro contexto mecânico.");}
        long existingActor=row.optLong("actor_character_id",0),wantedActor=safe.optLong("actor_character_id",0);
        if(existingActor>0&&wantedActor>0&&existingActor!=wantedActor)throw new IllegalStateException("A rolagem pertence a outro personagem.");
        ContentValues v=new ContentValues();v.put("test_key",testKey);v.put("action_signature",action);v.put("scene_fingerprint",scene);v.put("source_rule_id",source);v.put("procedure_id",procedure);v.put("campaign_fingerprint",campaign);v.put("target_signature",target);v.put("consequence_key",consequence);v.put("modifier",safe.optInt("modifier",0));v.put("modifier_source",modifierSource);v.put("roll_mode",rollMode);v.put("natural_roll",safe.optInt("natural_roll",0));v.put("critical",safe.optBoolean("critical",safe.optInt("natural_roll",0)==20)?1:0);v.put("critical_failure",safe.optBoolean("critical_failure",safe.optInt("natural_roll",0)==1)?1:0);
        if(wantedActor>0)v.put("actor_character_id",wantedActor);String actorName=safe.optString("actor_name","").trim();if(actorName.length()>0)v.put("actor_name",actorName);String outcome=safe.optString("outcome","").trim();if(outcome.length()>0)v.put("outcome",outcome);
        int changed=getWritableDatabase().update("dice_history",v,"id=? AND world_profile=? AND consumed_at=0",new String[]{String.valueOf(id),getActiveMapProfile()});
        if(changed!=1)throw new IllegalStateException("Não foi possível vincular a rolagem.");
    }
    public void consumeDiceHistoryChecked(long id,String expectedTestKey)throws Exception{
        JSONObject row=getDiceHistoryById(id);consumeDiceHistoryChecked(id,expectedTestKey,row.optString("consequence_key"));
    }
    public void consumeDiceHistoryChecked(long id,String expectedTestKey,String expectedConsequenceKey)throws Exception{
        JSONObject row=getDiceHistoryById(id);
        if(row.length()==0)throw new IllegalStateException("Rolagem vinculada não encontrada: "+id);
        if(row.optLong("consumed_at",0)>0)throw new IllegalStateException("Rolagem já consumida: "+id);
        String expected=expectedTestKey==null?"":expectedTestKey.trim();String actual=row.optString("test_key","").trim();
        if(expected.length()==0||!expected.equals(actual))throw new IllegalStateException("A rolagem não pertence ao teste atual.");
        String consequence=expectedConsequenceKey==null?"":expectedConsequenceKey.trim();if(consequence.length()==0||!consequence.equals(row.optString("consequence_key","").trim()))throw new IllegalStateException("A rolagem não pertence à consequência atual.");
        ContentValues v=new ContentValues();v.put("consumed_at",System.currentTimeMillis());
        int changed=getWritableDatabase().update("dice_history",v,"id=? AND world_profile=? AND consumed_at=0",new String[]{String.valueOf(id),getActiveMapProfile()});
        if(changed!=1)throw new IllegalStateException("Não foi possível consumir a rolagem vinculada.");
    }
    public List<JSONObject> listDiceHistory(){return queryList("SELECT * FROM dice_history WHERE world_profile=? ORDER BY created_at DESC LIMIT 200",new String[]{getActiveMapProfile()});}

    public void upsertManual(String driveId,String title,String category,String profile,boolean enabled,String mime,long size,String modified,String localUri,String source){
        ContentValues v=new ContentValues();v.put("drive_id",driveId);v.put("title",title);v.put("category",category);v.put("rule_profile",profile);v.put("enabled",enabled?1:0);v.put("mime_type",mime);v.put("size_bytes",size);v.put("modified_time",modified);v.put("local_uri",localUri);v.put("source",source);v.put("created_at",System.currentTimeMillis());
        long r=getWritableDatabase().insertWithOnConflict("manuals",null,v,SQLiteDatabase.CONFLICT_IGNORE); if(r==-1)getWritableDatabase().update("manuals",v,"drive_id=?",new String[]{driveId});
    }
    /** Retorna 1 para novo, 2 para alterado e 0 para inalterado. */
    public int upsertManualSynced(String driveId,String title,String category,String profile,String mime,long size,String modified,String localUri,String source,String sourceRoot,long seen){
        JSONObject before=queryOne("SELECT id,size_bytes,modified_time,local_uri FROM manuals WHERE drive_id=?",new String[]{driveId});
        boolean exists=before.length()>0;
        boolean changed=exists&&(before.optLong("size_bytes")!=size||!safe(before.optString("modified_time")).equals(safe(modified))||!safe(before.optString("local_uri")).equals(safe(localUri)));
        ContentValues v=new ContentValues();v.put("drive_id",driveId);v.put("title",title);v.put("category",category);v.put("rule_profile",profile);v.put("enabled",1);v.put("mime_type",mime);v.put("size_bytes",size);v.put("modified_time",modified);v.put("local_uri",localUri);v.put("source",source);v.put("source_root",sourceRoot==null?"":sourceRoot);v.put("last_seen",seen);v.put("availability","available");v.put("last_verified_at",System.currentTimeMillis());
        if(!exists)v.put("created_at",System.currentTimeMillis());
        if(changed){v.put("indexed",0);v.put("ai_selected",0);v.put("rag_status","not_indexed");v.put("rag_error","");v.put("rag_progress","");v.put("rag_modified_time","");v.put("rag_source_hash","");v.put("rag_part_count",0);v.put("rag_parts_ready",0);v.put("rag_page_count",0);}
        SQLiteDatabase sql=getWritableDatabase();long r=sql.insertWithOnConflict("manuals",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(r==-1)sql.update("manuals",v,"drive_id=?",new String[]{driveId});
        if(changed&&before.optLong("id",-1)>0){clearManualRagParts(before.optLong("id"));clearManualLocalIndex(before.optLong("id"));}
        return !exists?1:(changed?2:0);
    }
    public int disableMissingManuals(String sourceRoot,long seen){ContentValues v=new ContentValues();v.put("enabled",0);v.put("availability","missing");v.put("last_verified_at",System.currentTimeMillis());return getWritableDatabase().update("manuals",v,"source_root=? AND last_seen<>?",new String[]{sourceRoot==null?"":sourceRoot,String.valueOf(seen)});}
    /** Na primeira sincronização nova também absorve entradas antigas, que ainda
     * não possuíam source_root, sem atingir PDFs avulsos de outra origem. */
    public int disableMissingManuals(String sourceRoot,long seen,String legacySourcePrefix){
        ContentValues v=new ContentValues();v.put("enabled",0);v.put("availability","missing");v.put("last_verified_at",System.currentTimeMillis());
        String root=sourceRoot==null?"":sourceRoot,prefix=legacySourcePrefix==null?"":legacySourcePrefix;
        return getWritableDatabase().update("manuals",v,"(source_root=? OR (COALESCE(source_root,'')='' AND source LIKE ?)) AND last_seen<>?",new String[]{root,prefix+"%",String.valueOf(seen)});
    }
    public List<JSONObject> listManuals(String filter){
        if(filter==null||filter.trim().length()==0)return queryList("SELECT * FROM manuals ORDER BY category,title COLLATE NOCASE",null);
        return queryList("SELECT * FROM manuals WHERE title LIKE ? OR category LIKE ? ORDER BY category,title COLLATE NOCASE",new String[]{"%"+filter+"%","%"+filter+"%"});
    }
    public void setManualEnabled(long id,boolean enabled){ContentValues v=new ContentValues();v.put("enabled",enabled?1:0);getWritableDatabase().update("manuals",v,"id=?",new String[]{String.valueOf(id)});}
    public void setManualGemini(long id,String uri,String name,long expires){ContentValues v=new ContentValues();v.put("gemini_uri",uri);v.put("gemini_name",name);v.put("gemini_expires",expires);v.put("indexed",uri!=null&&uri.length()>0?1:0);v.put("ai_selected",uri!=null&&uri.length()>0?1:0);getWritableDatabase().update("manuals",v,"id=?",new String[]{String.valueOf(id)});}
    public void setManualAiSelected(long id,boolean selected){ContentValues v=new ContentValues();v.put("ai_selected",selected?1:0);getWritableDatabase().update("manuals",v,"id=?",new String[]{String.valueOf(id)});}
    public List<JSONObject> listAiManuals(){return queryList("SELECT * FROM manuals WHERE enabled=1 AND ai_selected=1 AND gemini_uri IS NOT NULL AND gemini_uri<>'' AND gemini_expires>? ORDER BY category,title LIMIT 5",new String[]{String.valueOf(System.currentTimeMillis())});}
    public List<JSONObject> listEnabledManuals(){return queryList("SELECT * FROM manuals WHERE enabled=1 ORDER BY category,title COLLATE NOCASE",null);}
    /** Remove somente entradas de manuais originadas da subpasta reservada tokens. */
    public int purgeManualsFromReservedTokenFolders(){
        SQLiteDatabase sql=getWritableDatabase();
        String where="lower(replace(replace(source,'\\','/'),'_',' ')) LIKE ? OR lower(replace(replace(source,'\\','/'),'_',' ')) LIKE ?";
        String[] args={"%/tokens/%","%• tokens/%"};
        sql.beginTransaction();
        try{
            sql.execSQL("DELETE FROM manual_local_pages WHERE manual_id IN (SELECT id FROM manuals WHERE "+where+")",args);
            sql.execSQL("DELETE FROM manual_rag_parts WHERE manual_id IN (SELECT id FROM manuals WHERE "+where+")",args);
            int removed=sql.delete("manuals",where,args);
            sql.setTransactionSuccessful();
            return removed;
        }finally{sql.endTransaction();}
    }
    /** Apaga a biblioteca de manuais inteira e todos os índices derivados. */
    public int deleteAllManuals(){
        SQLiteDatabase sql=getWritableDatabase();sql.beginTransaction();
        try{
            sql.delete("manual_local_pages",null,null);
            sql.delete("manual_rag_parts",null,null);
            int removed=sql.delete("manuals",null,null);
            sql.setTransactionSuccessful();
            return removed;
        }finally{sql.endTransaction();}
    }

    public List<JSONObject> listPendingRagManuals(String storeName){return queryList("SELECT * FROM manuals WHERE enabled=1 AND (rag_status<>'ready' OR rag_store_name<>? OR rag_modified_time<>COALESCE(modified_time,'')) ORDER BY CASE source WHEN 'Incluído no app; tabelas locais com páginas verificáveis; interpretação em PT-BR' THEN 0 ELSE 1 END, category,title COLLATE NOCASE",new String[]{storeName==null?"":storeName});}
    public List<JSONObject> listPendingRagManualsForWorld(String storeName,boolean ignoredLegacyProfile){
        String worldClause=manualWorldClause(ignoredLegacyProfile,"");
        return queryList("SELECT * FROM manuals WHERE enabled=1 AND "+worldClause+" AND (rag_status<>'ready' OR rag_store_name<>? OR rag_modified_time<>COALESCE(modified_time,'')) ORDER BY CASE source WHEN 'Incluído no app; tabelas locais com páginas verificáveis; interpretação em PT-BR' THEN 0 ELSE 1 END, category,title COLLATE NOCASE",new String[]{storeName==null?"":storeName});
    }
    public void setManualRagStatus(long id,String status,String document,String error,String storeName,String modifiedTime){ContentValues v=new ContentValues();v.put("rag_status",status==null?"not_indexed":status);if(document!=null)v.put("rag_document",document);v.put("rag_error",error==null?"":error);v.put("rag_store_name",storeName==null?"":storeName);v.put("rag_modified_time",modifiedTime==null?"":modifiedTime);if("ready".equals(status))v.put("rag_indexed_at",System.currentTimeMillis());getWritableDatabase().update("manuals",v,"id=?",new String[]{String.valueOf(id)});}
    public void setManualRagProgress(long id,String status,String progress,String error,String storeName,String modifiedTime,String sourceHash,int partCount,int readyParts,int pageCount,String splitMode){ContentValues v=new ContentValues();v.put("rag_status",status==null?"not_indexed":status);v.put("rag_progress",progress==null?"":progress);v.put("rag_error",error==null?"":error);v.put("rag_store_name",storeName==null?"":storeName);v.put("rag_modified_time",modifiedTime==null?"":modifiedTime);v.put("rag_source_hash",sourceHash==null?"":sourceHash);v.put("rag_part_count",Math.max(0,partCount));v.put("rag_parts_ready",Math.max(0,readyParts));v.put("rag_page_count",Math.max(0,pageCount));v.put("rag_split_mode",splitMode==null?"":splitMode);if("ready".equals(status))v.put("rag_indexed_at",System.currentTimeMillis());getWritableDatabase().update("manuals",v,"id=?",new String[]{String.valueOf(id)});}
    public void setManualRagDocument(long id,String documentJson){ContentValues v=new ContentValues();v.put("rag_document",documentJson==null?"":documentJson);getWritableDatabase().update("manuals",v,"id=?",new String[]{String.valueOf(id)});}
    public void clearManualRagParts(long manualId){getWritableDatabase().delete("manual_rag_parts","manual_id=?",new String[]{String.valueOf(manualId)});}
    public void clearManualRagPartsExceptHash(long manualId,String sourceHash){getWritableDatabase().delete("manual_rag_parts","manual_id=? AND source_hash<>?",new String[]{String.valueOf(manualId),sourceHash==null?"":sourceHash});}
    public void upsertManualRagPart(long manualId,String sourceHash,int partNo,int totalParts,int startPage,int endPage,int pageCount,long sizeBytes,String splitMode,String localPath,String document,String status,String error,String storeName,String modifiedTime){ContentValues v=new ContentValues();v.put("manual_id",manualId);v.put("source_hash",sourceHash==null?"":sourceHash);v.put("part_no",partNo);v.put("total_parts",totalParts);v.put("start_page",startPage);v.put("end_page",endPage);v.put("page_count",pageCount);v.put("size_bytes",sizeBytes);v.put("split_mode",splitMode==null?"":splitMode);v.put("local_path",localPath==null?"":localPath);v.put("rag_document",document==null?"":document);v.put("rag_status",status==null?"prepared":status);v.put("rag_error",error==null?"":error);v.put("rag_store_name",storeName==null?"":storeName);v.put("rag_modified_time",modifiedTime==null?"":modifiedTime);if("ready".equals(status))v.put("indexed_at",System.currentTimeMillis());SQLiteDatabase db=getWritableDatabase();long result=db.insertWithOnConflict("manual_rag_parts",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(result==-1)db.update("manual_rag_parts",v,"manual_id=? AND source_hash=? AND part_no=?",new String[]{String.valueOf(manualId),sourceHash==null?"":sourceHash,String.valueOf(partNo)});}
    public void updateManualRagPart(long manualId,String sourceHash,int partNo,String document,String status,String error,String storeName,String localPath){ContentValues v=new ContentValues();if(document!=null)v.put("rag_document",document);v.put("rag_status",status==null?"prepared":status);v.put("rag_error",error==null?"":error);v.put("rag_store_name",storeName==null?"":storeName);if(localPath!=null)v.put("local_path",localPath);if("ready".equals(status))v.put("indexed_at",System.currentTimeMillis());getWritableDatabase().update("manual_rag_parts",v,"manual_id=? AND source_hash=? AND part_no=?",new String[]{String.valueOf(manualId),sourceHash==null?"":sourceHash,String.valueOf(partNo)});}
    public List<JSONObject> listManualRagParts(long manualId,String sourceHash){return queryList("SELECT * FROM manual_rag_parts WHERE manual_id=? AND source_hash=? ORDER BY part_no",new String[]{String.valueOf(manualId),sourceHash==null?"":sourceHash});}
    public List<JSONObject> listAllManualRagParts(long manualId){return queryList("SELECT * FROM manual_rag_parts WHERE manual_id=? ORDER BY source_hash,part_no",new String[]{String.valueOf(manualId)});}
    public int countReadyManualRagParts(long manualId,String sourceHash,String storeName){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manual_rag_parts WHERE manual_id=? AND source_hash=? AND rag_status='ready' AND rag_store_name=?",new String[]{String.valueOf(manualId),sourceHash==null?"":sourceHash,storeName==null?"":storeName});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public JSONArray manualRagDocuments(long manualId){JSONArray out=new JSONArray();for(JSONObject p:listAllManualRagParts(manualId)){String doc=p.optString("rag_document");if(doc.contains("/documents/"))out.put(doc);}return out;}
    public void resetRagLibrary(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("rag_document","");v.put("rag_status","not_indexed");v.put("rag_indexed_at",0);v.put("rag_error","");v.put("rag_modified_time","");v.put("rag_store_name","");v.put("rag_part_count",0);v.put("rag_parts_ready",0);v.put("rag_page_count",0);v.put("rag_source_hash","");v.put("rag_progress","");v.put("rag_split_mode","");db.update("manuals",v,null,null);db.delete("manual_rag_parts",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public void resetRagLibraryForWorld(boolean ignoredLegacyProfile){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{String where=manualWorldClause(ignoredLegacyProfile,"");db.execSQL("DELETE FROM manual_rag_parts WHERE manual_id IN (SELECT id FROM manuals WHERE "+where+")");ContentValues v=new ContentValues();v.put("rag_document","");v.put("rag_status","not_indexed");v.put("rag_indexed_at",0);v.put("rag_error","");v.put("rag_modified_time","");v.put("rag_store_name","");v.put("rag_part_count",0);v.put("rag_parts_ready",0);v.put("rag_page_count",0);v.put("rag_source_hash","");v.put("rag_progress","");v.put("rag_split_mode","");db.update("manuals",v,where,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public int countRagReadyForWorld(String storeName,boolean ignoredLegacyProfile){String worldClause=manualWorldClause(ignoredLegacyProfile,"");Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manuals WHERE enabled=1 AND "+worldClause+" AND rag_status='ready' AND rag_store_name=?",new String[]{storeName==null?"":storeName});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}

    // Índice local dos manuais. O Gemini nunca é necessário para preparar os PDFs.
    public void clearManualLocalIndex(long manualId){getWritableDatabase().delete("manual_local_pages","manual_id=?",new String[]{String.valueOf(manualId)});}
    public void clearManualLocalIndexExcept(long manualId,String sourceKey){getWritableDatabase().delete("manual_local_pages","manual_id=? AND source_key<>?",new String[]{String.valueOf(manualId),sourceKey==null?"":sourceKey});}
    public void upsertManualLocalPage(long manualId,String sourceKey,int pageNo,String title,String content,String normalized){upsertManualLocalPage(manualId,sourceKey,pageNo,title,content,normalized,new JSONObject());}
    public void upsertManualLocalPage(long manualId,String sourceKey,int pageNo,String title,String content,String normalized,JSONObject structure){ContentValues v=new ContentValues();JSONObject s=structure==null?new JSONObject():structure;v.put("manual_id",manualId);v.put("source_key",sourceKey==null?"":sourceKey);v.put("page_no",Math.max(1,pageNo));v.put("title",title==null?"":title);v.put("content",content==null?"":content);v.put("normalized",normalized==null?"":normalized);v.put("section_title",s.optString("section_title"));v.put("chapter_title",s.optString("chapter_title"));v.put("content_kind",s.optString("content_kind","prose"));v.put("structure_json",s.toString());v.put("cross_refs_json",String.valueOf(s.optJSONArray("cross_references") instanceof JSONArray?s.optJSONArray("cross_references"):new JSONArray()));v.put("page_hash",s.optString("page_hash"));v.put("indexed_at",System.currentTimeMillis());SQLiteDatabase sql=getWritableDatabase();long r=sql.insertWithOnConflict("manual_local_pages",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(r==-1)sql.update("manual_local_pages",v,"manual_id=? AND source_key=? AND page_no=?",new String[]{String.valueOf(manualId),sourceKey==null?"":sourceKey,String.valueOf(Math.max(1,pageNo))});}
    public int countManualLocalPages(long manualId,String sourceKey){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manual_local_pages WHERE manual_id=? AND source_key=?",new String[]{String.valueOf(manualId),sourceKey==null?"":sourceKey});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public int countManualLocalTextPages(long manualId,String sourceKey){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manual_local_pages WHERE manual_id=? AND source_key=? AND LENGTH(TRIM(content))>0",new String[]{String.valueOf(manualId),sourceKey==null?"":sourceKey});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public boolean hasManualLocalTextPage(long manualId,String sourceKey,int pageNo){Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM manual_local_pages WHERE manual_id=? AND source_key=? AND page_no=? AND LENGTH(TRIM(content))>0 LIMIT 1",new String[]{String.valueOf(manualId),sourceKey==null?"":sourceKey,String.valueOf(pageNo)});try{return c.moveToFirst();}finally{c.close();}}
    public int maxManualLocalPage(long manualId,String sourceKey){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(page_no),0) FROM manual_local_pages WHERE manual_id=? AND source_key=?",new String[]{String.valueOf(manualId),sourceKey==null?"":sourceKey});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    /** Um manual só é mecanicamente utilizável quando todas as páginas foram
     * verificadas pelo indexador com numeração real. A presença de uma única
     * linha parcial nunca mais conta como índice pronto. */
    public int countLocalReadyForWorld(boolean ignoredLegacyProfile){
        String worldClause=manualWorldClause(ignoredLegacyProfile,"m");
        String sql="SELECT COUNT(*) FROM manuals m WHERE m.enabled=1 AND "+worldClause+
                " AND m.rag_status='ready' AND m.rag_store_name=? AND m.rag_page_count>0"+
                " AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%'"+
                " AND COALESCE(m.rag_document,'') LIKE 'local:%' AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'')"+
                " AND (SELECT COUNT(*) FROM manual_local_pages p WHERE p.manual_id=m.id AND p.source_key=SUBSTR(m.rag_document,7))=m.rag_page_count"+
                " AND (SELECT COUNT(*) FROM manual_local_pages p2 WHERE p2.manual_id=m.id AND p2.source_key=SUBSTR(m.rag_document,7) AND LENGTH(TRIM(p2.content))>0)>0";
        Cursor c=getReadableDatabase().rawQuery(sql,new String[]{LOCAL_MANUAL_STORE});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }

    public int countEnabledManualsForWorld(boolean ignoredLegacyProfile){
        String worldClause=manualWorldClause(ignoredLegacyProfile,"m");Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manuals m WHERE m.enabled=1 AND "+worldClause,null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }

    /** Contagem leve para UI e decisão rápida. A auditoria profunda fica em countLocalReadyForWorld(). */
    public int countLocalReadyStatusForWorld(boolean ignoredLegacyProfile){
        String worldClause=manualWorldClause(ignoredLegacyProfile,"m");String sql="SELECT COUNT(*) FROM manuals m WHERE m.enabled=1 AND "+worldClause+" AND m.rag_status='ready' AND m.rag_store_name=? AND m.rag_page_count>0 AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND COALESCE(m.rag_document,'') LIKE 'local:%' AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'')";Cursor c=getReadableDatabase().rawQuery(sql,new String[]{LOCAL_MANUAL_STORE});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }
    public boolean hasAnyLocalReadyStatusForWorld(boolean ignoredLegacyProfile){
        String worldClause=manualWorldClause(ignoredLegacyProfile,"m");String sql="SELECT 1 FROM manuals m WHERE m.enabled=1 AND "+worldClause+" AND m.rag_status='ready' AND m.rag_store_name=? AND m.rag_page_count>0 AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND COALESCE(m.rag_document,'') LIKE 'local:%' AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'') LIMIT 1";Cursor c=getReadableDatabase().rawQuery(sql,new String[]{LOCAL_MANUAL_STORE});try{return c.moveToFirst();}finally{c.close();}
    }

    /** Diagnóstico detalhado usado para bloquear regras quando um único manual
     * habilitado estiver parcial, desatualizado ou sem páginas confiáveis. */
    public JSONObject manualIndexHealth(boolean ignoredLegacyProfile){
        JSONObject out=new JSONObject();JSONArray manuals=new JSONArray(),missing=new JSONArray();int enabled=0,ready=0;
        try{
            String worldClause=manualWorldClause(ignoredLegacyProfile,"m");
            List<JSONObject> rows=queryList("SELECT m.id,m.title,m.rag_status,m.rag_store_name,m.rag_page_count,m.rag_split_mode,m.rag_document,m.rag_error,m.rag_progress,m.modified_time,m.rag_modified_time,"+
                    "(SELECT COUNT(*) FROM manual_local_pages p WHERE p.manual_id=m.id AND p.source_key=CASE WHEN m.rag_document LIKE 'local:%' THEN SUBSTR(m.rag_document,7) ELSE '' END) AS actual_pages,"+
                    "(SELECT COUNT(*) FROM manual_local_pages p2 WHERE p2.manual_id=m.id AND p2.source_key=CASE WHEN m.rag_document LIKE 'local:%' THEN SUBSTR(m.rag_document,7) ELSE '' END AND LENGTH(TRIM(p2.content))>0) AS text_pages "+
                    "FROM manuals m WHERE m.enabled=1 AND "+worldClause+" ORDER BY m.title COLLATE NOCASE",null);
            enabled=rows.size();
            for(JSONObject row:rows){
                boolean reliable="ready".equals(row.optString("rag_status"))&&LOCAL_MANUAL_STORE.equals(row.optString("rag_store_name"))&&row.optInt("rag_page_count",0)>0&&row.optInt("actual_pages",0)==row.optInt("rag_page_count",0)&&row.optInt("text_pages",0)>0&&!row.optString("rag_split_mode").toLowerCase(java.util.Locale.ROOT).startsWith("fallback")&&row.optString("rag_document").startsWith("local:")&&row.optString("rag_modified_time").equals(row.optString("modified_time"));
                String reason="";
                if(!reliable){
                    if(!"ready".equals(row.optString("rag_status")))reason="status="+row.optString("rag_status","not_indexed");
                    else if(!LOCAL_MANUAL_STORE.equals(row.optString("rag_store_name")))reason="índice de versão antiga";
                    else if(row.optString("rag_split_mode").toLowerCase(java.util.Locale.ROOT).startsWith("fallback"))reason="trechos sem página verificável";
                    else if(row.optInt("rag_page_count",0)<=0)reason="total de páginas desconhecido";
                    else if(row.optInt("actual_pages",0)!=row.optInt("rag_page_count",0))reason="páginas "+row.optInt("actual_pages",0)+"/"+row.optInt("rag_page_count",0);
                    else if(row.optInt("text_pages",0)<=0)reason="nenhuma página com texto";
                    else if(!row.optString("rag_modified_time").equals(row.optString("modified_time")))reason="manual alterado após a indexação";
                    else reason="índice local inconsistente";
                    row.put("reason",reason);missing.put(new JSONObject(row.toString()));
                }else ready++;
                row.put("reliable",reliable);manuals.put(row);
            }
            out.put("enabled",enabled).put("ready",ready).put("complete",enabled>0&&ready==enabled).put("manuals",manuals).put("missing",missing);
        }catch(Exception e){try{out.put("enabled",enabled).put("ready",ready).put("complete",false).put("error",String.valueOf(e.getMessage())).put("manuals",manuals).put("missing",missing);}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao registrar saúde do índice.",ignored);}}
        return out;
    }

    /**
     * Resumo rápido usado durante uma cena. A auditoria completa continua em
     * manualIndexHealth(), mas não deve recontar página por página a cada fala
     * do Mestre. O estado ready só é gravado ao final atômico da indexação e a
     * própria consulta abaixo exige páginas locais verificáveis.
     */
    public JSONObject manualIndexHealthFast(boolean ignoredLegacyProfile){
        JSONObject out=new JSONObject();int enabled=0,ready=0;
        try{
            String worldClause=manualWorldClause(ignoredLegacyProfile,"m");
            JSONObject row=queryOne("SELECT COUNT(*) AS enabled,COALESCE(SUM(CASE WHEN m.rag_status='ready' AND m.rag_store_name=? AND m.rag_page_count>0 AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND m.rag_document LIKE 'local:%' AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'') THEN 1 ELSE 0 END),0) AS ready FROM manuals m WHERE m.enabled=1 AND "+worldClause,new String[]{LOCAL_MANUAL_STORE});
            enabled=row.optInt("enabled",0);ready=row.optInt("ready",0);
            out.put("enabled",enabled).put("ready",ready).put("complete",enabled>0&&ready==enabled).put("fast_summary",true).put("search_index_ready",isManualSearchIndexReady()).put("manuals",new JSONArray()).put("missing",new JSONArray());
        }catch(Exception e){try{out.put("enabled",enabled).put("ready",ready).put("complete",false).put("fast_summary",true).put("error",String.valueOf(e.getMessage())).put("manuals",new JSONArray()).put("missing",new JSONArray());}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao registrar resumo rápido do índice.",ignored);}}
        return out;
    }

    public List<JSONObject> searchManualLocalPages(String query,boolean ignoredLegacyProfile,int limit){
        return searchManualLocalPagesInternal(query,ignoredLegacyProfile,limit,36,12,1800);
    }

    /** Uma única varredura compacta para a abertura da aventura. */
    public List<JSONObject> searchManualLocalPagesFast(String query,boolean ignoredLegacyProfile,int limit){
        return searchManualLocalPagesInternal(query,ignoredLegacyProfile,limit,20,8,320);
    }

    private List<JSONObject> searchManualLocalPagesInternal(String query,boolean ignoredLegacyProfile,int limit,int maxTerms,int maxPhrases,int candidateLimit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();String q=normalizeSearchText(query);
        java.util.HashSet<String> stop=new java.util.HashSet<String>(java.util.Arrays.asList(
                "que","com","para","uma","uns","das","dos","de","do","da","em","no","na","nos","nas","por","pela","pelo",
                "como","quando","onde","qual","quais","isso","esta","este","essa","esse","meu","minha","seu","sua","vou",
                "tento","quero","pode","posso","fazer","agora","entao","depois","antes","mestre","jogador","personagem",
                "the","and","for","with","from","into","your","this","that","when","where","what","which","can","may"));
        ArrayList<String> terms=new ArrayList<String>();
        for(String t:q.split("\\s+")){
            if(t.length()<3||stop.contains(t)||terms.contains(t))continue;
            terms.add(t);
            if(t.length()>=7){
                String stem=t.substring(0,Math.max(4,t.length()-2));
                if(!terms.contains(stem))terms.add(stem);
            }
            if(terms.size()>=Math.max(6,maxTerms/2))break;
        }
        expandSemanticTerms(terms);
        while(terms.size()>Math.max(6,maxTerms))terms.remove(terms.size()-1);
        if(terms.isEmpty())return out;
        ArrayList<String> phrases=new ArrayList<String>();
        String[] words=q.split("\\s+");
        for(int size=4;size>=2;size--)for(int i=0;i+size<=words.length&&phrases.size()<Math.max(2,maxPhrases);i++){
            StringBuilder phrase=new StringBuilder();int meaningful=0;
            for(int j=0;j<size;j++){String w=words[i+j];if(phrase.length()>0)phrase.append(' ');phrase.append(w);if(w.length()>=3&&!stop.contains(w))meaningful++;}
            if(meaningful>=2&&!phrases.contains(phrase.toString()))phrases.add(phrase.toString());
        }

        String worldClause=manualWorldClause(ignoredLegacyProfile,"m");int safeCandidateLimit=Math.max(80,Math.min(1800,candidateLimit));
        java.util.LinkedHashSet<String> ftsTokens=new java.util.LinkedHashSet<String>();
        for(String term:terms)for(String token:term.split("\\s+")){String safeToken=token.replaceAll("[^a-z0-9_]","");if(safeToken.length()>=3)ftsTokens.add(safeToken);}
        StringBuilder ftsExpression=new StringBuilder();for(String token:ftsTokens){if(ftsExpression.length()>0)ftsExpression.append(" OR ");ftsExpression.append(token).append('*');}
        String select="SELECT p.manual_id,p.source_key,p.page_no,p.title,p.content,p.normalized,p.section_title,p.chapter_title,p.content_kind,p.structure_json,p.cross_refs_json,p.page_hash,m.category,m.rule_profile,m.source,m.title AS manual_title,m.rag_status,m.rag_store_name,m.rag_page_count,m.rag_split_mode,m.rag_document ";
        List<JSONObject> candidates=null;
        if(isManualSearchIndexReady())try{
            String ftsSql=select+"FROM manual_local_pages_fts JOIN manual_local_pages p ON p.id=manual_local_pages_fts.docid JOIN manuals m ON m.id=p.manual_id WHERE manual_local_pages_fts MATCH ? AND m.enabled=1 AND "+worldClause+" AND m.rag_status='ready' AND m.rag_store_name=? AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'') AND m.rag_page_count>0 AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND m.rag_document='local:'||p.source_key AND LENGTH(TRIM(p.content))>0 ORDER BY LENGTH(offsets(manual_local_pages_fts)) DESC,p.manual_id,p.page_no LIMIT "+safeCandidateLimit;
            candidates=queryList(ftsSql,new String[]{ftsExpression.toString(),LOCAL_MANUAL_STORE});
        }catch(RuntimeException ftsFailure){AppLog.w("AppDb","Índice FTS dos manuais indisponível; usando núcleo local de compatibilidade.",ftsFailure);}
        if(candidates==null){
            // Durante a única reconstrução de migração, a abertura não espera
            // por todos os livros. A busca temporária fica restrita aos manuais
            // centrais de regras e usa o índice por manual_id, evitando uma
            // varredura integral da biblioteca.
            StringBuilder where=new StringBuilder(),sqlRank=new StringBuilder();ArrayList<String> args=new ArrayList<String>();args.add(LOCAL_MANUAL_STORE);
            for(String t:terms){if(where.length()>0)where.append(" OR ");where.append("p.normalized LIKE ?");args.add("%"+t+"%");}
            for(String t:terms){if(sqlRank.length()>0)sqlRank.append(" + ");sqlRank.append("CASE WHEN p.normalized LIKE ? THEN 1 ELSE 0 END");args.add("%"+t+"%");}
            String coreWorldClause=manualWorldClause(ignoredLegacyProfile,"cm");
            String coreManuals="p.manual_id IN (SELECT cm.id FROM manuals cm WHERE cm.enabled=1 AND "+coreWorldClause+" AND cm.rag_status='ready' AND cm.rag_store_name='"+LOCAL_MANUAL_STORE+"' ORDER BY CASE WHEN LOWER(cm.title) LIKE '%player core%' OR LOWER(cm.title) LIKE '%jogador%' OR LOWER(cm.title) LIKE '%pathfinder%' THEN 0 WHEN LOWER(cm.title) LIKE '%gm core%' OR LOWER(cm.title) LIKE '%monster core%' OR LOWER(cm.title) LIKE '%mestre%' OR LOWER(cm.category) LIKE '%regra%' THEN 1 WHEN LOWER(cm.title) LIKE '%lost omens%' OR LOWER(cm.category) LIKE '%lore%' OR LOWER(cm.category) LIKE '%cenario%' THEN 2 ELSE 3 END,cm.id LIMIT 12)";
            String coreSql=select+"FROM manual_local_pages p JOIN manuals m ON m.id=p.manual_id WHERE "+coreManuals+" AND m.enabled=1 AND "+worldClause+" AND m.rag_status='ready' AND m.rag_store_name=? AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'') AND m.rag_page_count>0 AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND m.rag_document='local:'||p.source_key AND LENGTH(TRIM(p.content))>0 AND ("+where+") ORDER BY ("+sqlRank+") DESC,p.manual_id,p.page_no LIMIT "+safeCandidateLimit;
            candidates=queryList(coreSql,args.toArray(new String[args.size()]));
        }
        String activeRules=normalizeSearchText(getCampaign().optString("rules_profile","Pathfinder 2e Remaster"));
        for(JSONObject row:candidates){
            String norm=row.optString("normalized").trim();if(norm.length()==0)norm=normalizeSearchText(row.optString("content"));
            int score=0;if(q.length()>0&&norm.contains(q))score+=140;
            int matched=0,totalOccurrences=0;
            for(String t:terms){
                int at=-1,count=0;while((at=norm.indexOf(t,at+1))>=0&&count<12)count++;
                if(count>0)matched++;totalOccurrences+=count;score+=Math.min(36,count*5);
                if(normalizeSearchText(row.optString("manual_title")).contains(t))score+=16;
                if(normalizeSearchText(row.optString("category")).contains(t))score+=8;
            }
            for(String phrase:phrases)if(norm.contains(phrase))score+=24+phrase.split("\\s+").length*7;
            double coverage=matched/(double)Math.max(1,terms.size());
            score+=(int)Math.round(coverage*90d)+matched*matched*2+Math.min(45,totalOccurrences*2);
            String rowProfile=normalizeSearchText(row.optString("rule_profile"));
            if(activeRules.length()>0&&(rowProfile.contains(activeRules)||activeRules.contains(rowProfile)))score+=38;
            if(rowProfile.contains("remaster")||rowProfile.contains("pf2")||rowProfile.contains("pathfinder 2"))score+=24;
            if(score<18)continue;
            try{row.put("_score",score).put("_coverage",coverage).put("_matched_terms",matched).put("_retrieval_modes",new JSONArray().put("lexical").put("radical").put("synonym").put("semantic_concept")).put("_authority","PF2E_REMATER_GROUNDED");}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao classificar trecho local.",ignored);}
            out.add(row);
        }
        java.util.Collections.sort(out,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){int score=Integer.compare(b.optInt("_score"),a.optInt("_score"));if(score!=0)return score;int coverage=Double.compare(b.optDouble("_coverage"),a.optDouble("_coverage"));if(coverage!=0)return coverage;return Integer.compare(a.optInt("page_no"),b.optInt("page_no"));}});
        int max=Math.max(1,Math.min(24,limit));if(out.size()>max)return new ArrayList<JSONObject>(out.subList(0,max));return out;
    }

    /** Páginas vizinhas preservam tabelas e procedimentos que atravessam uma
     * quebra de página. O mundo ativo continua isolado pelo vínculo do manual. */
    public List<JSONObject> getManualLocalPageWindow(long manualId,int pageNo,int radius){
        if(manualId<=0||pageNo<=0)return new ArrayList<JSONObject>();
        int safeRadius=Math.max(0,Math.min(3,radius));
        String worldClause=manualWorldClause(false,"m");
        return queryList("SELECT p.manual_id,p.source_key,p.page_no,p.title,p.content,p.normalized,p.section_title,p.chapter_title,p.content_kind,p.structure_json,p.cross_refs_json,p.page_hash,m.category,m.rule_profile,m.source,m.title AS manual_title,m.rag_status,m.rag_store_name,m.rag_page_count,m.rag_split_mode,m.rag_document FROM manual_local_pages p JOIN manuals m ON m.id=p.manual_id WHERE m.enabled=1 AND "+worldClause+" AND m.rag_status='ready' AND m.rag_store_name=? AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'') AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND m.rag_document='local:'||p.source_key AND p.manual_id=? AND p.page_no BETWEEN ? AND ? AND LENGTH(TRIM(p.content))>0 ORDER BY p.page_no",
                new String[]{LOCAL_MANUAL_STORE,String.valueOf(manualId),String.valueOf(Math.max(1,pageNo-safeRadius)),String.valueOf(pageNo+safeRadius)});
    }
    private static void expandSemanticTerms(ArrayList<String> terms){
        String[][] groups={
                {"ataque","atacar","attack","acerto","strike","golpe"},{"dano","damage","ferimento","wound"},
                {"salvamento","fortitude","reflexos","reflex","vontade","will","save"},{"condicao","condition","frightened","assustado","clumsy","desajeitado","enfeebled","enfraquecido","stupefied","estupefato"},
                {"magia","feitico","spell","conjuracao","casting"},{"foco","focus","focus point","ponto de foco"},
                {"descanso","rest","recuperacao","recovery"},{"viagem","travel","exploracao","exploration","journey","marcha"},
                {"reacao","reaction","acao","action","atividade","activity"},{"criatura","monstro","creature","monster","bloco","stat block"},
                {"item","equipamento","equipment","tesouro","treasure"},{"recurso","resource","carga","charge"},
                {"comprar","buy","vender","sell","preco","price"},{"fabricar","craft","crafting","criar"},
                {"morrendo","dying","ferido","wounded","condenado","doomed","dano persistente","persistent damage"},
                {"agarrar","grapple","derrubar","trip","empurrar","shove","desarmar","disarm"},
                {"penalidade ataques multiplos","multiple attack penalty","map","agil","agile"},
                {"graus sucesso","degrees success","critical success","sucesso critico","critical failure","falha critica"}
        };
        java.util.LinkedHashSet<String> expanded=new java.util.LinkedHashSet<String>(terms);
        for(String[] group:groups){boolean matched=false;for(String word:group)for(String term:terms)if(normalizeSearchText(word).contains(term)||term.contains(normalizeSearchText(word))){matched=true;break;}if(!matched)continue;for(String word:group){String n=normalizeSearchText(word);for(String part:n.split("\s+"))if(part.length()>=3)expanded.add(part);}}
        terms.clear();for(String term:expanded){terms.add(term);if(terms.size()>=36)break;}
    }
    private static String normalizeSearchText(String value){if(value==null)return "";String n=java.text.Normalizer.normalize(value.toLowerCase(new java.util.Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    /** Confirma livro, página e trecho contra o índice textual local do manual ativo.
     * Sem página local verificável, qualquer mutação mecânica deve permanecer bloqueada. */
    public boolean verifyEnabledManualExcerpt(String source,int page,String excerpt){
        if(source==null||source.trim().length()==0||page<=0||excerpt==null||excerpt.trim().length()<12)return false;
        boolean ignoredLegacyProfile=false;String worldClause=manualWorldClause(false,"m");
        List<JSONObject> rows=queryList("SELECT m.id,m.title,m.local_uri,m.source,m.source_root,p.content,p.normalized FROM manuals m JOIN manual_local_pages p ON p.manual_id=m.id WHERE m.enabled=1 AND "+worldClause+" AND m.rag_status='ready' AND m.rag_store_name=? AND COALESCE(m.rag_modified_time,'')=COALESCE(m.modified_time,'') AND COALESCE(m.rag_split_mode,'') NOT LIKE 'fallback%' AND m.rag_document='local:'||p.source_key AND p.page_no=? AND LENGTH(TRIM(p.content))>0",new String[]{LOCAL_MANUAL_STORE,String.valueOf(page)});
        String wanted=normalizeSearchText(baseNameForVerification(source)),proof=normalizeSearchText(excerpt);if(wanted.length()==0||proof.length()==0)return false;
        for(JSONObject row:rows){String title=normalizeSearchText(row.optString("title"));String identity=normalizeSearchText(row.optString("title")+" "+baseNameForVerification(row.optString("local_uri"))+" "+baseNameForVerification(row.optString("source"))+" "+baseNameForVerification(row.optString("source_root")));if(!(identity.contains(wanted)||(title.length()>0&&wanted.contains(title))))continue;String pageText=row.optString("normalized");if(pageText.trim().length()==0)pageText=normalizeSearchText(row.optString("content"));if(pageText.contains(proof)||manualExcerptOverlap(proof,pageText)>=0.55d)return true;}
        return false;
    }
    private static String baseNameForVerification(String value){String s=value==null?"":value.replace('\\','/').trim();int slash=s.lastIndexOf('/');return slash>=0?s.substring(slash+1):s;}
    private static double manualExcerptOverlap(String excerpt,String pageText){java.util.HashSet<String> terms=new java.util.HashSet<String>();for(String term:normalizeSearchText(excerpt).split("\\s+"))if(term.length()>3)terms.add(term);if(terms.isEmpty())return 0d;int found=0;for(String term:terms)if(pageText.contains(term))found++;return found/(double)terms.size();}
    public void resetLocalManualIndexForWorld(boolean ignoredLegacyProfile){SQLiteDatabase sql=getWritableDatabase();String where=manualWorldClause(ignoredLegacyProfile,"");sql.beginTransaction();try{sql.execSQL("DELETE FROM manual_local_pages WHERE manual_id IN (SELECT id FROM manuals WHERE "+where+")");ContentValues v=new ContentValues();v.put("rag_status","not_indexed");v.put("rag_indexed_at",0);v.put("rag_error","");v.put("rag_modified_time","");v.put("rag_store_name","local-v1");v.put("rag_part_count",0);v.put("rag_parts_ready",0);v.put("rag_page_count",0);v.put("rag_source_hash","");v.put("rag_progress","Índice local limpo.");v.put("rag_split_mode","local");v.put("rag_document","");sql.update("manuals",v,where,null);sql.setTransactionSuccessful();}finally{sql.endTransaction();}}

    public int countManuals(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manuals",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public int countEnabledManuals(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manuals WHERE enabled=1",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public int countRagReady(String storeName){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manuals WHERE enabled=1 AND rag_status='ready' AND rag_store_name=?",new String[]{storeName==null?"":storeName});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}

    public JSONObject getStoryState(){ensureWorldProfileData(activeMapProfile);return queryOne("SELECT * FROM story_state_profiles WHERE world_profile=?",new String[]{getActiveMapProfile()});}
    public void saveStoryState(boolean started,String narration,String visualPrompt,String optionsJson,String pendingTestJson,String lastAction){ContentValues v=new ContentValues();v.put("started",started?1:0);v.put("last_narration",narration==null?"":narration);v.put("visual_prompt",visualPrompt==null?"":visualPrompt);v.put("options_json",optionsJson==null?"[]":optionsJson);v.put("pending_test_json",pendingTestJson==null?"":pendingTestJson);v.put("last_action",lastAction==null?"":lastAction);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("story_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});}
    public String getSceneOptionHistory(){return getStoryState().optString("option_history_json","[]");}
    public void saveSceneOptionHistory(String json){ContentValues v=new ContentValues();v.put("option_history_json",validJsonArray(json));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("story_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});}
    public void clearStoryState(){saveStoryState(false,"","","[]","","");saveSceneOptionHistory("[]");}

    public synchronized void setActiveMapProfile(String profile){activeMapProfile=normalizeMapProfile(profile);ensureMapWorldState(activeMapProfile);ensureWorldProfileData(activeMapProfile);}
    public String getActiveMapProfile(){return normalizeMapProfile(activeMapProfile);}
    private static String manualWorldClause(boolean ignoredLegacyProfile,String alias){
        String prefix=alias==null||alias.trim().length()==0?"":alias.trim()+".";
        String title="LOWER(COALESCE("+prefix+"title,''))";
        String profile="LOWER(COALESCE("+prefix+"rule_profile,''))";
        // Firewall fail-closed para autoridade mecânica: nenhum corpus D&D/Mystara/PF1/Traveller
        // pode entrar na recuperação usada pelo Mestre Pathfinder 2e.
        return title+" NOT LIKE '%mystara%'"+
                " AND "+title+" NOT LIKE '%d&d%'"+
                " AND "+title+" NOT LIKE '%dnd%'"+
                " AND "+title+" NOT LIKE '%dungeons%dragons%'"+
                " AND "+title+" NOT LIKE '%pathfinder 1%'"+
                " AND "+title+" NOT LIKE '%pathfinder1%'"+
                " AND "+title+" NOT LIKE '%pf1%'"+
                " AND "+title+" NOT LIKE '%traveller%'"+
                " AND "+profile+" NOT LIKE '%5e%'"+
                " AND "+profile+" NOT LIKE '%dnd%'"+
                " AND "+profile+" NOT LIKE '%mystara%'"+
                " AND "+profile+" NOT LIKE '%pathfinder 1%'"+
                " AND "+profile+" NOT LIKE '%pf1%'"+
                " AND "+profile+" NOT LIKE '%traveller%'";
    }

    private static String normalizeMapProfile(String profile){return "pf2";}
    private void ensureWorldProfileData(String profile){SQLiteDatabase db=getWritableDatabase();createWorldProfileTables(db);String p="pf2";ContentValues c=new ContentValues();c.put("world_profile",p);c.put("name","Nova Campanha");c.put("rules_profile","Pathfinder 2e Remaster");c.put("tone","Exploração e presságios");c.put("feats",1);c.put("multiclass",0);c.put("flanking",1);c.put("notes","");db.insertWithOnConflict("campaign_profiles",null,c,SQLiteDatabase.CONFLICT_IGNORE);ContentValues state=new ContentValues();state.put("world_profile",p);db.insertWithOnConflict("story_state_profiles",null,state,SQLiteDatabase.CONFLICT_IGNORE);db.insertWithOnConflict("combat_state_profiles",null,state,SQLiteDatabase.CONFLICT_IGNORE);db.insertWithOnConflict("scene_state_profiles",null,state,SQLiteDatabase.CONFLICT_IGNORE);}
    private void ensureMapWorldState(String profile){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("world_profile",normalizeMapProfile(profile));db.insertWithOnConflict("map_world_state",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    public JSONObject getMapState(){return getMapStateForProfile(activeMapProfile);}
    public JSONObject getMapStateForProfile(String profile){String normalized=normalizeMapProfile(profile);ensureMapWorldState(normalized);return queryOne("SELECT * FROM map_world_state WHERE world_profile=?",new String[]{normalized});}
    private void updateMapStateForProfile(String profile,ContentValues v){String normalized=normalizeMapProfile(profile);ensureMapWorldState(normalized);getWritableDatabase().update("map_world_state",v,"world_profile=?",new String[]{normalized});}
    private void updateActiveMapState(ContentValues v){updateMapStateForProfile(activeMapProfile,v);}
    public void saveMapState(String uri,String grid,float gridSize,boolean fog,float tokenX,float tokenY,float scale,float ox,float oy){ContentValues v=new ContentValues();v.put("image_uri",uri);v.put("grid_type",grid);v.put("grid_size",gridSize);v.put("fog_enabled",fog?1:0);v.put("token_x",tokenX);v.put("token_y",tokenY);v.put("scale",scale);v.put("offset_x",ox);v.put("offset_y",oy);updateActiveMapState(v);}
    public void saveMapPackageState(String imageUri,String overlayUri,String manifestJson,String packageId,String currentHexId,boolean overlayVisible,String revealsJson,String grid,float gridSize,boolean fog,float tokenX,float tokenY,float scale,float ox,float oy){saveMapPackageStateForProfile(activeMapProfile,imageUri,overlayUri,manifestJson,packageId,currentHexId,overlayVisible,revealsJson,grid,gridSize,fog,tokenX,tokenY,scale,ox,oy);}
    public void saveMapPackageStateForProfile(String profile,String imageUri,String overlayUri,String manifestJson,String packageId,String currentHexId,boolean overlayVisible,String revealsJson,String grid,float gridSize,boolean fog,float tokenX,float tokenY,float scale,float ox,float oy){
        ContentValues v=new ContentValues();
        v.put("logical_map_id",logicalMapId(manifestJson,packageId));v.put("image_uri",imageUri==null?"":imageUri);v.put("overlay_uri",overlayUri==null?"":overlayUri);v.put("manifest_json",manifestJson==null?"":manifestJson);v.put("map_package_id",packageId==null?"":packageId);v.put("current_hex_id",currentHexId==null?"":currentHexId);v.put("overlay_visible",overlayVisible?1:0);v.put("reveals_json",revealsJson==null?"{}":revealsJson);v.put("grid_type",grid==null?"Quadrada":grid);v.put("grid_size",gridSize);v.put("fog_enabled",fog?1:0);v.put("token_x",tokenX);v.put("token_y",tokenY);v.put("scale",scale);v.put("offset_x",ox);v.put("offset_y",oy);updateMapStateForProfile(profile,v);
    }
    private static String logicalMapId(String manifestJson,String packageId){
        try{if(manifestJson!=null&&manifestJson.trim().length()>0){MapManifestGeometry g=MapManifestGeometry.parse(manifestJson);String id=g.getLogicalMapId();if(id==null||id.trim().length()==0)id=g.getMapId();if(id!=null&&id.trim().length()>0)return id.trim();}}catch(Exception e){AppLog.w("AppDb","Manifesto de mapa sem identidade lógica durante persistência.",e);}
        String value=packageId==null?"":packageId.trim();int at=value.indexOf('@');return at>0?value.substring(0,at):value;
    }
    public JSONObject getLatestMapPackageByMapId(String profile,String mapId){if(mapId==null||mapId.trim().length()==0)return new JSONObject();String wanted=mapId.trim(),normalized=normalizeMapProfile(profile);JSONObject exact=queryOne("SELECT * FROM map_packages WHERE ready=1 AND world_profile=? AND map_id=? ORDER BY imported_at DESC LIMIT 1",new String[]{normalized,wanted});if(exact.length()>0)return exact;for(JSONObject row:queryList("SELECT * FROM map_packages WHERE ready=1 AND world_profile=? ORDER BY imported_at DESC LIMIT 12",new String[]{normalized})){if(wanted.equals(logicalMapId(row.optString("manifest_json"),row.optString("package_id"))))return row;}return new JSONObject();}
    public JSONObject getMapPackageAny(String packageId){if(packageId==null||packageId.length()==0)return new JSONObject();return queryOne("SELECT * FROM map_packages WHERE package_id=?",new String[]{packageId});}
    public void markMapPackageUnavailable(String packageId,String reason){
        if(packageId==null||packageId.length()==0)return;
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            ContentValues packageValues=new ContentValues();packageValues.put("ready",0);db.update("map_packages",packageValues,"package_id=?",new String[]{packageId});
            JSONObject pack=getMapPackageAny(packageId);String profile=normalizeMapProfile(pack.optString("world_profile",getActiveMapProfile()));JSONObject state=getMapStateForProfile(profile);
            if(packageId.equals(state.optString("map_package_id"))){ContentValues stateValues=new ContentValues();stateValues.put("logical_map_id",pack.optString("map_id",state.optString("logical_map_id")));stateValues.put("image_uri","");stateValues.put("overlay_uri","");stateValues.put("map_package_id","");updateMapStateForProfile(profile,stateValues);}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        AppLog.w("AppDb","Pacote de mapa invalidado: "+packageId+" • "+(reason==null?"referência indisponível":reason));
    }

    public void saveMapViewExtras(String currentHexId,boolean overlayVisible,String revealsJson){ContentValues v=new ContentValues();v.put("current_hex_id",currentHexId==null?"":currentHexId);v.put("overlay_visible",overlayVisible?1:0);v.put("reveals_json",revealsJson==null?"{}":revealsJson);updateActiveMapState(v);}
    public void saveMapOverlayVisibility(boolean idsVisible,boolean regionsVisible,boolean politicalVisible){ContentValues v=new ContentValues();v.put("overlay_visible",idsVisible?1:0);v.put("regions_overlay_visible",regionsVisible?1:0);v.put("political_overlay_visible",politicalVisible?1:0);updateActiveMapState(v);}
    public void saveMapTravelState(boolean locked,String destination,String routeJson,String owner){ContentValues v=new ContentValues();v.put("position_locked",locked?1:0);v.put("destination_hex_id",destination==null?"":destination);v.put("route_json",routeJson==null?"[]":routeJson);v.put("route_owner",owner==null?"Jogador":owner);updateActiveMapState(v);}
    public void saveMapTravelStateForProfile(String profile,boolean locked,String destination,String routeJson,String owner){ContentValues v=new ContentValues();v.put("position_locked",locked?1:0);v.put("destination_hex_id",destination==null?"":destination);v.put("route_json",routeJson==null?"[]":routeJson);v.put("route_owner",owner==null?"Jogador":owner);updateMapStateForProfile(profile,v);}
    public void saveMapRenderMode(String mode,float minZoom){saveMapRenderModeForProfile(activeMapProfile,mode,minZoom);}
    public void saveMapRenderModeForProfile(String profile,String mode,float minZoom){ContentValues v=new ContentValues();String normalized=mode==null?"dynamic":mode.trim().toLowerCase();if(!normalized.equals("dynamic")&&!normalized.equals("vector")&&!normalized.equals("raster"))normalized="dynamic";v.put("overlay_mode",normalized);v.put("min_label_zoom",Math.max(.1f,minZoom));updateMapStateForProfile(profile,v);}
    public List<JSONObject> listMapPackages(String profile){return queryList("SELECT * FROM map_packages WHERE world_profile=? ORDER BY imported_at DESC",new String[]{normalizeMapProfile(profile)});}
    public List<JSONObject> listReadyMapPackages(String profile){return queryList("SELECT * FROM map_packages WHERE ready=1 AND world_profile=? ORDER BY imported_at DESC",new String[]{normalizeMapProfile(profile)});}
    public JSONObject getLatestMapPackageForProfile(String profile){return queryOne("SELECT * FROM map_packages WHERE ready=1 AND world_profile=? ORDER BY imported_at DESC LIMIT 1",new String[]{normalizeMapProfile(profile)});}
    public JSONObject getMapPackage(String packageId){return queryOne("SELECT * FROM map_packages WHERE package_id=? AND ready=1",new String[]{packageId});}
    public JSONObject getMapNation(String packageId,String nationId){if(packageId==null||nationId==null||nationId.trim().isEmpty())return new JSONObject();return queryOne("SELECT * FROM map_nations WHERE package_id=? AND nation_id=?",new String[]{packageId,nationId.trim()});}
    public int countMapNations(String packageId){if(packageId==null||packageId.isEmpty())return 0;Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM map_nations WHERE package_id=?",new String[]{packageId});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public JSONObject getMapMetaregion(String packageId,String id){if(packageId==null||id==null||id.trim().isEmpty())return new JSONObject();return queryOne("SELECT * FROM map_metaregions WHERE package_id=? AND metaregion_id=?",new String[]{packageId,id.trim()});}
    public JSONObject getMapLocation(String packageId,String locationId){if(packageId==null||locationId==null||locationId.trim().isEmpty())return new JSONObject();return queryOne("SELECT * FROM map_locations WHERE package_id=? AND location_id=?",new String[]{packageId,locationId.trim()});}
    public void activateMapPackage(String packageId){
        JSONObject pack=getMapPackage(packageId);if(pack.length()==0)return;String manifest=pack.optString("manifest_json","");JSONObject old=getMapState();String current=old.optString("current_hex_id","");JSONObject hex=current.length()>0?getRawMapHex(packageId,current):new JSONObject();if(hex.length()==0){double cx=pack.optDouble("image_width",1)/2d,cy=pack.optDouble("image_height",1)/2d;hex=queryOne("SELECT * FROM map_hex_atlas WHERE package_id=? ORDER BY abs(center_x-?)+abs(center_y-?) LIMIT 1",new String[]{packageId,String.valueOf(cx),String.valueOf(cy)});current=hex.optString("hex_id","");}
        float width=Math.max(1,pack.optInt("image_width",1)),height=Math.max(1,pack.optInt("image_height",1));float tx=hex.length()==0?.5f:(float)hex.optDouble("center_x",width/2f)/width,ty=hex.length()==0?.5f:(float)hex.optDouble("center_y",height/2f)/height;
        saveMapPackageStateForProfile("pf2",pack.optString("image_uri"),pack.optString("overlay_uri"),manifest,packageId,current,old.optInt("overlay_visible",1)==1,old.optString("reveals_json","{}"),"Hexagonal flat-top",52f,old.optInt("fog_enabled",0)==1,tx,ty,(float)old.optDouble("scale",0),(float)old.optDouble("offset_x",0),(float)old.optDouble("offset_y",0));
    }
    /** Quantidade total de verbetes preservados no catálogo do pacote, posicionados ou não. */
    public int countMapCatalogLocations(String packageId){
        if(packageId==null||packageId.length()==0)return 0;Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM map_locations WHERE package_id=?",new String[]{packageId});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }
    /** Quantidade de locais realmente ligados a uma célula e aptos para uso narrativo. */
    public int countMapPositionedLocations(String packageId){
        if(packageId==null||packageId.length()==0)return 0;String sql="SELECT COUNT(DISTINCT l.location_id) FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id WHERE l.package_id=? AND (COALESCE(l.hex_id,'')<>'' OR COALESCE(j.hex_id,'')<>'') AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%'";Cursor c=getReadableDatabase().rawQuery(sql,new String[]{packageId});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }
    /** Locais canônicos com algum hex de referência válido, inclusive aproximações explícitas. */
    public int countMapReferencedLocations(String packageId){
        if(packageId==null||packageId.length()==0)return 0;
        String sql="SELECT COUNT(DISTINCT l.location_id) FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id INNER JOIN map_hex_atlas h ON h.package_id=l.package_id AND h.hex_id=COALESCE(NULLIF(l.hex_id,''),j.hex_id) WHERE l.package_id=? AND COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>''";
        Cursor c=getReadableDatabase().rawQuery(sql,new String[]{packageId});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }
    /** Subconjunto referenciado cuja posição é declarada aproximada pelo próprio pacote. */
    public int countMapApproximateReferencedLocations(String packageId){
        if(packageId==null||packageId.length()==0)return 0;
        String sql="SELECT COUNT(DISTINCT l.location_id) FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id INNER JOIN map_hex_atlas h ON h.package_id=l.package_id AND h.hex_id=COALESCE(NULLIF(l.hex_id,''),j.hex_id) WHERE l.package_id=? AND COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>'' AND lower(COALESCE(l.placement_status,'')) LIKE '%approx%'";
        Cursor c=getReadableDatabase().rawQuery(sql,new String[]{packageId});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }
    /** POIs oficiais que podem ser projetados no mapa sem promover aproximação a fato exato. */
    public List<JSONObject> listMapLocationsWithHexReference(String packageId,int limit){
        if(packageId==null||packageId.length()==0)return new ArrayList<JSONObject>();
        int max=Math.max(1,Math.min(2000,limit));
        List<JSONObject> rows=queryList("SELECT l.*,COALESCE(NULLIF(l.hex_id,''),j.hex_id) AS linked_hex_id FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id INNER JOIN map_hex_atlas h ON h.package_id=l.package_id AND h.hex_id=COALESCE(NULLIF(l.hex_id,''),j.hex_id) WHERE l.package_id=? AND COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>'' GROUP BY l.package_id,l.location_id ORDER BY l.name COLLATE NOCASE LIMIT "+max,new String[]{packageId});
        for(JSONObject row:rows){try{String resolved=row.optString("hex_id","").trim();if(resolved.length()==0)resolved=row.optString("linked_hex_id","").trim();row.put("hex_id",resolved);row.put("canonical",true);}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao montar referência cartográfica canônica.",ignored);}}
        return rows;
    }

    /** Preserva revisões/anotações ao migrar entre revisões da mesma família cartográfica. */
    public void copyMapUserData(String fromMapId,String toMapId){
        if(fromMapId==null||toMapId==null||fromMapId.trim().isEmpty()||toMapId.trim().isEmpty()||fromMapId.equals(toMapId))return;
        SQLiteDatabase sql=getWritableDatabase();sql.beginTransaction();try{
            sql.execSQL("INSERT OR IGNORE INTO map_hex_corrections(map_id,hex_id,terrain_primary,terrain_secondary,terrain_confidence,roads_json,waterways_json,political_json,review_status,review_reasons,custom_locations_json,hide_unconfirmed_locations,review_note,updated_at) SELECT ?,hex_id,terrain_primary,terrain_secondary,terrain_confidence,roads_json,waterways_json,political_json,review_status,review_reasons,custom_locations_json,hide_unconfirmed_locations,review_note,updated_at FROM map_hex_corrections WHERE map_id=?",new Object[]{toMapId,fromMapId});
            sql.execSQL("INSERT OR IGNORE INTO map_hex_notes(world_profile,map_id,hex_id,note_text,note_title,note_details,icon,created_by,created_at,updated_at) SELECT world_profile,?,hex_id,note_text,note_title,note_details,icon,created_by,created_at,updated_at FROM map_hex_notes WHERE map_id=?",new Object[]{toMapId,fromMapId});
            ensureCampaignPoiSchema(sql);sql.execSQL("UPDATE campaign_pois SET map_id=? WHERE map_id=?",new Object[]{toMapId,fromMapId});
            sql.setTransactionSuccessful();
        }finally{sql.endTransaction();}
    }

    public JSONObject getRawMapHex(String packageId,String hexId){return queryOne("SELECT * FROM map_hex_atlas WHERE package_id=? AND hex_id=?",new String[]{packageId,hexId});}
    private String getMapIdForPackage(String packageId){if(packageId==null||packageId.length()==0)return "";return queryOne("SELECT map_id FROM map_packages WHERE package_id=?",new String[]{packageId}).optString("map_id");}
    public JSONObject getMapHexCorrection(String packageId,String hexId){String mapId=getMapIdForPackage(packageId);if(mapId.length()==0)return new JSONObject();return queryOne("SELECT * FROM map_hex_corrections WHERE map_id=? AND hex_id=?",new String[]{mapId,hexId});}
    private JSONObject mergeMapHex(JSONObject base,JSONObject correction){
        if(base==null)base=new JSONObject();if(correction==null||correction.length()==0)return base;
        try{JSONObject out=new JSONObject(base.toString());String[] fields={"terrain_primary","terrain_secondary","roads_json","waterways_json","political_json","review_status","review_reasons"};for(String field:fields){if(correction.has(field)&&!correction.isNull(field))out.put(field,correction.optString(field));}if(correction.has("terrain_confidence")&&!correction.isNull("terrain_confidence"))out.put("terrain_confidence",correction.optDouble("terrain_confidence"));out.put("manual_correction",1);out.put("review_note",correction.optString("review_note"));out.put("hide_unconfirmed_locations",correction.optInt("hide_unconfirmed_locations"));out.put("correction_updated_at",correction.optLong("updated_at"));return out;}catch(Exception e){return base;}
    }
    public JSONObject getMapHex(String packageId,String hexId){return mergeMapHex(getRawMapHex(packageId,hexId),getMapHexCorrection(packageId,hexId));}
    public List<JSONObject> getMapLocationsForHex(String packageId,String hexId){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();JSONObject correction=getMapHexCorrection(packageId,hexId);boolean hide=correction.optInt("hide_unconfirmed_locations",0)==1;
        List<JSONObject> base=queryList("SELECT l.*,j.hex_id AS linked_hex_id FROM map_locations l INNER JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id WHERE j.package_id=? AND j.hex_id=? ORDER BY l.name COLLATE NOCASE",new String[]{packageId,hexId});
        for(JSONObject item:base){String status=item.optString("placement_status").toLowerCase();if(hide&&(status.contains("review")||status.contains("approx")))continue;try{if(item.optString("hex_id","").trim().length()==0)item.put("hex_id",item.optString("linked_hex_id",hexId));}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada ao resolver vínculo de local.",ignored);}out.add(item);}
        try{JSONArray custom=new JSONArray(correction.optString("custom_locations_json","[]"));for(int i=0;i<custom.length();i++){JSONObject item=custom.optJSONObject(i);if(item!=null){JSONObject copy=new JSONObject(item.toString());copy.put("manual_correction",1);copy.put("hex_id",hexId);out.add(copy);}}}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}return out;
    }
    public List<JSONObject> findMapLocationsByQuery(String packageId,String query,int limit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();if(packageId==null||packageId.length()==0)return out;String q=normalizeSearchText(query);if(q.length()<2)return out;
        List<JSONObject> rows=queryList("SELECT * FROM map_locations WHERE package_id=? LIMIT 5000",new String[]{packageId});
        for(JSONObject row:rows){String name=normalizeSearchText(row.optString("name"));String region=normalizeSearchText(row.optString("region"));String raw=normalizeSearchText(row.optString("raw_json"));int score=0;if(name.equals(q))score+=1000;else if(name.startsWith(q))score+=450;else if(name.contains(q))score+=250;if(raw.contains(q))score+=120;if(region.contains(q))score+=40;for(String term:q.split("\\s+")){if(term.length()<2)continue;if(name.contains(term))score+=40;if(raw.contains(term))score+=18;if(region.contains(term))score+=8;}if(score>0){try{row.put("_score",score);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}out.add(row);}}
        java.util.Collections.sort(out,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){int s=Integer.compare(b.optInt("_score"),a.optInt("_score"));if(s!=0)return s;return a.optString("name").compareToIgnoreCase(b.optString("name"));}});int max=Math.max(1,Math.min(50,limit));return out.size()>max?new ArrayList<JSONObject>(out.subList(0,max)):out;
    }
    public List<JSONObject> findConfirmedLocationsByQuery(String packageId,String query,int limit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();if(packageId==null||packageId.length()==0)return out;String q=normalizeSearchText(query);if(q.length()<2)return out;
        List<JSONObject> rows=queryList("SELECT l.*,COALESCE(NULLIF(l.hex_id,''),j.hex_id) AS linked_hex_id FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id WHERE l.package_id=? AND COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>'' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%' GROUP BY l.package_id,l.location_id LIMIT 5000",new String[]{packageId});for(JSONObject row:rows){try{if(row.optString("hex_id","").length()==0)row.put("hex_id",row.optString("linked_hex_id",""));}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada ao resolver local pesquisável.",ignored);}}
        for(JSONObject row:rows){String name=normalizeSearchText(row.optString("name")),region=normalizeSearchText(row.optString("region")),raw=normalizeSearchText(row.optString("raw_json"));int score=0;if(name.equals(q))score+=1000;else if(name.startsWith(q))score+=450;else if(name.contains(q))score+=250;if(raw.contains(q))score+=150;if(region.contains(q))score+=35;for(String term:q.split("\s+")){if(term.length()<2)continue;if(name.contains(term))score+=35;if(raw.contains(term))score+=20;if(region.contains(term))score+=7;}if(score>0){try{row.put("_score",score);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}out.add(row);}}
        java.util.Collections.sort(out,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){int c=Integer.compare(b.optInt("_score"),a.optInt("_score"));return c!=0?c:a.optString("name").compareToIgnoreCase(b.optString("name"));}});int max=Math.max(1,Math.min(10,limit));return out.size()>max?new ArrayList<JSONObject>(out.subList(0,max)):out;
    }
    public List<JSONObject> getMapHexes(String packageId,List<String> hexIds){ArrayList<JSONObject> out=new ArrayList<JSONObject>();if(packageId==null||packageId.length()==0||hexIds==null||hexIds.size()==0)return out;StringBuilder q=new StringBuilder("SELECT * FROM map_hex_atlas WHERE package_id=? AND hex_id IN (");ArrayList<String> args=new ArrayList<String>();args.add(packageId);for(int i=0;i<hexIds.size();i++){if(i>0)q.append(',');q.append('?');args.add(hexIds.get(i));}q.append(')');List<JSONObject> raw=queryList(q.toString(),args.toArray(new String[args.size()]));for(JSONObject item:raw)out.add(mergeMapHex(item,getMapHexCorrection(packageId,item.optString("hex_id"))));return out;}
    private static boolean isNarrativeStartLocationType(String type){
        String value=type==null?"":type.trim().toLowerCase(Locale.US);
        return value.equals("cidade")||value.equals("vila")||value.equals("porto")||value.equals("fortaleza")||value.equals("castelo")||value.equals("ruina")||value.equals("masmorra")||value.equals("templo")||value.equals("caverna")||value.equals("marco_magico")||value.equals("bairro_edificio")||value.equals("outro")||value.equals("underdark")||value.equals("ilha");
    }
    private static boolean isConfirmedMapLocation(JSONObject location){
        if(location==null)return false;String resolved=location.optString("hex_id","").trim();if(resolved.length()==0)resolved=location.optString("linked_hex_id","").trim();if(resolved.length()==0)return false;
        String status=location.optString("placement_status","").toLowerCase(Locale.US);
        return !status.contains("review")&&!status.contains("approx")&&!status.contains("unassigned")&&isNarrativeStartLocationType(location.optString("type",""));
    }
    private static int adventureLocationPriority(String type){
        String value=type==null?"":type.trim().toLowerCase(Locale.US);
        if(value.equals("cidade"))return 140;
        if(value.equals("vila"))return 120;
        if(value.equals("porto"))return 105;
        if(value.equals("fortaleza")||value.equals("castelo"))return 95;
        if(value.equals("ruina")||value.equals("masmorra")||value.equals("templo")||value.equals("caverna")||value.equals("marco_magico"))return 80;
        if(value.equals("bairro_edificio")||value.equals("underdark")||value.equals("ilha"))return 65;
        return 55;
    }
    private JSONObject enrichAdventureStartCandidate(String packageId,JSONObject hex){
        if(hex==null||hex.length()==0)return new JSONObject();
        try{
            JSONObject out=new JSONObject(hex.toString());JSONObject best=null;int bestPriority=-1;
            for(JSONObject location:getMapLocationsForHex(packageId,hex.optString("hex_id",""))){
                if(!isConfirmedMapLocation(location))continue;int priority=adventureLocationPriority(location.optString("type",""));
                if(priority>bestPriority){best=location;bestPriority=priority;}
            }
            if(best!=null){
                out.put("start_kind","location");out.put("start_weight",bestPriority);
                out.put("start_location_name",best.optString("name",""));out.put("start_location_type",best.optString("type",""));out.put("start_location_region",best.optString("region",""));out.put("start_location_raw",best.optString("raw_json","{}"));
                return out;
            }
            String terrain=out.optString("terrain_primary","").trim().toLowerCase(Locale.US);double confidence=out.optDouble("terrain_confidence",0d);String review=out.optString("review_status","").toLowerCase(Locale.US);
            boolean reliable=terrain.length()>0&&!terrain.equals("unassigned")&&!terrain.equals("null")&&!terrain.equals("water")&&!terrain.equals("sea")&&!terrain.equals("ocean")&&!terrain.equals("coast_water")&&!terrain.equals("map_border")&&confidence>=0.60d&&!review.contains("needs")&&!review.contains("unassigned");
            if(reliable){out.put("start_kind","terrain");out.put("start_weight",50);return out;}
            return new JSONObject();
        }catch(Exception e){AppLog.w("AppDb","Falha ao enriquecer ponto inicial catalogado.",e);return new JSONObject();}
    }
    public JSONObject describeCatalogedAdventureStart(String packageId,String hexId){
        if(packageId==null||packageId.length()==0||hexId==null||hexId.trim().length()==0)return new JSONObject();
        return enrichAdventureStartCandidate(packageId,getMapHex(packageId,hexId.trim()));
    }
    public boolean isCatalogedAdventureStartHex(String packageId,String hexId){return describeCatalogedAdventureStart(packageId,hexId).length()>0;}

    /**
     * Transfere posições confirmadas de um pacote anterior do mesmo mundo para o
     * atlas ativo usando coordenadas normalizadas. Isso preserva cidades já
     * catalogadas ao trocar apenas a resolução/geometria da mesma cartografia.
     */
    public int migrateConfirmedLocationPlacementsToPackage(String targetPackageId){
        if(targetPackageId==null||targetPackageId.length()==0)return 0;
        JSONObject targetPack=getMapPackage(targetPackageId);if(targetPack.length()==0)return 0;
        double targetWidth=targetPack.optDouble("image_width",0d),targetHeight=targetPack.optDouble("image_height",0d);if(targetWidth<=0d||targetHeight<=0d)return 0;
        String profile=normalizeMapProfile(targetPack.optString("world_profile",getActiveMapProfile()));String targetMapId=targetPack.optString("map_id","").toLowerCase(Locale.US);int migrated=0;HashSet<String> migratedTargets=new HashSet<String>();
        final HashMap<String,JSONObject> targetById=new HashMap<String,JSONObject>();final HashMap<String,JSONObject> targetByName=new HashMap<String,JSONObject>();
        for(JSONObject targetLocation:queryList("SELECT * FROM map_locations WHERE package_id=?",new String[]{targetPackageId})){String id=targetLocation.optString("location_id","").trim();if(id.length()>0)targetById.put(id,targetLocation);String normalizedName=normalizeSearchText(targetLocation.optString("name",""));if(normalizedName.length()>0&&!targetByName.containsKey(normalizedName))targetByName.put(normalizedName,targetLocation);try{JSONObject raw=new JSONObject(targetLocation.optString("raw_json","{}"));JSONArray aliases=raw.optJSONArray("aliases");if(aliases!=null)for(int ai=0;ai<aliases.length();ai++){String alias=normalizeSearchText(aliases.optString(ai,""));if(alias.length()>0&&!targetByName.containsKey(alias))targetByName.put(alias,targetLocation);}}catch(Exception ignored){AppLog.ignored("AppDb","Alias de local ausente durante migração.",ignored);}}
        // Não exige ready=1: após uma atualização o arquivo do mapa antigo pode não estar mais acessível,
        // mas as posições confirmadas continuam preservadas nas tabelas locais e são suficientes para a migração.
        List<JSONObject> sources=queryList("SELECT * FROM map_packages WHERE world_profile=? AND package_id<>? ORDER BY imported_at DESC",new String[]{profile,targetPackageId});
        SQLiteDatabase sql=getWritableDatabase();sql.beginTransaction();try{
            for(JSONObject source:sources){
                String sourceId=source.optString("package_id","");String sourceMapId=source.optString("map_id","").toLowerCase(Locale.US);double sourceWidth=source.optDouble("image_width",0d),sourceHeight=source.optDouble("image_height",0d);if(sourceId.length()==0||sourceWidth<=0d||sourceHeight<=0d)continue;
                if(targetMapId.length()>0&&!targetMapId.equals(sourceMapId))continue;
                // O atlas legado registrava parte dos locais SOMENTE em map_hex_locations.
                // COALESCE resolve l.hex_id quando existe e, caso contrário, usa o vínculo real da tabela de junção.
                List<JSONObject> rows=queryList("SELECT l.*,COALESCE(NULLIF(l.hex_id,''),j.hex_id) AS linked_hex_id,h.center_x,h.center_y FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id INNER JOIN map_hex_atlas h ON h.package_id=l.package_id AND h.hex_id=COALESCE(NULLIF(l.hex_id,''),j.hex_id) WHERE l.package_id=? AND COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>'' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%'",new String[]{sourceId});
                for(JSONObject row:rows){String sourceLocationId=row.optString("location_id","").trim();JSONObject targetLocation=sourceLocationId.length()>0?targetById.get(sourceLocationId):null;if(targetLocation==null){String normalizedSourceName=normalizeSearchText(row.optString("name",""));targetLocation=targetByName.get(normalizedSourceName);}if(targetLocation==null||!isNarrativeStartLocationType(targetLocation.optString("type","")))continue;
                    String targetLocationId=targetLocation.optString("location_id","");if(targetLocationId.length()==0||migratedTargets.contains(targetLocationId))continue;
                    JSONObject existingJoin=queryOne("SELECT hex_id FROM map_hex_locations WHERE package_id=? AND location_id=? LIMIT 1",new String[]{targetPackageId,targetLocationId});if(targetLocation.optString("hex_id","").length()>0||existingJoin.optString("hex_id","").length()>0)continue;
                    double x=row.optDouble("center_x",Double.NaN),y=row.optDouble("center_y",Double.NaN);if(Double.isNaN(x)||Double.isNaN(y))continue;
                    double tx=Math.max(0d,Math.min(targetWidth,x/sourceWidth*targetWidth)),ty=Math.max(0d,Math.min(targetHeight,y/sourceHeight*targetHeight));
                    JSONObject nearest=queryOne("SELECT hex_id FROM map_hex_atlas WHERE package_id=? ORDER BY ((center_x-?)*(center_x-?)+(center_y-?)*(center_y-?)) LIMIT 1",new String[]{targetPackageId,String.valueOf(tx),String.valueOf(tx),String.valueOf(ty),String.valueOf(ty)});String targetHex=nearest.optString("hex_id","");if(targetHex.length()==0)continue;
                    ContentValues update=new ContentValues();update.put("hex_id",targetHex);update.put("placement_method","legacy_link_normalized_same_map_migration");update.put("placement_confidence",Math.min(.85d,Math.max(.65d,row.optDouble("placement_confidence",.8d))));update.put("placement_status","migrated_confirmed");
                    sql.update("map_locations",update,"package_id=? AND location_id=?",new String[]{targetPackageId,targetLocationId});ContentValues join=new ContentValues();join.put("package_id",targetPackageId);join.put("hex_id",targetHex);join.put("location_id",targetLocationId);sql.insertWithOnConflict("map_hex_locations",null,join,SQLiteDatabase.CONFLICT_REPLACE);migratedTargets.add(targetLocationId);migrated++;
                }
            }
            sql.setTransactionSuccessful();
        }finally{sql.endTransaction();}
        return migrated;
    }

    public boolean isAdventureStartHex(String packageId,String hexId){
        if(packageId==null||packageId.length()==0||hexId==null||hexId.trim().length()==0)return false;
        JSONObject hex=getMapHex(packageId,hexId.trim());if(hex.length()==0)return false;
        double x=hex.optDouble("center_x",Double.NaN),y=hex.optDouble("center_y",Double.NaN);if(Double.isNaN(x)||Double.isInfinite(x)||Double.isNaN(y)||Double.isInfinite(y))return false;
        String terrain=hex.optString("terrain_primary","").trim().toLowerCase(Locale.US);double confidence=hex.optDouble("terrain_confidence",0d);
        if(terrain.length()>0&&!"unassigned".equals(terrain)&&!"null".equals(terrain)&&confidence>=0.60d){
            if("water".equals(terrain)||"sea".equals(terrain)||"ocean".equals(terrain)||"coast_water".equals(terrain)||"map_border".equals(terrain))return false;
        }
        return true;
    }

    public List<JSONObject> listAdventureStartHexes(String packageId,String avoidHex,int limit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();if(packageId==null||packageId.length()==0)return out;
        String avoid=avoidHex==null?"":avoidHex;int requested=Math.max(20,Math.min(1000,limit));int locationLimit=Math.max(20,Math.min(500,requested));int terrainLimit=Math.max(20,Math.min(500,requested));
        String mapId=getMapIdForPackage(packageId);String allowed="'cidade','vila','porto','fortaleza','castelo','ruina','masmorra','templo','caverna','marco_magico','bairro_edificio','outro','underdark','ilha'";String confirmedLocation="lower(COALESCE(l.type,'')) IN ("+allowed+") AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%'";
        String locationSql="SELECT h.* FROM map_hex_atlas h WHERE h.package_id=? AND h.hex_id<>? AND EXISTS(SELECT 1 FROM map_locations l WHERE l.package_id=h.package_id AND "+confirmedLocation+" AND (l.hex_id=h.hex_id OR EXISTS(SELECT 1 FROM map_hex_locations j WHERE j.package_id=l.package_id AND j.location_id=l.location_id AND j.hex_id=h.hex_id))) ORDER BY RANDOM() LIMIT ?";
        String terrainSql="SELECT h.* FROM map_hex_atlas h WHERE h.package_id=? AND h.hex_id<>? AND (EXISTS(SELECT 1 FROM map_hex_corrections c WHERE c.map_id=? AND c.hex_id=h.hex_id AND lower(COALESCE(c.terrain_primary,'')) NOT IN ('water','sea','ocean','coast_water','map_border','unassigned','null') AND COALESCE(c.terrain_confidence,0)>=0.60 AND lower(COALESCE(c.review_status,'')) NOT LIKE '%needs%' AND lower(COALESCE(c.review_status,'')) NOT LIKE '%unassigned%') OR (lower(COALESCE(h.terrain_primary,'')) NOT IN ('','water','sea','ocean','coast_water','map_border','unassigned','null') AND COALESCE(h.terrain_confidence,0)>=0.60 AND lower(COALESCE(h.review_status,'')) NOT LIKE '%needs%' AND lower(COALESCE(h.review_status,'')) NOT LIKE '%unassigned%')) ORDER BY RANDOM() LIMIT ?";
        HashSet<String> seen=new HashSet<String>();List<JSONObject> rawLocations=queryList(locationSql,new String[]{packageId,avoid,String.valueOf(locationLimit)});for(JSONObject item:rawLocations){String hexId=item.optString("hex_id","");JSONObject enriched=enrichAdventureStartCandidate(packageId,mergeMapHex(item,getMapHexCorrection(packageId,hexId)));if(enriched.length()>0&&seen.add(hexId))out.add(enriched);}
        List<JSONObject> rawTerrains=queryList(terrainSql,new String[]{packageId,avoid,mapId,String.valueOf(terrainLimit)});for(JSONObject item:rawTerrains){String hexId=item.optString("hex_id","");if(seen.contains(hexId))continue;JSONObject enriched=enrichAdventureStartCandidate(packageId,mergeMapHex(item,getMapHexCorrection(packageId,hexId)));if(enriched.length()>0&&seen.add(hexId))out.add(enriched);}
        if(out.size()>requested)return new ArrayList<JSONObject>(out.subList(0,requested));return out;
    }


    /**
     * Pontos de abertura catalogados mais próximos de um hex tocado. O hex de
     * origem pode ser apenas geométrico; o resultado, porém, sempre passa pela
     * mesma validação rígida usada pelo sorteio de abertura.
     */
    public List<JSONObject> findNearestAdventureStartCandidates(String packageId,String fromHex,int limit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();
        if(packageId==null||packageId.length()==0||fromHex==null||fromHex.trim().length()==0)return out;
        JSONObject origin=getRawMapHex(packageId,fromHex.trim());if(origin.length()==0)return out;
        final double ox=origin.optDouble("center_x",Double.NaN),oy=origin.optDouble("center_y",Double.NaN);if(Double.isNaN(ox)||Double.isNaN(oy))return out;
        int wanted=Math.max(1,Math.min(12,limit)),perKind=Math.max(8,wanted*3);String avoid=fromHex.trim();String mapId=getMapIdForPackage(packageId);
        String allowed="'cidade','vila','porto','fortaleza','castelo','ruina','masmorra','templo','caverna','marco_magico','bairro_edificio','outro','underdark','ilha'";
        String confirmedLocation="lower(COALESCE(l.type,'')) IN ("+allowed+") AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%'";
        String distance="((h.center_x-?)*(h.center_x-?)+(h.center_y-?)*(h.center_y-?))";
        String locationSql="SELECT h.*,"+distance+" AS distance2 FROM map_hex_atlas h WHERE h.package_id=? AND h.hex_id<>? AND EXISTS(SELECT 1 FROM map_locations l WHERE l.package_id=h.package_id AND "+confirmedLocation+" AND (l.hex_id=h.hex_id OR EXISTS(SELECT 1 FROM map_hex_locations j WHERE j.package_id=l.package_id AND j.location_id=l.location_id AND j.hex_id=h.hex_id))) ORDER BY distance2 LIMIT ?";
        List<JSONObject> rawLocations=queryList(locationSql,new String[]{String.valueOf(ox),String.valueOf(ox),String.valueOf(oy),String.valueOf(oy),packageId,avoid,String.valueOf(perKind)});
        String terrainSql="SELECT h.*,"+distance+" AS distance2 FROM map_hex_atlas h WHERE h.package_id=? AND h.hex_id<>? AND (EXISTS(SELECT 1 FROM map_hex_corrections c WHERE c.map_id=? AND c.hex_id=h.hex_id AND lower(COALESCE(c.terrain_primary,'')) NOT IN ('water','sea','ocean','coast_water','map_border','unassigned','null') AND COALESCE(c.terrain_confidence,0)>=0.60 AND lower(COALESCE(c.review_status,'')) NOT LIKE '%needs%' AND lower(COALESCE(c.review_status,'')) NOT LIKE '%unassigned%') OR (lower(COALESCE(h.terrain_primary,'')) NOT IN ('','water','sea','ocean','coast_water','map_border','unassigned','null') AND COALESCE(h.terrain_confidence,0)>=0.60 AND lower(COALESCE(h.review_status,'')) NOT LIKE '%needs%' AND lower(COALESCE(h.review_status,'')) NOT LIKE '%unassigned%')) ORDER BY distance2 LIMIT ?";
        List<JSONObject> rawTerrains=queryList(terrainSql,new String[]{String.valueOf(ox),String.valueOf(ox),String.valueOf(oy),String.valueOf(oy),packageId,avoid,mapId,String.valueOf(perKind)});
        HashSet<String> seen=new HashSet<String>();
        for(JSONObject raw:rawLocations){String id=raw.optString("hex_id","");JSONObject enriched=enrichAdventureStartCandidate(packageId,mergeMapHex(raw,getMapHexCorrection(packageId,id)));if(enriched.length()==0||!seen.add(id))continue;try{enriched.put("_distance2",raw.optDouble("distance2",Double.MAX_VALUE));out.add(enriched);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada ao ordenar local de abertura.",ignored);}}
        for(JSONObject raw:rawTerrains){String id=raw.optString("hex_id","");if(seen.contains(id))continue;JSONObject enriched=enrichAdventureStartCandidate(packageId,mergeMapHex(raw,getMapHexCorrection(packageId,id)));if(enriched.length()==0||!seen.add(id))continue;try{enriched.put("_distance2",raw.optDouble("distance2",Double.MAX_VALUE));out.add(enriched);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada ao ordenar terreno de abertura.",ignored);}}
        java.util.Collections.sort(out,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return Double.compare(a.optDouble("_distance2",Double.MAX_VALUE),b.optDouble("_distance2",Double.MAX_VALUE));}});
        return out.size()>wanted?new ArrayList<JSONObject>(out.subList(0,wanted)):out;
    }

    public List<JSONObject> findNearestConfirmedLocations(String packageId,String fromHex,String[] types,int limit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();if(packageId==null||packageId.length()==0||fromHex==null||fromHex.length()==0||types==null||types.length==0)return out;JSONObject origin=getRawMapHex(packageId,fromHex);if(origin.length()==0)return out;double x=origin.optDouble("center_x"),y=origin.optDouble("center_y");StringBuilder sql=new StringBuilder("SELECT l.*,h.center_x,h.center_y,((h.center_x-?)*(h.center_x-?)+(h.center_y-?)*(h.center_y-?)) AS distance2 FROM map_locations l INNER JOIN map_hex_atlas h ON h.package_id=l.package_id AND h.hex_id=l.hex_id WHERE l.package_id=? AND l.hex_id<>? AND l.type IN (");ArrayList<String> args=new ArrayList<String>();args.add(String.valueOf(x));args.add(String.valueOf(x));args.add(String.valueOf(y));args.add(String.valueOf(y));args.add(packageId);args.add(fromHex);for(int i=0;i<types.length;i++){if(i>0)sql.append(',');sql.append('?');args.add(types[i]);}sql.append(") AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%' AND COALESCE(l.hex_id,'')<>'' ORDER BY distance2 LIMIT ?");args.add(String.valueOf(Math.max(1,limit)));return queryList(sql.toString(),args.toArray(new String[args.size()]));
    }
    public List<JSONObject> findNearestConfirmedWaterways(String packageId,String fromHex,int limit){
        if(packageId==null||packageId.length()==0||fromHex==null||fromHex.length()==0)return new ArrayList<JSONObject>();JSONObject origin=getRawMapHex(packageId,fromHex);if(origin.length()==0)return new ArrayList<JSONObject>();double x=origin.optDouble("center_x"),y=origin.optDouble("center_y");String sql="SELECT *,((center_x-?)*(center_x-?)+(center_y-?)*(center_y-?)) AS distance2 FROM map_hex_atlas WHERE package_id=? AND hex_id<>? AND (lower(COALESCE(review_status,''))='verified' OR (lower(COALESCE(review_status,''))='automatic' AND COALESCE(terrain_confidence,0)>=0.80)) AND (lower(COALESCE(waterways_json,'')) LIKE '%river%' OR lower(COALESCE(waterways_json,'')) LIKE '%rio%') ORDER BY distance2 LIMIT ?";return queryList(sql,new String[]{String.valueOf(x),String.valueOf(x),String.valueOf(y),String.valueOf(y),packageId,fromHex,String.valueOf(Math.max(1,limit))});
    }
    public List<JSONObject> findNearestConfirmedTerrain(String packageId,String fromHex,String terrain,int limit){
        if(packageId==null||packageId.length()==0||fromHex==null||fromHex.length()==0||terrain==null||terrain.length()==0)return new ArrayList<JSONObject>();JSONObject origin=getRawMapHex(packageId,fromHex);if(origin.length()==0)return new ArrayList<JSONObject>();double x=origin.optDouble("center_x"),y=origin.optDouble("center_y");String sql="SELECT *,((center_x-?)*(center_x-?)+(center_y-?)*(center_y-?)) AS distance2 FROM map_hex_atlas WHERE package_id=? AND hex_id<>? AND (lower(COALESCE(review_status,''))='verified' OR (lower(COALESCE(review_status,''))='automatic' AND COALESCE(terrain_confidence,0)>=0.80)) AND (lower(COALESCE(terrain_primary,''))=? OR lower(COALESCE(terrain_secondary,'')) LIKE ?) ORDER BY distance2 LIMIT ?";return queryList(sql,new String[]{String.valueOf(x),String.valueOf(x),String.valueOf(y),String.valueOf(y),packageId,fromHex,terrain.toLowerCase(),"%"+terrain.toLowerCase()+"%",String.valueOf(Math.max(1,limit))});
    }
    public void saveMapHexCorrection(String packageId,String hexId,JSONObject correction){String mapId=getMapIdForPackage(packageId);if(mapId.length()==0||hexId==null||hexId.length()==0)return;ContentValues v=new ContentValues();v.put("map_id",mapId);v.put("hex_id",hexId);v.put("terrain_primary",correction.optString("terrain_primary"));v.put("terrain_secondary",correction.optString("terrain_secondary","[]"));v.put("terrain_confidence",correction.optDouble("terrain_confidence",1));v.put("roads_json",correction.optString("roads_json","[]"));v.put("waterways_json",correction.optString("waterways_json","[]"));v.put("political_json",correction.optString("political_json","{}"));v.put("review_status",correction.optString("review_status","verified"));v.put("review_reasons",correction.optString("review_reasons","[]"));v.put("custom_locations_json",correction.optString("custom_locations_json","[]"));v.put("hide_unconfirmed_locations",correction.optInt("hide_unconfirmed_locations",0));v.put("review_note",correction.optString("review_note"));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("map_hex_corrections",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public void deleteMapHexCorrection(String packageId,String hexId){String mapId=getMapIdForPackage(packageId);if(mapId.length()>0)getWritableDatabase().delete("map_hex_corrections","map_id=? AND hex_id=?",new String[]{mapId,hexId});}
    public JSONObject getMapHexNote(String packageId,String hexId){
        String mapId=getMapIdForPackage(packageId);if(mapId.length()==0||hexId==null||hexId.length()==0)return new JSONObject();
        JSONObject note=queryOne("SELECT * FROM map_hex_notes WHERE world_profile=? AND map_id=? AND hex_id=?",new String[]{getActiveMapProfile(),mapId,hexId});
        if(note.length()>0){try{String legacy=note.optString("note_text","").trim();String title=note.optString("note_title","").trim();String details=note.optString("note_details","").trim();if(title.length()==0&&details.length()==0&&legacy.length()>0)note.put("note_details",legacy);if(note.optString("icon","").trim().length()==0)note.put("icon","✎");}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}}
        return note;
    }
    public void saveMapHexNote(String packageId,String hexId,String note,String createdBy){saveMapHexNote(packageId,hexId,"",note,"✎",createdBy);}
    public void saveMapHexNote(String packageId,String hexId,String title,String details,String icon,String createdBy){
        String mapId=getMapIdForPackage(packageId);if(mapId.length()==0||hexId==null||hexId.length()==0)return;
        String cleanTitle=title==null?"":title.trim();String cleanDetails=details==null?"":details.trim();String cleanIcon=icon==null||icon.trim().length()==0?"✎":icon.trim();
        String combined=cleanTitle.length()>0&&cleanDetails.length()>0?cleanTitle+"\n"+cleanDetails:(cleanTitle.length()>0?cleanTitle:cleanDetails);
        if(combined.length()==0){deleteMapHexNote(packageId,hexId);return;}
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("map_id",mapId);v.put("hex_id",hexId);v.put("note_text",combined);v.put("note_title",cleanTitle);v.put("note_details",cleanDetails);v.put("icon",cleanIcon);v.put("created_by",createdBy==null||createdBy.trim().length()==0?"player":createdBy.trim());long now=System.currentTimeMillis();JSONObject existing=getMapHexNote(packageId,hexId);v.put("created_at",existing.optLong("created_at",now));v.put("updated_at",now);getWritableDatabase().insertWithOnConflict("map_hex_notes",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public void deleteMapHexNote(String packageId,String hexId){String mapId=getMapIdForPackage(packageId);if(mapId.length()>0)getWritableDatabase().delete("map_hex_notes","world_profile=? AND map_id=? AND hex_id=?",new String[]{getActiveMapProfile(),mapId,hexId});}
    public List<JSONObject> getMapHexNotesForPackage(String packageId){String mapId=getMapIdForPackage(packageId);if(mapId.length()==0)return new ArrayList<JSONObject>();return queryList("SELECT * FROM map_hex_notes WHERE world_profile=? AND map_id=? AND note_text<>'' ORDER BY updated_at DESC",new String[]{getActiveMapProfile(),mapId});}
    public List<JSONObject> listAllMapHexNotes(){return queryList("SELECT * FROM map_hex_notes ORDER BY world_profile,map_id,hex_id",null);}
    public List<JSONObject> listMapHexNotes(String worldProfile,String mapId){if(mapId==null||mapId.length()==0)return new ArrayList<JSONObject>();return queryList("SELECT * FROM map_hex_notes WHERE world_profile=? AND map_id=? ORDER BY hex_id",new String[]{normalizeMapProfile(worldProfile),mapId});}
    public int countMapCorrections(String packageId){String mapId=getMapIdForPackage(packageId);if(mapId.length()==0)return 0;Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM map_hex_corrections WHERE map_id=?",new String[]{mapId});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public JSONObject exportMapCorrections(String packageId)throws Exception{String mapId=getMapIdForPackage(packageId);if(mapId.length()==0)throw new Exception("Nenhum pacote de mapa ativo.");JSONObject root=new JSONObject();root.put("format","braseiro-map-corrections");root.put("version",1);root.put("mapId",mapId);root.put("exportedAt",System.currentTimeMillis());root.put("corrections",new JSONArray(queryList("SELECT * FROM map_hex_corrections WHERE map_id=? ORDER BY hex_id",new String[]{mapId})));return root;}
    public int importMapCorrections(String packageId,JSONObject root)throws Exception{if(!"braseiro-map-corrections".equals(root.optString("format")))throw new Exception("JSON de revisões incompatível.");String mapId=getMapIdForPackage(packageId);if(mapId.length()==0)throw new Exception("Nenhum pacote de mapa ativo.");String sourceMapId=root.optString("mapId");if(sourceMapId.length()>0&&!mapId.equals(sourceMapId))throw new Exception("As revisões pertencem ao mapa "+sourceMapId+", não ao mapa ativo "+mapId+".");JSONArray list=root.optJSONArray("corrections");if(list==null)return 0;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();int count=0;try{for(int i=0;i<list.length();i++){JSONObject c=list.optJSONObject(i);if(c==null||c.optString("hex_id").length()==0)continue;ContentValues v=new ContentValues();v.put("map_id",mapId);v.put("hex_id",c.optString("hex_id"));v.put("terrain_primary",c.optString("terrain_primary"));v.put("terrain_secondary",c.optString("terrain_secondary","[]"));v.put("terrain_confidence",c.optDouble("terrain_confidence",1));v.put("roads_json",c.optString("roads_json","[]"));v.put("waterways_json",c.optString("waterways_json","[]"));v.put("political_json",c.optString("political_json","{}"));v.put("review_status",c.optString("review_status","verified"));v.put("review_reasons",c.optString("review_reasons","[]"));v.put("custom_locations_json",c.optString("custom_locations_json","[]"));v.put("hide_unconfirmed_locations",c.optInt("hide_unconfirmed_locations",0));v.put("review_note",c.optString("review_note"));v.put("updated_at",System.currentTimeMillis());db.insertWithOnConflict("map_hex_corrections",null,v,SQLiteDatabase.CONFLICT_REPLACE);count++;}db.setTransactionSuccessful();}finally{db.endTransaction();}return count;}
    public List<JSONObject> listAllMapCorrections(){return queryList("SELECT * FROM map_hex_corrections ORDER BY map_id,hex_id",null);}
    public List<JSONObject> listMapCorrections(String mapId){if(mapId==null||mapId.length()==0)return new ArrayList<JSONObject>();return queryList("SELECT * FROM map_hex_corrections WHERE map_id=? ORDER BY hex_id",new String[]{mapId});}
    public void importAllMapCorrections(JSONArray list){if(list==null)return;SQLiteDatabase db=getWritableDatabase();for(int i=0;i<list.length();i++){JSONObject c=list.optJSONObject(i);if(c==null)continue;ContentValues v=new ContentValues();v.put("map_id",c.optString("map_id"));v.put("hex_id",c.optString("hex_id"));v.put("terrain_primary",c.optString("terrain_primary"));v.put("terrain_secondary",c.optString("terrain_secondary","[]"));v.put("terrain_confidence",c.optDouble("terrain_confidence",1));v.put("roads_json",c.optString("roads_json","[]"));v.put("waterways_json",c.optString("waterways_json","[]"));v.put("political_json",c.optString("political_json","{}"));v.put("review_status",c.optString("review_status","verified"));v.put("review_reasons",c.optString("review_reasons","[]"));v.put("custom_locations_json",c.optString("custom_locations_json","[]"));v.put("hide_unconfirmed_locations",c.optInt("hide_unconfirmed_locations",0));v.put("review_note",c.optString("review_note"));v.put("updated_at",c.optLong("updated_at",System.currentTimeMillis()));if(c.optString("map_id").length()>0&&c.optString("hex_id").length()>0)db.insertWithOnConflict("map_hex_corrections",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
    public void deleteMapPackage(String packageId){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("map_hex_locations","package_id=?",new String[]{packageId});db.delete("map_locations","package_id=?",new String[]{packageId});db.delete("map_nations","package_id=?",new String[]{packageId});db.delete("map_metaregions","package_id=?",new String[]{packageId});db.delete("map_hex_atlas","package_id=?",new String[]{packageId});db.delete("map_packages","package_id=?",new String[]{packageId});db.setTransactionSuccessful();}finally{db.endTransaction();}}


    public void upsertToken(String driveId,String name,JSONObject classification,String mime,long size,String modified,String thumbnail,String source,long seen){upsertTokenSynced(driveId,name,classification,mime,size,modified,thumbnail,source,"",seen);}
    /** Retorna 1 para novo, 2 para alterado e 0 para inalterado. */
    public int upsertTokenSynced(String driveId,String name,JSONObject classification,String mime,long size,String modified,String thumbnail,String source,String sourceRoot,long seen){
        JSONObject before=queryOne("SELECT id,size_bytes,modified_time FROM tokens WHERE drive_id=?",new String[]{driveId});boolean exists=before.length()>0;boolean changed=exists&&(before.optLong("size_bytes")!=size||!safe(before.optString("modified_time")).equals(safe(modified)));
        ContentValues v=new ContentValues();v.put("drive_id",driveId);v.put("name",name);v.put("label",classification.optString("label",name));v.put("species",classification.optString("species","Não identificada"));v.put("creature_type",classification.optString("creatureType","Não identificado"));v.put("role",classification.optString("role","Geral"));v.put("category",classification.optString("category","Criatura"));v.put("tags",classification.optJSONArray("tags")==null?"[]":classification.optJSONArray("tags").toString());v.put("mime_type",mime);v.put("size_bytes",size);v.put("modified_time",modified);v.put("thumbnail_link",thumbnail);v.put("source",source);v.put("source_root",sourceRoot==null?"":sourceRoot);v.put("last_seen",seen);v.put("enabled",1);
        SQLiteDatabase sql=getWritableDatabase();long r=sql.insertWithOnConflict("tokens",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(r==-1)sql.update("tokens",v,"drive_id=?",new String[]{driveId});return !exists?1:(changed?2:0);
    }
    public int disableMissingTokens(String sourceRoot,long seen){ContentValues v=new ContentValues();v.put("enabled",0);return getWritableDatabase().update("tokens",v,"source_root=? AND last_seen<>?",new String[]{sourceRoot==null?"":sourceRoot,String.valueOf(seen)});}
    public List<JSONObject> listTokens(String filter,String species){
        String f=filter==null?"":filter.trim();String sp=species==null?"":species.trim();ArrayList<String> args=new ArrayList<String>();StringBuilder sql=new StringBuilder("SELECT * FROM tokens WHERE enabled=1");
        if(f.length()>0){sql.append(" AND (label LIKE ? OR name LIKE ? OR species LIKE ? OR creature_type LIKE ? OR role LIKE ? OR tags LIKE ?)");for(int i=0;i<6;i++)args.add("%"+f+"%");}
        if(sp.length()>0&&!"Todas".equals(sp)){sql.append(" AND species=?");args.add(sp);}sql.append(" ORDER BY species COLLATE NOCASE, role COLLATE NOCASE, label COLLATE NOCASE");return queryList(sql.toString(),args.toArray(new String[args.size()]));
    }
    public List<String> listTokenSpecies(){ArrayList<String> out=new ArrayList<String>();Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT species FROM tokens WHERE enabled=1 ORDER BY species COLLATE NOCASE",null);try{while(c.moveToNext())out.add(c.getString(0));}finally{c.close();}return out;}
    public JSONObject getToken(long id){return queryOne("SELECT * FROM tokens WHERE id=?",new String[]{String.valueOf(id)});}
    public JSONObject getTokenByDriveId(String driveId){return queryOne("SELECT * FROM tokens WHERE drive_id=?",new String[]{driveId==null?"":driveId});}
    public JSONObject findLocalTokenForScene(String name){
        String q=name==null?"":name.trim();if(q.length()==0)return new JSONObject();
        String low=q.toLowerCase();
        JSONObject exact=queryOne("SELECT * FROM tokens WHERE enabled=1 AND COALESCE(local_uri,'')<>'' AND (lower(label)=? OR lower(name)=?) ORDER BY last_seen DESC LIMIT 1",new String[]{low,low});
        if(exact.length()>0)return exact;
        return queryOne("SELECT * FROM tokens WHERE enabled=1 AND COALESCE(local_uri,'')<>'' AND (lower(label) LIKE ? OR lower(name) LIKE ? OR lower(tags) LIKE ?) ORDER BY last_seen DESC LIMIT 1",new String[]{"%"+low+"%","%"+low+"%","%"+low+"%"});
    }
    public void setTokenLocalUri(long id,String uri){ContentValues v=new ContentValues();v.put("local_uri",uri);getWritableDatabase().update("tokens",v,"id=?",new String[]{String.valueOf(id)});}
    public int countTokens(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tokens WHERE enabled=1",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}


    public long upsertTokenPdf(String sourceUri,String documentId,String name,JSONObject classification,long size,long modified,long seen){return upsertTokenPdfSynced(sourceUri,documentId,name,classification,size,modified,"",seen);}
    public long upsertTokenPdfSynced(String sourceUri,String documentId,String name,JSONObject classification,long size,long modified,String sourceRoot,long seen){
        JSONObject before=queryOne("SELECT id,size_bytes,modified_time FROM token_pdfs WHERE source_uri=?",new String[]{sourceUri});long previousId=before.optLong("id",-1);boolean changed=previousId>0&&(before.optLong("size_bytes")!=size||before.optLong("modified_time")!=modified);
        ContentValues v=new ContentValues();v.put("source_uri",sourceUri);v.put("document_id",documentId);v.put("name",name);v.put("label",classification.optString("label",name));v.put("group_key",classification.optString("groupKey","nao-classificados"));v.put("category",classification.optString("category","Não classificados"));v.put("class_name",classification.optString("className",""));v.put("species",classification.optString("species",""));v.put("set_number",classification.optInt("setNumber",0));v.put("size_bytes",size);v.put("modified_time",modified);v.put("source_root",sourceRoot==null?"":sourceRoot);v.put("last_seen",seen);v.put("enabled",1);if(changed){v.put("page_count",-1);v.put("processed_pages","[]");}
        SQLiteDatabase db=getWritableDatabase();long id=db.insertWithOnConflict("token_pdfs",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1){db.update("token_pdfs",v,"source_uri=?",new String[]{sourceUri});id=previousId>0?previousId:queryOne("SELECT id FROM token_pdfs WHERE source_uri=?",new String[]{sourceUri}).optLong("id",-1);}if(changed&&id>0)db.delete("pdf_tokens","pdf_id=?",new String[]{String.valueOf(id)});return id;
    }

    /** Desativa entradas criadas pela versão que embrulhou URIs SAF como gdrive://content... */
    public void disableBrokenTokenPdfSources(){
        ContentValues v=new ContentValues();v.put("enabled",0);
        getWritableDatabase().update("token_pdfs",v,"source_uri LIKE ? OR source_uri LIKE ?",new String[]{"gdrive://content%","gdrive://com.google%"});
    }

    public void disableMissingTokenPdfs(long seen){ContentValues v=new ContentValues();v.put("enabled",0);getWritableDatabase().update("token_pdfs",v,"last_seen<>?",new String[]{String.valueOf(seen)});}
    public int disableMissingTokenPdfs(String sourceRoot,long seen){ContentValues v=new ContentValues();v.put("enabled",0);return getWritableDatabase().update("token_pdfs",v,"source_root=? AND last_seen<>?",new String[]{sourceRoot==null?"":sourceRoot,String.valueOf(seen)});}
    public List<JSONObject> listTokenPdfs(String filter,String group){String f=filter==null?"":filter.trim();String g=group==null?"":group.trim();ArrayList<String> args=new ArrayList<String>();StringBuilder sql=new StringBuilder("SELECT * FROM token_pdfs WHERE enabled=1");if(f.length()>0){sql.append(" AND (label LIKE ? OR name LIKE ? OR category LIKE ? OR class_name LIKE ? OR species LIKE ?)");for(int i=0;i<5;i++)args.add("%"+f+"%");}if(g.length()>0&&!"Todos".equals(g)){sql.append(" AND group_key=?");args.add(g);}sql.append(" ORDER BY category COLLATE NOCASE,class_name COLLATE NOCASE,set_number,name COLLATE NOCASE");return queryList(sql.toString(),args.toArray(new String[args.size()]));}
    public List<JSONObject> listTokenPdfGroups(){return queryList("SELECT group_key,category,class_name,species,COUNT(*) AS pdf_count,SUM(CASE WHEN page_count>0 THEN page_count ELSE 0 END) AS page_count FROM token_pdfs WHERE enabled=1 GROUP BY group_key,category,class_name,species ORDER BY category COLLATE NOCASE,class_name COLLATE NOCASE",null);}
    public JSONObject getTokenPdf(long id){return queryOne("SELECT * FROM token_pdfs WHERE id=?",new String[]{String.valueOf(id)});}
    public JSONObject getTokenPdfBySource(String sourceUri){return queryOne("SELECT * FROM token_pdfs WHERE source_uri=?",new String[]{sourceUri});}
    public int countTokenPdfs(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM token_pdfs WHERE enabled=1",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public int countPdfTokens(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM pdf_tokens",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public void updateTokenPdfPageCount(long id,int pageCount){ContentValues v=new ContentValues();v.put("page_count",pageCount);getWritableDatabase().update("token_pdfs",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateTokenPdfLayout(long id,int columns,int rows,double marginX,double marginY){updateTokenPdfLayoutBounds(id,columns,rows,marginX,marginY,1d-marginX,1d-marginY);}
    public void updateTokenPdfLayoutBounds(long id,int columns,int rows,double left,double top,double right,double bottom){ContentValues v=new ContentValues();v.put("columns_count",Math.max(1,columns));v.put("rows_count",Math.max(1,rows));left=Math.max(0,Math.min(.92,left));top=Math.max(0,Math.min(.92,top));right=Math.max(left+.05,Math.min(1,right));bottom=Math.max(top+.05,Math.min(1,bottom));v.put("grid_left",left);v.put("grid_top",top);v.put("grid_right",right);v.put("grid_bottom",bottom);v.put("margin_x",Math.min(left,1-right));v.put("margin_y",Math.min(top,1-bottom));getWritableDatabase().update("token_pdfs",v,"id=?",new String[]{String.valueOf(id)});getWritableDatabase().delete("pdf_tokens","pdf_id=?",new String[]{String.valueOf(id)});}
    public void replacePdfPageTokens(long pdfId,int pageNo,List<JSONObject> tokens){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("pdf_tokens","pdf_id=? AND page_no=?",new String[]{String.valueOf(pdfId),String.valueOf(pageNo)});for(JSONObject t:tokens){ContentValues v=new ContentValues();v.put("pdf_id",pdfId);v.put("page_no",pageNo);v.put("token_index",t.optInt("tokenIndex"));v.put("row_no",t.optInt("row"));v.put("col_no",t.optInt("column"));v.put("crop_x",t.optDouble("x"));v.put("crop_y",t.optDouble("y"));v.put("crop_w",t.optDouble("width"));v.put("crop_h",t.optDouble("height"));v.put("thumb_path",t.optString("thumbPath"));v.put("last_used",System.currentTimeMillis());db.insertWithOnConflict("pdf_tokens",null,v,SQLiteDatabase.CONFLICT_REPLACE);}JSONObject p=getTokenPdf(pdfId);String pages=p.optString("processed_pages","[]");try{JSONArray a=new JSONArray(pages);boolean found=false;for(int i=0;i<a.length();i++)if(a.optInt(i)==pageNo)found=true;if(!found)a.put(pageNo);ContentValues u=new ContentValues();u.put("processed_pages",a.toString());db.update("token_pdfs",u,"id=?",new String[]{String.valueOf(pdfId)});}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public List<JSONObject> listPdfPageTokens(long pdfId,int pageNo){return queryList("SELECT * FROM pdf_tokens WHERE pdf_id=? AND page_no=? ORDER BY token_index",new String[]{String.valueOf(pdfId),String.valueOf(pageNo)});}
    public JSONObject getPdfToken(long id){return queryOne("SELECT * FROM pdf_tokens WHERE id=?",new String[]{String.valueOf(id)});}
    public void setPdfTokenFullPath(long id,String path,boolean assigned){ContentValues v=new ContentValues();v.put("full_path",path);v.put("assigned",assigned?1:0);v.put("last_used",System.currentTimeMillis());getWritableDatabase().update("pdf_tokens",v,"id=?",new String[]{String.valueOf(id)});}
    public void setPdfTokenFavorite(long id,boolean favorite){ContentValues v=new ContentValues();v.put("favorite",favorite?1:0);v.put("last_used",System.currentTimeMillis());getWritableDatabase().update("pdf_tokens",v,"id=?",new String[]{String.valueOf(id)});}

    public long addAuditEvent(String category,String title,String summary,String beforeJson,String afterJson,boolean reversible){
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("category",category==null?"geral":category);v.put("title",title==null?"Evento":title);v.put("summary",summary==null?"":summary);v.put("before_json",beforeJson==null?"{}":beforeJson);v.put("after_json",afterJson==null?"{}":afterJson);v.put("reversible",reversible?1:0);v.put("created_at",System.currentTimeMillis());
        long id=getWritableDatabase().insertOrThrow("audit_log",null,v);
        getWritableDatabase().execSQL("DELETE FROM audit_log WHERE world_profile=? AND id NOT IN (SELECT id FROM audit_log WHERE world_profile=? ORDER BY created_at DESC LIMIT 1000)",new Object[]{getActiveMapProfile(),getActiveMapProfile()});
        return id;
    }
    public List<JSONObject> listAuditEvents(int limit){return queryList("SELECT * FROM audit_log WHERE world_profile=? ORDER BY created_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public int countAuditEvents(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM audit_log WHERE world_profile=?",new String[]{getActiveMapProfile()});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}

    public JSONObject getSceneState(){ensureWorldProfileData(activeMapProfile);return queryOne("SELECT * FROM scene_state_profiles WHERE world_profile=?",new String[]{getActiveMapProfile()});}
    public void saveSceneState(JSONObject scene){
        if(scene==null)return;ContentValues v=new ContentValues();
        v.put("day_number",Math.max(1,scene.optInt("day_number",1)));v.put("clock_hour",normalizeClockHour(scene.optDouble("clock_hour",clockHourFromLabel(scene.optString("time_of_day","Manhã")))));v.put("location",scene.optString("location"));v.put("hex_id",scene.optString("hex_id"));v.put("time_of_day",scene.optString("time_of_day"));v.put("weather",scene.optString("weather"));v.put("lighting",scene.optString("lighting"));v.put("danger",scene.optString("danger"));v.put("objective",scene.optString("objective"));
        v.put("present_npcs_json",jsonValue(scene,"present_npcs","present_npcs_json","[]"));v.put("threats_json",jsonValue(scene,"threats","threats_json","[]"));v.put("clues_json",jsonValue(scene,"clues","clues_json","[]"));v.put("pending_consequences_json",jsonValue(scene,"pending_consequences","pending_consequences_json","[]"));v.put("updated_at",System.currentTimeMillis());
        getWritableDatabase().update("scene_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});
    }
    private static double normalizeClockHour(double value){double hour=value%24d;if(hour<0)hour+=24d;return hour;}
    private static double clockHourFromLabel(String value){String n=value==null?"":value.toLowerCase(java.util.Locale.ROOT);if(n.contains("madrugada"))return 3d;if(n.contains("amanhe"))return 6d;if(n.contains("manhã")||n.contains("manha"))return 9d;if(n.contains("meio-dia")||n.contains("meio dia"))return 12d;if(n.contains("tarde"))return 15d;if(n.contains("entard"))return 18d;if(n.contains("noite"))return 21d;return 9d;}
    private String jsonValue(JSONObject source,String objectKey,String storedKey,String fallback){Object value=source.opt(objectKey);if(value!=null&&value!=JSONObject.NULL)return String.valueOf(value);return source.optString(storedKey,fallback);}
    public void upsertNpcMemory(JSONObject npc){
        JSONObject normalized=NpcIdentityNormalizer.normalize(npc,getActiveMapProfile());
        if(normalized.length()==0)return;
        String profile=getActiveMapProfile(),name=normalized.optString("name").trim(),stableId=normalized.optString("stable_id",NpcIdentityNormalizer.stableId(profile,name));
        JSONObject previous=findNpcMemoryByName(name);
        ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("stable_id",stableId);v.put("name",name);
        putNpcField(v,normalized,previous,"summary","");putNpcField(v,normalized,previous,"goals","");putNpcField(v,normalized,previous,"secrets","");putNpcField(v,normalized,previous,"attitude","neutra");putNpcField(v,normalized,previous,"relationship","");putNpcField(v,normalized,previous,"knowledge","");putNpcField(v,normalized,previous,"debts","");putNpcField(v,normalized,previous,"last_seen","");putNpcField(v,normalized,previous,"token_uri","");putNpcField(v,normalized,previous,"visual_description","");putNpcField(v,normalized,previous,"stats_json","{}");putNpcField(v,normalized,previous,"hex_id","");putNpcField(v,normalized,previous,"voice_profile","");
        v.put("active",normalized.has("active")?normalized.optInt("active",1):previous.optInt("active",1));v.put("updated_at",System.currentTimeMillis());
        long id=getWritableDatabase().insertWithOnConflict("npc_memory_profiles",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1){if(previous.length()>0)getWritableDatabase().update("npc_memory_profiles",v,"id=?",new String[]{String.valueOf(previous.optLong("id"))});else getWritableDatabase().update("npc_memory_profiles",v,"world_profile=? AND stable_id=?",new String[]{profile,stableId});}
        ensureNpcPersonalityProvisioned(name);
    }
    private static void putNpcField(ContentValues v,JSONObject source,JSONObject previous,String key,String fallback){v.put(key,source.has(key)?source.optString(key,fallback):previous.optString(key,fallback));}
    public JSONArray listNpcMemories(int limit){return new JSONArray(queryList("SELECT * FROM npc_memory_profiles WHERE world_profile=? ORDER BY active DESC, updated_at DESC LIMIT "+Math.max(1,Math.min(5000,limit)),new String[]{getActiveMapProfile()}));}
    public JSONObject getNpcMemory(long id){return queryOne("SELECT * FROM npc_memory_profiles WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public JSONObject findNpcMemoryByName(String name){String safe=NpcIdentityNormalizer.displayName(name);if(safe.length()==0||NpcIdentityNormalizer.isReservedPlayerAlias(safe))return new JSONObject();return queryOne("SELECT * FROM npc_memory_profiles WHERE world_profile=? AND (stable_id=? OR name=? COLLATE NOCASE) LIMIT 1",new String[]{getActiveMapProfile(),NpcIdentityNormalizer.stableId(getActiveMapProfile(),safe),safe});}
    public void setNpcToken(long id,String uri){
        String safe=uri==null?"":uri.trim();JSONObject npc=queryOne("SELECT name FROM npc_memory_profiles WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});ContentValues v=new ContentValues();v.put("token_uri",safe);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("npc_memory_profiles",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});
        ContentValues combat=new ContentValues();combat.put("source_kind","npc");combat.put("source_id",id);combat.put("token_uri",safe);
        getWritableDatabase().update("combatants",combat,"world_profile=? AND (source_kind='npc' AND source_id=? OR is_player=0 AND lower(name)=lower(?))",new String[]{getActiveMapProfile(),String.valueOf(id),npc.optString("name","")});
    }
    public void setNpcActive(long id,boolean active){ContentValues v=new ContentValues();v.put("active",active?1:0);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("npc_memory_profiles",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void deleteNpcMemory(long id){getWritableDatabase().delete("npc_memory_profiles","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public JSONArray listKnownNpcs(int limit){return listNpcMemories(limit);}
    public JSONArray listSceneNpcs(int limit){
        JSONArray out=new JSONArray();java.util.HashSet<String> seen=new java.util.HashSet<String>();JSONObject scene=getSceneState();JSONArray raw;try{raw=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception e){raw=new JSONArray();}
        for(int i=0;i<raw.length()&&out.length()<Math.max(1,limit);i++){String name=NpcIdentityNormalizer.displayName(raw.opt(i));if(NpcIdentityNormalizer.isReservedPlayerAlias(name))continue;JSONObject npc=findNpcMemoryByName(name);if(npc.length()==0){try{npc=new JSONObject().put("name",name).put("stable_id",NpcIdentityNormalizer.stableId(getActiveMapProfile(),name)).put("active",1);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada registrada na consolidação 5.0.",ignored);}}String key=npc.optString("stable_id",NpcIdentityNormalizer.key(name));if(!seen.add(key))continue;out.put(npc);}
        return out;
    }
    public JSONArray listWorldActiveNpcs(int limit){
        String sql="SELECT n.*,c.emotion,c.trust,c.fear,c.suspicion,c.urgency,c.next_action,c.schedule_json,c.faction,c.current_location FROM npc_memory_profiles n LEFT JOIN npc_cognitive_state c ON c.world_profile=n.world_profile AND lower(c.npc_name)=lower(n.name) WHERE n.world_profile=? AND n.active=1 AND (COALESCE(c.next_action,'')<>'' OR COALESCE(c.schedule_json,'[]')<>'[]' OR COALESCE(c.faction,'')<>'' OR COALESCE(c.urgency,0)>0) ORDER BY COALESCE(c.urgency,0) DESC,n.updated_at DESC LIMIT "+Math.max(1,Math.min(1000,limit));return new JSONArray(queryList(sql,new String[]{getActiveMapProfile()}));
    }


    /** Persiste somente estado avançado válido de combate PF2e. As três colunas legadas
     * concentration/death_success/death_fail continuam no schema apenas para migração de
     * instalações antigas e são sempre neutralizadas aqui. Dying/Wounded/Doomed e dano
     * persistente vivem em resources_json com system=pathfinder_2e. */
    public void updateCombatantPf2Advanced(long id,int tempHp,boolean reactionUsed,String conditions,String timersJson,String resistances,String weaknesses,String immunities,String resourcesJson){
        ContentValues v=new ContentValues();v.put("temp_hp",Math.max(0,tempHp));v.put("reaction_used",reactionUsed?1:0);v.put("conditions",conditions==null?"":conditions);v.put("condition_timers_json",validJsonArray(timersJson));v.put("resistances",resistances==null?"":resistances);v.put("vulnerabilities",weaknesses==null?"":weaknesses);v.put("immunities",immunities==null?"":immunities);JSONObject state=parseJsonObject(resourcesJson);try{state.put("system","pathfinder_2e");}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao marcar estado PF2e.",ignored);}v.put("resources_json",state.toString());v.put("concentration","");v.put("death_success",0);v.put("death_fail",0);getWritableDatabase().update("combatants",v,"id=?",new String[]{String.valueOf(id)});
    }
    private String validJsonArray(String raw){try{return new JSONArray(raw==null?"[]":raw).toString();}catch(Exception e){return "[]";}}
    private String validJsonObject(String raw){try{return new JSONObject(raw==null?"{}":raw).toString();}catch(Exception e){return "{}";}}


    // ===== Mestre virtual e companhia (3.3.0) =====
    public JSONObject getPartyState(){createVirtualMasterTables(getWritableDatabase());return queryOne("SELECT * FROM party_state_profiles WHERE world_profile=?",new String[]{getActiveMapProfile()});}
    public void savePartyState(JSONObject state){if(state==null)return;ContentValues v=new ContentValues();v.put("marching_order_json",validJsonArray(state.optString("marching_order_json",String.valueOf(state.opt("marching_order")))));v.put("roles_json",validJsonObject(state.optString("roles_json",String.valueOf(state.opt("roles")))));v.put("company_resources_json",validJsonObject(state.optString("company_resources_json",String.valueOf(state.opt("company_resources")))));v.put("subgroups_json",validJsonArray(state.optString("subgroups_json",String.valueOf(state.opt("subgroups")))));v.put("relations_json",validJsonArray(state.optString("relations_json",String.valueOf(state.opt("relations")))));v.put("session_preferences_json",validJsonObject(state.optString("session_preferences_json",String.valueOf(state.opt("session_preferences")))));v.put("knowledge_mode",state.optString("knowledge_mode","jogador_total"));v.put("default_control_mode",state.optString("default_control_mode","manual"));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("party_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});}
    public long addPartyAction(long characterId,String characterName,String action,String intent){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("character_id",characterId);v.put("character_name",characterName);v.put("action_text",action);v.put("intent_type",intent);v.put("status","queued");v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("party_action_queue",null,v);}
    public List<JSONObject> listPartyActions(String status,int limit){String where="world_profile=?";ArrayList<String> args=new ArrayList<String>();args.add(getActiveMapProfile());if(status!=null&&status.length()>0){where+=" AND status=?";args.add(status);}return queryList("SELECT * FROM party_action_queue WHERE "+where+" ORDER BY priority DESC,created_at ASC LIMIT "+Math.max(1,Math.min(5000,limit)),args.toArray(new String[0]));}
    public int countPartyActions(String status){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM party_action_queue WHERE world_profile=? AND status=?",new String[]{getActiveMapProfile(),status});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public void deletePartyAction(long id){getWritableDatabase().delete("party_action_queue","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void deletePartyActionsByStatus(String status){getWritableDatabase().delete("party_action_queue","world_profile=? AND status=?",new String[]{getActiveMapProfile(),status});}
    public void markPartyActionsResolved(){ContentValues v=new ContentValues();v.put("status","resolved");v.put("resolved_at",System.currentTimeMillis());getWritableDatabase().update("party_action_queue",v,"world_profile=? AND status='queued'",new String[]{getActiveMapProfile()});}
    private static String normalizeCharacterControlMode(String mode){String value=mode==null?"":mode.trim().toLowerCase(java.util.Locale.US);if("gm".equals(value)||"master".equals(value)||"mestre".equals(value)||"companion".equals(value)||"combat_auto".equals(value)||"automatic".equals(value))return "gm";return "player";}
    public JSONObject getCharacterControl(long characterId){String[] args=new String[]{getActiveMapProfile(),String.valueOf(characterId)};JSONObject o=queryOne("SELECT * FROM character_control_profiles WHERE world_profile=? AND character_id=?",args);if(o.length()>0){String current=o.optString("control_mode","player"),normalized=normalizeCharacterControlMode(current);if(!normalized.equals(current)){ContentValues m=new ContentValues();m.put("control_mode",normalized);getWritableDatabase().update("character_control_profiles",m,"world_profile=? AND character_id=?",args);return queryOne("SELECT * FROM character_control_profiles WHERE world_profile=? AND character_id=?",args);}return o;}ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("character_id",characterId);v.put("control_mode",normalizeCharacterControlMode(getPartyState().optString("default_control_mode","player")));v.put("tactics_json","{}");getWritableDatabase().insertWithOnConflict("character_control_profiles",null,v,SQLiteDatabase.CONFLICT_IGNORE);return queryOne("SELECT * FROM character_control_profiles WHERE world_profile=? AND character_id=?",args);}
    public void saveCharacterControl(long characterId,String mode,String tacticsJson){ContentValues v=new ContentValues();v.put("control_mode",normalizeCharacterControlMode(mode));v.put("tactics_json",validJsonObject(tacticsJson));int changed=getWritableDatabase().update("character_control_profiles",v,"world_profile=? AND character_id=?",new String[]{getActiveMapProfile(),String.valueOf(characterId)});if(changed==0){v.put("world_profile",getActiveMapProfile());v.put("character_id",characterId);getWritableDatabase().insertOrThrow("character_control_profiles",null,v);}}
    public void saveCharacterArc(long characterId,String personalGoal,String status,String notes,String lastChoice){getCharacterControl(characterId);ContentValues v=new ContentValues();v.put("personal_goal",personalGoal==null?"":personalGoal.trim());v.put("arc_status",status==null?"active":status);v.put("arc_notes",notes==null?"":notes.trim());v.put("last_major_choice",lastChoice==null?"":lastChoice.trim());getWritableDatabase().update("character_control_profiles",v,"world_profile=? AND character_id=?",new String[]{getActiveMapProfile(),String.valueOf(characterId)});}
    public void updateSpotlight(java.util.Set<Long> mentioned){long now=System.currentTimeMillis();for(JSONObject h:listActiveCharacters()){long id=h.optLong("id");JSONObject c=getCharacterControl(id);ContentValues v=new ContentValues();int score=c.optInt("spotlight_score",0);if(mentioned!=null&&mentioned.contains(id)){v.put("spotlight_score",score+1);v.put("last_spotlight_at",now);}else v.put("spotlight_score",Math.max(0,score-1));getWritableDatabase().update("character_control_profiles",v,"world_profile=? AND character_id=?",new String[]{getActiveMapProfile(),String.valueOf(id)});}}
    public JSONObject lowestSpotlightCharacter(){return queryOne("SELECT c.*,COALESCE(p.spotlight_score,0) AS spotlight_score,COALESCE(p.last_spotlight_at,0) AS last_spotlight_at,COALESCE(p.personal_goal,'') AS personal_goal,COALESCE(p.arc_status,'active') AS arc_status,COALESCE(p.arc_notes,'') AS arc_notes,COALESCE(p.last_major_choice,'') AS last_major_choice FROM characters c LEFT JOIN character_control_profiles p ON p.world_profile=c.world_profile AND p.character_id=c.id WHERE c.world_profile=? AND c.active=1 ORDER BY COALESCE(p.last_spotlight_at,0) ASC,COALESCE(p.spotlight_score,0) ASC,c.name LIMIT 1",new String[]{getActiveMapProfile()});}
    public long addKnowledgeFact(String subject,String content,String truth,String visibility,String knownByJson,String revealCondition,String source){
        String safeSubject=subject==null?"":subject.trim(),safeContent=content==null?"":content.trim();if(safeContent.length()==0)return -1;String key=Integer.toHexString((safeSubject.toLowerCase(java.util.Locale.ROOT)+"|"+safeContent.toLowerCase(java.util.Locale.ROOT)).hashCode());JSONObject previous=queryOne("SELECT * FROM knowledge_facts WHERE world_profile=? AND fact_key=? LIMIT 1",new String[]{getActiveMapProfile(),key});ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("fact_key",key);v.put("subject",safeSubject);v.put("content",safeContent);v.put("truth_status",truth==null?"verdade":truth);v.put("visibility",visibility==null?"grupo":visibility);v.put("known_by_json",validJsonArray(knownByJson));v.put("reveal_condition",revealCondition==null?"":revealCondition);v.put("source",source==null?"":source);long now=System.currentTimeMillis();v.put("updated_at",now);if(previous.length()>0){getWritableDatabase().update("knowledge_facts",v,"id=? AND world_profile=?",new String[]{String.valueOf(previous.optLong("id")),getActiveMapProfile()});return previous.optLong("id");}v.put("created_at",now);return getWritableDatabase().insert("knowledge_facts",null,v);}
    public List<JSONObject> listKnowledgeFacts(int limit){return queryList("SELECT * FROM knowledge_facts WHERE world_profile=? ORDER BY updated_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public void deleteKnowledgeFact(long id){getWritableDatabase().delete("knowledge_facts","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public long addQuest(String title,String objective,String stakes,long ownerId){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("title",title);v.put("objective",objective);v.put("stakes",stakes);v.put("owner_character_id",ownerId);v.put("status","active");v.put("created_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());return getWritableDatabase().insert("quests",null,v);}
    public List<JSONObject> listQuests(String status,int limit){String sql="SELECT * FROM quests WHERE world_profile=?";ArrayList<String> a=new ArrayList<String>();a.add(getActiveMapProfile());if(status!=null&&status.length()>0){sql+=" AND status=?";a.add(status);}sql+=" ORDER BY updated_at DESC LIMIT "+Math.max(1,Math.min(1000,limit));return queryList(sql,a.toArray(new String[0]));}
    public void updateQuestStatus(long id,String status,String stage){JSONObject current=queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});KingmakerCampaignGate.requireTransition(current,status,queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}));ContentValues v=new ContentValues();v.put("status",status);v.put("stage",stage);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("quests",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void deleteQuest(long id){getWritableDatabase().delete("quests","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public long upsertFaction(String name,String agenda,String resources,int relationship,String plan,String location){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("name",name);v.put("agenda",agenda);v.put("resources",resources);v.put("relationship",Math.max(-100,Math.min(100,relationship)));v.put("current_plan",plan);v.put("location",location);v.put("active",1);v.put("updated_at",System.currentTimeMillis());long id=getWritableDatabase().insertWithOnConflict("factions",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1){getWritableDatabase().update("factions",v,"world_profile=? AND name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),name});JSONObject f=queryOne("SELECT id FROM factions WHERE world_profile=? AND name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),name});return f.optLong("id");}return id;}
    public List<JSONObject> listFactions(int limit){return queryList("SELECT * FROM factions WHERE world_profile=? AND active=1 ORDER BY updated_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public void deleteFaction(long id){getWritableDatabase().delete("factions","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public long addWorldClock(String name,int max,String consequence,boolean automatic){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("name",name);v.put("current_value",0);v.put("max_value",Math.max(2,max));v.put("consequence",consequence);v.put("automatic",automatic?1:0);v.put("active",1);v.put("updated_at",System.currentTimeMillis());return getWritableDatabase().insert("world_clocks",null,v);}
    public List<JSONObject> listWorldClocks(boolean activeOnly,int limit){return queryList("SELECT * FROM world_clocks WHERE world_profile=?"+(activeOnly?" AND active=1":"")+" ORDER BY updated_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public void setWorldClockValue(long id,int value){JSONObject c=queryOne("SELECT * FROM world_clocks WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});ContentValues v=new ContentValues();v.put("current_value",Math.max(0,Math.min(Math.max(1,c.optInt("max_value",4)),value)));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("world_clocks",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public long applyQuestUpdate(JSONObject q){if(q==null)return -1;long id=q.optLong("id",0);JSONObject previous=id>0?queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()}):queryOne("SELECT * FROM quests WHERE world_profile=? AND title=? COLLATE NOCASE LIMIT 1",new String[]{getActiveMapProfile(),q.optString("title").trim()});if(previous.length()>0){id=previous.optLong("id");q=KingmakerCampaignGate.preserveDefinition(previous,q);KingmakerCampaignGate.requireTransition(previous,q.optString("status",previous.optString("status")),queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}));}String title=q.has("title")?q.optString("title").trim():previous.optString("title");if(title.length()==0)return -1;ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("title",title);copyIfPresent(v,q,previous,"status","active");copyIfPresent(v,q,previous,"stage","");copyIfPresent(v,q,previous,"objective","");copyIfPresent(v,q,previous,"stakes","");copyLongIfPresent(v,q,previous,"owner_character_id",0);copyLongIfPresent(v,q,previous,"faction_id",0);copyIfPresent(v,q,previous,"progress_json","{}");v.put("updated_at",System.currentTimeMillis());if(id>0){getWritableDatabase().update("quests",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});return id;}v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("quests",null,v);}
    public long applyFactionUpdate(JSONObject f){if(f==null)return -1;String name=f.optString("name").trim();if(name.length()==0)return -1;JSONObject previous=queryOne("SELECT * FROM factions WHERE world_profile=? AND name=? COLLATE NOCASE LIMIT 1",new String[]{getActiveMapProfile(),name});ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("name",name);copyIfPresent(v,f,previous,"agenda","");copyIfPresent(v,f,previous,"resources","");for(String key:new String[]{"objectives_json","resources_json","territories_json","relations_json","allies_json","enemies_json","information_json","costs_json","risks_json","success_conditions_json","failure_conditions_json","memory_json"})copyIfPresent(v,f,previous,key,key.endsWith("_json")&&("objectives_json".equals(key)||"resources_json".equals(key)||"costs_json".equals(key))?"{}":"[]");int relationship=f.has("relationship")?f.optInt("relationship",previous.optInt("relationship",0)):previous.optInt("relationship",0);v.put("relationship",Math.max(-100,Math.min(100,relationship)));copyIfPresent(v,f,previous,"current_plan","");copyIfPresent(v,f,previous,"location","");copyIfPresent(v,f,previous,"plan_steps_json","[]");v.put("plan_cycle",f.has("plan_cycle")?(f.optBoolean("plan_cycle",true)?1:0):previous.optInt("plan_cycle",1));copyIfPresent(v,f,previous,"last_action_summary","");v.put("plan_progress",Math.max(0,f.has("plan_progress")?f.optInt("plan_progress",previous.optInt("plan_progress",0)):previous.optInt("plan_progress",0)));v.put("plan_interval_hours",Math.max(1d,f.has("plan_interval_hours")?f.optDouble("plan_interval_hours",previous.optDouble("plan_interval_hours",24d)):previous.optDouble("plan_interval_hours",24d)));v.put("plan_progress_hours",Math.max(0d,f.has("plan_progress_hours")?f.optDouble("plan_progress_hours",previous.optDouble("plan_progress_hours",0d)):previous.optDouble("plan_progress_hours",0d)));v.put("active",f.has("active")?(f.optBoolean("active",true)?1:0):previous.optInt("active",1));v.put("updated_at",System.currentTimeMillis());long id=getWritableDatabase().insertWithOnConflict("factions",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1){getWritableDatabase().update("factions",v,"world_profile=? AND name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),name});return previous.optLong("id");}return id;}
    public long applyWorldClockUpdate(JSONObject c){
        if(c==null)return -1;long id=c.optLong("id",0);JSONObject previous=id>0?queryOne("SELECT * FROM world_clocks WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()}):queryOne("SELECT * FROM world_clocks WHERE world_profile=? AND name=? COLLATE NOCASE LIMIT 1",new String[]{getActiveMapProfile(),c.optString("name").trim()});if(previous.length()>0)id=previous.optLong("id");String name=c.has("name")?c.optString("name").trim():previous.optString("name");if(name.length()==0)return -1;
        int max=Math.max(2,c.has("max_value")?c.optInt("max_value",previous.optInt("max_value",4)):previous.optInt("max_value",4));int current=Math.max(0,Math.min(max,c.has("current_value")?c.optInt("current_value",previous.optInt("current_value",0)):previous.optInt("current_value",0)));ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("name",name);v.put("current_value",current);v.put("max_value",max);copyIfPresent(v,c,previous,"consequence","");v.put("automatic",c.has("automatic")?(c.optBoolean("automatic",false)?1:0):previous.optInt("automatic",0));v.put("active",c.has("active")?(c.optBoolean("active",true)?1:0):previous.optInt("active",1));v.put("interval_hours",Math.max(1d,c.has("interval_hours")?c.optDouble("interval_hours",previous.optDouble("interval_hours",6d)):previous.optDouble("interval_hours",6d)));v.put("accumulated_hours",Math.max(0d,c.has("accumulated_hours")?c.optDouble("accumulated_hours",previous.optDouble("accumulated_hours",0d)):previous.optDouble("accumulated_hours",0d)));copyIfPresent(v,c,previous,"trigger_condition","");v.put("faction_id",Math.max(0L,c.has("faction_id")?c.optLong("faction_id",previous.optLong("faction_id",0)):previous.optLong("faction_id",0)));
        v.put("paused",c.has("paused")?(c.optBoolean("paused",false)?1:0):previous.optInt("paused",0));v.put("rate_multiplier",Math.max(.01d,Math.min(100d,c.has("rate_multiplier")?c.optDouble("rate_multiplier",previous.optDouble("rate_multiplier",1d)):previous.optDouble("rate_multiplier",1d))));int tickDelta=c.has("tick_delta")?c.optInt("tick_delta",previous.optInt("tick_delta",1)):previous.optInt("tick_delta",1);boolean regression=c.has("regression_allowed")?c.optBoolean("regression_allowed",false):previous.optInt("regression_allowed",0)==1;if(tickDelta<0&&!regression)tickDelta=0;v.put("tick_delta",Math.max(-max,Math.min(max,tickDelta)));v.put("regression_allowed",regression?1:0);copyIfPresent(v,c,previous,"owner_entity","");v.put("segments_json",validJsonArray(c.has("segments_json")?c.optString("segments_json","[]"):previous.optString("segments_json","[]")));v.put("updated_at",System.currentTimeMillis());if(id>0){getWritableDatabase().update("world_clocks",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});return id;}return getWritableDatabase().insert("world_clocks",null,v);
    }
    private static void copyIfPresent(ContentValues v,JSONObject incoming,JSONObject previous,String key,String fallback){v.put(key,incoming.has(key)?incoming.optString(key,previous.optString(key,fallback)):previous.optString(key,fallback));}
    private static void copyLongIfPresent(ContentValues v,JSONObject incoming,JSONObject previous,String key,long fallback){v.put(key,incoming.has(key)?incoming.optLong(key,previous.optLong(key,fallback)):previous.optLong(key,fallback));}
    public void deleteWorldClock(long id){getWritableDatabase().delete("world_clocks","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public long addWorldEvent(String title,String description,long dueAt,int dueDay,String consequenceJson){try{return addWorldEventStructured(new JSONObject().put("title",title==null?"":title).put("description",description==null?"":description).put("due_at",dueAt).put("due_day",dueDay).put("consequence_json",new JSONObject(validJsonObject(consequenceJson))));}catch(Exception error){AppLog.w("AppDb","Evento inválido não foi criado.",error);return -1;}}
    public long addWorldEventStructured(JSONObject event)throws Exception{
        if(event==null)return -1;String safeTitle=event.optString("title").trim();if(safeTitle.length()==0)return -1;long dueAt=Math.max(0L,event.optLong("due_at",0));int dueDay=Math.max(0,event.optInt("due_day",0));Object rawConsequence=event.opt("consequence_json");String consequence=rawConsequence instanceof JSONObject?rawConsequence.toString():validJsonObject(String.valueOf(rawConsequence==null?"{}":rawConsequence));Object rawTrigger=event.opt("trigger_json");String trigger=rawTrigger instanceof JSONObject?rawTrigger.toString():validJsonObject(String.valueOf(rawTrigger==null||String.valueOf(rawTrigger).trim().length()==0?new JSONObject().put("due_at",dueAt).put("due_day",dueDay):rawTrigger));Object rawProcedure=event.opt("procedure_json");String procedure=rawProcedure instanceof JSONObject?rawProcedure.toString():validJsonObject(String.valueOf(rawProcedure==null?"{}":rawProcedure));JSONObject procedureObject=new JSONObject(procedure);if(procedureObject.length()==0)procedure=new JSONObject().put("executable",false).put("review_required",true).put("reason","Evento sem procedimento executável; consequência mecânica exige revisão.").toString();String identity=getActiveMapProfile()+"|"+safeTitle.toLowerCase(java.util.Locale.ROOT)+"|"+trigger+"|"+procedure+"|"+consequence;String eventKey="event:"+java.util.UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();String transactionKey="world_event:"+eventKey;JSONObject previous=queryOne("SELECT * FROM world_events WHERE world_profile=? AND event_key=? LIMIT 1",new String[]{getActiveMapProfile(),eventKey});if(previous.length()>0)return previous.optLong("id");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("event_key",eventKey);v.put("title",safeTitle);v.put("description",event.optString("description"));v.put("due_at",dueAt);v.put("due_day",dueDay);v.put("status","scheduled");v.put("consequence_status","pending");v.put("consequence_json",consequence);v.put("trigger_json",trigger);v.put("procedure_json",procedure);v.put("state_json",new JSONObject().put("state","scheduled").put("created_at",System.currentTimeMillis()).toString());v.put("transaction_key",transactionKey);v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insertOrThrow("world_events",null,v);
    }
    public List<JSONObject> listDueWorldEvents(long now,int limit){int day=getSceneState().optInt("day_number",1);return queryList("SELECT * FROM world_events WHERE world_profile=? AND status='scheduled' AND ((due_at>0 AND due_at<=?) OR (due_day>0 AND due_day<=?)) ORDER BY CASE WHEN due_at>0 THEN due_at ELSE due_day END LIMIT "+Math.max(1,Math.min(5000,limit)),new String[]{getActiveMapProfile(),String.valueOf(now),String.valueOf(day)});}
    public List<JSONObject> listUpcomingWorldEvents(int limit){return queryList("SELECT * FROM world_events WHERE world_profile=? AND status='scheduled' ORDER BY CASE WHEN due_at>0 THEN due_at ELSE 9223372036854775807 END,due_day LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public void markWorldEventPendingReview(long eventId,String error){if(eventId<=0)return;JSONObject event=getWorldEvent(eventId);String transactionKey=requiredWorldEventTransactionKey(event,eventId);ContentValues v=new ContentValues();v.put("status","awaiting_review");v.put("consequence_status","pending_review");v.put("last_error",error==null?"":error);v.put("transaction_key",transactionKey);v.put("state_json","{\"state\":\"awaiting_review\"}");getWritableDatabase().update("world_events",v,"id=? AND world_profile=?",new String[]{String.valueOf(eventId),getActiveMapProfile()});}
    public void resolveWorldEventAfterReview(long eventId,String status,String note){
        if(eventId<=0)throw new IllegalArgumentException("Evento inválido.");
        JSONObject event=getWorldEvent(eventId);
        if(event.length()==0||!"awaiting_review".equalsIgnoreCase(event.optString("status")))throw new IllegalStateException("O evento não está aguardando revisão.");
        String safe="cancelled".equalsIgnoreCase(status)?"cancelled":"resolved";
        ContentValues v=new ContentValues();
        String transactionKey=requiredWorldEventTransactionKey(event,eventId);
        v.put("status",safe);v.put("consequence_status","resolved".equals(safe)?"applied":"cancelled");if("resolved".equals(safe))v.put("application_receipt",transactionKey);
        v.put("resolved_at",System.currentTimeMillis());v.put("applied_at",System.currentTimeMillis());v.put("last_error",note==null?"":note);
        int changed=getWritableDatabase().update("world_events",v,"id=? AND world_profile=? AND status='awaiting_review'",new String[]{String.valueOf(eventId),getActiveMapProfile()});
        if(changed!=1)throw new IllegalStateException("O evento mudou durante a revisão e não foi encerrado.");
        JSONObject auditAfter=new JSONObject();
        try{auditAfter.put("status",safe);auditAfter.put("note",note==null?"":note);}
        catch(Exception error){throw new IllegalStateException("Falha ao montar auditoria do evento.",error);}
        addAuditEvent("evento_mundo","Evento encerrado após revisão",event.optString("title")+" • "+safe,event.toString(),auditAfter.toString(),true);
    }
        public JSONObject getWorldEvent(long id){return queryOne("SELECT * FROM world_events WHERE id=? AND world_profile=? LIMIT 1",new String[]{String.valueOf(id),getActiveMapProfile()});}
public List<JSONObject> listWorldEventsForMaster(int limit){return queryList("SELECT * FROM world_events WHERE world_profile=? AND status IN ('scheduled','awaiting_review') ORDER BY CASE status WHEN 'awaiting_review' THEN 0 ELSE 1 END,CASE WHEN due_at>0 THEN due_at ELSE 9223372036854775807 END,due_day LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    private static String requiredWorldEventTransactionKey(JSONObject event,long eventId){if(event==null||event.length()==0)throw new IllegalStateException("Evento inexistente: "+eventId);String eventKey=event.optString("event_key").trim();if(eventKey.length()==0)throw new IllegalStateException("Evento sem event_key obrigatório: "+eventId);String expected=LegacyWorldEventIdentity.transactionKey(eventKey),stored=event.optString("transaction_key").trim();if(stored.length()>0&&!expected.equals(stored))throw new IllegalStateException("Evento "+eventId+" possui transaction_key divergente.");return expected;}
    public void markWorldEventConsequence(long eventId,boolean applied,String error){if(eventId<=0)return;JSONObject event=getWorldEvent(eventId);String transactionKey=requiredWorldEventTransactionKey(event,eventId);ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("consequence_status",applied?"applied":"failed");v.put("applied_at",applied?now:0);v.put("last_error",error==null?"":error);v.put("state_json","{\"state\":\""+(applied?"resolved":"failed")+"\",\"at\":"+now+"}");if(applied){v.put("status","resolved");v.put("resolved_at",now);v.put("application_receipt",transactionKey);}getWritableDatabase().update("world_events",v,"id=? AND world_profile=?",new String[]{String.valueOf(eventId),getActiveMapProfile()});}
    public void updateFactionSimulation(long factionId,int progress,double progressHours,int lastActionDay){if(factionId<=0)return;ContentValues v=new ContentValues();v.put("plan_progress",Math.max(0,progress));v.put("plan_progress_hours",Math.max(0d,progressHours));v.put("last_action_day",Math.max(0,lastActionDay));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("factions",v,"id=? AND world_profile=?",new String[]{String.valueOf(factionId),getActiveMapProfile()});}
    public void applyFactionSimulationStep(JSONObject action){if(action==null||action.optLong("id",0)<=0)return;ContentValues v=new ContentValues();v.put("plan_progress",Math.max(0,action.optInt("progress_after",0)));v.put("plan_progress_hours",Math.max(0d,action.optDouble("progress_hours_after",0d)));v.put("last_action_day",Math.max(0,action.optInt("day",0)));v.put("last_action_summary",action.optString("summary",""));if(action.has("location_after"))v.put("location",action.optString("location_after"));if(action.has("resources_after"))v.put("resources",action.optString("resources_after"));if(action.has("agenda_after"))v.put("agenda",action.optString("agenda_after"));if(action.has("current_plan_after"))v.put("current_plan",action.optString("current_plan_after"));if(action.has("plan_steps_json_after"))v.put("plan_steps_json",action.opt("plan_steps_json_after") instanceof JSONArray?action.optJSONArray("plan_steps_json_after").toString():action.optString("plan_steps_json_after","[]"));if(action.has("plan_interval_hours_after"))v.put("plan_interval_hours",Math.max(1d,action.optDouble("plan_interval_hours_after",24d)));if(action.has("plan_cycle_after"))v.put("plan_cycle",action.optInt("plan_cycle_after",0)==1?1:0);if(action.has("relationship_after"))v.put("relationship",Math.max(-100,Math.min(100,action.optInt("relationship_after"))));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("factions",v,"id=? AND world_profile=?",new String[]{String.valueOf(action.optLong("id")),getActiveMapProfile()});}
    public void updateWorldClockSimulation(long clockId,int value,double accumulatedHours,int lastAdvancedDay){if(clockId<=0)return;ContentValues v=new ContentValues();v.put("current_value",Math.max(0,value));v.put("accumulated_hours",Math.max(0d,accumulatedHours));v.put("last_advanced_day",Math.max(0,lastAdvancedDay));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("world_clocks",v,"id=? AND world_profile=?",new String[]{String.valueOf(clockId),getActiveMapProfile()});}
    public void setWorldClockCompletionReceipt(long clockId,String receipt){if(clockId<=0||receipt==null||receipt.trim().length()==0)return;ContentValues v=new ContentValues();v.put("completion_receipt",receipt.trim());v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("world_clocks",v,"id=? AND world_profile=?",new String[]{String.valueOf(clockId),getActiveMapProfile()});}
    public void updateLifeActivityResolution(long activityId,String status,String narrative,JSONObject resolution,boolean applied){if(activityId<=0)return;ContentValues v=new ContentValues();v.put("status",status==null?"":status);v.put("narrative_result",narrative==null?"":narrative);v.put("resolution_json",resolution==null?"{}":resolution.toString());v.put("applied_at",applied?System.currentTimeMillis():0);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("life_activities",v,"id=? AND world_profile=?",new String[]{String.valueOf(activityId),getActiveMapProfile()});}
    public void updateLifeActivityProgressChecked(long activityId,double progress,double total,String status,String narrative,JSONObject resolution,boolean applied)throws Exception{if(activityId<=0)throw new IllegalArgumentException("Atividade inválida.");double safeTotal=Math.max(1d,total);ContentValues v=new ContentValues();v.put("progress_units",Math.max(0d,Math.min(safeTotal,progress)));v.put("total_units",safeTotal);v.put("status",status==null?"":status);v.put("narrative_result",narrative==null?"":narrative);v.put("resolution_json",resolution==null?"{}":resolution.toString());v.put("applied_at",applied?System.currentTimeMillis():0);v.put("updated_at",System.currentTimeMillis());if(getWritableDatabase().update("life_activities",v,"id=? AND world_profile=?",new String[]{String.valueOf(activityId),getActiveMapProfile()})!=1)throw new IllegalStateException("O progresso da atividade não foi persistido.");}
    public void applyMapDirectorUpdate(JSONObject update){if(update==null||update.length()==0)return;JSONObject current=getMapState();String currentHex=current.optString("current_hex_id"),destination=current.optString("destination_hex_id"),route=current.optString("route_json","[]"),reveals=current.optString("reveals_json","{}");boolean locked=current.optInt("position_locked",0)==1,overlay=current.optInt("overlay_visible",1)==1;if(update.has("current_hex_id"))currentHex=update.optString("current_hex_id",currentHex);if(update.has("destination_hex_id"))destination=update.optString("destination_hex_id",destination);if(update.has("route_json"))route=validJsonArray(String.valueOf(update.opt("route_json")));if(update.has("reveals_json"))reveals=validJsonObject(String.valueOf(update.opt("reveals_json")));if(update.has("position_locked"))locked=update.optBoolean("position_locked",locked);if(update.has("overlay_visible"))overlay=update.optBoolean("overlay_visible",overlay);saveMapViewExtras(currentHex,overlay,reveals);saveMapTravelState(locked,destination,route,"Mestre confirmado");}
    public void applyTacticalDirectorUpdate(JSONObject update){if(update==null)return;long combatantId=update.optLong("combatant_id",0);if(combatantId<=0&&update.optString("name").trim().length()>0)combatantId=findCombatantByName(update.optString("name")).optLong("id",0);if(combatantId<=0)return;setTacticalTokenPosition(combatantId,(float)update.optDouble("x",.5d),(float)update.optDouble("y",.5d));}

    // ===== Gravações estritas usadas pelo pipeline transacional do Mestre =====
    public void saveSceneStateChecked(JSONObject scene)throws Exception{
        if(scene==null)throw new IllegalArgumentException("Estado de cena ausente.");
        ContentValues v=new ContentValues();
        v.put("day_number",Math.max(1,scene.optInt("day_number",1)));v.put("clock_hour",normalizeClockHour(scene.optDouble("clock_hour",clockHourFromLabel(scene.optString("time_of_day","Manhã")))));v.put("location",scene.optString("location"));v.put("hex_id",scene.optString("hex_id"));v.put("time_of_day",scene.optString("time_of_day"));v.put("weather",scene.optString("weather"));v.put("lighting",scene.optString("lighting"));v.put("danger",scene.optString("danger"));v.put("objective",scene.optString("objective"));
        v.put("present_npcs_json",jsonValue(scene,"present_npcs","present_npcs_json","[]"));v.put("threats_json",jsonValue(scene,"threats","threats_json","[]"));v.put("clues_json",jsonValue(scene,"clues","clues_json","[]"));v.put("pending_consequences_json",jsonValue(scene,"pending_consequences","pending_consequences_json","[]"));v.put("updated_at",System.currentTimeMillis());
        int changed=getWritableDatabase().update("scene_state_profiles",v,"world_profile=?",new String[]{getActiveMapProfile()});
        if(changed!=1)throw new IllegalStateException("A cena não pôde ser persistida no perfil ativo.");
    }
    public long applyQuestUpdateChecked(JSONObject update)throws Exception{long id=applyQuestUpdate(update);if(id<=0)throw new IllegalStateException("Falha ao persistir missão.");return id;}
    public long applyFactionUpdateChecked(JSONObject update)throws Exception{long id=applyFactionUpdate(update);if(id<=0)throw new IllegalStateException("Falha ao persistir facção.");return id;}
    public long applyWorldClockUpdateChecked(JSONObject update)throws Exception{long id=applyWorldClockUpdate(update);if(id<=0)throw new IllegalStateException("Falha ao persistir relógio do mundo.");return id;}
    public long addWorldEventChecked(String title,String description,long dueAt,int dueDay,String consequenceJson)throws Exception{long id=addWorldEvent(title,description,dueAt,dueDay,consequenceJson);if(id<=0)throw new IllegalStateException("Falha ao persistir evento do mundo.");return id;}
    public long addWorldEventChecked(JSONObject event)throws Exception{long id=addWorldEventStructured(event);if(id<=0)throw new IllegalStateException("Falha ao persistir evento estruturado do mundo.");return id;}
    public long addJournalChecked(String title,String body,String kind)throws Exception{long id=addJournal(title,body,kind);if(id<=0)throw new IllegalStateException("Falha ao persistir diário.");return id;}
    public long addKnowledgeFactChecked(String subject,String content,String truth,String visibility,String knownByJson,String revealCondition,String source)throws Exception{long id=addKnowledgeFact(subject,content,truth,visibility,knownByJson,revealCondition,source);if(id<=0)throw new IllegalStateException("Falha ao persistir conhecimento.");return id;}
    public void upsertNpcMemoryChecked(JSONObject npc)throws Exception{
        if(npc==null||npc.optString("name").trim().length()==0)throw new IllegalArgumentException("Memória de NPC sem nome.");
        upsertNpcMemory(npc);if(findNpcMemoryByName(npc.optString("name")).length()==0)throw new IllegalStateException("Falha ao persistir memória do NPC "+npc.optString("name")+".");ensureNpcPersonalityProvisioned(npc.optString("name"));
    }
    public void upsertNpcCognitiveStateChecked(JSONObject state)throws Exception{
        String name=state==null?"":state.optString("name",state.optString("npc_name","")).trim();if(name.length()==0)throw new IllegalArgumentException("Cognição de NPC sem nome.");
        ensureNpcPersonalityOnIncoming(state,name);upsertNpcCognitiveState(state);if(getNpcCognitiveState(name).length()==0)throw new IllegalStateException("Falha ao persistir cognição do NPC "+name+".");
    }
    public void applyFactionSimulationStepChecked(JSONObject action)throws Exception{
        if(action==null||action.optLong("id",0)<=0)throw new IllegalArgumentException("Etapa de facção inválida.");
        applyFactionSimulationStep(action);JSONObject row=queryOne("SELECT plan_progress,last_action_day FROM factions WHERE id=? AND world_profile=?",new String[]{String.valueOf(action.optLong("id")),getActiveMapProfile()});
        if(row.length()==0||row.optInt("plan_progress",-1)!=Math.max(0,action.optInt("progress_after",0)))throw new IllegalStateException("A etapa da facção não foi persistida.");
    }
    public void updateWorldClockSimulationChecked(long clockId,int value,double accumulatedHours,int lastAdvancedDay)throws Exception{
        if(clockId<=0)throw new IllegalArgumentException("Relógio inválido.");updateWorldClockSimulation(clockId,value,accumulatedHours,lastAdvancedDay);
        JSONObject row=queryOne("SELECT current_value FROM world_clocks WHERE id=? AND world_profile=?",new String[]{String.valueOf(clockId),getActiveMapProfile()});
        if(row.length()==0||row.optInt("current_value",-1)!=Math.max(0,value))throw new IllegalStateException("O relógio do mundo não foi persistido.");
    }
    public void updateLifeActivityResolutionChecked(long activityId,String status,String narrative,JSONObject resolution,boolean applied)throws Exception{
        if(activityId<=0)throw new IllegalArgumentException("Atividade inválida.");updateLifeActivityResolution(activityId,status,narrative,resolution,applied);
        JSONObject row=queryOne("SELECT status FROM life_activities WHERE id=? AND world_profile=?",new String[]{String.valueOf(activityId),getActiveMapProfile()});
        if(row.length()==0||!String.valueOf(status).equals(row.optString("status")))throw new IllegalStateException("A atividade não foi persistida.");
    }
    public void applyMapDirectorUpdateChecked(JSONObject update)throws Exception{
        if(update==null||update.length()==0)return;JSONObject before=getMapState();String packageId=before.optString("map_package_id");
        for(String key:new String[]{"current_hex_id","destination_hex_id","scene_hex_id"})if(update.has(key)&&update.optString(key).trim().length()>0&&getMapHex(packageId,update.optString(key).trim()).length()==0)throw new IllegalArgumentException(key+" não existe no atlas ativo: "+update.optString(key));
        if(update.has("route_json")){JSONArray route;Object raw=update.opt("route_json");try{route=raw instanceof JSONArray?(JSONArray)raw:new JSONArray(String.valueOf(raw));}catch(Exception e){throw new IllegalArgumentException("Rota do mapa inválida.",e);}for(int i=0;i<route.length();i++){Object item=route.opt(i);String hex=item instanceof JSONObject?((JSONObject)item).optString("hex_id",((JSONObject)item).optString("id")):String.valueOf(item);if(hex.trim().length()==0||getMapHex(packageId,hex.trim()).length()==0)throw new IllegalArgumentException("A rota contém hexágono inexistente: "+hex);}}
        if(update.optBoolean("interrupt_route",false))update.put("destination_hex_id","").put("route_json",new JSONArray()).put("position_locked",false);
        applyMapDirectorUpdate(update);JSONObject after=getMapState();
        JSONObject terrain=update.optJSONObject("terrain_change");if(terrain!=null){String hex=terrain.optString("hex_id");if(!terrain.optBoolean("reviewed",false))throw new IllegalArgumentException("Alteração de terreno exige revisão explícita.");if(getMapHex(packageId,hex).length()==0)throw new IllegalArgumentException("Hexágono de terreno inexistente.");saveMapHexCorrection(packageId,hex,new JSONObject(terrain.toString()).put("review_status","verified").put("review_note",terrain.optString("review_note","Alteração confirmada pelo Mestre.")));}
        JSONObject marker=update.optJSONObject("marker_add");if(marker!=null){String hex=marker.optString("hex_id");if(getMapHex(packageId,hex).length()==0)throw new IllegalArgumentException("Hexágono do marcador inexistente.");saveMapHexNote(packageId,hex,marker.optString("title"),marker.optString("details"),marker.optString("icon","✎"),"master_reviewed");}
        String removeMarker=update.optString("marker_remove_hex_id");if(removeMarker.length()>0){if(getMapHex(packageId,removeMarker).length()==0)throw new IllegalArgumentException("Hexágono do marcador inexistente.");deleteMapHexNote(packageId,removeMarker);}
        if(update.has("scene_hex_id")){JSONObject scene=getSceneState();scene.put("hex_id",update.optString("scene_hex_id"));saveSceneStateChecked(scene);}
        if(update.has("current_hex_id")&&!update.optString("current_hex_id").equals(after.optString("current_hex_id")))throw new IllegalStateException("A posição do mapa não foi persistida.");
        if(update.has("destination_hex_id")&&!update.optString("destination_hex_id").equals(after.optString("destination_hex_id")))throw new IllegalStateException("O destino do mapa não foi persistido.");
        if(update.has("position_locked")&&update.optBoolean("position_locked")!=(after.optInt("position_locked",0)==1))throw new IllegalStateException("O bloqueio do mapa não foi persistido.");
    }
    public void applyTacticalDirectorUpdateChecked(JSONObject update)throws Exception{
        if(update==null)throw new IllegalArgumentException("Atualização tática ausente.");long id=update.optLong("combatant_id",0);if(id<=0&&update.optString("name").trim().length()>0)id=findCombatantByName(update.optString("name")).optLong("id",0);
        if(id<=0||queryOne("SELECT id FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()}).length()==0)throw new IllegalStateException("Combatente tático inexistente.");
        ensureTacticalTokensForCombat();ContentValues v=new ContentValues();v.put("x",Math.max(.01d,Math.min(.99d,update.optDouble("x",.5d))));v.put("y",Math.max(.01d,Math.min(.99d,update.optDouble("y",.5d))));v.put("updated_at",System.currentTimeMillis());
        int changed=getWritableDatabase().update("tactical_tokens",v,"world_profile=? AND combatant_id=?",new String[]{getActiveMapProfile(),String.valueOf(id)});if(changed!=1)throw new IllegalStateException("A posição tática não foi persistida.");
    }
    public void markWorldEventConsequenceChecked(long eventId,boolean applied,String error)throws Exception{
        markWorldEventConsequence(eventId,applied,error);JSONObject row=getWorldEvent(eventId);String expected=applied?"applied":"failed";if(row.length()==0||!expected.equals(row.optString("consequence_status")))throw new IllegalStateException("O estado da consequência do evento não foi persistido.");
    }
    public void resolvePendingEffectChecked(long id,String status)throws Exception{resolvePendingEffect(id,status);JSONObject row=getPendingTransaction(id);String expected="applied".equals(status)?"applied":("cancelled".equals(status)?"discarded":"failed");if(row.length()==0||!expected.equals(row.optString("status")))throw new IllegalStateException("A pendência não foi encerrada.");}
    public void setCharacterHpChecked(long id,int hp)throws Exception{setCharacterHp(id,hp);JSONObject row=getCharacter(id);int expected=Math.max(0,Math.min(Math.max(1,row.optInt("max_hp",1)),hp));if(row.length()==0||row.optInt("hp",-1)!=expected)throw new IllegalStateException("Os PV do personagem não foram persistidos.");}
    public void setCharacterEquipmentAndNotesChecked(long id,String equipment,String notes)throws Exception{setCharacterEquipmentAndNotes(id,equipment,notes);JSONObject row=getCharacter(id);if(row.length()==0||!String.valueOf(equipment==null?"":equipment).equals(row.optString("equipment"))||!String.valueOf(notes==null?"":notes).equals(row.optString("notes")))throw new IllegalStateException("O inventário ou as notas do personagem não foram persistidos.");}
    public void updateCharacterSheetChecked(long id,String sheetJson)throws Exception{updateCharacterSheet(id,sheetJson);JSONObject row=getCharacter(id);if(row.length()==0||!validJsonObject(sheetJson).equals(validJsonObject(row.optString("sheet_json"))))throw new IllegalStateException("A ficha estruturada não foi persistida.");}
    public long addCombatantChecked(String name,int initiative,int hp,int maxHp,int ac,String conditions,boolean player)throws Exception{long id=addCombatant(name,initiative,hp,maxHp,ac,conditions,player);if(id<=0||queryOne("SELECT id FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()}).length()==0)throw new IllegalStateException("O combatente não foi persistido.");return id;}
    public long addCombatantWithVisualChecked(String name,int initiative,int hp,int maxHp,int ac,String conditions,boolean player,String sourceKind,long sourceId,String tokenUri)throws Exception{long id=addCombatantWithVisual(name,initiative,hp,maxHp,ac,conditions,player,sourceKind,sourceId,tokenUri);if(id<=0||queryOne("SELECT id FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()}).length()==0)throw new IllegalStateException("O combatente com retrato não foi persistido.");return id;}
    public void setCombatRunningChecked(boolean running)throws Exception{setCombatRunning(running);if((getCombatState().optInt("running",0)==1)!=running)throw new IllegalStateException("O estado do combate não foi persistido.");}
    public void adjustCombatantHpChecked(long id,int delta,int expectedHp)throws Exception{if(queryOne("SELECT id FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()}).length()==0)throw new IllegalStateException("Combatente inexistente.");adjustCombatantHp(id,delta);JSONObject row=queryOne("SELECT hp FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});if(row.optInt("hp",-1)!=Math.max(0,expectedHp))throw new IllegalStateException("Os PV do combatente não foram persistidos.");}
    public void updateConditionsChecked(long id,String conditions)throws Exception{updateConditions(id,conditions);JSONObject row=queryOne("SELECT conditions FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});if(row.length()==0||!String.valueOf(conditions==null?"":conditions).equals(row.optString("conditions")))throw new IllegalStateException("As condições do combatente não foram persistidas.");}
    public void updateCombatantPf2AdvancedChecked(long id,int tempHp,boolean reactionUsed,String conditions,String timersJson,String resistances,String weaknesses,String immunities,String resourcesJson)throws Exception{updateCombatantPf2Advanced(id,tempHp,reactionUsed,conditions,timersJson,resistances,weaknesses,immunities,resourcesJson);JSONObject persisted=queryOne("SELECT id,resources_json FROM combatants WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});if(persisted.length()==0)throw new IllegalStateException("O estado PF2e do combatente não foi persistido.");JSONObject state=parseJsonObject(persisted.optString("resources_json","{}"));if(!"pathfinder_2e".equals(state.optString("system")))throw new IllegalStateException("O banco não confirmou o perfil PF2e do combatente.");}
    public void resetCombatChecked()throws Exception{resetCombat();if(getCombatState().optInt("running",1)!=0||!listCombatants().isEmpty())throw new IllegalStateException("O combate não foi reiniciado integralmente.");}
    public List<JSONObject> listWorldEvents(int limit){return queryList("SELECT * FROM world_events WHERE world_profile=? ORDER BY created_at DESC LIMIT "+Math.max(1,Math.min(2000,limit)),new String[]{getActiveMapProfile()});}
    public void deleteWorldEvent(long id){getWritableDatabase().delete("world_events","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void markWorldEventResolved(long id){JSONObject event=getWorldEvent(id);if(event.length()==0)throw new IllegalArgumentException("Evento inexistente.");String consequence=event.optString("consequence_status");if(!"applied".equals(consequence)&&!"cancelled".equals(consequence))throw new IllegalStateException("O evento não pode ser encerrado sem aplicar ou cancelar sua própria consequência.");ContentValues v=new ContentValues();v.put("status","cancelled".equals(consequence)?"cancelled":"resolved");v.put("resolved_at",System.currentTimeMillis());getWritableDatabase().update("world_events",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public long addPendingTransaction(String title,String summary,String payload){return addPendingTransaction(title,summary,payload,"");}
    public long addPendingTransaction(String title,String summary,String payload,String effectKey){
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("title",title);v.put("summary",summary);v.put("payload_json",validJsonObject(payload));v.put("effect_key",effectKey==null?"":effectKey.trim());v.put("status","pending");v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("pending_transactions",null,v);
    }
    public synchronized long addPendingTransactionUnique(String title,String summary,JSONObject payload,String effectKey)throws Exception{
        String key=effectKey==null?"":effectKey.trim();
        if(key.length()==0){long id=addPendingTransaction(title,summary,payload==null?"{}":payload.toString(),"");if(id<=0)throw new IllegalStateException("Não foi possível criar a pendência.");return id;}
        JSONObject existing=findOpenPendingTransactionByEffectKey(key);
        if(existing.length()>0)return existing.optLong("id",-1);
        JSONObject safe=payload==null?new JSONObject():new JSONObject(payload.toString());safe.put("_effect_key",key);
        long id=addPendingTransaction(title,summary,safe.toString(),key);if(id<=0)throw new IllegalStateException("Não foi possível criar a pendência idempotente "+key+".");return id;
    }
    public JSONObject findOpenPendingTransactionByEffectKey(String effectKey){
        String key=effectKey==null?"":effectKey.trim();if(key.length()==0)return new JSONObject();
        return queryOne("SELECT * FROM pending_transactions WHERE world_profile=? AND effect_key=? AND status IN ('pending','reviewing') ORDER BY id DESC LIMIT 1",new String[]{getActiveMapProfile(),key});
    }
    public boolean hasOpenPendingEffect(String effectKey){return findOpenPendingTransactionByEffectKey(effectKey).length()>0;}
    public void resolvePendingEffect(long id,String status){
        if(id<=0)return;String safe="applied".equals(status)?"applied":("cancelled".equals(status)?"discarded":"failed");
        setPendingTransactionStatus(id,safe);
    }
    public boolean hasEffectReceipt(String effectKey){
        String key=effectKey==null?"":effectKey.trim();if(key.length()==0)return false;
        return queryOne("SELECT effect_key FROM effect_receipts WHERE world_profile=? AND effect_key=?",new String[]{getActiveMapProfile(),key}).length()>0;
    }
    public void recordEffectReceipt(String effectKey,String source){
        String key=effectKey==null?"":effectKey.trim();if(key.length()==0)return;
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("effect_key",key);v.put("source",source==null?"":source);v.put("applied_at",System.currentTimeMillis());
        long id=getWritableDatabase().insertWithOnConflict("effect_receipts",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        if(id==-1&&!hasEffectReceipt(key))throw new IllegalStateException("Não foi possível registrar a idempotência do efeito "+key+".");
    }
    public JSONObject getPendingTransaction(long id){return queryOne("SELECT * FROM pending_transactions WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public List<JSONObject> listPendingTransactions(String status,int limit){String sql="SELECT * FROM pending_transactions WHERE world_profile=?";ArrayList<String> a=new ArrayList<String>();a.add(getActiveMapProfile());if(status!=null&&status.length()>0){sql+=" AND status=?";a.add(status);}sql+=" ORDER BY created_at DESC LIMIT "+Math.max(1,Math.min(5000,limit));return queryList(sql,a.toArray(new String[0]));}
    public void setPendingTransactionStatus(long id,String status){ContentValues v=new ContentValues();v.put("status",status);if("applied".equals(status))v.put("applied_at",System.currentTimeMillis());getWritableDatabase().update("pending_transactions",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void setPendingTransactionRollbackSnapshot(long id,String fileName){try{JSONObject row=getPendingTransaction(id);JSONObject payload=new JSONObject(row.optString("payload_json","{}"));payload.put("_rollback_snapshot",fileName==null?"":fileName);ContentValues v=new ContentValues();v.put("payload_json",payload.toString());getWritableDatabase().update("pending_transactions",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}}
    public JSONObject getLastAppliedTransaction(){return queryOne("SELECT * FROM pending_transactions WHERE world_profile=? AND status='applied' ORDER BY applied_at DESC,id DESC LIMIT 1",new String[]{getActiveMapProfile()});}
    public void markPendingTransactionRolledBack(long id){ContentValues v=new ContentValues();v.put("status","rolled_back");getWritableDatabase().update("pending_transactions",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public void ensureTacticalTokensForCombat(){List<JSONObject> combatants=listCombatants();int i=0;for(JSONObject c:combatants){long id=c.optLong("id");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("combatant_id",id);v.put("name",c.optString("name"));v.put("is_player",c.optInt("is_player",0));v.put("x",c.optInt("is_player",0)==1?0.18:0.82);v.put("y",0.12+(i++%6)*0.14);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("tactical_tokens",null,v,SQLiteDatabase.CONFLICT_IGNORE);}getWritableDatabase().execSQL("DELETE FROM tactical_tokens WHERE world_profile=? AND combatant_id NOT IN (SELECT id FROM combatants WHERE world_profile=?)",new Object[]{getActiveMapProfile(),getActiveMapProfile()});}
    public List<JSONObject> listTacticalTokens(){
        ensureTacticalTokensForCombat();
        String sql="SELECT tt.*,c.initiative,c.hp,c.max_hp,c.temp_hp,c.conditions,c.active_turn,c.resources_json,c.source_kind,c.source_id,c.token_uri AS combatant_token_uri "+
                "FROM tactical_tokens tt JOIN combatants c ON c.id=tt.combatant_id AND c.world_profile=tt.world_profile WHERE tt.world_profile=? ORDER BY tt.is_player DESC,c.initiative DESC,tt.name";
        List<JSONObject> rows=queryList(sql,new String[]{getActiveMapProfile()});
        for(JSONObject row:rows){
            JSONObject visual=resolveCombatantVisualIdentity(row.optString("name"),row.optInt("is_player",0)==1,row.optString("source_kind"),row.optLong("source_id"),firstNonBlank(row.optString("combatant_token_uri"),tokenFromResources(row.optString("resources_json"))));
            try{row.put("token_uri",visual.optString("token_uri"));row.put("token_source",visual.optString("source_kind"));}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao anexar o retrato ao token tático.",ignored);}
            if(row.optLong("combatant_id")>0&&( !visual.optString("token_uri").equals(row.optString("combatant_token_uri")) || !visual.optString("source_kind").equals(row.optString("source_kind")) || visual.optLong("source_id")!=row.optLong("source_id"))){
                setCombatantVisualIdentity(row.optLong("combatant_id"),visual.optString("source_kind"),visual.optLong("source_id"),visual.optString("token_uri"));
            }
        }
        return rows;
    }
    public void setCombatantVisualIdentity(long combatantId,String sourceKind,long sourceId,String tokenUri){
        if(combatantId<=0)return;ContentValues v=new ContentValues();v.put("source_kind",sourceKind==null?"":sourceKind.trim());v.put("source_id",Math.max(0,sourceId));v.put("token_uri",tokenUri==null?"":tokenUri.trim());getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",new String[]{String.valueOf(combatantId),getActiveMapProfile()});
    }
    private JSONObject resolveCombatantVisualIdentity(String name,boolean player,String sourceKind,long sourceId,String tokenUri){
        JSONObject out=new JSONObject();String kind=sourceKind==null?"":sourceKind.trim();long id=Math.max(0,sourceId);String uri=usableTokenUri(tokenUri);String clean=canonicalCombatantName(name);
        try{
            if(uri.length()==0){String resource="";JSONObject source=new JSONObject();
                if("character".equals(kind)&&id>0)source=getCharacter(id);else if("npc".equals(kind)&&id>0)source=queryOne("SELECT * FROM npc_memory_profiles WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});else if("token".equals(kind)&&id>0)source=getToken(id);
                resource="token".equals(kind)?source.optString("local_uri"):source.optString("token_uri");uri=usableTokenUri(resource);
            }
            if(uri.length()==0&&player){JSONObject ch=findCharacterByName(clean);if(ch.length()>0){kind="character";id=ch.optLong("id");uri=usableTokenUri(ch.optString("token_uri"));}}
            if(uri.length()==0&&!player){JSONObject npc=findNpcMemoryByName(clean);if(npc.length()==0&&!clean.equals(name))npc=findNpcMemoryByName(name);if(npc.length()>0){kind="npc";id=npc.optLong("id");uri=usableTokenUri(npc.optString("token_uri"));}}
            if(uri.length()==0){JSONObject token=findLocalTokenForScene(clean);if(token.length()==0&&!clean.equals(name))token=findLocalTokenForScene(name);if(token.length()>0){kind="token";id=token.optLong("id");uri=usableTokenUri(token.optString("local_uri"));}}
            out.put("source_kind",kind);out.put("source_id",id);out.put("token_uri",uri);
        }catch(Exception e){AppLog.w("CombatToken","Não foi possível resolver a imagem associada de "+name,e);}
        return out;
    }
    private String usableTokenUri(String value){String uri=value==null?"":value.trim();if(uri.length()==0)return "";try{return isUriReadable(uri)?uri:"";}catch(Exception ignored){return "";}}
    private static String tokenFromResources(String raw){try{return new JSONObject(raw==null?"{}":raw).optString("token_uri","").trim();}catch(Exception ignored){return "";}}
    private static String firstNonBlank(String first,String second){String a=first==null?"":first.trim();return a.length()>0?a:(second==null?"":second.trim());}
    private static String canonicalCombatantName(String value){String name=value==null?"":value.trim();name=name.replaceFirst("\\s+(?:#?\\d+|[IVX]+)$","").trim();name=name.replaceFirst("\\s*\\((?:\\d+|[A-Za-z])\\)$","").trim();return name.length()>0?name:(value==null?"":value.trim());}

    public void setTacticalTokenPosition(long combatantId,float x,float y){ContentValues v=new ContentValues();v.put("x",Math.max(.01f,Math.min(.99f,x)));v.put("y",Math.max(.01f,Math.min(.99f,y)));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("tactical_tokens",v,"world_profile=? AND combatant_id=?",new String[]{getActiveMapProfile(),String.valueOf(combatantId)});}

    public long addSessionMemory(int sessionNo,String title,String summary,String openThreadsJson,String characterBeatsJson){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("session_no",Math.max(1,sessionNo));v.put("title",title);v.put("summary",summary);v.put("open_threads_json",validJsonArray(openThreadsJson));v.put("character_beats_json",validJsonObject(characterBeatsJson));v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("session_memories",null,v);}
    public List<JSONObject> listSessionMemories(int limit){return queryList("SELECT * FROM session_memories WHERE world_profile=? ORDER BY session_no DESC,id DESC LIMIT "+Math.max(1,Math.min(5000,limit)),new String[]{getActiveMapProfile()});}
    public int nextSessionNumber(){JSONObject x=queryOne("SELECT COALESCE(MAX(session_no),0)+1 AS next_no FROM session_memories WHERE world_profile=?",new String[]{getActiveMapProfile()});return Math.max(1,x.optInt("next_no",1));}

    // ===== Núcleo cognitivo do Mestre (3.4.0) =====
    public JSONObject getTravelRulesState(){
        createTravelRulesTables(getWritableDatabase());
        JSONObject row=queryOne("SELECT * FROM travel_rules_state WHERE world_profile=?",new String[]{getActiveMapProfile()});
        try{return new JSONObject(row.optString("state_json","{}"));}catch(Exception e){return new JSONObject();}
    }
    public void saveTravelRulesState(JSONObject state){
        if(state==null)return;createTravelRulesTables(getWritableDatabase());
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("state_json",state.toString());v.put("updated_at",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("travel_rules_state",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public JSONObject getEnvironmentalDirectorState(){
        createCognitiveMasterTables(getWritableDatabase());
        JSONObject row=queryOne("SELECT * FROM environmental_director_state WHERE world_profile=?",new String[]{getActiveMapProfile()});
        try{return new JSONObject(row.optString("state_json","{}"));}catch(Exception e){return new JSONObject();}
    }
    public void saveEnvironmentalDirectorState(JSONObject state){
        if(state==null)return;createCognitiveMasterTables(getWritableDatabase());
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("state_json",state.toString());v.put("updated_at",System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("environmental_director_state",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public void saveEnvironmentalDirectorStateChecked(JSONObject state)throws Exception{saveEnvironmentalDirectorState(state);JSONObject after=getEnvironmentalDirectorState();if(state==null||after.length()==0)throw new IllegalStateException("O estado ambiental não foi persistido.");}
    public void saveMasterRuntimeStateChecked(JSONObject state)throws Exception{
        if(state==null)throw new IllegalArgumentException("Estado cognitivo ausente.");
        saveMasterRuntimeState(state);JSONObject after=getMasterRuntimeState();if(after.length()==0)throw new IllegalStateException("O estado cognitivo do Mestre não foi persistido.");
        String[] textKeys={"scene_mode","last_action_fingerprint","last_context_fingerprint","last_response_fingerprint","last_cognitive_plan_json","unresolved_threads_json","last_summary","validation_warnings_json"};
        for(String key:textKeys)if(state.has(key)&&!String.valueOf(state.opt(key)).equals(after.optString(key,"")))throw new IllegalStateException("Persistência cognitiva divergente em "+key+".");
        for(String key:new String[]{"tension","beat_no"})if(state.has(key)&&state.optInt(key)!=after.optInt(key))throw new IllegalStateException("Persistência cognitiva divergente em "+key+".");
        if(after.optLong("updated_at",0L)<=0L)throw new IllegalStateException("Persistência cognitiva sem confirmação temporal.");
    }
    public void savePartyStateChecked(JSONObject state)throws Exception{savePartyState(state);if(getPartyState().length()==0)throw new IllegalStateException("O estado da companhia não foi persistido.");}
    public void saveTravelRulesStateChecked(JSONObject state)throws Exception{saveTravelRulesState(state);JSONObject after=getTravelRulesState();if(state==null||after.length()==0)throw new IllegalStateException("O estado de viagem não foi persistido.");}
    public JSONObject getMasterRuntimeState(){createCognitiveMasterTables(getWritableDatabase());return queryOne("SELECT * FROM master_runtime_state WHERE world_profile=?",new String[]{getActiveMapProfile()});}
    public void saveMasterRuntimeState(JSONObject state){
        if(state==null)return;
        SQLiteDatabase sql=getWritableDatabase();createCognitiveMasterTables(sql);
        String profile=normalizeMapProfile(getActiveMapProfile());
        ContentValues v=new ContentValues();String[] keys={"scene_mode","tension","beat_no","last_action_fingerprint","last_context_fingerprint","last_response_fingerprint","last_cognitive_plan_json","unresolved_threads_json","last_summary","validation_warnings_json"};
        for(String key:keys)if(state.has(key)){Object value=state.opt(key);if(value instanceof JSONArray)v.put(key,((JSONArray)value).toString());else if(value instanceof JSONObject)v.put(key,((JSONObject)value).toString());else if(value instanceof Number)v.put(key,((Number)value).intValue());else v.put(key,String.valueOf(value));}
        v.put("updated_at",System.currentTimeMillis());
        int changed=sql.update("master_runtime_state",v,"world_profile=?",new String[]{profile});
        if(changed==0){v.put("world_profile",profile);long inserted=sql.insertWithOnConflict("master_runtime_state",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(inserted<0){v.remove("world_profile");changed=sql.update("master_runtime_state",v,"world_profile=?",new String[]{profile});if(changed==0)throw new IllegalStateException("Não foi possível criar nem atualizar o estado cognitivo do Mestre.");}}
    }
    public long addMasterEpisode(String action,String summary,String actorsJson,String sceneMode,String location,String hexId,String tagsJson,int significance){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("action_text",action==null?"":action);v.put("summary",summary==null?"":summary);v.put("actors_json",validJsonArray(actorsJson));v.put("scene_mode",sceneMode==null?"":sceneMode);v.put("location",location==null?"":location);v.put("hex_id",hexId==null?"":hexId);v.put("tags_json",validJsonArray(tagsJson));v.put("significance",Math.max(1,Math.min(5,significance)));v.put("canonical",1);v.put("created_at",System.currentTimeMillis());return getWritableDatabase().insert("master_episodes",null,v);}
    public List<JSONObject> listMasterEpisodes(int limit){createCognitiveMasterTables(getWritableDatabase());return queryList("SELECT * FROM master_episodes WHERE world_profile=? AND canonical=1 ORDER BY significance DESC,created_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public JSONObject getNpcCognitiveState(String name){createCognitiveMasterTables(getWritableDatabase());String safe=name==null?"":name.trim();JSONObject out=queryOne("SELECT * FROM npc_cognitive_state WHERE world_profile=? AND npc_name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),safe});if(out.length()==0){ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("npc_name",safe);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("npc_cognitive_state",null,v,SQLiteDatabase.CONFLICT_IGNORE);}ensureNpcPersonalityProvisioned(safe);return queryOne("SELECT * FROM npc_cognitive_state WHERE world_profile=? AND npc_name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),safe});}
    public void upsertNpcCognitiveState(JSONObject state){if(state==null)return;String name=state.optString("name",state.optString("npc_name","")).trim();if(name.length()==0)return;ensureNpcPersonalityOnIncoming(state,name);JSONObject previous=getNpcCognitiveState(name);ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("npc_name",name);putNpcField(v,state,previous,"emotion","neutro");putNpcInt(v,state,previous,"trust",0,-100,100);putNpcInt(v,state,previous,"fear",0,0,100);putNpcInt(v,state,previous,"suspicion",0,0,100);putNpcInt(v,state,previous,"urgency",0,0,100);putNpcField(v,state,previous,"next_action","");for(String key:new String[]{"schedule_json","beliefs_json","goals_json","knowledge_json","recent_events_json"})putNpcField(v,state,previous,key,"[]");for(String key:new String[]{"personality_json","resources_json"})putNpcField(v,state,previous,key,"{}");putNpcField(v,state,previous,"faction","");putNpcField(v,state,previous,"current_location","");putNpcField(v,state,previous,"last_interaction_summary","");v.put("last_action_at",state.has("last_action_at")?state.optLong("last_action_at"):previous.optLong("last_action_at",0));v.put("last_action_day",state.has("last_action_day")?Math.max(0,state.optInt("last_action_day")):previous.optInt("last_action_day",0));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("npc_cognitive_state",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static void putNpcInt(ContentValues v,JSONObject incoming,JSONObject previous,String key,int fallback,int min,int max){int value=incoming.has(key)?incoming.optInt(key,previous.optInt(key,fallback)):previous.optInt(key,fallback);v.put(key,Math.max(min,Math.min(max,value)));}
    public List<JSONObject> listNpcCognitiveStates(int limit){createCognitiveMasterTables(getWritableDatabase());return queryList("SELECT * FROM npc_cognitive_state WHERE world_profile=? ORDER BY updated_at DESC LIMIT "+Math.max(1,Math.min(5000,limit)),new String[]{getActiveMapProfile()});}
    public void saveMasterCache(String cacheKey,String mode,String model,String actionHash,String stateHash,String response){if(cacheKey==null||cacheKey.length()==0||response==null||response.length()==0)return;ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("cache_key",cacheKey);v.put("mode",mode==null?"":mode);v.put("model",model==null?"":model);v.put("action_hash",actionHash==null?"":actionHash);v.put("state_hash",stateHash==null?"":stateHash);v.put("response_json",response);v.put("created_at",System.currentTimeMillis());v.put("last_used_at",0);v.put("use_count",0);getWritableDatabase().insertWithOnConflict("master_response_cache",null,v,SQLiteDatabase.CONFLICT_REPLACE);getWritableDatabase().execSQL("DELETE FROM master_response_cache WHERE world_profile=? AND created_at < ?",new Object[]{getActiveMapProfile(),System.currentTimeMillis()-7L*24L*60L*60L*1000L});}
    public String getMasterCache(String cacheKey,long maxAgeMs){if(cacheKey==null||cacheKey.length()==0)return "";JSONObject row=queryOne("SELECT * FROM master_response_cache WHERE world_profile=? AND cache_key=?",new String[]{getActiveMapProfile(),cacheKey});if(row.length()==0||System.currentTimeMillis()-row.optLong("created_at",0)>Math.max(60000L,maxAgeMs))return "";ContentValues v=new ContentValues();v.put("last_used_at",System.currentTimeMillis());v.put("use_count",row.optInt("use_count",0)+1);getWritableDatabase().update("master_response_cache",v,"world_profile=? AND cache_key=?",new String[]{getActiveMapProfile(),cacheKey});return row.optString("response_json");}

    public JSONObject healthCheck(){
        JSONObject out=new JSONObject();Cursor c=null;
        try{
            SQLiteDatabase sql=getReadableDatabase();
            c=sql.rawQuery("PRAGMA quick_check",null);String quick=c.moveToFirst()?c.getString(0):"sem resposta";c.close();c=null;
            JSONArray missing=new JSONArray();String[] tables={"campaign","campaign_profiles","characters","journal","combatants","combat_state","combat_state_profiles","manuals","manual_rag_parts","manual_local_pages","dice_history","tokens","token_pdfs","pdf_tokens","map_state","map_world_state","map_packages","map_hex_atlas","map_locations","map_nations","map_metaregions","map_hex_locations","map_hex_corrections","map_hex_notes","campaign_poi_meta","campaign_pois","campaign_poi_history","campaign_poi_proposals","story_state","story_state_profiles","audit_log","scene_state_profiles","npc_memory_profiles","party_state_profiles","party_action_queue","character_control_profiles","knowledge_facts","quests","factions","world_clocks","world_events","pending_transactions","session_memories","tactical_tokens","master_runtime_state","master_episodes","npc_cognitive_state","master_response_cache","environmental_director_state","travel_rules_state","library_sync_state","effect_receipts","living_world_state","hero_life_profiles","life_relationships","life_activities","living_troupes","guilds","guild_ranks","guild_members","guild_contracts","guild_projects","guild_sites","guild_ledger","guild_relations","world_seeds","emergent_arcs","settlement_states","world_history_events","rumors","rumor_knowledge","campaign_legacies","npc_voice_profiles","tactical_scenes","local_table_sessions","local_table_players","local_table_secrets"};
            boolean transactionTablesPresent=true;
            for(String table:tables){Cursor t=sql.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});boolean exists=t.moveToFirst();t.close();if(!exists){missing.put(table);if("pending_transactions".equals(table)||"effect_receipts".equals(table)||"world_events".equals(table))transactionTablesPresent=false;}}
            Cursor fk=sql.rawQuery("PRAGMA foreign_key_check",null);int foreignKeyErrors=0;while(fk.moveToNext())foreignKeyErrors++;fk.close();
            int blankEffectKeys=-1,appliedWithoutReceipt=-1,openEffectKeyCollisions=-1,duplicateActiveEvents=-1,eventIdentityErrors=-1,appliedEventsWithoutReceipt=-1;
            if(transactionTablesPresent){
                blankEffectKeys=countQuery(sql,"SELECT COUNT(*) FROM pending_transactions WHERE TRIM(COALESCE(effect_key,''))=''",null);
                appliedWithoutReceipt=countQuery(sql,"SELECT COUNT(*) FROM pending_transactions p LEFT JOIN effect_receipts r ON r.world_profile=p.world_profile AND r.effect_key=p.effect_key WHERE p.status='applied' AND r.effect_key IS NULL",null);
                openEffectKeyCollisions=countQuery(sql,"SELECT COUNT(*) FROM (SELECT world_profile,effect_key FROM pending_transactions WHERE status IN ('pending','reviewing') AND TRIM(COALESCE(effect_key,''))<>'' GROUP BY world_profile,effect_key HAVING COUNT(*)>1)",null);
                duplicateActiveEvents=countQuery(sql,"SELECT COUNT(*) FROM (SELECT world_profile,lower(title),due_at,due_day,consequence_json FROM world_events WHERE status IN ('scheduled','awaiting_review') GROUP BY world_profile,lower(title),due_at,due_day,consequence_json HAVING COUNT(*)>1)",null);
                eventIdentityErrors=countQuery(sql,"SELECT COUNT(*) FROM world_events WHERE TRIM(COALESCE(event_key,''))='' OR TRIM(COALESCE(transaction_key,''))='' OR transaction_key<>('world_event:'||event_key)",null);
                appliedEventsWithoutReceipt=countQuery(sql,"SELECT COUNT(*) FROM world_events e LEFT JOIN effect_receipts r ON r.world_profile=e.world_profile AND r.effect_key=e.transaction_key WHERE (e.status='resolved' OR e.consequence_status='applied') AND (TRIM(COALESCE(e.application_receipt,''))='' OR r.effect_key IS NULL)",null);
            }
            JSONObject campaign=getCampaign();boolean transactionIntegrity=transactionTablesPresent&&blankEffectKeys==0&&appliedWithoutReceipt==0&&openEffectKeyCollisions==0&&duplicateActiveEvents==0&&eventIdentityErrors==0&&appliedEventsWithoutReceipt==0;
            boolean databaseHealthy="ok".equalsIgnoreCase(quick)&&missing.length()==0&&campaign.length()>0&&foreignKeyErrors==0&&transactionIntegrity;
            JSONObject mapAssets=mapAssetHealth();
            out.put("healthy",databaseHealthy);out.put("database_healthy",databaseHealthy);out.put("transaction_integrity",transactionIntegrity);out.put("blank_effect_keys",blankEffectKeys);out.put("applied_without_receipt",appliedWithoutReceipt);out.put("open_effect_key_collisions",openEffectKeyCollisions);out.put("duplicate_active_events",duplicateActiveEvents);out.put("event_identity_errors",eventIdentityErrors);out.put("applied_events_without_receipt",appliedEventsWithoutReceipt);out.put("assets_healthy",mapAssets.optBoolean("healthy",true));out.put("map_assets",mapAssets);out.put("quick_check",quick);out.put("missing_tables",missing);out.put("foreign_key_errors",foreignKeyErrors);out.put("database_version",sql.getVersion());out.put("characters",databaseHealthy?listCharacters().size():-1);out.put("last_error",lastDatabaseError);
        }catch(Exception e){lastDatabaseError=safeDatabaseError(e);AppLog.e("AppDb","Verificação de integridade do banco falhou.",e);try{out.put("healthy",false).put("database_healthy",false).put("error",lastDatabaseError);}catch(Exception jsonError){AppLog.w("AppDb","Falha ao registrar erro de integridade.",jsonError);}}
        finally{if(c!=null)c.close();}
        return out;
    }
    public JSONObject mapAssetHealth(){
        JSONObject out=new JSONObject();JSONArray profiles=new JSONArray();boolean allHealthy=true;
        for(String profile:new String[]{"pf2"}){
            JSONObject item=new JSONObject();try{
                JSONObject state=getMapStateForProfile(profile);String packageId=state.optString("map_package_id");String logicalId=state.optString("logical_map_id",logicalMapId(state.optString("manifest_json"),packageId));
                item.put("world_profile",profile).put("logical_map_id",logicalId).put("package_id",packageId);
                if(packageId.length()==0){item.put("status","not_installed").put("healthy",true);profiles.put(item);continue;}
                JSONObject pack=getMapPackageAny(packageId);boolean row=pack.length()>0;boolean ready=row&&pack.optInt("ready",0)==1;boolean image=ready&&isUriReadable(pack.optString("image_uri"));boolean manifest=ready&&pack.optString("manifest_json").trim().length()>0;int expected=pack.optInt("hex_count",0);int actual=0;if(ready){Cursor count=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM map_hex_atlas WHERE package_id=?",new String[]{packageId});try{actual=count.moveToFirst()?count.getInt(0):0;}finally{count.close();}}
                boolean healthy=row&&ready&&image&&manifest&&expected>0&&actual==expected;allHealthy&=healthy;item.put("healthy",healthy).put("row_present",row).put("ready",ready).put("image_readable",image).put("manifest_present",manifest).put("expected_hexes",expected).put("actual_hexes",actual).put("status",healthy?"ready":"repair_required");
            }catch(Exception e){allHealthy=false;try{item.put("world_profile",profile).put("healthy",false).put("status","error").put("error",safeDatabaseError(e));}catch(Exception jsonError){AppLog.w("AppDb","Falha ao registrar saúde do mapa.",jsonError);}}
            profiles.put(item);
        }
        try{out.put("healthy",allHealthy).put("profiles",profiles);}catch(Exception e){AppLog.w("AppDb","Falha ao concluir saúde dos mapas.",e);}return out;
    }
    public boolean isMapPackageUsable(String packageId){
        if(packageId==null||packageId.trim().length()==0)return false;
        JSONObject pack=getMapPackageAny(packageId);if(pack.length()==0||pack.optInt("ready",0)!=1||!isUriReadable(pack.optString("image_uri"))||pack.optString("manifest_json").trim().length()==0)return false;
        int expected=pack.optInt("hex_count",0);if(expected<=0)return false;Cursor count=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM map_hex_atlas WHERE package_id=?",new String[]{packageId});try{return count.moveToFirst()&&count.getInt(0)==expected;}finally{count.close();}
    }

    public boolean isUriReadable(String value){
        if(value==null||value.trim().length()==0)return false;InputStream in=null;try{Uri uri=Uri.parse(value);String scheme=uri.getScheme();if(scheme==null||scheme.length()==0)return new File(value).isFile()&&new File(value).canRead();if("file".equalsIgnoreCase(scheme)){String path=uri.getPath();return path!=null&&new File(path).isFile()&&new File(path).canRead();}in=appContext.getContentResolver().openInputStream(uri);return in!=null;}catch(Exception e){return false;}finally{if(in!=null)try{in.close();}catch(Exception closeError){AppLog.w("AppDb","Falha ao fechar referência de arquivo.",closeError);}}
    }
    public JSONObject ragIndexHealth(){
        JSONObject out=new JSONObject();JSONArray problems=new JSONArray();int ready=0;try{
            List<JSONObject> manuals=listManuals("");for(JSONObject manual:manuals){if(manual.optInt("enabled",1)!=1||!"ready".equals(manual.optString("rag_status")))continue;ready++;long id=manual.optLong("id");String store=manual.optString("rag_store_name");int declaredParts=manual.optInt("rag_part_count",0);int actualParts=0;Cursor parts=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manual_rag_parts WHERE manual_id=?",new String[]{String.valueOf(id)});try{actualParts=parts.moveToFirst()?parts.getInt(0):0;}finally{parts.close();}int declaredPages=manual.optInt("rag_page_count",0);int actualPages=0;Cursor pages=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM manual_local_pages WHERE manual_id=?",new String[]{String.valueOf(id)});try{actualPages=pages.moveToFirst()?pages.getInt(0):0;}finally{pages.close();}boolean local=store.startsWith("local");boolean coherent=local?(declaredPages>0&&actualPages==declaredPages):(declaredParts==0||actualParts==declaredParts);if(!coherent)problems.put(new JSONObject().put("manual_id",id).put("title",manual.optString("title")).put("store",store).put("declared_parts",declaredParts).put("actual_parts",actualParts).put("declared_pages",declaredPages).put("actual_pages",actualPages));}
            out.put("healthy",problems.length()==0).put("ready_manuals",ready).put("problems",problems);
        }catch(Exception e){try{out.put("healthy",false).put("error",safeDatabaseError(e)).put("problems",problems);}catch(Exception ignored){AppLog.e("AppDb","Falha ao avaliar coerência RAG.",e);}}return out;
    }

    public String getLastDatabaseError(){return lastDatabaseError;}

    JSONObject queryOne(String sql,String[] args){
        Cursor c=null;try{c=getReadableDatabase().rawQuery(sql,args);if(c.moveToFirst())return rowToJson(c);return new JSONObject();}
        catch(Exception e){lastDatabaseError=safeDatabaseError(e);AppLog.e("AppDb","Falha na consulta de leitura: "+sql,e);throw new IllegalStateException("Falha ao ler o banco; nenhum dado foi tratado como vazio. "+lastDatabaseError,e);}
        finally{if(c!=null)c.close();}
    }
    List<JSONObject> queryList(String sql,String[] args){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();Cursor c=null;
        try{c=getReadableDatabase().rawQuery(sql,args);while(c.moveToNext())out.add(rowToJson(c));return out;}
        catch(Exception e){lastDatabaseError=safeDatabaseError(e);AppLog.e("AppDb","Falha na consulta de lista: "+sql,e);throw new IllegalStateException("Falha ao listar dados; a operação foi interrompida. "+lastDatabaseError,e);}
        finally{if(c!=null)c.close();}
    }
    private static String safeDatabaseError(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private JSONObject rowToJson(Cursor c)throws Exception{JSONObject o=new JSONObject();for(int i=0;i<c.getColumnCount();i++){String n=c.getColumnName(i);switch(c.getType(i)){case Cursor.FIELD_TYPE_INTEGER:o.put(n,c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:o.put(n,c.getDouble(i));break;case Cursor.FIELD_TYPE_STRING:o.put(n,c.getString(i));break;case Cursor.FIELD_TYPE_BLOB:o.put(n,"[blob]");break;default:o.put(n,JSONObject.NULL);}}return o;}

    public void saveLibrarySyncState(String sourceKey,String sourceRoot,String status,int itemCount,int addedCount,int changedCount,int missingCount,long started,long success,String error,JSONObject details){ContentValues v=new ContentValues();v.put("source_key",sourceKey==null?"":sourceKey);v.put("source_root",sourceRoot==null?"":sourceRoot);v.put("status",status==null?"":status);v.put("item_count",Math.max(0,itemCount));v.put("added_count",Math.max(0,addedCount));v.put("changed_count",Math.max(0,changedCount));v.put("missing_count",Math.max(0,missingCount));v.put("last_started",Math.max(0,started));v.put("last_success",Math.max(0,success));v.put("last_error",error==null?"":error);v.put("details_json",details==null?"{}":details.toString());getWritableDatabase().insertWithOnConflict("library_sync_state",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public JSONObject getLibrarySyncState(String sourceKey){return queryOne("SELECT * FROM library_sync_state WHERE source_key=?",new String[]{sourceKey==null?"":sourceKey});}
    public List<JSONObject> listLibrarySyncStates(){return queryList("SELECT * FROM library_sync_state ORDER BY last_success DESC,source_key",null);}
    public JSONObject databaseIntegrity(){JSONObject out=new JSONObject();Cursor c=null;try{c=getReadableDatabase().rawQuery("PRAGMA integrity_check",null);String value=c.moveToFirst()?c.getString(0):"sem resposta";out.put("integrity",value);out.put("ok","ok".equalsIgnoreCase(value));out.put("manuals",countTable("manuals"));out.put("tokens",countTable("tokens"));out.put("token_pdfs",countTable("token_pdfs"));}catch(Exception e){try{out.put("ok",false);out.put("error",e.getMessage()==null?e.toString():e.getMessage());}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada; a execução continuou.",ignored);}}finally{if(c!=null)c.close();}return out;}
    public JSONObject masterStateCounts(){JSONObject out=new JSONObject();try{
        out.put("active_heroes",countWorldTableWhere("characters","active=1"));
        out.put("combatants",countWorldTableWhere("combatants","1=1"));
        out.put("npcs",countWorldTableWhere("npc_memory_profiles","1=1"));
        out.put("npc_cognition",countWorldTableWhere("npc_cognitive_state","1=1"));
        out.put("knowledge",countWorldTableWhere("knowledge_facts","1=1"));
        out.put("quests",countWorldTableWhere("quests","status='active'"));
        out.put("factions",countWorldTableWhere("factions","1=1"));
        ensureGuildTables();out.put("guilds",countWorldTableWhere("guilds","1=1"));
        out.put("clocks",countWorldTableWhere("world_clocks","active=1"));
        out.put("events",countWorldTableWhere("world_events","status IN ('scheduled','awaiting_review')"));
        out.put("pending_transactions",countWorldTableWhere("pending_transactions","status='pending'"));
        out.put("tactical_tokens",countWorldTableWhere("tactical_tokens","1=1"));
        out.put("journal",countWorldTableWhere("journal","1=1"));
        out.put("dice",countWorldTableWhere("dice_history","1=1"));
        out.put("manuals",countTable("manuals"));
    }catch(Exception failure){AppLog.e("AppDb","Falha ao contar o estado do barramento.",failure);}return out;}
    private int countWorldTableWhere(String table,String clause){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+" WHERE world_profile=? AND "+clause,new String[]{getActiveMapProfile()});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    private int countTable(String table){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}

    public int schemaVersion(){return DB_VERSION;}
    public boolean quickIntegrityCheck(){
        Cursor c=null;try{c=getReadableDatabase().rawQuery("PRAGMA quick_check(1)",null);return c.moveToFirst()&&"ok".equalsIgnoreCase(c.getString(0));}catch(Exception e){lastDatabaseError=String.valueOf(e.getMessage());AppLog.e("AppDb","Falha no quick_check.",e);return false;}finally{if(c!=null)c.close();}
    }
    public JSONObject databaseHealth5(){JSONObject o=new JSONObject();try{o.put("schema",DB_VERSION).put("quick_check",quickIntegrityCheck()).put("last_error",lastDatabaseError).put("wal",true);}catch(Exception ignored){AppLog.ignored("AppDb","Falha tolerada registrada na consolidação 5.0.",ignored);}return o;}

    /** Índice operacional único das consequências que exigem atenção. */
    public synchronized void syncPendingActionQueue(){
        ensureFiveZeroSchema(getWritableDatabase());String w=getActiveMapProfile();long now=System.currentTimeMillis();SQLiteDatabase sql=getWritableDatabase();
        // A fila é um índice operacional, não outra fonte de verdade. Se a origem deixou de estar
        // pendente, a entrada espelhada também é encerrada automaticamente.
        sql.execSQL("UPDATE pending_action_queue SET status='resolved',updated_at=? WHERE world_profile=? AND status='pending' AND source_kind='transaction' AND NOT EXISTS (SELECT 1 FROM pending_transactions p WHERE p.world_profile=pending_action_queue.world_profile AND CAST(p.id AS TEXT)=pending_action_queue.source_id AND p.status IN ('pending','review','pending_review'))",new Object[]{now,w});
        sql.execSQL("UPDATE pending_action_queue SET status='resolved',updated_at=? WHERE world_profile=? AND status='pending' AND source_kind='world_event' AND NOT EXISTS (SELECT 1 FROM world_events e WHERE e.world_profile=pending_action_queue.world_profile AND CAST(e.id AS TEXT)=pending_action_queue.source_id AND e.status IN ('awaiting_review','blocked'))",new Object[]{now,w});
        sql.execSQL("UPDATE pending_action_queue SET status='resolved',updated_at=? WHERE world_profile=? AND status='pending' AND source_kind='activity' AND NOT EXISTS (SELECT 1 FROM life_activities a WHERE a.world_profile=pending_action_queue.world_profile AND CAST(a.id AS TEXT)=pending_action_queue.source_id AND a.status IN ('aguardando_regra','aguardando_rolagem','aguardando_aplicacao'))",new Object[]{now,w});
        for(JSONObject x:queryList("SELECT id,title,summary,payload_json,effect_key,created_at FROM pending_transactions WHERE world_profile=? AND status IN ('pending','review','pending_review')",new String[]{w}))upsertPendingAction("transaction",String.valueOf(x.optLong("id")),"state_mutation",x.optString("title"),x.optString("summary"),"pending",0,80,x.optString("payload_json","{}"),"",nonEmpty(x.optString("effect_key"),"tx:"+x.optLong("id")),x.optLong("created_at",now));
        for(JSONObject x:queryList("SELECT id,title,description,due_day,status,consequence_json,event_key,created_at FROM world_events WHERE world_profile=? AND status IN ('awaiting_review','blocked')",new String[]{w}))upsertPendingAction("world_event",String.valueOf(x.optLong("id")),"event_review",x.optString("title"),x.optString("description"),"pending",x.optInt("due_day"),90,x.optString("consequence_json","{}"),"",nonEmpty(x.optString("event_key"),"event:"+x.optLong("id")),x.optLong("created_at",now));
        for(JSONObject x:queryList("SELECT id,title,status,rule_source,rule_excerpt,resolution_json,created_at FROM life_activities WHERE world_profile=? AND status IN ('aguardando_regra','aguardando_rolagem','aguardando_aplicacao')",new String[]{w}))upsertPendingAction("activity",String.valueOf(x.optLong("id")),x.optString("status"),x.optString("title"),x.optString("status"),"pending",0,70,x.optString("resolution_json","{}"),x.optString("rule_source")+" "+x.optString("rule_excerpt"),"activity:"+x.optLong("id")+":"+x.optString("status"),x.optLong("created_at",now));
    }
    private static String nonEmpty(String a,String b){return a==null||a.trim().isEmpty()?b:a.trim();}
    public long upsertPendingAction(String sourceKind,String sourceId,String actionKind,String title,String detail,String status,int dueDay,int priority,String payload,String ruleQuery,String effectKey,long createdAt){
        ensureFiveZeroSchema(getWritableDatabase());ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("source_kind",sourceKind);v.put("source_id",sourceId);v.put("action_kind",actionKind);v.put("title",title==null?"Pendência":title);v.put("detail",detail==null?"":detail);v.put("status",status==null?"pending":status);v.put("due_day",Math.max(0,dueDay));v.put("priority",priority);v.put("payload_json",validJsonObject(payload));v.put("rule_query",ruleQuery==null?"":ruleQuery);v.put("effect_key",effectKey);v.put("created_at",createdAt>0?createdAt:System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());long id=getWritableDatabase().insertWithOnConflict("pending_action_queue",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1){getWritableDatabase().update("pending_action_queue",v,"world_profile=? AND effect_key=?",new String[]{getActiveMapProfile(),effectKey});JSONObject found=queryOne("SELECT id FROM pending_action_queue WHERE world_profile=? AND effect_key=?",new String[]{getActiveMapProfile(),effectKey});return found.optLong("id");}return id;
    }
    public JSONArray listPendingActions(String status,int limit){syncPendingActionQueue();String where="world_profile=?";ArrayList<String> args=new ArrayList<String>();args.add(getActiveMapProfile());if(status!=null&&!status.trim().isEmpty()){where+=" AND status=?";args.add(status.trim());}return new JSONArray(queryList("SELECT * FROM pending_action_queue WHERE "+where+" ORDER BY priority DESC,CASE WHEN due_day>0 THEN due_day ELSE 2147483647 END,created_at LIMIT "+Math.max(1,Math.min(1000,limit)),args.toArray(new String[0])));}
    public void resolvePendingAction(long id,String status){ContentValues v=new ContentValues();v.put("status",status==null?"resolved":status);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("pending_action_queue",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}

    /** O Atlas PF2e já traz suas posições no próprio pacote canônico. Não injeta catálogos de outros jogos. */
    public synchronized int applyBundledResolvedLocations(String packageId){
        if(packageId==null||packageId.trim().isEmpty())return 0;
        // Compatibilidade com chamadores antigos: valida apenas que o pacote existe.
        // Nunca altera placementStatus nem promove posições aproximadas a verificadas.
        return getMapPackage(packageId).length()>0?0:0;
    }
    private String getLogicalMapIdForPackage(String packageId){JSONObject p=queryOne("SELECT manifest_json FROM map_packages WHERE package_id=? LIMIT 1",new String[]{packageId});return logicalMapId(p.optString("manifest_json"),packageId);}
    private boolean mapHexExists5(String packageId,String hex){return queryOne("SELECT hex_id FROM map_hex_atlas WHERE package_id=? AND hex_id=? LIMIT 1",new String[]{packageId,hex}).length()>0;}

    public synchronized JSONObject exportAll() throws Exception {
        String profile=getActiveMapProfile();
        JSONObject root=new JSONObject();
        root.put("format","braseiro-pathfinder2e-backup").put("version",22).put("backupScope","campaign").put("systemId","pathfinder_2e");
        root.put("appVersion",BuildConfig.VERSION_NAME);
        root.put("worldProfile",profile).put("exportedAt",System.currentTimeMillis());
        root.put("campaign",getCampaign());
        root.put("storyState",getStoryState());
        root.put("sceneState",getSceneState());
        root.put("npcMemories",new JSONArray(queryList("SELECT * FROM npc_memory_profiles WHERE world_profile=? ORDER BY updated_at",new String[]{profile})));
        root.put("auditLog",new JSONArray(queryList("SELECT * FROM audit_log WHERE world_profile=? ORDER BY created_at",new String[]{profile})));
        root.put("characters",new JSONArray(listCharacters()));
        root.put("journal",new JSONArray(listJournal()));
        root.put("combatants",new JSONArray(listCombatants()));
        root.put("combatState",getCombatState());
        root.put("diceHistory",new JSONArray(queryList("SELECT * FROM dice_history WHERE world_profile=? ORDER BY created_at",new String[]{profile})));
        JSONObject portableMap=portableMapState(getMapState());
        String mapId=portableMap.optString("logical_map_id");
        root.put("mapState",portableMap);
        root.put("mapCorrections",new JSONArray(listMapCorrections(mapId)));
        root.put("mapHexNotes",new JSONArray(listMapHexNotes(profile,mapId)));
        ensureCampaignPoiSchema(getWritableDatabase());
        root.put("campaignPoiMeta",queryOne("SELECT * FROM campaign_poi_meta WHERE world_profile=?",new String[]{profile}));
        root.put("campaignPois",new JSONArray(queryList("SELECT * FROM campaign_pois WHERE world_profile=? ORDER BY created_at,poi_id",new String[]{profile})));
        root.put("campaignPoiHistory",new JSONArray(queryList("SELECT h.* FROM campaign_poi_history h JOIN campaign_pois p ON p.poi_id=h.poi_id WHERE p.world_profile=? ORDER BY h.id",new String[]{profile})));
        root.put("campaignPoiProposals",new JSONArray(queryList("SELECT * FROM campaign_poi_proposals WHERE world_profile=? ORDER BY updated_at",new String[]{profile})));
        root.put("partyState",getPartyState());
        root.put("partyActions",new JSONArray(queryList("SELECT * FROM party_action_queue WHERE world_profile=? ORDER BY created_at",new String[]{profile})));
        root.put("characterControls",new JSONArray(queryList("SELECT * FROM character_control_profiles WHERE world_profile=? ORDER BY character_id",new String[]{profile})));
        root.put("knowledgeFacts",new JSONArray(queryList("SELECT * FROM knowledge_facts WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("quests",new JSONArray(queryList("SELECT * FROM quests WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("factions",new JSONArray(queryList("SELECT * FROM factions WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("worldClocks",new JSONArray(queryList("SELECT * FROM world_clocks WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("worldEvents",new JSONArray(queryList("SELECT * FROM world_events WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("pendingTransactions",new JSONArray(queryList("SELECT * FROM pending_transactions WHERE world_profile=? ORDER BY id",new String[]{profile})));
        syncPendingActionQueue();root.put("pendingActions",new JSONArray(queryList("SELECT * FROM pending_action_queue WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("effectReceipts",new JSONArray(queryList("SELECT * FROM effect_receipts WHERE world_profile=? ORDER BY applied_at,effect_key",new String[]{profile})));
        root.put("sessionMemories",new JSONArray(queryList("SELECT * FROM session_memories WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("tacticalTokens",new JSONArray(listRawTacticalTokensForBackup()));
        root.put("masterRuntimeState",getMasterRuntimeState());
        root.put("environmentalDirectorState",getEnvironmentalDirectorState());
        root.put("travelRulesState",getTravelRulesState());
        ensureLivingWorldTables(getWritableDatabase());
        root.put("livingWorldState",queryOne("SELECT * FROM living_world_state WHERE world_profile=?",new String[]{profile}));
        root.put("heroLifeProfiles",new JSONArray(queryList("SELECT * FROM hero_life_profiles WHERE world_profile=? ORDER BY character_id",new String[]{profile})));
        root.put("lifeRelationships",new JSONArray(queryList("SELECT * FROM life_relationships WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("lifeActivities",new JSONArray(queryList("SELECT * FROM life_activities WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("livingTroupes",new JSONArray(queryList("SELECT * FROM living_troupes WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("masterEpisodes",new JSONArray(queryList("SELECT * FROM master_episodes WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("npcCognitiveStates",new JSONArray(queryList("SELECT * FROM npc_cognitive_state WHERE world_profile=? ORDER BY npc_name",new String[]{profile})));
        ensureGuildTables();
        root.put("guilds",new JSONArray(queryList("SELECT * FROM guilds WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("guildRanks",new JSONArray(queryList("SELECT * FROM guild_ranks WHERE world_profile=? ORDER BY guild_id,rank_order,id",new String[]{profile})));
        root.put("guildMembers",new JSONArray(queryList("SELECT * FROM guild_members WHERE world_profile=? ORDER BY guild_id,id",new String[]{profile})));
        root.put("guildContracts",new JSONArray(queryList("SELECT * FROM guild_contracts WHERE world_profile=? ORDER BY guild_id,id",new String[]{profile})));
        root.put("guildProjects",new JSONArray(queryList("SELECT * FROM guild_projects WHERE world_profile=? ORDER BY guild_id,id",new String[]{profile})));
        root.put("guildSites",new JSONArray(queryList("SELECT * FROM guild_sites WHERE world_profile=? ORDER BY guild_id,id",new String[]{profile})));
        root.put("guildLedger",new JSONArray(queryList("SELECT * FROM guild_ledger WHERE world_profile=? ORDER BY guild_id,id",new String[]{profile})));
        root.put("guildRelations",new JSONArray(queryList("SELECT * FROM guild_relations WHERE world_profile=? ORDER BY guild_id,id",new String[]{profile})));
        ensureSixZeroSchema(getWritableDatabase());
        root.put("worldSeed",queryOne("SELECT * FROM world_seeds WHERE world_profile=?",new String[]{profile}));
        root.put("emergentArcs",new JSONArray(queryList("SELECT * FROM emergent_arcs WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("settlementStates",new JSONArray(queryList("SELECT * FROM settlement_states WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("worldHistoryEvents",new JSONArray(queryList("SELECT * FROM world_history_events WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("rumors",new JSONArray(queryList("SELECT * FROM rumors WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("rumorKnowledge",new JSONArray(queryList("SELECT * FROM rumor_knowledge WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("campaignLegacies",new JSONArray(queryList("SELECT * FROM campaign_legacies WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("npcVoiceProfiles",new JSONArray(queryList("SELECT * FROM npc_voice_profiles WHERE world_profile=? ORDER BY npc_name",new String[]{profile})));
        root.put("tacticalScenes",new JSONArray(queryList("SELECT * FROM tactical_scenes WHERE world_profile=? ORDER BY id",new String[]{profile})));
        root.put("localTableSecrets",new JSONArray(queryList("SELECT * FROM local_table_secrets WHERE world_profile=? ORDER BY id",new String[]{profile})));
        KingmakerRepository kingmakerRepository=new KingmakerRepository(this);JSONObject kingmaker=kingmakerRepository.exportJson();Iterator<String> kingmakerKeys=kingmaker.keys();while(kingmakerKeys.hasNext()){String key=kingmakerKeys.next();root.put(key,kingmaker.opt(key));}root.put("kingmakerContinuityCheckpoint",new KingmakerSoloDirector(this,kingmakerRepository).snapshot().optJSONObject("continuity_checkpoint"));
        root.put("derivedIndexesExcluded",true);
        root.put("externalLibrariesExcluded",new JSONArray().put("manuals").put("manual_rag_parts").put("manual_local_pages").put("tokens").put("token_pdfs").put("pdf_tokens").put("audio_library").put("map_package_files"));
        root.put("restoreContract","A importação substitui somente a campanha Pathfinder 2e indicada; bibliotecas externas permanecem preservadas.");
        return root;
    }

    private JSONObject portableMapState(JSONObject source)throws Exception{
        JSONObject out=new JSONObject();String packageId=source.optString("map_package_id");String logicalId=source.optString("logical_map_id",logicalMapId(source.optString("manifest_json"),packageId));
        out.put("logical_map_id",logicalId).put("manifest_json",source.optString("manifest_json")).put("current_hex_id",source.optString("current_hex_id")).put("overlay_visible",source.optInt("overlay_visible",1)).put("overlay_mode",source.optString("overlay_mode","dynamic")).put("min_label_zoom",source.optDouble("min_label_zoom",1.6)).put("reveals_json",source.optString("reveals_json","{}")).put("grid_type",source.optString("grid_type","Quadrada")).put("grid_size",source.optDouble("grid_size",64)).put("fog_enabled",source.optInt("fog_enabled",0)).put("token_x",source.optDouble("token_x",.5)).put("token_y",source.optDouble("token_y",.5)).put("scale",source.optDouble("scale",1)).put("offset_x",source.optDouble("offset_x",0)).put("offset_y",source.optDouble("offset_y",0)).put("position_locked",source.optInt("position_locked",0)).put("destination_hex_id",source.optString("destination_hex_id")).put("route_json",source.optString("route_json","[]")).put("route_owner",source.optString("route_owner","Jogador"));
        return out;
    }

    public synchronized void importAll(JSONObject root) throws Exception {
        BackupContractValidator.requireValid(root);
        if(!"braseiro-pathfinder2e-backup".equals(root.optString("format")))throw new Exception("Arquivo de backup incompatível com Pathfinder 2e.");if(!"pathfinder_2e".equals(root.optString("systemId","pathfinder_2e")))throw new Exception("Backup pertence a outro sistema.");
        int backupVersion=root.optInt("version",0);if(backupVersion<=0||backupVersion>22)throw new Exception("Versão de backup não suportada: "+backupVersion+".");
        String scope=root.optString("backupScope",backupVersion>=12?"campaign":"legacy");if(backupVersion>=12&&!"campaign".equals(scope))throw new Exception("Escopo de backup inválido.");
        String backupProfile=normalizeMapProfile(root.optString("worldProfile",getActiveMapProfile()));
        if(!backupProfile.equals(getActiveMapProfile()))throw new Exception("Este backup não pertence ao perfil Pathfinder 2e ativo.");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            JSONObject cp=root.optJSONObject("campaign");if(cp!=null)saveCampaign(cp.optString("name"),cp.optString("rules_profile"),cp.optString("tone"),cp.optInt("feats")==1,cp.optInt("multiclass")==1,cp.optInt("flanking")==1,cp.optString("notes"));
            JSONObject ss=root.optJSONObject("storyState");
            Map<Long,Long> characterIdMap=new HashMap<Long,Long>();
            JSONArray chars=root.optJSONArray("characters");if(chars!=null){db.delete("characters","world_profile=?",new String[]{backupProfile});for(int i=0;i<chars.length();i++){JSONObject c=new JSONObject(chars.getJSONObject(i).toString());CharacterIdentityCoherence.repairLegacyRow(c);long oldId=c.optLong("id",0);long cid=addCharacter(c.optString("name"),c.optString("species"),c.optString("class_name"),c.optInt("level",1),c.optInt("hp",1),c.optInt("max_hp",1),c.optInt("ac",10),c.optInt("str",10),c.optInt("dex",10),c.optInt("con",10),c.optInt("int_score",10),c.optInt("wis",10),c.optInt("cha",10),c.optInt("active",1)==1,c.optString("notes"));if(oldId>0&&cid>0)characterIdMap.put(oldId,cid);updateCharacterRuleDetails(cid,c.optString("background"),c.optString("visual_description"),c.optString("proficiencies"),c.optString("equipment"),c.optString("spells"),c.optString("rules_source"));updateCharacterSheet(cid,c.optString("sheet_json","{}"));String token=c.optString("token_uri");if(isUriReadable(token))setCharacterToken(cid,token);}}
            if(root.has("journal")){db.delete("journal","world_profile=?",new String[]{backupProfile});JSONArray j=root.optJSONArray("journal");if(j!=null)for(int i=0;i<j.length();i++){JSONObject x=j.getJSONObject(i);addJournal(x.optString("title"),x.optString("body"),x.optString("kind"));}}
            Map<Long,Long> combatantIdMap=root.has("combatants")?restoreCombatants(db,root.optJSONArray("combatants")):new HashMap<Long,Long>();
            Map<Long,Long> diceIdMap=root.has("diceHistory")?restoreDiceHistory(db,root.optJSONArray("diceHistory"),backupProfile,backupVersion,characterIdMap):new HashMap<Long,Long>();
            JSONObject combatState=root.optJSONObject("combatState");if(combatState!=null){ContentValues stateValues=jsonValues(combatState);stateValues.remove("id");stateValues.remove("world_profile");db.update("combat_state_profiles",stateValues,"world_profile=?",new String[]{backupProfile});}
            JSONObject ms=root.optJSONObject("mapState");String logicalMapId=ms==null?getMapStateForProfile(backupProfile).optString("logical_map_id"):restorePortableMapState(backupProfile,ms);
            String legacyMapPackageId=ms==null?"":ms.optString("map_package_id");
            JSONArray mapCorrections=root.optJSONArray("mapCorrections");if(mapCorrections!=null&&logicalMapId.length()>0){db.delete("map_hex_corrections","map_id=?",new String[]{logicalMapId});restoreMapCorrections(db,logicalMapId,legacyMapPackageId,mapCorrections);}
            JSONArray mapHexNotes=root.optJSONArray("mapHexNotes");if(mapHexNotes!=null&&logicalMapId.length()>0){db.delete("map_hex_notes","world_profile=? AND map_id=?",new String[]{backupProfile,logicalMapId});restoreMapNotes(db,backupProfile,logicalMapId,legacyMapPackageId,mapHexNotes);}
            ensureCampaignPoiSchema(db);
            if(backupVersion<21&&!root.has("campaignPois")){db.delete("campaign_poi_history","poi_id IN (SELECT poi_id FROM campaign_pois WHERE world_profile=?)",new String[]{backupProfile});db.delete("campaign_poi_proposals","world_profile=?",new String[]{backupProfile});db.delete("campaign_pois","world_profile=?",new String[]{backupProfile});db.delete("campaign_poi_meta","world_profile=?",new String[]{backupProfile});}
            if(root.has("campaignPois")){db.delete("campaign_poi_history","poi_id IN (SELECT poi_id FROM campaign_pois WHERE world_profile=?)",new String[]{backupProfile});db.delete("campaign_poi_proposals","world_profile=?",new String[]{backupProfile});db.delete("campaign_pois","world_profile=?",new String[]{backupProfile});JSONArray pois=profileRowsForRestore(root.optJSONArray("campaignPois"),backupProfile,backupVersion);for(int i=0;i<pois.length();i++){JSONObject row=pois.optJSONObject(i);if(row==null||row.optInt("canonical",0)!=0)continue;ContentValues v=jsonValues(row);v.put("world_profile",backupProfile);v.put("canonical",0);db.insertOrThrow("campaign_pois",null,v);}JSONArray hist=root.optJSONArray("campaignPoiHistory");if(hist!=null)for(int i=0;i<hist.length();i++){JSONObject row=hist.optJSONObject(i);if(row==null)continue;ContentValues v=jsonValues(row);v.remove("id");db.insertOrThrow("campaign_poi_history",null,v);}JSONArray proposals=profileRowsForRestore(root.optJSONArray("campaignPoiProposals"),backupProfile,backupVersion);for(int i=0;i<proposals.length();i++){JSONObject row=proposals.optJSONObject(i);if(row==null)continue;ContentValues v=jsonValues(row);v.put("world_profile",backupProfile);db.insertWithOnConflict("campaign_poi_proposals",null,v,SQLiteDatabase.CONFLICT_REPLACE);}JSONObject poiMeta=root.optJSONObject("campaignPoiMeta");if(poiMeta!=null){ContentValues v=jsonValues(poiMeta);v.put("world_profile",backupProfile);db.insertWithOnConflict("campaign_poi_meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
            JSONObject scene=root.optJSONObject("sceneState");if(scene!=null)saveSceneState(scene);
            JSONArray npcs=root.optJSONArray("npcMemories");if(npcs!=null){db.delete("npc_memory_profiles","world_profile=?",new String[]{backupProfile});for(int i=0;i<npcs.length();i++)upsertNpcMemory(npcs.optJSONObject(i));}
            JSONArray audit=root.optJSONArray("auditLog");if(audit!=null){JSONArray safeAudit=profileRowsForRestore(audit,backupProfile,backupVersion);if(backupVersion>=12||safeAudit.length()>0){db.delete("audit_log","world_profile=?",new String[]{backupProfile});for(int i=0;i<safeAudit.length();i++){JSONObject x=safeAudit.optJSONObject(i);if(x==null)continue;ContentValues v=jsonValues(x);v.remove("id");v.put("world_profile",backupProfile);db.insertOrThrow("audit_log",null,v);}}}
            JSONObject partyState=root.optJSONObject("partyState");if(partyState!=null)savePartyState(partyState);
            if(root.has("partyActions"))restorePartyActions(db,root.optJSONArray("partyActions"),characterIdMap);
            if(root.has("characterControls"))restoreCharacterControls(db,root.optJSONArray("characterControls"),characterIdMap);
            if(root.has("knowledgeFacts"))restoreProfileRowsWithoutId(db,"knowledge_facts",root.optJSONArray("knowledgeFacts"),backupProfile,backupVersion);
            Map<Long,Long> factionIdMap=root.has("factions")?restoreFactions(db,root.optJSONArray("factions")):new HashMap<Long,Long>();
            Map<Long,Long> questIdMap=root.has("quests")?restoreQuests(db,root.optJSONArray("quests"),characterIdMap,factionIdMap):new HashMap<Long,Long>();
            ensureLivingWorldTables(db);
            BackupReferenceRemapper.Maps livingPartialMaps=new BackupReferenceRemapper.Maps(characterIdMap,diceIdMap,combatantIdMap,factionIdMap,new HashMap<Long,Long>(),new HashMap<Long,Long>());
            Map<Long,Long> activityIdMap=root.has("lifeActivities")?restoreLifeActivities(db,root.optJSONArray("lifeActivities"),backupProfile,characterIdMap,livingPartialMaps):new HashMap<Long,Long>();
            Map<Long,Long> troupeIdMap=root.has("livingTroupes")?restoreProfileRowsWithIdMap(db,"living_troupes",root.optJSONArray("livingTroupes"),backupProfile,backupVersion):new HashMap<Long,Long>();
            Map<Long,Long> clockIdMap=root.has("worldClocks")?restoreProfileRowsWithIdMap(db,"world_clocks",root.optJSONArray("worldClocks"),backupProfile,backupVersion):new HashMap<Long,Long>();
            BackupReferenceRemapper.Maps partialMaps=new BackupReferenceRemapper.Maps(characterIdMap,diceIdMap,combatantIdMap,factionIdMap,new HashMap<Long,Long>(),clockIdMap,troupeIdMap,activityIdMap,questIdMap,new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<String,String>(),new HashMap<String,String>());
            JSONArray portableEvents=prepareLegacyWorldEventRows(root.optJSONArray("worldEvents"));
            Map<Long,Long> eventIdMap=root.has("worldEvents")?restoreProfileRowsWithIdMap(db,"world_events",remapRows(portableEvents,partialMaps),backupProfile,backupVersion):new HashMap<Long,Long>();
            BackupReferenceRemapper.Maps referenceMaps=new BackupReferenceRemapper.Maps(characterIdMap,diceIdMap,combatantIdMap,factionIdMap,eventIdMap,clockIdMap,troupeIdMap,activityIdMap,questIdMap,new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<String,String>(),new HashMap<String,String>());
            JSONArray portableTransactions=prepareLegacyTransactionRows(root.optJSONArray("pendingTransactions"));
            Map<Long,Long> transactionIdMap=root.has("pendingTransactions")?restoreProfileRowsWithIdMap(db,"pending_transactions",remapRows(portableTransactions,referenceMaps),backupProfile,backupVersion):new HashMap<Long,Long>();
            if(root.has("pendingActions"))restoreProfileRowsWithIdMap(db,"pending_action_queue",root.optJSONArray("pendingActions"),backupProfile,backupVersion);
            referenceMaps=new BackupReferenceRemapper.Maps(characterIdMap,diceIdMap,combatantIdMap,factionIdMap,eventIdMap,clockIdMap,troupeIdMap,activityIdMap,questIdMap,new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>(),transactionIdMap,new HashMap<String,String>(),new HashMap<String,String>());
            rewriteMappedJsonRows(db,"life_activities",root.optJSONArray("lifeActivities"),activityIdMap,referenceMaps,new String[]{"check_json","cost_json","reward_json","resolution_json"});
            rewriteMappedJsonRows(db,"living_troupes",root.optJSONArray("livingTroupes"),troupeIdMap,referenceMaps,new String[]{"members_json","itinerary_json","resources_json","travel_progress_json"});
            rewriteMappedJsonRows(db,"quests",root.optJSONArray("quests"),questIdMap,referenceMaps,new String[]{"progress_json"});
            rewriteMappedJsonRows(db,"world_events",portableEvents,eventIdMap,referenceMaps,new String[]{"consequence_json","trigger_json","procedure_json","transaction_key","application_receipt"});
            rewriteMappedJsonRows(db,"pending_transactions",portableTransactions,transactionIdMap,referenceMaps,new String[]{"payload_json","effect_key"});
            if(ss!=null){JSONObject pending=parseBackupObject(ss.optString("pending_test_json","{}"));JSONObject remappedPending=BackupReferenceRemapper.remapObject(pending,referenceMaps);saveStoryState(ss.optInt("started")==1,ss.optString("last_narration"),ss.optString("visual_prompt"),ss.optString("options_json","[]"),remappedPending.length()==0?"":remappedPending.toString(),ss.optString("last_action"));saveSceneOptionHistory(ss.optString("option_history_json","[]"));}
            if(root.has("effectReceipts"))restoreProfileRowsWithoutId(db,"effect_receipts",remapRows(root.optJSONArray("effectReceipts"),referenceMaps),backupProfile,backupVersion);
            backfillLegacyTransactionIdentities(db,backupProfile);
            backfillLegacyWorldEventIdentities(db,backupProfile);
            if(root.has("sessionMemories"))restoreProfileRowsWithoutId(db,"session_memories",root.optJSONArray("sessionMemories"),backupProfile,backupVersion);
            if(root.has("tacticalTokens"))restoreTacticalTokens(db,root.optJSONArray("tacticalTokens"),combatantIdMap);
            JSONObject masterRuntime=root.optJSONObject("masterRuntimeState");if(masterRuntime!=null)saveMasterRuntimeState(masterRuntime);
            JSONObject environmentalState=root.optJSONObject("environmentalDirectorState");if(environmentalState!=null)saveEnvironmentalDirectorState(environmentalState);
            JSONObject travelRulesState=root.optJSONObject("travelRulesState");if(travelRulesState!=null)saveTravelRulesState(travelRulesState);
            JSONObject livingWorldState=root.optJSONObject("livingWorldState");if(livingWorldState!=null){ContentValues v=jsonValues(livingWorldState);v.remove("world_profile");v.put("updated_at",System.currentTimeMillis());db.update("living_world_state",v,"world_profile=?",new String[]{backupProfile});}
            if(root.has("heroLifeProfiles"))restoreLivingRows(db,"hero_life_profiles",root.optJSONArray("heroLifeProfiles"),backupProfile,characterIdMap,true);
            if(root.has("lifeRelationships"))restoreLivingRows(db,"life_relationships",root.optJSONArray("lifeRelationships"),backupProfile,characterIdMap,false);
            if(root.has("masterEpisodes"))restoreProfileRowsWithoutId(db,"master_episodes",root.optJSONArray("masterEpisodes"),backupProfile,backupVersion);
            if(root.has("npcCognitiveStates"))restoreNpcCognitiveStates(db,root.optJSONArray("npcCognitiveStates"));
            if(root.has("guilds"))restoreGuildSubsystem(db,root,characterIdMap,factionIdMap,questIdMap,backupVersion);
            if(backupVersion>=19){ensureSixZeroSchema(db);restoreSixZeroRows(db,root,backupProfile,characterIdMap);}
            KingmakerRepository restoredKingmaker=new KingmakerRepository(this);restoredKingmaker.restoreJson(root,backupProfile);
            JSONObject kingmakerRestoreAudit=restoredKingmaker.validateRestoreAgainst(root);if(!kingmakerRestoreAudit.optBoolean("healthy",false))throw new IllegalStateException("A restauração foi revertida pela auditoria Kingmaker: "+kingmakerRestoreAudit.optJSONArray("problems"));
            JSONObject expectedContinuity=root.optJSONObject("kingmakerContinuityCheckpoint");if(expectedContinuity!=null&&expectedContinuity.length()>0){JSONObject actualContinuity=new KingmakerSoloDirector(this,restoredKingmaker).snapshot().optJSONObject("continuity_checkpoint");JSONObject continuityAudit=KingmakerContinuityContract.compare(expectedContinuity,actualContinuity);if(!continuityAudit.optBoolean("ok",false))throw new IllegalStateException("A restauração foi revertida pela auditoria de continuidade Solo: "+continuityAudit.optJSONArray("differences"));}
            JSONObject restoreAudit=auditRestoredReferences(backupProfile);if(!restoreAudit.optBoolean("healthy",false))throw new IllegalStateException("A restauração foi revertida pela auditoria de referências: "+restoreAudit.optJSONArray("problems"));
            addAuditEvent("backup","Restauração auditada","Referências internas e agregado Kingmaker verificados após importar backup v"+backupVersion,"{}",new JSONObject().put("references",restoreAudit).put("kingmaker",kingmakerRestoreAudit).toString(),false);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }


    private void restoreSixZeroRows(SQLiteDatabase db,JSONObject root,String profile,Map<Long,Long> characterIdMap)throws Exception{
        JSONObject seed=root.optJSONObject("worldSeed");if(seed!=null){ContentValues v=jsonValues(seed);v.put("world_profile",profile);db.insertWithOnConflict("world_seeds",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        String[] keys={"emergentArcs","settlementStates","worldHistoryEvents","rumors","campaignLegacies","npcVoiceProfiles","tacticalScenes"};
        String[] tables={"emergent_arcs","settlement_states","world_history_events","rumors","campaign_legacies","npc_voice_profiles","tactical_scenes"};
        for(int k=0;k<keys.length;k++){db.delete(tables[k],"world_profile=?",new String[]{profile});JSONArray rows=root.optJSONArray(keys[k]);if(rows==null)continue;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);db.insertWithOnConflict(tables[k],null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
        // rumor_knowledge references rumor ids; resolve by old rumor_key rather than physical id when possible.
        db.delete("rumor_knowledge","world_profile=?",new String[]{profile});JSONArray knowledge=root.optJSONArray("rumorKnowledge"),oldRumors=root.optJSONArray("rumors");java.util.HashMap<Long,String> oldKeys=new java.util.HashMap<Long,String>();if(oldRumors!=null)for(int i=0;i<oldRumors.length();i++){JSONObject r=oldRumors.optJSONObject(i);if(r!=null)oldKeys.put(r.optLong("id"),r.optString("rumor_key"));}if(knowledge!=null)for(int i=0;i<knowledge.length();i++){JSONObject row=knowledge.optJSONObject(i);if(row==null)continue;String rk=oldKeys.get(row.optLong("rumor_id"));JSONObject current=rk==null?new JSONObject():queryOne("SELECT id FROM rumors WHERE world_profile=? AND rumor_key=?",new String[]{profile,rk});if(current.length()==0)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);v.put("rumor_id",current.optLong("id"));db.insertWithOnConflict("rumor_knowledge",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        db.delete("local_table_secrets","world_profile=?",new String[]{profile});JSONArray secrets=root.optJSONArray("localTableSecrets");if(secrets!=null)for(int i=0;i<secrets.length();i++){JSONObject row=secrets.optJSONObject(i);if(row==null)continue;Long mapped=characterIdMap.get(row.optLong("character_id"));if(mapped==null||mapped<=0)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);v.put("character_id",mapped);db.insert("local_table_secrets",null,v);}
    }

    private String restorePortableMapState(String profile,JSONObject ms)throws Exception{
        if(ms==null)return "";String logicalId=ms.optString("logical_map_id",logicalMapId(ms.optString("manifest_json"),ms.optString("map_package_id")));JSONObject pack=getLatestMapPackageByMapId(profile,logicalId);String packageId=pack.optString("package_id");String image=pack.optString("image_uri");String overlay=pack.optString("overlay_uri");String manifest=pack.length()>0?pack.optString("manifest_json",ms.optString("manifest_json")):ms.optString("manifest_json");
        ContentValues v=new ContentValues();v.put("logical_map_id",logicalId);v.put("image_uri",image);v.put("overlay_uri",overlay);v.put("manifest_json",manifest);v.put("map_package_id",packageId);v.put("current_hex_id",ms.optString("current_hex_id"));v.put("overlay_visible",ms.optInt("overlay_visible",1));v.put("overlay_mode",pack.length()>0?pack.optString("overlay_mode",ms.optString("overlay_mode","dynamic")):ms.optString("overlay_mode","dynamic"));v.put("min_label_zoom",pack.length()>0?pack.optDouble("min_label_zoom",ms.optDouble("min_label_zoom",1.6)):ms.optDouble("min_label_zoom",1.6));v.put("reveals_json",ms.optString("reveals_json","{}"));v.put("grid_type",ms.optString("grid_type","Quadrada"));v.put("grid_size",ms.optDouble("grid_size",64));v.put("fog_enabled",ms.optInt("fog_enabled",0));v.put("token_x",ms.optDouble("token_x",.5));v.put("token_y",ms.optDouble("token_y",.5));v.put("scale",ms.optDouble("scale",1));v.put("offset_x",ms.optDouble("offset_x",0));v.put("offset_y",ms.optDouble("offset_y",0));v.put("position_locked",ms.optInt("position_locked",ms.optString("current_hex_id").length()>0?1:0));v.put("destination_hex_id",ms.optString("destination_hex_id"));v.put("route_json",ms.optString("route_json","[]"));v.put("route_owner",ms.optString("route_owner","Jogador"));updateMapStateForProfile(profile,v);return logicalId;
    }
    private static void restoreMapCorrections(SQLiteDatabase db,String mapId,String legacyPackageId,JSONArray rows)throws Exception{for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null||row.optString("hex_id").length()==0||!sameLogicalMap(row.optString("map_id"),mapId,legacyPackageId))continue;ContentValues v=jsonValues(row);v.put("map_id",mapId);db.insertWithOnConflict("map_hex_corrections",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
    private static void restoreMapNotes(SQLiteDatabase db,String profile,String mapId,String legacyPackageId,JSONArray rows)throws Exception{for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);String sourceWorld=row==null?"":row.optString("world_profile");if(row==null||row.optString("hex_id").length()==0||(sourceWorld.length()>0&&!normalizeMapProfile(sourceWorld).equals(profile))||!sameLogicalMap(row.optString("map_id"),mapId,legacyPackageId))continue;ContentValues v=jsonValues(row);v.put("world_profile",profile);v.put("map_id",mapId);db.insertWithOnConflict("map_hex_notes",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
    private static boolean sameLogicalMap(String source,String logical,String legacyPackageId){if(source==null||source.trim().length()==0)return false;String value=source.trim();return value.equals(logical)||value.equals(legacyPackageId)||value.startsWith(logical+"@");}

    private Map<Long,Long> restoreDiceHistory(SQLiteDatabase db,JSONArray rows,String profile,int backupVersion,Map<Long,Long> characterIdMap)throws Exception{
        Map<Long,Long> idMap=new HashMap<Long,Long>();JSONArray safe=profileRowsForRestore(rows,profile,backupVersion);if(backupVersion<12&&safe.length()==0)return idMap;
        db.delete("dice_history","world_profile=?",new String[]{profile});
        for(int i=0;i<safe.length();i++){JSONObject row=safe.optJSONObject(i);if(row==null)continue;long oldId=row.optLong("id",0),oldActor=row.optLong("actor_character_id",0);ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);if(oldActor>0){Long mapped=characterIdMap.get(oldActor);v.put("actor_character_id",mapped==null?0:mapped);}long newId=db.insertOrThrow("dice_history",null,v);if(oldId>0&&newId>0)idMap.put(oldId,newId);}
        return idMap;
    }
    private static JSONArray profileRowsForRestore(JSONArray rows,String profile,int backupVersion){JSONArray out=new JSONArray();if(rows==null)return out;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;String source=row.optString("world_profile","").trim();if(source.length()>0&&profile.equals(normalizeMapProfile(source)))out.put(row);}return out;}

    private Map<Long,Long> restoreProfileRowsWithIdMap(SQLiteDatabase db,String table,JSONArray rows,String profile,int backupVersion)throws Exception{
        Map<Long,Long> idMap=new HashMap<Long,Long>();JSONArray safe=profileRowsForRestore(rows,profile,backupVersion);if(backupVersion<12&&safe.length()==0)return idMap;db.delete(table,"world_profile=?",new String[]{profile});
        for(int i=0;i<safe.length();i++){JSONObject row=safe.optJSONObject(i);if(row==null)continue;long oldId=row.optLong("id",0);ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);long newId=db.insertOrThrow(table,null,v);if(oldId>0&&newId>0)idMap.put(oldId,newId);}
        return idMap;
    }

    private static JSONArray remapRows(JSONArray rows,BackupReferenceRemapper.Maps maps)throws Exception{
        JSONArray out=new JSONArray();if(rows==null)return out;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;JSONObject copy=BackupReferenceRemapper.remapObject(new JSONObject(row.toString()),maps);java.util.Iterator<String> keys=copy.keys();java.util.ArrayList<String> names=new java.util.ArrayList<String>();while(keys.hasNext())names.add(keys.next());for(String key:names){Object value=copy.opt(key);if(!(value instanceof String))continue;String raw=((String)value).trim();try{if(raw.startsWith("{"))copy.put(key,BackupReferenceRemapper.remapObject(new JSONObject(raw),maps).toString());else if(raw.startsWith("["))copy.put(key,BackupReferenceRemapper.remapArray(new JSONArray(raw),maps).toString());}catch(Exception invalidJson){AppLog.w("AppDb","Campo JSON de backup preservado sem remapeamento: "+key,invalidJson);}}out.put(copy);}return out;
    }

    private static JSONArray prepareLegacyTransactionRows(JSONArray rows)throws Exception{
        JSONArray out=new JSONArray();if(rows==null)return out;
        for(int i=0;i<rows.length();i++){
            JSONObject source=rows.optJSONObject(i);if(source==null)continue;JSONObject copy=new JSONObject(source.toString());
            String key=copy.optString("effect_key","").trim();if(key.length()==0)key=LegacyTransactionIdentity.keyForBackupRow(source);
            JSONObject payload=parseBackupObject(copy.optString("payload_json","{}"));payload.put("_effect_key",key);
            copy.put("effect_key",key).put("payload_json",payload.toString());out.put(copy);
        }
        return out;
    }

    private static JSONArray prepareLegacyWorldEventRows(JSONArray rows)throws Exception{
        JSONArray out=new JSONArray();if(rows==null)return out;
        for(int i=0;i<rows.length();i++){
            JSONObject source=rows.optJSONObject(i);if(source==null)continue;JSONObject copy=new JSONObject(source.toString());
            String eventKey=copy.optString("event_key").trim();if(eventKey.length()==0)eventKey=LegacyWorldEventIdentity.keyForBackupRow(source);
            String transactionKey=copy.optString("transaction_key").trim();if(transactionKey.length()==0)transactionKey=LegacyWorldEventIdentity.transactionKey(eventKey);
            copy.put("event_key",eventKey).put("transaction_key",transactionKey);
            if(("resolved".equalsIgnoreCase(copy.optString("status"))||"applied".equalsIgnoreCase(copy.optString("consequence_status")))&&copy.optString("application_receipt").trim().length()==0)copy.put("application_receipt",transactionKey);
            out.put(copy);
        }
        return out;
    }

    private static void rewriteMappedJsonRows(SQLiteDatabase db,String table,JSONArray original,Map<Long,Long> idMap,BackupReferenceRemapper.Maps maps,String[] fields)throws Exception{
        if(original==null||idMap==null||fields==null)return;
        for(int i=0;i<original.length();i++){
            JSONObject row=original.optJSONObject(i);if(row==null)continue;Long newId=idMap.get(row.optLong("id",0));if(newId==null||newId<=0)continue;ContentValues values=new ContentValues();boolean hasValues=false;
            for(String field:fields){if(!row.has(field))continue;String raw=row.optString(field,"");try{String trimmed=raw.trim();if(trimmed.startsWith("{"))values.put(field,BackupReferenceRemapper.remapObject(new JSONObject(trimmed),maps).toString());else if(trimmed.startsWith("["))values.put(field,BackupReferenceRemapper.remapArray(new JSONArray(trimmed),maps).toString());else values.put(field,BackupReferenceRemapper.remapString(field,raw,maps));hasValues=true;}catch(Exception failure){throw new IllegalStateException("Referência inválida em "+table+"."+field+" do ID antigo "+row.optLong("id",0)+".",failure);}}
            if(hasValues&&db.update(table,values,"id=?",new String[]{String.valueOf(newId)})!=1)throw new IllegalStateException("Não foi possível regravar as referências de "+table+" ID "+newId+".");
        }
    }

    private JSONObject auditRestoredReferences(String profile)throws Exception{
        JSONObject out=new JSONObject();JSONArray problems=new JSONArray();SQLiteDatabase sql=getReadableDatabase();Cursor c=null;
        try{
            c=sql.rawQuery("PRAGMA foreign_key_check",null);while(c.moveToNext())problems.put("chave estrangeira órfã em "+c.getString(0));c.close();c=null;
            c=sql.rawQuery("SELECT d.id FROM dice_history d LEFT JOIN characters c ON c.id=d.actor_character_id AND c.world_profile=d.world_profile WHERE d.world_profile=? AND d.actor_character_id>0 AND c.id IS NULL",new String[]{profile});while(c.moveToNext())problems.put("rolagem "+c.getLong(0)+" aponta para personagem inexistente");c.close();c=null;
            c=sql.rawQuery("SELECT d.id FROM dice_history d JOIN characters c ON c.id=d.actor_character_id AND c.world_profile=d.world_profile WHERE d.world_profile=? AND TRIM(COALESCE(d.actor_name,''))<>'' AND lower(TRIM(d.actor_name))<>lower(TRIM(c.name))",new String[]{profile});while(c.moveToNext())problems.put("rolagem "+c.getLong(0)+" está ligada ao nome de personagem errado");c.close();c=null;
            c=sql.rawQuery("SELECT a.id,a.character_id,a.check_json FROM life_activities a LEFT JOIN characters c ON c.id=a.character_id AND c.world_profile=a.world_profile WHERE a.world_profile=? AND c.id IS NULL",new String[]{profile});while(c.moveToNext())problems.put("atividade "+c.getLong(0)+" aponta para personagem inexistente");c.close();c=null;
            c=sql.rawQuery("SELECT effect_key,COUNT(*) FROM pending_transactions WHERE world_profile=? AND status IN ('pending','reviewing') AND TRIM(COALESCE(effect_key,''))<>'' GROUP BY effect_key HAVING COUNT(*)>1",new String[]{profile});while(c.moveToNext())problems.put("recibo/effect_key pendente colidiu: "+c.getString(0));c.close();c=null;
            c=sql.rawQuery("SELECT id FROM pending_transactions WHERE world_profile=? AND TRIM(COALESCE(effect_key,''))=''",new String[]{profile});while(c.moveToNext())problems.put("transação "+c.getLong(0)+" permaneceu sem effect_key");c.close();c=null;
            c=sql.rawQuery("SELECT p.id FROM pending_transactions p LEFT JOIN effect_receipts r ON r.world_profile=p.world_profile AND r.effect_key=p.effect_key WHERE p.world_profile=? AND p.status='applied' AND r.effect_key IS NULL",new String[]{profile});while(c.moveToNext())problems.put("transação aplicada "+c.getLong(0)+" não possui recibo");c.close();c=null;
            c=sql.rawQuery("SELECT id FROM world_events WHERE world_profile=? AND (TRIM(COALESCE(event_key,''))='' OR TRIM(COALESCE(transaction_key,''))='' OR transaction_key<>('world_event:'||event_key))",new String[]{profile});while(c.moveToNext())problems.put("evento "+c.getLong(0)+" permaneceu sem identidade transacional válida");c.close();c=null;
            c=sql.rawQuery("SELECT e.id FROM world_events e LEFT JOIN effect_receipts r ON r.world_profile=e.world_profile AND r.effect_key=e.transaction_key WHERE e.world_profile=? AND (e.status='resolved' OR e.consequence_status='applied') AND (TRIM(COALESCE(e.application_receipt,''))='' OR r.effect_key IS NULL)",new String[]{profile});while(c.moveToNext())problems.put("evento aplicado "+c.getLong(0)+" não possui recibo completo");c.close();c=null;
            c=sql.rawQuery("SELECT title,due_at,due_day,COUNT(*) FROM world_events WHERE world_profile=? AND status IN ('scheduled','awaiting_review') GROUP BY lower(title),due_at,due_day,consequence_json HAVING COUNT(*)>1",new String[]{profile});while(c.moveToNext())problems.put("evento restaurado em duplicidade: "+c.getString(0));c.close();c=null;
            for(JSONObject activity:queryList("SELECT id,character_id,check_json FROM life_activities WHERE world_profile=?",new String[]{profile})){
                JSONObject check=parseBackupObject(activity.optString("check_json","{}"));long rollId=check.optLong("roll_id",0);if(rollId<=0)continue;JSONObject roll=queryOne("SELECT * FROM dice_history WHERE id=? AND world_profile=?",new String[]{String.valueOf(rollId),profile});
                if(roll.length()==0)problems.put("atividade "+activity.optLong("id")+" aponta para rolagem antiga/inexistente");else if(roll.optLong("actor_character_id",0)!=activity.optLong("character_id",0))problems.put("atividade "+activity.optLong("id")+" está ligada à rolagem do personagem errado");
            }
            for(JSONObject pending:queryList("SELECT id,payload_json FROM pending_transactions WHERE world_profile=? AND status IN ('pending','reviewing')",new String[]{profile}))if(pending.optString("payload_json").matches("(?s).*(?:character|roll|combatant|faction|event|clock|troupe|activity|quest|scene|map|route|transaction):0\\b.*"))problems.put("pendência "+pending.optLong("id")+" contém referência antiga sem destino");
            c=sql.rawQuery("SELECT gm.id FROM guild_members gm LEFT JOIN guild_ranks gr ON gr.id=gm.rank_id AND gr.guild_id=gm.guild_id LEFT JOIN characters ch ON ch.id=gm.character_id AND ch.world_profile=gm.world_profile WHERE gm.world_profile=? AND ((gm.rank_id>0 AND gr.id IS NULL) OR (gm.member_kind='hero' AND (gm.character_id<=0 OR ch.id IS NULL)) OR (gm.member_kind='npc' AND TRIM(COALESCE(gm.npc_name,''))=''))",new String[]{profile});while(c.moveToNext())problems.put("membro de guilda "+c.getLong(0)+" possui referência inválida");c.close();c=null;
            c=sql.rawQuery("SELECT gc.id FROM guild_contracts gc LEFT JOIN quests q ON q.id=gc.linked_quest_id AND q.world_profile=gc.world_profile WHERE gc.world_profile=? AND gc.linked_quest_id>0 AND q.id IS NULL",new String[]{profile});while(c.moveToNext())problems.put("contrato de guilda "+c.getLong(0)+" aponta para missão inexistente");c.close();c=null;
            c=sql.rawQuery("SELECT gr.id FROM guild_relations gr LEFT JOIN guilds target ON target.id=gr.target_guild_id AND target.world_profile=gr.world_profile WHERE gr.world_profile=? AND gr.target_guild_id>0 AND target.id IS NULL",new String[]{profile});while(c.moveToNext())problems.put("relação de guilda "+c.getLong(0)+" aponta para guilda inexistente");c.close();c=null;
            c=sql.rawQuery("SELECT g.id FROM guilds g LEFT JOIN factions f ON f.id=g.faction_id AND f.world_profile=g.world_profile WHERE g.world_profile=? AND g.faction_id>0 AND f.id IS NULL",new String[]{profile});while(c.moveToNext())problems.put("guilda "+c.getLong(0)+" aponta para facção inexistente");c.close();c=null;
            out.put("healthy",problems.length()==0).put("problems",problems).put("foreign_keys_checked",true).put("roll_actor_links_checked",true).put("activity_roll_links_checked",true).put("effect_key_collisions_checked",true).put("event_receipts_checked",true).put("duplicate_events_checked",true).put("guild_references_checked",true);
            return out;
        }finally{if(c!=null)c.close();}
    }

    private static JSONObject parseBackupObject(String raw){try{return new JSONObject(raw==null||raw.trim().length()==0?"{}":raw);}catch(Exception ignored){return new JSONObject();}}

    private Map<Long,Long> restoreLifeActivities(SQLiteDatabase db,JSONArray rows,String profile,Map<Long,Long> characterIdMap,BackupReferenceRemapper.Maps maps)throws Exception{
        Map<Long,Long> idMap=new HashMap<Long,Long>();db.delete("life_activities","world_profile=?",new String[]{profile});if(rows==null)return idMap;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldId=row.optLong("id",0),oldCharacter=row.optLong("character_id",0);Long mappedCharacter=characterIdMap.get(oldCharacter);if(mappedCharacter==null||mappedCharacter<=0)continue;JSONObject copy=new JSONObject(row.toString());copy.put("character_id",mappedCharacter);JSONObject oldCheck=parseBackupObject(row.optString("check_json","{}"));long oldRoll=oldCheck.optLong("roll_id",0);JSONObject remappedCheck=BackupReferenceRemapper.remapObject(oldCheck,maps);copy.put("check_json",remappedCheck.toString());if(oldRoll>0&&remappedCheck.optLong("roll_id",0)<=0){copy.put("status","aguardando_rolagem");copy.put("narrative_result","");}ContentValues v=jsonValues(copy);v.remove("id");v.put("world_profile",profile);long newId=db.insertOrThrow("life_activities",null,v);if(oldId>0&&newId>0)idMap.put(oldId,newId);}
        return idMap;
    }

    private static void restoreRows(SQLiteDatabase db,String table,JSONArray rows)throws Exception{
        db.delete(table,null,null);if(rows==null)return;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;db.insertOrThrow(table,null,jsonValues(row));}
    }
    private static void restoreLivingRows(SQLiteDatabase db,String table,JSONArray rows,String profile,Map<Long,Long> characterIdMap,boolean replace)throws Exception{
        db.delete(table,"world_profile=?",new String[]{profile});if(rows==null)return;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldCharacter=row.optLong("character_id",0),newCharacter=characterIdMap.containsKey(oldCharacter)?characterIdMap.get(oldCharacter):0;if(newCharacter<=0)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);v.put("character_id",newCharacter);long oldRelated=row.optLong("related_character_id",0);if(oldRelated>0)v.put("related_character_id",characterIdMap.containsKey(oldRelated)?characterIdMap.get(oldRelated):0);db.insertWithOnConflict(table,null,v,replace?SQLiteDatabase.CONFLICT_REPLACE:SQLiteDatabase.CONFLICT_ABORT);}
    }
    private void restoreProfileRows(SQLiteDatabase db,String table,JSONArray rows)throws Exception{
        db.delete(table,"world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;ContentValues v=jsonValues(row);v.put("world_profile",getActiveMapProfile());db.insertWithOnConflict(table,null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    }


    private Map<Long,Long> restoreCombatants(SQLiteDatabase db,JSONArray rows)throws Exception{
        Map<Long,Long> idMap=new HashMap<Long,Long>();db.delete("combatants","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return idMap;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldId=row.optLong("id",0);ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());long newId=db.insertOrThrow("combatants",null,v);if(oldId>0&&newId>0)idMap.put(oldId,newId);}
        return idMap;
    }

    /** Exporta apenas as colunas físicas de tactical_tokens. Campos de retrato, PV e origem
     * são derivados de combatants e não podem ser reinseridos nesta tabela. */
    private List<JSONObject> listRawTacticalTokensForBackup(){
        return queryList("SELECT world_profile,combatant_id,name,is_player,x,y,zone,updated_at FROM tactical_tokens WHERE world_profile=? ORDER BY combatant_id",new String[]{getActiveMapProfile()});
    }

    private void restoreTacticalTokens(SQLiteDatabase db,JSONArray rows,Map<Long,Long> combatantIdMap)throws Exception{
        db.delete("tactical_tokens","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            long oldCombatantId=row.optLong("combatant_id",0);Long mapped=combatantIdMap.get(oldCombatantId);if(mapped==null||mapped<=0)continue;
            // Allowlist explícita: também torna restauráveis snapshots 4.9.2 que continham
            // source_id, token_uri, hp e outras colunas do JOIN com combatants.
            ContentValues v=new ContentValues();
            v.put("world_profile",getActiveMapProfile());v.put("combatant_id",mapped);
            v.put("name",row.optString("name","Combatente"));v.put("is_player",row.optInt("is_player",0));
            v.put("x",row.optDouble("x",0.5));v.put("y",row.optDouble("y",0.5));
            v.put("zone",row.optString("zone",""));v.put("updated_at",row.optLong("updated_at",System.currentTimeMillis()));
            db.insertWithOnConflict("tactical_tokens",null,v,SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void restorePartyActions(SQLiteDatabase db,JSONArray rows,Map<Long,Long> characterIdMap)throws Exception{
        db.delete("party_action_queue","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldCharacterId=row.optLong("character_id",0);Long mapped=characterIdMap.get(oldCharacterId);if(mapped==null||mapped<=0)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());v.put("character_id",mapped);db.insertOrThrow("party_action_queue",null,v);}
    }

    private void restoreCharacterControls(SQLiteDatabase db,JSONArray rows,Map<Long,Long> characterIdMap)throws Exception{
        db.delete("character_control_profiles","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldCharacterId=row.optLong("character_id",0);Long mapped=characterIdMap.get(oldCharacterId);if(mapped==null||mapped<=0)continue;ContentValues v=jsonValues(row);v.put("world_profile",getActiveMapProfile());v.put("character_id",mapped);db.insertWithOnConflict("character_control_profiles",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    }

    private void restoreNpcCognitiveStates(SQLiteDatabase db,JSONArray rows)throws Exception{
        db.delete("npc_cognitive_state","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;JSONObject copy=new JSONObject(row.toString());copy.put("name",row.optString("npc_name"));upsertNpcCognitiveState(copy);}
    }

    private Map<Long,Long> restoreFactions(SQLiteDatabase db,JSONArray rows)throws Exception{
        Map<Long,Long> idMap=new HashMap<Long,Long>();db.delete("factions","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return idMap;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldId=row.optLong("id",0);ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());long newId=db.insertOrThrow("factions",null,v);if(oldId>0&&newId>0)idMap.put(oldId,newId);}
        return idMap;
    }

    private Map<Long,Long> restoreQuests(SQLiteDatabase db,JSONArray rows,Map<Long,Long> characterIdMap,Map<Long,Long> factionIdMap)throws Exception{
        Map<Long,Long> idMap=new HashMap<Long,Long>();db.delete("quests","world_profile=?",new String[]{getActiveMapProfile()});if(rows==null)return idMap;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;long oldId=row.optLong("id",0);ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());long oldOwner=row.optLong("owner_character_id",0);Long owner=characterIdMap.get(oldOwner);v.put("owner_character_id",owner==null?0:owner);long oldFaction=row.optLong("faction_id",0);Long faction=factionIdMap.get(oldFaction);v.put("faction_id",faction==null?0:faction);long newId=db.insertOrThrow("quests",null,v);if(oldId>0&&newId>0)idMap.put(oldId,newId);}return idMap;
    }

    private void restoreProfileRowsWithoutId(SQLiteDatabase db,String table,JSONArray rows,String profile,int backupVersion)throws Exception{
        JSONArray safe=profileRowsForRestore(rows,profile,backupVersion);if(backupVersion<12&&safe.length()==0)return;db.delete(table,"world_profile=?",new String[]{profile});
        for(int i=0;i<safe.length();i++){JSONObject row=safe.optJSONObject(i);if(row==null)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",profile);db.insertOrThrow(table,null,v);}
    }

    private static ContentValues jsonValues(JSONObject row)throws Exception{
        ContentValues values=new ContentValues();Iterator<String> keys=row.keys();
        while(keys.hasNext()){String key=keys.next();Object value=row.opt(key);
            if(value==null||value==JSONObject.NULL)values.putNull(key);
            else if(value instanceof Boolean)values.put(key,(Boolean)value?1:0);
            else if(value instanceof Integer)values.put(key,(Integer)value);
            else if(value instanceof Long)values.put(key,(Long)value);
            else if(value instanceof Float)values.put(key,(Float)value);
            else if(value instanceof Double)values.put(key,(Double)value);
            else values.put(key,String.valueOf(value));
        }
        return values;
    }
    // ===== Sistema dedicado de guildas =====
    private NpcPersonalityCatalog personalityCatalog(){NpcPersonalityCatalog c=npcPersonalityCatalog;if(c==null){synchronized(this){c=npcPersonalityCatalog;if(c==null)npcPersonalityCatalog=c=new NpcPersonalityCatalog(appContext);}}return c;}
    private void ensureNpcPersonalityOnIncoming(JSONObject state,String name){if(state==null||name==null||name.trim().length()==0)return;try{JSONObject current=parseJsonObject(state.optString("personality_json","{}"));if(current.length()>0)return;JSONObject memory=findNpcMemoryByName(name);JSONObject selected=personalityCatalog().select(name,memory);if(selected.length()>0)state.put("personality_json",selected.toString());}catch(Exception e){AppLog.w("NpcPersonality","Não foi possível aplicar personalidade a "+name,e);}}
    public void ensureNpcPersonalityProvisioned(String name){if(name==null||name.trim().length()==0)return;try{JSONObject state=queryOne("SELECT * FROM npc_cognitive_state WHERE world_profile=? AND npc_name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),name.trim()});JSONObject current=parseJsonObject(state.optString("personality_json","{}"));if(current.length()>0)return;JSONObject selected=personalityCatalog().select(name,findNpcMemoryByName(name));if(selected.length()==0)return;ContentValues v=new ContentValues();v.put("personality_json",selected.toString());v.put("updated_at",System.currentTimeMillis());if(state.length()==0){v.put("world_profile",getActiveMapProfile());v.put("npc_name",name.trim());getWritableDatabase().insertWithOnConflict("npc_cognitive_state",null,v,SQLiteDatabase.CONFLICT_IGNORE);}else getWritableDatabase().update("npc_cognitive_state",v,"world_profile=? AND npc_name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),name.trim()});}catch(Exception e){AppLog.w("NpcPersonality","Falha ao provisionar personalidade de "+name,e);}}
    public JSONObject backfillNpcPersonalities(){JSONObject out=new JSONObject();int before=0,after=0,updated=0;try{for(JSONObject row:listNpcCognitiveStates(10000))if(parseJsonObject(row.optString("personality_json","{}")).length()>0)before++;JSONArray memories=listNpcMemories(10000);for(int i=0;i<memories.length();i++){String name=memories.optJSONObject(i).optString("name");JSONObject state=getNpcCognitiveState(name);if(parseJsonObject(state.optString("personality_json","{}")).length()==0){ensureNpcPersonalityProvisioned(name);updated++;}}for(JSONObject row:listNpcCognitiveStates(10000))if(parseJsonObject(row.optString("personality_json","{}")).length()>0)after++;out.put("catalog_profiles",personalityCatalog().size()).put("before",before).put("after",after).put("updated",updated).put("complete",after>=listNpcMemories(10000).length());}catch(Exception e){try{out.put("complete",false).put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("AppDb","Falha ao registrar diagnóstico.",ignored);}}return out;}
    private static JSONObject parseJsonObject(String raw){try{return new JSONObject(raw==null||raw.trim().length()==0?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static Object parseJsonValue(String raw){try{String safe=raw==null?"":raw.trim();if(safe.startsWith("["))return new JSONArray(safe);if(safe.startsWith("{"))return new JSONObject(safe);if(safe.length()>0)return safe;}catch(Exception ignored){AppLog.ignored("AppDb","JSON editorial inválido ignorado.",ignored);}return new JSONObject();}
    private static boolean jsonValueHasContent(String raw){Object value=parseJsonValue(raw);if(value instanceof JSONObject)return ((JSONObject)value).length()>0;if(value instanceof JSONArray)return ((JSONArray)value).length()>0;return value!=null&&String.valueOf(value).trim().length()>0;}

    public List<JSONObject> listGuilds(int limit){ensureGuildTables();return queryList("SELECT * FROM guilds WHERE world_profile=? ORDER BY active DESC,influence DESC,name LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile()});}
    public JSONObject getGuild(long id){ensureGuildTables();return queryOne("SELECT * FROM guilds WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});}
    public JSONObject findGuildByName(String name){ensureGuildTables();return queryOne("SELECT * FROM guilds WHERE world_profile=? AND name=? COLLATE NOCASE LIMIT 1",new String[]{getActiveMapProfile(),name==null?"":name.trim()});}
    public long upsertGuildChecked(JSONObject guild)throws Exception{ensureGuildTables();if(guild==null)throw new IllegalArgumentException("Guilda ausente.");String name=guild.optString("name").trim();if(name.length()<3)throw new IllegalArgumentException("Nome de guilda inválido.");long id=guild.optLong("id",0);JSONObject previous=id>0?getGuild(id):findGuildByName(name);ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("name",name);for(String key:new String[]{"type","description","charter","headquarters","currency","current_plan","dues_policy_json","admission_json","benefits_json","services_json","rules_json","stock_json"})v.put(key,guild.has(key)?guild.optString(key):previous.optString(key,"stock_json".equals(key)?"{}":key.endsWith("_json")?"[]":""));v.put("treasury",Math.max(0d,guild.has("treasury")?guild.optDouble("treasury"):previous.optDouble("treasury",0)));v.put("party_reputation",Math.max(-100,Math.min(100,guild.has("party_reputation")?guild.optInt("party_reputation"):previous.optInt("party_reputation",0))));v.put("influence",Math.max(0,Math.min(100,guild.has("influence")?guild.optInt("influence"):previous.optInt("influence",25))));v.put("active",guild.has("active")?(guild.optBoolean("active",true)?1:0):previous.optInt("active",1));v.put("faction_id",guild.has("faction_id")?guild.optLong("faction_id"):previous.optLong("faction_id",0));long now=System.currentTimeMillis();v.put("created_at",previous.length()==0?now:previous.optLong("created_at",now));v.put("updated_at",now);long out;if(previous.length()==0)out=getWritableDatabase().insertOrThrow("guilds",null,v);else{out=previous.optLong("id");if(getWritableDatabase().update("guilds",v,"id=? AND world_profile=?",new String[]{String.valueOf(out),getActiveMapProfile()})!=1)throw new IllegalStateException("Guilda não persistida.");}return out;}
    public void syncGuildFactionChecked(long guildId)throws Exception{JSONObject g=getGuild(guildId);if(g.length()==0)throw new IllegalStateException("Guilda inexistente.");String resources=new JSONObject().put("treasury",g.optDouble("treasury")).put("currency",g.optString("currency")).put("stock",parseJsonObject(g.optString("stock_json","{}"))).toString();long factionId=upsertFaction(g.optString("name"),g.optString("charter",g.optString("description")),resources,g.optInt("party_reputation"),g.optString("current_plan"),g.optString("headquarters"));ContentValues v=new ContentValues();v.put("faction_id",factionId);v.put("updated_at",System.currentTimeMillis());if(getWritableDatabase().update("guilds",v,"id=? AND world_profile=?",new String[]{String.valueOf(guildId),getActiveMapProfile()})!=1)throw new IllegalStateException("Vínculo guilda/facção não persistido.");}
    public void deleteGuild(long id){ensureGuildTables();JSONObject g=getGuild(id);getWritableDatabase().delete("guilds","id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});if(g.optLong("faction_id")>0)getWritableDatabase().delete("factions","id=? AND world_profile=?",new String[]{String.valueOf(g.optLong("faction_id")),getActiveMapProfile()});}
    public List<JSONObject> listGuildRanks(long guildId){ensureGuildTables();return queryList("SELECT * FROM guild_ranks WHERE world_profile=? AND guild_id=? AND active=1 ORDER BY rank_order,id",new String[]{getActiveMapProfile(),String.valueOf(guildId)});}
    public JSONObject defaultGuildRank(long guildId){ensureGuildTables();return queryOne("SELECT * FROM guild_ranks WHERE world_profile=? AND guild_id=? AND active=1 ORDER BY rank_order,id LIMIT 1",new String[]{getActiveMapProfile(),String.valueOf(guildId)});}
    public long addGuildRankChecked(JSONObject rank)throws Exception{ensureGuildTables();long gid=rank.optLong("guild_id");if(getGuild(gid).length()==0)throw new IllegalArgumentException("Guilda da patente não existe.");String name=rank.optString("name").trim();if(name.length()==0)throw new IllegalArgumentException("Patente sem nome.");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("name",name);v.put("rank_order",Math.max(0,rank.optInt("rank_order")));v.put("requirements_json",rank.opt("requirements_json") instanceof JSONObject?rank.optJSONObject("requirements_json").toString():rank.optString("requirements_json","{}"));v.put("benefits_json",rank.opt("benefits_json") instanceof JSONArray?rank.optJSONArray("benefits_json").toString():rank.optString("benefits_json","[]"));v.put("permissions_json",rank.opt("permissions_json") instanceof JSONArray?rank.optJSONArray("permissions_json").toString():rank.optString("permissions_json","[]"));v.put("dues_amount",Math.max(0d,rank.optDouble("dues_amount")));v.put("active",1);long now=System.currentTimeMillis();v.put("created_at",now);v.put("updated_at",now);long id=getWritableDatabase().insertWithOnConflict("guild_ranks",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1){JSONObject row=queryOne("SELECT id FROM guild_ranks WHERE guild_id=? AND name=? COLLATE NOCASE",new String[]{String.valueOf(gid),name});id=row.optLong("id");getWritableDatabase().update("guild_ranks",v,"id=?",new String[]{String.valueOf(id)});}if(id<=0)throw new IllegalStateException("Patente não persistida.");return id;}
    public List<JSONObject> listGuildMembers(long guildId,String status,int limit){ensureGuildTables();String where=status==null||status.trim().length()==0?"":" AND gm.status=?";ArrayList<String> args=new ArrayList<String>();args.add(getActiveMapProfile());args.add(String.valueOf(guildId));if(where.length()>0)args.add(status);String sql="SELECT gm.*,gr.name rank_name,gr.dues_amount rank_dues_amount,c.name hero_name FROM guild_members gm LEFT JOIN guild_ranks gr ON gr.id=gm.rank_id LEFT JOIN characters c ON c.id=gm.character_id WHERE gm.world_profile=? AND gm.guild_id=?"+where+" ORDER BY gm.status='active' DESC,gr.rank_order DESC,COALESCE(c.name,gm.npc_name) LIMIT "+Math.max(1,Math.min(1000,limit));return queryList(sql,args.toArray(new String[0]));}
    public JSONObject findGuildMember(long guildId,String kind,long characterId,String npcName){ensureGuildTables();if("hero".equals(kind))return queryOne("SELECT * FROM guild_members WHERE world_profile=? AND guild_id=? AND member_kind='hero' AND character_id=?",new String[]{getActiveMapProfile(),String.valueOf(guildId),String.valueOf(characterId)});return queryOne("SELECT * FROM guild_members WHERE world_profile=? AND guild_id=? AND member_kind='npc' AND npc_name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),String.valueOf(guildId),npcName==null?"":npcName.trim()});}
    public List<JSONObject> listGuildMembershipsForHero(long characterId){ensureGuildTables();return queryList("SELECT gm.*,g.name guild_name,g.type guild_type,gr.name rank_name,gr.dues_amount rank_dues_amount FROM guild_members gm INNER JOIN guilds g ON g.id=gm.guild_id LEFT JOIN guild_ranks gr ON gr.id=gm.rank_id WHERE gm.world_profile=? AND gm.member_kind='hero' AND gm.character_id=? ORDER BY gm.status='active' DESC,g.influence DESC",new String[]{getActiveMapProfile(),String.valueOf(characterId)});}
    public List<JSONObject> listGuildMembershipsForNpc(String npcName){ensureGuildTables();return queryList("SELECT gm.*,g.name guild_name,g.type guild_type,gr.name rank_name,gr.dues_amount rank_dues_amount FROM guild_members gm INNER JOIN guilds g ON g.id=gm.guild_id LEFT JOIN guild_ranks gr ON gr.id=gm.rank_id WHERE gm.world_profile=? AND gm.member_kind='npc' AND gm.npc_name=? COLLATE NOCASE ORDER BY gm.status='active' DESC,g.influence DESC",new String[]{getActiveMapProfile(),npcName==null?"":npcName.trim()});}
    public JSONObject getGuildMember(long memberId){ensureGuildTables();return queryOne("SELECT gm.*,g.name guild_name,g.type guild_type,gr.name rank_name,gr.dues_amount rank_dues_amount FROM guild_members gm INNER JOIN guilds g ON g.id=gm.guild_id LEFT JOIN guild_ranks gr ON gr.id=gm.rank_id WHERE gm.world_profile=? AND gm.id=?",new String[]{getActiveMapProfile(),String.valueOf(memberId)});}
    public void adjustGuildMemberReputationChecked(long memberId,int delta,double contributionDelta)throws Exception{JSONObject member=getGuildMember(memberId);if(member.length()==0)throw new IllegalArgumentException("Membro de guilda inexistente.");int reputation=Math.max(-100,Math.min(100,member.optInt("reputation",0)+delta));double contribution=Math.max(0d,member.optDouble("contribution",0)+contributionDelta);long rankId=defaultGuildRank(member.optLong("guild_id")).optLong("id",0);for(JSONObject rank:listGuildRanks(member.optLong("guild_id"))){JSONObject req=parseJsonObject(rank.optString("requirements_json","{}"));if(reputation>=req.optInt("reputation",0))rankId=rank.optLong("id");}ContentValues v=new ContentValues();v.put("reputation",reputation);v.put("contribution",contribution);v.put("rank_id",rankId);v.put("updated_at",System.currentTimeMillis());if(getWritableDatabase().update("guild_members",v,"id=? AND world_profile=?",new String[]{String.valueOf(memberId),getActiveMapProfile()})!=1)throw new IllegalStateException("Reputação de guilda não persistida.");}
    public long addGuildMemberChecked(JSONObject member)throws Exception{ensureGuildTables();long gid=member.optLong("guild_id");if(getGuild(gid).length()==0)throw new IllegalArgumentException("Guilda do membro não existe.");String kind="hero".equalsIgnoreCase(member.optString("member_kind"))?"hero":"npc";long cid=member.optLong("character_id",0);String npc=member.optString("npc_name").trim();if("hero".equals(kind)&&cid<=0)throw new IllegalArgumentException("Herói inválido.");if("npc".equals(kind)&&npc.length()==0)throw new IllegalArgumentException("NPC inválido.");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("member_kind",kind);v.put("character_id",cid);v.put("npc_name",npc);v.put("rank_id",member.optLong("rank_id",0));v.put("status",member.optString("status","active"));v.put("reputation",Math.max(-100,Math.min(100,member.optInt("reputation",0))));v.put("contribution",Math.max(0d,member.optDouble("contribution",0)));v.put("joined_day",Math.max(1,member.optInt("joined_day",getSceneState().optInt("day_number",1))));v.put("dues_paid_day",Math.max(0,member.optInt("dues_paid_day",0)));v.put("dues_due_day",Math.max(0,member.optInt("dues_due_day",0)));v.put("last_dues_notice_day",Math.max(0,member.optInt("last_dues_notice_day",0)));v.put("notes",member.optString("notes"));long now=System.currentTimeMillis();v.put("created_at",now);v.put("updated_at",now);long id=getWritableDatabase().insertWithOnConflict("guild_members",null,v,SQLiteDatabase.CONFLICT_IGNORE);JSONObject old=findGuildMember(gid,kind,cid,npc);if(id==-1&&old.length()>0){id=old.optLong("id");getWritableDatabase().update("guild_members",v,"id=?",new String[]{String.valueOf(id)});}if(id<=0)throw new IllegalStateException("Membro não persistido.");if("npc".equals(kind)){ensureNpcPersonalityProvisioned(npc);JSONObject state=getNpcCognitiveState(npc);state.put("name",npc).put("faction",getGuild(gid).optString("name"));upsertNpcCognitiveState(state);}return id;}
    public void updateGuildMemberStatus(long memberId,String status){ContentValues v=new ContentValues();v.put("status",status);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("guild_members",v,"id=? AND world_profile=?",new String[]{String.valueOf(memberId),getActiveMapProfile()});}
    public int countGuildMembers(long guildId){return countQuery(getReadableDatabase(),"SELECT COUNT(*) FROM guild_members WHERE world_profile=? AND guild_id=? AND status='active'",new String[]{getActiveMapProfile(),String.valueOf(guildId)});}
    public List<JSONObject> listGuildContracts(long guildId,String status,int limit){ensureGuildTables();String where=status==null||status.trim().length()==0?"":" AND status=?";ArrayList<String> args=new ArrayList<String>();args.add(getActiveMapProfile());args.add(String.valueOf(guildId));if(where.length()>0)args.add(status);return queryList("SELECT * FROM guild_contracts WHERE world_profile=? AND guild_id=?"+where+" ORDER BY status='open' DESC,deadline_day,id DESC LIMIT "+Math.max(1,Math.min(1000,limit)),args.toArray(new String[0]));}
    public long addGuildContractChecked(JSONObject c)throws Exception{ensureGuildTables();long gid=c.optLong("guild_id");if(getGuild(gid).length()==0)throw new IllegalArgumentException("Guilda do contrato não existe.");String title=c.optString("title").trim();if(title.length()==0)throw new IllegalArgumentException("Contrato sem título.");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("title",title);for(String k:new String[]{"description","issuer","status","assignee_kind","assignee_name","requirements_json","reward_json","penalty_json","progress_json"})v.put(k,c.optString(k,k.endsWith("_json")?"{}":""));v.put("assignee_id",c.optLong("assignee_id",0));v.put("deadline_day",Math.max(0,c.optInt("deadline_day",0)));v.put("linked_quest_id",Math.max(0,c.optLong("linked_quest_id",0)));long now=System.currentTimeMillis();v.put("created_at",now);v.put("updated_at",now);long id=getWritableDatabase().insertOrThrow("guild_contracts",null,v);if(id<=0)throw new IllegalStateException("Contrato não persistido.");return id;}
    public void updateGuildContractStatus(long id,String status,JSONObject progress)throws Exception{ContentValues v=new ContentValues();v.put("status",status);if(progress!=null)v.put("progress_json",progress.toString());v.put("updated_at",System.currentTimeMillis());if(getWritableDatabase().update("guild_contracts",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()})!=1)throw new IllegalStateException("Contrato não atualizado.");}
    public JSONObject getGuildContract(long id){ensureGuildTables();return queryOne("SELECT c.*,g.name guild_name,g.faction_id FROM guild_contracts c INNER JOIN guilds g ON g.id=c.guild_id WHERE c.world_profile=? AND c.id=?",new String[]{getActiveMapProfile(),String.valueOf(id)});}
    public long acceptGuildContractChecked(long contractId,long ownerCharacterId)throws Exception{
        JSONObject c=getGuildContract(contractId);if(c.length()==0)throw new IllegalArgumentException("Contrato inexistente.");
        if(!"open".equals(c.optString("status")))throw new IllegalStateException("Este contrato não está aberto.");
        Object reward=parseJsonValue(c.optString("reward_json","{}")),penalty=parseJsonValue(c.optString("penalty_json","{}")),requirements=parseJsonValue(c.optString("requirements_json","{}"));
        JSONObject stakes=new JSONObject().put("reward",reward).put("penalty",penalty).put("guild_contract_id",contractId).put("guild_id",c.optLong("guild_id"));
        JSONObject questProgress=new JSONObject().put("guild_contract_id",contractId).put("guild_id",c.optLong("guild_id")).put("requirements",requirements).put("accepted_day",getSceneState().optInt("day_number",1));
        long questId=applyQuestUpdateChecked(new JSONObject().put("title",c.optString("title")).put("objective",c.optString("description")).put("stakes",stakes.toString()).put("status","active").put("stage","Contrato aceito de "+c.optString("guild_name")).put("owner_character_id",Math.max(0,ownerCharacterId)).put("faction_id",c.optLong("faction_id",0)).put("progress_json",questProgress.toString()));
        ContentValues v=new ContentValues();v.put("status","accepted");v.put("assignee_kind",ownerCharacterId>0?"hero":"party");v.put("assignee_id",Math.max(0,ownerCharacterId));v.put("linked_quest_id",questId);v.put("progress_json",new JSONObject().put("accepted_day",getSceneState().optInt("day_number",1)).put("reward_pending_review",false).put("requirements",requirements).toString());v.put("updated_at",System.currentTimeMillis());
        if(getWritableDatabase().update("guild_contracts",v,"id=? AND world_profile=?",new String[]{String.valueOf(contractId),getActiveMapProfile()})!=1)throw new IllegalStateException("Contrato não aceito.");return questId;
    }
    public void completeGuildContractChecked(long contractId)throws Exception{
        JSONObject c=getGuildContract(contractId);if(c.length()==0)throw new IllegalArgumentException("Contrato inexistente.");
        if(!"accepted".equals(c.optString("status")))throw new IllegalStateException("Somente contratos aceitos podem ser concluídos.");
        JSONObject progress=parseJsonObject(c.optString("progress_json","{}"));progress.put("completed_day",getSceneState().optInt("day_number",1)).put("reward_pending_review",jsonValueHasContent(c.optString("reward_json","{}")));
        updateGuildContractStatus(contractId,"completed",progress);long questId=c.optLong("linked_quest_id",0);if(questId>0)updateQuestStatus(questId,"completed","Contrato concluído para "+c.optString("guild_name"));
        addAuditEvent("guilda","Contrato concluído",c.optString("title")+" • recompensa aguardando revisão mecânica",c.toString(),getGuildContract(contractId).toString(),true);
    }
    public int countGuildContracts(long gid,String status){return countQuery(getReadableDatabase(),"SELECT COUNT(*) FROM guild_contracts WHERE world_profile=? AND guild_id=? AND status=?",new String[]{getActiveMapProfile(),String.valueOf(gid),status});}
    public List<JSONObject> listGuildProjects(long guildId,String status,int limit){ensureGuildTables();String where=status==null||status.trim().length()==0?"":" AND status=?";ArrayList<String> args=new ArrayList<String>();args.add(getActiveMapProfile());args.add(String.valueOf(guildId));if(where.length()>0)args.add(status);return queryList("SELECT * FROM guild_projects WHERE world_profile=? AND guild_id=?"+where+" ORDER BY status='active' DESC,updated_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),args.toArray(new String[0]));}
    public long addGuildProjectChecked(JSONObject p)throws Exception{ensureGuildTables();long gid=p.optLong("guild_id");if(getGuild(gid).length()==0)throw new IllegalArgumentException("Guilda do projeto não existe.");String title=p.optString("title").trim();if(title.length()==0)throw new IllegalArgumentException("Projeto sem título.");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("title",title);v.put("description",p.optString("description"));v.put("kind",p.optString("kind","general"));v.put("status",p.optString("status","active"));v.put("progress",Math.max(0d,p.optDouble("progress",0)));v.put("goal",Math.max(1d,p.optDouble("goal",100)));v.put("progress_per_day",Math.max(.1d,p.optDouble("progress_per_day",5)));v.put("cost_json",p.optString("cost_json","{}"));v.put("result_json",p.optString("result_json","{}"));v.put("location",p.optString("location"));v.put("started_day",Math.max(1,p.optInt("started_day",getSceneState().optInt("day_number",1))));v.put("completed_day",0);long now=System.currentTimeMillis();v.put("created_at",now);v.put("updated_at",now);long id=getWritableDatabase().insertOrThrow("guild_projects",null,v);if(id<=0)throw new IllegalStateException("Projeto não persistido.");return id;}
    public int countGuildProjects(long gid,String status){return countQuery(getReadableDatabase(),"SELECT COUNT(*) FROM guild_projects WHERE world_profile=? AND guild_id=? AND status=?",new String[]{getActiveMapProfile(),String.valueOf(gid),status});}
    public List<JSONObject> listGuildSites(long guildId){ensureGuildTables();return queryList("SELECT * FROM guild_sites WHERE world_profile=? AND guild_id=? AND active=1 ORDER BY site_type,name",new String[]{getActiveMapProfile(),String.valueOf(guildId)});}
    public long addGuildSiteChecked(JSONObject site)throws Exception{ensureGuildTables();long gid=site.optLong("guild_id");JSONObject guild=getGuild(gid);if(guild.length()==0)throw new IllegalArgumentException("Guilda do local não existe.");String name=site.optString("name").trim();if(name.length()==0)throw new IllegalArgumentException("Local sem nome.");String hex=site.optString("hex_id").trim();if(hex.length()>0){String packageId=getMapState().optString("map_package_id");if(packageId.length()==0||getRawMapHex(packageId,hex).length()==0)throw new IllegalArgumentException("Hexágono da sede não existe no atlas ativo.");}ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("name",name);v.put("site_type",site.optString("site_type","sede"));v.put("location",site.optString("location"));v.put("hex_id",hex);v.put("services_json",site.optString("services_json","[]"));v.put("defenses_json",site.optString("defenses_json","{}"));v.put("stock_json",site.optString("stock_json","{}"));v.put("active",1);long now=System.currentTimeMillis();v.put("created_at",now);v.put("updated_at",now);long id=getWritableDatabase().insertOrThrow("guild_sites",null,v);if(id<=0)throw new IllegalStateException("Local da guilda não persistido.");if(hex.length()>0){String packageId=getMapState().optString("map_package_id");saveMapHexNote(packageId,hex,name,guild.optString("name")+" • "+site.optString("site_type","sede"),"⚒","guild_system");}return id;}
    public void recordGuildDuesPaymentChecked(final long memberId,final double requestedAmount,final String actor)throws Exception{
        final JSONObject member=getGuildMember(memberId);if(member.length()==0)throw new IllegalArgumentException("Membro da guilda inexistente.");
        final double due=Math.max(0d,member.optDouble("rank_dues_amount",0d));if(due<=0d)throw new IllegalStateException("A patente atual não possui mensalidade.");
        final double amount=requestedAmount<=0d?due:requestedAmount;if(Math.abs(amount-due)>.0001d)throw new IllegalArgumentException("O pagamento deve corresponder exatamente à mensalidade de "+due+" "+getGuild(member.optLong("guild_id")).optString("currency","po")+".");
        runInTransactionChecked(new CheckedTransaction(){public void run()throws Exception{
            JSONObject current=getGuildMember(memberId);if(current.length()==0)throw new IllegalStateException("O membro deixou de existir durante o pagamento.");
            int day=Math.max(1,getSceneState().optInt("day_number",1));addGuildLedgerEntryChecked(new JSONObject().put("guild_id",current.optLong("guild_id")).put("entry_type","dues").put("amount",amount).put("description","Mensalidade de "+(current.optString("hero_name").length()>0?current.optString("hero_name"):current.optString("npc_name"))).put("actor",actor==null||actor.trim().length()==0?"membro":actor.trim()).put("day_number",day));
            ContentValues v=new ContentValues();v.put("dues_paid_day",day);v.put("dues_due_day",0);v.put("last_dues_notice_day",0);v.put("updated_at",System.currentTimeMillis());
            if(getWritableDatabase().update("guild_members",v,"id=? AND world_profile=?",new String[]{String.valueOf(memberId),getActiveMapProfile()})!=1)throw new IllegalStateException("Pagamento não vinculado ao membro.");
            addAuditEvent("guilda","Mensalidade registrada",current.optString("guild_name")+" • "+amount+" "+getGuild(current.optLong("guild_id")).optString("currency","po"),current.toString(),getGuildMember(memberId).toString(),true);
        }});
    }
    public List<JSONObject> listGuildLedger(long guildId,int limit){ensureGuildTables();return queryList("SELECT * FROM guild_ledger WHERE world_profile=? AND guild_id=? ORDER BY created_at DESC LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{getActiveMapProfile(),String.valueOf(guildId)});}
    public long addGuildLedgerEntryChecked(JSONObject entry)throws Exception{ensureGuildTables();long gid=entry.optLong("guild_id");JSONObject guild=getGuild(gid);if(guild.length()==0)throw new IllegalArgumentException("Guilda do lançamento não existe.");double amount=entry.optDouble("amount");double after=guild.optDouble("treasury")+amount;if(after<0d)throw new IllegalArgumentException("A guilda não possui fundos suficientes.");ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("entry_type",entry.optString("entry_type","adjustment"));v.put("amount",amount);v.put("currency",entry.optString("currency",guild.optString("currency","po")));v.put("description",entry.optString("description"));v.put("actor",entry.optString("actor"));v.put("day_number",Math.max(1,entry.optInt("day_number",getSceneState().optInt("day_number",1))));v.put("created_at",System.currentTimeMillis());long id=getWritableDatabase().insertOrThrow("guild_ledger",null,v);ContentValues g=new ContentValues();g.put("treasury",after);g.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("guilds",g,"id=? AND world_profile=?",new String[]{String.valueOf(gid),getActiveMapProfile()});if(id<=0)throw new IllegalStateException("Livro-caixa não persistido.");syncGuildFactionChecked(gid);return id;}
    public List<JSONObject> listGuildRelations(long guildId){ensureGuildTables();return queryList("SELECT * FROM guild_relations WHERE world_profile=? AND guild_id=? ORDER BY relation DESC,target_name",new String[]{getActiveMapProfile(),String.valueOf(guildId)});}
    public long upsertGuildRelationChecked(JSONObject r)throws Exception{ensureGuildTables();long gid=r.optLong("guild_id");if(getGuild(gid).length()==0)throw new IllegalArgumentException("Guilda da relação não existe.");String target=r.optString("target_name").trim();if(target.length()==0)throw new IllegalArgumentException("Alvo da relação ausente.");JSONObject old=queryOne("SELECT * FROM guild_relations WHERE world_profile=? AND guild_id=? AND target_kind=? AND target_name=? COLLATE NOCASE",new String[]{getActiveMapProfile(),String.valueOf(gid),r.optString("target_kind","guild"),target});ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);v.put("target_kind",r.optString("target_kind","guild"));v.put("target_guild_id",r.optLong("target_guild_id",0));v.put("target_faction_id",r.optLong("target_faction_id",0));v.put("target_name",target);v.put("relation",Math.max(-100,Math.min(100,r.optInt("relation",0))));v.put("stance",r.optString("stance","neutral"));v.put("treaties_json",r.optString("treaties_json","[]"));v.put("notes",r.optString("notes"));v.put("updated_at",System.currentTimeMillis());long id;if(old.length()==0)id=getWritableDatabase().insertOrThrow("guild_relations",null,v);else{id=old.optLong("id");getWritableDatabase().update("guild_relations",v,"id=?",new String[]{String.valueOf(id)});}return id;}
    public int countGuildRows(String table){if(!new java.util.HashSet<String>(java.util.Arrays.asList("guilds","guild_ranks","guild_members","guild_contracts","guild_projects","guild_sites","guild_ledger","guild_relations")).contains(table))return 0;return countQuery(getReadableDatabase(),"SELECT COUNT(*) FROM "+table+" WHERE world_profile=?",new String[]{getActiveMapProfile()});}
    public void applyGuildSimulationActionChecked(JSONObject action)throws Exception{if(action==null)return;String kind=action.optString("kind");if("project_progress".equals(kind)){long id=action.optLong("project_id");ContentValues v=new ContentValues();v.put("progress",Math.max(0d,action.optDouble("progress_after")));if(action.optBoolean("completed",false)){v.put("status","completed");v.put("completed_day",getSceneState().optInt("day_number",1));}v.put("updated_at",System.currentTimeMillis());if(getWritableDatabase().update("guild_projects",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()})!=1)throw new IllegalStateException("Projeto de guilda não avançou.");}else if("contract_expired".equals(kind)){JSONObject c=getGuildContract(action.optLong("contract_id"));updateGuildContractStatus(action.optLong("contract_id"),"expired",new JSONObject().put("expired_day",getSceneState().optInt("day_number",1)));if(c.optLong("linked_quest_id",0)>0)updateQuestStatus(c.optLong("linked_quest_id"),"failed","Prazo do contrato expirado");}else if("dues_due".equals(kind)){ContentValues v=new ContentValues();v.put("dues_due_day",Math.max(1,action.optInt("due_day")));v.put("last_dues_notice_day",Math.max(1,action.optInt("notice_day")));v.put("updated_at",System.currentTimeMillis());if(getWritableDatabase().update("guild_members",v,"id=? AND world_profile=?",new String[]{String.valueOf(action.optLong("member_id")),getActiveMapProfile()})!=1)throw new IllegalStateException("Pendência de mensalidade não persistida.");}}

    private void restoreGuildSubsystem(SQLiteDatabase db,JSONObject root,Map<Long,Long> characterIdMap,Map<Long,Long> factionIdMap,Map<Long,Long> questIdMap,int backupVersion)throws Exception{createGuildTables(db);for(String t:new String[]{"guild_relations","guild_ledger","guild_sites","guild_projects","guild_contracts","guild_members","guild_ranks","guilds"})db.delete(t,"world_profile=?",new String[]{getActiveMapProfile()});Map<Long,Long> guildMap=new HashMap<Long,Long>(),rankMap=new HashMap<Long,Long>();JSONArray guilds=root.optJSONArray("guilds");if(guilds!=null)for(int i=0;i<guilds.length();i++){JSONObject row=guilds.optJSONObject(i);if(row==null)continue;long old=row.optLong("id");ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());Long faction=factionIdMap.get(row.optLong("faction_id"));v.put("faction_id",faction==null?0:faction);long id=db.insertOrThrow("guilds",null,v);if(old>0)guildMap.put(old,id);}JSONArray ranks=root.optJSONArray("guildRanks");if(ranks!=null)for(int i=0;i<ranks.length();i++){JSONObject row=ranks.optJSONObject(i);if(row==null)continue;Long gid=guildMap.get(row.optLong("guild_id"));if(gid==null)continue;long old=row.optLong("id");ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);long id=db.insertOrThrow("guild_ranks",null,v);if(old>0)rankMap.put(old,id);}restoreGuildRows(db,root.optJSONArray("guildMembers"),"guild_members",guildMap,rankMap,characterIdMap,questIdMap);restoreGuildRows(db,root.optJSONArray("guildContracts"),"guild_contracts",guildMap,rankMap,characterIdMap,questIdMap);restoreGuildRows(db,root.optJSONArray("guildProjects"),"guild_projects",guildMap,rankMap,characterIdMap,questIdMap);restoreGuildRows(db,root.optJSONArray("guildSites"),"guild_sites",guildMap,rankMap,characterIdMap,questIdMap);restoreGuildRows(db,root.optJSONArray("guildLedger"),"guild_ledger",guildMap,rankMap,characterIdMap,questIdMap);restoreGuildRows(db,root.optJSONArray("guildRelations"),"guild_relations",guildMap,rankMap,characterIdMap,questIdMap);}
    private void restoreGuildRows(SQLiteDatabase db,JSONArray rows,String table,Map<Long,Long> guildMap,Map<Long,Long> rankMap,Map<Long,Long> characterIdMap,Map<Long,Long> questIdMap)throws Exception{if(rows==null)return;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;Long gid=guildMap.get(row.optLong("guild_id"));if(gid==null)continue;ContentValues v=jsonValues(row);v.remove("id");v.put("world_profile",getActiveMapProfile());v.put("guild_id",gid);if(v.containsKey("rank_id")){Long rank=rankMap.get(row.optLong("rank_id"));v.put("rank_id",rank==null?0:rank);}if(v.containsKey("character_id")){Long ch=characterIdMap.get(row.optLong("character_id"));v.put("character_id",ch==null?0:ch);}if(v.containsKey("assignee_id")&&"hero".equals(row.optString("assignee_kind"))){Long ch=characterIdMap.get(row.optLong("assignee_id"));v.put("assignee_id",ch==null?0:ch);}if(v.containsKey("linked_quest_id")){Long q=questIdMap.get(row.optLong("linked_quest_id"));v.put("linked_quest_id",q==null?0:q);}if(v.containsKey("target_guild_id")){Long target=guildMap.get(row.optLong("target_guild_id"));v.put("target_guild_id",target==null?0:target);}db.insertOrThrow(table,null,v);}}

}
