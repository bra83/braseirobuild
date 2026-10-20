/** Source-grounded Gollum rule Q&A. Read-only and never enters the campaign turn pipeline. */
import {getStoreData,type Manual,type ManualChunk} from './db';
import {mayProvideMechanics} from './sourceAuthority';
import {rankManualIndex,type ManualSearchIndexRecord} from './autonomy/ragIndex';
import {answerRulesQuestion} from '../services/geminiService';

export type GollumRuleReply={answer:string;sources:string[];uncertainty:string};
export async function askGollumRules(question:string):Promise<GollumRuleReply>{
 const prompt=String(question||'').trim().slice(0,500);
 if(!prompt)return {answer:'Faça uma pergunta sobre regras ou sobre esta interface, precioso.',sources:[],uncertainty:''};
 const manuals=await getStoreData<Manual>('manuals');
 const allowed=new Set(manuals.filter(m=>mayProvideMechanics(m)).map(m=>m.id));
 if(allowed.size===0)return {answer:'Ainda não encontro manuais oficiais de regras 2e indexados nesta biblioteca, precioso. Importe ou indexe o Livro Básico na aba Manuais.',sources:[],uncertainty:'Não há fontes consultáveis.'};
 const index=(await getStoreData<ManualSearchIndexRecord>('manual_search_index')).filter(item=>allowed.has(item.manualId));
 const priority=new Map(manuals.map(m=>[m.id,Number(m.priority)||4]));
 const ranked=rankManualIndex(index,[prompt],priority,8);
 if(!ranked.length)return {answer:'Não encontrei um trecho autorizado dos manuais para responder com segurança.',sources:[],uncertainty:'Verifique o Livro Básico na aba Manuais.'};
 const selected=new Set(ranked.map(r=>r.id));
 const chunks=(await getStoreData<ManualChunk>('manual_chunks')).filter(x=>selected.has(x.id)&&allowed.has(x.manualId));
 const byId=new Map(chunks.map(c=>[c.id,c]));
 const excerpts=ranked.map(r=>byId.get(r.id)).filter((x):x is ManualChunk=>Boolean(x)).slice(0,5);
 if(!excerpts.length)return {answer:'O índice de regras existe, mas os trechos ainda não estão acessíveis. Reindexe o manual.',sources:[],uncertainty:'Conteúdo indisponível.'};
 const sources=excerpts.map(c=>c.manualName+' — página '+(c.pageLabel||c.pageNum));
 const context='CONSULTA ESTRITAMENTE DE REGRAS DO JOGADOR. Não há acesso a planos do Mestre, cenas secretas ou aventuras. Responda SOMENTE usando os trechos numerados abaixo. Se eles forem insuficientes, declare incerteza.\n'+
 excerpts.map((c,i)=>'[FONTE '+(i+1)+'] '+sources[i]+'\n'+String(c.text||'').slice(0,1800)).join('\n\n');
 const response=await answerRulesQuestion(context,prompt);
 const refs=(response.usedSourceNumbers||[]).map(n=>sources[n-1]).filter((v):v is string=>Boolean(v));
 return {answer:String(response.answer||'Não encontrei uma regra suficiente para responder.'),sources:[...new Set(refs.length?refs:sources.slice(0,3))],uncertainty:String(response.uncertainty||'')};
}
