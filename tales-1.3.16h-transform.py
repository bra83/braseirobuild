from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16g1-transform.py')}
code=(repo/'tales-1.3.16g1-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def one(text,old,new,label):
    if old not in text: raise SystemExit('missing anchor: '+label)
    return text.replace(old,new,1)

# JVM unit tests exercise MotorBarbaraWireAdapter, which uses org.json at runtime.
# The Android SDK exposes only mock JSON methods to local unit tests, so provide
# the real JVM implementation as a test-only dependency. Production deps stay unchanged.
rel='app/build.gradle.kts'; s=read(rel)
if 'testImplementation("org.json:json:20240303")' not in s:
    s=one(s,
'''  testImplementation("junit:junit:4.13.2")''',
'''  testImplementation("junit:junit:4.13.2")
  testImplementation("org.json:json:20240303")''','JVM org.json test dependency')
write(rel,s)

# 1.3.16f deliberately removed the Tales-only gate around bonus effects and
# routed the catalogue through RulesProfiles. Update the older UI contract test
# so it checks the intended profile-aware behavior instead of the superseded gate.
rel='app/src/test/java/com/braseiro/talesvtt/game/FloodProfileUiV1316Test.kt'; s=read(rel)
s=one(s,
'''    assertTrue(s.contains("if(!flood) CanonicalBonusEffects"))''',
'''    assertTrue(s.contains("CanonicalBonusEffects(gameState,onPurchaseBonusEffect)"))
    assertTrue(s.contains("RulesProfiles.effectsForSkill(gameState.rulesProfile,skill)"))
    assertFalse(s.contains("if(!flood) CanonicalBonusEffects"))''','routed Flood bonus UI regression')
write(rel,s)

# Add one negative wire assertion too: profile aliases are accepted, but Friction
# remains rejected by the firewall under Tales. The existing g test covers the
# positive parse and firewall paths with the real JSON implementation now present.
rel='app/src/test/java/com/braseiro/talesvtt/game/FloodMotorAndFrictionV1316Test.kt'; s=read(rel)
anchor='''  @Test fun floodFrictionRecoveryHealsOnlyThroughFloodFirewall(){'''
insert='''  @Test fun wireFloodAliasAlsoRecognizesFrictionRecovery(){
    val parsed=MotorBarbaraWireAdapter.parse("""{"profile":"flood","actions":[{"type":"recovery","approved":true,"source":"fricção"}]}""")
    assertEquals(MotorProfile.FLOOD_TEENS,parsed.packet.profile)
    assertEquals(RecoveryKind.FRICTION,(parsed.packet.actions.single() as MotorAction.Recovery).kind)
  }

  @Test fun floodFrictionRecoveryHealsOnlyThroughFloodFirewall(){'''
s=one(s,anchor,insert,'wire alias regression')
write(rel,s)

print('TALES/FLOOD 1.3.16h regression harness closure: OK')
