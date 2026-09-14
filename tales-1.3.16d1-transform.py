from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16d-transform.py')}
code=(repo/'tales-1.3.16d-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

p=root/'app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'
s=p.read_text(encoding='utf-8')
old='''  data class ApplyBetweenMysteryChanges(
    val problem: String? = null,
    val drive: String? = null,
    val prideText: String? = null,
    val shameText: String? = null,
    val iconicItemName: String? = null,
    val relationships: List<KidRelationship>? = null
  ) : GameEvent'''
new='''  data class ApplyBetweenMysteryChanges(
    val problem: String? = null,
    val drive: String? = null,
    val prideText: String? = null,
    val iconicItemName: String? = null,
    val relationships: List<KidRelationship>? = null,
    val shameText: String? = null
  ) : GameEvent'''
if old not in s:
    raise SystemExit('ApplyBetweenMysteryChanges compatibility anchor missing')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')
print('TALES/FLOOD 1.3.16d1 positional compatibility: OK')
