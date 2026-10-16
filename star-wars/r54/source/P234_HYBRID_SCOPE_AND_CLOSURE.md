# BRASEIRO STAR WARS — FECHAMENTO HÍBRIDO P234

**Base:** P233 intacto, P234 evolutivo. **Escopo de aventuras congelado em cinco**:

1. Sob um Sol Negro (`under-black-sun` / SWEP01)
2. Além da Orla (`beyond-the-rim` / SWE05)
3. O Longo Braço do Hutt (`long-arm-of-the-hutt` / SWE02)
4. A Joia de Yavin (`jewel-of-yavin` / SWE09)
5. A Máscara da Rainha Pirata (`mask-of-the-pirate-queen` / SWE13)

**Nenhuma sexta aventura será adicionada** ao registro ou alimentará o Mestre pelo RAG quando classificada como AVENTURA. PDFs de regras, erratas e suplementos permanecem utilizáveis. Nenhum material do usuário é apagado; fontes externas ao escopo continuam apenas na Biblioteca, se importadas.

## Autoridade híbrida

| Camada | Responsabilidade | Limite inegociável |
|---|---|---|
| **Código determinístico** | Rolagens, dificuldade, talentos, inventários, combates, status, clocks, viagem, XP, NPCs, persistência e consequências executadas | Ação sem executor real não é resolvida por IA. Validar recibos, atomicidade, replay e saves. |
| **RAG local / Biblioteca** | Encontrar o texto e a referência de regra, cena e suplemento com proveniência, páginas e prioridade | Trecho sem fonte identificada não autoriza mecânica inventada. Aventura estrangeira fica fora do índice ativo do Mestre. |
| **Gemini + Motor Bárbara** | Narração sensorial, diálogo, descrição dinâmica e **propostas** estruturadas com contexto e regras recuperadas | Não ganha autoridade de modificar ficha, dados, XP, inventário ou consequência. Referências de regra inventadas são rejeitadas, não removidas silenciosamente. |
| **Fallback** | Persistir a intenção original e preservar o estado quando Gemini não responde ou apresenta proposta inválida | Sem texto fingindo que Gemini respondeu; sem efeitos ou PE gerados como substitutos. |

## Trabalho P234 de fonte real

- `hybridFiveAdventurePolicy.mjs`: allowlist estrita de cinco módulos e classificação por título/código da fonte; contrato de roteamento sem mutação de estado.
- `publishedAdventureRegistry.mjs`: rejeita registries que tragam módulo extra, faltante ou duplicado.
- `nativeLibrary.mjs` e `sourceSearch.mjs`: fontes de aventura estrangeiras ou desativadas não chegam ao Mestre/RAG; fontes de regras e erratas permanecem.
- `master/context.mjs`: fornece contrato híbrido com escopo, fonte, estado do narrador, versão; compatibilidade com consultas existentes.
- `master/provider.mjs`: prompt remoto consistente com as cinco aventuras, executor local e limites de narrativa.
- `core/uiBridge.mjs`: **não apaga** IDs de regras inventados pelo Gemini; rejeita proposta e recua para apenas registrar intenção do PJ em caso de falha.
- Bundle offline regenerado; versão fonte Android 1.0.234; Java/Kokoro preservados.

## Auditoria de cobertura — NÃO é certificação dos PDFs

O inventário automático de grafos encontra **100 nós de cena alcançáveis e 141 verificações de perícia declaradas** no conjunto de cinco aventuras, com três episódios declarados por aventura, e **54 registros `TESTADO` no corpus RAG estruturado**. Esses números comprovam só o escopo indexado, **não** equivalência de regras, talentos, estatísticas, diálogos, encontros, itens, capítulos e consequências às fontes oficiais.

Ver `validation/P234_HYBRID_COVERAGE_LEDGER.json` e `evidence/P234/`.

## Critérios de encerramento obrigatórios antes do APK

1. Auditoria PDF→grafo de **todos** os episódios/cenas/caminhos dos cinco módulos, com fonte, localização, teste, encontro, dados mecânicos, consequência, recompensa e estado de verificação. Não encerrar parte sem resolver lacunas.
2. Cobertura total de regras nucleares EotE/AoR e expansões em arquitetura híbrida: todas as ações com executor, fonte RAG e/ou pendência explícita; nenhum talento marcado certificado por mera presença em lista/árvore.
3. Mecânicas locais, Gemini/RAG/TTS, save/load e reaberturas integradas e comprovadas em fluxos completos das cinco aventuras. Falha de IA fica contida sem corrupção de estado. Não é possível garantir taxa de falha zero de rede, modelo, sistema operacional ou dispositivo.
4. **Somente após 1–3:** Gradle/Android Studio, APK reproduzível, instalação e homologação real nos dispositivos Android (incluindo Galaxy A57), e entrega final.

**Situação P234:** proteção híbrida implementada; completude editorial de 5/5 aventuras e de todas as regras **não certificada**. APK/homologação deliberadamente postergados conforme solicitado. Não adicionar novas aventuras e não regressar checkpoints anteriores.
