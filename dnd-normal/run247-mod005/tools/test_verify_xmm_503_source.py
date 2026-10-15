"""Negative-path, no-network tests for the XMM 503 independent comparator."""
import unittest
import json
from io import BytesIO
import verify_xmm_503_source as xmm

class XmmIndependentSourceTests(unittest.TestCase):
    def test_five_werebeast_multiattacks_keep_attack_choices_and_substitution_positions(self):
        source=[{"name":"prefix-"+str(i),"action":[]} for i in range(469)];plans=[]
        for index,(monster,(first,second,substitute,text)) in enumerate(
                xmm.WEREBEAST_MULTIATTACKS.items(),469):
            source.append({"name":monster,"action":[{"name":"Multiattack","entries":[text]},
                {"name":first,"entries":[]},{"name":second,"entries":[]},
                {"name":substitute,"entries":[]}]})
            plans.append({"id":monster,"ptr":f"/monster/{index}/action/0/entries",
                "v":xmm.expected_werebeast_multiattack_variants(first,second,substitute)})
        self.assertEqual(([],5),xmm.check_werebeast_multiattack_variants(source,plans))
        plans[0]["v"][1][0]["o"]=["Rend"]
        errors,count=xmm.check_werebeast_multiattack_variants(source,plans)
        self.assertEqual(0,count)
        self.assertIn("Werebear:WEREBEAST_MULTIATTACK_SOURCE_PLAN_MISMATCH",errors)

    def test_ten_compound_multiattacks_match_source_variants_and_context(self):
        source=[];plans=[]
        for index,(monster,(text,(variants,context))) in enumerate(xmm.COMPOUND_MULTIATTACKS.items()):
            names=sorted({name for group in variants for step in group for name in step["o"]})
            source.append({"name":monster,"action":[{"name":"Multiattack","entries":[text]}]+
                [{"name":name,"entries":[]} for name in names]})
            row={"id":monster,"ptr":f"/monster/{index}/action/0/entries","v":variants}
            if context is not None: row["context"]=json.loads(json.dumps(context))
            plans.append(row)
        self.assertEqual(([],10),xmm.check_compound_multiattack_variants(source,plans))
        clay_index=list(xmm.COMPOUND_MULTIATTACKS).index("Clay Golem")
        plans[clay_index]["context"]["variant_index"]=0
        errors,count=xmm.check_compound_multiattack_variants(source,plans)
        self.assertEqual(0,count)
        self.assertIn("Clay Golem:COMPOUND_MULTIATTACK_SOURCE_PLAN_MISMATCH",errors)

    def test_zero_hp_condition_profiles_require_source_duration_link_and_stability(self):
        source=[];compiled=[];profiles=[]
        for index,((monster,action_name),(ptr,conditions,stable,_phrases)) in enumerate(
                xmm.ZERO_HP_CONDITION_CONTRACTS.items()):
            text=("If this damage reduces the target to 0 HP, the target becomes {@variantrule Stable|XPHB} and has "
                "{@condition Poisoned|XPHB} for 1 hour. While {@condition Poisoned|XPHB}, "
                "the target has {@condition Paralyzed|XPHB}." if monster=="Phase Spider" else
                "If the target is reduced to 0 HP by this attack, it becomes {@variantrule Stable|XPHB} but has "
                "{@condition Poisoned|XPHB} for 1 hour. While it has the {@condition Poisoned|XPHB} "
                "condition, the target has the {@condition Paralyzed|XPHB} condition.")
            source.append({"name":monster,"action":[{"name":action_name,"entries":[text]}]})
            compiled.append({"id":monster,"actions":[{"name":action_name,"source_bound":True,
                "manual_unmodeled_source_effect_required":True,
                "condition_on_target_reduced_to_zero_hp":conditions,
                "target_stable_when_reduced_to_zero_hp":stable}]})
            profiles.append({"id":monster,"action":action_name,"ptr":ptr,"conditions":conditions,
                "duration_minutes":60,"stable":stable,"linked":"paralyzed while poisoned"})
        self.assertEqual(([],2),xmm.check_zero_hp_condition_profiles(source,compiled,profiles))
        profiles[0]["duration_minutes"]=1
        errors,count=xmm.check_zero_hp_condition_profiles(source,compiled,profiles)
        self.assertEqual(0,count);self.assertIn("Phase Spider:Bite:ZERO_HP_CONTRACT_MISMATCH",errors)

    def test_multiattack_catalog_checks_source_pointer_and_simple_sequence(self):
        source=[{"name":"Ape","action":[{"name":"Multiattack","entries":[
            "The ape makes two Fist attacks."]},{"name":"Fist","entries":[]}]}]
        plans=[{"id":"Ape","ptr":"/monster/0/action/0/entries",
            "v":[[{"k":"attack","o":["Fist"],"c":2}]]}]
        self.assertEqual(([],1,1,0),xmm.check_multiattack_plan_catalog(source,plans,1,1,1))
        plans[0]["ptr"]="/monster/4/action/0/entries"
        errors,structured,simple,manual=xmm.check_multiattack_plan_catalog(source,plans,1,1,1)
        self.assertEqual((1,1,0),(structured,simple,manual))
        self.assertIn("Ape:MULTIATTACK_SOURCE_POINTER_MISMATCH",errors)

    def test_sphinx_multiattack_exactly_allows_one_riddle_replacement(self):
        source=[{"name":"Sphinx of Secrets","action":[{"name":"Multiattack","entries":[
            "The sphinx makes three Claw attacks. It can replace one attack with a use of Curses of the Riddle."]}]}]
        expected={"id":"Sphinx of Secrets","ptr":"/monster/407/action/0/entries","v":[
            [{"k":"attack","o":["Claw"],"c":3}],
            [{"k":"attack","o":["Claw"],"c":2},{"k":"action","o":["Curse of the Riddle"],"c":1}]]}
        expected["v"].extend([
            [{"k":"attack","o":["Claw"],"c":1},{"k":"action","o":["Curse of the Riddle"],"c":1},
             {"k":"attack","o":["Claw"],"c":1}],
            [{"k":"action","o":["Curse of the Riddle"],"c":1},{"k":"attack","o":["Claw"],"c":2}],
        ])
        self.assertEqual(([],1),xmm.check_sphinx_of_secrets_multiattack(source,[expected]))
        invalid=json.loads(json.dumps(expected))
        invalid["v"][1][1]["c"]=2
        errors,count=xmm.check_sphinx_of_secrets_multiattack(source,[invalid])
        self.assertEqual(0,count)
        self.assertEqual(["SPHINX_MULTIATTACK_SOURCE_PLAN_MISMATCH"],errors)

    def test_manual_save_condition_catalog_is_source_exact(self):
        source=[{"name":"Test Monster","action":[{"name":"Save Gaze","entries":[
            "{@actSave wis} {@dc 12} {@actSaveFail} The target has the "
            "{@condition Frightened|XPHB} and {@condition Paralyzed|XPHB} conditions."]}]}]
        compiled=[{"id":"Test Monster","actions":[{"name":"Save Gaze","category":"action",
            "source_bound":True,"manual_unmodeled_source_effect_required":True,
            "save_ability":"wis","save_dc":12,
            "condition_on_fail":"frightened, paralyzed"}]}]
        expected=xmm.expected_save_condition_profiles(source,compiled)
        self.assertEqual(1,len(expected))
        self.assertEqual(([],1),xmm.check_save_condition_profiles(source,compiled,expected,1))
        altered=json.loads(json.dumps(expected))
        altered[0]["conditions"]="paralyzed, frightened"
        errors,count=xmm.check_save_condition_profiles(source,compiled,altered,1)
        self.assertEqual(0,count)
        self.assertEqual(["MANUAL_SAVE_CONDITION_PROFILE_SOURCE_MISMATCH"],errors)

    def test_condition_choice_profiles_remain_manual_until_host_integration(self):
        source={"name":"Pixie","action":[{"name":"Faerie Dust","entries":[
            "The target has the {@condition Charmed|XPHB} or {@condition Poisoned|XPHB} condition (pixie's choice) until the start of the pixie's next turn."]}]}
        compiled={"name":"Faerie Dust","condition_choice_on_hit":"charmed, poisoned",
            "condition_duration_rounds":1,"condition_duration_tick":"source_turn_start",
            "manual_rider_resolution_required":True,"manual_unmodeled_source_effect_required":True}
        self.assertTrue(xmm.condition_choice_contract_matches("Pixie",source["action"][0],compiled))
        compiled["manual_unmodeled_source_effect_required"]=False
        self.assertFalse(xmm.condition_choice_contract_matches("Pixie",source["action"][0],compiled))

    def test_slaad_condition_choice_requires_source_ordered_d4_contract(self):
        source={"name":"Death Slaad","action":[{"name":"Chaos Blade","entries":[
            "The target has a condition determined by rolling {@dice 1d4}: on a 1, {@condition Charmed|XPHB}; on a 2, {@condition Frightened|XPHB}; on a 3, {@condition Poisoned|XPHB}; or on a 4, {@condition Incapacitated|XPHB}."]}]}
        compiled={"name":"Chaos Blade","condition_choice_on_hit":"charmed, frightened, poisoned, incapacitated",
            "condition_duration_rounds":1,"condition_duration_tick":"source_turn_start",
            "manual_rider_resolution_required":True,"manual_unmodeled_source_effect_required":True}
        self.assertTrue(xmm.condition_choice_contract_matches("Death Slaad",source["action"][0],compiled))
        compiled["condition_choice_on_hit"]="frightened, charmed, poisoned, incapacitated"
        self.assertFalse(xmm.condition_choice_contract_matches("Death Slaad",source["action"][0],compiled))

    def test_condition_choice_rejects_unpinned_action_family(self):
        self.assertFalse(xmm.condition_choice_contract_matches("Pixie",{"name":"Faerie Dust","entries":[]},{}))

    def test_scarecrow_glare_requires_both_source_conditions_and_turn_expiry(self):
        source={"name":"Scarecrow","action":[{"name":"Terrifying Glare","entries":[
            "{@actSave wis} {@dc 11}, one creature the scarecrow can see within 30 feet. "
            "{@actSaveFail} The target has the {@condition Frightened|XPHB} condition until "
            "the end of the scarecrow's next turn. While {@condition Frightened|XPHB}, the "
            "target has the {@condition Paralyzed|XPHB} condition."]}]}
        action=source["action"][0]
        compiled={"save_ability":"wis","save_dc":11,"target_range_ft":30,
            "condition_on_fail":"frightened, paralyzed","condition_duration_rounds":1,
            "condition_duration_tick":"source_turn_end","manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract("Scarecrow",action,compiled))
        compiled["condition_duration_tick"]="target_turn_end"
        self.assertFalse(xmm.source_action_has_typed_condition_contract("Scarecrow",action,compiled))

    def test_simple_multiattack_requires_exact_source_count_and_choices(self):
        source=[{"name":"Goblin Boss","action":[{"name":"Multiattack","entries":[
            "The goblin makes two attacks, using Scimitar or Shortbow in any combination."]},
            {"name":"Scimitar","entries":["{@atkr m} {@hit 4}, reach 5 ft."]},
            {"name":"Shortbow","entries":["{@atkr r} {@hit 4}, range 80/320 ft."]}]}]
        compiled=[{"id":"Goblin Boss","actions":[{"name":"Multiattack",
            "manual_rider_resolution_required":False},{"name":"Scimitar","to_hit":4},{"name":"Shortbow","to_hit":4}],
            "multiattack":{"source_bound":True,"count":2,"options":"Scimitar|Shortbow"}}]
        self.assertEqual(([],1),xmm.check_simple_multiattacks(source,compiled))
        compiled[0]["multiattack"]["count"]=3
        errors,count=xmm.check_simple_multiattacks(source,compiled)
        self.assertEqual(1,count)
        self.assertEqual(["Goblin Boss:Multiattack:SOURCE_SEQUENCE_MISMATCH"],errors)
        extra={"name":"Abominable Yeti","action":[{"name":"Multiattack","entries":[
            "The yeti can use its Chilling Gaze and makes two attacks, using Claw or Ice Throw in any combination."]}]}
        self.assertIsNone(xmm.simple_multiattack_plan(extra["action"][0]))
        and_list={"name":"Bandit Captain","entries":["The bandit makes two attacks, using Scimitar and Pistol in any combination."]}
        self.assertEqual((2,"Scimitar|Pistol"),xmm.simple_multiattack_plan(and_list))

    def test_simple_repeated_multiattack_is_bound_to_exact_component_and_count(self):
        source=[{"name":"Ape","action":[{"name":"Multiattack","entries":[
            "The ape makes two Fist attacks."]},{"name":"Fist","entries":["{@atkr m} {@hit 5}, reach 5 ft."]}]}]
        compiled=[{"id":"Ape","actions":[{"name":"Multiattack",
            "manual_rider_resolution_required":False},{"name":"Fist","to_hit":5,
            "manual_rider_resolution_required":False}],
            "multiattack":{"source_bound":True,"count":2,"sequence":["Fist","Fist"]}}]
        self.assertEqual(([],1),xmm.check_simple_multiattacks(source,compiled))
        compiled[0]["multiattack"]["sequence"]=["Fist","Claw"]
        errors,count=xmm.check_simple_multiattacks(source,compiled)
        self.assertEqual(1,count)
        self.assertEqual(["Ape:Multiattack:SOURCE_SEQUENCE_MISMATCH"],errors)

    def test_fixed_mixed_multiattack_preserves_source_order_and_repetition(self):
        monster={"name":"Brown Bear","action":[
            {"name":"Multiattack","entries":["The bear makes one Bite attack and one Claw attack."]},
            {"name":"Bite","entries":["{@atkr m} {@hit 5}"]},
            {"name":"Claw","entries":["{@atkr m} {@hit 5}"]}]}
        self.assertEqual(["Bite","Claw"],xmm.simple_ordered_multiattack_plan(monster,monster["action"][0]))
        compiled=[{"id":"Brown Bear","actions":[{"name":"Multiattack","manual_rider_resolution_required":False},
            {"name":"Bite","to_hit":5,"manual_rider_resolution_required":False},
            {"name":"Claw","to_hit":5,"manual_rider_resolution_required":False}],
            "multiattack":{"source_bound":True,"count":2,"sequence":["Bite","Claw"]}}]
        self.assertEqual(([],1),xmm.check_simple_multiattacks([monster],compiled))
        compiled[0]["multiattack"]["sequence"]=["Claw","Bite"]
        errors,count=xmm.check_simple_multiattacks([monster],compiled)
        self.assertEqual(1,count)
        self.assertEqual(["Brown Bear:Multiattack:SOURCE_SEQUENCE_MISMATCH"],errors)

    def test_fixed_three_attack_multiattack_preserves_full_source_order(self):
        monster={"name":"Grick Ancient","action":[
            {"name":"Multiattack","entries":["The grick makes one Beak attack, one Slam attack, and one Tentacles attack."]},
            {"name":"Beak","entries":["{@atkr m} {@hit 5}"]},
            {"name":"Slam","entries":["{@atkr m} {@hit 5}"]},
            {"name":"Tentacles","entries":["{@atkr m} {@hit 5}"]}]}
        self.assertEqual(["Beak","Slam","Tentacles"],
            xmm.simple_ordered_multiattack_plan(monster,monster["action"][0]))
        compiled=[{"id":"Grick Ancient","actions":[{"name":"Multiattack","manual_rider_resolution_required":False},
            {"name":"Beak","to_hit":5,"manual_rider_resolution_required":False},
            {"name":"Slam","to_hit":5,"manual_rider_resolution_required":False},
            {"name":"Tentacles","to_hit":5,"manual_rider_resolution_required":False}],
            "multiattack":{"source_bound":True,"count":3,"sequence":["Beak","Slam","Tentacles"]}}]
        self.assertEqual(([],1),xmm.check_simple_multiattacks([monster],compiled))
        compiled[0]["multiattack"]["sequence"]=["Beak","Tentacles","Slam"]
        errors,count=xmm.check_simple_multiattacks([monster],compiled)
        self.assertEqual(1,count)
        self.assertEqual(["Grick Ancient:Multiattack:SOURCE_SEQUENCE_MISMATCH"],errors)

    def test_multiattack_with_leading_extra_action_is_not_reduced_to_attack_only(self):
        monster={"name":"Carrion Crawler","action":[
            {"name":"Multiattack","entries":["The carrion crawler uses Paralyzing Tentacles and makes one Bite attack."]},
            {"name":"Multiattack"},{"name":"Paralyzing Tentacles"},{"name":"Bite"}]}
        self.assertIsNone(xmm.simple_ordered_multiattack_plan(monster,monster["action"][0]))

    def test_simple_syntax_with_manual_component_is_not_claimed_as_executable(self):
        source=[{"name":"Fixture","action":[
            {"name":"Multiattack","entries":["The creature makes one Bite attack and one Gaze attack."]},
            {"name":"Bite","entries":["{@atkr m} {@hit 4}, reach 5 ft. {@h}1 damage."]},
            {"name":"Gaze","entries":["{@actSave wis} {@dc 12}. {@actSaveFail} Blinded."]}]}]
        compiled=[{"id":"Fixture","actions":[
            {"name":"Multiattack","manual_rider_resolution_required":False},
            {"name":"Bite","to_hit":4,"manual_rider_resolution_required":False},
            {"name":"Gaze","manual_rider_resolution_required":True}],
            "multiattack":{"source_bound":True,"count":2,"sequence":["Bite","Gaze"]}}]
        self.assertEqual(([],0),xmm.check_simple_multiattacks(source,compiled))

    def test_advantage_bonus_damage_is_typed_for_all_six_source_attacks(self):
        rows=[
            ("Goblin Boss","Scimitar","1d6 + 2",5,2,"1d4","slashing",4,"m",5,0,0),
            ("Goblin Boss","Shortbow","1d6 + 2",5,2,"1d4","piercing",4,"r",0,80,320),
            ("Goblin Warrior","Scimitar","1d6 + 2",5,2,"1d4","slashing",4,"m",5,0,0),
            ("Goblin Warrior","Shortbow","1d6 + 2",5,2,"1d4","piercing",4,"r",0,80,320),
            ("Scout Captain","Shortsword","1d6 + 3",6,10,"3d6","piercing",5,"m",5,0,0),
            ("Scout Captain","Longbow","1d8 + 3",7,10,"3d6","piercing",5,"r",0,150,600),
        ]
        source_by_id={};compiled_by_id={}
        for monster,name,base,base_average,bonus_average,bonus,kind,to_hit,mode,reach,normal,long_range in rows:
            profile=("reach "+str(reach)+" ft." if reach else
                "range "+str(normal)+"/"+str(long_range)+" ft.")
            source_by_id.setdefault(monster,{"name":monster,"action":[]})["action"].append({"name":name,"entries":[
                "{@atkr "+mode+"} {@hit "+str(to_hit)+"}, "+profile+" {@h}"+
                str(base_average)+" ({@damage "+base+"}) "+kind+
                " damage, plus "+str(bonus_average)+" ({@damage "+bonus+"}) "+kind+
                " damage if the attack roll had {@variantrule Advantage|XPHB}."]})
            compiled_action={"name":name,"damage":base,"to_hit":to_hit,"attack_modes":[mode],
                "damage_type":kind,"extra_damage_if_attack_advantage":bonus,
                "extra_damage_if_attack_advantage_type":kind,
                "manual_rider_resolution_required":False,
                "manual_unmodeled_source_effect_required":False}
            if reach:compiled_action["reach_ft"]=reach
            else:compiled_action.update({"range_normal_ft":normal,"range_long_ft":long_range})
            compiled_by_id.setdefault(monster,{"id":monster,"actions":[]})["actions"].append(compiled_action)
        source_by_id["Chimera"]={"name":"Chimera","action":[{"name":"Bite","entries":[
            "{@atkr m} {@hit 7}, reach 5 ft. {@h}11 ({@damage 2d6 + 4}) Piercing damage, or 18 ({@damage 4d6 + 4}) Piercing damage if the chimera had {@variantrule Advantage|XPHB} on the attack roll."]}]}
        compiled_by_id["Chimera"]={"id":"Chimera","actions":[{"name":"Bite","to_hit":7,
            "attack_modes":["m"],"reach_ft":5,"damage":"2d6 + 4","damage_type":"piercing",
            "extra_damage_if_attack_advantage":"2d6","extra_damage_if_attack_advantage_type":"piercing",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]}
        source=list(source_by_id.values());compiled=list(compiled_by_id.values())
        self.assertEqual(([],7),xmm.check_advantage_bonus_attack_damage(source,compiled))
        compiled[0]["actions"][0]["extra_damage_if_attack_advantage"]="2d4"
        errors,count=xmm.check_advantage_bonus_attack_damage(source,compiled)
        self.assertEqual(6,count)
        self.assertIn("ADVANTAGE_BONUS_DAMAGE_MISMATCH",errors[0])

    def test_wounded_target_advantage_is_bound_to_four_shark_and_piranha_attacks(self):
        cases=[
            ("Giant Shark","Bite",9,"3d10 + 6",22),
            ("Hunter Shark","Bite",6,"3d6 + 4",14),
            ("Piranha","Bite",5,"1",1),
            ("Swarm of Piranhas","Bites",5,"2d4 + 3",8),
        ]
        source_by_id={};compiled_by_id={}
        for monster,name,to_hit,damage,average in cases:
            source_by_id.setdefault(monster,{"name":monster,"action":[]})["action"].append({
                "name":name,"entries":["{@atkr m} {@hit "+str(to_hit)+
                "} (with {@variantrule Advantage|XPHB} if the target doesn't have all its "+
                "{@variantrule Hit Points|XPHB}), reach 5 ft. {@h}"+str(average)+
                (" ({@damage "+damage+"})" if damage!="1" else "")+
                " Piercing damage"+(", or 5 ({@damage 1d4 + 3}) Piercing damage if the swarm is {@status Bloodied|XPHB}" if monster=="Swarm of Piranhas" else "")+"."]})
            runtime={"name":name,"to_hit":to_hit,"attack_modes":["m"],"reach_ft":5,
                "damage":damage,"damage_type":"piercing",
                "attack_advantage_if_target_missing_hit_points":True,
                "manual_rider_resolution_required":False,
                "manual_unmodeled_source_effect_required":False}
            if monster=="Swarm of Piranhas":runtime["damage_bloodied"]="1d4 + 3"
            compiled_by_id.setdefault(monster,{"id":monster,"actions":[]})["actions"].append(runtime)
        source=list(source_by_id.values());compiled=list(compiled_by_id.values())
        self.assertEqual(([],4),xmm.check_wounded_target_advantage_attacks(source,compiled))
        swarm_source=[source_by_id["Swarm of Piranhas"]]
        swarm_compiled=[compiled_by_id["Swarm of Piranhas"]]
        self.assertEqual(([],{"source_bloodied_attack_branches":1,
            "source_alternate_attack_branches_manual":0}),
            xmm.check_conditional_attack_damage(swarm_source,swarm_compiled,1,0))
        compiled[-1]["actions"][0]["attack_advantage_if_target_missing_hit_points"]=False
        errors,count=xmm.check_wounded_target_advantage_attacks(source,compiled)
        self.assertEqual(3,count)
        self.assertIn("WOUNDED_TARGET_ADVANTAGE_MISMATCH",errors[0])

    def test_grappled_target_advantage_is_bound_to_bugbear_warrior_light_hammer(self):
        source=[{"name":"Bugbear Warrior","action":[{"name":"Light Hammer","entries":[
            "{@atkr m,r} {@hit 4} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the bugbear), reach 10 ft. or range 20/60 ft. {@h}9 ({@damage 3d4 + 2}) Bludgeoning damage."]}]}]
        compiled=[{"id":"Bugbear Warrior","actions":[{"name":"Light Hammer","to_hit":4,
            "attack_modes":["m","r"],"reach_ft":10,"range_normal_ft":20,"range_long_ft":60,
            "damage":"3d4 + 2","damage_type":"bludgeoning",
            "attack_advantage_condition":"target_grappled_by_attacker",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]}]
        source.append({"name":"Ankheg","action":[{"name":"Bite","entries":[
            "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the ankheg), reach 5 ft. {@h}10 ({@damage 2d6 + 3}) Slashing damage plus 3 ({@damage 1d6}) Acid damage. If the target is a Large or smaller creature, it has the {@condition Grappled|XPHB} condition (escape {@dc 13})."]}]})
        compiled.append({"id":"Ankheg","actions":[{"name":"Bite","to_hit":5,
            "attack_modes":["m"],"reach_ft":5,"damage":"2d6 + 3 + 1d6",
            "damage_type":"slashing + acid","escape_dc":13,
            "condition_on_hit":"grappled","condition_max_target_size":"large",
            "attack_advantage_condition":"target_grappled_by_attacker",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}]})
        source.append({"name":"Bugbear Stalker","action":[{"name":"Morningstar","entries":[
            "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the bugbear), reach 10 ft. {@h}12 ({@damage 2d8 + 3}) Piercing damage."]}]})
        compiled.append({"id":"Bugbear Stalker","actions":[{"name":"Morningstar","to_hit":5,
            "attack_modes":["m"],"reach_ft":10,"damage":"2d8 + 3","damage_type":"piercing",
            "attack_advantage_condition":"target_grappled_by_attacker",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]})
        source.append({"name":"Mimic","action":[{"name":"Bite","entries":[
            "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the mimic), reach 5 ft. {@h}7 ({@damage 1d8 + 3}) Piercing damage—or 12 ({@damage 2d8 + 3}) Piercing damage if the target is {@condition Grappled|XPHB} by the mimic—plus 4 ({@damage 1d8}) Acid damage."]}]})
        compiled.append({"id":"Mimic","actions":[{"name":"Bite","to_hit":5,
            "attack_modes":["m"],"reach_ft":5,"damage":"1d8 + 3 + 1d8",
            "damage_type":"piercing + acid","attack_advantage_condition":"target_grappled_by_attacker",
            "extra_damage_if_target_grappled_by_attacker":"1d8","extra_damage_if_target_grappled_by_attacker_type":"piercing",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]})
        self.assertEqual(([],4),xmm.check_grappled_target_advantage_attacks(source,compiled))
        compiled[0]["actions"][0]["attack_advantage_condition"]="target_bloodied"
        errors,count=xmm.check_grappled_target_advantage_attacks(source,compiled)
        self.assertEqual(3,count)
        self.assertEqual(["Bugbear Warrior:Light Hammer:GRAPPLED_TARGET_ADVANTAGE_MISMATCH",
            "GRAPPLED_TARGET_ADVANTAGE_COUNT:3"],errors)

    def test_ankheg_bite_advantage_contract_includes_its_grapple_and_typed_damage(self):
        source=[{"name":"Ankheg","action":[{"name":"Bite","entries":[
            "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the ankheg), reach 5 ft. {@h}10 ({@damage 2d6 + 3}) Slashing damage plus 3 ({@damage 1d6}) Acid damage. If the target is a Large or smaller creature, it has the {@condition Grappled|XPHB} condition (escape {@dc 13})."]}]}]
        compiled=[{"id":"Ankheg","actions":[{"name":"Bite","to_hit":5,
            "attack_modes":["m"],"reach_ft":5,"damage":"2d6 + 3 + 1d6",
            "damage_type":"slashing + acid","escape_dc":13,
            "condition_on_hit":"grappled","condition_max_target_size":"large",
            "attack_advantage_condition":"target_grappled_by_attacker",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}]}]
        source.append({"name":"Bugbear Warrior","action":[{"name":"Light Hammer","entries":[
            "{@atkr m,r} {@hit 4} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the bugbear), reach 10 ft. or range 20/60 ft. {@h}9 ({@damage 3d4 + 2}) Bludgeoning damage."]}]})
        compiled.append({"id":"Bugbear Warrior","actions":[{"name":"Light Hammer","to_hit":4,
            "attack_modes":["m","r"],"reach_ft":10,"range_normal_ft":20,"range_long_ft":60,
            "damage":"3d4 + 2","damage_type":"bludgeoning",
            "attack_advantage_condition":"target_grappled_by_attacker",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]})
        source.append({"name":"Bugbear Stalker","action":[{"name":"Morningstar","entries":[
            "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the bugbear), reach 10 ft. {@h}12 ({@damage 2d8 + 3}) Piercing damage."]}]})
        compiled.append({"id":"Bugbear Stalker","actions":[{"name":"Morningstar","to_hit":5,
            "attack_modes":["m"],"reach_ft":10,"damage":"2d8 + 3","damage_type":"piercing",
            "attack_advantage_condition":"target_grappled_by_attacker",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]})
        source.append({"name":"Mimic","action":[{"name":"Bite","entries":[
            "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the mimic), reach 5 ft. {@h}7 ({@damage 1d8 + 3}) Piercing damage—or 12 ({@damage 2d8 + 3}) Piercing damage if the target is {@condition Grappled|XPHB} by the mimic—plus 4 ({@damage 1d8}) Acid damage."]}]})
        compiled.append({"id":"Mimic","actions":[{"name":"Bite","to_hit":5,
            "attack_modes":["m"],"reach_ft":5,"damage":"1d8 + 3 + 1d8",
            "damage_type":"piercing + acid","attack_advantage_condition":"target_grappled_by_attacker",
            "extra_damage_if_target_grappled_by_attacker":"1d8","extra_damage_if_target_grappled_by_attacker_type":"piercing",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]})
        self.assertEqual(([],4),xmm.check_grappled_target_advantage_attacks(source,compiled))
        compiled[0]["actions"][0]["escape_dc"]=12
        errors,count=xmm.check_grappled_target_advantage_attacks(source,compiled)
        self.assertEqual(3,count)
        self.assertIn("Ankheg:Bite:GRAPPLED_TARGET_ADVANTAGE_MISMATCH",errors)
        self.assertIn("GRAPPLED_TARGET_ADVANTAGE_COUNT:3",errors)

    def test_chain_devil_infernal_chain_requires_every_source_bound_rider(self):
        source={"name":"Conjure Infernal Chain","entries":[
            "The devil conjures a fiery chain to bind a creature. {@actSave dex} {@dc 15}, one creature the devil can see within 60 feet. {@actSaveFail} 9 ({@damage 2d4 + 4}) Fire damage, and the target has the {@condition Restrained|XPHB} condition until the end of the devil's next turn, at which point the chain disappears. If the target is Large or smaller, the devil moves the target up to 30 feet straight toward itself. {@actSaveSuccess} The chain disappears."]}
        compiled={"save_ability":"dex","save_dc":15,"damage":"2d4 + 4",
            "damage_type":"fire","target_range_ft":60,"condition_on_fail":"restrained",
            "condition_duration_rounds":1,"condition_duration_tick":"source_turn_end",
            "effect_on_fail":"pull:30ft;restrained",
            "effect_on_fail_movement_max_target_size":"Large",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.chain_devil_infernal_chain_matches(source,compiled))
        for field,value in (("target_range_ft",30),("effect_on_fail","pull:30ft"),
                            ("effect_on_fail_movement_max_target_size","Huge"),
                            ("condition_duration_tick","source_turn_start")):
            with self.subTest(field=field):
                wrong=dict(compiled);wrong[field]=value
                self.assertFalse(xmm.chain_devil_infernal_chain_matches(source,wrong))

    def test_colossus_slam_push_is_typed_only_for_exact_source_rider(self):
        action={"name":"Slam","entries":[
            "{@atkr m} {@hit 18}, reach 20 ft. {@h}32 ({@damage 4d10 + 10}) Bludgeoning damage, and the colossus pushes the target up to 20 feet straight away from itself."]}
        compiled={"to_hit":18,"reach_ft":20,"damage":"4d10 + 10",
            "damage_type":"bludgeoning","effect_on_hit":"push:20ft",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract(
            "Colossus",action,compiled))
        compiled["effect_on_hit"]="pull:20ft"
        self.assertFalse(xmm.source_action_has_typed_condition_contract(
            "Colossus",action,compiled))

    def test_size_limited_forced_movement_is_source_bound(self):
        cases=[
            ("Merrow","Harpoon",["{@atkr m,r} {@hit 6}, reach 5 ft. or range 20/60 ft. {@h}11 ({@damage 2d6 + 4}) Piercing damage. If the target is a Large or smaller creature, the merrow pulls the target up to 15 feet straight toward itself."],
             {"effect_on_hit":"pull:15ft","effect_on_hit_max_target_size":"Large","damage":"2d6 + 4","damage_type":"piercing"}),
            ("Satyr","Hooves",["{@atkr m} {@hit 5}, reach 5 ft. {@h}5 ({@damage 1d4 + 3}) Bludgeoning damage. If the target is a Medium or smaller creature, the satyr pushes the target up to 10 feet straight away from itself."],
             {"effect_on_hit":"push:10ft","effect_on_hit_max_target_size":"Medium","damage":"1d4 + 3","damage_type":"bludgeoning"}),
            ("Shambling Mound","Charged Tendril",["{@atkr m} {@hit 7}, reach 10 ft. {@h}7 ({@damage 1d6 + 4}) Bludgeoning damage plus 5 ({@damage 2d4}) Lightning damage. If the target is a Medium or smaller creature, the shambling mound pulls the target 5 feet straight toward itself."],
             {"effect_on_hit":"pull:5ft","effect_on_hit_max_target_size":"Medium","damage":"1d6 + 4 + 2d4","damage_type":"bludgeoning + lightning","reach_ft":10}),
        ]
        for monster,name,entries,compiled in cases:
            with self.subTest(monster=monster):
                action={"name":name,"entries":entries}
                compiled.update({"manual_rider_resolution_required":False,
                    "manual_unmodeled_source_effect_required":False})
                self.assertTrue(xmm.source_action_has_typed_condition_contract(
                    monster,action,compiled))
                compiled["effect_on_hit_max_target_size"]="Tiny"
                self.assertFalse(xmm.source_action_has_typed_condition_contract(
                    monster,action,compiled))

    def test_hook_horror_hook_allows_source_choice_of_direction(self):
        action={"name":"Hook","entries":[
            "{@atkr m} {@hit 6}, reach 10 ft. {@h}11 ({@damage 2d6 + 4}) Piercing damage. If the target is a Large or smaller creature, the hook horror moves the target 5 feet straight toward or away from itself."]}
        compiled={"damage":"2d6 + 4","damage_type":"piercing",
            "effect_on_hit":"either:5ft","effect_on_hit_max_target_size":"Large",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract(
            "Hook Horror",action,compiled))
        compiled["effect_on_hit"]="push:5ft"
        self.assertFalse(xmm.source_action_has_typed_condition_contract(
            "Hook Horror",action,compiled))

    def test_lacedon_icy_bite_speed_reduction_is_source_and_tick_bound(self):
        action={"name":"Icy Bite","entries":[
            "{@atkr m} {@hit 4}, reach 5 ft. {@h}9 ({@damage 2d6 + 2}) Cold damage, and the target's {@variantrule Speed|XPHB} decreases by 5 feet until the start of the ghoul's next turn."]}
        compiled={"to_hit":4,"reach_ft":5,"damage":"2d6 + 2",
            "damage_type":"cold","speed_reduction_ft":5,
            "speed_reduction_duration_tick":"source_turn_start",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract(
            "Lacedon Ghoul",action,compiled))
        compiled["speed_reduction_duration_tick"]="target_turn_end"
        self.assertFalse(xmm.source_action_has_typed_condition_contract(
            "Lacedon Ghoul",action,compiled))

    def test_balor_flame_whip_compounds_size_limited_pull_and_prone(self):
        action={"name":"Flame Whip","entries":[
            "{@atkr m} {@hit 14}, reach 30 ft. {@h}18 ({@damage 3d6 + 8}) Force damage plus 17 ({@damage 5d6}) Fire damage. If the target is a Huge or smaller creature, the balor pulls the target up to 25 feet straight toward itself, and the target has the {@condition Prone|XPHB} condition."]}
        compiled={"effect_on_hit":"pull:25ft;prone","effect_on_hit_max_target_size":"Huge",
            "condition_on_hit":"prone","condition_ends_when":"stands_up",
            "condition_max_target_size":"Huge","damage":"3d6 + 8 + 5d6",
            "damage_type":"force + fire","manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract("Balor",action,compiled))
        compiled["effect_on_hit_max_target_size"]="Gargantuan"
        self.assertFalse(xmm.source_action_has_typed_condition_contract("Balor",action,compiled))

    def test_wind_javelin_return_is_typed_only_for_the_ranged_mode(self):
        action={"name":"Wind Javelin","entries":[
            "{@atkr m,r} {@hit 4}, reach 5 ft. or range 30/120 ft. {@h}5 ({@damage 1d6 + 2}) Piercing damage plus 2 ({@damage 1d4}) Thunder damage. The javelin magically returns to the aarakocra's hand immediately after a ranged attack."]}
        compiled={"damage":"1d6 + 2 + 1d4","damage_type":"piercing + thunder",
            "returns_to_attacker_after_ranged_attack":True,
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract(
            "Aarakocra Skirmisher",action,compiled))
        compiled["returns_to_attacker_after_ranged_attack"]=False
        self.assertFalse(xmm.source_action_has_typed_condition_contract(
            "Aarakocra Skirmisher",action,compiled))

    def test_source_bound_returning_weapons_return_only_after_ranged_attacks(self):
        cases=[
            ("Kenku","Shadow Blade","the kenku's hand"),
            ("Mezzoloth","Mercurial Trident","the mezzoloth's claw"),
            ("Modron Duodrone","Clockwork Blade","the modron's hand"),
            ("Modron Tridrone","Clockwork Spear","the modron's hand"),
            ("Nycaloth","Mercurial Axe","the nycaloth's hand"),
            ("Salamander","Flame Spear","the salamander's hand"),
            ("Salamander Inferno Master","Flame Trident","the salamander's hand"),
        ]
        for monster,name,owner in cases:
            with self.subTest(monster=monster):
                action={"name":name,"entries":[
                    "{@atkr m,r} {@hit 5}. The weapon magically returns to "+owner+
                    " immediately after a ranged attack."]}
                compiled={"returns_to_attacker_after_ranged_attack":True,
                    "manual_rider_resolution_required":False,
                    "manual_unmodeled_source_effect_required":False}
                self.assertTrue(xmm.source_action_has_typed_condition_contract(
                    monster,action,compiled))
                compiled["returns_to_attacker_after_ranged_attack"]=False
                self.assertFalse(xmm.source_action_has_typed_condition_contract(
                    monster,action,compiled))

    def test_ongoing_attack_damage_is_an_independent_manual_effect(self):
        source=[
            {"name":"Half-Dragon","action":[{"name":"Claw","entries":[
                "Hit: slashing damage plus damage of the type chosen for the Draconic Origin trait."]}]},
            {"name":"Vine Blight","action":[{"name":"Constricting Vine","entries":[
                "Hit: Bludgeoning damage. While grappled, target takes Bludgeoning damage at the start of each of its turns."]}]},
        ]
        compiled=[
            {"id":"Half-Dragon","actions":[{"name":"Claw","manual_unmodeled_source_effect_required":True}]},
            {"id":"Vine Blight","actions":[{"name":"Constricting Vine","manual_unmodeled_source_effect_required":True}]},
        ]
        self.assertEqual(([],2),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[1]["actions"][0]["manual_unmodeled_source_effect_required"]=False
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(2,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
    def test_primary_hit_damage_is_compared_against_source_not_compiled_value(self):
        source=[{"name":"Test Monster","action":[{"name":"Claw","entries":[
            "{@atkr m} {@hit 5}, reach 5 ft. {@h}6 ({@damage 1d6 + 3}) Slashing damage."]}]}]
        compiled=[{"id":"Test Monster","actions":[{"name":"Claw","damage":"1d6 + 3",
            "damage_type":"slashing"}]}]
        self.assertEqual(([],1),xmm.check_attack_primary_damage(source,compiled,1))
        compiled[0]["actions"][0]["damage"]="2d6 + 3"
        errors,count=xmm.check_attack_primary_damage(source,compiled,1)
        self.assertEqual(1,count)
        self.assertIn("PRIMARY_HIT_DAMAGE_MISMATCH",errors[0])
        compiled[0]["actions"][0]["damage"]="1d6 + 3"
        compiled[0]["actions"][0]["damage_type"]="piercing"
        errors,count=xmm.check_attack_primary_damage(source,compiled,1)
        self.assertEqual(1,count)
        self.assertIn("PRIMARY_HIT_DAMAGE_MISMATCH",errors[0])
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
    def test_goat_ram_movement_alternative_has_a_typed_executor(self):
        source=[{"name":"Goat","action":[{"name":"Ram","entries":[
          "{@atkr m} {@hit 2}, reach 5 ft. {@h}1 Bludgeoning damage, or 2 "
          "({@damage 1d4}) Bludgeoning damage if the goat moved 20+ feet straight "
          "toward the target immediately before the hit."]}]}]
        compiled=[{"id":"Goat","actions":[{"name":"Ram","damage":"1",
          "damage_type":"bludgeoning","manual_rider_resolution_required":False,
          "damage_alternative_expression":"1d4",
          "damage_alternative_condition":"moved_20ft_straight_toward_target_immediately_before_hit"}]}]
        errors,counts=xmm.check_conditional_attack_damage(source,compiled,0,0)
        self.assertEqual([],errors);self.assertEqual(0,counts["source_alternate_attack_branches_manual"])
        compiled[0]["actions"][0]["damage_alternative_condition"]="moved_10ft_straight_toward_target_immediately_before_hit"
        errors,_=xmm.check_conditional_attack_damage(source,compiled,0,0)
        self.assertIn("Goat:Ram:MOVEMENT_ALTERNATE_DAMAGE_NOT_EXECUTABLE",errors)
    def test_mimic_grappled_bite_prerequisite_is_not_a_condition_rider(self):
        source=[{"name":"Mimic","action":[{"name":"Bite","entries":[
          "{@atkr m} {@hit 5} (with {@variantrule Advantage|XPHB} if the target is {@condition Grappled|XPHB} by the mimic), reach 5 ft. {@h}7 ({@damage 1d8 + 3}) Piercing damage—or 12 "
          "({@damage 2d8 + 3}) Piercing damage if the target is {@condition Grappled|XPHB} by the mimic—plus 4 "
          "({@damage 1d8}) Acid damage."]}]}]
        compiled=[{"id":"Mimic","actions":[{"name":"Bite","damage":"1d8 + 3 + 1d8",
          "damage_type":"piercing + acid","to_hit":5,"attack_modes":["m"],"reach_ft":5,
          "attack_advantage_condition":"target_grappled_by_attacker",
          "extra_damage_if_target_grappled_by_attacker":"1d8","extra_damage_if_target_grappled_by_attacker_type":"piercing",
          "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]}]
        self.assertEqual(([],{"source_bloodied_attack_branches":0,
            "source_alternate_attack_branches_manual":0}),
            xmm.check_conditional_attack_damage(source,compiled,0,0))
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
    def test_beard_poison_source_binds_healing_suppression_until_source_turn(self):
        source=[{"name":"Bearded Devil","action":[{"name":"Beard","entries":[
            "The target has the {@condition Poisoned|XPHB} condition until the start of the devil's next turn. Until this poison ends, the target can't regain {@variantrule Hit Points|XPHB}."]}]}]
        compiled=[{"id":"Bearded Devil","actions":[{"name":"Beard",
            "condition_on_hit":"poisoned","condition_duration_rounds":1,
            "condition_duration_tick":"source_turn_start",
            "prevents_hit_point_regain_while_condition":"poisoned",
            "manual_rider_resolution_required":False,
            "manual_unmodeled_source_effect_required":False}]}]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[0]["actions"][0]["condition_duration_tick"]="target_turn_start"
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
    def test_two_other_source_poison_riders_also_suppress_healing(self):
        source=[
            {"name":"Bone Devil","action":[{"name":"Infernal Sting","entries":[
                "The target has the {@condition Poisoned|XPHB} condition until the start of the devil's next turn. While Poisoned, the target can't regain Hit Points."]}]},
            {"name":"Dire Worg","action":[{"name":"Bite","entries":[
                "The target has the {@condition Poisoned|XPHB} condition until the start of the worg's next turn. While Poisoned, the target can't regain Hit Points."]}]},
        ]
        compiled=[
            {"id":"Bone Devil","actions":[{"name":"Infernal Sting","condition_on_hit":"poisoned",
                "condition_duration_rounds":1,"condition_duration_tick":"source_turn_start",
                "prevents_hit_point_regain_while_condition":"poisoned",
                "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]},
            {"id":"Dire Worg","actions":[{"name":"Bite","condition_on_hit":"poisoned",
                "condition_duration_rounds":1,"condition_duration_tick":"source_turn_start",
                "prevents_hit_point_regain_while_condition":"poisoned",
                "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]},
        ]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[1]["actions"][0]["prevents_hit_point_regain_while_condition"]=""
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
    def test_dread_scythe_healing_suppression_uses_target_turn_end(self):
        source=[{"name":"Death Cultist","action":[{"name":"Dread Scythe","entries":[
            "Hit: damage, and the target can't regain Hit Points until the end of its next turn."]}]}]
        compiled=[{"id":"Death Cultist","actions":[{"name":"Dread Scythe",
            "healing_suppression_duration_rounds":1,"healing_suppression_tick":"target_turn_end",
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}]}]
        self.assertEqual(([],0),xmm.check_unmodeled_action_effects(source,compiled))
        compiled[0]["actions"][0]["healing_suppression_tick"]="source_turn_start"
        errors,count=xmm.check_unmodeled_action_effects(source,compiled)
        self.assertEqual(1,count)
        self.assertIn("UNTYPED_SOURCE_EFFECT_NOT_MANUAL",errors[0])
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
    def test_scout_captain_aim_requires_turn_limited_next_attack_advantage(self):
        source={"name":"Aim","entries":["The scout has {@variantrule Advantage|XPHB} on the next attack roll it makes during the current turn."]}
        compiled={"category":"bonus_action","grants_advantage_next_attack_this_turn":True,
            "manual_rider_resolution_required":False,"manual_unmodeled_source_effect_required":False}
        self.assertTrue(xmm.source_action_has_typed_condition_contract("Scout Captain",source,compiled))
        compiled["grants_advantage_next_attack_this_turn"]=False
        self.assertFalse(xmm.source_action_has_typed_condition_contract("Scout Captain",source,compiled))

    def test_not_503_is_not_success(self):
        with self.assertRaises(ValueError):xmm.check_monsters([],[],[])
if __name__=="__main__":unittest.main()
