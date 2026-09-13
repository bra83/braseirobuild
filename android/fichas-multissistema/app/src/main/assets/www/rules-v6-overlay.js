(function(global){
'use strict';
const base=global.BRASEIRO_RULES;if(!base)return;
const VERSION='3.1.0';
const filled=v=>!(v===undefined||v===null||String(v).trim()==='');
const num=v=>v===''||v===null||v===undefined?NaN:Number(v);
const traitIds=['chaste_lustful','energetic_lazy','forgiving_vengeful','generous_selfish','honest_deceitful','just_arbitrary','merciful_cruel','modest_proud','pious_worldly','prudent_reckless','temperate_indulgent','trusting_suspicious','valorous_cowardly'];
function pendragon(s={},strict=false){
 const r={engineVersion:VERSION,status:'VALID',errors:[],warnings:[],missing:[],needsReferee:[],info:[],derived:{},sources:['Pendragon 6e Core Rulebook — Creating Your Player-knight pp.39–60; Personal Information p.43']};
 const required=[['name','Nome'],['culture','Cultura'],['religion','Religião'],['homeland','Terra Natal'],['siz','TAM'],['dex','DES'],['str','FOR'],['con','CON'],['app','APA'],['passion_primary','Paixão principal']];
 for(const [id,label] of required)if(!filled(s[id]))r.errors.push(`${label} é obrigatório.`);
 if(filled(s.culture)&&s.culture!=='Cymric')r.errors.push('O perfil Core 6e desta build usa a cultura Cymric; outras culturas exigem o suplemento/campanha correspondente.');
 if(filled(s.religion)&&!['Cristão','Pagão'].includes(s.religion))r.errors.push('O perfil inicial do Core 6e permite Cristão ou Pagão.');
 if(filled(s.homeland)&&s.homeland!=='Salisbury')r.errors.push('O perfil Core 6e desta build usa Salisbury como Terra Natal; outras terras exigem suplemento/campanha correspondente.');
 for(const id of ['siz','dex','str','con','app'])if(filled(s[id])&&(!Number.isFinite(num(s[id]))||num(s[id])<1||num(s[id])>20))r.errors.push(`${id.toUpperCase()} deve ficar entre 1 e 20.`);
 for(const id of traitIds){if(!filled(s[id]))r.errors.push(`${id} é obrigatório.`);else if(!Number.isFinite(num(s[id]))||num(s[id])<0||num(s[id])>20)r.errors.push(`${id} deve ficar entre 0 e 20.`);else r.info.push(`${id}: valor oposto ${20-num(s[id])}.`)}
 r.info.unshift('Perfil Core 6e: cavaleiro Cymric de Salisbury; religião Cristã ou Pagã.');
 if(r.errors.length)r.status='INVALID';
 return r;
}
function validate(system,s={},opts={}){if(system==='pendragon')return pendragon(s,!!opts.strict);const r=base.validate(system,s,opts);r.engineVersion=VERSION;return r;}
function auditImport(payload,currentSystem){if(!payload||payload.schema!=='braseiro.multisheet.v3')return base.auditImport(payload,currentSystem);if(currentSystem&&payload.system!==currentSystem)return base.auditImport(payload,currentSystem);return validate(payload.system,payload.character||{},{strict:true});}
global.BRASEIRO_RULES=Object.freeze({...base,VERSION,validate,auditImport});
})(globalThis);
