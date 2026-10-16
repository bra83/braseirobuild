import test from 'node:test';
import assert from 'node:assert/strict';
import { searchVerifiedRules, ruleRetrievalStatus, verifiedCorpusGapForQuery } from '../app/src/main/assets/vtt/js/rag/lexicalSearch.mjs';
import { VERIFIED_RULE_CORPUS } from '../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs';

test('RAG lexical encontra Obligation inicial EotE e preserva autoridade',()=>{
  const results=searchVerifiedRules('Obligation inicial EotE quantidade personagens benefícios PE créditos',{limit:5});
  assert.equal(results[0]?.id,'SW-EOTE-OBLIGATION-001');
  assert.equal(results[0]?.official,true);
  assert.equal(results[0]?.status,'TESTADO');
  assert.match(results[0]?.source ?? '',/Edge of the Empire/i);
  assert.equal(ruleRetrievalStatus(results).grounded,true);
});

test('RAG lexical encontra estrutura EotE 8/6/18',()=>{
  const results=searchVerifiedRules('estrutura EotE 8 espécies 6 carreiras 18 especializações',{limit:5});
  assert.equal(results[0]?.id,'SW-EOTE-CHAR-001');
  assert.match(results[0]?.summary ?? '',/8 espécies/i);
  assert.match(results[0]?.summary ?? '',/6 carreiras/i);
  assert.match(results[0]?.summary ?? '',/18 especializações/i);
});

test('RAG EotE apresenta raridade/disponibilidade auditada pelo Core e sem AoR',()=>{
  const results=searchVerifiedRules('raridade disponibilidade mercado negro modificador',{limit:10});
  assert.ok(results.some(r=>r.id==='SW-EOTE-RARITY-AVAILABILITY-001'));
  assert.equal(results.some(r=>String(r.id).startsWith('SW-AOR-')),false);
  assert.equal(ruleRetrievalStatus(results).grounded,true);
});

test('dado da Força é autoridade mecânica EotE Core auditada no RAG',()=>{
  assert.equal(verifiedCorpusGapForQuery('dado da Força pips luminosos sombrios'),null);
  const results=searchVerifiedRules('dado da Força pips luminosos sombrios',{limit:8});
  assert.ok(results.some(r=>r.id==='SW-EOTE-FORCE-DIE-001'));
  assert.equal(ruleRetrievalStatus(results).grounded,true);
});

test('compra de especialização adicional é autoridade mecânica EotE Core auditada no RAG',()=>{
  assert.equal(verifiedCorpusGapForQuery('comprar especialização adicional custo'),null);
  const results=searchVerifiedRules('comprar especialização adicional custo',{limit:8});
  assert.ok(results.some(r=>r.id==='SW-EOTE-SPECIALIZATION-PURCHASE-001'));
  assert.equal(ruleRetrievalStatus(results).grounded,true);
});

test('corpus ativo mantém AoR restrito ao subconjunto Duty/Contribution explicitamente auditado',()=>{
  const allowed=new Set(['SW-AOR-CHAR-DUTY-002','SW-AOR-BASE-DUTY-CONTRIBUTION-001']);
  const aor=VERIFIED_RULE_CORPUS.filter(r=>String(r.id).startsWith('SW-AOR-'));
  assert.deepEqual(aor.map(r=>r.id).sort(),[...allowed].sort());
  assert.ok(aor.every(r=>r.official===true));
  const runtime=VERIFIED_RULE_CORPUS.find(r=>r.id==='VTT-AOR-DUTY-RUNTIME-001');
  assert.equal(runtime?.official,false);
});

test('políticas operacionais do VTT permanecem explicitamente não oficiais',()=>{
  const results=searchVerifiedRules('fluxo sessão abertura decisão encerramento Mestre',{limit:6});
  const flow=results.find(x=>x.id==='SW-VTT-SESSION-FLOW-001');
  assert.ok(flow);
  assert.equal(flow.official,false);
});

test('RAG encontra os três poderes da Força do Core EotE',()=>{
  for(const [query,id] of [['Sentir Sense seres vivos pensamentos','SW-EOTE-FORCE-SENSE-001'],['Influenciar Influence emoções tensão mente','SW-EOTE-FORCE-INFLUENCE-001'],['Mover Move silhueta arremessar objetos','SW-EOTE-FORCE-MOVE-001']]){
    const results=searchVerifiedRules(query,{limit:10});
    assert.ok(results.some(r=>r.id===id),`regra ausente no RAG: ${id}`);
    assert.equal(ruleRetrievalStatus(results).grounded,true);
  }
});
