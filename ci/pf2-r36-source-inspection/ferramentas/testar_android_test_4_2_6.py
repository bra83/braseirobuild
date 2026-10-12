from pathlib import Path
root=Path(__file__).resolve().parents[1]
app=root/'android/app'
main=app/'src/main'
assert not (app/'src/androidTest').exists(), 'src/androidTest não deve integrar o pacote de produção'
assert not (app/'src/test').exists(), 'src/test não deve integrar o pacote de produção'
misplaced=list(main.rglob('*Test.java'))+list(main.rglob('*Tests.java'))
assert not misplaced, 'Testes indevidos em src/main: '+', '.join(str(p) for p in misplaced)
build=(app/'build.gradle.kts').read_text(encoding='utf-8')
for token in ('testImplementation(', 'androidTestImplementation(', 'testInstrumentationRunner'):
    assert token not in build, f'Dependência/configuração de teste ainda ativa: {token}'
archived=root/'qa_tests/androidTest/java/com/braseiro/pathfinder2e/AppDbBackupIsolationTest.java'
assert archived.is_file(), 'Teste de backup arquivado ausente'
test=archived.read_text(encoding='utf-8')
assert '.remove("world_profile").put(' not in test
assert 'JSONObject legacyDiceRow=' in test
assert 'VERSION_NAME=' in (root/'version.properties').read_text(encoding='utf-8')
print('OK — pacote de produção sem source sets de teste; testes arquivados em qa_tests.')
