from pathlib import Path
import sys

repo = Path(__file__).resolve().parent
src = (repo / "tales-1.3.15-transform.py").read_text(encoding="utf-8")
old = r'''# Insert helper map decoder and state fields at GameState return.
anchor='''    val routeHistory = if (version >= 9) map["route.history"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec) else emptyList()'''
new=anchor+r'''
    fun decodeIntMap(key: String): Map<String, Int> = map[key].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p=raw.split('~',limit=2); p.getOrNull(1)?.toIntOrNull()?.let { dec(p[0]) to it }
    }.toMap()
'''
t=rep(t,anchor,new,"snapshot int map helper")
# Find tail constructor fields via lastError anchor.
t=t.replace('''      change = ChangeState(''','''      informationBonusBank = if (version >= 12) int("bonus.informationBank") else 0,\n      leadershipPoolDice = if (version >= 12) int("bonus.leadershipPool") else 0,\n      leadershipAssignments = if (version >= 12) decodeIntMap("bonus.leadershipAssignments") else emptyMap(),\n      grantedSuccesses = if (version >= 12) decodeIntMap("bonus.grantedSuccesses") else emptyMap(),\n      waivedTroubles = if (version >= 12) map["bonus.waivedTroubles"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec).toSet() else emptySet(),\n      change = ChangeState(''',1)
write(rel,t)'''
new = r'''# Insert helper map decoder and state fields at GameState return.
t=rep(t,
'''    val scenes = map["ext.scenes"].orEmpty().split('|')''',
'''    fun decodeIntMap(key: String): Map<String, Int> = map[key].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p=raw.split('~',limit=2); p.getOrNull(1)?.toIntOrNull()?.let { dec(p[0]) to it }
    }.toMap()

    val scenes = map["ext.scenes"].orEmpty().split('|')''',"snapshot int map helper")
t=rep(t,
'''      endingMode = endingMode,
      change = change,
      lastError = null''',
'''      endingMode = endingMode,
      change = change,
      informationBonusBank = if (version >= 12) int("bonus.informationBank") else 0,
      leadershipPoolDice = if (version >= 12) int("bonus.leadershipPool") else 0,
      leadershipAssignments = if (version >= 12) decodeIntMap("bonus.leadershipAssignments") else emptyMap(),
      grantedSuccesses = if (version >= 12) decodeIntMap("bonus.grantedSuccesses") else emptyMap(),
      waivedTroubles = if (version >= 12) map["bonus.waivedTroubles"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec).toSet() else emptySet(),
      lastError = null''',"snapshot GameState bonus decode")
write(rel,t)'''
if old not in src:
    raise SystemExit("meta anchor missing in 1.3.15 transform")
src = src.replace(old, new, 1)
ns={"__name__":"__main__","__file__":str(repo/"tales-1.3.15-transform.py")}
old_argv=sys.argv[:]
sys.argv=[ns["__file__"], sys.argv[1]]
try:
    exec(compile(src, ns["__file__"], "exec"), ns)
finally:
    sys.argv=old_argv
