import com.braseiro.talesvtt.game.SceneIllustrationSelector
import com.braseiro.talesvtt.game.SceneState
import com.braseiro.talesvtt.game.FloodSceneState

data class Case(val expected:String,val location:String,val narrative:String,val weather:String="Nublado",val daylight:String="Tarde")

fun main() {
  val cases = listOf(
    Case("scene_family_kitchen","Cozinha","A família senta para jantar. Os pais perguntam por que voltamos tão tarde."),
    Case("scene_living_room","Sala","A televisão está ligada e todos conversam na sala depois de voltar para casa."),
    Case("scene_teen_bedroom","Quarto","Sozinho no quarto de madrugada, o telefone toca e a música ainda está alta."),
    Case("scene_school_corridor","Corredor da escola","O corredor e os armários estão vazios no intervalo."),
    Case("scene_classroom","Sala de aula","A professora começa a aula e entrega a prova para a classe."),
    Case("scene_library","Biblioteca","Vamos pesquisar livros e arquivos para a investigação."),
    Case("scene_hideout","Esconderijo","O grupo se reúne para planejar junto das bicicletas."),
    Case("scene_forest_path","Floresta","Seguimos de bicicleta pelo caminho da floresta, voltando para casa ao anoitecer.","Chuva","Anoitecer"),
    Case("scene_lakeshore","Margem do lago","A neblina cobre o lago e algo estranho se move na água."),
    Case("scene_farm","Fazenda","O celeiro está quieto e os animais não fazem barulho."),
    Case("scene_quarry","Pedreira","A escavação abandonada parece perigosa e merece investigação."),
    Case("scene_scrapyard","Ferro velho","Carros e sucata formam esconderijos entre montes de metal."),
    Case("scene_town_square","Praça","Encontramos todos na praça da cidade durante a tarde."),
    Case("scene_bus_stop","Ponto de ônibus","Esperamos o ônibus depois da escola sob a chuva."),
    Case("scene_convenience_shop","Loja","Compramos lanche na loja durante a noite."),
    Case("scene_gas_station","Posto","O carro para no posto da rodovia durante a viagem noturna.","Chuva","Noite"),
    Case("scene_hospital","Hospital","Visitamos o amigo ferido no hospital para acompanhar a recuperação."),
    Case("scene_police_station","Delegacia","A polícia nos leva para a delegacia e começa o interrogatório."),
    Case("scene_loop_gate","Portão de Serviço","A cerca e o portão de entrada da Loop Industries estão fechados."),
    Case("scene_maintenance_corridor","Loop Industries","Avançamos pelo corredor de manutenção cheio de cabos."),
    Case("scene_control_room","Sala de controle","O console e os computadores disparam um alarme enquanto tentamos programação."),
    Case("scene_reactor_hall","Reator","O reator entra em clímax, energia e máquinas tremem ao redor."),
    Case("scene_antenna_field","Campo de antenas","As antenas captam um sinal de rádio e tentamos comunicação."),
    Case("scene_flooded_tunnel","Túnel","O túnel está alagado e a água sobe perigosamente.")
  )
  var pass=0
  var total=0
  val seenTales=mutableSetOf<String>()
  val seenFlood=mutableSetOf<String>()
  for ((i,c) in cases.withIndex()) {
    val tales=SceneState(location=c.location,weather=c.weather,daylight=c.daylight,narrative=listOf(c.narrative),visualAssetKey="scene_loop_gate")
    val gotTales=SceneIllustrationSelector.choose(tales)
    val okTales=gotTales==c.expected
    total++; if(okTales) pass++
    seenTales+=gotTales
    println("${if(okTales) "PASS" else "FAIL"} TALES ${i+1} ${c.expected} -> $gotTales")

    val flood=FloodSceneState(location=c.location,weather=c.weather,daylight=c.daylight,narrative=listOf(c.narrative),visualAssetKey="scene_flooded_tunnel")
    val gotFlood=SceneIllustrationSelector.choose(flood)
    val okFlood=gotFlood==c.expected
    total++; if(okFlood) pass++
    seenFlood+=gotFlood
    println("${if(okFlood) "PASS" else "FAIL"} FLOOD ${i+1} ${c.expected} -> $gotFlood")
  }
  val uniqueOk=seenTales.size==24 && seenFlood.size==24
  println("RESULT $pass/$total tales_unique=${seenTales.size} flood_unique=${seenFlood.size} ${if(pass==total && uniqueOk) "PASS" else "FAIL"}")
  if(pass!=total || !uniqueOk) error("Scene illustration selector regression")
}
