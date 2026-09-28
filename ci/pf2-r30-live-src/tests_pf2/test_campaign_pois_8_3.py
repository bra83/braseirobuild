from pathlib import Path
import re, sqlite3, json

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'

def read(name): return (SRC/name).read_text(encoding='utf-8')

SERVICE=read('CampaignPoiService.java')
DB=read('AppDb.java')
MAIN=read('MainActivity.java')
MAP=read('MapBoardView.java')
MASTER=read('MasterOrchestrator.java')
WORLD=read('LivingWorldEngine.java')
RULES=read('Pathfinder2eRulesEngine.java')
BACKUP=read('BackupContractValidator.java')


def test_01_schema_separates_canonical_campaign_procedural_and_forces_noncanonical():
    assert 'CREATE TABLE IF NOT EXISTS campaign_pois' in DB
    assert "CHECK(canonical=0)" in DB
    assert "CHECK(source IN ('campaign','procedural'))" in DB
    assert 'map_locations/map_hex_atlas continuam somente leitura' in DB
    assert 'SOURCE_CAMPAIGN="campaign"' in SERVICE
    assert 'SOURCE_PROCEDURAL="procedural"' in SERVICE


def test_02_manual_tavern_is_saved_in_clicked_hex_not_official_atlas():
    assert 'public JSONObject createManual(JSONObject spec,String hexId)' in SERVICE
    assert '.put("hex_id",hexId)' in SERVICE
    assert 'insertOrThrow("campaign_pois"' in SERVICE
    create_region=SERVICE[SERVICE.index('private JSONObject create('):SERVICE.index('public JSONObject get(')]
    assert 'map_locations' not in create_region and 'map_hex_atlas' not in create_region
    assert 'ADICIONAR LOCAL' in MAIN and 'editCampaignPoiDialog(null,hexId)' in MAIN


def test_03_settlement_reuses_living_settlement_state_instead_of_duplicate_mechanics():
    assert 'private JSONObject linkSettlement' in SERVICE
    assert 'settlement_states' in SERVICE
    assert 'campaign-poi:' in SERVICE
    assert 'LivingSettlementEngine' in MAIN
    assert 'KingmakerRulesEngine' not in SERVICE
    assert 'Pathfinder2eRulesEngine' not in SERVICE


def test_04_subpois_preserve_parent_relation_and_validate_parent():
    assert 'parent_poi_id' in SERVICE
    assert 'POI pai inexistente' in SERVICE
    assert 'idx_campaign_pois_parent' in DB
    assert 'POI PAI (para locais internos)' in MAIN


def test_05_hidden_poi_is_not_in_player_projection_or_visible_atlas():
    assert 'hidden=0 AND discovered=1' in SERVICE
    assert 'private_notes' in SERVICE
    player_section=SERVICE[SERVICE.index('public JSONObject playerProjection'):SERVICE.index('public JSONObject masterProjection')]
    assert 'visibleForAtlas' in player_section
    assert 'playerSafe' in player_section
    assert 'private_notes' not in player_section.replace('playerSafe','')
    assert 'privateNotes=masterMode?' in MAIN and 'if(masterMode&&privateNotes!=null)' in MAIN


def test_06_discovery_changes_same_poi_instead_of_creating_replacement():
    assert 'public JSONObject discover(String poiId' in SERVICE
    assert '.put("hidden",false).put("discovered",true)' in SERVICE
    discover=SERVICE[SERVICE.index('public JSONObject discover'):SERVICE.index('public JSONObject changeStatus')]
    assert 'evolve(poiId' in discover
    assert 'DESCOBRIR' in MAIN


def test_07_living_world_generation_is_clocked_and_causal_not_blind_random():
    assert 'tick-last<72' in SERVICE
    assert 'no_world_causal_signal' in SERVICE
    assert 'world_events' in SERVICE and 'factions' in SERVICE
    assert 'Math.random' not in SERVICE and 'new Random' not in SERVICE
    assert 'applyWorldAdvance(before,after)' in WORLD


def test_08_evolution_and_abandonment_keep_history_and_same_poi_id():
    assert 'public JSONObject evolve(String poiId' in SERVICE
    assert 'campaign_poi_history' in SERVICE
    assert 'recordHistory(after,before,after' in SERVICE
    assert 'abandoned' in SERVICE and 'ruined' in SERVICE and 'destroyed' in SERVICE and 'rebuilding' in SERVICE
    assert 'MUDAR ESTADO' in MAIN


def test_09_dynamic_poi_search_and_name_resolution_exist():
    assert 'findVisibleByQuery' in SERVICE
    assert 'resolveVisibleByName' in SERVICE
    assert 'campaignPoiService.findVisibleByQuery' in MAIN


def test_10_dynamic_poi_can_become_travel_destination_and_route():
    guidance=MAIN[MAIN.index('private String buildDestinationGuidance'):MAIN.index('private boolean containsAny')]
    assert 'campaignPoiService.resolveVisibleByName(userText)' in guidance
    assert 'hexDistance(current,hex)' in guidance
    assert 'nextStepTowards(current,hex,mapGeometry)' in guidance
    assert 'routePreview(current,hex,8,mapGeometry)' in guidance


def test_11_backup_v22_roundtrip_contains_pois_history_proposals_and_meta():
    assert '.put("version",22)' in DB
    assert 'backupVersion>22' in DB
    for key in ('campaignPoiMeta','campaignPois','campaignPoiHistory','campaignPoiProposals'):
        assert f'root.put("{key}"' in DB
    assert 'MAX_SUPPORTED_VERSION=22' in BACKUP
    assert 'validateCampaignPois' in BACKUP


def test_12_campaign_poi_export_import_validates_ids_hex_parent_and_no_canonical():
    assert 'braseiro-pathfinder2e-campaign-pois' in SERVICE
    assert 'ID ausente ou duplicado' in SERVICE
    assert 'ID de POI já existe nesta campanha' in SERVICE
    assert 'Hex inválido no POI' in SERVICE
    assert 'parent_poi_id inexistente' in SERVICE
    assert 'POI canônico não pode entrar na camada de campanha' in SERVICE
    assert 'EXPORTAR POIs' in MAIN and 'IMPORTAR POIs' in MAIN
    assert 'campaign_pois.json' in MAIN


def test_13_master_projection_keeps_secrets_separate_and_prompt_forbids_leak():
    assert 'public_pois' in SERVICE and 'private_pois' in SERVICE
    assert 'não revelar ao jogador sem descoberta ficcional válida' in SERVICE
    assert 'campaign_pois_public' in MASTER and 'campaign_pois_private' in MASTER
    assert 'conhecimento EXCLUSIVO do Mestre' in MASTER and 'nunca o revele' in MASTER


def test_14_service_never_writes_canonical_atlas_tables():
    # Reading getMapHex is allowed; mutation of official tables is not.
    forbidden_write_patterns=[
        r'insert(?:OrThrow|WithOnConflict)?\("map_locations"',
        r'update\("map_locations"', r'delete\("map_locations"',
        r'insert(?:OrThrow|WithOnConflict)?\("map_hex_atlas"',
        r'update\("map_hex_atlas"', r'delete\("map_hex_atlas"',
        r'insert(?:OrThrow|WithOnConflict)?\("map_packages"',
        r'update\("map_packages"', r'delete\("map_packages"',
    ]
    for pat in forbidden_write_patterns:
        assert not re.search(pat,SERVICE)


def test_15_atlas_fuses_visible_dynamic_pois_without_polluting_canonical_rows():
    assert 'setCampaignPois(List<JSONObject> pois)' in MAP
    assert 'drawCampaignPois(canvas)' in MAP
    assert 'campaignPoisByHex' in MAP
    assert 'parent_poi_id' in MAP  # macro renderer filters sub-POIs
    assert 'refreshCampaignPoiMarkers' in MAIN
    assert 'getMapLocationsForHex' in MAIN  # canonical remains independent


def test_16_pf2_mechanics_authority_remains_pathfinder_engine():
    assert 'class Pathfinder2eRulesEngine' in RULES
    assert 'new Pathfinder2eRulesEngine()' in MAIN
    assert 'CampaignPoiService' not in RULES
    assert 'Pathfinder2eRulesEngine' not in SERVICE


def test_17_barbara_context_has_public_and_private_campaign_projections_with_authority_boundary():
    assert 'campaign_pois_public' in MASTER
    assert 'campaign_pois_private' in MASTER
    assert 'O aplicativo é a autoridade mecânica e persistente' in MASTER
    assert 'conhecimento EXCLUSIVO do Mestre' in MASTER


def test_18_dynamic_poi_position_is_hex_grounded_and_uses_same_map_geometry():
    assert 'hex_id' in SERVICE and 'getMapHex(packageId,hex)' in SERVICE
    assert 'geometry.byId' in MAP or 'geometry.center' in MAP or 'geometry.byId(entry.getKey())' in MAP
    assert 'canonical_position' in MASTER
    assert 'map_package_id' in MASTER and 'hex_id' in MASTER


def test_19_sql_constraint_rejects_canonical_dynamic_poi_and_accepts_campaign_procedural():
    con=sqlite3.connect(':memory:')
    con.execute("CREATE TABLE campaign_pois (poi_id TEXT PRIMARY KEY, source TEXT NOT NULL, canonical INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'active', CHECK(canonical=0), CHECK(source IN ('campaign','procedural')), CHECK(status IN ('active','abandoned','ruined','destroyed','occupied','rebuilding')))" )
    con.execute("INSERT INTO campaign_pois VALUES('a','campaign',0,'active')")
    con.execute("INSERT INTO campaign_pois VALUES('b','procedural',0,'abandoned')")
    try:
        con.execute("INSERT INTO campaign_pois VALUES('c','campaign',1,'active')")
        raised=False
    except sqlite3.IntegrityError:
        raised=True
    assert raised


def test_20_manual_narrative_detection_is_proposal_only_save_edit_ignore():
    assert 'proposeFromNarration' in SERVICE
    assert 'score<3' in SERVICE
    assert 'Novo POI detectado:' in MAIN
    for action in ('IGNORAR','EDITAR','SALVAR'):
        assert action in MAIN
    # The proposal detector itself must not persist a campaign_pois row.
    block=SERVICE[SERVICE.index('public JSONObject proposeFromNarration'):SERVICE.index('/**\n     * Geração consequencial')]
    assert 'insertOrThrow("campaign_pois"' not in block


def test_21_settlement_ui_exposes_campaign_fields_instead_of_raw_json_only():
    for label in ('POPULAÇÃO','NÍVEL DO ASSENTAMENTO','TAMANHO','GOVERNO','LEALDADE / ESTABILIDADE','PROSPERIDADE 0–100','SEGURANÇA 0–100','RECURSOS (separados por vírgula)','CONSTRUÇÕES (separadas por vírgula)','NPCs IMPORTANTES (separados por vírgula)','MERCADO','SERVIÇOS','DEFESA'):
        assert label in MAIN
    assert 'ASSENTAMENTO — JSON OPCIONAL' not in MAIN
    assert 'spec.put("settlement",sj)' in MAIN
    assert 'market_strength' in SERVICE and 'population_band' in SERVICE

def test_22_procedural_generation_uses_world_context_not_blind_random():
    for signal in ('terrain','has_road','has_waterway','weather','danger','threat_count','canonical_density','campaign_density','faction_count','regional_trade','regional_prosperity','political','suitability_score'):
        assert f'"{signal}"' in SERVICE
    assert 'world_context_incompatible' in SERVICE
    assert 'Math.random' not in SERVICE and 'new Random' not in SERVICE

def test_23_world_events_can_evolve_existing_poi_id_idempotently():
    assert 'applyWorldEvolution(dayBefore,dayAfter)' in SERVICE
    assert 'historyHasReason(eventKey)' in SERVICE
    assert 'targetPoiForEvent' in SERVICE and 'worldEvolutionMutation' in SERVICE
    assert 'evolve(target.optString("poi_id")' in SERVICE
    for state in ('destroyed','abandoned','ruined','occupied','rebuilding'):
        assert f'"{state}"' in SERVICE

def test_24_growth_chain_keeps_identity_and_can_promote_campaign_places():
    block=SERVICE[SERVICE.index('private JSONObject worldEvolutionMutation'):SERVICE.index('private static String evolutionReason')]
    assert 'development_level' in block
    assert 'trading_post' in block and 'settlement' in block
    assert 'create(' not in block and 'createManual' not in block and 'createProcedural' not in block
