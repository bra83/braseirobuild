# P156 — Conferência parcial de *Beyond the Rim* com o PDF fornecido

Fonte do usuário: `Beyond the Rim.pdf` no Google Drive, ID `1z-GpNm5QU1RGJSUdrP4vYbfBExTlF531`, texto extraído pelo conector em 12/10/2026.

## Comparado e implementado

- **Página impressa 29:** ficha textual de *Yiyar Clan Members* (minions): grupo usa Athletics, Discipline, Melee, Piloting (Planetary), Ranged (Light), Stealth. Equipamento: pistola blaster (dano 6, crítico 3), vibrofaca (dano 3, crítico 3, Pierce 2, Vicious 1), armadura acolchoada (+2 soak), comunicador. Código: `rules/adversaries.mjs`.
- **Página impressa 29:** *“Too Low” Talo*, rival, com perícias, talento Adversary 1 e armamento textual cotejados. A ação em Scrapheap Point é anunciada no próprio perfil e desenvolvida no episódio III.
- **Episódio III, páginas ~84–87:** ataque do clã Yiyar a Scrapheap Point, liderado por Talo e apoiado por mercenários trandoshanos. Encontro `btr-yiyar-scrapheap-raid` adicionado ao local `dropoff-point`, com resultados persistentes distintos.
- **Episódio III, quadro *Imperial Deployment*:** marcos de 5, 10, 15, 25 e 30 minutos implementados como relógio explícito, persistente e idempotente, com agravamento ao fim do prazo.

## Limites declarados

- Os valores gráficos de características, limiares de ferimentos e proteção total dos NPCs **não** foram transcritos das caixas de atributos do PDF. Valores provisórios seguem identificados em `source.provisionalFields`.
- O perfil de mercenário trandoshano permanece provisório. A presença narrativa está confirmada, mas os números e armamento ainda exigem conferência visual.
- O novo encontro e o relógio **não** certificam o livro completo nem as cinco aventuras.
- Os quatro encontros de *Além da Orla* continuam identificados como `BRASEIRO_PROVISIONAL_NOT_PDF_CERTIFIED` até a certificação editorial integral.
