import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {validateMatrixBundle} from './matrix-engine.mjs';
/** Read only declared local assets. No arbitrary file paths from imported adventure JSON. */
export async function loadMatrixBundle(packId){
 const directory=new URL('../data/matrices/',import.meta.url),catalog=JSON.parse(await readFile(new URL('catalog.json',directory),'utf8'));
 const pack=catalog.packs.find(p=>p.id===packId);if(!pack)throw Error('Pacote de sistema inexistente');
 const tables={};for(const [axis,id] of Object.entries(pack.tables)){
  const descriptor=catalog.tables.find(t=>t.id===id);if(!descriptor)throw Error('Tabela ausente');
  const body=await readFile(new URL(descriptor.file,directory));
  if(createHash('sha256').update(body).digest('hex')!==descriptor.sha256)throw Error('Checksum da tabela divergente');
  tables[axis]=JSON.parse(body).rows;
 }
 return validateMatrixBundle({schema:'braseiro.matrix-bundle/v1',pack:{...pack,version:catalog.version},tables});
}
