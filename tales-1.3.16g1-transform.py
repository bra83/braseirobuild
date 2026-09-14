from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
source=(repo/'tales-1.3.16g-transform.py').read_text(encoding='utf-8')
source=source.replace("tales-1.3.16f-transform.py","tales-1.3.16f1-transform.py")
if "tales-1.3.16f1-transform.py" not in source:
    raise SystemExit('Could not redirect 1.3.16g to fixed 1.3.16f1 base')
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16g-transform.py')}
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(source,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv
print('TALES/FLOOD 1.3.16g1 fixed chain: OK')
