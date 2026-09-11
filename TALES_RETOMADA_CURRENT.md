# TALES FROM THE LOOP — RETOMADA ATUAL

Atualizado em 11/09/2026.

## Base preservada
- Continuidade parte da versão 1.3.8 já compilada no GitHub.
- Preservadas as correções anteriores: início de aventura, TTS/fallback, opções de ação, parser, texto maior, narrativa sensorial e item icônico coerente.
- Nenhuma tela aprovada foi redesenhada.

## Bloco 1.3.9
- GRUPO: o Kid atual agora usa a mesma foto/retrato personalizado exibido em KID, em vez do retrato genérico antigo do membro do grupo.
- IMAGEM DA CENA: o prompt enviado ao Gemini agora usa o local atual e a narração visível recente como autoridade. A referência de composição da biblioteca foi removida do prompt.
- O título do Mistério não pode mais deslocar visualmente a cena para outro local.

## Verificações exigidas neste bloco
- contrato estático das duas correções;
- testes Gradle da fonte;
- compilação Android real no GitHub;
- assinatura/zipalign/validação do APK Android 10;
- teste final no aparelho continua sendo evidência do usuário, não será fingido por automação.

## Próximo teste no aparelho
1. Abrir KID, trocar/importar a foto do Kid e conferir a mesma foto em GRUPO.
2. Em uma cena diante da escola, usar ILUSTRAÇÃO e confirmar que o prompt descreve/resume a cena atual e mantém a escola como local.

## Evidência executada no GitHub
- testDebugUnitTest: PASS no workflow 34561347348.
- assembleRelease: PASS no workflow 34561347348.
- assinatura, zipalign, package, versionCode 13, targetSdk 28 e ABIs ARM: PASS.
- APK publicado na release TALES-1.3.9.
