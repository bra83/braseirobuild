# BRASEIRO STAR WARS — R54 P137 | Checkpoint Android Studio

## Baseline e integridade

Base exclusiva: `BRASEIRO_STAR_WARS_EDGE_R54_P136_SOURCE_CHECKPOINT.zip` (SHA-256 `1e1b0474eb843854cbc8ccddbe4c62868777ff63e4cac567696168d8dc3ec801`). P136 mantido intacto para recuperação. Versão Android `versionCode=10137`, `versionName=1.0.137`; shell offline `1.0.137`. Bundle regenerado: **74 módulos**.

## Implementação P137 (código real)

- **AoR + EotE, seis famílias / 27 posições novas**: cobertura de efeitos executáveis passou de **178 para 205 das 360 posições AoR**. Compra nas árvores reais, custos de PE, pré-requisitos e persistência mantidos. Restam **155 posições** sem efeito completo. Não se confundiu `SUBIR DE UN SALTO` (embarque em veículo) com `LEVANTARSE DE UN SALTO` (levantar-se em combate).
- `REPARACIÓN EFICAZ` (7 posições): alias `SOLREP` aplicado a Controle de Danos de casco e reparos manuais, por nível e somente após sucesso.
- `ROBUSTO` (4): `DURA` reduz em 10 por nível o resultado de crítico sofrido, limitado a mínimo 1, preservando registro do modificador e o journal de ferimentos.
- `CONFIANZA` (6): `CONF` reduz dificuldade de medo com **Disciplina**, nunca com Frieza, respeitando dificuldade mínima 1 e upgrades.
- `DISPARO CERTERO` (4): `TRUEAIM` consome uma manobra real, adiciona bônus de Mirar e aprimora o pool de ataques à distância por nível, **uma vez por rodada**, com estado persistente e opção condicional na UI Android.
- `ELUDIR SEGURIDAD` (3): `BYP` remove Contratempos por nível apenas em testes explicitamente contextualizados de violação de segurança (Computadores/Atividade criminosa); novo painel no fluxo de sessão Android.
- `ESPECIALISTA EN ESTIMULANTES` (3): `STIMSPEC` aumenta a cura dos injetores por nível, preservando consumo real de item e usos diários.
- **UAE / cinco aventuras**: regressão integrada de serialização, importação e validação do checkpoint de cada uma das cinco aventuras; motores de objetivos, consequências, triggers e clocks preservados do P136. Nenhum encontro oficial ou recompensa foi inventado. A presente rodada não altera o conteúdo narrativo das aventuras.
- **Kokoro/TTS**: arquivos de voz não modificados; regressão específica preservada.

## Testes executados após o último rebuild

- `python3 tools/run-node-suite.py --regular`: **1.434/1.434 PASS**, 250 arquivos.
- `python3 tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test --test-reporter=tap tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**.
- `node --test --test-reporter=tap tests/uae-p137-aor-cross-system-mechanics.test.mjs`: **16/16 PASS** (incluídos na suíte regular).
- Bundle offline: `node tools/build-ui-bridge-bundle.mjs`, **74 módulos**; `node --check` PASS.
- Java: `node tools/java-syntax-gate.mjs`: PASS sintático para 8 arquivos; 699 símbolos Android não resolvidos neste ambiente sem SDK (não é compilação Android).
- JSON 22 arquivos e XML 2 arquivos parseados: PASS.
- ZIP completo: integridade e SHA-256 verificados na entrega.

## Pendências explícitas

1. **155 posições AoR** ainda sem efeitos completos; expansões EotE/AoR ainda não certificadas contra todos os PDFs originais (não estão neste ZIP).
2. **Além da Orla**: encontros de combate oficiais ainda não implementados/certificados; `encounters:false` permanece. As cinco aventuras possuem integração UAE, mas **não** certificação narrativa integral contra os livros.
3. **Android**: APK não compilado. SDK Android ausente neste ambiente; necessário runner BRELGA/Android Studio para compilar, instalar em Galaxy S23 Ultra/A57 e validar três reaberturas físicas, TTS Kokoro e sessão prolongada.

## Retomada

Usar **P137** como baseline e **P136** como fallback. Priorizar as 155 posições AoR restantes com efeitos reais, conferir os cinco módulos contra os PDFs oficiais, completar o combate de Além da Orla com especificações verificadas e executar homologação APK. Não declarar 100% até esses gates passarem.
