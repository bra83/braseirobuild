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
        // DO40 (letra O) e a coordenada real do Atlas instalada por
        // NewarreAndWaystoneEngine. D040 (numero zero) foi emitida por
        // versoes antigas e e aceita SOMENTE como alias de compatibilidade.
        boolean correctHex=NewarreAndWaystoneEngine.HEX_ID.equalsIgnoreCase(hex)
            || "D040".equalsIgnoreCase(hex);
        return correctHex &&
            (location.contains("marco do percurso")||location.contains("marco de percurso"));
    }

    /** NPCs can join temporary campus outings by accepting a real invitation. */
    public JSONObject inviteCampusNpc(String name,boolean playerInvited,
                                      boolean npcAccepted)throws Exception {
        JSONObject result=new JSONObject().put("member_kind","npc").put("joined",false);
        JSONObject scene=db.getSceneState();
        if(!inCampus(scene))return result.put("reason","not_on_strixhaven_campus");
        KvotheCanonical2024.restoreIfNeeded(db);
        int protagonistLevel=0;
        for(JSONObject h:db.listActiveCharacters())if("Kvothe".equalsIgnoreCase(h.optString("name")))
            protagonistLevel=h.optInt("level",1);
        if(protagonistLevel>0)StrixhavenFriends2024.syncBeforeMarco(db,protagonistLevel);
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
            .put("location_required","Marco do Percurso, DO40");
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
                    long membership=party.recruitHero(id,"marco_do40_player_choice");
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

    /** Promote the three canonical NPC sheets. No hero rows exist before this
     * explicitly accepted D040 transition; afterward their XP is independent. */
    public JSONObject recruitAtMarco(boolean playerConfirmed,boolean allAccepted)throws Exception{
        KvotheCanonical2024.restoreIfNeeded(db);
        if(!atMarco(db.getSceneState(),db.getMapState())||
            inCampus(db.getSceneState()))
            return new JSONObject().put("joined",false).put("reason","not_at_marco_d040");
        int level=0;
        for(JSONObject h:db.listActiveCharacters())if("Kvothe".equalsIgnoreCase(h.optString("name")))
            level=h.optInt("level",0);
        if(level<1)return new JSONObject().put("joined",false).put("reason","protagonist_sheet_missing");
        for(String name:new String[]{"Wilem","Simmon","Auri"})
            if(db.findNpcMemoryByName(name).length()==0)
                return new JSONObject().put("joined",false).put("reason","student_npc_not_established")
                    .put("name",name);
        StrixhavenFriends2024.syncBeforeMarco(db,level);
        return StrixhavenFriends2024.promoteAtMarco(db,playerConfirmed,allAccepted);
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
            out.put("kvothe_canonical",KvotheCanonical2024.restoreIfNeeded(db));
            if(campus){
                int kvotheLevel=0;
                for(JSONObject hero:heroes)if("Kvothe".equalsIgnoreCase(hero.optString("name")))
                    kvotheLevel=db.getCharacter(hero.optLong("id")).optInt("level",0);
                if(kvotheLevel>0)out.put("strixhaven_npc_friends",
                    StrixhavenFriends2024.syncBeforeMarco(db,kvotheLevel));
            }
            out.put("enabled",true);
            out.put("post_strixhaven_arrival",new JSONObject()
                .put("city","Yartar")
                .put("hex_id","DM28")
                .put("atlas_location_id","yartar")
                .put("world_profile","dnd")
                .put("region","Vale do Dessarin")
                .put("role","primeira chegada a Faerun apos o prologo")
                .put("must_not_auto_recruit_companions",true)
                .put("rendezvous_settlement","Newarre")
                .put("rendezvous_hex_id",NewarreAndWaystoneEngine.HEX_ID)
                .put("rendezvous_is_optional",true));
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
               .put("player_agency","Wilem, Simmon e Auri sao NPCs ate D040. No reencontro consentido, suas fichas NPC viram herois e passam a ter XP/progressao independentes.")
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
