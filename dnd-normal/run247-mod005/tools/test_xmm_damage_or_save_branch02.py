"""JDK-only executable tests; no Android SDK or downloaded dependencies needed."""
import copy
import json
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

from verify_xmm_damage_or_save_branch02 import compare, EVIDENCE
from verify_xmm_503_source import java_rows

class Branch02CoreTest(unittest.TestCase):
    def test_exact_contracts_and_blocked_status(self):
        job = json.loads((EVIDENCE / "job.json").read_text())
        rows = java_rows((ROOT / "app/src/main/java/com/braseiro/dnd5e/Xmm2025CompiledBestiaryEngine.java").read_text())
        result = compare(job, rows)
        self.assertEqual(len(result), 12)
        self.assertTrue(all(not r["compiled_contract_mismatches"] for r in result))
        self.assertTrue(all(not r["completed"] for r in result))
        changed = copy.deepcopy(job)
        changed["entries"][0]["mechanical_contract"]["damage"] = "99d99"
        self.assertEqual(compare(changed, rows)[0]["compiled_contract_mismatches"], ["damage"])
        changed = copy.deepcopy(job)
        changed["entries"][0]["action_id"] = "Beard"
        self.assertIn("action_id", compare(changed, rows)[0]["compiled_contract_mismatches"])

    def test_compile_and_execute(self):
        package = 'com/braseiro/dnd5e/'
        main = ROOT / 'app/src/main/java' / package
        test = ROOT / 'app/src/test/java' / package
        with tempfile.TemporaryDirectory(prefix='xmm-branch02-') as out:
            subprocess.run(['javac', '--release', '8', '-d', out,
                            str(main / 'AreaSaveDamageRules.java'),
                            str(main / 'XmmDamageOrSaveBranch02.java'),
                            str(test / 'XmmDamageOrSaveBranch02CoreTest.java')], check=True)
            result = subprocess.run(['java', '-cp', out,
                'com.braseiro.dnd5e.XmmDamageOrSaveBranch02CoreTest'], check=True,
                capture_output=True, text=True)
            self.assertIn('PASS', result.stdout)
            print(result.stdout.strip())

if __name__ == '__main__':
    unittest.main()
