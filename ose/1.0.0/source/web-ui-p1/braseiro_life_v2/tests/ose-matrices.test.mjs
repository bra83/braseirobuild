import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {loadMatrixBundle} from '../src/node-matrices.mjs';
import {sampleCrossedMatrix, checkMatrixCoherence} from '../src/matrix-engine.mjs';

const osePacks = ['ose-classic', 'ose-advanced'];

for (const packId of osePacks) {
    test(`OSE pack ${packId} carrega e valida 500 manifestações e 100 situações distintas`, async () => {
        const bundle = await loadMatrixBundle(packId);
        assert.ok(bundle, `Pacote ${packId} deve existir`);
        assert.equal(bundle.tables.manifestation.length, 500, 'Deve conter 500 manifestações');
        const situations = new Set(bundle.tables.manifestation.map(r => r.baseSituationId));
        assert.equal(situations.size, 100, 'Deve conter 100 situações distintas');
        assert.equal(bundle.tables.cause.length, 500, 'Causas expandidas para 500');
        assert.equal(bundle.tables.domain.length, 500, 'Domínios expandidos para 500');
        assert.equal(bundle.tables.timing.length, 500, 'Timing expandido para 500');
        assert.equal(bundle.tables.response.length, 500, 'Respostas expandidas para 500');
    });

    test(`cruzamento determinístico e coerente: ${packId}`, async () => {
        const bundle = await loadMatrixBundle(packId);
        const ctx = {
            system: bundle.pack.system,
            mode: bundle.pack.mode,
            day: 14,
            origin: { id: 'evt-01', day: 0, causeId: 'cause-victory-1', actorIds: ['garrick'], summary: 'Cripta explorada com sucesso' },
            characterId: 'garrick',
            capabilities: ['torch', 'iron_spikes'],
            facts: {}
        };
        const res = sampleCrossedMatrix(bundle, ctx, 42);
        assert.equal(res.status, 'proposal');
        assert.equal(res.proposal.canonMutation, 'none', 'Motor de vida não deve aplicar mutação mecânica');
        assert.equal(res.proposal.mechanicalEffect, 'none', 'Motor de vida não altera PV/XP diretamente');
        const coherence = checkMatrixCoherence(bundle, res.selection, ctx);
        assert.equal(coherence.valid, true, 'Sorteio cruzado deve ser estritamente coerente');
    });
}
