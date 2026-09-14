from pathlib import Path
import re,sys

repo=Path(__file__).resolve().parent
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15g-transform.py')}
code=(repo/'tales-1.3.15g-transform.py').read_text(encoding='utf-8')
old=sys.argv[:]
sys.argv=[ns['__file__'],sys.argv[1]]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old

root=Path(sys.argv[1])
p=root/'app/src/test/java/com/braseiro/talesvtt/game/CanonicalRulesV1313Test.kt'
t=p.read_text(encoding='utf-8')
pat=r'''  @Test fun mysteryBoundaryDropsMysteryItemsButKeepsOrdinaryObjects\(\) \{.*?\n  \}\n'''
replacement='''  @Test fun mysteryBoundaryKeepsMysteryItemsUntilChangeThenDropsRemaining() {
    val ordinary=KidItemState("Caderno","",0,KidItemKind.ORDINARY)
    val mystery=KidItemState("Detector","",2,KidItemKind.MYSTERY)
    val state=GameState(kid=KidState(items=listOf(ordinary,mystery)),endingMode=EndingMode.MYSTERY,xpAnswers=listOf(true,false,false,false,false))
    val ended=TalesReducer.reduce(state,GameEvent.ConfirmMysteryEnd)
    assertNull(ended.lastError)
    assertTrue(ended.kid.items.any{it.kind==KidItemKind.MYSTERY})
    val changed=TalesReducer.reduce(ended,GameEvent.ConfirmBetweenMysteryReview)
    assertNull(changed.lastError)
    assertTrue(changed.kid.items.any{it.kind==KidItemKind.ORDINARY})
    assertFalse(changed.kid.items.any{it.kind==KidItemKind.MYSTERY})
  }
'''
t,n=re.subn(pat,replacement,t,count=1,flags=re.S)
if n!=1: raise SystemExit(f'legacy item test replacement failed: {n}')
p.write_text(t,encoding='utf-8')
print('TALES 1.3.15h legacy-test migration: OK')
