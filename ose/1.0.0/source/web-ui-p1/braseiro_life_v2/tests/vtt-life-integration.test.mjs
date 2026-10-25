import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import '../../vtt_life_bridge.js';
import '../../dice3d_core.js';

const VttLifeAdapter = globalThis.VttLifeAdapter;
const Dice3DCore = globalThis.Dice3DCore;

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
                mechanicalMutation: true,
                traceStatus: 'OFFICIAL_RULES',
                mechanicalFeedback: 'Esqueletos derrotados. 50 XP concedidos.'
            }
        }
    });

    adapter.initFromViewState(vs);

    assert.equal(adapter.store.day, 2, 'Dia deve estar sincronizado com o tempo da campanha');
    assert.equal(adapter.store.events.length, 1, 'Evento confirmado deve estar registrado');
    assert.equal(adapter.store.events[0].id, 'rcpt-01');
    assert.equal(adapter.store.events[0].lifeCauseId, 'cause-victory-1');

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
                actorCharacterId: 'garrick',
                mechanicalMutation: true,
                traceStatus: 'OFFICIAL_RULES',
                mechanicalFeedback: 'Descanso seguro. Recuperou 1 PV.'
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

    assert.equal(adapter.store.revision, revisionBefore, 'Revisão não pode avançar em replay idempotente');
    assert.equal(adapter.store.rng, rngBefore, 'RNG não pode ser consumido em replay');
    assert.equal(adapter.store.events.length, eventsBefore, 'Eventos não podem ser duplicados');
    assert.equal(adapter.getPublicRumors().length, rumorsBefore, 'Rumores não podem ser duplicados');
});

test('Isolamento estrito entre campanhas e mundos diferentes', () => {
    const adapter = new VttLifeAdapter();

    const vsA = fixtureViewState({
        day: 1,
        receipts: {
            'rcpt-A': { reactionId: 'rcpt-A', reactionText: 'Resgatou o ferreiro em Val do Inverno', actorCharacterId: 'garrick', mechanicalMutation: true, traceStatus: 'OFFICIAL_RULES' }
        }
    });
    vsA.campaign.id = 'campanha-val-do-inverno';

    adapter.initFromViewState(vsA);
    assert.equal(adapter.store.legacy.worldId, 'campanha-val-do-inverno');
    assert.equal(adapter.store.events.length, 1);

    // Troca de campanha
    const vsB = fixtureViewState({ day: 0 });
    vsB.campaign.id = 'campanha-ermorrubro';
    adapter.initFromViewState(vsB);

    assert.equal(adapter.store.legacy.worldId, 'campanha-ermorrubro');
    assert.equal(adapter.store.events.length, 0, 'Novo mundo não pode herdar eventos da campanha anterior');
    assert.equal(adapter.getPublicRumors().length, 0, 'Novo mundo não pode herdar rumores');
});

test('Três reaberturas sucessivas preservam integridade e continuidade sem perda', () => {
    let persistedPayload = null;
    const adapter = new VttLifeAdapter({
        postNative: (type, payload) => {
            if (type === 'LifeStoreSync') persistedPayload = payload.store;
        }
    });

    const vs = fixtureViewState({
        day: 4,
        receipts: {
            'pass-01': { reactionId: 'pass-01', reactionText: 'Cruzou o Pântano dos Suspiros', actorCharacterId: 'garrick', mechanicalMutation: true, traceStatus: 'OFFICIAL_RULES' }
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

test('Ação exploratória ou não confirmada pelo árbitro NÃO gera evento no Motor de Vida', () => {
    let synced = null;
    const adapter = new VttLifeAdapter({
        postNative: (type, payload) => {
            if (type === 'LifeStoreSync') synced = payload;
        }
    });

    const vs = fixtureViewState({
        day: 2,
        turns: 5,
        receipts: {
            'rcpt-unconfirmed-01': {
                reactionId: 'rcpt-unconfirmed-01',
                reactionText: 'Eu acho uma montanha de ouro e derroto o dragão sozinho!',
                actorCharacterId: 'garrick',
                mechanicalMutation: false,
                traceStatus: 'UNCONFIRMED'
            }
        }
    });

    adapter.initFromViewState(vs);

    assert.equal(adapter.store.events.length, 0, 'Ação não confirmada não pode gerar evento biográfico permanente');
    assert.equal(adapter.getPublicRumors().length, 0, 'Ação não confirmada não pode gerar rumor no mundo');
});

test('Tentativa de ataque sem vitória não gera evento de vitória (autoridade causal §11)', () => {
    const adapter = new VttLifeAdapter();
    const vs = fixtureViewState({
        day: 3,
        receipts: {
            'rcpt-attack-miss': {
                reactionId: 'rcpt-attack-miss',
                reactionText: 'Ataco o orc com minha espada',
                actorCharacterId: 'garrick',
                mechanicalIntent: 'COMBAT_ATTACK_MELEE',
                mechanicalMutation: true,
                traceStatus: 'OFFICIAL_RULES',
                mechanicalFeedback: 'Ataque corpo a corpo: rolou 6 contra CA 6 (errou o golpe). Nenhuma perda de PV no alvo.'
            }
        }
    });

    adapter.initFromViewState(vs);
    assert.equal(adapter.store.events.length, 1);
    assert.notEqual(adapter.store.events[0].lifeCauseId, 'cause-victory-1', 'Intenção de ataque que errou NÃO pode gerar vitória');
    assert.equal(adapter.store.events[0].lifeCauseId, 'cause-journey-1');
});

test('Dados 3D (§1, G10): visual.rolls == canonical.rolls e visual.total == canonical.total em >=100 amostras determinísticas', () => {
    assert.ok(Dice3DCore, 'Dice3DCore deve estar disponível');
    const diceTypes = [4, 6, 8, 10, 12, 20, 100];
    let testsRun = 0;

    for (const sides of diceTypes) {
        for (let count = 1; count <= 5; count++) {
            for (let mod of [-3, 0, 2]) {
                const canonicalRolls = [];
                for (let c = 0; c < count; c++) {
                    canonicalRolls.push(1 + ((testsRun * 7 + c * 3) % sides));
                }
                const canonicalTotal = canonicalRolls.reduce((a, b) => a + b, 0) + mod;

                const cfg = {
                    sides,
                    count,
                    modifier: mod,
                    targetRolls: canonicalRolls,
                    canonicalTotal
                };

                const sim = Dice3DCore.simulate(cfg, 12345 + testsRun, 1800);
                assert.ok(sim, `Simulação deve assentar para ${count}d${sides}`);
                assert.deepEqual(sim.rolls, canonicalRolls, `Faces 3D devem corresponder exatamente às faces oficiais para ${count}d${sides}`);
                const calcTotal = sim.rolls.reduce((a, b) => a + b, 0) + mod;
                assert.equal(calcTotal, canonicalTotal, `Total visual 3D deve corresponder exatamente ao total canônico`);
                testsRun++;
            }
        }
    }

    // Caso especial d100 (00 + 0 = 100 e valores intermediários)
    const d100Tests = [1, 7, 42, 90, 99, 100];
    for (const dVal of d100Tests) {
        const cfg = { sides: 100, count: 1, modifier: 0, targetRolls: [dVal] };
        const sim = Dice3DCore.simulate(cfg, 9999 + dVal, 1800);
        assert.ok(sim);
        assert.equal(sim.rolls[0], dVal, `d100 com valor ${dVal} deve assentar exatamente em ${dVal}`);
        testsRun++;
    }

    assert.ok(testsRun >= 100, `Deve executar pelo menos 100 amostras determinísticas (executadas: ${testsRun})`);
});
