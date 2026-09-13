# Braseiro Fichas Multissistema — Android Studio

Aplicativo Android offline que empacota a entrega final das Fichas Multissistema em um WebView local.

## Projeto
- Application ID: `com.braseiro.fichas`
- minSdk: 23
- targetSdk: 35
- compileSdk: 35
- versão: 1.0.0 (versionCode 1)
- sem dependências de runtime externas além do Android WebView do aparelho
- conteúdo web em `app/src/main/assets/www/`

## Recursos Android adicionados
- importação de JSON via seletor de arquivos do Android;
- exportação de JSON via Storage Access Framework;
- cópia de JSON usando clipboard nativo;
- persistência local via DOM Storage/WebView;
- funcionamento offline.

## Build
Abra a pasta no Android Studio e execute `app`, ou rode:

```bash
./gradlew :app:assembleDebug
```

O APK debug fica em `app/build/outputs/apk/debug/app-debug.apk`.
