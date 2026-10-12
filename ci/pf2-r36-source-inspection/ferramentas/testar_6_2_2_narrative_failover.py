from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
gem=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java').read_text(encoding='utf-8')
off=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/OfflineMasterEngine.java').read_text(encoding='utf-8')
san=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/GeneratedTextSanitizer.java').read_text(encoding='utf-8')
ver=(root/'version.properties').read_text(encoding='utf-8')
checks={
 'version_622':'VERSION_NAME=6.2.2' in ver and 'VERSION_CODE=602200' in ver,
 'narrator_default_lite':'prefs.getString("narrator_text_model","gemini-3.5-flash-lite")' in main,
 'no_latest_alias':'gemini-flash-latest' not in gem and 'gemini-flash-latest' not in main,
 'stable_failover':all(x in gem for x in ['gemini-3.5-flash-lite','gemini-3.6-flash','gemini-3.5-flash','gemini-3.1-flash-lite']),
 'stream_retry':'streamTextWithRetry' in gem and all(x in gem for x in ['s==429','s==500','s==502','s==503','s==504']),
 'single_turn_failover':'failover.size()<3' in gem,
 'offline_opening_naturalized':all(x in off for x in ['isOpeningDirective(action)','naturalizeCatalog(catalog)','playerFacingAction(action)']),
 'offline_no_old_meta':all(x not in off for x in ['Nada no estado da campanha é alterado por suposição','A resposta do ambiente é clara o bastante','continuidade preservada no banco local']),
 'hard_leak_guard':'hasInternalNarrationLeak' in main and 'vazamento de instruções internas do motor' in main,
 'sanitizer_leak_guard':'stripInternalNarrativeLeak' in san and 'ponto inicial catalogado desta aventura' in san,
 'no_fake_chapters':'TextView title=text("CAPÍTULO "+(i+1)+" DE "' not in main,
 'offline_mechanics_no_opening_test':'if(!opening&&verified.length()==0&&suggested!=null&&suggested.length()>0)' in off,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('PASS' if v else 'FAIL')+': '+k)
if failed: raise SystemExit('Falhas: '+', '.join(failed))
print(f'RESULTADO: {len(checks)}/{len(checks)} PASS')
