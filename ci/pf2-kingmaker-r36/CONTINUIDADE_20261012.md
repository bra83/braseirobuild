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
