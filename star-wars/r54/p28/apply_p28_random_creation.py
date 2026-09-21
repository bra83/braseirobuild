#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
VTT=ROOT/'app/src/main/assets/vtt'
controller=VTT/'js/core/controller.mjs';rt=VTT/'r46/final-runtime.js'
for p in (controller,rt):
    if not p.is_file(): raise SystemExit(f'FAIL: ausente {p}')

def replace_js_block(text, signature, replacement):
    start=text.find(signature)
    if start<0: raise SystemExit('FAIL: assinatura não encontrada: '+signature)
    brace=start+len(signature)-1
    if brace>=len(text) or text[brace]!='{': raise SystemExit('FAIL: assinatura precisa terminar no corpo JS: '+signature)
    depth=0;quote=None;esc=False
    for i in range(brace,len(text)):
        ch=text[i]
        if quote:
            if esc:esc=False
            elif ch=='\\':esc=True
            elif ch==quote:quote=None
            continue
        if ch in ("'",'"','`'):quote=ch;continue
        if ch=='{':depth+=1
        elif ch=='}':
            depth-=1
            if depth==0:return text[:start]+replacement+text[i+1:]
    raise SystemExit('FAIL: bloco sem fechamento')

src=controller.read_text(encoding='utf-8')
marker='R54_P28_RANDOM_CREATION'
if marker not in src:
    build=r'''    buildRandomCharacter(options = {}) {
        // R54_P28_RANDOM_CREATION
        const mode=campaignMode(this.state);
        const requested=String(options.creationOrigin??(mode==='AOR'?'AOR_CORE':'EOTE_CORE')).toUpperCase();
        if(!['EOTE_CORE','AOR_CORE'].includes(requested))throw new Error('Origem aleatória inválida; use EOTE_CORE ou AOR_CORE.');
        if(mode==='EOTE'&&requested!=='EOTE_CORE')throw new Error('Campanha EotE exige criação EotE.');
        if(mode==='AOR'&&requested!=='AOR_CORE')throw new Error('Campanha AoR exige criação AoR.');
        return requested==='AOR_CORE'?characterCreation_mjs_1.createRandomAorCoreCharacter(options):eoteCharacterCreation_mjs_1.createRandomEoteCoreCharacter(options);
    }'''
    src=replace_js_block(src,'    buildRandomCharacter(options = {}) {',build)
    create=r'''    createRandomCharacter(options = {}) {
        const character=this.buildRandomCharacter(options);
        if(character?.creation?.rulesProfile==='AOR_CORE'){
            if(!modeAllowsDuty(this.state))throw new Error('Ative campanha AOR ou MIXED antes de criar personagem AoR.');
            this.setCharacter(character);
            const first=character.duties?.[0];
            if(first&&!(this.state.group.dutyEntries??[]).some(e=>String(e.member??'').toLocaleLowerCase('pt-BR')===String(character.name).toLocaleLowerCase('pt-BR')&&String(e.type??'')===String(first.type??''))){
                const group=fringeGroup_mjs_1.addDutyEntry(this.state.group,{value:first.value,member:character.name,type:first.type,description:first.description??''});
                this.commit('GROUP_UPDATED',{group});
            }
            return this.state.player.character;
        }
        return this.setCharacter(character);
    }'''
    src=replace_js_block(src,'    createRandomCharacter(options = {}) {',create)
    controller.write_text(src,encoding='utf-8')

r=rt.read_text(encoding='utf-8')
old="function quickRandomCharacter(statusEl){const id=randomCharacterIdentity();const r=bridgeResult('generateRandomCharacter',id,{createSnapshot:true});if(!r.ok){if(statusEl)statusEl.textContent=r.message||'Não foi possível gerar a ficha.';return false}if(statusEl)statusEl.textContent=`Ficha criada: ${id.name}.`;setTimeout(enhance,0);return true}"
new=r'''function quickRandomCharacter(statusEl){
 const id=randomCharacterIdentity(),mode=campaignMode();
 const run=origin=>{const result=bridgeResult('generateRandomCharacter',{...id,creationOrigin:origin},{createSnapshot:true});if(!result.ok){if(statusEl)statusEl.textContent=result.message||'Não foi possível gerar a ficha.';return false}if(statusEl)statusEl.textContent=`Ficha criada: ${id.name} · ${origin==='AOR_CORE'?'AoR':'EotE'}.`;setTimeout(enhance,0);return true};
 if(mode==='MIXED'&&statusEl){
   statusEl.innerHTML='<span>Escolha a origem da ficha aleatória:</span> <button type="button" data-random-origin="EOTE_CORE">EotE</button> <button type="button" data-random-origin="AOR_CORE">AoR</button>';
   statusEl.querySelectorAll('[data-random-origin]').forEach(b=>b.addEventListener('click',()=>run(b.dataset.randomOrigin),{once:true}));
   return false;
 }
 return run(mode==='AOR'?'AOR_CORE':'EOTE_CORE');
}'''
if old not in r: raise SystemExit('FAIL: quickRandomCharacter P27/P28 não encontrado')
r=r.replace(old,new,1)
rt.write_text(r,encoding='utf-8')
print('P28_RANDOM_CREATION_PATCHED',controller,rt)
