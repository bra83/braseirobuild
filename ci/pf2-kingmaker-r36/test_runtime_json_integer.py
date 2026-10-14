"""A JVM test of the real R35 runtime JSON adapter, not merely a source grep."""
from pathlib import Path
import importlib.util
import shutil
import subprocess

CI = Path(__file__).resolve().parents[1]
PATCH = Path(__file__).with_name("patch_runtime_json_integer.py")
INSPECTION = CI / "pf2-r36-source-inspection/tools/runtime_json/org/json"


def test_r36_json_parser_preserves_integral_character_ids(tmp_path):
    spec = importlib.util.spec_from_file_location("r36_int_patch", PATCH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    (tmp_path / "version.properties").write_text("VERSION_NAME=9.0.37\n", encoding="utf-8")
    dest = tmp_path / "tools/runtime_json/org/json"
    dest.mkdir(parents=True)
    for name in ("JSONObject.java", "JSONArray.java", "JSONException.java"):
        shutil.copy2(INSPECTION / name, dest / name)
    old = (dest / "JSONObject.java").read_text(encoding="utf-8")
    assert old.count(mod.OLD) == 1
    mod.patch(tmp_path)
    new = (dest / "JSONObject.java").read_text(encoding="utf-8")
    assert mod.NEW in new and mod.OLD not in new
    probe = tmp_path / "NumberProbe.java"
    probe.write_text("""
import org.json.*;
public class NumberProbe {
 public static void main(String[] args) throws Exception {
  JSONArray a=new JSONArray("[100,101,9007199254740993]");
  for(int i=0;i<a.length();i++){
   if(!(a.opt(i) instanceof Long))throw new AssertionError("Integer parsed as non-long: "+a.opt(i));
  }
  if(!"100".equals(a.optString(0)))throw new AssertionError("Banquet PC ID 100 changed");
  if(!"9007199254740993".equals(a.optString(2)))throw new AssertionError("Precision lost above 2^53");
  JSONObject obj=new JSONObject("{\\"feast_contributor_ids\\":[100,101]}");
  if(!"101".equals(obj.optJSONArray("feast_contributor_ids").optString(1)))
   throw new AssertionError("Companion save identity changed");
  System.out.println("R36_LONG_JSON_IDS_EXACT=PASS");
 }
}
""", encoding="utf-8")
    classes = tmp_path / "classes"
    classes.mkdir()
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes),
                    *[str(dest / n) for n in ("JSONObject.java", "JSONArray.java", "JSONException.java")],
                    str(probe)], check=True, capture_output=True, text=True)
    result = subprocess.run(["java", "-cp", str(classes), "NumberProbe"],
                            check=True, capture_output=True, text=True)
    assert "R36_LONG_JSON_IDS_EXACT=PASS" in result.stdout


def test_prohibits_patch_on_immutable_r35(tmp_path):
    spec = importlib.util.spec_from_file_location("r36_int_patch", PATCH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    (tmp_path / "version.properties").write_text("VERSION_NAME=9.0.36\n", encoding="utf-8")
    try:
        mod.patch(tmp_path)
    except RuntimeError as ex:
        assert "immutable R35" in str(ex)
    else:
        raise AssertionError("Modified immutable R35")
