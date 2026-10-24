import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {validateMatrixBundle} from './matrix-engine.mjs';
/** Read only declared local assets. No arbitrary file paths from imported adventure JSON. */
export async function loadMatrixBundle(packId){
 const directory=new URL('../data/matrices/',import.meta.url);
 let catalog;
 if(typeof packId==='string'&&packId.startsWith('ose-')){
  try{catalog=JSON.parse(await readFile(new URL('catalog-ose.json',directory),'utf8'));}catch{}
 }
 if(!catalog)catalog=JSON.parse(await readFile(new URL('catalog.json',directory),'utf8'));
 const pack=catalog.packs.find(p=>p.id===packId);if(!pack)throw Error('Pacote de sistema inexistente');
 const tables={};for(const [axis,id] of Object.entries(pack.tables)){
  const descriptor=catalog.tables.find(t=>t.id===id);if(!descriptor)throw Error('Tabela ausente');
  const body=await readFile(new URL(descriptor.file,directory));
  const rawHash=createHash('sha256').update(body).digest('hex');
  if(rawHash!==descriptor.sha256){
   const lfHash=createHash('sha256').update(Buffer.from(body.toString('utf8').replace(/\r\n/g,'\n'),'utf8')).digest('hex');
   const crlfHash=createHash('sha256').update(Buffer.from(body.toString('utf8').replace(/\r\n/g,'\n').replace(/\n/g,'\r\n'),'utf8')).digest('hex');
   if(lfHash!==descriptor.sha256 && crlfHash!==descriptor.sha256){
    throw Error('Checksum da tabela divergente');
   }
  }
  tables[axis]=JSON.parse(body).rows;
 }
 return validateMatrixBundle({schema:'braseiro.matrix-bundle/v1',pack:{...pack,version:catalog.version},tables});
}
