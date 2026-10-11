"""Behavioral JVM gate: PF2 Remaster Stunned overrides Slowed (Player Core).
Runs the shipped Java engine, not a Python imitation or source-string test.
"""
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JSON = ROOT / "tools/runtime_json/org/json"

HARNESS = r"""
package com.braseiro.pathfinder2e;
import org.json.JSONObject;
public final class Pf2R35ActionOverlapHarness {
    private static int checks = 0;
    private static void eq(String label, int actual, int expected) {
        checks++;
        if(actual != expected) throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
    }
    private static void ok(String label, boolean cond) {
        checks++;
        if(!cond) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        Pathfinder2eRulesEngine r = new Pathfinder2eRulesEngine();
        // Baseline and isolated effects.
        eq("normal action budget", r.turnBudget(0,false,false,false,0).getInt("actions_max"), 3);
        eq("quickened", r.turnBudget(0,false,true,false,0).getInt("actions_max"), 4);
        eq("slowed 1", r.turnBudget(0,false,false,true,0).getInt("actions_max"), 2);
        eq("stunned 1", r.turnBudget(0,false,false,false,1).getInt("actions_max"), 2);
        eq("stunned 4 clamps to zero", r.turnBudget(0,false,false,false,4).getInt("actions_max"), 0);
        // Stunned overrides slowed: do not subtract overlapping penalties twice.
        eq("slowed1 + stunned1", r.turnBudget(0,false,false,true,1).getInt("actions_max"), 2);
        eq("quickened + slowed1 + stunned1", r.turnBudget(0,false,true,true,1).getInt("actions_max"), 3);
        eq("quickened + slowed1 + stunned2", r.turnBudget(0,false,true,true,2).getInt("actions_max"), 2);
        eq("used actions clamp", r.turnBudget(9,false,true,true,1).getInt("actions_remaining"), 0);
        // The condition parser is a separate path and must give the same result.
        JSONObject s = r.evaluateConditions("quickened, slowed 2, stunned 1");
        eq("condition budget overlapping", s.getInt("actions_max"), 2);
        eq("condition actions lost overlapping", s.getInt("actions_lost"), 2);
        JSONObject t = r.evaluateConditions("slowed 2, stunned 1");
        eq("stunned/slowed condition budget", t.getInt("actions_max"), 1);
        eq("stunned/slowed condition loss", t.getInt("actions_lost"), 2);
        JSONObject u = r.evaluateConditions("stunned 4, slowed 2");
        eq("stunned 4 blocks action", u.getInt("actions_max"), 0);
        eq("stunned 4 dominant", u.getInt("actions_lost"), 4);
        ok("quickened restriction retained", r.turnBudget(0,false,true,false,0).getBoolean("quickened_action_restricted"));
        // Independent PF2 operation does not regress.
        ok("persistent flat check DC15 fail on 14", !r.persistentDamageFlatCheck(14,false).getBoolean("recovered"));
        ok("persistent flat check DC15 succeed on 15", r.persistentDamageFlatCheck(15,false).getBoolean("recovered"));
        ok("assisted persistent flat check DC10", r.persistentDamageFlatCheck(10,true).getBoolean("recovered"));
        System.out.println("PF2_R35_ACTION_OVERLAP_PASS checks=" + checks);
    }
}
"""

def test_pf2_real_jvm_action_overlap_and_persistent_damage(tmp_path):
    javac = shutil.which("javac")
    java = shutil.which("java")
    assert javac and java, "JDK 21 is required for behavioral mechanics certification"
    p = tmp_path / "Pf2R35ActionOverlapHarness.java"
    p.write_text(HARNESS, encoding="utf-8")
    source = [
        JSON / "JSONException.java",
        JSON / "JSONArray.java",
        JSON / "JSONObject.java",
        SRC / "Pathfinder2eRulesEngine.java",
        p,
    ]
    for filename in source:
        assert filename.is_file(), f"Missing Java source: {filename}"
    compile_result = subprocess.run([javac, "-encoding", "UTF-8", "-d", str(tmp_path), *map(str, source)],
                                    capture_output=True, text=True, timeout=90)
    assert compile_result.returncode == 0, compile_result.stderr
    run_result = subprocess.run([java, "-cp", str(tmp_path),
                                 "com.braseiro.pathfinder2e.Pf2R35ActionOverlapHarness"],
                                capture_output=True, text=True, timeout=30)
    assert run_result.returncode == 0, run_result.stderr
    assert "PF2_R35_ACTION_OVERLAP_PASS checks=19" in run_result.stdout

def test_r35_source_identity():
    assert "VERSION_NAME=9.0.36" in (ROOT / "version.properties").read_text(encoding="utf-8")
    r = (SRC / "Pathfinder2eRulesEngine.java").read_text(encoding="utf-8")
    assert ".put(\"actions_lost\",Math.max(slowed,stunned))" in r
