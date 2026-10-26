# USER FIXES 1.3.2

Fechamento solicitado após o primeiro smoke test em aparelho.

- MENU é a entrada padrão; nenhuma aventura é empurrada automaticamente.
- NOVA AVENTURA ALEATÓRIA cria um seed novo, persiste histórico e só libera a CENA depois de uma resposta válida do Motor.
- Clima inicial varia entre céu limpo, nublado, garoa, chuva, chuva forte, neblina, vento frio e neve leve; horário e luz agora são coerentes.
- CRIAR KID ALEATÓRIO PELAS REGRAS gera idade, atributos, perícias, Sorte e Type dentro dos limites Tales/Kids e valida antes de aplicar.
- Ícone de ajuda no header abre ORDEM DE JOGO.
- Hambúrguer de qualquer tela retorna ao MENU; Biblioteca também ganhou VOLTAR ÀS CONFIGURAÇÕES.
- O texto da Narrativa do Mestre tem rolagem interna real; a barra ciano acompanha a posição.
- Decisões e ação livre chamam `submitPlayerIntent`; o status do Motor fica visível na CENA.
- O prompt enviado ao Gemini para ilustração não contém o texto do Mestre, apenas título/local/época/clima/horário/luz/referência visual da cena.
- Motor Barbara pede abertura longa (aprox. 700–1100 palavras) e cenas importantes mais desenvolvidas (aprox. 250–650), com geografia, ambiente, pessoas, relações e detalhes sensoriais.
- Gemini TTS é o padrão. Falha do Gemini é exibida; não existe fallback silencioso para a voz Android. Android TTS só toca se o usuário selecionar explicitamente esse modo.
- `compileSdk = 36` ficou em formato detectável pelo builder GitHub.

## QA

- `USER_REQUEST_1.3.2_AUDIT.json`: PASS
- `RANDOM_KID_10000_PASS`
- `ADVENTURE_10000_UNIQUE_PASS`
- 8/8 climas observados no harness
- Tales engine: 113 PASS / 0 FAIL
- Flood isolated core: 46 PASS / 0 FAIL
- biblioteca base: 48/48 PASS
- expansão da biblioteca: 78/78 PASS
- auditorias estática, produto, firewall e visual: PASS

O build Android completo continua sendo um gate externo porque este ambiente não possui a distribuição Gradle 9.3.1 em cache e não consegue baixá-la de `services.gradle.org`.
