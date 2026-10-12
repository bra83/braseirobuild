#!/usr/bin/env python3
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
main=root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java'
scanner=root/'android/app/src/main/java/com/braseiro/pathfinder2e/SafFolderScanner.java'
build=root/'android/app/build.gradle.kts'
text=main.read_text(encoding='utf-8')
errors=[]
requirements={
 'REQ_MANUAL_FOLDER':r'REQ_MANUAL_FOLDER\s*=\s*1023',
 'REQ_TOKEN_IMAGE_FOLDER':r'REQ_TOKEN_IMAGE_FOLDER\s*=\s*1024',
 'selectManualFolder':r'private\s+void\s+selectManualFolder\s*\(',
 'syncManualFolder':r'private\s+void\s+syncManualFolder\s*\(',
 'selectTokenImageFolder':r'private\s+void\s+selectTokenImageFolder\s*\(',
 'openTokenInput':r'private\s+InputStream\s+openTokenInput\s*\(',
 'fallback OAuth':r'UnregisteredOnApiConsole|unregisteredonapiconsole',
 'captura pasta manual':r'if\s*\(request==REQ_MANUAL_FOLDER\)',
 'captura pasta imagens':r'if\s*\(request==REQ_TOKEN_IMAGE_FOLDER\)',
 'versão':r'versionName\s*=\s*appVersionName',
}
for name,pat in requirements.items():
    target=build.read_text(encoding='utf-8') if name=='versão' else text
    if not re.search(pat,target,re.I): errors.append(name)
for method in ['selectManualFolder','syncManualFolder','selectTokenImageFolder','openTokenInput','showDriveOAuthFallback']:
    if len(re.findall(r'private\s+[\w<>]+\s+'+method+r'\s*\(',text))!=1: errors.append(method+' deve existir exatamente uma vez')
if not scanner.exists(): errors.append('SafFolderScanner.java ausente')
else:
    st=scanner.read_text(encoding='utf-8')
    for needle in ['listPdfs','listImages','ACTION_OPEN_DOCUMENT_TREE']:
        if needle not in st and needle!='ACTION_OPEN_DOCUMENT_TREE': errors.append('scanner sem '+needle)
# Remove strings/comments and confirm methods at class depth.
def strip(src):
    out=[];i=0;state='code'
    while i<len(src):
        c=src[i];n=src[i+1] if i+1<len(src) else ''
        if state=='code':
            if c=='"':state='str';out.append(' ')
            elif c=="'":state='char';out.append(' ')
            elif c=='/' and n=='/':state='line';out.extend('  ');i+=1
            elif c=='/' and n=='*':state='block';out.extend('  ');i+=1
            else:out.append(c)
        elif state=='str':
            if c=='\\':out.extend('  ');i+=1
            elif c=='"':state='code';out.append(' ')
            else:out.append(' ')
        elif state=='char':
            if c=='\\':out.extend('  ');i+=1
            elif c=="'":state='code';out.append(' ')
            else:out.append(' ')
        elif state=='line':
            out.append('\n' if c=='\n' else ' ')
            if c=='\n':state='code'
        else:
            out.append(' ')
            if c=='*' and n=='/':out.append(' ');i+=1;state='code'
        i+=1
    return ''.join(out)
clean=strip(text);depth=0
for line_no,line in enumerate(clean.splitlines(),1):
    if re.search(r'private\s+[^=;]+\s+(selectManualFolder|syncManualFolder|selectTokenImageFolder|openTokenInput|showDriveOAuthFallback)\s*\(',line):
        if depth!=1: errors.append(f'método de Drive aninhado na linha {line_no}, profundidade {depth}')
    depth+=line.count('{')-line.count('}')
if depth!=0: errors.append(f'chaves desequilibradas: {depth}')
if errors:
    print('FALHA:')
    for e in errors: print(' -',e)
    sys.exit(1)
print('OK: contrato do Drive sem OAuth e pastas SAF validado.')
