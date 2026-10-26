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


## Bloco 1.3.15-dev — Itens e sucessos extras
- Itens do Mistério permanecem até Mudança; um pode substituir o Item Icônico (+2) antes de os demais serem perdidos.
- Os efeitos compráveis por sucessos extras das 12 Perícias foram codificados localmente e não dependem de RAG/Gemini.
- Bônus de informação, dispensa do mesmo Problema, sucesso cedido e pool de Liderança ganharam estado local persistente.
- Criação por Mecânica/Programação usa Item +1 base e upgrades locais até +3.
- XP de participação não pode ser negado a um Kid presente.
- Bloco somente código/teste; nenhum APK publicado.

## Bloco 1.3.16a-dev — fundação Things from the Flood
- Perfil mecânico explícito TALES_KIDS / FLOOD_TEENS adicionado ao estado e ao firewall do Mestre.
- Regras de criação Flood locais: Teens 14–19, 14 pontos de Atributos, 10 de Perícias e 10 Tipos oficiais codificados.
- RandomTeenFactory e conversão Kid→Teen redistribuem Atributos/Perícias em vez de carregar progressão mecânica antiga.
- Vergonha, aniversário, Cicatrizes, Hooks e Friction ganharam estado persistível; Sorte/Orgulho são bloqueados no perfil Flood.
- Pool mínimo de 1 dado e alvos 3x/4x/5x ficam exclusivos de Flood; Tales continua isolado em 2x/3x/4x.
- Motor/RAG recebe somente o contrato mecânico do perfil ativo.
- Este bloco é código/teste, sem APK.

## Bloco 1.3.16b-dev — correção da fundação Flood
- Corrigida a restauração do perfil FLOOD_TEENS dentro de Problema Estendido após save/load.
- Migrado teste antigo de autoridade mecânica para aceitar contrato dinâmico por perfil sem enfraquecer a separação RAG/mecânica.
- Nenhum APK publicado.

## Bloco 1.3.16c-dev — Cicatrizes, Friction e Problema Estendido Flood
- Ficar Quebrado em Flood agora exige registrar uma Cicatriz antes de recuperação.
- Cicatriz concede um único sucesso automático; ao ser usada torna-se aceita e pode ser curada após sessão com aprovação do grupo.
- Da 2ª à 5ª Cicatriz há teste local de permanência; a 6ª remove o Teen automaticamente.
- Friction pode curar Condições em cena segura/intimista; qualquer Trouble na mesma cena cancela toda a cura.
- Problema Estendido Flood usa 3x/4x/5x; cena de Liderança cria pool e soma zero ao Threat; falha completa deixa todos Quebrados e exige Cicatriz do Teen ativo.
- Recuperação por Esconderijo permanece exclusiva de Tales/Kids.
- Nenhum APK publicado.

## Bloco 1.3.16d-dev — seleção Tales/Flood + ciclo de personagem
- Menu ganhou seletor real TALES/KIDS ↔ FLOOD/TEENS, bloqueado durante campanha ativa.
- Factory, início de aventura, import/export JSON e Motor preservam o perfil selecionado.
- Flood Change agora permite Problem/Shame/Iconic Item/Relationships sem importar Drive/Pride de Tales.
- Flood aniversário aumenta apenas idade; aos 20 o Teen deixa o jogo.
- Cicatriz pendente bloqueia fechamento de XP antes de ser registrada.
- Nenhum APK publicado.

## Bloco 1.3.16e-dev — UI Flood ligada ao motor
- Ficha preserva layout aprovado e exibe Teen, aniversário, Shame, Hooks, Friction, Scars e registro de Scar pendente.
- Trouble esconde Sorte/Orgulho no Flood e expõe Shame + Scars; efeitos Tales ficam bloqueados na UI Flood até catálogo Flood local.
- Encerramento usa as cinco perguntas Flood, Change Flood, cura de Scars aceitas, Friction e idade até saída aos 20.
- Mochila troca Kid/Teen sem alterar mecânicas.
- Nenhum APK publicado.

## Bloco 1.3.16f-dev — efeitos e Itens Flood
- Flood ganhou catálogo local próprio de Bonus Effects, derivado das 12 Perícias do manual e com IDs separados de Tales.
- Reducer, Motor Barbara, UI de Trouble e validação de Itens consultam o perfil ativo via RulesProfiles.
- Flood aceita Itens mecânicos +1..+3, Item Icônico +2, criação com upgrades até +3 e perda de Itens não icônicos na Mudança.
- Efeitos de informação, waiver, Lead e cura usam o catálogo Flood; nenhum ID Tales é aceito no perfil Flood.
- Nenhum APK publicado.

## Bloco 1.3.16g-dev — Motor neutro por perfil + Friction jogável
- SYSTEM_PROMPT deixou de impor Tales/Kids sobre campanhas Flood; active_profile + mechanical_contract passam a decidir a mecânica.
- Recovery ganhou fonte Friction, aceita somente em Flood/Teens e sempre após aprovação ficcional do Motor.
- Grupo Flood mostra Teens + Friction, não a recuperação por Esconderijo de Tales.
- Botão de Friction pede uma cena ao Mestre; nunca cura diretamente pela UI.
- Nenhum APK publicado.
