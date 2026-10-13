# Braseiro D&D Normal — fonte 5e.tools verificada para livros novos (13/10/2026)

**Escopo:** somente RHW (Ravenloft: The Horrors Within, 2026) e EFA (Eberron: Forge of the Artificer, 2025); Mystara não foi tocado. Este arquivo não declara 483/483 certificados.

## Proveniência reproduzível

- Índice navegável: https://5e.tools/books.html
- RHW: https://github.com/5etools-mirror-3/5etools-src/blob/main/data/feats.json
  - Git blob imutável `99b18fd68c050cbc6b5a8d2e1f6da3a5c175ec08`.
  - Seção concreta: feat `Gathered Whispers`, página 33, `Unearthly Scream`.
  - Confirma: ação de reação SOMENTE após ataque acertar; bônus PB na CA para aquele ataque; usos limitados a PB/descanso longo.
- RHW: https://github.com/5etools-mirror-3/5etools-src/blob/main/data/races.json
  - Git blob imutável `853a6e0f5b25075d826fd005e45276d1e91fe328`.
  - Seção concreta: espécie `Dhampir`, `Vampiric Bite`.
  - Confirma: mordida causa dano perfurante antes de decidir empoderamento; não aplicável a Constructs nem Undead; recursos por PB.
- EFA: mesmo `data/races.json` e hash acima.
  - Seção concreta: espécie `Kalashtar`, `Mind Link`.
  - Confirma: alcance em pés = 10 × nível; permitir resposta telepática por 1 hora exige ação Magic.

## Alterações de código reais (não confundir com certificação do livro)

1. `RavenloftHorrorsWithinRulesEngine.resolveCharacterAction`:
   `gathered_whispers_scream` deixa de aceitar ataque que já errou, exige CA válida e preserva gasto de uso somente no gatilho correto; `dhampir_bite_empower` bloqueia dano não positivo e alvo não informado.
2. `Ravenloft2026SourceRulesTest`: três cenários individuais incluindo erro anterior, bônus de CA, consumo de uso e mordida válida/inválida.
3. `EberronForgeCompleteEngine.resolveSpeciesAction`:
   `telepathy` exige distância fornecida dentro de 10×nível; nova ação `mind_link_reply` exige confirmação de Magic action, alvo, distância e apresenta duração de 60 minutos (expiração ainda requer controle externo).
4. `Eberron2025KalashtarSourceTest`: testes com distâncias no limite, fora do limite, falta de dado e nova ação.

## Resultado ainda NÃO afirmado

O workflow BRELGA para estes SHAs precisa concluir com XML de teste verde. Essas provas abrangem os **comportamentos indicados**, não todo o capítulo de RHW/EFA nem todos os 483 IDs. O índice de 483 seções e suas ocorrências no JSON está em `docs/SOURCE_BOOK_JSON_ANCHORS_483_20261013.csv` e exige comparação de comportamento por seção.
