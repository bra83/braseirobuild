#!/usr/bin/env python3
"""Non-semantic 483 section path and provenance gate regression."""
import unittest

import editorial_483_runtime_assessment as audit


class Editorial483AssessmentTest(unittest.TestCase):
    def test_exact_483_sections_have_runtime_and_test_bindings(self):
        report = audit.run(audit.ROOT)
        self.assertTrue(report["structural_gate_pass"], report["problems"][:12])
        self.assertEqual(483, report["sections_evaluated"])
        self.assertEqual(audit.EXPECTED, report["by_book"])
        self.assertEqual(483, report["structural_evidence"]["RUNTIME_CLASS_PATH_LINKED"])
        self.assertEqual(483, report["structural_evidence"]["TEST_METHOD_DECLARED"])
        self.assertEqual(483, report["pending_semantic_certification"]
                         + report["source_verified_and_semantic_passed"])
        self.assertEqual(483, len(set(r["key"] for r in report["sections"])))

    def test_au_sections_link_real_rule_engines_not_merely_translation(self):
        report = audit.run(audit.ROOT)
        sections = [s for s in report["sections"] if s["book_id"] == "AU"]
        self.assertEqual(45, len(sections))
        for section in sections:
            ch = section["key"].split(":")[1]
            if ch in {"ch_1", "ch_2", "ch_4", "ch_5", "ch_6", "ch_7"}:
                self.assertNotEqual("com.braseiro.dnd5e.ArcanaUnleashedPtBr",
                                    section["runtime_class"], section["key"])
        unfinished = [s for s in sections
                      if s["next_gate"] == "AU_STANDALONE_ADVENTURE_GRAPH_NOT_YET_SOURCE_CERTIFIED"]
        self.assertEqual(6, len(unfinished))

    def test_fraif_distinguishes_creatures_from_headings_and_items(self):
        report = audit.run(audit.ROOT)
        fraif = [s for s in report["sections"] if s["book_id"] == "FRAIF"]
        self.assertEqual(151, len(fraif))
        self.assertEqual(39, sum(s["next_gate"] == "EXACT_FRAIF_CREATURE_STATBLOCK_REQUIRED"
                                 for s in fraif))
        self.assertEqual(23, sum(s["next_gate"] == "SOURCE_CREATURE_FAMILY_HEADING"
                                 for s in fraif))
        self.assertEqual(7, sum(s["next_gate"] == "EXACT_FRAIF_MAGIC_ITEM_EFFECTS_REQUIRED"
                                 for s in fraif))

    def test_editorial_metadata_is_not_a_gameplay_mechanic(self):
        self.assertEqual("EDITORIAL_ONLY", audit.gameplay_tier(
            "editorial_metadata", "Credits"))
        self.assertEqual("GAMEPLAY_RULE_OR_CATALOG", audit.gameplay_tier(
            "airships_mechanics", "Operating an Airship"))
        self.assertEqual("NARRATIVE_AND_CONSEQUENCES", audit.gameplay_tier(
            "adventure_narrative", "Beginning the Adventure"))


if __name__ == "__main__":
    unittest.main()
