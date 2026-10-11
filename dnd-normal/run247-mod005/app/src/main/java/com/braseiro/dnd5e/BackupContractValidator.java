package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Valida o contrato portátil sem alterar o banco. */
public final class BackupContractValidator {
    private static final int MAX_SUPPORTED_VERSION=21;
    private static final String[] FORBIDDEN_GLOBAL_KEYS={"manuals","manualRagParts","manualLocalPages","tokens","tokenPdfs","pdfTokens","mapPackages"};
    private BackupContractValidator(){}

    public static JSONObject validate(JSONObject backup){/*BR_JSON_WRAP*/try{
        JSONObject result=new JSONObject();JSONArray errors=new JSONArray();JSONArray warnings=new JSONArray();
        try{
            if(backup==null)errors.put("JSON ausente");
            else{
                if(!"braseiro-dnd-backup".equals(backup.optString("format")))errors.put("formato incompatível");
                int version=backup.optInt("version",0);boolean legacy=version>0&&version<12;if(version<=0||version>MAX_SUPPORTED_VERSION)errors.put("versão não suportada");
                if(!legacy&&!"campaign".equals(backup.optString("backupScope")))errors.put("escopo deve ser campaign");
                if(legacy)warnings.put("backup legado: bibliotecas globais, caminhos físicos e históricos sem perfil serão ignorados com segurança");
                String world=backup.optString("worldProfile");if(!"dnd".equalsIgnoreCase(world))errors.put("worldProfile inválido para Braseiro D&D: somente dnd é aceito");
                if(backup.optJSONObject("campaign")==null)errors.put("campanha ausente");
                if(backup.optJSONArray("characters")==null)errors.put("fichas ausentes");
                // RUN217: reject truncated CURRENT BACKUP21 snapshots before any table is
                // deleted. A partial 21 backup could otherwise silently keep stale world
                // rows while replacing characters and their IDs. Older formats retain their
                // existing migration/compatibility path.
                String integrity=Backup21IntegrityEnvelope.check(backup);
                if(!integrity.isEmpty())errors.put("BACKUP21 inválido: "+integrity);
                if(version==21&&!backup.has(Backup21IntegrityEnvelope.KEY))
                    warnings.put("BACKUP21 anterior à RUN217: sem digest integral; compatibilidade mantida");
                if(!legacy&&errors.length()==0){
                    validateObjectWorld(backup,"campaign",world,errors);
                    validateObjectWorld(backup,"storyState",world,errors);
                    validateObjectWorld(backup,"sceneState",world,errors);
                    validateObjectWorld(backup,"combatState",world,errors);
                    validateObjectWorld(backup,"partyState",world,errors);
                    validateObjectWorld(backup,"masterRuntimeState",world,errors);
                    validateObjectWorld(backup,"environmentalDirectorState",world,errors);
                    validateObjectWorld(backup,"travelRulesState",world,errors);
                    validateObjectWorld(backup,"livingWorldState",world,errors);
                    String[] scopedArrays={"characters","journal","combatants","diceHistory","npcMemories","auditLog","mapHexNotes","partyActions","characterControls","knowledgeFacts","quests","factions","worldClocks","worldEvents","pendingTransactions","pendingActions","effectReceipts","sessionMemories","tacticalTokens","masterEpisodes","npcCognitiveStates","heroLifeProfiles","lifeRelationships","lifeActivities","livingTroupes","guilds","guildRanks","guildMembers","guildContracts","guildProjects","guildSites","guildLedger","guildRelations","emergentArcs","settlementStates","worldHistoryEvents","rumors","rumorKnowledge","campaignLegacies","npcVoiceProfiles","tacticalScenes","localTableSecrets","sceneCommitJournal","worldDomainEvents","campaignPois","campaignPoiEvents"};
                    for(String key:scopedArrays)validateArrayWorld(backup,key,world,errors);
                }
                for(String key:FORBIDDEN_GLOBAL_KEYS)if(backup.has(key)){if(legacy)warnings.put("campo global legado ignorado: "+key);else errors.put("biblioteca global indevida: "+key);}
                JSONObject map=backup.optJSONObject("mapState");if(map!=null){if(map.has("image_uri")||map.has("overlay_uri")||map.has("map_package_id")){if(legacy)warnings.put("caminhos físicos legados do mapa serão descartados");else errors.put("estado do mapa contém caminho físico");}if(map.optString("logical_map_id").length()==0)warnings.put("mapa sem identificador lógico");if(!legacy)validateMapRows(backup,map.optString("logical_map_id"),world,errors);}
                if(!legacy&&!backup.optBoolean("derivedIndexesExcluded",false))warnings.put("índices derivados não estão declarados como excluídos");
                JSONArray exclusions=backup.optJSONArray("externalLibrariesExcluded");if(!legacy&&(exclusions==null||exclusions.length()<5))warnings.put("lista de bibliotecas externas excluídas incompleta");
            }
            result.put("valid",errors.length()==0).put("errors",errors).put("warnings",warnings);
        }catch(Exception e){try{errors.put(e.getMessage());result.put("valid",false).put("errors",errors).put("warnings",warnings);}catch(Exception ignored){AppLog.e("BackupContract","Falha ao registrar validação.",e);}}
        return result;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static void validateObjectWorld(JSONObject root,String key,String world,JSONArray errors){/*BR_JSON_WRAP*/try{
        JSONObject value=root.optJSONObject(key);if(value==null)return;String source=value.optString("world_profile",world);if(!world.equals(source))errors.put(key+" pertence a "+source+", não a "+world);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static void validateArrayWorld(JSONObject root,String key,String world,JSONArray errors){/*BR_JSON_WRAP*/try{
        JSONArray rows=root.optJSONArray(key);if(rows==null)return;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null){errors.put(key+" contém linha inválida em "+i);continue;}String source=row.optString("world_profile","");if(!world.equals(source))errors.put(key+"["+i+"] pertence a "+(source.length()==0?"mundo não informado":source));}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static void validateMapRows(JSONObject root,String logicalMapId,String world,JSONArray errors){/*BR_JSON_WRAP*/try{
        if(logicalMapId==null||logicalMapId.length()==0)return;JSONArray corrections=root.optJSONArray("mapCorrections");if(corrections!=null)for(int i=0;i<corrections.length();i++){JSONObject row=corrections.optJSONObject(i);if(row==null||!logicalMapId.equals(row.optString("map_id")))errors.put("mapCorrections["+i+"] não pertence ao mapa lógico "+logicalMapId);}
        JSONArray notes=root.optJSONArray("mapHexNotes");if(notes!=null)for(int i=0;i<notes.length();i++){JSONObject row=notes.optJSONObject(i);if(row==null||!logicalMapId.equals(row.optString("map_id"))||!world.equals(row.optString("world_profile")))errors.put("mapHexNotes["+i+"] não pertence ao mundo/mapa declarado");}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static void requireValid(JSONObject backup)throws Exception{
        JSONObject result=validate(backup);if(result.optBoolean("valid",false))return;throw new Exception("Backup incompatível: "+result.optJSONArray("errors"));
    }
}
