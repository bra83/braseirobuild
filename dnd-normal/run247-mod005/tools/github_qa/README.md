# QA portátil (sem Android)

Executar `bash tools/github_qa/run_java_contract_tests.sh` na raiz com JDK 17+.

`tools/github_qa/src/org/json` e `tools/github_qa/src/com/braseiro/.../AdventureEngine` são SIMULADORES para testes em JDK, não usados no aplicativo nem em APK. Não são equivalentes ao `org.json` real Android nem a SQLite. `app/src/test` contém os testes formais do motor.

A função real `UniversalGithubLab` utiliza **AdventureEngine** e **AdventurePersistenceAdapter** da aplicação Android; o simulador só exercita a lógica e recuperações de estado previstas.
