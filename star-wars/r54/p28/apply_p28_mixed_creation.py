#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
VTT=ROOT/'app/src/main/assets/vtt'
ui=VTT/'js/core/uiBridge.bundle.js'
rt=VTT/'r46/final-runtime.js'
for p in (ui,rt):
    if not p.is_file(): raise SystemExit(f'FAIL: ausente {p}')

def replace_js_block(text, signature, replacement):
    start=text.find(signature)
    if start<0: raise SystemExit('FAIL: assinatura não encontrada: '+signature)
    brace=text.find('{',start)
    depth=0; quote=None; esc=False; template_expr=0
    for i in range(brace,len(text)):
        ch=text[i]
        if quote:
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch==quote: quote=None
            continue
        if ch in ("'",'"','`'): quote=ch; continue
        if ch=='{': depth+=1
        elif ch=='}':
            depth-=1
            if depth==0: return text[:start]+replacement+text[i+1:]
    raise SystemExit('FAIL: bloco JS sem fechamento: '+signature)

src=ui.read_text(encoding='utf-8')
marker='R54_P28_MIXED_CREATION'
if marker not in src:
    catalog=r'''    creationCatalog() {
        // R54_P28_MIXED_CREATION — MIXED expõe as duas origens sem somar economias de criação.
        try {
            const state=this.repository.load();
            const mode=String(state?.campaignMode ?? 'EOTE').toUpperCase();
            const useAor=mode==='AOR';
            const mapSpecies=(source,profile,labelPrefix='')=>Object.entries(source).map(([key,value])=>({
                key:profile?profile+'::'+key:key,label:(labelPrefix?labelPrefix+' · ':'')+value.label,profile:profile||null,
                characteristics:clone(value.characteristics),soakBonus:value.soakBonus??0,woundBase:value.woundBase,strainBase:value.strainBase,xp:value.xp,
                careerFreeCount:value.careerFreeCount??4,specializationFreeCount:value.specFreeCount??2,freeSkill:value.freeSkill?clone(value.freeSkill):null,
                freeTalents:clone(value.freeTalents??[]),speciesChoice:value.speciesChoice?clone(value.speciesChoice):null,abilities:clone(value.abilities??[])
            }));
            const mapCareers=(source,profile,labelPrefix='')=>Object.entries(source).map(([key,value])=>({
                key:profile?profile+'::'+key:key,label:(labelPrefix?labelPrefix+' · ':'')+value.label,profile:profile||null,skills:clone(value.skills),
                specializations:Object.entries(value.specializations??{}).map(([specializationKey,specialization])=>({key:specializationKey,label:specialization.label,skills:clone(specialization.skills)}))
            }));
            if(mode==='MIXED'){
                return ok({
                    profile:'MIXED',activeProfile:'MIXED',campaignMode:mode,mixedProfiles:['EOTE_CORE','AOR_CORE'],
                    species:[...mapSpecies(eoteCharacterCreation_mjs_1.EOTE_SPECIES,'EOTE_CORE','EotE'),...mapSpecies(characterCreation_mjs_1.AOR_SPECIES,'AOR_CORE','AoR')],
                    careers:[...mapCareers(eoteCharacterCreation_mjs_1.EOTE_CAREERS,'EOTE_CORE','EotE'),...mapCareers(characterCreation_mjs_1.AOR_CAREERS,'AOR_CORE','AoR')],
                    obligationTypes:[
                        ...clone(eoteCharacterCreation_mjs_1.OBLIGATION_TYPES_PT_BR).map(v=>'Obrigação · '+v),
                        ...clone(characterCreation_mjs_1.DUTY_TYPES_PT_BR).map(v=>'Dever · '+v)
                    ],
                    obligationOnlyTypes:clone(eoteCharacterCreation_mjs_1.OBLIGATION_TYPES_PT_BR),
                    dutyTypes:clone(characterCreation_mjs_1.DUTY_TYPES_PT_BR),
                    characteristicLabels:clone(labelsPtBr_mjs_1.CHARACTERISTIC_LABELS_PT_BR),skillLabels:clone(labelsPtBr_mjs_1.SKILL_LABELS_PT_BR)
                });
            }
            const speciesSource=useAor?characterCreation_mjs_1.AOR_SPECIES:eoteCharacterCreation_mjs_1.EOTE_SPECIES;
            const careerSource=useAor?characterCreation_mjs_1.AOR_CAREERS:eoteCharacterCreation_mjs_1.EOTE_CAREERS;
            return ok({profile:'EOTE_CORE',activeProfile:useAor?'AOR_CORE':'EOTE_CORE',campaignMode:mode,species:mapSpecies(speciesSource,null),careers:mapCareers(careerSource,null),
                obligationTypes:clone(useAor?characterCreation_mjs_1.DUTY_TYPES_PT_BR:eoteCharacterCreation_mjs_1.OBLIGATION_TYPES_PT_BR),
                characteristicLabels:clone(labelsPtBr_mjs_1.CHARACTERISTIC_LABELS_PT_BR),skillLabels:clone(labelsPtBr_mjs_1.SKILL_LABELS_PT_BR)});
        } catch(error){return fail('CREATION_CATALOG_FAILED',messageOf(error));}
    }'''
    src=replace_js_block(src,'    creationCatalog() {',catalog)

    preview=r'''    creationPreview(options = {}) {
        try {
            const state=this.repository.load();
            const mode=String(state?.campaignMode ?? 'EOTE').toUpperCase();
            const prefix=value=>{const m=/^(EOTE_CORE|AOR_CORE)::(.+)$/.exec(String(value??''));return m?{profile:m[1],key:m[2]}:null};
            const aorResult=opts=>{const mapped={...clone(opts),dutyType:opts.obligationType,dutyReductions:clone(opts.obligationBenefits??{})};const p=characterCreation_mjs_1.creationPreview(mapped);if(!p)return fail('INVALID_INPUT','Selecione uma espécie válida para calcular o orçamento de criação.');return ok({xpBudget:p.xpBudget,characteristicCost:p.characteristicCost,xpRemaining:p.xpRemaining,credits:p.credits,obligation:p.duty,startingObligation:p.duty+p.dutySpent,additionalObligation:-p.dutySpent,obligationOfficial:p.dutyOfficial,obligationNote:p.dutyNote??null,speciesXpBonus:0,activeProfile:'AOR_CORE',duty:p.duty,dutySpent:p.dutySpent,mixed:mode==='MIXED'});};
            if(mode==='MIXED'){
                const sp=prefix(options.speciesKey),cp=prefix(options.careerKey);
                if(!sp||!cp||sp.profile!==cp.profile)return fail('INVALID_INPUT','Em campanha MISTA, espécie e carreira devem usar a mesma origem de criação: EotE ou AoR.');
                const rawType=String(options.obligationType??'');
                const native={...clone(options),speciesKey:sp.key,careerKey:cp.key};
                if(sp.profile==='AOR_CORE'){
                    if(rawType&& !rawType.startsWith('Dever · '))return fail('INVALID_INPUT','Escolha um Dever AoR para uma criação AoR.');
                    native.obligationType=rawType.replace(/^Dever · /,'');
                    return aorResult(native);
                }
                if(rawType&& !rawType.startsWith('Obrigação · '))return fail('INVALID_INPUT','Escolha uma Obrigação EotE para uma criação EotE.');
                native.obligationType=rawType.replace(/^Obrigação · /,'');
                const p=eoteCharacterCreation_mjs_1.eoteCreationPreview(native);
                if(!p)return fail('INVALID_INPUT','Selecione uma espécie válida para calcular o orçamento de criação.');
                return ok({...clone(p),activeProfile:'EOTE_CORE',mixed:true});
            }
            if(mode!=='AOR'){const p=eoteCharacterCreation_mjs_1.eoteCreationPreview(options);if(!p)return fail('INVALID_INPUT','Selecione uma espécie válida para calcular o orçamento de criação.');return ok(clone(p));}
            return aorResult(options);
        } catch(error){return fail('CREATION_PREVIEW_FAILED',messageOf(error));}
    }'''
    src=replace_js_block(src,'    creationPreview(options = {}) {',preview)

    create=r'''    createCustomCharacter(options = {}, { createSnapshot = true } = {}) {
        const controllerResult=this._controllerForExistingCampaign();if(!controllerResult.ok)return controllerResult;
        const game=controllerResult.data;let snapshotId=null;
        try {
            const mode=String(game.state.campaignMode??'EOTE').toUpperCase();
            const prefix=value=>{const m=/^(EOTE_CORE|AOR_CORE)::(.+)$/.exec(String(value??''));return m?{profile:m[1],key:m[2]}:null};
            let origin=mode==='AOR'?'AOR_CORE':'EOTE_CORE',native=clone(options);
            if(mode==='MIXED'){
                const sp=prefix(options.speciesKey),cp=prefix(options.careerKey);
                if(!sp||!cp||sp.profile!==cp.profile)throw new Error('Em campanha MISTA, espécie e carreira devem usar a mesma origem de criação.');
                origin=sp.profile;native.speciesKey=sp.key;native.careerKey=cp.key;
                const rawType=String(options.obligationType??'');
                if(origin==='AOR_CORE'){
                    if(rawType&&!rawType.startsWith('Dever · '))throw new Error('Escolha um Dever AoR para uma criação AoR.');
                    native.obligationType=rawType.replace(/^Dever · /,'');
                } else {
                    if(rawType&&!rawType.startsWith('Obrigação · '))throw new Error('Escolha uma Obrigação EotE para uma criação EotE.');
                    native.obligationType=rawType.replace(/^Obrigação · /,'');
                }
            }
            let mapped=null;
            if(origin==='AOR_CORE'){mapped={...clone(native),dutyType:native.obligationType,dutyReductions:clone(native.obligationBenefits??{})};characterCreation_mjs_1.createAorCoreCharacter(mapped);}
            else eoteCharacterCreation_mjs_1.createEoteCoreCharacter(native);
            if(createSnapshot&&game.state.player?.character)snapshotId=this.snapshots.create(game.state,'Antes de criar ficha manual').id;
            const created=origin==='AOR_CORE'?game.createAorCharacter(mapped):game.createCustomCharacter(native);
            return ok({character:clone(created),snapshotId,state:clone(game.state),creationOrigin:origin,campaignMode:mode});
        } catch(error){if(snapshotId){try{this.snapshots.remove(snapshotId)}catch{}}return fail('CUSTOM_CHARACTER_FAILED',messageOf(error));}
    }'''
    src=replace_js_block(src,'    createCustomCharacter(options = {}, { createSnapshot = true } = {}) {',create)
    ui.write_text(src,encoding='utf-8')

rts=rt.read_text(encoding='utf-8')
relabel=r'''function relabelCreationForCampaign(){
 const mode=campaignMode(),host=document.body;if(!host)return;host.dataset.campaignMode=mode;
 const cfg={EOTE:{bond:'Obrigação',type:'Tipo de Obrigação',pick:'Selecione uma Obrigação'},AOR:{bond:'Dever',type:'Tipo de Dever',pick:'Selecione um Dever'},MIXED:{bond:'Obrigação e Dever',type:'Origem e tipo do vínculo',pick:'Escolha Obrigação (EotE) ou Dever (AoR)'}}[mode]||{bond:'Obrigação',type:'Tipo de Obrigação',pick:'Selecione uma Obrigação'};
 const replacements=new Map([
  ['Obligation / vínculo de campanha',cfg.bond+' / vínculo de campanha'],['Tipo de Obligation',cfg.type],['Selecione uma Obligation',cfg.pick],
  ['Obligation inicial',mode==='AOR'?'Dever inicial':mode==='MIXED'?'Vínculo inicial':'Obrigação inicial'],['Obligation adicional',mode==='AOR'?'Dever gasto':mode==='MIXED'?'Ajuste do vínculo':'Obrigação adicional'],['Obligation final',mode==='AOR'?'Dever final':mode==='MIXED'?'Vínculo final':'Obrigação final'],
  ['Obligation',mode==='AOR'?'Dever':mode==='MIXED'?'Vínculo de campanha':'Obrigação'],['Duty','Dever'],['Contribution','Contribuição']
 ]);
 const walker=document.createTreeWalker(host,NodeFilter.SHOW_TEXT);let n;while((n=walker.nextNode())){const raw=String(n.nodeValue||''),v=raw.trim();if(replacements.has(v))n.nodeValue=raw.replace(v,replacements.get(v));}
 window.__BRASEIRO_CAMPAIGN_UI__={mode,obligation:mode!=='AOR',duty:mode!=='EOTE',contribution:mode!=='EOTE',mixedCreationOrigins:mode==='MIXED'?['EOTE_CORE','AOR_CORE']:[]};
}'''
rts=replace_js_block(rts,'function relabelCreationForCampaign(){',relabel)
rt.write_text(rts,encoding='utf-8')
print('P28_MIXED_CREATION_PATCHED',ui,rt)
