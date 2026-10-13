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

    def test_unicode_accent_punctuation_normalization_is_conservative(self):
        self.assertEqual(source.normalized("Faerûn — Heroes!"), source.normalized("Faerun Heroes"))
        self.assertNotEqual(source.normalized("Crown of Horns"), source.normalized("Crown of Stars"))


if __name__ == "__main__":
    unittest.main()
