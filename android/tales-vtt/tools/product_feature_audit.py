#!/usr/bin/env python3
from pathlib import Path
import json, re, sys
root=Path(sys.argv[1] if len(sys.argv)>1 else '.')
java=root/'app/src/main/java/com/braseiro/talesvtt'
checks={
 'secure_api_key_store': (java/'data/SecureSecretStore.kt', ['AndroidKeyStore','AES/GCM/NoPadding']),
 'gemini_text_model_config': (java/'game/MotorBarbaraTransport.kt', ['ttsModel','ttsVoice','saveConfig','model']),
 'gemini_voice_tts': (java/'audio/GeminiNarrationTts.kt', ['responseModalities','AUDIO','speechConfig','voiceName']),
 'android_tts_fallback': (java/'components/NarrationTts.kt', ['gemini.enabled()','speakAndroid']),
 'portable_campaign_backup': (java/'data/PortableCampaignBackup.kt', ['braseiro-tales-backup','Round-trip','createAutomaticBackup','attachments']),
 'kid_json_import_export': (java/'data/KidJsonCodec.kt', ['braseiro-tales-kid','profile','attributes','skills']),
 'scene_prompt_export': (java/'data/SceneImageExchange.kt', ['buildPrompt','ACTION_SEND','pending_fingerprint']),
 'scene_image_share_import': (java/'data/SceneImageExchange.kt', ['ShareReceiverActivity','acceptImportedToken','storeIncoming']),
 'scene_image_persisted': (java/'game/GameSnapshotCodec.kt', ['scene.importedImageUri']),
 'manual_pdf_indexer': (java/'data/ManualLibraryRepository.kt', ['PDFTextStripper','PdfRenderer','TextRecognition','jsonl']),
 'rag_search_ui': (java/'screens/LibraryScreen.kt', ['ADICIONAR PDFs','BUSCAR NO RAG','onSearch']),
 'rag_into_motor': (java/'game/MotorBarbaraTransport.kt', ['rag_context','manualLibrary.search','rag_instruction']),
 'menu_data_tools': (java/'screens/MenuScreen.kt', ['EXPORTAR BACKUP COMPLETO','IMPORTAR FICHA JSON','ABRIR BIBLIOTECA','Modelo Gemini • voz']),
 'share_receiver_manifest': (root/'app/src/main/AndroidManifest.xml', ['.data.ShareReceiverActivity','android.intent.action.SEND','image/*']),
}
results={}
for name,(path,tokens) in checks.items():
    text=path.read_text(encoding='utf-8') if path.exists() else ''
    missing=[t for t in tokens if t not in text]
    results[name]={'pass':path.exists() and not missing,'file':str(path.relative_to(root)) if path.exists() else str(path),'missing':missing}
# firewall sanity
alltext='\n'.join(p.read_text(encoding='utf-8',errors='ignore') for p in java.rglob('*.kt'))
forbidden={
 'dnd_package': r'package\s+com\.braseiro\.dnd',
 'dnd_rules_engine': r'DndRulesEngine|MystaraRulesEngine|CombatStateMachine',
 'tactical_hp': r'val\s+hp\b|var\s+hp\b|HitPoints|InitiativeOrder|RADAR TÁTICO',
}
for k,pat in forbidden.items():
    hits=re.findall(pat,alltext,re.I)
    results['forbidden_'+k]={'pass':not hits,'hits':hits[:10]}
summary={'pass':all(x.get('pass',False) for x in results.values()),'checks':results}
print(json.dumps(summary,ensure_ascii=False,indent=2))
sys.exit(0 if summary['pass'] else 1)
