import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
const app=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');

test('interface possui aba Biblioteca e fluxo de importação em PT-BR',()=>{
  assert.match(html,/data-view="libraryView">Biblioteca</);
  assert.match(html,/Importar PDF/);
  assert.match(html,/Buscar na Biblioteca/);
  assert.match(html,/armazenamento privado do aplicativo/);
});

test('interface diferencia PDF indexado de PDF anexado sem texto extraível',()=>{
  assert.match(app,/ANEXADO_SEM_TEXTO|não contém texto extraível/);
  assert.match(app,/Pronto para uso offline|libraryStatusLabel/);
});

test('Biblioteca expõe autoridade e ativação da fonte em PT-BR',()=>{
  const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
  const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
  assert.match(html,/Errata → Livro básico → Complemento → Aventura\/Cenário → Referência/);
  assert.match(js,/Complemento de regras/);
  assert.match(js,/Usar no Mestre/);
  assert.match(js,/updateLibraryDocumentPolicy/);
});

test('Biblioteca oferece reindexação sem exigir nova importação',()=>{
  const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
  assert.match(js,/Reindexar/);
  assert.match(js,/reindexLibraryDocument/);
  assert.match(js,/O PDF permanece disponível durante o processo/);
});
