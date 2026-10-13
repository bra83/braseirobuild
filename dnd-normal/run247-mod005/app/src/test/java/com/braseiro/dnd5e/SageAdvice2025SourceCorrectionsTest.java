package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source-derived independent regression assertions for the pinned 2025 XSAC edition.
 * Does not claim that merely mentioning a rule certifies all gameplay semantics.
 */
public final class SageAdvice2025SourceCorrectionsTest {
    private final SageAdvice2025RulesEngine rules = new SageAdvice2025RulesEngine();

    @Test public void editorialCatalogueHasExactly163QuestionsPlusSeparateDmPolicy() throws Exception {
        JSONObject coverage=rules.coverage();
        assertEquals(163,coverage.getInt("source_rulings"));
        assertEquals(163,coverage.getInt("editorial_qas"));
        assertEquals(1,coverage.getInt("editorial_policy_endpoints"));
        assertEquals(57,coverage.getJSONObject("category_counts").getInt("spellcasting"));
        assertFalse(coverage.getJSONObject("category_counts").has("rules_role_meta"));
        int officialQaCount=0;
        org.json.JSONObject totals=coverage.getJSONObject("category_counts");
        for(java.util.Iterator<String> it=totals.keys(); it.hasNext();) {
            officialQaCount+=totals.getInt(it.next());
        }
        assertEquals(163,officialQaCount);
        assertFalse(coverage.getBoolean("source_fidelity_semantically_certified"));
        assertEquals(163,SageAdvice163RuntimeContractTest.cases().size());
        assertEquals("MANUAL_DM",rules.resolve("dm_final_say",null).getString("resolution_mode"));
    }

    @Test public void mageArmorIsNamedFigurativelyRatherThanWornArmor() throws Exception {
        JSONObject answer=rules.resolve("mage_armor_name_is_not_literal_armor",new JSONObject());
        assertTrue(answer.getBoolean("resolved"));
        assertFalse(answer.getBoolean("counts_as_worn_armor"));
        assertEquals("spellcasting",answer.getString("category"));
        assertFalse(answer.getBoolean("auto_mutation_allowed"));
    }

    @Test public void ordinaryScrollAndSpellScrollHaveDifferentRequirements() throws Exception {
        JSONObject answer=rules.resolve("spell_scroll_class_list",new JSONObject());
        assertTrue(answer.getBoolean("resolved"));
        assertFalse(answer.getBoolean("general_scroll_requires_spell_list"));
        assertTrue(answer.getBoolean("spell_scroll_requires_spell_list"));
        assertTrue(answer.getString("rule").contains("alfabetizado"));
    }

    @Test public void concentrationCheckRetainsExactThirtyDamageDcFifteenExample() throws Exception {
        JSONObject answer=rules.resolve("temp_hp_concentration_dc",new JSONObject());
        assertEquals(30,answer.getInt("example_incoming_damage"));
        assertEquals(15,answer.getInt("example_concentration_dc"));
        assertFalse(answer.getBoolean("auto_rolls"));
    }

    @Test public void dispellingGlobeDoesNotDispellInsideEffects() throws Exception {
        JSONObject answer=rules.resolve("dispel_globe_invulnerability",new JSONObject());
        assertTrue(answer.getBoolean("dispels_globe_barrier"));
        assertFalse(answer.getBoolean("dispels_other_inside_effects"));
    }

    @Test public void scrollDecisionRequiresExplicitBookClassAndLiteracyInputs() throws Exception {
        JSONObject ordinary=rules.resolve("spell_scroll_class_list",
                new JSONObject().put("scroll_kind","general_scroll").put("literate",true));
        assertTrue(ordinary.getBoolean("can_attempt_activation"));
        assertFalse(rules.resolve("spell_scroll_class_list",
                new JSONObject().put("scroll_kind","general_scroll").put("literate",false))
                .getBoolean("can_attempt_activation"));
        assertTrue(rules.resolve("spell_scroll_class_list",
                new JSONObject().put("scroll_kind","spell_scroll").put("spell_on_class_list",true))
                .getBoolean("can_read_spell_scroll"));
        assertFalse(rules.resolve("spell_scroll_class_list",
                new JSONObject().put("scroll_kind","spell_scroll").put("spell_on_class_list",false))
                .getBoolean("can_read_spell_scroll"));
        JSONObject unknown=rules.resolve("spell_scroll_class_list",
                new JSONObject().put("scroll_kind","mystery"));
        assertFalse(unknown.getBoolean("resolved"));
        JSONObject incomplete=rules.resolve("spell_scroll_class_list",
                new JSONObject().put("scroll_kind","spell_scroll"));
        assertTrue(incomplete.isNull("can_read_spell_scroll"));
    }

    @Test public void concentrationDcComputedOnlyOnExplicitDamageWithoutHiddenRolls() throws Exception {
        JSONObject input=new JSONObject().put("damage_taken",30);
        String original=input.toString();
        JSONObject result=rules.resolve("temp_hp_concentration_dc",input);
        assertEquals(15,result.getInt("calculated_dc"));
        assertTrue(result.getBoolean("requires_check"));
        assertEquals(original,input.toString());
        assertFalse(result.getBoolean("auto_rolls"));
        assertEquals(10,rules.resolve("temp_hp_concentration_dc",
                new JSONObject().put("damage_taken",7)).getInt("calculated_dc"));
        assertFalse(rules.resolve("temp_hp_concentration_dc",
                new JSONObject().put("damage_taken",0)).getBoolean("requires_check"));
        assertFalse(rules.resolve("temp_hp_concentration_dc",
                new JSONObject().put("damage_taken",-1)).getBoolean("resolved"));
    }

    @Test public void dmPolicyAndUnknownIdsDoNotMutateInput() throws Exception {
        JSONObject input=new JSONObject().put("hp",17).put("spell_slot",3);
        String before=input.toString();
        JSONObject meta=rules.resolve("dm_final_say",input);
        assertEquals("MANUAL_DM",meta.getString("resolution_mode"));
        assertFalse(meta.getBoolean("auto_mutation_allowed"));
        assertEquals(before,input.toString());
        JSONObject unknown=rules.resolve("unknown_ruling_code",input);
        assertFalse(unknown.getBoolean("resolved"));
        assertEquals("blocked",unknown.getString("status"));
        assertEquals(before,input.toString());
    }

    @Test public void gatewayPreservesSourceRulingAndNeverExecutesHiddenChanges() throws Exception {
        OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
        JSONObject input=new JSONObject().put("rule_id","spell_scroll_class_list");
        JSONObject out=gateway.execute("xsac","ruling",input);
        assertTrue(out.getBoolean("executed"));
        assertFalse(out.getBoolean("auto_mutation_allowed"));
        assertTrue(out.getJSONObject("result").getBoolean("spell_scroll_requires_spell_list"));
        JSONObject blocked=gateway.execute("xsac","ruling",new JSONObject().put("rule_id","invalid_id"));
        assertFalse(blocked.getBoolean("executed"));
    }
}
