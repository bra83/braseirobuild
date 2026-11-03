package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Valida o contrato portátil sem alterar o banco. */
public final class BackupContractValidator {
    private static final int MAX_SUPPORTED_VERSION=22;
    private static final String[] FORBIDDEN_GLOBAL_KEYS={"manuals","manualRagParts","manualLocalPages","tokens","tokenPdfs","pdfTokens","mapPackages"};
    private BackupContractValidator(){}

    public static JSONObject validate(JSONObject backup){
        JSONObject result=new JSONObject();JSONArray errors=new JSONArray();JSONArray warnings=new JSONArray();
        try{
            if(backup==null)errors.put("JSON ausente");
            else{
                if(!"braseiro-pathfinder2e-backup".equals(backup.optString("format")))errors.put("formato incompatível");
                int version=backup.optInt("version",0);boolean legacy=version>0&&version<12;if(version<=0||version>MAX_SUPPORTED_VERSION)errors.put("versão não suportada");
                if(!legacy&&!"campaign".equals(backup.optString("backupScope")))errors.put("escopo deve ser campaign");
                if(legacy)warnings.put("backup legado: bibliotecas globais, caminhos físicos e históricos sem perfil serão ignorados com segurança");
                String world=backup.optString("worldProfile");if(!"pf2".equals(world)&&!"pathfinder_2e".equals(world))errors.put("worldProfile inválido");if(!"pathfinder_2e".equals(backup.optString("systemId","pathfinder_2e")))errors.put("systemId incompatível");
                if(backup.optJSONObject("campaign")==null)errors.put("campanha ausente");
                if(backup.optJSONArray("characters")==null)errors.put("fichas ausentes");
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
                    validateObjectWorld(backup,"campaignPoiMeta",world,errors);
                    String[] scopedArrays={"characters","journal","combatants","diceHistory","npcMemories","auditLog","mapHexNotes","partyActions","characterControls","knowledgeFacts","quests","factions","worldClocks","worldEvents","pendingTransactions","pendingActions","effectReceipts","sessionMemories","tacticalTokens","masterEpisodes","npcCognitiveStates","heroLifeProfiles","lifeRelationships","lifeActivities","livingTroupes","guilds","guildRanks","guildMembers","guildContracts","guildProjects","guildSites","guildLedger","guildRelations","emergentArcs","settlementStates","worldHistoryEvents","rumors","rumorKnowledge","campaignLegacies","npcVoiceProfiles","tacticalScenes","localTableSecrets","campaignPois","campaignPoiProposals","kingmakerSkills","kingmakerLeaders","kingmakerFeatures","kingmakerHexes","kingmakerSettlements","kingmakerStructures","kingmakerTurns","kingmakerTurnActivities","kingmakerEvents","kingmakerArmies","kingmakerWarEncounters","kingmakerCompanions","kingmakerCamps","kingmakerTradeAgreements"};
                    for(String key:scopedArrays)validateArrayWorld(backup,key,world,errors);
                }
                if(!legacy&&backup.optInt("version",0)>=21)validateCampaignPois(backup,world,errors);
                if(!legacy&&backup.optInt("version",0)>=22)validateKingmaker(backup,world,errors);
                for(String key:FORBIDDEN_GLOBAL_KEYS)if(backup.has(key)){if(legacy)warnings.put("campo global legado ignorado: "+key);else errors.put("biblioteca global indevida: "+key);}
                JSONObject map=backup.optJSONObject("mapState");if(map!=null){if(map.has("image_uri")||map.has("overlay_uri")||map.has("map_package_id")){if(legacy)warnings.put("caminhos físicos legados do mapa serão descartados");else errors.put("estado do mapa contém caminho físico");}if(map.optString("logical_map_id").length()==0)warnings.put("mapa sem identificador lógico");if(!legacy)validateMapRows(backup,map.optString("logical_map_id"),world,errors);}
                if(!legacy&&!backup.optBoolean("derivedIndexesExcluded",false))warnings.put("índices derivados não estão declarados como excluídos");
                JSONArray exclusions=backup.optJSONArray("externalLibrariesExcluded");if(!legacy&&(exclusions==null||exclusions.length()<5))warnings.put("lista de bibliotecas externas excluídas incompleta");
            }
            result.put("valid",errors.length()==0).put("errors",errors).put("warnings",warnings);
        }catch(Exception e){try{errors.put(e.getMessage());result.put("valid",false).put("errors",errors).put("warnings",warnings);}catch(Exception ignored){AppLog.e("BackupContract","Falha ao registrar validação.",e);}}
        return result;
    }

    private static void validateObjectWorld(JSONObject root,String key,String world,JSONArray errors){
        JSONObject value=root.optJSONObject(key);if(value==null)return;String source=value.optString("world_profile",world);if(!world.equals(source))errors.put(key+" pertence a "+source+", não a "+world);
    }

    private static void validateArrayWorld(JSONObject root,String key,String world,JSONArray errors){
        JSONArray rows=root.optJSONArray(key);if(rows==null)return;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null){errors.put(key+" contém linha inválida em "+i);continue;}String source=row.optString("world_profile","");if(!world.equals(source))errors.put(key+"["+i+"] pertence a "+(source.length()==0?"mundo não informado":source));}
    }

    private static void validateMapRows(JSONObject root,String logicalMapId,String world,JSONArray errors){
        if(logicalMapId==null||logicalMapId.length()==0)return;JSONArray corrections=root.optJSONArray("mapCorrections");if(corrections!=null)for(int i=0;i<corrections.length();i++){JSONObject row=corrections.optJSONObject(i);if(row==null||!logicalMapId.equals(row.optString("map_id")))errors.put("mapCorrections["+i+"] não pertence ao mapa lógico "+logicalMapId);}
        JSONArray notes=root.optJSONArray("mapHexNotes");if(notes!=null)for(int i=0;i<notes.length();i++){JSONObject row=notes.optJSONObject(i);if(row==null||!logicalMapId.equals(row.optString("map_id"))||!world.equals(row.optString("world_profile")))errors.put("mapHexNotes["+i+"] não pertence ao mundo/mapa declarado");}
    }

    private static void validateCampaignPois(JSONObject root,String world,JSONArray errors){
        JSONArray pois=root.optJSONArray("campaignPois");if(pois==null){errors.put("POIs de campanha ausentes no backup v21");return;}
        java.util.HashSet<String> ids=new java.util.HashSet<String>();
        for(int i=0;i<pois.length();i++){JSONObject row=pois.optJSONObject(i);if(row==null){errors.put("campaignPois contém linha inválida em "+i);continue;}String id=row.optString("poi_id");if(id.length()==0||!ids.add(id))errors.put("campaignPois contém ID ausente/duplicado em "+i);if(row.optInt("canonical",0)!=0||row.optBoolean("canonical",false))errors.put("campaignPois não pode conter local canônico");String source=row.optString("source");if(!"campaign".equals(source)&&!"procedural".equals(source))errors.put("campaignPois["+i+"] possui source inválido");if(row.optString("hex_id").trim().length()==0)errors.put("campaignPois["+i+"] não possui hex");}
        for(int i=0;i<pois.length();i++){JSONObject row=pois.optJSONObject(i);if(row==null)continue;String parent=row.optString("parent_poi_id").trim();if(parent.length()>0&&!ids.contains(parent))errors.put("campaignPois["+i+"] aponta para parent_poi_id ausente");}
        JSONArray history=root.optJSONArray("campaignPoiHistory");if(history!=null)for(int i=0;i<history.length();i++){JSONObject row=history.optJSONObject(i);if(row==null||!ids.contains(row.optString("poi_id")))errors.put("campaignPoiHistory["+i+"] aponta para POI ausente");}
    }

    private static void validateKingmaker(JSONObject root,String world,JSONArray errors){
        JSONObject state=root.optJSONObject("kingmakerState");if(state==null){errors.put("estado Kingmaker ausente no backup v22");return;}if(!world.equals(state.optString("world_profile")))errors.put("kingmakerState pertence a outro mundo");int level=state.optInt("level",0),partyCap=state.optInt("party_level_cap",1);if(level<1||level>20)errors.put("nível do Reino inválido");if(partyCap<1||partyCap>20||level>partyCap)errors.put("limite de nível do grupo/Reino inválido");if(state.optInt("size",0)<1)errors.put("tamanho do Reino inválido");if(state.optInt("consumption",0)<0||Math.abs(state.optInt("consumption_modifier",0))>1000)errors.put("Consumo do Reino inválido");String phase=state.optString("turn_phase");if(!phase.matches("UPKEEP|COMMERCE|ACTIVITY|EVENT|COMPLETE"))errors.put("fase do Reino inválida");
        String[] required={"kingmakerSkills","kingmakerLeaders","kingmakerFeatures","kingmakerHexes","kingmakerSettlements","kingmakerStructures","kingmakerTurns","kingmakerTurnActivities","kingmakerEvents","kingmakerArmies","kingmakerWarEncounters","kingmakerCompanions","kingmakerCamps","kingmakerTradeAgreements"};for(String key:required)if(root.optJSONArray(key)==null)errors.put(key+" ausente no backup v22");
        java.util.HashSet<String> skillKeys=new java.util.HashSet<String>(),roleKeys=new java.util.HashSet<String>(),featureKeys=new java.util.HashSet<String>();JSONArray skills=root.optJSONArray("kingmakerSkills");if(skills!=null){for(int i=0;i<skills.length();i++){JSONObject row=skills.optJSONObject(i);if(row==null)continue;String key=row.optString("skill_key");if(key.length()==0||!skillKeys.add(key))errors.put("perícia Kingmaker sem chave única");}if(skillKeys.size()!=16)errors.put("backup Kingmaker deve conter as 16 perícias do Reino");}JSONArray leaders=root.optJSONArray("kingmakerLeaders");int invested=0;if(leaders!=null){for(int i=0;i<leaders.length();i++){JSONObject row=leaders.optJSONObject(i);if(row==null)continue;String key=row.optString("role_key");if(key.length()==0||!roleKeys.add(key))errors.put("cargo Kingmaker sem chave única");if(row.optInt("invested")==1&&row.optInt("vacant")==0)invested++;}if(roleKeys.size()!=8)errors.put("backup Kingmaker deve conter os oito cargos de liderança");if(invested>4)errors.put("backup Kingmaker possui mais de quatro cargos investidos");}JSONArray features=root.optJSONArray("kingmakerFeatures");if(features!=null)for(int i=0;i<features.length();i++){JSONObject row=features.optJSONObject(i);if(row==null)continue;String key=row.optString("feature_key");if(key.length()==0||!featureKeys.add(key))errors.put("traço/talento Kingmaker sem chave única");try{new JSONObject(row.optString("effects_json","{}"));}catch(Exception e){errors.put("efeitos JSON inválidos em kingmakerFeatures["+i+"]");}}
        java.util.HashSet<String> claimed=new java.util.HashSet<String>(),settlements=new java.util.HashSet<String>(),settlementHexes=new java.util.HashSet<String>(),turns=new java.util.HashSet<String>(),armies=new java.util.HashSet<String>();JSONArray hexes=root.optJSONArray("kingmakerHexes");if(hexes!=null)for(int i=0;i<hexes.length();i++){JSONObject row=hexes.optJSONObject(i);if(row==null)continue;if(row.optString("hex_id").trim().length()==0)errors.put("kingmakerHexes["+i+"] sem hex");if(row.optInt("claimed",0)==1){if(row.optInt("reconnoitered",0)!=1||row.optInt("cleared",0)!=1)errors.put("hex Kingmaker reivindicado sem reconhecimento/limpeza");claimed.add(row.optString("hex_id"));}}
        JSONArray settlementRows=root.optJSONArray("kingmakerSettlements");if(settlementRows!=null)for(int i=0;i<settlementRows.length();i++){JSONObject row=settlementRows.optJSONObject(i);if(row==null)continue;String key=row.optString("settlement_key"),hex=row.optString("hex_id"),type=row.optString("type");if(key.length()==0||!settlements.add(key))errors.put("assentamento Kingmaker sem chave única");if(!claimed.contains(hex))errors.put("assentamento Kingmaker fora de hex reivindicado");if(!settlementHexes.add(hex))errors.put("mais de um assentamento Kingmaker no mesmo hex");if(!type.matches("village|town|city|metropolis"))errors.put("tipo de assentamento Kingmaker inválido");int blocks=row.optInt("developed_blocks",1),occupied=row.optInt("occupied_lots",0),residential=row.optInt("residential_lots",0),min="metropolis".equals(type)?10:"city".equals(type)?5:"town".equals(type)?2:1,max="metropolis".equals(type)?Integer.MAX_VALUE/4:"city".equals(type)?9:"town".equals(type)?4:1;if(blocks<min||blocks>max)errors.put("quantidade de blocos inválida em kingmakerSettlements["+i+"]");if(occupied<0||((long)occupied)>((long)blocks)*4L||residential<0||residential>occupied)errors.put("ocupação de lotes inválida em kingmakerSettlements["+i+"]");int overcrowded=occupied>0&&residential<blocks?1:0;if(row.optInt("overcrowded",0)!=overcrowded)errors.put("superlotação inconsistente em kingmakerSettlements["+i+"]");try{new JSONObject(row.optString("grid_json","{}"));new JSONObject(row.optString("storage_json","{}"));}catch(Exception e){errors.put("grade/armazém JSON inválido em kingmakerSettlements["+i+"]");}}
        JSONArray turnRows=root.optJSONArray("kingmakerTurns");if(turnRows!=null)for(int i=0;i<turnRows.length();i++){JSONObject row=turnRows.optJSONObject(i);if(row!=null)turns.add(String.valueOf(row.optLong("id")));}if(state.optLong("active_turn_id",0)>0&&!turns.contains(String.valueOf(state.optLong("active_turn_id"))))errors.put("estado Kingmaker aponta para turno ativo ausente");
        JSONArray activities=root.optJSONArray("kingmakerTurnActivities");if(activities!=null)for(int i=0;i<activities.length();i++){JSONObject row=activities.optJSONObject(i);if(row!=null&&!turns.contains(String.valueOf(row.optLong("turn_id"))))errors.put("atividade Kingmaker aponta para turno ausente");}
        JSONArray structures=root.optJSONArray("kingmakerStructures");if(structures!=null)for(int i=0;i<structures.length();i++){JSONObject row=structures.optJSONObject(i);if(row!=null&&!settlements.contains(row.optString("settlement_key")))errors.put("estrutura Kingmaker aponta para assentamento ausente");}
        JSONArray armyRows=root.optJSONArray("kingmakerArmies");if(armyRows!=null)for(int i=0;i<armyRows.length();i++){JSONObject row=armyRows.optJSONObject(i);if(row==null)continue;String key=row.optString("army_key");if(key.length()==0||!armies.add(key))errors.put("exército Kingmaker sem chave única");if(row.optInt("hp",-1)<0||row.optInt("hp",0)>row.optInt("max_hp",0))errors.put("PV de exército Kingmaker inválidos");if(row.optInt("consumption",0)<0)errors.put("Consumo de exército Kingmaker inválido");}
        JSONArray wars=root.optJSONArray("kingmakerWarEncounters");if(wars!=null)for(int i=0;i<wars.length();i++){JSONObject row=wars.optJSONObject(i);if(row==null)continue;try{JSONArray participants=new JSONArray(row.optString("participants_json","[]"));for(int j=0;j<participants.length();j++){Object entry=participants.opt(j);String key=entry instanceof JSONObject?((JSONObject)entry).optString("army_key"):String.valueOf(entry);if(!armies.contains(key))errors.put("batalha Kingmaker aponta para exército ausente");}}catch(Exception e){errors.put("participantes JSON inválidos em kingmakerWarEncounters["+i+"]");}}
        JSONArray camps=root.optJSONArray("kingmakerCamps");if(camps!=null)for(int i=0;i<camps.length();i++){JSONObject row=camps.optJSONObject(i);if(row==null)continue;if(row.optInt("current_encounter_dc",0)<0||row.optInt("elapsed_hours",0)<0)errors.put("estado de acampamento Kingmaker inválido");try{new JSONArray(row.optString("activities_json","[]"));new JSONArray(row.optString("encounter_checks_json","[]"));new JSONObject(row.optString("encounter_json","{}"));}catch(Exception e){errors.put("JSON inválido em kingmakerCamps["+i+"]");}}
    }

    public static void requireValid(JSONObject backup)throws Exception{
        JSONObject result=validate(backup);if(result.optBoolean("valid",false))return;throw new Exception("Backup incompatível: "+result.optJSONArray("errors"));
    }
}
