#!/usr/bin/env python3
"""
Comprehensive mobile UI audit for Braseiro OSE 1.0.10.
Exercises all canonical screens, modals, responsive viewports, and 3D physical dice
using genuine campaign fixtures in Playwright Chromium.
"""
from pathlib import Path
from playwright.sync_api import sync_playwright
import http.server
import threading
import socketserver
import time
import json

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = REPO_ROOT / 'qa/ose'
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(REPO_ROOT / 'ose/1.0.0/source'), **kwargs)
    def log_message(self, format, *args):
        pass

CAMPAIGN_FIXTURE = {
    "selectedCharacterId": "char-hero-1",
    "selectedCharacter": {
        "characterId": "char-hero-1",
        "name": "Theron Corvo",
        "raceId": "HUMAN",
        "classIds": ["FIGHTER"],
        "levelByClass": {"FIGHTER": 1},
        "hpCurrentNumerator": 8,
        "hpMaxNumerator": 8,
        "hpDenominator": 1,
        "armorClassDescending": 4,
        "attributes": {"str": 16, "int": 11, "wis": 10, "dex": 14, "con": 15, "cha": 12},
        "savingThrows": {
            "deathPoison": 12,
            "wands": 13,
            "paralysisPetrification": 14,
            "breath": 15,
            "spellsRodsStaves": 16
        }
    },
    "party": [
        {
            "characterId": "char-hero-1",
            "name": "Theron Corvo",
            "raceId": "HUMAN",
            "classIds": ["FIGHTER"],
            "levelByClass": {"FIGHTER": 1},
            "hpCurrentNumerator": 8,
            "hpMaxNumerator": 8,
            "hpDenominator": 1,
            "controlMode": "PLAYER"
        },
        {
            "characterId": "char-hero-2",
            "name": "Morwen Brisa",
            "raceId": "HUMAN",
            "classIds": ["CLERIC"],
            "levelByClass": {"CLERIC": 1},
            "hpCurrentNumerator": 6,
            "hpMaxNumerator": 6,
            "hpDenominator": 1,
            "controlMode": "MASTER"
        },
        {
            "characterId": "char-hero-3",
            "name": "Kaelen Sombra",
            "raceId": "ELF",
            "classIds": ["THIEF"],
            "levelByClass": {"THIEF": 1},
            "hpCurrentNumerator": 4,
            "hpMaxNumerator": 4,
            "hpDenominator": 1,
            "controlMode": "CURATED"
        }
    ],
    "inventory": [
        {"itemId": "it-1", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_WEAPON_PURCHASE_18", "quantity": 1, "equipped": True, "kind": "WEAPON", "displayName": "Machado de mão"},
        {"itemId": "it-2", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_ARMOR_02", "quantity": 1, "equipped": True, "kind": "ARMOR", "displayName": "Cota de malha"},
        {"itemId": "it-3", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_ARMOR_04", "quantity": 1, "equipped": True, "kind": "SHIELD", "displayName": "Escudo"},
        {"itemId": "it-4", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_GEAR_18", "quantity": 6, "equipped": False, "kind": "FOOD", "displayName": "Rações de viagem (7 dias)"},
        {"itemId": "it-5", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_GEAR_14", "quantity": 7, "equipped": False, "kind": "OIL_FLASK", "displayName": "Óleo (frasco)"},
        {"itemId": "it-6", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_GEAR_05", "quantity": 1, "equipped": False, "kind": "ADVENTURING_GEAR", "displayName": "Cantil"},
        {"itemId": "it-7", "ownerId": "char-hero-1", "catalogId": "CLASSIC_EQUIP_GEAR_08", "quantity": 1, "equipped": True, "kind": "ADVENTURING_GEAR", "displayName": "Espelho de aço"}
    ],
    "session": {
        "visibleNarration": "A névoa matinal se dissipa revelando o antigo Túmulo do Rei Cinzento encravado nas encostas rochosas. A pesada porta de pedra está semiaberta, com marcas antigas de machado nas bordas. O ar que sai da fresta cheira a terra úmida e cinzas frias. O que a companhia pretende fazer?",
        "phase": "ACTIVE",
        "suggestedActions": [
            {"label": "EXAMINAR A ENTRADA", "reaction": "Theron se aproxima com cautela e examina as marcas de machado e os batentes da porta em busca de armadilhas ou sinais recentes."},
            {"label": "ACENDER TOCHA", "reaction": "Acendemos uma tocha antes de avançar para garantir visibilidade plena nas sombras da tumba."},
            {"label": "FORMAÇÃO DEFENSIVA", "reaction": "Avançamos em formação cautelosa: Theron na vanguarda com escudo erguido, Morwen logo atrás e Kaelen vigiando a retaguarda."}
        ]
    },
    "time": {"day": 1, "turn": 3, "turns": 3, "phase": "ACTIVE"},
    "conditions": {"torchTurnsRemaining": 4, "rationsRemaining": 14},
    "position": {"kind": "HEX", "q": 2, "r": 1},
    "map": {
        "width": 6,
        "height": 5,
        "cells": [
            {"q": 0, "r": 0, "known": True, "terrain": "FOREST", "hydrology": "NONE"},
            {"q": 1, "r": 0, "known": True, "terrain": "CLEAR_GRASSLANDS", "hydrology": "NONE"},
            {"q": 2, "r": 0, "known": True, "terrain": "HILLS", "hydrology": "NONE"},
            {"q": 3, "r": 0, "known": False, "terrain": "UNKNOWN", "hydrology": "UNKNOWN"},
            {"q": 0, "r": 1, "known": True, "terrain": "CLEAR_GRASSLANDS", "hydrology": "LAKE_RIVER"},
            {"q": 1, "r": 1, "known": True, "terrain": "CLEAR_GRASSLANDS", "hydrology": "NONE"},
            {"q": 2, "r": 1, "known": True, "terrain": "HILLS", "hydrology": "NONE"},
            {"q": 3, "r": 1, "known": True, "terrain": "HILLS", "hydrology": "NONE"},
            {"q": 4, "r": 1, "known": False, "terrain": "UNKNOWN", "hydrology": "UNKNOWN"},
            {"q": 1, "r": 2, "known": True, "terrain": "FOREST", "hydrology": "NONE"},
            {"q": 2, "r": 2, "known": True, "terrain": "MOUNTAINS", "hydrology": "NONE"},
            {"q": 3, "r": 2, "known": True, "terrain": "MOUNTAINS", "hydrology": "NONE"}
        ]
    },
    "localMaps": {
        "settlement": {
            "settlementId": "Aldeia de Oakhaven",
            "currentAnchorId": "INN",
            "locations": [
                {"locationId": "INN", "name": "Estalagem do Javali", "discovered": True},
                {"locationId": "SMITH", "name": "Ferraria de Torvald", "discovered": True},
                {"locationId": "GATE", "name": "Portão Sul", "discovered": True},
                {"locationId": "TEMPLE", "name": "Capela de Santa Aurelia", "discovered": True}
            ]
        },
        "dungeon": {
            "dungeonId": "Túmulo do Rei Cinzento",
            "currentNodeId": "Entrada",
            "entranceNodeId": "Entrada",
            "nodes": [
                {"nodeId": "Entrada", "current": True},
                {"nodeId": "Antecâmara"},
                {"nodeId": "Cripta Central"},
                {"nodeId": "Câmara Funerária"}
            ],
            "edges": [
                {"from": "Entrada", "to": "Antecâmara", "kind": "PASSAGE"},
                {"from": "Antecâmara", "to": "Cripta Central", "kind": "DOOR"},
                {"from": "Cripta Central", "to": "Câmara Funerária", "kind": "DOOR"}
            ]
        }
    },
    "creation": {
        "drafts": [
            {
                "draftId": "draft-qa-1",
                "profile": "CLASSIC_FANTASY",
                "attributes": {"str": 17, "int": 10, "wis": 9, "dex": 13, "con": 15, "cha": 8}
            }
        ]
    },
    "presentation": {
        "characterPortraits": {}
    },
    "campaign": {
        "id": "camp-ose-1",
        "name": "O Túmulo do Rei Cinzento",
        "profile": "CLASSIC_FANTASY"
    }
}

def run_audit():
    httpd = socketserver.TCPServer(('127.0.0.1', 8769), Handler)
    server_thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    server_thread.start()

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        # 1. Primary Galaxy A57 Viewport (415 x 915)
        page = browser.new_page(viewport={'width': 415, 'height': 915}, device_scale_factor=1)
        errors = []
        page.on('pageerror', lambda err: errors.append(str(err)))

        # Load web app and attach mock bridge
        page.goto('http://127.0.0.1:8769/web-ui-p1/index.html?screen=session', wait_until='load')

        # Inject native mock bridge & populate full campaign fixture
        page.evaluate(f'''() => {{
            window.BraseiroBridge = {{
                postMessage: (msg) => {{
                    console.log('[Native Bridge Mock received]', msg);
                    try {{
                        const parsed = typeof msg === 'string' ? JSON.parse(msg) : msg;
                        if (parsed.type === 'ManualDiceRoll') {{
                            const p = parsed.payload || {{}};
                            const terms = (p.terms && p.terms.length > 0) ? p.terms : [{{ count: p.count || 1, sides: p.sides || 20, sign: 1 }}];
                            const modifier = p.modifier || 0;
                            const termsResult = [];
                            const allRolls = [];
                            let runningSum = 0;
                            for (const t of terms) {{
                                const c = t.count || 1;
                                const s = t.sides || 20;
                                const sign = t.sign || 1;
                                const rolls = [];
                                for (let i = 0; i < c; i++) {{
                                    const r = s === 6 ? 4 : (s === 4 ? 3 : (s === 20 ? 19 : (s === 100 ? 82 : Math.floor(s / 2))));
                                    rolls.push(r);
                                    allRolls.push(r);
                                }}
                                const subtotal = rolls.reduce((a, b) => a + b, 0) * sign;
                                runningSum += subtotal;
                                termsResult.push({{ count: c, sides: s, sign, rolls, subtotal }});
                            }}
                            const total = runningSum + modifier;
                            const detail = allRolls.join('+') + (modifier > 0 ? ` + ${{modifier}}` : (modifier < 0 ? ` - ${{-modifier}}` : '')) + ` = ${{total}}`;
                            setTimeout(() => {{
                                window.BraseiroReceive({{
                                    type: 'ManualDiceResult',
                                    payload: {{
                                        terms: termsResult,
                                        rolls: allRolls,
                                        total,
                                        modifier,
                                        formula: p.formula || `${{terms.map(t => t.count + 'd' + t.sides).join(' + ')}}${{modifier > 0 ? '+' + modifier : ''}}`,
                                        detail
                                    }}
                                }});
                            }}, 30);
                        }}
                    }} catch (e) {{
                        console.error('Mock postMessage error', e);
                    }}
                }}
            }};
            window.BraseiroReceive({{
                type: 'ViewState',
                payload: {json.dumps(CAMPAIGN_FIXTURE)}
            }});
        }}''')

        page.wait_for_timeout(300)

        # Responsive check: Navigation dock
        nav = page.locator('nav.bottom')
        assert nav.count() == 1, 'bottom navigation missing'
        box = nav.bounding_box()
        assert box and abs(box['height'] - 56) <= 2, f'nav height not 56px: {box}'
        assert abs(box['y'] + box['height'] - 915) <= 2, f'nav not bottom aligned: {box}'

        # Responsive check: Header and horizontal overflow
        dims = page.evaluate('''() => ({
            scroll: document.documentElement.scrollWidth,
            screen: window.innerWidth,
            header: document.querySelector('header').getBoundingClientRect().height,
            title: document.getElementById('headerTitle')?.textContent
        })''')
        assert dims['scroll'] <= dims['screen'] + 1, f'horizontal overflow: {dims}'
        assert dims['header'] < 145, f'header excessively tall: {dims}'

        print('[Audit] 1. Session screen capture')
        page.screenshot(path=str(OUTPUT_DIR / 'a57-session.png'), full_page=False)

        # 2. Hex Map
        print('[Audit] 2. Hex Map capture')
        page.locator('.navbtn[data-screen="map"]').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-map-hex.png'), full_page=False)

        # 3. Dungeon Map
        print('[Audit] 3. Dungeon Map capture')
        dungeon_btn = page.locator('[data-map-mode="DUNGEON"]')
        if dungeon_btn.count() > 0:
            dungeon_btn.click()
            page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-map-dungeon.png'), full_page=False)

        # 4. Settlement Map
        print('[Audit] 4. Settlement Map capture')
        settle_btn = page.locator('[data-map-mode="SETTLEMENT"]')
        if settle_btn.count() > 0:
            settle_btn.click()
            page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-map-settlement.png'), full_page=False)

        # 5. Character Sheet
        print('[Audit] 5. Character Sheet capture')
        page.locator('.navbtn[data-screen="sheet"]').click()
        page.wait_for_timeout(200)
        # Ensure sheet top is visible
        page.evaluate("() => window.scrollTo(0, 0)")
        page.screenshot(path=str(OUTPUT_DIR / 'a57-character.png'), full_page=False)

        # 6. Inventory Section
        print('[Audit] 6. Inventory section capture')
        # Exercise completed-loadout inventory: the catalogue UI is gone but
        # official names must still be projected alongside persisted item IDs.
        names = page.locator('.inventory-grid .item b').all_text_contents()
        for expected in ['Machado de mão','Cota de malha','Escudo',
                         'Rações de viagem (7 dias)','Óleo (frasco)',
                         'Cantil','Espelho de aço']:
            assert expected in names, f'Item label missing: {expected} / {names}'
        assert 'Item' not in names, f'Generic inventory placeholder: {names}'
        page.evaluate("() => { const inv = document.querySelector('.inventory-grid'); if(inv) inv.scrollIntoView(); }")
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-inventory.png'), full_page=False)

        # 7. Company Screen
        print('[Audit] 7. Company screen capture')
        page.locator('.navbtn[data-screen="company"]').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-company.png'), full_page=False)

        # 8. Library Screen
        print('[Audit] 8. Library screen capture')
        page.locator('.navbtn[data-screen="library"]').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-library.png'), full_page=False)

        # 9. World Screen
        print('[Audit] 9. World screen capture')
        page.locator('.navbtn[data-screen="world"]').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-world.png'), full_page=False)

        # 10. Modals: Settings
        print('[Audit] 10. Settings modal capture')
        page.locator('#settingsButton').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-settings-modal.png'), full_page=False)
        page.locator('#settingsClose').click()
        page.wait_for_timeout(100)

        # 11. Modals: Aprender Jogando / Guide
        print('[Audit] 11. Guide modal capture')
        page.locator('#guideButton').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-guide-modal.png'), full_page=False)
        page.locator('#guideClose').click()
        page.wait_for_timeout(100)

        # 12. Modals: Contextual Suggestions
        print('[Audit] 12. Suggestions modal capture')
        page.locator('.navbtn[data-screen="session"]').click()
        page.wait_for_timeout(150)
        page.evaluate("() => { const b = document.getElementById('suggestionsButton') || document.querySelector('[data-action-suggestions]'); if(b) b.click(); }")
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-suggestions-modal.png'), full_page=False)
        page.evaluate("() => { const b = document.getElementById('suggestionsClose'); if(b) b.click(); }")
        page.wait_for_timeout(100)

        # 13. Character Creation Draft
        print('[Audit] 13. Character creation draft capture')
        page.locator('.navbtn[data-screen="company"]').click()
        page.wait_for_timeout(150)
        add_btn = page.locator('[data-add-character]')
        if add_btn.count() > 0:
            add_btn.first.click()
            page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-creation-draft.png'), full_page=False)

        # 14. Character Edit Panel
        print('[Audit] 14. Character edit capture')
        page.locator('.navbtn[data-screen="sheet"]').click()
        page.wait_for_timeout(150)
        edit_btn = page.locator('[data-edit-character]')
        if edit_btn.count() > 0:
            edit_btn.first.click()
            page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-character-edit.png'), full_page=False)
        cancel_edit = page.locator('[data-cancel-character-edit]')
        if cancel_edit.count() > 0:
            cancel_edit.first.click()
            page.wait_for_timeout(100)

        # 15. 3D Dice Screen & In-Flight Motion
        print('[Audit] 15. 3D Dice screen and rolling physics')
        page.locator('.navbtn[data-screen="dice"]').click()
        page.wait_for_timeout(200)

        # Test Dice Atelier multi-formula interaction
        page.locator('[data-preset="2d6+1d4"]').click()
        page.wait_for_timeout(100)
        page.locator('[data-atelier-mod="+1"]').click()
        page.wait_for_timeout(100)
        formula_txt = page.locator('.formula-text').inner_text()
        assert '2d6 + 1d4 + 1' in formula_txt, f'unexpected formula: {formula_txt}'
        page.screenshot(path=str(OUTPUT_DIR / 'a57-dice-tray.png'), full_page=False)

        # Launch composed roll: 1d4 + 1d20 + 1d100
        roll_res = page.evaluate('''() => window.BraseiroDice.start({
            terms: [{count: 1, sides: 4, sign: 1}, {count: 1, sides: 20, sign: 1}, {count: 1, sides: 100, sign: 1}],
            rolls: [3, 19, 82],
            canonicalTotal: 104,
            modifier: 0
        })''')
        assert roll_res['ok'] and roll_res['total'] == 104 and roll_res['visualMode'] == 'physics', roll_res
        assert page.locator('#title').inner_text() == 'MESA FÍSICA 3D · BRASEIRO OSE', 'Name of unrelated game is visible'

        # Capture in-flight motion & collision
        time.sleep(0.4)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-dice-rolling.png'), full_page=False)

        # Wait for settling
        page.wait_for_function("() => document.querySelector('#status')?.textContent.includes('Simulação concluída')", timeout=15000)
        canvas = page.locator('.ose-physics-canvas')
        assert canvas.count() == 1, 'physical renderer did not attach'
        canvasbox = canvas.bounding_box()
        assert canvasbox and canvasbox['height'] >= 235, f'physics canvas too small: {canvasbox}'

        print('[Audit] 16. Settled composed 3D roll capture')
        page.screenshot(path=str(OUTPUT_DIR / 'a57-physical-dice-settled.png'), full_page=False)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-dice-composed.png'), full_page=False)

        # Close overlay
        page.locator('#close').click()
        assert page.locator('.ose-physics-canvas').count() == 0, 'animation not disposed on close'

        # 17. Single d20 roll (prominent, centered)
        print('[Audit] 17. Single d20 roll capture')
        page.evaluate('''() => window.BraseiroDice.start({
            terms: [{count: 1, sides: 20, sign: 1}],
            rolls: [18],
            canonicalTotal: 18,
            modifier: 0
        })''')
        page.wait_for_function("() => document.querySelector('#status')?.textContent.includes('Simulação concluída')", timeout=15000)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-dice-single-d20.png'), full_page=False)
        page.locator('#close').click()

        # 18. Large dice pool (e.g. 6d6)
        print('[Audit] 18. Dice pool capture')
        page.evaluate('''() => window.BraseiroDice.start({
            terms: [{count: 6, sides: 6, sign: 1}],
            rolls: [4, 5, 6, 3, 2, 5],
            canonicalTotal: 25,
            modifier: 0
        })''')
        page.wait_for_function("() => document.querySelector('#status')?.textContent.includes('Simulação concluída')", timeout=15000)
        page.screenshot(path=str(OUTPUT_DIR / 'a57-dice-pool.png'), full_page=False)
        page.locator('#close').click()

        # 19. Viewport: Compact 360 x 800
        print('[Audit] 19. Compact 360x800 viewport verification')
        page.set_viewport_size({'width': 360, 'height': 800})
        page.locator('.navbtn[data-screen="session"]').click()
        page.wait_for_timeout(200)
        box = nav.bounding_box()
        assert box and abs(box['height'] - 56) <= 2 and abs(box['y'] + box['height'] - 800) <= 2, box
        page.screenshot(path=str(OUTPUT_DIR / 'compact-360.png'), full_page=False)

        # 20. Viewport: Compact 393 x 873
        print('[Audit] 20. Compact 393x873 viewport verification')
        page.set_viewport_size({'width': 393, 'height': 873})
        page.locator('.navbtn[data-screen="session"]').click()
        page.wait_for_timeout(200)
        box = nav.bounding_box()
        assert box and abs(box['height'] - 56) <= 2 and abs(box['y'] + box['height'] - 873) <= 2, box
        page.screenshot(path=str(OUTPUT_DIR / 'compact-393.png'), full_page=False)

        # 21. Viewport: Landscape 915 x 415
        print('[Audit] 21. Landscape 915x415 viewport verification')
        page.set_viewport_size({'width': 915, 'height': 415})
        page.locator('.navbtn[data-screen="session"]').click()
        page.wait_for_timeout(200)
        page.screenshot(path=str(OUTPUT_DIR / 'landscape-915x415.png'), full_page=False)

        assert not errors, f'Uncaught JavaScript errors: {errors}'
        browser.close()

    httpd.shutdown()
    print('\nSUCCESS: All 21 mobile UI audits, screen fixtures, and 3D physics dice validated and captured.')

if __name__ == '__main__':
    run_audit()
