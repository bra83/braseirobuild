import com.braseiro.talesvtt.game.SceneIllustrationSelector
import com.braseiro.talesvtt.game.SceneState
import com.braseiro.talesvtt.game.FloodSceneState

data class ExpansionCase(val expected:String,val location:String,val narrative:String,val weather:String="Chuva",val daylight:String="Anoitecer")

fun main() {
  val cases = listOf(
    ExpansionCase("scene_home_arrival_rain","casa","chegando em casa; chegar em casa; volta para casa; retorno para casa; bicicleta","Chuva","Noite"),
    ExpansionCase("scene_kitchen_empty_rain","casa","cozinha vazia; casa vazia; jantar acabou; depois do jantar; chuva","Chuva","Noite"),
    ExpansionCase("scene_bedroom_after_school","quarto","depois da escola; quarto; fim de tarde; entardecer; mochila","Nublado","Anoitecer"),
    ExpansionCase("scene_homework_rain","quarto","dever de casa; lição de casa; estudar; estudo; caderno","Chuva","Noite"),
    ExpansionCase("scene_bedroom_music_rain","quarto","ouvindo música; musica; walkman; fita; cassete","Chuva","Noite"),
    ExpansionCase("scene_family_argument","casa","discussão com os pais; discussao com os pais; briga com os pais; família; familia","Nublado","Tarde"),
    ExpansionCase("scene_bedroom_insomnia","quarto","insônia; insonia; sem dormir; madrugada; acordado","Chuva","Noite"),
    ExpansionCase("scene_bedroom_morning_winter","quarto","manhã; manha; antes da escola; inverno; arrumar mochila","Nublado","Tarde"),
    ExpansionCase("scene_kitchen_quiet_night","casa","cozinha silenciosa; cozinha vazia; café; cafe; chuva","Chuva","Noite"),
    ExpansionCase("scene_bedroom_empty_retro","quarto","quarto vazio; ausência; ausencia; desaparecido; desaparecida","Nublado","Tarde"),
    ExpansionCase("scene_bedroom_late_study","quarto","estudo tarde; estudar à noite; estudar a noite; caderno; pesquisa","Chuva","Noite"),
    ExpansionCase("scene_bedroom_journal_dusk","quarto","diário; diario; escrever; anotar; música","Chuva","Tarde"),
    ExpansionCase("scene_home_return_evening","casa","regresso; retorno; volta para casa; chegando em casa; bicicleta","Chuva","Noite"),
    ExpansionCase("scene_home_arrival_sunset","casa","chegando em casa; portão de casa; porta de casa; volta para casa; bicicleta","Nublado","Anoitecer"),
    ExpansionCase("scene_home_return_heavy_rain","casa","chegando em casa; volta para casa; chuva forte; encharcado; encharcada","Chuva","Noite"),
    ExpansionCase("scene_school_arrival_rain","escola","chegada à escola; chegada na escola; antes da aula; bicicletas; chuva","Chuva","Tarde"),
    ExpansionCase("scene_school_corridor_busy","escola","corredor cheio; corredor movimentado; alunos; chuva","Chuva","Tarde"),
    ExpansionCase("scene_classroom_after_hours","escola","sala vazia; depois da aula; fim da aula; quadro negro; professor saiu","Chuva","Tarde"),
    ExpansionCase("scene_school_yard_rain","escola","pátio; patio; intervalo; saída; saida","Chuva","Tarde"),
    ExpansionCase("scene_library_study_rain","biblioteca","biblioteca; estudar; pesquisar; livros; depois da aula","Chuva","Tarde"),
    ExpansionCase("scene_school_lab_rain","escola","laboratório; laboratorio; experimento; ciência; ciencia","Chuva","Tarde"),
    ExpansionCase("scene_locker_room_empty","escola","vestiário vazio; vestiario vazio; ginásio; ginasio; depois do treino","Nublado","Tarde"),
    ExpansionCase("scene_principal_office_rain","escola","diretoria; diretor; diretora; professor; conversa séria","Chuva","Tarde"),
    ExpansionCase("scene_school_exit_rain","escola","saída da escola; saida da escola; fim das aulas; voltando para casa; bicicletas","Chuva","Tarde"),
    ExpansionCase("scene_school_yard_evening","escola","pátio vazio; patio vazio; depois da aula; anoitecer; bicicletas","Chuva","Noite"),
    ExpansionCase("scene_bus_shelter_rain","ponto de ônibus","esperando ônibus; esperando onibus; ponto de ônibus; ponto de onibus; chuva","Chuva","Tarde"),
    ExpansionCase("scene_neighborhood_bike_sunset","bairro","pedalando; bicicleta; rua residencial; entardecer; pôr do sol","Nublado","Anoitecer"),
    ExpansionCase("scene_convenience_store_rain","loja","loja de conveniência; loja de conveniencia; compras; lanche; revistas","Chuva","Noite"),
    ExpansionCase("scene_group_bikes_sunset","bairro","grupo de kids; grupo de teens; bicicletas; pedalando juntos; voltando para casa","Nublado","Anoitecer"),
    ExpansionCase("scene_neighborhood_loop_sunset","bairro","loop ao longe; torres ao longe; cidade; bicicleta; entardecer","Nublado","Anoitecer"),
    ExpansionCase("scene_home_loop_rain","casa","chegando em casa; loop ao fundo; anel ao longe; chuva; bicicleta","Chuva","Tarde"),
    ExpansionCase("scene_loop_towers_sunset","bairro","torres do loop; loop ao longe; bicicleta; cidade; entardecer","Nublado","Anoitecer"),
    ExpansionCase("scene_hideout_planning_rain","esconderijo","reunião do grupo; reuniao do grupo; planejar; mapa; pistas","Chuva","Noite"),
    ExpansionCase("scene_lakeshore_bikes_mist","lago","bicicletas abandonadas; bicicletas; lago; neblina; silêncio","Nublado","Tarde"),
    ExpansionCase("scene_quarry_overlook","pedreira","observando pedreira; pedreira; vigiar; investigar; investigação","Nublado","Anoitecer"),
    ExpansionCase("scene_abandoned_radar_facility","instalação","radar abandonado; cúpula; cupula; instalação abandonada; instalacao abandonada","Nublado","Tarde"),
    ExpansionCase("scene_abandoned_amusement_park","parque de diversões","parque abandonado; roda gigante; brinquedos abandonados; investigação; investigacao","Nublado","Tarde"),
    ExpansionCase("scene_abandoned_river_factory","fábrica","fábrica abandonada; fabrica abandonada; guindaste; cais; ruína industrial","Nublado","Tarde"),
    ExpansionCase("scene_group_bikes_ring_sunset","bairro","grupo de kids; grupo de teens; anel gigante; loop ao longe; bicicletas","Nublado","Anoitecer")
  )
  var pass=0; var total=0
  val seenTales=mutableSetOf<String>(); val seenFlood=mutableSetOf<String>()
  for ((i,c) in cases.withIndex()) {
    val tales=SceneState(location=c.location,weather=c.weather,daylight=c.daylight,narrative=listOf(c.narrative),visualAssetKey="scene_loop_gate")
    val gt=SceneIllustrationSelector.choose(tales); val okt=gt==c.expected; total++; if(okt) pass++; seenTales+=gt
    println("${if(okt) "PASS" else "FAIL"} TALES ${i+1} ${c.expected} -> $gt")
    val flood=FloodSceneState(location=c.location,weather=c.weather,daylight=c.daylight,narrative=listOf(c.narrative),visualAssetKey="scene_flooded_tunnel")
    val gf=SceneIllustrationSelector.choose(flood); val okf=gf==c.expected; total++; if(okf) pass++; seenFlood+=gf
    println("${if(okf) "PASS" else "FAIL"} FLOOD ${i+1} ${c.expected} -> $gf")
  }
  val uniqueOk=seenTales.size==cases.size && seenFlood.size==cases.size
  println("RESULT $pass/$total tales_unique=${seenTales.size} flood_unique=${seenFlood.size} ${if(pass==total && uniqueOk) "PASS" else "FAIL"}")
  if(pass!=total || !uniqueOk) error("Expanded scene illustration selector regression")
}
