# Braseiro Android 4.3.2

Este diretório também pode ser aberto diretamente no Android Studio, mas o caminho recomendado é abrir a pasta raiz do pacote, que contém `settings.gradle.kts` e aponta o módulo `:app` para `android/app`.

Requisitos: JDK 17, Android SDK 35 e acesso aos repositórios Google Maven/Maven Central na primeira sincronização.

Para verificar dentro deste diretório:

```bash
./gradlew :app:compileDebugJavaWithJavac :app:lintDebug :app:assembleDebug --stacktrace --no-daemon
```
