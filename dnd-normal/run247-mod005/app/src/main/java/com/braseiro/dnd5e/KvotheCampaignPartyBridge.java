package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Explicit player-authorized transition from Kvothe's solo academic prologue
 * to a four-hero party at D040. Never spawns, teleports, levels up, or
 * auto-controls a player character.
 */
public final class KvotheCampaignPartyBridge {
    private final AppDb db;
    private final PartyDirector party;

    public KvotheCampaignPartyBridge(AppDb database) {
        db=database;
        party=new PartyDirector(database);
    }

    private static String norm(String raw) {
        return Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }

    public static boolean inCampus(JSONObject scene) {
        if(scene==null)return false;
        return "strixhaven_curriculum".equals(scene.optString("adventure_profile"))
            || StrixhavenSceneBoundary.strixhavenLocation(scene);
    }

    public static boolean atMarco(JSONObject scene, JSONObject map) {
        if(scene==null)return false;
        String hex=scene.optString("hex_id","");
        if(hex.trim().isEmpty()&&map!=null)hex=map.optString("current_hex_id","");
        String location=norm(scene.optString("location",""));
        return "D040".equalsIgnoreCase(hex) &&
            (location.contains("marco do percurso")||location.contains("marco de percurso"));
    }

    /** NPCs can join temporary campus outings by accepting a real invitation. */
    public JSONObject inviteCampusNpc(String name,boolean playerInvited,
                                      boolean npcAccepted)throws Exception {
        JSONObject result=new JSONObject().put("member_kind","npc").put("joined",false);
        JSONObject scene=db.getSceneState();
        if(!inCampus(scene))return result.put("reason","not_on_strixhaven_campus");
        if(!playerInvited||!npcAccepted)return result.put("reason","invitation_or_npc_consent_missing");
        if(name==null||name.trim().isEmpty())return result.put("reason","npc_name_missing");
        JSONObject npc=db.findNpcMemoryByName(name.trim());
        if(npc.length()==0||npc.optInt("active",1)!=1)
            return result.put("reason","npc_not_active_in_living_world");
        // A character whose presence is not grounded cannot teleport to the hero.
        String scenePlace=norm(scene.optString("location"));
        String npcPlace=norm(db.getNpcCognitiveState(name).optString("current_location",
                                  npc.optString("last_seen","")));
        boolean inSamePlace= !scenePlace.isEmpty()&&!npcPlace.isEmpty()&&
            (scenePlace.contains(npcPlace)||npcPlace.contains(scenePlace));
        boolean present=false;
        JSONArray presentNpcs=new JSONArray(scene.optString("present_npcs_json","[]"));
        for(int i=0;i<presentNpcs.length();i++){
            JSONObject p=presentNpcs.optJSONObject(i);
            String label=p==null?presentNpcs.optString(i):p.optString("name","");
            if(norm(name).equals(norm(label)))present=true;
        }
        if(!inSamePlace&&!present)return result.put("reason","npc_not_present_no_teleport");
        if(db.findActivePartyNpc(name).length()>0)
            return result.put("joined",true).put("already_member",true);
        long membership=party.recruitNpc(name,"amigo_de_strixhaven",new JSONObject());
        return result.put("joined",membership>0).put("membership_id",membership)
            .put("npc_controls","independent").put("hero_player_control_unchanged",true);
    }

    /**
     * The three existing 2024 sheets are selected by the player at the inn,
     * not silently generated. No character levels, hit points, or equipment
     * are ever altered by recruiting.
     */
    public JSONObject recruitAtMarco(final long wilemId,final long simmonId,
                                     final long auriId,boolean playerConfirmed,
                                     boolean allAccepted)throws Exception {
        JSONObject result=new JSONObject().put("joined",false)
            .put("location_required","Marco do Percurso, D040");
        if(inCampus(db.getSceneState()))return result.put("reason","academic_prologue_not_exited");
        if(!atMarco(db.getSceneState(),db.getMapState()))
            return result.put("reason","not_at_marco_d040");
        if(!playerConfirmed||!allAccepted)
            return result.put("reason","meeting_and_consent_not_confirmed");
        // Companheiros nao entram com nivel diferente: a transicao 2024 de
        // ficha/level-up deve ser validada antes do encontro, nunca improvisada.
        int protagonistLevel=0;
        for(JSONObject member:party.activeHeroRoster()){
            if("kvothe".equals(norm(member.optString("name")))){
                protagonistLevel=db.getCharacter(member.optLong("id")).optInt("level",0);
                break;
            }
        }
        if(protagonistLevel<1)return result.put("reason","protagonist_sheet_missing");
        long[] ids={wilemId,simmonId,auriId};
        String[] names={"Wilem","Simmon","Auri"};
        Set<Long> distinct=new HashSet<Long>();
        for(int i=0;i<ids.length;i++){
            if(ids[i]<=0||!distinct.add(ids[i]))
                return result.put("reason","three_distinct_existing_sheets_required");
            JSONObject hero=db.getCharacter(ids[i]);
            if(hero.length()==0||hero.optInt("active",1)!=1||
                    !norm(names[i]).equals(norm(hero.optString("name"))))
                return result.put("reason","missing_matching_hero_sheet")
                             .put("required_character",names[i]);
            if(hero.optInt("level",0)!=protagonistLevel)
                return result.put("reason","companion_level_mismatch")
                    .put("required_character",names[i])
                    .put("required_level",protagonistLevel)
                    .put("actual_level",hero.optInt("level",0))
                    .put("next_step","Validar progressao D&D 2024 e atualizar a ficha do companheiro antes de recrutar");
        }
        final JSONArray idsJoined=new JSONArray();
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            @Override public void run() throws Exception {
                for(long id:ids){
                    long membership=party.recruitHero(id,"marco_d040_player_choice");
                    if(membership<=0)throw new IllegalStateException("Falha ao entrar na companhia.");
                    // Existing player UI supports three modes; the user owns all four.
                    db.saveCharacterControl(id,"manual","{}");
                    idsJoined.put(id);
                }
            }
        });
        return result.put("joined",true).put("hero_ids",idsJoined)
            .put("control_mode","manual")
            .put("player_authority",true).put("level_unchanged",true)
            .put("inventory_unchanged",true);
    }

    /** No surprise companions: simply tells the Master what is possible now. */
    public JSONObject context() {
        JSONObject out=new JSONObject();
        try{
            JSONObject scene=db.getSceneState();
            boolean campus=inCampus(scene),marco=!campus&&atMarco(scene,db.getMapState());
            List<JSONObject> heroes=party.activeHeroRoster();
            boolean hasKvothe=false;
            for(JSONObject h:heroes)if("kvothe".equals(norm(h.optString("name"))))hasKvothe=true;
            if(!hasKvothe)return out.put("enabled",false);
            out.put("enabled",true);
            JSONArray roster=new JSONArray();
            for(JSONObject h:heroes){
                long id=h.optLong("id");
                roster.put(new JSONObject().put("id",id).put("name",h.optString("name"))
                    .put("control_mode",CompanionAgencyEngine.mode(
                        db.getCharacterControl(id).optString("control_mode","manual"))));
            }
            out.put("academic_prologue",campus).put("at_marco_d040",marco)
               .put("active_hero_count",heroes.size()).put("active_heroes",roster)
               .put("campus_invites","NPCs acompanham por convite e aceite, podem recusar e partir. Nunca se tornam herois controlaveis automaticamente.")
               .put("marco_rendezvous",marco?"Encontro opcional. Wilem, Simmon e Auri precisam de fichas reais e aceite; somente o jogador decide pelo recrutamento.":"Nao antecipar reencontro nem teletransportar colegas.")
               .put("player_agency","So personagens recrutados com fichas importadas entram no controle manual do jogador.")
               .put("encounter_adaptation",encounterPolicy(heroes.size(),0,false));
        }catch(Exception ignored){AppLog.ignored("KvothePartyBridge","Contexto conservador de recrutamento.",ignored);}
        return out;
    }

    /**
     * Existing official encounters assume a group. Never silently invent
     * reinforcements or tamper with published statblocks.
     */
    public static JSONObject encounterPolicy(int heroCount,int explicitAlliedCombatants,
                                             boolean validatedForActualParty) {
        JSONObject out=new JSONObject();
        try{
            int partySize=Math.max(0,heroCount)+Math.max(0,explicitAlliedCombatants);
            boolean solo=partySize<=1;
            out.put("party_size",partySize).put("solo",solo)
               .put("combat_encounter_requires_review",solo&&!validatedForActualParty)
               .put("unmodified_official_encounter_authorized",!solo||validatedForActualParty)
               .put("do_not_invent_allies",true).put("do_not_change_statblocks",true)
               .put("guidance",solo&&!validatedForActualParty
                    ?"Nao iniciar encontro oficial dimensionado para grupo sem revisar perigo real, opcoes nao violentas, fuga e eventual ajuda de NPCs presentes que aceitem participar."
                    :"Resolver as mecanicas pelo grupo realmente presente; nao presumir participantes.");
        }catch(Exception ignored){}
        return out;
    }
}
