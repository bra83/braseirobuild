from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
kroot=root/'app/src/main/java/com/braseiro/wwn'
manifest=(root/'app/src/main/AndroidManifest.xml').read_text('utf-8')
main=(kroot/'MainActivity.kt').read_text('utf-8')
bridge=(kroot/'bridge/BraseiroBridge.kt').read_text('utf-8')
repo=(kroot/'data/CampaignRepository.kt').read_text('utf-8')
dao=(kroot/'data/BraseiroDao.kt').read_text('utf-8')
models=(kroot/'data/PersistenceModels.kt').read_text('utf-8')
scene=(kroot/'bridge/SceneImageRuntime.kt').read_text('utf-8')
tts=(kroot/'bridge/NativeTtsRuntime.kt').read_text('utf-8')
port=(root/'app/src/main/assets/www/core/persistence-port.js').read_text('utf-8')
checks=[]
def ck(name, cond):
    checks.append((name,bool(cond)))

def expression_body_return_violations(text):
    out=[]
    # Catch expression-body try functions that contain a non-local return inside try block.
    for m in re.finditer(r'fun\s+\w+\s*\([^)]*\)[^{=\n]*=\s*try\s*\{', text):
        start=text.find('{',m.start())+1; depth=1; i=start
        while i<len(text) and depth:
            if text[i]=='{': depth+=1
            elif text[i]=='}': depth-=1
            i+=1
        body=text[start:i-1]
        if re.search(r'(?<!@)\breturn\b', body): out.append(text[m.start():text.find('\n',m.start())])
    return out

allkt='\n'.join(p.read_text('utf-8') for p in kroot.rglob('*.kt'))
ck('kotlin_no_expression_body_nonlocal_return', not expression_body_return_violations(allkt))
for name in ['PortableRestoreEvent','PortableRestoreSnapshot','PortableRestorePayload','PortableRestoreReceipt','CampaignRestoreConflictException','CampaignRestoreValidationException']:
    ck('model_'+name, name in models)
for name in ['listSnapshots','getActiveCampaignId','setActiveCampaignId','restorePortableBackup']:
    ck('repo_'+name, re.search(r'fun\s+'+name+r'\s*\(',repo) is not None)
for name in ['listSnapshots','getAppMeta','upsertAppMeta','deleteEvents','deleteSnapshots','deleteCampaignState','deleteCampaignMeta']:
    ck('dao_'+name, re.search(r'fun\s+'+name+r'\s*\(',dao) is not None)
ck('restore_transactional', 'db.runInTransaction(Callable {' in repo and 'restorePortableBackup' in repo)
ck('restore_requires_explicit_overwrite', 'TARGET_CAMPAIGN_EXISTS' in repo and 'if (existing != null && !overwrite)' in repo)
ck('restore_reactivates_campaign', 'upsertAppMeta(AppMetaEntity(ACTIVE_CAMPAIGN_KEY, payload.campaignId))' in repo)
ck('restore_requires_current_snapshot', 'portable backup lacks a current-state snapshot' in repo)
ck('native_port_loads_concrete_snapshots', 'this.native.listSnapshots' in port and 'loaded.snapshots = snapshots.snapshots' in port)
ck('tts_manifest_visibility', 'android.intent.action.TTS_SERVICE' in manifest)
ck('tts_shutdown', 'ttsRuntime.shutdown()' in main and 'engine?.shutdown()' in tts)
ck('tts_narration_contract_nonmechanical', '.put("mechanicalMutation", false)' in tts)
ck('scene_picker_mime', 'image/png' in main and 'image/jpeg' in main and 'image/webp' in main)
ck('scene_private_storage', 'scene-images-v1' in scene and 'privateStorage' in scene)
ck('scene_bounds', 'MAX_IMAGE_BYTES' in scene and 'MAX_DIMENSION' in scene and 'MAX_PIXELS' in scene)
ck('scene_internal_origin_only', 'appassets.androidplatform.net' in scene and 'private-scene' in scene)
ck('webview_debug_off', 'WebView.setWebContentsDebuggingEnabled(false)' in main)
ck('webview_file_access_off', 'settings.allowFileAccess = false' in main and 'settings.allowContentAccess = false' in main)
ck('webview_external_navigation_blocked', 'shouldOverrideUrlLoading' in main and 'LOCAL_HOST' in main)
ck('no_broad_storage_permissions', 'READ_EXTERNAL_STORAGE' not in manifest and 'WRITE_EXTERNAL_STORAGE' not in manifest and 'MANAGE_EXTERNAL_STORAGE' not in manifest)
ck('credential_filter_restore', 'rejectCredentialKeys' in bridge and 'CREDENTIAL_KEY' in bridge)
failed=[n for n,v in checks if not v]
for n,v in checks: print(f'{"PASS" if v else "FAIL"} {n}')
print(f'CLOSURE065_NATIVE_CONTRACT checks={len(checks)} pass={len(checks)-len(failed)} fail={len(failed)}')
if failed:
    print('FAILED='+','.join(failed)); sys.exit(1)
