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
          "{@condition Grappled|XPHB}, escape {@dc 13}."],}]}]
        compiled=[{"id":"Ankheg","actions":[{"name":"Bite","escape_dc":13,
          "condition_on_hit":"grappled","condition_max_target_size":"large"}]}]
        contracts=[{"id":"Ankheg","name":"Bite","slot":0,"condition":"grappled",
          "dc":13,"size":"Large"}]
        errors,count=xmm.check_grapple_escape_riders(source,compiled,contracts,1)
        self.assertEqual([],errors); self.assertEqual(1,count)
        compiled[0]["actions"][0]["save_dc"]=13
        errors,_=xmm.check_grapple_escape_riders(source,compiled,contracts,1)
        self.assertEqual(["Ankheg:Bite:ESCAPE_DC_MISCLASSIFIED","GRAPPLE_ESCAPE_RECORD_COUNT:0"],errors)
    def test_not_503_is_not_success(self):
        with self.assertRaises(ValueError):xmm.check_monsters([],[],[])
if __name__=="__main__":unittest.main()
