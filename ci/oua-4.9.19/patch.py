from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4919.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# A) Gemini network resilience: retry transient DNS/socket aborts natively,
#    then surface a clean message while preserving the current scene.
# ---------------------------------------------------------------------------
main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)

old_call = """    private fun callGemini(model: String, body: String, key: String): GeminiHttpResult {
        require(Regex("^[A-Za-z0-9._-]{3,80}$").matches(model)) { "Modelo Gemini inválido." }
        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            GeminiHttpResult(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } finally { connection.disconnect() }
    }"""
new_call = """    private fun callGeminiOnce(model: String, body: String, key: String): GeminiHttpResult {
        require(Regex("^[A-Za-z0-9._-]{3,80}$").matches(model)) { "Modelo Gemini inválido." }
        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            GeminiHttpResult(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } finally { connection.disconnect() }
    }

    private fun isTransientGeminiNetworkError(error: Throwable): Boolean {
        val text = buildString {
            append(error::class.java.simpleName)
            append(' ')
            append(error.message.orEmpty())
            append(' ')
            append(error.cause?.message.orEmpty())
        }.lowercase(Locale.ROOT)
        return listOf(
            "unknownhost", "unable to resolve host", "no address associated with hostname",
            "software caused connection abort", "connection abort", "connection reset",
            "sockettimeout", "timed out", "connectexception", "network is unreachable",
            "failed to connect", "temporary failure in name resolution"
        ).any(text::contains)
    }

    private fun callGemini(model: String, body: String, key: String): GeminiHttpResult {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                return callGeminiOnce(model, body, key)
            } catch (error: Throwable) {
                lastError = error
                if (!isTransientGeminiNetworkError(error) || attempt >= 2) {
                    if (isTransientGeminiNetworkError(error)) {
                        throw IllegalStateException("Conexão temporariamente indisponível para o Gemini. A cena atual foi preservada; verifique a internet e tente novamente.", error)
                    }
                    throw error
                }
                Thread.sleep(700L * (attempt + 1))
            }
        }
        throw IllegalStateException("Conexão temporariamente indisponível para o Gemini. A cena atual foi preservada; tente novamente.", lastError)
    }"""
if old_call not in main:
    raise SystemExit("native Gemini HTTP anchor missing")
main = main.replace(old_call, new_call, 1)

# ---------------------------------------------------------------------------
# B) Persist the image target in Android SharedPreferences, not only WebView
#    localStorage. This survives Gemini handoff, process death and task changes.
# ---------------------------------------------------------------------------
inbox_rel = "android/app/src/main/java/com/braseiro/oumanel/SharedImageInbox.kt"
inbox = read(inbox_rel)

key_anchor = '    private const val KEY_CREATED_AT = "pending_shared_image_created_at"\n'
if key_anchor not in inbox:
    raise SystemExit("SharedImageInbox key anchor missing")
inbox = inbox.replace(key_anchor, key_anchor + '    private const val KEY_TARGET_JSON = "pending_shared_image_target_json"\n', 1)

pending_anchor = """    fun mimeTypeFor(context: Context, fileName: String): String {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return preferences.getString(KEY_MIME, null)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substringAfterLast('.', "").lowercase())
            ?: "image/*"
    }
"""
if pending_anchor not in inbox:
    raise SystemExit("SharedImageInbox mime anchor missing")
target_methods = pending_anchor + """
    fun setTarget(context: Context, json: String): Boolean {
        val clean = json.trim()
        if (clean.isBlank()) return false
        val parsed = org.json.JSONObject(clean)
        val kind = parsed.optString("kind")
        val campaignId = parsed.optString("campaignId")
        if (campaignId.isBlank() || kind !in setOf("scene", "npc")) return false
        if (kind == "npc" && parsed.optString("npcId").isBlank()) return false
        if (kind == "scene" && parsed.optString("sceneId").isBlank()) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TARGET_JSON, clean)
            .apply()
        return true
    }

    fun pendingTarget(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TARGET_JSON, "").orEmpty()

    fun clearTarget(context: Context): Boolean {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_TARGET_JSON).apply()
        return true
    }
"""
inbox = inbox.replace(pending_anchor, target_methods, 1)

clear_old = """        preferences.edit().remove(KEY_FILE).remove(KEY_MIME).remove(KEY_TOKEN).remove(KEY_CREATED_AT).apply()
        directory(context).listFiles()?.filter { it.name.startsWith(".") }?.forEach { it.delete() }
        return deleted"""
clear_new = """        preferences.edit().remove(KEY_FILE).remove(KEY_MIME).remove(KEY_TOKEN).remove(KEY_CREATED_AT).remove(KEY_TARGET_JSON).apply()
        directory(context).listFiles()?.filter { it.name.startsWith(".") }?.forEach { it.delete() }
        return deleted"""
if clear_old not in inbox:
    raise SystemExit("SharedImageInbox clear anchor missing")
inbox = inbox.replace(clear_old, clear_new, 1)
write(inbox_rel, inbox)

# Native bridge methods + JS wrapper. Insert relative to the stable consumeSharedImage
# bridge instead of depending on the exact formatting of shareText.
native_consume_anchor = '        @JavascriptInterface fun consumeSharedImage(): String = pendingSharedImageReference.orEmpty()\n'
if native_consume_anchor not in main:
    raise SystemExit("native consumeSharedImage anchor missing")
native_methods = """        @JavascriptInterface fun setSharedImageTarget(json: String): Boolean = try {
            SharedImageInbox.setTarget(this@MainActivity, json)
        } catch (error: Exception) {
            Log.e("BRASEIRO_SHARE", "Falha ao persistir destino da imagem", error)
            false
        }
        @JavascriptInterface fun getSharedImageTarget(): String = SharedImageInbox.pendingTarget(this@MainActivity)
        @JavascriptInterface fun clearSharedImageTarget(): Boolean = SharedImageInbox.clearTarget(this@MainActivity)
"""
if "@JavascriptInterface fun setSharedImageTarget" not in main:
    main = main.replace(native_consume_anchor, native_methods + native_consume_anchor, 1)

js_consume_anchor = "                consumeSharedImage: () => Promise.resolve(BraseiroNative.consumeSharedImage() || null),"
if js_consume_anchor not in main:
    raise SystemExit("JS consumeSharedImage anchor missing")
js_methods = """                setSharedImageTarget: json => Promise.resolve(BraseiroNative.setSharedImageTarget(String(json||''))),
                getSharedImageTarget: () => Promise.resolve(BraseiroNative.getSharedImageTarget() || null),
                clearSharedImageTarget: () => Promise.resolve(BraseiroNative.clearSharedImageTarget()),
"""
if "setSharedImageTarget: json =>" not in main:
    main = main.replace(js_consume_anchor, js_methods + js_consume_anchor, 1)

# Version cache-buster later after version bump.
write(main_rel, main)

# TypeScript global bridge contract.
global_rel = "global.d.ts"
global_ts = read(global_rel)
global_anchor = """      shareText?: (title: string, text: string) => Promise<void>;
      consumeSharedImage?: () => Promise<string | null>;"""
if global_anchor not in global_ts:
    raise SystemExit("global bridge shared-image anchor missing")
global_ts = global_ts.replace(global_anchor, """      shareText?: (title: string, text: string) => Promise<void>;
      setSharedImageTarget?: (json: string) => Promise<boolean>;
      getSharedImageTarget?: () => Promise<string | null>;
      clearSharedImageTarget?: () => Promise<boolean>;
      consumeSharedImage?: () => Promise<string | null>;""", 1)
write(global_rel, global_ts)

# GameView: persist target natively before opening Gemini.
game_rel = "components/GameView.tsx"
game = read(game_rel)
share_old = """      localStorage.setItem('braseiro.pendingSharedImageTarget', JSON.stringify(target));
      if (window.BraseiroAndroid?.shareText) {"""
share_new = """      const targetJson = JSON.stringify(target);
      localStorage.setItem('braseiro.pendingSharedImageTarget', targetJson);
      if (window.BraseiroAndroid?.setSharedImageTarget) {
        await window.BraseiroAndroid.setSharedImageTarget(targetJson);
      }
      if (window.BraseiroAndroid?.shareText) {"""
if share_old not in game:
    raise SystemExit("GameView shared target anchor missing")
game = game.replace(share_old, share_new, 1)
write(game_rel, game)

# App: always read latest NPCs from IndexedDB and prefer native persisted target.
app_rel = "App.tsx"
app = read(app_rel)

latest_old = "    const latestNpcs = allNpcs.length ? allNpcs : await getStoreData<Npc>('npcs');\n    const target = latestNpcs.find(npc => npc.id === npcId);"
latest_new = "    const latestNpcs = await getStoreData<Npc>('npcs');\n    const target = latestNpcs.find(npc => npc.id === npcId);"
if latest_old not in app:
    raise SystemExit("NPC portrait latestNpcs anchor missing")
app = app.replace(latest_old, latest_new, 1)

# Verify persistence before telling UI it succeeded and notify immersive renderer.
status_old = """    setAllNpcs(updatedNpcs);
    setPresentNpcs(updatedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
    setImageImportStatus('Retrato de ' + target.name + ' recebido, salvo e vinculado ao NPC.');
  };"""
status_new = """    const verifiedToken = (await getStoreData<IndividualToken>('individual_tokens')).find(item => item.id === tokenId && item.blob?.size > 0);
    const verifiedNpc = (await getStoreData<Npc>('npcs')).find(item => item.id === npcId && item.tokenId === tokenId);
    if (!verifiedToken || !verifiedNpc) throw new Error('O retrato foi recebido, mas a verificação de persistência falhou. A imagem não foi descartada; tente importar novamente.');
    const verifiedNpcs = await getStoreData<Npc>('npcs');
    setAllNpcs(verifiedNpcs);
    setPresentNpcs(verifiedNpcs.filter(npc => persisted.presentNpcIds.includes(npc.id)));
    window.dispatchEvent(new CustomEvent('braseiro-npc-portrait-updated', { detail: { npcId, tokenId, updatedAt: Date.now() } }));
    setImageImportStatus('Retrato de ' + target.name + ' recebido, salvo e vinculado ao NPC.');
  };"""
if status_old not in app:
    raise SystemExit("NPC portrait status anchor missing")
app = app.replace(status_old, status_new, 1)

# Native target takes precedence over localStorage, so NPC mapping survives process recreation.
target_old = """          const target = JSON.parse(localStorage.getItem('braseiro.pendingSharedImageTarget') || 'null') as {
            kind?: 'scene' | 'npc'; campaignId?: string; sceneId?: string; npcId?: string; fingerprint?: string; createdAt?: number;
          } | null;"""
target_new = """          const nativeTarget = bridge?.getSharedImageTarget ? await bridge.getSharedImageTarget().catch(() => null) : null;
          const target = JSON.parse(nativeTarget || localStorage.getItem('braseiro.pendingSharedImageTarget') || 'null') as {
            kind?: 'scene' | 'npc'; campaignId?: string; sceneId?: string; npcId?: string; fingerprint?: string; createdAt?: number;
          } | null;"""
if target_old not in app:
    raise SystemExit("shared image target parse anchor missing")
app = app.replace(target_old, target_new, 1)

# Clear both representations only after successful persistence.
clear_anchor = """        await bridge?.clearSharedImage?.().catch(() => undefined);
        localStorage.removeItem('braseiro.pendingSharedImageTarget');"""
clear_new = """        await bridge?.clearSharedImage?.().catch(() => undefined);
        await bridge?.clearSharedImageTarget?.().catch(() => undefined);
        localStorage.removeItem('braseiro.pendingSharedImageTarget');"""
if clear_anchor not in app:
    raise SystemExit("shared image clear anchor missing")
app = app.replace(clear_anchor, clear_new, 1)

# Friendly transient-network message, with scene preservation explicit.
friendly_anchor = """  if (/429|quota|cota|resource_exhausted|limite temporario|limite temporário/i.test(raw)) {
    return 'A cota do Gemini está temporariamente esgotada. Tente novamente mais tarde; a campanha não foi alterada.';
  }"""
friendly_new = friendly_anchor + """
  if (/unable to resolve host|no address associated with hostname|unknownhost|software caused connection abort|connection abort|connection reset|sockettimeout|timed out|network is unreachable|failed to connect|temporary failure in name resolution|conexão temporariamente indisponível/i.test(raw)) {
    return 'A conexão com o Gemini caiu antes de concluir o turno. A cena anterior foi preservada sem alterações. Verifique a internet e tente a ação novamente.';
  }"""
if friendly_anchor not in app:
    raise SystemExit("friendlyTurnError network anchor missing")
app = app.replace(friendly_anchor, friendly_new, 1)
write(app_rel, app)

# Immersive NPC renderer: instant override on successful import, even before
# parent props settle; also reload token blobs on portraitUpdatedAt changes.
imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)
state_anchor = """  const [npcTokenUrls, setNpcTokenUrls] = useState<Record<string, string>>({});
  const [selectedNpc, setSelectedNpc] = useState<Npc | null>(null);"""
if state_anchor not in imm:
    raise SystemExit("immersive NPC token state anchor missing")
imm = imm.replace(state_anchor, """  const [npcTokenUrls, setNpcTokenUrls] = useState<Record<string, string>>({});
  const [npcTokenOverrides, setNpcTokenOverrides] = useState<Record<string, string>>({});
  const [npcPortraitRevision, setNpcPortraitRevision] = useState(0);
  const [selectedNpc, setSelectedNpc] = useState<Npc | null>(null);""", 1)

effect_dep_old = "  }, [props.presentNpcs.map(npc => npc.id + ':' + (npc.tokenId || '')).join('|')]);"
effect_dep_new = "  }, [props.presentNpcs.map(npc => npc.id + ':' + (npc.tokenId || '') + ':' + ((npc as any).portraitUpdatedAt || '')).join('|'), npcPortraitRevision]);"
if effect_dep_old not in imm:
    raise SystemExit("immersive NPC token effect dependency anchor missing")
imm = imm.replace(effect_dep_old, effect_dep_new, 1)

effect_insert_anchor = effect_dep_new + "\n"
listener = """  useEffect(() => {
    const onPortraitUpdated = (event: Event) => {
      const detail = (event as CustomEvent<{ npcId?: string; tokenId?: string }>).detail || {};
      if (detail.npcId && detail.tokenId) {
        setNpcTokenOverrides(current => ({ ...current, [detail.npcId!]: detail.tokenId! }));
      }
      setNpcPortraitRevision(value => value + 1);
    };
    window.addEventListener('braseiro-npc-portrait-updated', onPortraitUpdated as EventListener);
    return () => window.removeEventListener('braseiro-npc-portrait-updated', onPortraitUpdated as EventListener);
  }, []);

"""
if listener.strip() not in imm:
    imm = imm.replace(effect_insert_anchor, effect_insert_anchor + listener, 1)

portrait_old = "            const portrait = npc.tokenId ? npcTokenUrls[npc.tokenId] : null;"
portrait_new = """            const effectiveTokenId = npcTokenOverrides[npc.id] || npc.tokenId || '';
            const portrait = effectiveTokenId ? npcTokenUrls[effectiveTokenId] : null;"""
if portrait_old not in imm:
    raise SystemExit("immersive NPC portrait render anchor missing")
imm = imm.replace(portrait_old, portrait_new, 1)
write(imm_rel, imm)

# ---------------------------------------------------------------------------
# Version bump 4.9.18 -> 4.9.19.
# ---------------------------------------------------------------------------
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.19"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.18"', '"version": "4.9.19"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.18'", "APP_VERSION = '4.9.19'", version)
version = re.sub(r"APP_VERSION_CODE = 49018", "APP_VERSION_CODE = 49019", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49018", "versionCode = 49019", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.18"', 'versionName = "4.9.19"', gradle)
write(gradle_rel, gradle)

main = read(main_rel)
main = main.replace("index.html?v=49018&immersive=1", "index.html?v=49019&immersive=1")
write(main_rel, main)

# ---------------------------------------------------------------------------
# Hard gates.
# ---------------------------------------------------------------------------
main = read(main_rel)
inbox = read(inbox_rel)
app = read(app_rel)
game = read(game_rel)
imm = read(imm_rel)
global_ts = read(global_rel)
db = read("lib/db.ts")
models = read("lib/geminiModels.ts")

checks = {
    "network retry native": "repeat(3)" in main and "isTransientGeminiNetworkError" in main and "callGeminiOnce" in main,
    "network error friendly": "A conexão com o Gemini caiu antes de concluir o turno" in app,
    "scene rollback preserved": "campaignRef.current = base;" in app and "setCampaign(base);" in app,
    "native image target persisted": "KEY_TARGET_JSON" in inbox and "setTarget(context" in inbox and "pendingTarget(context" in inbox,
    "native bridge target methods": "setSharedImageTarget" in main and "getSharedImageTarget" in main and "clearSharedImageTarget" in main,
    "web bridge target types": "setSharedImageTarget?: " in global_ts and "getSharedImageTarget?: " in global_ts,
    "prompt target dual persisted": "setSharedImageTarget(targetJson)" in game,
    "import prefers native target": "bridge?.getSharedImageTarget" in app,
    "npc import fresh db": "const latestNpcs = await getStoreData<Npc>('npcs');" in app,
    "npc import verified": "verifiedToken" in app and "verifiedNpc" in app,
    "npc portrait update event": "braseiro-npc-portrait-updated" in app and "braseiro-npc-portrait-updated" in imm,
    "immersive override": "npcTokenOverrides" in imm and "npcPortraitRevision" in imm,
    "one Master model preserved": "const TEXT_KEY = 'braseiro.geminiModel.text'" in models and "getGeminiTextModel(role" not in models,
    "party anchor preserved": 'data-party-world-anchored="true"' in read("components/EriadorMap.tsx"),
    "db identity preserved": "const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
    "version": "4.9.19" in read(version_rel) and "49019" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4919_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.19_NETWORK_NPC_IMAGE_OK")
