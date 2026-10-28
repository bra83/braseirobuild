#!/usr/bin/env python3
"""Source-anchored repair of auto-recalculation on a valid random 2024 sheet.

Only this existing D&D MainActivity file is touched. No broad formatting edits.
A changed or ambiguous source stops the patch.
"""
from pathlib import Path
file=Path(__file__).resolve().parents[1]/"app/src/main/java/com/braseiro/dnd5e/MainActivity.java"
text=file.read_bytes().decode("utf-8")
old='if(f.auto.isChecked()){int lvl=o.optInt("level",1),conScore=o.optInt("con",10)'
replacement='''boolean preserveRandomMechanics=false;
            if(f.sourceSheet!=null&&f.sourceSheet.optBoolean("generation_valid",false)){
                JSONObject original=f.sourceSheet;
                String[] stable={"species","class_name","background","inventory","selected_feats"};
                String[] abilities={"str","dex","con","int_score","wis","cha","level"};
                preserveRandomMechanics=true;
                for(String key:stable)
                    if(!original.optString(key).equals(o.optString(key)))
                        preserveRandomMechanics=false;
                for(String key:abilities)
                    if(original.optInt(key,-100)!=o.optInt(key,-101))
                        preserveRandomMechanics=false;
            }
            // The randomized character already has finalized HP (including
            // Tough), AC and class features. Do not overwrite them with generic
            // no-feat formulas merely because AUTO is checked in the editor.
            if(f.auto.isChecked()&&!preserveRandomMechanics){int lvl=o.optInt("level",1),conScore=o.optInt("con",10)'''
# Match only one semantic occurrence, never unrelated characters or systems.
assert text.count(old)==1, "Auto-save rule changed; abort without modifying source"
text=text.replace(old,replacement,1)
file.write_bytes(text.encode("utf-8"))
print("PATCH_DND_6620_PRESERVE_VALIDATED_RANDOM_MECHANICS=PASS")
