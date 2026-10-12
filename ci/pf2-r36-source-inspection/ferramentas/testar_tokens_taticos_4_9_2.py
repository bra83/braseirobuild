from pathlib import Path
import sys
root=Path(__file__).resolve().parents[1]
app=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text()
view=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/TacticalBoardView.java').read_text()
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text()
checks={
    'banco v51 ou superior': bool(__import__('re').search(r'DB_VERSION\s*=\s*(?:5[1-9]|[6-9][0-9])',app)) and 'oldVersion < 50' in app and 'oldVersion < 51' in app,
    'combatente guarda origem visual': all(x in app for x in ['source_kind TEXT DEFAULT', 'source_id INTEGER DEFAULT 0', 'token_uri TEXT DEFAULT']),
    'migração vincula heróis':'source_kind=\'character\'' in app and 'retratos legados dos heróis' in app,
    'migração vincula NPCs':'source_kind=\'npc\'' in app and 'retratos legados dos NPCs' in app,
    'criação resolve visual':'resolveCombatantVisualIdentity(name,player' in app and 'addCombatantWithVisual' in app,
    'token de herói sincroniza combate':'setCharacterToken' in app and 'source_kind=\'character\' AND source_id=?' in app,
    'token de NPC sincroniza combate':'setNpcToken' in app and 'source_kind=\'npc\' AND source_id=?' in app,
    'legado recupera token de resources':'tokenFromResources' in app,
    'nomes numerados usam criatura-base':'canonicalCombatantName' in app and '#?\\\\d+' in app,
    'biblioteca local é fallback':'findLocalTokenForScene(clean)' in app,
    'mesa recebe token_uri':'row.put("token_uri"' in app and 'combatant_token_uri' in app,
    'imagem é recortada sem deformar':'drawCenterCrop' in view and 'Rect source' in view,
    'iniciais apenas como fallback':'if(bitmap!=null)' in view and 'else{\n            drawInitials' in view,
    'cache é limpo ao atualizar':'public void refresh(){tokenCache.clear()' in view,
    'NPC de cena leva imagem ao combatente':'addCombatantWithVisualChecked(name,initiative' in main and 'npcToken=candidate.optString' in main,
    'inimigo do Mestre aceita token':'e.optString("token_uri","")' in main,
    'cartões mostram retrato':'createAvatarView(c.optString("token_uri",""),c.optString("name","Combatente"))' in main,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('OK  ' if v else 'FALHA  ')+k)
if failed:
    print(f'RESULTADO: {len(checks)-len(failed)}/{len(checks)}')
    sys.exit(1)
print(f'RESULTADO TOKENS TÁTICOS: {len(checks)}/{len(checks)} verificações aprovadas.')
