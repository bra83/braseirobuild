# Braseiro D&D 2024 — correções adicionais após 6.6.38

Base confirmada: release \`dnd-6.6.38-preserved-narrative-tts-20261031\`.
Branch de integração isolada: \`fix/dnd-dialogue-alignment-leak-20261031\`.
A versão publicada 6.6.38 permanece intacta. Este relatório NÃO substitui os testes automatizados nem o teste físico no Galaxy A57.

## A — Alinhamentos (D&D 2024)
- Manter as nove opções já disponíveis no formulário: Leal/Neutro/Caótico × Bom/Neutro/Mau.
- Canonizar valores em inglês ou português apenas para a projeção narrativa, sem alterar a ficha persistida.
- Enviar o alinhamento declarado dos heróis ao barramento do Mestre e o alinhamento preexistente do NPC quando constar da memória ou statblock.
- Não inventar alinhamento para Kvothe, Wilem, Simmon ou Auri.
- Alinhamento é tendência/valores, não detecção de bem/mal, regra de dano, comando do Mestre nem restrição de agência.
- Não alterar Mystara.

## B — Diálogos
- Instruções do Mestre favorecem fala direta de NPCs efetivamente presentes em interações sociais, com identidade, relação e conhecimento contextual.
- Não reduzir todas as conversas a descrições como "conversam"; narrar troca real sem criar falas obrigatórias dos heróis manuais.
- Não criar interlocutores em viagem solitária ou cena vazia; não forçar falas em combate ou monólogo para atingir cota.
- Fallback offline responde brevemente apenas a abordagem social explícita de NPC presente registrado, sem supor segredo ou informação desconhecida.
- Preservar separação de falas por linhas e pontuação em todos os sanitizadores.

## C — Firewall narrativo
- Detectar metainstruções brutas antes de qualquer normalização/limpeza, conservar marcador \`_narration_leak_detected\` até o validador e bloquear mutações.
- Remover instruções vazadas em nível de sentença/linha preservando parágrafos e diálogos válidos, inclusive quando contíguos ao texto contaminado.
- Não remover aspas finais legítimas do personagem em narração normal.
- Não ocultar problemas de contaminação apenas com correção cosmética.
- Confirmar isolamento de IA antiga, concorrência, TTS, importação assíncrona e validade da base 6.6.38 por suíte completa.

## D — Aceitação
- Exigir JVM completo, testes novos de alinhamento, diálogos, vazamento e entrada JSON, suíte Wilem, Kvothe, Strixhaven e regras D&D 2024.
- Exigir versionCode crescente, mesma identidade de pacote e certificado da 6.6.38, DB_SCHEMA=61 e BACKUP_FORMAT=21.
- APK somente após CI aprovado. Testes físicos do Galaxy A57 permanecem explicitamente pendentes.
