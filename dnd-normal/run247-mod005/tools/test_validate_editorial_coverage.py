#!/usr/bin/env python3
"""Regression tests: an editorial CSV can never self-certify its own claims."""
import csv
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import validate_editorial_coverage as gate


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


if __name__ == "__main__":
    unittest.main()
