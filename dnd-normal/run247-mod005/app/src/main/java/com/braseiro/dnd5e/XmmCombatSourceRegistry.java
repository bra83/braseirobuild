package com.braseiro.dnd5e;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact inventory identities, not a certificate of mechanical semantics.
 * No caller (including a model preview) can register or enable an executor.
 * Admission requires a reviewed host implementation and current integration tests.
 */
public final class XmmCombatSourceRegistry {
    private XmmCombatSourceRegistry() { }
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
    public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
    public enum Family { DAMAGE_OR_SAVE_BRANCH, OTHER_SOURCE_EFFECT, ATTACK_HIT_CONDITION,
        SWALLOW_AND_ESCAPE, EXPLICIT_EFFECT_CHOICE, MONSTER_SPELL_ACTION }
    public enum Status { NOT_INTEGRATED }
    public static final class Entry {
        public final String pointer,monster,action,lot,executor,blocker;
        public final Family family;
        public final Status status=Status.NOT_INTEGRATED;
        private Entry(String pointer,String monster,String action,String lot,Family family,
                String executor,String blocker) {
            this.pointer=pointer;this.monster=monster;this.action=action;this.lot=lot;
            this.family=family;this.executor=executor;this.blocker=blocker;
        }
    }
    private static final Map<String,Entry> ENTRIES;
    static {
        Map<String,Entry> entries=new LinkedHashMap<>();
        add(entries,"/monster/311/action/1/entries/0","Mummy","Rotting Fist","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/312/action/1/entries/0","Mummy Lord","Rotting Fist","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/339/action/2/entries/0","Peryton","Talons","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/344/action/1/entries/0","Pirate Admiral","Scimitar","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/345/action/1/entries/0","Pirate Captain","Rapier","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/358/action/0/entries/0","Psychic Gray Ooze","Pseudopod","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/365/action/1/entries/0","Rakshasa","Cursed Touch","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/369/action/1/entries/0","Red Slaad","Injecting Claw","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/393/action/0/entries/0","Shadow","Draining Swipe","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/403/action/1/entries/0","Solar","Flying Sword","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/405/action/0/entries/0","Specter","Life Drain","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/407/action/2/entries/0","Sphinx of Secrets","Curse of the Riddle","04",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch04","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/416/action/1/entries/0","Steam Mephit","Steam Breath {@recharge}","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/447/action/1/entries/0","Ultroloth","Mercurial Whip","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/452/action/1/entries/0","Vampire Nightbringer","Bite","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/454/bonus/0/entries/0","Vampire Umbral Lord","Sanguine Drain","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/463/action/1/entries/0","Warrior Commander","Greatsword","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/469/action/1/entries/0","Werebear","Bite (Bear or Hybrid Form Only)","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/470/action/1/entries/0","Wereboar","Gore (Boar or Hybrid Form Only)","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/471/action/1/entries/0","Wererat","Bite (Rat or Hybrid Form Only)","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/472/action/1/entries/0","Weretiger","Bite (Tiger or Hybrid Form Only)","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/473/action/1/entries/0","Werewolf","Bite (Wolf or Hybrid Form Only)","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/475/action/3/entries/0","Wight","Life Drain","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/480/action/0/entries/0","Worg","Bite","05",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch05","pending_damage_riders_and_recovery_have_no_reviewed_sqlite_host");
        add(entries,"/monster/481/action/0/entries/0","Wraith","Life Drain","06",Family.DAMAGE_OR_SAVE_BRANCH,"XmmDamageOrSaveBranch06","pinned_source_prose_unavailable_critical_temp_hp_max_hp_recovery_not_verified");
        add(entries,"/monster/9/action/3/entries/0","Adult Copper Dragon","Slowing Breath","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/10/action/3/entries/0","Adult Gold Dragon","Weakening Breath","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/21/action/3/entries/0","Ancient Copper Dragon","Slowing Breath","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/22/action/3/entries/0","Ancient Gold Dragon","Weakening Breath","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/60/action/2/entries/0","Beholder","Eye Rays","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/61/action/2/entries/0","Beholder Zombie","Eye Rays","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/63/bonus/0/entries/0","Berserker Commander","Frenzied Rush","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/103/action/2/entries/0","Copper Dragon Wyrmling","Slowing Breath","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/112/reaction/0/entries/0","Cyclops Sentry","Limited Foresight {@recharge}","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/120/action/2/entries/0","Death Tyrant","Eye Rays","07",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect01","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/120/bonus/0/entries/0","Death Tyrant","Negative Energy Cone","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/122/legendary/0/entries/0","Demilich","Energy Drain","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/140/action/2/entries/0","Elemental Cataclysm","Cataclysmic Event {@recharge 4}","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/141/reaction/0/entries/0","Elemental Cultist","Elemental Absorption (1/Day)","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/144/legendary/0/entries/0","Empyrean","Bolster","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/161/bonus/0/entries/0","Frost Giant","War Cry {@recharge 5}","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/211/action/3/entries/0","Gnoll Pack Lord","Incite Rampage {@recharge 5}","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/218/action/3/entries/0","Gold Dragon Wyrmling","Weakening Breath","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/317/reaction/0/entries/0","Nalfeshnee","Pursuit","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/344/bonus/0/entries/0","Pirate Admiral","Rally (1/Day)","08",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect02","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/372/bonus/0/entries/0","Revenant","Vow of Revenge (1/Day)","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/386/action/2/entries/0","Satyr Revelmaster","Fey Melody {@recharge 4}","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/404/action/2/entries/0","Spectator","Eye Rays","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/408/action/2/entries/0","Sphinx of Valor","Roar (3/Day)","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/409/reaction/0/entries/0","Sphinx of Wonder","Burst of Ingenuity (2/Day)","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/448/bonus/0/entries/0","Umber Hulk","Confusing Gaze {@recharge 5}","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/449/legendary/1/entries/0","Unicorn","Shimmering Shield","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/490/action/3/entries/0","Young Copper Dragon","Slowing Breath","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/491/action/3/entries/0","Young Gold Dragon","Weakening Breath","09",Family.OTHER_SOURCE_EFFECT,"XmmOtherSourceEffect03","pending_effects_and_trigger_economy_have_no_reviewed_sqlite_host");
        add(entries,"/monster/300/action/0/entries/0","Mind Flayer","Tentacles","10",Family.ATTACK_HIT_CONDITION,"XmmAttackHitCondition01","linked_grapple_conditions_and_recurring_damage_not_hosted");
        add(entries,"/monster/376/action/2/entries/0","Roper","Tentacle","10",Family.ATTACK_HIT_CONDITION,"XmmAttackHitCondition01","linked_grapple_conditions_and_recurring_damage_not_hosted");
        add(entries,"/monster/431/action/0/entries/0","Swarm of Stirges","Swarm of Proboscises","10",Family.ATTACK_HIT_CONDITION,"XmmAttackHitCondition01","linked_grapple_conditions_and_recurring_damage_not_hosted");
        add(entries,"/monster/433/action/1/entries/0","Tarrasque","Bite","10",Family.ATTACK_HIT_CONDITION,"XmmAttackHitCondition01","linked_grapple_conditions_and_recurring_damage_not_hosted");
        add(entries,"/monster/182/action/1/entries/0","Giant Frog","Swallow","11",Family.SWALLOW_AND_ESCAPE,"XmmSwallowAndEscape01","swallow_ownership_escape_and_recurring_damage_not_hosted");
        add(entries,"/monster/194/action/1/entries/0","Giant Toad","Swallow","11",Family.SWALLOW_AND_ESCAPE,"XmmSwallowAndEscape01","swallow_ownership_escape_and_recurring_damage_not_hosted");
        add(entries,"/monster/31/action/0/entries/0","Animated Rug of Smothering","Smother","12",Family.EXPLICIT_EFFECT_CHOICE,"XmmExplicitEffectChoice01","explicit_choice_bundle_not_hosted");
        add(entries,"/monster/71/action/1/entries/0","Blue Slaad","Mutating Claw","13",Family.MONSTER_SPELL_ACTION,"XmmMonsterSpellAction01","curse_recovery_and_24h_effects_not_hosted");
        if(entries.size()!=62)throw new IllegalStateException("xmm_registry_count_drift");
        ENTRIES=Collections.unmodifiableMap(entries);
    }
    private static void add(Map<String,Entry> entries,String pointer,String monster,String action,
            String lot,Family family,String executor,String blocker) {
        if(entries.put(pointer,new Entry(pointer,monster,action,lot,family,executor,blocker))!=null)
            throw new IllegalStateException("duplicate_xmm_source_pointer");
    }
    public static Entry get(String pointer){return ENTRIES.get(pointer);}
    public static Map<String,Entry> entries(){return ENTRIES;}
}
