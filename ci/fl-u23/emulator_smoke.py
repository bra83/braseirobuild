"""Smoke the ACTUAL installed APK's Android WebView via page-scoped DevTools.
Android WebView does not support Playwright's Browser.setDownloadBehavior;
only Chrome DevTools Runtime.evaluate on /devtools/page endpoints is used.
Three cold process reopens, React UI, native Capacitor, source build identity,
persistent localStorage marker, app process and screenshot are asserted.
"""
import json, os, re, secrets, subprocess, time, urllib.request, urllib.error
from pathlib import Path
import websocket

APP='com.braseiro.forbiddenlands'
EXPECTED='423798ac3f8cefb8fc14caf114cb2779b6d57d92'
MARKER_KEY='fl-u23-android-smoke-three-reopenings'
MARKER=secrets.token_hex(12)

def adb(*args):
    return subprocess.check_output(['adb',*args],text=True,stderr=subprocess.PIPE,timeout=30).strip()

def detach_forward():
    subprocess.run(['adb','forward','--remove','tcp:9222'],
                   stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=10,check=False)

def find_webview_socket():
    deadline=time.monotonic()+95
    while time.monotonic()<deadline:
        try:
            sockets=adb('shell','cat','/proc/net/unix')
            match=re.findall(r'@(webview_devtools_remote[^\s]*)',sockets)
            if match:
                return match[-1]
        except (subprocess.CalledProcessError,subprocess.TimeoutExpired):
            pass
        time.sleep(.75)
    raise AssertionError('WebView debugging socket not available in actual installed app')

def find_page():
    deadline=time.monotonic()+45
    while time.monotonic()<deadline:
        try:
            with urllib.request.urlopen('http://127.0.0.1:9222/json/list',timeout=4) as res:
                targets=json.load(res)
            pages=[t for t in targets if t.get('type')=='page'
                   and t.get('webSocketDebuggerUrl')
                   and t.get('url','').startswith(('http://localhost','https://localhost','capacitor://localhost'))]
            if pages:return pages[0]
        except (OSError,ValueError,urllib.error.URLError):
            pass
        time.sleep(.7)
    raise AssertionError('No localhost app page exposed through Android WebView DevTools')

def evaluate(connection,script,request_id=1):
    connection.send(json.dumps({'id':request_id,'method':'Runtime.evaluate',
      'params':{'expression':script,'returnByValue':True,'awaitPromise':True}}))
    deadline=time.monotonic()+12
    while time.monotonic()<deadline:
        message=json.loads(connection.recv())
        if message.get('id')!=request_id:continue
        if message.get('error'):raise RuntimeError('WebView CDP refused Runtime.evaluate: '+str(message['error']))
        response=message.get('result',{})
        if response.get('exceptionDetails'):
            raise RuntimeError('WebView JavaScript error: '+str(response['exceptionDetails'])[:700])
        value=response.get('result',{})
        return value.get('value')
    raise TimeoutError('WebView Runtime.evaluate did not return within 12 seconds')

def snapshot(script,iteration):
    deadline=time.monotonic()+90
    last_error=''
    while time.monotonic()<deadline:
        try:
            target=find_page()
            address=target['webSocketDebuggerUrl']
            conn=websocket.create_connection(address,timeout=12)
            try:
                result=evaluate(conn,script,iteration+1)
            finally:
                conn.close()
            if isinstance(result,dict) and result.get('ready') is True:
                return target,result
            last_error=str(result)[:400]
        except Exception as exc:
            last_error=str(exc)[:400]
        time.sleep(.8)
    raise AssertionError('Installed native WebView did not render the app: '+last_error)

def main():
    results=[]
    for iteration in range(3):
        adb('shell','am','force-stop',APP)
        detach_forward()
        adb('shell','monkey','-p',APP,'-c','android.intent.category.LAUNCHER','1')
        socket=find_webview_socket()
        adb('forward','tcp:9222','localabstract:'+socket)
        script=r"""(async function() {
          try {
            const root=document.querySelector('#root');
            if(document.readyState!=='complete'||!root||root.children.length===0)
              return {ready:false,stage:'React not rendered'};
            const body=document.body?.innerText?.trim()||'';
            if(body.length<=80)return {ready:false,stage:'Body still loading',length:body.length};
            const response=await fetch('/fl-build-source.json',{cache:'no-store'});
            if(!response.ok)return {ready:false,stage:'No source marker',status:response.status};
            const source=await response.json();
            const key=SMOKE_KEY_PLACEHOLDER, token=SMOKE_TOKEN_PLACEHOLDER;
            if(!localStorage.getItem(key))localStorage.setItem(key,token);
            return {ready:true,url:location.href,sourceCommit:source.sourceCommit,
              runtimeFilesVerified:source.runtimeFilesVerified,
              native:!!window.Capacitor?.isNativePlatform?.(),
              bodyLength:body.length,title:document.title,
              uiRecognized:/Braseiro|Forbidden|Aventura|Campanha|Configura|Personage/i.test(body),
              marker:localStorage.getItem(key)};
          }catch(e){return {ready:false,stage:String(e)}}
        })()"""
        script=script.replace('SMOKE_KEY_PLACEHOLDER',json.dumps(MARKER_KEY)).replace(
            'SMOKE_TOKEN_PLACEHOLDER',json.dumps(MARKER))
        target,state=snapshot(script,iteration)
        assert state['native'],'Capacitor reported a non-native page: '+str(state)
        assert state['sourceCommit']==EXPECTED,'Wrong APK source version '+str(state)
        assert state['runtimeFilesVerified']==602,'Wrong source manifest: '+str(state)
        assert state['uiRecognized'],'App rendered unexpected UI: '+str(state)
        assert state['marker']==MARKER,'WebView localStorage lost or changed across process reopens'
        results.append({'launch':iteration+1,'status':'PASS',
            'pid':adb('shell','pidof',APP),'pageId':target.get('id'),**state})
        print('FL_U26_ANDROID_NATIVE_REOPEN '+json.dumps(
            {'launch':iteration+1,'pid':results[-1]['pid'],'native':state['native'],
             'bodyLength':state['bodyLength'],'sourceCommit':state['sourceCommit']}),
             flush=True)
    screenshot=subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=45)
    assert screenshot.startswith(b'\x89PNG\r\n\x1a\n'),'Android screenshot not a PNG'
    Path('fl-u23-android-smoke.png').write_bytes(screenshot)
    logs=adb('logcat','-d','-t','2000')
    assert not re.search(r'FATAL EXCEPTION[\s\S]{0,1500}Process: '+re.escape(APP),logs),'App fatal exception in logcat'
    report={'status':'PASS','androidApi':adb('shell','getprop','ro.build.version.sdk'),
      'launches':results,'reopensCertified':3,'sourceCommit':EXPECTED,
      'localStoragePreserved':True,'nativeWebView':True,
      'scope':'Published signed APK installed via adb; three cold native WebView process reopens, React UI, source manifest, localStorage, PNG, no app fatal exception. NOT physical Galaxy, Kokoro engine, Gemini OAuth, or full campaign restore.'}
    Path('fl-u23-android-smoke.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('FL_U26_ANDROID_SMOKE_PASS '+json.dumps({'launches':3,'native':True,
      'webview':True,'persistence':True,'androidApi':report['androidApi']}),flush=True)

if __name__=='__main__':
    main()
