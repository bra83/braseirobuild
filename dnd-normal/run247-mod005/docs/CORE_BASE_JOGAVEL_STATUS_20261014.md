# Núcleo básico jogável — estado em 14/10/2026

## Escopo definido pelo proprietário

O primeiro objetivo é preparar o jogo base antes dos suplementos. O proprietário esclareceu que XMM é um dos livros básicos. Portanto, o núcleo deste trabalho é **XPHB 2024 + XMM 2025 + XDMG 2024**. Suplementos e expansões ficam depois desse núcleo.

## Entregue nesta rodada

- Campanhas D&D novas começam no perfil revisado **2024/2025**. Campanhas já salvas em 2014 mantêm essa edição; o teste reabre o banco e confirma a preservação.
- Um teste Robolectric abre a tela de campanha e navega pelas telas de Heróis, Combate, Dados e Mapa.
- Dois efeitos compostos do XMM agora têm aplicação persistente em combate e expiração no início do próximo turno da criatura de origem: Rakshasa / *Baleful Command* aplica frightened e incapacitated; Storm Giant / *Thunderbolt* aplica blinded e deafened.
- O ataque Fearsome Claw do Espantalho agora aplica frightened no acerto e a remove no fim do próximo turno do Espantalho. A passagem de turno distingue fim do turno e início do próximo; o efeito fica salvo no banco e seu evento é rastreável.
- O comparador independente continuou conferindo os 503 monstros e as 36 seções XMM.

## Evidência local

Commit do perfil 2024: `5b4bdf056be0a5689f1506f666cd15deacd95b3d`.
Commit da correção XMM: `4ebcecd14373bfea47935230d994fa0f5c8f5fe5`.

As provas do perfil e navegação estão em [`core-base-local-20261014-5b4bdf05`](editorial-evidence/core-base-local-20261014-5b4bdf05/manifest.json). Os XMLs e a comparação atualizados para a correção do Espantalho estão em [`xmm-turn-end-local-20261014-4ebcecd1`](editorial-evidence/xmm-turn-end-local-20261014-4ebcecd1/manifest.json).

- Suíte Android/Robolectric no commit XMM: **2.508 testes, 0 falhas, 0 erros, 0 ignorados**, em 105 arquivos XML.
- Testes Python no commit XMM: **58 passaram**.
- Comparação XMM: 503 monstros, 36 seções, 120 ações de recarga, 132 registros de conjuração, 178 contratos de traços e 59 ações simples de salvaguarda conferidos.
- Efeitos com condições tipados no comparador: dois pacotes compostos e uma condição simples com expiração no fim do turno de origem.

## Ainda não está fechado como núcleo pronto

A lista versionada de ações manuais ainda tem **246 entradas**, concentradas em outros efeitos que aguardam modelagem ou decisão explícita. O comparador amplo reporta 246 ações/efeitos fora dos seus contratos tipados; esse número tem denominador próprio e não significa que cada ação tenha sido homologada individualmente. O Fearsome Claw deixou de depender de um efeito manual. Ainda não é possível afirmar que todos os monstros estejam prontos para combate. Também continuam pendentes o CI BRELGA neste SHA e a instalação/abertura em Android. Não havia aparelho ADB conectado; não foi gerado APK, conforme o gate atual da etapa XMM.

O núcleo básico permanece em andamento. A próxima implementação fica no XMM: dar consequência persistente aos efeitos restantes ou encaminhá-los por uma decisão explícita e aplicável do Mestre, sempre com prova fonte→runtime→estado. O registro do Espantalho usa a comparação estruturada auxiliar 5e.tools; validação da ação contra o PDF licenciado continua pendente. Os suplementos não avançam antes da conclusão do núcleo.
