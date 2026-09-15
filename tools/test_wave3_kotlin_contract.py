from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
checks={
 'dao_get_event': ('app/src/main/java/com/braseiro/wwn/data/BraseiroDao.kt','fun getEvent('),
 'dao_list_events': ('app/src/main/java/com/braseiro/wwn/data/BraseiroDao.kt','fun listEvents('),
 'repo_append_narrative': ('app/src/main/java/com/braseiro/wwn/data/CampaignRepository.kt','fun appendNarrative('),
 'repo_no_state_update_in_append': ('app/src/main/java/com/braseiro/wwn/data/CampaignRepository.kt','checksumBefore = parent.checksumAfter'),
 'bridge_append_narrative': ('app/src/main/java/com/braseiro/wwn/bridge/BraseiroBridge.kt','fun appendNarrative('),
 'bridge_list_events': ('app/src/main/java/com/braseiro/wwn/bridge/BraseiroBridge.kt','fun listEvents('),
 'model_narrative_request': ('app/src/main/java/com/braseiro/wwn/data/PersistenceModels.kt','data class NarrativeAppendRequest('),
}
failed=[]
for name,(rel,needle) in checks.items():
    if needle not in (ROOT/rel).read_text(): failed.append(name)
if failed:
    print('WAVE3_KOTLIN_CONTRACT_FAIL',','.join(failed)); raise SystemExit(1)
print('WAVE3_KOTLIN_CONTRACT_STATIC_PASS checks='+str(len(checks))+' compile=NOT_EXECUTED_ANDROID_TOOLCHAIN_UNAVAILABLE')
