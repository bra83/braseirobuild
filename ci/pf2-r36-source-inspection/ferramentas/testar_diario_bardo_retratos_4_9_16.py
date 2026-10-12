from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
composer=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/BardChronicleComposer.java').read_text(encoding='utf-8')
exporter=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/AdventurePdfExporter.java').read_text(encoding='utf-8')
version=(root/'version.properties').read_text(encoding='utf-8')
checks={
    'versao 4.9.16+':(int(dict(line.split('=',1) for line in version.splitlines() if '=' in line).get('VERSION_CODE','0'))>=490916),
    'botao crônica de bardo':'EXPORTAR COMO CRÔNICA DE BARDO (PDF)' in main,
    'exportador literal preservado':'EXPORTAR AVENTURA COMO LIVRO PDF' in main,
    'dialogo do bardo':'private void bardChronicleDialog()' in main,
    'nome do bardo configuravel':'Nome do bardo' in main,
    'tom configuravel':'Épico e caloroso' in main and 'Sombrio e dramático' in main,
    'ia opcional uma chamada':'Lapidar a prosa com Gemini quando disponível (1 chamada)' in main,
    'fallback local':'BardChronicleComposer.localManuscript' in main,
    'limite protege campanha longa':'BardChronicleComposer.aiSourceFits(prompt)' in main,
    'gemini single call':'BRAS_SINGLE_CALL' in composer,
    'contrato nao inventa fatos':'Não invente combates, tesouros, falas, mortes, vitórias' in composer,
    'ordem cronologica preservada':'Collections.reverse(entries)' in composer,
    'pdf de bardo dedicado':'exportBardChronicle' in exporter,
    'pdf possui capa':'Crônica de Bardo' in exporter and 'Como cantada por ' in exporter,
    'auditoria distingue bardo':'Crônica de bardo exportada' in main,
    'heroi continua kind aventureiro':'"AVENTUREIRO",hero.optString("token_uri","")' in main,
    'rotulo usa especie':'heroSpeciesLabel(hero)' in main and 'sceneCastVisibleRole(entry)' in main and 'entry.optString("hint","")' in main,
    'npc continua npc':'addSceneCastEntry(out,seen,name,"NPC"' in main,
    'token cena sem recuo interno':'circle.setPadding(dp(3)' not in main[main.find('private View sceneTokenTile'):main.find('private void renderSceneCast') if main.find('private void renderSceneCast')>0 else main.find('private void addOptionButton')],
    'token geral sem padding':'view.setPadding(dp(3),dp(3),dp(3),dp(3))' not in main[main.find('private ImageView createAvatarView'):main.find('private Uri storeCharacterAvatar')],
    'borda sobre imagem':'view.setForeground(ring)' in main and 'circle.setForeground(ring)' in main,
    'center crop preservado':'ImageView.ScaleType.CENTER_CROP' in main,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('OK' if v else 'FALHA')+' - '+k)
if failed: raise SystemExit('Falhas: '+', '.join(failed))
print(f'OK — {len(checks)}/{len(checks)} verificações do Diário de bardo e retratos 4.9.16.')
