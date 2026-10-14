# Núcleo básico jogável — estado em 14/10/2026

## Escopo definido pelo proprietário

O primeiro objetivo é preparar o jogo base antes dos suplementos. O proprietário esclareceu que XMM é um dos livros básicos. Portanto, o núcleo deste trabalho é **XPHB 2024 + XMM 2025 + XDMG 2024**. Suplementos e expansões ficam depois desse núcleo.

## Entregue nesta rodada

- Campanhas D&D novas começam no perfil revisado **2024/2025**. Campanhas já salvas em 2014 mantêm essa edição; o teste reabre o banco e confirma a preservação.
- Um teste Robolectric abre a tela de campanha e navega pelas telas de Heróis, Combate, Dados e Mapa.
- Dois efeitos compostos do XMM agora têm aplicação persistente em combate e expiração no início do próximo turno da criatura de origem: Rakshasa / *Baleful Command* aplica frightened e incapacitated; Storm Giant / *Thunderbolt* aplica blinded e deafened.
- O ataque Fearsome Claw do Espantalho agora aplica frightened no acerto e a remove no fim do próximo turno do Espantalho. A passagem de turno distingue fim do turno e início do próximo; o efeito fica salvo no banco e seu evento é rastreável.
- A mordida do Merrow aplica poisoned no acerto até o fim do próximo turno do Merrow, com a mesma expiração persistente no limite correto.
- Seis efeitos simples do XMM agora aplicam condição com expiração no limite correto do turno: quatro no fim do turno do alvo e Couatl / Bite e Ghast Gravecaller / Horrific Necrosis nos prazos definidos pela fonte. A Claw condicional do Ghast permanece manual.
- Quatorze riders do XMM cobertos pelo lote Run246 agora têm teste E2E de combate persistido: a condição só expira quando o atacante inicia o próximo turno.
- A lista de ataques da interface exibe em português os nomes cobertos por esses testes e mantém os identificadores originais internamente.
- Os 17 nomes de ataques com riders tipados cobertos pelos testes desta rodada agora têm rótulos em português na lista de ações. Os nomes fonte continuam sendo usados internamente. Isso não representa tradução completa do aplicativo.
- Corrigi o Bite do Mimic: dano normal de perfuração e ácido conferido, sem impor grappled indevidamente; vantagem e dano alternativo ficam manuais até haver contexto do alvo. A matriz auxiliar registra essa fronteira.
- Corrigi os dados-fonte dos ataques de investida XMM e conectei 22 ataques diretos ao trajeto persistido: dano extra/alternativo e “caído” só entram quando o percurso comprometido cumpre a distância, direção e tamanho declarados.
- Corrigi o Peryton / Gore: o dano da investida é adicional, não um ramo alternativo; o ramo agora usa a mesma validação de rota do restante das investidas.
- O deslocamento voluntário no grid persiste as células percorridas, posição e custo, restaura no backup com remapeamento de ID e invalida o marcador após ação/recurso, deslocamento fora do grid, levantar-se ou fim do turno. Quando uma faixa horizontal/vertical está livre, o planejador mantém a rota reta, em vez de escolher um desvio empatado.
- As telas de escolha de ação com salvaguarda e recarga agora exibem nomes PT-BR, mantendo o nome-fonte no comando; 27 rótulos cobertos pelo teste foram adicionados ao glossário. Isso ainda não é cobertura completa da tradução de todo o aplicativo.
- Corrigi 30 ações sem salvaguarda que registravam efeitos em `condition_on_fail`; condições de acerto, pré-requisitos, efeitos condicionais e CDs de fuga/desengate estão classificados conforme a fonte e continuam manuais.
- O executor direto de investidas tem escopo qualificado por rota; os indicadores amplos `manual_rider_resolution_required` continuam verdadeiros para outras rotas sem contexto, e multiataques com investida seguem bloqueados. A reconciliação desse escopo está registrada na evidência local do executor.
- Corrigi 30 estados contraditórios na matriz XMM: as notas já diziam que o rider estava manual, mas o campo de status afirmava homologação. Os registros agora deixam o executor e a ação integral como abertos; linhas RHW não foram alteradas.
- O comparador independente continuou conferindo os 503 monstros e as 36 seções XMM.
- A suíte Android/Robolectric completa foi executada localmente após a integração: 2.524 testes, zero falhas/erros/ignorados em 109 XMLs. A execução revelou duas regressões de rota/dano tipado; ambas foram corrigidas e o conjunto completo passou no commit seguinte. Os 34 testes Python dos comparadores XMM passaram; fonte pinada conferiu 503 criaturas e 36 seções.

## Evidência local

Commit do perfil 2024: `5b4bdf056be0a5689f1506f666cd15deacd95b3d`.
Commits das correções XMM: `4ebcecd14373bfea47935230d994fa0f5c8f5fe5`, `fc7696f50f75efeb60d93e1e6dba1fc81971f158`, `b0dddc3dc081f8e34022b8b199ab5f6d5d310326`, `4768d992da4f4aa812eab01b613ec8545b64c11b`, `c6c5b2a0109aa3673cabc3e609069d515e86ad20`, `dbec8417a0fd836401caea514ff97e708b990515`, `522a5f80a707b71c83b522cbaf21b69aeb642a76`, `2cf336e81f5515994c5a511d33fa44f3037a6137`, `857ab221acd24024e683b2f0299d923bcab03ab8` e `55eec85d1765c52ad8a60dcd78e293b0df853c18` e `e29cf40152d4f6f229d92d267172a3c96ce91fec` e `0abce0c14eec979bf676cb38df5f184d7606c614` e `7b462342e7058e5b7b763739bbde135303a08c43`.

Os rótulos PT-BR nos seletores de salvaguarda/recarga e seus testes estão em [`xmm-ptbr-action-selectors-local-20261014-7b462342`](editorial-evidence/xmm-ptbr-action-selectors-local-20261014-7b462342/manifest.json). O gate de salvaguarda/CD atualizado e a conferência do spell save da Succubus estão em [`xmm-save-dc-gate-local-20261014-0abce0c1`](editorial-evidence/xmm-save-dc-gate-local-20261014-0abce0c1/manifest.json). A correção dos 30 campos `condition_on_fail` e seus testes estão em [`xmm-condition-semantics-local-20261014-e29cf401`](editorial-evidence/xmm-condition-semantics-local-20261014-e29cf401/manifest.json). A normalização das 13 investidas e seus testes estão em [`xmm-charge-extra-local-20261014-55eec85d`](editorial-evidence/xmm-charge-extra-local-20261014-55eec85d/manifest.json). A correção Peryton / Gore e seus testes estão em [`xmm-peryton-additive-local-20261014-857ab221`](editorial-evidence/xmm-peryton-additive-local-20261014-857ab221/manifest.json). A correção do Bite do Mimic e seus testes estão em [`xmm-mimic-prerequisite-local-20261014-2cf336e8`](editorial-evidence/xmm-mimic-prerequisite-local-20261014-2cf336e8/manifest.json). As provas do perfil e navegação estão em [`core-base-local-20261014-5b4bdf05`](editorial-evidence/core-base-local-20261014-5b4bdf05/manifest.json). As provas dos efeitos com limite no fim do turno estão em [`xmm-turn-end-local-20261014-fc7696f5`](editorial-evidence/xmm-turn-end-local-20261014-fc7696f5/manifest.json) e [`xmm-target-turn-end-local-20261014-b0dddc3d`](editorial-evidence/xmm-target-turn-end-local-20261014-b0dddc3d/manifest.json). Os testes de combate e a comparação por fonte estão em [`xmm-source-turn-start-e2e-local-20261014-c6c5b2a0`](editorial-evidence/xmm-source-turn-start-e2e-local-20261014-c6c5b2a0/manifest.json), [`xmm-source-turn-audit-local-20261014-dbec8417`](editorial-evidence/xmm-source-turn-audit-local-20261014-dbec8417/manifest.json) e [`xmm-couatl-ghast-riders-local-20261014-522a5f80`](editorial-evidence/xmm-couatl-ghast-riders-local-20261014-522a5f80/manifest.json). As traduções testadas dos nomes de ação estão em [`xmm-ptbr-target-riders-local-20261014-4768d992`](editorial-evidence/xmm-ptbr-target-riders-local-20261014-4768d992/manifest.json). A correção dos estados contraditórios está em [`xmm-matrix-status-audit-local-20261014-4768d992`](editorial-evidence/xmm-matrix-status-audit-local-20261014-4768d992/manifest.json).

O executor de rota para ataques diretos de investida, seus 19 testes selecionados e a matriz de escopo (incluindo os 22 ataques suportados e multiataques ainda bloqueados) estão em [`xmm-charge-path-attack-local-20261014`](editorial-evidence/xmm-charge-path-attack-local-20261014/manifest.json).

A suíte completa Android/Robolectric, os hashes de 109 XMLs, comparador XMM e registro das duas regressões corrigidas estão em [`xmm-full-suite-local-20261014-7101d61d`](editorial-evidence/xmm-full-suite-local-20261014-7101d61d/manifest.json).

O contrato do Diabo Barbado / Barba, o bloqueio persistente de cura, a expiração no turno correto e a regressão integral atualizada estão em [`xmm-bearded-devil-healing-local-20261014-cce2c2b0`](editorial-evidence/xmm-bearded-devil-healing-local-20261014-cce2c2b0/manifest.json).

- Suíte Android/Robolectric no commit XMM: **2.516 testes, 0 falhas, 0 erros, 0 ignorados**, em 105 arquivos XML.
- Testes Python no commit XMM: **65 passaram**.
- Comparação XMM: 503 monstros, 36 seções, 120 ações de recarga, 132 registros de conjuração, 178 contratos de traços e 59 ações simples de salvaguarda conferidos.
- Efeitos com condições tipados no comparador: dois pacotes compostos e vinte e cinco condições simples com expiração tipada; quatro das simples expiram no fim do turno do alvo. O comparador cobre, além disso, 14 riders de origem com prazo de um turno.

## Ainda não está fechado como núcleo pronto

A lista manual arquivada no SHA anterior tinha **246 entradas**. A comparação pinada mais recente, após tipar os efeitos de recuperação de PV da Barba do Diabo Barbado, Ferrão Infernal do Diabo Ósseo, Mordida do Worg Atroz e Foice do Pavor do Death Cultist, conta **242 efeitos fora dos contratos tipados**; o denominador é ação/efeito do comparador, não uma certificação de cada monstro. O VTT segue sem cobertura integral de combate. Também estão pendentes CI BRELGA e instalação/abertura em Android físico; não havia aparelho ADB conectado e nenhum APK foi gerado nesta etapa.

O proprietário informou em 14/10/2026 que adquiriu os materiais e autorizou sua consulta no VTT de uso estritamente pessoal. Essa autorização orienta a conferência interna do conteúdo mecânico; o projeto continua sem reproduzir integralmente os livros. O núcleo permanece em andamento: XMM ainda tem efeitos manuais, e não avanço para suplementos.


A expansão do mesmo efeito para três monstros, com as 2.525 regressões e comparação fonte atualizadas, está em [`xmm-poison-healing-three-monsters-local-20261014-a9c9200e`](editorial-evidence/xmm-poison-healing-three-monsters-local-20261014-a9c9200e/manifest.json).


A Foice do Pavor agora persiste a supressão de cura até o fim do próximo turno do alvo. No commit `a1c8caff9da8fe1589f374863620d6e060a2ddc9`, passaram 2.526 testes Android/Robolectric em 111 XMLs, os 37 testes Python do comparador e a comparação XMM de 503 monstros/36 seções. Evidência, hashes e inventário por ponteiro-fonte dos 242 itens ainda manuais: [`xmm-dread-scythe-healing-local-20261014-a1c8caff`](editorial-evidence/xmm-dread-scythe-healing-local-20261014-a1c8caff/manifest.json). Os 242 efeitos manuais, a falta de execução BRELGA e a validação pendente em aparelho mantêm o núcleo aberto.

## Última rodada — auditoria de dano XMM (14/10/2026)

No código `886e40e0f3720b380beca0de17b37bb8da40a617`, separei seis danos imediatos de riders secundários e corrigi a escolha alternativa do Smother. A comparação pinada cobriu 581 fórmulas/tipos de dano primário, os seis contratos secundários, 503 criaturas e 36 seções. A suíte Android/Robolectric passou com **2.527 testes, 111 XMLs, 0 falhas/erros/ignorados**; os comparadores Python passaram 39/39; o verificador de proveniência passou 5/5 testes e o ledger validou 483/483 linhas sem erro. Nenhuma seção XMM tem homologação semântica individual; 35 seguem pendentes e uma é não aplicável. O inventário manual subiu para 244 itens após detectar dois ataques ainda não classificados. Evidência: [`xmm-primary-attack-damage-local-20261014-886e40e0`](editorial-evidence/xmm-primary-attack-damage-local-20261014-886e40e0/manifest.json).

O proprietário confirmou compra e uso pessoal do material; registrei essa base declaratória nas 36 linhas XMM do ledger, sem mudar o status de homologação. XMM permanece aberto: falta prova semântica individual das seções e efeitos ainda manuais; BRELGA para o SHA `886e40e0` segue em execução e o auditor hospedado falhou antes dos passos; APK não foi gerado. Progresso homologado permanece **2/15**.

A falha Windows do sandbox SQLite no BRELGA de `886e40e0` foi corrigida no fixture de teste com nome de arquivo curto e criação explícita da pasta. No código `68ae2cf8dc74ac8ad7f71ca7866c9a6603986d5f`, o BRELGA passou 2.527 testes em 111 XMLs, sem falhas/erros/ignorados; a suíte local também passou. Os XMLs e a falha/correção comparados estão no manifesto [`xmm-primary-attack-damage-local-20261014-68ae2cf8`](editorial-evidence/xmm-primary-attack-damage-local-20261014-68ae2cf8/manifest.json). Isso remove a regressão encontrada na rodada, mas não fecha XMM: 35 seções continuam sem homologação semântica e 244 efeitos ficam manuais.
