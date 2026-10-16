function text(v,max=2000){ return String(v ?? '').trim().slice(0,max); }
function int(v,label,{min=0,max=9999}={}){ const n=Number(v); if(!Number.isInteger(n)||n<min||n>max) throw new Error(`${label} deve estar entre ${min} e ${max}.`); return n; }
export function createRebelGroup(){ return {schemaVersion:1,base:{name:'Base de Operações',location:'',description:''},dutyEntries:[],contributionRank:0,resources:[],belongings:[],contacts:[],destiny:{light:0,dark:0}}; }
export function normalizeRebelGroup(value){
  const d=createRebelGroup(), v=value&&typeof value==='object'?value:{};
  return {schemaVersion:1,base:{...d.base,...(v.base??{})},dutyEntries:Array.isArray(v.dutyEntries)?v.dutyEntries:[],contributionRank:Number.isInteger(v.contributionRank)?Math.max(0,Math.min(10,v.contributionRank)):0,resources:Array.isArray(v.resources)?v.resources:[],belongings:Array.isArray(v.belongings)?v.belongings:[],contacts:Array.isArray(v.contacts)?v.contacts:[],destiny:{...d.destiny,...(v.destiny??{})}};
}
export function validateRebelGroup(value){
  const v=normalizeRebelGroup(value); int(v.contributionRank,'Faixa de Contribuição',{min:0,max:10}); int(Number(v.destiny.light),'Destino do Lado Luminoso',{min:0,max:20}); int(Number(v.destiny.dark),'Destino do Lado Sombrio',{min:0,max:20});
  for(const key of ['resources','belongings','contacts']) if(!Array.isArray(v[key])) throw new Error(`Campo ${key} inválido.`); return true;
}
export function updateBase(value,{name,location,description}={}){ const v=normalizeRebelGroup(value); v.base={name:text(name||v.base.name,120)||'Base de Operações',location:text(location,240),description:text(description,2000)}; return v; }
export function setContributionRank(value,rank){ const v=normalizeRebelGroup(value); v.contributionRank=int(Number(rank),'Faixa de Contribuição',{min:0,max:10}); return v; }
export function setDestinyReserve(value,{light,dark}){ const v=normalizeRebelGroup(value); v.destiny={light:int(Number(light),'Destino do Lado Luminoso',{min:0,max:20}),dark:int(Number(dark),'Destino do Lado Sombrio',{min:0,max:20})}; return v; }
export function setTextList(value,key,items){ if(!['resources','belongings','contacts'].includes(key)) throw new Error('Lista de grupo inválida.'); const v=normalizeRebelGroup(value); v[key]=(Array.isArray(items)?items:[]).map(x=>text(x,500)).filter(Boolean).slice(0,100); return v; }
export function addDutyEntry(value,entry={}){ const v=normalizeRebelGroup(value); const e={id:text(entry.id,100)||`duty_${Date.now()}`,value:int(Number(entry.value),'Valor de Dever',{min:0,max:1000}),member:text(entry.member,120),type:text(entry.type,160),description:text(entry.description,600),motivations:text(entry.motivations,600)}; v.dutyEntries.push(e); return v; }
export function removeDutyEntry(value,id){ const v=normalizeRebelGroup(value); v.dutyEntries=v.dutyEntries.filter(e=>e.id!==id); return v; }
export function groupDutyTotal(value){ return normalizeRebelGroup(value).dutyEntries.reduce((sum,e)=>sum+(Number(e.value)||0),0); }
