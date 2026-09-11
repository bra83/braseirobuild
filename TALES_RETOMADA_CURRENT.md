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

## Bloco 1.3.10
- ÍCONE DO APP: launcher Android substituído pelo logo empilhado TALES / FROM THE / LOOP da referência visual aprovada.
- O retrato de personagem selecionado incorretamente no primeiro ensaio deste bloco foi descartado antes de qualquer APK ser publicado.
- Ícones normal, redondo e adaptativo foram atualizados sem alterar telas aprovadas.
- Correções 1.3.9 preservadas integralmente.

## Bloco 1.3.11
- MAPA: adicionada camada dinâmica de rota percorrida. A rota cresce somente quando o Mestre muda o local real da cena, acompanha o progresso da aventura e é salva/restaurada com a campanha.
- MAPA: removidos os dois cartões redundantes abaixo do mapa ("LOCAL ATUAL" e "AÇÃO NO MAPA"). O local atual já está no topo e a viagem continua pelos destinos revelados, sem ocupar espaço com informação repetida.
- TTS: voz Gemini usa áudio de mídia e timeout menor; em falha cai para TTS Android. Narrativas longas são divididas em trechos seguros em vez de falharem silenciosamente.
- TTS Android tenta pt-BR, pt-PT, idioma do aparelho e fallback final; informa quando a voz não pôde iniciar.
- Correções e ícone da 1.3.10 preservados.

## Evidência executada no GitHub — 1.3.11
- testes automatizados de rota, persistência e TTS longo: PASS no workflow 34656622825.
- assembleRelease: PASS no workflow 34656622825.
- assinatura, zipalign, versionCode 15, targetSdk 28, ícone e ABIs ARM: PASS.
- APK publicado na release TALES-1.3.11.

## Próximo teste no aparelho
1. Mudar de local durante a aventura e abrir MAPA: confirmar que a rota aparece e cresce conforme os deslocamentos.
2. Fechar/reabrir ou restaurar save e confirmar que a rota continua registrada.
3. Tocar OUVIR (TTS) numa narração curta e numa narração longa; se Gemini falhar, a voz do aparelho deve assumir em vez de ficar em silêncio.
4. Confirmar que os cartões redundantes abaixo do mapa não aparecem mais.
