import asyncio,json,os,re,subprocess,time,urllib.request
from pathlib import Path
from playwright.async_api import async_playwright

APP='com.braseiro.forbiddenlands'
def adb(*args):return subprocess.check_output(['adb',*args],text=True).strip()
async def main():
    async with async_playwright() as p:
        results=[]
        for iteration in range(3):
            adb('shell','am','force-stop',APP)
            adb('shell','monkey','-p',APP,'-c','android.intent.category.LAUNCHER','1')
            socket=None
            for _ in range(90):
                sockets=adb('shell','cat','/proc/net/unix')
                matches=re.findall(r'@(webview_devtools_remote[^\s]*)',sockets)
                if matches:socket=matches[-1];break
                await asyncio.sleep(1)
            assert socket,'Installed app WebView did not become available'
            adb('forward','tcp:9222','localabstract:'+socket)
            browser=await p.chromium.connect_over_cdp('http://127.0.0.1:9222')
            pages=[page for ctx in browser.contexts for page in ctx.pages]
            assert pages,'No installed WebView page'
            page=pages[0]
            await page.wait_for_function("document.readyState==='complete' && document.querySelector('#root')?.children.length>0",timeout=90000)
            assert page.url.startswith(('http://localhost','https://localhost','capacitor://localhost')),page.url
            await page.wait_for_function("document.body.innerText.trim().length>80",timeout=60000)
            text=await page.locator('body').inner_text()
            assert re.search(r'Braseiro|Forbidden|Aventura|Campanha|Configura|Personage',text,re.I),text[:300]
            data=await page.evaluate("async()=>({source:await(await fetch('/fl-build-source.json')).json(),native:!!window.Capacitor?.isNativePlatform?.(),textLength:document.body.innerText.trim().length})")
            assert data['native'] and data['source']['sourceCommit']=='b4e48fb77b7b7fdc5f85d1c998518196a0aaac32',data
            results.append({'launch':iteration+1,'status':'PASS','url':page.url,**data})
            if iteration==2:await page.screenshot(path='fl-u23-android-smoke.png',full_page=True)
            await browser.close()
        log=adb('logcat','-d','-t','2000')
        assert not re.search(r'FATAL EXCEPTION[\s\S]{0,1500}Process: '+re.escape(APP),log),'App fatal exception in logcat'
        report={'status':'PASS','androidApi':adb('shell','getprop','ro.build.version.sdk'),'launches':results,'scope':'Published APK installed; actual bundled native WebView rendered and reopened three times; no physical Galaxy testing or external TTS engine certification'}
        Path('fl-u23-android-smoke.json').write_text(json.dumps(report,indent=2)+'\n')
        print(json.dumps(report))
asyncio.run(main())
