"""Offline regressions for the pinned source provenance validator."""
import hashlib
import unittest

import verify_book_sources_483 as source


class PinnedSourceUnitTests(unittest.TestCase):
    def test_git_blob_sha_is_not_raw_file_sha(self):
        data = b'{"bookData": []}\n'
        expected = hashlib.sha1(
            b"blob " + str(len(data)).encode("ascii") + b"\0" + data
        ).hexdigest()
        self.assertEqual(expected, source.git_blob_sha1(data))
        self.assertNotEqual(hashlib.sha1(data).hexdigest(), expected)

    def test_483_pointer_style_resolves_nested_json(self):
        obj = {"bookData": [{"data": [
            {"entries": [{"type": "image", "title": "A realm."}]}
        ]}]}
        title = source.json_pointer(obj, "/bookData/0/data/0/entries/0/title")
        self.assertEqual("A realm.", title)
        parent = source.json_pointer(obj, "/bookData/0/data/0/entries/0")
        self.assertEqual("image", parent["type"])

    def test_nonexistent_path_is_not_silently_accepted(self):
        with self.assertRaises(ValueError):
            source.json_pointer({"bookData": []}, "/bookData/0/data/0/name")

    def test_ten_reviewed_editorial_exclusions_require_individual_pinned_proof(self):
        self.assertEqual(10, len(source.EDITORIAL_EXCLUSIONS))
        key = ("XMM", "ch_4", "sec_4_0")
        sha = "a" * 40
        provenance = {"source_git_blob_sha1": sha,
                      "source_file": "data/book/book-xmm.json",
                      "source_commit": source.SOURCE_COMMIT}
        matrix = {"content_type": "editorial_metadata",
                  "verification_result": "NOT_APPLICABLE",
                  "evidence_of_access": "gitblob=" + sha}
        ledger = {"status": "NOT_APPLICABLE",
                  "actual_result": "NON_RULE_EDITORIAL_REVIEWED",
                  "expected_behavior": "No game action required; staff credits",
                  "rights_basis": "REVIEW_REQUIRED",
                  "review_note": ("STAGE01_SOURCE_CONTENT_REVIEW:STAFF_CREDITS; "
                                  "content inspected; data/book/book-xmm.json#pointer; "
                                  "blob=" + sha)}
        self.assertTrue(source.valid_editorial_exclusion(key, matrix, ledger, provenance))
        tampered = {**ledger, "review_note": "CREDITS_NO_RULE"}
        self.assertFalse(source.valid_editorial_exclusion(key, matrix, tampered, provenance))
        self.assertFalse(source.valid_editorial_exclusion(
            ("ABH", "ch_0", "sec_0_1"), matrix, ledger, provenance))
        tampered = {**matrix, "content_type": "rules_mechanics"}
        self.assertFalse(source.valid_editorial_exclusion(key, tampered, ledger, provenance))

    def test_unicode_accent_punctuation_normalization_is_conservative(self):
        self.assertEqual(source.normalized("Faerûn — Heroes!"), source.normalized("Faerun Heroes"))
        self.assertNotEqual(source.normalized("Crown of Horns"), source.normalized("Crown of Stars"))


if __name__ == "__main__":
    unittest.main()
