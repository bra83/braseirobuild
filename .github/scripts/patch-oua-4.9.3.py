from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-oua-4.9.3.py <project-root>')
root = Path(sys.argv[1]).resolve()

def replace(path: str, old: str, new: str, count: int = 1):
    p = root / path
    s = p.read_text(encoding='utf-8')
    n = s.count(old)
    if n != count:
        raise SystemExit(f'{path}: expected {count} occurrence(s), found {n}')
    p.write_text(s.replace(old, new, count), encoding='utf-8')

replace('lib/version.ts', "export const APP_VERSION = '4.9.2';\nexport const APP_VERSION_CODE = 49002;", "export const APP_VERSION = '4.9.3';\nexport const APP_VERSION_CODE = 49003;")
replace('package.json', '"version": "4.9.2"', '"version": "4.9.3"')
replace('android/app/build.gradle.kts', 'versionCode = 49002\n        versionName = "4.9.2"', 'versionCode = 49003\n        versionName = "4.9.3"')
replace('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt', 'index.html?v=49002&immersive=1', 'index.html?v=49003&immersive=1', 2)

view = 'components/immersive/ImmersiveJourneyView.tsx'
replace(view,
    'return <section className="relative h-[100dvh] w-full overflow-hidden bg-transparent font-serif text-[#ead9b7]">\n    <div className="absolute inset-0 z-0 bg-[#100d09]">',
    'return <section className="relative w-full overflow-hidden font-serif" style={{ height: \'100dvh\', minHeight: \'100dvh\', background: \'#100d09\', color: \'#ead9b7\' }} data-braseiro-immersive-root="true">\n    <div className="absolute inset-0 z-0" style={{ background: \'#100d09\' }}>')
replace(view,
    '<header className="pointer-events-none absolute inset-x-0 top-0 z-[60] px-3 pt-[max(8px,env(safe-area-inset-top))] [text-shadow:0_2px_5px_rgba(0,0,0,.9)]">',
    '<header className="pointer-events-none absolute inset-x-0 top-0 px-3" style={{ zIndex: 60, paddingTop: \'max(8px, env(safe-area-inset-top))\', textShadow: \'0 2px 5px rgba(0,0,0,.9)\' }}>')
replace(view,
    '<div className="absolute inset-x-[3.5%] bottom-[calc(76px+env(safe-area-inset-bottom,0px))] z-[70] max-h-[56dvh]">\n      <form onSubmit={event => props.onCustomActionSubmit(event, false)} className="relative flex max-h-[56dvh] flex-col px-[7%] pb-5 pt-10 text-[#2d2116]">',
    '<div className="absolute" style={{ left: \'3.5%\', right: \'3.5%\', bottom: \'calc(76px + env(safe-area-inset-bottom, 0px))\', zIndex: 70, maxHeight: \'56dvh\' }}>\n      <form onSubmit={event => props.onCustomActionSubmit(event, false)} className="relative flex flex-col pb-5 pt-10" style={{ maxHeight: \'56dvh\', paddingLeft: \'7%\', paddingRight: \'7%\', color: \'#2d2116\' }}>')

p = root / view
s = p.read_text(encoding='utf-8')
old = '{moreOpen&&<div className="absolute bottom-[calc(76px+env(safe-area-inset-bottom,0px))] left-3 right-3 z-[85] rounded-xl border border-[#7b6547]/70 bg-[#16100a]/95 p-2 shadow-2xl backdrop-blur-sm">'
new = '{moreOpen&&<div className="absolute left-3 right-3 rounded-xl p-2 shadow-2xl backdrop-blur-sm" style={{ bottom: \'calc(76px + env(safe-area-inset-bottom, 0px))\', zIndex: 85, border: \'1px solid rgba(123,101,71,.7)\', background: \'rgba(22,16,10,.95)\' }}>'
if old not in s:
    raise SystemExit('more panel pattern not found')
s = s.replace(old, new, 1)
old = '<nav className="absolute inset-x-0 bottom-0 z-[90] min-h-[72px] px-2 pb-[max(6px,env(safe-area-inset-bottom))] pt-1">'
new = '<nav className="absolute inset-x-0 bottom-0 px-2 pt-1" style={{ zIndex: 90, minHeight: \'72px\', paddingBottom: \'max(6px, env(safe-area-inset-bottom))\' }}>'
if old not in s:
    raise SystemExit('nav pattern not found')
s = s.replace(old, new, 1)
old = '<div className="relative z-[1] grid min-h-[64px] grid-cols-5 items-stretch gap-1">'
new = '<div className="relative grid grid-cols-5 items-stretch gap-1" style={{ zIndex: 1, minHeight: \'64px\' }}>'
if old not in s:
    raise SystemExit('nav inner pattern not found')
s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')

replace('components/immersive/PartyCluster.tsx',
    'className="pointer-events-none absolute left-1/2 top-[46%] z-[40] h-[142px] w-[166px] -translate-x-1/2 -translate-y-1/2"',
    'className="pointer-events-none absolute left-1/2 -translate-x-1/2 -translate-y-1/2" style={{ top: \'46%\', zIndex: 40, height: \'142px\', width: \'166px\' }}')

p = root / 'App.tsx'
s = p.read_text(encoding='utf-8')
needle = "markBootStage('ui:first-paint');"
pos = s.find(needle)
if pos < 0:
    raise SystemExit('App.tsx: ui:first-paint marker not found')
if 'BRASEIRO_BOOT_LAYOUT' not in s:
    line_start = s.rfind('\n', 0, pos) + 1
    indent = s[line_start:pos]
    line_end = s.find('\n', pos)
    if line_end < 0:
        raise SystemExit('App.tsx: malformed ui:first-paint line')
    insert = '\n'.join([
        indent + "const immersiveRoot = document.querySelector<HTMLElement>('[data-braseiro-immersive-root=\"true\"]');",
        indent + 'if (immersiveRoot) {',
        indent + '  const rect = immersiveRoot.getBoundingClientRect();',
        indent + '  const minimumVisibleHeight = Math.min(320, Math.max(180, window.innerHeight * 0.45));',
        indent + '  if (rect.height < minimumVisibleHeight) {',
        indent + '    console.error(`[BRASEIRO_BOOT_LAYOUT] immersive-root-collapsed height=${rect.height} viewport=${window.innerHeight}`);',
        indent + "    localStorage.setItem('braseiro.tor2e.immersiveUi', '0');",
        indent + '    const safeUrl = new URL(window.location.href);',
        indent + "    safeUrl.searchParams.set('classic', '1');",
        indent + "    safeUrl.searchParams.set('immersive', '0');",
        indent + '    window.clearTimeout(bootWatchdog);',
        indent + '    window.location.replace(safeUrl.toString());',
        indent + '    return;',
        indent + '  }',
        indent + '}',
        indent + "markBootStage('ui:verified');",
    ]) + '\n'
    s = s[:line_end + 1] + insert + s[line_end + 1:]
    p.write_text(s, encoding='utf-8')

print('PATCH_4.9.3_OK')
