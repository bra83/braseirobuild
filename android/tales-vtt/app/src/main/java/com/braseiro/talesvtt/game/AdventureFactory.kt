package com.braseiro.talesvtt.game

import com.braseiro.talesvtt.life.AdventurePackage
import com.braseiro.talesvtt.life.AdventurePackageValidator
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class AdventureSeed(
  val signature: String,
  val title: String,
  val location: String,
  val seasonYear: String,
  val weather: String,
  val time: String,
  val daylight: String,
  val hook: String,
  val anomaly: String,
  val importantNpc: String,
  val everydayPressure: String,
  val visualAsset: String
) {
  fun localOpeningNarrative(): List<String> {
    val weatherSense = when (weather) {
      "Chuva forte" -> "A água bate duro em telhados, bicicletas e placas; a sarjeta corre cheia, o ar cheira a asfalto molhado e tecido encharcado, e cada carro que passa empurra uma lâmina de água para a calçada."
      "Chuva" -> "A chuva fina mas constante deixa metal, vidro e asfalto brilhando; há cheiro de terra molhada e o ruído das gotas cobre as conversas mais distantes."
      "Garoa" -> "Uma garoa fria fica suspensa no ar, pousando no cabelo e nas mangas; o chão escurece devagar e as superfícies de metal ficam úmidas ao toque."
      "Neblina" -> "A neblina achata as distâncias e engole as partes mais afastadas do bairro; sons chegam abafados e sem direção clara, enquanto o ar úmido esfria a pele."
      "Vento frio" -> "Rajadas frias correm entre as construções, fazem placas vibrarem e folhas secas rasparem pelo chão; respirar fundo traz um gosto metálico de ar gelado."
      "Neve leve" -> "Flocos pequenos se prendem a telhados, ombros e guidões; os sons da rua parecem mais baixos, e o frio morde dedos e orelhas sempre que o vento aumenta."
      "Céu limpo" -> "A luz é nítida e alonga sombras no chão; o ar está seco, bicicletas rangem ao passar e vozes viajam com clareza entre as casas."
      else -> "O céu fechado espalha uma luz opaca sobre a região; o ar está frio e parado, e os ruídos cotidianos parecem mais próximos do que deveriam."
    }
    val placeSense = when (location) {
      "Escola Björkskolan" -> "A escola se ergue atrás de cercas baixas e janelas compridas. Há bicicletas presas aos suportes, cartazes úmidos junto à entrada e o cheiro de piso encerado escapando cada vez que a porta abre."
      "Praça de Björkäsen" -> "A praça se abre entre fileiras de casas baixas, estacionamento, bancos e um pequeno quiosque. Bicicletas se acumulam perto da calçada, vitrines refletem a rua e, acima dos telhados, as estruturas impossivelmente grandes da Loop dominam o horizonte."
      "Margem do Lago Mälaren" -> "A margem mistura pedras escuras, capim úmido e pequenos píeres. A água bate em madeira e casco de barco com um ritmo irregular, e o cheiro do lago se mistura ao de combustível distante."
      "Fazenda nos arredores" -> "A estrada perde o asfalto e passa entre campos, valas e construções de madeira. Há lama nas marcas de pneus, cheiro de palha e óleo de máquina, e muito espaço aberto para qualquer coisa ser vista de longe."
      "Ferro-velho" -> "Pilhas de metal, carcaças de carros e cercas tortas formam corredores estreitos. Ferrugem, borracha velha e óleo dão ao ar um cheiro pesado, enquanto chapas soltas rangem quando o vento passa."
      "Pedreira" -> "O terreno cai em degraus de pedra e cascalho, com poças fundas e equipamentos silenciosos nas bordas. Cada passo solta pedrinhas, e ecos fazem qualquer ruído parecer vir de dois lugares ao mesmo tempo."
      "Bairro residencial" -> "Casas parecidas se alinham em ruas calmas, com jardins pequenos, garagens e bicicletas deixadas perto das portas. Cortinas se mexem em algumas janelas; televisões e rádios vazam sons abafados para fora."
      "Ponto de ônibus" -> "O abrigo de vidro e metal fica ao lado de uma estrada longa. Há horários amassados presos ao poste, marcas de sapato no chão e o ruído de motores que cresce muito antes de qualquer veículo aparecer."
      "Floresta próxima à Loop" -> "A trilha estreita passa entre pinheiros, raízes expostas e rochas cobertas de musgo. O cheiro de resina é forte, galhos estalam sem aviso e as torres da Loop aparecem e desaparecem entre as copas."
      "Posto de gasolina" -> "Bombas, letreiros e uma pequena loja iluminada recortam a beira da estrada. O ar mistura gasolina, café velho e borracha quente; caminhões passam perto o bastante para fazer o chão vibrar."
      else -> "O lugar tem detalhes demais para ser apenas um ponto no mapa: caminhos de entrada e saída, objetos deixados para trás, ruídos vindos de longe e pessoas que podem perceber os Kids."
    }
    return listOf(
      "$seasonYear. São $time em $location. $weatherSense",
      "$placeSense Vocês estão realmente aqui: dá para escolher onde ficar, o que observar primeiro e quem pode notar a presença do grupo. Nada parece congelado esperando uma decisão; a vida comum continua ao redor.",
      "A Vida Cotidiana continua apertando mesmo agora: $everydayPressure. Isso não impede a cena atual; é uma preocupação concreta que acompanha o relógio e pode cobrar seu preço se vocês demorarem demais.",
      "Então algo quebra o ritmo normal. $hook. Quase ao mesmo tempo, vocês percebem $anomaly. $importantNpc também está por perto, fazendo parte da cena antes mesmo de vocês saberem se essa pessoa é importante. Não há resposta pronta nem explicação visível: há apenas o que seus olhos, ouvidos e instinto conseguem alcançar daqui, e a decisão de onde olhar primeiro."
    )
  }

  fun motorBrief(): String = buildString {
    appendLine("Crie uma aventura nova e original a partir deste seed. Não reutilize a aventura demo nem pressuponha eventos de partidas anteriores.")
    appendLine("Identificador único desta aventura: $signature")
    appendLine("Título provisório: $title")
    appendLine("Local inicial: $location")
    appendLine("Época: $seasonYear; clima: $weather; horário: $time; luz: $daylight")
    appendLine("Gancho: $hook")
    appendLine("Anomalia: $anomaly")
    appendLine("NPC importante: $importantNpc")
    appendLine("Pressão da Vida Cotidiana: $everydayPressure")
    appendLine("Abra como um Mestre de RPG: apresente geografia, ambiente, pessoas, relações, sons, cheiros, temperatura, texturas e detalhes concretos. Não revele o Mistério inteiro.")
    appendLine("A Vida Cotidiana deve ser coerente com a posição atual do Kid. Se houver castigo, toque de recolher ou proibição e o Kid já estiver fora de casa, trate isso como risco de ser descoberto ou obrigação de voltar, nunca como se fosse impossível ele estar na cena.")
    appendLine("Não use frases meta como 'o Mistério começou' ou 'nada além do que o Kid percebeu foi revelado'. Mostre isso pela ficção.")
  }.trim()
}

object AdventureFactory {
  private val locations = listOf(
    "Escola Björkskolan", "Praça de Björkäsen", "Margem do Lago Mälaren", "Fazenda nos arredores",
    "Ferro-velho", "Pedreira", "Bairro residencial", "Ponto de ônibus", "Floresta próxima à Loop", "Posto de gasolina"
  )
  private val hooks = listOf(
    "um colega desapareceu depois da aula", "um aparelho doméstico começou a transmitir uma voz desconhecida",
    "animais estão fugindo da mesma região", "uma bicicleta foi encontrada abandonada com marcas incomuns",
    "um adulto confiável pediu ajuda em segredo", "uma fita cassete contém sons que parecem responder a perguntas",
    "as luzes da cidade apagam sempre na mesma sequência", "alguém está deixando desenhos idênticos em vários lugares"
  )
  private val anomalies = listOf(
    "um zumbido subterrâneo que muda de frequência", "objetos metálicos que se movem alguns centímetros sozinhos",
    "uma névoa localizada que não segue o vento", "relógios que perdem exatamente sete minutos",
    "rádios que captam uma conversa impossível", "pássaros que formam padrões geométricos sobre a cidade",
    "um clarão silencioso vindo da mata", "máquinas desligadas que ligam durante poucos segundos"
  )
  private val npcs = listOf(
    "uma professora de ciências recém-chegada", "o dono cansado da loja de conveniência", "uma policial que conhece as famílias do bairro",
    "um técnico da Loop que evita perguntas", "uma bibliotecária que guarda recortes antigos", "um fazendeiro que viu algo à noite",
    "um irmão mais velho prestes a sair de casa", "uma vizinha idosa que observa tudo da janela"
  )
  private val pressures = listOf(
    "uma prova importante na manhã seguinte", "pais exigindo que o Kid volte cedo", "uma briga recente entre amigos",
    "um aniversário de família que ninguém pode faltar", "problemas na escola por chegar atrasado", "um compromisso com a Âncora",
    "você saiu sem permissão apesar do castigo e precisa voltar antes que percebam", "um campeonato ou apresentação escolar no dia seguinte"
  )
  private val weather = listOf("Céu limpo", "Nublado", "Garoa", "Chuva", "Chuva forte", "Neblina", "Vento frio", "Neve leve")
  private val seasons = listOf("Primavera, 1986", "Verão, 1986", "Outono, 1986", "Inverno, 1986", "Primavera, 1987")
  private val timeSlots = listOf(
    "07:18" to "Manhã",
    "15:37" to "Tarde",
    "16:52" to "Fim de tarde",
    "18:11" to "Crepúsculo",
    "19:46" to "Noite",
    "21:08" to "Noite"
  )
  private val visualByLocation = mapOf(
    "Escola Björkskolan" to "scene_school_corridor",
    "Praça de Björkäsen" to "scene_town_square",
    "Margem do Lago Mälaren" to "scene_lakeshore",
    "Fazenda nos arredores" to "scene_farm",
    "Ferro-velho" to "scene_scrapyard",
    "Pedreira" to "scene_quarry",
    "Bairro residencial" to "scene_living_room",
    "Ponto de ônibus" to "scene_bus_stop",
    "Floresta próxima à Loop" to "scene_forest_path",
    "Posto de gasolina" to "scene_gas_station"
  )

  fun create(usedSignatures: Set<String> = emptySet(), random: Random = Random.Default): AdventureSeed {
    // The combinatorial seed space is very large. Keep drawing until an unused exact seed is found;
    // the persisted history therefore prevents replaying the same generated starting setup.
    var attempts = 0
    while (true) {
      val location = locations.random(random)
      val hook = hooks.random(random)
      val anomaly = anomalies.random(random)
      val npc = npcs.random(random)
      val pressure = pressures.random(random)
      val season = seasons.random(random)
      val climate = weather.random(random)
      val (time, light) = timeSlots.random(random)
      val baseSignature = listOf(location, hook, anomaly, npc, pressure, season, climate, time, light).joinToString("|")
      val signature = if (attempts < 512) baseSignature else "$baseSignature|run-${usedSignatures.size + attempts}"
      if (signature !in usedSignatures) {
        val title = listOf("Ecos", "Sombras", "Sinais", "Vozes", "Luzes", "O Segredo", "A Noite", "O Ruído").random(random) +
          " " + listOf("no Lago", "de Björkäsen", "na Floresta", "sob a Cidade", "da Loop", "da Estrada", "da Escola").random(random)
        return AdventureSeed(signature, title, location, season, climate, time, light, hook, anomaly, npc, pressure, visualByLocation[location] ?: "scene_town_square")
      }
      attempts++
    }
  }

  fun createProceduralAdventurePackage(
    profile: String,
    worldId: String = "malaren-islands",
    random: Random = Random.Default
  ): AdventurePackage {
    val isFlood = (profile == "FLOOD_TEENS")
    val system = if (isFlood) "flood-teens" else "tales-kids"
    val timestamp = System.currentTimeMillis()
    val randSuffix = random.nextInt(1000, 9999)
    val advId = "procedural-${timestamp}-${randSuffix}"
    val titlePrefix = if (isFlood) listOf("A Inundação", "O Silêncio", "A Máquina Negra", "A Fuga", "Sombras Químicas").random(random)
      else listOf("O Mistério", "O Sinal Oculto", "O Zumbido", "A Fita Perdida", "O Androide Fugitivo").random(random)
    val titleSuffix = listOf("no Canal 4", "na Loop", "de Björkäsen", "da Pedreira", "do Ferro-Velho").random(random)
    val advTitle = "$titlePrefix $titleSuffix"

    val root = JSONObject().apply {
      put("schema", "braseiro.adventure/v1")
      put("id", advId)
      put("system", system)
      put("worldId", worldId)
      put("title", advTitle)
      put("author", "Mestre Bárbara Procedural")
      put("description", "Mistério gerado a partir do estado atual do mundo e das marcas do grupo.")
      put("tags", JSONArray(listOf("investigacao", "procedural", if (isFlood) "flood" else "tales")))

      val startObj = JSONObject().apply {
        put("locationId", "escola")
        put("hook", if (isFlood) "Um boato sobre aparelhos confiscados circula entre os adolescentes." else "Um colega não apareceu na aula e deixou a mochila trancada no armário.")
      }
      put("start", startObj)

      val storyObj = JSONObject().apply {
        put("premise", if (isFlood) "Fios de alta tensão submersos no canal estão emitindo pulsos de áudio gravados nos anos 80." else "Um sinal de rádio pirata parece prever o que vai acontecer na escola com cinco minutos de antecedência.")
        put("background", "Informações reservadas ao Mestre: Técnicos da Loop tentam encobrir uma sobrecarga subterrânea antes da inspeção municipal.")
        put("playerIntroduction", if (isFlood) "O céu está cinzento e o cheiro de diesel queimado desce pelo canal. Algo está errado." else "A tarde começa fria no pátio da escola. Vocês encontram um bilhete no chão com caligrafia conhecida.")
      }
      put("story", storyObj)

      val locsArr = JSONArray().apply {
        put(JSONObject().apply {
          put("id", "escola")
          put("name", "Escola Björkskolan")
          put("description", "Pátio e corredores com armários de metal.")
          put("tags", JSONArray(listOf("urbano", "cotidiano")))
        })
        put(JSONObject().apply {
          put("id", "ferro-velho")
          put("name", "Ferro-Velho de Stenhamra")
          put("description", "Montanhas de carcaças enferrujadas e peças industriais.")
          put("tags", JSONArray(listOf("industrial", "isolado")))
        })
      }
      put("locations", locsArr)

      val npcsArr = JSONArray().apply {
        put(JSONObject().apply {
          put("id", "npc-zelador")
          put("name", "Gunnar, o Zelador")
          put("role", "Testemunha relutante")
          put("description", "Homem reservado que trabalhou nas instalações antigas da Loop.")
          put("goals", JSONArray(listOf("Evitar problemas com a diretoria", "Manter os alunos longe do porão")))
          put("secretIds", JSONArray(listOf("segredo-chave-porao")))
        })
      }
      put("npcs", npcsArr)

      val secretsArr = JSONArray().apply {
        put(JSONObject().apply {
          put("id", "segredo-chave-porao")
          put("truth", "O zelador escondeu uma caixa de ferramentas da Loop no duto de ventilação.")
          put("visibility", "gm_only")
        })
      }
      put("secrets", secretsArr)

      put("enemies", JSONArray())
      put("encounters", JSONArray())

      val objectivesArr = JSONArray().apply {
        put(JSONObject().apply {
          put("id", "obj-descobrir-origem")
          put("description", "Descobrir a origem do sinal anômalo.")
          put("required", true)
        })
      }
      put("objectives", objectivesArr)
    }

    val res = AdventurePackageValidator.parseAndValidate(root.toString())
    return (res as AdventurePackageValidator.ValidationResult.Valid).adventurePackage
  }
}
