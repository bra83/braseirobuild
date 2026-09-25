(()=>{
function normalizeImportPayload(raw){
  if(raw?.schema==='braseiro.multisheet.v3'||raw?.schema==='braseiro.multisheet.v4')return {payload:raw,sourceLabel:null};
  const mapped=globalThis.BRASEIRO_IMPORT_ADAPTERS?.map?.(raw);
  if(!mapped)throw Error('Formato de ficha não reconhecido por esta build.');
  if(mapped.native)return {payload:mapped.payload,sourceLabel:null};
  const sys=SYSTEMS.find(x=>x.id===mapped.system);
  return {payload:{
    schema:'braseiro.multisheet.v4',
    engineVersion:RULES.VERSION,
    system:mapped.system,
    systemName:sys?.name||mapped.system,
    importedAt:new Date().toISOString(),
    importedFrom:mapped.sourceLabel||raw.format||raw.system||raw.profile||'externo',
    character:mapped.character||{}
  },sourceLabel:mapped.sourceLabel||null};
}
globalThis.normalizeImportPayload=normalizeImportPayload;
globalThis.importJson=function(file){
  if(!file)return;
  if(file.size>16*1024*1024){toast('Importação rejeitada: JSON maior que 16 MB.');return}
  const r=new FileReader();
  r.onload=()=>{
    try{
      const raw=JSON.parse(r.result),normalized=normalizeImportPayload(raw),p=normalized.payload;
      if(current&&p.system!==current.id)throw Error(`A ficha é ${p.system}, mas o perfil aberto é ${current.id}. Troca silenciosa é proibida.`);
      const audit=RULES.auditImport(p,null);
      if(!normalized.sourceLabel&&audit.status==='INVALID')throw Error(`${audit.status}: ${[...audit.errors,...audit.missing,...audit.needsReferee].join(' | ')}`);
      state[p.system]=p.character||{};
      saveState();
      openSystem(p.system);
      toast(normalized.sourceLabel?`${normalized.sourceLabel}: ficha mapeada (${audit.status})`:`Ficha importada e revalidada (${audit.status})`);
    }catch(e){toast('Importação rejeitada: '+e.message)}
  };
  r.onerror=()=>toast('Importação rejeitada: falha de leitura.');
  r.readAsText(file);
};
})();