from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
checks={
    'card individual separado':'TOKEN INDIVIDUAL DO CELULAR' in main,
    'pasta separada':'BIBLIOTECA PNG/JPG EM PASTA' in main and 'IMPORTAR PASTA DE TOKENS' in main,
    'individual nao usa arvore': 'chooseIndividualDeviceToken()' in main,
    'abre seletor de imagem': 'openFile("image/*",REQ_TOKEN,false)' in main,
    'escolhe alvo quando ausente':'Vincular token individual' in main and 'HERÓI • ' in main and 'NPC • ' in main,
    'usa personagens reais':'db.listCharacters()' in main,
    'usa npcs persistidos':'db.listNpcMemories(5000)' in main,
    'ignora npc arquivado':'npc.optInt("active",1)!=1' in main,
    'destino hero exclusivo':'pendingTokenNpcId="";pendingTokenCharacterId=String.valueOf(ids.get(which))' in main,
    'destino npc exclusivo':'pendingTokenCharacterId="";pendingTokenNpcId=String.valueOf(ids.get(which))' in main,
    'salvamento hero preservado':'db.setCharacterToken(characterId,stored.toString())' in main,
    'salvamento npc preservado':'db.setNpcToken(npcId,stored.toString())' in main,
    'copia segura hero':'importSelectedTokenAsync(data.getData())' in main and 'storeCharacterAvatar(source,characterId)' in main and 'AppExecutors.io().execute' in main,
    'copia segura npc':'importSelectedTokenAsync(data.getData())' in main and 'storeNpcAvatar(source,npcId)' in main and 'AppExecutors.io().execute' in main,
    'pasta ainda usa ACTION_OPEN_DOCUMENT_TREE':'selectTokenImageFolder(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)' in main,
    'individual usa ACTION_OPEN_DOCUMENT':'private void openFile(String type,int req,boolean multiple){Intent x=new Intent(Intent.ACTION_OPEN_DOCUMENT)' in main,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('OK' if v else 'FALHA')+' - '+k)
if failed: raise SystemExit('Falhas: '+', '.join(failed))
print(f'OK — {len(checks)}/{len(checks)} verificações do token individual 4.9.15.')
