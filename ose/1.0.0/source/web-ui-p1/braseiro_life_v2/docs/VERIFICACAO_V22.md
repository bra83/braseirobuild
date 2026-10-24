# Verificação V2.2

134 testes passaram; zero falhas. A suíte inclui uma varredura de 900 cruzamentos entre 20 classes de causa, cinco épocas e nove perfis.

## Seis pontos verificados

1. Cooldown pela ocorrência mais recente, com isolamento por personagem.
2. Revalidação de propostas e escolhas conforme fatos, tempo, capacidades e limites atuais.
3. 100 situações distintas por perfil, com 500 entradas temáticas; 720 novos núcleos editoriais no conjunto dos nove perfis.
4. Automação por eventos, descanso, viagem, visita, passagem do tempo e preparação de cena; intervalos e resultado sem acontecimento.
5. Arcos persistentes com latência, progresso por evidência, encerramento e reabertura por gatilho confirmado.
6. Pesos por personagem, vínculos, acontecimentos e escolhas anteriores, sem conceder ações ou efeitos mecânicos.

## Regressão e persistência

- As duas falhas reproduzidas na revisão têm testes específicos.
- Falha de asset ou gravação não salva metade do pipeline automático.
- Reprocessamento não duplica sorteio ou progresso do arco.
- Propostas legadas sem garantias atuais exigem novo sorteio, preservando o histórico.
- A resposta cruzada chega ao Mestre e à cena como sugestão, sem ser uma ação consumada.
- A demonstração automática passou por três reaberturas reais do save.
- O núcleo V1 permanece byte a byte igual ao recebido.
- Hashes dos assets e integridade do ZIP foram conferidos.

Esta é a verificação do núcleo isolado em Node. Não certifica integração aos APKs, IndexedDB em WebView ou execução Android física.
