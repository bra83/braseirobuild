package com.braseiro.pathfinder2e;
import org.json.JSONObject;

/** Hierarquia explícita das autoridades de estado. A IA nunca é a fonte da verdade. */
public final class SourceOfTruthRegistry {
    private SourceOfTruthRegistry(){}
    public static JSONObject manifest(){
        JSONObject o=new JSONObject();try{
            o.put("characters","characters + sheet_json");
            o.put("scene","scene_state_profiles");
            o.put("map_location","map_world_state/map_hex_atlas/map_locations");
            o.put("combat","combat_state_profiles + combatants + tactical_tokens");
            o.put("npc_identity_memory","npc_memory_profiles");
            o.put("npc_agency","npc_cognitive_state");
            o.put("guilds","guilds/guild_members/guild_contracts/guild_projects/guild_ledger");
            o.put("world","world_events/world_clocks/factions/life_activities");
            o.put("journal_facts","journal where canonical_fact=1");
            o.put("world_history","world_history_events projected only from canonical journal facts");
            o.put("emergent_campaign","emergent_arcs derived from canonical world/faction/clock/settlement state; no fixed ending");
            o.put("settlements_regions","settlement_states scoped by world_profile");
            o.put("kingmaker","kingmaker_state/skills/leaders/features/hexes/settlements/structures/turns/activities/events/armies/war_encounters/companions/camps/trade_agreements; KingmakerRulesEngine resolves mechanics; canonical Atlas remains read-only");
            o.put("rumors","rumors + rumor_knowledge; objective truth remains separate from each knower");
            o.put("campaign_legacy","world_seeds + campaign_legacies; same world seed survives generations");
            o.put("npc_voices","npc_voice_profiles keyed by stable NPC identity");
            o.put("tactical_scene","tactical_scenes geometry; mechanics still belong to combat/rules");
            o.put("local_table","local_table_sessions/players/secrets; player views are filtered by character identity");
            o.put("rules","deterministic rules engines OR verified local manual evidence");
            o.put("ai","proposal_only");
            o.put("conflict_policy","structured database authority wins over generated prose");
        }catch(Exception ignored){AppLog.ignored("SourceOfTruthRegistry","Falha tolerada registrada na consolidação 5.0.",ignored);}
        return o;
    }
}
