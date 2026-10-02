# ENGINE-002 AUDIT

A base foi reconstruída segundo QA001_RELEASE.md do commit b8e59f6c0a8d35bbc93865830359afecd0642d10. Os oito hashes do INTEGRATED_FILE_HASHES.txt conferiram antes do delta.

Evidências:
- B-003A: PASS — pending/cancel/invalid não gastam; resume válida gasta uma vez; replay não gasta.
- B-003B: PASS — Grand Finale e Cold-Blooded validam inputs antes do spend e são idempotentes.
- M-001: PASS — fallback genérico removido para feat/subclasse/Aurora; action desconhecida é acao_nao_suportada; AppDb não persiste rejeições.
- 19/19 scripts PASS.
- ENGINE-002.patch aplica limpo sobre uma segunda reconstrução da base.
- Nenhum APK/AAB.

Arquivos proibidos confirmados byte-idênticos à base:
- MainActivity.java 3ebf198aed95e74c6a6930457dcecafa2c9c0d4575a872aa783c52d7736defa7
- WorldStateEngine.java 4113faadfe60aa6730a4e8ee589e790621d5f8ed69a568b60f20dc1a0ed3ce57
- GameplaySandboxRuntimeEngine.java 31b03324277fe4d2accd047abc8ac19ce6ea9b51f1bbac139f042635d8025680
- AndroidManifest.xml c221e7d9a4bffbc6b0d6791375dabf1fc057193684c6ff633440574e579a9ca1
- AndroidTtsBackend.java 504cb95ec2dd703cbc3253d9c4bc9871a56b161e49ce431dd9ba0789775706c2
- NarrationChunker.java 078e6e139c3965253cb3055df2483bc3d42c9600815bf360a76a293702dc6459

PATCH_SHA256=323892809682860d480cbc39f82786f53eb91d2c3ab51b1f840e329b9629f2e1
CHECKPOINT_SHA256=fb5975ecf25b33270b5c3601f2bec9a8d43fca8c9f4c14e88af7e509756671e3

B-001, B-002, B-004 e N-001 são explicitamente fora do escopo ENGINE-002 e não foram corrigidos silenciosamente.
Os cinco grupos SOURCE_BLOCKED/PARTIAL permanecem. BOOK_COMPLETE=false.
