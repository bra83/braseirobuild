from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16f-transform.py')}
code=(repo/'tales-1.3.16f-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

p=root/'app/src/main/java/com/braseiro/talesvtt/game/CanonicalFloodRules.kt'
s=p.read_text(encoding='utf-8')
s=s.replace(r'Regex("\s+")', r'Regex("\\s+")')
if r'Regex("\\s+")' not in s:
    raise SystemExit('Flood troubleKey regex escape fix not applied')
p.write_text(s,encoding='utf-8')
print('TALES/FLOOD 1.3.16f1 regex escape: OK')
