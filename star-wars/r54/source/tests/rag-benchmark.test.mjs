import test from 'node:test';
import assert from 'node:assert/strict';
import { searchVerifiedRules } from '../app/src/main/assets/vtt/js/rag/lexicalSearch.mjs';

const CASES=[
  ['Obligation inicial quantidade personagens benefícios PE créditos','SW-EOTE-OBLIGATION-001'],
  ['estrutura EotE 8 espécies 6 carreiras 18 especializações','SW-EOTE-CHAR-001'],
  ['18 árvores de talentos 360 posições custos 5 10 15 20 25','SW-EOTE-TALENT-TREES-001'],
  ['stimpack primeiro 5 depois 4 3 2 1','SW-EOTE-STIMPACK-001'],
  ['Recuperação rápida tensão depois do encontro','SW-EOTE-TALENT-RECOVERY-001'],
  ['Alvo ardiloso silhueta veículo','SW-EOTE-TALENT-VEHICLE-001'],
  ['Navegador mestre Astronavegação dificuldade','SW-EOTE-TALENT-ASTROGATOR-001'],
  ['Command Liderança dado de Impulso','SW-EOTE-TALENT-COMMAND-001'],
  ['facções persistentes objetivos relações consequências do mundo','VTT-WORLD-FACTIONS-001'],
  ['plano de facção por etapas persistentes avanço explícito','VTT-WORLD-FACTION-PLANS-001'],
  ['adversário de encontro ligado a facção persistente','VTT-COMBAT-FACTION-AFFILIATION-001'],
  ['histórico persistente de encontro e desfecho causal','VTT-WORLD-ENCOUNTER-HISTORY-001'],
  ['consequência duradoura aftermath pós conflito','VTT-WORLD-AFTERMATH-001']
];

test('benchmark RAG EotE recupera a entrada esperada em primeiro lugar',()=>{
  const failures=[];
  for(const [query,expected] of CASES){
    const got=searchVerifiedRules(query,{limit:1})[0]?.id ?? null;
    if(got!==expected) failures.push({query,expected,got});
  }
  assert.deepEqual(failures,[]);
});
