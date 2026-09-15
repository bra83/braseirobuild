from pathlib import Path
import hashlib,re,sys
root=Path(__file__).resolve().parents[1]
k=root/'app/src/main/java/com/braseiro/wwn'
main=(k/'MainActivity.kt').read_text('utf8'); bridge=(k/'bridge/BraseiroBridge.kt').read_text('utf8'); tts=(k/'bridge/NativeTtsRuntime.kt').read_text('utf8'); audio=(k/'bridge/NativeAudioRuntime.kt').read_text('utf8'); synth=(k/'audio/ProceduralAudioSynth.java').read_text('utf8')
checks={
'bridge_audio_status':'fun audioStatus' in bridge,
'bridge_audio_configure':'fun audioConfigure' in bridge,
'bridge_audio_scene_plan':'fun audioApplyScenePlan' in bridge,
'bridge_audio_stop':'fun audioStopAll' in bridge,
'capability_audio_session':'.put("audioSession", audioRuntime != null)' in bridge,
'main_audio_runtime':'NativeAudioRuntime(this)' in main,
'lifecycle_pause':'audioRuntime.pauseForLifecycle()' in main,
'lifecycle_resume':'audioRuntime.resumeFromLifecycle()' in main,
'lifecycle_shutdown':'audioRuntime.shutdown()' in main,
'tts_ptbr':'Locale("pt", "BR")' in tts,
'tts_session_flush':'TextToSpeech.QUEUE_FLUSH' in tts,
'tts_pitch':'engine?.setPitch(pitch)' in tts,
'tts_duck_callback':'onSpeakingChanged(true)' in tts and 'onSpeakingChanged(false)' in tts,
'audio_track': 'AudioTrack.Builder()' in audio,
'audio_loop': 'setLoopPoints' in audio,
'audio_narration_duck': '0.62f' in audio and '0.42f' in audio,
'audio_no_mechanics': 'mechanicalMutation' in audio,
'procedural_no_external_assets': 'renderLoop' in synth and 'renderOneShot' in synth,
'no_math_random_js': not any('Math.random' in p.read_text('utf8',errors='ignore') for p in (root/'app/src/main/assets/www/core').glob('*.js')),
}
for n,v in checks.items(): print(('PASS ' if v else 'FAIL ')+n)
print(f'CLOSURE069_NATIVE_AUDIO checks={len(checks)} pass={sum(checks.values())} fail={len(checks)-sum(checks.values())}')
if not all(checks.values()): sys.exit(1)
