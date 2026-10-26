from pathlib import Path

# 1. Update MenuScreen.kt
menu_path = Path(r"c:\Users\bcruz\Downloads\BRASEIRO_TALES_FLOOD_DEV\app\src\main\java\com\braseiro\talesvtt\screens\MenuScreen.kt")
menu_text = menu_path.read_text(encoding="utf-8")

# Add parameters to MenuScreen
param_anchor = "  onStartRandomAdventure: () -> Unit,\n"
param_insert = "  onOpenAdventures: () -> Unit = {},\n  onOpenVoiceSettings: () -> Unit = {},\n"
if "onOpenAdventures" not in menu_text:
    menu_text = menu_text.replace(param_anchor, param_insert + param_anchor, 1)

# Add buttons in COMEÇAR section
button_anchor = 'PrimaryButton("NOVA AVENTURA ALEAT'
button_idx = menu_text.find(button_anchor)
if button_idx != -1 and "AVENTURAS E MISTÉRIOS" not in menu_text:
    line_start = menu_text.rfind('\n', 0, button_idx) + 1
    new_buttons = """        PrimaryButton("AVENTURAS E MISTÉRIOS (JSON / BÁRBARA)", onClick = onOpenAdventures, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
"""
    menu_text = menu_text[:line_start] + new_buttons + menu_text[line_start:]

# Add button in VOZ & LEITURA section
tts_anchor = 'Text("OUVIR lê somente a narrativa'
tts_idx = menu_text.find(tts_anchor)
if tts_idx != -1 and "CONFIGURAÇÕES DE VOZ (TTS V3)" not in menu_text:
    # Find end of Column
    col_end = menu_text.find('}', tts_idx)
    insert_pos = menu_text.find('}', col_end + 1) # end of Row
    tts_button = """        Spacer(Modifier.height(10.dp))
        SecondaryButton("CONFIGURAÇÕES DE VOZ (TTS V3)", onClick = onOpenVoiceSettings, modifier = Modifier.fillMaxWidth())
"""
    menu_text = menu_text[:insert_pos + 1] + "\n" + tts_button + menu_text[insert_pos + 1:]

menu_path.write_text(menu_text, encoding="utf-8")
print("MenuScreen.kt updated successfully!")

# 2. Update AppNavGraph.kt
nav_path = Path(r"c:\Users\bcruz\Downloads\BRASEIRO_TALES_FLOOD_DEV\app\src\main\java\com\braseiro\talesvtt\navigation\AppNavGraph.kt")
nav_text = nav_path.read_text(encoding="utf-8")

# Add imports
imports = """import androidx.compose.runtime.remember
import com.braseiro.talesvtt.audio.UniversalTtsV3Engine
import com.braseiro.talesvtt.screens.AdventuresScreen
import com.braseiro.talesvtt.screens.VoiceSettingsScreen
"""
if "AdventuresScreen" not in nav_text:
    nav_text = nav_text.replace("import com.braseiro.talesvtt.screens.TroubleScreen\n", "import com.braseiro.talesvtt.screens.TroubleScreen\n" + imports, 1)

# Pass onOpenAdventures and onOpenVoiceSettings in MenuScreen call
menu_call_anchor = "        onCreateRandomKid = gameViewModel::createRandomKid,\n"
menu_call_insert = "        onOpenAdventures = { openInternal(InternalRoutes.ADVENTURES) },\n        onOpenVoiceSettings = { openInternal(InternalRoutes.VOICE_SETTINGS) },\n"
if "onOpenAdventures" not in nav_text:
    nav_text = nav_text.replace(menu_call_anchor, menu_call_insert + menu_call_anchor, 1)

# Add composables for ADVENTURES and VOICE_SETTINGS
end_anchor = "    composable(InternalRoutes.ENDING) {"
adventures_routes = """    composable(InternalRoutes.ADVENTURES) {
      AdventuresScreen(
        repository = gameViewModel.adventureRepo,
        currentProfile = gameState.rulesProfile.name,
        onStartAdventure = { pkg ->
          gameViewModel.startAdventureFromPackage(pkg)
          onNavigate(AppDestination.SCENE)
        },
        onNavigateBack = { onNavigate(AppDestination.MENU) }
      )
    }
    composable(InternalRoutes.VOICE_SETTINGS) {
      val ttsEngine = remember { UniversalTtsV3Engine(navController.context) }
      VoiceSettingsScreen(
        ttsEngine = ttsEngine,
        onNavigateBack = { onNavigate(AppDestination.MENU) }
      )
    }
"""
if "InternalRoutes.ADVENTURES" not in nav_text:
    nav_text = nav_text.replace(end_anchor, adventures_routes + "    " + end_anchor, 1)

nav_path.write_text(nav_text, encoding="utf-8")
print("AppNavGraph.kt updated successfully!")
