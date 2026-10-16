# Braseiro Star Wars VTT — R54 P142 (11/10/2026)

## Base e entrega

Checkpoint de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P141_SOURCE_CHECKPOINT.zip`.
Versão Android: `versionCode=10142`, `versionName=1.0.142`. Shell offline `data-version=1.0.142`. Bundle Android reconstruído a partir dos `.mjs`: **77 módulos**. O ZIP P142 contém o projeto Android Studio completo, não APK.

## Código implementado

1. **Gatilhos UAE — proveniência causal exata:** todo novo evento `TRIGGER_FIRED` registra `causeEventId` com ID do evento originador, além de `triggerId` e `eventType`. A auditoria rejeita causas inexistentes, posteriores ou de tipo incompatível, duplicação de gatilho pela mesma causa e estouro de `maxFires`.
2. **Compatibilidade de persistência P141 → P142:** `causalAuditFromSequence` identifica a primeira sequência que exige a nova prova causal. Novas aventuras já iniciam com auditoria estrita. Checkpoints antigos recebem marcador apenas no próximo commit atômico, preservando seu histórico e exigindo causalidade para eventos posteriores. Erros provocam rollback sem alteração do checkpoint.
3. **Proteção de eventos internos:** chamadas externas e efeitos não podem falsificar a origem `engine` nem injetar eventos internos de gatilho, consequência ou objetivo. Os métodos legítimos do motor usam despacho interno autenticado por token privado de módulo; a fila de cascatas e o journal permanecem integrados.
4. **Relógios UAE:** novos eventos `CLOCK_ADVANCED` passam por validação de ID, limite, valor e status; `CLOCK_COMPLETED` exige avanço anterior consistente e valor final igual ao máximo, inclusive após restauração.
5. **Cinco aventuras:** os cinco adaptadores publicados continuam usando o UAE. Nova suíte de regressão executa transições reais, replay JSON com três reaberturas, rollback multietapa, proteção contra adulteração, gatilhos, consequências, objetivos e relógios em cada aventura.
6. **Android:** bundle offline reconstruído e shell/versionCode alinhados. Kokoro/TTS não teve código modificado.

## Testes efetivamente executados

- Suíte regular: **1586/1586 PASS** (256 arquivos).
- Stress: **6/6 PASS** (6 arquivos). Total **1592/1592 PASS**.
- Testes novos P142: **32/32 PASS**, inclusive os cinco adaptadores.
- Kokoro/TTS: **27/27 PASS** (testes automatizados de código, não reprodução real em dispositivo).
- Java: gate sintático PASS (8 arquivos; símbolos Android não resolvidos sem SDK).
- JS: sintaxe dos módulos alterados e bundle PASS; JSON 65 e XML 2 PASS.
- Evidências: `evidence/P142/`.

## Pendências não encerradas

- **AoR:** 215/360 posições com efeito executável; **145 ainda pendentes**. Nenhuma posição foi declarada concluída sem implementação baseada em regras verificáveis.
- **Expansões EotE/AoR:** conferência integral contra PDFs oficiais ainda não certificada.
- **Cinco aventuras:** integração técnica testada; não houve homologação textual/encontros/recompensas contra os PDFs oficiais. *Além da Orla* continua sem certificação do combate de encontros oficiais.
- **Android:** APK não compilado neste ambiente (sem Android SDK/runner de build). Sem instalação em S23 Ultra/A57 ou teste real de Kokoro.

## Retomada

Usar P142 como base. Priorizar os 145 efeitos AoR restantes **apenas** com referência mecânica verificável, certificação das expansões e aventuras contra livros, fechar combate oficial de *Além da Orla*, depois compilar no runner BRELGA e homologar Android. Não confundir PASS de testes Node com certificação dos PDFs ou APK.
