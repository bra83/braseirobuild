"""Negative-path, no-network tests for the XMM 503 independent comparator."""
import unittest
from io import BytesIO
import verify_xmm_503_source as xmm

class XmmIndependentSourceTests(unittest.TestCase):
    def test_source_fetch_retries_transient_timeout_and_keeps_pin_url(self):
        calls=[]
        class Response(BytesIO):
            def __enter__(self): return self
            def __exit__(self,*args): self.close()
        def opener(request,timeout):
            calls.append((request.full_url,timeout))
            if len(calls)==1: raise TimeoutError("transient mirror timeout")
            return Response(b"pinned")
        result=xmm.fetch_source_bytes("https://example.invalid/pinned.json",
            attempts=2,opener=opener)
        self.assertEqual(b"pinned",result)
        self.assertEqual(2,len(calls))
        self.assertEqual(["https://example.invalid/pinned.json"]*2,
                         [x[0] for x in calls])
    def test_git_blob_has_null_separator(self):
        import hashlib
        self.assertEqual(hashlib.sha1(b"blob 4\0test").hexdigest(),xmm.blob_sha(b"test"))
    def test_compiled_java_row_parser_fails_invalid_json(self):
        with self.assertRaises(ValueError):
            xmm.java_rows('"{not a json object}"')
    def test_recharge_4_5_6_are_distinct_manual_thresholds(self):
        m={"trait":[],"action":[{"name":"Flame {@recharge 4}"},
                                 {"name":"Frost {@recharge 5}"},
                                 {"name":"Breath {@recharge}"}]}
        self.assertEqual([4,5,6],[x["min"] for x in xmm.recharge_rows(m)])
        self.assertEqual("Flame",xmm.recharge_rows(m)[0]["name"])
    def test_statblock_type_choice_cannot_fabricate_new_type(self):
        self.assertEqual("celestial|fiend",xmm.monster_type({"type":{"choose":["celestial","fiend"]}}))
        with self.assertRaises(ValueError):xmm.monster_type({"type":{"broken":True}})
    def test_book_source_locator_dereferences_real_section(self):
        doc={"bookData":[{"data":[{"name":"example"}]}]}
        self.assertEqual("example",xmm.deref(doc,"/bookData/0/data/0")["name"])
    def test_spell_usage_rows_are_precise_and_not_raw_prose(self):
        source=[{"name":"Wizard","spellcasting":[
          {"name":"Spellcasting","will":["{@spell Mage Hand|XPHB}"],
           "daily":{"2":["{@spell Fireball|XPHB}"]},"ability":"int",
           "displayAs":"action","headerEntries":["Do not include prose"]}]}]
        data=xmm.usage_rows(source)
        self.assertEqual(1,len(data))
        entry=data[0]["entries"][0]
        self.assertEqual(["Mage Hand"],entry["will"])
        self.assertEqual(["Fireball"],entry["daily"]["2"])
        self.assertNotIn("headerEntries",entry)
        self.assertEqual("/monster/0/spellcasting",data[0]["source_pointer"])
    def test_habitat_group_comparison_preserves_order_and_membership(self):
        ref={"entries":[{"type":"table","caption":"Forest Monsters",
                         "rows":[["2","{@creature Dryad|XMM}, {@creature Satyr|XMM}"]]}]}
        expected=xmm.source_habitat_groups(ref)
        self.assertEqual({"Forest Monsters":["Dryad","Satyr"]},expected)
        self.assertNotEqual({"Forest Monsters":["Dryad"]},expected)
    def test_named_groups_preserve_source_list_membership(self):
        ref={"entries":[{"type":"list","items":[
            {"name":"Fiends","type":"list","items":["{@creature Balor|XMM}","{@creature Pit Fiend|XMM}"]}]}]}
        expected=xmm.source_named_groups(ref)
        self.assertEqual(["Balor","Pit Fiend"],expected["Fiends"])
        with self.assertRaises(ValueError):xmm.source_named_groups({"entries":[]})
    def test_trait_semantics_drift_is_hard_failure(self):
        source=[{"name":"Fake","trait":[{"name":"Magic Resistance","entries":["Attack bonus"]}]}]
        with self.assertRaisesRegex(ValueError,"XMM_TRAIT_SOURCE_SEMANTICS_CHANGED"):
            xmm.trait_rows(source)
    def test_legendary_resistance_requires_pinned_daily_limit(self):
        source=[{"name":"Fake","trait":[{"name":"Legendary Resistance",
                 "entries":["If the dragon fails a saving throw, it can choose to succeed instead."]}]}]
        with self.assertRaisesRegex(ValueError,"XMM_LEGENDARY_USES_NOT_PINNED"):
            xmm.trait_rows(source)
    def test_grapple_escape_dc_is_on_hit_not_a_saving_throw(self):
        source=[{"name":"Ankheg","action":[{"name":"Bite","entries":[
          "{@atkr m} reach 5 ft. {@condition Grappled|XPHB}, escape {@dc 13}."],}]}]
        compiled=[{"id":"Ankheg","actions":[{"name":"Bite","escape_dc":13,
          "condition_on_hit":"grappled","condition_max_target_size":"large","reach_ft":5}]}]
        contracts=[{"id":"Ankheg","name":"Bite","slot":0,"condition":"grappled",
          "dc":13,"size":"Large"}]
        errors,count=xmm.check_grapple_escape_riders(source,compiled,contracts,1)
        self.assertEqual([],errors); self.assertEqual(1,count)
        compiled[0]["actions"][0]["save_dc"]=13
        errors,_=xmm.check_grapple_escape_riders(source,compiled,contracts,1)
        self.assertEqual(["Ankheg:Bite:ESCAPE_DC_MISCLASSIFIED","GRAPPLE_ESCAPE_RECORD_COUNT:0"],errors)
        compiled[0]["actions"][0].pop("save_dc")
        compiled[0]["actions"][0]["reach_ft"]=10
        errors,_=xmm.check_grapple_escape_riders(source,compiled,contracts,1)
        self.assertEqual(["Ankheg:Bite:GRAPPLE_REACH_MISMATCH",
                          "GRAPPLE_ESCAPE_RECORD_COUNT:0"],errors)
    def test_save_damage_contract_is_compared_to_source_behavior(self):
        source=[{"name":"Test Dragon","action":[{"name":"Fire Breath {@recharge 5}",
          "entries":["{@actSave dex} {@dc 14}, each creature. {@actSaveFail} 21 ({@damage 6d6}) Fire damage. {@actSaveSuccess} Half damage."]}]}]
        row={"id":"Test Dragon","name":"Fire Breath","category":"action",
          "slot":0,"source_pointer":"/monster/0/action/0/entries/0","ability":"dex",
          "dc":14,"average":21,"dice":"6d6","damage_type":"fire","recharge":True}
        errors,count=xmm.check_save_damage_actions(source,[row],1)
        self.assertEqual([],errors); self.assertEqual(1,count)
        row["dc"]=15
        errors,_=xmm.check_save_damage_actions(source,[row],1)
        self.assertEqual(["Test Dragon:Fire Breath:SAVE_DAMAGE_SEMANTICS_MISMATCH",
                          "SAVE_DAMAGE_RECORD_COUNT:0"],errors)
        row["dc"]=14
        errors,_=xmm.check_save_damage_actions(source,[row,row.copy()],1)
        self.assertEqual(["Test Dragon:Fire Breath:DUPLICATE_SAVE_SOURCE_POINTER"],errors)
    def test_save_damage_contract_rejects_wrong_slot_and_extra_effect_shape(self):
        source=[{"name":"Test Dragon","action":[{"name":"Other","entries":["No save."]}]}]
        row={"id":"Test Dragon","name":"Fire Breath","category":"action",
          "source_pointer":"/monster/0/action/0/entries/0","ability":"dex",
          "dc":14,"average":21,"dice":"6d6","damage_type":"fire","recharge":True}
        errors,_=xmm.check_save_damage_actions(source,[row],1)
        self.assertEqual(["Test Dragon:Fire Breath:SAVE_ACTION_SLOT_MISMATCH",
                          "SAVE_DAMAGE_RECORD_COUNT:0"],errors)
        source[0]["action"][0]["name"]="Fire Breath"
        errors,_=xmm.check_save_damage_actions(source,[row],1)
        self.assertEqual(["Test Dragon:Fire Breath:SOURCE_NOT_SIMPLE_SAVE_FOR_HALF",
                          "SAVE_DAMAGE_RECORD_COUNT:0"],errors)
    def test_bloodied_damage_branch_matches_source_and_is_not_summed(self):
        source=[{"name":"Blood Hawk","action":[{"name":"Beak","entries":[
          "{@atkr m} {@hit 4}, reach 5 ft. {@h}4 ({@damage 1d4 + 2}) Piercing damage, "
          "or 6 ({@damage 1d8 + 2}) Piercing damage if the target is {@status Bloodied|XPHB}."]}]}]
        compiled=[{"id":"Blood Hawk","actions":[{"name":"Beak","damage":"1d4 + 2",
          "damage_bloodied":"1d8 + 2","damage_type":"piercing",
          "manual_rider_resolution_required":False}]}]
        errors,counts=xmm.check_conditional_attack_damage(source,compiled,1,0)
        self.assertEqual([],errors);self.assertEqual(1,counts["source_bloodied_attack_branches"])
        compiled[0]["actions"][0]["damage_bloodied"]="1d4 + 2 + 1d8 + 2"
        errors,_=xmm.check_conditional_attack_damage(source,compiled,1,0)
        self.assertTrue(any("BLOODIED_DAMAGE:" in e for e in errors))
    def test_unmodeled_conditional_damage_must_remain_manual(self):
        source=[{"name":"Goat","action":[{"name":"Ram","entries":[
          "{@atkr m} {@hit 2}, reach 5 ft. {@h}1 Bludgeoning damage, or 2 "
          "({@damage 1d4}) Bludgeoning damage if the goat moved 20+ feet straight "
          "toward the target immediately before the hit."]}]}]
        compiled=[{"id":"Goat","actions":[{"name":"Ram","damage":"1",
          "damage_type":"bludgeoning","manual_rider_resolution_required":True,
          "damage_alternative_expression":"1d4"}]}]
        errors,counts=xmm.check_conditional_attack_damage(source,compiled,0,1)
        self.assertEqual([],errors);self.assertEqual(1,counts["source_alternate_attack_branches_manual"])
        compiled[0]["actions"][0]["manual_rider_resolution_required"]=False
        errors,_=xmm.check_conditional_attack_damage(source,compiled,0,1)
        self.assertIn("Goat:Ram:UNMODELED_ALTERNATE_DAMAGE_NOT_MANUAL",errors)
    def test_mimic_grappled_bite_prerequisite_is_not_a_condition_rider(self):
        source=[{"name":"Mimic","action":[{"name":"Bite","entries":[
          "{@atkr m} {@h}7 ({@damage 1d8 + 3}) Piercing damage—or 12 "
          "({@damage 2d8 + 3}) Piercing damage if grappled by the mimic—plus 4 "
          "({@damage 1d8}) Acid damage."]}]}]
        compiled=[{"id":"Mimic","actions":[{"name":"Bite","damage":"1d8 + 3 + 1d8",
          "damage_type":"piercing + acid","manual_rider_resolution_required":True}]}]
        self.assertEqual(([],{"source_bloodied_attack_branches":0,
            "source_alternate_attack_branches_manual":1}),
            xmm.check_conditional_attack_damage(source,compiled,0,1))
        compiled[0]["actions"][0]["condition_on_fail"]="grappled"
        errors,_=xmm.check_conditional_attack_damage(source,compiled,0,1)
        self.assertIn("Mimic:Bite:GRAPPLED_PREREQUISITE_MISMODELED_AS_RIDER",errors)
    def test_peryton_gore_extra_damage_is_additive_and_movement_gated(self):
        source={"name":"Gore","entries":[
          "{@h}7 ({@damage 1d8 + 3}) Piercing damage. If the peryton moved 30+ feet straight toward the target immediately before the hit, the target takes an extra 9 ({@damage 2d8}) Piercing damage."]}
        compiled={"damage":"1d8 + 3","damage_type":"piercing","extra_damage":"2d8",
          "extra_damage_type":"piercing",
          "extra_damage_condition":"moved_30ft_straight_toward_target_immediately_before_hit",
          "manual_rider_resolution_required":True}
        self.assertTrue(xmm.peryton_gore_extra_damage_matches(source,compiled))
        compiled["damage_alternative_expression"]="2d8"
        self.assertFalse(xmm.peryton_gore_extra_damage_matches(source,compiled))
    def test_charge_damage_is_separate_from_base_and_has_hit_gates(self):
        source={"name":"Gore","entries":[
          "{@h}4 ({@damage 1d6 + 1}) Piercing damage. If the target is a Medium or smaller creature and the boar moved 20+ feet straight toward it immediately before the hit, the target takes an extra 3 ({@damage 1d6}) Piercing damage and has the {@condition Prone|XPHB} condition."]}
        compiled={"damage":"1d6 + 1","damage_type":"piercing","extra_damage":"1d6",
          "extra_damage_type":"piercing",
          "extra_damage_condition":"moved_20ft_straight_toward_target_immediately_before_hit",
          "extra_damage_condition_on_hit":"prone","extra_damage_max_target_size":"medium",
          "manual_rider_resolution_required":True}
        self.assertTrue(xmm.charge_extra_damage_matches("Boar",source,compiled))
        compiled["condition_on_fail"]="prone"
        self.assertFalse(xmm.charge_extra_damage_matches("Boar",source,compiled))
    def test_condition_on_fail_requires_a_source_saving_throw(self):
        source={"name":"Gore","entries":["{@atkr m} hit; the target has {@condition Prone|XPHB}."]}
        self.assertFalse(xmm.condition_on_fail_matches_source(source,{"condition_on_fail":"prone"}))
        saved={"name":"Breath","entries":["{@actSave dex} {@dc 15}, target. {@actSaveFail} {@condition Prone|XPHB}."]}
        self.assertTrue(xmm.condition_on_fail_matches_source(saved,{"condition_on_fail":"prone","save_dc":15}))
        spell_save={"name":"Charm","entries":["The spell requires a spell save {@dc 15}."]}
        self.assertTrue(xmm.source_action_has_saving_throw(spell_save))
    def test_unmodeled_action_riders_fail_closed(self):
        source=[{"name":"Giant Axe Beak","action":[{"name":"Sharpened Beak",
          "entries":["Hit the target, and a creature within 5 feet of the target takes damage."]}]}]
        compiled=[{"id":"Giant Axe Beak","actions":[{"name":"Sharpened Beak",
          "manual_unmodeled_source_effect_required":False}]}]
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertEqual(["Giant Axe Beak:Sharpened Beak:UNTYPED_SOURCE_EFFECT_NOT_MANUAL"],errors)
        compiled[0]["actions"][0]["manual_unmodeled_source_effect_required"]=True
        self.assertEqual(([],1),xmm.check_unmodeled_action_effects(source,compiled))
    def test_unmodeled_movement_object_and_persistent_effects_fail_closed(self):
        effects=[
          "the target's Hit Point maximum decreases by 5.",
          "the target gains Temporary Hit Points equal to the damage taken.",
          "the attacker pushes the target up to 15 feet straight away.",
          "the attacker can teleport the target up to 10 feet.",
          "{@hom}The weapon magically returns to the wielder's hand.",
          "the target starts {@hazard burning|XPHB}.",
          "While attached, the target takes 2d4 damage at the start of each turn."
        ]
        source=[];compiled=[]
        for i,effect in enumerate(effects):
            name=f"Monster {i}"
            source.append({"name":name,"action":[{"name":"Effect","entries":[effect]}]})
            compiled.append({"id":name,"actions":[{"name":"Effect",
                "manual_unmodeled_source_effect_required":False}]})
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(len(effects),count)
        self.assertEqual(len(effects),len(errors))
        self.assertTrue(all(e.endswith(":UNTYPED_SOURCE_EFFECT_NOT_MANUAL") for e in errors))
    def test_compound_conditions_and_repeated_saves_require_manual_resolution(self):
        self.assertTrue(xmm.source_action_requires_manual_resolution({"entries":[
            "The target has {@condition Deafened|XPHB} and {@condition Frightened|XPHB} conditions."]}))
        self.assertTrue(xmm.source_action_requires_manual_resolution({"entries":[
            "The target has the {@condition Poisoned|XPHB} condition and repeats the save at the end of each of its turns."]}))
        self.assertFalse(xmm.source_action_requires_manual_resolution({"entries":[
            "The target has the {@condition Frightened|XPHB} condition until the end of its next turn."]}))
    def test_rakshasa_source_turn_condition_package_has_a_typed_contract(self):
        source=[{"name":"Rakshasa","action":[{"name":"Baleful Command {@recharge 5}","entries":[
            "The target has the {@condition Frightened|XPHB} and {@condition Incapacitated|XPHB} conditions until the start of the rakshasa's next turn."]}]}]
        compiled=[{"id":"Rakshasa","actions":[{"name":"Baleful Command",
            "condition_on_fail":"frightened, incapacitated","condition_duration_rounds":1,
            "condition_duration_tick":"source_turn_start",
            "manual_unmodeled_source_effect_required":False}]}]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[0]["actions"][0]["condition_duration_tick"]="target_turn_start"
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertEqual(["Rakshasa:Baleful Command {@recharge 5}:UNTYPED_SOURCE_EFFECT_NOT_MANUAL"],errors)
    def test_storm_giant_compound_hit_conditions_need_typed_duration(self):
        source=[{"name":"Storm Giant","action":[{"name":"Thunderbolt","entries":[
            "Hit: damage, and the target has the {@condition Blinded|XPHB} and {@condition Deafened|XPHB} conditions until the start of the giant's next turn."]}]}]
        compiled=[{"id":"Storm Giant","actions":[{"name":"Thunderbolt","condition_on_hit":"blinded, deafened",
            "condition_duration_rounds":1,"condition_duration_tick":"source_turn_start",
            "manual_unmodeled_source_effect_required":False}]}]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[0]["actions"][0]["condition_on_hit"]="blinded"
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
    def test_scarecrow_fearsome_claw_expires_at_end_of_its_next_turn(self):
        source=[{"name":"Scarecrow","action":[{"name":"Fearsome Claw","entries":[
            "Hit: damage, and the target has the {@condition Frightened|XPHB} condition until the end of the scarecrow's next turn."]}]}]
        compiled=[{"id":"Scarecrow","actions":[{"name":"Fearsome Claw","condition_on_hit":"frightened",
            "condition_duration_rounds":1,"condition_duration_tick":"source_turn_end"}]}]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[0]["actions"][0]["condition_duration_tick"]="source_turn_start"
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
    def test_merrow_bite_uses_its_own_next_turn_end(self):
        source=[{"name":"Merrow","action":[{"name":"Bite","entries":[
            "Hit: damage, and the target has the {@condition Poisoned|XPHB} condition until the end of the merrow's next turn."]}]}]
        compiled=[{"id":"Merrow","actions":[{"name":"Bite","condition_on_hit":"poisoned",
            "condition_duration_rounds":1,"condition_duration_tick":"source_turn_end"}]}]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[0]["actions"][0]["condition_duration_tick"]="target_turn_end"
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
    def test_source_turn_start_on_hit_condition_is_bound_to_pinned_owner_turn(self):
        source={"name":"Assassin","action":{"name":"Shortsword","entries":[
            "Hit: the target has the {@condition Poisoned|XPHB} condition until the start of the assassin's next turn."]}}
        compiled={"condition_on_hit":"poisoned","condition_duration_rounds":1,
            "condition_duration_tick":"source_turn_start","manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_requires_manual_resolution(source["action"],"Assassin"))
        self.assertTrue(xmm.source_turn_start_hit_condition_matches("Assassin",source["action"],compiled))
        compiled["condition_duration_tick"]="target_turn_start"
        self.assertFalse(xmm.source_turn_start_hit_condition_matches("Assassin",source["action"],compiled))
        compiled["condition_duration_tick"]="source_turn_start"
        compiled["condition_on_hit"]="frightened"
        self.assertFalse(xmm.source_turn_start_hit_condition_matches("Assassin",source["action"],compiled))
    def test_couatl_source_turn_end_and_ghast_target_turn_end_are_distinct(self):
        couatl={"name":"Bite","entries":[
            "The target has the {@condition Poisoned|XPHB} condition until the end of the couatl's next turn."]}
        couatl_runtime={"condition_on_hit":"poisoned","condition_duration_rounds":1,
            "condition_duration_tick":"source_turn_end","manual_rider_resolution_required":False}
        self.assertTrue(xmm.source_turn_end_hit_condition_matches("Couatl",couatl,couatl_runtime))
        couatl_runtime["condition_duration_tick"]="target_turn_end"
        self.assertFalse(xmm.source_turn_end_hit_condition_matches("Couatl",couatl,couatl_runtime))

        ghast={"name":"Horrific Necrosis","entries":[
            "The target has the {@condition Frightened|XPHB} condition until the end of its next turn."]}
        ghast_runtime={"condition_on_hit":"frightened","condition_duration_rounds":1,
            "condition_duration_tick":"target_turn_end","manual_rider_resolution_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract(
            "Ghast Gravecaller",ghast,ghast_runtime))
        ghast_runtime["condition_duration_tick"]="source_turn_end"
        self.assertFalse(xmm.source_action_has_typed_condition_contract(
            "Ghast Gravecaller",ghast,ghast_runtime))
    def test_not_503_is_not_success(self):
        with self.assertRaises(ValueError):xmm.check_monsters([],[],[])
if __name__=="__main__":unittest.main()
