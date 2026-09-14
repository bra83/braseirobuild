from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15i-transform.py')}
code=(repo/'tales-1.3.15i-transform.py').read_text(encoding='utf-8')
old=sys.argv[:]
sys.argv=[ns['__file__'],sys.argv[1]]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old

root=Path(sys.argv[1])
def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def replace_once(text,old,new,label):
    if old not in text: raise SystemExit(f'anchor not found: {label}')
    return text.replace(old,new,1)

# Deterministic GM-private stores for recurring NPCs, world facts and countdowns.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt'
t=read(rel)
t=replace_once(t,
'''data class PrivateLocation(
  val id: String,
  val name: String
)

data class GmPrivateState(
  val clues: Map<String, PrivateClue> = emptyMap(),
  val connections: Map<String, PrivateConnection> = emptyMap(),
  val locations: Map<String, PrivateLocation> = emptyMap()
)''',
'''data class PrivateLocation(
  val id: String,
  val name: String
)

data class PrivateNpc(
  val id: String,
  val name: String,
  val role: String = "",
  val description: String = "",
  val location: String = "",
  val status: String = "active"
)

data class PrivateWorldFact(
  val id: String,
  val subject: String,
  val value: String,
  val scope: String = "campaign"
)

data class PrivateCountdown(
  val id: String,
  val title: String,
  val stage: Int = 0,
  val maxStage: Int = 4,
  val consequence: String = ""
)

data class GmPrivateState(
  val clues: Map<String, PrivateClue> = emptyMap(),
  val connections: Map<String, PrivateConnection> = emptyMap(),
  val locations: Map<String, PrivateLocation> = emptyMap(),
  val npcs: Map<String, PrivateNpc> = emptyMap(),
  val worldFacts: Map<String, PrivateWorldFact> = emptyMap(),
  val countdowns: Map<String, PrivateCountdown> = emptyMap()
)''','GM private models')

t=replace_once(t,
'''  data class SecretLocationAdd(val location: PrivateLocation) : MotorAction
  data class RevealClue(val clueId: String) : MotorAction''',
'''  data class SecretLocationAdd(val location: PrivateLocation) : MotorAction
  data class SecretNpcUpsert(val npc: PrivateNpc) : MotorAction
  data class SecretWorldFactUpsert(val fact: PrivateWorldFact) : MotorAction
  data class SecretCountdownUpsert(val countdown: PrivateCountdown) : MotorAction
  data class RevealClue(val clueId: String) : MotorAction''','GM private actions')

location_block='''        is MotorAction.SecretLocationAdd -> {
          val l = action.location
          if (l.id.isBlank() || l.name.isBlank()) rejected += "ação[$index] secret_location_add: local inválido."
          else if (gm.locations.containsKey(l.id)) rejected += "ação[$index] secret_location_add: id duplicado."
          else {
            gm = gm.copy(locations = gm.locations + (l.id to l))
            accepted++
          }
        }

        is MotorAction.RevealClue -> {'''
private_new='''        is MotorAction.SecretLocationAdd -> {
          val l = action.location
          if (l.id.isBlank() || l.name.isBlank()) rejected += "ação[$index] secret_location_add: local inválido."
          else if (gm.locations.containsKey(l.id)) rejected += "ação[$index] secret_location_add: id duplicado."
          else {
            gm = gm.copy(locations = gm.locations + (l.id to l))
            accepted++
          }
        }

        is MotorAction.SecretNpcUpsert -> {
          val n=action.npc.copy(id=action.npc.id.trim(),name=action.npc.name.trim(),role=action.npc.role.trim(),description=action.npc.description.trim(),location=action.npc.location.trim(),status=action.npc.status.trim().ifBlank{"active"})
          when {
            n.id.isBlank() || n.name.isBlank() -> rejected += "ação[$index] secret_npc_upsert: id/nome inválidos."
            n.id.length>120 || n.name.length>180 || n.role.length>240 || n.description.length>4000 || n.location.length>240 || n.status.length>120 -> rejected += "ação[$index] secret_npc_upsert: campo excede limite local."
            gm.npcs.size>=500 && n.id !in gm.npcs -> rejected += "ação[$index] secret_npc_upsert: limite de NPCs privados atingido."
            else -> { gm=gm.copy(npcs=gm.npcs+(n.id to n)); accepted++ }
          }
        }

        is MotorAction.SecretWorldFactUpsert -> {
          val f=action.fact.copy(id=action.fact.id.trim(),subject=action.fact.subject.trim(),value=action.fact.value.trim(),scope=action.fact.scope.trim().ifBlank{"campaign"})
          when {
            f.id.isBlank() || f.subject.isBlank() || f.value.isBlank() -> rejected += "ação[$index] secret_world_fact_upsert: fato inválido."
            f.id.length>120 || f.subject.length>300 || f.value.length>4000 || f.scope.length>120 -> rejected += "ação[$index] secret_world_fact_upsert: campo excede limite local."
            gm.worldFacts.size>=1000 && f.id !in gm.worldFacts -> rejected += "ação[$index] secret_world_fact_upsert: limite de fatos privados atingido."
            else -> { gm=gm.copy(worldFacts=gm.worldFacts+(f.id to f)); accepted++ }
          }
        }

        is MotorAction.SecretCountdownUpsert -> {
          val c=action.countdown.copy(id=action.countdown.id.trim(),title=action.countdown.title.trim(),consequence=action.countdown.consequence.trim())
          when {
            c.id.isBlank() || c.title.isBlank() -> rejected += "ação[$index] secret_countdown_upsert: id/título inválidos."
            c.maxStage !in 1..12 || c.stage !in 0..c.maxStage -> rejected += "ação[$index] secret_countdown_upsert: estágio inválido."
            c.id.length>120 || c.title.length>300 || c.consequence.length>4000 -> rejected += "ação[$index] secret_countdown_upsert: campo excede limite local."
            gm.countdowns.size>=100 && c.id !in gm.countdowns -> rejected += "ação[$index] secret_countdown_upsert: limite de contagens privadas atingido."
            else -> { gm=gm.copy(countdowns=gm.countdowns+(c.id to c)); accepted++ }
          }
        }

        is MotorAction.RevealClue -> {'''
t=replace_once(t,location_block,private_new,'private memory firewall')
write(rel,t)

# Backward-compatible GM snapshot v2.
gmcodec='''package com.braseiro.talesvtt.game

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Private persistence for GM-only facts. Never expose through player UI StateFlow. */
object GmPrivateSnapshotCodec {
  private const val VERSION = 2
  private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8.name())
  private fun dec(v: String) = URLDecoder.decode(v, StandardCharsets.UTF_8.name())

  fun encode(state: GmPrivateState): String = buildString {
    append("version=").append(VERSION).append('\\n')
    append("clues=").append(state.clues.values.joinToString("|") {
      listOf(enc(it.id), enc(it.title), enc(it.text), enc(it.location)).joinToString("~")
    }).append('\\n')
    append("connections=").append(state.connections.values.joinToString("|") {
      listOf(enc(it.id), enc(it.from), enc(it.to)).joinToString("~")
    }).append('\\n')
    append("locations=").append(state.locations.values.joinToString("|") {
      listOf(enc(it.id), enc(it.name)).joinToString("~")
    }).append('\\n')
    append("npcs=").append(state.npcs.values.joinToString("|") {
      listOf(enc(it.id),enc(it.name),enc(it.role),enc(it.description),enc(it.location),enc(it.status)).joinToString("~")
    }).append('\\n')
    append("worldFacts=").append(state.worldFacts.values.joinToString("|") {
      listOf(enc(it.id),enc(it.subject),enc(it.value),enc(it.scope)).joinToString("~")
    }).append('\\n')
    append("countdowns=").append(state.countdowns.values.joinToString("|") {
      listOf(enc(it.id),enc(it.title),it.stage.toString(),it.maxStage.toString(),enc(it.consequence)).joinToString("~")
    }).append('\\n')
  }

  fun decode(text: String): GmPrivateState? = runCatching {
    val map = text.lineSequence().filter { '=' in it }
      .associate { it.substringBefore('=') to it.substringAfter('=') }
    val version=map["version"]?.toIntOrNull() ?: return null
    if(version !in 1..VERSION) return null

    val clues = map["clues"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p = raw.split('~')
      if (p.size >= 4) PrivateClue(dec(p[0]), dec(p[1]), dec(p[2]), dec(p[3])) else null
    }.associateBy { it.id }

    val connections = map["connections"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p = raw.split('~')
      if (p.size >= 3) PrivateConnection(dec(p[0]), dec(p[1]), dec(p[2])) else null
    }.associateBy { it.id }

    val locations = map["locations"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p = raw.split('~')
      if (p.size >= 2) PrivateLocation(dec(p[0]), dec(p[1])) else null
    }.associateBy { it.id }

    val npcs = if(version>=2) map["npcs"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
      val p=raw.split('~'); if(p.size>=6) PrivateNpc(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),dec(p[4]),dec(p[5])) else null
    }.associateBy{it.id} else emptyMap()

    val facts = if(version>=2) map["worldFacts"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
      val p=raw.split('~'); if(p.size>=4) PrivateWorldFact(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3])) else null
    }.associateBy{it.id} else emptyMap()

    val countdowns = if(version>=2) map["countdowns"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
      val p=raw.split('~'); if(p.size>=5) {
        val stage=p[2].toIntOrNull(); val max=p[3].toIntOrNull()
        if(stage!=null && max!=null && max in 1..12 && stage in 0..max) PrivateCountdown(dec(p[0]),dec(p[1]),stage,max,dec(p[4])) else null
      } else null
    }.associateBy{it.id} else emptyMap()

    GmPrivateState(clues,connections,locations,npcs,facts,countdowns)
  }.getOrNull()
}
'''
write('app/src/main/java/com/braseiro/talesvtt/game/GmPrivateSnapshotCodec.kt',gmcodec)

# Wire format: private authoring is accepted only into deterministic GM stores.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt'
t=read(rel)
anchor='''    "secret_location_add" -> {
      val l = a.optJSONObject("secretLocation") ?: return null
      val id = l.optString("id").trim()
      val name = l.optString("name").trim()
      if (id.isEmpty() || name.isEmpty()) null else MotorAction.SecretLocationAdd(PrivateLocation(id, name))
    }

    "reveal_clue" ->'''
insert='''    "secret_location_add" -> {
      val l = a.optJSONObject("secretLocation") ?: return null
      val id = l.optString("id").trim()
      val name = l.optString("name").trim()
      if (id.isEmpty() || name.isEmpty()) null else MotorAction.SecretLocationAdd(PrivateLocation(id, name))
    }

    "secret_npc_upsert", "secret_npc_add" -> {
      val n=a.optJSONObject("secretNpc") ?: a.optJSONObject("npc") ?: return null
      val id=n.optString("id").trim(); val name=n.optString("name").trim()
      if(id.isEmpty()||name.isEmpty()) null else MotorAction.SecretNpcUpsert(PrivateNpc(id,name,n.optString("role"),n.optString("description"),n.optString("location"),n.optString("status","active")))
    }

    "secret_world_fact_upsert", "secret_fact_upsert" -> {
      val f=a.optJSONObject("secretWorldFact") ?: a.optJSONObject("fact") ?: return null
      val id=f.optString("id").trim(); val subject=f.optString("subject").trim(); val value=f.optString("value").trim()
      if(id.isEmpty()||subject.isEmpty()||value.isEmpty()) null else MotorAction.SecretWorldFactUpsert(PrivateWorldFact(id,subject,value,f.optString("scope","campaign")))
    }

    "secret_countdown_upsert", "secret_countdown_add" -> {
      val c=a.optJSONObject("secretCountdown") ?: a.optJSONObject("countdown") ?: return null
      val id=c.optString("id").trim(); val title=c.optString("title").trim()
      if(id.isEmpty()||title.isEmpty()) null else MotorAction.SecretCountdownUpsert(PrivateCountdown(id,title,c.optInt("stage",0),c.optInt("maxStage",4),c.optString("consequence")))
    }

    "reveal_clue" ->'''
t=replace_once(t,anchor,insert,'wire private memory actions')
t=t.replace('''
    // NPC/countdown/private campaign authoring is intentionally not yet accepted by the
    // player-state engine. Those domains remain GM-private until their own deterministic stores exist.
    else -> null''','''
    else -> null''',1)
write(rel,t)

# Send private world memory back to the GM and document the supported authoring actions.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt'
t=read(rel)
t=replace_once(t,
'''      .put("locations", JSONArray(gm.locations.values.map { l ->
        JSONObject().put("id", l.id).put("name", l.name)
      }))

    val ragQuery''',
'''      .put("locations", JSONArray(gm.locations.values.map { l ->
        JSONObject().put("id", l.id).put("name", l.name)
      }))
      .put("npcs", JSONArray(gm.npcs.values.map { n ->
        JSONObject().put("id",n.id).put("name",n.name).put("role",n.role).put("description",n.description).put("location",n.location).put("status",n.status)
      }))
      .put("worldFacts", JSONArray(gm.worldFacts.values.map { f ->
        JSONObject().put("id",f.id).put("subject",f.subject).put("value",f.value).put("scope",f.scope)
      }))
      .put("countdowns", JSONArray(gm.countdowns.values.map { c ->
        JSONObject().put("id",c.id).put("title",c.title).put("stage",c.stage).put("maxStage",c.maxStage).put("consequence",c.consequence)
      }))

    val ragQuery''','transport private JSON')

t=replace_once(t,
'''- Nunca mencione quantidade de segredos, IDs privados ou conteúdo não revelado.
- Para revelar pista/local/conexão privada, use apenas reveal_clue/reveal_location/reveal_connection com ID existente em gm_private.''',
'''- Nunca mencione quantidade de segredos, IDs privados ou conteúdo não revelado.
- Preserve continuidade de NPCs recorrentes, fatos duradouros e ameaças em progresso usando secret_npc_upsert, secret_world_fact_upsert e secret_countdown_upsert. Reutilize o mesmo id para atualizar; essas ações são memória privada e nunca revelam conteúdo por si.
- Registre NPC recorrente quando ele ganhar nome/papel persistente ou mudar de local/status. Registre fato privado quando ele precisar continuar verdadeiro em cenas futuras. Atualize countdown somente quando a ficção realmente avançar a ameaça.
- Para revelar pista/local/conexão privada, use apenas reveal_clue/reveal_location/reveal_connection com ID existente em gm_private.''','system private memory rule')

t=replace_once(t,
'''- secret_location_add {secretLocation:{id,name}}
- reveal_clue {clueId}''',
'''- secret_location_add {secretLocation:{id,name}}
- secret_npc_upsert {secretNpc:{id,name,role,description,location,status}}
- secret_world_fact_upsert {secretWorldFact:{id,subject,value,scope}}
- secret_countdown_upsert {secretCountdown:{id,title,stage,maxStage,consequence}}
- reveal_clue {clueId}''','supported private memory actions')
write(rel,t)

# Regression coverage for private continuity and backward migration.
test='''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class GmPrivateWorldMemoryV1315Test {
  @Test fun privateMemoryRoundTripV2() {
    val state=GmPrivateState(
      clues=mapOf("c1" to PrivateClue("c1","Pista","segredo","Escola")),
      npcs=mapOf("npc-1" to PrivateNpc("npc-1","Agnes","Âncora","Vizinha atenta","Casa de Agnes","active")),
      worldFacts=mapOf("f1" to PrivateWorldFact("f1","antena","emite pulsos à noite","mystery")),
      countdowns=mapOf("d1" to PrivateCountdown("d1","Falha da antena",2,4,"apagão"))
    )
    val decoded=GmPrivateSnapshotCodec.decode(GmPrivateSnapshotCodec.encode(state))!!
    assertEquals(state,decoded)
  }

  @Test fun legacyV1StillMigrates() {
    val old="version=1\\nclues=c1~Pista~texto~Escola\\nconnections=\\nlocations=l1~Escola\\n"
    val decoded=GmPrivateSnapshotCodec.decode(old)!!
    assertEquals("Pista",decoded.clues["c1"]!!.title)
    assertTrue(decoded.npcs.isEmpty())
    assertTrue(decoded.worldFacts.isEmpty())
    assertTrue(decoded.countdowns.isEmpty())
  }

  @Test fun privateUpsertsNeverMutatePlayerState() {
    val game=GameState()
    val packet=MotorPacket(actions=listOf(
      MotorAction.SecretNpcUpsert(PrivateNpc("n1","Rolf","Professor",location="Escola")),
      MotorAction.SecretWorldFactUpsert(PrivateWorldFact("f1","Rolf","esconde uma chave","mystery")),
      MotorAction.SecretCountdownUpsert(PrivateCountdown("d1","Patrulha",1,4,"guardas chegam"))
    ))
    val result=MotorBarbaraFirewall.apply(game,GmPrivateState(),packet)
    assertTrue(result.isClean)
    assertEquals(3,result.acceptedActions)
    assertEquals(game,result.gameState)
    assertEquals("Rolf",result.privateState.npcs["n1"]!!.name)
    assertEquals(1,result.privateState.countdowns["d1"]!!.stage)
  }

  @Test fun samePrivateIdUpdatesInsteadOfDuplicating() {
    var gm=GmPrivateState()
    val first=MotorBarbaraFirewall.apply(GameState(),gm,MotorPacket(actions=listOf(MotorAction.SecretNpcUpsert(PrivateNpc("n1","Rolf",location="Escola")))))
    gm=first.privateState
    val second=MotorBarbaraFirewall.apply(GameState(),gm,MotorPacket(actions=listOf(MotorAction.SecretNpcUpsert(PrivateNpc("n1","Rolf",location="Hospital",status="injured")))))
    assertTrue(second.isClean)
    assertEquals(1,second.privateState.npcs.size)
    assertEquals("Hospital",second.privateState.npcs["n1"]!!.location)
    assertEquals("injured",second.privateState.npcs["n1"]!!.status)
  }

  @Test fun invalidCountdownIsRejected() {
    val result=MotorBarbaraFirewall.apply(GameState(),GmPrivateState(),MotorPacket(actions=listOf(
      MotorAction.SecretCountdownUpsert(PrivateCountdown("d1","Ameaça",5,4,"fim"))
    )))
    assertFalse(result.isClean)
    assertTrue(result.privateState.countdowns.isEmpty())
    assertEquals(0,result.acceptedActions)
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/GmPrivateWorldMemoryV1315Test.kt',test)

print('TALES 1.3.15j GM-private world memory closure: OK')
