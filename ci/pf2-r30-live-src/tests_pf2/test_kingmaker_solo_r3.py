from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
def txt(n): return (SRC/n).read_text(encoding='utf-8')
def test_campaign_flow_is_clickable():
    s=txt('MainActivity.java')
    for token in ['PRÓXIMOS PASSOS','campaign_flow','runKingmakerSoloStep(step)','"camp".equals(key)','"war".equals(key)','"event".equals(key)','"companions".equals(key)','"explore".equals(key)','"kingdom_turn".equals(key)','"kingdom_transition".equals(key)']:
        assert token in s
def test_solo_routing_stays_on_existing_surfaces():
    s=txt('MainActivity.java')
    for token in ['showKingmakerCampsDialog()','showKingmakerArmiesDialog()','showKingmakerEventsDialog()','showKingmakerCompanionsDialog()','navigate("Mapa")','showStartKingdomTurnDialog()','beginAdventure(false)']:
        assert token in s
def test_locked_authorities_preserved():
    props=(ROOT/'version.properties').read_text(encoding='utf-8')
    service=txt('KingmakerService.java'); solo=txt('KingmakerSoloDirector.java')
    assert 'VERSION_NAME=9.0.' in props and 'BACKUP_FORMAT=22' in props
    assert 'KingmakerRulesEngine' in service and 'Pathfinder2eRulesEngine' not in solo
    assert 'getWritableDatabase' not in solo
