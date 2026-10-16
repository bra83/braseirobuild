import fs from 'node:fs';
import crypto from 'node:crypto';

const matrixPath=new URL('../rules/rules_matrix.json',import.meta.url);
const corpusPath=new URL('../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs',import.meta.url);
const manifestPath=new URL('../rules/rag_manifest.json',import.meta.url);
const raw=fs.readFileSync(matrixPath);
const matrix=JSON.parse(raw.toString('utf8'));
const rows=matrix.map(x=>({
  id:x.regra_id,status:x.status,title:x.secao || x.regra_id,summary:x.resultado || '',inputs:x.entradas || '',source:x.fonte || '',edition:x.edicao || '',page:x.pagina_pdf ?? null,printedPage:x.pagina_impressa ?? null,function:x.funcao ?? null,test:x.teste ?? null,official:x.oficial!==false
}));
const moduleText=`// GERADO a partir de rules/rules_matrix.json. Não editar manualmente.\nexport const RULE_CORPUS_VERSION = 2;\nexport const VERIFIED_RULE_CORPUS = ${JSON.stringify(rows,null,2)};\n`;
fs.writeFileSync(corpusPath,moduleText,'utf8');
const statuses=Object.fromEntries([...new Set(rows.map(r=>r.status))].sort().map(status=>[status,rows.filter(r=>r.status===status).length]));
fs.writeFileSync(manifestPath,JSON.stringify({version:2,source:'rules/rules_matrix.json',sourceSha256:crypto.createHash('sha256').update(raw).digest('hex'),documentCount:rows.length,statuses},null,2)+'\n','utf8');
console.log(`RAG corpus: ${rows.length} regras; manifest atualizado.`);
