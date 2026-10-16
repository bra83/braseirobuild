# R54 P17 — TTS provider failover

## Correção
O runtime final tinha um caminho silencioso: quando `ttsProvider=gemini` e `BraseiroGemini.speakTts()` retornava `{ok:false}` (chave ausente, erro remoto, quota etc.), `speak()` devolvia a falha imediatamente e não alcançava Android TTS nem Web Speech.

P17 mantém Gemini/Charon como primeira escolha, mas uma falha de inicialização agora cai para Android PT-BR; se o bridge Android não existir, cai para Web Speech. Um sucesso Gemini continua retornando imediatamente, portanto não há fala duplicada.

## Validação
- gate focal novo + gates TTS existentes: 3/3 PASS
- regressão regular completa: 784/784 PASS, 0 fail, 0 skipped
- stress: nova tentativa iniciou as suítes separadamente, mas a janela do host terminou durante a terceira; não foi registrada como PASS. A última prova integral anterior continua 6/6 PASS. A mudança P17 é isolada em `r46/final-runtime.js` e não altera motor de campanha/mundo vivo.

## Ainda depende de ambiente real
- medir latência até primeiro áudio Charon/Gemini com chave/rede reais;
- confirmar auditivamente transição entre blocos;
- build/instalação Android quando Gradle/rede estiverem disponíveis.
