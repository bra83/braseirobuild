#!/usr/bin/env python3
"""R36: make warfare state writes atomic and reject unearned battle completion."""
from pathlib import Path
import sys

SRC=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")
MARKER="R36_WAR_ATOMIC_STATE"
METHODS=[
 ("resolveWarAttack",
  "String encounterKey,String attackerKey,String targetKey,int natural,int attackIndex,boolean agile,int weakness,int resistance",
  "encounterKey,attackerKey,targetKey,natural,attackIndex,agile,weakness,resistance"),
 ("resolveWarfareAction",
  "String encounterKey,String actorKey,String targetKey,String action,int naturalRoll,JSONObject options",
  "encounterKey,actorKey,targetKey,action,naturalRoll,options"),
]
OLD='if("complete".equals(nextStatus)&&hasStandingOpposition(participants,active)){String kind=entry.optString("kind");if(!"automatic_complete".equals(kind)&&!"opposition_defeated".equals(kind))throw new IllegalStateException("A batalha ainda possui oposição viva; registre uma retirada/cessar-fogo ou resolva a próxima rodada.");}'
NEW='if("complete".equals(nextStatus)&&hasStandingOpposition(participants,active))throw new IllegalStateException("A batalha ainda possui oposição viva; registre uma retirada/cessar-fogo ou resolva a próxima rodada.");'
def transform(source):
    if MARKER in source:raise ValueError("Atomic warfare patch applied twice")
    if source.count(OLD)!=1:raise ValueError("Battle completion guard drift")
    source=source.replace(OLD,NEW,1)
    for name,signature,args in METHODS:
        original="    public JSONObject "+name+"("+signature+")throws Exception{"
        if source.count(original)!=1:raise ValueError(name+" signature drift")
        wrapper=("    // "+MARKER+": damage, conditions and battle log commit together.\n"
          +original+"\n"
          +"        final JSONObject[] result={new JSONObject()};\n"
          +"        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{\n"
          +"            result[0]="+name+"Atomic("+args+");\n"
          +"        }});\n"
          +"        return result[0];\n"
          +"    }\n"
          +"    private JSONObject "+name+"Atomic("+signature+")throws Exception{")
        source=source.replace(original,wrapper,1)
    begin=source.index("    public JSONObject advanceWarEncounter(")
    end=source.index("\n    private boolean hasStandingOpposition",begin)
    portion=source[begin:end]
    anchor='if(before.length()==0)throw new IllegalArgumentException("Batalha inexistente.");'
    if portion.count(anchor)!=1:raise ValueError("advanceWarEncounter guard drift")
    portion=portion.replace(anchor,anchor+'\n        if(!"active".equals(before.optString("status")))throw new IllegalStateException("A batalha já foi encerrada; não pode ser reaberta por uma ação antiga.");',1)
    return source[:begin]+portion+source[end:]
def apply(root):
    root=Path(root)
    if "VERSION_NAME=9.0.37" not in (root/"version.properties").read_text(encoding="utf-8"):
        raise ValueError("Only canonical R36 eligible")
    target=root/SRC
    old=target.read_text(encoding="utf-8")
    new=transform(old)
    target.write_text(new,encoding="utf-8")
    print("R36_WAR_DAMAGE_LOG_ATOMIC_PATCH_APPLIED",target)
if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_war_atomic.py <R36_project>")
    apply(sys.argv[1])
