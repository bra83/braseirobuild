from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
def text(rel): return (ROOT/rel).read_text(encoding='utf-8')
checks={}
def ck(name, cond, detail=''):
    checks[name]={'pass':bool(cond),'detail':detail}

nav=text('app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt')
lib=text('app/src/main/java/com/braseiro/talesvtt/screens/LibraryScreen.kt')
panel=text('app/src/main/java/com/braseiro/talesvtt/components/NarrativePanel.kt')
exchange=text('app/src/main/java/com/braseiro/talesvtt/data/SceneImageExchange.kt')
scene=text('app/src/main/java/com/braseiro/talesvtt/screens/SceneScreen.kt')
menu=text('app/src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt')
shell=text('app/src/main/java/com/braseiro/talesvtt/components/AppShell.kt')
header=text('app/src/main/java/com/braseiro/talesvtt/components/AppHeader.kt')
tts=text('app/src/main/java/com/braseiro/talesvtt/components/NarrationTts.kt')
gtts=text('app/src/main/java/com/braseiro/talesvtt/audio/GeminiNarrationTts.kt')
motor=text('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt')
gv=text('app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt')
adventure=text('app/src/main/java/com/braseiro/talesvtt/game/AdventureFactory.kt')
kid=text('app/src/main/java/com/braseiro/talesvtt/game/RandomKidFactory.kt')
gradle=text('app/build.gradle.kts')

ck('starts_in_menu_not_forced_adventure', 'startDestination: String = AppDestination.MENU.route' in nav)
ck('navigation_does_not_restore_library_state', 'restoreState = false' in nav and 'saveState = false' in nav)
ck('library_hamburger_goes_menu', 'onMenuClick = { onNavigate(AppDestination.MENU) }' in lib)
ck('library_explicit_back_to_config', 'VOLTAR ÀS CONFIGURAÇÕES' in lib and 'onNavigate(AppDestination.MENU)' in lib)
ck('master_text_has_real_inner_scroll', 'rememberScrollState()' in panel and '.verticalScroll(narrativeScroll)' in panel)

# Restrict the prompt check to buildPrompt, because fingerprint intentionally includes narration only to bind an imported image to the same scene.
prompt=exchange.split('fun buildPrompt',1)[1].split('fun prepareShare',1)[0]
ck('gemini_scene_prompt_excludes_master_narration', 'scene.narrative' not in prompt and 'Não copie, resuma nem escreva o texto do Mestre' in prompt)
ck('gemini_scene_prompt_is_per_scene', 'Título da cena:' in prompt and 'Local:' in prompt and 'Clima:' in prompt and 'Horário:' in prompt)
ck('decisions_send_player_intent', 'onDeclareAction("${choice.title}: ${choice.description}")' in scene)
ck('free_text_sends_player_intent', 'FreeActionPrompt(onSubmit' in scene and 'onDeclareAction(text)' in scene)
ck('scene_shows_motor_feedback', 'motorStatus.lastMessage' in scene)
ck('random_adventure_button', 'NOVA AVENTURA ALEATÓRIA' in menu and 'onStartRandomAdventure' in menu)
ck('random_adventure_history_prevents_exact_repeat', 'KEY_ADVENTURE_HISTORY' in gv and 'seed.signature' in gv and 'signature !in usedSignatures' in adventure)
ck('random_adventure_waits_for_valid_motor_packet', 'val acceptedStart = result.acceptedActions > 0' in gv)
ck('random_kid_button', 'CRIAR KID ALEATÓRIO PELAS REGRAS' in menu and 'onCreateRandomKid' in menu)
ck('random_kid_canonical_bounds', 'random.nextInt(10, 16)' in kid and 'remainingSkills = 10' in kid and '15 - age' in kid and 'sum() != kid.age' in kid)
ck('help_order_icon_and_dialog', 'ORDEM DE JOGO' in shell and 'onHelpClick' in header)
ck('gemini_tts_is_default', 'prefs.getString("tts_mode", "gemini")' in gtts and 'ttsMode: String = "gemini"' in motor)
# In Gemini mode failure is surfaced, not sent to Android TTS.
gemini_branch=tts.split('if (gemini.enabled())',1)[1].split('} else {',1)[0]
ck('no_silent_android_tts_fallback', 'speakAndroid' not in gemini_branch and 'Gemini TTS falhou' in gemini_branch)
ck('long_gm_opening', '700–1100 palavras' in motor and 'geografia local' in motor and 'sons, cheiros, pessoas' in motor)
ck('longer_gm_regular_scenes', '250–650 palavras' in motor)
weathers=re.findall(r'"(Céu limpo|Nublado|Garoa|Chuva|Chuva forte|Neblina|Vento frio|Neve leve)"', adventure)
ck('weather_variety', len(set(weathers)) == 8, ', '.join(sorted(set(weathers))))
ck('weather_icon_is_dynamic', 'weatherLower' in header and 'WeatherRain' in header and 'WeatherSnow' in header and 'WeatherClear' in header)
ck('gradle_project_complete', all((ROOT/p).exists() for p in ['gradlew','gradlew.bat','settings.gradle.kts','build.gradle.kts','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties','app/build.gradle.kts','app/src/main/AndroidManifest.xml']))
ck('github_builder_can_detect_compile_sdk', 'compileSdk = 36' in gradle)
ck('version_133', 'versionCode = 6' in gradle and 'versionName = "1.3.3"' in gradle)
ck('manual_save_preserves_setup_vs_adventure', 'KEY_MANUAL_SAVE_ADVENTURE' in gv and 'restoredAdventureStarted' in gv)

result={'version':'1.3.3','pass':all(v['pass'] for v in checks.values()),'checks':checks}
out=ROOT/'docs/USER_REQUEST_1.3.3_AUDIT.json'
out.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
raise SystemExit(0 if result['pass'] else 1)
