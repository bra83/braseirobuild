from pathlib import Path
root=Path(__file__).resolve().parents[1]
version={}
for line in (root/'version.properties').read_text(encoding='utf-8').splitlines():
    if '=' in line:
        k,v=line.split('=',1);version[k.strip()]=v.strip()
expected_name=version.get('VERSION_NAME','')
expected_code=version.get('VERSION_CODE','')
assert expected_name and expected_code, 'version.properties incompleto'
app=(root/'android/app/build.gradle.kts').read_text(encoding='utf-8')
settings=(root/'settings.gradle.kts').read_text(encoding='utf-8')
assert 'import java.util.Locale' in app and 'Locale.US' in app, 'Locale deve usar import explícito'
assert 'java.util.Locale.US' not in app, 'Referência ambígua java.util.Locale.US reapareceu'
assert 'version.properties' in app and 'appVersionName' in app and 'appVersionCode' in app, 'Gradle não usa versão central'
assert 'project(":app").projectDir = file("android/app")' in settings, 'Raiz não aponta para android/app'
assert not any((root/'android/app/src/main').rglob('*Test.java')), 'Teste foi copiado para src/main'
print(f'OK — Gradle Kotlin DSL e layout de produção {expected_name} ({expected_code}) validados.')
