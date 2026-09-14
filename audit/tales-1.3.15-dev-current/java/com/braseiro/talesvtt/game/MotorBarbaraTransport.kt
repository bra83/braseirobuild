package com.braseiro.talesvtt.game

import android.content.Context
import com.braseiro.talesvtt.BuildConfig
import com.braseiro.talesvtt.audio.GeminiNarrationTts
import com.braseiro.talesvtt.data.ManualLibraryRepository
import com.braseiro.talesvtt.data.SecureSecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MotorIntent(
  val type: String,
  val text: String? = null,
  val successes: Int? = null,
  val requiredSuccesses: Int? = null,
  val extendedKid: String? = null,
  val extendedSkill: String? = null,
  val extendedSceneIndex: Int? = null,
  val bonusEffects: List<String> = emptyList()
)

data class MotorTransportConfig(
  val apiKeyConfigured: Boolean,
  val usingLocalOverride: Boolean,
  val model: String,
  val ttsModel: String = GeminiNarrationTts.DEFAULT_TTS_MODEL,
  val ttsVoice: String = GeminiNarrationTts.DEFAULT_VOICE,
  val ttsMode: String = "gemini"
)

data class MotorTransportResult(
  val parsed: MotorParseResult,
  val rawJson: String
)

/**
 * Thin transport only. It never applies state changes; every response still goes through
 * MotorBarbaraWireAdapter and MotorBarbaraFirewall.
 */
class MotorBarbaraTransport(private val context: Context) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

  private val prefs = context.getSharedPreferences("braseiro_motor_cfg_v1", Context.MODE_PRIVATE)
  private val secrets = SecureSecretStore(context)
  private val manualLibrary = ManualLibraryRepository(context)

  init {
    // v1.3.2 migration: narration voice defaults to Gemini, never silently to Android.
    // Also move the old experimental text-model default to the stable generateContent model.
    if (!prefs.getBoolean("runtime_defaults_v132", false)) {
      val currentModel = prefs.getString("model", "").orEmpty()
      prefs.edit()
        .putString("tts_mode", "gemini")
        .putString("model", if (currentModel.isBlank() || currentModel == "gemini-3.8-flash") DEFAULT_MODEL else currentModel)
        .putBoolean("runtime_defaults_v132", true)
        .apply()
    }
  }

  fun hasApiKey(): Boolean = resolveApiKey().isNotBlank()

  fun currentConfig(): MotorTransportConfig = MotorTransportConfig(
    apiKeyConfigured = hasApiKey(),
    usingLocalOverride = secrets.has("gemini_api_key"),
    model = prefs.getString("model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL },
    ttsModel = prefs.getString("tts_model", GeminiNarrationTts.DEFAULT_TTS_MODEL).orEmpty().ifBlank { GeminiNarrationTts.DEFAULT_TTS_MODEL },
    ttsVoice = prefs.getString("tts_voice", GeminiNarrationTts.DEFAULT_VOICE).orEmpty().ifBlank { GeminiNarrationTts.DEFAULT_VOICE },
    ttsMode = prefs.getString("tts_mode", "gemini").orEmpty().ifBlank { "gemini" }
  )

  fun saveConfig(apiKey: String?, model: String, ttsModel: String = GeminiNarrationTts.DEFAULT_TTS_MODEL, ttsVoice: String = GeminiNarrationTts.DEFAULT_VOICE, ttsMode: String = "gemini") {
    val editor = prefs.edit()
    apiKey?.trim()?.takeIf { it.isNotEmpty() }?.let { secrets.put("gemini_api_key", it) }
    editor.putString("model", model.trim().ifBlank { DEFAULT_MODEL })
    editor.putString("tts_model", ttsModel.trim().ifBlank { GeminiNarrationTts.DEFAULT_TTS_MODEL })
    editor.putString("tts_voice", ttsVoice.trim().ifBlank { GeminiNarrationTts.DEFAULT_VOICE })
    editor.putString("tts_mode", if (ttsMode == "gemini") "gemini" else "android")
    editor.apply()
  }

  fun clearLocalApiKey() {
    secrets.remove("gemini_api_key")
    prefs.edit().remove("api_key").apply()
  }

  suspend fun request(
    intent: MotorIntent,
    gameState: GameState,
    privateState: GmPrivateState
  ): Result<MotorTransportResult> = withContext(Dispatchers.IO) {
    runCatching {
      val key = resolveApiKey()
      require(key.isNotBlank()) { "Chave Gemini não configurada para o Motor Barbara." }
      val model = prefs.getString("model", DEFAULT_MODEL).orEmpty().ifBlank { DEFAULT_MODEL }

      val requestJson = JSONObject()
        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
        .put("contents", JSONArray().put(
          JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text", buildUserPrompt(intent, gameState, privateState))))
        ))
        .put("generationConfig", JSONObject()
          .put("temperature", 0.65)
          .put("responseMimeType", "application/json")
        )

      val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        .header("Content-Type", "application/json; charset=utf-8")
        .header("x-goog-api-key", key)
        .post(requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

      var lastTransientCode: Int? = null
      repeat(MAX_TRANSIENT_ATTEMPTS) { attempt ->
        client.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            if (response.code in TRANSIENT_HTTP_CODES && attempt + 1 < MAX_TRANSIENT_ATTEMPTS) {
              lastTransientCode = response.code
              Thread.sleep(RETRY_BASE_DELAY_MS * (attempt + 1L))
              return@use
            }
            error(userSafeHttpError(response.code))
          }

          val root = JSONObject(body)
          val candidates = root.optJSONArray("candidates")
          require(candidates != null && candidates.length() > 0) { "Motor Gemini respondeu sem candidato utilizável." }
          val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts")
            ?: error("Motor Gemini respondeu sem conteúdo textual.")
          val text = buildString {
            for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty())
          }.trim()
          require(text.isNotEmpty()) { "Motor Gemini não retornou JSON textual." }
          val parsed = MotorBarbaraWireAdapter.parse(text)
          val needsNarration = intent.type in setOf("adventure_start", "free_action", "travel_request", "test_result", "extended_test_result")
          if (needsNarration && parsed.packet.actions.none { it is MotorAction.Narration }) {
            error("O Mestre respondeu de forma incompleta.")
          }
          return@runCatching MotorTransportResult(parsed, text)
        }
      }
      error("Motor Gemini temporariamente indisponível (HTTP ${lastTransientCode ?: 500}). Tente novamente.")
    }
  }

  private fun userSafeHttpError(code: Int): String = when (code) {
    400 -> "O Gemini recusou a solicitação (HTTP 400). Verifique o modelo configurado."
    401, 403 -> "O Gemini recusou a autenticação (HTTP $code). Verifique a chave API."
    404 -> "O modelo Gemini configurado não foi encontrado (HTTP 404). Escolha outro modelo."
    429 -> "Limite temporário do Gemini atingido (HTTP 429). Tente novamente em instantes."
    in TRANSIENT_HTTP_CODES -> "Motor Gemini temporariamente indisponível (HTTP $code). Tente novamente."
    else -> "Falha de comunicação com o Gemini (HTTP $code)."
  }

  private fun resolveApiKey(): String {
    val secure = secrets.get("gemini_api_key").trim()
    if (secure.isNotEmpty()) return secure
    val legacy = prefs.getString("api_key", "").orEmpty().trim()
    if (legacy.isNotEmpty()) { secrets.put("gemini_api_key", legacy); prefs.edit().remove("api_key").apply(); return legacy }
    return runCatching {
      BuildConfig::class.java.getField("GEMINI_API_KEY").get(null)?.toString().orEmpty().trim()
    }.getOrDefault("")
  }

  private fun buildUserPrompt(intent: MotorIntent, state: GameState, gm: GmPrivateState): String {
    val intentJson = JSONObject().put("type", intent.type)
    intent.text?.let { intentJson.put("text", it) }
    intent.successes?.let { intentJson.put("successes", it) }
    intent.requiredSuccesses?.let { intentJson.put("required", it) }
    intent.extendedKid?.let { intentJson.put("extendedKid", it) }
    intent.extendedSkill?.let { intentJson.put("extendedSkill", it) }
    intent.extendedSceneIndex?.let { intentJson.put("extendedSceneIndex", it) }
    if (intent.bonusEffects.isNotEmpty()) intentJson.put("bonusEffects", JSONArray(intent.bonusEffects))

    val publicState = JSONObject()
      .put("profile", "tales_kid")
      .put("scene", JSONObject()
        .put("title", state.scene.title)
        .put("location", state.scene.location)
        .put("weather", state.scene.weather)
        .put("time", state.scene.time)
        .put("daylight", state.scene.daylight)
        .put("narrative", JSONArray(state.scene.narrative))
      )
      .put("kid", JSONObject()
        .put("name", state.kid.name)
        .put("age", state.kid.age)
        .put("luck", state.kid.luck)
        .put("conditions", JSONArray(state.kid.conditions.map { it.label }))
        .put("broken", state.kid.broken)
        .put("prideAvailable", state.kid.prideAvailable)
        .put("iconicItem", state.kid.iconicItemName)
        .put("items", JSONArray(state.kid.items.map { item ->
          JSONObject().put("name", item.name).put("description", item.description).put("bonus", item.bonus).put("kind", item.kind.name.lowercase())
        }))
        .put("attributes", JSONObject(state.kid.attributes))
        .put("skills", JSONObject(state.kid.skills))
      )
      .put("group", JSONObject()
        .put("members", JSONArray(state.group.members.map { it.name }))
        .put("hideout", state.group.hideoutName)
      )
      .put("revealed", JSONObject()
        .put("clues", JSONArray(state.revealed.clues.toList()))
        .put("locations", JSONArray(state.revealed.locations.toList()))
        .put("connections", JSONArray(state.revealed.connections.map { JSONArray(listOf(it.first, it.second)) }))
      )

    state.activeTest?.let { test ->
      publicState.put("activeTest", JSONObject()
        .put("reason", test.reason)
        .put("attribute", test.attributeName)
        .put("skill", test.skillName)
        .put("required", test.requiredSuccesses)
        .put("extendedSceneIndex", test.extendedSceneIndex)
        .put("purpose", test.purpose.name.lowercase())
        .put("targetKid", test.targetKid)
        .put("informationBonus", test.informationBonus)
      )
    }

    if (state.extended.active) {
      publicState.put("extendedTrouble", JSONObject()
        .put("stakes", state.extended.stakes)
        .put("threatLevel", state.extended.threatLevel.label)
        .put("target", state.extended.target)
        .put("totalSuccesses", state.extended.totalSuccesses)
        .put("scenes", JSONArray(state.extended.scenes.mapIndexed { index, scene ->
          JSONObject()
            .put("index", index)
            .put("kid", scene.kid)
            .put("skill", scene.skill)
            .put("successes", scene.successes)
        }))
        .put("compromiseAvailable", state.extended.compromiseAvailable)
        .put("compromiseDeclined", state.extended.compromiseDeclined)
      )
    }

    val privateJson = JSONObject()
      .put("clues", JSONArray(gm.clues.values.map { c ->
        JSONObject().put("id", c.id).put("title", c.title).put("text", c.text).put("location", c.location)
      }))
      .put("connections", JSONArray(gm.connections.values.map { c ->
        JSONObject().put("id", c.id).put("a", c.from).put("b", c.to)
      }))
      .put("locations", JSONArray(gm.locations.values.map { l ->
        JSONObject().put("id", l.id).put("name", l.name)
      }))

    val ragQuery = buildString {
      append(intent.text.orEmpty()).append(' ')
      append(state.scene.title).append(' ')
      append(state.scene.location).append(' ')
      state.activeTest?.let { append(it.skillName.orEmpty()).append(' ').append(it.reason) }
    }.trim()
    val ragHits = if (ragQuery.isBlank()) emptyList() else manualLibrary.search(ragQuery, 5)
    val ragContext = JSONArray(ragHits.map { hit ->
      JSONObject().put("manual", hit.title).put("page", hit.page).put("snippet", hit.snippet)
    })

    return JSONObject()
      .put("active_profile", "tales_kid")
      .put("player_intent", intentJson)
      .put("player_visible_state", publicState)
      .put("gm_private", privateJson)
      .put("mechanical_contract", CanonicalTalesRules.localMechanicalContract())
      .put("reference_context", ragContext)
      .put("reference_instruction", "Contexto opcional somente para ambientação, nomes, lore e conteúdo narrativo. NUNCA use estes trechos para decidir ou alterar regras, pools, bônus, condições, XP, recuperação, Itens ou Problema Estendido. A autoridade mecânica é mechanical_contract + engine local; se houver conflito, ignore o contexto de referência.")
      .put("allowed_scene_assets", JSONArray(MotorBarbaraFirewall.allowedSceneAssets.toList()))
      .put("task", if (intent.type == "adventure_start") {
        "Inicie uma aventura NOVA a partir do seed do player_intent. A abertura deve ter 700–1100 palavras reais, divididas em parágrafos, e fazer o jogador sentir que está fisicamente no lugar: descreva geografia imediata, caminhos, distâncias, clima, temperatura, luz, sons próximos e distantes, cheiros, texturas, movimento de pessoas e Vida Cotidiana antes de aproximar o estranho. Mantenha continuidade causal: nenhuma pressão cotidiana pode contradizer o fato de o Kid já estar na cena. Não use frases meta sobre 'o Mistério começar'. Não revele a solução. Termine com exatamente 3 escolhas concretas. Em cada objeto de actions use a chave type, nunca action. Retorne somente JSON válido com actions."
      } else {
        "Continue a partida como Mestre de RPG. Toda mudança de local, chegada, saída, transição de cena ou descoberta importante deve receber 450–800 palavras sensoriais antes das decisões; ações dentro do mesmo local devem receber 180–450 palavras quando não exigirem uma transição. Descreva o que o Kid consegue ver, ouvir, cheirar, sentir no corpo e alcançar fisicamente, mantendo posições, pessoas, clima, horário e consequências coerentes. Nunca resuma uma nova cena em duas frases e nunca use texto meta como 'o Mistério começou'. Termine a narration com 2 ou 3 escolhas concretas, sem impedir ação livre. Em cada objeto de actions use a chave type, nunca action. Retorne somente JSON válido com actions. Use test_request somente quando houver Trouble real. Se player_intent.type=help_request, aprove apenas uma ajuda ficcionalmente plausível com help_approve e um membro real do grupo; caso contrário, narre a recusa sem bônus. Se player_intent.type=item_proposal, identifique o Item exato já presente em player_visible_state.kid. Para o Item Icônico use iconic_item_approve (+2 fixo). Para um Mystery Item existente use mystery_item_approve com o nome e exatamente o bônus armazenado (+1..+3). Objeto comum nunca recebe bônus automático. Se player_intent.type=extended_test_result, trate o valor recebido como o resultado definitivo da cena daquele Kid, inclua test_clear e continue o Problema Estendido sem criar rolagem final. extended_scene_result só pode reproduzir um resultado verificado fornecido por outro cliente/jogador; nunca invente sucessos para outro Kid. Nunca altere recursos do Kid diretamente e nunca revele gm_private sem uma action explícita de reveal."
      })
      .toString(2)
  }

  companion object {
    private const val DEFAULT_MODEL = "gemini-2.5-flash"
    private const val MAX_TRANSIENT_ATTEMPTS = 3
    private const val RETRY_BASE_DELAY_MS = 700L
    private val TRANSIENT_HTTP_CODES = setOf(500, 502, 503, 504)

    private val SYSTEM_PROMPT = """
Você é o Motor Barbara, Mestre autônomo do VTT BRASEIRO para Tales from the Loop no perfil Tales/Kids.
Responda sempre em português do Brasil e somente com JSON válido no formato {"actions":[...]}.

REGRAS INEGOCIÁVEIS:
- Recuperação nunca é genérica: Anchor/Âncora qualificada cura todas as Condições; Hideout/Esconderijo qualificado com 2+ Kids cura todas; comfort/conforto por Perícia cura exatamente uma Condição leve e nunca cura Quebrado. Se a cena não satisfizer os requisitos ficcionais, recovery deve vir approved=false.
- O bloco mechanical_contract recebido no pedido é a autoridade mecânica local. reference_context/RAG é opcional e nunca pode alterar regra.
- Testes usam Atributo + Perícia canônica, d6; cada 6 é 1 sucesso.
- Perícias: Furtividade/Força/Movimentação -> Corpo; Mecânica/Programação/Cálculo -> Tecnologia; Contatos/Charme/Liderança -> Coração; Investigação/Conhecimento/Empatia -> Mente.
- Trouble normal exige 1, 2 ou 3 sucessos, definidos pelo Mestre.
- Condições: Chateado, Assustado, Exausto e Machucado; Quebrado falha automaticamente.
- Sorte, Forçar e Orgulho são resolvidos pelo engine. Ajuda e Item precisam primeiro de sua validação ficcional: help_approve aplica exatamente +1 dado; iconic_item_approve aplica exatamente +2 dados ao Item Icônico; mystery_item_approve só aceita o bônus +1..+3 já armazenado no Item real. Objeto comum tem bônus 0. Você nunca inventa outros bônus nem gasta recursos do jogador.
- NPCs não rolam dados. Não crie HP, iniciativa, rounds ou combate tático.
- A ação do jogador é intenção; você decide consequência, mudança de cena e se há Trouble.
- Problema Estendido Tales: Normal=2x Kids, Difícil=3x Kids, Quase impossível=4x Kids; uma cena por Kid.
- Não use Shame, Scars, Friction ou regras de Things from the Flood.
- Nunca mencione quantidade de segredos, IDs privados ou conteúdo não revelado.
- Para revelar pista/local/conexão privada, use apenas reveal_clue/reveal_location/reveal_connection com ID existente em gm_private.
- scene_visual/world_patch só podem usar os assets de cena enviados em allowed_scene_assets.
- session_end/mystery_end apenas abrem o fluxo de encerramento; você nunca calcula ou concede XP.
- Quando player_intent.type for test_result ou extended_test_result, resolva a consequência e inclua test_clear antes de continuar a narrativa, a menos que um novo teste seja solicitado explicitamente.
- extended_scene_result só pode registrar resultado verificado de outro Kid/cliente; nunca fabrique sucessos.
- Não encerre Problema Estendido enquanto o compromisso estiver aguardando decisão do jogador.
- Clima e horário fazem parte do mundo ficcional: varie entre céu limpo, nublado, garoa, chuva, neblina, vento, neve e outros estados plausíveis. Nunca trate chuva como default.
- Quando player_intent.type=adventure_start, crie uma aventura realmente nova a partir do seed recebido. Não recicle "Ecos no Lago", o portão de serviço, NPCs ou revelações da aventura demo, salvo coincidência inevitável do seed.
- O estilo de narração deve ser de Mestre de RPG: textos ricos o suficiente para situar geografia, ambiente, pessoas e Vida Cotidiana. Não reduza cenas importantes a duas frases.
- ABERTURA e MUDANÇA DE CENA são obrigatoriamente longas e sensoriais. O jogador deve conseguir imaginar onde está, o que há à esquerda/direita/frente, quem está por perto e o que os sentidos captam antes de decidir.
- Preserve coerência causal. Se o Kid já está fora de casa, um castigo ou proibição só pode aparecer como risco de ser descoberto, prazo para voltar ou consequência futura; nunca diga que sair é impossível.
- Nunca escreva frases meta como 'o Mistério começou', 'nada além do que o Kid percebeu foi revelado' ou explicações sobre o funcionamento do jogo dentro da narração.
- Em JSON, cada diretiva usa obrigatoriamente o campo "type". Nunca use "action" no lugar de "type".

AÇÕES SUPORTADAS:
- narration {text, choices:[[titulo,descricao], ...]}
- scene_visual {asset}
- world_patch {patch:{location,weather,time,daylight}}
- test_request {skill,required,label,purpose?: standard|build_item|program_item|leadership_prepare|leadership_comfort|questioning,targetKid?}
- test_clear
- help_approve {helperKid}
- iconic_item_approve
- mystery_item_approve {item,bonus}
- item_add {item:{name,description,bonus,kind: ordinary|mystery}}
- item_remove {item}
- condition {key: upset|scared|exhausted|injured} OU {broken:true}
- recovery {approved:true|false,source: anchor|hideout|comfort,condition?}
- secret_clue_add {secretClue:{id,title,text,location}}
- secret_connection_add {secretConnection:{id,a,b}}
- secret_location_add {secretLocation:{id,name}}
- reveal_clue {clueId}
- reveal_connection {connectionId}
- reveal_location {locationId}
- extended_patch {patch:{active:true,stakes,threatLevel,kids,scenes:[{kid,skill},...]}} ou {patch:{active:false}}
- extended_scene_result {kid,successes} — somente para resultado verificado de outro cliente/jogador
- player_intent.bonusEffects contém somente efeitos já comprados e validados localmente; apenas narre suas consequências, nunca troque/invente efeitos.
- session_end
- mystery_end
- message {text}

Prefira narration + 2 ou 3 choices quando a cena continua. Na abertura de uma aventura, faça uma introdução longa; nas demais cenas, adapte o tamanho ao momento sem perder contexto espacial e humano. Não inclua markdown.
""".trimIndent()
  }
}
