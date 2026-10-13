"""Small, deterministic unit tests for the APK atlas verifier (no Android SDK)."""
import io
from pathlib import Path
import tempfile
import unittest
from unittest import mock
import zipfile

import verify_dnd_apk as checker


class VerifyDndApkTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.root = Path(self.dir.name)
        source_dir = self.root / "app/src/main/assets/maps"
        source_dir.mkdir(parents=True)
        self.asset = source_dir / checker.ASSET_NAME
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("map_manifest.json", '{"worldProfile":"dnd"}')
        self.good_zip = buf.getvalue()
        self.asset.write_bytes(self.good_zip)
        self.patcher = mock.patch.object(checker, "CANONICAL_BYTES", len(self.good_zip))
        self.patcher.start()
        self.addCleanup(self.patcher.stop)
        self.apk = self.root / "app-debug.apk"

    def apk_with_map(self, map_bytes):
        with zipfile.ZipFile(self.apk, "w") as z:
            z.writestr("AndroidManifest.xml", b"fixture")
            if map_bytes is not None:
                z.writestr(checker.APK_PATH, map_bytes)

    def test_byte_identical_map_is_accepted(self):
        self.apk_with_map(self.good_zip)
        result = checker.verify(self.apk, self.root)
        self.assertTrue(result["mapped_in_apk"])
        self.assertEqual(len(self.good_zip), result["faerun_zip_bytes"])

    def test_missing_map_is_rejected(self):
        self.apk_with_map(None)
        with self.assertRaisesRegex(ValueError, "FAERUN_APK_ENTRY_COUNT"):
            checker.verify(self.apk, self.root)

    def test_modified_map_with_same_size_is_rejected(self):
        altered = self.good_zip[:-1] + bytes([self.good_zip[-1] ^ 1])
        self.apk_with_map(altered)
        with self.assertRaisesRegex(ValueError, "FAERUN_EMBEDDED_HASH_MISMATCH"):
            checker.verify(self.apk, self.root)


if __name__ == "__main__":
    unittest.main()
