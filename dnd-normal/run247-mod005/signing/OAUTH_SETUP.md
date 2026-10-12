# Configuração OAuth 2.0 (Google Drive API) - Braseiro D&D

Para habilitar a integração direta via REST API do Google Drive (opcional), registre o cliente OAuth no Google Cloud Console com os parâmetros exatos do certificado identificado:

## Parâmetros de Registro no Google Cloud Console

1. **Tipo de Aplicativo:** Android
2. **Nome do Pacote (Package Name / Application ID):** `com.braseiro.dnd5e`
3. **Fingerprint do Certificado SHA-1:** `6F:47:92:78:1F:11:ED:FF:C8:92:AF:EB:95:69:4E:3C:03:F3:5C:DF`
4. **Fingerprint do Certificado SHA-256:** `6A:A8:7A:24:A2:EB:47:62:C3:F4:93:C9:D5:FD:DB:F1:0B:60:CC:86:59:A9:04:23:AE:D7:CD:E8:AE:58:2B:D3`
5. **DN do Certificado:** `CN=Braseiro Stable Debug, O=Braseiro, C=BR`
6. **Escopo Solicitado (Scope):** `https://www.googleapis.com/auth/drive.readonly`

## Assinatura Determinística da Build Debug
O repositório inclui a chave de depuração estável em `signing/debug.keystore` (`alias: androiddebugkey`, senha: `android`), garantindo que tanto compilações locais quanto em CI (BRELGA) gerem sempre o mesmo fingerprint SHA-1 compatível com o cliente OAuth cadastrado.

## Alternativa Recomendada (Sem OAuth)
O aplicativo suporta o seletor nativo de diretórios do Android (Storage Access Framework - SAF), que acessa as pastas do Google Drive sem depender de cadastro no Google Cloud Console ou aprovação de consentimento OAuth.
