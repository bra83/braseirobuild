from pathlib import Path
root=Path(__file__).resolve().parents[1]
for fn,cap,id in [('longArmOfTheHutt.mjs','LongArmOfTheHutt','teemo-palace-guards'),('jewelOfYavin.mjs','JewelOfYavin','bespin-security-intercept')]:
 p=root/'app/src/main/assets/vtt/js/adventure'/fn;s=p.read_text()
 mark=f'export function mark{cap}EncounterActive(world, encounterId) {{'
 a=s.index(mark);b=s.index(f'export function record{cap}EncounterOutcome',a)
 s=s[:a]+f'''export function mark{cap}EncounterActive(world, encounterId) {{
  const next = clone(world || {{}});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não ativa.');
  const current = current{cap}Encounter(next);
  if (!current || current.id !== encounterId || !current.available || current.status !== 'ready')
    throw new Error('Encontro incompatível com a cena atual ou já resolvido.');
  p.activeEncounterId = encounterId;
  p.updatedAt = now();
  next.publishedAdventure = p;
  return next;
}}

'''+s[b:]
 a=s.index(f'export function record{cap}EncounterOutcome(');b=s.index(f'export function mark{cap}RewardsApplied',a)
 flag='teemoDefeated' if cap=='LongArmOfTheHutt' else 'securityEvaded'
 s=s[:a]+f'''export function record{cap}EncounterOutcome(world, {{ encounterId, outcome, adversaries = [] }} = {{}}) {{
  const next = clone(world || {{}});
  const p = next.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não ativa.');
  const current = current{cap}Encounter(next);
  if (!current || current.id !== encounterId || !current.available || p.activeEncounterId !== encounterId)
    throw new Error('Encontro incompatível, não iniciado ou já resolvido.');
  if (!['adversaries_defeated','player_incapacitated','ended','escaped','negotiated'].includes(outcome))
    throw new Error('Resultado de encontro inválido.');
  p.encounterHistory = Array.isArray(p.encounterHistory) ? p.encounterHistory : [];
  const record = {{ at: now(), nodeId: p.currentNodeId, encounterId, outcome }};
  p.encounterHistory.push(record);
  p.activeEncounterId = null;
  p.flags = {{ ...(p.flags || {{}}), {flag}: outcome === 'adversaries_defeated' }};
  p.updatedAt = now();
  next.publishedAdventure = p;
  return {{ world: next, record }};
}}

'''+s[b:]
 p.write_text(s)
# Explicit source-driven outcomes: never automatically mark Teemo defeated by pressing a button.
p=root/'app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs';s=p.read_text()
s=s.replace("{ id: 'teemo-resolved', label: 'Encerrar a caçada e selar o destino de Teemo', to: 'laoth-epilogue', effects: { teemoDefeated: true } }", "{ id: 'withdraw-from-palace', label: 'Retirar-se sem derrubar Teemo', to: 'laoth-retreat' },\n        { id: 'teemo-resolved', label: 'Confirmar a derrota de Teemo após combate adjudicado', to: 'laoth-epilogue', condition:'encounter-resolved:teemo-palace-guards' }")
s=s.replace("    'laoth-epilogue': {", "    'laoth-retreat': {\n      episode:3,title:'Retirada de Mos Shuuta',locationId:LOC.teemoPalace,\n      summary:'A tripulação escapou do palácio, mas a perseguição de Teemo continua.',\n      narration:'Os guardas retomam os postos. A tripulação deixa o palácio sem provas de que Teemo perdeu o poder.',\n      visibleNpcIds:[],checks:[],choices:[]\n    },\n    'laoth-epilogue': {")
s=s.replace("  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);", "  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);\n  if (c.startsWith('encounter-resolved:')) return (progress.encounterHistory || []).some(x => x.encounterId === c.slice(19) && x.outcome === 'adversaries_defeated');")
# UAE checks only that encounter is resolved, so add guard on advance for stronger outcome equivalence.
s=s.replace("  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');", "  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');\n  if (choiceId === 'teemo-resolved' && p.flags?.teemoDefeated !== true) throw new Error('Derrota de Teemo não confirmada.');")
p.write_text(s)
print('P183 encounter integrity hardened')
