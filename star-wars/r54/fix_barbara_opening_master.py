#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_barbara_opening_master.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
controller_path = root / 'app/src/main/assets/vtt/js/core/controller.mjs'
runtime_path = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
controller = controller_path.read_text(encoding='utf-8')
runtime = runtime_path.read_text(encoding='utf-8')
marker = 'R54_BARBARA_OPENING_MASTER'


def replace_method(src: str, signature: str, replacement: str) -> str:
    start = src.find(signature)
    if start < 0:
        raise SystemExit(f'controller anchor not found: {signature}')
    # Signature contains destructuring ({ rng = ... }); the final char is the real body brace.
    brace = start + len(signature) - 1
    if brace >= len(src) or src[brace] != '{':
        raise SystemExit('controller method opening brace not found')
    depth = 0
    quote = None
    esc = False
    i = brace
    while i < len(src):
        ch = src[i]
        if quote is not None:
            if esc:
                esc = False
            elif ch == '\\':
                esc = True
            elif ch == quote:
                quote = None
        else:
            if ch in ('\"', "'", '`'):
                quote = ch
            elif ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    return src[:start] + replacement + src[i + 1:]
        i += 1
    raise SystemExit('controller method closing brace not found')

if marker not in controller:
    new_method = r'''    startLivingAdventure({ rng = Math.random } = {}) {
        // R54_BARBARA_OPENING_MASTER: bootstrap session/world only; Barbara owns first narration.
        if (!this.state.player?.character)
            throw new Error('Crie, gere ou importe um personagem antes de iniciar a aventura.');
        const opening = sandbox_mjs_1.sandboxOpening(this.state.world, { locationId: this.state.scene?.locationId ?? this.state.world?.sandbox?.startLocationId ?? null, limit: 6 });
        if (!opening?.ready || !opening.location)
            throw new Error('Crie o mundo vivo e escolha o ponto inicial antes de iniciar a aventura.');
        let session = this.state.session;
        if (session?.status !== 'ativa') {
            const started = this.startSession({ playerCount: 1, rng });
            session = started.session;
        }
        const knownOpportunity = (opening.opportunities ?? []).find(item => item?.knownToPlayer) ?? (opening.opportunities ?? [])[0] ?? null;
        const knownNpc = (opening.npcs ?? []).find(item => item?.knownToPlayer) ?? null;
        const knownFaction = (opening.factions ?? []).find(item => item?.knownToPlayer) ?? null;
        const locationName = String(opening.location.name ?? this.state.scene?.title ?? 'Ponto inicial');
        const suggestions = [];
        if (knownOpportunity?.title) suggestions.push(`Investigar: ${knownOpportunity.title}`);
        if (knownNpc?.name) suggestions.push(`Conversar com ${knownNpc.name}`);
        suggestions.push('Observar o local antes de agir');
        if (opening.market) suggestions.push('Consultar o mercado local');
        suggestions.push('Procurar rumores e oportunidades');
        suggestions.push('Ignorar a situação e seguir adiante');
        return {
            opening,
            proposal: null,
            awaitingMaster: true,
            masterOpeningRequest: {
                kind: 'living_adventure_opening',
                locationName,
                sceneLocationId: opening.location.id,
                opportunity: knownOpportunity ? { id: knownOpportunity.id ?? null, title: knownOpportunity.title ?? '', summary: knownOpportunity.summary ?? '' } : null,
                npc: knownNpc ? { id: knownNpc.id ?? null, name: knownNpc.name ?? '' } : null,
                faction: knownFaction ? { id: knownFaction.id ?? null, name: knownFaction.name ?? '' } : null,
                suggestedActions: suggestions.slice(0, 6)
            },
            session: structuredClone(session),
            state: structuredClone(this.state)
        };
    }'''
    controller = replace_method(controller, '    startLivingAdventure({ rng = Math.random } = {}) {', new_method)

if marker not in runtime:
    anchor = "function adventureStarted(){const st=currentState();return Boolean(String(st?.master?.narration||st?.scene?.narration||'').trim())}\n"
    if anchor not in runtime:
        raise SystemExit('runtime adventureStarted anchor not found')
    helper = r'''async function startBarbaraOpening(){
 // R54_BARBARA_OPENING_MASTER: no local opening narration; use the canonical public Master channel.
 const prep=bridgeResult('startLivingAdventure',{});if(!prep?.ok)return prep;
 const game=globalThis.BraseiroGameBridge;if(!game||typeof game.submitPlayerIntent!=='function')return {ok:false,message:'Ponte do Mestre Barbara indisponível.'};
 const req=prep.data?.masterOpeningRequest||{};const location=String(req.locationName||'o ponto inicial');
 const opportunity=String(req.opportunity?.summary||req.opportunity?.title||'').trim();
 const npc=String(req.npc?.name||'').trim();const faction=String(req.faction?.name||'').trim();
 const facts=[`Local inicial: ${location}.`,opportunity?`Situação ativa: ${opportunity}.`:'',npc?`NPC conhecido/presente: ${npc}.`:'',faction?`Facção relevante: ${faction}.`:''].filter(Boolean).join(' ');
 const intentText=`[ABERTURA_BARBARA] Inicie a aventura agora como Mestre Barbara. ${facts} Gere a primeira cena a partir do mundo vivo persistente, sem inventar uma missão obrigatória. Não resuma, não escolha nem aja pelo personagem. Narre a situação em português do Brasil conforme o contrato do Mestre e termine deixando a decisão ao jogador.`;
 try{const result=await game.submitPlayerIntent(intentText);if(result?.ok===false)return result;return result?.ok===true?result:{ok:true,data:result}}catch(error){return {ok:false,message:String(error?.message||error)}}
}
'''
    runtime = runtime.replace(anchor, anchor + helper, 1)

    gate_old = "gate.querySelector('[data-start-adventure]')?.addEventListener('click',()=>{const b=gate.querySelector('[data-start-adventure]');b.disabled=true;b.textContent='INICIANDO…';const r=bridgeResult('startLivingAdventure',{});"
    gate_new = "gate.querySelector('[data-start-adventure]')?.addEventListener('click',async()=>{const b=gate.querySelector('[data-start-adventure]');b.disabled=true;b.textContent='INICIANDO…';const r=await startBarbaraOpening();"
    if gate_old not in runtime:
        raise SystemExit('primary start-adventure handler anchor not found')
    runtime = runtime.replace(gate_old, gate_new, 1)

    bar_old = "bar.onclick=()=>{const r=bridgeResult('startLivingAdventure',{});"
    bar_new = "bar.onclick=async()=>{const r=await startBarbaraOpening();"
    if bar_old not in runtime:
        raise SystemExit('secondary start-adventure handler anchor not found')
    runtime = runtime.replace(bar_old, bar_new, 1)

if 'A campanha começa em ${locationName}.' in controller or "narration: parts.join(' ')" in controller:
    raise SystemExit('short local opening template still present in controller')
if marker not in controller or marker not in runtime:
    raise SystemExit('Barbara opening marker missing after patch')
if "game.submitPlayerIntent(intentText)" not in runtime:
    raise SystemExit('runtime does not use canonical Master submitPlayerIntent channel')

controller_path.write_text(controller, encoding='utf-8')
runtime_path.write_text(runtime, encoding='utf-8')
print('R54_BARBARA_OPENING_MASTER_PASS local_template=0 master_channel=submitPlayerIntent')
