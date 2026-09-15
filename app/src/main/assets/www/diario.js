import { NativePersistencePort, MemoryPersistenceAdapter } from './core/persistence-port.js';
import { ensureStarterCampaign } from './core/starter-bootstrap.js';
import { resolveActiveCampaignId, setRuntimeActiveCampaignId } from './core/runtime-campaign.js';
import { installSharedBottomNav } from './core/shared-navigation.js';

const pack=await fetch('data/wwn.starter-region.v1.json').then(r=>r.json());
const canonicalIria=await fetch('data/wave6.iria.canonical.fixture.json').then(r=>r.json());
const persistence=globalThis.BraseiroNative?new NativePersistencePort():new MemoryPersistenceAdapter();
const boot=ensureStarterCampaign({persistence,character:canonicalIria,pack});
if(!String(boot.status).startsWith('OK'))throw new Error(`Starter boot failed: ${boot.status}`);
const campaignId=resolveActiveCampaignId(persistence)??boot.campaignId;setRuntimeActiveCampaignId(campaignId);
const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')throw new Error(`Campaign load failed: ${loaded.status}`);
const eventsResult=persistence.listEvents(campaignId);const events=eventsResult.status==='OK'?(eventsResult.events??[]):[];
const $=id=>document.getElementById(id);
$('campaignName').textContent=loaded.displayName??campaignId;
$('eventCount').textContent=`${events.length} EVENTO${events.length===1?'':'S'}`;
function parsePayload(raw){try{return typeof raw==='string'?JSON.parse(raw):raw??{}}catch{return{}}}
function firstText(payload){for(const value of [payload?.text,payload?.summary,payload?.narrative?.text,payload?.resolution?.summary,payload?.result?.summary])if(typeof value==='string'&&value.trim())return value.trim();return''}
function labelFor(kind){return String(kind??'EVENTO').replaceAll('_',' ')}
function renderEvent(event){const payload=parsePayload(event.payloadJson);const article=document.createElement('article');const narrative=String(event.eventKind??payload.kind??'').toUpperCase()==='NARRATIVE'||payload.narrative===true;article.className='journal-entry'+(narrative?' narrative':'');const meta=document.createElement('div');meta.className='meta';const kind=document.createElement('span');kind.textContent=narrative?'NARRAÇÃO':labelFor(event.eventKind??payload.kind);const rev=document.createElement('span');rev.textContent=`REV ${event.revision??payload.revision??'—'}`;meta.append(kind,rev);const title=document.createElement('b');title.textContent=narrative?'Registro narrativo':labelFor(event.eventKind??payload.kind);const body=document.createElement('p');const text=firstText(payload);body.textContent=text||`Evento ${event.eventId??'sem identificador'} registrado sem resumo público adicional.`;article.append(meta,title,body);return article}
const list=$('journalList');for(const event of [...events].sort((a,b)=>(b.revision??0)-(a.revision??0)))list.appendChild(renderEvent(event));$('journalEmpty').classList.toggle('hidden',events.length!==0);
installSharedBottomNav({root:document.getElementById('app'),active:'diario',zIndex:900});
window.__BRASEIRO_DIARIO__={campaignId,eventCount:events.length,refresh:()=>location.reload()};
