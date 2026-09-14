from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16i-transform.py')}
code=(repo/'tales-1.3.16i-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

build=root/'app/build.gradle.kts'
s=build.read_text(encoding='utf-8')
old='versionName = "1.3.16-dev"'
new='versionName = "1.3.16"'
if old not in s:
    raise SystemExit('missing final versionName anchor')
s=s.replace(old,new,1)
build.write_text(s,encoding='utf-8')

# Final source identity only. Evidence remains outside the generated project so
# no gate is claimed by the source transform itself.
identity=root/'BRASEIRO_TALES_FLOOD_BUILD_ID.txt'
identity.write_text(
    'BRASEIRO TALES FROM THE LOOP + THINGS FROM THE FLOOD\n'
    'VERSION_NAME=1.3.16\n'
    'VERSION_CODE=20\n'
    'SOURCE_CHAIN=TALES-1.3.8 + cumulative transforms through 1.3.16i\n',
    encoding='utf-8'
)
print('TALES/FLOOD 1.3.16 final source transform: OK')
