from pathlib import Path
import re,sys
repo=Path(__file__).resolve().parent
src=(repo/'tales-1.3.15d-transform.py').read_text(encoding='utf-8')
replacement=r"""s=one(s,'''        is MotorAction.GiveItem -> {
          if (reduce(GameEvent.AddItem(action.item), \"item_add\")) raiseRoute(MotorRouteHint.SCENE)
        }''','''        is MotorAction.GiveItem -> {
          val test=state.activeTest; val result=state.testResolution
          val creating=test?.purpose in setOf(TestPurpose.BUILD_ITEM,TestPurpose.PROGRAM_ITEM) && result!=null
          val upgrades=result?.bonusEffects?.count{CanonicalTalesRules.effect(it.effectId)?.kind==BonusEffectKind.ITEM_BONUS_UPGRADE} ?: 0
          val expected=(1+upgrades).coerceAtMost(3)
          when {
            !CanonicalTalesRules.validItem(action.item) -> rejected += \"ação[$index] item_add: Item viola bônus/tipo canônico.\"
            creating && (action.item.kind!=KidItemKind.MYSTERY || action.item.bonus!=expected) -> rejected += \"ação[$index] item_add: Item criado deve ter bônus local +$expected.\"
            else -> if (reduce(GameEvent.AddItem(action.item),\"item_add\")) raiseRoute(MotorRouteHint.SCENE)
          }
        }''','craft firewall')"""
pat=r"s=one\(s,'''        is MotorAction\.GiveItem -> \{.*?\},'''craft firewall'\)"
patched,n=re.subn(pat,replacement,src,count=1,flags=re.S)
if n!=1: raise SystemExit(f'craft meta patch failed {n}')
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15d-transform.py')}
old=sys.argv[:]; sys.argv=[ns['__file__'],sys.argv[1]]
try: exec(compile(patched,ns['__file__'],'exec'),ns)
finally: sys.argv=old
