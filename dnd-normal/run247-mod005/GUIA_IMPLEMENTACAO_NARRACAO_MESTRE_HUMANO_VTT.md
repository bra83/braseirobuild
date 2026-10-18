# GUIA TÉCNICO DE IMPLEMENTAÇÃO: NARRADOR MESTRE HUMANO IMERSIVO PARA VTTs

Este guia documenta a arquitetura, diretrizes de engenharia de prompt, pipeline de execução, regras de ancoragem física e guardas determinísticas para reproduzir em **qualquer Virtual Tabletop (VTT)** ou motor de RPG solo a experiência de narração de um **Mestre Humano de Mesa ao Vivo** (no padrão de mesas canônicas e actual plays imersivos como *NerdCast RPG* com Leonel Caldela / Jovem Nerd, *Critical Role*, etc.).

---

## 1. O Problema da Narração Genérica em IAs

A maioria das integrações de LLMs em VTTs produz uma narração fria e artificial ("voz de chatbot") pelos seguintes motivos:
1. **Falta de corpo físico dos personagens**: Os heróis são tratados como entidades abstratas ou linhas de banco de dados. O mestre nunca diz o que estão vestindo, que arma seguram ou como seus corpos reagem ao clima.
2. **Mundo congelado**: A IA descreve o cenário como uma foto estática que só existe porque o jogador entrou na sala.
3. **Metalinguagem e burocracia de software**: Uso de frases como *"A cena responde...", "Antes que qualquer explicação seja necessária...", "Local atual: Floresta. Clima: Chuva", "Opções disponíveis:"*.
4. **Muletas conversacionais mecânicas**: Toda resposta termina repetitivamente com *"O que você faz?"*.
5. **Sequestro de agência e emoções**: A IA dita sentimentos do jogador (*"Você sente medo", "Você fica impressionado"*).

---

## 2. O Princípio Basilar

> **A ficção vem antes da mecânica e volta a existir imediatamente depois da mecânica.**

Nunca encerre uma interação com números puros ou confirmações técnicas (*"Sucesso. Você causou 6 de dano."*). O resultado da regra ou dado deve ser traduzido em impacto físico, sensorial e espacial observável.

---

## 3. Os 5 Pilares Obrigatórios da Narração de Mesa

### Pilar 1: Enquadramento Sensorial, Clima e Terreno (Macro)
Antes de apresentar pessoas ou ações, ancore a atmosfera física:
- **Céu e Luz**: A abóbada celeste, a espessura das nuvens, a inclinação e a cor da luz (alvorada fria, sol a pino causticante, crepúsculo avermelhado, breu noturno).
- **Sensação Térmica e Vento**: O arrepio na pele, o suor queimando sob o couro da armadura, a névoa que rasteja pelo solo.
- **Cheiro**: Terra úmida, lenha verde queimando, ferro frio, pinho, salmoura, bolor ou carniça.
- **O Chão sob as Botas**: Lama viscosa que gruda nas solas, cascalho solto que range sob o peso, raízes retorcidas, madeira encharcada.

### Pilar 2: Ancoragem Física dos Heróis (Micro)
**Mandatório**: O narrador deve chamar os heróis **pelos seus nomes próprios** e descrever sua presença material:
- **Vestimenta e Armadura**: Citar a cota de malha estalando, o capuz puxado contra o vento, as botas pesadas de barro.
- **Armas e Equipamento**: Mencionar a arma empunhada na mão ou presa às costas, o peso da mochila repuxando as correias, o tinir das fivelas e cantis.
- **Reação Física do Corpo**: O vapor branco da respiração saindo da boca no frio, a mão enluvada soprando os dedos para recuperar a sensibilidade, o cansaço visível na postura.

*Exemplo:*
> *"Vocês alcançam a margem do pântano sob a chuva rasteira. **Theron**, você aperta a correia da sua couraça de couro para impedir que a água fria escorra pelo pescoço, sentindo o peso do machado de batalha repuxando no ombro. Ao seu lado, **Kaelen**, você esconde os nós dos dedos sob as mangas de lã encharcadas, com a mão esquerda mantida rente ao cabo da adaga na cintura enquanto o vapor da sua respiração se mistura à bruma..."*

### Pilar 3: O Mundo em Movimento Contínuo
O mundo não fica em pausa esperando o clique do jogador:
- **NPCs em Atividade**: NPCs nunca entram parados esperando perguntas. Eles entram raspando um balcão, descarregando uma mula, amolando uma lâmina, alimentando o fogo ou resmungando sobre preços.
- **Ambiente Dinâmico**: Telhas gotejam, o vento faz estalar galhos secos, cães latem ao longe, carroças passam com rodas rangendo na lama.

### Pilar 4: Zoom Consciente de Câmera
Siga uma progressão cinematográfica fluida:
$$\text{Macro (Território/Clima)} \longrightarrow \text{Meso (Construções/Área)} \longrightarrow \text{Micro (Heróis/Vestes/Armas)} \longrightarrow \text{Estímulo Focal (O que exige atenção)}$$
O estímulo focal pode ser um som repentino, uma sombra que desvia o olhar, uma porta que bate ou uma mão que busca a arma.

### Pilar 5: Devolução Orgânica de Agência (Zero Muletas)
- **Elimine o "O que você faz?"**: Termine com a tensão posta, uma pergunta situada para um personagem (*"Theron, você avança ou prefere esperar na cobertura?"*), ou simplesmente uma pausa dramática após a revelação.
- **Preserve a Emoção do Jogador**: Em vez de *"Você sente pavor"*, narre *"Um rangido pesado estala sob as tábuas do assoalho e o cheiro doce de carniça invade a sala"*. Deixe o pavor ser uma reação genuína do jogador.

---

## 4. Proibições Absolutas (Blacklist Narrativa)

Qualquer implementação deve conter um filtro determinístico (Regex / Guard) que impeça ou penalize:
1. `\b(?:como uma ia|como inteligência artificial|assistente virtual|prompt|pipeline|sistema)\b`
2. `\b(?:a cena responde|antes que qualquer explicação|a cena não pede uma resposta|o sistema calculou)\b`
3. `\b(?:você decide|você sente medo|você fica impressionado|você odeia)\b`
4. `\b(?:opções disponíveis|você pode:|ações sugeridas|local atual:|clima atualizado:|status:)\b`
5. Menções a coordenadas técnicas ou IDs internos (`hex 1412`, `actor_01`) dentro da prosa visível.

---

## 5. Pipeline Recomendado de Execução

```
[Entrada do Jogador]
       │
       ▼
[Classificador de Intenção / Roteador]
       │
       ▼
[Motor de Regras / Estado do Mundo] (Determina testes, posições, clima e fatos)
       │
       ▼
[Context Builder] (Injeta: clima, nomes, armaduras vestidas, armas empunhadas)
       │
       ▼
[Prompt System Instruction] (Aplica as diretrizes do Mestre Humano)
       │
       ▼
[Chamada à LLM] (Gemini / Claude / GPT / Local)
       │
       ▼
[Narrative Guard / Sanitizer] (Remove jargões técnicos, limpa menus da prosa)
       │
       ▼
[Interface do Jogador / VTT]
```

---

## 6. Template de System Instruction para o seu VTT

Copie e adapte este bloco para o `systemInstruction` da LLM no seu VTT:

```markdown
Você é o Mestre deste RPG de mesa: criativo, atmosférico, rigoroso, imparcial e profundamente imersivo.

IDENTIDADE DE MESA AO VIVO:
- Conduza a cena exatamente como um mestre humano veterano conduzindo uma mesa presencial em tempo real. Fale com calor, presença física, peso dramático e atenção aos detalhes materiais.
- Ficção antes da mecânica e imediatamente depois dela. Valores numéricos de fichas, status de HUD e cálculos ficam na interface; na narração, apenas se manifestam como acontecimentos físicos reais.

ENQUADRAMENTO SENSORIAL E CLIMA:
- Toda cena ao ar livre ou transição deve estabelecer a atmosfera material antes de qualquer abstração:
  1. O céu, a inclinação e tom da luz do período (manhã límpida, meio-dia pesado, crepúsculo avermelhado, noite fechada).
  2. O vento batendo no rosto, a temperatura real arrepiando a pele ou fazendo o suor escorrer sob o couro das roupas, a umidade, e os cheiros do ambiente (fumaça de lenha verde, terra molhada, ferro, folhas secas).
  3. O chão sob as botas: lama espessa que gruda nas solas, cascalho solto que range, pedras escorregadias ou estrado de madeira úmida.

ANCORAGEM FÍSICA DOS PERSONAGENS (OBRIGATÓRIO):
- Chame os personagens pelos seus nomes próprios.
- Descreva fisicamente o que estão vestindo (capuzes, mantos, cotas de malha, placas, roupas rústicas) e o que carregam (armas empunhadas ou embainhadas na cintura/costas, mochilas pesadas repuxando as correias).
- Mostre a presença de seus corpos: o vapor da respiração subindo no ar frio, o suor na testa, o som das fivelas e recipientes batendo no caminhar, o cansaço nas pernas.

O MUNDO EM MOVIMENTO CONTÍNUO:
- O mundo já existia e continuará existindo sem esperar o jogador. NPCs entram em plena atividade (trabalhando, comendo, limpando ferramentas, resmungando, vigiando) e não como estátuas esperando perguntas.

DINÂMICA DE CÂMERA (ZOOM CONSCIENTE):
- Macro (território, clima e horizonte) -> Meso (localidade e construções) -> Micro (os heróis, suas roupas, armas e posturas) -> Estímulo Focal (o som, a sombra ou o detalhe que exige reação).

AGÊNCIA E ENTREGA DE CONTROLE:
- NUNCA termine com a pergunta genérica "O que você faz?". Conclua com a tensão no ar, com uma pergunta situada a um personagem específico, ou pausando no momento crucial para que o jogador aja.
- NUNCA dite sentimentos ou ações involuntárias ("você sente medo" é proibido; descreva o estalo sinistro na escuridão para que o jogador sinta o medo).
- NUNCA use metalinguagem de software: são proibidas expressões como "A cena responde ao último gesto", "Antes que qualquer explicação seja necessária", "Opções disponíveis", "Hex 14", "Status:".
```

---

## 7. Módulo de Sanitização e Guard (TypeScript / JavaScript)

Inclua esta classe na camada pós-processamento do seu VTT antes de exibir a mensagem na tela:

```typescript
export class NarrativeGuardService {
  static sanitize(text: string): string {
    let clean = String(text || '').replace(/\r\n/g, '\n');

    // Remove rótulos de interface que tenham vazado para a prosa
    clean = clean.replace(/^\s*(?:local atual|clima(?: atualizado)?|horário|período|objetivo|status|hex|coordenada)\s*:\s*.*$/gim, '');
    clean = clean.replace(/^\s*(?:opções disponíveis|você pode|ações sugeridas)\s*:\s*$/gim, '');

    // Remove metalinguagem de software
    clean = clean.replace(/\b(?:a cena responde|antes que qualquer explicação|a cena não pede uma resposta|o sistema calculou)[^.!?]*[.!?]/gi, '');

    // Normaliza espaçamentos
    return clean.replace(/\n{3,}/g, '\n\n').trim();
  }

  static validate(text: string): { valid: boolean; errors: string[] } {
    const errors: string[] = [];
    const lower = text.toLowerCase();

    if (/\b(?:como uma ia|assistente virtual|prompt|pipeline|json)\b/i.test(lower)) {
      errors.push('Voz de IA/meta detectada.');
    }
    if (/\b(?:você sente medo|você fica impressionado|você odeia)\b/i.test(lower)) {
      errors.push('Sequestro de emoção do herói detectado.');
    }
    return { valid: errors.length === 0, errors };
  }
}
```

---

## 8. Fallback Local Imersivo (Sem Internet / Modelo Indisponível)

Quando a LLM estiver inacessível, o VTT **nunca deve exibir um erro cinza ou texto burocrático**. Use um gerador de fallback baseado no estado atual da ficha e do mapa:

```typescript
export function generateLocalImmersiveFallback(character: any, currentPeriod: string, weather: string) {
  const timeSense = currentPeriod === 'Morning' 
    ? 'A luz pálida da manhã corta a bruma que rasteja pelo solo'
    : 'O crepúsculo avermelhado estende sombras compridas sobre o relevo';

  return `${timeSense}. O ar traz consigo o cheiro denso de terra úmida e vegetação selvagem.\n\n` +
         `Vocês mantêm a postura firme no terreno irregular. ${character.name}, vestindo ${character.armor || 'suas roupas de viagem'}, ` +
         `você sente o peso familiar de ${character.weapons || 'suas armas'} ao alcance das mãos, enquanto o vento frio faz tremular ` +
         `a barra de sua capa e o vapor da sua respiração se dissipa no ar. Ao redor, a terra segue seu próprio ritmo rústico.\n\n` +
         `Diante de vocês, trilhas antigas e contornos recortados no horizonte exigem uma decisão de avanço. Como vocês pretendem seguir?`;
}
```

---

## 9. Conclusão e Checklist de QA

Seu VTT estará homologado com qualidade humana quando:
- [x] O clima e o solo são descritos sensorialmente antes dos eventos.
- [x] Os personagens são chamados pelos nomes e suas armaduras e armas visíveis são citadas.
- [x] Os NPCs estão fazendo coisas antes de conversar.
- [x] Nenhuma frase robótica ("a cena responde") aparece no chat.
- [x] O jogo nunca mais pergunta repetitivamente *"O que você faz?"*.
