"""Restore only historical Android configuration from a hash-pinned donor."""
import hashlib, json, re, shutil, sys, zipfile
from pathlib import Path

project, archive = map(Path, sys.argv[1:3])
expected = '2ee4e3af34459421a4b1e0389125cfcf057ce6938e8ddb8e281a770fca599f83'
if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
    raise SystemExit('Historical native donor digest mismatch')
paths = ['android/app/braseiro-debug.jks', 'android/app/google-services.json', 'android/keystore.properties']
with zipfile.ZipFile(archive) as donor:
    for name in paths:
        data = donor.read(name)
        target = project / name
        if target.exists() and target.read_bytes() != data:
            raise SystemExit('Existing native configuration differs: ' + name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        target.chmod(0o600)

google = json.loads((project / paths[1]).read_text(encoding='utf-8'))
if google['client'][0]['client_info']['android_client_info']['package_name'] != 'com.braseiro.forbiddenlands':
    raise SystemExit('Historical native donor package mismatch')
gradle = project / 'android/app/build.gradle'
text = gradle.read_text(encoding='utf-8')
if 'configuredStoreFile' not in text or not re.search(r'braseiro\s*\{', text):
    raise SystemExit('Historical Gradle signing definition is missing')
for build_type in ('debug', 'release'):
    pattern = re.compile(r'(?m)^([ \t]*)' + build_type + r'\s*\{\s*\n')
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise SystemExit('Ambiguous Android build type: ' + build_type)
    match = matches[0]
    existing_block = text[match.end():text.index('}', match.end())]
    if 'signingConfig' not in existing_block:
        indent = match.group(1) + '    '
        insertion = indent + 'if (configuredStoreFile.exists()) { signingConfig signingConfigs.braseiro }\n'
        text = text[:match.end()] + insertion + text[match.end():]
gradle.write_text(text, encoding='utf-8')
proof_path = project / 'public/fl-build-source.json'
if proof_path.exists():
    proof = json.loads(proof_path.read_text(encoding='utf-8'))
    proof['nativeDonorSha256'] = expected
    proof['nativeSigning'] = 'historical R23 native configuration restored; certificate checked by signing gate'
    proof_path.write_text(json.dumps(proof, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'nativeDonorSha256': expected, 'restoredFiles': len(paths), 'historicalSigningConnected': True}))
