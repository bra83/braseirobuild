package com.braseiro.talesvtt.game

import java.text.Normalizer

/**
 * Deterministic, mechanics-neutral scene-art selector.
 * It only projects narrative/context into a packaged illustration key; it never mutates game state.
 */
object SceneIllustrationSelector {
  data class Entry(
    val assetKey: String,
    val locations: List<String>,
    val tags: List<String>,
    val profiles: Set<String> = setOf("tales_kid", "flood_teen")
  )

  val library: List<Entry> = listOf(
    Entry("scene_family_kitchen", listOf("casa", "cozinha"), listOf("jant", "jantar", "familia", "pais", "mae", "pai", "volta para casa", "retorno", "cotidiano")),
    Entry("scene_living_room", listOf("casa", "sala", "bairro"), listOf("casa", "televisao", "familia", "conversa", "cotidiano", "retorno", "volta para casa")),
    Entry("scene_teen_bedroom", listOf("quarto", "casa"), listOf("quarto", "sozinho", "sozinha", "musica", "telefone", "depois da festa", "ressaca", "madrugada", "teen")),
    Entry("scene_school_corridor", listOf("escola", "colegio"), listOf("corredor", "armario", "aula", "intervalo", "escola", "cotidiano")),
    Entry("scene_classroom", listOf("escola", "sala de aula"), listOf("aula", "professor", "professora", "classe", "prova", "licao")),
    Entry("scene_library", listOf("biblioteca"), listOf("pesquisa", "livro", "arquivo", "estudar", "investigacao")),
    Entry("scene_hideout", listOf("esconderijo", "refugio"), listOf("grupo", "amizade", "planejar", "plano", "reuniao", "bicicleta")),
    Entry("scene_forest_path", listOf("floresta", "bosque", "estrada", "caminho"), listOf("floresta", "caminho", "bicicleta", "indo para casa", "voltando para casa", "retorno", "chuva", "anoitecer")),
    Entry("scene_lakeshore", listOf("lago", "margem", "malaren"), listOf("lago", "agua", "neblina", "isolamento", "estranho", "misterio")),
    Entry("scene_farm", listOf("fazenda", "campo", "rural"), listOf("celeiro", "campo", "animal", "fazenda", "rural", "cotidiano")),
    Entry("scene_quarry", listOf("pedreira"), listOf("pedreira", "escavacao", "perigo", "investigacao")),
    Entry("scene_scrapyard", listOf("ferro velho", "sucata"), listOf("sucata", "carro", "perseguicao", "esconder", "mecanica")),
    Entry("scene_town_square", listOf("bjorkasen", "cidade", "praca"), listOf("cidade", "praca", "encontro", "social", "cotidiano", "tarde")),
    Entry("scene_bus_stop", listOf("ponto de onibus", "estrada"), listOf("onibus", "espera", "escola", "retorno", "chuva", "viagem")),
    Entry("scene_convenience_shop", listOf("loja", "mercado"), listOf("loja", "compras", "lanche", "social", "cotidiano", "noite")),
    Entry("scene_gas_station", listOf("posto", "estrada", "rodovia"), listOf("posto", "carro", "estrada", "viagem", "fuga", "noite", "chuva")),
    Entry("scene_hospital", listOf("hospital"), listOf("hospital", "ferido", "ferida", "visita", "recuperacao", "medico")),
    Entry("scene_police_station", listOf("delegacia", "policia"), listOf("policia", "delegacia", "autoridade", "interrogatorio")),
    Entry("scene_loop_gate", listOf("portao de servico", "loop industries", "instalacao"), listOf("portao", "cerca", "entrada", "seguranca", "loop", "suspense")),
    Entry("scene_maintenance_corridor", listOf("loop industries", "instalacao"), listOf("corredor", "manutencao", "cabos", "tecnico", "invasao")),
    Entry("scene_control_room", listOf("loop industries", "sala de controle"), listOf("controle", "computador", "console", "alarme", "programacao")),
    Entry("scene_reactor_hall", listOf("loop industries", "reator"), listOf("reator", "climax", "energia", "maquina", "perigo")),
    Entry("scene_antenna_field", listOf("antenas", "campo de antenas"), listOf("antena", "radio", "sinal", "comunicacao")),
    Entry("scene_flooded_tunnel", listOf("tunel"), listOf("tunel", "alagado", "inundado", "agua", "esgoto", "flood", "perigo")),
    Entry("scene_home_arrival_rain", listOf("casa","bairro","rua residencial"), listOf("chegando em casa","chegar em casa","volta para casa","retorno para casa","bicicleta","chuva","anoitecer","fim de tarde")),
    Entry("scene_kitchen_empty_rain", listOf("casa","cozinha"), listOf("cozinha vazia","casa vazia","jantar acabou","depois do jantar","chuva","noite","cotidiano")),
    Entry("scene_bedroom_after_school", listOf("quarto","casa"), listOf("depois da escola","quarto","fim de tarde","entardecer","mochila","cotidiano","descansar")),
    Entry("scene_homework_rain", listOf("quarto","casa"), listOf("dever de casa","lição de casa","estudar","estudo","caderno","chuva","noite")),
    Entry("scene_bedroom_music_rain", listOf("quarto","casa"), listOf("ouvindo música","musica","walkman","fita","cassete","quarto","chuva","noite")),
    Entry("scene_family_argument", listOf("casa","cozinha","sala"), listOf("discussão com os pais","discussao com os pais","briga com os pais","família","familia","pais","conflito","tensão","tensao")),
    Entry("scene_bedroom_insomnia", listOf("quarto","casa"), listOf("insônia","insonia","sem dormir","madrugada","acordado","acordada","2:17","chuva","noite")),
    Entry("scene_bedroom_morning_winter", listOf("quarto","casa"), listOf("manhã","manha","antes da escola","inverno","arrumar mochila","amanhecer","frio")),
    Entry("scene_kitchen_quiet_night", listOf("casa","cozinha"), listOf("cozinha silenciosa","cozinha vazia","café","cafe","chuva","noite","cotidiano")),
    Entry("scene_bedroom_empty_retro", listOf("quarto","casa"), listOf("quarto vazio","ausência","ausencia","desaparecido","desaparecida","procurar quarto","revistar quarto","cotidiano")),
    Entry("scene_bedroom_late_study", listOf("quarto","casa"), listOf("estudo tarde","estudar à noite","estudar a noite","caderno","pesquisa","chuva","noite","madrugada")),
    Entry("scene_bedroom_journal_dusk", listOf("quarto","casa"), listOf("diário","diario","escrever","anotar","música","musica","fone","fim de tarde","chuva")),
    Entry("scene_home_return_evening", listOf("casa","bairro","rua residencial"), listOf("regresso","retorno","volta para casa","chegando em casa","bicicleta","anoitecer","chuva")),
    Entry("scene_home_arrival_sunset", listOf("casa","bairro"), listOf("chegando em casa","portão de casa","porta de casa","volta para casa","bicicleta","entardecer","pôr do sol","por do sol")),
    Entry("scene_home_return_heavy_rain", listOf("casa","bairro"), listOf("chegando em casa","volta para casa","chuva forte","encharcado","encharcada","tempestade","bicicleta","noite")),
    Entry("scene_school_arrival_rain", listOf("escola","colégio"), listOf("chegada à escola","chegada na escola","antes da aula","bicicletas","chuva","manhã","manha")),
    Entry("scene_school_corridor_busy", listOf("escola"), listOf("corredor cheio","corredor movimentado","alunos","chuva")),
    Entry("scene_classroom_after_hours", listOf("escola","sala de aula"), listOf("sala vazia","depois da aula","fim da aula","quadro negro","professor saiu","chuva")),
    Entry("scene_school_yard_rain", listOf("escola","pátio","patio"), listOf("pátio","patio","intervalo","saída","saida","chuva","fim de tarde")),
    Entry("scene_library_study_rain", listOf("biblioteca","escola"), listOf("biblioteca","estudar","pesquisar","livros","depois da aula","chuva","silêncio","silencio")),
    Entry("scene_school_lab_rain", listOf("escola","laboratório","laboratorio"), listOf("laboratório","laboratorio","experimento","ciência","ciencia","aula de ciências","chuva")),
    Entry("scene_locker_room_empty", listOf("escola","vestiário","vestiario","ginásio","ginasio"), listOf("vestiário vazio","vestiario vazio","ginásio","ginasio","depois do treino","armários","armarios","fim de tarde")),
    Entry("scene_principal_office_rain", listOf("escola","diretoria","gabinete"), listOf("diretoria","diretor","diretora","professor","conversa séria","conversa seria","problema na escola","chuva")),
    Entry("scene_school_exit_rain", listOf("escola","pátio","patio"), listOf("saída da escola","saida da escola","fim das aulas","voltando para casa","bicicletas","chuva","fim de tarde")),
    Entry("scene_school_yard_evening", listOf("escola","pátio","patio"), listOf("pátio vazio","patio vazio","depois da aula","anoitecer","bicicletas","chuva")),
    Entry("scene_bus_shelter_rain", listOf("ponto de ônibus","ponto de onibus","rua","bairro"), listOf("esperando ônibus","esperando onibus","ponto de ônibus","ponto de onibus","chuva","fim de tarde","volta para casa")),
    Entry("scene_neighborhood_bike_sunset", listOf("bairro","rua residencial","cidade"), listOf("pedalando","bicicleta","rua residencial","entardecer","pôr do sol","por do sol","volta para casa")),
    Entry("scene_convenience_store_rain", listOf("loja","mercado","mercadinho"), listOf("loja de conveniência","loja de conveniencia","compras","lanche","revistas","chuva","noite","cotidiano")),
    Entry("scene_group_bikes_sunset", listOf("bairro","rua residencial","cidade"), listOf("grupo de kids","grupo de teens","bicicletas","pedalando juntos","voltando para casa","pôr do sol","por do sol")),
    Entry("scene_neighborhood_loop_sunset", listOf("bairro","cidade","rua residencial"), listOf("loop ao longe","torres ao longe","cidade","bicicleta","entardecer","pôr do sol","por do sol","estranho")),
    Entry("scene_home_loop_rain", listOf("casa","bairro","rua residencial"), listOf("chegando em casa","loop ao fundo","anel ao longe","chuva","bicicleta","estranho","mistério","misterio")),
    Entry("scene_loop_towers_sunset", listOf("bairro","cidade","rua residencial"), listOf("torres do loop","loop ao longe","bicicleta","cidade","entardecer","estranho","mistério","misterio")),
    Entry("scene_hideout_planning_rain", listOf("esconderijo","refúgio","refugio"), listOf("reunião do grupo","reuniao do grupo","planejar","mapa","pistas","rádio","radio","chuva","noite")),
    Entry("scene_lakeshore_bikes_mist", listOf("lago","margem","mälaren","malaren"), listOf("bicicletas abandonadas","bicicletas","lago","neblina","silêncio","silencio","investigação","investigacao","mistério","misterio")),
    Entry("scene_quarry_overlook", listOf("pedreira","floresta"), listOf("observando pedreira","pedreira","vigiar","investigar","investigação","investigacao","perigo","entardecer")),
    Entry("scene_abandoned_radar_facility", listOf("instalação","instalacao","floresta","radar","loop industries"), listOf("radar abandonado","cúpula","cupula","instalação abandonada","instalacao abandonada","cerca","invasão","invasao","mistério","misterio")),
    Entry("scene_abandoned_amusement_park", listOf("parque de diversões","parque de diversoes","floresta","cidade"), listOf("parque abandonado","roda gigante","brinquedos abandonados","investigação","investigacao","mistério","misterio","estranho")),
    Entry("scene_abandoned_river_factory", listOf("fábrica","fabrica","industrial","rio","cais"), listOf("fábrica abandonada","fabrica abandonada","guindaste","cais","ruína industrial","ruina industrial","investigação","investigacao","flood","mistério","misterio")),
    Entry("scene_group_bikes_ring_sunset", listOf("bairro","cidade","estrada"), listOf("grupo de kids","grupo de teens","anel gigante","loop ao longe","bicicletas","pôr do sol","por do sol","mistério","misterio")),
    Entry("scene_064_basement_party_lights", listOf("porão", "casa", "festa"), listOf("festa", "porão", "música", "luzes", "social", "noite"), setOf("flood_teen")),
    Entry("scene_065_teen_porch", listOf("casa", "varanda", "bairro"), listOf("chegar em casa", "sair de casa", "varanda", "discussão", "anoitecer", "teen"), setOf("flood_teen")),
    Entry("scene_066_suburb_dusk", listOf("bairro", "rua residencial", "cidade"), listOf("subúrbio", "bairro", "crepúsculo", "cotidiano", "seco", "fim de tarde"), setOf("tales_kid", "flood_teen")),
    Entry("scene_067_kids_loop_dusk", listOf("bairro", "cidade", "loop"), listOf("kids", "loop ao longe", "bicicleta", "grupo", "entardecer", "mistério"), setOf("tales_kid")),
    Entry("scene_068_anchor_dinner", listOf("casa", "cozinha"), listOf("jantar", "âncora", "anchor", "conversa", "família", "recuperação", "cotidiano"), setOf("tales_kid", "flood_teen")),
    Entry("scene_069_teen_room_rain_mystery", listOf("quarto", "casa"), listOf("quarto", "teen", "chuva", "mistério", "sozinho", "telefone", "noite"), setOf("flood_teen")),
    Entry("scene_070_winter_suburb_bike", listOf("bairro", "rua residencial"), listOf("inverno", "neve", "bicicleta", "bairro", "mistério", "frio"), setOf("tales_kid", "flood_teen")),
    Entry("scene_071_diner_rain", listOf("lanchonete", "cidade"), listOf("lanchonete", "diner", "lanche", "social", "chuva", "noite"), setOf("tales_kid", "flood_teen")),
    Entry("scene_072_schoolyard_dusk", listOf("escola", "pátio"), listOf("escola", "pátio", "bicicletas", "fim de aula", "crepúsculo"), setOf("tales_kid")),
    Entry("scene_073_garage_dusk", listOf("garagem", "casa", "oficina"), listOf("garagem", "consertar", "rádio", "mecânica", "bicicleta", "crepúsculo"), setOf("tales_kid", "flood_teen")),
    Entry("scene_074_distant_towers", listOf("floresta", "bairro", "loop"), listOf("torres", "explorar", "vigiar", "mistério", "aventura", "entardecer"), setOf("tales_kid", "flood_teen")),
    Entry("scene_075_waiting_room_comfort", listOf("hospital", "sala de espera"), listOf("sala de espera", "hospital", "recuperação", "âncora", "conforto", "noite"), setOf("tales_kid", "flood_teen")),
    Entry("scene_076_forest_dawn", listOf("floresta", "bosque", "caminho"), listOf("floresta", "amanhecer", "manhã", "exploração", "bicicleta", "céu limpo"), setOf("tales_kid", "flood_teen")),
    Entry("scene_077_industrial_tunnel", listOf("túnel", "instalação", "industrial"), listOf("túnel", "industrial", "exploração", "invasão", "lanterna", "perigo"), setOf("tales_kid", "flood_teen")),
    Entry("scene_078_teen_room_snow", listOf("quarto", "casa"), listOf("quarto", "teen", "neve", "inverno", "sozinho", "música", "noite"), setOf("flood_teen")),
    Entry("scene_079_laundromat_rain", listOf("lavanderia", "cidade"), listOf("lavanderia", "cotidiano", "espera", "chuva", "social", "noite"), setOf("tales_kid", "flood_teen")),
    Entry("scene_080_teen_meeting_dusk", listOf("bairro", "cidade", "escola"), listOf("adolescentes", "encontro", "conversa", "relação", "crepúsculo", "social"), setOf("flood_teen")),
    Entry("scene_081_bus_rain_dusk", listOf("ônibus", "cidade", "bairro"), listOf("ônibus", "viagem", "retorno", "chuva", "entardecer", "transporte"), setOf("tales_kid", "flood_teen")),
    Entry("scene_082_teens_rain_dusk", listOf("bairro", "cidade"), listOf("adolescentes", "chuva", "crepúsculo", "conversa", "friction", "social"), setOf("flood_teen")),
    Entry("scene_083_departure_dome", listOf("bairro", "casa", "loop"), listOf("partida", "sair de casa", "bicicleta", "domo", "loop", "anoitecer"), setOf("tales_kid", "flood_teen")),
    Entry("scene_084_schoolyard_towers", listOf("escola", "pátio"), listOf("escola", "pátio", "torres", "loop", "depois da aula", "mistério"), setOf("tales_kid")),
    Entry("scene_085_diner_dusk", listOf("lanchonete", "cidade"), listOf("lanchonete", "diner", "social", "entardecer", "cotidiano", "lanche"), setOf("tales_kid", "flood_teen")),
    Entry("scene_086_winter_suburb_tower", listOf("bairro", "rua residencial"), listOf("inverno", "neve", "torre", "bairro", "cotidiano", "mistério"), setOf("tales_kid", "flood_teen")),
    Entry("scene_087_substation_teens", listOf("subestação", "industrial", "loop"), listOf("subestação", "eletricidade", "adolescentes", "vigiar", "invasão", "perigo"), setOf("flood_teen")),
    Entry("scene_088_basement_party", listOf("porão", "casa", "festa"), listOf("festa", "porão", "adolescentes", "música", "estranho", "noite"), setOf("flood_teen")),
    Entry("scene_089_workshop_dusk", listOf("oficina", "garagem", "casa"), listOf("oficina", "consertar", "bicicleta", "ferramentas", "mecânica", "anoitecer"), setOf("tales_kid", "flood_teen")),
    Entry("scene_090_kids_bikes_fog", listOf("bairro", "estrada", "floresta"), listOf("kids", "bicicletas", "neblina", "viagem", "mistério", "grupo"), setOf("tales_kid")),
    Entry("scene_091_teen_room_moon", listOf("quarto", "casa"), listOf("quarto", "teen", "lua", "noite", "sozinho", "música", "reflexão"), setOf("flood_teen")),
    Entry("scene_092_railway_tunnel", listOf("túnel", "ferrovia", "industrial"), listOf("túnel ferroviário", "trilhos", "exploração", "lanterna", "invasão", "perigo"), setOf("tales_kid", "flood_teen")),
    Entry("scene_093_waiting_room_cold", listOf("hospital", "sala de espera"), listOf("sala de espera", "hospital", "espera", "recuperação", "noite", "frio"), setOf("tales_kid", "flood_teen")),
    Entry("scene_094_teen_loop_dusk", listOf("bairro", "cidade", "loop"), listOf("teen", "loop", "entardecer", "sozinho", "cidade", "mistério"), setOf("flood_teen")),
    Entry("scene_095_bus_sunset", listOf("ônibus", "cidade", "bairro"), listOf("ônibus", "viagem", "pôr do sol", "entardecer", "retorno", "transporte"), setOf("tales_kid", "flood_teen")),
    Entry("scene_096_kids_bikes_loop", listOf("bairro", "cidade", "loop"), listOf("kids", "bicicletas", "loop", "grupo", "viagem", "aventura"), setOf("tales_kid")),
    Entry("scene_097_kiosk_ring", listOf("loja", "quiosque", "cidade"), listOf("quiosque", "loja", "npc", "conversa", "cotidiano", "anel"), setOf("tales_kid", "flood_teen")),
    Entry("scene_098_youth_frozen_loop", listOf("campo", "bairro", "loop"), listOf("jovens", "inverno", "congelado", "loop", "descoberta", "mistério"), setOf("tales_kid", "flood_teen")),
    Entry("scene_099_suburb_edge_mystery", listOf("bairro", "campo", "estrada"), listOf("limite do subúrbio", "objeto estranho", "descoberta", "investigação", "mistério"), setOf("tales_kid", "flood_teen")),
    Entry("scene_100_teen_room_rain_loop", listOf("quarto", "casa"), listOf("quarto", "teen", "chuva", "loop", "telefone", "mistério", "noite"), setOf("flood_teen"))
  )

  fun choose(scene: SceneState): String = chooseFromContext(
    locationRaw = scene.location,
    narrativeRaw = scene.narrative.joinToString(" "),
    weatherRaw = scene.weather,
    daylightRaw = "${scene.daylight} ${scene.time}",
    explicitRaw = scene.visualAssetKey,
    profile = "tales_kid",
    fallback = "scene_loop_gate"
  )

  fun choose(scene: FloodSceneState): String = chooseFromContext(
    locationRaw = scene.location,
    narrativeRaw = scene.narrative.joinToString(" "),
    weatherRaw = scene.weather,
    daylightRaw = "${scene.daylight} ${scene.time}",
    explicitRaw = scene.visualAssetKey,
    profile = "flood_teen",
    fallback = "scene_flooded_tunnel"
  )

  private fun chooseFromContext(
    locationRaw: String,
    narrativeRaw: String,
    weatherRaw: String,
    daylightRaw: String,
    explicitRaw: String,
    profile: String,
    fallback: String
  ): String {
    val location = norm(locationRaw)
    val narrative = norm(narrativeRaw)
    val weather = norm(weatherRaw)
    val daylight = norm(daylightRaw)
    val all = "$location $narrative $weather $daylight"
    var best: Entry? = null
    var bestScore = -1.0

    library.forEach { entry ->
      if (profile !in entry.profiles) return@forEach
      var score = 0.0
      entry.locations.forEach { raw ->
        val tag = norm(raw)
        if (tag.isNotBlank() && (location.contains(tag) || (location.isNotBlank() && tag.contains(location)))) score += 8.0
      }
      for (raw in entry.tags) {
        val tag = norm(raw)
        if (tag.isBlank()) continue
        if (narrative.contains(tag)) score += 3.0
        if (weather.contains(tag)) score += 2.0
        if (daylight.contains(tag)) score += 1.0
        if (all.contains(tag)) score += 1.0
      }
      if (entry.assetKey == explicitRaw.removeSuffix(".png")) score += 1.5
      if (score > bestScore) {
        bestScore = score
        best = entry
      }
    }

    val explicit = explicitRaw.removeSuffix(".png").takeIf { key -> library.any { it.assetKey == key && profile in it.profiles } }
    return if (best != null && bestScore >= 4.0) best!!.assetKey else explicit ?: fallback
  }

  private fun norm(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
}
