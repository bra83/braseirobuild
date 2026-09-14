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
- TTS: voz Gemini usa áudio de mídia e timeout menor; em falha cai para TTS Android. Narrativas longas são divididas em trechos seguros em vez de falharem silenciosamente.
- TTS Android tenta pt-BR, pt-PT, idioma do aparelho e fallback final; informa quando a voz não pôde iniciar.
- Correções e ícone da 1.3.10 preservados.

## Bloco 1.3.12
- MAPA TOTALMENTE INTERATIVO: pinos agora têm área de toque real e selecionam o local.
- Ao selecionar um local revelado/visitado aparece ação funcional para solicitar viagem diretamente pelo mapa.
- FILTROS agora alterna entre revelados, visitados e local atual.
- CONEXÕES liga/desliga as conexões reveladas entre locais.
- ANOTAÇÕES liga/desliga pistas e notas dos Kids sobre o mapa.
- Os cartões de locais revelados também são clicáveis e sincronizam a seleção do mapa.
- Toques próximos aos pinos também são reconhecidos para melhorar o uso no celular.
- Rotas dinâmicas e correção de TTS da 1.3.11 foram preservadas.

## Bloco 1.3.13 — autoridade mecânica local / inventário real
- Regras mecânicas centrais Tales/Kids consolidadas em autoridade local determinística; Gemini/RAG não pode definir regra.
- Contexto de manuais importados passou a ser somente referência opcional de ambientação/lore; conflitos mecânicos são ignorados pelo Motor.
- MOCHILA deixou de exibir Lanterna/Fita/Caderno/Binóculos fictícios fixos. Agora mostra somente objetos realmente presentes no estado do Kid.
- Objetos comuns têm bônus 0. Itens do Mistério guardam seu bônus específico +1..+3. Item Icônico permanece +2.
- Mestre pode adicionar/remover Itens reais por ações validadas; bônus proposto só entra se nome/tipo/valor coincidirem exatamente com o estado persistido.
- Itens do Mistério são removidos no limite de fim de Mistério; objetos comuns persistem.
- Inventário e música favorita passam a persistir em save e JSON do Kid. Saves antigos migram com inventário vazio, sem itens inventados.
- Locais continuam sendo estado dinâmico revelado pelo Mestre, não catálogo rígido nem dependência de RAG.
- TAL-EDGE-001 continua bloqueado: nenhuma regra Flood de mínimo 1 dado foi importada.

## Bloco 1.3.14 — recuperação e Mudança canônicas
- Recuperação genérica removida: Âncora e Esconderijo qualificados curam todas as Condições; conforto por Perícia cura exatamente uma Condição leve e nunca cura Quebrado.
- Motor precisa declarar a fonte da recuperação; sem fonte, a cura é rejeitada em vez de inventada.
- Entre Mistérios, Problema, Ímpeto, Orgulho, Item Icônico e Relacionamentos podem ser revisados em estado próprio de Mudança.
- Próximo Mistério só inicia depois da revisão de Mudança e da revisão de aniversário.
- Confirmação de Mudança persiste no save; edição continua proibida durante jogo ativo.

## Evidência executada no GitHub — 1.3.14
- regras locais + inventário + independência de RAG + Recuperação + Mudança: PASS no workflow 34792323959.
- assembleRelease, assinatura e validação Android: PASS.
- APK publicado na release TALES-1.3.14.
