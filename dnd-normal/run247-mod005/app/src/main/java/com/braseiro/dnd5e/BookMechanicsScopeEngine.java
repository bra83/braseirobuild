package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * External-source scope guard for the D&D manual folder audited in RUN118.
 *
 * This is intentionally fail-closed: the runtime may be complete for its own
 * compiled catalog while the external manual folder still contains unreviewed
 * mechanical sources. A release cannot claim 100% until the book scope itself
 * has been reviewed and declared complete.
 *
 * User exclusion: files whose names start with "mystara" (case-insensitive), regardless of separator
 * are outside this audit and do not count as missing mechanics.
 */
public final class BookMechanicsScopeEngine {
    private BookMechanicsScopeEngine(){}

    public static JSONObject coverage(){
        JSONObject o=new JSONObject();
        try{
            o.put("snapshot_date","2026-09-27")
             .put("source","Google Drive D&D manual folder")
             .put("direct_entries_total",253)
             .put("pdf_total",243)
             .put("excluded_mystara_prefix_entries",66)
             .put("excluded_mystara_prefix_pdfs",66)
             .put("in_scope_entries",187)
             .put("in_scope_pdfs",177)
             .put("excluded_prefix","mystara*")
             .put("excluded_case_insensitive",true)
             .put("target_known",false)
             .put("scope_includes_non_mystara_third_party_and_homebrew",true)
             .put("complete",false)
             .put("status","book_mechanics_scope_review_pending")
             .put("reason","The runtime completion target only covers already-registered compiled content; the remaining in-scope manuals have not yet been exhaustively classified and mechanically bound.")
             .put("policy","RAG/index presence does not count as mechanical integration.")
             .put("known_source_bound_packs",new JSONArray()
                     .put("Caldeirao de Tasha para tudo.pdf")
                     .put("Cozinheiro.pdf")
                     .put("Animal Handlers Manual.pdf")
                     .put("System Reference Document 5.1 (compiled bestiary batches)"))
             .put("reviewed_groups",new JSONArray()
                     .put(reviewed("core_2014_2024","partial_core_runtime_only","DndRulesEngine exposes a finite deterministic core catalog and explicitly requires local source/structured resolution for class, spell, monster and item specifics."))
                     .put(reviewed("core_2024_specific_systems","partial_phase1_compiled","RUN122 compiles Heroic Inspiration, all eight Weapon Mastery properties, revised Analyze/Influence/Search/Use Object validation, tool-skill synergy, nonmagical/healing-potion/spell-scroll crafting, and Bastion acquisition/turn/basic facilities/special-facility counts/orders/event bands. Classes, subclasses, backgrounds, species, feats, spells, magic items, full Bastion special-facility benefits and other 2024 effects still require closure."))
                     .put(reviewed("tasha","run131_book_closure_pass","RUN131 closes Tasha with 30/30 subclasses, 21/21 new spells, 47/47 magic items, 6/6 artifacts, 15/15 feats and 92/92 supplementary procedures. Do not reopen except for a demonstrated regression."))
                     .put(reviewed("xanathar","partial_downtime_compiled","Downtime Revisited pp.123-134 has 15 compiled procedures. RUN122 adds the general tool+skill synergy rule, the solo-monster CR table for parties of 4-6, simple/complex trap profiles, initiative and XP guidance. Full individual tool special uses, multiple-monster encounter tables, random encounter tables, subclasses, feats and spells remain outside complete effect coverage."))
                     .put(reviewed("strixhaven","partial_character_plus_academic_runtime","Academic procedures remain compiled. RUN132/133 add executable Owlin, all five student backgrounds, Strixhaven Initiate explicit-choice runtime and Strixhaven Mascot prerequisite/use runtime. Strixhaven spells, magic items and statblocks remain to close; campaign chapters stay post-APK."))
                     .put(reviewed("volo_ravnica_eberron_character_options","partial_crossbook_character_runtime","RUN132/133 bind 24 supplemental species across Volo, Ravnica, Strixhaven and Eberron plus all ten Ravnica guild backgrounds. Deterministic traits mutate/persist state and required choices remain explicit. Monsters, items, lore and additional character options from these books remain pending."))
                     .put(reviewed("mordenkainen_fizban_sword_coast","character_and_creature_closure_pending","These sources still require transversal character/creature/lore closure; catalog or RAG presence is not counted as implementation."))
                     .put(reviewed("cozinheiro_animal_handlers","compiled_phase_1","Both dedicated engines self-identify as compiled_phase_1, so they are not evidence of exhaustive book integration."))
                     .put(reviewed("srd51_bestiary","compiled_statblocks","175 authorized statblocks are compiled inside the currently declared SRD bestiary scope.")));
        }catch(Exception ignored){}
        return o;
    }

    private static JSONObject reviewed(String group,String status,String evidence){
        JSONObject x=new JSONObject();try{x.put("group",group).put("status",status).put("evidence",evidence);}catch(Exception ignored){}return x;
    }
}

