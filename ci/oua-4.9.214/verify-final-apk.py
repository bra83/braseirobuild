import json,zipfile,sys
with zipfile.ZipFile(sys.argv[1]) as z:
 files=set(z.namelist())
 m=json.loads(z.read('assets/tor2e-ui/hero-token-pack-352.json'))
 old=json.loads(z.read('assets/tor2e-ui/party-token-manifest.json'))
 assert len(m['tokens'])==352 and len(old['tokens'])==120
 for t in m['tokens']:assert 'assets/tor2e-ui/assets/'+t['asset'] in files
 assert sum(p.startswith('assets/tor2e-ui/assets/party/characters/braseiro352/') and p.endswith('.png') for p in files)==352
print('OUA_49214_472_TOKEN_IN_APK_PASS')