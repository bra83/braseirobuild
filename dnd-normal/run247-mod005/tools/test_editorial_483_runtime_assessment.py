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
        non_rules = sum(1 for item in report["sections"]
                        if item["tier"] == "EDITORIAL_ONLY" or
                        item["kind"] == "illustration_caption")
        # The strict ledger already counts EVERY unproven section, including
        # non-gameplay editorial entries. Do not count them twice (the prior
        # assertion gave 500 for an actual inventory of 483).
        # The strict ledger deliberately excludes not-applicable editorial
        # entries from semantic pending count. Account for them once, not zero
        # times (437) or twice (500).
        self.assertEqual(report["sections_evaluated"],
                         report["pending_semantic_certification"]
                         + report["source_verified_and_semantic_passed"]
                         + report["non_rule_items"])
        # Source metadata makes a section a possible non-rule; only a
        # justified NOT_APPLICABLE entry is actually reviewed. The latest
        # inventory has 63 candidates, 46 reviewed and 17 still pending.
        self.assertEqual(non_rules, report["editorial_only_candidates"])
        self.assertEqual(
            non_rules - report["non_rule_items"],
            report["editorial_only_pending_review"])
        self.assertEqual(
            report["editorial_only_pending_review"],
            len(report["editorial_only_pending_review_ids"]))
        self.assertEqual(
            len(set(report["editorial_only_pending_review_ids"])),
            report["editorial_only_pending_review"])
        self.assertGreater(non_rules, 0)
        self.assertLess(non_rules, report["sections_evaluated"])
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

    def test_stage01_source_locator_and_editorial_classification(self):
        """483 exact pinned source records; editorial decisions must not be guessed."""
        import csv
        from pathlib import Path

        root = audit.ROOT / "docs"
        def load(name):
            with (root / name).open(encoding="utf-8-sig", newline="") as f:
                return list(csv.DictReader(f))
        matrix = load("PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv")
        ledger = load("EDITORIAL_SECTION_ACCEPTANCE.csv")
        provenance = load("EDITORIAL_SOURCE_PROVENANCE_483.csv")
        anchors = load("SOURCE_BOOK_JSON_ANCHORS_483_20261013.csv")
        section_key = lambda r: (r["book_id"], r["chapter_id"], r["section_id"])
        self.assertEqual(483, len(matrix))
        self.assertEqual(483, len(ledger))
        self.assertEqual(483, len(provenance))
        self.assertEqual(483, len(anchors))
        pm = {section_key(row): row for row in provenance}
        am = {section_key(row): row for row in anchors}
        lm = {section_key(row): row for row in ledger}
        self.assertEqual(483, len(pm))
        self.assertEqual(set(pm), set(am))
        self.assertEqual(set(pm), set(lm))
        reviewed = 0
        for row in matrix:
            k = section_key(row)
            self.assertIn(k, pm)
            p, a = pm[k], am[k]
            self.assertEqual(p["source_git_blob_sha1"], a["mirror_json_blob_sha"])
            self.assertTrue(p["source_commit"] and len(p["source_commit"]) == 40)
            self.assertTrue(p["json_pointer_to_exact_title"].startswith("/bookData/"))
            self.assertTrue(a["mirror_json_pointer"].startswith("/bookData/"))
            self.assertEqual(
                p["json_pointer_to_exact_title"].rsplit("/", 1)[0],
                a["mirror_json_pointer"], k)
            self.assertIn(p["source_git_blob_sha1"], row["evidence_of_access"])
            self.assertIn(a["mirror_json_pointer"], row["evidence_of_access"])
            self.assertIn("/blob/" + p["source_commit"] + "/", a["source_url"])
            if row["content_type"] in {"editorial_metadata", "illustration_caption"}:
                self.assertEqual("NOT_APPLICABLE", lm[k]["status"], k)
                self.assertTrue(lm[k]["review_note"], k)
                reviewed += 1
        self.assertEqual(56, reviewed)
        self.assertEqual(56, sum(1 for x in ledger if x["status"] == "NOT_APPLICABLE"))

    def test_editorial_metadata_is_not_a_gameplay_mechanic(self):
        self.assertEqual("EDITORIAL_ONLY", audit.gameplay_tier(
            "editorial_metadata", "Credits"))
        self.assertEqual("GAMEPLAY_RULE_OR_CATALOG", audit.gameplay_tier(
            "airships_mechanics", "Operating an Airship"))
        self.assertEqual("NARRATIVE_AND_CONSEQUENCES", audit.gameplay_tier(
            "adventure_narrative", "Beginning the Adventure"))


if __name__ == "__main__":
    unittest.main()
