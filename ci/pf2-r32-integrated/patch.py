from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()
main = root / "android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java"
version = root / "version.properties"
text = main.read_text(encoding="utf-8")

anchor1 = '''final CheckBox active=check("Personagem em jogo",base.optInt("active",1)==1);form.addView(active);'''
insert1 = '''final CheckBox active=check("Personagem em jogo",base.optInt("active",1)==1);form.addView(active);
        final JSONObject existingCharacterControl=edit?db.getCharacterControl(base.optLong("id")):new JSONObject();
        final Spinner characterControl=spinner(new String[]{"Jogador — eu controlo","Mestre — Barbara controla"},controlLabel(existingCharacterControl.optString("control_mode","player")));form.addView(cardWithLabel("QUEM CONTROLA ESTE PERSONAGEM?",characterControl));
        form.addView(text("Solo significa 1 jogador humano, não 1 personagem. Em JOGADOR, você decide tudo deste herói. Em MESTRE, Barbara decide intenção, fala e ações voluntárias, mas testes, PV, condições, recursos, inventário, magias e demais efeitos continuam obrigatoriamente no mesmo motor PF2e.",11,MUTED,false));'''
if anchor1 not in text:
    raise SystemExit("anchor control UI not found")
text = text.replace(anchor1, insert1, 1)

anchor2 = '''db.updateCharacterSheet(id,sheet.toString());db.addAuditEvent("ficha",edit?"Ficha PF2e atualizada":"Personagem PF2e criado",nm,edit?existing.toString():"{}",sheet.toString(),true);'''
insert2 = '''db.updateCharacterSheet(id,sheet.toString());String previousTactics=edit?db.getCharacterControl(id).optString("tactics_json","{}"):"{}";db.saveCharacterControl(id,controlMode(String.valueOf(characterControl.getSelectedItem())),previousTactics);db.addAuditEvent("ficha",edit?"Ficha PF2e atualizada":"Personagem PF2e criado",nm,edit?existing.toString():"{}",sheet.toString(),true);'''
if anchor2 not in text:
    raise SystemExit("anchor control persistence not found")
text = text.replace(anchor2, insert2, 1)

main.write_text(text, encoding="utf-8")

vp = version.read_text(encoding="utf-8")
vp = vp.replace("VERSION_NAME=9.0.31", "VERSION_NAME=9.0.33")
vp = vp.replace("VERSION_CODE=90031", "VERSION_CODE=90033")
version.write_text(vp, encoding="utf-8")

required = [
    'HumanGmNarrativeProfile.prompt()',
    'new KingmakerTurnGuide(db,kingmakerRepository,kingmakerRules)',
    'ⓘ GUIA COMPLETO DO TURNO',
    'saveCharacterControl(id,controlMode(String.valueOf(characterControl.getSelectedItem()))',
    'Mestre — Barbara controla',
    'CONTROLE • MESTRE / BARBARA',
]
final = main.read_text(encoding="utf-8")
for token in required:
    if token not in final and token != 'HumanGmNarrativeProfile.prompt()':
        raise SystemExit(f"required token missing: {token}")

director = (root / "android/app/src/main/java/com/braseiro/pathfinder2e/BraseiroNarrativeDirector.java").read_text(encoding="utf-8")
if 'HumanGmNarrativeProfile.prompt()' not in director:
    raise SystemExit("canonical narrator profile is not wired into BraseiroNarrativeDirector")
guide = root / "android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerTurnGuide.java"
if not guide.exists():
    raise SystemExit("KingmakerTurnGuide missing")
g = guide.read_text(encoding="utf-8")
for token in ["SEQUÊNCIA COMPLETA","OPÇÕES VÁLIDAS NESTE MOMENTO","configuration(state, control)","rules.turnProcedureGuide()","rules.activityGuideCatalog()"]:
    if token not in g:
        raise SystemExit(f"Kingmaker guide incomplete: {token}")
party = (root / "android/app/src/main/java/com/braseiro/pathfinder2e/PartyDirector.java").read_text(encoding="utf-8")
for token in ['"mode","solo_multi_character"','"human_players",1','"gm_controlled"','"mechanics_rule"']:
    if token not in party:
        raise SystemExit(f"party control integration missing: {token}")

print("PF2 R32 integrated patch applied successfully")
