# AUDITORIA OUA 4.9.13 — INÍCIO DA CRÔNICA, GEMINI TEXTO E TTS

Data: 2026-09-21

## Escopo

Auditoria restrita ao caminho que vinha falhando no aparelho:

1. tocar em **Começar aventura**;
2. Gemini produzir a abertura;
3. a cena ser persistida e aberta na UI;
4. a origem/modelo do texto ficar identificável;
5. Charon/Gemini TTS ler a narração inteira;
6. erros não deixarem a tela num estado falso de "iniciada";
7. APK só ser publicado depois de transpilação, build Android e verificação de update.

## Achados

### A. O release 4.9.13 estava sendo barrado por um erro no próprio gate do workflow

O APK compilava como `49013 / 4.9.13`, e o baseline era corretamente `49012 / 4.9.12`.
O workflow, porém, tinha as duas comparações trocadas:

- baseline era comparado com 49013;
- APK novo era comparado com 49012.

Isso não era um defeito do APK, mas impedia a validação final e a publicação. Corrigido.

### B. A abertura podia terminar sem ficar evidente se o texto veio do Gemini

A cena agora grava em `narrativeMeta`:

- `generationSource`;
- `generationModel`;
- `narrationWordCount`.

Na crônica aparece **Gemini · <modelo>** ou, em outros turnos, **Fallback local**.
Na abertura, fallback local silencioso foi desativado: se o Gemini falhar, o início falha de forma visível.

### C. Abertura narrativa não deve ser bloqueada por ausência de um trecho RAG aplicável

Abertura sem resolução mecânica pode prosseguir sem um trecho específico do manual.
Isso não libera invenção de regra: apenas evita que uma cena puramente narrativa seja bloqueada porque a busca não encontrou uma regra aplicável.

### D. O TTS aceitava qualquer WAV suficientemente grande como "completo"

Esse era um caminho plausível para o sintoma **"narra o primeiro bloco / primeira frase e para"**.

A resposta do Gemini podia conter apenas um prefixo do texto pedido; se houvesse áudio e o arquivo tivesse tamanho mínimo, o app tratava a síntese como concluída.

Correção:

- o áudio é validado também pela duração;
- para textos com 18+ palavras, duração incompatível com o tamanho pedido é tratada como **áudio truncado**;
- a tentativa é descartada e o fluxo tenta novamente / usa o próximo modelo / cai para TTS Android quando necessário.

O limite é intencionalmente muito permissivo (aprox. 375 palavras/minuto) para detectar truncamento grave, não para avaliar estilo de fala.

### E. Uma cena longa não deve ser enviada ao TTS como uma única síntese opaca

O código anterior tinha sido alterado para mandar a cena inteira em uma única chamada. Isso reduz variação de timbre, mas aumenta:

- latência até a primeira fala;
- risco de truncamento;
- custo de repetir tudo após uma falha.

Novo fluxo:

- separa a narração em blocos de até ~680 caracteres, em pontuação natural;
- sintetiza o primeiro bloco;
- começa a reproduzir;
- enquanto ele toca, pré-carrega o próximo;
- usa sempre Charon e o mesmo perfil vocal;
- continua até o último bloco;
- cada bloco passa pelo guard de integridade de duração.

### F. Cache TTS antigo poderia perpetuar um áudio truncado

Cache mudou de `tts-v8` para `tts-v9`.
Assim, um WAV curto aceito por uma versão anterior não é reutilizado pela correção.

### G. SFX em narração fragmentada estava com faixa incorreta

Ao trocar de bloco, a lista preparada de efeitos era filtrada pela extensão da cena inteira.
Isso podia associar efeitos de outros trechos ao bloco atual.

Agora cada bloco recebe somente eventos com `charIndex` dentro do próprio intervalo.

### H. Estado visual de início

O pedido de início agora tem um estado explícito:

- **Gemini criando a cena inicial…**;
- a crônica só abre automaticamente depois que um novo `currentNode` existe;
- em erro, o estado pendente é liberado;
- o diagnóstico fica visível e o botão volta a ficar utilizável.

A pegada é marcador pré-início. Com cena criada e personagens na Companhia, o mapa deixa de usar a pegada como marcador principal.

## Modelos configuráveis

O app não fica preso ao ID compilado.

Configurações expõe campos editáveis para:

- texto econômico;
- texto principal / cenas importantes;
- TTS.

Também há testes diretos:

- **Testar modelo de texto principal**;
- **Testar modelo TTS com Charon**.

Padrões do build:

- texto econômico: `gemini-3.5-flash-lite`;
- texto principal: `gemini-3.5-flash`;
- TTS: `gemini-3.1-flash-tts-preview`.

## Gate antes de entregar APK

Não considerar concluído enquanto estes itens não passarem:

- patch sem FAIL;
- transpilação web;
- build Android;
- applicationId igual;
- versionCode 49012 -> 49013;
- assinatura igual à versão instalada;
- IndexedDB `braseiro-db`, schema 19;
- gate de modelo manual de texto;
- gate de modelo manual TTS;
- gate de TTS em blocos;
- gate de integridade de duração;
- gate de abertura sem fallback local silencioso;
- release publicado somente depois de todos os passos acima.

## Limitação de validação

O CI consegue validar código, transpilação, Android, assinatura e o caminho completo da ponte.
Uma chamada real ao Gemini com a chave privada do aparelho só pode ser validada no próprio aparelho. Por isso a 4.9.13 inclui os dois testes diretos de modelo na tela de Configurações e mostra o modelo usado na própria narração.
