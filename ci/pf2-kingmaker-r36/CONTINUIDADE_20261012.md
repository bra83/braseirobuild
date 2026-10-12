# Atualização de execução — 12/10/2026, após pedido de upgrades/campanha/companheiros

Código atual: 08768ac7703d541f2c126bbafe880819f2577035. Não declarar os três blocos integralmente encerrados.

- Upgrades agora têm UI, execução transacional, pagamentos históricos com fonte, seleção no mesmo bloco preservando lotes antigos, reserva só dos lotes adicionais, retomada sem repagar, falha crítica com escombros a demolir, efeitos antigos ativos até conclusão, diferenças negativas devolvidas somente ao concluir, recibo cumulativo e conversão Wooden Wall→Stone Wall. Narradora usa build_structure + upgrade_from no mesmo backend. Commit1727bd9:289JVM/420regressões/Android Java PASS.
- Diário da campanha resolve objetivos por ID, registra evidência e ramificações, impede conclusão sem resolução e vincula os sete arcos publicados aos registros q_cg_*. Missões genéricas antigas não comprovam conclusão publicada. Identidades npc_* oficiais são reconciliadas, registros completos preservados em features inativas; decisões canônicas mais recentes prevalecem. Commit6301e95:299JVM/423regressões/Android Java PASS.
- Narradora pode enviar objective_resolutions com IDs e evidência; definições, recompensas e pré-requisitos ficam protegidos. Primeiro objetivo ativa a missão. AppDb valida o estado atualizado e timing dos companheiros; novas missões publicadas só pelo importador. SoloDirector recebe aventuras q_cg_* elegíveis por nível, disponibilidade, Vordakai e assentamento quando exigido. Últimos304JVM locais PASS; validação Android da versão08768ac está aguardando BRELGA.
- SEM APK. Não cancelar jobs de outros projetos. Alterações somente branch PF2Kingmaker.

Pendência de escopo substancial: conteúdo mecânico completo dos11capítulos/40partes e das7aventuras (todos os encontros, mapas, ramos, tesouros, consequências), atividades/benefícios de companheiros ainda ausentes e homologação funcional. Diário/progressão corrigidos NÃO equivalem a cobertura integral do livro. Continuar implementação; não apenas retestar nem declarar concluído.

---

# Continuidade — correções de 12/10/2026

O usuário autorizou fechar TODOS os blocos, sem declarar completude enquanto houver pendências. Exclusivo PF2 Kingmaker; R35 e demais VTTs intocados. Não gerar APK antes de terminar implementações.

## Código entregue

- 9f54a3e: crescimento real por blocos/lotes completos, contiguidade, habitação, nível do Reino e novas malhas. Transação de construção atualiza porte/consumo/influência e XP uma única vez. Promoção administrativa bloqueada. JVM273, regressões419, Android Java PASS (job114983429205).
- 745b7cb5: effects_json.construction_payments por exemplar; pagamento líquido após devoluções de sucesso crítico; gratuito=zero; custos legados desconhecidos preservam índice como authority=unknown. Demolição corta recibo correspondente. JVM278/regressões419 PASS; compilação desse run interrompida por execução seguinte.
- e7b1e647: 26 relações de upgrade impressas e cálculo de diferença contra pagamento comprovado. Inclui Luxury Store→Magic Shop; Watchtower→Keep rejeitado. Diferenças negativas preservadas conforme p543. Recibos antigos desconhecidos rejeitam cálculo. JVM282 local PASS e contrato de integração de recibos PASS. Run38302084509/job114987427549 em andamento ao registrar este checkpoint.

## Pendências reais — NÃO declarar encerradas

O cálculo de upgrade ainda precisa de UI/execução transacional completa: manter estrutura antiga efetiva até conclusão, reservar apenas lotes adicionais no mesmo bloco, tentativas futuras sem repagar, preservar recibo cumulativo e infraestrutura Wood→Stone. Método upgradeCost é fundamento, não fluxo pronto. Custos históricos devem ser declarados com prova, nunca inferidos do catálogo.

Continuam os outros blocos do handoff principal: Museum/Menagerie, deslocamento com ruas, quotas e capital, efeitos reais em personagens/lojas; encontros/mapas/tesouros/ramificações das11cap/40partes; conteúdo e atividades das7aventuras de companheiros, camping e recuperação real; aliases de saves e falsas conclusões; Helix em saving throws, quotas por ator e rotas administrativas; homologação Android não vazia, guerras/exércitos,3reaberturas,backup/export/import.

## Runtime de continuidade

Código atual em current/ci/pf2-kingmaker-r36 no scratch. PDFs originais Kingmaker.pdf e CompanionGuide.pdf disponíveis. javac via /usr/lib/jvm/java-17-openjdk-amd64/bin/java com.sun.tools.javac.Main; helpers/harness + org/json compilam em growth_classes. Repository.payments.fixture.java testa âncoras e contrato de pagamentos; não substitui reconstrução canônica do CI. Branch pf2-kingmaker-r36-official-rule-fixes-20261011. Continuar a implementação, sem pedir novo continue.
