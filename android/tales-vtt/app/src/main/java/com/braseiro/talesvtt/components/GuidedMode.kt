package com.braseiro.talesvtt.components

import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.MotorRuntimeStatus
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.model.AppDestination
import androidx.compose.runtime.compositionLocalOf

/** Read-only guide; never dispatches rules, advances time, reveals secrets, or triggers GM requests. */
data class GuideContext(
  val game: GameState = GameState(),
  val motor: MotorRuntimeStatus = MotorRuntimeStatus(),
  val adventureStarted: Boolean = false,
  val hasGeminiKey: Boolean = false
)

val LocalGuideContext = compositionLocalOf { GuideContext() }

data class GuideStep(val instruction: String, val reason: String)
data class GuidedPage(
  val id: String,
  val title: String,
  val rule: String,
  val next: String,
  val why: String,
  val steps: List<GuideStep>,
  val destination: AppDestination? = null,
  val destinationLabel: String? = null
)

/**
 * Deterministic advice based only on player-visible state. This is not a second rules engine.
 * Source for numerical mechanics is the same TestRequest/GameState used by TroubleScreen.
 */
object GuidedMode {
  fun resolve(destination: AppDestination, screenTitle: String, context: GuideContext): GuidedPage {
    val g = context.game
    val teen = g.rulesProfile == RulesProfile.FLOOD_TEENS
    val who = if (teen) "Teen" else "Kid"
    val title = screenTitle.lowercase()
    val test = g.activeTest
    val result = g.testResolution

    // Internal routes often use SCENE or MYSTERY as the bottom navigation destination.
    if (title.contains("encerramento")) return GuidedPage(
      "ending_${g.endingMode}", "Fechamento e experiência",
      "Experiência e evolução são aplicadas pela tela e pelo motor de regras, não pelo guia.",
      "Revise as perguntas de XP e confirme as alterações na tela antes de começar a próxima sessão.",
      "Assim a história pessoal e as consequências ficam registradas.",
      listOf(GuideStep("Responda às perguntas da sessão.", "Não marque respostas que não aconteceram."),
        GuideStep("Revise relações, condições e mudanças.", "A ficha acompanha o que ocorreu na ficção."),
        GuideStep("Confirme o fechamento na tela.", "Somente o motor do jogo altera o estado."))
    )
    if (title.contains("clímax")) return GuidedPage(
      "climax_${g.extended.resolvedScenes}", "Problema estendido",
      "Um desafio prolongado envolve contribuições dos participantes e um alvo coletivo definido pelo sistema.",
      "Confira o progresso e resolva a próxima cena do grupo; não é preciso inventar um alvo novo.",
      "Os resultados de cada integrante devem ser considerados antes da resolução coletiva.",
      listOf(GuideStep("Observe o objetivo do problema.", "A ameaça está descrita na tela."),
        GuideStep("Resolva as contribuições pendentes.", "O motor registra sucessos, condições e consequências."),
        GuideStep("Confirme a conclusão ou o compromisso permitido.", "O guia não escolhe pelo jogador."))
    )
    if (title.contains("voz")) return GuidedPage(
      "voice_settings", "Configuração de voz",
      "O modelo de texto do Mestre e o modelo de síntese vocal são independentes; mudar a voz não muda a história.",
      "Selecione Android/Kokoro/Gemini conforme disponibilidade, escolha uma voz e configure perfil e estilo. Teste antes de voltar.",
      "As preferências permanecem salvas e instruções de voz não devem aparecer como texto narrado.",
      listOf(GuideStep("Escolha o motor vocal.", "Android e Kokoro usam os serviços compatíveis instalados; Gemini requer rede e cota."),
        GuideStep("Selecione modelo e voz.", "Nem todo modelo suporta todas as vozes."),
        GuideStep("Preencha perfil e estilo vocal.", "Descreva timbre e interpretação sem inserir essas instruções na história."),
        GuideStep("Teste uma frase.", "Confira som, pausas e conclusão, depois retorne ao jogo."))
    )
    if (title.contains("aventura")) return GuidedPage(
      "adventures_${context.adventureStarted}", "Criar, importar ou continuar aventura",
      "Aventuras JSON possuem locais, personagens e pistas. Só informações descobertas podem ser mostradas ao jogador.",
      "Importe um pacote compatível, gere um mistério com a Bárbara ou retome a aventura existente.",
      "Uma aventura nova não deve apagar a cronologia dos personagens; o Guia não inicia aventuras automaticamente.",
      listOf(GuideStep("Escolha importar ou criar.", "Um JSON inválido deve ser rejeitado antes de começar."),
        GuideStep("Confirme o perfil Tales ou Flood.", "As regras não se misturam."),
        GuideStep("Inicie ou continue o mistério.", "O Mestre deverá escrever a primeira cena."))
    )
    if (title.contains("biblioteca")) return GuidedPage(
      "library", "Biblioteca de regras",
      "Livros e consultas orientam, mas não alteram automaticamente as regras locais.",
      "Importe um PDF do seu acervo e procure o tema da sua dúvida.",
      "A Biblioteca permite conferir detalhes sem avançar a campanha.",
      listOf(GuideStep("Importe o livro correspondente ao perfil ativo.", "Tales e Flood têm regras diferentes."),
        GuideStep("Pesquise um termo específico.", "Confira sempre a fonte retornada."))
    )
    if (title.contains("conexões")) return GuidedPage(
      "connections", "Cruzar pistas",
      "Aqui entram somente conexões e pistas já reveladas ao jogador.",
      "Compare o que foi descoberto e volte à cena para investigar a próxima hipótese.",
      "Um palpite não transforma uma informação secreta em fato confirmado.",
      listOf(GuideStep("Leia as pistas conhecidas.", "Separe evidência de suspeita."),
        GuideStep("Observe as conexões.", "Pessoas, locais e eventos podem estar ligados."),
        GuideStep("Formule uma ação na cena.", "O Mestre reage à tentativa; não ao guia.")),
      AppDestination.SCENE, "VOLTAR À CENA"
    )
    if (destination == AppDestination.SCENE && test != null && result == null) return GuidedPage(
      "trouble_before_${test.reason.hashCode()}", "Antes da rolagem",
      "Pool exibido pelo sistema: ${g.finalPool()} dados de seis faces. Cada 6 conta como um sucesso; alvo: ${test.requiredSuccesses}. Ajuda válida deve ser escolhida antes de rolar.",
      "Confira a perícia, peça ajuda de outro $who presente se fizer sentido e toque em ROLAR DADOS.",
      "Uma rolagem não deve ser refeita só porque o resultado não agradou; use apenas os recursos permitidos pela tela.",
      listOf(GuideStep("Leia por que o Mestre pediu o teste.", "O risco determina a consequência narrada."),
        GuideStep("Revise ${test.attributeName} e ${test.skillName ?: "atributo sem perícia"}.", "O cálculo e as condições vêm do motor de regras."),
        GuideStep("Peça ajuda antes de lançar os dados.", "O ajudante precisa estar disponível e participar de verdade."),
        GuideStep("Role e observe as faces.", "Os números visíveis são os mesmos registrados pelo sistema."))
    )
    if (destination == AppDestination.SCENE && test != null && result != null) return GuidedPage(
      "trouble_after_${test.reason.hashCode()}_${result.reportedToGm}_${context.motor.busy}",
      if (context.motor.busy) "Mestre processando o teste" else "Depois da rolagem",
      "Você obteve ${result.successes} sucesso(s) contra o alvo ${test.requiredSuccesses}. As faces são ${result.dice.joinToString()}. " +
        (if (teen) "No Flood, verifique as opções de Vergonha/Cicatrizes disponíveis." else "No Tales, verifique Sorte, Forçar e Orgulho quando disponíveis."),
      when {
        context.motor.busy -> "O resultado está sendo processado. Não role novamente; observe o status do Mestre."
        result.reportedToGm -> "Resultado enviado. Aguarde ou consulte o estado do Mestre na cena."
        else -> "Use somente os ajustes permitidos na tela e toque em ENVIAR RESULTADO AO MESTRE."
      },
      "O guia não envia dados nem decide o resultado; acompanha o estado persistido pelo jogo.",
      listOf(GuideStep("Leia as faces e o total de sucessos.", "As faces são o registro mecânico verificável."),
        GuideStep("Avalie recursos opcionais disponíveis.", "Aplique mudanças somente pelos botões de regras."),
        GuideStep("Envie o resultado uma única vez.", "A cena avança apenas após uma resposta aceita do Mestre."))
    )
    if (destination == AppDestination.MENU) return GuidedPage(
      "menu_${context.adventureStarted}_${context.hasGeminiKey}", "Preparar e continuar a campanha",
      "Escolha Tales/Kids ou Flood/Teens, crie/importe personagens e inicie ou continue uma aventura. A IA narra; as regras são resolvidas pelo VTT.",
      when {
        !context.hasGeminiKey -> "Configure a chave e o modelo de texto do Gemini para usar o Mestre. Aventuras locais podem existir sem conexão."
        !context.adventureStarted -> "Crie ou importe uma aventura, depois entre na CENA."
        else -> "Continue a aventura pela CENA e use o ícone ? em cada etapa."
      },
      "Este guia fica disponível durante toda a campanha sem criar eventos nem consumir sua cota de IA.",
      listOf(GuideStep("Selecione o sistema correto.", "Kids e Teens usam regras diferentes."),
        GuideStep("Confira as fichas do grupo.", "Cada personagem tem seus próprios atributos."),
        GuideStep("Configure os motores necessários.", "Voz e texto são opções independentes."),
        GuideStep("Inicie/continue a história.", "A cena apresenta opções e ação livre.")),
      if (context.adventureStarted) AppDestination.SCENE else AppDestination.KID,
      if (context.adventureStarted) "IR À CENA" else "CONFERIR FICHA"
    )
    if (destination == AppDestination.KID) return GuidedPage(
      "kid_${g.kid.id}", "Ficha de ${g.kid.name}",
      "$who ativo: ${g.kid.name}. Há ${g.group.members.size} integrante(s) no grupo; não significa que só exista uma ficha.",
      "Use o seletor de integrantes na tela para conferir o $who desejado e suas condições.",
      "Trocar o personagem ativo preserva a ficha individual; durante testes em andamento a troca fica bloqueada.",
      listOf(GuideStep("Confira idade, atributos e perícias.", "Eles compõem os pools de rolagem."),
        GuideStep("Observe condições e recursos.", "Condições alteram a capacidade de agir."),
        GuideStep("Selecione outro integrante se necessário.", "Cada ficha pertence a uma pessoa diferente."),
        GuideStep("Volte à cena para agir.", "A ficha é consulta, não uma ação fictícia.")),
      AppDestination.GROUP, "VER O GRUPO"
    )
    if (destination == AppDestination.GROUP) return GuidedPage(
      "group_${g.group.members.size}_${teen}", "Seu grupo de ${g.group.members.size} ${if (teen) "Teens" else "Kids"}",
      if (teen) "Observe relações e Friction; use a resolução permitida pelo sistema." else "Relações, esconderijo e ajuda do grupo fazem parte da aventura.",
      "Confira quem está disponível e volte à cena. Em testes, a ajuda só é aplicada antes da rolagem.",
      "O botão de ajuda do teste exige um companheiro elegível; o guia não concede bônus.",
      listOf(GuideStep("Leia os nomes do grupo.", "Não confunda personagem ativo com todos os presentes."),
        GuideStep("Observe relações e condições.", "A disponibilidade afeta a participação."),
        GuideStep("Use a ajuda pelo botão do teste.", "É preciso vincular um ajudante ao desafio real.")),
      AppDestination.KID, "ABRIR FICHAS"
    )
    if (destination == AppDestination.MYSTERY) return GuidedPage(
      "mystery_${g.revealed.clues.size}", "Investigue sem spoilers",
      "Pistas conhecidas: ${g.revealed.clues.size}. A tela mostra apenas fatos já descobertos.",
      "Revise pistas e conexões; depois formule uma hipótese e aja na CENA.",
      "Nem todas as pistas são provas conclusivas, e o guia nunca revela segredos privados do Mestre.",
      listOf(GuideStep("Revise as pistas vistas.", "O que o personagem sabe limita suas decisões."),
        GuideStep("Busque relações entre fatos.", "Conexões são hipóteses até serem verificadas."),
        GuideStep("Investigue na cena.", "O Mestre avalia consequências e novas descobertas.")),
      AppDestination.SCENE, "VOLTAR À CENA"
    )
    if (destination == AppDestination.MAP) return GuidedPage(
      "map_${g.scene.location}", "Locais e deslocamento",
      "Você está em ${g.scene.location}; a cena registra ${g.scene.time} e ${g.scene.daylight.lowercase()}.",
      "Selecione um destino permitido e declare a viagem, observando o retorno do Mestre.",
      "O guia não teletransporta, não altera relógio e não revela locais desconhecidos.",
      listOf(GuideStep("Localize a posição atual.", "O mapa exibe somente locais aprovados."),
        GuideStep("Escolha onde tentar ir.", "Tempo e acesso dependem do mundo e das regras."),
        GuideStep("Confirme o deslocamento na tela.", "Somente o jogo registra a viagem.")),
      AppDestination.SCENE, "VOLTAR À CENA"
    )
    if (destination == AppDestination.BACKPACK) return GuidedPage(
      "backpack_${g.kid.items.size}", "Objetos e equipamento",
      "Itens disponíveis na ficha: ${g.kid.items.size}. Um item só dá bônus se as regras e o contexto permitirem.",
      "Escolha o objeto e declare como pretende usá-lo; o Mestre e o motor decidem seu efeito.",
      "Descrição narrativa e bônus mecânico são coisas distintas.",
      listOf(GuideStep("Observe seus itens.", "Nem todo objeto pode ser usado em qualquer teste."),
        GuideStep("Diga como ele ajuda.", "O contexto deve justificar o uso."),
        GuideStep("Confira o retorno do Mestre.", "O guia não cria bônus automaticamente.")),
      AppDestination.SCENE, "VOLTAR À CENA"
    )
    if (!context.adventureStarted) return GuidedPage(
      "no_adventure", "Comece uma aventura", "Você ainda não iniciou uma aventura nesta campanha.",
      "Abra o MENU, escolha criar/importar um mistério e volte à CENA.",
      "A preparação não consome tempo fictício.", listOf(GuideStep("Abra MENU e Aventuras.", "Use um mistério adequado ao perfil.")),
      AppDestination.MENU, "IR AO MENU"
    )
    return GuidedPage(
      if (context.motor.busy) "scene_busy" else "scene_${g.scene.title}",
      if (context.motor.busy) "Aguarde o Mestre" else "Sua cena atual",
      "Local: ${g.scene.location}. Horário: ${g.scene.time}, ${g.scene.daylight.lowercase()}. ${g.group.members.size} integrante(s) no grupo.",
      when {
        context.motor.busy -> "Aguarde o resultado. Não repita a ação enquanto o Mestre estiver processando."
        g.lastError != null -> "Leia o aviso da cena e tente novamente quando fizer sentido."
        else -> "Leia a narração, escolha uma das sugestões ou escreva uma ação livre."
      },
      "A Bárbara interpreta a intenção; o motor local valida testes e consequências. O guia não avança a história sozinho.",
      listOf(GuideStep("Leia o que acabou de acontecer.", "Observe o local e os personagens presentes."),
        GuideStep("Escolha uma ação ou descreva sua intenção.", "Você não fica limitado às escolhas prontas."),
        GuideStep("Aguarde o Mestre ou resolva o teste solicitado.", "Rolagens e efeitos são decididos pelo motor de regras.")),
      AppDestination.MYSTERY, "VER PISTAS"
    )
  }

  fun ruleCards(profile: RulesProfile): List<Pair<String, String>> {
    val shared = listOf(
      "Quando rolar" to "O Mestre solicita um teste quando uma ação tem risco e resultado incerto. Confira o pool e a dificuldade mostrados na tela.",
      "Resultado" to "Cada dado com face 6 vale um sucesso. O alvo aparece no teste; o guia não altera dados ou resultados.",
      "Ajuda" to "Se for plausível e o companheiro estiver disponível, selecione AJUDA antes da rolagem. O bônus é aplicado pelo motor local.",
      "Condições" to "Verifique condições e efeitos na ficha e na tela de rolagem; não invente modificadores.",
      "Vida e tempo" to "O relógio, as cenas, relações e consequências pertencem à cronologia da campanha, não ao guia."
    )
    return if (profile == RulesProfile.FLOOD_TEENS) shared + listOf(
      "Flood/Teens" to "Não use Sorte de Kids no Flood; recursos como Vergonha e Cicatrizes dependem das opções realmente disponíveis no teste."
    ) else shared + listOf(
      "Tales/Kids" to "Use Sorte, Orgulho e Forçar somente quando as opções estiverem ativas. O resultado é registrado pelo motor local."
    )
  }
}