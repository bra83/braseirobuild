# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P208

## Base imutável e escopo

Continuar a partir do ZIP Android Studio **P208**, sobre **P207** SHA-256 `e91281adf202f8744cbfc9fe975cdc20de6729b3379e8ba655abd770cd501fb6`, 6.453 arquivos. Não reconstruir o projeto do zero. Preservar combate, banco, Arrow-7, Arend, saves legados e Kokoro. **P208 não é APK nem jogo integralmente homologado.**

## Implementação REAL P208 — SWE09 The Jewel of Yavin

### Naboka, página impressa 69

O PDF oficial do Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K` informa que Naboka **recusa quaisquer novas tentativas de slicing depois da primeira falha**. Implementado diretamente no motor `jewelBankingOperations.mjs`: tentativa NPC `naboka` malsucedida impede nova rolagem com esse NPC. O jogador pode prosseguir pessoalmente, sem contratação inventada de outro NPC. Não foram apagados pagamento, histórico, fichas ou recibos antigos.

Cada nova rolagem de slicing inclui `slicerPolicyVersion:2` e âncora `bankSlicerPolicy` com recibo de início; replay rejeita adulteração isolada do recibo/âncora. Recibos antigos P207 sem essa política continuam reabrindo. A disponibilidade é derivada de `BANK_STEP` efetivo e exibida em `jewelOfYavinStatus` e no painel Android PT-BR, que desabilita a opção de Naboka após a recusa. Não inventar penalidade ou devolução de créditos.

### Grand Galaxy Holo-Park, páginas impressas 92–94

Novo módulo `jewelHoloPark.mjs`, com API `enterJewelHoloPark`, `recordJewelHoloParkRound`, `confirmJewelHoloParkOutcome`, replay/validação. Exige **cena de fuga real, bomba resolvida, combate real ativo**, confirmação pelo Mestre da chegada física e de quais facções oficiais estão presentes: Wing Guard, capangas de Arend e/ou segurança de Grayson. Não cria facções, perfis ou NPCs sem fonte.

A Tabela 3-1 fornece paisagem por d100: 1–23 Alderaan; 24–47 Christophsis; 47–71 Coruscant; 72–95 Kashyyyk; 96–100 projetores desligados. **A impressão sobrepõe 47**: o motor exige decisão expressa do Mestre para esse resultado, não inventa uma correção editorial. As paisagens mudam após **duas rodadas reais de combate**, sem incrementar `combat.round` artificialmente, sem modificar a distância nem conceder bônus não adjudicados. O Mestre confirma a distância obtida por movimentação real em cada rodada. Alcance Longo/Extremo ou perseguidores derrotados/dispersos gera `ESCAPE_PENDING_GM`; dez rodadas gera `ROUND_LIMIT_PENDING_GM`, não fuga automática. Um recibo separado confirma o desfecho. Nenhum XP, Obligation, ferimento ou fuga planetária são atribuídos automaticamente.

Recibos `JEWEL_HOLO_ENTER`, `JEWEL_HOLO_ROUND`, `JEWEL_HOLO_END`, com replay de d100, intervalos, identidade, facções, sequência de rodadas, paisagens, distância confirmada e desfecho. Rollback por `GameController.atomicAction`, persistência após três reaberturas. Controller, `uiBridge.mjs`, `jewelOfYavin.mjs`, painel Android PT-BR e bundle offline integrados.

**Limite declarado:** não é ainda o combate completo das três facções no Holo-Park; os efeitos de símbolos narrativos particulares de Alderaan/Christophsis/Coruscant/Kashyyyk não são aplicados automaticamente. Modo narrativo por dois minutos ainda não implementado. Não alegar conclusão de Flight over Fight.

## Testes e plataforma

`evidence/P208/TEST_SUMMARY.json` e `PER_FILE_RESULTS.json`: **327 arquivos regulares, 3.016/3.016; seis stress 6/6; total 3.022/3.022 em 333 arquivos individuais**, sem suíte monolítica. 11 testes novos P208, incluindo Naboka, compatibilidade P207, d100=47, dez rodadas, rollback e três reaberturas. Sintaxe MJS/JS, JSON, XML e bundle aprovadas. Oito arquivos Java/Kokoro preservados byte-idênticos ao ZIP P207. Android **1.0.208**, versionCode **10208**, bundle offline **103 módulos**. Fonte: cotejo **direcionado**, não certificação editorial integral.

**Não houve Android SDK, Gradle, adb, APK, CI BRELGA ou homologação física.** O código P208 não foi enviado ao GitHub neste bloco.

## Pendências reais P209+

1. **SWE09 Flight over Fight:** três facções em encontros reais e efeitos de símbolos de combate por paisagem, sem duplicar NPCs, deslocamentos ou tempo; eventual modo narrativo de dois minutos com relógio real. Arend após localização, Aris e alternativa de perdão de Lando (+5 Obligation: Favor **por personagem**), somente com gatilhos oficiais e consequências na ficha.
2. **Arrow-7:** completar manobras/colisões/dano apenas com atributos de veículo/piloto comprovados; o perfil SWE09 p90 não fornece todos. Não inventar velocidade, casco, blindagem, handling, strain nem piloto.
3. **SWE02:** New Meen, Geonosis, Teemo; cotejo de cinco aventuras cena a cena e certificação semântica individual dos 360 consumidores EotE/AoR/expansões.
4. **Android real:** compilar no GitHub `bra83/braseirobuild` runner BRELGA somente com acesso real e fonte correta, instalar, validar TTS/Kokoro e três reaberturas no aparelho.

## Procedimento obrigatório de continuidade

Verificar SHA/CRC do ZIP P208 e comparar os hashes com o manifesto do delta P207→P208; preservar oito Java/Kokoro e saves. Implementar código substancial antes de gerar novo checkpoint. Rodar testes focais + regressão global por arquivo + seis stress. Gerar ZIP Android Studio completo, SHA-256 externo, delta com reconstrução de hashes, handoff e evidências; salvar na Biblioteca `STAR_WARS_EDGE_OF_THE_GALAXY_WORKING/checkpoints/P209` e conferir readback SHA/CRC. Não inventar APK, CI, certificação editorial ou conclusão do projeto.
