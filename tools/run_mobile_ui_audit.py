from pathlib import Path
from playwright.sync_api import sync_playwright
import http.server
import threading
import socketserver
import time

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory='ose/1.0.0/source', **kwargs)
    def log_message(self, format, *args):
        pass

httpd = socketserver.TCPServer(('127.0.0.1', 8765), Handler)
t = threading.Thread(target=httpd.serve_forever, daemon=True)
t.start()

output = Path('qa/ose')
output.mkdir(parents=True, exist_ok=True)

with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 415, 'height': 915}, device_scale_factor=1)
    errors = []
    page.on('pageerror', lambda err: errors.append(str(err)))
    page.goto('http://127.0.0.1:8765/web-ui-p1/index.html?screen=session&state=active', wait_until='load')
    nav = page.locator('nav.bottom')
    assert nav.count() == 1, 'bottom navigation missing'
    box = nav.bounding_box()
    assert box and abs(box['height'] - 56) <= 2, f'nav height not 56px: {box}'
    assert abs(box['y'] + box['height'] - 915) <= 2, f'nav not bottom aligned: {box}'
    dimensions = page.evaluate('''() => ({
        scroll: document.documentElement.scrollWidth,
        screen: window.innerWidth,
        header: document.querySelector('header').getBoundingClientRect().height
    })''')
    assert dimensions['scroll'] <= dimensions['screen'] + 1, f'horizontal overflow: {dimensions}'
    assert dimensions['header'] < 145, f'header excessively tall: {dimensions}'
    page.screenshot(path=str(output / 'a57-session.png'), full_page=False)

    # Character screen screenshot
    page.goto('http://127.0.0.1:8765/web-ui-p1/index.html?screen=character', wait_until='load')
    page.screenshot(path=str(output / 'a57-character.png'), full_page=False)

    # Inventory screen screenshot
    page.goto('http://127.0.0.1:8765/web-ui-p1/index.html?screen=inventory', wait_until='load')
    page.screenshot(path=str(output / 'a57-inventory.png'), full_page=False)

    # Back to session
    page.goto('http://127.0.0.1:8765/web-ui-p1/index.html?screen=session&state=active', wait_until='load')
    page.evaluate('''() => {
        const img = document.createElement('img');
        img.id = 'qa-icon';
        img.className = 'equipment-art';
        img.src = 'equipment-icons/CLASSIC_EQUIP_GEAR_06.png';
        document.body.appendChild(img);
        window.OSEEquipmentArt.refresh(document.body);
    }''')
    page.wait_for_function("() => document.querySelector('#qa-icon')?.dataset.equipmentCleaned === 'yes'", timeout=10000)
    pixels = page.evaluate('''() => {
        const img = document.getElementById('qa-icon');
        const canvas = document.createElement('canvas');
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        const data = ctx.getImageData(0, 0, canvas.width, canvas.height).data;
        let transparent = 0;
        for (let i = 3; i < data.length; i += 4) if (data[i] === 0) transparent++;
        return {transparent, total: data.length / 4};
    }''')
    assert pixels['transparent'] > 0, f'processed icon not transparent: {pixels}'
    page.locator('#qa-icon').evaluate('(img) => img.remove()')

    # Rolling dice animation capture
    result = page.evaluate('''() => window.BraseiroDice.start({
        terms: [{count: 1, sides: 4, sign: 1}, {count: 1, sides: 20, sign: 1}, {count: 1, sides: 100, sign: 1}],
        rolls: [3, 19, 82],
        canonicalTotal: 104,
        modifier: 0
    })''')
    assert result['ok'] and result['total'] == 104 and result['visualMode'] == 'physics', result
    time.sleep(0.5)
    page.screenshot(path=str(output / 'a57-dice-rolling.png'), full_page=False)

    page.wait_for_function("() => document.querySelector('#status')?.textContent.includes('Simulação concluída')", timeout=15000)
    canvas = page.locator('.ose-physics-canvas')
    assert canvas.count() == 1, 'physical renderer did not attach'
    canvasbox = canvas.bounding_box()
    assert canvasbox and canvasbox['height'] >= 235, f'physics canvas too small: {canvasbox}'
    page.screenshot(path=str(output / 'a57-physical-dice-settled.png'), full_page=False)

    page.locator('#close').click()
    assert page.locator('.ose-physics-canvas').count() == 0, 'animation not disposed on close'

    # Compact 360x800
    page.set_viewport_size({'width': 360, 'height': 800})
    page.wait_for_timeout(100)
    box = nav.bounding_box()
    assert box and abs(box['height'] - 56) <= 2 and abs(box['y'] + box['height'] - 800) <= 2, box
    assert not errors, f'uncaught JavaScript errors: {errors}'
    page.screenshot(path=str(output / 'compact-360.png'), full_page=False)

    browser.close()

httpd.shutdown()
print('PASS Chromium mobile responsive nav, header, pixels, physical dice and cleanup')
