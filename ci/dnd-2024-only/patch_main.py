#!/usr/bin/env python3
"""Minimal, fail-closed 2024 new-character UI patch. No Mystara runtime edits."""
from pathlib import Path
from re import subn
p=Path("dnd-normal/run247-mod005/app/src/main/java/com/braseiro/dnd5e/MainActivity.java")
with p.open("r",encoding="utf-8",newline="") as f:
    s=f.read()
def one(before,after,label):
    global s
    n=s.count(before)
    if n!=1:
        raise SystemExit("PATCH_PRECONDITION_FAILED "+label+" matches="+str(n))
    s=s.replace(before,after,1)
one(
    'final Spinner profile=spinner(new String[]{DndRulesEngine.PROFILE_2014,DndRulesEngine.PROFILE_2024,DndRulesEngine.PROFILE_HYBRID},selectedRulesProfile);',
    'final Spinner profile=spinner(isMystaraWorld()?new String[]{DndRulesEngine.PROFILE_2014,DndRulesEngine.PROFILE_2024,DndRulesEngine.PROFILE_HYBRID}:new String[]{DndRulesEngine.PROFILE_2024},isMystaraWorld()?selectedRulesProfile:DndRulesEngine.PROFILE_2024);if(!isMystaraWorld())profile.setEnabled(false);',
    "campaign 2024-only spinner"
)
one(
    'profileCard.addView(text("Escolha 5e 2014, 5.5e revisada ou Híbrido compatível. O perfil híbrido continua disponível e nunca será convertido silenciosamente durante migração, restauração ou salvamento.",12,MUTED,false));',
    'profileCard.addView(text(isMystaraWorld()?"Escolha o perfil de regras da crônica.":"Novas campanhas e novos personagens usam exclusivamente D&D 2024. Campanhas antigas são preservadas sem conversão silenciosa.",12,MUTED,false));if(!isMystaraWorld()&&!DndRulesEngine.PROFILE_2024.equals(selectedRulesProfile))profileCard.addView(text("Esta campanha foi criada com regras antigas; a gravação de outras configurações não mudará seu perfil original. Inicie uma campanha 2024 para criar personagens sob o novo perfil.",12,MUTED,false));',
    "campaign rules caption"
)
one(
    'db.saveCampaign(name.getText().toString().trim(),profile.getSelectedItem().toString(),tone.getSelectedItem().toString()',
    'db.saveCampaign(name.getText().toString().trim(),(!isMystaraWorld()&&!DndRulesEngine.PROFILE_2024.equals(selectedRulesProfile))?selectedRulesProfile:profile.getSelectedItem().toString(),tone.getSelectedItem().toString()',
    "keep legacy campaign profile on settings save"
)
one(
    'String profile=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2024);JSONObject seed=characterGenerator.randomComplete(profile,speciesForProfile(profile),db.listCharacters());',
    'String profile=DndRulesEngine.PROFILE_2024;JSONObject seed=characterGenerator.randomComplete(profile,speciesForProfile(profile),db.listCharacters());',
    "new random hero 2024"
)
one(
    'startCharacterImport(null,db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2024));',
    'startCharacterImport(null,DndRulesEngine.PROFILE_2024);',
    "new sheet import expects 2024"
)
old=('final String selectedCampaignRules=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2024);'
     'final String rulesProfile=edit?characterEditingProfile(existing):'
     '((importedRules.contains("2024")||importedRules.startsWith("5.5"))?importedRules:'
     '((isStrixhavenAdventure()&&!rulesEngine.is2024(selectedCampaignRules))?DndRulesEngine.PROFILE_2014:selectedCampaignRules));')
new=('if(!edit&&!importedRules.trim().isEmpty()&&!rulesEngine.is2024(importedRules))'
     '{toast("Importação recusada: novas fichas exigem D&D 2024. A ficha original foi preservada.");return;}'
     'final String rulesProfile=edit?characterEditingProfile(existing):DndRulesEngine.PROFILE_2024;')
one(old,new,"character editor new-vs-legacy profile")
with p.open("w",encoding="utf-8",newline="") as f:
    f.write(s)
print("DND_NEW_2024_ONLY_UI_PATCHED PASS; unchanged Mystara code paths")
