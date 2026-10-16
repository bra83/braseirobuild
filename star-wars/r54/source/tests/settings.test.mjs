import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('configuração do Mestre remoto persiste somente endpoint, sem campo de chave',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'settings'));
  game.configureMasterProvider({mode:'http',endpoint:'https://mestre.example/api'});
  assert.equal(game.state.settings.masterProvider,'http');
  assert.equal(game.state.settings.masterEndpoint,'https://mestre.example/api');
  assert.equal('apiKey' in game.state.settings,false);
  const reopened=new GameController(new LocalStorageRepository(storage,'settings'));
  assert.equal(reopened.state.settings.masterEndpoint,'https://mestre.example/api');
});

test('modo local remove endpoint remoto persistido',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'settings-local'));
  game.configureMasterProvider({mode:'http',endpoint:'https://mestre.example/api'});
  game.configureMasterProvider({mode:'none'});
  assert.equal(game.state.settings.masterProvider,'none');
  assert.equal(game.state.settings.masterEndpoint,null);
});
