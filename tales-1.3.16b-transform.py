from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])

# Apply the complete 1.3.16a foundation first.
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16a-transform.py')}
code=(repo/'tales-1.3.16a-transform.py').read_text(encoding='utf-8')
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

# Snapshot v14 already writes ext.profile in 1.3.16a. Ensure the decoder restores it too.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt'; s=read(rel)
old='''    val extended = ExtendedTroubleState(
      active = bool("ext.active"),'''
new='''    val extended = ExtendedTroubleState(
      rulesProfile = if (version >= 14) runCatching { RulesProfile.valueOf(map["ext.profile"].orEmpty()) }.getOrDefault(RulesProfile.TALES_KIDS) else RulesProfile.TALES_KIDS,
      active = bool("ext.active"),'''
if old in s:
    s=one(s,old,new,'extended profile decoder')
elif 'rulesProfile = if (version >= 14)' not in s:
    raise SystemExit('extended profile decoder not found')
write(rel,s)

# Migrate the old 1.3.13 transport test: the intent remains mechanical authority separated
# from RAG, but the mechanical contract is now selected by the active Tales/Flood profile.
rel='app/src/test/java/com/braseiro/talesvtt/game/CanonicalRulesV1313Test.kt'; s=read(rel)
old='''    assertTrue(src.contains("CanonicalTalesRules.localMechanicalContract()"))'''
new='''    assertTrue(src.contains("RulesProfiles.mechanicalContract(state.rulesProfile)"))'''
if old in s:
    s=one(s,old,new,'legacy transport assertion')
elif new not in s:
    raise SystemExit('legacy transport assertion not found')
write(rel,s)

# Strengthen the Flood snapshot regression so failures identify the exact restored field.
rel='app/src/test/java/com/braseiro/talesvtt/game/FloodProfileFoundationV1316Test.kt'; s=read(rel)
s=s.replace('assertEquals(6,restored.extended.target)','assertEquals("Flood Extended Trouble profile must survive save/load",6,restored.extended.target)',1)
write(rel,s)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16b-dev — correção da fundação Flood\n- Corrigida a restauração do perfil FLOOD_TEENS dentro de Problema Estendido após save/load.\n- Migrado teste antigo de autoridade mecânica para aceitar contrato dinâmico por perfil sem enfraquecer a separação RAG/mecânica.\n- Nenhum APK publicado.\n'''
resume.write_text(text,encoding='utf-8')

assert 'rulesProfile = if (version >= 14)' in read('app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt')
assert 'RulesProfiles.mechanicalContract(state.rulesProfile)' in read('app/src/test/java/com/braseiro/talesvtt/game/CanonicalRulesV1313Test.kt')
print('TALES/FLOOD 1.3.16b regression fixes: OK')
