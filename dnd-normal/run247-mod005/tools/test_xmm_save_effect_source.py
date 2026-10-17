"""Validate execution eligibility against the complete, SHA-pinned independent source."""
import copy
import json
import os
from pathlib import Path
import unittest
import verify_xmm_503_source as xmm


@unittest.skipUnless(os.environ.get("XMM_SOURCE_DIR"), "Set XMM_SOURCE_DIR to the pinned independent source files")
class SaveEffectSourceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        raw=(Path(os.environ["XMM_SOURCE_DIR"])/"bestiary-xmm.json").read_bytes()
        if xmm.blob_sha(raw)!=xmm.BESTIARY_SHA:
            raise ValueError("XMM_BESTIARY_GIT_BLOB_MISMATCH")
        cls.source={m["name"]:m for m in json.loads(raw)["monster"]}
        cls.compiled={m["id"]:m for m in xmm.java_rows((xmm.ROOT/xmm.JAVA_RUNTIME).read_text())}

    def rows(self,block):
        source=next(a for a in self.source[block]["action"] if a["name"]=="Slowing Breath")
        compiled=next(a for a in self.compiled[block]["actions"] if a["name"]=="Slowing Breath")
        return copy.deepcopy(source),copy.deepcopy(compiled)

    def test_four_copper_variants_match_full_source(self):
        for block in ("Adult Copper Dragon","Ancient Copper Dragon","Copper Dragon Wyrmling","Young Copper Dragon"):
            source,compiled=self.rows(block)
            self.assertTrue(xmm.source_action_has_typed_condition_contract(block,source,compiled),block)

    def test_rule_mutations_fail_closed(self):
        for field,value in {"save_dc":19,"save_ability":"str","target_range_ft":30,
                "save_effect_duration":"one_minute","target_area_shape":"line",
                "condition_on_fail":"slowed","recharge":"5-6","damage":"1d6"}.items():
            source,compiled=self.rows("Adult Copper Dragon");compiled[field]=value
            self.assertFalse(xmm.source_action_has_typed_condition_contract("Adult Copper Dragon",source,compiled),field)

    def test_changed_source_consequence_remains_manual(self):
        source,compiled=self.rows("Adult Copper Dragon")
        source["entries"][0]=source["entries"][0].replace("halved","reduced by 10 feet")
        self.assertFalse(xmm.source_action_has_typed_condition_contract("Adult Copper Dragon",source,compiled))

    def test_unsupported_three_families_are_not_partially_enabled(self):
        count=0
        for block in self.compiled.values():
            for action in block.get("actions",[]):
                if action["name"] in ("Sleep Breath","Weakening Breath","Paralyzing Breath"):
                    count+=1
                    self.assertTrue(action.get("manual_unmodeled_source_effect_required"),block["id"])
                    self.assertTrue(action.get("manual_rider_resolution_required"),block["id"])
                    self.assertNotIn("save_effect_kind",action)
        self.assertEqual(12,count)

    def test_known_breath_effects_pass_source_manual_boundary(self):
        errors,count=xmm.check_unmodeled_action_effects(list(self.source.values()),list(self.compiled.values()))
        self.assertEqual([],errors)
        self.assertGreater(count,0)

    def test_save_effect_marker_on_other_creature_is_rejected(self):
        compiled=copy.deepcopy(list(self.compiled.values()))
        other=next(m for m in compiled if m["id"]=="Adult Brass Dragon")
        other["actions"][3]["save_effect_kind"]="slowing_breath"
        errors,count=xmm.check_save_effect_contracts(list(self.source.values()),compiled)
        self.assertEqual(4,count)
        self.assertEqual(["Adult Brass Dragon:Sleep Breath:SAVE_EFFECT_SOURCE_CONTRACT_MISMATCH"],errors)


if __name__=="__main__":
    unittest.main()
