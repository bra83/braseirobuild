# STAR WARS — EDGE OF THE GALAXY — R54 P9 TTS + MUNDO VIVO

Data: 2026-09-18
Base: R54 P8 Semantic Closure

## TTS

- Android TTS agora divide narrações longas em blocos seguros e usa `QUEUE_ADD` após o primeiro bloco; não depende de um único utterance gigante.
- Se o usuário tocar para narrar enquanto o motor Android ainda inicializa, a narração fica pendente e começa assim que o TTS estiver pronto.
- Gemini TTS tem executor dedicado: não fica atrás da fila de imagem/Mestre.
- Gemini TTS é transmitido por blocos: primeiro bloco curto (~420 caracteres) para reduzir tempo até o primeiro áudio; blocos seguintes de até ~850 caracteres.
- O primeiro arquivo de áudio toca assim que fica pronto; os demais são gerados enquanto a reprodução prossegue.
- Fila MediaPlayer usa `prepareAsync()` e segue automaticamente para o próximo bloco.
- Web Speech fallback também foi dividido em blocos sequenciais.
- Parar TTS cancela sessão e limpa a fila.

## Mundo Vivo — estrutura adaptada do Braseiro DnD

Foi usada como referência de engenharia a implementação funcional DnD (`DND_6.6.2_SANDBOX_WORKING_PATCH_LATEST.zip`), adaptando o modelo ao estado e às entidades Star Wars, sem copiar regras de D&D.

O estado Star Wars agora mantém/avança:

- tempo persistente (dia + período do dia);
- clima por local;
- pressão de mercado, oferta, demanda, segurança e acesso de rota;
- riqueza individual dos NPCs e faixas de condição econômica;
- ocupação e atividade autônoma;
- movimentação autônoma pelas rotas reais do sandbox;
- envelhecimento e mortalidade fora de cena em probabilidade conservadora;
- memória persistente de NPC;
- relações NPC↔entidade com deltas persistentes;
- compromissos/promessas/dívidas estruturadas e vencimento;
- planos e poder/tesouro de facções;
- relações entre facções;
- tratados e guerras estruturados quando eventos do mundo os estabelecem;
- eventos recentes do mundo incorporados à memória e ao estado;
- Barbara recebe snapshot do Mundo Vivo no contexto do Mestre;
- painel MUNDO VIVO mostra dia/período, clima, mercado e estado dos NPCs.

A simulação não altera créditos do jogador por conta própria.

## Validação

- Gate focado P9: 46/46 PASS.
- Regressão Node regular completa: **769/769 PASS**.
- Bundle offline recompilado com 57 módulos.
- `TtsBridge.java` e `GeminiBridge.java`: verificação `javac` chegou apenas a dependências Android ausentes; nenhum erro sintático Java foi detectado.

## Limitações que exigem aparelho/serviço externo

- Não é possível medir nesta sessão a latência real da voz Charon contra o serviço Gemini, porque isso exige chave válida + rede + aparelho/Android host.
- Não é possível confirmar reprodução acústica real de todos os blocos sem aparelho; a fila e os contratos de continuidade estão cobertos por testes estáticos/regressão.
- As 6 suítes `stress-*` continuam fora deste gate regular; serão tratadas separadamente porque algumas excedem a janela normal de execução.
- APK não foi gerado neste runtime sem Android SDK/Gradle disponível.
