"""Negative-path, no-network tests for the XMM 503 independent comparator."""
import unittest
import verify_xmm_503_source as xmm

class XmmIndependentSourceTests(unittest.TestCase):
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
    def test_not_503_is_not_success(self):
        with self.assertRaises(ValueError):xmm.check_monsters([],[],[])
if __name__=="__main__":unittest.main()
