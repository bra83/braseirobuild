import fs from 'node:fs';
import { chromium } from 'playwright-core';

const candidates = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe'
];
const exe = candidates.find(p => fs.existsSync(p));
if (!exe) throw new Error('Chrome/Edge not found for real capture');

const browser = await chromium.launch({ headless: true, executablePath: exe });
const page = await browser.newPage({ viewport: { width: 415, height: 915 }, deviceScaleFactor: 1 });
const pageErrors = [];
const consoleErrors = [];
const failedResponses = [];

page.on('pageerror', error => pageErrors.push(String(error)));
page.on('console', msg => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
page.on('response', response => {
  if (response.status() >= 400 && /party-app-audit|\.tsx(?:\?|$)/.test(response.url())) {
    failedResponses.push(`${response.status()} ${response.url()}`);
  }
});

await page.goto('http://127.0.0.1:4178/party-app-audit.html', { waitUntil: 'networkidle' });

const root = page.locator('[data-braseiro-immersive-root="true"]');
try {
  await root.waitFor({ state: 'visible', timeout: 30000 });
} catch {
  const body = (await page.locator('body').innerText().catch(() => '')).slice(0, 4000);
  throw new Error(`ImmersiveJourneyView did not mount. pageErrors=${pageErrors.join(' | ')} consoleErrors=${consoleErrors.join(' | ')} failedResponses=${failedResponses.join(' | ')} body=${body}`);
}

const sheet = page.locator('[data-braseiro-chronicle-sheet="true"]');
if (await sheet.count()) await sheet.locator('button[aria-label="Fechar crônica"]').click();

const marker = page.locator('[data-party-map-marker="true"]');
const cluster = page.locator('[data-party-cluster="true"]');
const base = page.locator('[data-party-base="true"]');
const softShadow = page.locator('[data-party-soft-shadow="true"]');

await cluster.waitFor({ state: 'visible', timeout: 30000 });
await marker.waitFor({ state: 'attached', timeout: 30000 });
await base.waitFor({ state: 'visible', timeout: 30000 });
await softShadow.waitFor({ state: 'attached', timeout: 30000 });

const worldAnchored = await marker.getAttribute('data-party-world-anchored');
const currentHexId = await marker.getAttribute('data-party-current-hex');
if (worldAnchored !== 'true' || !currentHexId) throw new Error('Party is not attached to a canonical world-space hex');

const box0 = await cluster.boundingBox();
const base0 = await base.boundingBox();
if (!box0 || !base0 || box0.width < 20 || box0.height < 20) throw new Error('PartyCluster/base is not visibly rendered');

const overflow = await cluster.evaluate(el => getComputedStyle(el).overflow);
if (overflow !== 'visible') throw new Error('PartyCluster is clipping its own base');

const canvas = page.locator('canvas').first();
const cb = await canvas.boundingBox();
if (!cb) throw new Error('Map canvas missing');

const sx = cb.x + Math.min(cb.width - 80, Math.max(80, cb.width * 0.42));
const sy = cb.y + Math.min(cb.height - 150, Math.max(180, cb.height * 0.58));
await page.mouse.move(sx, sy);
await page.mouse.down();
await page.mouse.move(sx + 64, sy + 46, { steps: 8 });
await page.mouse.up();
await page.waitForTimeout(220);

const boxPan = await cluster.boundingBox();
if (!boxPan) throw new Error('PartyCluster disappeared after pan');
const panDx = boxPan.x - box0.x;
const panDy = boxPan.y - box0.y;
if (Math.abs(panDx) < 24 && Math.abs(panDy) < 24) {
  throw new Error(`Party stayed pinned to screen during map pan: dx=${panDx.toFixed(2)} dy=${panDy.toFixed(2)}`);
}

await page.mouse.move(boxPan.x + boxPan.width / 2, boxPan.y + boxPan.height / 2);
await page.mouse.wheel(0, -520);
await page.waitForTimeout(280);

const boxZoom = await cluster.boundingBox();
if (!boxZoom) throw new Error('PartyCluster disappeared after zoom');
const zoomRatio = boxZoom.width / boxPan.width;
if (zoomRatio < 1.08) throw new Error(`Party size stayed fixed while map zoomed: ratio=${zoomRatio.toFixed(3)}`);

const out = process.env.OUA_OUT + '/OUA_4.9.16_PARTY_WORLD_ANCHORED_415x915.png';
await page.screenshot({ path: out, fullPage: false, animations: 'disabled' });

fs.writeFileSync(
  process.env.OUA_OUT + '/OUA_4.9.16_PARTY_WORLD_ANCHORED_CAPTURE.json',
  JSON.stringify({
    engine: 'Chromium/Edge via Playwright',
    viewport: '415x915',
    actualComponent: 'ImmersiveJourneyView + EriadorMap + PartyCluster',
    currentHexId, worldAnchored, initialBox: box0, afterPanBox: boxPan, panDx, panDy,
    afterZoomBox: boxZoom, zoomRatio, baseBox: base0, clusterOverflow: overflow,
    pageErrors, consoleErrors, failedResponses
  }, null, 2)
);

if (pageErrors.length || failedResponses.length) {
  throw new Error(`Runtime errors: pageErrors=${pageErrors.join(' | ')} failedResponses=${failedResponses.join(' | ')}`);
}
await browser.close();
