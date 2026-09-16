import { NativePersistencePort, MemoryPersistenceAdapter } from './core/persistence-port.js';
import { ensureStarterCampaign } from './core/starter-bootstrap.js';
import { resolveActiveCampaignId, setRuntimeActiveCampaignId } from './core/runtime-campaign.js';
import { installSharedBottomNav } from './core/shared-navigation.js';
import { JournalService } from './core/journal-service.js';

const pack=await fetch('data/wwn.starter-region.v1.json').then(r=>r.json());
const canonicalIria=await fetch('data/wwn.starter-character.v1.json').then(r=>r.json());
const persistence=globalThis.BraseiroNative?new NativePersistencePort():new MemoryPersistenceAdapter();
const boot=ensureStarterCampaign({persistence,character:canonicalIria,pack});
if(!String(boot.status).startsWith('OK'))throw new Error(`Starter boot failed: ${boot.status}`);
const campaignId=resolveActiveCampaignId(persistence)??boot.campaignId;setRuntimeActiveCampaignId(campaignId);
const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')throw new Error(`Campaign load failed: ${loaded.status}`);
const journal=new JournalService({persistence});
const $=id=>document.getElementById(id);
$('campaignName').textContent=loaded.displayName??campaignId;
function parsePayload(raw){try{return typeof raw==='string'?JSON.parse(raw):raw??{}}catch{return{}}}
function firstText(payload){for(const value of [payload?.text,payload?.summary,payload?.narrative?.text,payload?.resolution?.summary,payload?.result?.summary])if(typeof value==='string'&&value.trim())return value.trim();return''}
function labelFor(kind){return String(kind??'EVENTO').replaceAll('_',' ')}
function renderEvent(event){const payload=parsePayload(event.payloadJson);const article=document.createElement('article');const narrative=String(event.eventKind??payload.kind??'').toUpperCase()==='NARRATIVE'||payload.narrative===true;article.className='journal-entry'+(narrative?' narrative':'');const meta=document.createElement('div');meta.className='meta';const kind=document.createElement('span');kind.textContent=narrative?'NARRAÇÃO':labelFor(event.eventKind??payload.kind);const rev=document.createElement('span');rev.textContent=`REV ${event.revision??payload.revision??'—'}`;meta.append(kind,rev);const title=document.createElement('b');title.textContent=narrative?'Registro narrativo':labelFor(event.eventKind??payload.kind);const body=document.createElement('p');const text=firstText(payload);body.textContent=text||`Evento ${event.eventId??'sem identificador'} registrado sem resumo público adicional.`;article.append(meta,title,body);return article}
function refresh(){const kind=$('journalKind').value;let kinds=null;if(kind==='NARRATIVE')kinds=['NARRATIVE'];const result=journal.list({campaignId,query:$('journalSearch').value,kinds,limit:1000});let events=result.events??[];if(kind==='MECHANICAL')events=events.filter(e=>String(e.eventKind??'').toUpperCase()!=='NARRATIVE');const list=$('journalList');list.replaceChildren();for(const event of events)list.appendChild(renderEvent(event));$('eventCount').textContent=`${events.length} EVENTO${events.length===1?'':'S'}`;$('journalEmpty').classList.toggle('hidden',events.length!==0);return result}
$('journalSearch').addEventListener('input',refresh);$('journalKind').addEventListener('change',refresh);refresh();
installSharedBottomNav({root:document.getElementById('app'),active:'diario',zIndex:900});
window.__BRASEIRO_DIARIO__={campaignId,journal,refresh};
