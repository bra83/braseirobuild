# P237 — fechamento real SWE02 Geonosis / bloqueadores restantes

**Base imutável:** P236 verificado (`a5a7e549b4aec05e18bbc266084019ca01fdb8a25db3caf72f92215f24c8738d`), 12.765 arquivos. Não recomeçar, não apagar fonte, saves, Kokoro, TTS, Motor Bárbara ou outros VTTs.

## Implementação P237 (código executável)

- Carapaça de Sivor: recolhimento explícito da carga da Krayt Fang, identificação com Piddock e recibo persistido; nenhum ponto por presença no porão.
- Drombb: registros para Thwheek só podem ser recuperados após neutralização comprovada e teste **Difícil de Computadores**, página impressa 23. Derrota sem acesso ao terminal não gera prova.
- Piddock: informação sobre o espião Kubaz e interesse de Teemo nos B1 após **Charme Difícil**, página 34; o histórico de testes sustenta as flags.
- Dimmock: audiência oficial com **Charme Médio** (+1), **Coerção Formidável** falhada (-2), oposição ao Império (+1), lealdade ao Império (-1), rudeza (-1), fatos individualmente apresentados (+1 cada). Não pontuar um fato duas vezes nem aceitar evidência sem origem. Faixas 0–2/3–5/6+ dão 5/3/0 soldados, respectivamente, SWE02 p.36.
- Gantry AA7: fichas oficiais distintas do soldado geonosiano e do Gand Vrixx’tt (p.37), com caçador Gand presente mesmo quando não há soldados. Embarque na Lucky Guess exige confronto real, fuga ou resgate pelos irmãos Vio; não transformar resgate em vitória nem suborno sem débito de créditos em passagem livre. Três reaberturas e controlador real testados.
- Rota legada Geonosis preservada por compatibilidade, **não recebe pontuação oficial nem apoio de Dimmock**.

## Provas

- `tests/uae-p237-swe02-geonosis-dimmock-gantry.test.mjs`: 12 testes PASS.
- Regressão global **356 arquivos regulares, 3.254 testes PASS; seis stress 6 PASS; total 3.260/3.260 PASS em 362 arquivos**. Zero falhas, zero skip observados nos logs.
- Sintaxe **570 JS/MJS, 22 JSON, 2 XML PASS**, oito arquivos Java/Kokoro idênticos a P236.
- Bundle offline recomposto pelo `tools/build-ui-bridge-bundle.mjs`, 121 módulos, Android source versionName `1.0.237` / versionCode `10237`. **APK não compilado.**
- `evidence/P237/P237_REGRESSION_RESULTS.json`, `evidence/P237/SYNTAX_AND_JAVA.json`, `validation/P237_SWE02_SOURCE_PROOFS.json`.

## Lacunas bloqueadoras, não encobertas por PASS estrutural

1. SWE02 restante: Ato 1 completo (nave, rastreador, Thwheek, Nabat, New Meen não violenta); Ato 2 restante (convidados da festa, Piddock limite de perguntas, Anatta pagos, Vrixx’tt bribe, cobertura dos contêineres, nave e pilotos); Ato 3 (lustre, alarmes, gladiadores, Anatta/Jabba, recompensa, epílogo e ficha Hutt Crime Lord oficial).
2. SWE02 exige cotejo de **todas as páginas** e cada alternativa de derrota/fuga, fonte→executor→UI→três reaberturas.
3. Under a Black Sun, Beyond the Rim, The Jewel of Yavin, Mask of the Pirate Queen: revisão editorial integral ainda pendente.
4. EotE/AoR/expansões selecionadas, incluindo **360 consumidores AoR**: sem certificado individual de todas as regras.
5. APK GitHub/BRELGA e Android físico A57/S23 Ultra/Kokoro são os **últimos gates**, não executados neste checkpoint.

**Não afirmar jogo fechado, não dar percentual arbitrário.**
