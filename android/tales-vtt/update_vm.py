from pathlib import Path

vm_path = Path(r"c:\Users\bcruz\Downloads\BRASEIRO_TALES_FLOOD_DEV\app\src\main\java\com\braseiro\talesvtt\game\GameViewModel.kt")
text = vm_path.read_text(encoding="utf-8")

# 1. Add imports if not present
imports_to_add = """import com.braseiro.talesvtt.life.AdventurePackage
import com.braseiro.talesvtt.life.AdventureRepository
import com.braseiro.talesvtt.life.PersistentLifeEngine
import com.braseiro.talesvtt.life.CharacterIdentityKey
import com.braseiro.talesvtt.life.CharacterLegacyRecord
import com.braseiro.talesvtt.life.ConfirmedDeed
"""

if "import com.braseiro.talesvtt.life.AdventurePackage" not in text:
    target_import = "import com.braseiro.talesvtt.data.KidJsonCodec\n"
    text = text.replace(target_import, target_import + imports_to_add, 1)

# 2. Add repo and life engine properties
props = """
  val adventureRepo by lazy { AdventureRepository(getApplication()) }
  val lifeEngine by lazy { PersistentLifeEngine(getApplication()) }
"""
if "val adventureRepo by lazy" not in text:
    target_class = "class GameViewModel(application: Application) : AndroidViewModel(application) {\n"
    text = text.replace(target_class, target_class + props, 1)

# 3. Add startAdventureFromPackage method
method = """
  fun startAdventureFromPackage(pkg: AdventurePackage) {
    if (_motorStatus.value.busy) return
    val targetProfile = if (pkg.isFloodTeens()) RulesProfile.FLOOD_TEENS else RulesProfile.TALES_KIDS
    _state.value = _state.value.copy(rulesProfile = targetProfile)

    val refreshedKid = when (targetProfile) {
      RulesProfile.TALES_KIDS -> _state.value.kid.copy(
        luck = _state.value.kid.maxLuck,
        conditions = emptySet(),
        broken = false,
        prideAvailable = true
      )
      RulesProfile.FLOOD_TEENS -> _state.value.kid.copy(
        luck = 0,
        conditions = emptySet(),
        broken = false,
        prideAvailable = false,
        shameAvailable = true,
        scarPending = false
      )
    }

    val locName = pkg.locations.firstOrNull { it.id == pkg.start.locationId }?.name ?: pkg.start.locationId
    val startNarrative = listOf(
      "=== ${pkg.title.uppercase()} ===",
      pkg.story.playerIntroduction.ifEmpty { pkg.story.premise },
      "Local inicial: $locName. O que vocês fazem a seguir?"
    )

    _state.value = GameState(
      rulesProfile = targetProfile,
      kid = refreshedKid,
      group = _state.value.group,
      scene = SceneState(
        title = pkg.title,
        chapter = if (targetProfile == RulesProfile.FLOOD_TEENS) "MISTÉRIO IMPORTADO • FLOOD" else "MISTÉRIO IMPORTADO • TALES",
        seasonYear = if (targetProfile == RulesProfile.FLOOD_TEENS) "Outono de 1995" else "Outono de 1986",
        location = locName,
        weather = "Nublado com vento frio",
        time = "16:30",
        daylight = "Crepúsculo",
        visualAssetKey = "approved_scene_outono_loop",
        narrative = startNarrative,
        choices = SceneChoiceDefaults.standard()
      ),
      revealed = RevealedState(
        clues = emptySet(),
        locations = pkg.locations.map { it.name }.toSet().ifEmpty { setOf(locName) },
        connections = emptySet()
      ),
      routeHistory = listOf(locName)
    )

    // Store GM-only secrets in private state so players cannot see them
    val gmSecrets = pkg.secrets.filter { it.visibility.equals("gm_only", ignoreCase = true) }
      .map { PrivateSecretState(id = it.id, text = it.truth, revealed = false) }
    gmPrivateState = GmPrivateState(secrets = gmSecrets)

    // Persist and navigate
    _adventureStarted.value = true
    prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, true).apply()
    _dataStatus.value = "Mistério '${pkg.title}' iniciado com sucesso."
    persistAutosave()
    _motorRoute.value = MotorRouteHint.SCENE
  }
"""

if "fun startAdventureFromPackage" not in text:
    marker = "  fun startRandomAdventure() {\n"
    text = text.replace(marker, method + "\n" + marker, 1)

vm_path.write_text(text, encoding="utf-8")
print("Updated GameViewModel.kt successfully")
