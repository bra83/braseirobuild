from pathlib import Path
import shutil
import sys

project = Path(sys.argv[1]).resolve()
repo = Path(__file__).resolve().parents[2]
live = repo / "ci/pf2-r30-live-src/android/app/src/main/java/com/braseiro/pathfinder2e"
dst = project / "android/app/src/main/java/com/braseiro/pathfinder2e"

# Port only the reviewed requested-feature diffs from the exact R30-derived snapshot.
for name in [
    "MainActivity.java",
    "AppDb.java",
    "PartyDirector.java",
    "BraseiroNarrativeDirector.java",
    "KingmakerRulesEngine.java",
]:
    src = live / name
    if not src.exists():
        raise SystemExit(f"reviewed source missing: {src}")
    shutil.copy2(src, dst / name)

# New integration classes: didactic Kingdom guide + user's canonical human GM profile.
for name in ["KingmakerTurnGuide.java", "HumanGmNarrativeProfile.java"]:
    src = live / name
    if not src.exists():
        raise SystemExit(f"new integration source missing: {src}")
    shutil.copy2(src, dst / name)

# MasterOrchestrator: port ONLY per-character control routing.
# Do not port unrelated experimental narrative-profile code.
master_path = dst / "MasterOrchestrator.java"
master = master_path.read_text(encoding="utf-8")

old1 = '''boolean narrative="Narrador".equals(mode);boolean explicitAction="PLAYER_ACTION".equals(inputChannel);boolean explicitHelp="GM_HELP".equals(inputChannel);BraseiroNarrativeDirector.Intent routedIntent=BraseiroNarrativeDirector.classify(rawAction);boolean nonAdvancing=narrative&&(explicitHelp||(!explicitAction&&BraseiroNarrativeDirector.isNonAdvancing(routedIntent)));boolean queued=narrative&&!nonAdvancing&&party.hasQueuedActions();String action=queued?party.buildActionBatch(rawAction):(rawAction==null?"":rawAction.trim());boolean opening=RuleRetrievalEngine.isNarrativeOpeningRequest(action);JSONObject tick=narrative&&!nonAdvancing?world.preview(action):new JSONObject();'''
new1 = '''boolean narrative="Narrador".equals(mode);boolean explicitAction="PLAYER_ACTION".equals(inputChannel);boolean explicitHelp="GM_HELP".equals(inputChannel);BraseiroNarrativeDirector.Intent routedIntent=BraseiroNarrativeDirector.classify(rawAction);boolean nonAdvancing=narrative&&(explicitHelp||(!explicitAction&&BraseiroNarrativeDirector.isNonAdvancing(routedIntent)));boolean queued=narrative&&!nonAdvancing&&party.hasQueuedActions();boolean gmDirected=narrative&&!nonAdvancing&&party.hasGmControlledHeroes();String action=(queued||gmDirected)?party.buildActionBatch(rawAction):(rawAction==null?"":rawAction.trim());boolean opening=RuleRetrievalEngine.isNarrativeOpeningRequest(action);JSONObject tick=narrative&&!nonAdvancing?world.preview(action):new JSONObject();'''
if old1 not in master:
    raise SystemExit("MasterOrchestrator control-routing anchor missing")
master = master.replace(old1, new1, 1)

old2 = '''JSONObject compactSceneState=compactScene(db.getSceneState());JSONObject compactPartyState=party.compactStateForPrompt(quota.economyMode()?3300:4400);JSONObject compactWorldState=compactWorld();'''
new2 = '''JSONObject compactSceneState=compactScene(db.getSceneState());JSONObject compactPartyState=party.compactStateForPrompt(quota.economyMode()?3300:4400);JSONObject compactWorldState=compactWorld();root.put("party_control_policy",party.controlPolicyForPrompt());'''
if old2 not in master:
    raise SystemExit("MasterOrchestrator policy anchor missing")
master = master.replace(old2, new2, 1)
master_path.write_text(master, encoding="utf-8")

# Add control choice directly to create/edit character, not only after creation.
main_path = dst / "MainActivity.java"
main = main_path.read_text(encoding="utf-8")
anchor_ui = '''final CheckBox active=check("Personagem em jogo",base.optInt("active",1)==1);form.addView(active);'''
ui = '''final CheckBox active=check("Personagem em jogo",base.optInt("active",1)==1);form.addView(active);
        final JSONObject existingCharacterControl=edit?db.getCharacterControl(base.optLong("id")):new JSONObject();
        final Spinner characterControl=spinner(new String[]{"Jogador — eu controlo","Mestre — Barbara controla"},controlLabel(existingCharacterControl.optString("control_mode","player")));form.addView(cardWithLabel("QUEM CONTROLA ESTE PERSONAGEM?",characterControl));
        form.addView(text("Solo significa 1 jogador humano, não 1 personagem. Em JOGADOR, você decide tudo deste herói. Em MESTRE, Barbara decide intenção, fala e ações voluntárias, mas testes, PV, condições, recursos, inventário, magias e demais efeitos continuam obrigatoriamente no mesmo motor PF2e.",11,MUTED,false));'''
if anchor_ui not in main:
    raise SystemExit("character creation UI anchor missing")
main = main.replace(anchor_ui, ui, 1)

anchor_save = '''db.updateCharacterSheet(id,sheet.toString());db.addAuditEvent("ficha",edit?"Ficha PF2e atualizada":"Personagem PF2e criado",nm,edit?existing.toString():"{}",sheet.toString(),true);'''
save = '''db.updateCharacterSheet(id,sheet.toString());String previousTactics=edit?db.getCharacterControl(id).optString("tactics_json","{}"):"{}";db.saveCharacterControl(id,controlMode(String.valueOf(characterControl.getSelectedItem())),previousTactics);db.addAuditEvent("ficha",edit?"Ficha PF2e atualizada":"Personagem PF2e criado",nm,edit?existing.toString():"{}",sheet.toString(),true);'''
if anchor_save not in main:
    raise SystemExit("character creation persistence anchor missing")
main = main.replace(anchor_save, save, 1)
main_path.write_text(main, encoding="utf-8")

# Version only; package/database identities remain unchanged.
vp = project / "version.properties"
version = vp.read_text(encoding="utf-8")
version = version.replace("VERSION_NAME=9.0.31", "VERSION_NAME=9.0.33")
version = version.replace("VERSION_CODE=90031", "VERSION_CODE=90033")
vp.write_text(version, encoding="utf-8")

# Fail closed if any requested integration is missing.
files = {
    "main": main_path.read_text(encoding="utf-8"),
    "appdb": (dst/"AppDb.java").read_text(encoding="utf-8"),
    "party": (dst/"PartyDirector.java").read_text(encoding="utf-8"),
    "master": master_path.read_text(encoding="utf-8"),
    "director": (dst/"BraseiroNarrativeDirector.java").read_text(encoding="utf-8"),
    "profile": (dst/"HumanGmNarrativeProfile.java").read_text(encoding="utf-8"),
    "guide": (dst/"KingmakerTurnGuide.java").read_text(encoding="utf-8"),
    "rules": (dst/"KingmakerRulesEngine.java").read_text(encoding="utf-8"),
}

checks = {
    "character-control-create": 'QUEM CONTROLA ESTE PERSONAGEM?' in files["main"],
    "character-control-toggle": 'CONTROLE • MESTRE / BARBARA' in files["main"] and 'PASSAR AO MESTRE' in files["main"],
    "character-control-persist": 'saveCharacterControl(id,controlMode(String.valueOf(characterControl.getSelectedItem()))' in files["main"],
    "control-normalization": 'normalizeCharacterControlMode' in files["appdb"],
    "solo-multi-character": '"mode","solo_multi_character"' in files["party"] and '"human_players",1' in files["party"],
    "gm-action-routing": 'hasGmControlledHeroes()' in files["party"] and 'party.hasGmControlledHeroes()' in files["master"],
    "mechanics-authority": '"mechanics_rule"' in files["party"] and 'party_control_policy' in files["master"],
    "kingdom-guide-ui": 'ⓘ GUIA COMPLETO DO TURNO' in files["main"] and 'showKingdomTurnGuideDialog' in files["main"],
    "kingdom-guide-engine": 'rules.turnProcedureGuide()' in files["guide"] and 'rules.activityGuideCatalog()' in files["guide"] and 'repository.activeTurn()' in files["guide"],
    "kingdom-guide-content": 'SEQUÊNCIA COMPLETA' in files["guide"] and 'OPÇÕES VÁLIDAS NESTE MOMENTO' in files["guide"] and 'CONFIGURAÇÃO QUE EXIGE SUA ATENÇÃO' in files["guide"],
    "rules-guide-metadata": 'turnProcedureGuide()' in files["rules"] and 'activityGuideCatalog()' in files["rules"],
    "human-gm-profile": 'PERFIL TÉCNICO CANÔNICO' in files["profile"] and 'HumanGmNarrativeProfile.prompt()' in files["director"],
    "narrative-only-authority": 'governa SOMENTE apresentação narrativa' in files["director"] and 'regras PF2e/Kingmaker e decisões do jogador sempre têm precedência' in files["director"],
    "no-experimental-human-master-profile": 'HumanMasterNarrativeProfile' not in files["master"],
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items():
    print(("PASS" if v else "FAIL"), "-", k)
if failed:
    raise SystemExit("integration gate failed: "+", ".join(failed))
print("R32 requested-feature integration applied to exact post-Antigravity R30")
