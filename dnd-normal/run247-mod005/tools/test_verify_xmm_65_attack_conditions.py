"""No-network tests for XMM attack rider source comparisons."""
import unittest
import verify_xmm_65_attack_conditions as xmm

class XmmAttackConditionRangeTests(unittest.TestCase):
    def setUp(self):
        self.source=[{"name":"Djinni","action":[{"name":"Storm Bolt","entries":[
            "{@atkr r} range 120 feet. {@damage 3d8}."]}]}]
        self.compiled=[{"id":"Djinni","actions":[{"name":"Storm Bolt",
            "attack_modes":["r"],"range_normal_ft":120,"range_long_ft":120}]}]
        self.contracts=[{"id":"Djinni","name":"Storm Bolt","slot":0,"condition":"prone"}]

    def test_ranged_prone_attack_range_matches_source(self):
        self.assertEqual(1,xmm.check_ranged_prone_attack_ranges(
            self.source,self.compiled,self.contracts))

    def test_ranged_prone_attack_range_drift_fails_closed(self):
        self.compiled[0]["actions"][0]["range_normal_ft"]=60
        with self.assertRaisesRegex(ValueError,"XMM_RANGED_PRONE_SOURCE_RANGE_MISMATCH"):
            xmm.check_ranged_prone_attack_ranges(self.source,self.compiled,self.contracts)

    def test_alternate_damage_prone_action_is_manual(self):
        source=[{"name":"Swarm","action":[{"name":"Hands","entries":[
            "{@atkr m} {@hit 4}, reach 5 ft. 20 ({@damage 4d8 + 2}) damage, or 11 "
            "({@damage 2d8 + 2}) damage if Bloodied. If the target is a Medium "
            "or smaller creature, it has the {@condition Prone|XPHB} condition."]}]}]
        row=xmm.expected_rows(source)[0]
        self.assertTrue(row["other"])

if __name__=="__main__":
    unittest.main()
