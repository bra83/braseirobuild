#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(__file__).resolve().parents[1]
src=root/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(src/'MainActivity.java').read_text()
db=(src/'AppDb.java').read_text()
options=(src/'SceneOptionDiversifier.java').read_text()
opening=(src/'AdventureOpeningPlanner.java').read_text()
binder=(src/'NpcTokenAutoBinder.java').read_text()
audio=(src/'NarrativeAudioEngine.java').read_text()
version=(root/'version.properties').read_text()
checks={
 'versão 4.9.3+':tuple(int(x) for x in dict(line.split('=',1) for line in version.splitlines() if '=' in line).get('VERSION_NAME','0.0.0').split('.')[:3]) >= (4,9,3) and int(dict(line.split('=',1) for line in version.splitlines() if '=' in line).get('VERSION_CODE','0')) >= 490300,
 'banco v51 ou superior':bool(__import__('re').search(r'DB_VERSION\s*=\s*(?:5[1-9]|[6-9][0-9])',db)) and 'oldVersion < 51' in db,
 'backup tático exporta linha física':'listRawTacticalTokensForBackup' in db and 'SELECT world_profile,combatant_id,name,is_player,x,y,zone,updated_at FROM tactical_tokens' in db,
 'restore tático usa allowlist':'ContentValues v=new ContentValues()' in db and 'row.optDouble("x",0.5)' in db and 'jsonValues(row);v.put("world_profile"' not in db[db.find('private void restoreTacticalTokens'):db.find('private void restorePartyActions')],
 'aberturas possuem 24 arquiteturas':opening.count('{"') >= 24 and 'recent_opening_texts_' in main,
 'abertura bane clichês recentes':'recentMotifs' in opening and 'fumaça' in opening and 'guarita' in opening,
 'sugestões usam cena projetada':'projectSceneForSuggestions(env,currentSceneCastPreview)' in main,
 'sugestões genéricas rejeitadas':all(x in options for x in ['examinar a entrada','localizar o som','repeatsIntent']),
 'NPC presente orienta opção':('PEDIR A " + upperShort(npc)' in options or 'NEGOCIAR A EXIGÊNCIA COM " + upperShort(npc)' in options) and 'CONFRONTAR A FALA DE " + upperShort(npc)' in options,
 'token local garantido':'NpcFallbackAvatarFactory.create' in binder and (src/'NpcFallbackAvatarFactory.java').is_file(),
 'token é atualizado na cena':'scheduleSceneNpcTokenBinding' in main and 'last_scene_cast_preview' in main,
 'imagem vinculada por fingerprint':'PREF_SCENE_IMAGE_FINGERPRINT' in main and 'currentSceneVisualFingerprint' in main,
 'imagem antiga é invalidada':'invalidateSceneImageIfContextChanged' in main,
 'retorno Gemini obsoleto é rejeitado':'A cena mudou depois que o Gemini foi aberto' in main,
 'prompt visual inclui NPCs':'currentSceneCastPreview' in main[main.find('private String buildGeminiImagePrompt'):main.find('private void editVisualPrompt')],
 'imagem decodifica fora da UI':'private void displaySceneImage(final Uri uri)' in main and 'decodeSampled(MainActivity.this' in main and 'AppExecutors.io().execute(new Runnable(){public void run(){try{final android.graphics.Bitmap bitmap' in main,
 'Charon inicia sem varredura prévia e trabalha fora da UI':'continueCharonPlayback' in main and 'countUncachedTts(chunks)' not in main and 'AppExecutors.io().execute' in main,
 'SFX primeiro bloco fora da UI':'Nenhum trecho é analisado na thread da interface' in audio,
 'PLAY não chama stopService':'private String startAudioServiceSafely(){' in main and 'return "";' in main[main.find('private String startAudioServiceSafely'):main.find('private String geminiKey')] and 'try{stopService' not in main[main.find('private String startAudioServiceSafely'):main.find('private String geminiKey')],
 'timeout Android não está duplicado':main.count('final long timeout=Math.max(20000L,Math.min(100000L,sentence.length()*100L+10000L));')==1,
}
failed=[]
for k,v in checks.items():
 print(('OK  ' if v else 'FALHA  ')+k)
 if not v: failed.append(k)
print(f'RESULTADO CENA 4.9.3: {len(checks)-len(failed)}/{len(checks)}')
if failed: sys.exit(1)
