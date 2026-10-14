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
- Corrigi 13 ataques de investida (Boar, Elk, Galeb Duhr, Giant Boar, Giant Elk, Giant Goat, Minotaurs, Owlbear, Rhinoceros, Triceratops, Warhorse e Wereboar): dano base e adicional, tipos, limites de tamanho, movimento e “caído” agora correspondem ao ramo fonte; nada aplica automaticamente sem rastreamento de movimento.
- Corrigi o Peryton / Gore: o dano da investida é adicional, não um ramo alternativo. Continua bloqueado como manual até o combate rastrear movimento em linha reta antes do ataque.
- Corrigi mais 13 linhas da matriz de executor que ainda declaravam investidas como homologadas: agora mostram ramo de dano corrigido, movimento manual e ação/XMM ainda abertos.
- Corrigi 30 estados contraditórios na matriz XMM: as notas já diziam que o rider estava manual, mas o campo de status afirmava homologação. Os registros agora deixam o executor e a ação integral como abertos; linhas RHW não foram alteradas.
- O comparador independente continuou conferindo os 503 monstros e as 36 seções XMM.

## Evidência local

Commit do perfil 2024: `5b4bdf056be0a5689f1506f666cd15deacd95b3d`.
Commits das correções XMM: `4ebcecd14373bfea47935230d994fa0f5c8f5fe5`, `fc7696f50f75efeb60d93e1e6dba1fc81971f158`, `b0dddc3dc081f8e34022b8b199ab5f6d5d310326`, `4768d992da4f4aa812eab01b613ec8545b64c11b`, `c6c5b2a0109aa3673cabc3e609069d515e86ad20`, `dbec8417a0fd836401caea514ff97e708b990515`, `522a5f80a707b71c83b522cbaf21b69aeb642a76`, `2cf336e81f5515994c5a511d33fa44f3037a6137`, `857ab221acd24024e683b2f0299d923bcab03ab8` e `55eec85d1765c52ad8a60dcd78e293b0df853c18`.

A normalização das 13 investidas e seus testes estão em [`xmm-charge-extra-local-20261014-55eec85d`](editorial-evidence/xmm-charge-extra-local-20261014-55eec85d/manifest.json). A correção Peryton / Gore e seus testes estão em [`xmm-peryton-additive-local-20261014-857ab221`](editorial-evidence/xmm-peryton-additive-local-20261014-857ab221/manifest.json). A correção do Bite do Mimic e seus testes estão em [`xmm-mimic-prerequisite-local-20261014-2cf336e8`](editorial-evidence/xmm-mimic-prerequisite-local-20261014-2cf336e8/manifest.json). As provas do perfil e navegação estão em [`core-base-local-20261014-5b4bdf05`](editorial-evidence/core-base-local-20261014-5b4bdf05/manifest.json). As provas dos efeitos com limite no fim do turno estão em [`xmm-turn-end-local-20261014-fc7696f5`](editorial-evidence/xmm-turn-end-local-20261014-fc7696f5/manifest.json) e [`xmm-target-turn-end-local-20261014-b0dddc3d`](editorial-evidence/xmm-target-turn-end-local-20261014-b0dddc3d/manifest.json). Os testes de combate e a comparação por fonte estão em [`xmm-source-turn-start-e2e-local-20261014-c6c5b2a0`](editorial-evidence/xmm-source-turn-start-e2e-local-20261014-c6c5b2a0/manifest.json), [`xmm-source-turn-audit-local-20261014-dbec8417`](editorial-evidence/xmm-source-turn-audit-local-20261014-dbec8417/manifest.json) e [`xmm-couatl-ghast-riders-local-20261014-522a5f80`](editorial-evidence/xmm-couatl-ghast-riders-local-20261014-522a5f80/manifest.json). As traduções testadas dos nomes de ação estão em [`xmm-ptbr-target-riders-local-20261014-4768d992`](editorial-evidence/xmm-ptbr-target-riders-local-20261014-4768d992/manifest.json). A correção dos estados contraditórios está em [`xmm-matrix-status-audit-local-20261014-4768d992`](editorial-evidence/xmm-matrix-status-audit-local-20261014-4768d992/manifest.json).

- Suíte Android/Robolectric no commit XMM: **2.516 testes, 0 falhas, 0 erros, 0 ignorados**, em 105 arquivos XML.
- Testes Python no commit XMM: **64 passaram**.
- Comparação XMM: 503 monstros, 36 seções, 120 ações de recarga, 132 registros de conjuração, 178 contratos de traços e 59 ações simples de salvaguarda conferidos.
- Efeitos com condições tipados no comparador: dois pacotes compostos e vinte e duas condições simples com expiração tipada; quatro das simples expiram no fim do turno do alvo. O comparador cobre, além disso, 14 riders de origem com prazo de um turno.

## Ainda não está fechado como núcleo pronto

A lista versionada de ações manuais ainda tem **246 entradas**, concentradas em outros efeitos que aguardam modelagem ou decisão explícita. O comparador amplo reporta 246 ações/efeitos fora dos seus contratos tipados; esse número tem denominador próprio e não significa que cada ação tenha sido homologada individualmente. O Fearsome Claw deixou de depender de um efeito manual. Ainda não é possível afirmar que todos os monstros estejam prontos para combate. Também continuam pendentes o CI BRELGA neste SHA e a instalação/abertura em Android. Não havia aparelho ADB conectado; não foi gerado APK, conforme o gate atual da etapa XMM.

O núcleo básico permanece em andamento. A próxima implementação fica no XMM: dar consequência persistente aos efeitos restantes ou encaminhá-los por uma decisão explícita e aplicável do Mestre, sempre com prova fonte→runtime→estado. O registro do Espantalho usa a comparação estruturada auxiliar 5e.tools; validação da ação contra o PDF licenciado continua pendente. Os suplementos não avançam antes da conclusão do núcleo.
