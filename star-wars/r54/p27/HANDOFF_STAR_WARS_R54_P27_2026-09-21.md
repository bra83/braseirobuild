# STAR WARS R54 P27 — checkpoint de trabalho

Data: 2026-09-21

## Base
Aplicar somente sobre P25 completo + P26 validado. Não recriar projeto.

## Trabalho persistido
- Patch idempotente/abort-safe para corrigir a UI de criação conforme EOTE/AOR/MIXED.
- EOTE: Obrigação; AOR: Dever + Contribuição; MIXED: Obrigação + Dever + Contribuição simultaneamente.
- Terminologia visível PT-BR.
- Scaffold da aventura **Sob um Sol Negro**, ancorado no PDF do Drive do usuário (ID registrado no manifest), com Coruscant e bindings para assets/NPCs já presentes.
- Gate executável P27.

## Importante
Este checkpoint contém código de patch e gate, não afirma que o APK P27 foi compilado. A aplicação deve abortar se a assinatura P26 esperada não existir.

## Próximo passo
1. Materializar P25+P26.
2. Executar apply_p27_campaign_ui.py <raiz>.
3. Executar gate_p27.py <raiz>.
4. Integrar cenas da aventura a partir do PDF do usuário, sem substituir regras EotE.
5. Rodar regressão completa e Gradle/self-hosted.
