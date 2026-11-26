# ECO — inteligência clínica, perspectivas e fundamentação — 26/11/2026

## Base anterior
CI 45749898966, 89c143e, success (separação neural). Esta rodada não altera ASR.

## Entrega
- 6158e6c: contrato de seis campos para respostas por autor, citações de conceito/hipótese/justificativa e perguntas abertas; rejeição de referências inexistentes, formato incompleto e alguns padrões categóricos ou prescritivos; 42 perguntas didáticas distintas para os 21 autores. Minutas recusadas não entram na leitura principal. Roteiro editorial explicitamente identificado, não simulando autoria nem geração neural. Comparação lado a lado dos focos, sem ranking de teorias.
- Recuperação e reranking independentes por autor, com NLI automático entre conceito teórico produzido e passagens citadas. Possível incompatibilidade forte ou retorno inválido leva ao roteiro editorial. Sustentação indeterminada é sinalizada, nunca apresentada como diagnóstico ou validação de fatos clínicos.
- As 42 fichas autorais passam a integrar o corpus geral: 215 passagens antes de materiais pessoais, preservando as 173 anteriores.
- Arquivamento de síntese exige referências numeradas válidas; no modo integrado exige fontes de ambos os tipos. Revisão humana e verificação de alterações de fontes continuam obrigatórias.
- e69e48e: prompt nativo ajustado e cache segregado por caso nas consultas de perspectivas. Auditoria do GGUF real com três casos fictícios (Freud, Klein, Winnicott), inclusive uma fala com instrução adversarial. Usa o mesmo validador de formato do aplicativo e contabiliza aceitação neural e fallback separadamente. APK exige presença desse relatório. Não confundir gate de contrato com precisão interpretativa, nem exigir que uma hipótese seja verdade.
- 6b2880d: regressão de Chromium atualizada para exigir 215 passagens e exatamente 42 novas fichas de autoria. A execução 45752176201 passou lógica e falhou na contagem antiga (215 vs 173); corrigida.

## Correções bibliográficas efetivas (metadados)
1. Freud, Recordar, repetir e elaborar (1914): registro específico Standard Edition 12:145–156. https://pep-web.org/browse/document/se.012.0145a?index=27
2. Klein, A Contribution to the Psychogenesis of Manic-Depressive States (1935): IJP 16:145–174, registro bibliográfico contemporâneo (não texto integral). https://www.persee.fr/doc/psy_0003-5033_1935_num_36_1_30695
3. Winnicott, Ego Distortion in Terms of True and False Self: registro de capítulo na edição Collected Works (não verificação de páginas da edição de 1965). https://academic.oup.com/book/1192/chapter-abstract/140028848
4. Bromberg, The Shadow of the Tsunami (2011): corrigido link que apontava a outra obra de 1998. https://www.routledge.com/The-Shadow-of-the-Tsunami-and-the-Growth-of-the-Relational-Mind/Bromberg/p/book/9780203834954

## Limites
Não houve revisão integral das obras primárias e direitos de reprodução; são sínteses didáticas, não trechos primários. O detector de padrões é heurístico e incompleto, e NLI não valida uma interpretação psicanalítica. O gate com Qwen mede estrutura, não qualidade clínica; taxa de aceitação neural deve ser divulgada mesmo se zero. Não há validação clínica humana de todo o catálogo. CI final ainda não concluído. Não afirmar 100%.
