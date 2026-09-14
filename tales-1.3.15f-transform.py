from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
src=(repo/'tales-1.3.15d-transform.py').read_text(encoding='utf-8')
old='''          if (!CanonicalTalesRules.validItem(action.item)) rejected += "ação[$index] item_add: Item viola bônus/tipo canônico."
          else reduce(GameEvent.AddItem(action.item), "item_add")'''
new='''          if (reduce(GameEvent.AddItem(action.item), "item_add")) raiseRoute(MotorRouteHint.SCENE)'''
if old not in src:
    raise SystemExit('embedded GiveItem old anchor not found in 1.3.15d source')
src=src.replace(old,new,1)
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15d-transform.py')}
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],sys.argv[1]]
try:
    exec(compile(src,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv
