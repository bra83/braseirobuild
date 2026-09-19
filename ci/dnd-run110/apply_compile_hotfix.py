#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_compile_hotfix.py <project-root>")

root = Path(sys.argv[1])
target = root / "android/app/src/main/java/com/braseiro/dnd5e/StatblockConditionRuntimeEngine.java"
text = target.read_text(encoding="utf-8")

old = '  return StatblockEncounterStateEngine.setConditionRounds(token,condition,rounds).put("applied",true);'
new = '''  JSONObject updated=StatblockEncounterStateEngine.setConditionRounds(token,condition,rounds);
  try{return updated.put("applied",true);}catch(JSONException checked){throw new IllegalStateException(checked);}'''

if old in text:
    text = text.replace(old, new, 1)

if 'try{return updated.put("applied",true);}' not in text:
    raise SystemExit("RUN110 statblock JSONException hotfix not applied")

target.write_text(text, encoding="utf-8")
print("RUN110_COMPILE_HOTFIX_APPLIED")
