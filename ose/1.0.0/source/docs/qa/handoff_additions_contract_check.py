#!/usr/bin/env python3
from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[2]
main=(ROOT/'app-android/src/main/kotlin/braseiro/ose/app/MainActivity.kt').read_text()
secure=(ROOT/'app-android/src/main/kotlin/braseiro/ose/app/GeminiSecureStore.kt').read_text()
tts=(ROOT/'app-android/src/main/kotlin/braseiro/ose/app/GeminiTtsController.kt').read_text()
client=(ROOT/'app-android/src/main/kotlin/braseiro/ose/app/GeminiInteractionsClient.kt').read_text()
js=(ROOT/'web-ui-p1/p1.js').read_text()
html=(ROOT/'web-ui-p1/index.html').read_text()
css=(ROOT/'web-ui-p1/p1.css').read_text()
checks={}
checks['tts_reads_canonical_visible_narration']='tts.playVisibleNarration(current.campaignState.game.session.visibleNarration)' in main
checks['tts_replay_bound_to_current_visible_narration']='tts.replayVisibleNarration(current.campaignState.game.session.visibleNarration)' in main and 'lastNarration == currentText' in tts
checks['two_independent_secure_slots']=all(x in secure for x in ['TEXT_SCENE("TEXT_SCENE")','TTS_AUDIO("TTS_AUDIO")','AndroidKeyStore','noBackupFilesDir'])
checks['no_key_echo_to_webview']='put("key"' not in main and 'Plaintext keys are never returned to the WebView' in secure
checks['scene_image_explicit_user_action']= "SceneImageGenerate',{explicitUserAction:true}" in js
checks['scene_image_presentation_only']='mechanicalDelta", false' in main and 'Presentation-only media' in (ROOT/'app-android/src/main/kotlin/braseiro/ose/app/PresentationMediaStore.kt').read_text()
checks['context_suggestions_server_projected']='ContextualDecisionSuggester.asJson(current)' in main
checks['suggestions_prefill_not_send']='data-suggest' in js and "t.value=b.dataset.suggest" in js
checks['free_player_reaction_preserved']='id="reactionInput"' in js and "postNative('PlayerReaction'" in js
checks['two_masked_fields']=len(re.findall(r'<input[^>]+type="password"',html,re.I))==2
checks['exact_bottom_nav_labels']=all(x in html for x in ['>SESSÃO</button>','>MAPA</button>','>FICHA</button>','>COMPANHIA</button>']) and html.count('class="navbtn')==4
checks['geometry_v1_constants']='hexW:60,hexH:69.282,xStep:60,yStep:51.962,oddOffset:30,originX:42,originY:40' in js and 'data-geometry-version="1"' in js
checks['map_read_only']='data-direct-movement="forbidden"' in js and 'CÂMERA ≠ POSIÇÃO' in js
checks['gemini_new_steps_schema']='optJSONArray("steps")' in client and 'step.optString("type") != "model_output"' in client
checks['api_revision_2026_05_20']='Api-Revision", "2026-05-20"' in client
checks['approved_dark_gold_nav']='background:#241107' in css and 'border-top:2px solid #8e6e2e' in css and 'background:#d7a84c' in css
checks['no_obvious_api_key_literal']=not re.search(r'(AIza[0-9A-Za-z_-]{20,}|sk-[0-9A-Za-z]{20,})', '\n'.join([main,secure,tts,client,js,html,css]))
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(f'{k}={"PASS" if v else "FAIL"}')
print(f'TOTAL={sum(checks.values())}/{len(checks)}')
if failed:
    print('FAILED='+','.join(failed)); sys.exit(1)
