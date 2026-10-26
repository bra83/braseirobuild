import com.braseiro.talesvtt.game.*
import kotlin.system.measureTimeMillis

fun main(){
  val clues=(0 until 10000).map{"Pista número $it — áéíóú ç 日本語"}.toSet()
  val locs=(0 until 2000).map{"Local $it — Björkäsen"}.toSet()
  val conns=(0 until 5000).map{("Pista número ${it%10000} — áéíóú ç 日本語") to ("Local ${it%2000} — Björkäsen")}.toSet()
  val longNarrative="Narração longa çãõ 日本語 — "+"x".repeat(20000)
  val s=GameState(
    scene=SceneState(narrative=listOf(longNarrative),choices=emptyList()),
    revealed=RevealedState(clues,locs,conns)
  )
  lateinit var enc:String
  val encMs=measureTimeMillis{enc=GameSnapshotCodec.encode(s)}
  lateinit var dec:GameState
  val decMs=measureTimeMillis{dec=requireNotNull(GameSnapshotCodec.decode(enc))}
  require(dec==s.copy(lastError=null))

  val gm=GmPrivateState(
    clues=(0 until 20000).associate{val id="secret-$it";id to PrivateClue(id,"Título $it","Segredo UTF-8 $it çãõ 日本語 "+"z".repeat(100),"Local ${it%2000}")},
    connections=(0 until 10000).associate{val id="conn-$it";id to PrivateConnection(id,"secret-${it%20000}","Local ${it%2000}")},
    locations=(0 until 2000).associate{val id="loc-$it";id to PrivateLocation(id,"Local secreto $it")}
  )
  lateinit var ge:String
  val gmEnc=measureTimeMillis{ge=GmPrivateSnapshotCodec.encode(gm)}
  lateinit var gd:GmPrivateState
  val gmDec=measureTimeMillis{gd=requireNotNull(GmPrivateSnapshotCodec.decode(ge))}
  require(gd==gm)
  println("SNAPSHOT_SCALE_PASS publicBytes=${enc.toByteArray().size} publicEncodeMs=$encMs publicDecodeMs=$decMs privateBytes=${ge.toByteArray().size} privateEncodeMs=$gmEnc privateDecodeMs=$gmDec")
}
