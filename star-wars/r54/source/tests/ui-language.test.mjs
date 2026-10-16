import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');

test('interface principal declara português do Brasil e usa rótulos PT-BR',()=>{
  assert.match(html,/lang="pt-BR"/);
  for(const label of ['Cena','Personagem','O que você faz?','Resolver uma ação','Perícia','Dificuldade','Contratempo','Importar','Exportar','Combate','Manobras','Ataque','Recuperação','Consultar regras verificadas','Ajustes','Mestre do jogo']) assert.ok(html.includes(label),`faltou ${label}`);
});

test('interface principal não expõe rótulos espanhóis conhecidos',()=>{
  for(const word of ['HOJA DE PERSONAJE','HABILIDADES','HERIDAS','TENSIÓN','DINERO','PROFESIÓN']) assert.equal(html.includes(word),false,`rótulo espanhol visível: ${word}`);
});
