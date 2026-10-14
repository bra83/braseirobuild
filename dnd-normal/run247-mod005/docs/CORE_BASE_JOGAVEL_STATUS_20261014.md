# Núcleo básico jogável — estado em 14/10/2026

## Escopo definido pelo proprietário

O primeiro objetivo é preparar o jogo base antes dos suplementos. O proprietário esclareceu que XMM é um dos livros básicos. Portanto, o núcleo deste trabalho é **XPHB 2024 + XMM 2025 + XDMG 2024**. Suplementos e expansões ficam depois desse núcleo.

## Entregue nesta rodada

- Campanhas D&D novas começam no perfil revisado **2024/2025**. Campanhas já salvas em 2014 mantêm essa edição; o teste reabre o banco e confirma a preservação.
- Um teste Robolectric abre a tela de campanha e navega pelas telas de Heróis, Combate, Dados e Mapa.
- Dois efeitos compostos do XMM agora têm aplicação persistente em combate e expiração no início do próximo turno da criatura de origem: Rakshasa / *Baleful Command* aplica frightened e incapacitated; Storm Giant / *Thunderbolt* aplica blinded e deafened.
- O comparador independente continuou conferindo os 503 monstros e as 36 seções XMM.

## Evidência local

Commit de código: `5b4bdf056be0a5689f1506f666cd15deacd95b3d`.

O manifesto, XMLs Android, comparação-fonte e inventário das ações que ainda exigem tratamento estão em [`core-base-local-20261014-5b4bdf05`](editorial-evidence/core-base-local-20261014-5b4bdf05/manifest.json).

- Suíte Android/Robolectric: **2.506 testes, 0 falhas, 0 erros, 0 ignorados**, em 104 arquivos XML.
- Testes Python: **57 passaram**.
- Comparação XMM: 503 monstros, 36 seções, 120 ações de recarga, 132 registros de conjuração, 178 contratos de traços e 59 ações simples de salvaguarda conferidos.
- Efeitos compostos tipados: 2.

## Ainda não está fechado como núcleo pronto

A auditoria identifica **246 ações XMM com efeitos ainda sem implementação tipada**. Elas continuam bloqueadas no runtime, então esta rodada não afirma que todos os monstros já podem ser usados normalmente em combate. Também continuam pendentes o CI BRELGA neste SHA e a instalação/abertura em Android. Não havia aparelho ADB conectado; não foi gerado APK nesta rodada.

O núcleo básico permanece em andamento. A próxima implementação fica no XMM: dar consequência persistente aos efeitos restantes ou encaminhá-los por uma decisão explícita e aplicável do Mestre, sempre com prova fonte→runtime→estado. Os suplementos não avançam antes da conclusão do núcleo.
