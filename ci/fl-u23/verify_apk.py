import hashlib,json,sys,zipfile
from pathlib import Path

root=Path(sys.argv[1]);apk=Path(sys.argv[2]);out=apk.parent
source='b4e48fb77b7b7fdc5f85d1c998518196a0aaac32'
gate=json.loads((root/'e2e-results/u22-complete-82.json').read_text())
assert gate['status']=='PASS' and gate['certified']==82 and gate['pending']==0 and not gate['invalidEvidence']
assert gate['sourceCommit']==source
with zipfile.ZipFile(apk) as archive:
    manifest=json.loads(archive.read('assets/public/fl-build-source.json'))
    assert manifest['sourceCommit']==source and manifest['runtimeFilesVerified']==584
    assert manifest['nativeDonorSha256']=='2ee4e3af34459421a4b1e0389125cfcf057ce6938e8ddb8e281a770fca599f83'
    count=0
    for p in (root/'dist').rglob('*'):
        if not p.is_file():continue
        name='assets/public/'+p.relative_to(root/'dist').as_posix()
        assert archive.read(name)==p.read_bytes(), 'APK differs from compiled web output: '+name
        count+=1
    marker=json.loads(archive.read('assets/public/braseiro-release-marker.json'))
    assert marker['version']=='3.3.24' and marker['generatedBy']=='npm run android:sync'
sha=hashlib.sha256(apk.read_bytes()).hexdigest()
(out/(apk.name+'.sha256')).write_text(sha+'  '+apk.name+'\n')
proof={'status':'PASS','sourceCommit':source,'runtimeFilesVerified':584,'nativeDonorSha256':'2ee4e3af34459421a4b1e0389125cfcf057ce6938e8ddb8e281a770fca599f83','downstreamCertified':82,'apkSha256':sha,'apkBytes':apk.stat().st_size,'webAssetsCompared':count,'scope':'Android binary, source identity, fresh web assets and strict browser gate; emulator evidence separate; physical Galaxy tests not performed'}
(out/'BUILD_PROOF_U23.json').write_text(json.dumps(proof,indent=2)+'\n')
print(json.dumps(proof))
