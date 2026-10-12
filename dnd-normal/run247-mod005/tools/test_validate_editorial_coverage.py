#!/usr/bin/env python3
"""Regression tests: an editorial CSV can never self-certify its own claims."""
import csv
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import validate_editorial_coverage as gate
import generate_acceptance_ledger as generator


class EditorialEvidenceGateTest(unittest.TestCase):
    base = {
        "book_id": "FRAIF",
        "chapter_id": "ch_2",
        "section_id": "sec_2_9",
        "section_title": "Restoring Freedale",
        "source_url": "https://5e.tools/book.html#fraif",
        "evidence_of_access": "HTTP 200 via mirror (UNVERIFIED)",
        "verification_result": "IMPLEMENTED_AND_TESTED",
        "implementation_path": "app/src/main/java/com/braseiro/dnd5e/canon/FaerunCanonCatalog.java",
    }
    ledger = {
        "book_id": "FRAIF",
        "chapter_id": "ch_2",
        "section_id": "sec_2_9",
        "expected_behavior": "Aplicar consequências concretas ao restaurar Freedale",
        "runtime_symbol": "com.braseiro.dnd5e.canon.FaerunCanonCatalog",
        "assertion_id": "EditorialSectionProofTest#testFraifEditorialSectionsCoverage",
        "actual_result": "NOT_RUN",
        "source_version": "UNVERIFIED",
        "rights_basis": "REVIEW_REQUIRED",
        "status": "IMPLEMENTED_NEEDS_TEST",
        "evidence_ref": "",
        "review_note": "",
    }

    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / "docs").mkdir(parents=True)

    def write(self, sections=None, ledger=None):
        s = list(sections if sections is not None else [self.base])
        l = list(ledger if ledger is not None else [self.ledger])
        for path, header, rows in (
            (gate.SECTIONS, sorted(gate.SECTIONS_COLUMNS), s),
            (gate.ACCEPTANCE, sorted(gate.ACCEPTANCE_COLUMNS | {"evidence_ref", "review_note"}), l),
        ):
            with (self.root / path).open("w", newline="", encoding="utf-8") as fh:
                writer = csv.DictWriter(fh, fieldnames=header)
                writer.writeheader()
                writer.writerows(rows)

    def test_unproven_section_is_queue_not_homologation(self):
        self.write()
        result = gate.inspect(self.root)
        self.assertEqual([], result["errors"])
        self.assertEqual(0, result["certified"])
        self.assertEqual(1, result["pending"])

    def test_automatically_accepted_status_is_fatal(self):
        self.write(ledger=[{**self.ledger, "status": "ACCEPTED",
                            "actual_result": "VERIFIED_PASSED"}])
        result = gate.inspect(self.root)
        self.assertTrue(any("estado proibido" in e for e in result["errors"]))

    def test_claimed_success_without_specific_proof_is_fatal(self):
        self.write(ledger=[{**self.ledger, "status": "IMPLEMENTED_AND_TESTED",
                            "actual_result": "VERIFIED_PASSED"}])
        result = gate.inspect(self.root)
        self.assertTrue(any("comprovante JSON" in e for e in result["errors"]))

    def test_not_applicable_requires_explicit_review_reason(self):
        self.write(ledger=[{**self.ledger, "status": "NOT_APPLICABLE"}])
        result = gate.inspect(self.root)
        self.assertTrue(any("justificativa editorial" in e for e in result["errors"]))

    def test_duplicate_section_rejected(self):
        self.write(sections=[self.base, self.base])
        result = gate.inspect(self.root)
        self.assertTrue(any("seção duplicada" in e for e in result["errors"]))

    def test_pending_must_not_pretend_test_passed(self):
        self.write(ledger=[{**self.ledger, "actual_result": "VERIFIED_PASSED"}])
        result = gate.inspect(self.root)
        self.assertTrue(any("falso para estado" in e for e in result["errors"]))

    def test_ledger_must_not_omit_any_inventory_row(self):
        self.write(ledger=[])
        result = gate.inspect(self.root)
        self.assertTrue(any("seção ausente" in e for e in result["errors"]))


    def prepare_generator_source(self):
        # The generation source requires more metadata than the inventory gate.
        row = {**self.base, "content_type": "lore", "delta": ""}
        path = self.root / gate.SECTIONS
        with path.open("w", newline="", encoding="utf-8") as fh:
            writer = csv.DictWriter(
                fh, fieldnames=sorted(gate.SECTIONS_COLUMNS | {"content_type", "delta"})
            )
            writer.writeheader()
            writer.writerow(row)

    def read_generated_ledger(self):
        with (self.root / gate.ACCEPTANCE).open(newline="", encoding="utf-8") as fh:
            return list(csv.DictReader(fh))

    def test_generator_discards_fake_proof_on_regeneration(self):
        prior = {
            **self.ledger,
            "status": "IMPLEMENTED_AND_TESTED",
            "actual_result": "VERIFIED_PASSED",
            "source_version": "2026-release",
            "rights_basis": "PERMISSION_VERIFIED",
            "evidence_ref": "docs/editorial-evidence/does-not-exist.json",
        }
        self.write(ledger=[prior])
        self.prepare_generator_source()
        generator.generate(self.root)
        regenerated = self.read_generated_ledger()
        self.assertEqual(1, len(regenerated))
        self.assertEqual("IMPLEMENTED_NEEDS_TEST", regenerated[0]["status"])
        self.assertEqual("NOT_RUN", regenerated[0]["actual_result"])
        self.assertEqual("", regenerated[0]["evidence_ref"])
        self.assertEqual([], gate.inspect(self.root)["errors"])

    def test_generator_preserves_valid_structural_proof(self):
        # Synthetic fixture. Passing this is NOT verification of any real book.
        prior = {
            **self.ledger,
            "status": "IMPLEMENTED_AND_TESTED",
            "actual_result": "VERIFIED_PASSED",
            "source_version": "licensed-2026-edition",
            "rights_basis": "SPECIFIC_LICENSE_REVIEW",
            "evidence_ref": "docs/editorial-evidence/fraif-sec_2_9.json",
        }
        self.write(ledger=[prior])
        self.prepare_generator_source()
        main_java = (
            self.root / "app/src/main/java/com/braseiro/dnd5e/canon/FaerunCanonCatalog.java"
        )
        main_java.parent.mkdir(parents=True)
        main_java.write_text("class FaerunCanonCatalog { void applyFreedale() {} }")
        test_java = (
            self.root / "app/src/test/java/com/braseiro/dnd5e/EditorialSectionProofTest.java"
        )
        test_java.parent.mkdir(parents=True)
        test_java.write_text(
            'class EditorialSectionProofTest { void testFraifEditorialSectionsCoverage() '
            '{ String section = "sec_2_9"; } }'
        )
        proof_file = self.root / prior["evidence_ref"]
        proof_file.parent.mkdir(parents=True)
        xml_path = proof_file.parent / "TEST-EditorialSectionProofTest.xml"
        xml_path.write_text(
            '<testsuite tests="1" failures="0" errors="0">'
            '<testcase classname="com.braseiro.dnd5e.EditorialSectionProofTest" '
            'name="testFraifEditorialSectionsCoverage"/></testsuite>'
        )
        proof_file.write_text(json.dumps({
            "book_id": self.base["book_id"],
            "chapter_id": self.base["chapter_id"],
            "section_id": self.base["section_id"],
            "source_sha256": "a" * 64,
            "source_url": "https://example.org/fixture",
            "rights_basis": "SPECIFIC_LICENSE_REVIEW",
            "source_version": "licensed-2026-edition",
            "expected_game_behavior": "Freedale consequence is applied",
            "observed_game_behavior": "Freedale consequence was observed in fixture",
            "runtime_method": "applyFreedale",
            "test_xml": str(xml_path.relative_to(self.root)).replace("\\", "/"),
        }))
        generator.generate(self.root)
        regenerated = self.read_generated_ledger()
        self.assertEqual(1, len(regenerated))
        self.assertEqual("IMPLEMENTED_AND_TESTED", regenerated[0]["status"])
        self.assertEqual(prior["evidence_ref"], regenerated[0]["evidence_ref"])
        self.assertEqual([], gate.inspect(self.root)["errors"])


if __name__ == "__main__":
    unittest.main()
