from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15f-transform.py')}
code=(repo/'tales-1.3.15f-transform.py').read_text(encoding='utf-8')
old=sys.argv[:]
sys.argv=[ns['__file__'],sys.argv[1]]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old

root=Path(sys.argv[1])
p=root/'app/src/main/java/com/braseiro/talesvtt/game/CanonicalTalesRules.kt'
t=p.read_text(encoding='utf-8')
t=t.replace('Regex("\\s+")','Regex("\\\\s+")',1)
p.write_text(t,encoding='utf-8')
assert 'Regex("\\\\s+")' in t
print('TALES 1.3.15g regex escape: OK')
