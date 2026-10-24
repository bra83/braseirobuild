import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import '../../vtt_life_bridge.js';

const VttLifeAdapter = globalThis.VttLifeAdapter;

function fixtureViewState({day = 0, turns = 0, receipts = {}, profile = 'OSE_CLASSIC_FANTASY'} = {}) {
    return {
        campaign: {
            id: 'mundo-campanha-01',
            profile: profile
        },
        time: {
            days: day,
            turns: turns
        },
        party: [
            { id: 'garrick', name: 'Garrick', classIds: ['FIGHTER'], hpCurrentNumerator: 8, hpMaxNumerator: 8 }
        ],
        selectedCharacterId: 'garrick',
        session: {
            phase: 'ACTIVE',
            reactionReceipts: receipts
        },
        lifeStore: null
    };
}

test('VttLifeAdapter inicializa estado canônico com perfil OSE e mundo isolado', () => {
    let synced = null;
    const adapter = new VttLifeAdapter({
        postNative: (type, payload) => {
            if (type === 'LifeStoreSync') synced = payload;
        }
    });

    const vs = fixtureViewState({ day: 0 });
    adapter.initFromViewState(vs);

    assert.ok(adapter.store, 'Store deve existir');
    assert.equal(adapter.store.legacy.system, 'ose-classic');
    assert.equal(adapter.store.legacy.worldId, 'mundo-campanha-01');
    assert.equal(adapter.store.legacy.timelineId, 'canonical');
});

test('Avanço de calendário e ação confirmada acionam Motor de Vida causalmente com rumor', () => {
    let synced = null;
    const adapter = new VttLifeAdapter({
        postNative: (type, payload) => {
            if (type === 'LifeStoreSync') synced = payload;
        }
    });

    const vs = fixtureViewState({
        day: 2,
        turns: 12,
        receipts: {
            'rcpt-01': {
                reactionId: 'rcpt-01',
                reactionText: 'Derrotou os esqueletos guardiões da cripta',
                actorCharacterId: 'garrick',
                mechanicalIntent: 'MELEE_ATTACK'
            }
        }
    });

    adapter.initFromViewState(vs);

    assert.equal(adapter.store.day, 2, 'Dia deve estar sincronizado com o tempo da campanha');
    assert.equal(adapter.store.events.length, 1, 'Evento confirmado deve estar registrado');
    assert.equal(adapter.store.events[0].id, 'rcpt-01');

    const rumors = adapter.getPublicRumors();
    assert.equal(rumors.length, 1, 'Deve gerar manifestação/rumor fora de cena');
    assert.ok(rumors[0].text.includes('Derrotou os esqueletos'), 'Rumor deve refletir o acontecimento verificado');
    assert.ok(synced, 'Deve ter sincronizado com o Room no Android');
});

test('Idempotência estrita: replay do mesmo receiptId não duplica eventos nem rumores', () => {
    const adapter = new VttLifeAdapter();
    const vs = fixtureViewState({
        day: 1,
        receipts: {
            'rcpt-idem': {
                reactionId: 'rcpt-idem',
                reactionText: 'Descansou na estalagem do vilarejo',
                actorCharacterId: 'garrick'
            }
        }
    });

    adapter.initFromViewState(vs);
    const revisionBefore = adapter.store.revision;
    const rngBefore = adapter.store.rng;
    const eventsBefore = adapter.store.events.length;
    const rumorsBefore = adapter.getPublicRumors().length;

    // Simula reabertura ou replay com o mesmo estado
    adapter.initFromViewState(vs);

    assert.equal(adapter.store.revision, revisionBefore, 'Revisão não deve mudar no replay idêntico');
    assert.equal(adapter.store.rng, rngBefore, 'RNG não deve avançar no replay idêntico');
    assert.equal(adapter.store.events.length, eventsBefore, 'Eventos não devem duplicar');
    assert.equal(adapter.getPublicRumors().length, rumorsBefore, 'Rumores não devem duplicar');
});

test('Transição de aventura preserva legado, arcos e rumores do personagem', () => {
    const adapter = new VttLifeAdapter();

    // Aventura 1: Classic
    const vs1 = fixtureViewState({
        day: 3,
        receipts: {
            'act-01': { reactionId: 'act-01', reactionText: 'Resgatou o relicário da cripta', actorCharacterId: 'garrick' }
        }
    });
    adapter.initFromViewState(vs1);
    assert.equal(adapter.store.events.length, 1);

    // Conclusão e início de Aventura 2: Advanced Fantasy
    const savedState = JSON.parse(JSON.stringify(adapter.store));
    const vs2 = fixtureViewState({
        day: 5,
        profile: 'OSE_ADVANCED_FANTASY',
        receipts: {
            'act-01': { reactionId: 'act-01', reactionText: 'Resgatou o relicário da cripta', actorCharacterId: 'garrick' },
            'act-02': { reactionId: 'act-02', reactionText: 'Chegou ao Enclave do Espelho', actorCharacterId: 'garrick' }
        }
    });
    vs2.lifeStore = savedState;

    adapter.initFromViewState(vs2);

    assert.equal(adapter.store.events.length, 2, 'Deve manter eventos da aventura anterior e somar o novo');
    assert.equal(adapter.store.events[0].id, 'act-01');
    assert.equal(adapter.store.events[1].id, 'act-02');
    assert.equal(adapter.getPublicRumors().length, 2, 'Rumores anteriores devem persistir');
});

test('Persistência e reabertura física 3x sem perda de estado ou duplicação', () => {
    let persistedPayload = null;
    const adapter = new VttLifeAdapter({
        postNative: (type, payload) => {
            if (type === 'LifeStoreSync') persistedPayload = JSON.parse(payload.stateJson);
        }
    });

    const vs = fixtureViewState({
        day: 4,
        receipts: {
            'pass-01': { reactionId: 'pass-01', reactionText: 'Cruzou o Pântano dos Suspiros', actorCharacterId: 'garrick' }
        }
    });
    adapter.initFromViewState(vs);
    assert.ok(persistedPayload);

    // Reabertura 1
    const adapter1 = new VttLifeAdapter();
    const vs1 = fixtureViewState({ day: 4, receipts: vs.session.reactionReceipts });
    vs1.lifeStore = persistedPayload;
    adapter1.initFromViewState(vs1);
    assert.equal(adapter1.store.events.length, 1);

    // Reabertura 2
    const adapter2 = new VttLifeAdapter();
    const vs2 = fixtureViewState({ day: 4, receipts: vs.session.reactionReceipts });
    vs2.lifeStore = adapter1.store;
    adapter2.initFromViewState(vs2);
    assert.equal(adapter2.store.events.length, 1);

    // Reabertura 3
    const adapter3 = new VttLifeAdapter();
    const vs3 = fixtureViewState({ day: 4, receipts: vs.session.reactionReceipts });
    vs3.lifeStore = adapter2.store;
    adapter3.initFromViewState(vs3);
    assert.equal(adapter3.store.events.length, 1);
    assert.equal(adapter3.getPublicRumors()[0].text.includes('Pântano dos Suspiros'), true);
});
